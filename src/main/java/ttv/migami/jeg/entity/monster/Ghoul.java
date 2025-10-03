package ttv.migami.jeg.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModSounds;

/**
 * Simplified NeoForge 1.21 port of the original Ghoul zombie variant.
 */
public class Ghoul extends Zombie {
    public Ghoul(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean isSunSensitive() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return resolveSound("entity.ghoul.ambient", SoundEvents.ZOMBIE_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return resolveSound("entity.ghoul.hurt", SoundEvents.ZOMBIE_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return resolveSound("entity.ghoul.death", SoundEvents.ZOMBIE_DEATH);
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.HUSK_STEP;
    }

    @Override
    public void aiStep() {
        if (this.isAlive() && this.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.random.nextFloat() <= 0.2F) {
                serverLevel.sendParticles(
                        ParticleTypes.SOUL,
                        this.getX(),
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ(),
                        1,
                        0.2,
                        1.2,
                        0.2,
                        0.0
                );
            }
        }

        super.aiStep();
    }

    @Override
    protected boolean convertsInWater() {
        return true;
    }

    @Override
    protected void doUnderWaterConversion() {
        this.convertToZombieType(EntityType.ZOMBIE);
        if (!this.isSilent()) {
            this.level().levelEvent(null, 1041, this.blockPosition(), 0);
        }
    }

    @Override
    protected ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    @Override
    public MobCategory getClassification(boolean forSpawnCount) {
        return super.getClassification(forSpawnCount);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.17F)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    public static boolean checkMonsterSpawnRules(
            EntityType<? extends Monster> type,
            ServerLevelAccessor level,
            EntitySpawnReason spawnReason,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Monster.isDarkEnoughToSpawn(level, pos, random)
                && checkMobSpawnRules(type, level, spawnReason, pos, random);
    }

    private static SoundEvent resolveSound(String path, SoundEvent fallback) {
        var holder = ModSounds.ALL.get(Reference.id(path));
        return holder != null ? holder.get() : fallback;
    }
}
