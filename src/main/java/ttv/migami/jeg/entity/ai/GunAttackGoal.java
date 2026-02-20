package ttv.migami.jeg.entity.ai;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.gun.BulletPenetrationHelper;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

import java.util.EnumSet;

public class GunAttackGoal<T extends PathfinderMob> extends Goal {
    protected final T shooter;
    protected final double speedModifier;
    protected int seeTime;
    protected int attackTime;
    protected final float attackRadiusSqr;
    protected boolean strafingClockwise;
    protected boolean strafingBackwards;
    protected int strafingTime;

    protected int reloadTick = 0;
    protected boolean isReloading = false;

    protected boolean isPanicked = false;
    protected int panickTimer = 0;

    protected AIType aiType = AIType.TACTICAL;

    protected Vec3 lastKnownPosition;

    protected float spreadModifier = 10;
    protected int burstAmount = 3;
    protected int burstTimer = 20;

    public GunAttackGoal(T shooter, double stopRange, float speedModifier, AIType aiType, int difficulty) {
        this.shooter = shooter;
        this.speedModifier = speedModifier;
        this.attackTime = -1;
        this.attackRadiusSqr = (float) (stopRange * stopRange);
        this.aiType = aiType;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        if (this.shooter.getTarget() != null) {
            this.lastKnownPosition = this.shooter.getTarget().position();
        }

        this.spreadModifier /= difficulty;
        this.burstAmount *= difficulty;
        this.burstTimer /= difficulty;
    }

    @Override
    public boolean canUse() {
        if (this.shooter.getTarget() == null || !this.isHoldingGun() || this.shooter.getTarget().isDeadOrDying()) {
            return false;
        }

        // For Drowned gunners: only attack if in water or in shade
        if (this.shooter instanceof net.minecraft.world.entity.monster.Drowned && this.shooter.getTags().contains("DrownedGunner")) {
            return isInWaterOrShade();
        }

        return true;
    }

    /**
     * Check if the Drowned gunner is in water or in shade (favorable conditions)
     */
    private boolean isInWaterOrShade() {
        // Check if in water
        if (this.shooter.isInWater()) {
            return true;
        }

        // Check if in shade (not directly exposed to sunlight)
        BlockPos pos = this.shooter.blockPosition();
        boolean isRaining = this.shooter.level().isRaining();
        boolean isNight = !this.shooter.level().canSeeSky(pos);
        // Use day time to determine if it's dark (0 = noon, 12000 = midnight, 24000 = next noon)
        long dayTime = this.shooter.level().getDayTime() % 24000L;
        boolean isDarkTime = dayTime > 12500L && dayTime < 23500L; // Roughly sunset to sunrise

        return isRaining || isNight || !isDarkTime;
    }

    protected boolean isHoldingGun() {
        return this.shooter.getMainHandItem().getItem() instanceof GunItem;
    }

    @Override
    public void start() {
        super.start();
        this.shooter.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        this.shooter.setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.shooter.stopUsingItem();
        this.reloadTick = 0;
        this.isReloading = false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.shooter.getTarget();
        ItemStack heldItem = this.shooter.getMainHandItem();

        if (target != null && heldItem.getItem() instanceof GunItem gunItem) {
            GunStats stats = gunItem.getStats();

            double distanceToTarget = this.shooter.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean canSeeTarget = this.shooter.getSensing().hasLineOfSight(target) || BulletPenetrationHelper.hasLineOfSightThroughPenetrable(this.shooter, target);
            boolean sawTargetPreviously = this.seeTime > 0;

            if (canSeeTarget != sawTargetPreviously) {
                this.seeTime = 0;
            }

            if (this.isReloading) {
                ++this.seeTime;
            }
            else if (canSeeTarget) {
                this.lastKnownPosition = new Vec3(target.getX(), target.getY(), target.getZ());
                ++this.seeTime;
            } else {
                if (this.aiType == AIType.TACTICAL && this.lastKnownPosition != null) {
                    Vec3 flankingPosition = findFlankingPosition(this.lastKnownPosition, target);
                    if (flankingPosition != null) {
                        this.shooter.getNavigation().moveTo(flankingPosition.x, flankingPosition.y, flankingPosition.z, this.speedModifier);
                    }
                }
                --this.seeTime;
            }

            // Panic State
            if (this.aiType == AIType.COWARD &&
                (this.shooter.getHealth() < (this.shooter.getMaxHealth() / 3) || this.shooter.invulnerableTime != 0)) {
                this.isPanicked = true;
                this.panickTimer = 20;
            }

            if (this.isPanicked) {
                Vec3 vec3 = DefaultRandomPos.getPos(this.shooter, 5, 4);
                if (vec3 != null) {
                    this.shooter.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, this.speedModifier);
                }
                this.panickTimer--;
            }

            if (this.panickTimer <= 0) {
                this.isPanicked = false;
            }

            // Reloading
            if (stats.usesMagazine()) {
                Integer ammoComponent = heldItem.get(ModDataComponents.GUN_AMMO.get());
                int currentAmmo = ammoComponent != null ? ammoComponent : 0;
                if (currentAmmo <= 0) {
                    if (!this.isReloading) {
                        if (this.aiType != AIType.RECKLESS) {
                            Vec3 coverLocation = findCoverLocation();
                            this.shooter.getNavigation().moveTo(coverLocation.x, coverLocation.y, coverLocation.z, 1.2D);
                        }
                        this.isReloading = true;
                        this.reloadTick = Math.max(20, stats.totalReloadTime());
                        playReloadSound(stats);
                    } else if (this.reloadTick == 0) {
                        finishReload(heldItem, stats);
                        this.isReloading = false;
                    } else {
                        --this.reloadTick;
                    }
                }
            }

            // Flanking
            if (this.shooter.level().random.nextFloat() < 0.1 && this.aiType == AIType.TACTICAL) {
                Vec3 flankingPosition = findFlankingPosition(this.lastKnownPosition, target);
                if (flankingPosition != null) {
                    this.shooter.getNavigation().moveTo(flankingPosition.x, flankingPosition.y, flankingPosition.z, this.speedModifier);
                }
            }

            // Shooting & Strafing
            if (!this.isReloading && !this.isPanicked) {
                if (distanceToTarget <= this.attackRadiusSqr && this.seeTime >= 20) {
                    this.shooter.getNavigation().stop();
                    ++this.strafingTime;
                } else if (this.aiType == AIType.RECKLESS) {
                    this.shooter.getNavigation().moveTo(target, this.speedModifier);
                    this.strafingTime = -1;
                } else {
                    this.shooter.getNavigation().moveTo(target, 1.0F);
                    this.strafingTime = -1;
                }

                if (this.strafingTime >= 20) {
                    if (this.shooter.getRandom().nextFloat() < 0.3) {
                        this.strafingClockwise = !this.strafingClockwise;
                    }
                    if (this.shooter.getRandom().nextFloat() < 0.3) {
                        this.strafingBackwards = !this.strafingBackwards;
                    }
                    this.strafingTime = 0;
                }

                if (this.strafingTime > -1) {
                    if (distanceToTarget > (double)(this.attackRadiusSqr * 0.75F)) {
                        this.strafingBackwards = false;
                    } else if (distanceToTarget < (double)(this.attackRadiusSqr * 0.25F)) {
                        this.strafingBackwards = true;
                    }
                    this.shooter.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                }

                // Shooting
                boolean canShoot = stats.usesMagazine() ?
                    (heldItem.get(ModDataComponents.GUN_AMMO.get()) != null ? heldItem.get(ModDataComponents.GUN_AMMO.get()) : 0) > 0 : true;

                if (canShoot && --this.attackTime <= 0 && this.seeTime >= -20 && this.seeTime >= 10) {
                    shoot(target, gunItem, stats);
                    this.attackTime = Math.max(6, stats.fireDelay());
                }
            }

            // Look at target
            double targetEyeY = target.getEyeY();
            this.shooter.getLookControl().setLookAt(target.getX(), targetEyeY, target.getZ());
            this.shooter.lookAt(EntityAnchorArgument.Anchor.FEET, target.getBoundingBox().getCenter());
        }
    }

    protected void shoot(LivingEntity target, GunItem gunItem, GunStats stats) {
        ItemStack heldItem = this.shooter.getMainHandItem();
        RandomSource random = this.shooter.getRandom();
        int pellets = Math.max(1, stats.projectileAmount());
        ResourceLocation gunId = stats.id();
        int shotsPerBurst = 1;

        Vec3 origin = new Vec3(this.shooter.getX(), this.shooter.getEyeY(), this.shooter.getZ());

        boolean grenadeLauncher = gunId.equals(ResourceLocation.fromNamespaceAndPath("jeg", "grenade_launcher"));

        for (int burst = 0; burst < shotsPerBurst; burst++) {
            if (stats.usesMagazine()) {
                Integer ammoComponent = heldItem.get(ModDataComponents.GUN_AMMO.get());
                int currentAmmo = ammoComponent != null ? ammoComponent : 0;
                if (currentAmmo <= 0) {
                    break;
                }
                heldItem.set(ModDataComponents.GUN_AMMO.get(), Math.max(0, currentAmmo - 1));
            }

            for (int i = 0; i < pellets; i++) {
                Vec3 direction = computeDirection(this.shooter, origin, target, random, stats);
                Vec3 muzzle = origin.add(direction.scale(0.35F));

                if (grenadeLauncher) {
                    GrenadeEntity grenade = new GrenadeEntity(this.shooter.level(), this.shooter, 2.0F, 40, true);
                    grenade.initialisePosition(muzzle);
                    Vec3 launchVelocity = direction.scale(stats.projectileSpeed() * 0.8F);
                    grenade.setDeltaMovement(launchVelocity);
                    this.shooter.level().addFreshEntity(grenade);
                } else {
                    Vec3 velocity = direction.scale(stats.projectileSpeed());
                    BulletEntity bullet = new BulletEntity(this.shooter.level(), this.shooter, stats, velocity);
                    bullet.setPos(muzzle);
                    this.shooter.level().addFreshEntity(bullet);
                    if (this.shooter.level() instanceof ServerLevel serverLevel && GunItem.isBulletClassWeapon(stats.id())) {
                        bullet.sendTrailToClients(serverLevel);
                    }

                    // Add bullet trail particles for all guns EXCEPT flamethrower
                    if (!stats.flameTrail() && this.shooter.level() instanceof ServerLevel serverLevel) {
                        // Use penetration-aware raycast to spawn particles along actual bullet path
                        spawnBulletTrailParticles(serverLevel, muzzle, direction, stats);
                    }
                }
            }

            playGunshotSound(stats);
            InteractionHand usedHand = this.shooter.getUsedItemHand(); EquipmentSlot slot = usedHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND; heldItem.hurtAndBreak(1, this.shooter, slot);
        }
    }

    /**
     * Compute shooting direction with spread for each projectile
     * Uses the same spread calculation as player guns for consistency
     */
    private Vec3 computeDirection(LivingEntity shooter, Vec3 origin, LivingEntity target, RandomSource random, GunStats stats) {
        Vec3 base = target.getEyePosition().subtract(origin);
        return applyLegacySpread(shooter, base, stats, random);
    }

    /**
     * Apply spread to a direction vector using the same method as player guns
     */
    private static Vec3 applyLegacySpread(LivingEntity shooter, Vec3 baseDirection, GunStats stats, RandomSource random) {
        Vec3 forwards = baseDirection.normalize();
        if (forwards.lengthSqr() < 1.0E-6D) {
            forwards = shooter.getViewVector(1.0F);
        }

        float gunSpread = stats.spread();
        if (gunSpread == 0.0F) {
            return forwards.normalize();
        }

        if (!(shooter instanceof Player)) {
            float earlySpreadMultiplier = shooter.level().getDifficulty() != Difficulty.HARD ? 10.0F : 5.0F;
            float scaledSpreadMultiplier = Config.scaleGunnerSpreadMultiplier(shooter.level(), earlySpreadMultiplier);
            gunSpread *= scaledSpreadMultiplier;
            if (isShotgun(stats.id())) {
                gunSpread *= (float) Config.gunnerShotgunSpreadMultiplier();
            }
        }
        if (gunSpread <= 0.0F) {
            return forwards.normalize();
        }

        float spreadRadians = Math.min(gunSpread, 170.0F) * 0.5F * Mth.DEG_TO_RAD;
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 sideways = forwards.cross(worldUp);
        if (sideways.lengthSqr() < 1.0E-6D) {
            sideways = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideways = sideways.normalize();
        }
        Vec3 upwards = sideways.cross(forwards).normalize();

        float theta = random.nextFloat() * 2.0F * (float) Math.PI;
        float r = Mth.sqrt(random.nextFloat()) * (float) Math.tan(spreadRadians);
        float a1 = Mth.cos(theta) * r;
        float a2 = Mth.sin(theta) * r;

        return forwards.add(sideways.scale(a1)).add(upwards.scale(a2)).normalize();
    }

    private static boolean isShotgun(ResourceLocation gunId) {
        String path = gunId.getPath();
        return path.equals("double_barrel_shotgun")
                || path.equals("holy_shotgun")
                || path.equals("pump_shotgun")
                || path.equals("repeating_shotgun")
                || path.equals("supersonic_shotgun")
                || path.equals("waterpipe_shotgun");
    }

    protected void playGunshotSound(GunStats stats) {
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> this.shooter.level().playSound(null, this.shooter, sound, SoundSource.HOSTILE, 7.5F, 0.9F + this.shooter.level().random.nextFloat() * 0.2F),
                () -> this.shooter.level().playSound(null, this.shooter, net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 7.5F, 0.9F + this.shooter.level().random.nextFloat() * 0.2F)
        );
    }

    protected void playReloadSound(GunStats stats) {
        stats.reloadStartSoundEvent().ifPresent(sound ->
            this.shooter.level().playSound(null, this.shooter, sound, SoundSource.HOSTILE, 1.0F, 1.0F)
        );
    }

    protected void finishReload(ItemStack stack, GunStats stats) {
        if (stats.usesMagazine()) {
            stack.set(ModDataComponents.GUN_AMMO.get(), stats.magazineSize());
        }
        stats.reloadEndSoundEvent().ifPresent(sound ->
            this.shooter.level().playSound(null, this.shooter, sound, SoundSource.HOSTILE, 1.0F, 1.0F)
        );
    }

    private Vec3 findFlankingPosition(Vec3 lastKnownPosition, LivingEntity target) {
        Vec3 mobPos = this.shooter.position();

        if (lastKnownPosition == null) {
            return null;
        }

        Vec3 directionToTarget = lastKnownPosition.subtract(mobPos).normalize();

        for (int i = 0; i < 5; i++) {
            // Use simple up vector for NeoForge 1.21.10 compatibility
            Vec3 upVector = new Vec3(0, 1, 0);
            Vec3 offset = directionToTarget.cross(upVector).scale(3).add(randomOffset());
            Vec3 flankingPosition = mobPos.add(offset);

            if (this.shooter.level().getBlockState(BlockPos.containing(flankingPosition)).isAir() &&
                    this.shooter.getSensing().hasLineOfSight(target)) {
                return flankingPosition;
            }
        }
        return null;
    }

    private Vec3 randomOffset() {
        return new Vec3(
                this.shooter.getRandom().nextDouble() - 0.5,
                0,
                this.shooter.getRandom().nextDouble() - 0.5
        ).scale(1.5);
    }

    private Vec3 findCoverLocation() {
        Vec3 targetPos = new Vec3(this.shooter.getTarget().getX(), this.shooter.getTarget().getY(), this.shooter.getTarget().getZ());
        Vec3 mobPos = this.shooter.position();
        return mobPos.add(mobPos.subtract(targetPos).normalize().scale(3));
    }

    /**
     * Spawn bullet trail particles with penetration-aware raycast.
     * This is the EXACT SAME system that player shooting uses.
     */
    private void spawnBulletTrailParticles(ServerLevel level, Vec3 start, Vec3 direction, GunStats stats) {
        // Muzzle-only black dust with chance per shot to avoid constant spam.
        if (level.random.nextFloat() < 0.25F) {
            int count = level.random.nextFloat() < 0.35F ? 2 : 1;
            level.sendParticles(
                ParticleTypes.SMOKE,
                start.x, start.y, start.z,
                count, 0.02, 0.02, 0.02, 0.01
            );
        }
    }

}




