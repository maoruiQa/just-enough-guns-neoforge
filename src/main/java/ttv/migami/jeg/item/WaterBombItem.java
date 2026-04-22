package ttv.migami.jeg.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.entity.TimedThrowableItemProjectile;
import ttv.migami.jeg.entity.WaterBombEntity;

public final class WaterBombItem extends ThrowableWeaponItem {
    private static final int MAX_FUSE_TICKS = 120;

    public WaterBombItem(Properties properties) {
        super(properties, MAX_FUSE_TICKS);
    }

    @Override
    protected TimedThrowableItemProjectile createProjectile(Level level, LivingEntity livingEntity, int fuseTicks) {
        return new WaterBombEntity(level, livingEntity, fuseTicks);
    }
}
