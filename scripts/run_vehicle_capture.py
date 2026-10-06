"""Record every vehicle seat/weapon/view, or fixed server-tick controls, in a copied flat world."""
import argparse
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--world", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--mode", choices=("static", "drive"), default="static")
    parser.add_argument("--handheld", action="store_true")
    parser.add_argument("--hud-only", action="store_true")
    parser.add_argument("--survival", action="store_true")
    parser.add_argument("--tilt", action="store_true")
    parser.add_argument("--width", type=int, default=854)
    parser.add_argument("--height", type=int, default=480)
    parser.add_argument("--gui-scale", type=int, default=0)
    args = parser.parse_args()
    assert not (args.hud_only and args.mode == "drive"), "HUD check needs static input"
    assert not (args.handheld and args.mode == "drive"), "Handheld check is a static Truck fixture"
    module = Path(__file__).resolve().parents[1]
    output = args.output.resolve()
    assert not (output / "camera.csv").exists() and not (output / "motion.csv").exists(), "Use a fresh capture directory"
    output.mkdir(parents=True, exist_ok=True)
    runtime = Path(tempfile.mkdtemp(prefix="runtime-", dir=output))
    shutil.copytree(args.world, runtime / "saves/Fixture", ignore=shutil.ignore_patterns("session.lock"))
    options = (module / "run/options.txt").read_text(encoding="utf8")
    overrides = {"pauseOnLostFocus": "false", "guiScale": str(args.gui_scale), "overrideWidth": "0", "overrideHeight": "0"}
    lines = [line.split(":", 1)[0] + ":" + overrides[line.split(":", 1)[0]] if line.split(":", 1)[0] in overrides else line for line in options.splitlines()]
    (runtime / "options.txt").write_text("\n".join(lines) + "\n", encoding="utf8")
    command = [str(module / "gradlew.bat"), "runClient", "--no-configuration-cache", "--no-problems-report", "--console=plain",
               "--init-script", str(module / "scripts/vehicle_capture.init.gradle"),
               "-PvehicleCapture=" + str(output), "-PcaptureRunDir=" + str(runtime), "-PcaptureWorld=Fixture",
               "-PcaptureMode=" + args.mode, "-PcaptureTilt=" + str(args.tilt).lower(),
               "-PcaptureHandheld=" + str(args.handheld).lower(), "-PcaptureHudOnly=" + str(args.hud_only).lower(), "-PcaptureSurvival=" + str(args.survival).lower(), "-PcaptureWidth=" + str(args.width), "-PcaptureHeight=" + str(args.height)]
    with (output / "runClient.log").open("w", encoding="utf8") as log:
        result = subprocess.run(command, cwd=module, stdout=log, stderr=subprocess.STDOUT, timeout=900)
    assert result.returncode == 0, output / "runClient.log"
    csv_file = output / ("motion.csv" if args.mode == "drive" else "camera.csv")
    assert csv_file.exists(), "Client exited without recording fixtures"
    print(module.name + ": runClient and " + args.mode + " capture completed", flush=True)


if __name__ == "__main__": main()
