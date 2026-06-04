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
- `AttachmentMenu` is the first NeoForge menu port. It exposes six functional slots plus the three Forge cosmetic slots and writes through `GunAttachments`.
- `OpenAttachmentsPayload` opens the menu from the client keybinding.
- `MeleePayload` sends Forge-style gun melee key presses to the server for flashlight toggles and sword bayonet sweeps.
- `AttachmentScreen` draws Forge-style slot icons, disabled-slot crosses, incompatible-slot hover feedback, and a rotating held-gun preview from the current menu validation/rendering rules.
- Recipes are standard 1.21 crafting recipes under `src/main/resources/data/jeg/recipe/`.
- Missing item definitions were added under `src/main/resources/assets/jeg/items/`; missing Forge models/textures were copied from Forge 1.20.1.
- Forge attachment Geo assets were copied under `src/main/resources/assets/jeg/geo/item/attachment/`.
- Active Forge cosmetic attachment items are registered: classic/toy/whiteout/golden spray cans plus creeper birthday, headpopper, and trickshot badges.
- Functional attachments, active spray cans, and kill-effect badges have explicit crafting recipes and are included in the gunsmith manual unlock list.

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
- The attachment screen now renders the Forge medal toggle button, sends a server-authoritative toggle payload, and stores the gun's kill-medal toggle in the synced `gun_medals_enabled` component.
  - Forge's global medal-disable behavior is adapted as the persistent server config `ui.hideMedals`, exposed through `/justEnoughGuns config ui hideMedals` and synced to clients with the existing UI config payload; when enabled, the attachment-screen medal button shows the disabled tooltip and ignores clicks.
- Cosmetic slots now accept and persist their matching items:
  - `paint_job`: registered spray cans.
  - `dye`: vanilla dye items.
  - `kill_effect`: registered kill-effect badges.
- Functional attachment slot availability now follows the Forge 1.20.1 per-gun attachment matrix instead of default-opening all functional slots for guns without explicit NeoForge entries. Cosmetic slots remain available for every gun, matching Forge's attachment-screen rules.
- Functional attachment items are tagged as vanilla `minecraft:dyeable` and have a client item-color handler that reads `DataComponents.DYED_COLOR`, adapting Forge 1.20.1's `IColored` attachment item data semantics to NeoForge 1.21.1 components. Because attachment slots persist full `ItemStack`s, dyed attachment components are preserved when installed on guns.
- Functional attachment item tooltips now show Forge-style `Perks:` entries derived from the currently ported modifier fields, including silencer, explosive ammo, flashlight, laser pointer, trumpet, damage, spread, recoil/kick, and ADS speed effects.
- Forge-style pseudo vanilla attachments now pass attachment menu validation and persist as full stored stacks:
  - Vanilla `spyglass` items can be installed in supported scope slots.
  - Vanilla sword items can be installed in supported barrel slots.
  - Vanilla `spyglass` scope attachments provide Forge-style scope modifiers; vanilla sword bayonets affect systems that check the stored stack directly.
- Sword bayonet sprint-charge behavior is partially wired:
  - Sprinting with a sword installed in the barrel slot for 40 ticks enables a Forge-style forward charge hit.
  - Charge damage uses the installed sword's attack damage plus Sharpness.
  - Knockback and Fire Aspect enchantments on the installed sword affect hit targets.
  - Successful charge hits damage the stored bayonet stack by 8 and clear the barrel slot when the sword breaks.
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
  - Flashlight attachments now refuse to toggle when server config disables flashlights.
  - Powered flashlight attachments drain battery for non-creative players, turn off at zero, and show Forge's dead-battery chat message.
  - Powered flashlight attachments refresh `dynamic_light` blocks along the player's look ray, gated by `attachments.allowFlashlights` and `attachments.flashlightDistance`.
  - Animated gun `flashlight_glow` bones now mirror the installed flashlight powered state, matching Forge's glow-bone visibility rule.
  - Standalone flashlight items now keep Forge-style battery/powered state, right-click toggling, attack-key charging, durability-bar battery display, and dynamic-light output while held.
  - Laser pointer attachments apply Glowing to aimed-at living entities while ADS is active when `attachments.glowingLaserPointers` is enabled.
  - Laser pointer attachments also pull nearby cats/ocelots toward the hit point like Forge 1.20.1.
  - Laser pointer attachments emit custom `entity_laser` beam particles along the traced ray.
  - Laser pointer attachments emit the Forge-style custom red block-face laser particle on block hits, plus a small vanilla endpoint glint.
- Sword bayonet melee-key behavior is wired:
  - Pressing `key.jeg.melee` sends a dedicated `MeleePayload`, so sword bayonets work even when the gun has no flashlight.
  - The server validates that the main-hand item is a gun and the barrel slot stores a vanilla sword stack.
  - The sweep uses Forge's 2 block range and 100 degree forward arc.
  - Sweep damage uses the installed sword's attack damage plus Sharpness, divided by 1.5 like Forge.
  - Knockback and Fire Aspect enchantments on the installed sword affect hit targets.
  - Successful sweep hits damage the stored bayonet stack by 8 and clear the barrel slot when the sword breaks.
  - The sweep applies a 15 tick gun cooldown, or 40 ticks while sprinting, and emits the vanilla sweep sound and particle.
- Attachment durability/breakage is partially wired for the Forge-damaged firing slots:
  - `scope`, `barrel`, `stock`, and `under_barrel` each have a persistent integer damage component on the gun stack for legacy compatibility.
  - Firing damages those installed attachments by 1 per shot and writes the updated damage back to the stored attachment `ItemStack`.
  - When an attachment reaches its registered max damage, the slot is cleared, `item_break` plays, and `chat.jeg.attachment_broke` is shown.
  - The integer damage component is still read as a fallback for guns written before full attachment stack storage existed.
- Trumpet barrel attachment soundwave behavior is partially wired:
  - Firing with `trumpet` still plays `item.doot`.
  - Server-side soundwave now clears fire blocks in a forward cone, damages living entities in that cone with sonic-boom damage, resets hit invulnerability, and emits vanilla sculk/sonic particles.
  - The custom `sonic_ring` and `big_sonic_ring` particle types/providers are registered and emitted from the trumpet soundwave.
- Attachment renderer visibility has initial magazine coverage:
  - Default mag bones (`default_mag`, `default_mag_2`) stay visible until an extended/drum magazine attachment is installed.
  - Installed extended/drum magazine attachments reveal both the primary and secondary model bones where the copied gun models provide dual-mag variants.
- Attachment bone visibility preserves authored base rails such as `service_rifle`'s `railing` while still hiding unsupported generic rail bones by default.
- Light machine gun render visibility now mirrors Forge's `bullet_1` through `bullet_7` bone thresholds by hiding exposed bullet bones above the current synced `gun_ammo` count.
- Gun paint-job rendering is partially wired:
  - If the cosmetic `paint_job` slot contains a spray can, animated gun rendering checks `textures/animated/gun/paintjob/<paintJob>/<gun>.png`.
  - Guns without a matching paint-job texture fall back to the existing animated gun texture, then the item texture.
  - Animated gun model lookup now checks `geo/item/gun/paintjob/<paintJob>/<gun>.geo.json` with the currently rendered gun stack before falling back to the base gun Geo model.
  - The only Forge reference paint-job Geo asset found is `paintjob/scorched/burst_rifle.geo.json`; `scorched_spray_can` is disabled in Forge 1.20.1 and is not registered in this NeoForge port, and this branch currently has no gun paint-job Geo override assets for active spray cans. The support path is present for future assets, but has no active resource hit yet.
- Scope attachment model rendering is partially wired:
  - Installed scope-slot stacks render as Geo models at the gun model's `attachment_bone` after validation through `GunAttachments.canAttachStack`.
  - Scope attachment rendering resolves `geo/item/attachment/<attachment>.geo.json`.
  - Vanilla `spyglass` scope attachments render through the same scope layer using Forge 1.20.1's copied `spyglass.geo.json` and `spyglass.png` assets.
  - Vanilla `spyglass` scope attachments render the vanilla spyglass overlay while aiming; bolt-action rifle scoped ADS keeps using the existing JEG long-scope overlay.
  - If the gun has a `paint_job` cosmetic slot, scope attachment rendering first checks `textures/animated/attachment/paintjob/<paintJob>/<attachment>.png`, then falls back to `textures/animated/attachment/<attachment>.png`.
  - This replaces the earlier hard-coded bolt-action `combat_scope` layer, so non-combat scopes now use their own model assets where present.
- Positioned non-scope attachment rendering is partially wired:
  - Forge 1.20.1 generated gun attachment transforms are adapted into a NeoForge client helper for `barrel`, `under_barrel`, and `special` slots.
  - Installed attachment Geo models for those slots render at the gun model's `attachment_bone` with the Forge position/scale data and the same paint-job -> base -> fallback texture resolution.
  - Vanilla sword barrel attachments are skipped here and remain owned by the dedicated bayonet layer to avoid duplicate rendering.
  - `stock` and `magazine` are intentionally still handled by baked gun-model bone visibility for now because the Forge reference data uses `scale: 0.0` for those slots on most guns.
- Vanilla sword bayonet render assets are staged:
  - Forge 1.20.1's `wooden_sword`, `stone_sword`, `iron_sword`, `golden_sword`, `diamond_sword`, and `netherite_sword` bayonet Geo assets are copied under `geo/item/attachment/`.
  - Matching `textures/animated/attachment/*_sword.png` assets are present and match the Forge reference hashes.
  - Runtime bayonet rendering uses a dedicated `GunBayonetAttachmentLayer` that renders validated vanilla sword barrel stacks at `attachment_bone`, matching Forge 1.20.1's bayonet render hook without sharing scope-layer state.
  - Bayonet rendering checks `textures/animated/attachment/paintjob/<paintJob>/<sword>.png` before the base sword attachment texture, matching the Forge attachment texture fallback path.
  - Bayonet rendering still needs runtime visual validation across first-person and third-person contexts before broader non-scope attachment rendering is attempted.
- Kill-effect badges are partially wired for held-gun bullets:
  - Bullets carry the firing gun's `kill_effect` cosmetic slot id.
  - Headshot kills with `creeper_birthday_party_badge` spawn confetti/explosion particles and play `item.kill_effect.birthday_party`.
  - Headshot kills with `headpoppper_badge` apply the `popped` kill effect, which emits popcorn particles and beehive pop sounds while ticking.
  - Headshot kills with `trickshot_badge` apply the `trickshotted` kill effect, which emits hit-marker particles and Forge-style hit/air-horn/goose sounds while ticking.
- Dye cosmetic behavior is partially wired for `flare_gun`:
  - Bullets carry the firing gun's `dye` cosmetic slot color when the gun is `flare_gun`.
  - Flare smoke uses `colored_flare_smoke` with the dye's firework RGB color when a dye is installed.
  - Flare guns without a dye keep the existing default red flare smoke.

Still to port:

- Remaining Forge attachment-screen layout polish that depends on Forge-only client config options such as button hiding/alignment.
- Positional model rendering for non-scope attachments. The copied Forge models/textures are present, but barrel, stock, under-barrel, magazine, and special attachment models still need the equivalent of Forge's attachment-position/scale data before they can be rendered independently without overlapping or mounting at the wrong point.
- Remaining pseudo vanilla attachment runtime/render behavior:
  - Vanilla sword bayonet rendering is code-wired through the copied Forge assets, but needs runtime visual validation and positioning follow-up.
- Cosmetic slots: attachment dye/render behavior beyond item tint and flare smoke, active gun paint-job Geo assets if new registered spray cans need them, broader non-scope attachment paint-job rendering once non-scope positional models are ported, and runtime validation of kill-effect/dye visuals.

Sync checklist for the other maintained branches:

1. Port `AttachmentType`, `AttachmentModifiers`, `AttachmentItem`, `GunAttachments`, and `GunAttachmentRules`, including pseudo vanilla attachment stack validation for spyglass and swords.
2. Add equivalent persistent/network-synced gun attachment id and stack components, with id fallback for old guns.
3. Register the same attachment items and add them to combat creative tab/manual recipes.
4. Copy item definitions, item models, textures, and recipes.
5. Rewire bolt-action scoped ADS to read the stack's scope slot instead of a global flag.
6. Rewire silencer sound behavior through the barrel slot.
7. Port `AttachmentMenu`, `AttachmentScreen`, `OpenAttachmentsPayload`, and the attachment keybinding.
   - Include Forge's binding-curse enchantment support for attachment items and pickup lock on attachment slots.
8. Port the combined runtime modifier helper and wire damage, spread, recoil/kick, ADS FOV, and ADS speed through the active gameplay/client paths.
9. Port magazine capacity modifiers and update reload, tooltip, and HUD cap display.
10. Port barrel attachment fire side effects for trumpet/explosive muzzle audio, explosive muzzle gun wear, and explosive muzzle block interaction.
11. Port dynamic-light infrastructure plus flashlight/laser server tick behavior.
12. Port attachment durability/breakage for the firing-damaged functional slots.
13. Port trumpet soundwave gameplay behavior with adapted vanilla particles.
14. Port scope attachment model rendering with paint-job texture fallback, including the copied Forge spyglass pseudo-scope model asset.
15. Port pseudo vanilla sword bayonet melee-key behavior with the dedicated melee payload.
16. Copy vanilla sword bayonet Geo/texture assets, then add a dedicated bayonet render layer and runtime-validate its first-person/third-person positioning.
17. Add the vanilla `minecraft:dyeable` tag and item-color handler for functional attachment items so dyed attachment stacks survive menu install/removal.
18. Then expand remaining runtime modifier/render behavior.
