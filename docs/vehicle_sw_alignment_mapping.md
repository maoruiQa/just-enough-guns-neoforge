# Six-vehicle SW alignment

Baseline: local Superb Warfare 0.8.8 / Minecraft 1.21.1, commit
`0cfd00d560e3a458b1319826137b669a6351084a`. No SW runtime dependency.
This document covers NeoForge-1.21.1 only; the same behavior is ported to all six
maintained Fabric / NeoForge 1.21.1, 26.2 and 26.3 modules.

| JEG entity/data id | SW id | Seats | Visible inventory | Control / view |
| --- | --- | ---: | ---: | --- |
| `jeg:bmp2` | `bmp_2` | 7 | 54 | Tracks, barrel/turret cameras, cannon elevation 74 degrees |
| `jeg:lav150` | `lav_150` | 5 | 54 | Wheels, turret, simulated passenger third person |
| `jeg:truck` | `truck` | 2 | 102 | Wheels, cabin views, ordinary handheld items and guns |
| `jeg:speedboat` | `speedboat` | 5 | 54 | Buoyancy, water/land transitions, machine-gun sight |
| `jeg:mi28` | `mi_28` | 2 | 54 | Helicopter, separate pilot/gunner HUD and normal/aim cameras |
| `jeg:ah6` | `ah_6` | 4 | 54 | Helicopter, free look and oriented passenger views |

Inventory sizes deliberately follow SW JSON, as requested, rather than SW's
hardcoded 102-slot runtime screen. Existing physical inventory storage and saved
slot indices remain intact, including old contents beyond the visible slots.
The standard menu quick-move logic operates on the exposed slots. No entity,
item or save-key renaming is involved.

## Behavior and data

Only these six records opt into `engine.sw_controls`. Other vehicles retain their
existing motion and camera paths. Existing server authority, seat ownership,
client prediction, JEG key bindings and custom remaps remain in use. In 26.x the
native player-authority shortcut is disabled for these vehicles so the dedicated
server continues to tick movement. Damage, armor, ammunition cost, energy cost
and recipes retain JEG values. Truck uses handheld guns rather than a synthetic
mounted pistol.

SW acceleration/reverse/braking, steering inertia, terrain attitude and buoyancy
use the existing vehicle base class. Helicopters use SW startup/lift, pitch/yaw/
roll, hover, braking and tagged-pad landing. Mouse sensitivity precedes each
helicopter's own smoothing. Menus, focus loss, seat changes, dismounts and
disconnects clear input, free look, zoom, smoothing and hit feedback.

Seat data now includes orientation, transform and head/body rotation constraints.
Camera data separates normal/aim positions and directions. Engine data adds
buoyancy, steering, terrain and helicopter coefficients; turret data adds turn
speed; weapon data adds zoom, reticles, HUD/view origins, directions and transform.
Old JSON and constructor callers receive compatible defaults. The existing JSON
reload/sync path transports these fields. Existing vehicle input/state packets
also carry seat aiming and predicted power, steering, rudder, roll and engine
state; existing synchronized entity data carries real component health.

Body, turret and barrel transforms drive camera, passenger, model and HUD
projection from the same interpolated pose. Camera rotation owns bank correction
once. Its forward vector and native 26.x cached view matrix stay consistent with
the camera quaternion. Native wall clipping and SW helicopter-camera clipping
remain active. Zoom comes from the current station/weapon.

The SW HUD uses JEG's actual health, energy, ammunition, reload/lock/decoy state
and bindings. Layout includes status bars, all seats, the animated weapon
selector, land sights/part health/range, helicopter instruments, reticles,
warnings and hit/kill feedback. Empty reticles remain empty. Truck retains the
native handheld crosshair and mouse path. Survival health/food/armor/XP HUD stays
available. Weapon switching uses the SW 300 ms animation. SW HUD/feedback PNGs
and terrain/pull-up warning OGGs are reused and checked by SHA-256.

## Runnable verification

Use Java 21 for 1.21.1 and Java 25 for 26.2 / 26.3. Run Gradle from this module.

```powershell
.\gradlew.bat build
.\gradlew.bat runServer
.\gradlew.bat runClient
python scripts/verify_vehicle_sw_alignment.py --module . --sw ../external/SuperbWarfare-0.8.8-1.21.1
```

NeoForge 1.21.1 also provides:

```powershell
.\gradlew.bat runVehicleControlTests
.\gradlew.bat runGameTestServer
```

The nine focused GameTests check motion curves, reverse/braking/steering,
buoyancy, startup/lift/landing, BMP turret limits, seats/permissions, save/quick
move, AI drivers, non-target vehicles, camera/wall clipping and real damage/hit
feedback with a network-codec round trip. The normal namespace has 20 gun/gameplay
regressions.

The opt-in capture hooks do nothing in ordinary gameplay. They copy an existing
disposable flat world and record actual window frames, camera quaternions and
server-tick motion; they never mutate the supplied world.

```powershell
python scripts/run_vehicle_capture.py --world "run/saves/Your Flat Test World" --output "E:/verification/base"
python scripts/run_vehicle_capture.py --world "run/saves/Your Flat Test World" --output "E:/verification/tilt" --tilt --width 1024 --height 768 --gui-scale 3
python scripts/run_vehicle_capture.py --world "run/saves/Your Flat Test World" --output "E:/verification/survival" --hud-only --survival
python scripts/run_vehicle_capture.py --world "run/saves/Your Flat Test World" --output "E:/verification/handheld" --handheld
python scripts/run_vehicle_capture.py --world "run/saves/Your Flat Test World" --output "E:/verification/motion" --mode drive
python scripts/compare_vehicle_sw_capture.py SW/camera.csv JEG/camera.csv --output camera-result.json
python scripts/compare_vehicle_sw_capture.py SW/motion.csv JEG/motion.csv --motion --output motion-result.json
python scripts/verify_vehicle_sw_hud_pixels.py SW JEG --output hud-result.json
python scripts/run_vehicle_network_check.py --world "run/saves/Your Flat Test World" --output "E:/verification/network" --port 25582 --delay-ms 100
```

Configuration, reticle sampling, capture, curve and network scripts use Python's
standard library. Only the optional screenshot pixel comparison needs Pillow.
Use a fresh output directory for each capture. The network check launches a real
dedicated server and three real clients (driver, gunner, observer), delays every
TCP forwarding chunk by 100 ms in each direction, and checks seats/permissions,
input clearing, menu access, zero energy, component HUD and driver relogin.
This delay is not a claim of a precisely fixed end-to-end RTT.

The full static matrix contains 198 camera states (every seat, every station
weapon, three perspectives, normal/aim) plus six inventory screenshots, at
854x480 auto GUI and 1024x768 GUI scale 3 with yaw 25 / pitch 12 / roll 18.
Camera tolerance is 1 cm / 0.1 degree; visible SW HUD anchors are compared in both
directions within one GUI pixel. Different native fonts, localized names,
bindings and actual JEG state values are not pixel-equality targets. Unarmed
seats use the native crosshair; their scene pixels are not vehicle-reticle anchors.
The real part-health outlines remain visible to show JEG component health even
where the pinned SW renderer makes that layer transparent.

Motion uses 181 fixed server ticks per vehicle with forward, reverse, turning,
braking and helicopter ascent/descent/mouse input. Velocity error is normalized
to the SW curve's peak speed (limit 1%); attitude tolerance is 0.1 degree. Saved
frames can be replayed as GIFs using their server-tick timestamps.

Validation artifacts and the six commit ids are indexed in the workspace's
`vehicle-sw-evidence/README.md`. Checks were run in the existing working trees;
unrelated config-menu, monitor, release-note and runtime changes are preserved.
26.1 legacy, assembling-table and charging-station screens are outside this change.
