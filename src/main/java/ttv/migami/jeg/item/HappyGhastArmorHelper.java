package ttv.migami.jeg.item;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.init.ModDataComponents;

final class HappyGhastArmorHelper {
    private HappyGhastArmorHelper() {}

    static boolean isArmoredHarness(ItemStack stack) {
        return stack.getItem() instanceof ArmoredJoyHarnessItem;
    }

    static float getPlating(ItemStack stack) {
        float stored = stack.getOrDefault(ModDataComponents.ARMORED_HARNESS_PLATING.get(), 0.0F);
        float max = getMaxPlating(stack);
        return max > 0.0F ? Math.min(stored, max) : stored;
    }

    static void setPlating(ItemStack stack, float value) {
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

    static float getMaxPlating(ItemStack stack) {
        if (stack.getItem() instanceof ArmoredJoyHarnessItem harness) {
            return harness.getMaxPlating();
        }
        return 0.0F;
    }
}
