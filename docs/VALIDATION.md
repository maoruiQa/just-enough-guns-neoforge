# Four-chapter redesign verification - 2026-10-05

This redesign and the requested Sky Ship/Raid/special-enemy additions target all six maintained modules. Runtime directories are isolated under `tmp/advancement-redesign/` in the parallel workspace. Existing uncommitted source files were hash-checked and preserved.

## Current automated and server results

- Six module builds passed after adding the enemy-role hooks. Fabric 1.21.1, 26.2 and 26.3 also passed `compileClientJava`.
- Six generator checks passed: four visible tabs, 72 nodes / 53 core goals for 1.21.1 and 73 / 54 for 26.x, no visible node with more than three children, no parent cycles, complete English/Chinese, valid recipe references and migration sources. Independent challenges and frozen old records are excluded from mastery.
- NeoForge 1.21.1: all 20 required native GameTests passed. The added tests cover OR alternatives and AND combinations, partial-history import, silent chapter/career closure, normal rewards after partial import, native save/reload without repeated rewards, real elite/bomber/Phantom Gunner deaths, vest-only and already-detonated rejection, special pillager classification and observer isolation. Existing source attribution, explosive/fire, vehicle geometry and collision tests also passed.
- Final resources for all six dedicated servers reached `Done`, loaded 113 JEG recipes in 1.21.1 and 163 in 26.x, and accepted `/reload` without advancement/recipe parse errors. Their logs are `server-<loader>-<version>-final.log`. GameTest build/results are in `gametest-enemies-final.log` (20/20 at 00:18:33).

## Actual old-world migration

A copy of the previous Fabric 26.3 test world was used with its real native saved criteria and survival player. On login, an old C4 hit imported only the `hit` half of One Touch; the remote half remained missing. Old enemy BMP2 destruction imported the matching anti-armour OR criterion. Recipe-view and timed-C4 timestamps were retained. Crafting, loading and career completion were not fabricated from inventory. XP stayed at 65 through login, `/reload`, save, client exit and re-login. Imported timestamps and partial progress stayed unchanged.

Evidence files are `before-old-native-progress.json`, `after-old-native-progress.json`, `after-reload-native-progress.json` and `after-relogin-native-progress.json` under `tmp/advancement-redesign/`. The older player world was copied, not modified in place.

## Window acceptance limitation

Both isolated Fabric 26.3 and 1.21.1 clients connected, but the Windows capture/input session produced black or white game surfaces. A Java thread sample showed the 1.21.1 render loop running in its FPS limiter. Captures are saved as `ui-capture-failed-26.3.jpg` and `ui-capture-failed-1.21.1.jpg`; these are failure evidence, not accepted advancement-layout screenshots. The final four-chapter layout, lines and tooltip appearance still require a usable native-window check. The full natural-spawn progression and multiplayer raid acceptance matrix remain manual checks; native GameTests do not establish those complete playthrough outcomes.

The prior record below describes the earlier vehicle implementation and eight-tab guide. Its old guide counts/possession checks do not describe this redesign; retained vehicle observations remain useful historical evidence.

---

# Prior vehicle and eight-tab verification - 2026-10-05

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
