# Superb Warfare vehicle damage alignment

Reference: the workspace's `external/SuperbWarfare-0.8.8-1.21.1`, including `entity/vehicle/base/VehicleEntity`, `entity/vehicle/damage/DamageModifier`, `tools/CustomExplosion`, and `data/superbwarfare/sbw/vehicles`.

## Covered vehicles

All eleven existing SW-derived vehicles: A10, AH6, BMP2, HPJ11, laser tower, LAV150, MI28, speedboat, TOM6, truck, and waveforce tower. Their entire OBB arrays (centers, half extents, parts, position/rotation transforms), hull health, immunity/modifier tables, repair settings, mass, directional armor, warning flags and destruction settings match this reference. Vehicles without an SW OBB use their entity AABB.

## Behavior

- F3+B draws every oriented part box in green (interactive boxes in yellow), using the same interpolated vehicle/turret transforms as hit detection. Vanilla entity AABB debug lines remain visible.
- Non-bullet-trail projectile impacts preserve the part selected by vanilla projectile picking instead of forcing BODY; area damage still affects only the hull.
- Bullets, cannon/rocket direct hits, missiles, and vanilla projectile picking carry the selected OBB part to server damage. Blast damage affects the hull.
- Every component starts at 50 health and regenerates 0.125 per tick independently of hull repair cooldown. Negative health breaks the component; a broken component recovers only above 47.5 health. Fault flags survive save/reload. Old 10-point component saves migrate proportionally.
- Damage applies immunities, fixed reductions, multipliers, then directional armor. Registered damage types and entity/damage tags work with data-pack rules. JEG bullet, shell, rocket, missile and destruction sources map to the corresponding SW families without stacking mutually exclusive explosion rules.
- Damaged wheels/tracks reduce power and cause one-sided drift; engine damage reduces propulsion; helicopter tail rotor damage induces yaw. A damaged turret cannot aim or fire. Existing vehicle controls and flight implementations remain the host for these effects.
- Hull repair defaults to 0.05 per tick after a 200-tick damage cooldown. At/below the configured critical health threshold, hull decays by 0.1 per tick; SW tower/TOM6 exceptions are retained. Negative hull repair cooldown disables hull auto-repair.
- Wall, landing, vehicle and living-entity collisions use the SW speed/mass damage formulas, including SW vehicles whose legacy collision level is `none`. The port retains its current movement/collision solver; fixed-wing unsafe landing detects tilt because this port has no retractable landing gear state.
- Destruction handles passenger death/crash attribution, separate blast damage/radius, the full vanilla exposure ray fan, 15% minimum exposure, SW weak block ray power, native block drops/hooks, and the configured particle tier. Native entity damage and knockback are disabled for the destruction blast to avoid duplicate application.
- Broken parts produce smoke/flame. Low-health warnings use the SW warning audio and translated passenger messages.

## Verification

Run from this module, using Java 21 for 1.21.1 or Java 25 for 26.2/26.3:

```powershell
.\gradlew.bat build
```

The standard `check` now includes `checkVehicleGeometry`, `checkVehicleDamage`, and `checkVehicleDamageModifiers`. Client presentation/audio still requires an in-game client check.

## Truck fall correction

The civilian truck uses structural landing damage instead of SW weapon resistance: drops up to three blocks are safe, each additional block removes 1/17 of maximum hull health, and a twenty-block fall destroys the truck. This is an intentional gameplay adjustment requested after natural-fall testing found that the old `none` collision gate made the truck entirely immune. Normal movement on supported ground does not apply this damage. A landing applies this structural damage once, without also applying the SW vertical impact hit. Other vehicle impact formulas are unchanged.

### Handheld anti-armor rockets and missile contact

RPG direct hits on vehicles use the SW standard warhead (340 direct damage, 80 vehicle splash, radius 5). Living-target values stay in the existing RPG path. Splash damages the hull; the direct ray damages the struck component. A BODY hit has no separate side-component fault.

Missiles fired on foot can collide with unmounted targets. Exclusion of passengers applies only when the shooter actually occupies a vehicle. NeoForge 1.21.1 launcher GameTests exercise RPG side components and all eleven SW vehicles, Javelin direct/top/unguided flight, and Igla airborne impact.
