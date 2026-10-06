# Enemy vehicle AI after SW control alignment

The maintained Fabric/NeoForge 1.21.1, 26.2 and 26.3 ports share the same AI decisions. The existing natural-spawn pool remains BMP-2, LAV-150, Mi-28 and AH-6; the shared controller also handles an enemy-tagged Truck or Speedboat. Truck remains unarmed.

| Vehicle | Behavior |
|---|---|
| BMP-2 | Pivot before sharp turns, brake on approach, reverse with corrected steering, slew the turret within the configured elevation and turn rates. |
| LAV-150 | Reduce throttle for sharp turns, approach and reverse using the wheel steering direction. |
| Truck | Use wheel pursuit/patrol control, avoid water, retain deliberate stops without triggering stuck recovery. |
| Speedboat | Choose water patrol points, avoid shorelines and use its own turret limits and machine gun. |
| Mi-28 | Use collective for lift and tilt for translation, stabilize patrol altitude, separate close-range retreat from obstacle climbing, retain independent pilot/gunner weapons. |
| AH-6 | Use the same flight feedback with its own engine coefficients; choose the machine gun when the target angle is too wide for an accurate rocket. |

AI flight translation requests up to 0.45 blocks/tick during patrol. Attack pitch blends back toward braking as horizontal speed rises from 0.60 to 0.85 blocks/tick. These are AI input decisions; SW player movement formulas, keybindings, vehicle JSON, damage, armor, ammunition, energy costs and recipes remain unchanged.

AI uses the actual projectile muzzle and weapon direction to decide whether to fire. Turret requests use the existing SW interpolation. Spawned airborne AI initializes rotor power from the matching engine's hover lift instead of a common legacy rotor value. Crew recovery, seat permissions, target memory and server authority use the existing code paths.

Run from this module, using its Java 21/25 toolchain:

```text
gradlew build
gradlew runServer
python scripts/verify_vehicle_enemy_ai.py
python scripts/verify_vehicle_sw_alignment.py
python scripts/run_vehicle_capture.py --world "run/saves/SW Vehicle Check" --output "path/to/fresh/capture" --mode ai
```

NeoForge 1.21.1 also provides `gradlew runVehicleControlTests` and `gradlew runGameTestServer`. The focused suite checks reverse steering, deliberate stops, water/shore avoidance, turret slew/elevation, actual muzzle alignment, AH-6 weapon fallback, and closed-loop patrol/attack flight in addition to the earlier SW control tests.

The opt-in `ai` capture uses a disposable copy of the supplied world, existing enemy spawn/crew logic, and the real integrated-server controller. It records 401 ticks for each surface vehicle and 601 for each helicopter, introduces an invulnerable survival target after tick 200, and records motion, firing, ammunition, energy, health and rendered frames. Its observation camera is enabled only for this fixture. Normal play does not enter the capture path.

The workspace `vehicle-ai-evidence/` contains runtime logs, recordings, source hashes, acceptance results and six commit IDs. `verify_vehicle_enemy_ai.py --evidence ../vehicle-ai-evidence --output ../vehicle-ai-evidence/acceptance.json` validates the recordings against the current sources and checks all twelve shared AI method bodies across six ports.
