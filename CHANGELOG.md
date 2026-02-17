# Changelog

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
