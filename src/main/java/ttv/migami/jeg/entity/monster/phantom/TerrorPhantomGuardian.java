package ttv.migami.jeg.entity.monster.phantom;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.Projectile;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.gun.BallisticProtection;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.entity.GrenadeEntity;

/**
 * Structure-bound Terror Phantom guardian variant with improved AI.
 * Stays within a certain radius of its spawn point and flies higher.
 */
public class TerrorPhantomGuardian extends TerrorPhantom {
    // Guardian parameters
    private static final double SKYSHIP_VERTICAL_RANGE = 128.0D;
    private static final double SKYSHIP_DETECTION_RADIUS = 160.0D;
    private static final double ACTIVE_DETECTION_RADIUS = 85.0D; // Increased from 56 by 29 blocks
    private static final double PASSIVE_RETALIATION_RADIUS = 180.0D;
    private static final int TARGET_LOST_SIGHT_FORGET_TICKS = 20 * 5;
    private static final int DEFAULT_TETHER_RADIUS = 120;
    // Guardian flies above Sky Ship structure: tightened altitude band relative to detected deck height
    private static final int MIN_FLIGHT_HEIGHT_ABOVE_DECK = 55;
    private static final int MAX_FLIGHT_HEIGHT_ABOVE_DECK = 70;
    private static final int PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK = 60;
    private static final int INITIAL_ASCENT_HEIGHT = 18;
    private static final int IDLE_ALTITUDE_BONUS = 10;
    private static final int MAX_SCAN_HEIGHT_OFFSET = 40;
    private static final int MIN_SCAN_HEIGHT_OFFSET = 32;
    private static final int[][] DECK_SAMPLE_OFFSETS = new int[][] {
        {0, 0},
        {12, 0},
        {-12, 0},
        {0, 12},
        {0, -12},
        {12, 12},
        {12, -12},
        {-12, 12},
        {-12, -12}
    };

    private static final String TAG_TETHER_RADIUS = "DeckTetherRadius";
    private static final String TAG_DECK_HEIGHT = "DeckHeight";
    private static final String TAG_DECK_RESOLVED = "DeckResolved";
    private static final String TAG_ANCHOR_X = "DeckAnchorX";
    private static final String TAG_ANCHOR_Y = "DeckAnchorY";
    private static final String TAG_ANCHOR_Z = "DeckAnchorZ";
    private static final int GUARDIAN_DEATH_HOVER_TICKS = 60;
    private static final int GUARDIAN_DEATH_DURATION = 200;
    private static final double PATROL_STEP_RADIANS = Math.toRadians(24.0D);
    private static final int SUPPORT_TARGET_COUNT = 4;
    private static final int SUPPORT_MAX_COUNT = 6;
    private static final int SUPPORT_SPAWN_COOLDOWN_TICKS = 20 * 25;
    private static final double SUPPORT_MIN_TRIGGER_DISTANCE = ACTIVE_DETECTION_RADIUS;
    private static final double SUPPORT_SPAWN_HEIGHT_ABOVE_PLAYER = 48.0D;
    private static final double SUPPORT_SPAWN_MIN_SPREAD = 14.0D;
    private static final double SUPPORT_SPAWN_RANDOM_SPREAD = 12.0D;

    private BlockPos anchorPos;
    private BlockPos desiredAnchor;
    private int tetherRadius = DEFAULT_TETHER_RADIUS;
    private int deckHeight = -1;
    private boolean deckResolved = false;
    private boolean guardianFinalExplosion = false;
    private final java.util.Set<java.util.UUID> engagedPlayers = new java.util.HashSet<>();
    private int idleRecenterTicks = 0;
    private int guardianDeathTicks = 0;
    private final java.util.List<PhantomGunnerMinion> guardianSupports = new java.util.ArrayList<>();
    // Death circling bombing fields
    private Vec3 deathCirclingCenter;
    private double deathCirclingAngle;
    private double deathCirclingRadius;
    private Vec3 lastEffectiveFirePos;
    private long lastEffectiveFireTick = Long.MIN_VALUE;
    private long lastSupportSpawnTick = -SUPPORT_SPAWN_COOLDOWN_TICKS;
    private int targetLostSightTicks = 0;

    public TerrorPhantomGuardian(EntityType<? extends TerrorPhantomGuardian> type, Level level) {
        super(type, level);
    }

    @Override
    protected Identifier getVariantTexture() {
        return Reference.id("textures/entity/phantom_gunner/phantom_gunner_friendly.png");
    }

    @Override
    protected float getModelScale() {
        return 3.0F; // Increased from 2.4F for better visibility
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new TerrorPhantomAttackStrategyGoal());
        this.goalSelector.addGoal(2, new TerrorPhantomSweepAttackGoal());
        this.goalSelector.addGoal(2, new TerrorPhantomRollAttackGoal());
        this.goalSelector.addGoal(2, new TerrorPhantomSwarmAttackGoal());
        this.goalSelector.addGoal(2, new TerrorPhantomBombingAttackGoal());
        this.goalSelector.addGoal(3, new GuardianPatrolGoal());
        this.goalSelector.addGoal(5, new TerrorPhantomShootGoal());
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, null));
    }

    @Override
    protected boolean shouldSpawnGunnerSkeletonsOnDeath() {
        return false; // Guardian spawns phantom gunners, not skeletons
    }

    @Override
    protected boolean shouldSpawnPhantomGunnersOnDeath() {
        return true;
    }

    @Override
    protected boolean canPerformBombing() {
        return false; // Guardian cannot perform dive bombing attacks
    }

    @Override
    protected boolean shouldTriggerRaidOnDeath() {
        return true; // Guardian should trigger air raid on death
    }

    @Override
    protected void onDefeated(ServerLevel level, DamageSource source) {
        // Trigger air raid for guardian death
        Player killer = source.getEntity() instanceof Player ? (Player) source.getEntity() : null;
        TerrorRaidManager.triggerAirRaid(level, this.blockPosition(), killer);
    }

    @Override
    public void die(DamageSource source) {
        // Call super.die() first to ensure proper boss bar cleanup and death handling
        super.die(source);

        // Handle guardian-specific death behavior
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {

            // Start guardian death sequence
            this.guardianDeathTicks = 0;
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Handle guardian-specific death animation
        if (!this.level().isClientSide() && this.getHealth() <= 0 && this.guardianDeathTicks < GUARDIAN_DEATH_DURATION) {
            tickGuardianDeathAnimation();
            return; // Skip normal ticking during guardian death sequence
        }
    }

  /**
     * Handles the circling bombing behavior during the first 60 ticks of guardian death.
     * The guardian circles around its death point and drops grenades periodically.
     */
    private void performDeathCirclingBombing() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Store death position for circling center
        if (this.deathCirclingCenter == null) {
            this.deathCirclingCenter = this.position();
            this.deathCirclingAngle = 0.0D;
            this.deathCirclingRadius = 20.0D;
        }

        // Update circling angle - make one complete circle over 60 ticks
        this.deathCirclingAngle += Math.toRadians(360.0D / GUARDIAN_DEATH_HOVER_TICKS);

        // Calculate new position for circling
        double targetX = this.deathCirclingCenter.x + Math.cos(this.deathCirclingAngle) * this.deathCirclingRadius;
        double targetZ = this.deathCirclingCenter.z + Math.sin(this.deathCirclingAngle) * this.deathCirclingRadius;
        double targetY = this.deathCirclingCenter.y;

        // Apply movement using move control to maintain smooth flight
        this.getMoveControl().setWantedPosition(targetX, targetY, targetZ, 1.5D);

        // Drop grenades every 10 ticks (6 grenades total during circling phase)
        if (this.guardianDeathTicks % 10 == 0) {
            // Create grenade with death animation parameters
            GrenadeEntity grenade = new GrenadeEntity(serverLevel, this, 3.0F, 40, true);
            Vec3 origin = this.position().add(0.0D, -2.0D, 0.0D);
            grenade.initialisePosition(origin);

            // Give grenade slight downward velocity
            grenade.setDeltaMovement(
                this.random.nextGaussian() * 0.1D,
                -0.3D,
                this.random.nextGaussian() * 0.1D
            );

            serverLevel.addFreshEntity(grenade);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 0.8F + this.random.nextFloat() * 0.2F);
        }

        // Add soul fire particles during circling
        if (this.guardianDeathTicks % 3 == 0) {
            serverLevel.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(),
                this.getY() + 1.0D,
                this.getZ(),
                8,
                0.3D,
                0.3D,
                0.3D,
                0.01D
            );
        }
    }
  private void tickGuardianDeathAnimation() {
        this.guardianDeathTicks++;

        if (this.guardianDeathTicks <= GUARDIAN_DEATH_HOVER_TICKS) {
            // 0-60 ticks: circling bombing phase
            performDeathCirclingBombing();
        } else {
            // 60+ ticks: final phase with explosions (original behavior)
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(this.getX(), this.getY(), this.getZ());

            if (this.level() instanceof ServerLevel serverLevel) {
                if (this.guardianDeathTicks % 5 == 0) {
                    serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(),
                        this.getY() + 2.0D,
                        this.getZ(),
                        18,
                        0.6D,
                        0.6D,
                        0.6D,
                        0.01D
                    );
                    serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.2F);
                    serverLevel.explode(this, this.getX(), this.getY(), this.getZ(), 1.0F, Level.ExplosionInteraction.NONE);
                }
            }
        }

        if (this.guardianDeathTicks >= GUARDIAN_DEATH_HOVER_TICKS) {
            triggerGuardianFinale();
        }
    }
    private void triggerGuardianFinale() {
        if (this.guardianFinalExplosion) {
            return;
        }
        this.guardianFinalExplosion = true;

        if (this.level() instanceof ServerLevel serverLevel) {
            // Final explosion
            serverLevel.explode(this, this.getX(), this.getY(), this.getZ(), 4.5F, Level.ExplosionInteraction.MOB);

            // Spawn phantom gunners
            if (shouldSpawnPhantomGunnersOnDeath()) {
                spawnPhantomGunners(serverLevel);
            }

            // Trigger air raid (this also handles loot barrels)
            Player killer = this.getKillCredit() instanceof Player ? (Player) this.getKillCredit() : null;
            TerrorRaidManager.triggerAirRaid(serverLevel, this.blockPosition(), killer);
        }

        this.guardianDeathTicks = Math.max(this.guardianDeathTicks, GUARDIAN_DEATH_DURATION);
        this.remove(RemovalReason.KILLED);
    }

    @Override
    protected void onSpawned(ServerLevel level, BlockPos spawnPos) {
        this.setPersistenceRequired();
    }

    /**
     * Spawn phantom gunners on death - consistent with original 1.20.1 behavior
     */
    private void spawnPhantomGunners(ServerLevel level) {
        int count = 2 + level.getRandom().nextInt(2); // Spawn 2-3 phantom gunners like original
        BlockPos origin = this.blockPosition();

        // Find and target nearest player
        LivingEntity target = level.getNearestEntity(Player.class, TargetingConditions.DEFAULT, null,
            origin.getX(), origin.getY(), origin.getZ(), new AABB(origin).inflate(64.0D));

        for (int i = 0; i < count; i++) {
            PhantomGunner gunner = new PhantomGunnerMinion(ModEntities.PHANTOM_GUNNER_MINION.get(), level);
            if (gunner == null) {
                continue;
            }
            Vec3 offset = Vec3.directionFromRotation(0.0F, level.getRandom().nextFloat() * 360.0F).scale(6.0D + level.getRandom().nextDouble() * 4.0D);
            Vec3 spawnCenter = this.position().add(offset.x, 4.0D + level.getRandom().nextInt(4), offset.z);
            BlockPos spawnPos = BlockPos.containing(spawnCenter);
            gunner.setPos(spawnCenter.x, spawnCenter.y, spawnCenter.z);
            gunner.setYRot(level.getRandom().nextFloat() * 360.0F);
            gunner.yRotO = gunner.getYRot();
            gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.EVENT, null);

            // Set target to nearest player
            if (target != null) {
                gunner.setTarget(target);
            }

            level.addFreshEntity(gunner);
        }
        playSummonEffects(level, this.blockPosition().above(4));
    }

    private void playSummonEffects(ServerLevel level, BlockPos center) {
        level.playSound(null, center, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.8F, 0.6F + level.getRandom().nextFloat() * 0.3F);
        for (int i = 0; i < 12; i++) {
            double dx = center.getX() + 0.5D + (level.getRandom().nextDouble() - 0.5D) * 4.0D;
            double dy = center.getY() + level.getRandom().nextDouble() * 2.0D;
            double dz = center.getZ() + 0.5D + (level.getRandom().nextDouble() - 0.5D) * 4.0D;
            level.sendParticles(ParticleTypes.SMOKE, dx, dy, dz, 1, 0.0D, 0.05D, 0.0D, 0.02D);
        }
    }

    /**
     * Sets the anchor point and tether radius for this guardian.
     */
    public void setAnchor(BlockPos pos, int radius) {
        this.desiredAnchor = pos.immutable();
        this.deckResolved = false;
        this.deckHeight = this.desiredAnchor.getY();
        this.anchorPos = this.desiredAnchor;
        this.tetherRadius = radius;
        Vec3 center = Vec3.atCenterOf(this.anchorPos);
        this.orbitCenter = center.add(0.0D, PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK, 0.0D);
        this.setPos(center.x, this.orbitCenter.y + INITIAL_ASCENT_HEIGHT, center.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setPersistenceRequired();
        this.anchorPoint = new BlockPos(
            this.anchorPos.getX(),
            this.anchorPos.getY() + PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK + IDLE_ALTITUDE_BONUS,
            this.anchorPos.getZ()
        );
    }

    public void initialiseDeckAnchor(BlockPos deckAnchor, int radius) {
        this.desiredAnchor = deckAnchor.immutable();
        this.anchorPos = deckAnchor;
        this.deckHeight = deckAnchor.getY();
        this.tetherRadius = radius;
        this.deckResolved = true;
        Vec3 center = Vec3.atCenterOf(this.anchorPos);
        this.orbitCenter = center.add(0.0D, PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK, 0.0D);
        this.setPos(center.x, this.orbitCenter.y + INITIAL_ASCENT_HEIGHT, center.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setPersistenceRequired();
        this.anchorPoint = new BlockPos(
            this.anchorPos.getX(),
            this.anchorPos.getY() + PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK + IDLE_ALTITUDE_BONUS,
            this.anchorPos.getZ()
        );
    }

    /**
     * Overrides orbit center calculation to stay near anchor point.
     * Ensures Guardian never flies outside the structure area.
     */
    @Override
    protected void updateOrbitCenter(LivingEntity target) {
        if (this.anchorPos == null) {
            // Fallback to default behavior if no anchor set
            super.updateOrbitCenter(target);
            return;
        }

        Vec3 anchorVec = Vec3.atCenterOf(this.anchorPos);
        int orbitY = this.anchorPos.getY() + PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK;

        // Calculate orbit center constrained to tether radius
        if (target != null && target.isAlive()) {
            Vec3 targetPos = target.position();

            // Calculate vector from anchor to target
            Vec3 toTarget = new Vec3(
                targetPos.x - anchorVec.x,
                0, // Ignore vertical component for horizontal tethering
                targetPos.z - anchorVec.z
            );
            double distance = toTarget.length();

            // If target is outside tether radius, clamp to edge of radius
            if (distance > this.tetherRadius * 0.8D) { // Use 80% of radius for safer margin
                toTarget = toTarget.normalize().scale(this.tetherRadius * 0.8D);
            }

            // Set orbit center within tether radius, elevated well above deck
            Vec3 desiredCenter = new Vec3(
                anchorVec.x + toTarget.x,
                orbitY,
                anchorVec.z + toTarget.z
            );

            // Smooth transition to new orbit center
            if (this.orbitCenter.equals(Vec3.ZERO)) {
                this.orbitCenter = desiredCenter;
            } else {
                // Slower lerp for Guardian to maintain stable patrol
                this.orbitCenter = this.orbitCenter.lerp(desiredCenter, 0.03D);
            }
        } else {
            // No target - orbit around anchor at high altitude
            this.orbitCenter = new Vec3(
                this.anchorPos.getX() + 0.5D,
                orbitY + IDLE_ALTITUDE_BONUS,
                this.anchorPos.getZ() + 0.5D
            );
        }
    }

    /**
     * Overrides flight height enforcement to work relative to anchor.
     * Strictly enforces both height and horizontal boundaries.
     */
    @Override
    protected Vec3 enforceFlightHeight(Vec3 targetPos) {
        if (this.anchorPos == null) {
            return super.enforceFlightHeight(targetPos);
        }

        double minY = this.getTarget() != null ? this.anchorPos.getY() - SKYSHIP_VERTICAL_RANGE * 0.5D : this.anchorPos.getY() + MIN_FLIGHT_HEIGHT_ABOVE_DECK;
        double maxY = this.getTarget() != null ? this.anchorPos.getY() + SKYSHIP_VERTICAL_RANGE : this.anchorPos.getY() + MAX_FLIGHT_HEIGHT_ABOVE_DECK + IDLE_ALTITUDE_BONUS;
        double clampedY = Mth.clamp(targetPos.y, minY, maxY);

        // Strictly enforce horizontal tether - prevent ANY movement outside radius
        Vec3 anchorVec = Vec3.atCenterOf(this.anchorPos);
        double dx = targetPos.x - anchorVec.x;
        double dz = targetPos.z - anchorVec.z;
        double horizontalDistSq = dx * dx + dz * dz;
        double maxDistSq = this.tetherRadius * this.tetherRadius;

        // If outside tether radius, pull back to edge
        if (horizontalDistSq > maxDistSq) {
            double horizontalDist = Math.sqrt(horizontalDistSq);
            double scale = this.tetherRadius / horizontalDist;
            return new Vec3(
                anchorVec.x + dx * scale,
                clampedY,
                anchorVec.z + dz * scale
            );
        }

        return new Vec3(targetPos.x, clampedY, targetPos.z);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        resolveDeckAnchor(level);
        if (this.anchorPos != null) {
            double minY = this.anchorPos.getY() + MIN_FLIGHT_HEIGHT_ABOVE_DECK;
            if (this.getY() < minY) {
                Vec3 motion = this.getDeltaMovement();
                this.setPos(this.getX(), minY, this.getZ());
                this.setDeltaMovement(motion.x, Math.max(0.0D, motion.y), motion.z);
            }
            ensureAggroOnNearbyPlayers(level);
            if (this.getTarget() == null) {
                idleRecenterTicks++;
                double idleY = this.anchorPos.getY() + PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK + IDLE_ALTITUDE_BONUS;
                if (this.getY() < idleY) {
                    Vec3 motion = this.getDeltaMovement();
                    double lift = Math.max(0.06D, Math.min(0.3D, idleY - this.getY()) * 0.05D);
                    this.setDeltaMovement(motion.x, Math.max(lift, motion.y), motion.z);
                }
                if (idleRecenterTicks >= 200) {
                    idleRecenterTicks = 0;
                    recenterAboveAnchor(level);
                }
            } else {
                idleRecenterTicks = 0;
            }
        }
        super.customServerAiStep(level);
    }

    private void recenterAboveAnchor(ServerLevel level) {
        if (this.anchorPos == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(this.anchorPos);
        Vec3 current = this.position();
        double horizontalDist = current.subtract(center.x, current.y, center.z).horizontalDistance();

        // Allow patrol within 50 block radius - only recenter if outside this area
        if (horizontalDist <= 50.0D) {
            return;
        }

        // Smoothly guide back to center via orbit center instead of teleporting
        double desiredY = this.anchorPos.getY() + PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK + IDLE_ALTITUDE_BONUS;
        Vec3 targetOrbit = center.add(0.0D, PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK, 0.0D);

        // Gradual orbit center adjustment for smooth return
        this.orbitCenter = this.orbitCenter.lerp(targetOrbit, 0.1D);

        // Apply gentle pull towards center through movement control
        Vec3 toCenter = center.subtract(current.x, current.y, current.z).normalize();
        this.getMoveControl().setWantedPosition(
            current.x + toCenter.x * 10.0D,
            desiredY,
            current.z + toCenter.z * 10.0D,
            1.2D
        );
    }

    private void resolveDeckAnchor(ServerLevel level) {
        if (this.deckResolved || this.desiredAnchor == null) {
            return;
        }

        if (!level.isLoaded(this.desiredAnchor)) {
            return;
        }

        int highest = Integer.MIN_VALUE;
        int minY = level.dimensionType().minY();
        int worldTop = minY + level.dimensionType().logicalHeight() - 1;
        int scanTop = Math.min(worldTop, this.desiredAnchor.getY() + MAX_SCAN_HEIGHT_OFFSET);
        int scanFloor = Math.max(minY, this.desiredAnchor.getY() - MIN_SCAN_HEIGHT_OFFSET);
        MutableBlockPos cursor = new MutableBlockPos();

        for (int[] offset : DECK_SAMPLE_OFFSETS) {
            int sampleX = this.desiredAnchor.getX() + Mth.clamp(offset[0], -this.tetherRadius, this.tetherRadius);
            int sampleZ = this.desiredAnchor.getZ() + Mth.clamp(offset[1], -this.tetherRadius, this.tetherRadius);
            cursor.set(sampleX, scanTop, sampleZ);
            if (!level.isLoaded(cursor)) {
                continue;
            }
            for (int y = scanTop; y >= scanFloor; y--) {
                cursor.setY(y);
                BlockState state = level.getBlockState(cursor);
                if (!state.isAir() && !state.getCollisionShape(level, cursor).isEmpty()) {
                    highest = Math.max(highest, y);
                    break;
                }
            }
        }

        if (highest == Integer.MIN_VALUE) {
            highest = this.desiredAnchor.getY();
        }

        this.deckHeight = highest;
        this.anchorPos = new BlockPos(this.desiredAnchor.getX(), highest, this.desiredAnchor.getZ());
        this.orbitCenter = Vec3.atCenterOf(this.anchorPos).add(0.0D, PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK, 0.0D);
        this.deckResolved = true;
        this.anchorPoint = new BlockPos(
            this.anchorPos.getX(),
            this.anchorPos.getY() + PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK + IDLE_ALTITUDE_BONUS,
            this.anchorPos.getZ()
        );
    }

    class GuardianPatrolGoal extends TerrorPhantomMoveTargetGoal {
        private double angle;
        private double patrolRadius;
        private boolean clockwise;
        private int retargetCooldown;

        @Override
        public boolean canUse() {
            return !TerrorPhantomGuardian.this.isDying() &&
                   TerrorPhantomGuardian.this.getTarget() == null &&
                   TerrorPhantomGuardian.this.anchorPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            RandomSource random = TerrorPhantomGuardian.this.random;
            this.clockwise = random.nextBoolean();
            this.angle = random.nextDouble() * Mth.TWO_PI;
            this.patrolRadius = computeRadius(random);
            this.retargetCooldown = 0;
            TerrorPhantomGuardian.this.attackPhase = AttackPhase.CIRCLE;
            placeTarget(false);
        }

        @Override
        public void stop() {
            this.retargetCooldown = 0;
        }

        @Override
        public void tick() {
            if (TerrorPhantomGuardian.this.anchorPos == null) {
                return;
            }

            if (touchingTarget()) {
                RandomSource random = TerrorPhantomGuardian.this.random;
                if (random.nextInt(3) == 0) {
                    this.patrolRadius = computeRadius(random);
                }
                placeTarget(true);
                this.retargetCooldown = 0;
            } else if (++this.retargetCooldown >= 120) {
                this.retargetCooldown = 0;
                placeTarget(true);
            }
        }

        private void placeTarget(boolean advance) {
            if (TerrorPhantomGuardian.this.anchorPos == null) {
                return;
            }

            if (advance) {
                this.angle += this.clockwise ? PATROL_STEP_RADIANS : -PATROL_STEP_RADIANS;
            }

            Vec3 center = Vec3.atCenterOf(TerrorPhantomGuardian.this.anchorPos);
            double baseY = TerrorPhantomGuardian.this.anchorPos.getY() + PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK + IDLE_ALTITUDE_BONUS;
            Vec3 rawTarget = new Vec3(
                center.x + this.patrolRadius * Math.cos(this.angle),
                baseY,
                center.z + this.patrolRadius * Math.sin(this.angle)
            );
            Vec3 enforced = TerrorPhantomGuardian.this.enforceFlightHeight(rawTarget);
            TerrorPhantomGuardian.this.moveTargetPoint = enforced;
        }

        private double computeRadius(RandomSource random) {
            double max = Math.max(28.0D, TerrorPhantomGuardian.this.tetherRadius * 0.75D);
            double min = Math.min(32.0D, max - 4.0D);
            if (max - min < 1.0D) {
                return max;
            }
            double sampled = min + (max - min) * random.nextDouble();
            return Mth.clamp(sampled, 24.0D, max);
        }
    }

    private void ensureAggroOnNearbyPlayers(ServerLevel level) {
        if (this.anchorPos == null || this.isRemoved()) {
            return;
        }

        Vec3 anchorVec = Vec3.atCenterOf(this.anchorPos);
        double radius = Math.min(this.tetherRadius, ACTIVE_DETECTION_RADIUS);
        AABB searchArea = new AABB(
                this.anchorPos.getX() - radius,
                this.anchorPos.getY() - SKYSHIP_VERTICAL_RANGE * 0.5D,
                this.anchorPos.getZ() - radius,
                this.anchorPos.getX() + radius,
                this.anchorPos.getY() + SKYSHIP_VERTICAL_RANGE,
                this.anchorPos.getZ() + radius
        );

        java.util.List<Player> players = level.getEntitiesOfClass(
                Player.class,
                searchArea,
                player -> isValidCombatTarget(player) && this.getSensing().hasLineOfSight(player)
        );

        Player currentTarget = this.getTarget() instanceof Player player ? player : null;
        if (currentTarget != null && !isValidCombatTarget(currentTarget)) {
            disengageCurrentTarget();
            currentTarget = null;
        }
        if (currentTarget != null && !trackCurrentTargetVisibility(currentTarget)) {
            disengageCurrentTarget();
            currentTarget = null;
        }

        if (players.isEmpty()) {
            if (currentTarget != null) {
                double distSq = currentTarget.distanceToSqr(anchorVec.x, anchorVec.y, anchorVec.z);
                if (distSq <= PASSIVE_RETALIATION_RADIUS * PASSIVE_RETALIATION_RADIUS) {
                    return;
                }
            }
            disengageCurrentTarget();
            this.engagedPlayers.clear();
            return;
        }

        java.util.HashSet<java.util.UUID> present = new java.util.HashSet<>();

        for (Player player : players) {
            present.add(player.getUUID());
            if (this.engagedPlayers.add(player.getUUID())) {
                this.setTarget(player);
                this.setAggressive(true);
                JustEnoughGuns.LOGGER.debug("[TerrorGuardian] Aggro acquired on player {} at {}", player.getName().getString(), player.blockPosition());
                return;
            }
        }

        this.engagedPlayers.retainAll(present);

        if (currentTarget == null || !present.contains(currentTarget.getUUID())) {
            Player nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Player player : players) {
                double dist = player.distanceToSqr(this);
                if (nearest == null || dist < nearestDistance) {
                    nearest = player;
                    nearestDistance = dist;
                }
            }

            if (nearest != null) {
                this.setTarget(nearest);
                this.setAggressive(true);
                this.engagedPlayers.add(nearest.getUUID());
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (amount > 0.0F) {
            handleEffectiveFireSupport(level, source);
        }

        float adjustedAmount = applyBallisticProtectionReduction(source, amount);
        boolean damaged = super.hurtServer(level, source, adjustedAmount);
        if (!damaged || amount <= 0.0F || this.anchorPos == null) {
            return damaged;
        }

        LivingEntity retaliateTarget = null;
        Entity causing = source.getEntity();
        Entity direct = source.getDirectEntity();

        if (causing instanceof LivingEntity living) {
            retaliateTarget = living;
        } else if (direct instanceof LivingEntity living) {
            retaliateTarget = living;
        } else if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            retaliateTarget = owner;
        }

        if (retaliateTarget instanceof Player player && isValidCombatTarget(player)) {
            Vec3 anchorVec = Vec3.atCenterOf(this.anchorPos);
            double distSq = player.distanceToSqr(anchorVec.x, anchorVec.y, anchorVec.z);
            if (distSq <= PASSIVE_RETALIATION_RADIUS * PASSIVE_RETALIATION_RADIUS) {
                if (this.engagedPlayers.add(player.getUUID())) {
                    JustEnoughGuns.LOGGER.debug("[TerrorGuardian] Retaliating against {} at distance {}", player.getName().getString(), Math.sqrt(distSq));
                }
                this.setTarget(player);
                this.setAggressive(true);
            }
        }

        return damaged;
    }

    private void handleEffectiveFireSupport(ServerLevel level, DamageSource source) {
        if (this.getType() != ModEntities.TERROR_PHANTOM_GUARDIAN.get()) {
            return;
        }
        if (!(source.getDirectEntity() instanceof BulletEntity bullet)) {
            return;
        }
        if (!(bullet.getOwner() instanceof Player player) || !isValidCombatTarget(player)) {
            return;
        }

        Vec3 rangeCenter = this.anchorPos != null ? Vec3.atCenterOf(this.anchorPos) : this.position();
        double playerDistanceSq = player.distanceToSqr(rangeCenter.x, rangeCenter.y, rangeCenter.z);
        if (playerDistanceSq <= SUPPORT_MIN_TRIGGER_DISTANCE * SUPPORT_MIN_TRIGGER_DISTANCE
                || playerDistanceSq > PASSIVE_RETALIATION_RADIUS * PASSIVE_RETALIATION_RADIUS) {
            return;
        }

        this.lastEffectiveFirePos = player.position();
        this.lastEffectiveFireTick = level.getGameTime();
        updateGuardianSupport(level, this.lastEffectiveFirePos);
    }

    private void updateGuardianSupport(ServerLevel level, Vec3 firePos) {
        java.util.List<PhantomGunnerMinion> supports = getActiveGuardianSupports(level);
        for (PhantomGunnerMinion support : supports) {
            support.assignGuardianSupport(this, firePos);
        }

        int spawnSlots = Math.min(SUPPORT_TARGET_COUNT - supports.size(), SUPPORT_MAX_COUNT - supports.size());
        if (spawnSlots <= 0) {
            return;
        }

        long gameTime = level.getGameTime();
        if (gameTime - this.lastSupportSpawnTick < SUPPORT_SPAWN_COOLDOWN_TICKS) {
            return;
        }

        int spawned = 0;
        for (int i = 0; i < spawnSlots; i++) {
            if (spawnGuardianSupport(level, firePos, i, spawnSlots)) {
                spawned++;
            }
        }
        if (spawned > 0) {
            this.lastSupportSpawnTick = gameTime;
            playSummonEffects(level, this.blockPosition().above(4));
        }
    }

    private java.util.List<PhantomGunnerMinion> getActiveGuardianSupports(ServerLevel level) {
        this.guardianSupports.removeIf(support -> support == null
                || support.level() != level
                || support.isRemoved()
                || !support.isAlive()
                || !support.isGuardianSupportFor(this));
        return this.guardianSupports;
    }

    private boolean spawnGuardianSupport(ServerLevel level, Vec3 firePos, int index, int total) {
        PhantomGunnerMinion gunner = new PhantomGunnerMinion(ModEntities.PHANTOM_GUNNER_MINION.get(), level);
        double angle = Mth.TWO_PI * ((index + level.getRandom().nextDouble() * 0.35D) / Math.max(1, total));
        double radius = SUPPORT_SPAWN_MIN_SPREAD + level.getRandom().nextDouble() * SUPPORT_SPAWN_RANDOM_SPREAD;
        Vec3 spawnCenter = firePos.add(
                Math.cos(angle) * radius,
                SUPPORT_SPAWN_HEIGHT_ABOVE_PLAYER + level.getRandom().nextInt(5) - 2.0D,
                Math.sin(angle) * radius
        );
        BlockPos spawnPos = BlockPos.containing(spawnCenter);
        gunner.setPos(spawnCenter.x, spawnCenter.y, spawnCenter.z);
        gunner.setYRot(level.getRandom().nextFloat() * 360.0F);
        gunner.yRotO = gunner.getYRot();
        gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.EVENT, null);
        gunner.assignGuardianSupport(this, firePos);
        boolean added = level.addFreshEntity(gunner);
        if (added) {
            this.guardianSupports.add(gunner);
        }
        return added;
    }

    private static float applyBallisticProtectionReduction(DamageSource source, float amount) {
        if (!(source.getDirectEntity() instanceof BulletEntity bullet)) {
            return amount;
        }
        return BallisticProtection.applyToIntrinsicArmor(
                amount,
                bullet.getGunStats(),
                BallisticProtection.BOUND_TERROR_PHANTOM,
                BallisticProtection.isRocketDirectHit(bullet.getGunStats())
        ).finalDamage();
    }

    private boolean isValidCombatTarget(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private boolean trackCurrentTargetVisibility(Player player) {
        if (this.getSensing().hasLineOfSight(player)) {
            this.targetLostSightTicks = 0;
            return true;
        }
        return ++this.targetLostSightTicks < TARGET_LOST_SIGHT_FORGET_TICKS;
    }

    private void disengageCurrentTarget() {
        if (this.getTarget() instanceof Player player) {
            this.engagedPlayers.remove(player.getUUID());
        }
        this.targetLostSightTicks = 0;
        this.setTarget(null);
        this.setAggressive(false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt(TAG_TETHER_RADIUS, this.tetherRadius);
        output.putInt(TAG_DECK_HEIGHT, this.deckHeight);
        output.putBoolean(TAG_DECK_RESOLVED, this.deckResolved);
        if (this.desiredAnchor != null) {
            output.putInt(TAG_ANCHOR_X, this.desiredAnchor.getX());
            output.putInt(TAG_ANCHOR_Y, this.desiredAnchor.getY());
            output.putInt(TAG_ANCHOR_Z, this.desiredAnchor.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.tetherRadius = input.getIntOr(TAG_TETHER_RADIUS, this.tetherRadius <= 0 ? DEFAULT_TETHER_RADIUS : this.tetherRadius);
        this.deckHeight = input.getIntOr(TAG_DECK_HEIGHT, this.deckHeight);
        boolean resolved = input.getBooleanOr(TAG_DECK_RESOLVED, false);
        int anchorX = input.getIntOr(TAG_ANCHOR_X, Integer.MIN_VALUE);
        int anchorY = input.getIntOr(TAG_ANCHOR_Y, Integer.MIN_VALUE);
        int anchorZ = input.getIntOr(TAG_ANCHOR_Z, Integer.MIN_VALUE);

        if (anchorX != Integer.MIN_VALUE && anchorY != Integer.MIN_VALUE && anchorZ != Integer.MIN_VALUE) {
            BlockPos anchor = new BlockPos(anchorX, anchorY, anchorZ);
            if (resolved) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    this.initialiseDeckAnchor(anchor, this.tetherRadius);
                } else {
                    this.desiredAnchor = anchor;
                    this.anchorPos = anchor;
                    this.deckHeight = anchor.getY();
                    this.tetherRadius = this.tetherRadius <= 0 ? DEFAULT_TETHER_RADIUS : this.tetherRadius;
                    this.deckResolved = true;
                    this.orbitCenter = Vec3.atCenterOf(this.anchorPos).add(0.0D, PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK, 0.0D);
                }
            } else {
                this.desiredAnchor = anchor;
                this.anchorPos = anchor;
                this.deckResolved = false;
                if (this.deckHeight <= 0) {
                    this.deckHeight = anchor.getY();
                }
                this.orbitCenter = Vec3.atCenterOf(this.anchorPos).add(0.0D, PREFERRED_FLIGHT_HEIGHT_ABOVE_DECK, 0.0D);
            }
        } else {
            this.deckResolved = false;
        }
    }

    @Override
    public boolean isDying() {
        // Guardian uses custom death sequence, but still reports parent death state for compatibility
        return this.getHealth() <= 0 || this.guardianDeathTicks > 0;
    }
}
