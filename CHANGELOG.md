# Changelog

## 1.8.0 - 2026-08-04

### Added
- Ported SuperbWarfare-style special equipment into JEG: FPV drones with monitor control, C4 bombs (including remote/detonator variants), claymore mines, and a C4 vest wearable charge.
- Added the C4 Defuser tool for safely removing placed C4 charges, with durability-based uses.
- Added guided launchers **Javelin** and **Igla 9K38**, including lock-on fire, SW-aligned first-person poses/ADS, inventory icons, walk/sprint root motion, and full reload animation support.
- Added C4 drone FPV payload HUD with detonate guidance and a **KAMIKAZE** dive presentation for explosive drone runs.
- Added a C4 vest bomber gunner variant with configurable spawn rates so suicide-bomber pressure can be tuned per server.
- Added SW-style smoke screens that deny missile locks while active, including denser custom smoke particles and dedicated release audio.
- Added SW-style vehicle missile lock frames and seek audio so lockable targets get clear on-screen seek boxes and feedback.
- Soft-disabled natural Terror Phantom spawns by default to reduce unsolicited aerial boss pressure in normal worlds.

### Changed
- Set the module version to `1.8.0`.
- Rebalanced guided missiles: retuned tracking/damage feel and allow unguided Javelin dumps when lock is not available.
- Improved anti-vehicle rocket performance against armored targets.
- Strengthened drone descent power cut and raised max drone descent speed by about 1.33x for snappier FPV kamikaze dives.
- Aligned drone camera pitch with SuperbWarfare FPV framing.
- Only draw lock boxes for missile/drone targets that are in range and have line of sight.
- Hide the vanilla armor bar while riding a vehicle so vehicle HUDs stay readable.
- Improved vehicle state sync with broader tracking coverage and a 256-block fallback for distant vehicles.

### Fixed
- Fixed drone FPV rubber-banding and missing pilot render during monitor control.
- Fixed missile/drone seek frames missing when targets were due north.
- Fixed helicopter rotor spin-down and engine sound continuing after dismount.
- Fixed guided launcher fire/lock handling, ADS raise, hand orientation, and inventory GUI presentation (including flat Igla icons that fill the item slot).
- Fixed special equipment audio, monitor link behavior, and payload HUD tip text; repaired related language JSON.
- Fixed vehicle missile and special-equipment presentation issues carried over from the special equipment port.

### Verification
- Passed `.\gradlew build`.

## 1.7.5 - 2026-07-22

### Added
- Added category-level configuration reset controls that restore all settings in the selected category to their defaults before applying.

### Changed
- Fixed gunner spawn chance scaling so a configured `maxSpawnChance` below `minSpawnChance` is respected.
- Corrected configuration-menu ranges for gunner probabilities, weapon tiers, armor tiers, rocket launcher timing, and weapon aggression.

## 1.7.4 - 2026-07-12

### Changed
- Set the module version to `1.7.4`.
- Made login intro messages configurable.
- Updated bulletproof helmet IV-VI textures and normalized the IV/V armor-layer UV framing.
- Scaled elite gunner chance by in-game day so advanced gunner pressure ramps later instead of appearing at a flat rate.
- Tuned gunner tactics and smoke concealment.
- Scaled late-game rocket launcher and vehicle spawn chances by in-game day, reaching maximum rocket chance by day 150 and the vehicle conversion cap by day 180.

### Added
- Added the Magazine Loader block for filling compatible magazines from loose ammo.
- Added a gun headshot damage multiplier path for gun damage resolution.
- Added the in-game JEGN server configuration UI.
- Added dense campfire smoke to smoke grenades.

### Fixed
- Fixed gunners and vehicles targeting invisible players.
- Fixed bulletproof vest hand UV texture rendering.
- Removed the enchantment glint from bulletproof armor.
- Restored bulletproof vest shoulder UV textures so shoulder armor renders in-game.
- Fixed machine gun belt visibility in third person.
- Fixed third-person rocket launcher missile visibility.
- Fixed light machine gun reload interruption.
- Fixed duplicate config screen background rendering.
- Fixed blurred server config labels.
- Guarded unregistered attack target memory.
- Fixed gunner spawn egg item colors so each gunner spawn egg better matches its vanilla mob family.
- Fixed animated gun draw lock handling so held actions are released consistently after the authored draw timing.

### Verification
- Passed `.\gradlew build`.

## 1.7.2 - 2026-06-24

### Changed
- Set the module version to `1.7.2`.

### Fixed
- Reworked ADS alignment for reflex, monocle, and holographic sights so their open-sight reticles use the Forge 1.20.1-derived scope camera position and each gun's scope mount transform.
- Kept telescopic sight and spyglass ADS corrections on their existing per-rifle path.
- Matched gun draw operation locks to each weapon's authored draw animation length, unlocking shoot, reload, inspect, melee, attachments, and ADS after 85% of the draw animation instead of a fixed delay.
- Preserved held fire and held action-key input through the draw unlock point while preventing released keys from firing late.

### Verification
- Passed `.\gradlew build`.

## 1.7.1 - 2026-06-17

### Added
- Restored the `jeg:repair_kit` recipe from the disabled recipe file while keeping the existing item ID.
- Added a repair kit tooltip explaining anvil repair support for guns and bulletproof armor.
- Added first-person held-gun left/right movement camera sway, separate from recoil handling.
- Added Y-key gun inspect animations for held animated guns, including tooltip guidance.

### Changed
- Increased Terror Phantom and drivable vehicle entity tracking range to 14 chunks so they can stay synced and visible out to roughly 224 blocks.
- Set the module version to `1.7.1`.
- Renamed the visible repair kit item to `Gun Repair Kit` / `枪械维修包` without changing the `jeg:repair_kit` registry ID.
- Changed gun anvil repair material from iron ingots to `jeg:repair_kit`.
- Made `jeg:repair_kit` the bulletproof armor anvil repair material.
- Converted targeted player-visible hardcoded strings to translation keys in vehicle assembly UI, attachment UI, gun overheating tooltip/message, Terror Raid bossbar, and command feedback.
- Reduced rocket and missile block damage and splash reach when directly hitting vehicles, while keeping direct vehicle hit damage intact.
- Increased aircraft no-input descent acceleration so powerless aircraft descend at roughly three times the previous terminal speed.
- Updated vehicle missile profiles, increasing ground-attack missile turn limits and retuning the 9M336 air-target missile turn limit.
- Increased helicopter maximum descent speed to `1.05D/tick` and made unmanned, unpowered, and critically damaged helicopters descend smoothly toward that speed.
- Slowed unmanned helicopter forced descent so falling from the old safe descent speed to max descent takes about 20 seconds.
- Changed unmanned, unpowered, and low-health helicopter forced descent back to a linear curve with 70% of the previous fast transition as the base step, and made low-health forced descent start at 20 health.

### Fixed
- Fixed Fabric 1.21.1 gun reload input so custom reload key bindings work instead of only the default R key.
- Fixed Flamethrower reload timing so gameplay reload completion matches the authored 10.25 second first-person reload animation.
- Fixed first-person gun draw startup during hotbar switches so pending draw playback becomes authoritative immediately after switch-in.
- Fixed Grenade Launcher reload animation so it uses the authored single reload clip instead of missing segmented reload loop/stop clips.
- Fixed repair kit behavior so it no longer repairs vehicles on right-click; repair-tool vehicle repair remains intact.
- Fixed helicopter and aircraft impact damage so controlled slow landings no longer explode from accumulated height alone, while uncontrolled high falls still deal crash damage.
- Restored helicopter rotor block-contact damage while airborne.
- Kept tracked backup copies of `GunItem.java` consistent with the repair-kit material change.
- Kept inspect playback from being interrupted by melee, draw, and normal sprint poses; shooting, reloading, and bayonet sprinting can interrupt it and replay starts from the beginning.
- Changed helicopter vertical crash damage to scale only with descent speed, keeping `0.35D/tick` landings safe and capping full-speed vertical impact damage near 70% of max health.
- Kept helicopter rotor motion and engine sound active while helicopters are descending in unmanned, unpowered, or low-health states.
- Let helicopter rotor speed wind down gradually after players leave, matching normal landed shutdown behavior.
- Restored fatal high-speed helicopter impact damage for block and vehicle collisions while leaving non-vehicle entity collisions unchanged.
- Played the missile warning alert for helicopter passengers during unsafe descent.
- Played the helicopter warning alert during forced descent even while the current descent speed is still within the safe-speed range.
- Prevented helicopter lift from holding forced-descent states above their target descent speed.
- Made helicopter forced descent accumulate from the tick-start vertical speed so air drag cannot cap the fall around a low terminal speed.
- Prevented active vehicle engines from continuing to move at very low energy without draining the remaining energy.
- Made missile and helicopter danger warnings flash red, increased helicopter warning volume, and added helicopter HUD warnings for low speed, power loss, and critical damage.
- Moved vehicle warning prompts to a flashing crosshair HUD message instead of the old passenger actionbar position.
- Stopped recovered helicopters from continuing low-health self-destruct decay after being repaired above the critical threshold.
- Prevented no-energy helicopters from spinning rotors or playing engine sound from pilot input alone.
- Prevented stopped helicopters from briefly twitching their rotors when the pilot exits.
- Smoothed fixed-wing no-pilot, no-energy, and critical-damage forced descent from the current vertical speed instead of snapping to maximum descent.
- Lowered the helicopter low-health self-destruct threshold to 20 health.
- Kept stopped helicopter rotors at zero when the pilot exits instead of spinning up briefly before winding down.
- Kept helicopter low-speed warnings from switching into the missile incoming warning while forced descent continues.

### Localization
- Expanded `zh_cn.json` to cover every key present in this branch's `en_us.json`.

### Verification
- Passed `.\gradlew compileJava compileClientJava clientClasses`.
- Passed `.\gradlew compileJava compileClientJava` after the vehicle direct-hit explosion tuning.
- Passed `.\gradlew compileJava compileClientJava` after the air vehicle impact/descent tuning.
- Passed `.\gradlew compileJava compileClientJava` after the gun animation fixes.
- Confirmed `repair_kit.json` parses and outputs `jeg:repair_kit`.
- Confirmed `zh_cn.json` has no missing keys relative to `en_us.json`.
- Full `build` and in-game gameplay checks were not run.
- Passed `.\gradlew compileJava compileClientJava`.
- Passed `.\gradlew compileJava compileClientJava` after the helicopter descent, crash damage, and feedback tuning.
- Passed `.\gradlew compileJava compileClientJava` after the helicopter rotor, descent, fatal impact, and warning updates.
- Passed `.\gradlew compileJava compileClientJava` after the exponential helicopter forced-descent curve update.
- Passed `.\gradlew compileJava compileClientJava` after increasing the helicopter forced-descent base step to 120%.
- Passed `.\gradlew compileJava compileClientJava` after restoring linear forced descent and widening forced-descent warnings.
- Passed `.\gradlew compileJava compileClientJava` after preventing forced-descent lift from limiting descent speed.
- Passed `.\gradlew compileJava compileClientJava` after making forced descent accumulate before air drag.
- Passed `.\gradlew compileJava compileClientJava` after the low-energy engine and helicopter warning updates.
- Passed `.\gradlew compileJava compileClientJava` after crosshair warnings, repair recovery, no-energy rotor gating, rotor-exit stability, and fixed-wing forced-descent smoothing.
- Passed `.\gradlew compileJava compileClientJava` after the stopped-rotor exit, low-speed warning, and 20-health self-destruct threshold fixes.

## 1.7.0-hotfix150620261622 - 2026-06-15

### Fixed
- Fixed recipe loading so joining players receive the full active `jeg` recipe set.
- Fixed recipe distribution on player login so loaded recipes are consistently assigned instead of dropping entries on Fabric 1.21.1.

## 1.7.0-hotfix140620261037 - 2026-06-14

### Fixed
- Fixed a dedicated Fabric server crash when reload action-bar feedback tried to inspect client-only player methods and loaded `net.minecraft.client.resources.sounds.SoundInstance`.
- Kept reload gameplay, animation state, and client rendering behavior unchanged; only the server-safe action-bar feedback path was changed.

## 1.7.0 - 2026-06-12

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
- Fixed sword bayonet melee so V-key hits use reliable sword damage, target-center arc checks, and the proper barrel attachment transform.
- Fixed bayonet first-person animation priority so switching to a bayonet-equipped gun plays the draw animation and sprinting with a bayonet uses the bayonet charge animation instead of the normal run pose.
- Restored Forge 1.20.1 explosive-muzzle entity-hit ignition while reducing explosive-muzzle armor piercing by 25% for balance.
- Fixed draw, reload, shoot, sprint, idle, and melee animation priority so first-person GeckoLib controllers recover cleanly after reload cancellation, hotbar switches, and local gunfire.
- Fixed grenade launcher grenades so launcher-fired grenades explode promptly instead of using an excessive fuse.
- Fixed telescope-sight ADS mouse sensitivity so scoped bolt-action aiming slows the mouse further.
- Fixed the attachment screen mod label for built-in JEGN guns.
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
- Restored balanced gun stats and updated sonic weapon range constraints for anti-armor tuning.

### Documentation
- Documented the ballistic armor interception formula and durability pressure model.

## 1.5.0 - 2026-05-04

### Changed
- Updated Fabric 1.21.1 gun item rendering to use static GUI baked models in inventory and hotbar views.

### Fixed
- Fixed Fabric 1.21.1 reload, cooling, and first-person render state from driving unintended item-in-hand sinking or cooldown flashes.
- Fixed Fabric 1.21.1 first-person sprint animation detection to avoid fragile perspective checks.
- Added third-person reload arm motion for player-held guns during reload progress.

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
- Fixed magazine-fed guns showing "No ammo" instead of "No compatible magazine" when no suitable loaded magazine is available for reload.

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
