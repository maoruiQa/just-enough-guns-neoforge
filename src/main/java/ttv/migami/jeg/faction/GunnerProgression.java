package ttv.migami.jeg.faction;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;

public final class GunnerProgression {
    private static final float WEAPON_DROP_CHANCE = 0.06F;

    private GunnerProgression() {}

    public static Item selectGun(List<Item> pool, Level level, RandomSource random) {
        return selectGun(pool, level, random, "generic");
    }

    public static Item selectGun(List<Item> pool, Level level, RandomSource random, String gunnerType) {
        return selectGun(pool, level, random, gunnerType, true);
    }

    /**
     * @param allowRocket when false, skip the independent rocket-launcher roll (used for bomber gunners).
     */
    public static Item selectGun(List<Item> pool, Level level, RandomSource random, String gunnerType, boolean allowRocket) {
        if (pool.isEmpty()) {
            return null;
        }

        Item rocketLauncher = resolveRocketLauncher();
        if (allowRocket && rocketLauncher != null && Config.shouldGunnerUseRocketLauncher(level, gunnerType, random)) {
            return rocketLauncher;
        }

        return selectNormalGun(pool, level, random, gunnerType, rocketLauncher);
    }

    /** Tier-weighted gun pick that never returns a rocket launcher. */
    public static Item selectNormalGun(List<Item> pool, Level level, RandomSource random, String gunnerType) {
        return selectNormalGun(pool, level, random, gunnerType, resolveRocketLauncher());
    }

    private static Item selectNormalGun(
            List<Item> pool,
            Level level,
            RandomSource random,
            String gunnerType,
            Item rocketLauncher
    ) {
        if (pool.isEmpty()) {
            return null;
        }

        int allowedTier = Config.gunnerWeaponMaxTier(level, gunnerType);
        List<Item> candidates = pool.stream()
                .filter(item -> item != rocketLauncher)
                .filter(item -> weaponTier(item) <= allowedTier)
                .toList();
        if (candidates.isEmpty()) {
            candidates = pool.stream()
                    .filter(item -> item != rocketLauncher)
                    .toList();
        }
        if (candidates.isEmpty()) {
            return pool.get(random.nextInt(pool.size()));
        }
        return selectWeightedByTier(candidates, random, Config.gunnerWeaponAggression(gunnerType));
    }

    public static void prepareDroppedWeapon(Mob mob, ItemStack stack) {
        damageWeaponToLowDurability(stack, mob.getRandom());
        mob.setDropChance(EquipmentSlot.MAINHAND, WEAPON_DROP_CHANCE);
    }

    public static void damageWeaponToLowDurability(ItemStack stack, RandomSource random) {
        if (stack.isDamageableItem()) {
            int maxDamage = stack.getMaxDamage();
            int minimumDamage = Math.max(0, (int) Math.ceil(maxDamage * 0.75D));
            int maximumDamage = Math.max(minimumDamage, maxDamage - 1);
            int damageRange = maximumDamage - minimumDamage + 1;
            stack.setDamageValue(minimumDamage + random.nextInt(damageRange));
        }
    }

    private static Item selectWeightedByTier(List<Item> candidates, RandomSource random, double aggression) {
        int highestTier = candidates.stream().mapToInt(GunnerProgression::weaponTier).max().orElse(0);
        if (aggression >= 0.999D) {
            List<Item> highest = candidates.stream()
                    .filter(item -> weaponTier(item) == highestTier)
                    .toList();
            return highest.get(random.nextInt(highest.size()));
        }

        // Keep low and mid-tier guns alive in late game while still letting config bias upward.
        double totalWeight = 0.0D;
        double[] weights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            int distanceFromBest = highestTier - weaponTier(candidates.get(i));
            double highTierBias = Math.pow(0.35D, distanceFromBest);
            weights[i] = (1.0D - aggression) + aggression * highTierBias;
            totalWeight += weights[i];
        }

        double roll = random.nextDouble() * totalWeight;
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights[i];
            if (roll <= 0.0D) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static Item resolveRocketLauncher() {
        var holder = ModItems.GUNS.get(Reference.id("rocket_launcher"));
        return holder != null ? holder.get() : null;
    }

    public static int weaponTier(Item item) {
        if (!(item instanceof GunItem gun)) {
            return 0;
        }

        ResourceLocation gunId = gun.getStats().id();
        String path = gunId.getPath();
        if ("bolt_action_rifle".equals(path)) {
            return 2;
        }
        if ("minigun".equals(path) || "light_machine_gun".equals(path)) {
            return 3;
        }
        if ("combat_pistol".equals(path)) {
            return 1;
        }

        ResourceLocation ammo = gun.getStats().ammoItem();
        if (ammo != null && "jeg".equals(ammo.getNamespace())) {
            if ("pistol_ammo".equals(ammo.getPath())) {
                return 0;
            }
            if ("rifle_ammo".equals(ammo.getPath())) {
                return 1;
            }
        }
        return 2;
    }
}
