package ttv.migami.jeg.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;

public final class MolotovCocktailEntity extends TimedThrowableItemProjectile {
    private static final double FIRE_RADIUS = 3.0D;

    public MolotovCocktailEntity(net.minecraft.world.entity.EntityType<? extends MolotovCocktailEntity> type, Level level) {
        super(type, level);
        this.setItem(ModItems.AMMO.get(Reference.id("molotov_cocktail")).get().getDefaultInstance());
    }

    public MolotovCocktailEntity(Level level, LivingEntity owner, int fuseTicks) {
        this(ModEntities.MOLOTOV_COCKTAIL.get(), level);
        this.setOwner(owner);
        this.setFuse(fuseTicks);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AMMO.get(Reference.id("molotov_cocktail")).get();
    }

    @Override
    protected boolean shouldBounce() {
        return false;
    }

    @Override
    protected boolean shouldDetonateOnImpact() {
        return true;
    }

    @Override
    protected void spawnFlightParticles() {
        this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.15D, this.getZ(), 0.0D, 0.0D, 0.0D);
        this.level().addParticle(ParticleTypes.LAVA, this.getX(), this.getY() + 0.15D, this.getZ(), 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void explode() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.3F, 1.0F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), resolveSound("entity.molotov.explosion", SoundEvents.FIRECHARGE_USE), SoundSource.BLOCKS, 2.0F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 40, 1.4D, 0.4D, 1.4D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 18, 0.7D, 0.3D, 0.7D, 0.01D);

        Entity owner = this.getOwner();
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(FIRE_RADIUS))) {
            if (!target.isAlive() || target.distanceToSqr(this) > FIRE_RADIUS * FIRE_RADIUS) {
                continue;
            }
            var source = this.damageSources().explosion(this, owner);
            ModDamageTypes.hurtWithPlayerKillCredit(target, serverLevel, source, 6.0F, owner);
            target.igniteForSeconds(8.0F);
        }

        igniteNearby(serverLevel, this.blockPosition(), 2);
    }

    private static void igniteNearby(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 1, radius))) {
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            if (level.getRandom().nextBoolean() && Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    private static SoundEvent resolveSound(String path, SoundEvent fallback) {
        var holder = ModSounds.ALL.get(Reference.id(path));
        return holder != null ? holder.get() : fallback;
    }
}
