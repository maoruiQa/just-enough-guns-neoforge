# NeoForge 1.21.1 Attachment Port Sync Notes

Source reference: `../Just-Enough-Guns` Forge 1.20.1 attachment system.

Current NeoForge 1.21.1 foundation:

- Attachment item definitions live in `src/main/java/ttv/migami/jeg/item/attachment/`.
- Registered attachment items live in `ModItems.ATTACHMENTS`.
- Gun attachment state is stored as one item id data component plus one full `ItemStack` data component per slot in `ModDataComponents`. The item id remains the compatibility/read fallback for guns written before stack storage existed:
  - `gun_scope_attachment`
  - `gun_barrel_attachment`
  - `gun_stock_attachment`
  - `gun_under_barrel_attachment`
  - `gun_magazine_attachment`
  - `gun_special_attachment`
  - `gun_paint_job_attachment`
  - `gun_dye_attachment`
  - `gun_kill_effect_attachment`
  - `gun_scope_attachment_stack`
  - `gun_barrel_attachment_stack`
  - `gun_stock_attachment_stack`
  - `gun_under_barrel_attachment_stack`
  - `gun_magazine_attachment_stack`
  - `gun_special_attachment_stack`
  - `gun_paint_job_attachment_stack`
  - `gun_dye_attachment_stack`
  - `gun_kill_effect_attachment_stack`
- Flashlight attachment runtime state is stored on the gun stack:
  - `gun_flashlight_powered`
  - `gun_flashlight_battery`
- `GunAttachments` is the canonical read/write helper. Future UI, renderer, and gameplay code should use it instead of adding ad-hoc tags.
- `GunAttachmentRules` is the current per-gun support matrix for slot validation. It mirrors Forge 1.20.1's generated `data/jeg/guns/*.json` `modules.attachments` slot presence, with `phantom_smg` treated like its NeoForge-local `custom_smg` source because Forge 1.20.1 has no `phantom_smg` gun JSON.
- Static attachment-rule audit: all 32 current NeoForge `GunDefinitions.ALL` gun ids have a Forge 1.20.1 generated gun JSON source, using the intentional `phantom_smg` -> `custom_smg` mapping. The six functional slot rules in `GunAttachmentRules` match Forge `modules.attachments` presence for every current gun, with no extra NeoForge rule entries.
- `AttachmentMenu` is the first NeoForge menu port. It exposes six functional slots plus the three Forge cosmetic slots and writes through `GunAttachments`.
- `OpenAttachmentsPayload` opens the menu from the client keybinding.
- `MeleePayload` sends Forge-style gun melee key presses to the server for flashlight toggles and sword bayonet sweeps.
  - `minigun` is blocked from this melee-key path on both client and server, matching Forge 1.20.1's client guard.
- `AttachmentScreen` draws Forge-style slot icons, disabled-slot crosses, incompatible-slot hover feedback, and a rotating held-gun preview from the current menu validation/rendering rules.
- Recipes are standard 1.21 crafting recipes under `src/main/resources/data/jeg/recipe/`.
- Missing item definitions were added under `src/main/resources/assets/jeg/items/`; missing Forge models/textures were copied from Forge 1.20.1.
- Forge attachment Geo assets were copied under `src/main/resources/assets/jeg/geo/item/attachment/`.
- Active Forge cosmetic attachment items are registered: classic/toy/whiteout/golden spray cans plus creeper birthday, headpopper, and trickshot badges.
- Static registration audit: the active Forge 1.20.1 attachment/cosmetic item set matches this NeoForge port exactly: 5 scopes, 4 stocks, 3 barrels, 3 under-barrels, 2 magazine attachments, 2 special attachments, 4 spray cans, and 3 kill-effect badges.
- Static resource audit: every active attachment/cosmetic item has `assets/jeg/items/<id>.json`, `assets/jeg/models/item/<id>.json`, and `data/jeg/recipe/<id>.json` with a matching `jeg:<id>` result. Every active functional attachment also has `geo/item/attachment/<id>.geo.json` and `textures/animated/attachment/<id>.png`.
- Functional attachments, active spray cans, and kill-effect badges have explicit crafting recipes and are included in the gunsmith manual and explicit gun-recipe unlock paths.

Behavior wired so far:

- `bolt_action_rifle` only uses the scoped ADS/FOV/overlay path when its scope slot has an attachment.
- A barrel slot `silencer` switches gunfire to the gun's silenced fire sound when available.
- Pressing `key.jeg.attachments` opens a usable attachment menu while a gun is held in the main hand.
- The menu now exposes the Forge slot shape: six functional slots plus cosmetic `paint_job`, `dye`, and `kill_effect` slots.
- Registered attachment items now accept Binding Curse through NeoForge 1.21.1's holder-based enchantment support hook.
- Attachment slots now preserve Forge's pickup rule for binding-curse attachments: non-creative players cannot remove a bound attachment from a gun.
- Empty attachment slots now draw the Forge icon strip, inactive slots draw a cross, and dragging an incompatible item over an empty attachment slot shows the Forge incompatibility tooltip.
- The empty barrel slot tooltip now includes Forge's sword-bayonet hint, and the rotating gun preview shows the owning mod display name instead of a raw mod id.
- The attachment screen closes when the selected main-hand item stops being a gun, matching the Forge screen lifetime behavior.
- The attachment screen renders the held gun preview and gun name above the main panel.
- The attachment screen now renders the Forge mini config button. It opens a registered NeoForge `IConfigScreenFactory` screen when available, otherwise it shows the existing Forge-style "Install Configured" chat hint.
- The attachment screen now adapts Forge's client-side config-button polish as NeoForge client config keys:
  - `rendering.hideAttachmentConfigButton` hides the config button.
  - `rendering.attachmentButtonAlignment` accepts `left` or `right` and positions the config button with the same Forge title-width/right-edge formulas.
- The attachment screen now preserves the Forge left-panel thank-you hover tooltip.
- The attachment screen now renders the Forge medal toggle button, sends a server-authoritative toggle payload, and stores the gun's kill-medal toggle in the synced `gun_medals_enabled` component.
  - Forge's global medal-disable behavior is adapted as the persistent server config `ui.hideMedals`, exposed through `/justEnoughGuns config ui hideMedals` and synced to clients with the existing UI config payload; when enabled, the attachment-screen medal button shows the disabled tooltip and ignores clicks.
  - Headshot kills from bullets fired with `gun_medals_enabled` true now send a client headshot medal payload unless `ui.hideMedals` is enabled. The client renders the existing `combat_headshot` medal texture and plays `ui.medal.headshot`.
  - Forge-style kill medal runtime is now queued client-side through `KillMedalPayload` and `MedalPayload`: normal gun kills use the multikill window, and special medals cover `gear_boom`, `combat_hush`, `gear_bbq`, `combat_kingslayer`, and `combat_just_enough_ammo`.
  - Bullet deaths use the firing bullet's medal-enabled and last-ammo snapshots, so bullet medal behavior follows the gun state at shot time instead of the player's later main-hand state.
- Cosmetic slots now accept and persist their matching items:
  - `paint_job`: registered spray cans.
  - `dye`: vanilla dye items.
  - `kill_effect`: registered kill-effect badges.
- Functional attachment slot availability now follows the Forge 1.20.1 per-gun attachment matrix instead of default-opening all functional slots for guns without explicit NeoForge entries. Cosmetic slots remain available for every gun, matching Forge's attachment-screen rules.
- Functional attachment items are tagged as vanilla `minecraft:dyeable` and have a client item-color handler that reads `DataComponents.DYED_COLOR`, adapting Forge 1.20.1's `IColored` attachment item data semantics to NeoForge 1.21.1 components. Because attachment slots persist full `ItemStack`s, dyed attachment components are preserved when installed on guns.
- Functional attachment item tooltips now show Forge-style `Perks:` entries derived from the currently ported modifier fields, including silencer, explosive ammo, flashlight, laser pointer, trumpet, damage, spread, recoil/kick, and ADS speed effects.
- Functional attachment modifier parity audit:
  - Registered NeoForge modifier values match Forge 1.20.1's active `GunModifiers` for scope FOV, ADS speed, recoil/kick, spread, damage, silencer, silencer fire sound radius, explosive ammo, flashlight, laser pointer, trumpet, extended magazine, and drum magazine behavior.
  - Static modifier-surface audit: Forge active attachment registration references `ANNOYING`, `BETTER_CONTROL`, `EXPLOSIVE_AMMO`, `FLASHLIGHT`, `INCREASED_JAMMING`, `INSCREASED_DAMAGE`, `LASER_POINTER`, `LIGHT_RECOIL`, `MAKESHIFT_CONTROL`, `REDUCED_DAMAGE`, `REDUCED_RECOIL`, `SILENCED`, `SLOW_ADS`, `SLOWER_ADS`, `SLOWEST_ADS`, `STABILISED`, `SUPER_STABILISED`, and `WORSE_CONTROL`; each maps to the current NeoForge `AttachmentModifiers` fields or the explicit magazine-capacity adaptation.
  - Forge's `INCREASED_JAMMING` marker on `explosive_muzzle` is preserved as the negative tooltip perk only. The actual Forge jam risk is driven by gun durability, and this NeoForge port already mirrors the gameplay-relevant explosive-muzzle side effect by damaging the gun 5 durability per shot instead of 1.
- Forge-style pseudo vanilla attachments now pass attachment menu validation and persist as full stored stacks:
  - Vanilla `spyglass` items can be installed in supported scope slots.
  - Vanilla sword items can be installed in supported barrel slots.
  - Vanilla `spyglass` scope attachments provide Forge-style scope modifiers; vanilla sword bayonets affect systems that check the stored stack directly.
- `makeshift_stock` is now accepted by any gun that supports the stock slot, plus the old Forge makeshift-only guns (`semi_auto_pistol` and `waterpipe_shotgun`) through a makeshift-only stock-slot exception. Standard stocks still use the normal per-gun `STOCK` support matrix.
- Sword bayonet sprint-charge behavior is partially wired:
  - Sprinting with a sword installed in the barrel slot for 40 ticks enables a Forge-style forward charge hit.
  - Charge damage uses the installed sword's attack damage plus Sharpness, divided by 1.5 like Forge.
  - Knockback and Fire Aspect enchantments on the installed sword affect hit targets.
  - Charge hits skip targets still in hit invulnerability, set the player hit-invulnerability window, emit Forge-style sweep audio and damage-indicator particles, and push the player backward when Sweeping Edge is below level 3.
  - Charge block collisions play the Forge break sound and push the player backward, scaled by bayonet Knockback.
  - Successful charge hits damage the stored bayonet stack by 15, gated by `combat.gunDurability`, creative mode, and Mending, then clear the barrel slot when the sword breaks.
- Installed attachment modifiers are now combined through `GunAttachments.modifiers(ItemStack)`.
- Installed attachments now preserve their full stored `ItemStack` when present, including damage and other item components. Legacy guns that only have the attachment id components still resolve through the id fallback.
- Damage multipliers are applied to spawned bullet damage.
  - Bullet creation for held-gun firing now goes through `GunItem.createBullet`, so player, gunner, phantom, and legacy `GunAttackGoal` gunfire share the same damage, explosive-ammo, kill-effect, and flare-dye attachment state propagation. Vehicle-mounted weapons still construct bullets from vehicle weapon data because they do not have a gun `ItemStack` attachment context.
- Spread multipliers are applied to server projectile spread and the client dynamic crosshair.
- Recoil/kick multipliers are applied to local visual recoil and heavy-gun backstep.
- Scope FOV modifiers affect ADS FOV, and ADS speed multipliers affect client aim-in/aim-out progress.
  - Vanilla `spyglass` scope attachments use Forge's pseudo-scope behavior: `0.2F` aim FOV modifier and slowest ADS speed.
- Magazine capacity modifiers are applied to loaded gun capacity, reload limits, tooltips, and the ammo HUD:
  - `extended_mag` uses Forge parity behavior: +50% capacity, with `infantry_rifle` forced to 20.
  - `drum_mag` uses Forge parity behavior: +100% capacity, with `infantry_rifle` forced to 40.
  - The separate NeoForge loaded `MagazineItem` ammo containers keep their existing fixed capacities and do not need extended/drum variants. When a gun has a magazine attachment installed, reloads use the Forge-style loose-ammo path so the gun can fill to the attachment-modified capacity; guns without a magazine attachment keep the existing NeoForge magazine-swap reload path and HUD reserve display.
- Barrel attachment fire side effects are partially wired:
  - `trumpet` plays `item.doot` after firing.
  - `explosive_muzzle` plays the fire-charge sound after firing and consumes 5 gun durability per shot.
- `explosive_muzzle` marks spawned bullets as explosive ammo and adds Forge-style wood/stone block break chances on block impact:
  - Wood chance: `0.1 / (destroySpeed + 1)`.
  - Stone chance: `0.05 / (destroySpeed + 1)`.
  - Block breaking is gated by the existing NeoForge `Config.bulletBlockDestructionEnabled()` toggle instead of porting Forge's separate griefing config keys.
  - Non-player shooters still respect `mobGriefing`.
- Dynamic-light infrastructure is registered for attachment flashlights:
  - `dynamic_light` is an invisible, waterloggable, self-expiring light block.
  - `gun_flashlight_powered` stores the attached flashlight's powered state because this port stores attachment IDs instead of full attachment `ItemStack`s.
  - `gun_flashlight_battery` stores adapted flashlight battery life on the gun stack, using Forge's 600 tick max battery.
- Special-slot flashlight and laser pointer runtime behavior is partially wired:
  - Pressing `key.jeg.melee` while holding a gun with a flashlight toggles the powered state and plays `item.flashlight`.
  - Pressing `key.jeg.melee` while holding a gun with a vanilla sword in the barrel slot performs the Forge-style bayonet sweep.
  - Gun-mounted flashlight and laser pointer output pauses while the player is sprinting and the main-hand gun is not on cooldown; the powered state is preserved and resumes after sprinting stops.
  - Flashlight attachments now refuse to toggle when server config disables flashlights.
  - Powered flashlight attachments drain battery for non-creative players, turn off at zero, and show Forge's dead-battery chat message.
  - Powered flashlight attachments refresh `dynamic_light` blocks along the player's look ray, gated by `attachments.allowFlashlights` and `attachments.flashlightDistance`.
  - Animated gun `flashlight_glow` bones now mirror the installed flashlight powered state, matching Forge's glow-bone visibility rule.
  - Standalone flashlight items now keep Forge-style battery/powered state, right-click toggling, attack-key charging, durability-bar battery display, and dynamic-light output while held.
  - Laser pointer attachments apply Glowing to aimed-at living entities while ADS is active when `attachments.glowingLaserPointers` is enabled.
  - Laser pointer attachments also pull nearby cats/ocelots toward the hit point like Forge 1.20.1.
  - Laser pointer attachments now emit only the Forge-style custom red block-face laser particle on block hits; they no longer spawn red beam particles along the ray or a vanilla endpoint glint.
- Sword bayonet melee-key behavior is wired:
  - Pressing `key.jeg.melee` sends a dedicated `MeleePayload`, so sword bayonets work even when the gun has no flashlight.
  - The server validates that the main-hand item is a gun and the barrel slot stores a `SwordItem` stack.
  - The sweep uses Forge's 2 block range and 100 degree forward arc.
  - Sweep damage uses the installed sword's attack damage plus Sharpness, divided by 1.5 like Forge.
  - Knockback and Fire Aspect enchantments on the installed sword affect hit targets.
  - Successful sweep hits damage the stored bayonet stack by 8, gated by `combat.gunDurability`, creative mode, and Mending, then clear the barrel slot when the sword breaks.
  - The sweep applies a 15 tick gun cooldown, or 40 ticks while sprinting, and emits the vanilla sweep sound and particle.
- Attachment durability/breakage is partially wired for the Forge-damaged firing slots:
  - Forge's `gunDurability` config is adapted as the persistent server config `combat.gunDurability`; when disabled, firing does not damage the gun or installed attachments.
  - Forge's global low-durability gun jamming config is adapted as persistent server config `combat.gunJamming`; when enabled, low-durability guns can fail before consuming ammo, play the pistol cock sound, show `chat.jeg.jam`, and receive the Forge-style extended cooldown.
  - Guns at or past their next-shot break threshold fail before firing and play the item break sound, matching Forge's pre-shot durability gate.
  - `explosive_muzzle` still increases gun wear to 5 durability per shot and carries the `increased_jamming` perk, causing the low-durability jam threshold to start earlier than the base Forge threshold.
  - `scope`, `barrel`, `stock`, and `under_barrel` each have a persistent integer damage component on the gun stack for legacy compatibility.
  - Firing damages those installed attachments by 1 per shot and writes the updated damage back to the stored attachment `ItemStack`.
  - Forge's Mending gates are preserved: a Mending gun skips attachment wear entirely, and Mending attachments skip wear except for the Forge `explosive_muzzle` barrel exception.
  - When an attachment reaches its registered max damage, the slot is cleared, `item_break` plays, and `chat.jeg.attachment_broke` is shown.
  - The integer damage component is still read as a fallback for guns written before full attachment stack storage existed.
- Trumpet barrel attachment soundwave behavior is partially wired:
  - Firing with `trumpet` still plays `item.doot`.
  - Forge-style blast behavior is limited to guns with `projectileAmount > 3`: the shooter is pushed backward and nearby living entities in the forward cone are pushed away without extra damage.
  - The custom `sonic_ring` and `big_sonic_ring` particle types/providers are registered and emitted from the trumpet soundwave.
- Gunfire visual parity is partially wired:
  - Successful non-silenced player gunfire refreshes a very short-lived `dynamic_light` block near the player's eye/forward view position, excluding Forge's non-flash weapons such as `finger_gun`, `typhoonee`, `atlantean_spear`, bows, and blowpipes.
  - Bullet impacts on hard blocks emit yellow spark particles and vanilla electric sparks; because the NeoForge 1.21.1 branch does not currently ship all Forge `jeg:stone`/`jeg:wood`/`jeg:metal` block tags, hard-hit detection also falls back to vanilla pickaxe/axe block tags.
  - Stone-like and wood-like impacts also emit a small cloud puff through the long-distance particle send path so hit feedback remains visible outside the normal short particle radius.
  - Casing particles are registered for `casing`, `shell`, and `spectre_casing`; successful gunfire emits one server-side casing/shell particle near the shooter's gun side based on the gun ammo item instead of one particle per pellet.
  - Forge's yellow trail-color gun list is applied at bullet creation without hand-editing generated `GunDefinitions.java`.
  - Client muzzle flash rendering is no longer suppressed while the local player is ADS.
  - Local first-person muzzle flashes are rendered while the animated gun renderer visits the gun model's `attachment_bone`, using the Forge muzzle-flash profile position relative to that bone's pivot so ADS/draw/reload transforms move the flash with the held gun. World-space billboard flashes remain for third-person and other entities.
  - Third-person/world-space muzzle flashes use a stronger forward view-vector correction so the billboard sits at the muzzle instead of the front-middle of the gun body.
  - Muzzle flash UV selection mirrors Forge's forced alternate half for `subsonic_rifle`, `flamethrower`, `supersonic_shotgun`, `hypersonic_cannon`, `soulhunter_mk2`, `blossom_rifle`, and `holy_shotgun`; missing Forge muzzle profiles for `atlantean_spear`, `bubble_cannon`, `vindicator_smg`, and `fire_sweeper` are staged in the profile table.
- Draw/reload interaction parity is partially wired:
  - Animated guns set a synced `gun_draw_ticks_remaining` component when first held and play the authored `draw` animation while blocking firing/reloading.
  - Draw operation locking remains short and server-authoritative, but the client animation predicate now keeps the visual `draw` sequence alive long enough for authored draw animations to finish instead of letting sprint/idle immediately replace it.
  - The animation predicate now continues already-playing idle and sprint animations instead of resetting the same fallback animation every frame.
  - Non-first-person item render perspectives stop the GeckoLib gun animation predicate, and third-person render entry points explicitly stop the item controller and clear snapshots, so third-person gun renders keep the static Geo pose and do not play draw, reload, shoot, sprint, or idle animations.
  - Reload requests now start a pending reload instead of immediately consuming ammo or swapping magazines.
  - Loaded ammo or magazine swap state is applied only when the reload visual timer completes.
  - Switching the held hand or main-hand hotbar slot during reload cancels pending progress and clears reload visual components.
  - The client advances its own reload visual timer and segmented reload stage while the gun remains held, but it still does not apply ammo or magazine state. This keeps GeckoLib from waiting on delayed item-stack component sync after the server-side reload has already finished.
  - The client now tracks normal held-gun transitions separately from the logical server draw lock. Any non-reloading gun that becomes held again gets an immediate local GeckoLib `draw` restart, covering ordinary switch-back cases as well as guns that were just reloaded and not fired.
  - Reload cancellation now queues a fresh draw animation and preserves that queued draw while the interrupted stack is not held, but it does not start the local GeckoLib draw window at cancellation time. This keeps the draw window from expiring while the player is holding another item.
  - The queued reload-cancel draw is tracked separately on the logical server and client; the client consumes its own queue only when the interrupted stack becomes held again, then strips the stale `gun_reload_ticks_*` visual components and force-resets the controller so GeckoLib restarts `draw` at switch-back time.
  - First-person animation decisions resolve the GeckoLib render-copy stack back to the player's live held stack before checking reload/draw components. This prevents a lagged render stack with old reload components from continuing a down-pressed reload pose for several seconds and then snapping to idle without playing the queued draw.
  - Client draw replay also keeps a short local draw window independent of the stack's `gun_draw_ticks_remaining` component because the ItemStack handed to GeckoLib rendering can lag behind the inventory stack component after slot changes.
  - Draw restart requests are consumed before the predicate's normal "continue current draw" branch; otherwise GeckoLib can continue a stale down-tilted draw/reload pose until the visual window expires and then snap directly back to idle.
  - Animated gun controllers now register a Forge-style GeckoLib sound keyframe handler. Authored `sound_effects` keys such as `rustle`, `screw`, `reload_mag_out`, `reload_mag_in`, `reload_end`, `ejector_pull`, `ejector_release`, and `jammed` resolve to the current gun's `GunStats` sounds or the shared Forge rustle/screw/jam sounds, so draw/switch animations can play their authored audio.
- Attachment renderer visibility has initial magazine coverage:
  - Default mag bones (`default_mag`, `default_mag_2`) stay visible until an extended/drum magazine attachment is installed.
  - Installed extended/drum magazine attachments reveal both the primary and secondary model bones where the copied gun models provide dual-mag variants.
  - Guns whose copied Geo model only has a baked `makeshift_stock` stock visual (`abstract_gun`, `assault_rifle`, `custom_smg`, `double_barrel_shotgun`, `phantom_smg`, `pump_shotgun`, `revolver`, and `semi_auto_rifle`) reveal that stock visual only when `makeshift_stock` is installed. Guns with dedicated `light_stock`, `tactical_stock`, and `weighted_stock` bones still reveal only the matching installed stock.
- Attachment bone visibility preserves authored base rails such as `service_rifle`'s `railing` while still hiding unsupported generic rail bones by default.
- Light machine gun render visibility now mirrors Forge's `bullet_1` through `bullet_7` bone thresholds by hiding exposed bullet bones above the current synced `gun_ammo` count.
- Gun paint-job rendering is partially wired:
  - If the cosmetic `paint_job` slot contains a spray can, animated gun rendering checks `textures/animated/gun/paintjob/<paintJob>/<gun>.png`.
  - Guns without a matching paint-job texture fall back to the existing animated gun texture, then the item texture.
  - Animated gun model lookup now checks `geo/item/gun/paintjob/<paintJob>/<gun>.geo.json` with the currently rendered gun stack before falling back to the base gun Geo model.
  - The only Forge reference paint-job Geo asset found is `paintjob/scorched/burst_rifle.geo.json`; `scorched_spray_can` is disabled in Forge 1.20.1 and is not registered in this NeoForge port, and this branch currently has no gun paint-job Geo override assets for active spray cans. The support path is present for future assets, but has no active resource hit yet.
  - Static active paint-job texture audit: the registered Forge/NeoForge spray cans are `classic`, `toy`, `whiteout`, and `golden`; this branch has the matching active texture sets for current NeoForge guns (`classic`: 6, `toy`: 10, `whiteout`: 3, `golden`: 15). Forge has one extra active `golden/compound_bow.png`, but `compound_bow` is not a current NeoForge 1.21.1 gun target.
  - Static paint-job fallback audit: Forge 1.20.1's attachment paint-job model override is commented out while its attachment texture override is active. NeoForge mirrors that active behavior by checking attachment paint-job textures first and falling back to base attachment textures; scope rendering only uses a paint-job attachment Geo model when such a resource actually exists.
  - `AnimatedGunRenderer.getTextureLocation` owns the animated gun texture override, so `AnimatedGunGeoModel.getTextureResource` intentionally keeps returning the base texture resource.
- Scope attachment model rendering is partially wired:
  - Installed scope-slot stacks render as Geo models at the gun model's `attachment_bone` after validation through `GunAttachments.canAttachStack`.
  - Scope attachment rendering resolves `geo/item/attachment/<attachment>.geo.json`.
  - Scope attachments use the Forge 1.20.1 generated `setScope(...)` slot transforms through `GunAttachmentTransforms`, so guns with different authored scope rails such as `combat_rifle` and `assault_rifle` mount the same scope assets through data instead of ad-hoc per-gun offsets.
  - Vanilla `spyglass` scope attachments render through the same scope layer using Forge 1.20.1's copied `spyglass.geo.json` and `spyglass.png` assets.
  - The JEG long-scope overlay, scoped FOV path, and scoped mouse-sensitivity reduction are now gated specifically by the installed `telescopic_sight` attachment. Other scope-slot attachments follow the normal iron-sight/attachment ADS path.
  - `combat_rifle` and `service_rifle` with `holographic_sight` keep the normal non-telescopic ADS path and apply only a small per-gun Y correction for their slightly high holographic-sight view.
  - The telescopic-sight scoped path is gun-agnostic: any gun with `telescopic_sight` uses the scoped overlay/FOV path, while bolt-action forced ADS release remains limited to bolt-action rifle plus `telescopic_sight`.
  - If the gun has a `paint_job` cosmetic slot, scope attachment rendering first checks `textures/animated/attachment/paintjob/<paintJob>/<attachment>.png`, then falls back to `textures/animated/attachment/<attachment>.png`.
  - This replaces the earlier hard-coded bolt-action `combat_scope` layer, so non-combat scopes now use their own model assets where present.
- Positioned non-scope attachment rendering is partially wired:
  - Forge 1.20.1 generated gun attachment transforms are adapted into a NeoForge client helper for `scope`, `barrel`, `under_barrel`, and `special` slots, with an added `assault_rifle` standard-stock transform derived from the baked makeshift-stock anchor because that gun has no baked `light_stock`/`tactical_stock`/`weighted_stock` bones. The assault-rifle standard-stock mount is lowered to align the generic stock top with the baked makeshift-stock visual range.
  - Static coverage check: every Forge 1.20.1 gun JSON that declares a supported `scope`, `barrel`, `underBarrel`, or `special` slot has a matching `GunAttachmentTransforms` entry; `phantom_smg` intentionally reuses the local `custom_smg` transform mapping.
  - Installed attachment Geo models for those slots render at the gun model's `attachment_bone` with the Forge position/scale data and the same paint-job -> base -> fallback texture resolution.
  - The shared `attachment_bone` remains visible when any scope, barrel, stock, under-barrel, special, or bayonet render path is active, so independent Geo attachments such as `vertical_grip`, `angled_grip`, and standard SMG stocks have a render anchor even when no scope is installed.
  - The gun model's baked barrel and special attachment bones remain hidden while the independent Geo layer owns those slots, avoiding duplicate attachment geometry.
  - Gun models that already include baked `light_grip`, `vertical_grip`, and `angled_grip` bones (`combat_rifle`, `pump_shotgun`, and `holy_shotgun`) now reveal their authored parent/child grip bones for the installed under-barrel attachment instead of rendering a second independent Geo grip at the wrong coordinate frame. Their `under_barrel`/`grip` parent bones explicitly keep children visible so vertical and angled grip child geometry is not suppressed after the default hidden-bone pass.
  - Independent under-barrel attachment Geo roots named `vertical_grip` or `angled_grip` are no longer run through the gun model visibility pass during attachment re-rendering, preventing the shared default hidden-bone list from hiding the attachment model itself while `LightGrip` remains unaffected.
  - Baked under-barrel parent bones now set child-hidden state from the same installed-grip decision as their own hidden state, so `combat_rifle`, `pump_shotgun`, and `holy_shotgun` do not retain hidden child grip geometry after switching between grip states.
  - Standard stock rendering stays baked for guns with authored `light_stock`, `tactical_stock`, and `weighted_stock` bones, while `assault_rifle` renders standard stocks through the independent attachment layer. Its baked `makeshift_stock` bone remains reserved for the installed `makeshift_stock`.
  - `custom_smg` and `phantom_smg` now use independent stock transforms for standard stocks because their animated Geo models only include a baked `makeshift_stock` bone. Their installed `makeshift_stock` still stays owned by the baked gun model.
  - Non-baked `makeshift_stock` rendering has independent stock transforms for `burst_rifle`, `combat_rifle`, `hollenfire_mk2`, `service_rifle`, `semi_auto_pistol`, and `waterpipe_shotgun`; standard stocks on baked-stock guns remain owned by the gun Geo model.
  - Vanilla sword barrel attachments are skipped here and remain owned by the dedicated bayonet layer to avoid duplicate rendering.
  - `magazine` is intentionally still handled by baked gun-model bone visibility for now because the Forge reference data uses `scale: 0.0` for that slot on most guns.
- Vanilla sword bayonet render assets are staged:
  - Forge 1.20.1's `wooden_sword`, `stone_sword`, `iron_sword`, `golden_sword`, `diamond_sword`, and `netherite_sword` bayonet Geo assets are copied under `geo/item/attachment/`.
  - Forge 1.20.1's `modded_sword.geo.json` fallback is copied so non-vanilla `SwordItem` barrel attachments can render with their item texture when the texture exists.
  - Matching `textures/animated/attachment/*_sword.png` assets are present and match the Forge reference hashes.
  - Runtime bayonet rendering uses a dedicated `GunBayonetAttachmentLayer` that renders validated `SwordItem` barrel stacks at `attachment_bone`, matching Forge 1.20.1's bayonet render hook without sharing scope-layer state.
  - Bayonet rendering checks `textures/animated/attachment/paintjob/<paintJob>/<sword>.png` before the base sword attachment texture, matching the Forge attachment texture fallback path.
  - Bayonet rendering still needs runtime visual validation across first-person and third-person contexts before broader non-scope attachment rendering is attempted.
- Kill-effect badges and kill medals are partially wired for held-gun bullets:
  - Bullet headshot kill handling now marks dead targets with the Forge `JEGDying` guard even when no kill-effect badge is installed, so medal/kill-effect delivery does not repeat on the same dead target.
  - Bullets carry the firing gun's `kill_effect` cosmetic slot id.
  - Bullet deaths now feed the Forge-style generic and special kill medal event path; headshot medals remain emitted from the bullet headshot kill-effect path.
  - Headshot kills with `creeper_birthday_party_badge` spawn confetti/explosion particles and play `item.kill_effect.birthday_party`.
  - Headshot kills with `headpoppper_badge` apply the `popped` kill effect, which emits popcorn particles and beehive pop sounds while ticking.
  - Headshot kills with `trickshot_badge` apply the `trickshotted` kill effect, which emits hit-marker particles and Forge-style hit/air-horn/goose sounds while ticking.
- Dye cosmetic behavior is partially wired for `flare_gun`:
  - Animated gun Geo rendering uses the installed dye cosmetic's firework color as its render tint, matching Forge 1.20.1's `DyeUtils.getStoredDyeRGB` renderer path.
  - Bullets carry the firing gun's `dye` cosmetic slot color when the gun is `flare_gun`.
  - Flare smoke uses `colored_flare_smoke` with the dye's firework RGB color when a dye is installed.
  - Flare guns without a dye keep the existing default red flare smoke.
- Cosmetic item parity audit:
  - Forge 1.20.1's active cosmetic attachment item set is mirrored: `classic_spray_can`, `toy_spray_can`, `whiteout_spray_can`, `golden_spray_can`, `creeper_birthday_party_badge`, `headpoppper_badge`, and `trickshot_badge`.
  - The disabled Forge spray cans (`camo`, `scorched`, `anime`) remain unregistered in this port; matching resource leftovers are not active attachment items.
  - Paint-job can and kill-effect badge tooltip/foil behavior matches the Forge 1.20.1 item classes.

Still to port:

- Runtime visual validation remains intentionally skipped for now per current task scope; smoke tests are the gate for these slices.
- Remaining pseudo vanilla attachment render behavior:
  - Vanilla and modded sword bayonet rendering is code-wired through the copied Forge assets, but still needs later runtime visual validation and positioning follow-up.
- Cosmetic slots: runtime validation of active paint-job, kill-effect, and dye visuals.

Sync checklist for the other maintained branches:

1. Port `AttachmentType`, `AttachmentModifiers`, `AttachmentItem`, `GunAttachments`, and `GunAttachmentRules`, including pseudo vanilla attachment stack validation for spyglass and swords.
2. Add equivalent persistent/network-synced gun attachment id and stack components, with id fallback for old guns.
3. Register the same attachment items and add them to combat creative tab/manual recipes.
4. Copy item definitions, item models, textures, and recipes.
5. Rewire bolt-action scoped ADS to read the stack's scope slot instead of a global flag.
6. Rewire silencer sound behavior through the barrel slot.
7. Port `AttachmentMenu`, `AttachmentScreen`, `OpenAttachmentsPayload`, and the attachment keybinding.
   - Include Forge's binding-curse enchantment support for attachment items and pickup lock on attachment slots.
   - Include the attachment-screen config button's hide/alignment client config equivalents if the target branch has a client config surface.
8. Port the combined runtime modifier helper and wire damage, spread, recoil/kick, ADS FOV, and ADS speed through the active gameplay/client paths.
   - Include `combat.gunJamming`, the low-durability pre-shot jam gate, the next-shot break gate, and the `increased_jamming` threshold behavior used by `explosive_muzzle`.
9. Port magazine capacity modifiers and update reload, tooltip, and HUD cap display.
10. Port barrel attachment fire side effects for trumpet/explosive muzzle audio, explosive muzzle gun wear, and explosive muzzle block interaction.
11. Port dynamic-light infrastructure plus flashlight/laser server tick behavior.
   - Preserve gun-mounted flashlight/laser sprint pause and laser point-only behavior; do not reintroduce beam particles along the ray.
12. Port attachment durability/breakage for the firing-damaged functional slots.
13. Port trumpet soundwave gameplay behavior with adapted vanilla particles.
14. Port scope attachment model rendering with paint-job texture fallback, including the copied Forge spyglass pseudo-scope model asset.
15. Port pseudo vanilla sword bayonet melee-key behavior with the dedicated melee payload.
16. Copy vanilla sword bayonet Geo/texture assets, then add a dedicated bayonet render layer and runtime-validate its first-person/third-person positioning.
17. Add the vanilla `minecraft:dyeable` tag and item-color handler for functional attachment items so dyed attachment stacks survive menu install/removal.
18. Port cosmetic dye render tint for animated guns and flare-smoke color propagation.
19. Port the attachment-screen kill-medal toggle, synced `ui.hideMedals`, bullet headshot medals, generic kill medals, and Forge special kill medals.
20. Port gunfire visual parity: player gunfire dynamic light, hard-block impact sparks/cloud puffs, Forge yellow bullet trail color overrides, ADS-visible item-pose first-person muzzle flashes, and forced alternate muzzle-flash UVs.
21. Port draw/reload interaction parity: synced draw timer, draw operation lock, delayed reload completion, and reload cancellation when the player changes hand/slot.
22. Then expand remaining runtime modifier/render behavior.
