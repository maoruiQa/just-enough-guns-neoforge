# NeoForge 1.21.11 First-Person Compensation Pipeline Report

## Scope
This report documents the effective first-person offset/compensation pipeline in NeoForge `1.21.11`, with emphasis on why visual parity cannot be inferred from `GunItemClientExtensions` alone.

## Executive Summary
- `1.21.11` does **not** use a legacy category/per-gun compensation table inside `GunItemClientExtensions`.
- Effective first-person result is still multi-layer:
  - gun pose profile interpolation,
  - ADS uplift,
  - arm-bone transform overrides,
  - animation bone tracks,
  - first-person special item render path.
- Therefore, parity backporting to `1.21.1` must treat compensation as a pipeline problem, not a single constant/table problem.

## Layer 1: Gun Transform (Profile + ADS)
- File: `src/main/java/ttv/migami/jeg/client/GunItemClientExtensions.java`
- Key points:
  - `ADS_LIFT_Y = 0.03F`.
  - Uses `GunPoseProfile.forGun(...)`.
  - Interpolates `hipX/Y/Z -> adsX/Y/Z` by ADS progress.
  - Applies final translate as:
    - `translate(direction * xOffset, yOffset + ads * ADS_LIFT_Y, zOffset)`.

This is the base item pose layer for first person.

## Layer 2: Per-Gun Arm Transform
- File: `src/main/java/ttv/migami/jeg/client/render/gun/layer/GunFirstPersonArmsLayer.java`
- Key points:
  - First-person only arm rendering from GeckoLib arm bones.
  - Applies profile arm transform (`tx/ty/tz`, `rx/ry/rz`, `sx/sy/sz`) per side before hand render.
  - Uses direct player hand render (`renderLeftHand` / `renderRightHand`), no generic event fallback in `1.21.11`.

This layer can significantly change perceived weapon posture because arm framing influences visual reference.

## Layer 3: Animation Bone Tracks
- Files:
  - `src/main/resources/assets/jeg/animations/item/minigun.animation.json`
  - `src/main/resources/assets/jeg/animations/item/rocket_launcher.animation.json`
- Key points:
  - Animation files drive `gun_body` and arm bones each frame.
  - Example:
    - `minigun` has `barrel` spin and distinct arm tracks.
    - `rocket_launcher` has `missile/latch` tracks and different arm trajectories.

Animation curves are an additional dynamic offset layer, not represented by static profile numbers.

## Layer 4: Item Render Path Baseline
- Files:
  - `src/main/resources/assets/jeg/items/minigun.json`
  - `src/main/resources/assets/jeg/items/rocket_launcher.json`
  - `src/main/resources/assets/jeg/items/abstract_gun.json`
- Key points:
  - Uses `minecraft:select` by `minecraft:display_context`.
  - First person (`firstperson_righthand`, `firstperson_lefthand`) routes to `minecraft:special` (`geckolib:geckolib`).
  - Non-first-person routes to fallback `minecraft:model`.

This split path changes baseline behavior between first-person and fallback contexts.

## Important Negative Finding
- File: `src/main/java/ttv/migami/jeg/client/FirstPersonGunArmRenderEvents.java`
- `onRenderHand` returns immediately in `1.21.11`.
- So event-based vanilla arm overlay is intentionally disabled in this branch.

This avoids a second arm pipeline that can flatten weapon-specific motion.

## Why This Matters For 1.21.1 Parity
Backport parity must align:
1. first-person render path behavior,
2. arm rendering source (bone-driven, no event overlay by default),
3. profile + ADS transform,
4. animation-driven posture.

Only after those are aligned should final per-gun compensation be tuned.

## Practical Migration Constraints
- Do not assume one compensation table can replace animation and arm-bone differences.
- Use a fixed reference weapon (`combat_rifle`) as a parity anchor, then compute per-gun compensation after pipeline alignment.
- Validate heavy weapon pairs (`minigun` vs `rocket_launcher`) after disabling any fallback arm overlay path.
