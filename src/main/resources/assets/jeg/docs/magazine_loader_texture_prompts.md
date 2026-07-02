# Magazine Loader Texture Prompt

Create directly usable Minecraft Java block textures for `jeg:magazine_loader`.

Output requirements:

- Produce separate square 16x16 pixel-art PNG textures, one image per block face.
- No transparency. Every pixel must be opaque.
- No text, letters, numbers, logos, watermarks, labels, UI icons, or item overlays.
- Use hard pixel edges only. No blur, no antialiasing, no smooth gradients, no photorealism.
- Keep the design readable at native 16x16 scale, with low noise and clear silhouette.
- Match vanilla Minecraft block texture style: simple shaded pixels, 2-4 shade ramps, crisp metal panels.
- Palette: dark gunmetal gray, iron gray, black shadow pixels, copper/brass bullet accents, tiny redstone-red details, tiny green indicator light only on the front/active front.
- The six faces must align visually as one cube: same outer metal frame thickness, same corner rivets, consistent lighting from upper left.

Suggested output file names:

- `magazine_loader_front.png`
- `magazine_loader_front_active.png`
- `magazine_loader_back.png`
- `magazine_loader_left.png`
- `magazine_loader_right.png`
- `magazine_loader_top.png`
- `magazine_loader_bottom.png`

Face designs:

- Front face, `magazine_loader_front.png`: This is the only main working face. Use a dark metal outer frame around all edges, about 1 pixel thick. Put a vertical black magazine slot/tray in the lower center, about 4 pixels wide and 6 pixels tall. Add a small horizontal brass bullet feed channel entering from the upper left toward the center, using 2-3 brass pixels that look like rounds. Add a compact loading press or piston-like metal plate in the center-right. Put one tiny green indicator light near the lower right, 1-2 pixels, dim but visible. The front must look busier and more functional than every other face.
- Active front face, `magazine_loader_front_active.png`: Same layout as the normal front so it aligns perfectly. Make the green indicator brighter, add 1-2 brighter brass pixels in the feed channel, and make the lower magazine tray look occupied by a filled magazine. Do not change the overall silhouette or frame.
- Back face, `magazine_loader_back.png`: Plain reinforced machine back. Use the same outer frame, but no magazine slot, no brass rounds, no green light. Add a central rectangular maintenance panel, darker than the frame, with 2-4 rivets. This face should clearly not be the working face.
- Left face, `magazine_loader_left.png`: Dark gunmetal side casing. Use the same edge frame. Add a narrow horizontal feed-belt strip running from back toward front across the middle, with very subtle brass/copper hints. Add 2-3 rivets and one panel seam. Keep it simpler than the front.
- Right face, `magazine_loader_right.png`: Mirrored side casing but not identical to the left. Use the same edge frame. Add a small redstone-red wiring detail or vent near the rear half, plus a simple dark panel seam. No bullet tray and no green light.
- Top face, `magazine_loader_top.png`: Two input hatches on the top surface. Left hatch should suggest loose-round input, with a tiny brass/copper slit or two brass pixels. Right hatch should suggest magazine input, a dark rectangular slot. Add directional metal panels leading toward the front edge so players can tell orientation. Keep the corners and border consistent with the side faces.
- Bottom face, `magazine_loader_bottom.png`: Simple underside. Mostly dark metal with a reinforced square plate in the center and four dark bolt pixels near the corners. No functional details, no brass, no green/red lights.

Negative prompt:

Do not create a full 3D render, isometric block, inventory icon, sprite sheet with padding, GUI texture, high-resolution illustration, realistic metal, rounded modern machinery, glowing neon panels, unreadable clutter, text labels, or a front/back design that looks the same.
