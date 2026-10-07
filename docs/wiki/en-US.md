# Just Enough Guns New — Player and Server Wiki

**Reference language:** English · **Release:** `1.8.2`

[Wiki index](README.md) · [简体中文](zh-CN.md) · [日本語](ja-JP.md) · [Deutsch](de-DE.md) · [Español](es-ES.md)

## About this mod

Just Enough Guns New is an unofficial modern port of MigaMi's Forge 1.20.1 **Just Enough Guns**. It keeps the vanilla-friendly survival progression while adding firearms, magazines, attachments, hostile gunners, faction raids, Walkürenritt vehicles, special equipment and aerial threats.

The project is split into independent Fabric and NeoForge modules. A server and every connecting client must use the same Minecraft version, loader family and mod release.

## Quick start

1. Choose the row in the [compatibility table](#compatibility) that matches your Minecraft version and loader.
2. Install the required loader, Fabric API or NeoForge, and the GeckoLib version listed in that row.
3. Put the matching `jegn-1.8.2` JAR in the instance's `mods` folder. Do not combine Fabric and NeoForge JARs.
4. Start the game once, create or copy a test world, and confirm that Just Enough Guns appears in the mod list.
5. Keep the first test world separate from a long-running server until recipes, keybinds, server configuration and any addon dependencies are confirmed.

### First-session checklist

- Craft or locate the relevant workbench, ammunition and a compatible magazine.
- Load the magazine with the correct loose ammunition before inserting it into a magazine-fed weapon.
- Check the keybind screen for conflicts, then test shooting, aiming, reloading and inspection.
- Carry spare magazines, repair or cooling items, and enough ammunition for the weapon's firing mode.
- Begin with normal gunner encounters before enabling large raids, vehicles or Terror Phantom events on a new server.

### Follow the in-game career guide

Version 1.8.2 adds a native advancement guide with four chapters: **Ready for Action**, **Front-line Experience**, **Special Operations**, and **Steel and Expeditions**. Open the Advancements screen and follow the first visible objective instead of guessing which workbench or weapon the progression expects. The guide covers representative guns and vehicles, so equivalent supported equipment can satisfy many objectives.

Only successful server-confirmed actions count. A failed shot, a friendly target, a Creative/Spectator action, or simply owning an item does not complete a gameplay objective. Vehicle objectives require actual movement; helicopter objectives require controlled ascent and landing. The guide is a learning route, not a checklist requiring every weapon.

## Compatibility

| Loader | Minecraft | Java | Mod | Required dependencies |
| --- | --- | --- | --- | --- |
| Fabric | 1.21.1 | 21 | 1.8.2 | Fabric API, GeckoLib 4.8.3 |
| NeoForge | 1.21.1–1.21.4 | 21 | 1.8.2 | NeoForge 21.1.x, GeckoLib 4.8.3 |
| Fabric | 26.2 | 25 | 1.8.2 | Fabric API, GeckoLib 5.5+ |
| NeoForge | 26.2 | 25 | 1.8.2 | NeoForge 26.2.x, GeckoLib 5.5.1 |
| Fabric | 26.3 | 25 | 1.8.2 | Fabric API, GeckoLib 5.5.7 |
| NeoForge | 26.3 | 25 | 1.8.2 | NeoForge 26.3.x, GeckoLib 5.5.7 |

Fabric 26.1 and NeoForge 26.1 are legacy lines. Use 26.2 for the maintained Java 25 line unless a pack specifically requires 26.1.

## Controls

### Guns

| Action | Default input |
| --- | --- |
| Shoot | Left Click |
| Aim down sights | Right Click |
| Reload | `R` |
| Inspect an animated gun | `Y` |
| Gun melee or flashlight toggle, where supported | `V` |
| Sneak behavior, where supported | `Shift` |

These are defaults for version 1.3.0 and later. Older releases used right-click shooting, `F` reload and `Shift` aiming. Recheck the keybind screen after an upgrade.

### Vehicles

| Action | Default input |
| --- | --- |
| Enter or interact | Right Click |
| Steer and throttle | `W` / `A` / `S` / `D` |
| Brake or reverse | `S` |
| Look or aim a mounted weapon | Mouse movement |
| Fire the active weapon | Left Click |
| Aim, lock, zoom or use a secondary function | Right Click |
| Reload, where supported | `R` |
| Dismount | `Shift` |

Seats define which actions are available. Driver seats move the vehicle; weapon or co-pilot seats may control turrets, missiles, target locks or countermeasures.

## Gameplay guide

### Weapons and ammunition

The weapon roster covers pistols, revolvers, rifles, SMGs, shotguns, machine guns, launchers, bows, flamethrower-style weapons and late-game special guns. Most weapons require a matching ammo or magazine type.

Magazine-fed weapons use loaded magazines. Manual and single-item weapons use their matching ammunition directly. Extended and drum magazines exist for supported rifle, SMG and shotgun reloads. Attachments, stocks, grips, sights, skins, badges and special ammunition are part of the normal survival progression.

Recoil, movement spread, overheating, dynamic crosshair expansion, hit markers, muzzle effects and bullet trails communicate the weapon state. If a weapon refuses to fire, check its ammunition, magazine, heat, reload state and server configuration before treating it as a crash.

On first join, the server may show an **Ammunition Rules** popup. **Magazine feed** means you load compatible ammunition into physical magazines and reload supported guns by swapping those magazines; extended and drum magazine recipes are available. **Direct feed** means supported guns reload from loose ammunition and the legacy capacity attachments are available instead. Click **Got it** or press `Esc`; the confirmation is saved per player. When the server changes mode, the popup appears again and the server refreshes recipes without deleting existing items.

### Ballistic protection

Bulletproof helmets and vests intercept gun damage through an armor-piercing comparison rather than vanilla Projectile Protection. The shot's effective AP comes from the ammunition and the gun multiplier. A helmet is used for a protected head hit; other protected body hits use the vest. Without the matching piece, gun damage is unchanged.

Under-matching shots still deal partial damage and wear the armor down. Over-matching shots deal more damage and apply heavier durability pressure. The exact values are branch data and may change with balance updates; use the in-game tooltip and the current branch sources as the authority for a specific build.

### Gunners, factions and raids

Gunner variants can appear across zombie, skeleton, piglin, pillager/vindicator, phantom, ghoul and parched families. Faction encounters build from patrols and faction omen events into home-triggered raids, raid flares, boss bars and configurable waves. Some variants, including the C4 vest bomber, are controlled by server configuration.

### Vehicles

Walkürenritt content includes assembled land vehicles, boats, aircraft, helicopters and fixed weapon platforms. Vehicles have inventories, seats, repair tools, charging or energy support, missiles, decoys and vehicle-specific HUD feedback.

Build the vehicle through its assembly progression, deploy it in the world, then keep ammunition, repair tools and charged support items in the inventory expected by that vehicle. Watch the HUD for weapon readiness, reload timing, missile-lock warnings, damage and countermeasure state.

Enemy vehicle AI can patrol, pursue, reverse, avoid unsuitable terrain and control turrets according to the vehicle type. The maintained branches share the same player-facing vehicle behavior while their loader code remains separate.

### Special equipment

- **FPV drones:** use the monitor to control the camera and payload; explosive payloads can enter a guided kamikaze dive.
- **C4 and claymores:** place charges, use the correct detonator or trigger, and carry a C4 defuser when clearing hostile charges.
- **C4 vest:** a configurable bomber-gunner variant can carry an explosive vest.
- **Javelin and Igla 9K38:** guided launchers use lock-on behavior where a target is in range and line of sight; smoke can deny a missile lock.
- **Vehicle missile lock UI:** in-range lockable targets receive seek frames and audio feedback.

### Terror Phantom

Terror Phantom content is a rare aerial threat with Bound Terror Phantom, phantom gunner summons, configurable death explosions and End Ship Armada encounters. Natural Terror Phantom spawning is soft-disabled by default in the 1.8.0 feature line; server owners can enable or tune the behavior in configuration.

## Too hard or too easy?

Change one group at a time, test for a few in-game days, and keep a copy of the world before changing progression. The examples below use the Fabric 26.2 command names documented by this branch.

| What you feel as a player | First adjustment | Why it works |
| --- | --- | --- |
| Gunners are too strong near the start | `/justEnoughGuns config combat naturalGunnerDynamicDifficultyEnabled false` | Stops natural gunners from matching the strongest nearby Survival player while time-based growth still works. |
| Patrols interrupt every trip home | `/justEnoughGuns config patrol enabled false` or `/justEnoughGuns config patrol minimumDays 15` | Disables natural patrols or delays their earliest day. |
| Patrols are fine but happen too often | `/justEnoughGuns config patrol intervalDays 10` and `/justEnoughGuns config patrol spawnChance 0.15` | Uses a longer fixed interval and a lower final spawn roll. |
| The home raid is too punishing | Set `factionRaid.dynamicDifficultyEnabled` to `false` in the server UI or `config/jeg-server.toml` | Keeps raid difficulty from scaling with nearby equipment, world age and party size. |
| The player cannot hit anything from the hip | `/justEnoughGuns config combat hipFireSpreadMultiplier 1.0` | The default is `1.5`; lower values tighten hip-fire spread. Aiming still gives the intended accuracy advantage. |
| Magazine logistics are too demanding | `/justEnoughGuns config combat magazineFeed false` | Switches supported guns to direct-feed reloads from loose ammunition. The server shows an ammunition-rules popup and refreshes recipes. |
| You want the magazine gameplay to matter more | `/justEnoughGuns config combat magazineFeed true` | Requires physical compatible magazines and enables extended/drum magazine recipes for supported weapons. |
| Rockets or C4 arrive too early | In the server UI, select **Mobs**, choose **All Gunners**, then raise `Rocket Launcher Start Day` or `Bomber Gunner Start Day`. | These settings delay special threats without removing ordinary gunners. |
| Enemy vehicles overwhelm a new base | `/justEnoughGuns config vehicle enemySpawning enabled false` | Stops natural enemy vehicle conversion and raid vehicle reinforcements while leaving player vehicles enabled. |
| You want a vehicle-focused late game | Edit `vehicle.enemyVehicleStartDay`, `vehicle.enemyVehicleConversionChance` and `vehicle.enemyVehicleMaxConversionChance` in `config/jeg-server.toml`. | Starts enemy vehicles earlier or raises their conversion cap without disabling the vehicle system. |
| Explosions are visually exhausting | Set `rendering.explosionScreenShake = 0` in `config/jeg-client.toml`. | This is client-only camera feedback; it does not change damage. |

### What not to change first

Do not lower every gunner chance to zero when only raids feel hard; patrol, faction raid, natural gunner growth and enemy vehicles are separate systems. Do not set every growth value to `0` without understanding inheritance: `-1` means “inherit the balanced default or the **All Gunners** override.”

## In-game server configuration UI

The 26.2 Fabric client adds a **JEGN Configuration** button to the pause menu. The server must be running the same JEG version, and the player needs operator permission level 2. In a normal single-player world, cheats/commands permission grants this access; on a dedicated server use `/op <player>` or the server's permission manager.

1. Press `Esc` to open the pause menu.
2. Click **JEGN Configuration** near the top of the menu.
3. Choose a category: **Interface**, **Patrols**, **Mobs**, **Combat** or **Vehicles**.
4. Toggle booleans or type a number into the field. In **Mobs**, use the left/right buttons to select a gunner type before editing growth values.
5. Hover a label to see its command key, allowed range and the `-1` inheritance rule.
6. Click **Apply**. The server validates the values, saves `config/jeg-server.toml`, refreshes magazine rules and broadcasts UI changes where needed.
7. Use **Reset** to restore only the selected category. **Done** returns to the pause menu; if a value is unsaved, the screen asks whether to discard it.

The UI exposes the live-editable subset:

- **Interface:** `ui.showCrosshair`, `ui.showHitFeedback`, and `ui.hideMedals` where present.
- **Patrols:** `patrol.enabled`, `patrol.intervalDays` (`0–30`), `patrol.minimumDays` (`0–100`) and `patrol.spawnChance` (`0.0–1.0`).
- **Mobs:** Phantom Gunner death explosion, natural-gunner dynamic difficulty, and 15 gunner growth profiles: `all`, `skeleton`, `stray`, `zombie`, `husk`, `parched`, `drowned`, `zombieVillager`, `zombifiedPiglin`, `piglin`, `piglinBrute`, `witherSkeleton`, `pillager`, `vindicator`, `generic`.
- **Combat:** bullet block damage, magazine feed, headshot multiplier, hip-fire spread (`0.0–5.0`), gunner terrain support and maximum terrain-break tier (`0–3`), plus dynamic faction-raid difficulty.
- **Vehicles:** vehicle system enabled, adaptive enemy combat, enemy vehicle spawning, start day (`0–5000`) and conversion probabilities (`0.0–1.0`).

If the button is missing or returns to the pause menu, check the permission level and the server log. The request is server-authoritative; a client cannot change the server by editing its own client file.

## Command reference

All commands start with `/justEnoughGuns`. Press `Tab` after each word to see suggestions from the installed branch. Reading a config command without the final value prints the current value; adding a value changes it. Setting values requires permission level 2.

### Player and test-world commands

```text
/justEnoughGuns unlockGunRecipes
/justEnoughGuns spawnPatrol <faction> <size> <pos> [forceGuns] [spawnRadius]
/justEnoughGuns simulatePatrol <faction> <size> <player> [forceGuns]
```

`unlockGunRecipes` is player-only and awards every JEG gun recipe, so use it for a test world or an admin decision rather than normal progression. The six built-in faction names are `night_of_the_undead`, `the_rattlers`, `nosy_business`, `bad_piggies`, `hell_hogs` and `lost_souls`. Patrol size is `1–20`; spawn radius is `0–16` and defaults to `10` when omitted. Example:

```text
/justEnoughGuns spawnPatrol night_of_the_undead 6 ~ ~ ~ true 10
/justEnoughGuns simulatePatrol the_rattlers 4 @s true
```

Patrol commands fail in Peaceful and require gamemaster permission. `simulatePatrol` is useful for checking a faction loadout without waiting for a natural encounter.

### Configuration command tree

```text
/justEnoughGuns config ui crosshair [true|false]
/justEnoughGuns config ui hitFeedback [true|false]

/justEnoughGuns config patrol enabled [true|false]
/justEnoughGuns config patrol intervalDays [0-30]
/justEnoughGuns config patrol minimumDays [0-100]
/justEnoughGuns config patrol spawnChance [0.0-1.0]

/justEnoughGuns config mob mechanism terror chance [0.0-1.0]
/justEnoughGuns config mob mechanism terror max [0.0-1.0]
/justEnoughGuns config mob mechanism phantom deathExplosion [true|false]
/justEnoughGuns config mob spawn <type> <setting> [value]

/justEnoughGuns config combat naturalGunnerDynamicDifficultyEnabled [true|false]
/justEnoughGuns config combat blockDamage [true|false]
/justEnoughGuns config combat magazineFeed [true|false]
/justEnoughGuns config combat headshotMultiplier [true|false]
/justEnoughGuns config combat hipFireSpreadMultiplier [0.0-5.0]
/justEnoughGuns config combat gunnerTerrainPlacement enabled [true|false]
/justEnoughGuns config combat gunnerTerrainPlacement block <block_id>
/justEnoughGuns config combat gunnerTerrainBreak maxTier [0-3]

/justEnoughGuns config factionRaid dynamicDifficultyEnabled [true|false]

/justEnoughGuns config vehicle enabled [true|false]
/justEnoughGuns config vehicle enemyVehicleAdaptiveCombatEnabled [true|false]
/justEnoughGuns config vehicle enemySpawning enabled [true|false]
/justEnoughGuns config vehicle enemySpawning startDay [0-5000]
/justEnoughGuns config vehicle enemySpawning conversionChance [0.0-1.0]
/justEnoughGuns config vehicle enemySpawning maxConversionChance [0.0-1.0]
/justEnoughGuns config vehicle enemySpawning conversionChancePerDay [0.0-1.0]
```

For example, these commands create a gentler early game without removing weapons:

```text
/justEnoughGuns config patrol minimumDays 15
/justEnoughGuns config patrol spawnChance 0.15
/justEnoughGuns config combat hipFireSpreadMultiplier 1.0
/justEnoughGuns config vehicle enemySpawning startDay 120
```

The gunner-growth command accepts these `type` values: `all`, `skeleton`, `stray`, `zombie`, `husk`, `parched`, `drowned`, `zombieVillager`, `zombifiedPiglin`, `piglin`, `piglinBrute`, `witherSkeleton`, `pillager`, `vindicator`, `generic`. Its `setting` values are:

| Setting | Meaning |
| --- | --- |
| `minSpawnChance`, `maxSpawnChance`, `spawnChancePerDay` | Gunner conversion probability floor, cap and daily growth. |
| `weaponInitialTier`, `weaponMaxTier`, `weaponTierPerDay` | Weapon tier at the start, tier cap and daily growth. |
| `armorInitialTier`, `armorMaxTier`, `armorTierPerDay` | Armor tier at the start, tier cap and daily growth. |
| `rocketLauncherStartDay`, `rocketLauncherChance`, `rocketLauncherMaxChance`, `rocketLauncherChancePerDay` | Rocket-launcher timing and probability. |
| `bomberStartDay`, `bomberChance`, `bomberMaxChance`, `bomberChancePerDay` | C4 bomber timing and probability. |
| `weaponAggression` | How readily the profile chooses more aggressive weapon roles. |

Example: delay bombers for every gunner type by changing the shared profile:

```text
/justEnoughGuns config mob spawn all bomberStartDay 100
/justEnoughGuns config mob spawn all bomberChance 0.03
/justEnoughGuns config mob spawn all bomberMaxChance 0.06
```

## Configuration files

Use commands or the server UI for live server changes. For direct editing, stop the relevant instance first; otherwise the running server can overwrite the file.

### Client file: `config/jeg-client.toml`

These settings affect only the local player's presentation:

| Key | Default | Player-facing effect |
| --- | --- | --- |
| `rendering.showAmmoHud` | `true` | Shows the weapon ammo panel. |
| `rendering.showTimersHud` | `true` | Shows overheat, cooling and other timer bars. |
| `rendering.crosshair` | `"jeg:dynamic"` | Uses `default`, `jeg:dynamic`, `jeg:tech` or a custom crosshair texture ID. |
| `rendering.showHitmarker` | `true` | Shows a short marker on living-entity hits. |
| `rendering.dynamicCrosshairDotMode` | `"at_min_spread"` | `never`, `at_min_spread`, `threshold` or `always`. |
| `rendering.dynamicCrosshairDotThreshold` | `0.8` | Spread threshold used by `threshold` mode. |
| `rendering.explosionScreenShake` | `100` | Camera shake strength from `0` to `100`; `0` disables it. |
| `rendering.legacyBulletTrailEnabled` | `true` | Uses the legacy bullet-trail renderer when enabled. |
| `rendering.hideAttachmentConfigButton` | `false` | Hides the attachment-screen configuration button. |
| `rendering.attachmentButtonAlignment` | `"right"` | `left` or `right`. |

### Server file: `config/jeg-server.toml`

The main groups are `ui`, `attachments`, `spawns`, `gunnerGrowth`, `combat`, `terrorRaid`, `factionPatrol`, `factionRaid` and `vehicle`. Useful player-facing keys include:

- `combat.magazineFeed`: chooses physical-magazine or direct-feed reload rules.
- `combat.hipFireSpreadMultiplier`: default `1.5`, range `0.0–5.0`.
- `combat.naturalGunnerDynamicDifficultyEnabled`: lets natural gunners adapt to nearby Survival equipment.
- `combat.gunnerTerrainPlacementEnabled`, `combat.gunnerTerrainSupportBlock`, `combat.gunnerTerrainBreakMaxTier`: whether gunners place support blocks and what terrain tier they can break.
- `factionPatrol.enabled`, `factionPatrol.intervalDays`, `factionPatrol.minimumDays`, `factionPatrol.spawnChance`: patrol frequency and earliest day.
- `factionRaid.enabled`, `factionRaid.minimumDays`, `factionRaid.homeTriggerRadius`, `factionRaid.dynamicDifficultyEnabled`: raid availability and scaling.
- `vehicle.enemyVehicleSpawningEnabled`, `vehicle.enemyVehicleStartDay`, `vehicle.enemyVehicleConversionChance`, `vehicle.enemyVehicleMaxConversionChance`: enemy vehicle pressure.

When `combat.magazineFeed` changes, online players receive the ammunition-rules popup immediately; `/reload`, startup and configuration changes also refresh filtered recipes. Existing items are not deleted or converted.

## Server administration

Use the UI for safe live edits, commands for quick experiments, and the TOML file for settings that are not exposed in the live editor. Test changes on a copy of the world first.

Configuration areas include:

- **Interface:** server-controlled crosshair, hit feedback and medals; local HUD visibility remains in `jeg-client.toml`.
- **Gunners:** base conversion probabilities, day-based growth, armor/weapon tiers, rocket launchers, bombers, Phantom Gunner explosions and dynamic difficulty.
- **Patrols and raids:** earliest days, intervals, spawn chances, home radius, wave behavior and dynamic difficulty.
- **Combat:** magazine mode, headshot multiplier, hip-fire spread, terrain support, bullet block damage and progression timing.
- **Vehicles:** player vehicle system, enemy spawning, adaptive combat and conversion timing.

The server configuration screen applies validated values live and saves them. A value rejected by the server is shown in red; fix the range or type before pressing **Apply** again.

## Troubleshooting

### The game refuses to load

Confirm the Minecraft version, loader family, Java version and GeckoLib version against the compatibility table. Remove duplicate or cross-loader JARs, then test with only the required dependencies and Just Enough Guns.

### A dedicated server crashes during startup

Make sure the JAR matches the server loader and Minecraft version. Do not place client-only addons or a Fabric JAR in a NeoForge server, or the reverse. Reproduce on a clean test instance and keep the first crash log from the failed launch.

### A gun, recipe or vehicle is missing

Check the recipe book, item search and server log after a clean restart. Confirm that the server and client use the same mod file and that the dependency version is not from another Minecraft line. A missing recipe can be a branch-specific data issue; report it with the exact item ID and log line.

### Controls or HUD do not appear

Search the keybind screen for conflicts, restore the default binding, and test with dynamic crosshair, hit markers and ammo HUD enabled. Vehicle HUD elements can also depend on the seat and the active weapon.

### A missile will not lock

Check range, line of sight, target type, ammunition and launcher state. Smoke screens deliberately deny missile locks. The lock frame should only appear for an eligible target that is in range and visible.

## Bug reports

Use the repository issue tracker and include:

1. Minecraft version and loader (Fabric or NeoForge).
2. Just Enough Guns version and the exact JAR filename.
3. Java version and Fabric API, NeoForge and GeckoLib versions.
4. A short reproduction sequence, including the world state and relevant configuration.
5. The complete crash report or latest log, plus a screenshot or recording when the problem is visual or audio.
6. Whether the issue reproduces on a clean instance without unrelated addons.

Do not paste only “it crashes” or a screenshot of the launcher. The exact version matrix and the first meaningful stack trace usually determine whether the problem belongs to the loader, a dependency, the mod or another addon.

## Developer and maintainer links

- [Root README](../../README.md) and [description copy](../../description.md)
- [1.8.2 release notes](../../CHANGELOG.md)
- [1.8.1 and 1.8.0 history](../../CHANGELOG.md)
- [Advancement guide](../../docs/ADVANCEMENT_GUIDE.md)
- [Enemy vehicle AI notes](../../docs/vehicle_enemy_ai.md)
- [Validation notes](../../docs/VALIDATION.md)

When changing player-facing behavior, update this English page first and then synchronize the four translations. Keep branch-specific implementation details in the module that owns them.

## Release, credits and license

The public 1.8.2 release adds the four-chapter advancement guide, server ammunition-rules popup, live configuration feedback, SW-aligned vehicle behavior, revised ADS/hip-fire handling and combat/configuration fixes. The 1.8.1 release fixed dedicated-server startup paths and server config option handling; 1.8.0 added FPV drones, C4, claymores, C4 vest, Javelin, Igla, smoke denial, missile-lock UI, kill-credit fixes and the vehicle/missile/rocket balance pass.

Just Enough Guns New is an unofficial port and is not affiliated with or endorsed by Just Enough Guns or Superb Warfare. Code based on Just Enough Guns is GPL-3.0. Original JEG assets are ARR and used with author authorization. SBW-derived vehicle and special-equipment materials retain their stated attribution and license requirements; see the root README for the complete credit list.
