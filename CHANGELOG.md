# Changelog

## Unreleased

### Added
- Added Forge-style gun melee handling on the V key, including flashlight toggles, baseline melee sweeps, sword bayonet damage, and first-person `melee`/`bayonet` animations for animated guns.
- Added extended and drum magazine items for rifle, SMG, and shotgun magazine-fed reloads, using the same capacities as the old extended-mag and drum-mag attachment modifiers.
- Added loaded-magazine item tracking for magazine-fed guns so the current base, extended, or drum magazine type controls capacity, returned magazines, and gun-model magazine visibility.
- Added yellow tooltip hints for gun attachment-screen access and attachment items, including the magazine-feed incompatibility warning on the drum-mag attachment.

### Fixed
- Fixed magazine-fed servers so extended-mag and drum-mag attachments can no longer be installed on guns or used from old gun data to bypass magazine-fed reload behavior.
- Fixed magazine-fed reload swaps so full guns can replace the current magazine with a different valid magazine type, and reload completion validates the exact magazine selected when the reload began instead of re-scanning inventory.
- Fixed magazine-fed reload visuals so animated gun models show the old magazine type during the first half of reload and the new magazine type during the second half.
- Fixed interrupted reloads so switching away cancels reload progress and switching back replays the gun draw animation visibly instead of continuing or hiding a stale reload pose.
- Fixed first-person gun draw animations so they only trigger when switching from another hotbar item into the gun, not after inventory screen refreshes or reload state cleanup.
- Fixed reload animations ending early when an authored GeckoLib reload clip is longer than the gun's gameplay reload timer.
- Fixed first-person sprint animations so sprinting immediately after switching to a gun can interrupt lingering draw playback before firing.
- Restored Forge 1.20.1 explosive-muzzle entity-hit ignition while reducing explosive-muzzle armor piercing by 25% for balance.
- Fixed draw, reload, shoot, sprint, idle, and melee animation priority so first-person GeckoLib controllers recover cleanly after reload cancellation, hotbar switches, and local gunfire.
- Removed temporary animation and overheat debug logging added during the reload/draw diagnosis pass.

### Documentation
- Documented magazine-fed extended/drum magazine item parity and capacity-attachment gating.
- Updated the NeoForge 1.21.1 attachment-port sync notes with the final draw/reload replay behavior and triggerable draw controller handoff.

## 1.6.2 - 2026-05-27

### Changed
- Rebalanced damaged sky ship armada loot so pistol and rifle ammo are more common while iron and lapis are less dominant.
- Tuned vehicle fall impact damage to scale more noticeably with drop height.

### Fixed
- Fixed high vehicle falls sometimes landing without impact damage.
- Fixed vehicle impact sounds playing inconsistently and stopped vehicle sounds from consuming streaming sound handles.
- Fixed vehicle machine gun and coaxial machine gun fire sounds so sustained fire plays reliably.
- Fixed Waterpipe Shotgun reload animation falling into missing segmented reload clips.

## 1.6.1 - 2026-05-25

### Theme
- Enemy vehicle spawning and recovery.

### Added
- Added an admin login tip showing `/justEnoughGuns config vehicle enemySpawning enabled false` so server admins can disable enemy vehicle spawning.

### Fixed
- Fixed enemy BMP2 recovery after server restarts and improved unstuck behavior when enemy vehicles block each other.
- Fixed truck model collision coverage while keeping large AABB collision and ram damage disabled.

## 1.6.0 - 2026-05-23

### Added
- Added vehicle muzzle flare visuals and missile explosion effects.
- Added a server vehicle assembly toggle with action bar feedback when vehicle assembly is disabled.
- Added an action bar prompt when using a vehicle container without a crowbar.

### Changed
- Reworked gunner growth and loadout progression with per-type progression data and configurable mob scaling commands.
- Tuned bolt rifle gunner progression and increased minigun armor-piercing strength.
- Refined the mob config command hierarchy for gunner spawn, growth, armor, and weapon tuning.
- Added configurable gunner terrain placement controls.

### Fixed
- Fixed vehicle entity collision opt-in and aligned vehicle collision bounds with OBB data.
- Fixed OBB vehicle entity collision, including speedboat collision behavior.
- Moved and tuned the MI-28 dismount position to keep players clear of missile pods.
- Fixed server-side class loading by keeping client-only vehicle effect code off dedicated servers.
- Fixed gunner spawn and progression scaling so natural spawns and spawn eggs receive the intended loadouts.
- Fixed oversized gunner ammo drops.
- Fixed rocket gunner friendly-fire aggro.
- Fixed faction raid startup scheduling.
- Fixed gunner support block placement pacing.

### Removed
- Removed obsolete joyous armor plate item remnants and loot references.

## 1.6.0-pre - 2026-05-16

### Added
- Added the configured vehicle system with data-driven vehicles, vehicle inventories, assembling recipes, charging station support, repair tools, missile and decoy entities, vehicle HUDs, vehicle controls, and dedicated renderers.
- Added the LAV-150, BMP-2, speedboat, truck, AH-6, MI-28, A-10, TOM-6, HPJ-11, laser tower, and waveforce tower vehicle content and assets.
- Added SuperbWarfare-aligned vehicle weapons, missiles, sounds, icons, models, textures, recipes, and assembly data.

### Changed
- Tuned land, boat, and aircraft vehicle handling, camera behavior, seat positions, passenger visibility, weapon stations, reverse speeds, collision boxes, armor profiles, and damage behavior.
- Tuned LAV-150 and BMP-2 weapon, mobility, turret, missile, and armor behavior.
- Increased vehicle charging station throughput and added repair-tool vehicle support.
- Increased speedboat, LAV-150, and BMP-2 smoke-screen height, thickness, and density.

### Fixed
- Fixed vehicle sync, dismount, ghosting, menu validation, hotbar selection, reload HUD, missile lock, target-warning, and blocked-wall handling issues.
- Fixed MI-28 co-pilot weapon state, MI-28 turret pitch limits, speedboat turret control, BMP-2 target road speed, vehicle ammo display names, and duplicate vehicle reload timer display.
- Reduced vehicle debug logging noise.

### Documentation
- Added vehicle SuperbWarfare alignment mapping documentation.

## 1.5.1pre2 - 2026-05-09

### Changed
- Aligned muzzle flash and trail rendering with the current branch behavior.

## 1.5.1 - 2026-05-07

### Added
- Added custom bulletproof helmet and vest textures.
- Added ballistic armor interception for bulletproof helmets, bulletproof vests, and Bound Terror Phantom protection.
- Added gun tooltip armor-piercing display and bulletproof armor tooltip values for rating, undermatch multiplier, and overmatch multiplier.

### Changed
- Replaced bulletproof armor Projectile Protection behavior with ammo AP versus armor rating mitigation.
- Set helmet ballistic ratings to 80% of the same-tier vest rating.
- Restored balanced gun stats and kept recent first-person pose fixes.

### Documentation
- Documented the ballistic armor interception formula and durability pressure model.

## 1.4.2 - 2026-04-27

### Added
- Added NeoForge 1.21.1 combat HUD rendering for gun name, current ammo, reserve ammo, magazine reserve counts, timer bars, and hit markers.
- Added NeoForge custom crosshair rendering with dynamic crosshair support tied to current ballistic spread.
- Added S2C hit marker feedback for successful living-entity bullet hits.
- Added configurable Phantom Gunner death explosions with `/justEnoughGuns config mob phantom deathExplosion`.

### Changed
- Replaced periodic ammo actionbar updates with the right-side combat HUD while keeping contextual warning prompts.
- Updated timer rendering to use `textures/gui/timer/overheat.png` for overheat and preserve water-cooling progress.
- Rebalanced dynamic spread penalties:
  - rifle, sniper, and `light_machine_gun` walking spread uses `3.30x`, sprinting spread uses `5.50x`, and firing spread cap uses `2.70x`,
  - pistol and SMG walking spread uses `1.425x`, sprinting spread uses `2.475x`, and firing spread cap uses `1.725x`.
- Reduced `light_machine_gun` passive cooling speed to 50% of the previous value.
- Reduced `minigun` overheat gain from `8/6` heat per shot to `6/6`.

### Fixed
- Fixed dynamic crosshair not reflecting movement spread before the first shot.
- Fixed hit markers triggering on block impacts by only sending them after successful living-entity damage.
- Fixed magazine-fed guns showing "No ammo" instead of "No compatible magazine" when no suitable loaded magazine is available for reload.

## 1.3.1 - 2026-02-19 (NeoForge 1.21.11 v1.3.1 Sync + Bow Removal)

### Added
- Backported faction patrol/raid runtime flow from NeoForge 1.21.11 v1.3.1, including `Faction Omen` effect, related commands, and scheduler/spawn helpers.
- Synced `faction_omen` effect icon and localization keys (`message.jeg.faction_patrol.*`, `message.jeg.faction_raid.home_triggered`, `effect.jeg.faction_omen`) into NeoForge 1.21.1.

### Changed
- Synced 1.3.1 gunner spread scaling behavior to NeoForge 1.21.1 (non-player spread multiplier and shotgun spread multiplier paths).

### Removed
- Temporarily hard-removed non-vanilla bows (`compound_bow`, `primitive_bow`) from NeoForge 1.21.1:
  - removed gun definitions and gameplay/client mapping references,
  - removed bow recipes and bow-specific item/model resources,
  - removed bow localization entries.

## 1.3.0 - 2026-02-19 (1.21.1 Service Rifle ADS Visibility + Handguard Hide)

### Changed
- In first-person, forced `service_rifle` aiming-related bones visible: `railing`, `iron_sight`, `modified_iron_sight`, `stock_iron_sight`.
- In first-person, forced `service_rifle` handguard bones hidden: `handguard`, `light_handguard`, `tactical_handguard`, `weighted_handguard`.
- Lowered `service_rifle` ADS height by a medium step: ADS extra uplift `0.045F` -> `0.020F`.

## 1.3.0 - 2026-02-19 (1.21.1 ADS Raise Pass: Burst + Shotgun/SMG + Hypersonic Centering)

### Changed
- Raised `burst_rifle` ADS height slightly by changing ADS extra uplift from `0.000F` to `0.010F`.
- Raised `burst_rifle` ADS height again slightly by increasing its ADS extra uplift from `0.010F` to `0.020F`.
- Raised `burst_rifle` ADS height slightly again by increasing its ADS extra uplift from `0.020F` to `0.030F`.
- Raised ADS height for shotgun family and `custom_smg`/`phantom_smg` by setting their ADS extra uplift to `0.075F`.
- Raised `supersonic_shotgun` ADS height by a medium-small amount, increasing its ADS extra uplift from `0.020F` to `0.045F`.
- Lowered `service_rifle` ADS height slightly by moving it from default uplift (`0.060F`) to the stable uplift tier (`0.050F`).
- Lowered `service_rifle` ADS height slightly again by reducing ADS extra uplift from `0.050F` to `0.045F`.
- Lowered `service_rifle` ADS height by a medium step by reducing ADS extra uplift from `0.045F` to `0.020F`.
- Added first-person `service_rifle` bone-visibility override:
  - force-show aiming-related bones (`railing`, `iron_sight`, `modified_iron_sight`, `stock_iron_sight`),
  - force-hide handguard bones (`handguard`, `light_handguard`, `tactical_handguard`, `weighted_handguard`).
- Kept previous low-uplift retunes for `flamethrower` and `light_machine_gun` (`0.020F`) and preserved stable-anchor group behavior.
- Centered `hypersonic_cannon` in ADS by shifting `adsX` from `0.35F` to `0.24F`.

## 1.3.0 - 2026-02-19 (1.21.1 ADS Retune: Burst/Flamethrower/Supersonic + Global Others Up)

### Changed
- Raised global ADS extra-height baseline for "other guns" from `0.050F` to `0.060F` (`adsExtraHeight` default path), so non-exempt weapons aim slightly higher.
- Kept these weapons unchanged at their previous ADS uplift level (`0.050F`): `hollenfire_mk2`, `infantry_rifle`, `blossom_rifle`, `subsonic_rifle`, `soulhunter_mk2`.
- Kept `combat_rifle` excluded from ADS extra uplift (`0.0F`) as parity anchor.
- Slightly raised `rocket_launcher` ADS net height indirectly by letting it receive the new default ADS uplift path.
- Applied medium-large ADS downshift for `burst_rifle` via dedicated compensation override: `LegacyComp(0.60F, 0.68F, -0.03F)` and zero ADS extra uplift.
- Lowered `flamethrower` ADS (medium-small) and shifted its overall pose left/down:
  - pose `hipX/hipY`: `0.62/-0.38` -> `0.56/-0.42`
  - pose `adsX/adsY`: `0.34/-0.48` -> `0.28/-0.52`
  - ADS extra uplift reduced to `0.020F`.
- Added dedicated `SUPSERSONIC_SHOTGUN_PROFILE`:
  - ADS lowered slightly-medium (`adsY`: `-0.52` -> `-0.56`)
  - ADS moved forward medium (`adsZ`: `-0.78` -> `-0.86`)
  - ADS extra uplift reduced to `0.020F`.
- Applied slight ADS uplift for `light_machine_gun` by moving it from no ADS extra uplift to `0.020F`.

## 1.3.0 - 2026-02-19 (1.21.1 ADS Coverage Correction + Targeted Recalibration)

### Changed
- Corrected global ADS extra-height coverage so it applies to all guns except only `light_machine_gun` and `combat_rifle`.
- Lowered `soulhunter_mk2` hip (non-ADS) height to a medium-lower profile while keeping its ADS net height near previous level.
- Slightly increased `rocket_launcher` ADS net height via dedicated compensation re-tuning under the corrected global ADS uplift.
- Recalibrated `minigun` ADS dedicated compensation to keep net ADS height stable under the broader global ADS uplift.

## 1.3.0 - 2026-02-19 (1.21.1 ADS Height Batch Retune)

### Changed
- Lowered ADS height moderately for `soulhunter_mk2` and `minigun` by reducing their dedicated ADS compensation targets.
- Lowered ADS height slightly for `rocket_launcher` and shifted its ADS pose slightly left by reducing `rocket_launcher` `adsX`.
- Increased global ADS extra height uplift for all other guns to `0.050F` while excluding `light_machine_gun`, `combat_rifle`, `rocket_launcher`, `minigun`, and `soulhunter_mk2`.

## 1.3.0 - 2026-02-19 (1.21.1 Visibility + ADS Height Retune)

### Changed
- Fixed first-person hand visibility for `rocket_launcher` and `minigun` by forcing dual-arm rendering and replacing their arm transforms with in-frame neutral heavy-arm transforms.
- Lowered `typhoonee` first-person height to a medium-lower profile: `LegacyComp(0.98F, 1.12F, -0.06F)`.
- Greatly lowered `soulhunter_mk2` first-person height with a dedicated override: `LegacyComp(0.22F, 0.30F, -0.04F)`.
- Added an ADS-only extra height uplift for all guns except `light_machine_gun` and `combat_rifle` via `adsExtraHeight(...)`.

## 1.3.0 - 2026-02-19 (1.21.1 Typhoonee Downshift + RPG/Minigun Arm Visibility)

### Changed
- Reduced `typhoonee` first-person compensation from `LegacyComp(1.70F, 1.86F, -0.06F)` to `LegacyComp(1.20F, 1.34F, -0.06F)` to move the weapon substantially lower while keeping it visible.
- Split `rocket_launcher`/`minigun` first-person pose mapping into body-vs-arm responsibilities:
  - kept the currently tuned swapped gun body offsets/scales,
  - restored arm-side behavior and arm transforms to their original weapon ownership to prevent missing-hands frames.

## 1.3.0 - 2026-02-19 (1.21.1 Typhoonee First-Person Visibility Hotfix)

### Changed
- Added a dedicated `typhoonee` first-person compensation override in `GunItemClientExtensions` instead of sharing the generic heavy-weapon compensation bucket.
- Set `typhoonee` compensation to `LegacyComp(1.70F, 1.86F, -0.06F)` to keep the weapon body consistently inside the first-person view window on NeoForge 1.21.1.

## 1.3.0 - 2026-02-19 (1.21.1 Render-Path Flattening Follow-up)

### Changed
- Flattened first-person arm rendering path in NeoForge 1.21.1 to match NeoForge 1.21.11 behavior: `FirstPersonGunArmRenderEvents` now does not render event-based arm overlays by default.
- Kept first-person arm rendering authoritative in GeckoLib bone layer to avoid fallback-path motion flattening across heavy weapons (e.g. `minigun` vs `rocket_launcher`).
- Clarified per-gun compensation intent in `GunItemClientExtensions`: values are anchored by `combat_rifle` parity and used after render-path alignment.

## 1.3.0 - 2026-02-19 (1.21.1 Per-Gun Compensation Table Applied)

### Changed
- Replaced NeoForge 1.21.1 first-person legacy category-based compensation with an explicit per-gun compensation table in `GunItemClientExtensions`.
- Compensation now uses per-gun `hipY/adsY/z` values (linearly blended by ADS progress) derived from the `combat_rifle` visual parity anchor against NeoForge 1.21.11.
- Restored `combat_rifle` `GunPoseProfile` hip/ADS Y values back to NeoForge 1.21.11 baseline values and moved parity uplift responsibility into the per-gun compensation table.

## 1.3.0 - 2026-02-19 (1.21.1 First-Person Arm Visibility Root-Cause Fix)

### Fixed
- Fixed NeoForge 1.21.1 first-person hand invisibility caused by brittle reflective hand-render invocation:
  - Added a shared hand-render invoker (`HandRenderInvoker`) that supports both 5-parameter and 6-parameter `renderLeftHand/renderRightHand` signatures.
  - Removed duplicated reflection paths between arm-bone layer and event fallback, so both use one compatibility implementation.
- Fixed fallback starvation where event arm rendering was skipped only because GeoLib arm layer was "operational" even when no arm was actually drawn in the current visual flow.

### Changed
- Updated `GunFirstPersonArmsLayer` to publish a recent-render signal and let event fallback activate when no arm was rendered recently.
- Kept GeoLib arm layer active across transient failures (no hard one-way shutdown), with event fallback covering missing-arm frames.

## 1.3.0 - 2026-02-18 (1.21.1 First-Person Compensation Recalibration Follow-up)

### Changed
- Recalibrated NeoForge 1.21.1 first-person legacy compensation in `GunItemClientExtensions` to better match NeoForge 1.21.11 visual framing under the same `GunPoseProfile` parameter system.
- Increased global first-person Y baseline compensation and ADS-stage Y uplift so guns no longer sit excessively low in the 1.21.1 render baseline.
- Increased category-specific Y compensation for `RIFLE/SHOTGUN/SNIPER/LMG/HEAVY/default` groups to preserve relative weapon class posture while lifting overall sightline alignment.

## 1.3.0 - 2026-02-18 (1.21.1 First-Person Parameter-System Adaptation)

### Added
- Added `GunFirstPersonArmsLayer` for the NeoForge 1.21.1 branch (`src/main/java/ttv/migami/jeg/client/render/gun/layer/GunFirstPersonArmsLayer.java`) to attempt GeckoLib-bone-driven first-person arm rendering with runtime safety fallback.
- Added compatibility helpers in `AnimatedGunRenderer` to resolve first-person context and active hand from current render state (`currentPerspective`, `isFirstPersonContext`, `resolveRenderedHand`).

### Changed
- Updated `AnimatedGunRenderer` in NeoForge 1.21.1 to register the first-person arms layer (`addRenderLayer(new GunFirstPersonArmsLayer(this))`), aligning renderer composition with the newer profile-based first-person pipeline.
- Updated `FirstPersonGunArmRenderEvents` to act as fallback-only path: when bone-layer rendering is operational, event-based hand rendering is skipped.
- Updated `GunItemClientExtensions` to keep the 1.21.11 profile interpolation model while adding 1.21.1-specific baseline compensation:
  - global Y/Z baseline compensation,
  - ADS extra Y compensation,
  - category-group compensation (`RIFLE/SHOTGUN/SNIPER/LMG/HEAVY/default`) via `GunCategory`.

### Fixed
- Fixed first-person guns appearing significantly too low in NeoForge 1.21.1 after profile-system migration by introducing explicit legacy baseline compensation in the hand-transform stage.
- Fixed first-person arm/gun reference-frame drift by prioritizing bone-driven arm rendering and demoting event overlay rendering to compatibility fallback only.

## 1.2.3 - 2026-02-14

### Changed
- Reworked `grenade_launcher`: reduced projectile travel range, aligned launcher grenade explosion damage with direct-thrown `grenade`, and made launched grenades explode immediately on any entity hit.
- Synced gun `damage` and `projectileAmount` values with `Just-Enough-Guns-NeoForge-1.21.11` across branches.
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

> Note: NeoForge 1.21.1 branch applies these 1.3.0 changes with behavior-equivalent backports where APIs differ.

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
