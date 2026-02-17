# Just Enough Guns – NeoForge 1.21.1 Port

## Objective
- Target Minecraft **1.21.1** as the baseline, but keep the build compatible through **1.21.4**.
- Use the **1.21.10** project as the feature reference (do not modify it).
- Track deltas from NeoForge **21.10.x → 21.1.x** and document any intentional deviations.

## Source Material
- Gameplay & registry logic: `../Just-Enough-Guns-NeoForge-1.21.10`
- Crafting & data packs: `../Just-Enough-Guns-NeoForge-1.21.10/src/main/resources`
- Empty template scaffold: `examplemod-template-1.21.1.zip`
- Mojang hotfix notes: [Minecraft 1.21.1 Release](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-1)
- NeoForge changelog: [neoforged.net/changelog](https://neoforged.net/changelog/)

## Porting Checklist
1. Mirror Gradle, toolchain, and metadata from 1.21.10 without altering the mother tree.
2. Introduce version constants / dependency ranges that cover 1.21.1–1.21.4 once verified.
3. Split client-only helpers into `src/client/java/ttv/migami/jeg`.
4. Copy common Java packages, registries, and data exactly; re-test ModelManager timing fixes.
5. Validate recipes stay under `data/jeg/recipe/` and regenerate tags if mappings require it.
6. Smoke-test via `./gradlew compileJava`, `runClient`, and `runServer` for each supported patch.

## Version Matrix Notes
- Default dev environment pins `minecraft_version=1.21.1` / `neo_version=21.1.214`.
- To smoke-test other hotfixes use Gradle properties, e.g.:
  - `./gradlew runClient -Pminecraft_version=1.21.4 -Pneo_version=21.4.X -Pparchment_minecraft_version=1.21.4`
- Keep `minecraft_version_range` at `[1.21.1,1.21.5)` so the published jar declares compatibility with 1.21.1–1.21.4.

## Client Separation
- Client-only packages now reside in `src/client/java/ttv/migami/jeg/client/**`.
- The common source set calls into client helpers exclusively through the reflective `ClientOnly` bridge to avoid dedicated-server class loading issues.

## Open Questions
- Confirm final NeoForge build numbers for 1.21.2–1.21.4 (documentation still sparse).
- Determine whether GeckoLib/framework jars need 1.21.1-specific updates or can stick to 1.21.8 builds.
- Re-evaluate projectile and flamethrower balance when backporting stats from 1.21.10 fixes.

_Updated:_ <!-- TODO: stamp once major milestones land -->
