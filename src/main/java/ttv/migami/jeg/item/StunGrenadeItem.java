package ttv.migami.jeg.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.entity.StunGrenadeEntity;
import ttv.migami.jeg.entity.TimedThrowableItemProjectile;

public final class StunGrenadeItem extends ThrowableWeaponItem {
    private static final int FUSE_TICKS = 20;

    public StunGrenadeItem(Properties properties) {
        super(properties, FUSE_TICKS);
    }

    @Override
    protected boolean canCook() {
        return false;
    }

    @Override
    protected int getRemainingFuseTicks(int useDuration) {
        return FUSE_TICKS;
    }

    @Override
    protected TimedThrowableItemProjectile createProjectile(Level level, LivingEntity livingEntity, int fuseTicks) {
        return new StunGrenadeEntity(level, livingEntity, FUSE_TICKS);
    }

    @Override
    protected void onThrown(Level level, LivingEntity livingEntity, TimedThrowableItemProjectile projectile, ItemStack stack) {
        level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), resolveSound("item.grenade.pin", net.minecraft.sounds.SoundEvents.SNOWBALL_THROW), SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
