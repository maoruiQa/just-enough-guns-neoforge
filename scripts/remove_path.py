#!/usr/bin/env python3
import os
import shutil
import sys

def remove(path: str) -> None:
    if not os.path.exists(path):
        return
    if os.path.isfile(path) or os.path.islink(path):
        os.remove(path)
    else:
        shutil.rmtree(path)


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: remove_path.py <path> [<path>...]")
        sys.exit(1)

    for raw in sys.argv[1:]:
        remove(os.path.abspath(raw))


if __name__ == "__main__":
    main()
