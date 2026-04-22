package ttv.migami.jeg.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;

public class GrenadeEntity extends TimedThrowableItemProjectile {
    private static final double DAMAGE_RADIUS_MULTIPLIER = 2.0D;
    private static final float BALANCED_DAMAGE_FACTOR = 5.0F;
    private static final float EDGE_DAMAGE_FLOOR = 0.35F;
    private static final double FALLOFF_EXPONENT = 0.8D;

    private float explosionPower = 3.0F;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
        this.setItem(ModItems.AMMO.get(Reference.id("grenade")).get().getDefaultInstance());
    }

    public GrenadeEntity(Level level, LivingEntity owner, float explosionPower, int fuseTicks, boolean launched) {
        this(ModEntities.GRENADE.get(), level);
        this.setOwner(owner);
        this.setFuse(fuseTicks);
        this.explosionPower = explosionPower;
        this.setLaunched(launched);
    }

    @Override
    protected void spawnFlightParticles() {
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();
        if (speed <= 0.1D) {
            return;
        }
        this.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.FLAME,
                this.getX(),
                this.getY() + 0.1D,
                this.getZ(),
                -motion.x * 0.1D,
                -motion.y * 0.1D,
                -motion.z * 0.1D
        );
        if (this.random.nextInt(2) == 0) {
            this.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.getX(),
                    this.getY() + 0.1D,
                    this.getZ(),
                    -motion.x * 0.05D,
                    -motion.y * 0.05D,
                    -motion.z * 0.05D
            );
        }
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AMMO.get(Reference.id("grenade")).get();
    }

    public void setExplosionPower(float power) {
        this.explosionPower = power;
    }

    @Override
    protected void explode() {
        ExplosionInteraction interaction = this.isLaunched() ? ExplosionInteraction.TNT : ExplosionInteraction.MOB;
        float visualPower = Math.max(1.2F, this.explosionPower * 0.5F);
        ServerLevel serverLevel = (ServerLevel) this.level();
        serverLevel.sendParticles(ModParticleTypes.BIG_EXPLOSION.get(), this.getX(), this.getY(), this.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.01D);
        serverLevel.sendParticles(ModParticleTypes.SMALL_EXPLOSION.get(), this.getX(), this.getY(), this.getZ(), 14, 0.8D, 0.8D, 0.8D, 0.12D);
        serverLevel.sendParticles(ModParticleTypes.SMOKE.get(), this.getX(), this.getY(), this.getZ(), 10, 1.0D, 1.0D, 1.0D, 0.02D);
        serverLevel.sendParticles(ModParticleTypes.FIRE.get(), this.getX(), this.getY(), this.getZ(), 8, 0.7D, 0.7D, 0.7D, 0.04D);
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), visualPower, interaction);
        this.applyBalancedBlastDamage();
        this.igniteNearby();
    }

    private void applyBalancedBlastDamage() {
        double radius = Math.max(2.6D, this.explosionPower * DAMAGE_RADIUS_MULTIPLIER);
        float baseDamage = this.explosionPower * BALANCED_DAMAGE_FACTOR;
        Entity owner = this.getOwner();
        AABB area = this.getBoundingBox().inflate(radius);

        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (!target.isAlive()) {
                continue;
            }

            double distance = target.distanceTo(this);
            if (distance > radius) {
                continue;
            }

            double t = 1.0D - (distance / radius);
            double curve = Math.pow(Math.max(0.0D, t), FALLOFF_EXPONENT);
            float scale = (float) (EDGE_DAMAGE_FLOOR + (1.0F - EDGE_DAMAGE_FLOOR) * curve);
            float damage = baseDamage * scale;

            if (owner != null && target == owner) {
                damage *= 0.65F;
            }

            if (damage > 0.5F) {
                target.hurt(this.damageSources().explosion(this, owner), damage);
            }
        }
    }

    private void igniteNearby() {
        if (this.isLaunched()) {
            return;
        }
        Level level = this.level();
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 1, 1))) {
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            if (this.random.nextBoolean() && Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }
    }
}
