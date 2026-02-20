# Changelog

## 1.3.1 - 2026-02-18

### Added
- Added NeoForge 1.21.11 faction patrol/raid runtime modules (`FactionEventTicker`, patrol encounter tracking, home-raid trigger flow, and raid controller entity) aligned with newer branch behavior.
- Added faction patrol boss bar UI and localized messages for patrol/omen/home-trigger events.
- Added `Faction Omen` status effect registration and icon resource (`textures/mob_effect/faction_omen.png`).
- Added patrol spawn diagnostics in command feedback/logs, including detailed spawn-failure categories for rapid troubleshooting.
- Added muzzle flame effects for gunfire presentation.
- Added explosion flame visual effects for explosive impact moments.
- Added new ballistic behavior for projectile trajectories.
- Added configurable day-scaling gunner accuracy controls (`gunnerAccuracyStartDay`, `gunnerAccuracyDaysToMax`, `gunnerAccuracyMaxSpreadMultiplier`).
- Added configurable gunner shotgun spread control (`gunnerShotgunSpreadMultiplier`).

### Changed
- Crosshair is now hidden while the player is holding any gun item (main hand or offhand).
- Patrol command/simulation now consistently tags spawned faction mobs as gunners in this branch flow.
- Faction omen duration is now 30 minutes (`36000` ticks) after clearing a patrol.
- Home-return raid trigger now consumes faction omen and starts raid near player respawn/home position.
- Removed the "no players in raid radius => monster auto-win" outcome; raids no longer end in monster victory for that condition.
- Patrol spawn position sampling was reworked to prefer valid surface positions near target players and avoid invalid/random unreachable placements.
- Path setup at spawn now uses tolerant fallback flow; path-init failure is recorded for debug instead of deleting already-spawned mobs.
- Same-faction gunners now treat each other as friendly targets and will not fight each other.
- Gunner shot spread now scales with in-game day progression toward a configurable late-game accuracy value.
- Gunner-fired shotguns now use tighter pellet spread by default.
- Gunner loadouts are now locked: gunner mobs are prevented from picking up loot and auto-recover a gun if their main hand is no longer a gun.
