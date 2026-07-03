#!/usr/bin/env python3
"""Reproduce the int8 seg_net checkpoint shipped in ./models.

Takes oemer's official fp32 seg_net ONNX checkpoint (2nd_model.onnx),
upgrades it to opset 13 (per-channel DequantizeLinear needs it), and
statically quantizes it to int8 (QDQ, per-channel weights), calibrating
with real patches from a sample sheet-music image.

Measured on the oemer sample photo: 38.4 MB -> 10.3 MB with 99.96%
pixelwise argmax agreement against fp32.

unet_big (1st_model.onnx) is deliberately NOT quantized: int8 collapses its
staffline/symbol channels to near-zero recall (~0.7%/0.4%) regardless of
calibration method or first/last-layer exclusions, so it ships fp32.

Note quantization is a download-size optimization only — measured inference
speed was the same or slightly slower than fp32 (QDQ overhead cancels the
int8 compute win on this architecture).

Usage:
    pip install onnx onnxruntime opencv-python-headless numpy
    python3 tools/quantize_models.py <2nd_model.onnx> <sample_image>

The fp32 checkpoint comes from
https://github.com/BreezeWhite/oemer/releases/tag/checkpoints
"""

import sys
from pathlib import Path

import numpy as np
import cv2
import onnx
from onnx import version_converter
from onnxruntime.quantization import (
    CalibrationDataReader,
    QuantFormat,
    QuantType,
    quantize_static,
)
from onnxruntime.quantization.shape_inference import quant_pre_process

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "models"
WIN = 288  # seg_net input window


def resize_like_oemer(image):
    """oemer resizes inputs to ~3.6 Mpx; calibrate at the same scale."""
    h, w = image.shape[:2]
    pis = w * h
    ratio = (((3000000 / pis) + (4350000 / pis)) / 2) ** 0.5
    return cv2.resize(image, (round(ratio * w), round(ratio * h)))


def patches_for(image, win, step=128, limit=48):
    out = []
    for y in range(0, image.shape[0], step):
        if y + win > image.shape[0]:
            y = image.shape[0] - win
        for x in range(0, image.shape[1], step):
            if x + win > image.shape[1]:
                x = image.shape[1] - win
            out.append(image[y : y + win, x : x + win])
    idx = np.linspace(0, len(out) - 1, min(limit, len(out))).astype(int)
    return [out[i] for i in idx]


class Reader(CalibrationDataReader):
    def __init__(self, patches):
        self.it = iter([{"input": p[None].astype(np.uint8)} for p in patches])

    def get_next(self):
        return next(self.it, None)


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    src, img_path = sys.argv[1:3]
    image = resize_like_oemer(cv2.imread(img_path))
    OUT.mkdir(exist_ok=True)

    dst = OUT / "seg_net_int8.onnx"
    print("opset upgrade to 13…")
    model = version_converter.convert_version(onnx.load(src), 13)
    up = str(OUT / "seg_net_op13.tmp.onnx")
    onnx.save(model, up)
    pre = str(OUT / "seg_net_pre.tmp.onnx")
    print("preprocess…")
    quant_pre_process(up, pre, skip_symbolic_shape=True)
    print("calibrate + quantize…")
    quantize_static(
        pre,
        str(dst),
        Reader(patches_for(image, WIN)),
        quant_format=QuantFormat.QDQ,
        activation_type=QuantType.QUInt8,
        weight_type=QuantType.QInt8,
        per_channel=True,
    )
    Path(up).unlink()
    Path(pre).unlink()
    print(f"-> {dst} ({dst.stat().st_size/1e6:.1f} MB)")


if __name__ == "__main__":
    main()
