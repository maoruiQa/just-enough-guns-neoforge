package ttv.migami.jeg.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModSounds;

public final class StunGrenadeEntity extends TimedThrowableItemProjectile {
    private static final double EFFECT_RADIUS = 12.0D;

    public StunGrenadeEntity(net.minecraft.world.entity.EntityType<? extends StunGrenadeEntity> type, Level level) {
        super(type, level);
        this.setItem(ModItems.AMMO.get(Reference.id("stun_grenade")).get().getDefaultInstance());
    }

    public StunGrenadeEntity(Level level, LivingEntity owner, int fuseTicks) {
        this(ModEntities.STUN_GRENADE.get(), level);
        this.setOwner(owner);
        this.setFuse(fuseTicks);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AMMO.get(Reference.id("stun_grenade")).get();
    }

    @Override
    protected void spawnFlightParticles() {
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.1D, this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void explode() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        SoundEvent sound = resolveSound("entity.stun_grenade.explosion", SoundEvents.GENERIC_EXPLODE.value());
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.BLOCKS, 2.0F, 0.7F + this.random.nextFloat() * 0.2F);
        serverLevel.sendParticles(ModParticleTypes.BIG_EXPLOSION.get(), this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(EFFECT_RADIUS))) {
            if (!target.isAlive()) {
                continue;
            }

            double distance = target.distanceToSqr(center);
            if (distance > EFFECT_RADIUS * EFFECT_RADIUS) {
                continue;
            }

            double distanceFactor = 1.0D - (Math.sqrt(distance) / EFFECT_RADIUS);
            if (distanceFactor <= 0.0D) {
                continue;
            }

            double facingFactor = getFacingFactor(target, center);
            boolean lineOfSight = hasLineOfSight(target, center);

            int deafenedTicks = Mth.floor(60 + 160 * distanceFactor);
            target.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.DEAFENED.get()), deafenedTicks, 0, false, false, true));

            if (lineOfSight && facingFactor > 0.2D) {
                int blindedTicks = Mth.floor(20 + 100 * distanceFactor * facingFactor);
                target.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.BLINDED.get()), blindedTicks, 0, false, false, true));
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                }
            }
        }
    }

    private static double getFacingFactor(LivingEntity target, Vec3 center) {
        Vec3 toGrenade = center.subtract(target.getEyePosition()).normalize();
        return Math.max(0.0D, target.getViewVector(1.0F).normalize().dot(toGrenade));
    }

    private boolean hasLineOfSight(LivingEntity target, Vec3 center) {
        HitResult hitResult = this.level().clip(new ClipContext(target.getEyePosition(), center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hitResult.getType() == HitResult.Type.MISS || hitResult.getLocation().distanceToSqr(center) < 0.25D;
    }

    private static SoundEvent resolveSound(String path, SoundEvent fallback) {
        var holder = ModSounds.ALL.get(Reference.id(path));
        return holder != null ? holder.get() : fallback;
    }
}
