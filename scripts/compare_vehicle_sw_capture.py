"""Compare real client camera CSVs and server control recordings with fixed SW captures."""
import argparse
import csv
import json
import math
from pathlib import Path


def cameras(reference, actual):
    expected = {r[0]: r for r in csv.reader(reference.open(encoding="utf8")) if not r[0].endswith("inventory")}
    observed = {r[0]: r for r in csv.reader(actual.open(encoding="utf8")) if not r[0].endswith("inventory")}
    assert expected.keys() == observed.keys(), "Camera states missing or duplicated"
    expected_rows = [r for r in csv.reader(reference.open(encoding="utf8")) if not r[0].endswith("inventory")]
    actual_rows = [r for r in csv.reader(actual.open(encoding="utf8")) if not r[0].endswith("inventory")]
    assert len(expected_rows) == len(expected) and len(actual_rows) == len(observed), "Duplicate camera states"
    failures = []
    max_distance = max_angle = 0.0
    for name, row in expected.items():
        got = observed[name]
        distance = math.dist(map(float, row[1:4]), map(float, got[1:4]))
        angle = max(abs((float(row[i]) - float(got[i]) + 180) % 360 - 180) for i in (4, 5))
        if len(row) >= 20 and len(got) >= 20:
            before, after = list(map(float, row[16:20])), list(map(float, got[16:20]))
            cosine = abs(sum(a*b for a,b in zip(before,after))) / math.sqrt(sum(a*a for a in before)*sum(b*b for b in after))
            angle = max(angle, math.degrees(2*math.acos(min(1, cosine))))
        assert math.isfinite(distance) and math.isfinite(angle), name
        max_distance = max(max_distance, distance)
        max_angle = max(max_angle, angle)
        if distance > .01 or angle > .1:
            failures.append({"state": name, "distance_m": distance, "angle_deg": angle})
    return {"states": len(expected), "max_distance_m": max_distance, "max_angle_deg": max_angle, "failures": failures}


def motion(reference, actual):
    expected = {(r[0], int(r[1])): list(map(float, r[2:])) for r in csv.reader(reference.open(encoding="utf8"))}
    observed = {(r[0], int(r[1])): list(map(float, r[2:])) for r in csv.reader(actual.open(encoding="utf8"))}
    assert expected.keys() == observed.keys(), "Control samples missing"
    assert sum(1 for _ in reference.open(encoding="utf8")) == len(expected), "Duplicate reference motion samples"
    assert sum(1 for _ in actual.open(encoding="utf8")) == len(observed), "Duplicate actual motion samples"
    assert all(math.isfinite(value) for rows in (expected,observed) for row in rows.values() for value in row), "Non-finite motion samples"
    results = {}
    for vehicle in sorted({key[0] for key in expected}):
        keys = [key for key in expected if key[0] == vehicle]
        peak_speed = max(math.sqrt(sum(v * v for v in expected[key][3:6])) for key in keys)
        speed_error = angle_error = 0.0
        for key in keys:
            before, after = expected[key], observed[key]
            speed_error = max(speed_error, math.dist(before[3:6], after[3:6]))
            angle_error = max(angle_error, *(abs((before[i] - after[i] + 180) % 360 - 180) for i in (6, 7, 8)))
        ratio = speed_error / max(peak_speed, .001)
        results[vehicle] = {"samples": len(keys), "peak_speed_blocks_per_tick": peak_speed,
                            "max_velocity_error_fraction": ratio, "max_angle_error_deg": angle_error,
                            "passed": ratio <= .01 and angle_error <= .1}
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reference", type=Path)
    parser.add_argument("actual", type=Path)
    parser.add_argument("--motion", action="store_true")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = motion(args.reference, args.actual) if args.motion else cameras(args.reference, args.actual)
    text = json.dumps(result, indent=2)
    print(text)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text + "\n", encoding="utf8")
    if args.motion:
        assert all(row["passed"] for row in result.values()), "Motion tolerance exceeded"
    else:
        assert not result["failures"], "Camera tolerance exceeded"


if __name__ == "__main__":
    main()
