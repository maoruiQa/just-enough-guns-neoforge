package ttv.migami.jeg.faction;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.item.GunItem;

public final class GunnerProgression {
    private static final float WEAPON_DROP_CHANCE = 0.06F;

    private GunnerProgression() {}

    public static Item selectGun(List<Item> pool, Level level, RandomSource random) {
        if (pool.isEmpty()) {
            return null;
        }

        int allowedTier = Math.min(3, (int) Math.floor(Config.gunnerProgressionScale(level) * 4.0F));
        List<Item> candidates = pool.stream()
                .filter(item -> weaponTier(item) <= allowedTier)
                .toList();
        if (candidates.isEmpty()) {
            int lowestTier = pool.stream().mapToInt(GunnerProgression::weaponTier).min().orElse(3);
            candidates = pool.stream()
                    .filter(item -> weaponTier(item) == lowestTier)
                    .toList();
        }
        return candidates.get(random.nextInt(candidates.size()));
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

    private static int weaponTier(Item item) {
        if (!(item instanceof GunItem gun)) {
            return 0;
        }

        Identifier gunId = gun.getStats().id();
        String path = gunId.getPath();
        if ("minigun".equals(path) || "light_machine_gun".equals(path)) {
            return 3;
        }
        if ("combat_pistol".equals(path)) {
            return 1;
        }

        Identifier ammo = gun.getStats().ammoItem();
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
