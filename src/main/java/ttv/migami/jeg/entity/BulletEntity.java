package ttv.migami.jeg.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.faction.GunnerFactionRelations;
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.item.BulletproofArmorItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.BulletTrailPayload;
import ttv.migami.jeg.network.NetworkHandler;

public class BulletEntity extends Projectile {
    private static final EntityDataAccessor<String> DATA_GUN = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FALLOFF_LIFE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRAIL_COLOR = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TRAIL_LENGTH = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final Identifier FLAMETHROWER_ID = Reference.id("flamethrower");
    private static final Identifier FLARE_GUN_ID = Reference.id("flare_gun");
    private static final Identifier ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher");
    private static final Identifier GRENADE_LAUNCHER_ID = Reference.id("grenade_launcher");
    private static final Identifier HYPERSONIC_ID = Reference.id("hypersonic_cannon");
    private static final Identifier TYPHOONEE_ID = Reference.id("typhoonee");
    private static final Identifier COMPOUND_BOW_ID = Reference.id("compound_bow");
    private static final Identifier PRIMITIVE_BOW_ID = Reference.id("primitive_bow");
    private static final Predicate<BlockState> IGNORE_LEAVES = state -> state != null
            && (state.getBlock() instanceof LeavesBlock || isGrassLikeFoliage(state));
    private static boolean isGrassLikeFoliage(BlockState state) {
        return state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN);
    }
    private static final double MIN_TRAIL_START_DISTANCE_SQR = 0.45D * 0.45D;
    private static final double TRAIL_SYNC_RANGE = 256.0D;
    private static final float ROCKET_EXPLOSION_POWER = 6.8F;
    private static final float ROCKET_DIRECT_HIT_DAMAGE = 70.0F;
    private static final float ROCKET_BLAST_BASE_DAMAGE = 32.0F;
    private static final double ROCKET_BLAST_RADIUS = 11.0D;
    private static final float ROCKET_BLAST_EDGE_FLOOR = 0.05F;
    private static final double ROCKET_BLAST_FALLOFF_EXPONENT = 3.4D;
    private static final float ROCKET_SELF_DAMAGE_SCALE = 0.55F;
    private static final String TERROR_RAID_MOB_TAG = "TerrorRaidMob";
    private static final EntityDataAccessor<Integer> DATA_TICKS_LIVED = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HIT_SOLID_BLOCK = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.BOOLEAN);

    // Client-side only: track if we've hit a solid block (to stop particle rendering permanently)
    private boolean clientHitSolidBlock = false;

    // Client-side rocket trail storage
    private List<Vec3> trailPositions;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        // CRITICAL: Disable physics so Projectile.tick() doesn't auto-handle collisions
        this.noPhysics = true;
    }

    public BulletEntity(Level level, LivingEntity shooter, GunStats stats, Vec3 velocity) {
        this(ModEntities.BULLET.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        this.entityData.set(DATA_GUN, stats.id().toString());
        this.entityData.set(DATA_DAMAGE, stats.damage());
        this.entityData.set(DATA_LIFE, Config.bulletLifetimeTicks());
        this.entityData.set(DATA_FALLOFF_LIFE, Math.max(1, stats.projectileLife()));
        this.entityData.set(DATA_TRAIL_COLOR, stats.trailColor());
        this.entityData.set(DATA_TRAIL_LENGTH, stats.clampedTrailLength());
        this.entityData.set(DATA_SIZE, stats.clampedProjectileSize());
        this.setVelocityAndRotation(velocity);
        this.setNoGravity(false);
        // DO NOT use noPhysics for flare gun - it prevents proper ticking
        this.refreshDimensions();
        this.setOldPosAndRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_GUN, Reference.id("assault_rifle").toString());
        builder.define(DATA_DAMAGE, 4.0F);
        builder.define(DATA_LIFE, 40);
        builder.define(DATA_FALLOFF_LIFE, 40);
        builder.define(DATA_TRAIL_COLOR, 0xFFFFFFFF);
        builder.define(DATA_TRAIL_LENGTH, 1.0F);
        builder.define(DATA_SIZE, 0.05F);
        builder.define(DATA_TICKS_LIVED, 0);
        builder.define(DATA_HIT_SOLID_BLOCK, false);
    }

    @Override
    public void tick() {
        // CRITICAL: Don't call super.tick()! Projectile.tick() auto-handles collisions
        // and will discard the entity before our penetration logic runs
        // Instead, manually do basic entity updates
        this.baseTick(); // Basic entity updates (fire, water, etc.)

        // If entity was already discarded, don't process
        if (!this.isAlive()) {
            return;
        }

        Identifier gunId = Identifier.parse(this.entityData.get(DATA_GUN));
        GunStats gunStats = getGunStats();

        // 1.20.1-style flare projectile: no timed detonation, just trail + ignition impacts.
        if (gunId.equals(FLARE_GUN_ID)) {
            int ticksLived = this.entityData.get(DATA_TICKS_LIVED);
            this.entityData.set(DATA_TICKS_LIVED, ticksLived + 1);

            Vec3 motion = this.getDeltaMovement();
            HitResult collisionResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (collisionResult.getType() != HitResult.Type.MISS) {
                if (collisionResult.getType() == HitResult.Type.ENTITY) {
                    this.onHitEntity((EntityHitResult) collisionResult);
                } else if (collisionResult.getType() == HitResult.Type.BLOCK) {
                    this.onHitBlock((BlockHitResult) collisionResult);
                }
                return;
            }

            if (this.level().isClientSide()) {
                spawnFlareParticles();
            }

            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            this.setDeltaMovement(applyGravity(motion, gunStats));

            if (this.entityData.get(DATA_TICKS_LIVED) > this.entityData.get(DATA_LIFE)) {
                this.discard();
            }
            return;
        }

        // Normal bullet logic below (not flare gun)
        // Increment ticksLived using synced data
        int ticksLived = this.entityData.get(DATA_TICKS_LIVED);
        this.entityData.set(DATA_TICKS_LIVED, ticksLived + 1);

        Vec3 motion = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(motion);
        boolean legacyBulletClass = shouldSendBulletTrail(gunStats);

        // SERVER SIDE: Full collision detection with continuous penetration checking
        if (!this.level().isClientSide()) {
            Vec3 remainingMotion = motion;
            Vec3 searchStart = currentPos;
            boolean ignoreLeavesForGun = !gunId.equals(FLAMETHROWER_ID);

            // Loop to handle multiple penetrable blocks in the path
            int maxIterations = 20; // Prevent infinite loops
            for (int i = 0; i < maxIterations; i++) {
                // Calculate search end point
                Vec3 searchEnd = searchStart.add(remainingMotion);

                // Perform precise block raycast that allows bullets to pass through leaves gaps
                BlockHitResult blockRaycast = performPreciseBlockRaycast(searchStart, searchEnd, ignoreLeavesForGun);
                Vec3 entitySearchEnd = blockRaycast.getType() == HitResult.Type.BLOCK ? blockRaycast.getLocation() : searchEnd;

                if (legacyBulletClass) {
                    EntityHitResult entityHit = findClosestEntityHit(searchStart, entitySearchEnd);
                    if (entityHit != null) {
                        Vec3 hitLoc = entityHit.getLocation();
                        this.setPos(hitLoc.x, hitLoc.y, hitLoc.z);
                        this.onHitEntity(entityHit);
                        return;
                    }
                } else {
                    // For non-legacy bullet classes, keep existing move-vector entity check.
                    Vec3 savedPos = this.position();
                    this.setPos(searchStart.x, searchStart.y, searchStart.z);
                    this.setDeltaMovement(remainingMotion);
                    HitResult entityHitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
                    this.setPos(savedPos.x, savedPos.y, savedPos.z);
                    this.setDeltaMovement(motion);

                    if (entityHitResult.getType() == HitResult.Type.ENTITY) {
                        Vec3 hitLoc = entityHitResult.getLocation();
                        this.setPos(hitLoc.x, hitLoc.y, hitLoc.z);
                        this.onHitEntity((EntityHitResult) entityHitResult);
                        return;
                    }
                }

                // Check block raycast result
                if (blockRaycast.getType() != HitResult.Type.BLOCK) {
                    break;
                }

                BlockPos hitPos = blockRaycast.getBlockPos();
                BlockState hitState = this.level().getBlockState(hitPos);
                Vec3 hitLocation = blockRaycast.getLocation();
                boolean ignoredLeaves = ignoreLeavesForGun && IGNORE_LEAVES.test(hitState);
                boolean isPenetrable = ignoredLeaves || ttv.migami.jeg.gun.BulletPenetrationHelper.isPenetrable(
                    this.level(), hitState);
                if (!ignoreLeavesForGun && IGNORE_LEAVES.test(hitState)) {
                    isPenetrable = false;
                }

                if (isPenetrable) {
                    // Penetrable block - send trail through it and continue
                    Vec3 direction = remainingMotion.normalize();
                    double distanceToHit = searchStart.distanceTo(hitLocation);
                    double remainingDistance = searchStart.distanceTo(searchEnd) - distanceToHit;

                    // Calculate the exit point by moving to the block boundary
                    Vec3 exitPoint = new Vec3(
                        hitPos.getX() + 0.5 + direction.x * 0.6,  // Center + half block + margin
                        hitPos.getY() + 0.5 + direction.y * 0.6,
                        hitPos.getZ() + 0.5 + direction.z * 0.6
                    );

                    // Update for next iteration
                    searchStart = exitPoint;
                    remainingMotion = direction.scale(remainingDistance);

                    // Continue loop to check for more collisions
                } else {
                    // Solid block - stop
                    this.entityData.set(DATA_HIT_SOLID_BLOCK, true);  // Sync to client!

                    // Set position to hit location before processing collision
                    this.setPos(hitLocation.x, hitLocation.y, hitLocation.z);
                    this.onHitBlock(blockRaycast);
                    return; // Bullet stopped
                }
            }

            // Move bullet to final position (if not stopped by solid block)
            this.setPos(nextPos.x, nextPos.y, nextPos.z);

            this.setDeltaMovement(applyGravity(motion, gunStats));
        }
        // CLIENT SIDE: Handle particle effects and movement
        else {
            // Client just moves the entity for hitbox synchronization
            this.setPos(nextPos.x, nextPos.y, nextPos.z);

            // Spawn fire particles for weapons configured for flame trails.
            if (gunStats.flameTrail()) {
                spawnFlameParticles();
            }

            // Spawn rocket trail particles on client side (3x density)
            else if (gunId.equals(ROCKET_LAUNCHER_ID)) {
                spawnRocketTrailParticles();
            }
            else if (gunId.equals(TYPHOONEE_ID)) {
                spawnTyphooneeTrailParticles();
            }

            this.setDeltaMovement(applyGravity(motion, gunStats));
        }

        // Check lifetime (both sides)
        if (this.entityData.get(DATA_TICKS_LIVED) > this.entityData.get(DATA_LIFE)) {
            if (!this.level().isClientSide()) {
                if (gunId.equals(FLAMETHROWER_ID)) {
                    igniteArea(this.blockPosition());
                }
            }
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Entity owner = this.getOwner();

        // 1.20.1 flare parity: direct hits ignite target, no explosion detonation.
        Identifier gunId = Identifier.parse(this.entityData.get(DATA_GUN));
        if (gunId.equals(FLARE_GUN_ID)) {
            if (!this.level().isClientSide()) {
                Entity hitEntity = result.getEntity();
                LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
                if (livingOwner != null && hitEntity instanceof LivingEntity livingTarget && isFriendlyFire(livingOwner, livingTarget)) {
                    this.discard();
                    return;
                }
                DamageSource source = livingOwner != null
                        ? this.damageSources().mobProjectile(this, livingOwner)
                        : this.damageSources().thrown(this, owner);
                if (hitEntity instanceof LivingEntity living) {
                    applyBulletproofWear(living);
                    boolean hurt = living.hurtServer((ServerLevel) this.level(), source, this.entityData.get(DATA_DAMAGE));
                    if (hurt && livingOwner instanceof ServerPlayer shooter) {
                        NetworkHandler.sendHitMarker(shooter, isCriticalHit(result, living));
                    }
                } else {
                    hitEntity.hurt(source, this.entityData.get(DATA_DAMAGE));
                }
                if (hitEntity instanceof LivingEntity living) {
                    living.igniteForSeconds(5);
                } else {
                    hitEntity.igniteForSeconds(5);
                }
            }
            this.discard();
            return;
        }

        // Special weapons (explosives, flamethrower) handle their own logic
        if (handleSpecialImpact(result)) {
            this.discard();
            return;
        }

        // Normal bullet damage logic
        if (!this.level().isClientSide()) {
            float damage = applyNormalDamageFalloff(this.entityData.get(DATA_DAMAGE), getGunStats());

            // Reduce damage for Terror Phantom and Phantom Gunner to balance fire rate (5 shots/sec)
            if (owner instanceof AbstractTerrorPhantom || owner instanceof PhantomGunner) {
                damage *= 0.3F; // Reduce damage by 70% to compensate for sustained fire
            }

            LivingEntity livingOwner = owner instanceof LivingEntity ? (LivingEntity) owner : null;
            if (livingOwner != null && entity instanceof LivingEntity livingTarget && isFriendlyFire(livingOwner, livingTarget)) {
                this.discard();
                return;
            }

            boolean creativeShooter = livingOwner instanceof Player player && (player.isCreative() || player.isSpectator());
            // Use direct damage source to ensure proper aggro attribution at all ranges
            DamageSource source;
            if (livingOwner != null) {
                source = creativeShooter ? this.damageSources().thrown(this, null) : this.damageSources().mobProjectile(this, livingOwner);
            } else {
                source = this.damageSources().thrown(this, owner);
            }

            if (entity instanceof LivingEntity living) {
                applyBulletproofWear(living);

                ServerLevel serverLevel = (ServerLevel) this.level();
                boolean hurt = living.hurtServer(serverLevel, source, damage);
                if (hurt && livingOwner instanceof ServerPlayer shooter) {
                    NetworkHandler.sendHitMarker(shooter, isCriticalHit(result, living));
                }

                boolean raidFriendlyPair = livingOwner != null && isRaidFriendlyPair(livingOwner, living);

                if (creativeShooter) {
                    living.setLastHurtByMob(null);
                    if (living instanceof Mob mob && livingOwner instanceof Player player && mob.getTarget() == player) {
                        mob.setTarget(null);
                    }
                } else if (!raidFriendlyPair && livingOwner instanceof Player player) {
                    living.setLastHurtByMob(player);

                    if (!player.isCreative() && !player.isSpectator() && living instanceof Mob mob) {
                        double followRange = getEffectiveFollowRange(mob);
                        if (mob.distanceToSqr(player) <= followRange * followRange) {
                            mob.setTarget(player);
                            mob.setAggressive(true);
                            mob.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ());
                        }
                    }
                } else if (!raidFriendlyPair && livingOwner != null) {
                    living.setLastHurtByMob(livingOwner);
                    if (living instanceof Mob mob) {
                        if (mob.getSensing().hasLineOfSight(livingOwner) || mob.getTarget() != null) {
                            mob.setTarget(livingOwner);
                        }
                    }
                }
            } else {
                entity.hurt(source, damage);
            }

            // 1.20.1 parity: allow shotgun pellets from players to apply within the same tick.
            if (owner instanceof Player) {
                entity.invulnerableTime = 0;
            }
        }

        this.discard();
    }

    private static boolean isCriticalHit(EntityHitResult result, LivingEntity living) {
        return result.getLocation().y >= living.getEyeY() - 0.20D;
    }

    private float applyNormalDamageFalloff(float baseDamage, GunStats stats) {
        if (GunRangeHelper.isRangeExempt(stats)) {
            return baseDamage;
        }

        double effectiveRange = GunRangeHelper.computeEffectiveRange(stats);
        if (effectiveRange <= 0.0D) {
            return baseDamage;
        }

        double traveled = Math.max(0, this.tickCount - 1) * Math.max(0.0D, stats.projectileSpeed());
        double fullDamageRange = effectiveRange * 0.7D;
        if (traveled <= fullDamageRange) {
            return baseDamage;
        }

        double scale;
        if (traveled <= effectiveRange) {
            double t = Mth.clamp((traveled - fullDamageRange) / Math.max(0.001D, effectiveRange - fullDamageRange), 0.0D, 1.0D);
            scale = Mth.lerp(t, 1.0D, 0.9D);
        } else if (traveled <= effectiveRange * 1.8D) {
            double t = Mth.clamp((traveled - effectiveRange) / Math.max(0.001D, effectiveRange * 0.8D), 0.0D, 1.0D);
            scale = 0.9D - 0.8D * t * t;
        } else {
            scale = 0.1D;
        }

        return Math.max(0.0F, baseDamage * (float) scale);
    }

    private boolean isFriendlyFire(LivingEntity owner, LivingEntity target) {
        if (owner == target) {
            return true;
        }
        if (isRaidFriendlyPair(owner, target)) {
            return true;
        }
        if (GunnerFactionRelations.areSameFactionGunners(owner, target)) {
            return true;
        }
        if (owner.isAlliedTo(target)) {
            return true;
        }
        if (owner instanceof Mob ownerMob && target instanceof Mob targetMob) {
            if (ownerMob.getType() == targetMob.getType()) {
                return true;
            }
        }
        return false;
    }

    private boolean isRaidFriendlyPair(LivingEntity owner, LivingEntity target) {
        return owner.entityTags().contains(TERROR_RAID_MOB_TAG) && target.entityTags().contains(TERROR_RAID_MOB_TAG);
    }

    private void applyBulletproofWear(LivingEntity target) {
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST }) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty() && BulletproofArmorItem.isBulletproof(stack)) {
                stack.hurtAndBreak(1, target, slot);
            }
        }
    }

    private static double getEffectiveFollowRange(Mob mob) {
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            return Math.max(16.0D, followRange.getValue());
        }
        return 16.0D;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) {
            return false;
        }

        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner && entity instanceof LivingEntity livingTarget) {
            return !isFriendlyFire(livingOwner, livingTarget);
        }

        return true;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // This method should only be called for SOLID blocks (penetrable blocks are filtered in tick())
        Identifier gunId = Identifier.parse(this.entityData.get(DATA_GUN));

        // 1.20.1 flare parity: chance to ignite nearby block on impact.
        if (gunId.equals(FLARE_GUN_ID)) {
            if (!this.level().isClientSide()) {
                BlockPos hitPos = result.getBlockPos();
                BlockPos offsetPos = hitPos.relative(result.getDirection());
                if (this.random.nextFloat() > 0.50F
                        && BaseFireBlock.canBePlacedAt(this.level(), offsetPos, result.getDirection())) {
                    BlockState fireState = BaseFireBlock.getState(this.level(), offsetPos);
                    this.level().setBlock(offsetPos, fireState, 11);
                    ((ServerLevel) this.level()).sendParticles(
                            ParticleTypes.LAVA,
                            result.getLocation().x,
                            result.getLocation().y,
                            result.getLocation().z,
                            4,
                            0.4D,
                            0.0D,
                            0.4D,
                            0.0D
                    );
                }
            }
            this.discard();
            return;
        }

        BlockPos hitPos = result.getBlockPos();
        BlockState hitState = this.level().getBlockState(hitPos);

        // Handle special weapons (explosives, flamethrower, etc.)
        if (handleSpecialImpact(result)) {
            super.onHitBlock(result);
            this.discard();
            return;
        }

        // Handle block destruction on server side only (for normal bullets)
        if (!this.level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            GunStats stats = getGunStats();

            if (Config.blockHitAnimationEnabled()) {
                serverLevel.levelEvent(2001, hitPos, Block.getId(hitState));
            }

            if (Config.bulletBlockDestructionEnabled()) {
                // Try to destroy the block based on tier and bullet power
                ttv.migami.jeg.gun.BulletPenetrationHelper.tryDestroyBlock(
                    serverLevel, hitPos, stats
                );
            }
        }

        // Block stopped the bullet - call super and discard it
        super.onHitBlock(result);
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("GunId", this.entityData.get(DATA_GUN));
        output.putFloat("Damage", this.entityData.get(DATA_DAMAGE));
        output.putInt("Life", this.entityData.get(DATA_LIFE));
        output.putInt("TrailColor", this.entityData.get(DATA_TRAIL_COLOR));
        output.putFloat("TrailLength", this.entityData.get(DATA_TRAIL_LENGTH));
        output.putFloat("ProjectileSize", this.entityData.get(DATA_SIZE));
        output.putInt("Ticks", this.entityData.get(DATA_TICKS_LIVED));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_GUN, input.getStringOr("GunId", Reference.id("assault_rifle").toString()));
        this.entityData.set(DATA_DAMAGE, input.getFloatOr("Damage", this.entityData.get(DATA_DAMAGE)));
        this.entityData.set(DATA_LIFE, input.getIntOr("Life", this.entityData.get(DATA_LIFE)));
        this.entityData.set(DATA_TRAIL_COLOR, input.getIntOr("TrailColor", this.entityData.get(DATA_TRAIL_COLOR)));
        this.entityData.set(DATA_TRAIL_LENGTH, input.getFloatOr("TrailLength", this.entityData.get(DATA_TRAIL_LENGTH)));
        this.entityData.set(DATA_SIZE, input.getFloatOr("ProjectileSize", this.entityData.get(DATA_SIZE)));
        this.entityData.set(DATA_TICKS_LIVED, input.getIntOr("Ticks", this.entityData.get(DATA_TICKS_LIVED)));
        this.entityData.set(DATA_LIFE, Config.bulletLifetimeTicks());
        this.refreshDimensions();
        this.setVelocityAndRotation(this.getDeltaMovement());
    }

    public GunStats getGunStats() {
        Identifier id = Identifier.parse(this.entityData.get(DATA_GUN));
        GunStats stats = GunDefinitions.ALL.get(id);
        if (stats == null) {
            return new GunStats(id, null, "jeg:mag_fed", 1, 20, 0, 10, this.entityData.get(DATA_DAMAGE), 4F, this.entityData.get(DATA_LIFE), true, false, 0F, 1, null, null, null, null, null, null, null, null, this.entityData.get(DATA_SIZE), this.entityData.get(DATA_TRAIL_COLOR), this.entityData.get(DATA_TRAIL_LENGTH));
        }
        return stats;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    public int getTrailColor() {
        return this.entityData.get(DATA_TRAIL_COLOR);
    }

    public float getTrailLengthMultiplier() {
        return this.entityData.get(DATA_TRAIL_LENGTH);
    }

    public float getProjectileSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public boolean hasHitSolidBlock() {
        return this.entityData.get(DATA_HIT_SOLID_BLOCK);
    }

    public String getGunId() {
        return this.entityData.get(DATA_GUN);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float diameter = Mth.clamp(this.getProjectileSize(), 0.05F, 1.0F);
        return EntityDimensions.scalable(diameter, diameter).withEyeHeight(0.0F);
    }

    public void initialisePosition(Vec3 position) {
        this.setPos(position);
        this.setOldPosAndRot();
    }

    private void setVelocityAndRotation(Vec3 velocity) {
        this.setDeltaMovement(velocity);
        double length = velocity.length();
        if (length <= 1.0E-5D) {
            return;
        }

        Vec3 normalized = velocity.scale(1.0D / length);
        float yaw = (float)(Mth.atan2(normalized.x, normalized.z) * (180F / Math.PI));
        float pitch = (float)(Mth.atan2(normalized.y, Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z)) * (180F / Math.PI));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    private boolean handleSpecialImpact(HitResult result) {
        GunStats stats = getGunStats();
        Identifier id = stats.id();

        if (id.equals(FLAMETHROWER_ID)) {
            if (!this.level().isClientSide()) {
                if (result instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
                    Entity hitEntity = entityHit.getEntity();
                    if (hitEntity instanceof LivingEntity living) {
                        living.igniteForSeconds(6);
                    } else {
                        hitEntity.igniteForSeconds(6);
                    }
                }
                igniteArea(BlockPos.containing(result.getLocation()));
            }
            return true;
        }

        if (id.equals(ROCKET_LAUNCHER_ID) || id.equals(HYPERSONIC_ID) || id.equals(TYPHOONEE_ID)) {
            if (!this.level().isClientSide()) {
                float power = 6.0F;
                float directDamage = 14.0F;
                if (id.equals(ROCKET_LAUNCHER_ID)) {
                    power = ROCKET_EXPLOSION_POWER;
                    directDamage = ROCKET_DIRECT_HIT_DAMAGE;
                }
                if (id.equals(HYPERSONIC_ID)) {
                    power = 7.5F;
                    directDamage = 18.0F;
                } else if (id.equals(TYPHOONEE_ID)) {
                    power = 6.75F;
                    directDamage = 16.0F;
                }

                Entity owner = this.getOwner();
                spawnCustomExplosionEffects((ServerLevel) this.level(), this.position(), id);
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), power, ExplosionInteraction.TNT);
                if (id.equals(ROCKET_LAUNCHER_ID)) {
                    applyRocketBlastDamage(owner);
                }

                if (result instanceof EntityHitResult entityHit) {
                    Entity hitEntity = entityHit.getEntity();
                    if (hitEntity.isAlive()) {
                        DamageSource source = this.damageSources().explosion(this, owner instanceof LivingEntity living ? living : null);
                        if (hitEntity instanceof LivingEntity living) {
                            boolean hurt = living.hurtServer((ServerLevel) this.level(), source, directDamage);
                            if (hurt && owner instanceof ServerPlayer shooter) {
                                NetworkHandler.sendHitMarker(shooter, isCriticalHit(entityHit, living));
                            }
                        } else {
                            hitEntity.hurt(source, directDamage);
                        }
                    }
                }
            }
            return true;
        }

        return false;
    }

    private void applyRocketBlastDamage(@Nullable Entity owner) {
        if (this.level().isClientSide()) {
            return;
        }

        AABB area = this.getBoundingBox().inflate(ROCKET_BLAST_RADIUS);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (!target.isAlive()) {
                continue;
            }

            double distance = target.distanceTo(this);
            if (distance > ROCKET_BLAST_RADIUS) {
                continue;
            }

            double t = 1.0D - (distance / ROCKET_BLAST_RADIUS);
            double curve = Math.pow(Math.max(0.0D, t), ROCKET_BLAST_FALLOFF_EXPONENT);
            float scale = (float) (ROCKET_BLAST_EDGE_FLOOR + (1.0F - ROCKET_BLAST_EDGE_FLOOR) * curve);
            float damage = ROCKET_BLAST_BASE_DAMAGE * scale;

            if (owner != null && target == owner) {
                damage *= ROCKET_SELF_DAMAGE_SCALE;
            }

            if (damage > 0.5F) {
                target.hurt(this.damageSources().explosion(this, owner instanceof LivingEntity living ? living : null), damage);
            }
        }
    }

    private void spawnCustomExplosionEffects(ServerLevel serverLevel, Vec3 pos, Identifier weaponId) {
        int bigCount = weaponId.equals(HYPERSONIC_ID) ? 3 : 2;
        int smallCount = weaponId.equals(HYPERSONIC_ID) ? 22 : 16;

        sendLongDistanceParticles(serverLevel, ModParticleTypes.BIG_EXPLOSION.get(), pos.x, pos.y, pos.z, bigCount, 0.2D, 0.2D, 0.2D, 0.01D);
        sendLongDistanceParticles(serverLevel, ModParticleTypes.SMALL_EXPLOSION.get(), pos.x, pos.y, pos.z, smallCount, 1.1D, 1.1D, 1.1D, 0.12D);
        sendLongDistanceParticles(serverLevel, ModParticleTypes.SMOKE.get(), pos.x, pos.y, pos.z, 12, 1.2D, 1.2D, 1.2D, 0.02D);
        sendLongDistanceParticles(serverLevel, ModParticleTypes.FIRE.get(), pos.x, pos.y, pos.z, 10, 0.9D, 0.9D, 0.9D, 0.04D);
    }

    private static <T extends ParticleOptions> void sendLongDistanceParticles(
            ServerLevel serverLevel,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double xOffset,
            double yOffset,
            double zOffset,
            double speed
    ) {
        for (ServerPlayer player : serverLevel.players()) {
            serverLevel.sendParticles(player, particle, true, false, x, y, z, count, xOffset, yOffset, zOffset, speed);
        }
    }

    private void igniteArea(BlockPos center) {
        Level level = this.level();
        if (level.isClientSide()) {
            return;
        }

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 1, 1))) {
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            if (!Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                continue;
            }
            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        }
    }

    private void spawnFlameParticles() {
        Level level = this.level();
        if (!level.isClientSide()) {
            return;
        }

        // Spawn particles from the very beginning to start from gun muzzle
        // No delay - particles should appear immediately from gun muzzle

        // Check if current position is inside a solid block
        BlockPos currentPos = this.blockPosition();
        BlockState stateAtPos = level.getBlockState(currentPos);

        // Only spawn particles if we're in air or penetrable blocks
        if (!stateAtPos.isAir() && !ttv.migami.jeg.gun.BulletPenetrationHelper.isPenetrable(level, stateAtPos)) {
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();

        // Significantly increase flame particle density for much more impressive effect
        // Spawn 15-25 particles per tick instead of 5 (3x-5x increase)
        int particleCount = 15 + this.random.nextInt(11); // 15-25 particles

        for (int i = 0; i < particleCount; i++) {
            // Slightly reduce spread for more concentrated stream
            double offsetX = (this.random.nextDouble() - 0.5) * 0.3; // Reduced from 0.4
            double offsetY = (this.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.3;

            // Add more velocity variation for dynamic flame effect
            double velX = motion.x * 0.15 + (this.random.nextDouble() - 0.5) * 0.08;
            double velY = motion.y * 0.15 + 0.03 + this.random.nextDouble() * 0.02; // Stronger upward bias
            double velZ = motion.z * 0.15 + (this.random.nextDouble() - 0.5) * 0.08;

            // Mix of flame and smoke particles for more realistic effect
            if (this.random.nextFloat() < 0.7) {
                // 70% flame particles
                level.addParticle(ParticleTypes.FLAME,
                    this.getX() + offsetX,
                    this.getY() + offsetY,
                    this.getZ() + offsetZ,
                    velX, velY, velZ);
            } else {
                // 30% smoke particles
                level.addParticle(ParticleTypes.SMOKE,
                    this.getX() + offsetX,
                    this.getY() + offsetY,
                    this.getZ() + offsetZ,
                    velX * 0.5, velY * 0.8, velZ * 0.5);
            }
        }

        // Add some extra large flame particles occasionally for visual variety
        if (this.random.nextFloat() < 0.3) { // 30% chance
            for (int i = 0; i < 3; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.2;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.2;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.2;

                level.addParticle(ParticleTypes.FLAME,
                    this.getX() + offsetX,
                    this.getY() + offsetY,
                    this.getZ() + offsetZ,
                    motion.x * 0.1, motion.y * 0.1, motion.z * 0.1);
            }
        }

        // Add more ember particles for extra visibility and dramatic effect
        if (this.random.nextFloat() < 0.5) { // 50% chance (increased from 25%)
            level.addParticle(ParticleTypes.SMALL_FLAME,
                this.getX() + (this.random.nextDouble() - 0.5) * 0.4,
                this.getY() + (this.random.nextDouble() - 0.5) * 0.4,
                this.getZ() + (this.random.nextDouble() - 0.5) * 0.4,
                motion.x * 0.1 + (this.random.nextDouble() - 0.5) * 0.05,
                motion.y * 0.1 + 0.05,
                motion.z * 0.1 + (this.random.nextDouble() - 0.5) * 0.05);
        }
    }

    private void spawnBulletTrailParticlesAlongPath(Vec3 motion) {
        Level level = this.level();
        if (!level.isClientSide()) {
            return;
        }

        // Check if current position is inside a solid block - don't spawn particles there
        BlockPos currentPos = this.blockPosition();
        BlockState stateAtPos = level.getBlockState(currentPos);

        // Only spawn particles if we're in air or penetrable blocks
        if (stateAtPos.isAir() || ttv.migami.jeg.gun.BulletPenetrationHelper.isPenetrable(level, stateAtPos)) {
            // Only spawn 1 smoke particle at current position per tick
            // This creates a thin continuous trail as the bullet moves
            // The BulletRenderer handles the visual trail line
            level.addParticle(ParticleTypes.SMOKE,
                this.getX(),
                this.getY(),
                this.getZ(),
                0, 0, 0);
        }
    }

    private void spawnFlareParticles() {
        Level level = this.level();
        if (!level.isClientSide()) {
            return;
        }

        int life = this.entityData.get(DATA_LIFE);
        if (this.tickCount <= 1 || this.tickCount >= life) {
            return;
        }

        // Check if current position is inside a solid block
        BlockPos currentPos = this.blockPosition();
        BlockState stateAtPos = level.getBlockState(currentPos);

        // Only spawn particles if we're in air or penetrable blocks
        if (!stateAtPos.isAir() && !ttv.migami.jeg.gun.BulletPenetrationHelper.isPenetrable(level, stateAtPos)) {
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        double px = this.getX() - motion.x;
        double py = this.getY() - motion.y;
        double pz = this.getZ() - motion.z;

        level.addParticle(ModParticleTypes.FLARE_SMOKE.get(), px, py, pz, 0.0D, 0.0D, 0.0D);
        level.addParticle(ModParticleTypes.FIRE.get(), px, py, pz, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.LAVA, px, py, pz, 0.0D, 0.0D, 0.0D);
    }

    private void spawnRocketTrailParticles() {
        Level level = this.level();
        if (!level.isClientSide()) {
            return;
        }

        // Spawn particles from the very beginning to start from gun muzzle
        // No delay - smoke trail should appear immediately from rocket launcher muzzle

        // Create continuous smoke trail from start to current position
        createRocketSmokeTrail(level);

        // Spawn fire particles at current position
        level.addParticle(ParticleTypes.FLAME,
            this.getX(),
            this.getY(),
            this.getZ(),
            0, 0, 0);

        level.addParticle(ParticleTypes.LAVA,
            this.getX(),
            this.getY(),
            this.getZ(),
            0, 0, 0);
    }

    private void spawnTyphooneeTrailParticles() {
        Level level = this.level();
        if (!level.isClientSide() || !canSpawnClientTrailParticles(level)) {
            return;
        }

        createTyphooneeTrail(level);

        Vec3 motion = this.getDeltaMovement();
        double px = this.getX() - motion.x * 0.35D;
        double py = this.getY() - motion.y * 0.35D;
        double pz = this.getZ() - motion.z * 0.35D;

        if (this.isUnderWater()) {
            level.addParticle(ParticleTypes.BUBBLE, px, py, pz, 0.0D, 0.02D, 0.0D);
            level.addParticle(ParticleTypes.BUBBLE_POP, px, py, pz, 0.0D, 0.0D, 0.0D);
        } else {
            level.addParticle(ParticleTypes.SPLASH, px, py, pz, 0.0D, 0.02D, 0.0D);
            level.addParticle(ParticleTypes.FALLING_WATER, px, py, pz, 0.0D, 0.0D, 0.0D);
            if (this.random.nextFloat() < 0.35F) {
                level.addParticle(ParticleTypes.CLOUD, px, py, pz, 0.0D, 0.01D, 0.0D);
            }
        }
    }

    private void createRocketSmokeTrail(Level level) {
        // Store trail positions for smoother effect
        if (this.trailPositions == null) {
            this.trailPositions = new ArrayList<>();
        }

        // Add current position to trail
        this.trailPositions.add(this.position());

        // Limit trail length to prevent performance issues
        int maxTrailLength = 30;
        if (this.trailPositions.size() > maxTrailLength) {
            this.trailPositions.remove(0);
        }

        // Generate smoke particles along the trail
        for (int i = 0; i < this.trailPositions.size(); i++) {
            Vec3 trailPos = this.trailPositions.get(i);

            // Fade older trail positions (fewer particles for older positions)
            int particleCount = Math.max(1, 3 - (this.trailPositions.size() - i) / 10);

            for (int j = 0; j < particleCount; j++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 0.4;
                double offsetY = (this.random.nextDouble() - 0.5) * 0.4;
                double offsetZ = (this.random.nextDouble() - 0.5) * 0.4;

                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    trailPos.x + offsetX,
                    trailPos.y + offsetY,
                    trailPos.z + offsetZ,
                    (this.random.nextDouble() - 0.5) * 0.02,
                    0.01 + this.random.nextDouble() * 0.02,
                    (this.random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    private void createTyphooneeTrail(Level level) {
        if (this.trailPositions == null) {
            this.trailPositions = new ArrayList<>();
        }

        this.trailPositions.add(this.position());

        int maxTrailLength = Math.max(12, Mth.ceil(this.getTrailLengthMultiplier() * 14.0F));
        if (this.trailPositions.size() > maxTrailLength) {
            this.trailPositions.remove(0);
        }

        boolean underwater = this.isUnderWater();
        for (int i = 0; i < this.trailPositions.size(); i++) {
            Vec3 trailPos = this.trailPositions.get(i);
            int ageOffset = this.trailPositions.size() - i;
            int particleCount = ageOffset <= 4 ? 2 : 1;

            for (int j = 0; j < particleCount; j++) {
                double offsetX = (this.random.nextDouble() - 0.5D) * 0.18D;
                double offsetY = (this.random.nextDouble() - 0.5D) * 0.18D;
                double offsetZ = (this.random.nextDouble() - 0.5D) * 0.18D;
                if (underwater) {
                    level.addParticle(ParticleTypes.BUBBLE,
                            trailPos.x + offsetX,
                            trailPos.y + offsetY,
                            trailPos.z + offsetZ,
                            0.0D,
                            0.01D,
                            0.0D);
                } else {
                    level.addParticle(ParticleTypes.CLOUD,
                            trailPos.x + offsetX,
                            trailPos.y + offsetY,
                            trailPos.z + offsetZ,
                            0.0D,
                            0.005D,
                            0.0D);
                    if ((i + j) % 2 == 0) {
                        level.addParticle(ParticleTypes.SPLASH,
                                trailPos.x + offsetX,
                                trailPos.y + offsetY,
                                trailPos.z + offsetZ,
                                0.0D,
                                0.01D,
                                0.0D);
                    }
                }
            }
        }
    }

    private boolean canSpawnClientTrailParticles(Level level) {
        BlockState stateAtPos = level.getBlockState(this.blockPosition());
        return stateAtPos.isAir() || ttv.migami.jeg.gun.BulletPenetrationHelper.isPenetrable(level, stateAtPos);
    }

    /**
     * Perform precise block raycast that better handles leaves and other penetrable blocks.
     * This allows bullets to pass through gaps between leaves that would otherwise be blocked.
     */
    private BlockHitResult performPreciseBlockRaycast(Vec3 start, Vec3 end, boolean ignoreLeaves) {
        Vec3 rayStart = start;
        for (int attempts = 0; attempts < 16; attempts++) {
            ClipContext clipContext = new ClipContext(
                rayStart,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                this
            );
            BlockHitResult hitResult = this.level().clip(clipContext);
            if (hitResult.getType() != HitResult.Type.BLOCK || !ignoreLeaves) {
                return hitResult;
            }

            BlockState hitState = this.level().getBlockState(hitResult.getBlockPos());
            if (!IGNORE_LEAVES.test(hitState)) {
                return hitResult;
            }

            Vec3 direction = end.subtract(rayStart);
            if (direction.lengthSqr() <= 1.0E-7D) {
                return hitResult;
            }
            rayStart = hitResult.getLocation().add(direction.normalize().scale(0.05D));
        }

        ClipContext fallback = new ClipContext(
            rayStart,
            end,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            this
        );
        return this.level().clip(fallback);
    }

    @Nullable
    private EntityHitResult findClosestEntityHit(Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        AABB searchBox = this.getBoundingBox().expandTowards(segment).inflate(1.0D);
        Entity closestEntity = null;
        Vec3 closestHitPos = null;
        double closestDistanceSqr = Double.MAX_VALUE;
        Entity owner = this.getOwner();

        for (Entity entity : this.level().getEntities(this, searchBox, this::canHitEntity)) {
            // Avoid pellet-to-pellet collisions for multi-projectile weapons.
            if (entity instanceof BulletEntity) {
                continue;
            }
            // Defensive owner skip; canHitEntity usually handles this but we keep parity-safe behavior.
            if (entity == owner) {
                continue;
            }
            Vec3 hitPos = entity.getBoundingBox().inflate(0.3D).clip(start, end).orElse(null);
            if (hitPos == null) {
                continue;
            }
            double distanceSqr = start.distanceToSqr(hitPos);
            if (distanceSqr < closestDistanceSqr) {
                closestDistanceSqr = distanceSqr;
                closestEntity = entity;
                closestHitPos = hitPos;
            }
        }

        if (closestEntity == null || closestHitPos == null) {
            return null;
        }
        return new EntityHitResult(closestEntity, closestHitPos);
    }

    /**
     * Send bullet trail data to all nearby clients
     */
    public void sendTrailToClients(ServerLevel serverLevel) {
        if (this.level().isClientSide()) {
            return;
        }

        Vec3 position = this.position();
        Vec3 motion = this.getDeltaMovement();
        int color = this.entityData.get(DATA_TRAIL_COLOR);
        float size = this.entityData.get(DATA_SIZE);
        int life = this.entityData.get(DATA_LIFE);

        GunStats stats = getGunStats();
        double gravity = -getGravityAcceleration(stats);

        Entity owner = this.getOwner();
        int shooterId = owner != null ? owner.getId() : -1;
        boolean trailVisible = true;
        if (owner != null) {
            trailVisible = owner.distanceToSqr(position.x, position.y, position.z) > MIN_TRAIL_START_DISTANCE_SQR;
        }

        BulletTrailPayload payload = new BulletTrailPayload(
                this.getId(),
                position,
                motion,
                color,
                size,
                life,
                gravity,
                shooterId,
                trailVisible
        );

        // Send to all nearby players
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (player.level() == serverLevel && player.distanceToSqr(this) <= TRAIL_SYNC_RANGE * TRAIL_SYNC_RANGE) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static Vec3 applyGravity(Vec3 motion, GunStats stats) {
        return new Vec3(motion.x, motion.y - getGravityAcceleration(stats), motion.z);
    }

    private static double getGravityAcceleration(GunStats stats) {
        double gravity = switch (GunCategory.fromStats(stats)) {
            case SNIPER -> 0.020D;
            case RIFLE -> 0.024D;
            case PISTOL -> 0.030D;
            case SMG -> 0.032D;
            case LMG -> 0.028D;
            case SHOTGUN -> 0.040D;
            case HEAVY -> 0.035D;
            case SPECIAL -> 0.050D;
        };
        if (shouldSendBulletTrail(stats) && stats.gravity()) {
            gravity = 0.040D;
        }
        return stats.id().equals(FLAMETHROWER_ID) ? gravity * 1.5D : gravity;
    }

    private static boolean shouldSendBulletTrail(GunStats stats) {
        return !stats.flameTrail() && GunItem.isBulletClassWeapon(stats.id());
    }
}
