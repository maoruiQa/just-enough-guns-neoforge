# Changelog

## 1.4.2 - 2026-04-27

### Added
- Added Fabric combat HUD rendering for gun name, current ammo, reserve ammo, magazine reserve counts, overheat/timer status, and hit markers.
- Added Fabric custom crosshair rendering with dynamic crosshair support tied directly to current ballistic spread.
- Added S2C hit marker feedback for successful living-entity bullet hits.
- Added configurable Phantom Gunner death explosions with `/justEnoughGuns config mob phantom deathExplosion`.

### Changed
- Reworked Fabric dynamic crosshair placement: removed the center dot, equalized all four reticle line lengths, and made reticle expansion proportional to current gun spread.
- Replaced periodic ammo actionbar spam with the right-side combat HUD while keeping contextual warning prompts.
- Updated magazine-fed gun reserve display to show magazine counts with a `MAG` label.
- Rebalanced dynamic spread penalties:
  - rifle, sniper, and `light_machine_gun` movement spread penalties are doubled from the previous tuning pass,
  - pistol and SMG movement spread penalties are increased by 1.5x from the previous tuning pass,
  - rifle, sniper, and `light_machine_gun` firing spread cap is increased to `2.70x`,
  - pistol and SMG firing spread cap is increased to `1.725x`.
- Reduced `light_machine_gun` passive cooling speed to 50% of the previous value.
- Reduced `minigun` overheat gain to 75% of the previous value.

### Fixed
- Fixed Fabric dynamic crosshair not reflecting movement spread before the first shot.
- Fixed Fabric dynamic crosshair center-dot alignment issues by removing the dot from the dynamic reticle.
- Fixed dynamic crosshair asymmetry caused by incorrect 9x9 texture sampling for the right and bottom reticle lines.
- Fixed magazine-fed guns showing reserve ammo ambiguously instead of loaded magazine count.

## 1.3.4 - 2026-03-10

### Fixed
- Fixed Fabric `gunner_spawn_egg` faction mobs spawning without a gun by restoring immediate loadout assignment on spawn.
- Restored Fabric natural spawn hooks for `pillager gunner` and `phantom gunner` conversions so their special gunner variants can spawn correctly again.

## 1.3.0 - 2026-02-19 (Fabric 1.21.1 First-Person Parity Sync from NeoForge 1.21.1)

### Added
- Added first-person arm rendering compatibility chain for Fabric 1.21.1:
  - `src/client/java/ttv/migami/jeg/client/render/gun/HandRenderInvoker.java`
  - `src/client/java/ttv/migami/jeg/client/render/gun/layer/GunFirstPersonArmsLayer.java`
- Added first-person context/hand-resolution helpers to Fabric `AnimatedGunRenderer` and registered the arm layer.

### Changed
- Synced `GunItemClientExtensions` to the latest NeoForge 1.21.1 compensation-driven parameter system, including:
  - per-gun `LegacyComp` compensation table (`hipY/adsY/z`),
  - ADS extra-height tiers and weapon-specific overrides (`service_rifle`, shotgun/custom-smg family, `burst_rifle`, etc.),
  - recoil/equip/swing first-person transform flow parity.
- Synced `GunPoseProfile` values to NeoForge 1.21.1 latest first-person tuning set (including dedicated `combat_rifle`, `hollenfire_mk2`, `supersonic_shotgun`, heavy-weapon profiles, and `hypersonic_cannon` ADS centering).
- Kept Fabric mixin call-chain compatibility by restoring static `GunItemClientExtensions.applyForStats(...)` as the shared transform entrypoint.
- Synced service-rifle first-person bone policy in arm-layer rendering:
  - force-show `railing`, `iron_sight`, `modified_iron_sight`, `stock_iron_sight`,
  - force-hide `handguard`, `light_handguard`, `tactical_handguard`, `weighted_handguard`.

## 1.2.3 - 2026-02-14

### Changed
- Reworked `grenade_launcher`: reduced projectile travel range, aligned launcher grenade explosion damage with direct-thrown `grenade`, and made launched grenades explode immediately on any entity hit.
- Rebalanced weapon crafting costs to align with 1.20.1 baselines and current 1.21.x weapon value (hybrid burst + sustained output).
- Rebalanced core ammo economy (`pistol_ammo`, `rifle_ammo`, `shotgun_shell`, `rocket`) to keep high-efficiency loadouts in line with progression.
- Rebalanced `light_machine_gun`: slightly increased damage and updated handling balance.
- Rebalanced `minigun`: higher practical fire output, lower recoil, and reduced per-shot damage.
- Updated `minigun` trigger behavior to fire 5 rounds per trigger for both players and AI.
- Removed trail flame particles from direct-fire (non-drop ballistic) weapons.
- Standardized gunfire audible range to 120 blocks.
- Greatly increased default natural `terror_phantom` spawn chance to `0.03` (3%).
- Increased `terror_phantom` health to `180` and `bound_terror_phantom` health to `200`.

### Fixed
- Fixed missing `gunner_spawn_egg` texture issues across synced branches.
- Removed `abstract_gun` from Terror Phantom reward/supply barrel outputs, including loot-table and fallback fill paths.
- Fixed raid scheduler `ConcurrentModificationException` crash during post-death terror raid wave execution.
- Fixed terror raid mob attrition caused by raid-on-raid friendly fire and invalid aggro redirection.
- Fixed terror phantom death-phase crash caused by duplicate follow-range modifier application during raid spawn.
- Hardened terror phantom death resolution to ensure one-time death completion and prevent death-phase lockups.

### Added
- Terror raid waves now target nearby players directly.
- Raid wave interval and wave counts are configurable.
- Terror raid now shows a village-raid-style boss bar for active waves.

## 1.3.0 - 2026-02-15

### Added
- NeoForge 1.21.11: GeckoLib v5 animation support for guns and Terror Phantom variants (Guardian/bound variant shares model/animations and stays in sync with Terror Phantom; texture differs).
- Added a dev smoke-test run config (`runClientJoinLocal`) for quickly joining a local server instance.

### Fixed
- Fixed client crash on world join caused by incorrect GeckoLib v5 animation resource identifiers (using `.animation` in the id caused GeckoLib to look for `*.animation.animation.json`).
- Fixed NeoForge datapack recipe parse error for `typhoonee` due to invalid shaped-recipe key ingredient format.

## 1.3.0 - 2026-02-16

### Changed
- Reworked NeoForge 1.21.11 first-person gun handling to match 1.20.1 gameplay intent: left click shoots, right click handles ADS.
- Added profile-driven first-person pose tuning via `GunPoseProfile`, including per-gun hip/ADS transforms and explicit `leftArm/rightArm` transform fields.
- Expanded major weapons to explicit profile entries and enabled broad profile reuse for fast iteration (`CUSTOM_SMG_PROFILE`, heavy weapon profiles, etc.).
- Unified multiple weapon groups to shared first-person tuning profiles (rifle family, selected heavy/launcher variants, selected shotgun/bow variants during tuning pass).

### Fixed
- Fixed first-person arm visibility/placement regressions introduced during 1.21.11 migration (arms occluding camera, detached offhand, and weapon framing drift).
- Added first-person arm rendering controls so per-weapon arm visibility can be configured through profile data (including launcher-specific behavior).
- Integrated client shoot request networking and `GunItem` shoot entrypoint normalization for stable client/server fire flow in 1.21.11.
- Fixed gun rendering in non-first-person contexts (third-person/inventory/GUI/ground/fixed) by restoring a strict vanilla base-model fallback path while keeping GeckoLib for first-person only.
- Prevented transform stacking between GeckoLib and vanilla item display transforms in non-first-person contexts, resolving "floating/invisible/air-like" gun renders.
- Temporarily hard-reverted gun item definitions from `minecraft:special` to `minecraft:model` (1.21.10-style) to guarantee stable third-person/inventory/GUI rendering while GeckoLib item-special pipeline issues are isolated.
- Switched gun item models to `minecraft:select` + `minecraft:display_context`: first-person (`firstperson_righthand`/`firstperson_lefthand`) uses GeckoLib special rendering, while all other contexts use vanilla `minecraft:model` fallback.

## 1.3.0 - 2026-02-16 (Ballistics And Trail Tuning)

### Changed
- Reworked bullet trail rendering in NeoForge 1.21.11 toward 1.20.1 behavior (legacy-style trail growth and per-bullet trail updates from server payloads).
- Updated trail rendering/material setup during visual parity tuning to stabilize visibility and color readability in-game.
- Reduced muzzle smoke (`SMOKE`) spawn chance to `25%` per shot and kept it muzzle-only for both player and AI firing paths.
- Synced spread values to 1.20.1 baselines for `light_machine_gun`, `minigun`, and shotgun family weapons (`double_barrel_shotgun`, `pump_shotgun`, `repeating_shotgun`, `holy_shotgun`, `supersonic_shotgun`, `waterpipe_shotgun`).
- Restored bullet launch speed to direct `stats.projectileSpeed()` for both player and AI paths after temporary slowdown tuning.

### Fixed
- Fixed multiple bullet trail visibility regressions encountered during 1.21.11 parity iteration (invisible/over-transparent/over-dark trail states depending on render path).

## 1.3.0 - 2026-02-16 (Spawn And Recoil Follow-up)

### Changed
- Reduced default natural `terror_phantom` conversion chance and added time-based spawn scaling controls so gunner/terror conversion rates ramp from early-game values to capped late-game values.
- Added a near-area cap to prevent multiple naturally spawned `bound_terror_phantom` entities from appearing too close together.
- Updated `minigun` trigger behavior to remove the extra per-trigger burst and match other guns (single shot per trigger event).
- Re-tuned NeoForge 1.21.11 shooter backstep recoil repeatedly toward lower force and restored vertical recoil contribution (using full look vector), with global server config scaling support.

### Fixed
- Set summoned `phantom_gunner` max health to `19` HP when created by `terror_phantom` summon flow.
- Added block-hit impact animation playback for bullet-vs-block hits (vanilla block impact particle event), controlled by server config.

## 1.3.0 - 2026-02-16 (1.20.1 Ballistics Parity Implementation)

### Changed
- Implemented 1.20.1-style bullet trajectory parity for bullet-class firearms in NeoForge 1.21.11, including legacy spread vector math, ADS-aware spread scaling, and server-side burst spread tracking.
- Switched legacy bullet trail synchronization from per-tick bullet updates to spawn-time payload dispatch with client-side trail simulation (position, gravity, age, and distance culling).
- Added C2S aiming-state sync payload to support server-authoritative ADS spread behavior parity.
- Updated player and AI bullet fire paths to dispatch trail payloads at projectile spawn for pellet-safe multi-projectile support.
- Aligned bullet-class gravity handling to 1.20.1 baseline behavior while keeping special projectile weapon behavior intact.

### Fixed
- Fixed shotgun multi-pellet damage collapsing to single effective hits by restoring 1.20.1-style post-hit invulnerability reset (`invulnerableTime = 0`) for player-fired bullet impacts.
- Fixed pellet cross-interference in legacy entity ray checks by ignoring bullet-on-bullet collisions in closest-hit resolution.

## 1.3.0 - 2026-02-16 (Gunner Spawn Egg Icon Fix)

### Fixed
- Fixed NeoForge 1.21.11 gunner spawn egg item icons incorrectly sharing the zombie gunner base egg appearance.
- Updated gunner spawn egg item model `layer0` textures to match each corresponding vanilla mob egg base (skeleton/stray/pillager/vindicator/piglin family/drowned/etc.), while preserving the JEG helmet overlay layer.

## 1.3.0 - 2026-02-16 (Terror Phantom Spawn Cadence And Timeout)

### Added
- Added a global natural Terror Phantom spawn cadence gate: at most one natural Terror Phantom conversion can occur every 10 in-game days.
- Added per-entity inactivity timeout for Terror Phantom variants: if not attacked for 1 in-game day, the entity despawns automatically.

### Changed
- Persisted each Terror Phantom's last-attacked game-time data through save/load so inactivity despawn timing remains consistent across chunk unloads/reloads.

## 1.3.0 - 2026-02-16 (Bound Terror Phantom Projectile Protection)

### Added
- Added configurable projectile-protection scaling for `bound_terror_phantom` (guardian variant) using a vanilla-like Projectile Protection reduction formula.
- Added server config `boundTerrorPhantomProjectileProtectionLevel` (default `1`, range `0-20`) to control guardian projectile damage reduction strength.
