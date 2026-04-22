package ttv.migami.jeg.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.entity.SmokeGrenadeEntity;
import ttv.migami.jeg.entity.TimedThrowableItemProjectile;

public final class SmokeGrenadeItem extends ThrowableWeaponItem {
    private static final int MAX_FUSE_TICKS = 60;

    public SmokeGrenadeItem(Properties properties) {
        super(properties, MAX_FUSE_TICKS);
    }

    @Override
    protected boolean canCook() {
        return false;
    }

    @Override
    protected TimedThrowableItemProjectile createProjectile(Level level, LivingEntity livingEntity, int fuseTicks) {
        return new SmokeGrenadeEntity(level, livingEntity, fuseTicks);
    }

    @Override
    protected void onThrown(Level level, LivingEntity livingEntity, TimedThrowableItemProjectile projectile, ItemStack stack) {
        level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), resolveSound("item.grenade.pin", net.minecraft.sounds.SoundEvents.SNOWBALL_THROW), SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
