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
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.gun.BulletPenetrationHelper;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.gun.GunCategory;
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
        if (this.shooter instanceof Drowned && this.shooter.getTags().contains("DrownedGunner")) {
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
        float timeOfDay = this.shooter.level().getTimeOfDay(1.0F);
        boolean isDarkTime = timeOfDay > 0.25F && timeOfDay < 0.75F; // Roughly sunset to sunrise

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

        Vec3 origin = new Vec3(this.shooter.getX(), this.shooter.getEyeY(), this.shooter.getZ());

        boolean grenadeLauncher = gunId.equals(ResourceLocation.fromNamespaceAndPath("jeg", "grenade_launcher"));
        boolean flamethrower = gunId.equals(Reference.id("flamethrower"));

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
                if (flamethrower) {
                    velocity = velocity.add(0, -0.05, 0);
                }

                BulletEntity bullet = new BulletEntity(this.shooter.level(), this.shooter, stats, velocity);
                bullet.setPos(muzzle);
                this.shooter.level().addFreshEntity(bullet);

                // Add bullet trail particles for all guns EXCEPT flamethrower
                if (!flamethrower && this.shooter.level() instanceof ServerLevel serverLevel) {
                    // Use penetration-aware raycast to spawn particles along actual bullet path
                    spawnBulletTrailParticles(serverLevel, muzzle, direction, stats);
                }
            }
        }

        // Play sound
        playGunshotSound(stats);

        // Consume ammo
        if (stats.usesMagazine()) {
            Integer ammoComponent = heldItem.get(ModDataComponents.GUN_AMMO.get());
            int currentAmmo = ammoComponent != null ? ammoComponent : 0;
            heldItem.set(ModDataComponents.GUN_AMMO.get(), Math.max(0, currentAmmo - 1));
        }

        // Damage gun
        heldItem.hurtAndBreak(1, this.shooter, this.shooter.getUsedItemHand());
    }

    /**
     * Compute shooting direction with spread for each projectile
     * Uses the same spread calculation as player guns for consistency
     */
    private Vec3 computeDirection(LivingEntity shooter, Vec3 origin, LivingEntity target, RandomSource random, GunStats stats) {
        Vec3 base = target.getEyePosition().subtract(origin);
        GunCategory category = resolveCategory(stats);
        float baseSpread = stats.spread();
        float multiplier = shooter.isCrouching() ? category.crouchMultiplier() : category.hipMultiplier();
        float actualSpread = Math.max(0.0F, baseSpread * multiplier);

        // Add extra spread for non-player entities to balance difficulty
        if (!(shooter instanceof Player)) {
            actualSpread += 1.5F; // Reduced from 2.5F for better accuracy
            if (shooter instanceof Skeleton) {
                // Check if this skeleton was converted from a pillager
                if (shooter.getTags().contains("jeg_pillager_converted")) {
                    actualSpread += 1.0F; // Moderately reduced spread for pillager gunners
                } else {
                    actualSpread += 1.5F; // Normal spread for other gunners
                }
            }
            if (shooter instanceof ttv.migami.jeg.entity.monster.phantom.PhantomGunner) {
                actualSpread += 2.0F; // Reduced from 3.0F
            }
        }

        return applySpread(base, actualSpread, random);
    }

    /**
     * Apply spread to a direction vector using the same method as player guns
     */
    private Vec3 applySpread(Vec3 direction, float spreadDeg, RandomSource random) {
        Vec3 normalized = direction.normalize();
        if (spreadDeg <= 0.0F) {
            return normalized;
        }

        double deviation = Math.tan(Math.toRadians(spreadDeg));
        double offsetX = random.triangle(0.0D, deviation);
        double offsetY = random.triangle(0.0D, deviation * 0.5D);
        double offsetZ = random.triangle(0.0D, deviation);
        Vec3 jitter = new Vec3(offsetX, offsetY, offsetZ);
        return normalized.add(jitter).normalize();
    }

    /**
     * Resolve the gun category from stats for applying appropriate multipliers
     */
    private static GunCategory resolveCategory(GunStats stats) {
        return GunCategory.fromStats(stats);
    }

    protected void playGunshotSound(GunStats stats) {
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> this.shooter.level().playSound(null, this.shooter, sound, SoundSource.HOSTILE, 1.0F, 0.9F + this.shooter.level().random.nextFloat() * 0.2F),
                () -> this.shooter.level().playSound(null, this.shooter, net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.9F + this.shooter.level().random.nextFloat() * 0.2F)
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
        double maxRange = GunRangeHelper.computeEffectiveRange(stats);
        Vec3 motion = direction.scale(maxRange);
        Vec3 searchStart = start;

        int maxIterations = 10;
        for (int i = 0; i < maxIterations; i++) {
            Vec3 searchEnd = searchStart.add(motion);

            // Check block collision
            ClipContext clipContext = new ClipContext(
                searchStart,
                searchEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this.shooter
            );
            net.minecraft.world.phys.BlockHitResult blockHit = level.clip(clipContext);

            if (blockHit.getType() != HitResult.Type.BLOCK) {
                // No collision - spawn particles to max range
                spawnParticleSegment(level, searchStart, searchEnd);
                return;
            }

            BlockPos hitPos = blockHit.getBlockPos();
            BlockState hitState = level.getBlockState(hitPos);
            Vec3 hitLocation = blockHit.getLocation();
            boolean isPenetrable = BulletPenetrationHelper.isPenetrable(level, hitState);

            if (isPenetrable) {
                // Calculate exit point
                Vec3 dir = motion.normalize();
                Vec3 exitPoint = new Vec3(
                    hitPos.getX() + 0.5 + dir.x * 0.6,
                    hitPos.getY() + 0.5 + dir.y * 0.6,
                    hitPos.getZ() + 0.5 + dir.z * 0.6
                );

                spawnParticleSegment(level, searchStart, exitPoint);

                // Continue from exit point
                double distanceToHit = searchStart.distanceTo(hitLocation);
                double remainingDistance = searchStart.distanceTo(searchEnd) - distanceToHit;
                searchStart = exitPoint;
                motion = dir.scale(remainingDistance);
            } else {
                // Hit solid block - spawn particles and stop
                spawnParticleSegment(level, searchStart, hitLocation);
                return;
            }
        }
    }

    /**
     * Spawn particle trail from start to end position.
     * Same as GunItem's implementation.
     */
    private void spawnParticleSegment(ServerLevel level, Vec3 start, Vec3 end) {
        double distance = start.distanceTo(end);
        int particleCount = Math.min(20, Math.max(3, (int) (distance / 2.0)));

        for (int i = 1; i < particleCount; i++) { // Start from 1 instead of 0 to skip muzzle position
            double fraction = (double) i / particleCount;
            Vec3 pos = start.add(end.subtract(start).scale(fraction));

            // Fire particles
            level.sendParticles(
                ParticleTypes.FLAME,
                pos.x, pos.y, pos.z,
                1, 0.01, 0.01, 0.01, 0.005
            );

            // Smoke particles
            level.sendParticles(
                ParticleTypes.SMOKE,
                pos.x, pos.y, pos.z,
                1, 0.02, 0.02, 0.02, 0.01
            );
        }
    }
}