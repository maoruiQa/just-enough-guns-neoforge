# Four-chapter combat career

72 visible nodes in four native tabs; 53 core field objectives. Each visible node has at most three direct children. Minecraft owns criteria, persistence, network synchronization and rewards. Parents organize the display without preventing early completion.

## Chapter routes

- Ready for Action: recipes, crafting, magazine loading and reload, aimed hits and first gunner kill. Related branches cover ballistic protection, repair/cooling, magazine logistics, functional attachments and powered electronics. Personal styling, kill badges and enchanting are independent challenges.
- Front-line Experience: three entrances cover combat roles, the faction Raid campaign, and enemy recognition. Patrol -> Faction Omen -> return home -> win the Raid. Enemy recognition requires one zombie-family, one skeleton-family and one pillager/piglin-family armed Gunner; their variants are alternatives. Elite Hunter -> Before the Last Beep covers actual elite and C4 bomber roles. The bomber must die from credited player damage before its vest detonates. Phantom Gunner is an independent challenge. Enemy armour -> enemy aircraft covers BMP2/LAV150 and AH-6/MI-28, respectively, with either model accepted.
- Special Operations: three entrances cover controlled throwing weapons, C4/mines/defusing, and drone operations. Guidance requires valid Javelin direct/top-attack and Igla anti-air hits. Any one of the three drone payloads is sufficient. Multiplayer vest detonation is independent.
- Steel and Expeditions: workshop and Sky Ship are separate entrances. Vehicle missions retain BMP2 cannon/missile, LAV150 cannon, Speedboat machine gun, AH-6 machine gun/rocket and MI-28 rocket/guided missile. Shared crew, supply, charging, repair, countermeasure and recovery tasks count once. Sky Ship Armada -> fleet loot / Bound Terror Phantom -> follow-up Terror Raid leads to expedition completion. Free-roaming Terror Phantom is an independent challenge and is disabled by default.

This version has no armoured happy-ghast harness task.

Core mastery includes the three representative gunner/elite/bomber goals added in response to the enemy-content request. It excludes every independent challenge and every archived node. No per-weapon, per-armour-tier or all-colour checklist is required. The full node list, criteria and item alternatives/references/exclusions are in `advancement_coverage.json`.

## Successful actions and rewards

Only authoritative successful enemy effects count. Projectiles and delayed explosives/fire retain the fired weapon, attachments and player source when the player changes held items. Allied targets, own vehicles, failed requests and creative/spectator gameplay actions are excluded. Combinations may be completed across sessions; only the distant-headshot challenge requires one hit to satisfy both conditions.

Ground vehicles require 20 blocks of actual driver movement; Speedboat must be in water. Helicopters require a controlled climb of 10 blocks, then a landing no faster than 0.4 blocks/tick downward without hull damage. Boarding, pushing and teleporting cannot replace driving.

Root/acquisition nodes give zero XP; operations give 5, stages and chapter completion 25, independent challenges 100 and career completion 250. No equipment or materials are awarded. English and Simplified Chinese are complete; other languages use Minecraft's English fallback. Enemy bombers start from the configured progression day (default 50); enemy vehicles default to day 80. Disabling a required mechanic does not silently complete its goal; the description identifies configuration-dependent content.

## Existing native saves

`legacy_advancement_criteria.json` is the immutable original criterion snapshot. Retired IDs keep their original criterion names and requirements, with impossible triggers and no display/rewards. New gameplay cannot update them.

After native saved progress is read, before automatic triggers/listeners and initial synchronization, `AdvancementMigration` maps each proven old criterion to the corresponding new criterion. Partial AND progress is preserved. Old specific actions can prove broader representative actions; ownership cannot prove crafting, shooting, driving or a victory. Native progress objects receive the import directly, without XP, chat or toast rewards. Chapter and career closure is imported in order. Repeated reload/login is idempotent. Existing equipment can still be rechecked, but inventory never fabricates past actions.

## Regeneration and checks

Run from this module:

```text
python scripts/generate_advancements.py
python scripts/generate_advancements.py --check
./gradlew build
```

`--all` generates/checks the six maintained siblings. Checks cover four roots, exact visible/core counts, at most three children, acyclic bounded-depth parents, translations, valid recipes, native AND/OR requirements, gameplay action entry points, migration references, frozen compatibility records and independent goals excluded from mastery.

NeoForge 1.21.1 contains native GameTests for criteria matching, persistence, silent partial/closure migration, same-category alternatives, failed/friendly effects, source ownership, actual special-enemy deaths and retained vehicle geometry/feedback checks. Run `runGameTestServer` in an isolated game directory; see `VALIDATION.md` for recorded evidence and remaining manual coverage.

## Retained vehicle alignment

Existing Superb Warfare 0.8.8 / 1.21.1 HUD, strike feedback and dynamic part OBB changes remain intact. This redesign changes advancement resources and native-save migration, not vehicle damage balance or physics.
