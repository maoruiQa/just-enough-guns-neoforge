"""Check all six maintained modules against fixed SW 0cfd00d5 (configuration/resources).

Use --module PATH during porting. Runtime curves and visual acceptance are separate.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys

VEHICLES = dict(bmp2="bmp_2", lav150="lav_150", truck="truck", speedboat="speedboat", mi28="mi_28", ah6="ah_6")
ENGINE = dict(Buoyancy="buoyancy", Increment="increment", Decrement="decrement", SteeringSpeed="steering_speed",
              MaxForwardSpeedRate="max_forward_speed", MaxBackwardSpeedRate="max_reverse_speed", PitchSpeed="pitch_speed",
              YawSpeed="yaw_speed", RollSpeed="roll_speed", LiftSpeed="lift_speed", BodyPitchRate="body_pitch_rate",
              BodyRollRate="body_roll_rate", WheelRotSpeed="wheel_rot_speed", WheelDifferential="wheel_differential",
              TrackRotSpeed="track_rot_speed", TrackDifferential="track_differential")

def read(path):
    return json.loads(path.read_text(encoding="utf-8"))

def same(actual, expected, label):
    assert actual == expected, f"{label}: {actual!r} != SW {expected!r}"

def verify(module, reference):
    resources = module / "src/main/resources"
    sw = reference / "src/main/resources"
    assets = {f"textures/gui/vehicle/inventory/{name}.png" for name in ("medium", "huge", "player_inventory")}
    for name, sw_name in VEHICLES.items():
        jeg = read(resources / f"data/jeg/vehicles/{name}.json")
        source = read(sw / f"data/superbwarfare/sbw/vehicles/{sw_name}.json")
        label = f"{module.name}/{name}"
        same(jeg["engine"].get("sw_controls"), True, label + "/SW controls")
        for key, target in ENGINE.items():
            if key in source["EngineInfo"]:
                same(jeg["engine"][target], source["EngineInfo"][key], label + "/" + target)
        for key, sw_key, default in (("terrain_points", "TerrainCompat", []), ("terrain_rotate_rate", "TerrainCompatRotateRate", 1), ("inertia_rotate_rate", "InertiaRotateRate", 0)):
            same(jeg["engine"][key], source.get(sw_key, default), label + "/" + key)
        same(jeg["container_type"], source.get("VehicleContainerType", "Medium").lower(), label + "/inventory")
        same([jeg["third_person_camera"][k] for k in ("x", "y", "z")], source["ThirdPersonCameraPos"], label + "/third person")
        same(jeg["turret"]["render_pivot_y"], source["RotateOffsetHeight"], label + "/pivot")
        if "TurretTurnSpeed" in source:
            same([jeg["turret"][k] for k in ("pitch_turn_speed", "yaw_turn_speed")], source["TurretTurnSpeed"], label + "/turret speed")
            same([jeg["turret"][k] for k in ("min_pitch", "max_pitch")], source["TurretPitchRange"], label + "/turret pitch")
        same(len(jeg["seats"]), len(source["Seats"]), label + "/seats")
        names = []
        for index, (seat, old) in enumerate(zip(jeg["seats"], source["Seats"], strict=True)):
            loc = label + f"/seat{index}"
            same([seat[k] for k in ("x", "y", "z")], old["Position"], loc + "/position")
            for key, sw_key, default in (("transform", "Transform", "Vehicle"), ("orientation", "Orientation", 0), ("can_rotate_head", "CanRotateHead", True), ("can_rotate_body", "CanRotateBody", False),
                                          ("min_pitch", "MinPitch", -90), ("max_pitch", "MaxPitch", 90), ("min_yaw", "MinYaw", -180), ("max_yaw", "MaxYaw", 180)):
                expected = old.get(sw_key, default)
                same(seat[key], expected.lower() if isinstance(expected, str) else expected, loc + "/" + key)
            same([seat[k] for k in ("sensitivity_x", "sensitivity_y", "sensitivity_z")], old.get("Sensitivity", [1, 1, 1]), loc + "/sensitivity")
            camera = seat["zoom_camera"]; cam = old.get("CameraPos", {})
            same([camera[k] for k in ("x", "y", "z")], cam.get("Position", [0, 0, 0]), loc + "/camera")
            same([camera[k] for k in ("zoom_x", "zoom_y", "zoom_z")], cam.get("ZoomPosition", [0, 0, 0]), loc + "/zoom")
            same(camera["has_zoom_position"], "ZoomPosition" in cam, loc + "/zoom present")
            for key, sw_key in (("transform", "Transform"), ("direction", "Direction"), ("zoom_direction", "ZoomDirection")):
                same(camera[key], cam.get(sw_key, "Vehicle" if key == "transform" else "Default").lower(), loc + "/" + key)
            value = old.get("Weapons", [])
            names.extend([value] if isinstance(value, str) else value)
        same(len(jeg.get("weapons", [])), len(names), label + "/weapons")
        for weapon, sw_weapon in zip(jeg.get("weapons", []), names, strict=True):
            old = source["Weapons"][sw_weapon]
            old = source["Weapons"].get(old.get("Template"), {}) | old
            for key, sw_key, default in (("default_zoom", "DefaultZoom", 3), ("crosshair", "Crosshair", "@Empty"), ("crosshair_zooming", "CrosshairZooming", old.get("Crosshair", "@Empty"))):
                same(weapon[key], old.get(sw_key, default), label + "/" + sw_weapon + "/" + key)
            if "icon" in weapon: assets.add(weapon["icon"].split(":", 1)[1])
        if name in ("mi28", "ah6"):
            client = read(sw / f"assets/superbwarfare/sbw/vehicles/{sw_name}.json")
            same([jeg["engine"][k] for k in ("mouse_speed_x", "mouse_speed_y")], client["MouseSpeed"], label + "/mouse")
            same(jeg["engine"]["mouse_sensitivity"], .25, label + "/mouse sensitivity")
    assets.update("textures/overlay/vehicle/" + folder + "/" + p.name for folder in ("base", "land", "crosshair") for p in (sw / ("assets/superbwarfare/textures/overlay/vehicle/" + folder)).glob("*.png") if (resources / "assets/jeg/textures/overlay/vehicle" / folder / p.name).exists())
    assets.update("sounds/vehicle/common/" + name + ".ogg" for name in ("terrain", "pull_up"))
    assets.update("textures/overlay/vehicle/helicopter/" + p.name for p in (sw / "assets/superbwarfare/textures/overlay/vehicle/helicopter").glob("*.png"))
    assets.update("textures/overlay/crosshair/" + name + ".png" for name in ("hit_marker", "hit_marker_vehicle", "headshot_marker", "kill_marker_1", "kill_marker_2", "kill_marker_3", "kill_marker_4"))
    for asset in assets:
        same(hashlib.sha256((resources / "assets/jeg" / asset).read_bytes()).digest(),
             hashlib.sha256((sw / "assets/superbwarfare" / asset).read_bytes()).digest(), f"{module.name}/{asset}")
    subprocess.run([sys.executable, str(module / "scripts/verify_vehicle_reticle_sampling.py")], check=True)
    print(f"{module.name}: 6 vehicles, 25 seats, SW controls/cameras/HUD resources match")

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--module", type=Path)
    parser.add_argument("--sw", type=Path)
    args = parser.parse_args()
    workspace = Path(__file__).resolve().parents[2]
    reference = args.sw or workspace / "external/SuperbWarfare-0.8.8-1.21.1"
    head = subprocess.check_output(["git", "-c", "safe.directory=*", "-C", str(reference), "rev-parse", "HEAD"], text=True).strip()
    assert head.startswith("0cfd00d5"), f"SW baseline moved: {head}"
    modules = [args.module.resolve()] if args.module else [workspace / f"Just-Enough-Guns-{loader}-{version}" for loader in ("NeoForge", "Fabric") for version in ("1.21.1", "26.2", "26.3")]
    for module in modules: verify(module, reference)

if __name__ == "__main__":
    main()
