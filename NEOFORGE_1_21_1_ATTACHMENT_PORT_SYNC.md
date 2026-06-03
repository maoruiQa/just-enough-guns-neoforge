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
- `GunAttachments` is the canonical read/write helper. Future UI, renderer, and gameplay code should use it instead of adding ad-hoc tags.
- `GunAttachmentRules` is the current per-gun support matrix for slot validation.
- Recipes are standard crafting recipes under `src/main/resources/data/jeg/recipes/`.
- Missing item definitions were added under `src/main/resources/assets/jeg/items/`; missing Forge models/textures were copied from Forge 1.20.1.

Behavior wired so far:

- `bolt_action_rifle` only uses the scoped ADS/FOV/overlay path when its scope slot has an attachment.
- A barrel slot `silencer` switches gunfire to the gun's silenced fire sound when available.

Still to port:

- Full attachment menu/container/screen from Forge 1.20.1.
- Client input and C2S open-menu packet for modifying attachments.
- Rendering visibility for installed attachments on every supported gun, not only the current bolt-action built-in scope layer.
- Full modifier application for damage, spread, recoil, ADS speed, magazine capacity, explosive muzzle, flashlight, laser pointer, trumpet, and durability/breakage.
- Cosmetic slots: paint job, dye, and kill effect.

Sync checklist for the other maintained branches:

1. Port `AttachmentType`, `AttachmentModifiers`, `AttachmentItem`, `GunAttachments`, and `GunAttachmentRules`.
2. Add equivalent persistent/network-synced gun attachment components.
3. Register the same attachment items and add them to combat creative tab/manual recipes.
4. Copy item definitions, item models, textures, and recipes.
5. Rewire bolt-action scoped ADS to read the stack's scope slot instead of a global flag.
6. Rewire silencer sound behavior through the barrel slot.
7. Then port UI/menu/network and expand runtime modifier/render behavior.
