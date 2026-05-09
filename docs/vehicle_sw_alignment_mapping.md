# Vehicle Superb Warfare Alignment Mapping

This file is the Phase 0 mapping for aligning the NeoForge 1.21.1 vehicle system with `external/SuperbWarfare-0.8.8-1.21.1`.

## Canonical Vehicle Ids

| JEG vehicle data id | JEG entity id | SW vehicle id | Type | Container | Current JEG model | Current JEG texture | Current JEG animation | JEG assembly recipe | HUD / crosshair target |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `a10` | `jeg:a10` | `a_10a` | aircraft | small | `geo/entity/vehicle/a10.geo.json` | `textures/entity/vehicle/a10.png` | generic fallback | `data/jeg/vehicle_assembly/a10.json` | aircraft / gun + missile |
| `ah6` | `jeg:ah6` | `ah_6` | helicopter | small | `geo/entity/vehicle/ah6.geo.json`, `lod/ah6_lod1-3.geo.json` | `textures/entity/vehicle/ah6.png` | generic fallback | `data/jeg/vehicle_assembly/ah6.json` | helicopter / gun + missile |
| `bmp2` | `jeg:bmp2` | `bmp_2` | land | medium | `geo/entity/vehicle/bmp2.geo.json`, `lod/bmp2_lod1-2.geo.json` | `textures/entity/vehicle/bmp2.png` | generic fallback | `data/jeg/vehicle_assembly/bmp2.json` | land APC / gun + missile |
| `hpj11` | `jeg:hpj11` | `hpj_11` | artillery | mini | `geo/entity/vehicle/hpj11.geo.json` | `textures/entity/vehicle/hpj11.png`, `hpj11_glow.png` | generic fallback | `data/jeg/vehicle_assembly/hpj11.json` | fixed weapon / HPJ zoom |
| `laser_tower` | `jeg:laser_tower` | `laser_tower` | artillery | mini | `geo/entity/vehicle/laser_tower.geo.json`, `lod/laser_tower_lod1-2.geo.json` | `textures/entity/vehicle/laser_tower.png`, `laser_tower_glow.png` | `animations/entity/vehicle/laser_tower.animation.json` | `data/jeg/vehicle_assembly/laser_tower.json` | fixed weapon / laser cannon |
| `lav150` | `jeg:lav150` | `lav_150` | land | medium | `geo/entity/vehicle/lav150.geo.json`, `lod/lav150_lod1-2.geo.json` | `textures/entity/vehicle/lav150.png`, `lod/lav150_lod1-2.png` | `animations/entity/vehicle/lav150.animation.json` | `data/jeg/vehicle_assembly/lav150.json` | land APC / gun + missile |
| `mi28` | `jeg:mi28` | `mi_28` | helicopter | small | `geo/entity/vehicle/mi28.geo.json`, `lod/mi28_lod1.geo.json` | `textures/entity/vehicle/mi28.png` | generic fallback | `data/jeg/vehicle_assembly/mi28.json` | helicopter / gun + missile |
| `speedboat` | `jeg:speedboat` | `speedboat` | boat | mini | `geo/entity/vehicle/speedboat.geo.json`, `lod/speedboat_lod1.geo.json` | `textures/entity/vehicle/speedboat.png`, `speedboat_glow.png` | `animations/entity/vehicle/speedboat.animation.json` | `data/jeg/vehicle_assembly/speedboat.json` | boat / gun |
| `tom6` | `jeg:tom6` | `tom_6` | aircraft | small | `geo/entity/vehicle/tom6.geo.json` | `textures/entity/vehicle/tom6.png` | generic fallback | `data/jeg/vehicle_assembly/tom6.json` | aircraft / gun + missile |
| `truck` | `jeg:truck` | `truck` | land | small | `geo/entity/vehicle/truck.geo.json` | `textures/entity/vehicle/truck.png` | generic fallback | `data/jeg/vehicle_assembly/truck.json` | land utility / pistol |
| `waveforce_tower` | `jeg:waveforce_tower` | `waveforce_tower` | artillery | mini | `geo/entity/vehicle/waveforce_tower.geo.json` | `textures/entity/vehicle/waveforce_tower.png`, `waveforce_tower_glow.png` | `animations/entity/vehicle/waveforce_tower.animation.json` | `data/jeg/vehicle_assembly/waveforce_tower.json` | fixed weapon / cannon |

## JEG-Only Compatibility Vehicles

These records are JEG test or compatibility vehicles. They do not have direct SW ids and should not be treated as proof that the SW alignment is complete.

| JEG vehicle data id | JEG entity id | Type | Container | Current role |
| --- | --- | --- | --- | --- |
| `test_wheel_vehicle` | `jeg:test_wheel_vehicle` | land | mini | minimal wheel test target |
| `light_combat_vehicle` | `jeg:light_combat_vehicle` | land | small | pre-SW generic combat vehicle |
| `test_helicopter` | `jeg:test_helicopter` | helicopter | mini | minimal rotor test target |
| `test_boat` | `jeg:test_boat` | boat | mini | minimal boat test target |
| `test_artillery` | `jeg:test_artillery` | artillery | small | minimal static weapon test target |
| `test_aircraft` | `jeg:test_aircraft` | aircraft | mini | minimal aircraft test target |

## SW Source Resource Names

| SW id | Geo | Texture | Extra textures | Animation | LOD geo |
| --- | --- | --- | --- | --- | --- |
| `a_10a` | `geo/a_10a.geo.json` | `textures/entity/a_10a.png` | none found | none found | none found |
| `ah_6` | `geo/ah_6.geo.json` | `textures/entity/ah_6.png` | none found | none found | `geo/vehicle_lod/ah_6_lod1-3.geo.json` |
| `bmp_2` | `geo/bmp_2.geo.json` | `textures/entity/bmp_2.png` | none found | none found | `geo/vehicle_lod/bmp_2_lod1-2.geo.json` |
| `hpj_11` | `geo/hpj_11.geo.json` | `textures/entity/hpj_11.png` | `hpj_11_e.png`, `hpj_11_heat.png` | none found | none found |
| `laser_tower` | `geo/laser_tower.geo.json` | `textures/entity/laser_tower.png` | `laser_tower_e.png`, `laser_tower_laser.png` | `animations/laser_tower.animation.json` | `geo/vehicle_lod/laser_tower_lod1-2.geo.json` |
| `lav_150` | `geo/lav_150.geo.json` | `textures/entity/lav_150.png` | `lav_150_lod1.png`, `lav_150_lod2.png` | `animations/lav_150.animation.json` | `geo/vehicle_lod/lav_150_lod1-2.geo.json` |
| `mi_28` | `geo/mi_28.geo.json` | `textures/entity/mi_28.png` | none found | none found | `geo/vehicle_lod/mi_28_lod1.geo.json` |
| `speedboat` | `geo/speedboat.geo.json` | `textures/entity/speedboat.png` | `speedboat_e.png`, `speedboat_heat.png`, `speedboat_power.png` | `animations/speedboat.animation.json` | `geo/vehicle_lod/speedboat_lod1.geo.json` |
| `tom_6` | `geo/tom_6.geo.json` | `textures/entity/tom_6.png` | none found | none found | none found |
| `truck` | `geo/truck.geo.json` | `textures/entity/truck_green.png` | `truck_red.png` | none found | none found |
| `waveforce_tower` | `geo/waveforce_tower.geo.json` | `textures/entity/waveforce_tower.png` | `waveforce_tower_glow_e.png` | `animations/waveforce_tower.animation.json` | none found |

## Naming Adaptation Rules

- Strip underscores when copying SW vehicle ids into JEG ids: `lav_150 -> lav150`, `bmp_2 -> bmp2`, `mi_28 -> mi28`, `tom_6 -> tom6`, `hpj_11 -> hpj11`.
- Map SW `a_10a` to JEG `a10`; keep this exception explicit because it is not a pure underscore removal.
- Keep SW `truck`, `speedboat`, `laser_tower`, and `waveforce_tower` names unchanged.
- Put copied entity resources under JEG's current loader paths: `geo/entity/vehicle/`, `textures/entity/vehicle/`, `animations/entity/vehicle/`.
- Put copied LOD geo under `geo/entity/vehicle/lod/`; copied LOD textures currently exist only for `lav150`.

## Remaining Mapping Gaps

- Vehicle icons from `textures/vehicle_icon/*_icon.png` are copied for mapped SW vehicles and shown in the JEG assembling preview; they are not yet used for category/filter UI.
- SW JSON behavior data under `assets/superbwarfare/sbw/vehicles/*.json` has not been fully translated into JEG `data/jeg/vehicles/*.json`.
- Representative SW weapon-fire sounds are copied and registered for `a10`, `ah6`, `bmp2`, `hpj11`, `laser_tower`, `lav150`, `mi28`, and `waveforce_tower`; engine loops, horns, distance variants, and remaining vehicle-specific sounds are not yet mapped.
- SW GUI inventory textures under `textures/gui/vehicle/inventory/*.png` are available as references, but JEG slot layout must be changed before using them directly.
- SW container/rendering assets for vehicle deployers, charging station, and vehicle assembling table are only partly copied into JEG block model usage.
