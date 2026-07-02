"""Bridge between the JS worker and oemer's Python pipeline.

oemer's ``inference()`` is a single function that (1) slices the input image
into overlapping patches, (2) runs an ONNX session over them in batches and
(3) merges the patch predictions back into a full-image class map. Step (2)
is synchronous in Python, but in the browser the neural nets run in ONNX
Runtime Web, whose ``session.run`` is async. Rather than block on a promise
(needs JSPI, not available everywhere), the function is split at the
``sess.run`` boundary:

    load_image()  -> once per image
    prepare()     -> patches for one model            (Python)
    begin_pred()  -> allocate accumulators            (Python)
    accumulate()  -> add one batch of predictions     (JS drives, per batch)
    merge()       -> finish the class map             (Python)
    finalize()    -> run the rest of oemer with the class maps patched in

The patch layout, ordering and merging replicate oemer.inference.inference()
exactly (including its quirk of re-clamping window origins inside the loop,
which produces duplicate windows at the edges — the merge averages them out).
"""

import os
import pickle

import numpy as np
from PIL import Image
import cv2

import oemer_compat

oemer_compat.apply_early()

from oemer import MODULE_PATH  # noqa: E402
from oemer.inference import resize_image  # noqa: E402

oemer_compat.apply()

_state = {}


def _window_coords(shape, win_size, step_size):
    """Window origins in exactly the order oemer's inference() visits them."""
    coords = []
    for y in range(0, shape[0], step_size):
        if y + win_size > shape[0]:
            y = shape[0] - win_size
        for x in range(0, shape[1], step_size):
            if x + win_size > shape[1]:
                x = shape[1] - win_size
            coords.append((y, x))
    return coords


def load_image(img_path):
    """Load + resize the image once; both models reuse it (oemer does the
    same work twice, once per inference() call, with identical results)."""
    image_pil = Image.open(img_path)
    if "GIF" != image_pil.format:
        image_cv = cv2.imread(img_path)
        image_pil = Image.fromarray(image_cv)
    image_pil = image_pil.convert("RGB")
    image = np.array(resize_image(image_pil))
    _state.clear()
    _state["image"] = np.ascontiguousarray(image, dtype=np.uint8)
    return list(image.shape[:2])


def prepare(model_name, step_size=128):
    model_path = os.path.join(MODULE_PATH, "checkpoints", model_name)
    with open(os.path.join(model_path, "metadata.pkl"), "rb") as f:
        metadata = pickle.load(f)
    win_size = metadata["input_shape"][1]
    out_ch = metadata["output_shape"][-1]

    image = _state["image"]
    coords = _window_coords(image.shape, win_size, step_size)
    patches = np.empty((len(coords), win_size, win_size, 3), dtype=np.uint8)
    for i, (y, x) in enumerate(coords):
        patches[i] = image[y : y + win_size, x : x + win_size]

    _state[model_name] = {
        "win": win_size,
        "out_ch": out_ch,
        "coords": coords,
        "patches": patches,
    }
    return [len(coords), win_size, out_ch]


def get_patches(model_name):
    return _state[model_name]["patches"]


def begin_pred(model_name, max_batch):
    info = _state[model_name]
    image = _state["image"]
    shape = image.shape[:2] + (info["out_ch"],)
    info["out"] = np.zeros(shape, dtype=np.float32)
    info["mask"] = np.zeros(shape, dtype=np.float32)
    info["hop"] = 0
    info["batch_buf"] = np.zeros(
        (max_batch, info["win"], info["win"], info["out_ch"]), dtype=np.float32
    )
    return info["batch_buf"]


def accumulate(model_name, count):
    info = _state[model_name]
    win = info["win"]
    out, mask, coords = info["out"], info["mask"], info["coords"]
    for j in range(count):
        y, x = coords[info["hop"]]
        out[y : y + win, x : x + win] += info["batch_buf"][j]
        mask[y : y + win, x : x + win] += 1
        info["hop"] += 1


def merge(model_name):
    info = _state[model_name]
    assert info["hop"] == len(info["coords"]), (info["hop"], len(info["coords"]))
    out = info["out"]
    out /= info["mask"]
    info["class_map"] = np.argmax(out, axis=-1)
    for key in ("out", "mask", "batch_buf", "patches"):
        info.pop(key, None)
    return True


def finalize(img_path, deskew=True):
    from argparse import Namespace

    from oemer import ete

    results = {
        name: _state[name]["class_map"] for name in ("unet_big", "seg_net")
    }

    def fake_inference(model_path, _img_path, step_size=128, batch_size=16,
                       manual_th=None, use_tf=False):
        name = os.path.basename(os.path.normpath(model_path))
        return results[name], None

    ete.inference = fake_inference
    ete.clear_data()

    out_dir = os.path.dirname(img_path) or "."
    out_path = ete.extract(
        Namespace(
            img_path=img_path,
            output_path=out_dir,
            use_tf=False,
            save_cache=False,
            without_deskew=not deskew,
        )
    )
    with open(out_path, encoding="utf-8") as f:
        return f.read()


def reset():
    _state.clear()
