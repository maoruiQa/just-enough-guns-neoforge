# Just Enough Guns New — Player and Server Wiki

**Reference language:** English · **Release:** `1.8.1`

[Wiki index](README.md) · [简体中文](zh-CN.md) · [日本語](ja-JP.md) · [Deutsch](de-DE.md) · [Español](es-ES.md)

## About this mod

Just Enough Guns New is an unofficial modern port of MigaMi's Forge 1.20.1 **Just Enough Guns**. It keeps the vanilla-friendly survival progression while adding firearms, magazines, attachments, hostile gunners, faction raids, Walkürenritt vehicles, special equipment and aerial threats.

The project is split into independent Fabric and NeoForge modules. A server and every connecting client must use the same Minecraft version, loader family and mod release.

## Quick start

1. Choose the row in the [compatibility table](#compatibility) that matches your Minecraft version and loader.
2. Install the required loader, Fabric API or NeoForge, and the GeckoLib version listed in that row.
3. Put the matching `jegn-1.8.1` JAR in the instance's `mods` folder. Do not combine Fabric and NeoForge JARs.
4. Start the game once, create or copy a test world, and confirm that Just Enough Guns appears in the mod list.
5. Keep the first test world separate from a long-running server until recipes, keybinds, server configuration and any addon dependencies are confirmed.

### First-session checklist

- Craft or locate the relevant workbench, ammunition and a compatible magazine.
- Load the magazine with the correct loose ammunition before inserting it into a magazine-fed weapon.
- Check the keybind screen for conflicts, then test shooting, aiming, reloading and inspection.
- Carry spare magazines, repair or cooling items, and enough ammunition for the weapon's firing mode.
- Begin with normal gunner encounters before enabling large raids, vehicles or Terror Phantom events on a new server.

## Compatibility

| Loader | Minecraft | Java | Mod | Required dependencies |
| --- | --- | --- | --- | --- |
| Fabric | 1.21.1 | 21 | 1.8.1 | Fabric API, GeckoLib 4.8.3 |
| NeoForge | 1.21.1–1.21.4 | 21 | 1.8.1 | NeoForge 21.1.x, GeckoLib 4.8.3 |
| Fabric | 26.2 | 25 | 1.8.1 | Fabric API, GeckoLib 5.5+ |
| NeoForge | 26.2 | 25 | 1.8.1 | NeoForge 26.2.x, GeckoLib 5.5.1 |
| Fabric | 26.3 | 25 | 1.8.1 | Fabric API, GeckoLib 5.5.7 |
| NeoForge | 26.3 | 25 | 1.8.1 | NeoForge 26.3.x, GeckoLib 5.5.7 |

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

## Server administration

Open the server configuration screen when the branch provides it, or edit the generated configuration after stopping the server. Test changes on a copy of the world first.

Configuration areas include:

- **UI:** ammo HUD, timer HUD, crosshair, dynamic crosshair and hit markers.
- **Gunners:** conversion chances, Parched conversion, Phantom Gunner death explosions and Terror Phantom behavior.
- **Raids:** faction patrols, raid timing, wave counts and gunner accuracy scaling.
- **Vehicles:** assembly, enemy vehicle spawning, combat behavior and vehicle support systems.

Recent branches also expose gunner-growth settings through the server config commands. The exact command names and available options can differ between loader and Minecraft version; use the command help for the installed branch rather than copying a command from another version.

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
- [1.8.1 release notes](../../CHANGELOG.md)
- [1.8.0 feature notes](../../CHANGELOG.md)
- [Advancement guide](../../docs/ADVANCEMENT_GUIDE.md)
- [Enemy vehicle AI notes](../../docs/vehicle_enemy_ai.md)
- [Validation notes](../../docs/VALIDATION.md)

When changing player-facing behavior, update this English page first and then synchronize the four translations. Keep branch-specific implementation details in the module that owns them.

## Release, credits and license

The public 1.8.1 release fixes dedicated-server startup paths and server config option handling across the maintained branches. The 1.8.0 feature release added FPV drones, C4, claymores, C4 vest, Javelin, Igla, smoke denial, missile-lock UI, kill-credit fixes and the vehicle/missile/rocket balance pass.

Just Enough Guns New is an unofficial port and is not affiliated with or endorsed by Just Enough Guns or Superb Warfare. Code based on Just Enough Guns is GPL-3.0. Original JEG assets are ARR and used with author authorization. SBW-derived vehicle and special-equipment materials retain their stated attribution and license requirements; see the root README for the complete credit list.

