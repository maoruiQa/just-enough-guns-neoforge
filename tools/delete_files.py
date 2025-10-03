#!/usr/bin/env python3
"""Remove files or directories recursively."""
from __future__ import annotations

import os
import shutil
import sys

def remove(path: str) -> None:
    if not os.path.exists(path):
        return
    if os.path.islink(path) or os.path.isfile(path):
        os.remove(path)
    else:
        shutil.rmtree(path)

def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print('Usage: delete_files.py <path> [<path> ...]', file=sys.stderr)
        return 1
    for target in argv[1:]:
        remove(target)
    return 0

if __name__ == '__main__':
    raise SystemExit(main(sys.argv))
