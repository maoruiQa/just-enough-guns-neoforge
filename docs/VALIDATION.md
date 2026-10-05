# Validation record - 2026-10-05

The implementation targets the six maintained Fabric/NeoForge modules for 1.21.1, 26.2 and 26.3. Dedicated-server and window tests used separate directories under the parallel workspace's `tmp/validation-*`. The final native GameTest rerun used the standard NeoForge run directory; existing player world directories were not used.

## Automated verification

- All six: `build checkVehicleGeometry` passed on the final source. Fabric also passed `compileClientJava`.
- All six: `python scripts/generate_advancements.py --check` and `python scripts/verify_vehicle_reticle_sampling.py` passed. Coverage includes valid recipes, item/weapon mappings, translations, acyclic parents and optional goals excluded from mastery.
- NeoForge 1.21.1: all 16 required native GameTests passed. They cover native criterion matching, creative/spectator rejection, save/reload, weapon snapshots, successful/failed installation, explosives and fire ownership, actual vehicle damage, feedback during damage cooldown, narrow-phase geometry, driving input, existing inventory recognition, reopening known recipes and own-vehicle filtering.
- Dedicated servers for all six reached `Done`, loaded the advancements and recipes, accepted `/reload`, and stopped normally. Fabric/NeoForge 1.21.1 loaded 113 JEG recipes; the four 26.x modules loaded 163. Fabric 26.2 was rerun after registering its missing Javelin ammunition, confirming that the earlier recipe parse error was resolved.

## Live window and save verification

Fabric 26.3 connected a survival player to a separate local dedicated server. The native interface displayed the guide tabs. Five vehicles were inspected in first-person, rear F5 and front F5 views at normal FOV and GUI scale 2. Rear-view reticles were visible after correcting the full-texture UV sampling; front generic reticles were hidden, and targets behind the camera were not drawn.

A real timed C4 explosion destroyed an enemy BMP-2 and awarded the C4 hit, timed detonation and enemy BMP-2 destruction criteria. Reopening a known JEG crafting recipe book awarded the tutorial criterion. `/reload`, save, server restart and login preserved those timestamps. A revoked possession criterion was restored from the unchanged inventory on login; shooting/driving/mastery were not inferred. The player's total XP stayed at 65 after saving/relogging completed criteria.

Local evidence is saved under `tmp/vehicle-validation/`: fifteen vehicle camera JPGs, native recipe/training/special-operations screenshots, C4 command setup and native advancement JSON snapshots. Dedicated-server logs remain under the corresponding isolated validation directories. The final GameTest rerun after the 1.21.1 blast-source fix is recorded in `Just-Enough-Guns-NeoForge-1.21.1/run/logs/latest.log` at 18:45:33 (16/16 passed), with a preserved copy in `tmp/vehicle-validation/final-gametest.log`.

## Remaining manual acceptance coverage

The full acceptance matrix has not been exercised: every seat and weapon under zoom and multiple FOV/GUI scales; an actual SW client window comparison; audible collision capture and sustained driving/landing cases; and simultaneous driver/passenger/observer or multiplayer raid qualification. The automated checks and single-client observations above do not establish those outcomes. Keep these as manual acceptance checks before declaring the entire gameplay matrix verified.
