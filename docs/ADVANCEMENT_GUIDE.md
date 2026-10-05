# Native combat guide

This module contains eight native advancement tabs, 445 guide nodes and 410 default mastery objectives. Minecraft saves, synchronizes and rewards these criteria. Parents organize the display; they do not gate successful actions.

## Completion

The tutorial leads from recipes and crafting to loading, aimed hits, gunners, attachments, repair and cooling. Mastery includes every default survival weapon, functional attachment, magazine, armour tier, special operation, faction, patrol/raid, End Armada and guardian follow-up battle, and the five available vehicles: Speedboat, BMP2, LAV150, AH-6 and MI-28.

Ground vehicles and boats require 20 blocks while the driver supplies movement input. Boats must be in water. Helicopters require controlled ascent of 10 blocks and a landing at no more than 0.4 blocks/tick downward with no hull damage. Boarding, pushing, teleporting and changing seats cannot replace driving. Hull and component repair are separate successful actions.

Weapon identity, aimed state, attachments and firing vehicle are captured by the projectile. Delayed explosives and fire preserve player ownership. Only server-confirmed enemy effects count. Failed requests, friendly targets, a player's own vehicles, creative and spectator actions cannot complete gameplay criteria.

Optional content never blocks mastery: all-colour collection, multiplayer vest detonation, extra faction raids, custom generic gunners, free-roaming Terror Phantom (disabled by default), Phantom SMG (no default survival acquisition), and legacy capacity attachments requiring magazineFeed to be disabled. The default magazine mode instead uses eleven physical magazine variants. The current Burst Rifle has automatic trigger behaviour; the guide describes the implementation that exists.

Possessions in inventory and worn equipment are re-evaluated on login. Past shooting, loading, driving or victory is not inferred. Existing native advancement IDs and saves remain intact. Acquisition gives no XP; operations give 5, stages 25, optional challenges 100 and final mastery 250. No equipment or materials are awarded.

English and Simplified Chinese text are generated together. Other languages use Minecraft's English fallback. If a server disables patrols, raids, structures or a weapon, restore that feature to pursue the corresponding default objective; changing configuration does not silently grant it.

## Regeneration and checks

Run from this module:

```text
python scripts/generate_advancements.py
python scripts/generate_advancements.py --check
./gradlew build checkVehicleGeometry compileClientJava
```

In the parallel workspace, `--all` generates/checks the six maintained siblings. The generator checks complete content coverage, action entry points, translation keys, valid recipes, parent cycles, criteria requirements, and exclusion of optional goals from mastery. The machine-readable content list and exclusions are in `advancement_coverage.json`.

The geometry regression check runs Java assertions for rotated body/turret/barrel boxes, real hit points/parts, gaps, separating axes, contact and high-speed rays. NeoForge 1.21.1 also contains native GameTests for successful/failed actions, persistence, player eligibility, weapon snapshots, attachment installation, timed explosives, fire and collision feedback. Execute `runGameTestServer` with a separate game directory.

## Vehicle parity

The reference is Superb Warfare 0.8.8 / Minecraft 1.21.1 from the workspace's external reference checkout. HUD selection uses current seat, weapon, camera and zoom. Rear-view reticles project the closest actual entity/block hit; front-view generic reticles follow SW visibility rules. Helicopters retain their separate pilot indicator.

Block movement uses the base entity AABB and vanilla movement; BMP2 width is 4.0. Dynamic part OBBs handle entity contact, picking, repair and projectile hits. Their union is only a candidate filter. Blast damage without a ray falls back to the hull instead of guessing a component from projectile position.

Contact feedback is independent of damage: SW strike audio is throttled to four ticks, horizontal rebound and 80% power reduction remain effective when armour or damage cooldown absorbs the hit. JEG damage balance is retained. Immediate authoritative correction does not add the dismount prediction pause to the active driver.
