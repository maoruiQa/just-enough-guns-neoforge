package ttv.migami.jeg.entity.monster.phantom;

import java.util.EnumSet;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.gameevent.GameEvent;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries.DeferredHolder;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;

public class PhantomGunner extends Phantom implements GeoEntity {
    private static final Identifier DEFAULT_GUN = Reference.id("phantom_smg");
    private static final float EXPLOSION_POWER = 2.6F;
    private static final Identifier GEO_TEXTURE = Reference.id("textures/entity/phantom_gunner/phantom_gunner.png");
    private static final float PROJECTILE_SPREAD_SCALE = 0.85F;

    private static final String GECKO_CONTROLLER = "Idle";
    private static final RawAnimation GECKO_IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);

    private GunStats cachedStats;
    private int magazine;
    private int reloadTicks;
    private int fireCooldown;

    public PhantomGunner(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setPhantomSize(0); // Base size before applying custom scaling
        equipDefaultGun();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(0.4F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                GECKO_CONTROLLER,
                0,
                state -> {
                    state.setAndContinue(GECKO_IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    public Identifier getGeoTexture() {
        return GEO_TEXTURE;
    }

    public float getGeoScale() {
        // Keep visuals consistent with getDefaultDimensions() scaling.
        return 0.4F;
    }

    @Override
    protected void registerGoals() {
        // Add shooting goal BEFORE calling super.registerGoals() to ensure higher priority
        this.goalSelector.addGoal(1, new ShootGunGoal());
        super.registerGoals();
        // Targeting filtered via canAttack() override
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, null));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.getPhantomSize() != 1) {
            this.setPhantomSize(1);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        tickCombatTimers();
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && Config.phantomGunnerDeathExplosionEnabled()) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER, ExplosionInteraction.MOB);
        }
        super.die(source);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        equipDefaultGun();
        return data;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    private void equipDefaultGun() {
        DeferredHolder<Item, GunItem> holder = ModItems.GUNS.get(DEFAULT_GUN);
        if (holder == null) {
            return;
        }
        ItemStack stack = new ItemStack(holder.get());
        configureLoadout(holder.get().getStats(), stack);
        this.setItemInHand(InteractionHand.MAIN_HAND, stack);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setCanPickUpLoot(false);
    }

    public void shootAt(LivingEntity target) {
        ItemStack stack = this.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        GunStats stats = getEquippedGunStats().orElseGet(gun::getStats);
        gun.fireDirectionally(this.level(), this, stack, computeProjectileDirection(target, stats));

        // Spawn bullet trail particles (fire + smoke)
        if (this.level() instanceof ServerLevel serverLevel) {
            net.minecraft.world.phys.Vec3 start = this.getEyePosition();
            net.minecraft.world.phys.Vec3 end = target.getEyePosition();
            net.minecraft.world.phys.Vec3 direction = end.subtract(start).normalize();

            // Spawn particles along the bullet path
            double distance = start.distanceTo(end);
            int particleCount = Math.min(20, (int) (distance / 2.0D));
            for (int i = 0; i < particleCount; i++) {
                double fraction = (double) i / particleCount;
                net.minecraft.world.phys.Vec3 particlePos = start.add(direction.scale(distance * fraction));

                // Fire particles
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.02D, 0.02D, 0.02D, 0.01D
                );

                // Smoke particles
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.02D, 0.02D, 0.02D, 0.01D
                );
            }
        }

        playGunshotSound(stats);
        stack.hurtAndBreak(1, this, InteractionHand.MAIN_HAND);
        this.gameEvent(GameEvent.ENTITY_ACTION);

        if (usesLoadedAmmo(stack, stats)) {
            this.magazine = Math.max(0, this.magazine - 1);
            stack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
            if (this.magazine <= 0) {
                startReload(stats, stack);
            } else {
                this.fireCooldown = 4; // 5 shots per second (20 ticks / 5 = 4)
            }
        } else {
            this.fireCooldown = 4;
        }
    }

    private Optional<GunStats> getEquippedGunStats() {
        if (this.cachedStats != null) {
            return Optional.of(this.cachedStats);
        }
        ItemStack stack = this.getMainHandItem();
        if (stack.getItem() instanceof GunItem gun) {
            this.cachedStats = gun.getStats();
            return Optional.of(this.cachedStats);
        }
        return Optional.empty();
    }

    private net.minecraft.world.phys.Vec3 computeProjectileDirection(LivingEntity target, GunStats stats) {
        net.minecraft.world.phys.Vec3 forwards = target.getEyePosition().subtract(this.getEyePosition()).normalize();
        if (forwards.lengthSqr() < 1.0E-6D) {
            forwards = this.getViewVector(1.0F);
        }

        float gunSpread = stats.spread();
        if (gunSpread <= 0.0F) {
            return forwards.normalize();
        }

        if (ttv.migami.jeg.gun.GunCategory.fromStats(stats) == ttv.migami.jeg.gun.GunCategory.SHOTGUN) {
            gunSpread = stats.spread() * 0.60F;
        } else {
            float earlySpreadMultiplier = this.level().getDifficulty() != net.minecraft.world.Difficulty.HARD ? 10.0F : 5.0F;
            float scaledSpreadMultiplier = Config.scaleGunnerSpreadMultiplier(this.level(), earlySpreadMultiplier);
            gunSpread *= scaledSpreadMultiplier * PROJECTILE_SPREAD_SCALE;
        }
        if (gunSpread <= 0.0F) {
            return forwards.normalize();
        }

        float spreadRadians = Math.min(gunSpread, 170.0F) * net.minecraft.util.Mth.DEG_TO_RAD;
        net.minecraft.world.phys.Vec3 worldUp = new net.minecraft.world.phys.Vec3(0.0D, 1.0D, 0.0D);
        net.minecraft.world.phys.Vec3 sideways = forwards.cross(worldUp);
        if (sideways.lengthSqr() < 1.0E-6D) {
            sideways = new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideways = sideways.normalize();
        }
        net.minecraft.world.phys.Vec3 upwards = sideways.cross(forwards).normalize();

        float theta = this.random.nextFloat() * 2.0F * (float) Math.PI;
        float r = net.minecraft.util.Mth.sqrt(this.random.nextFloat()) * (float) Math.tan(spreadRadians);
        float a1 = net.minecraft.util.Mth.cos(theta) * r;
        float a2 = net.minecraft.util.Mth.sin(theta) * r;

        return forwards.add(sideways.scale(a1)).add(upwards.scale(a2)).normalize();
    }

    private void configureLoadout(GunStats stats, ItemStack stack) {
        this.cachedStats = stats;
        if (usesLoadedAmmo(stack, stats)) {
            this.magazine = stats.magazineSize();
            stack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
        } else {
            this.magazine = Math.max(1, stats.projectileAmount());
        }
        this.reloadTicks = 0;
        this.fireCooldown = 0;
    }

    private void startReload(GunStats stats, ItemStack stack) {
        this.reloadTicks = Math.max(20, stats.totalReloadTime());
        stack.set(ModDataComponents.GUN_AMMO.get(), 0);
        stats.reloadStartSoundEvent().ifPresent(sound -> this.level().playSound(null, this, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
    }

    private void finishReload(ItemStack stack) {
        GunStats stats = getEquippedGunStats().orElse(null);
        if (stats == null) {
            return;
        }
        if (usesLoadedAmmo(stack, stats)) {
            this.magazine = stats.magazineSize();
            stack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
        } else {
            this.magazine = Math.max(1, stats.projectileAmount());
        }
        stats.reloadEndSoundEvent().ifPresent(sound -> this.level().playSound(null, this, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
        this.fireCooldown = Math.max(6, stats.fireDelay());
    }

    private void playGunshotSound(GunStats stats) {
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> this.level().playSound(null, this, sound, SoundSource.HOSTILE, 8.0F, 0.9F + this.random.nextFloat() * 0.2F),
                () -> this.level().playSound(null, this, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.9F + this.random.nextFloat() * 0.2F)
        );
    }

    // Note: isSunBurnTick removed in 1.21.11, phantoms don't burn in sun by default

    @Override
    public boolean canAttack(LivingEntity target) {
        // Don't attack creative mode players
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        // Don't attack other Phantom Gunners or Terror Phantoms
        if (target instanceof PhantomGunner || target instanceof AbstractTerrorPhantom) {
            return false;
        }

        return super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !this.canAttack(target)) {
            return;
        }
        super.setTarget(target);
    }

    private void tickCombatTimers() {
        if (this.fireCooldown > 0) {
            this.fireCooldown--;
        }
        if (this.reloadTicks > 0) {
            this.reloadTicks--;
            if (this.reloadTicks == 0) {
                finishReload(this.getMainHandItem());
            }
        }
    }

    private boolean isReloading() {
        return this.reloadTicks > 0;
    }

    private boolean readyToShoot() {
        if (this.isReloading()) {
            return false;
        }
        if (this.fireCooldown > 0) {
            return false;
        }
        ItemStack stack = this.getMainHandItem();
        GunStats stats = getEquippedGunStats().orElse(null);
        if (!usesLoadedAmmo(stack, stats)) {
            return true;
        }
        return this.magazine > 0;
    }

    private boolean usesLoadedAmmo(ItemStack stack, @Nullable GunStats stats) {
        return stats != null && stack.getItem() instanceof GunItem gun && gun.usesLoadedAmmo();
    }

    private int getCurrentFireDelay() {
        // Return 4 ticks for 5 shots per second fire rate
        return 4;
    }

    private class ShootGunGoal extends Goal {
        private ShootGunGoal() {
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = PhantomGunner.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = PhantomGunner.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = PhantomGunner.this.getTarget();
            if (target == null) {
                return;
            }

            PhantomGunner.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = PhantomGunner.this.distanceToSqr(target);
            boolean canSee = ttv.migami.jeg.gun.BulletPenetrationHelper.hasLineOfSightThroughPenetrable(PhantomGunner.this, target);

            if (!canSee || distance > 1024.0D) {
                return;
            }

            if (PhantomGunner.this.isReloading()) {
                return;
            }

            if (!PhantomGunner.this.readyToShoot()) {
                return;
            }

            // No additional cooldown - fireCooldown in readyToShoot() already handles timing
            PhantomGunner.this.shootAt(target);
        }
    }

}
