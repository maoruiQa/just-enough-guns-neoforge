package ttv.migami.jeg.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.gun.BallisticProtection;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModSounds;

public final class HappyGhastArmorHelper {
    private static final float AUTO_REPAIR_PER_TICK = 0.02F;
    private static final int AUTO_REPAIR_COOLDOWN_TICKS = 140;

    private HappyGhastArmorHelper() {}

    public static boolean isArmoredHarness(ItemStack stack) {
        return stack.getItem() instanceof ArmoredJoyHarnessItem;
    }

    public static float getPlating(ItemStack stack) {
        float stored = stack.getOrDefault(ModDataComponents.ARMORED_HARNESS_PLATING.get(), 0.0F);
        float max = getMaxPlating(stack);
        return max > 0.0F ? Math.min(stored, max) : stored;
    }

    public static void setPlating(ItemStack stack, float value) {
        float max = getMaxPlating(stack);
        if (max <= 0.0F) {
            return;
        }
        float clamped = Mth.clamp(value, 0.0F, max);
        stack.set(ModDataComponents.ARMORED_HARNESS_PLATING.get(), clamped);
    }

    static float removePlating(ItemStack stack, float value) {
        float current = getPlating(stack);
        float result = Math.max(0.0F, current - value);
        setPlating(stack, result);
        return result;
    }

    public static ArmorDamageResult applyIncomingDamage(HappyGhast ghast, DamageSource source, float amount) {
        ItemStack harness = ghast.getItemBySlot(EquipmentSlot.BODY);
        if (!isArmoredHarness(harness) || amount <= 0.0F) {
            return new ArmorDamageResult(amount, false);
        }

        float plating = getPlating(harness);
        if (plating <= 0.0F) {
            return new ArmorDamageResult(amount, false);
        }

        float armorDamage = amount;
        boolean penetrated = true;
        if (source.getDirectEntity() instanceof BulletEntity bullet && harness.getItem() instanceof ArmoredJoyHarnessItem armorItem) {
            BallisticProtection.BallisticResult result = BallisticProtection.applyToIntrinsicArmor(
                    amount,
                    bullet.getGunStats(),
                    new BallisticProtection.IntrinsicArmorProfile(
                            armorItem.tier().armorRating(),
                            armorItem.tier().undermatchMultiplier(),
                            armorItem.tier().overmatchMultiplier()
                    ),
                    BallisticProtection.isRocketDirectHit(bullet.getGunStats())
            );
            armorDamage = result.finalDamage();
            penetrated = !result.armorApplied() || result.overmatched();
        }

        float absorbed = Math.min(plating, armorDamage);
        float remaining = armorDamage - absorbed;
        removePlating(harness, absorbed);
        setRepairCooldown(harness, AUTO_REPAIR_COOLDOWN_TICKS);
        syncAbsorption(ghast);
        playArmorHitSound(ghast, penetrated || remaining > 0.0F);
        return new ArmorDamageResult(remaining, true);
    }

    static boolean repairWithTool(HappyGhast ghast) {
        ItemStack harness = ghast.getItemBySlot(EquipmentSlot.BODY);
        float max = getMaxPlating(harness);
        if (max <= 0.0F) {
            return false;
        }
        float plating = getPlating(harness);
        if (plating >= max) {
            return false;
        }
        setPlating(harness, plating + 0.5F + 0.0025F * max);
        setRepairCooldown(harness, 0);
        syncAbsorption(ghast);
        return true;
    }

    static boolean tickAutoRepair(HappyGhast ghast) {
        ItemStack harness = ghast.getItemBySlot(EquipmentSlot.BODY);
        float max = getMaxPlating(harness);
        if (max <= 0.0F) {
            return false;
        }
        int cooldown = getRepairCooldown(harness);
        if (cooldown > 0) {
            setRepairCooldown(harness, cooldown - 1);
            return false;
        }
        float plating = getPlating(harness);
        if (plating >= max) {
            return false;
        }
        setPlating(harness, plating + AUTO_REPAIR_PER_TICK);
        syncAbsorption(ghast);
        return true;
    }

    static void syncAbsorption(HappyGhast ghast) {
        ItemStack stack = ghast.getItemBySlot(EquipmentSlot.BODY);
        if (isArmoredHarness(stack)) {
            ghast.setAbsorptionAmount(getPlating(stack));
        } else if (ghast.getAbsorptionAmount() > 0.0F) {
            ghast.setAbsorptionAmount(0.0F);
        }
    }

    static boolean hasHarnessEquipped(HappyGhast ghast) {
        return isArmoredHarness(ghast.getItemBySlot(EquipmentSlot.BODY));
    }

    public static float getMaxPlating(ItemStack stack) {
        if (stack.getItem() instanceof ArmoredJoyHarnessItem harness) {
            return harness.getMaxPlating();
        }
        return 0.0F;
    }

    private static int getRepairCooldown(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ARMORED_HARNESS_REPAIR_COOLDOWN.get(), 0);
    }

    private static void setRepairCooldown(ItemStack stack, int value) {
        stack.set(ModDataComponents.ARMORED_HARNESS_REPAIR_COOLDOWN.get(), Math.max(0, value));
    }

    private static void playArmorHitSound(HappyGhast ghast, boolean penetrated) {
        var holder = ModSounds.ALL.get(Reference.id("block.hit.metal"));
        SoundEvent sound = holder == null ? SoundEvents.ANVIL_LAND : holder.get();
        ghast.level().playSound(null, ghast, sound, SoundSource.PLAYERS, 1.0F, penetrated ? 0.85F : 1.2F);
    }

    public record ArmorDamageResult(float finalDamage, boolean armorHit) {
    }
}
