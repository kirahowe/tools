"""Runtime compatibility patches for oemer 0.1.8 on modern numpy (2.x) /
OpenCV (4.10+), as shipped in Pyodide.

``apply_early()`` runs before any oemer module is imported; ``apply()`` runs
after. The same file works natively (CPython) and inside Pyodide.
"""

import os
import sys

import numpy as np
import cv2


def apply_early():
    # matplotlib is imported at module level by several oemer modules; in a
    # worker there is no DOM, so force the Agg backend.
    os.environ.setdefault("MPLBACKEND", "Agg")


def _patched_find_lines(data, min_len=10, max_gap=20):
    assert len(data.shape) == 2, f"{type(data)} {data.shape}"
    lines = cv2.HoughLinesP(
        data.astype(np.uint8), 1, np.pi / 180, 50, None, min_len, max_gap
    )
    new_line = []
    if lines is not None:
        # Older OpenCV returned shape (N, 1, 4); newer returns (N, 4).
        lines = np.asarray(lines).reshape(-1, 4)
        for line in lines:
            top_x, bt_x = (line[0], line[2]) if line[0] < line[2] else (line[2], line[0])
            top_y, bt_y = (line[1], line[3]) if line[1] < line[3] else (line[3], line[1])
            new_line.append((top_x, top_y, bt_x, bt_y))
    return new_line


def apply():
    from oemer import bbox as bbox_mod

    bbox_mod.find_lines = _patched_find_lines

    # Patch any already-imported oemer modules that bound find_lines by name.
    for name, mod in list(sys.modules.items()):
        if (
            name.startswith("oemer")
            and mod is not bbox_mod
            and getattr(mod, "find_lines", None) is not None
        ):
            mod.find_lines = _patched_find_lines
