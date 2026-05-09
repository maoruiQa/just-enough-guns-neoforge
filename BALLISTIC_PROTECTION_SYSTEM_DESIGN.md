# Ballistic Protection System Design

## Scope

Design a unified ballistic protection system for:

- `bound_terror_phantom`, replacing its current low-damage immunity behavior.
- Bulletproof helmets and vests, replacing the current protection behavior that mostly maps tiers to Projectile Protection levels.

This document is a design plan only. It does not change gameplay code.

## Assumptions

- The system is server-authoritative and runs only when bullet or direct rocket damage is applied.
- "Bulletproof gear" means `BulletproofArmorItem` helmets and vests.
- Helmet protects head hits; vest protects torso/body hits. If the current hit logic cannot identify body parts yet, the first implementation should use a simple fallback: head slot only for confirmed headshots, otherwise chest slot.
- Rocket explosion splash damage should not use the highest armor-piercing value. Only rocket direct-hit damage receives the highest armor-piercing value.
- Sonic weapons are the `echo_shard` weapons currently represented by `subsonic_rifle`, `supersonic_shotgun`, and `hypersonic_cannon`.
- `GunDefinitions.java` is generated. Final implementation should update the generator or add a non-generated helper table keyed by gun/ammo id instead of hand-editing generated output.

## Goals

1. Every bullet has a base armor-piercing value determined by ammo type.
2. Specific guns can multiply the base armor-piercing value.
3. Every bulletproof armor piece has:
   - a ballistic rating,
   - an undermatch damage multiplier,
   - an overmatch damage multiplier,
   - durability loss tuning.
4. If bullet armor-piercing is lower than armor rating, damage is heavily reduced but not always zero.
5. If bullet armor-piercing is equal to or higher than armor rating, damage still receives partial mitigation based on armor quality, but the armor is punished harder.
6. No wearable armor rating reaches sonic weapon armor-piercing. Sonic weapons therefore remain reliable anti-armor weapons, balanced by greatly reduced range.
7. `bound_terror_phantom` uses the same armor-piercing comparison model instead of ignoring low-damage hits.

## Armor-Piercing Scale

Use a float scale. Larger values mean better armor-piercing.

| Source | Base AP |
| --- | ---: |
| `handmade_shell` | 0.60 |
| `shotgun_shell` | 1.00 |
| `pistol_ammo` | 2.00 |
| `spectre_round` | 3.00 |
| `blaze_round` | 3.00 |
| `rifle_ammo` | 4.00 |
| Sonic ammo or sonic weapon projectile | 7.00 |
| Rocket direct hit | 10.00 |

Notes:

- Handmade shell stays clearly below normal shotgun shells.
- Spectre and blaze rounds sit between pistol and rifle rounds.
- Sonic AP is above every wearable armor tier.
- Rocket direct hit sits above sonic AP, but only for direct impact damage.

## Gun AP Multipliers

Apply gun multipliers after base ammo AP:

`effectiveAP = baseAmmoAP * gunApMultiplier`

| Gun | Multiplier | Reason |
| --- | ---: | --- |
| `combat_pistol` | 0.75 | User requested a large penetration reduction. It still fires pistol ammo, but should not be the best sidearm against armor. |
| `combat_rifle` | 0.70 | User requested a large penetration reduction. Its damage is already low, so damage can be tuned separately without giving it strong AP. |
| `burst_rifle` | 0.90 | Slight penetration reduction. |
| `light_machine_gun` | 1.10 | Slight penetration increase. |
| All other guns | 1.00 | Default behavior. |

Optional follow-up balance:

- If `combat_rifle` feels too weak after AP reduction, raise raw damage slightly instead of restoring AP. This keeps its intended weakness against high-tier armor.

## Sonic Weapon Range Nerf

Because no armor can exceed sonic AP, sonic weapons need sharply reduced reach.

Recommended projectile life changes:

| Gun | Current observed life | Recommended life | Result |
| --- | ---: | ---: | --- |
| `subsonic_rifle` | 32 | 12 | Short anti-armor rifle range. |
| `supersonic_shotgun` | 14 | 8 | Very short anti-armor shotgun burst. |
| `hypersonic_cannon` | 20 | 10 | Strong but risky close/mid-range anti-armor shot. |

If projectile speed is also used as practical range, keep speed unchanged first. Reducing life is simpler and easier to verify.

## Armor Ratings

Armor rating must stay below sonic AP `7.00`.

| Armor tier | Rating | Required relationship |
| --- | ---: | --- |
| I | 2.10 | At least blocks pistol AP `2.00`. |
| II | 3.10 | Blocks spectre/blaze AP `3.00`. |
| III | 3.60 | Slightly above tier II, below rifle AP `4.00`. |
| IV | 4.10 | Blocks rifle AP `4.00`. |
| V | 5.20 | Above rifle, below sonic. |
| VI | 6.20 | Above tier V, below sonic. |

This satisfies:

- Tier I >= pistol.
- Tier II >= spectre/blaze.
- Tier III > tier II and < rifle.
- Tier IV >= rifle.
- Tier V and VI > rifle and < sonic.
- Tier VI > tier V.

## Armor Quality Multipliers

Each armor piece has two quality multipliers:

- `undermatchMultiplier`: used when `effectiveAP < armorRating`. This should be low.
- `overmatchMultiplier`: used when `effectiveAP >= armorRating`. This should be higher, but still below `1.0`.

| Armor tier | Undermatch multiplier | Overmatch multiplier |
| --- | ---: | ---: |
| I | 0.30 | 0.85 |
| II | 0.25 | 0.80 |
| III | 0.22 | 0.76 |
| IV | 0.18 | 0.70 |
| V | 0.15 | 0.64 |
| VI | 0.12 | 0.58 |

This lets higher tiers be better even when the bullet overmatches the armor.

## Damage Algorithm

Inputs:

- `rawDamage`: incoming bullet or direct rocket damage.
- `effectiveAP`: base ammo AP multiplied by gun AP multiplier.
- `armorRating`: ballistic rating of the hit armor piece.
- `undermatchMultiplier`: armor quality value for blocked shots.
- `overmatchMultiplier`: armor quality value for penetrating shots.

Steps:

```text
apRatio = effectiveAP / armorRating

if effectiveAP < armorRating:
    damageMultiplier = clamp(apRatio, 0.05, 0.95) * undermatchMultiplier
else:
    damageMultiplier = clamp(apRatio, 1.00, 1.50) * overmatchMultiplier
    damageMultiplier = min(damageMultiplier, 0.95)

finalDamage = rawDamage * damageMultiplier
```

Important behavior:

- Under-penetrating bullets scale up as they get closer to the armor rating.
- Over-penetrating bullets scale up when AP exceeds armor rating, but armor can still reduce damage.
- The final overmatch multiplier is capped below full damage so high-quality armor always matters.

## Example Damage Outcomes

Assume `rawDamage = 10`.

| Hit | AP vs armor | Calculation | Final damage |
| --- | --- | --- | ---: |
| Pistol vs tier I | `2.00 / 2.10 = 0.95` undermatch | `10 * 0.95 * 0.30` | 2.86 |
| Combat pistol vs tier I | `1.50 / 2.10 = 0.71` undermatch | `10 * 0.71 * 0.30` | 2.14 |
| Spectre vs tier II | `3.00 / 3.10 = 0.97` undermatch | `10 * 0.97 * 0.25` | 2.42 |
| Rifle vs tier III | `4.00 / 3.60 = 1.11` overmatch | `10 * 1.11 * 0.76` | 8.44 |
| Rifle vs tier IV | `4.00 / 4.10 = 0.98` undermatch | `10 * 0.98 * 0.18` | 1.76 |
| LMG vs tier IV | `4.40 / 4.10 = 1.07` overmatch | `10 * 1.07 * 0.70` | 7.51 |
| Sonic vs tier VI | `7.00 / 6.20 = 1.13` overmatch | `10 * 1.13 * 0.58` | 6.55 |
| Rocket direct vs tier VI | `10.00 / 6.20 = 1.61`, clamped to `1.50` | `10 * 1.50 * 0.58`, capped below 1x | 8.70 |

## Durability Loss Algorithm

Armor durability loss should scale with both raw damage and AP pressure.

Inputs:

- `rawDamage`
- `effectiveAP`
- `armorRating`
- `slotDurabilityScale`: helmet `1.15`, vest `1.00`
- `tierDurabilityScale`: optional quality tuning per tier

Recommended tier durability scale:

| Tier | Durability scale |
| --- | ---: |
| I | 1.20 |
| II | 1.10 |
| III | 1.00 |
| IV | 0.90 |
| V | 0.80 |
| VI | 0.70 |

Formula:

```text
pressure = effectiveAP / armorRating

if effectiveAP < armorRating:
    durabilityMultiplier = 0.35 + pressure * 0.65
else:
    durabilityMultiplier = 1.00 + min(pressure - 1.00, 1.00) * 1.25

durabilityDamage = ceil(rawDamage * durabilityMultiplier * slotDurabilityScale * tierDurabilityScale)
durabilityDamage = clamp(durabilityDamage, 1, 40)
```

Behavior:

- Armor still loses durability when it successfully blocks a shot.
- Overmatching shots damage armor much harder.
- The cap prevents rockets or very high damage shots from deleting fresh high-tier armor in one hit unless that is explicitly desired later.

## Bound Terror Phantom Rating

`bound_terror_phantom` should use the same AP model as an intrinsic armor layer.

Recommended values:

```text
boundTerrorPhantomArmorRating = 4.60
boundTerrorPhantomUndermatchMultiplier = 0.20
boundTerrorPhantomOvermatchMultiplier = 0.72
boundTerrorPhantomDurability = none
```

This makes it:

- Strong against handmade shells, shotgun shells, pistol ammo, spectre/blaze rounds, and normal rifle ammo.
- Still meaningfully vulnerable to light machine gun rifle fire because LMG AP becomes `4.40`, close to its rating.
- Clearly vulnerable to sonic weapons and rocket direct hits.
- No longer immune to low-damage bullets. Low-damage hits can still land, but the AP comparison controls how much damage gets through.

The existing `MIN_DAMAGE_TO_HURT` check should be removed for `bound_terror_phantom`. If normal `terror_phantom` still needs low-damage filtering, keep that behavior only on the normal variant, not in shared `AbstractTerrorPhantom` logic.

## Implementation Shape

Recommended new shared helper:

```text
ttv.migami.jeg.gun.BallisticProtection
```

Responsibilities:

- Resolve base AP from `GunStats.ammoItem()`.
- Resolve gun AP multiplier from `GunStats.id()`.
- Detect direct rocket hit.
- Resolve armor rating and quality multipliers from `BulletproofArmorItem.Tier`.
- Compute final damage multiplier.
- Compute armor durability damage.
- Expose a small result object:

```text
record BallisticResult(float finalDamage, int durabilityDamage, boolean armorApplied, boolean overmatched)
```

Suggested data methods:

```text
baseArmorPiercing(GunStats stats, boolean rocketDirectHit)
gunArmorPiercingMultiplier(GunStats stats)
effectiveArmorPiercing(GunStats stats, boolean rocketDirectHit)
armorProfile(BulletproofArmorItem.Tier tier)
applyToArmorHit(float rawDamage, GunStats stats, ItemStack armorStack, boolean rocketDirectHit)
applyToIntrinsicArmor(float rawDamage, GunStats stats, IntrinsicArmorProfile profile, boolean rocketDirectHit)
```

## Hit Location Integration

Preferred:

- Add or reuse a hit-location classifier in `BulletEntity` when a bullet hits a `LivingEntity`.
- Compare hit Y against target bounding box:
  - top 25%: head
  - middle 55%: chest
  - lower 20%: no bulletproof gear unless leggings are added later

Fallback:

- If the target has a bulletproof helmet and the hit is a confirmed headshot, use helmet.
- Otherwise, if the target has a bulletproof vest, use vest.
- Otherwise, no ballistic armor applies.

## Current-Code Replacement Points

Observed in `Just-Enough-Guns-Fabric-26.1`:

- `BulletproofArmorItem.Tier` currently has `projectileLevel`; replace or supplement it with ballistic rating and quality multipliers.
- `AbstractTerrorPhantom` currently contains `MIN_DAMAGE_TO_HURT = 2.0F`; do not use that for `bound_terror_phantom`.
- `AbstractTerrorPhantom.applyDefaultProjectileProtectionReduction` currently applies a vanilla-like Projectile Protection reduction to normal `terror_phantom`; keep separate from the new bound phantom ballistic layer unless normal terror phantom is intentionally migrated later.
- `GunStats` currently does not carry AP. Add AP through a helper table first to avoid expanding the generated constructor everywhere.

## Verification Plan

1. Unit-style helper tests, if a test tree is added:
   - handmade shell AP < shotgun AP < pistol AP < spectre/blaze AP < rifle AP < sonic AP < rocket direct AP.
   - all armor ratings are below sonic AP.
   - tier VI rating > tier V rating.
   - combat pistol/rifle lower effective AP than their base ammo.
   - LMG effective AP is above base rifle AP.
2. Gameplay smoke tests:
   - Tier I vest strongly reduces pistol ammo.
   - Tier II vest strongly reduces spectre/blaze rounds.
   - Tier III vest is penetrated by rifle ammo.
   - Tier IV vest strongly reduces rifle ammo.
   - Tier VI vest is still penetrated by sonic weapons.
   - Rocket direct hit remains the highest AP hit.
   - `bound_terror_phantom` takes reduced but nonzero low-damage bullet hits.
3. Balance checks:
   - Sonic weapons feel strong against armor but are constrained by reduced projectile life.
   - `combat_rifle` raw damage can be increased separately if it becomes too weak after its AP reduction.
