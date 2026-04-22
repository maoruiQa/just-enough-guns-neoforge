package ttv.migami.jeg.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.entity.TimedThrowableItemProjectile;
import ttv.migami.jeg.init.ModItems;

public class GrenadeItem extends ThrowableWeaponItem {
    private static final int DEFAULT_FUSE = 60;
    private static final float DEFAULT_POWER = 4.0F;

    public GrenadeItem(Properties properties) {
        super(properties, DEFAULT_FUSE);
    }

    @Override
    protected TimedThrowableItemProjectile createProjectile(Level level, LivingEntity livingEntity, int fuseTicks) {
        return new GrenadeEntity(level, livingEntity, DEFAULT_POWER, fuseTicks, false);
    }

    @Override
    protected boolean handleSpecialUse(Level level, Player player, InteractionHand hand) {
        if (!player.isUnderWater()) {
            return false;
        }

        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            ItemStack waterBomb = new ItemStack(ModItems.AMMO.get(Reference.id("water_bomb")).get());
            if (!player.getInventory().add(waterBomb)) {
                player.drop(waterBomb, false);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return true;
    }
}
