package ttv.migami.jeg.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.faction.GunnerFactionRelations;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.SpecialExplosiveItem;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.util.SpecialExplosion;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

/**
 * Throwable special explosives ported toward Superb Warfare C4 / Claymore / TM-62 behavior.
 */
public final class PlacedExplosiveEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    public static final int C4_COUNTDOWN = 514;
    /** Timed C4: full damage, 200% of original radius (10 -> 20). */
    public static final float C4_TIMED_DAMAGE = 300.0F;
    public static final double C4_TIMED_RADIUS = 20.0D;
    /** Remote C4: 75% damage and radius of original (300/10 -> 225/7.5). */
    public static final float C4_REMOTE_DAMAGE = 225.0F;
    public static final double C4_REMOTE_RADIUS = 7.5D;
    /** @deprecated use timed/remote constants */
    public static final float C4_DAMAGE = C4_TIMED_DAMAGE;
    /** @deprecated use timed/remote constants */
    public static final double C4_RADIUS = C4_TIMED_RADIUS;
    public static final float CLAYMORE_DAMAGE = 140.0F;
    public static final float CLAYMORE_DESTROY_DAMAGE = 28.0F;
    public static final double CLAYMORE_RADIUS = 4.0D;
    public static final float CLAYMORE_HEALTH = 10.0F;
    public static final float TM62_DAMAGE = 450.0F;
    public static final double TM62_RADIUS = 13.0D;
    public static final float TM62_HEALTH = 100.0F;
    public static final int TM62_FUSE_TICKS = 100;

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REMOTE = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TIMED = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BOMB_TICK = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IN_GROUND = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> ATTACHED = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.STRING);

    private boolean settled;
    /** Prevents recursive explosion: hurt -> detonate -> explode -> hurt -> ... */
    private boolean detonating;
    /** Block this explosive stuck to (SW lastState / support). Null if free-falling. */
    @Nullable
    private BlockPos stuckBlockPos;
    @Nullable
    private BlockState stuckBlockState;

    public PlacedExplosiveEntity(EntityType<? extends PlacedExplosiveEntity> type, Level level) {
        super(type, level);
    }

    public boolean isDetonating() {
        return this.detonating;
    }

    public PlacedExplosiveEntity(ServerLevel level, SpecialExplosiveItem.Kind kind, @Nullable LivingEntity owner, Vec3 position, float yaw) {
        this(ModEntities.PLACED_EXPLOSIVE.get(), level);
        this.entityData.set(KIND, kind.ordinal());
        this.entityData.set(OWNER, owner == null ? "" : owner.getUUID().toString());
        this.setPos(position);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.entityData.set(HEALTH, kind == SpecialExplosiveItem.Kind.TM_62 ? TM62_HEALTH : CLAYMORE_HEALTH);
    }

    public static PlacedExplosiveEntity throwFrom(ServerLevel level, LivingEntity owner, SpecialExplosiveItem.Kind kind, boolean remoteOrFuse) {
        PlacedExplosiveEntity entity = new PlacedExplosiveEntity(level, kind, owner, owner.getEyePosition().add(owner.getLookAngle().scale(0.25D)).add(0.0D, -0.2D, 0.0D), owner.getYRot());
        Vec3 look = owner.getLookAngle();
        entity.setDeltaMovement(look.scale(0.5D));

        if (kind == SpecialExplosiveItem.Kind.C4) {
            entity.setRemote(remoteOrFuse);
            // Flight orientation follows throw arc
            double horizontal = look.horizontalDistance();
            entity.setYRot((float) (Mth.atan2(look.x, look.z) * (180.0D / Math.PI)));
            entity.setXRot((float) (Mth.atan2(look.y, horizontal) * (180.0D / Math.PI)));
        } else if (kind == SpecialExplosiveItem.Kind.TM_62) {
            entity.setTimed(remoteOrFuse);
            // SW random yaw on throw
            float randomRot = (float) Mth.clamp((2.0D * level.getRandom().nextDouble() - 1.0D) * 180.0D, -180.0D, 180.0D);
            entity.setYRot(randomRot);
            entity.setXRot(0.0F);
        } else {
            // Claymore: facing = player look yaw (kill zone in front of thrower)
            entity.setYRot(owner.getYRot());
            entity.setXRot(0.0F);
        }
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
        return entity;
    }

    /** Spawn already-settled (drone drop / tests). */
    public static PlacedExplosiveEntity placeSettled(ServerLevel level, SpecialExplosiveItem.Kind kind, @Nullable LivingEntity owner, Vec3 position, float yaw, boolean remoteOrFuse) {
        PlacedExplosiveEntity entity = new PlacedExplosiveEntity(level, kind, owner, position, yaw);
        if (kind == SpecialExplosiveItem.Kind.C4) {
            entity.setRemote(remoteOrFuse);
        } else if (kind == SpecialExplosiveItem.Kind.TM_62) {
            entity.setTimed(remoteOrFuse);
        }
        entity.settled = true;
        entity.entityData.set(IN_GROUND, true);
        entity.setDeltaMovement(Vec3.ZERO);
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, SpecialExplosiveItem.Kind.C4.ordinal());
        builder.define(REMOTE, false);
        builder.define(TIMED, false);
        builder.define(BOMB_TICK, 0);
        builder.define(HEALTH, CLAYMORE_HEALTH);
        builder.define(IN_GROUND, false);
        builder.define(ATTACHED, "");
        builder.define(OWNER, "");
    }

    @Nullable
    public UUID ownerId() {
        String raw = this.entityData.get(OWNER);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Nullable
    private UUID attachedId() {
        String raw = this.entityData.get(ATTACHED);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public SpecialExplosiveItem.Kind kind() {
        int value = this.entityData.get(KIND);
        SpecialExplosiveItem.Kind[] values = SpecialExplosiveItem.Kind.values();
        return value >= 0 && value < values.length ? values[value] : SpecialExplosiveItem.Kind.C4;
    }

    public void setRemote(boolean remote) {
        this.entityData.set(REMOTE, remote);
    }

    public void setTimed(boolean timed) {
        this.entityData.set(TIMED, timed);
    }

    public void setBombTick(int tick) {
        this.entityData.set(BOMB_TICK, tick);
    }

    public boolean isRemote() {
        return this.entityData.get(REMOTE);
    }

    public boolean isTimed() {
        return this.entityData.get(TIMED);
    }

    public boolean isInGround() {
        return this.entityData.get(IN_GROUND) || this.settled || this.attachedId() != null;
    }

    public boolean isRemoteC4OwnedBy(UUID owner) {
        UUID mine = this.ownerId();
        return this.kind() == SpecialExplosiveItem.Kind.C4
                && this.entityData.get(REMOTE)
                && mine != null
                && mine.equals(owner);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    public void attachTo(@Nullable Entity entity) {
        if (entity == null) {
            this.entityData.set(ATTACHED, "");
            return;
        }
        this.entityData.set(ATTACHED, entity.getUUID().toString());
        this.entityData.set(IN_GROUND, false);
        this.settled = true;
        this.clearStuckBlock();
        this.setDeltaMovement(Vec3.ZERO);
        this.setXRot(-90.0F);
        this.xRotO = -90.0F;
    }

    private void clearStuckBlock() {
        this.stuckBlockPos = null;
        this.stuckBlockState = null;
    }

    private void rememberStuckBlock(BlockPos pos) {
        this.stuckBlockPos = pos.immutable();
        this.stuckBlockState = this.level().getBlockState(pos);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.kind() == SpecialExplosiveItem.Kind.C4) {
            this.tickC4();
        } else if (this.kind() == SpecialExplosiveItem.Kind.CLAYMORE) {
            this.tickClaymore();
        } else {
            this.tickTm62();
        }
    }

    private void tickC4() {
        UUID attachedId = this.attachedId();
        if (attachedId != null) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                Entity attached = serverLevel.getEntity(attachedId);
                if (attached == null || !attached.isAlive()) {
                    // SW: host entity gone -> drop free (start falling)
                    this.entityData.set(ATTACHED, "");
                    this.startFalling();
                } else {
                    this.setPos(attached.getX(), attached.getY() + attached.getBbHeight(), attached.getZ());
                    this.setDeltaMovement(Vec3.ZERO);
                }
            }
        } else if (this.entityData.get(IN_GROUND) || this.settled) {
            // SW: stuck block destroyed / no support -> startFalling
            if (!this.level().isClientSide() && this.hasLostBlockSupport()) {
                this.startFalling();
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
        } else {
            this.tickProjectileMotion(true);
        }

        if (!this.level().isClientSide() && !this.entityData.get(REMOTE)) {
            int bombTick = this.entityData.get(BOMB_TICK);
            if (bombTick >= C4_COUNTDOWN) {
                this.detonate();
                return;
            }
            int remaining = C4_COUNTDOWN - bombTick;
            if (remaining > 39 && bombTick % ((20 * remaining) / C4_COUNTDOWN + 1) == 0) {
                this.playSound(Reference.id("item.c4.beep"), 1.0F, 1.0F);
            }
            if (bombTick == C4_COUNTDOWN - 39) {
                this.playSound(Reference.id("item.c4.final"), 2.0F, 1.0F);
            }
            this.entityData.set(BOMB_TICK, bombTick + 1);
        }
    }

    private void tickClaymore() {
        if (!this.entityData.get(IN_GROUND) && !this.settled) {
            this.tickMinePhysics();
        } else if (!this.level().isClientSide() && this.hasLostBlockSupport()) {
            // Support block broken -> resume gravity (SW mine physics while airborne)
            this.startFalling();
            this.tickMinePhysics();
        } else if (!this.level().isClientSide()) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.level().isClientSide()) {
            return;
        }
        if (this.tickCount >= 12000) {
            this.discard();
            return;
        }
        if (this.entityData.get(HEALTH) <= 0.0F) {
            if (this.level() instanceof ServerLevel serverLevel) {
                SpecialExplosion.explode(serverLevel, this, this.ownerEntity(), CLAYMORE_DESTROY_DAMAGE, CLAYMORE_RADIUS, SpecialExplosion.Tier.MEDIUM);
            }
            this.discard();
            return;
        }
        if (this.tickCount < 40) {
            return;
        }

        Vec3 look = this.getLookAngle();
        Vec3 center = this.position().add(look.scale(1.5D));
        AABB area = new AABB(center, center).inflate(1.25D);
        for (Entity target : this.level().getEntities(this, area, this::canTriggerClaymore)) {
            SpecialExplosion.explode((ServerLevel) this.level(), this, this.ownerEntity(), CLAYMORE_DAMAGE, CLAYMORE_RADIUS, SpecialExplosion.Tier.MEDIUM);
            this.discard();
            return;
        }
    }

    private boolean canTriggerClaymore(Entity target) {
        UUID owner = this.ownerId();
        if (!target.isAlive() || target == this || (owner != null && target.getUUID().equals(owner))) {
            return false;
        }
        if (!(target instanceof LivingEntity) && !(target instanceof VehicleEntity)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator() || player.isShiftKeyDown())) {
            return false;
        }
        if (target instanceof LivingEntity living && living.isShiftKeyDown()) {
            return false;
        }
        LivingEntity ownerEntity = this.ownerEntity();
        if (ownerEntity != null) {
            if (ownerEntity.isAlliedTo(target)) {
                return false;
            }
            if (target instanceof LivingEntity livingTarget
                    && GunnerFactionRelations.areSameFactionGunners(ownerEntity, livingTarget)) {
                return false;
            }
        }
        return true;
    }

    private void tickTm62() {
        if (!this.entityData.get(IN_GROUND) && !this.settled) {
            this.tickMinePhysics();
        } else if (!this.level().isClientSide() && this.hasLostBlockSupport()) {
            this.startFalling();
            this.tickMinePhysics();
        } else if (!this.level().isClientSide()) {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (this.level().isClientSide()) {
            return;
        }

        if (this.entityData.get(TIMED) && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.01D);
        }

        if (this.entityData.get(HEALTH) <= 0.0F || (this.entityData.get(TIMED) && this.tickCount >= TM62_FUSE_TICKS)) {
            this.detonate();
            return;
        }

        if (!this.entityData.get(TIMED) && this.tickCount >= 20 && (this.onGround() || this.isInGround())) {
            this.touchEntity();
        }
    }

    private void touchEntity() {
        try {
            AABB box = this.getBoundingBox().inflate(0.25D, 0.35D, 0.25D);
            boolean trigger = !this.level().getEntities(this, box, entity -> {
                if (entity == this || entity instanceof net.minecraft.world.entity.decoration.HangingEntity) {
                    return false;
                }
                // Prefer volume over getSize() for stability across versions
                AABB bb = entity.getBoundingBox();
                double volume = bb.getXsize() * bb.getYsize() * bb.getZsize();
                double avg = (bb.getXsize() + bb.getYsize() + bb.getZsize()) / 3.0D;
                return avg > 1.5D || volume > 1.5D || (avg > 0.9D && entity.getDeltaMovement().y < -0.35D)
                        || entity instanceof VehicleEntity;
            }).isEmpty();
            if (trigger) {
                this.detonate();
            }
        } catch (Throwable t) {
            // Never crash the server from mine pressure logic
            ttv.migami.jeg.JustEnoughGuns.LOGGER.warn("TM-62 touch check failed: {}", t.toString());
        }
    }

    private void tickMinePhysics() {
        // Claymore keeps player-facing yaw while flying/landing (do not pitch with velocity)
        if (this.kind() == SpecialExplosiveItem.Kind.CLAYMORE) {
            this.setXRot(0.0F);
            this.xRotO = 0.0F;
        }
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
        if (!this.level().noCollision(this.getBoundingBox())) {
            this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D, this.getZ());
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        float friction = 0.98F;
        if (this.onGround()) {
            BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
            friction = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
            this.entityData.set(IN_GROUND, true);
            this.settled = true;
            this.rememberStuckBlock(pos);
            if (this.kind() == SpecialExplosiveItem.Kind.CLAYMORE || this.kind() == SpecialExplosiveItem.Kind.TM_62) {
                this.setXRot(0.0F);
                this.xRotO = 0.0F;
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(friction, 0.98D, friction));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, -0.9D, 1.0D));
        }
    }

    private void tickProjectileMotion(boolean sticky) {
        if (this.entityData.get(IN_GROUND) || this.attachedId() != null) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        // Only gently face flight direction - avoid per-tick spin from noisy motion
        if (motion.lengthSqr() > 0.0025D) {
            double horizontal = motion.horizontalDistance();
            this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI)));
            this.setXRot((float) (Mth.atan2(motion.y, horizontal) * (180.0D / Math.PI)));
        }

        Vec3 position = this.position();
        Vec3 next = position.add(motion);
        HitResult hit = this.level().clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) {
            next = hit.getLocation();
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                this.level(), this, position, next,
                this.getBoundingBox().expandTowards(motion).inflate(1.0D),
                this::canStickTo,
                0.3F
        );
        if (entityHit != null) {
            hit = entityHit;
        }

        if (hit.getType() != HitResult.Type.MISS) {
            if (hit instanceof EntityHitResult entityResult) {
                Entity target = entityResult.getEntity();
                if (this.tickCount >= 2 && sticky) {
                    this.attachTo(target);
                }
            } else if (hit instanceof BlockHitResult blockHit) {
                this.onHitBlock(blockHit);
            }
            return;
        }

        double nX = this.getX() + motion.x;
        double nY = this.getY() + motion.y;
        double nZ = this.getZ() + motion.z;
        float drag = this.isInWater() ? 0.8F : 0.99F;
        this.setDeltaMovement(motion.scale(drag));
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.05D, 0.0D));
        }
        this.setPos(nX, nY, nZ);
    }

    private boolean canStickTo(Entity target) {
        if (!target.canBeHitByProjectile() || target instanceof PlacedExplosiveEntity) {
            return false;
        }
        Entity owner = this.ownerEntity();
        return owner == null || target != owner || this.tickCount > 2;
    }

    private void onHitBlock(BlockHitResult hit) {
        // Snap to contact and fully stop - SW C4 stays still once stuck
        this.setPos(hit.getLocation());
        this.setDeltaMovement(Vec3.ZERO);
        this.entityData.set(IN_GROUND, true);
        this.settled = true;
        this.rememberStuckBlock(hit.getBlockPos());

        // Floor: lay flat (xRot=0). Wall/ceiling: face outward like SW stick pose.
        switch (hit.getDirection()) {
            case UP -> {
                this.setXRot(0.0F);
                // keep current yaw from flight
            }
            case DOWN -> this.setXRot(180.0F);
            case NORTH -> {
                this.setYRot(180.0F);
                this.setXRot(90.0F);
            }
            case SOUTH -> {
                this.setYRot(0.0F);
                this.setXRot(90.0F);
            }
            case WEST -> {
                this.setYRot(90.0F);
                this.setXRot(90.0F);
            }
            case EAST -> {
                this.setYRot(-90.0F);
                this.setXRot(90.0F);
            }
        }
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    /**
     * SW C4: {@code shouldFall} - in ground and no solid collision near the entity.
     */
    private boolean shouldFall() {
        return this.level().noCollision(new AABB(this.position(), this.position()).inflate(0.06D));
    }

    /**
     * True when the stuck block was broken/changed, or the entity is floating with no support
     * (SW: lastState != current && shouldFall, plus entity-host death handled separately).
     */
    private boolean hasLostBlockSupport() {
        if (this.attachedId() != null) {
            return false;
        }
        if (!this.entityData.get(IN_GROUND) && !this.settled) {
            return false;
        }

        // Stuck block destroyed or replaced
        if (this.stuckBlockPos != null) {
            BlockState now = this.level().getBlockState(this.stuckBlockPos);
            if (now.isAir() || now.getCollisionShape(this.level(), this.stuckBlockPos).isEmpty()) {
                return true;
            }
            if (this.stuckBlockState != null && now.getBlock() != this.stuckBlockState.getBlock()) {
                return true;
            }
        }

        // No nearby collision (wall/floor gone under the entity)
        if (this.shouldFall()) {
            return true;
        }

        // Floor mines: block that affects movement gone
        if (this.kind() != SpecialExplosiveItem.Kind.C4) {
            BlockPos below = this.getBlockPosBelowThatAffectsMyMovement();
            BlockState belowState = this.level().getBlockState(below);
            if (belowState.isAir() || belowState.getCollisionShape(this.level(), below).isEmpty()) {
                return !this.onGround();
            }
        }
        return false;
    }

    /** SW C4 {@code startFalling}: detach and get a small random impulse. */
    private void startFalling() {
        this.entityData.set(IN_GROUND, false);
        this.settled = false;
        this.clearStuckBlock();
        this.setDeltaMovement(
                this.random.nextFloat() * 0.2F,
                this.random.nextFloat() * 0.2F,
                this.random.nextFloat() * 0.2F
        );
    }

    @Override
    public void move(MoverType type, Vec3 pos) {
        super.move(type, pos);
        // SW C4: external move (piston/shove) can also unstick
        if (type != MoverType.SELF
                && !this.level().isClientSide()
                && (this.entityData.get(IN_GROUND) || this.settled)
                && this.attachedId() == null
                && this.hasLostBlockSupport()) {
            this.startFalling();
        }
    }

    public void detonate() {
        detonate(false);
    }

    /**
     * @param weak if true, use reduced claymore-style destroy blast (chain/gun kill).
     */
    public void detonate(boolean weak) {
        if (this.detonating || this.isRemoved() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.detonating = true;

        Entity owner = this.ownerEntity();
        Vec3 pos = this.position();
        UUID attached = this.attachedId();
        if (attached != null) {
            Entity target = serverLevel.getEntity(attached);
            if (target != null) {
                pos = target.position();
            }
        }

        SpecialExplosiveItem.Kind kind = this.kind();
        float damage;
        double radius;
        SpecialExplosion.Tier tier;
        if (weak && kind == SpecialExplosiveItem.Kind.CLAYMORE) {
            damage = CLAYMORE_DESTROY_DAMAGE;
            radius = CLAYMORE_RADIUS;
            tier = SpecialExplosion.Tier.MEDIUM;
        } else if (kind == SpecialExplosiveItem.Kind.C4) {
            boolean remote = this.entityData.get(REMOTE);
            damage = remote ? C4_REMOTE_DAMAGE : C4_TIMED_DAMAGE;
            radius = remote ? C4_REMOTE_RADIUS : C4_TIMED_RADIUS;
            tier = SpecialExplosion.Tier.HUGE;
        } else {
            damage = switch (kind) {
                case CLAYMORE -> CLAYMORE_DAMAGE;
                case TM_62 -> TM62_DAMAGE;
                default -> C4_TIMED_DAMAGE;
            };
            radius = switch (kind) {
                case CLAYMORE -> CLAYMORE_RADIUS;
                case TM_62 -> TM62_RADIUS;
                default -> C4_TIMED_RADIUS;
            };
            tier = kind == SpecialExplosiveItem.Kind.CLAYMORE ? SpecialExplosion.Tier.MEDIUM : SpecialExplosion.Tier.HUGE;
        }

        // Remove from world BEFORE exploding so vanilla/custom blast cannot re-hurt this entity.
        this.discard();
        SpecialExplosion.explodeAt(serverLevel, pos, owner, damage, radius, tier);
    }

    @Nullable
    private LivingEntity ownerEntity() {
        UUID ownerId = this.ownerId();
        if (ownerId == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity owner = serverLevel.getEntity(ownerId);
        return owner instanceof LivingEntity living ? living : null;
    }

    /**
     * C4 beep/final use fixed-range SoundEvents (60 blocks = 50% of gun ~120).
     * Keep play volume moderate so LINEAR attenuation is audible across the range
     * (variable-range + high volume made distance falloff feel flat).
     */
    private static final float C4_BEEP_VOLUME = 0.9F;
    private static final float C4_FINAL_VOLUME = 1.15F;

    private void playSound(net.minecraft.resources.Identifier id, float volume, float pitch) {
        var holder = ModSounds.ALL.get(id);
        if (holder != null) {
            float playVolume = volume;
            if (id.getPath().contains("c4.beep")) {
                playVolume = C4_BEEP_VOLUME;
            } else if (id.getPath().contains("c4.final")) {
                playVolume = C4_FINAL_VOLUME;
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), holder.get(), SoundSource.BLOCKS, playVolume, pitch);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        UUID ownerId = this.ownerId();
        if (!this.level().isClientSide() && player.isShiftKeyDown() && ownerId != null && player.getUUID().equals(ownerId)) {
            ItemStack stack = this.pickupStack();
            if (!player.getAbilities().instabuild) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
            player.sendSystemMessage(Component.translatable("message.jeg.explosive.recovered", stack.getHoverName()));
            this.discard();
        }
        return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (amount <= 0.0F || this.isRemoved() || this.detonating) {
            return false;
        }
        // Damage multipliers; explosions/gunfire can destroy mines
        float applied = switch (this.kind()) {
            case CLAYMORE -> amount * 0.2F;
            case TM_62 -> amount * 0.33F;
            case C4 -> amount * 0.5F;
        };
        boolean fromExplosion = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        if (fromExplosion) {
            applied = Math.max(applied, this.kind() == SpecialExplosiveItem.Kind.TM_62 ? 25.0F : 15.0F);
        }
        float health = this.entityData.get(HEALTH) - applied;
        this.entityData.set(HEALTH, health);
        if (health <= 0.0F) {
            // Chain/gun kill: one-shot detonate with re-entry guard (discard before blast)
            this.detonate(fromExplosion && this.kind() == SpecialExplosiveItem.Kind.CLAYMORE);
        }
        return true;
    }

    /** Called by SpecialExplosion so mines take blast damage even though they are not LivingEntity. */
    public void hurtFromBlast(float amount, @Nullable Entity attacker) {
        if (amount <= 0.0F || this.isRemoved() || this.level().isClientSide() || this.detonating) {
            return;
        }
        float applied = switch (this.kind()) {
            case CLAYMORE -> Math.max(amount * 0.2F, 15.0F);
            case TM_62 -> Math.max(amount * 0.33F, 25.0F);
            case C4 -> Math.max(amount * 0.5F, 20.0F);
        };
        float health = this.entityData.get(HEALTH) - applied;
        this.entityData.set(HEALTH, health);
        if (health <= 0.0F) {
            // Chain reaction: claymore uses weak destroy blast; others full detonate once
            this.detonate(this.kind() == SpecialExplosiveItem.Kind.CLAYMORE);
        }
    }

    public ItemStack pickupStack() {
        return switch (this.kind()) {
            case C4 -> {
                ItemStack stack = new ItemStack(ModItems.C4_BOMB.get());
                stack.set(ModDataComponents.C4_REMOTE.get(), this.entityData.get(REMOTE));
                yield stack;
            }
            case CLAYMORE -> new ItemStack(ModItems.CLAYMORE_MINE.get());
            case TM_62 -> new ItemStack(ModItems.TM_62.get());
        };
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(KIND, input.getIntOr("Kind", 0));
        this.entityData.set(REMOTE, input.getBooleanOr("Remote", false));
        this.entityData.set(TIMED, input.getBooleanOr("Timed", false));
        this.entityData.set(BOMB_TICK, input.getIntOr("BombTick", 0));
        this.entityData.set(HEALTH, input.getFloatOr("Health", 20.0F));
        this.entityData.set(IN_GROUND, input.getBooleanOr("InGround", false));
        this.settled = input.getBooleanOr("Settled", false);
        input.read("Owner", net.minecraft.core.UUIDUtil.CODEC).ifPresent(id -> this.entityData.set(OWNER, id.toString()));
        input.read("Attached", net.minecraft.core.UUIDUtil.CODEC).ifPresent(id -> this.entityData.set(ATTACHED, id.toString()));
        int stuckX = input.getIntOr("StuckX", Integer.MIN_VALUE);
        if (stuckX != Integer.MIN_VALUE) {
            this.stuckBlockPos = new BlockPos(stuckX, input.getIntOr("StuckY", 0), input.getIntOr("StuckZ", 0));
            this.stuckBlockState = this.level().getBlockState(this.stuckBlockPos);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Kind", this.entityData.get(KIND));
        output.putBoolean("Remote", this.entityData.get(REMOTE));
        output.putBoolean("Timed", this.entityData.get(TIMED));
        output.putInt("BombTick", this.entityData.get(BOMB_TICK));
        output.putFloat("Health", this.entityData.get(HEALTH));
        output.putBoolean("InGround", this.entityData.get(IN_GROUND));
        output.putBoolean("Settled", this.settled);
        UUID ownerSave = this.ownerId(); if (ownerSave != null) { output.store("Owner", net.minecraft.core.UUIDUtil.CODEC, ownerSave); }
        UUID attachedSave = this.attachedId(); if (attachedSave != null) { output.store("Attached", net.minecraft.core.UUIDUtil.CODEC, attachedSave); }
        if (this.stuckBlockPos != null) {
            output.putInt("StuckX", this.stuckBlockPos.getX());
            output.putInt("StuckY", this.stuckBlockPos.getY());
            output.putInt("StuckZ", this.stuckBlockPos.getZ());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
