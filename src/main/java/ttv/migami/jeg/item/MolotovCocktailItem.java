package ttv.migami.jeg.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.entity.MolotovCocktailEntity;
import ttv.migami.jeg.entity.TimedThrowableItemProjectile;

public final class MolotovCocktailItem extends ThrowableWeaponItem {
    private static final int MAX_FUSE_TICKS = 60;

    public MolotovCocktailItem(Properties properties) {
        super(properties, MAX_FUSE_TICKS);
    }

    @Override
    protected TimedThrowableItemProjectile createProjectile(Level level, LivingEntity livingEntity, int fuseTicks) {
        return new MolotovCocktailEntity(level, livingEntity, fuseTicks);
    }

    @Override
    protected void playPrimeSound(Level level, LivingEntity livingEntity) {
        level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
