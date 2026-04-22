package ttv.migami.jeg.faction;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
        if (context.isSpecialSituation) {
            // Special situations: guaranteed level 1, increased chance for level 2
            float roll = random.nextFloat();
            if (roll < 0.60f) return BulletproofArmorItem.Tier.I;      // 60% level 1
            if (roll < 0.85f) return BulletproofArmorItem.Tier.II;     // 25% level 2
            if (roll < 0.95f) return BulletproofArmorItem.Tier.III;    // 10% level 3
            return BulletproofArmorItem.Tier.IV;                       // 5% level 4
        } else {
            // Normal spawning: most gunners have no armor, limited helmet chances
            float roll = random.nextFloat();
            if (roll < 0.85f) return null;                             // 85% no helmet (increased from 70%)
            if (roll < 0.95f) return BulletproofArmorItem.Tier.I;      // 10% level 1 helmet ONLY (decreased from 15%)
            if (roll < 0.98f) return BulletproofArmorItem.Tier.II;     // 3% level 2 helmet ONLY (decreased from 10%)
            if (roll < 0.995f) return BulletproofArmorItem.Tier.III;   // 1.5% level 3 helmet ONLY (decreased from 4%)
            return BulletproofArmorItem.Tier.IV;                       // 0.5% level 4 helmet ONLY (decreased from 1%)
        }
    }

    private static BulletproofArmorItem.Tier determineBodyArmorTier(RandomSource random, GunnerArmorContext context) {
        if (context.isElite) {
            // Elite gunners get better armor
            float roll = random.nextFloat();
            if (roll < 0.40f) return BulletproofArmorItem.Tier.II;     // 40% level 2
            if (roll < 0.70f) return BulletproofArmorItem.Tier.III;    // 30% level 3
            if (roll < 0.90f) return BulletproofArmorItem.Tier.IV;     // 20% level 4
            if (roll < 0.98f) return BulletproofArmorItem.Tier.V;      // 8% level 5
            return BulletproofArmorItem.Tier.VI;                       // 2% level 6
        } else if (context.isSpecialSituation) {
            // Special situations have moderate armor
            float roll = random.nextFloat();
            if (roll < 0.30f) return null;                             // 30% no body armor
            if (roll < 0.65f) return BulletproofArmorItem.Tier.I;      // 35% level 1
            if (roll < 0.85f) return BulletproofArmorItem.Tier.II;     // 20% level 2
            if (roll < 0.95f) return BulletproofArmorItem.Tier.III;    // 10% level 3
            if (roll < 0.99f) return BulletproofArmorItem.Tier.IV;     // 4% level 4
            return BulletproofArmorItem.Tier.V;                        // 1% level 5
        } else {
            // Normal spawning: very rare body armor - only if they already have a helmet
            float roll = random.nextFloat();
            if (roll < 0.92f) return null;                             // 92% no body armor (increased from 85%)
            if (roll < 0.975f) return BulletproofArmorItem.Tier.I;     // 5.5% level 1 (decreased from 9%)
            if (roll < 0.992f) return BulletproofArmorItem.Tier.II;    // 1.7% level 2 (decreased from 4%)
            if (roll < 0.998f) return BulletproofArmorItem.Tier.III;   // 0.6% level 3 (decreased from 1.5%)
            return BulletproofArmorItem.Tier.IV;                       // 0.2% level 4 (decreased from 0.5%)
        }
    }

    private static float getHelmetEquipChance(GunnerArmorContext context) {
        if (context.isElite) return 1.0f;                             // 100% for elites
        if (context.isSpecialSituation) return 0.8f;                   // 80% for special situations
        return 0.15f;                                                  // 15% for normal spawning (decreased from 30%)
    }

    private static float getBodyArmorEquipChance(GunnerArmorContext context) {
        if (context.isElite) return 1.0f;                             // 100% for elites
        if (context.isSpecialSituation) return 0.6f;                   // 60% for special situations
        return 0.08f;                                                  // 8% for normal spawning (decreased from 15%)
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
