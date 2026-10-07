
# Just Enough Guns New - NeoForge 26.3

A modern NeoForge 26.3 unofficial port of the Forge 1.20.1 mod Just Enough Guns, bringing vanilla-styled firearms, hostile gunners, faction raids, vehicles, and late-game aerial threats to newer Minecraft versions.

## Overview

This module is the Java 25 NeoForge 26.3 port of Just Enough Guns New **1.8.2**, carried forward from the NeoForge 26.2 line. It carries magazine-fed weapons, Walkürenritt vehicles, and the 1.8.0 special-equipment pack (FPV drones, C4 / claymore / C4 vest, Javelin & Igla, smoke denial, vehicle lock UI), plus kill-credit fixes and the vehicle / missile / rocket combat balance pass.

## Latest Release Notes

Version `1.8.2` adds a four-chapter combat-career guide with legacy progress migration, SW-aligned vehicle controls and part damage, ADS/spread adjustments, and server magazine-mode notices with matching recipes. It also fixes delayed trigger release, anti-armor impacts, truck fall damage, and config-menu feedback.

- Smoothed remote land vehicles, boats, and helicopters on NeoForge, preventing competing tracker updates from snapping them backward while preserving local driver prediction.

See [CHANGELOG.md](CHANGELOG.md) for the complete changes.

## Supported Version

| Loader | Minecraft | Java | Mod Version | Required Dependencies |
| --- | --- | --- | --- | --- |
| NeoForge | 26.3 | Java 25 | 1.8.2 | NeoForge 26.3.x, GeckoLib 5.5.7 |

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

This module is a NeoForge 26.3.x unofficial port of the original Just Enough Guns project by MigaMi: https://www.curseforge.com/minecraft/mc-mods/just-enough-guns

- Original Just Enough Guns **code** is by MigaMi and is licensed under GPL-3.0.
- Original Just Enough Guns **assets** (models, textures, sounds, animations, icons, and other art) are by MigaMi and are **All Rights Reserved (ARR)**. This project redistributes and uses those assets with **explicit authorization from the original author (MigaMi)**.
- Recent updates include substantial materials derived from **Superb Warfare (SBW / SW)** by the SBW development team. **SBW-derived assets require attribution** and are licensed under CC BY-NC-SA 3.0: https://www.curseforge.com/minecraft/mc-mods/superb-warfare
  - **Walkürenritt vehicle set:** LAV-150, BMP-2, speedboat, truck, AH-6, MI-28, A-10, TOM-6, HPJ-11, laser tower, and waveforce tower, plus vehicle workbenches, repair tools, models, textures, sounds, icons, recipes, and related data.
  - **Special equipment and related assets:** FGM-148 Javelin and missile, 9K38 Igla, FPV drone and monitor, C4 and detonator, Claymore, TM-62, smoke-screen / missile-lock UI and audio materials, and related models, textures, sounds, HUD art, and animations.
- SBW-derived materials require attribution to the SBW development team, are for non-commercial use, and must be shared under the same CC BY-NC-SA 3.0 terms when redistributed or adapted. Future content may have different source projects and license terms.
- Project **code** based on Just Enough Guns is licensed under GPL-3.0. Original JEG assets remain ARR under authorized use as noted above. SBW-derived assets remain under the SBW terms noted above.
- Contributor: **realorangewool**, who provided the Fabric 26.1.2 backport.
- This unofficial port is not affiliated with, endorsed by, or an official addon for Just Enough Guns or Superb Warfare.
