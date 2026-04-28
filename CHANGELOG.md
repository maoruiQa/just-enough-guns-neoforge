# Changelog

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
