package ttv.migami.jeg.gun;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.BulletproofArmorItem;

public final class BallisticProtection {
    public static final IntrinsicArmorProfile BOUND_TERROR_PHANTOM = new IntrinsicArmorProfile(4.60F, 0.20F, 0.72F);

    private static final ResourceLocation HANDMADE_SHELL = Reference.id("handmade_shell");
    private static final ResourceLocation SHOTGUN_SHELL = Reference.id("shotgun_shell");
    private static final ResourceLocation PISTOL_AMMO = Reference.id("pistol_ammo");
    private static final ResourceLocation SPECTRE_ROUND = Reference.id("spectre_round");
    private static final ResourceLocation BLAZE_ROUND = Reference.id("blaze_round");
    private static final ResourceLocation RIFLE_AMMO = Reference.id("rifle_ammo");
    private static final ResourceLocation AUTOCANNON_SHELL = Reference.id("autocannon_shell");
    private static final ResourceLocation ECHO_SHARD = ResourceLocation.fromNamespaceAndPath("minecraft", "echo_shard");
    private static final ResourceLocation SCULK_CATALYST = ResourceLocation.fromNamespaceAndPath("minecraft", "sculk_catalyst");
    private static final ResourceLocation SUBSONIC_RIFLE = Reference.id("subsonic_rifle");
    private static final ResourceLocation SUPERSONIC_SHOTGUN = Reference.id("supersonic_shotgun");
    private static final ResourceLocation HYPERSONIC_CANNON = Reference.id("hypersonic_cannon");
    private static final ResourceLocation ROCKET_LAUNCHER = Reference.id("rocket_launcher");

    private BallisticProtection() {
    }

    public static float baseArmorPiercing(GunStats stats, boolean rocketDirectHit) {
        if (rocketDirectHit) {
            return 10.0F;
        }
        if (stats == null) {
            return 1.0F;
        }
        if (isSonic(stats)) {
            return 7.0F;
        }

        ResourceLocation ammo = stats.ammoItem();
        if (HANDMADE_SHELL.equals(ammo)) {
            return 0.60F;
        }
        if (SHOTGUN_SHELL.equals(ammo)) {
            return 1.00F;
        }
        if (PISTOL_AMMO.equals(ammo)) {
            return 2.00F;
        }
        if (SPECTRE_ROUND.equals(ammo) || BLAZE_ROUND.equals(ammo)) {
            return 3.00F;
        }
        if (RIFLE_AMMO.equals(ammo)) {
            return 4.00F;
        }
        if (AUTOCANNON_SHELL.equals(ammo)) {
            return 6.50F;
        }
        return 1.00F;
    }

    public static float gunArmorPiercingMultiplier(GunStats stats) {
        if (stats == null) {
            return 1.0F;
        }
        String path = stats.id().getPath();
        return switch (path) {
            case "combat_pistol" -> 2.60F;
            case "combat_rifle" -> 1.24F;
            case "burst_rifle" -> 0.90F;
            case "light_machine_gun" -> 1.10F;
            case "vehicle_30mm_cannon" -> 3.00F;
            default -> 1.00F;
        };
    }

    public static float effectiveArmorPiercing(GunStats stats, boolean rocketDirectHit) {
        float override = explicitArmorPiercing(stats);
        if (override >= 0.0F) {
            return override;
        }
        return baseArmorPiercing(stats, rocketDirectHit) * gunArmorPiercingMultiplier(stats);
    }

    private static float explicitArmorPiercing(GunStats stats) {
        if (stats == null) {
            return -1.0F;
        }
        return switch (stats.id().getPath()) {
            case "vehicle_20mm_cannon" -> 5.20F;
            case "vehicle_30mm_cannon" -> 9.80F;
            case "vehicle_70mm_rocket", "vehicle_80mm_rocket" -> 5.50F;
            case "vehicle_9m336_missile" -> 8.40F;
            case "vehicle_bmp2_missile" -> 10.80F;
            case "vehicle_9m120_driver_missile", "vehicle_9m120_passenger_missile" -> 11.80F;
            case "vehicle_kh39_missile" -> 13.20F;
            default -> -1.0F;
        };
    }

    public static BallisticResult applyToArmorHit(float rawDamage, GunStats stats, ItemStack armorStack, EquipmentSlot slot, boolean rocketDirectHit) {
        if (!(armorStack.getItem() instanceof BulletproofArmorItem armorItem)) {
            return BallisticResult.unmodified(rawDamage);
        }
        ArmorProfile profile = armorProfile(armorItem.tier(), slot);
        return apply(rawDamage, effectiveArmorPiercing(stats, rocketDirectHit), profile);
    }

    public static BallisticResult applyToIntrinsicArmor(float rawDamage, GunStats stats, IntrinsicArmorProfile profile, boolean rocketDirectHit) {
        return apply(rawDamage, effectiveArmorPiercing(stats, rocketDirectHit), profile);
    }

    public static ArmorProfile armorProfile(BulletproofArmorItem.Tier tier, EquipmentSlot slot) {
        float slotDurabilityScale = slot == EquipmentSlot.HEAD ? 1.15F : 1.00F;
        return new ArmorProfile(
                effectiveArmorRating(tier, slot),
                tier.undermatchMultiplier(),
                tier.overmatchMultiplier(),
                slotDurabilityScale,
                tier.durabilityScale()
        );
    }

    public static float effectiveArmorRating(BulletproofArmorItem.Tier tier, EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? tier.ballisticRating() * 0.80F : tier.ballisticRating();
    }

    public static int projectileLifeOverride(GunStats stats) {
        return -1;
    }

    public static boolean isRocketDirectHit(GunStats stats) {
        return stats != null && ROCKET_LAUNCHER.equals(stats.id());
    }

    public static boolean isSonic(GunStats stats) {
        if (stats == null) {
            return false;
        }
        ResourceLocation id = stats.id();
        ResourceLocation ammo = stats.ammoItem();
        return SUBSONIC_RIFLE.equals(id)
                || SUPERSONIC_SHOTGUN.equals(id)
                || HYPERSONIC_CANNON.equals(id)
                || ECHO_SHARD.equals(ammo)
                || SCULK_CATALYST.equals(ammo);
    }

    private static BallisticResult apply(float rawDamage, float effectiveArmorPiercing, IntrinsicArmorProfile profile) {
        return apply(rawDamage, effectiveArmorPiercing, profile.rating(), profile.undermatchMultiplier(), profile.overmatchMultiplier());
    }

    private static BallisticResult apply(float rawDamage, float effectiveArmorPiercing, float rating, float undermatchMultiplier, float overmatchMultiplier) {
        if (rawDamage <= 0.0F || rating <= 0.0F) {
            return BallisticResult.unmodified(rawDamage);
        }

        float apRatio = effectiveArmorPiercing / rating;
        boolean overmatched = effectiveArmorPiercing >= rating;
        float damageMultiplier;
        if (overmatched) {
            damageMultiplier = Mth.clamp(apRatio, 1.00F, 1.50F) * overmatchMultiplier;
            damageMultiplier = Math.min(damageMultiplier, 0.95F);
        } else {
            damageMultiplier = Mth.clamp(apRatio, 0.05F, 0.95F) * undermatchMultiplier;
        }

        return new BallisticResult(rawDamage * damageMultiplier, 0, true, overmatched);
    }

    private static BallisticResult apply(float rawDamage, float effectiveArmorPiercing, ArmorProfile profile) {
        BallisticResult damageResult = apply(rawDamage, effectiveArmorPiercing, profile.rating(), profile.undermatchMultiplier(), profile.overmatchMultiplier());
        if (!damageResult.armorApplied()) {
            return damageResult;
        }

        float pressure = effectiveArmorPiercing / profile.rating();
        float durabilityMultiplier;
        if (effectiveArmorPiercing < profile.rating()) {
            durabilityMultiplier = 0.35F + pressure * 0.65F;
        } else {
            durabilityMultiplier = 1.00F + Math.min(pressure - 1.00F, 1.00F) * 1.25F;
        }

        int durabilityDamage = Mth.clamp(
                Mth.ceil(rawDamage * durabilityMultiplier * profile.slotDurabilityScale() * profile.tierDurabilityScale()),
                1,
                40
        );
        return new BallisticResult(damageResult.finalDamage(), durabilityDamage, true, damageResult.overmatched());
    }

    public record IntrinsicArmorProfile(float rating, float undermatchMultiplier, float overmatchMultiplier) {
    }

    public record ArmorProfile(
            float rating,
            float undermatchMultiplier,
            float overmatchMultiplier,
            float slotDurabilityScale,
            float tierDurabilityScale
    ) {
    }

    public record BallisticResult(float finalDamage, int durabilityDamage, boolean armorApplied, boolean overmatched) {
        private static BallisticResult unmodified(float rawDamage) {
            return new BallisticResult(rawDamage, 0, false, false);
        }
    }
}
