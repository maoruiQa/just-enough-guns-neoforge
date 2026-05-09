package ttv.migami.jeg.faction;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.BulletproofArmorItem;

import java.util.EnumSet;

/**
 * Utility class for equipping gunners with appropriate bulletproof armor
 * based on spawn situation and probability distributions.
 */
public class GunnerArmorEquiper {

    /**
     * Equips a gunner with armor based on the spawn situation.
     * HELMET PRIORITY: Always attempts to equip helmet first, then body armor.
     *
     * @param random Random source for probability calculations
     * @param context Armor context containing mob and situation information
     */
    public static void equipGunnerArmor(RandomSource random, GunnerArmorContext context) {
        // HELMET FIRST: Equip helmet with highest priority
        if (random.nextFloat() < getHelmetEquipChance(context)) {
            BulletproofArmorItem.Tier helmetTier = determineHelmetTier(random, context);
            if (helmetTier != null) {
                ItemStack helmet = new ItemStack(ModItems.BULLETPROOF_HELMETS.get(helmetTier).get());
                context.mob.setItemSlot(EquipmentSlot.HEAD, helmet);
            }
        }

        // BODY ARMOR SECOND: Only equip body armor after helmet attempt
        if (random.nextFloat() < getBodyArmorEquipChance(context)) {
            BulletproofArmorItem.Tier bodyTier = determineBodyArmorTier(random, context);
            if (bodyTier != null) {
                ItemStack vest = new ItemStack(ModItems.BULLETPROOF_VESTS.get(bodyTier).get());
                context.mob.setItemSlot(EquipmentSlot.CHEST, vest);
            }
        }

        // Elite gunners and special situations get only helmet and chest armor
        // No leggings or boots will be equipped
    }

    private static BulletproofArmorItem.Tier determineHelmetTier(RandomSource random, GunnerArmorContext context) {
        return randomTier(random, context, context.isElite ? 2 : 1);
    }

    private static BulletproofArmorItem.Tier determineBodyArmorTier(RandomSource random, GunnerArmorContext context) {
        return randomTier(random, context, context.isElite ? 2 : 1);
    }

    private static float getHelmetEquipChance(GunnerArmorContext context) {
        if (context.isElite) return 1.0f;                             // 100% for elites
        float scale = progressionScale(context);
        if (context.isSpecialSituation) return 0.80f + 0.15f * scale;
        return 0.15f + 0.50f * scale;
    }

    private static float getBodyArmorEquipChance(GunnerArmorContext context) {
        if (context.isElite) return 1.0f;                             // 100% for elites
        float scale = progressionScale(context);
        if (context.isSpecialSituation) return 0.60f + 0.30f * scale;
        return 0.08f + 0.37f * scale;
    }

    private static BulletproofArmorItem.Tier randomTier(RandomSource random, GunnerArmorContext context, int minimumTier) {
        float scale = progressionScale(context);
        int maximumTier = context.isElite ? 6 : context.isSpecialSituation ? 5 : 2 + (int) Math.floor(scale * 4.0F);
        maximumTier = Math.max(minimumTier, Math.min(6, maximumTier));
        int count = maximumTier - minimumTier + 1;
        float biased = (float) Math.pow(random.nextFloat(), 1.8D - 1.2D * scale);
        int tierNumber = minimumTier + Math.min(count - 1, (int) (biased * count));
        return BulletproofArmorItem.Tier.values()[tierNumber - 1];
    }

    private static float progressionScale(GunnerArmorContext context) {
        return Config.gunnerProgressionScale(context.mob.level());
    }

    /**
     * Context class for armor equipping decisions
     */
    public static class GunnerArmorContext {
        public final net.minecraft.world.entity.PathfinderMob mob;
        public final boolean isSpecialSituation;
        public final boolean isElite;

        public GunnerArmorContext(net.minecraft.world.entity.PathfinderMob mob, boolean isSpecialSituation, boolean isElite) {
            this.mob = mob;
            this.isSpecialSituation = isSpecialSituation;
            this.isElite = isElite;
        }

        public static GunnerArmorContext normal(net.minecraft.world.entity.PathfinderMob mob) {
            return new GunnerArmorContext(mob, false, false);
        }

        public static GunnerArmorContext special(net.minecraft.world.entity.PathfinderMob mob) {
            return new GunnerArmorContext(mob, true, false);
        }

        public static GunnerArmorContext elite(net.minecraft.world.entity.PathfinderMob mob) {
            return new GunnerArmorContext(mob, false, true);
        }

        public static GunnerArmorContext eliteSpecial(net.minecraft.world.entity.PathfinderMob mob) {
            return new GunnerArmorContext(mob, true, true);
        }
    }
}
