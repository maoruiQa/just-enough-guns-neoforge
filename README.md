
# Just Enough Guns New - NeoForge 26.2

A modern NeoForge 26.2 unofficial port of the Forge 1.20.1 mod Just Enough Guns, bringing vanilla-styled firearms, hostile gunners, faction raids, vehicles, and late-game aerial threats to newer Minecraft versions.

## Overview

This module is the maintained Java 25 NeoForge branch for Just Enough Guns New **1.8.0**. It carries magazine-fed weapons, Walkürenritt vehicles, and the 1.8.0 special-equipment pack (FPV drones, C4 / claymore / C4 vest, Javelin & Igla, smoke denial, vehicle lock UI), plus kill-credit fixes and the vehicle / missile / rocket combat balance pass.

## Latest Release Notes

Version `1.8.0` is the current release line. Interim local labels **1.8.1** / **1.8.2** were never separate public releases; their work is included here.

### Highlights

- **Special equipment** — FPV drones with monitor control, C4 / claymore / C4 vest, C4 defuser, Javelin & Igla guided launchers, smoke denial, and vehicle missile lock UI.
- **Kill credit** — gun / rocket / explosive kills correctly credit the player for advancements, loot, and boss kills ([#10](https://github.com/maoruiQa/just-enough-guns-neoforge/issues/10)).
- **Vehicle / missile / rocket balance** — SW-aligned missile damage model, ballistic AP for missiles, retuned vehicle armor & HE modifiers, rocket numbers and tooltip fixes.

### Added

- Ported SuperbWarfare-style special equipment: FPV drones, C4 (including remote/detonator), claymore mines, C4 vest, and C4 defuser.
- Added guided launchers **Javelin** and **Igla 9K38** with lock-on fire, SW-aligned first-person poses/ADS, icons, root motion, and reload animations.
- Added C4 drone FPV payload HUD with detonate guidance and a **KAMIKAZE** dive presentation.
- Added a C4 vest bomber gunner variant with configurable spawn rates.
- Added SW-style smoke screens that deny missile locks, denser smoke particles, and release audio.
- Added vehicle missile lock frames and seek audio for lockable targets.
- Soft-disabled natural Terror Phantom spawns by default.

### Changed / Fixed

- Rebalanced guided missiles, anti-vehicle rockets, drone descent power, and vehicle state sync.
- Vehicle armor and HE modifiers use SW-style damage modifiers with strict type matching; guided missiles use direct hit + explosion falloff and ballistic AP.
- Rocket launcher: direct **150**, blast **50**, radius **11**, AP **10**; tooltips show real combat numbers for rockets and guided launchers.
- Fixed drone FPV rubber-banding, seek frames due north, helicopter rotor spin-down after dismount, guided launcher presentation, and special-equipment audio/HUD.
- Fixed kill credit for guns, missiles, grenades, and molotovs (including 26.x damage-source attribution paths).
- Fixed stacked vehicle explosion modifiers and rocket/Javelin/Igla tooltip damage readouts.
- **(NeoForge 26.2)** Fixed C4/claymore entity scale and claymore facing; fixed missile/decoy deferred render pose; fixed GeckoLib v5 first-person guided launcher hip pose, ScreenProjection capture for HUD frames, and drone rotor bone spin.

## Supported Version

| Loader | Minecraft | Java | Mod Version | Required Dependencies |
| --- | --- | --- | --- | --- |
| NeoForge | 26.2 | Java 25 | 1.8.0 | NeoForge 26.2.x, GeckoLib 5.5.1 |

## Controls

| Action | Default Input |
| --- | --- |
| Shoot | Left Click |
| Aim down sights | Right Click |
| Reload | R |
| Inspect animated gun | Y |
| Gun melee / flashlight toggle, where supported | V |
| Dismount vehicle | Shift |

Ballistic Armor Interception
==========

Bulletproof helmets and vests use a ballistic interception model for gun damage instead of vanilla Projectile Protection. Each shot resolves an effective armor-piercing value from the ammo type and the gun multiplier:

```
effectiveAP = ammoArmorPiercing * gunArmorPiercingMultiplier
```

On a protected hit, headshots use the bulletproof helmet first. Other protected body hits use the bulletproof vest. If no matching bulletproof armor piece is equipped, gun damage is unchanged.

For the selected armor piece, the damage multiplier depends on whether the shot undermatches or overmatches the armor rating:

```
apRatio = effectiveAP / armorRating

if effectiveAP < armorRating:
    damageMultiplier = undermatchMultiplier * (0.55 + apRatio * 0.45)
else:
    damageMultiplier = overmatchMultiplier * min(1.25, 0.85 + (apRatio - 1.0) * 0.18)
    damageMultiplier = min(damageMultiplier, 0.95)

finalDamage = rawDamage * damageMultiplier
```

Armor durability loss scales with raw damage and AP pressure. Under-matching shots still wear armor down, while over-matching shots punish armor more heavily:

```
pressure = effectiveAP / armorRating

if effectiveAP < armorRating:
    durabilityDamage = rawDamage * (0.65 + pressure * 0.35)
else:
    durabilityDamage = rawDamage * (1.00 + min(1.50, pressure - 1.0) * 0.85)
```

The result is then scaled by the armor slot and armor tier durability multipliers and capped before being applied to the armor item.

Credits And License
==========

This module is a NeoForge 26.2.x unofficial port of the original Just Enough Guns project by MigaMi: https://www.curseforge.com/minecraft/mc-mods/just-enough-guns

- Original Just Enough Guns **code** is by MigaMi and is licensed under GPL-3.0.
- Original Just Enough Guns **assets** (models, textures, sounds, animations, icons, and other art) are by MigaMi and are **All Rights Reserved (ARR)**. This project redistributes and uses those assets with **explicit authorization from the original author (MigaMi)**.
- Recent updates include substantial materials derived from **Superb Warfare (SBW / SW)** by the SBW development team. **SBW-derived assets require attribution** and are licensed under CC BY-NC-SA 3.0: https://www.curseforge.com/minecraft/mc-mods/superb-warfare
  - **Walkürenritt vehicle set:** LAV-150, BMP-2, speedboat, truck, AH-6, MI-28, A-10, TOM-6, HPJ-11, laser tower, and waveforce tower, plus vehicle workbenches, repair tools, models, textures, sounds, icons, recipes, and related data.
  - **Special equipment and related assets:** FGM-148 Javelin and missile, 9K38 Igla, FPV drone and monitor, C4 and detonator, Claymore, TM-62, smoke-screen / missile-lock UI and audio materials, and related models, textures, sounds, HUD art, and animations.
- SBW-derived materials require attribution to the SBW development team, are for non-commercial use, and must be shared under the same CC BY-NC-SA 3.0 terms when redistributed or adapted. Future content may have different source projects and license terms.
- Project **code** based on Just Enough Guns is licensed under GPL-3.0. Original JEG assets remain ARR under authorized use as noted above. SBW-derived assets remain under the SBW terms noted above.
- This unofficial port is not affiliated with, endorsed by, or an official addon for Just Enough Guns or Superb Warfare.
