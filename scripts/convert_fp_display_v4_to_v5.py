#!/usr/bin/env python3
"""
Convert GeckoLib-v4 / MC 1.21.1 first-person ItemTransform JSON to MC 26.2 values.

Why conversion is needed
------------------------
1.21.1 ItemTransform.apply (used before GeckoLib v4 center):
    translate(T) → rotate(R) → scale(S)
then GeckoLib: translate(0.5, 0.51, 0.5)

MC 26.2 ItemTransform.apply always ends with an extra pad:
    translate(T) → rotate(R) → scale(S) → translate(-0.5, -0.5, -0.5)
then GeckoLib: translate(0.5, 0.51, 0.5)

With the same JSON T, the 26.2 chain is not equal to the 1.21.1 chain.
We solve for T_fp (JSON units) such that, with the same R:

    T_fp * R * Pad(-0.5) * C  ==  T_sw * R * C

which reduces to (S = I):

    T_fp_block = T_sw_block + R * (0.5, 0.5, 0.5)
    T_fp_json  = 16 * T_fp_block

Source of T_sw / R: SuperbWarfare displaysettings or NeoForge-1.21.1 models/item
firstperson_righthand (identical for javelin / igla_9k38 in this repo).

Usage
-----
  python convert_fp_display_v4_to_v5.py
  python convert_fp_display_v4_to_v5.py --write   # write first_person/*.json under assets
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

# NeoForge-1.21.1 / SW firstperson_righthand (JSON units, degrees)
V4_SOURCES: dict[str, dict] = {
    "javelin": {
        "translation": [-3.75, -4.75, 0.75],
        "rotation": [0.0, 0.0, 4.75],
    },
    "igla_9k38": {
        "translation": [-5.75, -3.25, 7.75],
        "rotation": [0.0, 0.0, 8.0],
    },
}


def rot_z_times_vec(theta_deg: float, v: tuple[float, float, float]) -> tuple[float, float, float]:
    th = math.radians(theta_deg)
    c, s = math.cos(th), math.sin(th)
    x, y, z = v
    return (x * c - y * s, x * s + y * c, z)


def convert_v4_to_v5_translation(
    t_json: list[float],
    rot_json: list[float],
) -> list[float]:
    """Return firstperson translation JSON for MC 26.2 ItemTransform + GeckoLib center."""
    tx, ty, tz = t_json
    # Only Z rotation is used by these SW displaysettings.
    rot_z = float(rot_json[2]) if len(rot_json) > 2 else 0.0
    t_sw = (tx / 16.0, ty / 16.0, tz / 16.0)
    # inv(Pad(-0.5)) expressed in the rotated frame: R * (0.5,0.5,0.5)
    pad_inv = rot_z_times_vec(rot_z, (0.5, 0.5, 0.5))
    t_fp = (t_sw[0] + pad_inv[0], t_sw[1] + pad_inv[1], t_sw[2] + pad_inv[2])
    return [t_fp[0] * 16.0, t_fp[1] * 16.0, t_fp[2] * 16.0]


def verify(t_json: list[float], rot_json: list[float], t_fp_json: list[float], eps: float = 1e-9) -> bool:
    """Check T_fp*R*Pad*C == T_sw*R*C for sample points (pure Z rotation)."""
    rot_z = float(rot_json[2]) if len(rot_json) > 2 else 0.0
    th = math.radians(rot_z)
    c, s = math.cos(th), math.sin(th)

    def rz(v: tuple[float, float, float]) -> tuple[float, float, float]:
        x, y, z = v
        return (x * c - y * s, x * s + y * c, z)

    t_sw = (t_json[0] / 16.0, t_json[1] / 16.0, t_json[2] / 16.0)
    t_fp = (t_fp_json[0] / 16.0, t_fp_json[1] / 16.0, t_fp_json[2] / 16.0)
    pad = (-0.5, -0.5, -0.5)
    center = (0.5, 0.51, 0.5)

    def apply_v4(p: tuple[float, float, float]) -> tuple[float, float, float]:
        # T_sw * R * C * p = T_sw + R*(C+p)
        q = (center[0] + p[0], center[1] + p[1], center[2] + p[2])
        rq = rz(q)
        return (t_sw[0] + rq[0], t_sw[1] + rq[1], t_sw[2] + rq[2])

    def apply_v5(p: tuple[float, float, float]) -> tuple[float, float, float]:
        # T_fp * R * Pad * C * p = T_fp + R*(Pad+C+p)
        q = (pad[0] + center[0] + p[0], pad[1] + center[1] + p[1], pad[2] + center[2] + p[2])
        rq = rz(q)
        return (t_fp[0] + rq[0], t_fp[1] + rq[1], t_fp[2] + rq[2])

    for p in ((0.0, 0.0, 0.0), (0.1, 0.2, 0.3), (0.0, 7.5 / 16.0, 0.0)):
        a, b = apply_v4(p), apply_v5(p)
        if max(abs(a[i] - b[i]) for i in range(3)) > eps:
            return False
    return True


def first_person_model_json(gun_id: str, t_fp: list[float], rot: list[float]) -> dict:
    t = [round(x, 6) for x in t_fp]
    r = [round(x, 6) for x in rot]
    return {
        "parent": "builtin/entity",
        "credit": (
            f"Auto-converted from GeckoLib v4 / NeoForge-1.21.1 firstperson_righthand for {gun_id}. "
            "MC 26.2 ItemTransform.apply appends translate(-0.5); translation compensated so "
            "T_fp*R*Pad*C equals v4 T_sw*R*C. Script: scripts/convert_fp_display_v4_to_v5.py"
        ),
        "display": {
            "firstperson_righthand": {
                "rotation": r,
                "translation": t,
            },
            "firstperson_lefthand": {
                "scale": [0, 0, 0],
            },
        },
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "--write",
        action="store_true",
        help="Write assets/.../models/item/first_person/<id>.json next to this module",
    )
    args = parser.parse_args()

    module_root = Path(__file__).resolve().parents[1]
    out_dir = module_root / "src/main/resources/assets/jeg/models/item/first_person"

    for gun_id, src in V4_SOURCES.items():
        t_v4 = list(src["translation"])
        r_v4 = list(src["rotation"])
        t_v5 = convert_v4_to_v5_translation(t_v4, r_v4)
        ok = verify(t_v4, r_v4, t_v5)
        print(f"{gun_id}:")
        print(f"  v4 translation={t_v4} rotation={r_v4}")
        print(f"  v5 translation={[round(x, 6) for x in t_v5]} rotation={r_v4}")
        print(f"  matrix_verify={ok}")
        if not ok:
            raise SystemExit(f"verification failed for {gun_id}")
        if args.write:
            out_dir.mkdir(parents=True, exist_ok=True)
            path = out_dir / f"{gun_id}.json"
            path.write_text(json.dumps(first_person_model_json(gun_id, t_v5, r_v4), indent=2) + "\n", encoding="utf-8")
            print(f"  wrote {path}")


if __name__ == "__main__":
    main()
