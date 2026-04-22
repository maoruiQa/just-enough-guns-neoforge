package ttv.migami.jeg.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;

public final class WaterBombEntity extends TimedThrowableItemProjectile {
    private static final double WATER_RADIUS = 3.5D;

    public WaterBombEntity(net.minecraft.world.entity.EntityType<? extends WaterBombEntity> type, Level level) {
        super(type, level);
        this.setItem(ModItems.AMMO.get(Reference.id("water_bomb")).get().getDefaultInstance());
    }

    public WaterBombEntity(Level level, LivingEntity owner, int fuseTicks) {
        this(ModEntities.WATER_BOMB.get(), level);
        this.setOwner(owner);
        this.setFuse(fuseTicks);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AMMO.get(Reference.id("water_bomb")).get();
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
        if (this.isUnderWater()) {
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            return;
        }
        this.level().addParticle(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        this.level().addParticle(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void explode() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.5F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(), 45, 1.2D, 0.6D, 1.2D, 0.03D);
        serverLevel.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY(), this.getZ(), 25, 1.0D, 0.5D, 1.0D, 0.02D);

        extinguishNearby(serverLevel, this.blockPosition(), 4);

        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(WATER_RADIUS))) {
            if (!target.isAlive() || target.distanceToSqr(this) > WATER_RADIUS * WATER_RADIUS) {
                continue;
            }
            target.extinguishFire();
        }
    }

    private static void extinguishNearby(ServerLevel level, BlockPos center, int radius) {
        boolean extinguished = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BaseFireBlock) {
                level.removeBlock(pos, false);
                extinguished = true;
                continue;
            }
            if (CampfireBlock.isLitCampfire(state)) {
                CampfireBlock.dowse(null, level, pos, state);
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, Boolean.FALSE), 11);
                extinguished = true;
                continue;
            }
            if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, level, pos);
                extinguished = true;
            }
        }

        if (extinguished) {
            level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.1F);
        }
    }
}
