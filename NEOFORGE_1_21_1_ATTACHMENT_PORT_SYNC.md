# NeoForge 1.21.1 Attachment Port Sync Notes

Source reference: `../Just-Enough-Guns` Forge 1.20.1 attachment system.

Current NeoForge 1.21.1 foundation:

- Attachment item definitions live in `src/main/java/ttv/migami/jeg/item/attachment/`.
- Registered attachment items live in `ModItems.ATTACHMENTS`.
- Gun attachment state is stored as one item id data component per slot in `ModDataComponents`:
  - `gun_scope_attachment`
  - `gun_barrel_attachment`
  - `gun_stock_attachment`
  - `gun_under_barrel_attachment`
  - `gun_magazine_attachment`
  - `gun_special_attachment`
  - `gun_paint_job_attachment`
  - `gun_dye_attachment`
  - `gun_kill_effect_attachment`
- Flashlight attachment runtime state is stored on the gun stack:
  - `gun_flashlight_powered`
  - `gun_flashlight_battery`
- `GunAttachments` is the canonical read/write helper. Future UI, renderer, and gameplay code should use it instead of adding ad-hoc tags.
- `GunAttachmentRules` is the current per-gun support matrix for slot validation.
- `AttachmentMenu` is the first NeoForge menu port. It exposes six functional slots plus the three Forge cosmetic slots and writes through `GunAttachments`.
- `OpenAttachmentsPayload` opens the menu from the client keybinding.
- `AttachmentScreen` draws Forge-style slot icons, disabled-slot crosses, incompatible-slot hover feedback, and a rotating held-gun preview from the current menu validation/rendering rules.
- Recipes are standard crafting recipes under `src/main/resources/data/jeg/recipes/`.
- Missing item definitions were added under `src/main/resources/assets/jeg/items/`; missing Forge models/textures were copied from Forge 1.20.1.
- Forge attachment Geo assets were copied under `src/main/resources/assets/jeg/geo/item/attachment/`.
- Active Forge cosmetic attachment items are registered: classic/toy/whiteout/golden spray cans plus creeper birthday, headpopper, and trickshot badges.
- Cosmetic attachment items have explicit crafting recipes and are included in the gunsmith manual unlock list.

Behavior wired so far:

- `bolt_action_rifle` only uses the scoped ADS/FOV/overlay path when its scope slot has an attachment.
- A barrel slot `silencer` switches gunfire to the gun's silenced fire sound when available.
- Pressing `key.jeg.attachments` opens a usable attachment menu while a gun is held in the main hand.
- The menu now exposes the Forge slot shape: six functional slots plus cosmetic `paint_job`, `dye`, and `kill_effect` slots.
- Empty attachment slots now draw the Forge icon strip, inactive slots draw a cross, and dragging an incompatible item over an empty attachment slot shows the Forge incompatibility tooltip.
- The attachment screen closes when the selected main-hand item stops being a gun, matching the Forge screen lifetime behavior.
- The attachment screen renders the held gun preview and gun name above the main panel.
- Cosmetic slots now accept and persist their matching items:
  - `paint_job`: registered spray cans.
  - `dye`: vanilla dye items.
  - `kill_effect`: registered kill-effect badges.
- Installed attachment modifiers are now combined through `GunAttachments.modifiers(ItemStack)`.
- Damage multipliers are applied to spawned bullet damage.
- Spread multipliers are applied to server projectile spread and the client dynamic crosshair.
- Recoil/kick multipliers are applied to local visual recoil and heavy-gun backstep.
- Scope FOV modifiers affect ADS FOV, and ADS speed multipliers affect client aim-in/aim-out progress.
- Magazine capacity modifiers are applied to loaded gun capacity, reload limits, tooltips, and the ammo HUD:
  - `extended_mag` uses Forge parity behavior: +50% capacity, with `infantry_rifle` forced to 20.
  - `drum_mag` uses Forge parity behavior: +100% capacity, with `infantry_rifle` forced to 40.
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
  - Flashlight attachments now refuse to toggle when server config disables flashlights.
  - Powered flashlight attachments drain battery for non-creative players, turn off at zero, and show Forge's dead-battery chat message.
  - Powered flashlight attachments refresh `dynamic_light` blocks along the player's look ray, gated by `attachments.allowFlashlights` and `attachments.flashlightDistance`.
  - Laser pointer attachments apply Glowing to aimed-at living entities while ADS is active when `attachments.glowingLaserPointers` is enabled.
  - Laser pointer attachments also pull nearby cats/ocelots toward the hit point like Forge 1.20.1.
  - Laser pointer attachments emit the Forge-style custom red block-face laser particle on block hits, plus a small vanilla endpoint glint.
- Attachment durability/breakage is partially wired for the Forge-damaged firing slots:
  - `scope`, `barrel`, `stock`, and `under_barrel` each have a persistent integer damage component on the gun stack.
  - Firing damages those installed attachments by 1 per shot.
  - When an attachment reaches its registered max damage, the slot is cleared, `item_break` plays, and `chat.jeg.attachment_broke` is shown.
  - This is an adapted representation because this NeoForge port stores attachment item IDs rather than full attachment `ItemStack`s with enchantments/damage.
- Trumpet barrel attachment soundwave behavior is partially wired:
  - Firing with `trumpet` still plays `item.doot`.
  - Server-side soundwave now clears fire blocks in a forward cone, damages living entities in that cone with sonic-boom damage, resets hit invulnerability, and emits vanilla sculk/sonic particles.
  - The custom `sonic_ring` and `big_sonic_ring` particle types/providers are registered and emitted from the trumpet soundwave.
- Attachment renderer visibility has initial magazine coverage:
  - Default mag bones (`default_mag`, `default_mag_2`) stay visible until an extended/drum magazine attachment is installed.
  - Installed extended/drum magazine attachments reveal both the primary and secondary model bones where the copied gun models provide dual-mag variants.
- Gun paint-job rendering is partially wired:
  - If the cosmetic `paint_job` slot contains a spray can, animated gun rendering checks `textures/animated/gun/paintjob/<paintJob>/<gun>.png`.
  - Guns without a matching paint-job texture fall back to the existing animated gun texture, then the item texture.
- Scope attachment model rendering is partially wired:
  - Installed scope-slot attachments render as Geo models at the gun model's `attachment_bone`.
  - Scope attachment rendering resolves `geo/item/attachment/<attachment>.geo.json`.
  - If the gun has a `paint_job` cosmetic slot, scope attachment rendering first checks `textures/animated/attachment/paintjob/<paintJob>/<attachment>.png`, then falls back to `textures/animated/attachment/<attachment>.png`.
  - This replaces the earlier hard-coded bolt-action `combat_scope` layer, so non-combat scopes now use their own model assets where present.
- Kill-effect badges are partially wired for player-fired bullets:
  - Bullets carry the firing gun's `kill_effect` cosmetic slot id.
  - Headshot kills with `creeper_birthday_party_badge` spawn confetti/explosion particles and play `item.kill_effect.birthday_party`.
  - Headshot kills with `headpoppper_badge` apply the `popped` kill effect, which emits popcorn particles and beehive pop sounds while ticking.
  - Headshot kills with `trickshot_badge` apply the `trickshotted` kill effect, which emits hit-marker particles and Forge-style hit/air-horn/goose sounds while ticking.
- Dye cosmetic behavior is partially wired for `flare_gun`:
  - Bullets carry the firing gun's `dye` cosmetic slot color when the gun is `flare_gun`.
  - Flare smoke uses `colored_flare_smoke` with the dye's firework RGB color when a dye is installed.
  - Flare guns without a dye keep the existing default red flare smoke.

Still to port:

- Forge-accurate attachment screen config/medal buttons and remaining layout polish.
- Positional model rendering for non-scope attachments. The copied Forge models/textures are present, but barrel, stock, under-barrel, magazine, and special attachment models still need the equivalent of Forge's attachment-position/scale data before they can be rendered independently without overlapping or mounting at the wrong point.
- Remaining runtime behavior for standalone flashlight item charging, laser pointer beam particles, custom trumpet sonic-ring visuals, and full attachment item-stack durability/enchantment parity.
- Decide whether the separate NeoForge loaded `MagazineItem` ammo containers need extended/drum variants or scaling. Current behavior changes the gun capacity, while existing loaded magazine items keep their own fixed container capacities.
- Cosmetic slots: attachment dye/render behavior beyond flare smoke, attachment paint-job rendering, any Forge paint-job model overrides, and runtime validation of kill-effect/dye visuals.

Sync checklist for the other maintained branches:

1. Port `AttachmentType`, `AttachmentModifiers`, `AttachmentItem`, `GunAttachments`, and `GunAttachmentRules`.
2. Add equivalent persistent/network-synced gun attachment components.
3. Register the same attachment items and add them to combat creative tab/manual recipes.
4. Copy item definitions, item models, textures, and recipes.
5. Rewire bolt-action scoped ADS to read the stack's scope slot instead of a global flag.
6. Rewire silencer sound behavior through the barrel slot.
7. Port `AttachmentMenu`, `AttachmentScreen`, `OpenAttachmentsPayload`, and the attachment keybinding.
8. Port the combined runtime modifier helper and wire damage, spread, recoil/kick, ADS FOV, and ADS speed through the active gameplay/client paths.
9. Port magazine capacity modifiers and update reload, tooltip, and HUD cap display.
10. Port barrel attachment fire side effects for trumpet/explosive muzzle audio, explosive muzzle gun wear, and explosive muzzle block interaction.
11. Port dynamic-light infrastructure plus flashlight/laser server tick behavior.
12. Port attachment durability/breakage for the firing-damaged functional slots.
13. Port trumpet soundwave gameplay behavior with adapted vanilla particles.
14. Port scope attachment model rendering with paint-job texture fallback.
15. Then expand remaining runtime modifier/render behavior.
