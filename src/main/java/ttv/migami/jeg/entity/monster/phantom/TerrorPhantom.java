package ttv.migami.jeg.entity.monster.phantom;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;

/**
 * Terror Phantom based on original 1.20.1 implementation.
 * Free-roaming Terror Phantom with 5-phase AI system.
 */
public class TerrorPhantom extends AbstractTerrorPhantom {
    private static final int MINIGUN_FIRE_DELAY_TICKS = 2;
    private static final int MINIGUN_BURST_SHOTS = 40;
    private static final int MINIGUN_BURST_COOLDOWN_TICKS = 60;
    private static final int ROCKET_COOLDOWN_TICKS = 50;
    private static final double ROCKET_MIN_RANGE = 16.0D;
    private static final double ROCKET_MAX_RANGE = 96.0D;
    private static final double ROCKET_GRAVITY_PER_TICK = 0.040D;
    private static final ResourceLocation ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher");
    private int rocketCooldownTicks = 0;
    // Entity data for syncing
    private static final EntityDataAccessor<Boolean> IS_ROLLING = SynchedEntityData.defineId(TerrorPhantom.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_DYING = SynchedEntityData.defineId(TerrorPhantom.class, EntityDataSerializers.BOOLEAN);

    // Attack phase system
    protected Vec3 moveTargetPoint = Vec3.ZERO;
    protected BlockPos anchorPoint = BlockPos.ZERO;
    protected Vec3 orbitCenter = Vec3.ZERO; // For Guardian subclass
    protected AttackPhase attackPhase = AttackPhase.CIRCLE;
    protected int attackTimer = 0;

    // Health-based behavior triggers
    private boolean isHalfHealth = false;
    private boolean isAngry = false;

    // Death animation
    private boolean isDying = false;
    private int deathTimer = 0;
    private boolean deathResolved = false;
    private static final int DEATH_ANIMATION_DURATION = 200;
    private double forwardSpeed = 0.1;
    private boolean looted = false;

    // Swarm spawning
    private final int MAX_SWARM_SPAWN_TICK = 300;
    private int swarmSpawnTick = MAX_SWARM_SPAWN_TICK;

    // Store the player who killed this phantom for raid targeting
    @Nullable
    private Player killer;

    public TerrorPhantom(EntityType<? extends TerrorPhantom> type, Level level) {
        super(type, level);
        this.moveControl = new TerrorPhantomMoveControl(this);
    }

    /**
     * Fire a gravity-led rocket at a vehicle-mounted target.
     * Independent of minigun magazine/reload so both weapon systems can coexist.
     */
    protected void tryShootRocketAtVehicleTarget(LivingEntity target) {
        if (this.level().isClientSide() || this.isDying()) {
            return;
        }
        if (this.rocketCooldownTicks > 0) {
            this.rocketCooldownTicks--;
            return;
        }
        if (target == null || !target.isAlive() || target.getVehicle() == null) {
            return;
        }

        Entity rootVehicle = target.getRootVehicle();
        if (rootVehicle == null) {
            return;
        }

        Vec3 muzzle = rocketMuzzlePosition();
        Vec3 targetPos = rootVehicle.getBoundingBox().getCenter();
        double distanceSqr = muzzle.distanceToSqr(targetPos);
        if (distanceSqr < ROCKET_MIN_RANGE * ROCKET_MIN_RANGE || distanceSqr > ROCKET_MAX_RANGE * ROCKET_MAX_RANGE) {
            return;
        }
        if (!ttv.migami.jeg.gun.BulletPenetrationHelper.hasLineOfSightThroughPenetrable(this, target)) {
            return;
        }

        var holder = ModItems.GUNS.get(ROCKET_LAUNCHER_ID);
        if (holder == null) {
            return;
        }
        GunItem rocketGun = holder.get();
        GunStats rocketStats = rocketGun.getStats();
        ItemStack rocketStack = new ItemStack(rocketGun);

        Vec3 targetVel = rootVehicle.getDeltaMovement();
        Vec3 direction = aimDirectionWithGravity(
                muzzle,
                targetPos,
                targetVel,
                Math.max(0.1D, rocketStats.projectileSpeed()),
                ROCKET_GRAVITY_PER_TICK
        );
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }

        rocketGun.fireDirectionallyFrom(this.level(), this, rocketStack, muzzle, direction);
        rocketStats.fireSoundEvent().ifPresent(sound ->
                this.level().playSound(null, this, sound, SoundSource.HOSTILE, 8.0F, 0.9F + this.random.nextFloat() * 0.2F)
        );
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.rocketCooldownTicks = ROCKET_COOLDOWN_TICKS;
    }

    private Vec3 rocketMuzzlePosition() {
        Vec3 forward = this.getViewVector(1.0F).normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        double scale = Math.max(0.25D, this.getGeoScale());
        return this.position()
                .add(0.0D, this.getBbHeight() * 0.35D, 0.0D)
                .add(up.scale(-0.35D * scale))
                .add(forward.scale(1.35D * scale));
    }

    /**
     * Iterative intercept aiming that compensates for discrete per-tick gravity drop.
     */
    private static Vec3 aimDirectionWithGravity(Vec3 muzzle, Vec3 targetPos, Vec3 targetVel, double speed, double gravityPerTick) {
        Vec3 aimPoint = targetPos;
        for (int i = 0; i < 8; i++) {
            double distance = muzzle.distanceTo(aimPoint);
            double flightTicks = distance / speed;
            double drop = gravityPerTick * flightTicks * (flightTicks + 1.0D) * 0.5D;
            aimPoint = targetPos.add(targetVel.scale(flightTicks)).add(0.0D, drop, 0.0D);
        }
        Vec3 delta = aimPoint.subtract(muzzle);
        double lengthSqr = delta.lengthSqr();
        if (lengthSqr < 1.0E-8D) {
            return Vec3.ZERO;
        }
        return delta.scale(1.0D / Math.sqrt(lengthSqr));
    }

    @Override
    protected ResourceLocation getVariantTexture() {
        // Original JEG atlas includes under-wing minigun UVs; phantom_gunner atlas does not.
        return Reference.id("textures/entity/terror_phantom/terror_phantom.png");
    }

    @Override
    protected float getModelScale() {
        return 2.8F;
    }

    @Override
    protected ResourceLocation defaultGunId() {
        return Reference.id("minigun");
    }

    @Override
    protected int getLoadedAmmoCapacity(GunStats stats) {
        if ("minigun".equals(stats.id().getPath())) {
            return Math.max(super.getLoadedAmmoCapacity(stats), MINIGUN_BURST_SHOTS * 2);
        }
        return super.getLoadedAmmoCapacity(stats);
    }

    @Override
    protected int getCurrentFireDelay() {
        return MINIGUN_FIRE_DELAY_TICKS;
    }

    @Override
    protected int getSustainedFireShotLimit() {
        return MINIGUN_BURST_SHOTS;
    }

    @Override
    protected int getSustainedFireCooldown() {
        return MINIGUN_BURST_COOLDOWN_TICKS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_ROLLING, false);
        builder.define(IS_DYING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new TerrorPhantomAttackStrategyGoal());
        this.goalSelector.addGoal(2, new TerrorPhantomSweepAttackGoal());
        this.goalSelector.addGoal(2, new TerrorPhantomRollAttackGoal());
        this.goalSelector.addGoal(2, new TerrorPhantomSwarmAttackGoal());
        this.goalSelector.addGoal(2, new TerrorPhantomBombingAttackGoal());
        this.goalSelector.addGoal(3, new TerrorPhantomCircleAroundAnchorGoal());
        this.goalSelector.addGoal(5, new TerrorPhantomShootGoal());
        // Targeting filtered via canAttack() override
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, null));
    }

    @Override
    protected boolean shouldSpawnGunnerSkeletonsOnDeath() {
        return true;
    }

    @Override
    protected boolean shouldTriggerRaidOnDeath() {
        return true;
    }

    @Override
    protected void onDefeated(ServerLevel level, DamageSource source) {
        this.killer = source.getEntity() instanceof Player ? (Player) source.getEntity() : null;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // Update health-based flags
            this.isHalfHealth = this.getHealth() <= this.getMaxHealth() / 2;
            this.isAngry = this.getHealth() <= (this.getMaxHealth() / 2) + 50;

            // Ensure correct phantom size
            if (this.getPhantomSize() != 6) {
                this.setPhantomSize(6);
            }

            // Handle death animation
            if (this.getHealth() <= 0 && !this.isDying) {
                this.isDying = true;
                this.setDying(true);
            }

            if (this.isDying()) {
                tickDeathAnimation();
                return; // Skip normal AI during death
            }

            // Attack timer countdown
            if (this.attackTimer > 0) {
                this.attackTimer--;
                if (this.attackTimer <= 0) {
                    this.attackPhase = AttackPhase.CIRCLE;
                }
            }

            // Swarm spawning when below half health
            if (this.isHalfHealth && --this.swarmSpawnTick <= 0 && this.getTarget() instanceof Player) {
                spawnPhantomSwarm();
                this.swarmSpawnTick = MAX_SWARM_SPAWN_TICK;
            }
        }

        // Physics control based on phase
        if (!this.isDying()) {
            if (this.attackPhase == AttackPhase.SWOOP) {
                this.horizontalCollision = false;
            }
            this.noPhysics = (this.getTarget() != null || this.horizontalCollision);
        } else {
            this.noPhysics = false;
        }
    }

    private void tickDeathAnimation() {
        // Keep vanilla death cleanup from truncating the custom 200-tick sequence.
        this.deathTime = 0;
        this.deathTimer++;

        // Accelerate forward during death dive
        this.forwardSpeed = Math.min(this.forwardSpeed + 0.05, 2.0);
        Vec3 forwardMotion = Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(this.forwardSpeed);
        this.setDeltaMovement(forwardMotion.x, -0.75, forwardMotion.z);

        // Spin during death
        this.setXRot(this.getXRot() + 2f);

        // Drop grenades periodically
        if (this.tickCount % 10 == 0) {
            dropGrenadesDuringDeath();
        }

        // Play explosion sound periodically
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 8 == 0) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 10.0F, 1.0F);
        }

        BlockPos impactPos = this.level() instanceof ServerLevel impactLevel ? resolveDeathImpactPosition(impactLevel) : this.blockPosition();
        boolean hitGround = this.getY() + this.getDeltaMovement().y <= impactPos.getY() + 0.25D;
        boolean nearWorldBottom = this.level() instanceof ServerLevel bottomLevel && this.getY() <= bottomLevel.getMinBuildHeight() + 2.0D;

        // End death animation
        if (!this.deathResolved && (this.deathTimer >= DEATH_ANIMATION_DURATION || this.horizontalCollision || this.verticalCollision || hitGround || nearWorldBottom)) {
            this.deathResolved = true;
            this.setPos(impactPos.getX() + 0.5D, impactPos.getY(), impactPos.getZ() + 0.5D);
            explodeOnDeath();
            this.remove(RemovalReason.KILLED);
        }
    }

    private BlockPos resolveDeathImpactPosition(ServerLevel level) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.blockPosition());
        if (surface.getY() <= level.getMinBuildHeight()) {
            return this.blockPosition();
        }
        return surface;
    }

    private void explodeOnDeath() {
        if (this.level() instanceof ServerLevel serverLevel) {
            // Main explosion
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.MOB);

            // Spawn loot barrels
            if (!this.looted) {
                spawnLootBarrels(serverLevel);
                this.looted = true;
            }

            // Trigger raid
            try {
                TerrorRaidManager.triggerGroundRaid(serverLevel, this.blockPosition(), this.killer);
            } catch (Exception ignored) {
            }

            // Award XP
            for (int i = 0; i < 25; i++) {
                ExperienceOrb.award(serverLevel, this.position().add(0, 10, 0), 100);
            }
        }
    }

    private void spawnLootBarrels(ServerLevel level) {
        BlockPos pos1 = findGroundPosition(level, this.getOnPos());
        BlockPos pos2 = findGroundPosition(level, this.getOnPos().offset(2, 0, 2));
        // Loot barrels are spawned by the raid manager
    }

    private static BlockPos findGroundPosition(Level level, BlockPos pos) {
        while (!level.getBlockState(pos).isSolid() && pos.getY() > level.getMinBuildHeight()) {
            pos = pos.below();
        }
        return pos.above();
    }

    private void dropGrenadesDuringDeath() {
        if (this.level() instanceof ServerLevel serverLevel) {
            RandomSource random = this.getRandom();
            int grenadeCount = 1 + random.nextInt(1);

            for (int i = 0; i < grenadeCount; i++) {
                GrenadeEntity grenade = new GrenadeEntity(serverLevel, this, 3.0F, 50 + random.nextInt(9), true);
                grenade.setPos(this.getX(), this.getY() + 1, this.getZ());

                double xVel = (random.nextDouble() - 0.5) * 1.5;
                double yVel = (0.4 + random.nextDouble() * 0.6) * -1;
                double zVel = (random.nextDouble() - 0.5) * 1.5;

                grenade.setDeltaMovement(xVel, yVel, zVel);
                serverLevel.addFreshEntity(grenade);
            }
        }
    }

    private void spawnPhantomSwarm() {
        if (this.level() instanceof ServerLevel serverLevel && this.getTarget() != null) {
            BlockPos spawnPos = this.getTarget().blockPosition().offset(
                (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1),
                0,
                (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1)
            );

            int count = 1 + this.random.nextInt(1);
            for (int i = 0; i < count; i++) {
                PhantomGunner phantom = new PhantomGunnerMinion(ModEntities.PHANTOM_GUNNER_MINION.get(), serverLevel);
                Vec3 pos = Vec3.atCenterOf(spawnPos).add(random.nextGaussian() * 2, 2, random.nextGaussian() * 2);
                phantom.setPos(pos.x, pos.y, pos.z);
                phantom.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
                phantom.setTarget(this.getTarget());
                serverLevel.addFreshEntity(phantom);
            }
        }
    }

    @Override
    public boolean isDeadOrDying() {
        return this.isDying && this.deathTimer >= DEATH_ANIMATION_DURATION;
    }

    public boolean isRolling() {
        return this.entityData.get(IS_ROLLING);
    }

    public void setRolling(boolean rolling) {
        this.entityData.set(IS_ROLLING, rolling);
    }

    public boolean isDying() {
        return this.entityData.get(IS_DYING);
    }

    public void setDying(boolean dying) {
        this.entityData.set(IS_DYING, dying);
    }

    public AttackPhase getAttackPhase() {
        return this.attackPhase;
    }

    /**
     * Override in subclasses to customize orbit center calculation.
     * Default implementation uses anchorPoint directly.
     */
    protected void updateOrbitCenter(LivingEntity target) {
        // Base implementation - subclasses can override
        this.orbitCenter = Vec3.atCenterOf(this.anchorPoint);
    }

    /**
     * Override in subclasses to enforce flight height constraints.
     * Default implementation returns the target position unchanged.
     */
    protected Vec3 enforceFlightHeight(Vec3 targetPos) {
        // Base implementation - subclasses can override
        return targetPos;
    }

    // ========== AI GOALS ==========

    /**
     * Main strategy goal that switches between attack phases.
     */
    class TerrorPhantomAttackStrategyGoal extends Goal {
        private int nextSweepTick;
        private boolean lastRolled = false;
        private int turnsUntilSwarm = 0;
        private int turnsUntilBombing = 0;
        private boolean isBombing = false;
        private int bombingTurns = 3;

        @Override
        public boolean canUse() {
            if (TerrorPhantom.this.isDying()) {
                return false;
            }
            LivingEntity target = TerrorPhantom.this.getTarget();
            return target != null && TerrorPhantom.this.canAttack(target);
        }

        @Override
        public void start() {
            this.nextSweepTick = this.adjustedTickDelay(10);
            TerrorPhantom.this.attackPhase = AttackPhase.CIRCLE;
            setAnchorAboveTarget();
            this.turnsUntilBombing = 0;
            this.bombingTurns = 3;
        }

        @Override
        public void stop() {
            TerrorPhantom.this.anchorPoint = TerrorPhantom.this.level().getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING,
                TerrorPhantom.this.anchorPoint
            ).above(10 + TerrorPhantom.this.random.nextInt(20));
        }

        @Override
        public void tick() {
            if (TerrorPhantom.this.attackPhase == AttackPhase.CIRCLE) {
                --this.nextSweepTick;

                if (this.bombingTurns <= 0) {
                    this.isBombing = false;
                }

                if (this.nextSweepTick <= 0) {
                    if (this.isBombing) {
                        doBombing();
                        this.bombingTurns--;
                    } else if (this.turnsUntilSwarm <= 0 && TerrorPhantom.this.isHalfHealth) {
                        doSwarm();
                        this.turnsUntilSwarm = 5;
                    } else if (this.turnsUntilBombing <= 0 && TerrorPhantom.this.isHalfHealth && TerrorPhantom.this.canPerformBombing()) {
                        this.isBombing = true;
                        this.turnsUntilBombing = 2;
                    } else {
                        // Randomly choose between swoop and roll
                        if (TerrorPhantom.this.random.nextBoolean()) {
                            doSwoop();
                            this.lastRolled = false;
                        } else {
                            if (!this.lastRolled) {
                                doRoll();
                                if (TerrorPhantom.this.getHealth() >= TerrorPhantom.this.getMaxHealth() / 2) {
                                    this.lastRolled = true;
                                }
                            } else {
                                doSwoop();
                                this.lastRolled = false;
                            }
                        }
                        this.turnsUntilSwarm--;
                        this.turnsUntilBombing--;
                    }
                }
            }
        }

        private void doSwoop() {
            TerrorPhantom.this.attackPhase = AttackPhase.SWOOP;
            TerrorPhantom.this.attackTimer = 80;
            setAnchorAboveTarget();
            this.nextSweepTick = this.adjustedTickDelay(100);
            TerrorPhantom.this.playSound(SoundEvents.PHANTOM_SWOOP, 30.0F, 0.45F + TerrorPhantom.this.random.nextFloat() * 0.1F);
        }

        private void doRoll() {
            TerrorPhantom.this.setRolling(true);
            TerrorPhantom.this.attackPhase = AttackPhase.ROLL;
            TerrorPhantom.this.attackTimer = 100;
            setAnchorOvershot();
            this.nextSweepTick = this.adjustedTickDelay(200);
            TerrorPhantom.this.playSound(SoundEvents.PHANTOM_SWOOP, 30.0F, 0.45F + TerrorPhantom.this.random.nextFloat() * 0.1F);
        }

        private void doSwarm() {
            TerrorPhantom.this.attackPhase = AttackPhase.SWARM;
            TerrorPhantom.this.attackTimer = 240;
            setAnchorAboveTarget();
            this.nextSweepTick = this.adjustedTickDelay(300);
            TerrorPhantom.this.playSound(SoundEvents.PHANTOM_SWOOP, 30.0F, 0.45F + TerrorPhantom.this.random.nextFloat() * 0.1F);
        }

        private void doBombing() {
            TerrorPhantom.this.attackPhase = AttackPhase.BOMBING;
            TerrorPhantom.this.attackTimer = 70;
            if (TerrorPhantom.this.getTarget() != null) {
                TerrorPhantom.this.getLookControl().setLookAt(TerrorPhantom.this.getTarget(), 255.0F, 255.0F);
            }
            setAnchorOvershot();
            this.nextSweepTick = this.adjustedTickDelay(60);
            TerrorPhantom.this.playSound(SoundEvents.PHANTOM_SWOOP, 30.0F, 0.45F + TerrorPhantom.this.random.nextFloat() * 0.1F);
        }

        private void setAnchorAboveTarget() {
            if (TerrorPhantom.this.getTarget() == null) return;

            int maxHeight = Math.min(
                TerrorPhantom.this.getTarget().getBlockY() + 32,
                TerrorPhantom.this.level().getMaxBuildHeight() - 1
            );
            int targetHeight = TerrorPhantom.this.getTarget().getBlockY() + 20 + TerrorPhantom.this.random.nextInt(12);
            TerrorPhantom.this.anchorPoint = new BlockPos(
                TerrorPhantom.this.getTarget().getBlockX(),
                Math.min(targetHeight, maxHeight),
                TerrorPhantom.this.getTarget().getBlockZ()
            );
        }

        private void setAnchorOvershot() {
            if (TerrorPhantom.this.getTarget() == null) return;

            LivingEntity target = TerrorPhantom.this.getTarget();
            Vec3 phantomPos = TerrorPhantom.this.position();
            Vec3 targetPos = target.position();
            Vec3 direction = targetPos.subtract(phantomPos).normalize();

            double overshootDistance = 32 + TerrorPhantom.this.random.nextInt(6);
            Vec3 overshotPos = targetPos.add(direction.scale(overshootDistance));

            int targetHeight = target.getBlockY();
            int maxHeight = Math.min(targetHeight + 32, TerrorPhantom.this.level().getMaxBuildHeight() - 1);
            int finalHeight = Math.min(targetHeight, maxHeight);

            TerrorPhantom.this.anchorPoint = new BlockPos((int) overshotPos.x, finalHeight, (int) overshotPos.z);
        }
    }

    /**
     * Swoop attack - dive at target
     */
    class TerrorPhantomSweepAttackGoal extends TerrorPhantomMoveTargetGoal {
        @Override
        public boolean canUse() {
            return !TerrorPhantom.this.isDying() &&
                   TerrorPhantom.this.getTarget() != null &&
                   TerrorPhantom.this.attackPhase == AttackPhase.SWOOP;
        }

        @Override
        public boolean canContinueToUse() {
            if (TerrorPhantom.this.isDying()) return false;

            LivingEntity target = TerrorPhantom.this.getTarget();
            if (target == null || !target.isAlive()) return false;

            if (target instanceof Player player) {
                if (player.isSpectator() || player.isCreative()) return false;
            }

            return canUse();
        }

        @Override
        public void stop() {
            TerrorPhantom.this.attackPhase = AttackPhase.CIRCLE;
        }

        @Override
        public void tick() {
            LivingEntity target = TerrorPhantom.this.getTarget();
            if (target != null) {
                TerrorPhantom.this.moveTargetPoint = new Vec3(target.getX(), target.getY(0.5), target.getZ());

                if (TerrorPhantom.this.getBoundingBox().inflate(0.2).intersects(target.getBoundingBox())) {
                    // Deal melee damage to target
                    target.hurt(TerrorPhantom.this.damageSources().mobAttack(TerrorPhantom.this), 8.0F);
                    TerrorPhantom.this.attackPhase = AttackPhase.CIRCLE;
                    if (!TerrorPhantom.this.isSilent()) {
                        TerrorPhantom.this.level().levelEvent(1039, TerrorPhantom.this.blockPosition(), 0);
                    }
                }
            }
        }
    }

    /**
     * Roll attack - barrel roll while dropping grenades, with invulnerability
     */
    class TerrorPhantomRollAttackGoal extends TerrorPhantomMoveTargetGoal {
        private int grenadeRollsLeft = 2;
        private int grenadeTick = 20;

        @Override
        public boolean canUse() {
            return !TerrorPhantom.this.isDying() &&
                   TerrorPhantom.this.getTarget() != null &&
                   TerrorPhantom.this.attackPhase == AttackPhase.ROLL;
        }

        @Override
        public boolean canContinueToUse() {
            if (TerrorPhantom.this.isDying()) return false;

            LivingEntity target = TerrorPhantom.this.getTarget();
            if (target == null || !target.isAlive()) return false;

            if (target instanceof Player player) {
                if (player.isSpectator() || player.isCreative()) return false;
            }

            return canUse();
        }

        @Override
        public void start() {
            this.grenadeRollsLeft = 2;
            this.grenadeTick = 20;
        }

        @Override
        public void stop() {
            TerrorPhantom.this.setRolling(false);
            TerrorPhantom.this.attackPhase = AttackPhase.CIRCLE;
        }

        @Override
        public void tick() {
            BlockPos anchor = TerrorPhantom.this.anchorPoint;
            TerrorPhantom.this.moveTargetPoint = new Vec3(
                anchor.getX(),
                TerrorPhantom.this.getTarget().getY() + 24,
                anchor.getZ()
            );

            // Grant invulnerability during roll
            TerrorPhantom.this.invulnerableTime = 20;

            // Drop grenades periodically
            if (--this.grenadeTick == 0 && this.grenadeRollsLeft > 0) {
                dropGrenadesRoll();
                this.grenadeTick = 20;
                this.grenadeRollsLeft--;
            }

            if (TerrorPhantom.this.attackTimer <= 0) {
                TerrorPhantom.this.setRolling(false);
            }
        }

        private void dropGrenadesRoll() {
            if (TerrorPhantom.this.level() instanceof ServerLevel serverLevel) {
                BlockPos pos = TerrorPhantom.this.blockPosition();
                RandomSource random = TerrorPhantom.this.getRandom();
                int grenadeCount = 6 + random.nextInt(3);

                for (int i = 0; i < grenadeCount; i++) {
                    GrenadeEntity grenade = new GrenadeEntity(serverLevel, TerrorPhantom.this, 3.0F, 50 + random.nextInt(9), true);
                    grenade.setPos(pos.getX(), pos.getY() + 1, pos.getZ());

                    double xVel = (random.nextDouble() - 0.5) * 1.5;
                    double yVel = (0.4 + random.nextDouble() * 0.6) * -1;
                    double zVel = (random.nextDouble() - 0.5) * 1.5;

                    grenade.setDeltaMovement(xVel, yVel, zVel);
                    serverLevel.addFreshEntity(grenade);
                }
            }
        }
    }

    /**
     * Swarm attack - summon phantom gunners and heal
     */
    class TerrorPhantomSwarmAttackGoal extends TerrorPhantomMoveTargetGoal {
        private final int summonInterval = 10;
        private int tickCounter = 0;

        @Override
        public boolean canUse() {
            return !TerrorPhantom.this.isDying() &&
                   TerrorPhantom.this.getTarget() != null &&
                   TerrorPhantom.this.attackPhase == AttackPhase.SWARM;
        }

        @Override
        public void tick() {
            this.tickCounter++;

            // Heal during swarm phase
            if (TerrorPhantom.this.isHalfHealth) {
                TerrorPhantom.this.setHealth(TerrorPhantom.this.getHealth() + 1);
            }

            // Grant invulnerability
            TerrorPhantom.this.invulnerableTime = 20;

            // Summon phantoms periodically
            if (this.tickCounter >= this.summonInterval) {
                this.tickCounter = 0;
                summonPhantoms();
            }
        }

        private void summonPhantoms() {
            if (TerrorPhantom.this.level() instanceof ServerLevel serverLevel) {
                int numPhantoms = 1 + TerrorPhantom.this.random.nextInt(1);

                for (int i = 0; i < numPhantoms; i++) {
                    // Half-health swarm should summon the minion variant, not regular phantoms.
                    Phantom newPhantom = new PhantomGunnerMinion(ModEntities.PHANTOM_GUNNER_MINION.get(), serverLevel);

                    if (newPhantom != null) {
                        Vec3 spawnPos = TerrorPhantom.this.position().add(
                            TerrorPhantom.this.getRandom().nextGaussian() * 2,
                            2,
                            TerrorPhantom.this.getRandom().nextGaussian() * 2
                        );
                        newPhantom.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                        newPhantom.finalizeSpawn(
                            serverLevel,
                            serverLevel.getCurrentDifficultyAt(TerrorPhantom.this.blockPosition()),
                            MobSpawnType.EVENT,
                            null
                        );
                        newPhantom.setTarget(TerrorPhantom.this.getTarget());
                        serverLevel.addFreshEntity(newPhantom);
                    }
                }
            }
        }
    }

    /**
     * Bombing attack - drop grenades straight down
     */
    class TerrorPhantomBombingAttackGoal extends TerrorPhantomMoveTargetGoal {
        private int grenadeTick = 0;

        @Override
        public boolean canUse() {
            return !TerrorPhantom.this.isDying() &&
                   TerrorPhantom.this.getTarget() != null &&
                   TerrorPhantom.this.canPerformBombing() &&
                   TerrorPhantom.this.attackPhase == AttackPhase.BOMBING;
        }

        @Override
        public boolean canContinueToUse() {
            if (TerrorPhantom.this.isDying()) return false;

            LivingEntity target = TerrorPhantom.this.getTarget();
            if (target == null || !target.isAlive()) return false;

            if (target instanceof Player player) {
                if (player.isSpectator() || player.isCreative()) return false;
            }

            return canUse();
        }

        @Override
        public void start() {
            this.grenadeTick = 2;
        }

        @Override
        public void stop() {
            TerrorPhantom.this.attackPhase = AttackPhase.CIRCLE;
        }

        @Override
        public void tick() {
            BlockPos anchor = TerrorPhantom.this.anchorPoint;
            TerrorPhantom.this.moveTargetPoint = new Vec3(
                anchor.getX(),
                TerrorPhantom.this.getTarget().getY() + 12,
                anchor.getZ()
            );

            // Grant brief invulnerability
            TerrorPhantom.this.invulnerableTime = 20;

            // Drop grenades
            if (--this.grenadeTick == 0) {
                dropGrenadeBomb();
                this.grenadeTick = 2;
            }

            if (TerrorPhantom.this.attackTimer <= 0) {
                TerrorPhantom.this.setRolling(false);
            }
        }

        private void dropGrenadeBomb() {
            if (TerrorPhantom.this.level() instanceof ServerLevel serverLevel) {
                BlockPos pos = TerrorPhantom.this.blockPosition();

                GrenadeEntity grenade = new GrenadeEntity(serverLevel, TerrorPhantom.this, 3.0F, 100, true);
                grenade.setPos(pos.getX(), pos.getY() + 1, pos.getZ());
                grenade.setDeltaMovement(0, -1, 0);
                serverLevel.addFreshEntity(grenade);
            }
        }
    }

    /**
     * Circle around anchor point
     */
    class TerrorPhantomCircleAroundAnchorGoal extends TerrorPhantomMoveTargetGoal {
        private float angle;
        private float distance;
        private float height;
        private float clockwise;

        @Override
        public boolean canUse() {
            return !TerrorPhantom.this.isDying() &&
                   (TerrorPhantom.this.getTarget() == null ||
                    TerrorPhantom.this.attackPhase == AttackPhase.CIRCLE ||
                    TerrorPhantom.this.attackPhase == AttackPhase.SWARM);
        }

        @Override
        public void start() {
            this.distance = 5.0F + TerrorPhantom.this.random.nextFloat() * 10.0F;
            this.height = -4.0F + TerrorPhantom.this.random.nextFloat() * 9.0F;
            this.clockwise = TerrorPhantom.this.random.nextBoolean() ? 1.0F : -1.0F;
            selectNext();
        }

        @Override
        public void tick() {
            if (TerrorPhantom.this.random.nextInt(this.adjustedTickDelay(350)) == 0) {
                this.height = -4.0F + TerrorPhantom.this.random.nextFloat() * 9.0F;
            }

            if (TerrorPhantom.this.random.nextInt(this.adjustedTickDelay(250)) == 0) {
                ++this.distance;
                if (this.distance > 15.0F) {
                    this.distance = 5.0F;
                    this.clockwise = -this.clockwise;
                }
            }

            if (TerrorPhantom.this.random.nextInt(this.adjustedTickDelay(450)) == 0) {
                this.angle = TerrorPhantom.this.random.nextFloat() * 2.0F * (float) Math.PI;
                selectNext();
            }

            if (touchingTarget()) {
                selectNext();
            }

            if (TerrorPhantom.this.moveTargetPoint.y < TerrorPhantom.this.getY() &&
                !TerrorPhantom.this.level().isEmptyBlock(TerrorPhantom.this.blockPosition().below(1))) {
                this.height = Math.max(1.0F, this.height);
                selectNext();
            }

            if (TerrorPhantom.this.moveTargetPoint.y > TerrorPhantom.this.getY() &&
                !TerrorPhantom.this.level().isEmptyBlock(TerrorPhantom.this.blockPosition().above(1))) {
                this.height = Math.min(-1.0F, this.height);
                selectNext();
            }
        }

        private void selectNext() {
            if (BlockPos.ZERO.equals(TerrorPhantom.this.anchorPoint)) {
                TerrorPhantom.this.anchorPoint = TerrorPhantom.this.level().getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING,
                    TerrorPhantom.this.blockPosition()
                ).above(40);
            }

            this.angle += this.clockwise * 15.0F * ((float) Math.PI / 180F);
            TerrorPhantom.this.moveTargetPoint = Vec3.atLowerCornerOf(TerrorPhantom.this.anchorPoint).add(
                this.distance * Mth.cos(this.angle),
                24,
                this.distance * Mth.sin(this.angle)
            );
        }
    }

    /**
     * Shoot at target with gun
     */
    class TerrorPhantomShootGoal extends Goal {
        public TerrorPhantomShootGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = TerrorPhantom.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = TerrorPhantom.this.getTarget();
            if (target == null) return;

            // Always look at target
            TerrorPhantom.this.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // Anti-vehicle rockets (independent of minigun reload/cooldown).
            TerrorPhantom.this.tryShootRocketAtVehicleTarget(target);

            // Check if we can shoot (can see through penetrable blocks like leaves)
            if (!ttv.migami.jeg.gun.BulletPenetrationHelper.hasLineOfSightThroughPenetrable(TerrorPhantom.this, target)) return;
            if (TerrorPhantom.this.isReloading() || !TerrorPhantom.this.readyToShoot()) return;

            TerrorPhantom.this.shootAt(target);
        }
    }

    /**
     * Base class for movement goals
     */
    abstract class TerrorPhantomMoveTargetGoal extends Goal {
        public TerrorPhantomMoveTargetGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        protected boolean touchingTarget() {
            return TerrorPhantom.this.moveTargetPoint.distanceToSqr(
                TerrorPhantom.this.getX(),
                TerrorPhantom.this.getY(),
                TerrorPhantom.this.getZ()
            ) < 4.0;
        }
    }

    /**
     * Custom move controller for smooth phantom flight
     */
    class TerrorPhantomMoveControl extends MoveControl {
        private float speed = 0.4F;

        public TerrorPhantomMoveControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
            // Adjust speed based on phase
            if (TerrorPhantom.this.isDying()) {
                this.speed = 0.5F;
            } else {
                this.speed = switch (TerrorPhantom.this.attackPhase) {
                    case SWARM -> 3.0F;
                    case ROLL -> 1.5F;
                    case BOMBING -> 2.0F;
                    default -> 0.5F;
                };
            }

            double dx = TerrorPhantom.this.moveTargetPoint.x - TerrorPhantom.this.getX();
            double dy = TerrorPhantom.this.moveTargetPoint.y - TerrorPhantom.this.getY();
            double dz = TerrorPhantom.this.moveTargetPoint.z - TerrorPhantom.this.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            if (Math.abs(horizontalDist) > 9.999999747378752E-6) {
                double verticalBias = 1.0 - Math.abs(dy * 0.699999988079071) / horizontalDist;
                dx *= verticalBias;
                dz *= verticalBias;
                horizontalDist = Math.sqrt(dx * dx + dz * dz);
                double totalDist = Math.sqrt(dx * dx + dz * dz + dy * dy);

                float currentYaw = TerrorPhantom.this.getYRot();
                float targetYaw = (float) Mth.atan2(dz, dx) * (180F / (float) Math.PI);
                float wrappedCurrent = Mth.wrapDegrees(currentYaw + 90.0F);
                float wrappedTarget = Mth.wrapDegrees(targetYaw);
                TerrorPhantom.this.setYRot(Mth.approachDegrees(wrappedCurrent, wrappedTarget, 4.0F) - 90.0F);
                TerrorPhantom.this.yBodyRot = TerrorPhantom.this.getYRot();

                if (Mth.degreesDifferenceAbs(currentYaw, TerrorPhantom.this.getYRot()) < 3.0F) {
                    this.speed = Mth.approach(this.speed, 2.5F, 0.005F * (1.8F / this.speed));
                } else {
                    this.speed = Mth.approach(this.speed, 0.4F, 0.025F);
                }

                float pitch = (float) (-(Mth.atan2(-dy, horizontalDist) * (180F / (float) Math.PI)));
                TerrorPhantom.this.setXRot(pitch);
                float yawForMotion = TerrorPhantom.this.getYRot() + 90.0F;
                double motionX = this.speed * Mth.cos(yawForMotion * ((float) Math.PI / 180F)) * Math.abs(dx / totalDist);
                double motionZ = this.speed * Mth.sin(yawForMotion * ((float) Math.PI / 180F)) * Math.abs(dz / totalDist);
                double motionY = this.speed * Mth.sin(pitch * ((float) Math.PI / 180F)) * Math.abs(dy / totalDist);
                Vec3 currentVel = TerrorPhantom.this.getDeltaMovement();
                TerrorPhantom.this.setDeltaMovement(currentVel.add((new Vec3(motionX, motionY, motionZ)).subtract(currentVel).scale(0.2)));
            }
        }
    }

    /**
     * Attack phases enum
     */
    public enum AttackPhase {
        CIRCLE,
        SWOOP,
        ROLL,
        SWARM,
        BOMBING
    }
}
