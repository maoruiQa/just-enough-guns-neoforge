
# Just Enough Guns New - NeoForge 26.1 Legacy

A legacy NeoForge 26.1 port of the Forge 1.20.1 mod Just Enough Guns, bringing vanilla-styled firearms, hostile gunners, faction raids, vehicles, and late-game aerial threats to newer Minecraft versions.

## Overview

This module is the NeoForge 26.1 legacy maintenance branch for Just Enough Guns New 1.7.1. The maintained Java 25 release line has moved to NeoForge 26.2, but this branch carries the 1.7.1 repair, localization, vehicle, helicopter, and gun animation fixes for players who still need 26.1.

## Latest Release Notes

Version `1.7.1` is the current legacy maintenance version for NeoForge 26.1.

- Restored the `jeg:repair_kit` recipe and made the repair kit the anvil repair material for guns and bulletproof armor.
- Fixed repair kit behavior so it no longer repairs vehicles on right-click; repair tools still repair vehicles.
- Added first-person held-gun left/right movement camera sway and Y-key gun inspect animations.
- Fixed Flamethrower reload timing and first-person inspect animation interruption behavior.
- Reduced rocket and missile block damage and splash reach on direct vehicle hits while preserving direct vehicle damage.
- Fixed MI-28 gunner pitch limits and service-rifle rear-grip rendering.
- Updated helicopter unsafe-descent, crash-damage, rotor, warning HUD/audio, low-energy, and missile-profile behavior.
- Expanded Chinese localization and converted targeted player-visible hardcoded strings to translation keys.

## Supported Version

| Loader | Minecraft | Java | Mod Version | Required Dependencies |
| --- | --- | --- | --- | --- |
| NeoForge | 26.1 legacy | Java 25 | 1.7.1 | NeoForge 26.1.x, GeckoLib 5.5 |

Use NeoForge 26.2 for the maintained Java 25 release line unless you specifically need this 26.1 legacy build.

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

This module is a NeoForge 26.1.x port of the original Just Enough Guns project by MigaMi: https://www.curseforge.com/minecraft/mc-mods/just-enough-guns

- Original Just Enough Guns code, design, and assets are by MigaMi and are licensed under GPL-3.0.
- The current SBW-derived Walkurenritt vehicle set covers the LAV-150, BMP-2, speedboat, truck, AH-6, MI-28, A-10, TOM-6, HPJ-11, laser tower, and waveforce tower, plus their vehicle workbenches, repair tools, models, textures, sounds, icons, recipes, and related data. These materials are derived from Superb Warfare (SBW) by the SBW development team and are licensed under CC BY-NC-SA 3.0: https://www.curseforge.com/minecraft/mc-mods/superb-warfare
- The SBW-derived Walkurenritt vehicle materials require attribution to the SBW development team, are for non-commercial use, and must be shared under the same CC BY-NC-SA 3.0 terms when redistributed or adapted. Future vehicle content may have different source projects and license terms.
- This port is not affiliated with, endorsed by, or an official addon for Just Enough Guns or Superb Warfare.
