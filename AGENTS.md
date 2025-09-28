# Repository Guidelines

## Project Structure & Module Organization
Core sources live in `src/main/java/ttv/migami/jeg`, grouped by concern (`init`, `item`, `entity`, `client`, etc.). In NeoForge 1.21.8 these packages mirror the original Forge layout—reuse existing registries under `init` when wiring new content. Runtime data and JSON assets belong in `src/main/resources`, while generated assets land in `src/generated/resources` (always inspect diffs before committing regenerated files). Recipes and tags should avoid third-party materials; rely on vanilla ingots, redstone pieces, and gunpowder.

## Build, Test, and Development Commands
`./gradlew build` compiles with NeoForge 21.8.47 and assembles the mod jar in `build/libs`. `./gradlew runClient` launches the integrated NeoForge client for functional checks. `./gradlew runServer` exercises dedicated-server logic for projectiles and AI. `./gradlew runData` regenerates loot tables, recipes, and tags into `src/generated/resources`—rerun whenever registries or recipes shift.

## Coding Style & Naming Conventions
Target Java 21 with four-space indentation. Classes remain PascalCase, methods/fields camelCase, and constants ALL_CAPS. Registry ids, JSON file names, and data components stay lower_snake_case (e.g., `jeg:desert_eagle`). Keep mixins beneath `mixin` and suffix them with `Mixin`. Prefer composition over ad-hoc singletons—extend the existing `ModItems`, `ModEntities`, and capability helpers for new guns or ammo.

## Testing Guidelines
Automated unit tests are not present; rely on manual passes through `runClient` (firing, reloading, HUD, recipe unlocks) and `runServer` for multiplayer syncing. Whenever data generators run, audit Git diffs to verify no unexpected vanilla overrides. Document manual checks in pull requests so other contributors can repeat them.

## Commit & Pull Request Guidelines
Recent history favors short, imperative commits (often with a `❤` prefix for player-facing fixes). Follow that tone when appropriate and keep each commit focused on one change set. Pull requests should summarize gameplay impact, list manual test coverage, link tracked issues, and include screenshots or clips for visual or animation work. Mention any known HUD fallbacks if original overlays cannot be replicated on 1.21.8.
