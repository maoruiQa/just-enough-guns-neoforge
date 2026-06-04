package ttv.migami.jeg.entity.ai;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.faction.raid.FactionRaidManager;
import ttv.migami.jeg.gun.BulletPenetrationHelper;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.NetworkHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class GunAttackGoal<T extends PathfinderMob> extends Goal {
    private static final float GUNNER_BREAK_REACH = 3.25F;
    private static final int TERRAIN_PATH_RECHECK_TICKS = 6;
    private static final int TERRAIN_ROUTE_COMMIT_TICKS = 10;
    private static final int TERRAIN_ROUTE_COMMIT_STEPS = 2;
    private static final int TERRAIN_PLACE_COOLDOWN_TICKS = 4;
    private static final int PROACTIVE_TERRAIN_PLACE_COOLDOWN_TICKS = 8;
    private static final int TERRAIN_BREAKOUT_PLACE_COOLDOWN_TICKS = 6;
    private static final int TERRAIN_BREAKOUT_ACTION_COOLDOWN_TICKS = 8;
    private static final int TERRAIN_PROGRESS_SAMPLE_TICKS = 4;
    private static final int TERRAIN_NO_PROGRESS_THRESHOLD_TICKS = 12;
    private static final int TERRAIN_BREAKOUT_THRESHOLD_TICKS = 28;
    private static final int TERRAIN_EDIT_WINDOW_TICKS = 120;
    private static final int TERRAIN_MAX_EDITS_PER_WINDOW = 4;
    private static final int TERRAIN_MAX_CONSECUTIVE_ACTIONS_WITHOUT_PROGRESS = 3;
    private static final int TERRAIN_MAX_ROUTE_ACTIONS = 4;
    private static final int TERRAIN_MAX_ROUTE_ASCENT_STEPS = 2;
    private static final int TERRAIN_MAX_ROUTE_BRIDGE_SPAN = 3;
    private static final int LOCAL_ROUTE_MAX_DEPTH = 6;
    private static final int LOCAL_ROUTE_MAX_PLACED_BLOCKS = 3;
    private static final int LOCAL_ROUTE_MAX_BROKEN_BLOCKS = 2;
    private static final int LOCAL_ROUTE_MAX_HORIZONTAL_RADIUS = 4;
    private static final int LOCAL_ROUTE_MAX_VERTICAL_DEVIATION = 2;
    private static final double TERRAIN_MIN_DISTANCE_SQR = 4.0D;
    private static final double PROACTIVE_TERRAIN_MIN_DISTANCE_SQR = 64.0D;
    private static final double PROACTIVE_TERRAIN_HEIGHT_ADVANTAGE = 0.75D;
    private static final double DIRECT_PATH_REACH_MARGIN = 2.0D;
    private static final double TERRAIN_PROGRESS_MOVEMENT_THRESHOLD_SQR = 0.0625D;
    private static final double TERRAIN_PROGRESS_DISTANCE_DELTA = 0.75D;
    private static final int PROACTIVE_BRIDGE_MAX_GAP = 1;
    private static final int ZBB_PATH_END_BREAK_BUILD_DISTANCE = 6;
    private static final int ZBB_CUSTOM_STUCK_TICKS_TO_BREAK_BUILD = 40;
    private static final int GROUP_SHARED_ROUTE_SEARCH_RADIUS = 1;
    private static final int SPIN_STUCK_THRESHOLD_TICKS = 8;
    private static final float SPIN_STUCK_YAW_DELTA_DEGREES = 35.0F;
    private static final int ZBB_DANGEROUS_BLOCKS_SEARCH_RADIUS = 1;
    private static final long ZBB_BREAK_COOLDOWN_TICKS = 20L;
    private static final long ZBB_BUILD_COOLDOWN_TICKS = 20L;
    private static final long ZBB_DANGEROUS_SCAN_COOLDOWN_TICKS = 20L;
    private static final long ZBB_BUILD_PROTECTION_TICKS = 15L;
    private static final long ZBB_DAMAGE_STORE_TICKS = 1200L;
    private static final Set<Block> ZBB_DANGEROUS_BLOCKS = Set.of(
        Blocks.FIRE,
        Blocks.SOUL_FIRE,
        Blocks.CAMPFIRE,
        Blocks.SOUL_CAMPFIRE,
        Blocks.CACTUS,
        Blocks.MAGMA_BLOCK,
        Blocks.SWEET_BERRY_BUSH,
        Blocks.WITHER_ROSE,
        Blocks.POWDER_SNOW,
        Blocks.LAVA,
        Blocks.COBWEB
    );
    private static final Map<ServerLevel, Map<String, SharedRouteCacheEntry>> SHARED_ROUTE_CACHE = new HashMap<>();

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

    protected BlockPos terrainBreakTarget;
    protected float terrainBreakProgress;
    protected int terrainBreakTicks;
    protected int terrainLastBreakStage = -1;
    protected int terrainPathRecheckCooldown = 0;
    protected boolean terrainPathAvailable = true;
    protected int terrainPlaceCooldown = 0;
    protected int terrainBreakoutCooldown = 0;
    protected TerrainPathState terrainPathState = TerrainPathState.PATHABLE;
    protected StuckReason terrainStuckReason = StuckReason.NONE;
    protected TerrainAction terrainLastAction = TerrainAction.NONE;
    protected Vec3 terrainLastSamplePos;
    protected double terrainLastSampleDistanceToTarget = Double.MAX_VALUE;
    protected int terrainSampleCooldown = 0;
    protected int terrainNoProgressTicks = 0;
    protected int terrainEditWindowTicks = 0;
    protected int terrainEditsInWindow = 0;
    protected int terrainConsecutiveActionsWithoutProgress = 0;
    protected TerrainRoutePhase terrainRoutePhase = TerrainRoutePhase.NONE;
    protected final Deque<TerrainPlan> terrainPlannedActions = new ArrayDeque<>();
    protected int terrainRouteCommitTicks = 0;
    protected int terrainCommittedStepsRemaining = 0;
    protected int terrainRouteStallTicks = 0;
    protected String lastFailedRouteSignature;
    protected int failedRouteRetryCooldown = 0;
    protected AttackMode attackMode = AttackMode.NORMAL;
    protected BlockPos lastPlacedSupportPos;
    protected long lastPlacedSupportGameTime = Long.MIN_VALUE;
    protected static final int SUPPORT_BREAK_PROTECTION_TICKS = 80;
    protected static final int TERRAIN_STANCE_HEADROOM_REQUIRED = 2;
    protected static final int MIN_TUNNEL_HEADROOM_ABOVE_WALL = 2;
    protected static final int MAX_TUNNEL_DESCENT = 1;
    protected static final int MAX_TUNNEL_ASCENT = 1;
    protected int breakBuildLockTicks = 0;
    protected int stableNormalPathTicks = 0;
    protected int supportFreezeTicks = 0;
    protected int terrainReplanCooldown = 0;
    protected int targetPersistenceTicks = 0;
    private long zbbBreakCooldownUntil = Long.MIN_VALUE;
    private long zbbBuildCooldownUntil = Long.MIN_VALUE;
    private long zbbDangerousCooldownUntil = Long.MIN_VALUE;
    private int zbbCustomStuckTicks = 0;
    private Vec3 zbbLastStuckCheckPos;
    private float spinStuckLastYaw;
    private int spinStuckTicks = 0;
    private final BlockPos.MutableBlockPos zbbLiftCurrentMobPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos zbbLiftBeforeJumpMobPos = new BlockPos.MutableBlockPos();
    private ZbbLiftState zbbLiftState = ZbbLiftState.IDLE;
    private final BlockPos.MutableBlockPos zbbBridgeFrontBlockPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos zbbBridgeBelowMobPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos zbbBridgeBelowFrontPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos zbbBridgeTwoBelowFrontPos = new BlockPos.MutableBlockPos();
    private final Map<BlockPos, Integer> zbbBlockDamage = new HashMap<>();
    private final Map<BlockPos, Integer> zbbBlockProgressIds = new HashMap<>();
    private final Map<BlockPos, Long> zbbProtectedBuiltBlocks = new HashMap<>();
    private static final AtomicInteger ZBB_NEXT_BLOCK_PROGRESS_ID = new AtomicInteger(1);
    private LiftExecutionState liftExecutionState = LiftExecutionState.IDLE;
    private BlockPos liftAnchorPos;
    private int liftExecutionTicks = 0;

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

        if (this.shooter instanceof net.minecraft.world.entity.monster.Drowned && this.shooter.getTags().contains("DrownedGunner")) {
            return isInWaterOrShade();
        }

        return true;
    }

    private boolean isInWaterOrShade() {
        if (this.shooter.isInWater()) {
            return true;
        }

        BlockPos pos = this.shooter.blockPosition();
        boolean isRaining = this.shooter.level().isRaining();
        boolean isNight = !this.shooter.level().canSeeSky(pos);
        long dayTime = this.shooter.level().getDayTime() % 24000L;
        boolean isDarkTime = dayTime > 12500L && dayTime < 23500L;

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
        this.terrainPathRecheckCooldown = 0;
        this.terrainPathAvailable = true;
        this.terrainPathState = TerrainPathState.PATHABLE;
        this.terrainPlaceCooldown = 0;
        this.terrainBreakoutCooldown = 0;
        this.terrainStuckReason = StuckReason.NONE;
        this.terrainLastAction = TerrainAction.NONE;
        this.terrainLastSamplePos = null;
        this.terrainLastSampleDistanceToTarget = Double.MAX_VALUE;
        this.terrainSampleCooldown = 0;
        this.terrainNoProgressTicks = 0;
        this.terrainEditWindowTicks = 0;
        this.terrainEditsInWindow = 0;
        this.terrainConsecutiveActionsWithoutProgress = 0;
        this.terrainRoutePhase = TerrainRoutePhase.NONE;
        this.terrainPlannedActions.clear();
        this.terrainRouteCommitTicks = 0;
        this.terrainCommittedStepsRemaining = 0;
        this.attackMode = AttackMode.NORMAL;
        this.breakBuildLockTicks = 0;
        this.stableNormalPathTicks = 0;
        this.supportFreezeTicks = 0;
        this.terrainReplanCooldown = 0;
        this.targetPersistenceTicks = 0;
        this.liftExecutionState = LiftExecutionState.IDLE;
        this.liftAnchorPos = null;
        this.liftExecutionTicks = 0;
        this.zbbBreakCooldownUntil = Long.MIN_VALUE;
        this.zbbBuildCooldownUntil = Long.MIN_VALUE;
        this.zbbDangerousCooldownUntil = Long.MIN_VALUE;
        this.zbbCustomStuckTicks = 0;
        this.zbbLastStuckCheckPos = null;
        this.spinStuckLastYaw = this.shooter.getYRot();
        this.spinStuckTicks = 0;
        this.zbbLiftState = ZbbLiftState.IDLE;
        this.zbbBlockDamage.clear();
        this.zbbBlockProgressIds.clear();
        this.zbbProtectedBuiltBlocks.clear();
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
            } else if (canSeeTarget) {
                this.lastKnownPosition = new Vec3(target.getX(), target.getY(), target.getZ());
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            if (this.aiType == AIType.COWARD &&
                (this.shooter.getHealth() < (this.shooter.getMaxHealth() / 3) || this.shooter.invulnerableTime != 0)) {
                this.isPanicked = true;
                this.panickTimer = 20;
            }

            if (this.isPanicked) {
                this.resetTerrainBreakTarget();
                Vec3 vec3 = DefaultRandomPos.getPos(this.shooter, 5, 4);
                if (vec3 != null) {
                    this.shooter.getNavigation().moveTo(vec3.x, vec3.y, vec3.z, this.speedModifier);
                }
                this.panickTimer--;
            }

            if (this.panickTimer <= 0) {
                this.isPanicked = false;
            }

            if (gunItem.usesLoadedAmmo()) {
                Integer ammoComponent = heldItem.get(ModDataComponents.GUN_AMMO.get());
                int currentAmmo = ammoComponent != null ? ammoComponent : 0;
                if (currentAmmo <= 0) {
                    this.resetTerrainBreakTarget();
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

            if (!this.isReloading && !this.isPanicked) {
                boolean shouldUseBuildPursuit = !canSeeTarget || this.shouldUseZbbBreakAndBuild((ServerLevel) this.shooter.level(), target);
                if (!shouldUseBuildPursuit) {
                    this.resetTerrainStateForVisibleTarget();
                } else {
                    this.shooter.getNavigation().moveTo(target, this.speedModifier);
                    this.tickZbbUnseenRuntime(target);
                    double targetEyeY = target.getEyeY();
                    this.shooter.getLookControl().setLookAt(target.getX(), targetEyeY, target.getZ());
                    this.shooter.lookAt(EntityAnchorArgument.Anchor.FEET, target.getBoundingBox().getCenter());
                    return;
                }
            }

            if (canSeeTarget && this.shooter.level().getRandom().nextFloat() < 0.1F && this.aiType == AIType.TACTICAL) {
                Vec3 flankingPosition = findFlankingPosition(this.lastKnownPosition, target);
                if (flankingPosition != null) {
                    this.shooter.getNavigation().moveTo(flankingPosition.x, flankingPosition.y, flankingPosition.z, this.speedModifier);
                }
            }

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
                    if (distanceToTarget > (double) (this.attackRadiusSqr * 0.75F)) {
                        this.strafingBackwards = false;
                    } else if (distanceToTarget < (double) (this.attackRadiusSqr * 0.25F)) {
                        this.strafingBackwards = true;
                    }
                    this.shooter.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                }

                boolean canShoot = gunItem.usesLoadedAmmo() ?
                    (heldItem.get(ModDataComponents.GUN_AMMO.get()) != null ? heldItem.get(ModDataComponents.GUN_AMMO.get()) : 0) > 0 : true;

                if (canShoot && --this.attackTime <= 0 && this.seeTime >= -20 && this.seeTime >= 10) {
                    shoot(target, gunItem, stats);
                    this.attackTime = gunnerFireDelay(stats);
                }
            }

            double targetEyeY = target.getEyeY();
            this.shooter.getLookControl().setLookAt(target.getX(), targetEyeY, target.getZ());
            this.shooter.lookAt(EntityAnchorArgument.Anchor.FEET, target.getBoundingBox().getCenter());
        }
    }


    private boolean tickZbbUnseenRuntime(LivingEntity target) {
        if (!(this.shooter.level() instanceof ServerLevel level) || target == null) {
            return false;
        }

        this.cleanupZbbStorages(level);

        if (this.shouldForceWallLift(level, target)) {
            Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
            if (direction != null && this.zbbTryImmediateWallSelfLift(level, this.shooter.blockPosition(), direction)) {
                return true;
            }
        }

        boolean acted = false;
        if (this.shouldUseZbbBreakAndBuild(level, target)) {
            acted = this.tickZbbBreakAndBuildState(level, target);
        } else {
            this.tickZbbDefaultState(level);
        }

        return acted;
    }

    private boolean shouldUseZbbBreakAndBuild(ServerLevel level, LivingEntity target) {
        if (this.zbbLiftState != ZbbLiftState.IDLE) {
            return true;
        }

        Path path = this.shooter.getNavigation().getPath();
        if (path == null) {
            return true;
        }

        if (this.isZbbCustomStuck(path)) {
            return true;
        }

        boolean hasActivePath = !path.isDone() && path.getNodeCount() > 0;
        boolean pathCanReachTarget = path.canReach();
        if (hasActivePath && pathCanReachTarget) {
            return false;
        }

        Node endNode = path.getEndNode();
        if (endNode == null) {
            return true;
        }

        double mobToEndNodeDistanceSq = this.shooter.distanceToSqr(endNode.x + 0.5D, endNode.y, endNode.z + 0.5D);
        boolean hasPartialPathAndMobReachedItsEnd = !pathCanReachTarget && mobToEndNodeDistanceSq < (double) (ZBB_PATH_END_BREAK_BUILD_DISTANCE * ZBB_PATH_END_BREAK_BUILD_DISTANCE);
        if (hasPartialPathAndMobReachedItsEnd) {
            return true;
        }

        if (path.isDone()) {
            double mobToTargetDistanceSq = this.shooter.distanceToSqr(target);
            if (mobToTargetDistanceSq > (double) (ZBB_PATH_END_BREAK_BUILD_DISTANCE * ZBB_PATH_END_BREAK_BUILD_DISTANCE)) {
                return true;
            }
        }

        return false;
    }

    private boolean isZbbCustomStuck(Path path) {
        if (this.shooter.getTarget() == null || path == null || path.isDone()) {
            this.zbbCustomStuckTicks = 0;
            this.zbbLastStuckCheckPos = null;
            this.spinStuckLastYaw = this.shooter.getYRot();
            this.spinStuckTicks = 0;
            return false;
        }

        Vec3 currentPos = this.shooter.position();
        if (this.zbbLastStuckCheckPos == null) {
            this.zbbLastStuckCheckPos = currentPos;
            this.zbbCustomStuckTicks = 0;
            this.spinStuckLastYaw = this.shooter.getYRot();
            this.spinStuckTicks = 0;
            return false;
        }

        double movedSq = currentPos.distanceToSqr(this.zbbLastStuckCheckPos);
        float currentYaw = this.shooter.getYRot();
        float yawDelta = Math.abs(Mth.degreesDifference(currentYaw, this.spinStuckLastYaw));
        this.zbbLastStuckCheckPos = currentPos;
        this.spinStuckLastYaw = currentYaw;
        if (movedSq < 0.0009D) {
            this.zbbCustomStuckTicks++;
            if (yawDelta >= SPIN_STUCK_YAW_DELTA_DEGREES) {
                this.spinStuckTicks++;
            } else {
                this.spinStuckTicks = 0;
            }
        } else {
            this.zbbCustomStuckTicks = 0;
            this.spinStuckTicks = 0;
        }
        return this.zbbCustomStuckTicks >= ZBB_CUSTOM_STUCK_TICKS_TO_BREAK_BUILD
            || this.spinStuckTicks >= SPIN_STUCK_THRESHOLD_TICKS;
    }

    private boolean tickZbbBreakAndBuildState(ServerLevel level, LivingEntity target) {
        boolean acted = this.tickZbbAdjustHeight(level, target);
        acted |= this.tickZbbClearObstacles(level, target);
        if (this.zbbLiftState == ZbbLiftState.IDLE) {
            acted |= this.tickZbbDetourOrBuildAroundUnbreakable(level, target);
            acted |= this.tickZbbMitigateDangerousBlocks(level);
        }
        return acted;
    }

    private void tickZbbDefaultState(ServerLevel level) {
        this.tickZbbMitigateDangerousBlocks(level);
    }

    private boolean tickZbbAdjustHeight(ServerLevel level, LivingEntity target) {
        int mobX = Mth.floor(this.shooter.getX());
        int mobY = Mth.floor(this.shooter.getY());
        int mobZ = Mth.floor(this.shooter.getZ());
        int targetY = Mth.floor(target.getY());
        this.zbbLiftCurrentMobPos.set(mobX, mobY, mobZ);

        for (int i = 0; i < 2; i++) {
            ZbbLiftState prevState = this.zbbLiftState;
            switch (this.zbbLiftState) {
                case IDLE -> {
                    if (this.zbbAdjustIdle(level, targetY)) {
                        this.zbbLiftState = ZbbLiftState.JUMPING;
                    }
                }
                case JUMPING -> {
                    if (this.zbbAdjustJumping()) {
                        this.zbbLiftState = ZbbLiftState.WAITING_FOR_BLOCK;
                    }
                }
                case WAITING_FOR_BLOCK -> {
                    if (this.zbbAdjustWaitingForBlock(level, targetY)) {
                        this.zbbLiftState = ZbbLiftState.IDLE;
                    }
                }
            }
            if (this.zbbLiftState == ZbbLiftState.WAITING_FOR_BLOCK || this.zbbLiftState == prevState) {
                break;
            }
        }
        return this.zbbLiftState != ZbbLiftState.IDLE;
    }

    private boolean zbbAdjustIdle(ServerLevel level, int targetY) {
        if (targetY <= this.zbbLiftCurrentMobPos.getY() + 1) {
            return false;
        }
        BlockPos blockAboveUs = this.getNearestCollidingBlockWithHitbox(level, this.shooter, new Vec3(0.0D, 1.0D, 0.0D));
        BlockPos posUnderBottomCenter = this.getZbbBlockUnderBottomCenter();
        return (blockAboveUs == null || this.isZbbFreePass(level, blockAboveUs)) && this.zbbCanBuild(level, posUnderBottomCenter.above());
    }

    private boolean zbbAdjustJumping() {
        this.zbbLiftBeforeJumpMobPos.set(this.zbbLiftCurrentMobPos.getX(), this.zbbLiftCurrentMobPos.getY(), this.zbbLiftCurrentMobPos.getZ());
        this.shooter.getJumpControl().jump();
        return true;
    }

    private boolean zbbAdjustWaitingForBlock(ServerLevel level, int targetY) {
        int startY = this.zbbLiftBeforeJumpMobPos.getY();
        double currentY = this.shooter.getY();
        double verticalSpeed = this.shooter.getDeltaMovement().y;
        if (targetY <= startY + 1 || this.shooter.onGround() || verticalSpeed < 0.0D) {
            return true;
        }
        if (currentY < startY + 1) {
            return false;
        }
        BlockPos posUnderBottomCenter = this.getZbbBlockUnderBottomCenter();
        if (this.zbbTryRawBuild(level, posUnderBottomCenter)) {
            this.zbbFreeze();
            return true;
        }
        if (this.zbbTryBreak(level, posUnderBottomCenter)) {
            this.zbbFreeze();
        }
        return true;
    }

    private boolean tickZbbClearObstacles(ServerLevel level, LivingEntity target) {
        double randomMultiplier = Mth.randomBetween(this.shooter.getRandom(), 0.01F, 1.0F);
        double checkDistance = 0.9D * randomMultiplier;
        Vec3 directionToTarget = target.position().subtract(this.shooter.position()).normalize();
        if (directionToTarget.lengthSqr() < 1.0E-6D) {
            return false;
        }
        BlockPos blockToBreak = this.getNearestCollidingBlockWithHitbox(level, this.shooter, directionToTarget.scale(checkDistance));
        return blockToBreak != null && this.zbbTryBreak(level, blockToBreak);
    }

    private boolean tickZbbDetourOrBuildAroundUnbreakable(ServerLevel level, LivingEntity target) {
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return this.tickZbbBridge(level, target);
        }

        RouteObstacleType obstacleType = this.getRouteObstacleType(level, target, false, direction);
        if (obstacleType != RouteObstacleType.UNBREAKABLE) {
            return this.tickZbbBridge(level, target);
        }

        if (this.tickZbbDetourAroundUnbreakable(level, target)) {
            return true;
        }

        if (this.tickZbbBuildBypassOverUnbreakable(level, target, direction)) {
            return true;
        }

        return this.tickZbbBridge(level, target);
    }

    private boolean tickZbbBuildBypassOverUnbreakable(ServerLevel level, LivingEntity target, Direction direction) {
        BlockPos shooterPos = this.shooter.blockPosition();
        BlockPos frontPos = shooterPos.relative(direction);

        if (this.zbbTryImmediateWallSelfLift(level, shooterPos, direction)) {
            return true;
        }

        TerrainPlan stepPlan = this.tryPlanForwardStep(level, target);
        if (stepPlan != null) {
            return this.executeTerrainPlan(level, target, false, this.shooter.distanceToSqr(target), stepPlan);
        }

        if (this.zbbCanBuildBypassAt(level, frontPos.below(), TerrainAction.PLACE_BRIDGE, target)
            && this.canMoveIntoSupportSpace(level, frontPos)
            && level.getBlockState(frontPos.below().below()).isFaceSturdy(level, frontPos.below().below(), Direction.UP)) {
            return this.zbbTryBuildBypass(level, frontPos.below(), TerrainAction.PLACE_BRIDGE, target);
        }

        BlockPos fallbackSupportPos = shooterPos.below();
        if (this.zbbCanBuildBypassAt(level, fallbackSupportPos, TerrainAction.PLACE_BRIDGE, target)
            && this.canOccupyTwoBlockStance(level, shooterPos)
            && level.getBlockState(fallbackSupportPos.below()).isFaceSturdy(level, fallbackSupportPos.below(), Direction.UP)) {
            return this.zbbTryBuildBypass(level, fallbackSupportPos, TerrainAction.PLACE_BRIDGE, target);
        }

        return false;
    }

    private boolean zbbTryImmediateWallSelfLift(ServerLevel level, BlockPos selfLiftPos, Direction direction) {
        BlockPos frontPos = selfLiftPos.relative(direction);
        boolean hardFrontBlocked = this.isUnbreakableObstacle(level, frontPos, Config.gunnerTerrainBreakMaxTier())
            || this.isUnbreakableObstacle(level, frontPos.above(), Config.gunnerTerrainBreakMaxTier());
        if (!hardFrontBlocked || !this.canForceWallSelfLift(level, selfLiftPos)) {
            return false;
        }
        if (!this.placeSupportBlock(level, selfLiftPos, PlacementIntent.REACTIVE)) {
            return false;
        }
        this.zbbBuildCooldownUntil = level.getGameTime() + ZBB_BUILD_COOLDOWN_TICKS;
        this.zbbProtectedBuiltBlocks.put(selfLiftPos.immutable(), level.getGameTime() + ZBB_BUILD_PROTECTION_TICKS);
        return true;
    }

    private boolean canForceWallSelfLift(ServerLevel level, BlockPos selfLiftPos) {
        return this.zbbCanBuild(level, selfLiftPos)
            && level.getBlockState(selfLiftPos.below()).isFaceSturdy(level, selfLiftPos.below(), Direction.UP)
            && this.hasSelfLiftHeadroom(level, selfLiftPos);
    }

    private boolean shouldForceWallLift(ServerLevel level, LivingEntity target) {
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return false;
        }
        BlockPos selfLiftPos = this.shooter.blockPosition();
        BlockPos frontPos = selfLiftPos.relative(direction);
        return this.isBreakBuildPathFailure(level, target, this.shooter.distanceToSqr(target))
            && (this.isUnbreakableObstacle(level, frontPos, Config.gunnerTerrainBreakMaxTier())
                || this.isUnbreakableObstacle(level, frontPos.above(), Config.gunnerTerrainBreakMaxTier()))
            && this.canForceWallSelfLift(level, selfLiftPos);
    }

    private boolean tickZbbDetourAroundUnbreakable(ServerLevel level, LivingEntity target) {
        LocalRouteResult route = this.searchLocalRoute(level, target, false);
        if (route == null || route.plans().isEmpty()) {
            return false;
        }

        TerrainPlan first = route.plans().getFirst();
        if (first.action() != TerrainAction.STEP_UP && first.action() != TerrainAction.NONE) {
            return false;
        }

        for (TerrainPlan plan : route.plans()) {
            if (plan.action() == TerrainAction.BREAK_OBSTACLE || plan.action() == TerrainAction.BREAKOUT_BREAK) {
                return false;
            }
            if (plan.action() == TerrainAction.PLACE_BRIDGE || plan.action() == TerrainAction.BREAKOUT_PLACE || plan.action() == TerrainAction.PLACE_SELF_LIFT) {
                return false;
            }
        }

        return this.executeTerrainPlan(level, target, false, this.shooter.distanceToSqr(target), first);
    }

    private boolean tickZbbBridge(ServerLevel level, LivingEntity target) {
        int mobX = Mth.floor(this.shooter.getX());
        int mobY = Mth.floor(this.shooter.getY());
        int mobZ = Mth.floor(this.shooter.getZ());
        int targetY = Mth.floor(target.getY());
        this.updateZbbFrontBlock(target);
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return false;
        }
        if (this.getRouteObstacleType(level, target, false, direction) == RouteObstacleType.UNBREAKABLE) {
            return false;
        }
        this.zbbBridgeBelowMobPos.set(mobX, mobY - 1, mobZ);
        this.zbbBridgeBelowFrontPos.set(this.zbbBridgeFrontBlockPos.getX(), this.zbbBridgeFrontBlockPos.getY() - 1, this.zbbBridgeFrontBlockPos.getZ());
        this.zbbBridgeTwoBelowFrontPos.set(this.zbbBridgeFrontBlockPos.getX(), this.zbbBridgeFrontBlockPos.getY() - 2, this.zbbBridgeFrontBlockPos.getZ());

        boolean acted = false;
        boolean belowUsEmpty = this.getNearestCollidingBlockWithHitbox(level, this.shooter, new Vec3(0.0D, -1.0D, 0.0D)) == null;
        if (belowUsEmpty && targetY >= mobY) {
            if (this.zbbTryRawBuild(level, this.zbbBridgeBelowMobPos)) {
                this.zbbFreeze();
                acted = true;
            }
        }

        boolean frontEmpty = this.isZbbFreePass(level, this.zbbBridgeFrontBlockPos);
        boolean belowFrontEmpty = this.isZbbFreePass(level, this.zbbBridgeBelowFrontPos);
        boolean below2FrontEmpty = this.isZbbFreePass(level, this.zbbBridgeTwoBelowFrontPos);
        if (frontEmpty && belowFrontEmpty && below2FrontEmpty) {
            if (this.zbbTryRawBuild(level, this.zbbBridgeBelowFrontPos)) {
                this.zbbFreeze();
                acted = true;
            } else if (this.zbbTryBreak(level, this.zbbBridgeBelowFrontPos)) {
                this.zbbFreeze();
                acted = true;
            }
        }
        return acted;
    }

    private boolean tickZbbMitigateDangerousBlocks(ServerLevel level) {
        long now = level.getGameTime();
        if (now < this.zbbDangerousCooldownUntil) {
            return false;
        }
        BlockPos dangerous = this.findNearestBlockInHitbox(level, this.shooter, ZBB_DANGEROUS_BLOCKS_SEARCH_RADIUS, state -> this.isZbbDangerous(level, state));
        this.zbbDangerousCooldownUntil = now + ZBB_DANGEROUS_SCAN_COOLDOWN_TICKS;
        if (dangerous == null) {
            return false;
        }
        if (!this.zbbTryRawBuild(level, dangerous)) {
            return this.zbbTryBreak(level, dangerous);
        }
        return true;
    }

    private boolean zbbCanBreak(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return level.isLoaded(pos)
            && this.zbbBreakCooldownUntil <= level.getGameTime()
            && !state.isAir()
            && !this.isProtectedRecentSupport(level, pos)
            && !this.zbbProtectedBuiltBlocks.containsKey(pos)
            && BulletPenetrationHelper.canGunnerBreakBlock(level, state, Config.gunnerTerrainBreakMaxTier());
    }

    private boolean zbbTryBreak(ServerLevel level, BlockPos pos) {
        if (!this.zbbCanBreak(level, pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        int blockHealth = this.zbbGetBlockHealth(level, pos, state);
        int newDamage = this.zbbGetDamageToBlocks(level, pos, state);
        int totalDamage = this.zbbBlockDamage.getOrDefault(pos, 0) + newDamage;
        int progressId = this.zbbBlockProgressIds.computeIfAbsent(pos.immutable(), ignored -> ZBB_NEXT_BLOCK_PROGRESS_ID.getAndIncrement());

        if (totalDamage >= blockHealth) {
            level.destroyBlockProgress(progressId, pos, -1);
            level.destroyBlock(pos, false);
            this.zbbBlockDamage.remove(pos);
            this.zbbBlockProgressIds.remove(pos);
        } else {
            this.zbbBlockDamage.put(pos.immutable(), totalDamage);
            int stage = Math.max(-1, Math.min(9, Mth.floor((float) totalDamage / (float) blockHealth * 10.0F) - 1));
            level.destroyBlockProgress(progressId, pos, stage);
            this.shooter.swing(InteractionHand.MAIN_HAND);
        }

        this.zbbBreakCooldownUntil = level.getGameTime() + ZBB_BREAK_COOLDOWN_TICKS;
        return true;
    }

    private boolean zbbCanBuild(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos)
            && this.zbbBuildCooldownUntil <= level.getGameTime()
            && this.canPlaceSupportBlock(level, pos);
    }

    private boolean zbbCanBuildBypassAt(ServerLevel level, BlockPos supportPos, TerrainAction action, LivingEntity target) {
        return this.zbbCanBuild(level, supportPos)
            && this.isSafeSupportPlacement(level, action, supportPos, target);
    }

    private boolean zbbTryBuildBypass(ServerLevel level, BlockPos pos, TerrainAction action, LivingEntity target) {
        if (!this.zbbCanBuildBypassAt(level, pos, action, target)) {
            return false;
        }
        return this.zbbTryBuild(level, pos);
    }

    private boolean zbbTryRawBuild(ServerLevel level, BlockPos pos) {
        return this.zbbTryBuild(level, pos);
    }

    private boolean zbbTryBuild(ServerLevel level, BlockPos pos) {
        if (!this.zbbCanBuild(level, pos)) {
            return false;
        }
        if (!this.placeSupportBlock(level, pos, PlacementIntent.REACTIVE)) {
            return false;
        }
        this.zbbBuildCooldownUntil = level.getGameTime() + ZBB_BUILD_COOLDOWN_TICKS;
        this.zbbProtectedBuiltBlocks.put(pos.immutable(), level.getGameTime() + ZBB_BUILD_PROTECTION_TICKS);
        return true;
    }

    private void zbbFreeze() {
        int blockX = this.shooter.blockPosition().getX();
        int blockZ = this.shooter.blockPosition().getZ();
        double centerX = blockX + 0.5D;
        double centerZ = blockZ + 0.5D;
        double currentY = this.shooter.getY();
        double currentVelY = this.shooter.getDeltaMovement().y;
        this.shooter.getMoveControl().setWantedPosition(centerX, currentY, centerZ, 0.0D);
        this.shooter.setDeltaMovement(0.0D, currentVelY, 0.0D);
    }

    private int zbbGetBlockHealth(ServerLevel level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) {
            return Integer.MAX_VALUE;
        }
        hardness = Math.min(hardness, 50.0F);
        return Math.max(1, (int) (hardness * 6.0F));
    }

    private int zbbGetDamageToBlocks(ServerLevel level, BlockPos pos, BlockState state) {
        int baseDamage = 3;
        double width = this.shooter.getBbWidth();
        double height = this.shooter.getBbHeight();
        double baseVolume = 0.6D * 0.6D * 1.95D;
        double mobVolume = width * width * height;
        double hitboxMultiplier = mobVolume / baseVolume;
        double finalHitboxMultiplier = 1.0D + (hitboxMultiplier - 1.0D) * 0.5D;
        ItemStack mainHand = this.shooter.getMainHandItem();
        ItemStack offHand = this.shooter.getOffhandItem();
        float destroySpeed = Math.max(mainHand.getDestroySpeed(state), offHand.getDestroySpeed(state));
        float toolMultiplier = Mth.clamp(destroySpeed, 1.0F, 30.0F);
        double finalToolMultiplier = 1.0D + (toolMultiplier - 1.0D) * 0.5D;
        return Math.max(1, (int) Math.round(baseDamage * finalHitboxMultiplier * finalToolMultiplier));
    }

    private void updateZbbFrontBlock(LivingEntity target) {
        AABB box = this.shooter.getBoundingBox();
        double dx = target.getX() - this.shooter.getX();
        double dz = target.getZ() - this.shooter.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-6D) {
            this.zbbBridgeFrontBlockPos.set(this.shooter.getX(), this.shooter.getY(), this.shooter.getZ());
            return;
        }
        dx /= len;
        dz /= len;
        double frontX = dx > 0.0D ? box.maxX : box.minX;
        double frontZ = dz > 0.0D ? box.maxZ : box.minZ;
        frontX += dx;
        frontZ += dz;
        this.zbbBridgeFrontBlockPos.set(Mth.floor(frontX), Mth.floor(box.minY), Mth.floor(frontZ));
    }

    private BlockPos getZbbBlockUnderBottomCenter() {
        AABB box = this.shooter.getBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5D;
        double bottomY = box.minY;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        return new BlockPos(Mth.floor(centerX), Mth.floor(bottomY - 1.0D), Mth.floor(centerZ));
    }

    private boolean isZbbDangerous(ServerLevel level, BlockState state) {
        if (!ZBB_DANGEROUS_BLOCKS.contains(state.getBlock())) {
            return false;
        }
        if (state.getBlock() instanceof CampfireBlock) {
            return state.getValue(CampfireBlock.LIT);
        }
        return true;
    }

    private boolean isZbbFreePass(ServerLevel level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return blockState.isAir() || blockState.getCollisionShape(level, pos).isEmpty();
    }

    private void cleanupZbbStorages(ServerLevel level) {
        long now = level.getGameTime();
        this.zbbProtectedBuiltBlocks.entrySet().removeIf(entry -> entry.getValue() < now);
        this.zbbBlockDamage.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            if (level.getBlockState(pos).isAir()) {
                Integer progressId = this.zbbBlockProgressIds.remove(pos);
                if (progressId != null) {
                    level.destroyBlockProgress(progressId, pos, -1);
                }
                return true;
            }
            return false;
        });
    }

    private BlockPos getNearestCollidingBlockWithHitbox(ServerLevel level, PathfinderMob mob, Vec3 offset) {
        AABB hitbox = mob.getBoundingBox().move(offset);
        return this.findNearestMatchingBlock(level, hitbox, mob.getBoundingBox().getCenter(), state -> !state.isAir(), true);
    }

    private BlockPos findNearestBlockInHitbox(ServerLevel level, PathfinderMob mob, double inflate, java.util.function.Predicate<BlockState> predicate) {
        AABB scanBox = mob.getBoundingBox().inflate(inflate);
        return this.findNearestMatchingBlock(level, scanBox, mob.getBoundingBox().getCenter(), predicate, false);
    }

    private BlockPos findNearestMatchingBlock(ServerLevel level, AABB scanBox, Vec3 origin, java.util.function.Predicate<BlockState> predicate, boolean requireCollision) {
        VoxelShape hitboxShape = Shapes.create(scanBox);
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;
        int minX = Mth.floor(scanBox.minX);
        int minY = Mth.floor(scanBox.minY);
        int minZ = Mth.floor(scanBox.minZ);
        int maxX = Mth.floor(scanBox.maxX - 1.0E-7D);
        int maxY = Mth.floor(scanBox.maxY - 1.0E-7D);
        int maxZ = Mth.floor(scanBox.maxZ - 1.0E-7D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!predicate.test(state)) {
                        continue;
                    }
                    if (requireCollision) {
                        VoxelShape collisionShape = state.getCollisionShape(level, pos);
                        if (collisionShape.isEmpty()) {
                            continue;
                        }
                        if (!collisionShape.bounds().move(pos.getX(), pos.getY(), pos.getZ()).intersects(scanBox)) {
                            continue;
                        }
                        if (!Shapes.joinIsNotEmpty(collisionShape.move(pos.getX(), pos.getY(), pos.getZ()), hitboxShape, BooleanOp.AND)) {
                            continue;
                        }
                    }
                    double dx = (x + 0.5D) - origin.x;
                    double dy = (y + 0.5D) - origin.y;
                    double dz = (z + 0.5D) - origin.z;
                    double dist = dx * dx + dy * dy + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        nearest = pos.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private void updateAttackMode(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        if (this.breakBuildLockTicks > 0) {
            --this.breakBuildLockTicks;
        }
        if (this.supportFreezeTicks > 0) {
            --this.supportFreezeTicks;
        }
        if (this.terrainReplanCooldown > 0) {
            --this.terrainReplanCooldown;
        }

        boolean shouldBreakBuild = this.shouldEnterBreakBuildMode(level, target, canSeeTarget, distanceToTarget)
            || this.shouldStayInBreakBuildMode(level, target, canSeeTarget, distanceToTarget);

        if (shouldBreakBuild) {
            if (this.attackMode != AttackMode.BREAK_BUILD) {
                this.breakBuildLockTicks = TERRAIN_ROUTE_COMMIT_TICKS;
            }
            this.attackMode = AttackMode.BREAK_BUILD;
            this.stableNormalPathTicks = 0;
            return;
        }

        if (this.hasWalkablePathToTarget(target, distanceToTarget)) {
            this.stableNormalPathTicks++;
        } else {
            this.stableNormalPathTicks = 0;
        }

        if (this.stableNormalPathTicks >= TERRAIN_PROGRESS_SAMPLE_TICKS * 2 || distanceToTarget <= this.attackRadiusSqr) {
            this.attackMode = AttackMode.NORMAL;
        }
    }

    private boolean shouldEnterBreakBuildMode(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        Path path = this.shooter.getNavigation().createPath(target, 0);
        return this.terrainBreakTarget != null
            || !this.terrainPlannedActions.isEmpty()
            || this.liftExecutionState != LiftExecutionState.IDLE
            || this.terrainStuckReason != StuckReason.NONE
            || path == null
            || this.isPartialPathExhausted(level, path, target, distanceToTarget)
            || this.terrainNoProgressTicks >= TERRAIN_NO_PROGRESS_THRESHOLD_TICKS;
    }

    private boolean shouldStayInBreakBuildMode(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        if (this.attackMode != AttackMode.BREAK_BUILD) {
            return false;
        }
        if (this.breakBuildLockTicks > 0 || this.supportFreezeTicks > 0) {
            return true;
        }
        if (this.terrainBreakTarget != null || !this.terrainPlannedActions.isEmpty() || this.liftExecutionState != LiftExecutionState.IDLE) {
            return true;
        }
        if (this.isTerrainActionActive()) {
            return true;
        }
        return !canSeeTarget && this.targetPersistenceTicks > 0 && this.isPathMeaningfullyBlocked(level, target, distanceToTarget);
    }

    private boolean isTerrainActionActive() {
        return this.terrainBreakTarget != null
            || !this.terrainPlannedActions.isEmpty()
            || this.liftExecutionState != LiftExecutionState.IDLE;
    }

    private boolean isPathMeaningfullyBlocked(ServerLevel level, LivingEntity target, double distanceToTarget) {
        Path path = this.shooter.getNavigation().createPath(target, 0);
        return path == null
            || path.isDone()
            || this.terrainStuckReason != StuckReason.NONE
            || this.isPartialPathExhausted(level, path, target, distanceToTarget);
    }

    private boolean shouldForceCloseDistanceToUnseenTarget(LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        return target != null
            && !canSeeTarget
            && distanceToTarget > this.attackRadiusSqr
            && !this.isTerrainActionActive();
    }

    private void moveTowardTargetEntity(LivingEntity target) {
        this.shooter.getNavigation().moveTo(target, this.speedModifier);
        this.shooter.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), this.speedModifier);
    }

    private boolean canOccupyTwoBlockStance(ServerLevel level, BlockPos stancePos) {
        for (int i = 0; i < TERRAIN_STANCE_HEADROOM_REQUIRED; i++) {
            if (!level.getBlockState(stancePos.above(i)).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    private boolean isSafeSupportPlacement(ServerLevel level, TerrainAction action, BlockPos placePos, LivingEntity target) {
        if (placePos == null || !this.canPlaceSupportBlock(level, placePos)) {
            return false;
        }

        BlockPos resultingStance = switch (action) {
            case STEP_UP, PLACE_BRIDGE, BREAKOUT_PLACE, PLACE_SELF_LIFT -> placePos.above();
            default -> null;
        };
        if (resultingStance == null) {
            return false;
        }
        if (!this.canOccupyTwoBlockStance(level, resultingStance)) {
            return false;
        }
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return false;
        }
        return target == null || this.canContinueFromSimulatedStance(level, resultingStance, target);
    }

    private boolean shouldUseOrderedTactics(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        return this.isTerrainActionActive() || this.isPathMeaningfullyBlocked(level, target, distanceToTarget);
    }

    private boolean runOrderedTactics(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        return this.tickBreakBuildFallback(level, target, canSeeTarget, distanceToTarget);
    }

    private boolean shouldRunPersistentBreakPhase(ServerLevel level, double distanceToTarget) {
        return this.shouldKeepBreakingCurrentTarget(distanceToTarget) && this.shouldKeepPersistentBreakTarget(level);
    }

    private boolean runPersistentBreakPhase(ServerLevel level, double distanceToTarget) {
        return this.tickPersistentBreak(level, distanceToTarget);
    }

    private void followTerrainPressure(LivingEntity target) {
        Vec3 destination = target != null ? target.position() : this.lastKnownPosition;
        if (destination == null) {
            return;
        }
        this.shooter.getNavigation().moveTo(destination.x, destination.y, destination.z, this.speedModifier);
        this.shooter.getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, this.speedModifier);
    }

    private boolean tickBreakBuildFallback(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        if (this.shouldBiasLiftFirst(target)) {
            TerrainPlan liftPlan = this.tryPlanSelfLift(level, target);
            if (liftPlan != null && this.executeTerrainPlan(level, target, canSeeTarget, distanceToTarget, liftPlan)) {
                return true;
            }
        }

        TerrainPlan obstaclePlan = this.tryPlanBreakObstacle(level, target, canSeeTarget);
        if (obstaclePlan != null && this.executeTerrainPlan(level, target, canSeeTarget, distanceToTarget, obstaclePlan)) {
            return true;
        }

        TerrainPlan supportPlan = this.tryPlanBridge(level, target, PlacementIntent.REACTIVE);
        if (supportPlan != null && this.executeTerrainPlan(level, target, canSeeTarget, distanceToTarget, supportPlan)) {
            return true;
        }

        TerrainPlan stepPlan = this.tryPlanForwardStep(level, target);
        if (stepPlan != null && this.executeTerrainPlan(level, target, canSeeTarget, distanceToTarget, stepPlan)) {
            return true;
        }

        if (!this.shouldBiasLiftFirst(target)) {
            TerrainPlan liftPlan = this.tryPlanSelfLift(level, target);
            if (liftPlan != null && this.executeTerrainPlan(level, target, canSeeTarget, distanceToTarget, liftPlan)) {
                return true;
            }
        }

        if (this.terrainStuckReason != StuckReason.NONE) {
            TerrainPlan breakoutPlan = this.createBreakoutPlan(level, target, canSeeTarget, distanceToTarget);
            if (breakoutPlan != null && this.executeTerrainPlan(level, target, canSeeTarget, distanceToTarget, breakoutPlan)) {
                return true;
            }
        }

        this.followTerrainPressure(target);
        return true;
    }

    private boolean shouldKeepBreakingCurrentTarget(double distanceToTarget) {
        return this.terrainBreakTarget != null
            && this.attackMode == AttackMode.BREAK_BUILD
            && distanceToTarget > this.attackRadiusSqr;
    }

    private boolean shouldKeepPersistentBreakTarget(ServerLevel level) {
        return this.terrainBreakTarget != null
            && this.isBreakableObstacle(level, this.terrainBreakTarget, Config.gunnerTerrainBreakMaxTier())
            && this.attackMode == AttackMode.BREAK_BUILD;
    }

    private void maybeResetTerrainBreakTarget(ServerLevel level) {
        if (!this.shouldKeepPersistentBreakTarget(level)) {
            this.resetTerrainBreakTarget();
        }
    }

    private void enterBreakBuildLock() {
        this.breakBuildLockTicks = Math.max(this.breakBuildLockTicks, TERRAIN_ROUTE_COMMIT_TICKS);
    }

    private void markSupportFollowThrough() {
        this.supportFreezeTicks = Math.max(this.supportFreezeTicks, 2);
        this.enterBreakBuildLock();
    }

    private void markActionPersistence() {
        this.enterBreakBuildLock();
        this.terrainReplanCooldown = 0;
    }

    private boolean shouldEscalateBreakBuild(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        return this.terrainStuckReason != StuckReason.NONE
            || this.terrainNoProgressTicks >= TERRAIN_NO_PROGRESS_THRESHOLD_TICKS
            || this.isPartialPathExhausted(level, this.shooter.getNavigation().createPath(target, 0), target, distanceToTarget);
    }

    private boolean isBreakBuildPathFailure(ServerLevel level, LivingEntity target, double distanceToTarget) {
        Path path = this.shooter.getNavigation().createPath(target, 0);
        return path == null || path.isDone() || this.isPartialPathExhausted(level, path, target, distanceToTarget);
    }

    private boolean shouldTreatAsBreakBuildHighPriority(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        return this.liftExecutionState != LiftExecutionState.IDLE
            || this.terrainBreakTarget != null
            || !this.terrainPlannedActions.isEmpty()
            || this.shouldEscalateBreakBuild(level, target, canSeeTarget, distanceToTarget)
            || this.isBreakBuildPathFailure(level, target, distanceToTarget);
    }

    private boolean shouldBiasLiftFirst(LivingEntity target) {
        if (this.liftExecutionState != LiftExecutionState.IDLE || this.terrainPathState == TerrainPathState.STEP_UP) {
            return true;
        }
        if (!(this.shooter.level() instanceof ServerLevel level)) {
            return false;
        }
        if (target.getY() <= this.shooter.getY() + PROACTIVE_TERRAIN_HEIGHT_ADVANTAGE) {
            return false;
        }
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return false;
        }
        return this.shouldUseLiftOverWall(level, this.shooter.blockPosition(), direction);
    }

    private boolean shouldUseLiftOverWall(ServerLevel level, BlockPos stancePos, Direction direction) {
        BlockPos frontPos = stancePos.relative(direction);
        if (!this.isUnbreakableObstacle(level, frontPos, Config.gunnerTerrainBreakMaxTier())
            || !this.isUnbreakableObstacle(level, frontPos.above(), Config.gunnerTerrainBreakMaxTier())) {
            return false;
        }
        if (!this.canPlaceSelfLift(level, stancePos)) {
            return false;
        }

        int wallHeight = 2;
        while (this.isUnbreakableObstacle(level, frontPos.above(wallHeight), Config.gunnerTerrainBreakMaxTier())) {
            wallHeight++;
        }
        for (int i = 0; i < wallHeight + MIN_TUNNEL_HEADROOM_ABOVE_WALL; i++) {
            if (!level.getBlockState(stancePos.above(1 + i)).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldUseTunnelBypassUnderWall(ServerLevel level, BlockPos stancePos, Direction direction, LivingEntity target) {
        BlockPos frontPos = stancePos.relative(direction);
        if (!this.isUnbreakableObstacle(level, frontPos, Config.gunnerTerrainBreakMaxTier())
            || !this.isUnbreakableObstacle(level, frontPos.above(), Config.gunnerTerrainBreakMaxTier())) {
            return false;
        }

        BlockPos lowerPos = stancePos.below();
        BlockPos lowerFrontPos = frontPos.below();
        if (!this.isWithinLocalRouteBounds(lowerPos) || !this.isWithinLocalRouteBounds(lowerFrontPos)) {
            return false;
        }
        if (!this.isStandableLocalRoutePosition(level, lowerPos) || !this.isStandableLocalRoutePosition(level, lowerFrontPos)) {
            return false;
        }
        return this.canContinueFromSimulatedStance(level, lowerFrontPos, target);
    }

    private boolean tickPersistentBreak(ServerLevel level, double distanceToTarget) {
        if (!this.shouldKeepBreakingCurrentTarget(distanceToTarget)) {
            this.maybeResetTerrainBreakTarget(level);
            return false;
        }
        this.terrainLastAction = this.terrainRoutePhase == TerrainRoutePhase.BREAKOUT ? TerrainAction.BREAKOUT_BREAK : TerrainAction.BREAK_OBSTACLE;
        this.markActionPersistence();
        return this.continueBreakingBlock(level);
    }

    private void onTerrainPlanMiss(ServerLevel level) {
        if (!this.shouldKeepPersistentBreakTarget(level)) {
            this.terrainReplanCooldown = TERRAIN_PROGRESS_SAMPLE_TICKS;
        }
    }

    private boolean canBreakBuildSuppressNormalPathing(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        return this.attackMode == AttackMode.BREAK_BUILD && this.shouldTreatAsBreakBuildHighPriority(level, target, canSeeTarget, distanceToTarget);
    }

    private void onTerrainSupportPlaced() {
        this.markSupportFollowThrough();
    }

    private void onTerrainBreakStarted() {
        this.markActionPersistence();
    }

    private void onTerrainBreakContinued() {
        this.markActionPersistence();
    }

    private boolean shouldPreserveTerrainBreakTarget(ServerLevel level) {
        return this.shouldKeepPersistentBreakTarget(level);
    }

    private boolean maybeContinuePersistentBreak(ServerLevel level, double distanceToTarget) {
        return this.tickPersistentBreak(level, distanceToTarget);
    }

    private void markBreakBuildPlanFailure(ServerLevel level) {
        this.onTerrainPlanMiss(level);
    }

    private boolean shouldRunBreakBuildFallback(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        return this.attackMode == AttackMode.BREAK_BUILD && this.shouldEscalateBreakBuild(level, target, canSeeTarget, distanceToTarget);
    }

    private boolean keepTryingBreakBuild(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        return this.tickBreakBuildFallback(level, target, canSeeTarget, distanceToTarget);
    }

    private void onQueuedRouteFailure(ServerLevel level) {
        this.onTerrainPlanMiss(level);
    }

    private void updateTerrainContext(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        this.purgeExpiredProtectedSupports(level);

        if (this.terrainPathRecheckCooldown > 0) {
            --this.terrainPathRecheckCooldown;
        }
        if (this.terrainPlaceCooldown > 0) {
            --this.terrainPlaceCooldown;
        }
        if (this.terrainBreakoutCooldown > 0) {
            --this.terrainBreakoutCooldown;
        }
        if (this.terrainSampleCooldown > 0) {
            --this.terrainSampleCooldown;
        }
        if (this.terrainRouteCommitTicks > 0) {
            --this.terrainRouteCommitTicks;
        }
        if (this.failedRouteRetryCooldown > 0) {
            --this.failedRouteRetryCooldown;
        }
        if (this.terrainEditWindowTicks > 0) {
            --this.terrainEditWindowTicks;
        } else {
            this.terrainEditWindowTicks = 0;
            this.terrainEditsInWindow = 0;
        }

        if (this.terrainPathRecheckCooldown <= 0) {
            Path path = this.shooter.getNavigation().createPath(target, 0);
            this.terrainPathAvailable = path != null;
            this.terrainPathState = this.assessTerrainPathState(level, target, canSeeTarget, distanceToTarget, path);
            this.terrainPathRecheckCooldown = this.isCommittedToTerrainRoute() ? TERRAIN_PATH_RECHECK_TICKS * 2 : TERRAIN_PATH_RECHECK_TICKS;
        }

        this.updateTerrainProgress(target, distanceToTarget);
        this.updateTerrainRouteStallState();
        this.terrainStuckReason = this.determineStuckReason(level, target, distanceToTarget);
    }

    private void updateTerrainRouteStallState() {
        if (this.terrainPlannedActions.isEmpty()) {
            this.terrainRouteStallTicks = 0;
            return;
        }
        if (this.terrainNoProgressTicks >= TERRAIN_PROGRESS_SAMPLE_TICKS) {
            this.terrainRouteStallTicks += TERRAIN_PROGRESS_SAMPLE_TICKS;
        } else {
            this.terrainRouteStallTicks = 0;
        }
    }

    private boolean hasWalkablePathToTarget(LivingEntity target, double distanceToTarget) {
        Path path = this.shooter.getNavigation().createPath(target, 0);
        if (path == null || path.isDone()) {
            return false;
        }
        if (this.isCommittedToTerrainRoute()) {
            return false;
        }
        if (!this.isPathMeaningfullyAdvancing(this.requireServerLevel(), path, target, distanceToTarget)) {
            return false;
        }
        if (distanceToTarget <= this.attackRadiusSqr) {
            return true;
        }
        return this.terrainNoProgressTicks < TERRAIN_NO_PROGRESS_THRESHOLD_TICKS;
    }

    private boolean isCommittedToTerrainRoute() {
        return this.terrainRouteCommitTicks > 0 && this.terrainCommittedStepsRemaining > 0;
    }

    private boolean executeQueuedTerrainPlan(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        if (this.terrainPlannedActions.isEmpty()) {
            return false;
        }

        if (!this.isCommittedToTerrainRoute() && this.attackMode == AttackMode.NORMAL && this.hasWalkablePathToTarget(target, distanceToTarget)) {
            this.clearTerrainRoute();
            return false;
        }

        TerrainPlan next = this.terrainPlannedActions.peekFirst();
        if (next == null) {
            this.clearTerrainRoute();
            return false;
        }

        if (!this.isTerrainPlanStillValid(level, target, next) || this.shouldForceRouteReplan(level, target, next)) {
            this.handleQueuedRouteFailure(next);
            return false;
        }

        boolean executed = this.executeTerrainPlan(level, target, canSeeTarget, distanceToTarget, next);
        if (!executed) {
            this.handleQueuedRouteFailure(next);
            return false;
        }

        if (next.action() != TerrainAction.BREAK_OBSTACLE && next.action() != TerrainAction.BREAKOUT_BREAK) {
            this.terrainPlannedActions.pollFirst();
            if (this.terrainCommittedStepsRemaining > 0) {
                this.terrainCommittedStepsRemaining--;
            }
            this.terrainRouteStallTicks = 0;
            this.advanceTerrainRoutePhase();
        }
        return true;
    }

    private void handleQueuedRouteFailure(TerrainPlan failedPlan) {
        this.markRouteFailure(failedPlan);
        if (!this.terrainPlannedActions.isEmpty()) {
            this.terrainPlannedActions.pollFirst();
        }
        this.terrainReplanCooldown = TERRAIN_PROGRESS_SAMPLE_TICKS;
        this.terrainRouteCommitTicks = 0;
        this.terrainCommittedStepsRemaining = 0;
        if (this.terrainPlannedActions.isEmpty()) {
            this.terrainRoutePhase = TerrainRoutePhase.NONE;
        } else {
            this.advanceTerrainRoutePhase();
        }
        this.onQueuedRouteFailure(this.requireServerLevel());
    }

    private void clearTerrainRoute() {
        this.terrainPlannedActions.clear();
        this.terrainRoutePhase = TerrainRoutePhase.NONE;
        this.terrainRouteCommitTicks = 0;
        this.terrainCommittedStepsRemaining = 0;
    }

    private void advanceTerrainRoutePhase() {
        if (!this.terrainPlannedActions.isEmpty()) {
            TerrainPlan next = this.terrainPlannedActions.peekFirst();
            this.terrainRoutePhase = switch (next.action()) {
                case PLACE_SELF_LIFT -> TerrainRoutePhase.VERTICAL_ASCENT;
                case PLACE_BRIDGE -> TerrainRoutePhase.FORWARD_BRIDGE;
                default -> this.terrainRoutePhase;
            };
        } else if (this.terrainRoutePhase != TerrainRoutePhase.NONE) {
            this.terrainRoutePhase = TerrainRoutePhase.FINAL_REJOIN;
        }
    }

    private boolean shouldForceRouteReplan(ServerLevel level, LivingEntity target, TerrainPlan next) {
        if (this.terrainRouteStallTicks >= TERRAIN_BREAKOUT_THRESHOLD_TICKS) {
            return true;
        }
        if (this.terrainNoProgressTicks >= TERRAIN_NO_PROGRESS_THRESHOLD_TICKS && this.isCommittedToTerrainRoute()) {
            return true;
        }
        return switch (next.action()) {
            case PLACE_SELF_LIFT -> next.placePos() == null
                || !this.hasSelfLiftHeadroom(level, next.placePos())
                || !this.canContinueFromSimulatedStance(level, next.placePos().above(), target)
                || this.isLiftExecutionOutOfSync(level, next.placePos(), target);
            case PLACE_BRIDGE -> next.placePos() == null
                || !this.canContinueFromSimulatedStance(level, next.placePos().above(), target)
                || this.isBridgeExecutionOutOfSync(level, next.placePos(), target);
            case STEP_UP -> next.placePos() == null
                || !this.canContinueFromSimulatedStance(level, next.placePos(), target)
                || this.isStepExecutionOutOfSync(level, next.placePos(), target);
            default -> false;
        };
    }

    private boolean isLiftExecutionOutOfSync(ServerLevel level, BlockPos placePos, LivingEntity target) {
        if (this.liftExecutionState == LiftExecutionState.IDLE) {
            return false;
        }
        if (this.liftAnchorPos == null || !this.liftAnchorPos.equals(placePos)) {
            return true;
        }
        if (!this.hasSelfLiftHeadroom(level, placePos) && this.liftExecutionState != LiftExecutionState.PLACING && this.liftExecutionState != LiftExecutionState.VERIFY) {
            return true;
        }
        return this.liftExecutionTicks > 8 && !this.canContinueFromSimulatedStance(level, placePos.above(), target);
    }

    private boolean isBridgeExecutionOutOfSync(ServerLevel level, BlockPos placePos, LivingEntity target) {
        BlockPos stancePos = placePos.above();
        if (!this.canPlaceSupportBlock(level, placePos) && !this.isPlacedSupportBlock(level, placePos)) {
            return true;
        }
        if (this.terrainRoutePhase == TerrainRoutePhase.FORWARD_BRIDGE && this.terrainNoProgressTicks >= TERRAIN_PROGRESS_SAMPLE_TICKS) {
            return !this.canContinueFromSimulatedStance(level, stancePos, target);
        }
        return false;
    }

    private boolean isStepExecutionOutOfSync(ServerLevel level, BlockPos placePos, LivingEntity target) {
        if (this.terrainRoutePhase != TerrainRoutePhase.NONE && this.terrainNoProgressTicks >= TERRAIN_PROGRESS_SAMPLE_TICKS) {
            return !this.canContinueFromSimulatedStance(level, placePos, target);
        }
        return false;
    }

    private boolean isPlacedSupportBlock(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Config.gunnerTerrainSupportBlockState().getBlock());
    }

    private void purgeExpiredProtectedSupports(ServerLevel level) {
        if (this.lastPlacedSupportPos == null) {
            return;
        }
        if (level.getGameTime() - this.lastPlacedSupportGameTime > SUPPORT_BREAK_PROTECTION_TICKS) {
            this.lastPlacedSupportPos = null;
            this.lastPlacedSupportGameTime = Long.MIN_VALUE;
        }
    }

    private boolean isProtectedRecentSupport(ServerLevel level, BlockPos pos) {
        if (this.lastPlacedSupportPos == null || !this.lastPlacedSupportPos.equals(pos)) {
            return false;
        }
        if (!this.isPlacedSupportBlock(level, pos)) {
            return false;
        }
        return level.getGameTime() - this.lastPlacedSupportGameTime <= SUPPORT_BREAK_PROTECTION_TICKS;
    }

    private void recordPlacedSupport(ServerLevel level, BlockPos pos) {
        this.lastPlacedSupportPos = pos.immutable();
        this.lastPlacedSupportGameTime = level.getGameTime();
        this.supportFreezeTicks = Math.max(this.supportFreezeTicks, 6);
    }

    private void markRouteFailure(TerrainPlan failedPlan) {
        if (failedPlan == null) {
            return;
        }
        this.lastFailedRouteSignature = this.getPlanSignature(failedPlan);
        this.failedRouteRetryCooldown = TERRAIN_PATH_RECHECK_TICKS * 2;
    }

    private String getPlanSignature(TerrainPlan plan) {
        return plan.action() + ":" + plan.placePos() + ":" + plan.breakPos();
    }

    private boolean isTerrainPlanStillValid(ServerLevel level, LivingEntity target, TerrainPlan plan) {
        return switch (plan.action()) {
            case STEP_UP -> plan.placePos() != null && this.canPlaceForwardStep(level, plan.placePos());
            case PLACE_BRIDGE, BREAKOUT_PLACE -> plan.placePos() != null && this.canPlaceSupportBlock(level, plan.placePos());
            case PLACE_SELF_LIFT -> plan.placePos() != null && this.canPlaceSelfLift(level, plan.placePos());
            case BREAK_OBSTACLE, BREAKOUT_BREAK -> plan.breakPos() != null && this.isBreakableObstacle(level, plan.breakPos(), Config.gunnerTerrainBreakMaxTier());
            default -> false;
        };
    }

    private void updateTerrainProgress(LivingEntity target, double distanceToTarget) {
        Vec3 currentPos = this.shooter.position();
        if (this.terrainLastSamplePos == null) {
            this.terrainLastSamplePos = currentPos;
            this.terrainLastSampleDistanceToTarget = distanceToTarget;
            this.terrainSampleCooldown = TERRAIN_PROGRESS_SAMPLE_TICKS;
            return;
        }

        if (this.terrainSampleCooldown > 0) {
            return;
        }

        double movedDistanceSq = this.terrainLastSamplePos.distanceToSqr(currentPos);
        double distanceImprovement = this.terrainLastSampleDistanceToTarget - distanceToTarget;
        boolean madeProgress = movedDistanceSq >= TERRAIN_PROGRESS_MOVEMENT_THRESHOLD_SQR || distanceImprovement >= TERRAIN_PROGRESS_DISTANCE_DELTA;
        if (madeProgress) {
            this.terrainNoProgressTicks = 0;
            this.terrainConsecutiveActionsWithoutProgress = 0;
        } else {
            this.terrainNoProgressTicks += TERRAIN_PROGRESS_SAMPLE_TICKS;
        }

        this.terrainLastSamplePos = currentPos;
        this.terrainLastSampleDistanceToTarget = distanceToTarget;
        this.terrainSampleCooldown = TERRAIN_PROGRESS_SAMPLE_TICKS;
    }

    private boolean isPathMeaningfullyAdvancing(ServerLevel level, Path path, LivingEntity target, double distanceToTarget) {
        if (path == null || path.isDone()) {
            return false;
        }
        if (this.terrainNoProgressTicks >= TERRAIN_NO_PROGRESS_THRESHOLD_TICKS || this.terrainRouteStallTicks >= TERRAIN_NO_PROGRESS_THRESHOLD_TICKS) {
            return false;
        }
        if (distanceToTarget <= this.attackRadiusSqr) {
            return true;
        }
        if (path.canReach()) {
            return true;
        }
        return !this.isPartialPathExhausted(level, path, target, distanceToTarget);
    }

    private boolean isPartialPathExhausted(ServerLevel level, Path path, LivingEntity target, double distanceToTarget) {
        if (path == null) {
            return false;
        }

        Node endNode = path.getEndNode();
        if (endNode == null) {
            return path.isDone() && distanceToTarget > this.attackRadiusSqr;
        }

        double endX = endNode.x + 0.5D;
        double endY = endNode.y;
        double endZ = endNode.z + 0.5D;
        double shooterToEndDistanceSq = this.shooter.distanceToSqr(endX, endY, endZ);
        double targetToEndDistanceSq = target.distanceToSqr(endX, endY, endZ);
        boolean reachedPathEnd = shooterToEndDistanceSq <= Math.max(4.0D, TERRAIN_PROGRESS_DISTANCE_DELTA * TERRAIN_PROGRESS_DISTANCE_DELTA * 4.0D);
        boolean targetStillFarFromEnd = targetToEndDistanceSq > Math.max(9.0D, this.attackRadiusSqr);
        if (!reachedPathEnd || !targetStillFarFromEnd) {
            return false;
        }

        return !this.canContinueFromSimulatedStance(level, BlockPos.containing(endX, endY, endZ), target);
    }

    private TerrainPathState assessTerrainPathState(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget, Path path) {
        if (path != null && !path.isDone() && this.isPathMeaningfullyAdvancing(level, path, target, distanceToTarget)) {
            return TerrainPathState.PATHABLE;
        }

        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return TerrainPathState.UNKNOWN;
        }

        RouteObstacleType routeObstacle = this.getRouteObstacleType(level, target, canSeeTarget, direction);
        if (routeObstacle == RouteObstacleType.BREAKABLE) {
            return TerrainPathState.LOCAL_OBSTACLE;
        }
        if (routeObstacle == RouteObstacleType.UNBREAKABLE) {
            return TerrainPathState.UNBREAKABLE_OBSTACLE;
        }

        BlockPos shooterPos = this.shooter.blockPosition();
        BlockPos frontPos = shooterPos.relative(direction);
        boolean targetIsHigher = target.getY() > this.shooter.getY() + PROACTIVE_TERRAIN_HEIGHT_ADVANTAGE;

        if (targetIsHigher && this.canPlaceForwardStep(level, frontPos)) {
            return TerrainPathState.STEP_UP;
        }

        if (this.canPlaceProactiveBridge(level, shooterPos, direction, target)) {
            return TerrainPathState.GAP;
        }

        if (distanceToTarget > this.attackRadiusSqr && this.canPlaceForwardBridgeFromStance(level, shooterPos, direction)) {
            return TerrainPathState.GAP;
        }

        return (this.terrainNoProgressTicks >= TERRAIN_NO_PROGRESS_THRESHOLD_TICKS || this.isPartialPathExhausted(level, path, target, distanceToTarget))
            ? TerrainPathState.PATH_NOT_PROGRESSING
            : TerrainPathState.UNKNOWN;
    }

    private RouteObstacleType getRouteObstacleType(ServerLevel level, LivingEntity target, boolean canSeeTarget, Direction direction) {
        return this.getRouteObstacleTypeFromStance(level, this.shooter.blockPosition(), target, canSeeTarget, direction);
    }

    private BlockPos[] getImmediateObstacleCandidates(BlockPos stancePos, Direction direction) {
        BlockPos frontPos = stancePos.relative(direction);
        BlockPos secondFrontPos = frontPos.relative(direction);
        return new BlockPos[] {
            frontPos,
            frontPos.above(),
            frontPos.above(2),
            secondFrontPos,
            secondFrontPos.above()
        };
    }

    private StuckReason determineStuckReason(ServerLevel level, LivingEntity target, double distanceToTarget) {
        if (this.shooter.isInWater()) {
            return this.terrainNoProgressTicks >= TERRAIN_NO_PROGRESS_THRESHOLD_TICKS ? StuckReason.WATER_FLOW : StuckReason.NONE;
        }
        boolean pathExhausted = this.isPartialPathExhausted(level, this.shooter.getNavigation().createPath(target, 0), target, distanceToTarget);
        if (this.terrainNoProgressTicks < TERRAIN_NO_PROGRESS_THRESHOLD_TICKS && !pathExhausted && this.terrainPathState != TerrainPathState.UNBREAKABLE_OBSTACLE) {
            return StuckReason.NONE;
        }
        return switch (this.terrainPathState) {
            case LOCAL_OBSTACLE -> StuckReason.BLOCKED_FRONT;
            case UNBREAKABLE_OBSTACLE, GAP -> StuckReason.GAP_EDGE;
            case STEP_UP -> StuckReason.CLIFF_FACE;
            case PATH_NOT_PROGRESSING -> pathExhausted ? StuckReason.GAP_EDGE : StuckReason.PATH_NOT_PROGRESSING;
            default -> distanceToTarget > this.attackRadiusSqr ? StuckReason.IMMOBILIZED : StuckReason.NONE;
        };
    }

    private boolean shouldPreferNormalPathing(double distanceToTarget) {
        if (distanceToTarget <= Math.max(this.attackRadiusSqr + DIRECT_PATH_REACH_MARGIN, TERRAIN_MIN_DISTANCE_SQR)) {
            return true;
        }
        if (this.terrainBreakoutCooldown > 0 && this.terrainStuckReason == StuckReason.NONE) {
            return true;
        }
        return this.terrainPathState == TerrainPathState.PATHABLE && this.terrainNoProgressTicks < TERRAIN_NO_PROGRESS_THRESHOLD_TICKS;
    }

    private TerrainPlan evaluateTerrainPlan(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        if (this.shouldUseBreakoutMode()) {
            this.clearTerrainRoute();
            TerrainPlan breakoutPlan = this.createBreakoutPlan(level, target, canSeeTarget, distanceToTarget);
            if (breakoutPlan != null) {
                return breakoutPlan;
            }
        }

        TerrainPlan sharedRoutePlan = null;
        if (sharedRoutePlan != null) {
            return sharedRoutePlan;
        }

        if (this.terrainPathState == TerrainPathState.UNBREAKABLE_OBSTACLE) {
            TerrainPlan supportPlan = this.tryPlanUnbreakableSupportRoute(level, target, canSeeTarget, distanceToTarget);
            if (supportPlan != null) {
                return supportPlan;
            }
        }

        TerrainPlan routePlan = this.tryPlanLocalRoute(level, target, canSeeTarget, distanceToTarget);
        if (routePlan != null) {
            return routePlan;
        }

        if (this.terrainPathState == TerrainPathState.STEP_UP) {
            TerrainPlan stepPlan = this.tryPlanForwardStep(level, target);
            if (stepPlan != null) {
                return stepPlan;
            }
        }

        if (this.terrainPathState == TerrainPathState.GAP) {
            TerrainPlan bridgePlan = this.tryPlanBridge(level, target, this.terrainNoProgressTicks >= TERRAIN_NO_PROGRESS_THRESHOLD_TICKS ? PlacementIntent.REACTIVE : PlacementIntent.PROACTIVE);
            if (bridgePlan != null) {
                return bridgePlan;
            }
        }

        if (this.terrainPathState == TerrainPathState.LOCAL_OBSTACLE || (!canSeeTarget && this.isPathMeaningfullyBlocked(level, target, distanceToTarget))) {
            TerrainPlan breakPlan = this.tryPlanBreakObstacle(level, target, canSeeTarget);
            if (breakPlan != null) {
                return breakPlan;
            }
        }

        return null;
    }

    private TerrainPlan tryPlanSharedRoute(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        if (this.hasWalkablePathToTarget(target, distanceToTarget)) {
            return null;
        }
        if (!this.terrainPlannedActions.isEmpty()) {
            return null;
        }
        boolean allowAggressiveGroupReuse = this.shouldAggressivelyReuseSharedRoute(target);
        if (this.failedRouteRetryCooldown > 0 && !allowAggressiveGroupReuse) {
            return null;
        }

        String cacheKey = this.getSharedRouteCacheKey(level, target);
        Map<String, SharedRouteCacheEntry> levelCache = SHARED_ROUTE_CACHE.get(level);
        if (levelCache == null) {
            return null;
        }

        SharedRouteCacheEntry cached = levelCache.get(cacheKey);
        if (cached == null || cached.isExpired(level.getGameTime())) {
            levelCache.remove(cacheKey);
            cached = allowAggressiveGroupReuse ? this.findNearbySharedRoute(level, target, levelCache) : null;
            if (cached == null) {
                return null;
            }
        }
        if (cached.plans().isEmpty()) {
            return null;
        }
        if (!allowAggressiveGroupReuse && this.lastFailedRouteSignature != null && this.lastFailedRouteSignature.equals(this.getPlanSignature(cached.plans().get(0)))) {
            return null;
        }

        this.terrainPlannedActions.clear();
        this.terrainPlannedActions.addAll(copyPlans(cached.plans()));
        this.terrainRoutePhase = TerrainRoutePhase.LOCAL_SEARCH;
        TerrainPlan first = this.terrainPlannedActions.pollFirst();
        this.advanceTerrainRoutePhase();
        return first;
    }

    private TerrainPlan tryPlanUnbreakableSupportRoute(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        if (this.hasWalkablePathToTarget(target, distanceToTarget)) {
            return null;
        }
        if (!this.terrainPlannedActions.isEmpty()) {
            return null;
        }

        LocalRouteResult result = this.searchLocalRoute(level, target, canSeeTarget);
        if (result == null || result.plans().isEmpty()) {
            return null;
        }

        this.terrainPlannedActions.clear();
        this.terrainPlannedActions.addAll(result.plans());
        this.terrainRoutePhase = TerrainRoutePhase.LOCAL_SEARCH;
        TerrainPlan first = this.terrainPlannedActions.pollFirst();
        this.advanceTerrainRoutePhase();
        return first;
    }

    private TerrainPlan tryPlanLocalRoute(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        if (distanceToTarget <= Math.max(this.attackRadiusSqr + DIRECT_PATH_REACH_MARGIN, PROACTIVE_TERRAIN_MIN_DISTANCE_SQR)) {
            return null;
        }
        if (this.hasWalkablePathToTarget(target, distanceToTarget)) {
            return null;
        }
        if (!this.terrainPlannedActions.isEmpty()) {
            return null;
        }
        if (this.terrainNoProgressTicks < TERRAIN_NO_PROGRESS_THRESHOLD_TICKS && this.terrainPathState != TerrainPathState.UNKNOWN) {
            return null;
        }

        LocalRouteResult result = this.searchLocalRoute(level, target, canSeeTarget);
        if (result == null || result.plans().isEmpty()) {
            return null;
        }

        this.terrainPlannedActions.clear();
        this.terrainPlannedActions.addAll(result.plans());
        this.terrainRoutePhase = TerrainRoutePhase.LOCAL_SEARCH;
        TerrainPlan first = this.terrainPlannedActions.pollFirst();
        this.advanceTerrainRoutePhase();
        return first;
    }

    private LocalRouteResult searchLocalRoute(ServerLevel level, LivingEntity target, boolean canSeeTarget) {
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return null;
        }

        RouteObstacleType routeObstacle = this.getRouteObstacleType(level, target, canSeeTarget, direction);
        boolean allowBreakExpansion = routeObstacle != RouteObstacleType.UNBREAKABLE;

        java.util.PriorityQueue<LocalRouteNode> open = new java.util.PriorityQueue<>((a, b) -> Double.compare(a.score(), b.score()));
        java.util.HashSet<String> visited = new java.util.HashSet<>();
        for (BlockPos startPos : this.getLocalRouteStartPositions(level, target)) {
            open.add(new LocalRouteNode(startPos, direction, List.of(), 0, 0, 0, this.estimateRouteScore(startPos, target, 0, 0, 0), SupportMode.NORMAL));
        }

        while (!open.isEmpty()) {
            LocalRouteNode node = open.poll();
            if (!visited.add(this.routeVisitedKey(node))) {
                continue;
            }

            if (this.canRejoinPathFrom(level, node.position(), target)) {
                List<TerrainPlan> plans = node.history();
                this.storeSharedRoute(level, target, plans);
                return new LocalRouteResult(plans);
            }

            if (node.depth() >= LOCAL_ROUTE_MAX_DEPTH) {
                continue;
            }

            if (routeObstacle == RouteObstacleType.UNBREAKABLE) {
                this.expandWalkSuccessors(level, target, node, open);
                this.expandTunnelSuccessor(level, target, node, open);
                this.expandPlaceLiftSuccessor(level, target, node, open);
                this.expandBridgeSuccessor(level, target, node, open);
            } else {
                this.expandWalkSuccessors(level, target, node, open);
                this.expandPlaceLiftSuccessor(level, target, node, open);
                this.expandBridgeSuccessor(level, target, node, open);
            }
            if (allowBreakExpansion) {
                this.expandBreakSuccessor(level, target, canSeeTarget, node, open);
            }
        }

        return null;
    }

    private List<BlockPos> getLocalRouteStartPositions(ServerLevel level, LivingEntity target) {
        List<BlockPos> starts = new ArrayList<>();
        BlockPos origin = this.shooter.blockPosition();
        starts.add(origin);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = origin.relative(dir);
            if (this.isWithinLocalRouteBounds(candidate) && this.isStandableLocalRoutePosition(level, candidate)) {
                starts.add(candidate);
            }
        }
        BlockPos lower = origin.below();
        if (this.isWithinLocalRouteBounds(lower) && this.isStandableLocalRoutePosition(level, lower)) {
            starts.add(lower);
        }
        return starts;
    }

    private void storeSharedRoute(ServerLevel level, LivingEntity target, List<TerrainPlan> plans) {
        if (plans.isEmpty()) {
            return;
        }
        SHARED_ROUTE_CACHE.computeIfAbsent(level, ignored -> new HashMap<>())
            .put(this.getSharedRouteCacheKey(level, target), new SharedRouteCacheEntry(copyPlans(plans), level.getGameTime() + 40L));
    }

    private boolean shouldAggressivelyReuseSharedRoute(LivingEntity target) {
        return this.spinStuckTicks > 0 || this.terrainNoProgressTicks >= TERRAIN_PROGRESS_SAMPLE_TICKS || this.isNearbyGroupedGunner(target);
    }

    private boolean isNearbyGroupedGunner(LivingEntity target) {
        if (target == null || !this.isGroupedGunner()) {
            return false;
        }
        BlockPos shooterPos = this.shooter.blockPosition();
        BlockPos targetPos = target.blockPosition();
        return shooterPos.closerThan(targetPos, 16.0D);
    }

    private boolean isGroupedGunner() {
        for (String tag : this.shooter.getTags()) {
            if (tag.startsWith("JEGPatrolId:") || tag.startsWith("JEGFactionRaidId:") || tag.startsWith("JEGTerrorRaidId:")) {
                return true;
            }
        }
        return false;
    }

    private SharedRouteCacheEntry findNearbySharedRoute(ServerLevel level, LivingEntity target, Map<String, SharedRouteCacheEntry> levelCache) {
        if (!this.isGroupedGunner()) {
            return null;
        }
        BlockPos shooterPos = this.shooter.blockPosition();
        BlockPos targetPos = target.blockPosition();
        String groupKey = this.resolveGroupRouteKey();
        SharedRouteCacheEntry best = null;
        double bestScore = Double.MAX_VALUE;
        java.util.Iterator<Map.Entry<String, SharedRouteCacheEntry>> iterator = levelCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SharedRouteCacheEntry> entry = iterator.next();
            SharedRouteCacheEntry candidate = entry.getValue();
            if (candidate == null || candidate.isExpired(level.getGameTime())) {
                iterator.remove();
                continue;
            }
            String key = entry.getKey();
            if (key == null || !key.startsWith(groupKey + ":")) {
                continue;
            }
            RouteKeyParts parts = this.parseSharedRouteCacheKey(key);
            if (parts == null) {
                continue;
            }
            if (!parts.targetCell().closerThan(targetPos, (GROUP_SHARED_ROUTE_SEARCH_RADIUS + 0.5D) * 4.0D)) {
                continue;
            }
            if (!parts.shooterCell().closerThan(shooterPos, (GROUP_SHARED_ROUTE_SEARCH_RADIUS + 0.5D) * 4.0D)) {
                continue;
            }
            double score = parts.shooterCell().distManhattan(shooterPos) + parts.targetCell().distManhattan(targetPos);
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private RouteKeyParts parseSharedRouteCacheKey(String key) {
        String[] parts = key.split(":");
        if (parts.length < 4) {
            return null;
        }
        BlockPos shooterCell = this.parseRouteCell(parts[parts.length - 2]);
        BlockPos targetCell = this.parseRouteCell(parts[parts.length - 1]);
        if (shooterCell == null || targetCell == null) {
            return null;
        }
        return new RouteKeyParts(shooterCell, targetCell);
    }

    private BlockPos parseRouteCell(String cell) {
        String[] coords = cell.split(",");
        if (coords.length != 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(coords[0]) << 2;
            int y = Integer.parseInt(coords[1]) << 2;
            int z = Integer.parseInt(coords[2]) << 2;
            return new BlockPos(x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String getSharedRouteCacheKey(ServerLevel level, LivingEntity target) {
        BlockPos shooterPos = this.shooter.blockPosition();
        BlockPos targetPos = target.blockPosition();
        String groupKey = this.resolveGroupRouteKey();
        return groupKey
            + ":" + level.dimension().toString()
            + ":" + (shooterPos.getX() >> 2) + "," + (shooterPos.getY() >> 2) + "," + (shooterPos.getZ() >> 2)
            + ":" + (targetPos.getX() >> 2) + "," + (targetPos.getY() >> 2) + "," + (targetPos.getZ() >> 2);
    }

    private String resolveGroupRouteKey() {
        for (String tag : this.shooter.getTags()) {
            if (tag.startsWith("JEGPatrolId:") || tag.startsWith("JEGFactionRaidId:") || tag.startsWith("JEGTerrorRaidId:")) {
                return tag;
            }
        }
        return this.shooter.getType().toString();
    }

    private List<TerrainPlan> copyPlans(List<TerrainPlan> plans) {
        return new ArrayList<>(plans);
    }

    private void expandWalkSuccessors(ServerLevel level, LivingEntity target, LocalRouteNode node, java.util.PriorityQueue<LocalRouteNode> open) {
        Direction preferredDirection = this.getHorizontalDirectionTo(target.position().subtract(Vec3.atCenterOf(node.position())));
        for (Direction stepDirection : Direction.Plane.HORIZONTAL) {
            BlockPos nextPos = node.position().relative(stepDirection);
            if (!this.isWithinLocalRouteBounds(nextPos)) {
                continue;
            }
            if (!this.isStandableLocalRoutePosition(level, nextPos)) {
                continue;
            }
            double cost = preferredDirection != null && stepDirection == preferredDirection ? 0.75D : 1.5D;
            open.add(this.routeNodeFrom(node, nextPos, stepDirection, LocalRouteAction.WALK, null, cost));
        }
    }

    private void expandPlaceLiftSuccessor(ServerLevel level, LivingEntity target, LocalRouteNode node, java.util.PriorityQueue<LocalRouteNode> open) {
        if (node.placedBlocks() >= LOCAL_ROUTE_MAX_PLACED_BLOCKS) {
            return;
        }
        if (target.getY() <= node.position().getY() + PROACTIVE_TERRAIN_HEIGHT_ADVANTAGE) {
            return;
        }
        if (!this.canPlaceSelfLift(level, node.position()) || !this.hasSelfLiftHeadroom(level, node.position())) {
            return;
        }
        BlockPos nextPos = node.position().above();
        if (!this.isWithinLocalRouteBounds(nextPos) || !this.canContinueFromSimulatedStance(level, nextPos, target)) {
            return;
        }
        TerrainPlan plan = new TerrainPlan(TerrainAction.PLACE_SELF_LIFT, node.position(), null, PlacementIntent.REACTIVE);
        open.add(this.routeNodeFrom(node, nextPos, node.direction(), LocalRouteAction.PLACE_SELF_LIFT, plan, 4.0D));
    }

    private void expandBridgeSuccessor(ServerLevel level, LivingEntity target, LocalRouteNode node, java.util.PriorityQueue<LocalRouteNode> open) {
        if (node.placedBlocks() >= LOCAL_ROUTE_MAX_PLACED_BLOCKS) {
            return;
        }
        BlockPos frontPos = node.position().relative(node.direction());
        BlockPos bridgePos = frontPos.below();
        if (!this.isWithinLocalRouteBounds(frontPos) || !this.canPlaceForwardBridge(level, bridgePos, frontPos) || !this.canContinueFromSimulatedStance(level, frontPos, target)) {
            return;
        }
        TerrainPlan plan = new TerrainPlan(TerrainAction.PLACE_BRIDGE, bridgePos, null, PlacementIntent.REACTIVE);
        open.add(this.routeNodeFrom(node, frontPos, node.direction(), LocalRouteAction.PLACE_BRIDGE, plan, 5.0D));
    }

    private void expandTunnelSuccessor(ServerLevel level, LivingEntity target, LocalRouteNode node, java.util.PriorityQueue<LocalRouteNode> open) {
        if (node.depth() >= LOCAL_ROUTE_MAX_DEPTH - 2) {
            return;
        }
        Direction direction = node.direction();
        if (direction == null || !this.shouldUseTunnelBypassUnderWall(level, node.position(), direction, target)) {
            return;
        }

        BlockPos lowerPos = node.position().below();
        BlockPos lowerFrontPos = lowerPos.relative(direction);
        if (!this.isStandableLocalRoutePosition(level, lowerPos) || !this.isStandableLocalRoutePosition(level, lowerFrontPos)) {
            return;
        }

        open.add(this.routeNodeFrom(node, lowerPos, direction, LocalRouteAction.WALK, null, 2.5D));
        open.add(this.routeNodeFrom(node, lowerFrontPos, direction, LocalRouteAction.WALK, null, 3.0D));
    }

    private void expandBreakSuccessor(ServerLevel level, LivingEntity target, boolean canSeeTarget, LocalRouteNode node, java.util.PriorityQueue<LocalRouteNode> open) {
        if (node.brokenBlocks() >= LOCAL_ROUTE_MAX_BROKEN_BLOCKS) {
            return;
        }

        Direction directionToTarget = this.getHorizontalDirectionTo(target.position().subtract(Vec3.atCenterOf(node.position())));
        Direction breakDirection = directionToTarget != null ? directionToTarget : node.direction();
        if (breakDirection == null) {
            return;
        }

        BlockPos obstacle = this.findBreakableObstacleFromStance(level, node.position(), breakDirection, target, canSeeTarget);
        if (obstacle == null || !this.isWithinLocalRouteBounds(obstacle)) {
            return;
        }

        TerrainPlan plan = new TerrainPlan(TerrainAction.BREAK_OBSTACLE, null, obstacle, PlacementIntent.REACTIVE);
        open.add(this.routeNodeFrom(node, node.position(), breakDirection, LocalRouteAction.BREAK_OBSTACLE, plan, 7.0D));
    }

    private LocalRouteNode routeNodeFrom(LocalRouteNode parent, BlockPos nextPos, Direction nextDirection, LocalRouteAction action, TerrainPlan plan, double actionCost) {
        ArrayList<TerrainPlan> history = new ArrayList<>(parent.history());
        int placed = parent.placedBlocks();
        int broken = parent.brokenBlocks();
        if (plan != null) {
            history.add(plan);
            if (action == LocalRouteAction.PLACE_BRIDGE || action == LocalRouteAction.PLACE_SELF_LIFT) {
                placed++;
            } else if (action == LocalRouteAction.BREAK_OBSTACLE) {
                broken++;
            }
        }
        SupportMode nextMode = switch (action) {
            case PLACE_SELF_LIFT -> SupportMode.ELEVATED;
            case PLACE_BRIDGE -> SupportMode.BRIDGING;
            default -> SupportMode.NORMAL;
        };
        double score = parent.score() + actionCost + this.estimateRouteScore(nextPos, this.shooter.getTarget(), parent.depth() + 1, placed, broken);
        return new LocalRouteNode(nextPos, nextDirection, history, parent.depth() + 1, placed, broken, score, nextMode);
    }

    private double estimateRouteScore(BlockPos pos, LivingEntity target, int depth, int placed, int broken) {
        return pos.distManhattan(target.blockPosition()) + depth + (placed * 3.0D) + (broken * 5.0D);
    }

    private boolean canRejoinPathFrom(ServerLevel level, BlockPos pos, LivingEntity target) {
        if (!this.isStandableLocalRoutePosition(level, pos) || !this.canContinueFromSimulatedStance(level, pos, target)) {
            return false;
        }

        BlockPos targetPos = target.blockPosition();
        if (Math.abs(pos.getY() - targetPos.getY()) > LOCAL_ROUTE_MAX_VERTICAL_DEVIATION + 1) {
            return false;
        }
        if (pos.distManhattan(targetPos) > LOCAL_ROUTE_MAX_HORIZONTAL_RADIUS + 4) {
            return false;
        }

        Path path = this.shooter.getNavigation().createPath(target, 0);
        if (path == null || path.isDone()) {
            return false;
        }

        if (!this.isPathMeaningfullyAdvancing(level, path, target, this.shooter.distanceToSqr(target))) {
            return false;
        }

        return !this.isPartialPathExhausted(level, path, target, this.shooter.distanceToSqr(target));
    }

    private boolean canContinueFromSimulatedStance(ServerLevel level, BlockPos pos, LivingEntity target) {
        if (!this.isStandableLocalRoutePosition(level, pos)) {
            return false;
        }

        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(Vec3.atCenterOf(pos)));
        if (direction == null) {
            return true;
        }

        BlockPos forward = pos.relative(direction);
        BlockPos forwardAbove = forward.above();
        BlockPos stepPos = pos.above();

        boolean canWalkForward = level.getBlockState(forward).canBeReplaced()
            && level.getBlockState(forwardAbove).canBeReplaced()
            && level.getBlockState(forward.below()).isFaceSturdy(level, forward.below(), Direction.UP);

        boolean canBridgeForward = this.canPlaceForwardBridge(level, forward.below(), forward)
            && this.isStandableAfterBridge(level, forward);

        boolean canStepForward = this.canPlaceForwardStep(level, forward)
            && level.getBlockState(stepPos).canBeReplaced()
            && level.getBlockState(stepPos.above()).canBeReplaced();

        if (canWalkForward || canBridgeForward || canStepForward) {
            return true;
        }

        if (target.getY() > pos.getY() + PROACTIVE_TERRAIN_HEIGHT_ADVANTAGE) {
            return this.canPlaceSelfLift(level, pos) && this.hasSelfLiftHeadroom(level, pos);
        }

        return false;
    }

    private boolean isStandableAfterBridge(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced()
            && level.getBlockState(pos.above()).canBeReplaced();
    }

    private boolean hasSelfLiftHeadroom(ServerLevel level, BlockPos basePos) {
        return level.getBlockState(basePos.above()).canBeReplaced() && level.getBlockState(basePos.above(2)).canBeReplaced();
    }

    private boolean isStandableLocalRoutePosition(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced()
            && level.getBlockState(pos.above()).canBeReplaced()
            && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private boolean isWithinLocalRouteBounds(BlockPos pos) {
        BlockPos origin = this.shooter.blockPosition();
        return Math.abs(pos.getX() - origin.getX()) <= LOCAL_ROUTE_MAX_HORIZONTAL_RADIUS
            && Math.abs(pos.getZ() - origin.getZ()) <= LOCAL_ROUTE_MAX_HORIZONTAL_RADIUS
            && Math.abs(pos.getY() - origin.getY()) <= LOCAL_ROUTE_MAX_VERTICAL_DEVIATION;
    }

    private String routeVisitedKey(LocalRouteNode node) {
        return node.position().toShortString() + ":" + node.placedBlocks() + ":" + node.brokenBlocks() + ":" + node.direction();
    }

    private boolean shouldUseBreakoutMode() {
        return this.terrainPlannedActions.isEmpty()
            && this.terrainBreakoutCooldown <= 0
            && this.terrainStuckReason != StuckReason.NONE
            && this.terrainNoProgressTicks >= TERRAIN_BREAKOUT_THRESHOLD_TICKS;
    }

    private TerrainPlan createBreakoutPlan(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget) {
        TerrainPlan breakPlan = this.tryPlanBreakObstacle(level, target, canSeeTarget);
        if (breakPlan != null) {
            return breakPlan.asBreakout();
        }

        TerrainPlan bridgePlan = this.tryPlanBridge(level, target, PlacementIntent.REACTIVE);
        if (bridgePlan != null) {
            return bridgePlan.withAction(TerrainAction.BREAKOUT_PLACE);
        }

        TerrainPlan selfLiftPlan = this.tryPlanSelfLift(level, target);
        if (selfLiftPlan != null) {
            return selfLiftPlan.withAction(TerrainAction.BREAKOUT_PLACE);
        }

        return null;
    }

    private boolean executeTerrainPlan(ServerLevel level, LivingEntity target, boolean canSeeTarget, double distanceToTarget, TerrainPlan plan) {
        if (this.shouldAbortTerrainAction(distanceToTarget) || !this.canPerformTerrainEdit(plan.action())) {
            return false;
        }

        this.terrainLastAction = plan.action();
        return switch (plan.action()) {
            case BREAK_OBSTACLE, BREAKOUT_BREAK -> {
                if (plan.breakPos() == null) {
                    yield false;
                }
                this.startBreakingBlock(plan.breakPos());
                this.recordTerrainEdit(plan.action());
                yield this.continueBreakingBlock(level);
            }
            case PLACE_SELF_LIFT -> {
                boolean progressing = this.executeLiftAction(level, plan);
                if (progressing && this.liftExecutionState == LiftExecutionState.IDLE) {
                    this.recordTerrainEdit(plan.action());
                    this.applyTerrainFollowThrough(target, plan.action(), plan.placePos());
                    if (!this.terrainPlannedActions.isEmpty()) {
                        this.terrainRouteCommitTicks = TERRAIN_ROUTE_COMMIT_TICKS;
                        this.terrainCommittedStepsRemaining = Math.max(this.terrainCommittedStepsRemaining, TERRAIN_ROUTE_COMMIT_STEPS);
                    }
                }
                yield progressing;
            }
            case STEP_UP, PLACE_BRIDGE, BREAKOUT_PLACE -> {
                if (plan.placePos() == null) {
                    yield false;
                }
                boolean placed = this.placeSupportBlock(level, plan.placePos(), plan.intent());
                if (placed) {
                    this.recordTerrainEdit(plan.action());
                    this.applyTerrainFollowThrough(target, plan.action(), plan.placePos());
                    if (!this.terrainPlannedActions.isEmpty()) {
                        this.terrainRouteCommitTicks = TERRAIN_ROUTE_COMMIT_TICKS;
                        this.terrainCommittedStepsRemaining = Math.max(this.terrainCommittedStepsRemaining, TERRAIN_ROUTE_COMMIT_STEPS);
                    }
                }
                yield placed;
            }
            default -> false;
        };
    }

    private void applyTerrainFollowThrough(LivingEntity target, TerrainAction action, BlockPos placePos) {
        if (placePos == null) {
            return;
        }

        BlockPos movePos = switch (action) {
            case PLACE_SELF_LIFT -> placePos.above();
            case STEP_UP -> placePos.above();
            case PLACE_BRIDGE, BREAKOUT_PLACE -> placePos.above();
            default -> null;
        };
        if (movePos == null) {
            return;
        }

        Vec3 destination = Vec3.atBottomCenterOf(movePos);
        this.shooter.getNavigation().moveTo(destination.x, destination.y, destination.z, this.speedModifier);
        this.shooter.getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, this.speedModifier);
        this.shooter.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
    }

    private boolean shouldProactivelyPlaceSupport(LivingEntity target, double distanceToTarget) {
        if (distanceToTarget <= Math.max(this.attackRadiusSqr + DIRECT_PATH_REACH_MARGIN, PROACTIVE_TERRAIN_MIN_DISTANCE_SQR)) {
            return false;
        }

        if (this.terrainPathState == TerrainPathState.PATHABLE && this.terrainNoProgressTicks < TERRAIN_NO_PROGRESS_THRESHOLD_TICKS) {
            return false;
        }

        if (!this.shooter.getNavigation().isInProgress() && this.terrainPathState == TerrainPathState.UNKNOWN) {
            return false;
        }

        ServerLevel level = (ServerLevel) this.shooter.level();
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return false;
        }

        BlockPos shooterPos = this.shooter.blockPosition();
        BlockPos frontPos = shooterPos.relative(direction);
        boolean targetIsHigher = target.getY() > this.shooter.getY() + PROACTIVE_TERRAIN_HEIGHT_ADVANTAGE;

        if (targetIsHigher && this.canPlaceForwardStep(level, frontPos)) {
            return true;
        }

        return this.canPlaceProactiveBridge(level, shooterPos, direction, target);
    }

    private TerrainPlan tryPlanForwardStep(ServerLevel level, LivingEntity target) {
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return null;
        }

        BlockPos stairPos = this.shooter.blockPosition().relative(direction);
        if (!this.canPlaceForwardStep(level, stairPos)) {
            return null;
        }

        return new TerrainPlan(TerrainAction.STEP_UP, stairPos, null, PlacementIntent.PROACTIVE);
    }

    private TerrainPlan tryPlanBridge(ServerLevel level, LivingEntity target, PlacementIntent intent) {
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return null;
        }

        BlockPos shooterPos = this.shooter.blockPosition();
        BlockPos bridgePos = this.findBridgePlacementFromStance(level, shooterPos, direction, target, intent == PlacementIntent.PROACTIVE);
        if (bridgePos == null) {
            return null;
        }

        return new TerrainPlan(TerrainAction.PLACE_BRIDGE, bridgePos, null, intent);
    }

    private BlockPos findBridgePlacementFromStance(ServerLevel level, BlockPos stancePos, Direction direction, LivingEntity target, boolean proactive) {
        if (proactive && target.getY() > this.shooter.getY() + 0.25D) {
            return null;
        }

        BlockPos frontPos = stancePos.relative(direction);
        BlockPos frontBridgePos = frontPos.below();
        if (this.canPlaceForwardBridge(level, frontBridgePos, frontPos)) {
            if (!proactive || this.isSafeProactiveBridge(level, stancePos, frontBridgePos)) {
                return frontBridgePos;
            }
        }

        BlockPos diagonalPos = frontPos.relative(this.getBridgeSidePreference(direction, target));
        BlockPos diagonalBridgePos = diagonalPos.below();
        if (this.canPlaceForwardBridge(level, diagonalBridgePos, diagonalPos) && this.canContinueFromSimulatedStance(level, diagonalPos, target)) {
            return diagonalBridgePos;
        }

        return null;
    }

    private Direction getBridgeSidePreference(Direction forward, LivingEntity target) {
        Vec3 delta = target.position().subtract(this.shooter.position());
        return switch (forward) {
            case NORTH, SOUTH -> delta.x >= 0.0D ? Direction.EAST : Direction.WEST;
            case EAST, WEST -> delta.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
            default -> Direction.NORTH;
        };
    }

    private boolean isSafeProactiveBridge(ServerLevel level, BlockPos stancePos, BlockPos bridgePos) {
        if (!level.getBlockState(stancePos.below()).isFaceSturdy(level, stancePos.below(), Direction.UP)) {
            return false;
        }

        int gapDepth = 0;
        BlockPos cursor = bridgePos;
        while (level.getBlockState(cursor).canBeReplaced() && gapDepth <= PROACTIVE_BRIDGE_MAX_GAP) {
            cursor = cursor.below();
            gapDepth++;
        }

        return gapDepth == 1 && level.getBlockState(cursor).isFaceSturdy(level, cursor, Direction.UP);
    }

    private boolean canPlaceForwardBridgeFromStance(ServerLevel level, BlockPos stancePos, Direction direction) {
        BlockPos frontPos = stancePos.relative(direction);
        return this.canPlaceForwardBridge(level, frontPos.below(), frontPos);
    }

    private ServerLevel requireServerLevel() {
        return (ServerLevel) this.shooter.level();
    }

    private boolean canPathRejoinFromNode(ServerLevel level, BlockPos pos, LivingEntity target) {
        return this.canContinueFromSimulatedStance(level, pos, target);
    }

    private boolean canPlaceProactiveBridge(ServerLevel level, BlockPos shooterPos, Direction direction, LivingEntity target) {
        return this.findBridgePlacementFromStance(level, shooterPos, direction, target, true) != null;
    }

    private boolean canPlaceForwardBridge(ServerLevel level, BlockPos bridgePos, BlockPos frontPos) {
        return this.isSafeSupportPlacement(level, TerrainAction.PLACE_BRIDGE, bridgePos, this.shooter.getTarget())
            && this.canMoveIntoSupportSpace(level, frontPos)
            && level.getBlockState(bridgePos.below()).isFaceSturdy(level, bridgePos.below(), Direction.UP);
    }

    private boolean canPlaceForwardStep(ServerLevel level, BlockPos stairPos) {
        return this.isSafeSupportPlacement(level, TerrainAction.STEP_UP, stairPos, this.shooter.getTarget())
            && level.getBlockState(stairPos.below()).isFaceSturdy(level, stairPos.below(), Direction.UP)
            && level.getBlockState(stairPos.above()).canBeReplaced();
    }

    private boolean canPlaceSelfLift(ServerLevel level, BlockPos selfLiftPos) {
        return this.isSafeSupportPlacement(level, TerrainAction.PLACE_SELF_LIFT, selfLiftPos, this.shooter.getTarget())
            && level.getBlockState(selfLiftPos.below()).isFaceSturdy(level, selfLiftPos.below(), Direction.UP)
            && level.getBlockState(selfLiftPos.above()).canBeReplaced();
    }

    private boolean canMoveIntoSupportSpace(ServerLevel level, BlockPos frontPos) {
        return level.getBlockState(frontPos).canBeReplaced() && level.getBlockState(frontPos.above()).canBeReplaced();
    }

    private TerrainPlan tryPlanSelfLift(ServerLevel level, LivingEntity target) {
        if (target.getY() <= this.shooter.getY() + PROACTIVE_TERRAIN_HEIGHT_ADVANTAGE) {
            return null;
        }

        BlockPos selfLiftPos = this.shooter.blockPosition();
        if (!this.canPlaceSelfLift(level, selfLiftPos)) {
            return null;
        }

        return new TerrainPlan(TerrainAction.PLACE_SELF_LIFT, selfLiftPos, null, PlacementIntent.REACTIVE);
    }

    private TerrainPlan tryPlanBreakObstacle(ServerLevel level, LivingEntity target, boolean canSeeTarget) {
        BlockPos obstacle = this.findBreakableObstacle(target, canSeeTarget);
        if (obstacle == null) {
            return null;
        }
        return new TerrainPlan(TerrainAction.BREAK_OBSTACLE, null, obstacle, PlacementIntent.REACTIVE);
    }

    private boolean canPerformTerrainEdit(TerrainAction action) {
        if (this.terrainEditsInWindow >= TERRAIN_MAX_EDITS_PER_WINDOW) {
            return false;
        }
        if (this.terrainConsecutiveActionsWithoutProgress >= TERRAIN_MAX_CONSECUTIVE_ACTIONS_WITHOUT_PROGRESS) {
            return false;
        }
        return switch (action) {
            case BREAKOUT_BREAK, BREAKOUT_PLACE -> this.terrainBreakoutCooldown <= 0;
            case STEP_UP, PLACE_BRIDGE, PLACE_SELF_LIFT -> this.terrainPlaceCooldown <= 0;
            default -> true;
        };
    }

    private void recordTerrainEdit(TerrainAction action) {
        if (this.terrainEditWindowTicks <= 0) {
            this.terrainEditWindowTicks = TERRAIN_EDIT_WINDOW_TICKS;
            this.terrainEditsInWindow = 0;
        }
        this.terrainEditsInWindow++;
        this.terrainConsecutiveActionsWithoutProgress++;
        this.terrainPathRecheckCooldown = 0;
        if (action == TerrainAction.BREAKOUT_BREAK || action == TerrainAction.BREAKOUT_PLACE) {
            this.terrainBreakoutCooldown = TERRAIN_BREAKOUT_ACTION_COOLDOWN_TICKS;
        }
    }

    private boolean shouldAbortTerrainAction(double distanceToTarget) {
        return distanceToTarget <= Math.max(this.attackRadiusSqr + DIRECT_PATH_REACH_MARGIN, TERRAIN_MIN_DISTANCE_SQR)
            || this.terrainEditsInWindow >= TERRAIN_MAX_EDITS_PER_WINDOW
            || this.terrainConsecutiveActionsWithoutProgress >= TERRAIN_MAX_CONSECUTIVE_ACTIONS_WITHOUT_PROGRESS;
    }

    private void resetTerrainState() {
        this.resetTerrainBreakTarget();
        this.clearTerrainRoute();
        this.terrainPathState = TerrainPathState.PATHABLE;
        this.terrainStuckReason = StuckReason.NONE;
        this.terrainLastAction = TerrainAction.NONE;
        this.terrainNoProgressTicks = 0;
        this.terrainConsecutiveActionsWithoutProgress = 0;
        this.terrainLastSamplePos = null;
        this.terrainLastSampleDistanceToTarget = Double.MAX_VALUE;
        this.terrainSampleCooldown = 0;
    }

    private void resetTerrainStateForVisibleTarget() {
        this.resetTerrainState();
        this.attackMode = AttackMode.NORMAL;
        this.breakBuildLockTicks = 0;
        this.supportFreezeTicks = 0;
        this.terrainReplanCooldown = 0;
        this.liftExecutionState = LiftExecutionState.IDLE;
        this.liftAnchorPos = null;
        this.liftExecutionTicks = 0;
        this.zbbBreakCooldownUntil = Long.MIN_VALUE;
        this.zbbBuildCooldownUntil = Long.MIN_VALUE;
        this.zbbDangerousCooldownUntil = Long.MIN_VALUE;
        this.zbbCustomStuckTicks = 0;
        this.zbbLastStuckCheckPos = null;
        this.spinStuckLastYaw = this.shooter.getYRot();
        this.spinStuckTicks = 0;
        this.zbbLiftState = ZbbLiftState.IDLE;
        this.zbbBlockDamage.clear();
        this.zbbProtectedBuiltBlocks.clear();
        if (this.shooter.level() instanceof ServerLevel level) {
            for (Map.Entry<BlockPos, Integer> entry : this.zbbBlockProgressIds.entrySet()) {
                level.destroyBlockProgress(entry.getValue(), entry.getKey(), -1);
            }
        }
        this.zbbBlockProgressIds.clear();
    }

    private BlockPos findBreakableObstacle(LivingEntity target, boolean canSeeTarget) {
        Level level = this.shooter.level();
        Direction direction = this.getHorizontalDirectionTo(target.position().subtract(this.shooter.position()));
        if (direction == null) {
            return null;
        }
        return this.findBreakableObstacleFromStance(level, this.shooter.blockPosition(), direction, target, canSeeTarget);
    }

    private BlockPos findBreakableObstacleFromStance(Level level, BlockPos stancePos, Direction direction, LivingEntity target, boolean canSeeTarget) {
        int maxTier = Config.gunnerTerrainBreakMaxTier();
        BlockPos obstacle = this.findImmediateBreakableObstacle(level, stancePos, direction, maxTier);
        if (obstacle != null) {
            return obstacle;
        }

        if (!canSeeTarget) {
            Vec3 shooterEye = Vec3.atCenterOf(stancePos).add(0.0D, this.shooter.getEyeHeight(), 0.0D);
            Vec3 targetEye = target.getEyePosition();
            BlockHitResult result = level.clip(new ClipContext(shooterEye, targetEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.shooter));
            if (result.getType() == HitResult.Type.BLOCK && shooterEye.distanceTo(Vec3.atCenterOf(result.getBlockPos())) <= GUNNER_BREAK_REACH + 0.75F) {
                BlockPos hitPos = result.getBlockPos();
                if (this.isBreakableObstacle(level, hitPos, maxTier)) {
                    return hitPos;
                }
            }
        }

        return null;
    }

    private boolean isBreakReachableFromStance(BlockPos stancePos, BlockPos obstaclePos) {
        return Vec3.atCenterOf(stancePos).distanceTo(Vec3.atCenterOf(obstaclePos)) <= GUNNER_BREAK_REACH + 0.75D;
    }

    private boolean isBreakableObstacleFromStance(Level level, BlockPos stancePos, BlockPos pos, int maxTier) {
        return this.isBreakReachableFromStance(stancePos, pos) && this.isBreakableObstacle(level, pos, maxTier);
    }

    private boolean isUnbreakableObstacleFromStance(Level level, BlockPos stancePos, BlockPos pos, int maxTier) {
        return this.isBreakReachableFromStance(stancePos, pos) && this.isUnbreakableObstacle(level, pos, maxTier);
    }

    private boolean classifyBlockedMovementFromStance(Level level, BlockPos stancePos, BlockPos pos, int maxTier) {
        return this.isBreakReachableFromStance(stancePos, pos) && !level.getBlockState(pos).isAir();
    }

    private RouteObstacleType classifyImmediateObstacleFromStance(Level level, BlockPos stancePos, BlockPos pos, int maxTier) {
        if (!this.classifyBlockedMovementFromStance(level, stancePos, pos, maxTier)) {
            return RouteObstacleType.NONE;
        }
        if (this.isBreakableObstacleFromStance(level, stancePos, pos, maxTier)) {
            return RouteObstacleType.BREAKABLE;
        }
        if (this.isUnbreakableObstacleFromStance(level, stancePos, pos, maxTier)) {
            return RouteObstacleType.UNBREAKABLE;
        }
        return RouteObstacleType.NONE;
    }

    private RouteObstacleType classifyImmediateObstacle(Level level, BlockPos pos, int maxTier) {
        return this.classifyImmediateObstacleFromStance(level, this.shooter.blockPosition(), pos, maxTier);
    }

    private BlockPos findImmediateBreakableObstacle(Level level, BlockPos stancePos, Direction direction, int maxTier) {
        for (BlockPos candidate : this.getImmediateObstacleCandidates(stancePos, direction)) {
            if (this.isBreakableObstacleFromStance(level, stancePos, candidate, maxTier)) {
                return candidate;
            }
        }
        return null;
    }

    private RouteObstacleType getRouteObstacleTypeFromStance(ServerLevel level, BlockPos stancePos, LivingEntity target, boolean canSeeTarget, Direction direction) {
        int maxTier = Config.gunnerTerrainBreakMaxTier();
        for (BlockPos candidate : this.getImmediateObstacleCandidates(stancePos, direction)) {
            RouteObstacleType type = this.classifyImmediateObstacleFromStance(level, stancePos, candidate, maxTier);
            if (type != RouteObstacleType.NONE) {
                return type;
            }
        }

        if (!canSeeTarget) {
            Vec3 shooterEye = Vec3.atCenterOf(stancePos).add(0.0D, this.shooter.getEyeHeight(), 0.0D);
            Vec3 targetEye = target.getEyePosition();
            BlockHitResult result = level.clip(new ClipContext(shooterEye, targetEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.shooter));
            if (result.getType() == HitResult.Type.BLOCK) {
                return this.classifyImmediateObstacleFromStance(level, stancePos, result.getBlockPos(), maxTier);
            }
        }

        return RouteObstacleType.NONE;
    }


    private boolean isBreakableObstacle(Level level, BlockPos pos, int maxTier) {
        if (this.shooter.distanceToSqr(Vec3.atCenterOf(pos)) > (double) (GUNNER_BREAK_REACH * GUNNER_BREAK_REACH)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return !state.isAir()
            && !state.hasBlockEntity()
            && state.getDestroySpeed(level, pos) >= 0.0F
            && !this.isProtectedRecentSupport(this.requireServerLevel(), pos)
            && BulletPenetrationHelper.canGunnerBreakBlock(level, state, maxTier);
    }

    private boolean isUnbreakableObstacle(Level level, BlockPos pos, int maxTier) {
        if (this.shooter.distanceToSqr(Vec3.atCenterOf(pos)) > (double) (GUNNER_BREAK_REACH * GUNNER_BREAK_REACH)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }

        return !BulletPenetrationHelper.canGunnerBreakBlock(level, state, maxTier);
    }

    private void startBreakingBlock(BlockPos pos) {
        if (pos.equals(this.terrainBreakTarget)) {
            return;
        }
        this.resetTerrainBreakTarget();
        this.terrainBreakTarget = pos.immutable();
        this.terrainBreakProgress = 0.0F;
        this.terrainBreakTicks = 0;
        this.terrainLastBreakStage = -1;
    }

    private boolean continueBreakingBlock(ServerLevel level) {
        if (this.terrainBreakTarget == null) {
            return false;
        }

        int maxTier = Config.gunnerTerrainBreakMaxTier();
        if (!this.isBreakableObstacle(level, this.terrainBreakTarget, maxTier)) {
            this.resetTerrainBreakTarget();
            return false;
        }

        BlockState state = level.getBlockState(this.terrainBreakTarget);
        this.shooter.getNavigation().stop();
        this.shooter.getLookControl().setLookAt(
            this.terrainBreakTarget.getX() + 0.5D,
            this.terrainBreakTarget.getY() + 0.5D,
            this.terrainBreakTarget.getZ() + 0.5D
        );

        if (this.terrainBreakTicks % 6 == 0) {
            this.shooter.swing(InteractionHand.MAIN_HAND);
        }

        this.terrainBreakProgress += this.getBreakProgressPerTick(level, this.terrainBreakTarget, state);
        ++this.terrainBreakTicks;

        int stage = Math.max(-1, Math.min(9, Mth.floor(this.terrainBreakProgress * 10.0F) - 1));
        if (stage != this.terrainLastBreakStage) {
            level.destroyBlockProgress(this.shooter.getId(), this.terrainBreakTarget, stage);
            this.terrainLastBreakStage = stage;
        }

        if (this.terrainBreakProgress >= 1.0F) {
            BlockPos brokenPos = this.terrainBreakTarget;
            level.destroyBlockProgress(this.shooter.getId(), brokenPos, -1);
            level.levelEvent(2001, brokenPos, Block.getId(state));
            level.destroyBlock(brokenPos, false);
            this.terrainBreakTarget = null;
            this.terrainBreakProgress = 0.0F;
            this.terrainBreakTicks = 0;
            this.terrainLastBreakStage = -1;
        }

        return true;
    }

    private float getBreakProgressPerTick(Level level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) {
            return 0.0F;
        }
        float breakTicks = hardness <= 0.0F ? 8.0F : hardness * 30.0F;
        return 1.0F / Mth.clamp(breakTicks, 8.0F, 200.0F);
    }

    private boolean tryPlaceSupportBlock(ServerLevel level, LivingEntity target, PlacementIntent intent) {
        TerrainPlan plan = this.tryPlanForwardStep(level, target);
        if (plan != null) {
            return this.placeSupportBlock(level, plan.placePos(), intent == PlacementIntent.REACTIVE ? PlacementIntent.REACTIVE : plan.intent());
        }

        plan = this.tryPlanBridge(level, target, intent);
        if (plan != null) {
            return this.placeSupportBlock(level, plan.placePos(), intent == PlacementIntent.REACTIVE ? PlacementIntent.REACTIVE : plan.intent());
        }

        if (intent == PlacementIntent.REACTIVE) {
            plan = this.tryPlanSelfLift(level, target);
            if (plan != null) {
                return this.placeSupportBlock(level, plan.placePos(), plan.intent());
            }
        }

        return false;
    }

    private boolean canPlaceSupportBlock(ServerLevel level, BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        return existing.canBeReplaced();
    }

    private enum PlacementIntent {
        REACTIVE,
        PROACTIVE,
        BREAKOUT
    }

    private enum AttackMode {
        NORMAL,
        BREAK_BUILD
    }

    private enum TerrainPathState {
        PATHABLE,
        LOCAL_OBSTACLE,
        UNBREAKABLE_OBSTACLE,
        GAP,
        STEP_UP,
        PATH_NOT_PROGRESSING,
        UNKNOWN
    }

    private enum StuckReason {
        NONE,
        BLOCKED_FRONT,
        GAP_EDGE,
        CLIFF_FACE,
        WATER_FLOW,
        IMMOBILIZED,
        PATH_NOT_PROGRESSING
    }

    private enum TerrainAction {
        NONE,
        STEP_UP,
        PLACE_BRIDGE,
        PLACE_SELF_LIFT,
        BREAK_OBSTACLE,
        BREAKOUT_BREAK,
        BREAKOUT_PLACE
    }

    private enum TerrainRoutePhase {
        NONE,
        LOCAL_SEARCH,
        VERTICAL_ASCENT,
        FORWARD_BRIDGE,
        FINAL_REJOIN,
        BREAKOUT
    }

    private enum LocalRouteAction {
        WALK,
        PLACE_SELF_LIFT,
        PLACE_BRIDGE,
        BREAK_OBSTACLE,
        REJOIN_PATH
    }

    private record TerrainPlan(TerrainAction action, BlockPos placePos, BlockPos breakPos, PlacementIntent intent) {
        private TerrainPlan withAction(TerrainAction action) {
            return new TerrainPlan(action, this.placePos, this.breakPos, this.intent == PlacementIntent.PROACTIVE ? PlacementIntent.BREAKOUT : this.intent);
        }

        private TerrainPlan asBreakout() {
            return this.withAction(TerrainAction.BREAKOUT_BREAK);
        }
    }

    private record LocalRouteNode(BlockPos position, Direction direction, List<TerrainPlan> history, int depth, int placedBlocks, int brokenBlocks, double score, SupportMode supportMode) {
    }

    private record LocalRouteResult(List<TerrainPlan> plans) {
    }

    private record RouteKeyParts(BlockPos shooterCell, BlockPos targetCell) {
    }

    private record SharedRouteCacheEntry(List<TerrainPlan> plans, long expiresAt) {
        private boolean isExpired(long gameTime) {
            return gameTime > this.expiresAt;
        }
    }

    private enum RouteObstacleType {
        NONE,
        BREAKABLE,
        UNBREAKABLE
    }

    private enum SupportMode {
        NORMAL,
        ELEVATED,
        BRIDGING
    }

    private enum LiftExecutionState {
        IDLE,
        PREPARE,
        JUMPING,
        PLACING,
        VERIFY
    }

    private enum ZbbLiftState {
        IDLE,
        JUMPING,
        WAITING_FOR_BLOCK
    }

    private boolean placeSupportBlock(ServerLevel level, BlockPos pos, PlacementIntent intent) {
        if (!Config.gunnerTerrainPlacementEnabled()) {
            return false;
        }

        BlockState supportBlock = Config.gunnerTerrainSupportBlockState();
        level.setBlockAndUpdate(pos, supportBlock);
        this.recordPlacedSupport(level, pos);
        this.terrainPlaceCooldown = switch (intent) {
            case PROACTIVE -> PROACTIVE_TERRAIN_PLACE_COOLDOWN_TICKS;
            case BREAKOUT -> TERRAIN_BREAKOUT_PLACE_COOLDOWN_TICKS;
            default -> TERRAIN_PLACE_COOLDOWN_TICKS;
        };
        this.terrainPathRecheckCooldown = 0;
        this.shooter.getNavigation().stop();
        if (pos.equals(this.shooter.blockPosition())) {
            this.shooter.setPos(this.shooter.getX(), this.shooter.getY() + 1.0D, this.shooter.getZ());
        }
        this.shooter.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private boolean executeLiftAction(ServerLevel level, TerrainPlan plan) {
        if (plan.placePos() == null) {
            return false;
        }

        return switch (this.liftExecutionState) {
            case IDLE -> {
                if (!this.hasSelfLiftHeadroom(level, plan.placePos())) {
                    yield false;
                }
                this.liftAnchorPos = plan.placePos();
                this.liftExecutionTicks = 0;
                this.liftExecutionState = LiftExecutionState.PREPARE;
                yield true;
            }
            case PREPARE -> {
                this.shooter.getJumpControl().jump();
                this.liftExecutionState = LiftExecutionState.JUMPING;
                this.liftExecutionTicks = 0;
                yield true;
            }
            case JUMPING -> {
                this.liftExecutionTicks++;
                if (this.shooter.onGround()) {
                    if (this.liftExecutionTicks > 2) {
                        this.resetLiftExecution();
                        yield false;
                    }
                    yield true;
                }
                if (this.shooter.getY() >= this.liftAnchorPos.getY() + 0.75D || this.shooter.getDeltaMovement().y <= 0.0D) {
                    this.liftExecutionState = LiftExecutionState.PLACING;
                }
                yield true;
            }
            case PLACING -> {
                boolean placed = this.placeSupportBlock(level, this.liftAnchorPos, plan.intent());
                if (!placed) {
                    this.resetLiftExecution();
                    yield false;
                }
                this.liftExecutionState = LiftExecutionState.VERIFY;
                yield true;
            }
            case VERIFY -> {
                boolean valid = this.canContinueFromSimulatedStance(level, this.liftAnchorPos.above(), this.shooter.getTarget());
                this.resetLiftExecution();
                yield valid;
            }
        };
    }

    private void resetLiftExecution() {
        this.liftExecutionState = LiftExecutionState.IDLE;
        this.liftAnchorPos = null;
        this.liftExecutionTicks = 0;
    }

    private Direction getHorizontalDirectionTo(Vec3 delta) {
        if (Math.abs(delta.x) < 1.0E-4D && Math.abs(delta.z) < 1.0E-4D) {
            return null;
        }
        return Math.abs(delta.x) >= Math.abs(delta.z)
            ? (delta.x >= 0.0D ? Direction.EAST : Direction.WEST)
            : (delta.z >= 0.0D ? Direction.SOUTH : Direction.NORTH);
    }

    private void resetTerrainBreakTarget() {
        if (this.terrainBreakTarget != null && this.shooter.level() instanceof ServerLevel serverLevel) {
            serverLevel.destroyBlockProgress(this.shooter.getId(), this.terrainBreakTarget, -1);
        }
        this.terrainBreakTarget = null;
        this.terrainBreakProgress = 0.0F;
        this.terrainBreakTicks = 0;
        this.terrainLastBreakStage = -1;
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
            if (gunItem.usesLoadedAmmo()) {
                Integer ammoComponent = heldItem.get(ModDataComponents.GUN_AMMO.get());
                int currentAmmo = ammoComponent != null ? ammoComponent : 0;
                if (currentAmmo <= 0) {
                    break;
                }
                heldItem.set(ModDataComponents.GUN_AMMO.get(), Math.max(0, currentAmmo - 1));
            }
            if (this.shooter.level() instanceof ServerLevel serverLevel) {
                NetworkHandler.sendGunFireFx(serverLevel, this.shooter.getId(), random.nextFloat());
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
                    BulletEntity bullet = GunItem.createBullet(this.shooter.level(), this.shooter, heldItem, stats, velocity);
                    bullet.initialisePosition(muzzle);
                    this.shooter.level().addFreshEntity(bullet);
                    if (this.shooter.level() instanceof ServerLevel serverLevel && GunItem.isBulletClassWeapon(stats.id())) {
                        bullet.sendTrailToClients(serverLevel);
                    }

                    if (!stats.flameTrail() && this.shooter.level() instanceof ServerLevel serverLevel) {
                        spawnBulletTrailParticles(serverLevel, muzzle, direction, stats);
                    }
                }
                FactionRaidManager.notifyRaidMobFired(this.shooter);
            }

            playGunshotSound(stats);
        }
    }

    private static int gunnerFireDelay(GunStats stats) {
        String path = stats.id().getPath();
        if ("minigun".equals(path)) {
            return Math.max(2, stats.fireDelay());
        }
        if ("light_machine_gun".equals(path)) {
            return Math.max(3, stats.fireDelay());
        }
        return Math.max(6, stats.fireDelay());
    }

    private Vec3 computeDirection(LivingEntity shooter, Vec3 origin, LivingEntity target, RandomSource random, GunStats stats) {
        Vec3 base = target.getEyePosition().subtract(origin);
        return applyLegacySpread(shooter, base, stats, random);
    }

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
            if (GunItem.isShotgunWeapon(stats.id())) {
                gunSpread = stats.spread() * 0.60F;
            } else {
                float earlySpreadMultiplier = shooter.level().getDifficulty() != Difficulty.HARD ? 10.0F : 5.0F;
                float scaledSpreadMultiplier = Config.scaleGunnerSpreadMultiplier(shooter.level(), earlySpreadMultiplier);
                gunSpread *= scaledSpreadMultiplier;
            }
        }
        if (gunSpread <= 0.0F) {
            return forwards.normalize();
        }

        float spreadRadians = Math.min(gunSpread, 170.0F) * Mth.DEG_TO_RAD;
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

    protected void playGunshotSound(GunStats stats) {
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
            sound -> this.shooter.level().playSound(null, this.shooter, sound, SoundSource.HOSTILE, 7.5F, 0.9F + this.shooter.level().getRandom().nextFloat() * 0.2F),
            () -> this.shooter.level().playSound(null, this.shooter, net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 7.5F, 0.9F + this.shooter.level().getRandom().nextFloat() * 0.2F)
        );
    }

    protected void playReloadSound(GunStats stats) {
        stats.reloadStartSoundEvent().ifPresent(sound ->
            this.shooter.level().playSound(null, this.shooter, sound, SoundSource.HOSTILE, 1.0F, 1.0F)
        );
    }

    protected void finishReload(ItemStack stack, GunStats stats) {
        if (stack.getItem() instanceof GunItem gun && gun.usesLoadedAmmo()) {
            stack.set(ModDataComponents.GUN_AMMO.get(), Math.max(1, stats.magazineSize()));
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

    private void spawnBulletTrailParticles(ServerLevel level, Vec3 start, Vec3 direction, GunStats stats) {
        if (level.getRandom().nextFloat() < 0.25F) {
            int count = level.getRandom().nextFloat() < 0.35F ? 2 : 1;
            level.sendParticles(
                ParticleTypes.SMOKE,
                start.x, start.y, start.z,
                count, 0.02, 0.02, 0.02, 0.01
            );
        }
    }
}
