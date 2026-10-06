"""Compare visible SW/JEG HUD anchors in real screenshots (Pillow required).

Values, weapon names, bindings, and part-health outlines are deliberately not
image-equality targets: JEG must display its own state. Camera CSV supplies the
GUI scale; every tested glyph/line must be within one GUI pixel in both images.
"""
import argparse
import csv
import json
from pathlib import Path
from PIL import Image

SEATS = dict(bmp2=7, lav150=5, truck=2, speedboat=5, mi28=2, ah6=4)


def mask(image, scale, region, color):
    x0, y0, x1, y1 = region
    pixels = image.load()
    result = set()
    for y in range(max(0, int(y0*scale)), min(image.height, int(y1*scale))):
        for x in range(max(0, int(x0*scale)), min(image.width, int(x1*scale))):
            r, g, b = pixels[x, y][:3]
            # SW's TV texture darkens earlier layers; compare hue/geometry.
            match = (r > 80 and b < 20 and abs(g/r-199/255) < .06) if color == "yellow" else (g > 80 and b < 20 and abs(r/g-.4) < .06) if color == "green" else min(r, g, b) > (240 if color == "white" else 100) and max(r, g, b)-min(r, g, b) < 2
            if match: result.add((int(x/scale), int(y/scale)))
    return result


def error(first, second):
    if not first and not second: return 0
    if not first or not second: return 3  # Missing, or farther than the two-pixel search.
    for distance in range(3):
        offsets = [(x, y) for x in range(-distance, distance+1) for y in range(-distance, distance+1)]
        if all(any((x+dx, y+dy) in second for dx, dy in offsets) for x, y in first) and all(any((x+dx, y+dy) in first for dx, dy in offsets) for x, y in second):
            return distance
    return 3


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reference", type=Path)
    parser.add_argument("actual", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    reference = {r[0]: r for r in csv.reader((args.reference/"camera.csv").open())}
    actual = {r[0]: r for r in csv.reader((args.actual/"camera.csv").open())}
    checks, failures = [], []
    for name, row in actual.items():
        if name.endswith("inventory"): continue
        vehicle, seat_text, _ = name.split("-", 2)
        seat = int(seat_text.removeprefix("seat"))
        w, h = map(int, row[7:9])
        assert reference[name][7:9] == row[7:9], "Use identical GUI settings"
        regions = [("seat numbers", (10, h-35-(SEATS[vehicle]-1)*12, 26, h-26), "green")]
        for label, y in (("energy", h-21), ("health", h-12)):
            regions.extend([(label+" frame top", (20, y, 80, y+1), "gray"),
                            (label+" frame bottom", (20, y+5, 80, y+6), "gray")])
        sight = "first_person" in name or "-aim" in name and row[14] == "true"
        pilot = vehicle in ("mi28", "ah6") and seat == 0
        land = vehicle in ("bmp2", "lav150") and seat == 0
        gunner = vehicle == "mi28" and seat == 1 and row[14] == "true"
        color = "yellow" if vehicle in ("bmp2", "mi28") else "green"
        if sight and (pilot or land or gunner):
            y = 6 if pilot else 10
            regions.append(("compass", (w/2-128, y, w/2+128, y+16), color))
            regions.append(("reticle and attitude", (w/2-64, h/2-48, w/2+64, h/2+22), color))
            if pilot:
                regions.append(("power ruler", (w/2+126, h/2-62, w/2+145, h/2+64), color))
                regions.append(("speed frame", (w/2-144, h/2-6, w/2-94, h/2+12), color))
            else:
                regions.append(("sight baseline", (w/2-64, h-57, w/2+64, h-55), color))
        elif sight and (pilot or land or vehicle in ("bmp2", "mi28") and seat == 1 or vehicle == "speedboat" and seat == 0):
            regions.append(("reticle", (w/2-70, h/2-48, w/2+70, h/2+35), "white" if vehicle == "speedboat" else color))
        images = [Image.open(p/"screenshots"/(name+".png")).convert("RGB") for p in (args.reference, args.actual)]
        scales = [im.height/h for im in images]
        for label, region, mask_color in regions:
            masks = [mask(im, scale, region, mask_color) for im, scale in zip(images, scales)]
            distance = error(*masks)
            item = dict(state=name, component=label, max_error_gui_pixels=distance, reference_pixels=len(masks[0]), actual_pixels=len(masks[1]))
            checks.append(item)
            if distance > 1: failures.append(item)
    result = dict(states=len(actual), anchors=len(checks), max_error_gui_pixels=max(c["max_error_gui_pixels"] for c in checks), failures=failures)
    args.output.write_text(json.dumps(result, indent=2)+"\n", encoding="utf8")
    print(json.dumps({k: v for k, v in result.items() if k != "failures"} | {"failure_count": len(failures)}, indent=2))
    assert not failures, "HUD anchor tolerance exceeded"


if __name__ == "__main__": main()
