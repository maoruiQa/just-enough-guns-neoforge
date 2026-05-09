# Just Enough Guns – NeoForge 1.21.1 Workspace

## Overview
- Builds the NeoForge branch of **Just Enough Guns** against Minecraft **1.21.1**, with declared compatibility through **1.21.4**.
- All gameplay logic and assets are sourced from the `Just-Enough-Guns-NeoForge-1.21.10` mother tree (which must remain untouched).
- Common code lives under `src/main/java/ttv/migami/jeg`, while client-only helpers belong in `src/client/java/ttv/migami/jeg`.

## Build & Verification
- `./gradlew compileJava` – quick syntax and mappings validation.
- `./gradlew runClient` / `./gradlew runServer` – smoke-test rendering, HUD, networking, and AI.
- Override Minecraft/NeoForge patches for broader testing:
  ```
  ./gradlew runClient \
    -Pminecraft_version=1.21.4 \
    -Pneo_version=21.4.X \
    -Pparchment_minecraft_version=1.21.4
  ```
- Use `./gradlew clean` whenever switching mappings or migrating dependency jars.

## Directory Notes
- `src/main/resources` – assets, data packs (`data/jeg/recipe/`, loot tables, tags).
- `src/client/java` – renderer/HUD/keybind code guarded via `ClientOnly`.
- `libs/` – drop third-party jars (e.g., GeckoLib) if Maven coordinates are unavailable.
- `PORTING_STATUS.md` – running checklist, references, and compatibility notes.

## Manual QA Expectations
- Targeted in-game checks per weapon type (fire modes, ammo cycling, projectile AI).
- `/reload` validation for JSON changes before packaging.
- Record any intentionally skipped GeckoLib or animation hooks for review.

## Ballistic Armor Interception

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
