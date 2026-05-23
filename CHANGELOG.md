# Changelog

## 1.6.0 - 2026-05-23

### Added
- Added vehicle muzzle flare visuals and missile explosion effects.
- Added a server vehicle assembly toggle with action bar feedback when vehicle assembly is disabled.
- Added an action bar prompt when using a vehicle container without a crowbar.
- Synced Happy Ghast armored joy harness behavior, HUD support, and damage handling.

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
- Fixed minigun third-person render alignment on the 26.1 item model path.
- Fixed oversized gunner ammo drops.
- Fixed rocket gunner friendly-fire aggro.
- Fixed faction raid startup scheduling.
- Fixed gunner support block placement pacing.

### Removed
- Removed obsolete joyous armor plate item remnants and loot references.

## 1.6.0-pre - 2026-05-16

### Added
- Synced the configured vehicle system to Fabric 26.1, including data-driven vehicles, inventories, assembling recipes, charging station support, repair tools, missile and decoy entities, vehicle HUDs, controls, cameras, and dedicated renderers.
- Added LAV-150, BMP-2, speedboat, truck, AH-6, MI-28, A-10, TOM-6, HPJ-11, laser tower, and waveforce tower vehicle content and assets.
- Added SuperbWarfare-aligned vehicle weapons, missiles, sounds, icons, models, textures, recipes, assembly data, and alignment documentation.

### Changed
- Tuned land, boat, and aircraft vehicle handling, camera behavior, seat positions, passenger visibility, weapon stations, reverse speeds, collision boxes, armor profiles, and damage behavior.
- Tuned LAV-150 and BMP-2 weapon, mobility, turret, missile, and armor behavior.
- Increased vehicle charging station throughput and added repair-tool vehicle support.
- Increased speedboat, LAV-150, and BMP-2 smoke-screen height, thickness, and density.

### Fixed
- Fixed vehicle sync, dismount, ghosting, menu validation, hotbar selection, reload HUD, missile lock, target-warning, and blocked-wall handling issues.
- Fixed MI-28 co-pilot weapon state, MI-28 turret pitch limits, speedboat turret control, BMP-2 target road speed, vehicle ammo display names, and duplicate vehicle reload timer display.
- Synced helicopter zoom camera/passenger fixes, deployed vehicle ten-percent starting energy, and first-person repair-tool player arm rendering.
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
- Restored balanced gun stats and preserved recent NeoForge ADS transform fixes.

### Documentation
- Documented the ballistic armor interception formula and durability pressure model.

## 1.4.2 - 2026-04-27

### Added
- Added configurable Phantom Gunner death explosions with `/justEnoughGuns config mob phantom deathExplosion`.

### Changed
- Synced NeoForge 26.1 gun HUD, dynamic crosshair, hit marker, spread, and overheat behavior with the NeoForge 1.21.1 1.4.2 implementation.
- Added configurable ammo HUD, timer HUD, crosshair, hit marker, and dynamic crosshair dot settings.
- Updated player gun spread to combine firing spread accumulation with movement spread degrees.
- Reduced light machine gun passive overheat cooling and lowered minigun heat gain.

### Fixed
- Hit markers now trigger only after successful living-entity bullet damage, including direct explosive hits, and no longer trigger for block hits or failed/friendly-fire hits.
- Overheat now renders with the timer texture while preserving water-cooling progress rendering.
- Fixed magazine-fed guns showing "No ammo" instead of "No compatible magazine" when no suitable loaded magazine is available for reload.

## 1.3.1 - 2026-02-20

### Added
- Added NeoForge 1.21.11 faction patrol/raid runtime modules (`FactionEventTicker`, patrol encounter tracking, home-raid trigger flow, raid controller entity) aligned with newer branch behavior.
- Added faction patrol boss bar UI and localized messages for patrol/omen/home-trigger events.
- Added `Faction Omen` status effect registration and icon resource (`textures/mob_effect/faction_omen.png`).
- Added patrol spawn diagnostics in command feedback/logs, including detailed spawn-failure categories for troubleshooting.
- Added configurable day-scaling gunner accuracy controls (`gunnerAccuracyStartDay`, `gunnerAccuracyDaysToMax`, `gunnerAccuracyMaxSpreadMultiplier`).
- Added configurable gunner shotgun spread control (`gunnerShotgunSpreadMultiplier`).

### Changed
- Crosshair is hidden while the player is holding any gun item (main hand/offhand).
- Faction raids are now bound to patrol omen faction context; patrol boss bar duplication was removed.
- Home-return raid trigger consumes faction omen and starts raid near player respawn/home position.
- Patrol spawn position sampling now prefers valid surface positions near target players.
- Path setup at spawn now uses tolerant fallback flow and records path-init failure for diagnostics.
- Same-faction gunners are treated as friendly targets and no longer attack each other.
- Gunner loadouts are locked: gunner mobs are prevented from picking up loot and can recover a gun when disarmed.
- Non-flamethrower guns now use `IGNORE_LEAVES` bullet collision behavior.
- Weapon handling pass updated recoil/movement spread behavior by weapon class.
- Weapon durability baseline was increased (about 2.5x), with additional durability/gravity tuning pass.

### Fixed
- Fixed raid ground spawns to avoid leaves and unsafe relocation outcomes.
- Fixed raid mob unreachable timeout behavior via forced repath/relocation.
- Fixed unreachable-cleanup edge case when no nearby players are present.
- Unified raid target validation and constrained terror raid target range.
- Removed monster auto-win outcome when no players are inside raid radius.
- Tightened empty-fire visual handling and removed empty-fire recoil animation side effects.
- Fixed machine-gun overheat bar visibility on NeoForge HUD.

### Balance
- Rebalanced terror phantom survivability and protection:
  - adjusted HP/protection values
  - set armor to `6`
  - increased projectile protection to `5`
- Reworked rocket damage model:
  - adjusted blast damage/falloff
  - increased direct-hit damage and ensured direct hits bypass terror projectile protection.
- Rebalanced recoil/spread across weapon classes and removed flamethrower recoil.
- Added/updated tiered low armor values for bulletproof helmets and vests.
- Gunner shot spread now scales with in-game day progression; shotgun pellet spread is tighter by default.
