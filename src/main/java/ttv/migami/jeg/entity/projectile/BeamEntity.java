package ttv.migami.jeg.entity.projectile;

import net.minecraft.nbt.ValueInput;
import net.minecraft.nbt.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.math.ExtendedEntityRayTraceResult;
import java.util.function.Predicate;

public class BeamEntity extends ProjectileEntity {
	private static final Predicate<BlockState> IGNORE_LEAVES = input -> input != null && Config.COMMON.gameplay.ignoreLeaves.get() && input.getBlock() instanceof LeavesBlock;

    protected float distance = -1f;
    protected float laserPitch = 0.0f;
    protected float laserYaw = 0.0f;
    protected short maxTicks = 0;
	protected Vec3 startVec = new Vec3(0 ,0 ,0);
	protected Vec3 endVec   = new Vec3(0 ,0 ,0);

    public static final EntityDataAccessor<Float> START_X = SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> START_Y = SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> START_Z = SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DISTANCE = SynchedEntityData.defineId(BeamEntity.class, EntityDataSerializers.FLOAT);

	public BeamEntity(EntityType<? extends Entity> entityType, Level worldIn) {
		super(entityType, worldIn);
		life = 20;
		maxTicks = (short) life;
	}

    public BeamEntity(EntityType<? extends Entity> entityType, Level worldIn,
                      LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {
        super(entityType, worldIn, shooter, weapon, item, modifiedGun);
        //this.startVec = new Vec3(this.getX(), this.getY(), this.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START_X, 0f);
        builder.define(START_Y, 0f);
        builder.define(START_Z, 0f);
        builder.define(END_X, 0f);
        builder.define(END_Y, 0f);
        builder.define(END_Z, 0f);
        builder.define(DISTANCE, 0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel && this.tickCount >= this.life) {
            if (this.isAlive()) {
                this.onExpired();
            }
            this.remove(RemovalReason.KILLED);
            return;
        }
        if (!this.isRemoved()) {
            this.trace();
        }
    }

    public float getDistance() {
        return getEntityData().get(DISTANCE);
    }
    public Vec3 getStartVec() {
        var x = this.getEntityData().get(START_X);
        var y = this.getEntityData().get(START_Y);
        var z = this.getEntityData().get(START_Z);
        return new Vec3(x, y, z);
    }
    public Vec3 getEndVec(){
        var x= this.getEntityData().get(END_X);
        var y= this.getEntityData().get(END_Y);
        var z= this.getEntityData().get(END_Z);
        return new Vec3(x, y, z);
    }

    public void trace() {
        if (shooter == null || level().isClientSide)
            return;

        var startVec = new Vec3(this.getX(), this.getY(), this.getZ());
        var endVec = startVec.add(this.getDeltaMovement());

        HitResult raytraceresult = rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this), IGNORE_LEAVES);
        if (raytraceresult.getType() != HitResult.Type.MISS) {
            endVec = raytraceresult.getLocation();
        }

        ProjectileEntity.EntityResult entityResult = this.findEntityOnPath(startVec, endVec);
        if (entityResult != null) {
            raytraceresult = new ExtendedEntityRayTraceResult(entityResult);
            if (((EntityHitResult) raytraceresult).getEntity() instanceof Player player) {
                if (this.shooter instanceof Player && !((Player) this.shooter).canHarmPlayer(player)) {
                    raytraceresult = null;
                }
            }
        }

        if (raytraceresult != null) {
            var hitVec = raytraceresult.getLocation();
            distance = (float) startVec.distanceTo(hitVec);
        }

        // cache and sync
        this.startVec = startVec;
        this.endVec = endVec;
        this.entityData.set(START_X, (float) startVec.x);
        this.entityData.set(START_Y, (float) startVec.y);
        this.entityData.set(START_Z, (float) startVec.z);
        this.entityData.set(END_X, (float) endVec.x);
        this.entityData.set(END_Y, (float) endVec.y);
        this.entityData.set(END_Z, (float) endVec.z);
        this.laserYaw = this.getYRot();
        if (distance <= 0) {
            distance = (float) this.projectile.getSpeed();
        }
        this.entityData.set(DISTANCE, distance);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        double sx = input.getDoubleOr("StartX", this.getX());
        double sy = input.getDoubleOr("StartY", this.getY());
        double sz = input.getDoubleOr("StartZ", this.getZ());
        this.startVec = new Vec3(sx, sy, sz);

        double ex = input.getDoubleOr("EndX", this.getX());
        double ey = input.getDoubleOr("EndY", this.getY());
        double ez = input.getDoubleOr("EndZ", this.getZ());
        this.endVec = new Vec3(ex, ey, ez);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("StartX", startVec.x);
        output.putDouble("StartY", startVec.y);
        output.putDouble("StartZ", startVec.z);
        output.putDouble("EndX", endVec.x);
        output.putDouble("EndY", endVec.y);
        output.putDouble("EndZ", endVec.z);
    }
}