# NeoForge 26.2 Port Log

## 2026-06-17

### Branches
- Created `neoforge26.2` from `04af798` and pushed it to `origin/neoforge-26.2`.
- Created `neoforge26.1-legacy` from `04af798` and pushed it to `origin/neoforge-26.1-legacy`.
- Removed the old `origin/neoforge-26.1` remote branch after both replacement branches existed.

### Dependency target
- Minecraft: `26.2`
- NeoForge: `26.2.0.1-beta`
- GeckoLib: `com.geckolib:geckolib-neoforge-26.2:5.5.1`
- Artifact suffix: `+neoforge26.2`

### Migration notes
- Replaced direct `Minecraft.screen` reads and `Minecraft.setScreen(...)` calls with `minecraft.gui.screen()` and `minecraft.gui.setScreen(...)`.
- Replaced `GameRenderer#getMainCamera()` calls with `GameRenderer#mainCamera()`.
- Migrated vanilla entity and block entity constants from `EntityType` / `BlockEntityType` to `EntityTypes` / `BlockEntityTypes`.
- Updated rarity color reads to use the 26.2 style modifier path.
- Replaced removed Happy Ghast body armor setter calls with `setItemSlot(EquipmentSlot.BODY, ...)`.
- Updated entity renderer `submit(...)` overrides to the 26.2 `SubmitNodeCollector` / `CameraRenderState` signature.
- Moved custom world bullet trail and muzzle flash submission onto `SubmitCustomGeometryEvent`.
- Updated `ItemInHandRendererMixin` from `renderArmWithItem` to `submitArmWithItem` after the old target crashed client startup.

### Verification
- `.\gradlew.bat compileJava` passed on `Just-Enough-Guns-NeoForge-26.2`.
- `.\gradlew.bat build` passed on `Just-Enough-Guns-NeoForge-26.2`.
- `.\gradlew.bat runClient` reached an integrated world, logged in `Dev`, loaded `150 jeg recipes`, and shut down cleanly after the mixin target fix.
- A temporary `neoforge26.1-legacy` worktree passed `.\gradlew.bat compileJava`, then was removed.

### Remaining manual checks
- First-person animated gun rendering and arms.
- Attachment screen preview and tooltips.
- Gun HUD, vehicle HUD, and Happy Ghast armor HUD.
- Bullet trails and muzzle flashes.
- Vehicle missile and decoy entity rendering.
- GeckoLib model rendering across gun, vehicle, repair tool, and terror phantom content.
