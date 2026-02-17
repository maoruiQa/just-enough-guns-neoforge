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
