"""Check six maintained AI ports and optional real-client patrol/combat recordings (stdlib only)."""
import argparse
import csv
import hashlib
import json
import math
import re
from pathlib import Path


def method(source, name):
    start = re.search(r"^    (?:private|public) static \S+ " + re.escape(name) + r"\(", source, re.MULTILINE).start()
    start = source.index("{", start)
    depth = 1
    end = start + 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    return source[start:end]


def recording(directory, module):
    hashes = json.loads((directory / "source-hashes.json").read_text(encoding="utf8"))
    assert all(hashlib.sha256((module / path).read_bytes()).hexdigest() == value for path, value in hashes.items()), "Runtime evidence predates source changes"
    motion = list(csv.reader((directory / "motion.csv").open(encoding="utf8")))
    combat = list(csv.reader((directory / "combat.csv").open(encoding="utf8")))
    results = {}
    for name in ("bmp2", "lav150", "truck", "speedboat", "mi28", "ah6"):
        rows = [row for row in motion if row[0] == name]
        shots = [row for row in combat if row[0] == name]
        air = name in ("mi28", "ah6")
        count = 601 if air else 401
        assert [int(row[1]) for row in rows] == list(range(count)), (name, "Missing motion samples")
        assert [int(row[1]) for row in shots] == list(range(count)), (name, "Missing combat samples")
        assert all(math.isfinite(float(value)) for row in rows for value in row[2:]), name
        distance = math.dist(map(float, rows[0][2:5]), map(float, rows[-1][2:5]))
        speed = max(math.hypot(float(row[5]), float(row[7])) for row in rows)
        roll = max(abs(float(row[10])) for row in rows)
        fire_ticks = sum(row[2] == "true" for row in shots)
        assert distance > 3, (name, "AI did not navigate")
        assert all(row[2] == "false" for row in shots[:201]), (name, "AI attacked a creative player")
        assert (fire_ticks == 0) if name == "truck" else (fire_ticks > 0), (name, "Weapon engagement mismatch")
        assert min(float(row[7]) for row in shots) > 0 and min(float(row[8]) for row in shots) > 0, (name, "Fixture exhausted or destroyed")
        if air:
            altitude = [float(row[3]) + 60 for row in rows]
            assert min(altitude) > 8 and max(altitude) < 65, (name, "Unsafe altitude")
            assert speed < 1.05 and roll < 40, (name, "Unstable attack maneuver")
        assert len(list((directory / "motion").rglob(name + "-*.png"))) > 20, (name, "Missing rendered frames")
        results[name] = {"samples": count, "distance_m": distance, "peak_speed_blocks_per_tick": speed,
                         "peak_roll_deg": roll, "firing_ticks": fire_ticks}
    assert "BUILD SUCCESSFUL" in (directory / "runClient.log").read_text(encoding="utf8", errors="replace"), "runClient did not exit cleanly"
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--evidence", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    methods = ("tickAh6Weapons", "tickMi28Weapons", "airPatrol", "airEngage", "flySwToward", "patrol",
               "nextDryPatrolTarget", "driveToward", "updateStuckState", "isForwardUnsafe", "aimAt", "weaponAligned")
    expected = None
    report = {}
    for loader in ("NeoForge", "Fabric"):
        for version in ("1.21.1", "26.2", "26.3"):
            name = loader + "-" + version
            module = args.root / ("Just-Enough-Guns-" + name)
            source = (module / "src/main/java/ttv/migami/jeg/vehicle/ai/EnemyVehicleController.java").read_text(encoding="utf8")
            actual = {name: method(source, name) for name in methods}
            if expected is None:
                expected = actual
            assert actual == expected, (name, "AI behavior differs between ports")
            report[name] = {"matched_methods": len(methods)}
            if args.evidence:
                directory = args.evidence / name / "ai-accepted"
                if not directory.exists():
                    directory = args.evidence / name / "ai-final"
                report[name]["recording"] = str(directory)
                report[name]["runtime"] = recording(directory, module)
            print(name + ": enemy AI parity" + (" + real runClient patrol/combat passed" if args.evidence else " passed"))
    if args.output:
        args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf8")


if __name__ == "__main__":
    main()
