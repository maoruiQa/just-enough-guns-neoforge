package ttv.migami.jeg.vehicle.entity.base;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.gun.BallisticProtection;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;
import ttv.migami.jeg.vehicle.client.VehicleClientState;
import ttv.migami.jeg.vehicle.data.DefaultVehicleData;
import ttv.migami.jeg.vehicle.data.VehiclePartArmorProfile;
import ttv.migami.jeg.vehicle.data.VehicleData;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.data.subdata.CollisionLevel;
import ttv.migami.jeg.vehicle.data.subdata.DismountInfo;
import ttv.migami.jeg.vehicle.data.subdata.EngineInfo;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeatInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeekInfo;
import ttv.migami.jeg.vehicle.data.subdata.VehicleContainerType;
import ttv.migami.jeg.vehicle.data.subdata.VehicleWeaponInfo;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;
import ttv.migami.jeg.vehicle.projectile.VehicleDecoyEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;
import ttv.migami.jeg.vehicle.util.VehicleSoundHelper;
import ttv.migami.jeg.vehicle.util.VehicleWeaponStats;

public class VehicleEntity extends Entity implements MenuProvider, GeoEntity {
    private static final double CLIENT_RESYNC_DISTANCE_SQR = 4.0D;
    private static final double CLIENT_RESYNC_HORIZONTAL_MOTION_DELTA_SQR = 0.16D;
    private static final float CLIENT_RESYNC_YAW_DELTA = 12.0F;
    private static final float CLIENT_RESYNC_PITCH_DELTA = 12.0F;
    private static final int DRIVER_STATE_SYNC_INTERVAL = 5;
    private static final int DISMOUNT_LERP_SUPPRESSION_TICKS = 30;

    private static final EntityDataAccessor<String> DATA_VEHICLE_ID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RIFLE_AMMO = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SELECTED_WEAPON = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SELECTED_WEAPON_AMMO = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SELECTED_WEAPON_RESERVE_AMMO = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SELECTED_WEAPON_RELOADING = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SELECTED_WEAPON_RELOAD_TICKS = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FLARE_AMMO = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DECOY_COOLDOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_MISSILE_LOCKED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_MISSILE_LOCK_TARGET = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LEFT_WHEEL_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RIGHT_WHEEL_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENGINE_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TURRET_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_TURRET_YAW = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TURRET_PITCH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final ResourceLocation RIFLE_AMMO = Reference.id("rifle_ammo");
    private static final ResourceLocation FLARE_AMMO = Reference.id("flare");
    private static final int DECOY_COOLDOWN_TICKS = 60;
    private static final int LAND_DECOY_COOLDOWN_TICKS = 500;
    private static final int REDSTONE_ENERGY_VALUE = 20;
    private static final int ENERGY_RECHARGE_INTERVAL = 20;
    private static final int LOW_HEALTH_DECAY_INTERVAL = 40;
    private static final int RAM_DAMAGE_COOLDOWN_TICKS = 10;
    private static final String TAG_VEHICLE_ID = "VehicleDataId";
    private static final String TAG_HEALTH = "Health";
    private static final String TAG_ENERGY = "Energy";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_REPAIR_COOLDOWN = "RepairCooldown";
    private static final String TAG_SELECTED_WEAPON = "SelectedWeapon";
    private static final String TAG_LOADED_WEAPON_AMMO = "LoadedWeaponAmmo";
    private static final String TAG_LOADED_WEAPON_SLOT = "WeaponSlot";
    private static final String TAG_LOADED_WEAPON_COUNT = "LoadedAmmo";
    private static final String TAG_DECOY_COOLDOWN = "DecoyCooldown";
    private static final String TAG_SEAT_ASSIGNMENTS = "SeatAssignments";
    private static final String TAG_SEAT_PASSENGER = "Passenger";
    private static final String TAG_SEAT_INDEX = "Seat";
    private static final String TAG_LEFT_WHEEL_HEALTH = "LeftWheelHealth";
    private static final String TAG_RIGHT_WHEEL_HEALTH = "RightWheelHealth";
    private static final String TAG_ENGINE_HEALTH = "EngineHealth";
    private static final String TAG_TURRET_HEALTH = "TurretHealth";
    private static final String GECKO_CONTROLLER = "Vehicle";
    private static final Map<String, String> VEHICLE_IDLE_ANIMATIONS = Map.of(
            "lav150", "animation.lav_150.idle",
            "speedboat", "animation.speedboat.idle",
            "laser_tower", "animation.lt.idle",
            "waveforce_tower", "animation.waveforce_tower.idle"
    );
    private static final float PART_MAX_HEALTH = 10.0F;
    private static final float REPAIR_KIT_HULL_REPAIR = 12.0F;
    private static final float REPAIR_KIT_PART_REPAIR = 5.0F;
    private static final float LOW_HEALTH_DECAY_THRESHOLD = 0.15F;
    private static final float LOW_HEALTH_DECAY_DAMAGE = 0.25F;
    private static final double RAM_DAMAGE_MIN_SPEED = 0.18D;
    private static final double DEFAULT_SEEK_RANGE = 64.0D;
    private static final double DEFAULT_SEEK_MIN_DOT = 0.985D;
    private static final double GRAVITY = 0.08D;
    private static final double BMP2_TURRET_POS_Y = 2.25D;
    private static final double BMP2_TURRET_POS_Z = -0.703125D;
    private static final double BMP2_BARREL_POS_X = 0.3625D;
    private static final double BMP2_BARREL_POS_Y = 0.293125D;
    private static final double BMP2_BARREL_POS_Z = 1.18095D;
    private static final double LAV150_ROTATE_OFFSET_HEIGHT = 2.2D;
    private static final double LAV150_TURRET_POS_Y = 2.4003D;
    private static final double LAV150_BARREL_POS_X = 0.0234375D;
    private static final double LAV150_BARREL_POS_Y = 0.33795D;
    private static final double LAV150_BARREL_POS_Z = 0.825D;
    private static final ResourceLocation BMP2_ID = Reference.id("bmp2");
    private static final ResourceLocation LAV150_ID = Reference.id("lav150");

    private final SimpleContainer inventory = new SimpleContainer(VehicleMenu.MAX_VEHICLE_SLOT_COUNT);
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Integer> seatAssignments = new HashMap<>();
    private final Map<Integer, Integer> loadedAmmoByWeaponSlot = new HashMap<>();
    private VehicleInput input = VehicleInput.EMPTY;
    private int repairCooldown;
    private int fireCooldown;
    private int activeReloadWeaponSlot = -1;
    private int activeReloadTicks;
    private int decoyCooldown;
    private int energyRechargeTick;
    private int ramDamageCooldown;
    private int weaponControllerId = -1;
    private boolean weaponFireInput;
    private int seekControllerId = -1;
    private boolean seekInput;
    private double wheelSteering;
    private double enginePower;
    private float turretYawO;
    private float turretPitchO;
    private int dismountLerpSuppressionTicks;
    private float leftWheelHealth = PART_MAX_HEALTH;
    private float rightWheelHealth = PART_MAX_HEALTH;
    private float engineHealth = PART_MAX_HEALTH;
    private float turretHealth = PART_MAX_HEALTH;

    public VehicleEntity(EntityType<? extends VehicleEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                GECKO_CONTROLLER,
                0,
                state -> {
                    state.setAndContinue(RawAnimation.begin().thenLoop(this.idleAnimationName()));
                    return PlayState.CONTINUE;
                }
        ));
    }

    private String idleAnimationName() {
        String path = this.vehicleDataId().getPath();
        return VEHICLE_IDLE_ANIMATIONS.getOrDefault(path, "idle");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geckoCache;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VEHICLE_ID, DefaultVehicleData.TEST_WHEEL.id().toString());
        builder.define(DATA_HEALTH, DefaultVehicleData.TEST_WHEEL.maxHealth());
        builder.define(DATA_ENERGY, DefaultVehicleData.TEST_WHEEL.maxEnergy());
        builder.define(DATA_RIFLE_AMMO, 0);
        builder.define(DATA_SELECTED_WEAPON, 0);
        builder.define(DATA_SELECTED_WEAPON_AMMO, 0);
        builder.define(DATA_SELECTED_WEAPON_RESERVE_AMMO, 0);
        builder.define(DATA_SELECTED_WEAPON_RELOADING, false);
        builder.define(DATA_SELECTED_WEAPON_RELOAD_TICKS, 0);
        builder.define(DATA_FLARE_AMMO, 0);
        builder.define(DATA_DECOY_COOLDOWN, 0);
        builder.define(DATA_MISSILE_LOCKED, false);
        builder.define(DATA_MISSILE_LOCK_TARGET, -1);
        builder.define(DATA_LEFT_WHEEL_DAMAGED, false);
        builder.define(DATA_RIGHT_WHEEL_DAMAGED, false);
        builder.define(DATA_ENGINE_DAMAGED, false);
        builder.define(DATA_TURRET_DAMAGED, false);
        builder.define(DATA_TURRET_YAW, 0.0F);
        builder.define(DATA_TURRET_PITCH, 0.0F);
    }

    public VehicleData vehicleData() {
        return VehicleDataManager.get(ResourceLocation.parse(this.entityData.get(DATA_VEHICLE_ID)));
    }

    public ResourceLocation vehicleDataId() {
        return this.vehicleData().id();
    }

    protected void setVehicleData(ResourceLocation id) {
        VehicleData data = VehicleDataManager.get(id);
        this.entityData.set(DATA_VEHICLE_ID, data.id().toString());
        this.entityData.set(DATA_HEALTH, data.defaults().maxHealth());
        this.entityData.set(DATA_ENERGY, data.defaults().maxEnergy());
    }

    public float vehicleHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public float maxVehicleHealth() {
        return this.vehicleData().defaults().maxHealth();
    }

    public int vehicleEnergy() {
        return this.entityData.get(DATA_ENERGY);
    }

    public int maxVehicleEnergy() {
        return this.vehicleData().defaults().maxEnergy();
    }

    public boolean consumeEnergy(int amount) {
        if (amount <= 0) {
            return true;
        }
        int current = this.vehicleEnergy();
        if (current < amount) {
            return false;
        }
        this.entityData.set(DATA_ENERGY, current - amount);
        return true;
    }

    public boolean addEnergy(int amount) {
        if (amount <= 0 || this.maxVehicleEnergy() <= 0) {
            return false;
        }
        int current = this.vehicleEnergy();
        if (current >= this.maxVehicleEnergy()) {
            return false;
        }
        this.entityData.set(DATA_ENERGY, Math.min(this.maxVehicleEnergy(), current + amount));
        return true;
    }

    public int vehicleRifleAmmo() {
        return this.entityData.get(DATA_RIFLE_AMMO);
    }

    public int selectedVehicleWeaponAmmo() {
        return this.hasVehicleWeapons() ? this.entityData.get(DATA_SELECTED_WEAPON_AMMO) : 0;
    }

    public int selectedVehicleWeaponReserveAmmo() {
        return this.hasVehicleWeapons() ? this.entityData.get(DATA_SELECTED_WEAPON_RESERVE_AMMO) : 0;
    }

    public boolean selectedVehicleWeaponReloading() {
        return this.hasVehicleWeapons() && this.entityData.get(DATA_SELECTED_WEAPON_RELOADING);
    }

    public int selectedVehicleWeaponReloadTicks() {
        return this.hasVehicleWeapons() ? this.entityData.get(DATA_SELECTED_WEAPON_RELOAD_TICKS) : 0;
    }

    public int vehicleFlareAmmo() {
        if (this.hasBuiltInDecoy()) {
            return this.decoyCooldown == 0 ? 1 : 0;
        }
        return this.entityData.get(DATA_FLARE_AMMO);
    }

    public int vehicleDecoyCooldown() {
        return this.entityData.get(DATA_DECOY_COOLDOWN);
    }

    @Nullable
    public ResourceLocation selectedVehicleWeaponId() {
        VehicleWeaponInfo weapon = this.selectedWeapon();
        return weapon == null ? null : weapon.weaponId();
    }

    public int selectedVehicleWeaponIndex() {
        var weapons = this.vehicleData().defaults().weapons();
        if (weapons.isEmpty()) {
            return -1;
        }
        return Mth.clamp(this.entityData.get(DATA_SELECTED_WEAPON), 0, weapons.size() - 1);
    }

    public boolean isSelectedVehicleWeaponGuided() {
        VehicleWeaponInfo weapon = this.selectedWeapon();
        return weapon != null && weapon.guided();
    }

    public boolean hasMissileLock() {
        return this.entityData.get(DATA_MISSILE_LOCKED);
    }

    public boolean isLeftWheelDamaged() {
        return this.entityData.get(DATA_LEFT_WHEEL_DAMAGED);
    }

    public boolean isRightWheelDamaged() {
        return this.entityData.get(DATA_RIGHT_WHEEL_DAMAGED);
    }

    public boolean isEngineDamaged() {
        return this.entityData.get(DATA_ENGINE_DAMAGED);
    }

    public boolean isTurretDamaged() {
        return this.entityData.get(DATA_TURRET_DAMAGED);
    }

    public float turretYaw() {
        return this.entityData.get(DATA_TURRET_YAW);
    }

    public float turretPitch() {
        return this.entityData.get(DATA_TURRET_PITCH);
    }

    public float turretYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.turretYawO, this.turretYaw());
    }

    public float turretPitch(float partialTick) {
        return Mth.lerp(partialTick, this.turretPitchO, this.turretPitch());
    }

    public boolean hasBuiltInDecoy() {
        return this.vehicleData().defaults().hasDecoy();
    }

    @Override
    public float maxUpStep() {
        return this.vehicleData().defaults().upStep();
    }

    public boolean isFreeLookInputDown() {
        return this.vehicleData().defaults().allowFreeCam() && this.input.freeLook();
    }

    public SimpleContainer vehicleInventory() {
        return this.inventory;
    }

    public boolean hasVehicleWeapons() {
        return !this.vehicleData().defaults().weapons().isEmpty();
    }

    public void processInput(ServerPlayer player, VehicleInput input) {
        if (player.getVehicle() != this) {
            return;
        }
        if (input.switchWeapon() && this.selectWeaponFor(player, 1)) {
            this.weaponControllerId = player.getId();
        }
        if (input.previousWeapon() && this.selectWeaponFor(player, -1)) {
            this.weaponControllerId = player.getId();
        }
        if (input.weaponSlot() >= 0 && input.weaponSlot() < this.vehicleData().defaults().weapons().size() && this.selectWeaponSlot(player, input.weaponSlot())) {
            this.weaponControllerId = player.getId();
        }
        VehicleWeaponInfo selectedWeapon = this.selectedWeapon();
        if (selectedWeapon != null && !this.isFreeLookInput(input) && this.canUseSelectedWeapon(player, selectedWeapon)) {
            this.entityData.set(DATA_TURRET_YAW, this.turretYawFromPlayer(player));
            this.entityData.set(DATA_TURRET_PITCH, this.weaponPitch(player));
            if (input.reload()) {
                this.startWeaponReload();
            }
        }
        if (input.deployDecoy()) {
            this.tryDeployDecoy(player);
        }
        if (input.seekTarget()) {
            this.seekControllerId = player.getId();
            this.seekInput = true;
        } else if (this.seekControllerId == player.getId()) {
            this.seekInput = false;
        }
        if (input.fire()) {
            this.weaponControllerId = player.getId();
            this.weaponFireInput = true;
        } else if (this.weaponControllerId == player.getId()) {
            this.weaponFireInput = false;
        }
        if (player == this.getControllingPassenger()) {
            this.input = input;
        }
    }

    public void processClientInput(Player player, VehicleInput input) {
        if (!this.level().isClientSide || player.getVehicle() != this) {
            return;
        }
        VehicleWeaponInfo selectedWeapon = this.selectedWeapon();
        if (selectedWeapon != null && !this.isFreeLookInput(input) && this.canUseSelectedWeapon(player, selectedWeapon)) {
            this.entityData.set(DATA_TURRET_YAW, this.turretYawFromPlayer(player));
            this.entityData.set(DATA_TURRET_PITCH, this.weaponPitch(player));
        }
        if (player == this.getControllingPassenger()) {
            this.input = input;
        }
    }

    private boolean isFreeLookInput(VehicleInput input) {
        return this.vehicleData().defaults().allowFreeCam() && input.freeLook();
    }

    private float turretYawFromPlayer(LivingEntity player) {
        return Mth.wrapDegrees(this.getYRot() - player.getYRot());
    }

    public void changeSeat(ServerPlayer player) {
        if (player.getVehicle() != this) {
            return;
        }
        int seatCount = this.vehicleData().defaults().seats().size();
        if (seatCount < 2) {
            return;
        }
        int fallbackIndex = this.getPassengers().indexOf(player);
        int currentSeat = this.seatIndexForPassenger(player, fallbackIndex);
        for (int offset = 1; offset < seatCount; offset++) {
            int nextSeat = (currentSeat + offset) % seatCount;
            if (!this.isSeatOccupied(nextSeat, player)) {
                this.seatAssignments.put(player.getUUID(), nextSeat);
                this.input = VehicleInput.EMPTY;
                this.syncSeatAssignments();
                return;
            }
        }
    }

    @Override
    public void tick() {
        this.turretYawO = this.turretYaw();
        this.turretPitchO = this.turretPitch();
        super.tick();
        if (this.level().isClientSide && this.dismountLerpSuppressionTicks > 0) {
            this.dismountLerpSuppressionTicks--;
        }
        this.applyPassengerYaw();
        if (!this.level().isClientSide) {
            this.clearStaleDriverInput();
            this.tickServerMovement();
            this.tickRammingDamage();
            this.tickMissileLock();
            this.tickWeaponReload();
            this.tickServerWeapon();
            this.tickDecoyCooldown();
            this.tickInventoryEnergyRecharge();
            this.tickAutoRepair();
            this.tickDriverStateSync();
            this.entityData.set(DATA_RIFLE_AMMO, this.countRifleAmmo());
            this.syncSelectedWeaponAmmoState();
            this.entityData.set(DATA_FLARE_AMMO, this.countAmmo(FLARE_AMMO));
            this.entityData.set(DATA_DECOY_COOLDOWN, this.decoyCooldown);
        } else if (this.shouldRunClientPrediction()) {
            this.tickClientPredictedLandMovement();
        }
    }

    private void tickDriverStateSync() {
        if (this.tickCount % DRIVER_STATE_SYNC_INTERVAL != 0 || this.vehicleData().defaults().vehicleType() != VehicleType.LAND) {
            return;
        }
        if (this.getControllingPassenger() instanceof ServerPlayer player) {
            NetworkHandler.sendVehicleState(player, this);
        }
    }

    private boolean shouldRunClientPrediction() {
        return this.isControlledByLocalInstance()
                && VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == this.getId();
    }

    private void clearStaleDriverInput() {
        if (this.getControllingPassenger() == null) {
            this.clearControlState(false);
        }
    }

    private void clearControlState(boolean stopHorizontalMotion) {
        this.input = VehicleInput.EMPTY;
        this.weaponFireInput = false;
        this.seekInput = false;
        this.weaponControllerId = -1;
        this.seekControllerId = -1;
        this.enginePower = 0.0D;
        this.wheelSteering = 0.0D;
        this.cancelWeaponReload();
        if (stopHorizontalMotion) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, motion.y, 0.0D);
            this.hasImpulse = true;
        }
    }

    public void clearClientControlState() {
        this.clearControlState(true);
    }

    public void applySeatAssignments(Map<UUID, Integer> assignments) {
        this.seatAssignments.clear();
        int seatCount = this.vehicleData().defaults().seats().size();
        for (Map.Entry<UUID, Integer> assignment : assignments.entrySet()) {
            int seatIndex = assignment.getValue();
            if (seatIndex >= 0 && seatIndex < seatCount) {
                this.seatAssignments.put(assignment.getKey(), seatIndex);
            }
        }
    }

    public Map<UUID, Integer> seatAssignmentsSnapshot() {
        return new HashMap<>(this.seatAssignments);
    }

    private void syncSeatAssignments() {
        if (!this.level().isClientSide) {
            NetworkHandler.broadcastVehicleSeatAssignments(this);
        }
    }

    public void syncAuthoritativeState(double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float pitch, boolean forceApply) {
        if (!this.shouldApplyAuthoritativeState(x, y, z, motionX, motionY, motionZ, yaw, pitch, forceApply)) {
            return;
        }
        this.setPos(x, y, z);
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.hasImpulse = true;
        if (forceApply && this.level().isClientSide) {
            this.dismountLerpSuppressionTicks = DISMOUNT_LERP_SUPPRESSION_TICKS;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (this.level().isClientSide && this.dismountLerpSuppressionTicks > 0) {
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        if (this.level().isClientSide && this.dismountLerpSuppressionTicks > 0) {
            return;
        }
        super.lerpMotion(x, y, z);
    }

    private boolean shouldApplyAuthoritativeState(double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float pitch, boolean forceApply) {
        if (forceApply || !this.level().isClientSide || !this.shouldRunClientPrediction()) {
            return true;
        }
        Vec3 syncedPosition = new Vec3(x, y, z);
        Vec3 syncedMotion = new Vec3(motionX, motionY, motionZ);
        Vec3 horizontalMotionDelta = new Vec3(this.getDeltaMovement().x - motionX, 0.0D, this.getDeltaMovement().z - motionZ);
        if (this.position().distanceToSqr(syncedPosition) >= CLIENT_RESYNC_DISTANCE_SQR) {
            return true;
        }
        if (horizontalMotionDelta.lengthSqr() >= CLIENT_RESYNC_HORIZONTAL_MOTION_DELTA_SQR) {
            return true;
        }
        return Math.abs(Mth.wrapDegrees(this.getYRot() - yaw)) >= CLIENT_RESYNC_YAW_DELTA
                || Math.abs(Mth.wrapDegrees(this.getXRot() - pitch)) >= CLIENT_RESYNC_PITCH_DELTA;
    }

    private void tickServerWeapon() {
        if (!this.hasVehicleWeapons()) {
            return;
        }
        if (this.fireCooldown > 0) {
            this.fireCooldown--;
        }
        LivingEntity shooter = this.weaponController();
        if (!this.weaponFireInput || this.fireCooldown > 0 || shooter == null || this.isWeaponReloading()) {
            return;
        }
        if (this.isTurretDamaged()) {
            return;
        }
        VehicleWeaponInfo weapon = this.selectedWeapon();
        int shooterSeat = this.seatIndexForPassenger(shooter, this.getPassengers().indexOf(shooter));
        if (!weapon.usableBySeat(shooterSeat)) {
            return;
        }
        GunStats stats = VehicleWeaponStats.get(weapon.weaponId());
        if (stats == null || !this.hasEnergy(weapon.energyCost())) {
            return;
        }
        if (this.usesSharedVehicleReloadSystem()) {
            if (this.selectedWeaponLoadedAmmo() <= 0) {
                this.startWeaponReload();
                return;
            }
        } else if (!this.hasAmmo(weapon.ammoId())) {
            return;
        }
        Vec3 direction = this.weaponAimDirection(shooter);
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }
        if (!this.consumeEnergy(weapon.energyCost())) {
            return;
        }
        if (this.usesSharedVehicleReloadSystem()) {
            int slot = this.selectedVehicleWeaponIndex();
            this.setWeaponLoadedAmmo(slot, this.weaponLoadedAmmo(slot) - 1);
            this.syncSelectedWeaponAmmoState();
        } else if (!this.consumeAmmo(weapon.ammoId())) {
            return;
        }
        this.playWeaponFireSound(weapon, stats);
        if (weapon.guided()) {
            this.launchMissile(shooter, direction, stats);
            return;
        }
        Vec3 muzzle = this.weaponMuzzlePosition(weapon, direction, 1.15D, 0.9D);
        BulletEntity bullet = new BulletEntity(this.level(), shooter, stats, direction.scale(stats.projectileSpeed()));
        bullet.initialisePosition(muzzle);
        this.level().addFreshEntity(bullet);
        if (this.level() instanceof ServerLevel serverLevel) {
            bullet.sendTrailToClients(serverLevel);
        }
        this.fireCooldown = Math.max(1, stats.fireDelay());
    }

    private void playWeaponFireSound(VehicleWeaponInfo weapon, GunStats stats) {
        SoundEvent sound = VehicleSoundHelper.fireSound(this, weapon, stats);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.PLAYERS, 7.5F, 1.0F);
    }

    private void playWeaponReloadSound(VehicleWeaponInfo weapon, @Nullable GunStats stats, boolean start) {
        if (stats == null) {
            return;
        }
        var sound = start ? stats.reloadStartSoundEvent() : stats.reloadEndSoundEvent().or(() -> stats.reloadLoadSoundEvent());
        sound.ifPresent(value -> this.level().playSound(null, this.getX(), this.getY(), this.getZ(), value, SoundSource.PLAYERS, 2.0F, 1.0F));
    }

    private Vec3 weaponMuzzlePosition(VehicleWeaponInfo weapon, Vec3 direction, double fallbackForwardOffset, double fallbackHeight) {
        Vec3 articulatedMuzzle = this.articulatedWeaponMuzzlePosition(weapon);
        if (articulatedMuzzle != null) {
            return articulatedMuzzle;
        }
        if (weapon.hasMuzzle()) {
            Vec3 aim = direction.normalize();
            Vec3 side = new Vec3(aim.z, 0.0D, -aim.x);
            if (side.lengthSqr() < 1.0E-4D) {
                side = this.rotateLocalOffset(1.0D, 0.0D, 0.0D);
            } else {
                side = side.normalize();
            }
            return this.position()
                    .add(0.0D, weapon.muzzleY(), 0.0D)
                    .add(side.scale(weapon.muzzleX()))
                    .add(aim.scale(weapon.muzzleZ()));
        }
        return this.position().add(0.0D, fallbackHeight, 0.0D).add(direction.scale(fallbackForwardOffset));
    }

    @Nullable
    private Vec3 articulatedWeaponMuzzlePosition(VehicleWeaponInfo weapon) {
        if (!this.usesArticulatedTurretMuzzle(weapon)) {
            return null;
        }
        if (this.vehicleDataId().equals(BMP2_ID)) {
            return this.bmp2BarrelPosition(
                    weapon.muzzleX() - BMP2_BARREL_POS_X,
                    weapon.muzzleY() - BMP2_TURRET_POS_Y - BMP2_BARREL_POS_Y,
                    weapon.muzzleZ() - BMP2_TURRET_POS_Z - BMP2_BARREL_POS_Z,
                    1.0F
            );
        }
        return this.lav150BarrelPosition(
                weapon.muzzleX() - LAV150_BARREL_POS_X,
                weapon.muzzleY() - LAV150_TURRET_POS_Y - LAV150_BARREL_POS_Y,
                weapon.muzzleZ() - LAV150_BARREL_POS_Z,
                1.0F
        );
    }

    private void tickMissileLock() {
        if (!this.hasVehicleWeapons()) {
            this.entityData.set(DATA_MISSILE_LOCKED, false);
            this.entityData.set(DATA_MISSILE_LOCK_TARGET, -1);
            return;
        }
        Entity target = null;
        VehicleWeaponInfo weapon = this.selectedWeapon();
        LivingEntity shooter = this.seekInput ? this.seekController() : null;
        if (shooter != null && weapon.guided()) {
            Vec3 direction = shooter.getViewVector(1.0F).normalize();
            if (direction.lengthSqr() >= 1.0E-4D) {
                target = this.findLookTarget(shooter, direction, this.seekRange(), this.seekMinDot());
            }
        }
        this.entityData.set(DATA_MISSILE_LOCKED, target != null);
        this.entityData.set(DATA_MISSILE_LOCK_TARGET, target == null ? -1 : target.getId());
        if (this.shouldWarnSeekTarget() && target instanceof ServerPlayer lockedPlayer && this.tickCount % 20 == 0) {
            this.warnLockedPlayer(lockedPlayer);
        }
    }

    private void warnLockedPlayer(ServerPlayer lockedPlayer) {
        lockedPlayer.displayClientMessage(Component.translatable("message.jeg.vehicle.lock_warning"), true);
        lockedPlayer.level().playSound(
                null,
                lockedPlayer.getX(),
                lockedPlayer.getY(),
                lockedPlayer.getZ(),
                VehicleSoundHelper.lockWarning(),
                SoundSource.PLAYERS,
                0.8F,
                1.7F
        );
    }

    private void launchMissile(LivingEntity shooter, Vec3 direction, GunStats stats) {
        VehicleWeaponInfo weapon = this.selectedWeapon();
        Vec3 muzzle = this.weaponMuzzlePosition(weapon, direction, 1.25D, 0.95D);
        Vec3 velocity = direction.scale(0.72D).add(this.getDeltaMovement().scale(0.15D));
        Entity target = this.seekInput ? this.findLookTarget(shooter, direction, this.seekRange(), this.seekMinDot()) : null;
        this.level().addFreshEntity(new VehicleMissileEntity(this.level(), shooter, target, muzzle, velocity));
        this.fireCooldown = Math.max(1, stats.fireDelay());
    }

    private Vec3 weaponAimDirection(LivingEntity shooter) {
        Vec3 articulatedAim = this.articulatedWeaponAimDirection(shooter);
        if (articulatedAim != null) {
            return articulatedAim;
        }
        return Vec3.directionFromRotation(this.weaponPitch(shooter), shooter.getYRot()).normalize();
    }

    @Nullable
    private Vec3 articulatedWeaponAimDirection(LivingEntity shooter) {
        if (!this.usesArticulatedTurretAim(shooter)) {
            return null;
        }
        if (this.vehicleDataId().equals(BMP2_ID)) {
            return this.bmp2BarrelDirection(1.0F).normalize();
        }
        return this.lav150BarrelDirection(1.0F).normalize();
    }

    private float weaponPitch(LivingEntity shooter) {
        SeatInfo seat = this.seatForPassenger(shooter, this.getPassengers().indexOf(shooter));
        return Mth.clamp(shooter.getXRot(), seat.minPitch(), seat.maxPitch());
    }

    @Nullable
    private LivingEntity weaponController() {
        Entity controller = this.weaponControllerId < 0 ? this.getControllingPassenger() : this.level().getEntity(this.weaponControllerId);
        if (controller instanceof LivingEntity living && living.getVehicle() == this) {
            return living;
        }
        this.weaponControllerId = -1;
        this.weaponFireInput = false;
        return null;
    }

    @Nullable
    private LivingEntity seekController() {
        Entity controller = this.seekControllerId < 0 ? null : this.level().getEntity(this.seekControllerId);
        VehicleWeaponInfo weapon = this.selectedWeapon();
        if (weapon != null && controller instanceof LivingEntity living && living.getVehicle() == this && this.canUseSelectedWeapon(living, weapon)) {
            return living;
        }
        this.seekControllerId = -1;
        this.seekInput = false;
        return null;
    }

    @Nullable
    private Entity findLookTarget(LivingEntity shooter, Vec3 direction, double range, double minDot) {
        Vec3 eye = shooter.getEyePosition();
        Entity bestTarget = null;
        double bestScore = minDot;
        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range))) {
            if (candidate == shooter || candidate.getVehicle() == this || !candidate.isAlive()) {
                continue;
            }
            Vec3 toTarget = candidate.getEyePosition().subtract(eye);
            double distance = toTarget.length();
            if (distance <= 0.0D || distance > range) {
                continue;
            }
            double dot = toTarget.normalize().dot(direction);
            if (dot > bestScore) {
                bestScore = dot;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    private double seekRange() {
        SeekInfo seek = this.vehicleData().defaults().seek();
        return seek.range() > 0.0D ? seek.range() : DEFAULT_SEEK_RANGE;
    }

    private double seekMinDot() {
        SeekInfo seek = this.vehicleData().defaults().seek();
        return seek.angle() > 0.0D ? Math.cos(Math.toRadians(seek.angle())) : DEFAULT_SEEK_MIN_DOT;
    }

    private boolean shouldWarnSeekTarget() {
        SeekInfo seek = this.vehicleData().defaults().seek();
        return seek.warnsTarget() || seek == SeekInfo.NONE;
    }

    private void tickDecoyCooldown() {
        if (this.decoyCooldown > 0) {
            this.decoyCooldown--;
        }
    }

    private void tickRammingDamage() {
        if (this.ramDamageCooldown > 0) {
            this.ramDamageCooldown--;
            return;
        }
        CollisionLevel collisionLevel = this.vehicleData().defaults().collisionLevel();
        if (collisionLevel == CollisionLevel.NONE) {
            return;
        }
        double speed = this.getDeltaMovement().horizontalDistance();
        if (speed < RAM_DAMAGE_MIN_SPEED) {
            return;
        }
        float damage = this.ramDamageAmount(collisionLevel, speed);
        if (damage <= 0.0F) {
            return;
        }
        boolean damaged = false;
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.25D, 0.15D, 0.25D))) {
            if (!target.isAlive() || target.getVehicle() == this) {
                continue;
            }
            damaged |= target.hurt(this.damageSources().cramming(), damage);
        }
        if (damaged) {
            this.ramDamageCooldown = RAM_DAMAGE_COOLDOWN_TICKS;
        }
    }

    private float ramDamageAmount(CollisionLevel collisionLevel, double speed) {
        float baseDamage = switch (collisionLevel) {
            case LIGHT -> 3.0F;
            case MEDIUM -> 5.0F;
            case HEAVY -> 8.0F;
            case NONE -> 0.0F;
        };
        return baseDamage * (float) Mth.clamp(speed / 0.35D, 0.35D, 1.5D);
    }

    private void tryDeployDecoy(ServerPlayer player) {
        if (this.decoyCooldown > 0) {
            return;
        }
        if (!this.hasBuiltInDecoy() && !this.consumeAmmo(FLARE_AMMO)) {
            return;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        if (this.hasBuiltInDecoy() && this.vehicleData().defaults().vehicleType() == VehicleType.LAND) {
            for (int index = 0; index < 8; index++) {
                double yaw = Math.toRadians(-78.75D + 22.5D * index);
                Vec3 direction = look.yRot((float) yaw).normalize();
                Vec3 position = this.position().add(0.0D, this.getBbHeight(), 0.0D);
                Vec3 velocity = direction.scale(0.28D).add(0.0D, 0.08D, 0.0D).add(this.getDeltaMovement().scale(0.25D));
                this.level().addFreshEntity(new VehicleDecoyEntity(this.level(), position, velocity));
            }
            this.decoyCooldown = LAND_DECOY_COOLDOWN_TICKS;
            return;
        }
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 position = this.position().add(0.0D, 0.8D, 0.0D).add(side.scale(0.65D));
        Vec3 velocity = side.scale(0.16D).add(0.0D, 0.08D, 0.0D).add(this.getDeltaMovement().scale(0.25D));
        this.level().addFreshEntity(new VehicleDecoyEntity(this.level(), position, velocity));
        this.decoyCooldown = DECOY_COOLDOWN_TICKS;
    }

    private void tickInventoryEnergyRecharge() {
        if (this.maxVehicleEnergy() <= 0 || this.vehicleEnergy() >= this.maxVehicleEnergy()) {
            return;
        }
        this.energyRechargeTick++;
        if (this.energyRechargeTick < ENERGY_RECHARGE_INTERVAL) {
            return;
        }
        this.energyRechargeTick = 0;
        if (this.consumeInventoryItem(net.minecraft.world.item.Items.REDSTONE, 1)) {
            this.entityData.set(DATA_ENERGY, Math.min(this.maxVehicleEnergy(), this.vehicleEnergy() + REDSTONE_ENERGY_VALUE));
        }
    }

    private boolean consumeInventoryItem(Item item, int count) {
        int remaining = count;
        for (int slot = 0; slot < this.inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) {
                this.inventory.setItem(slot, ItemStack.EMPTY);
            }
            remaining -= removed;
        }
        if (remaining == 0) {
            this.inventory.setChanged();
            return true;
        }
        return false;
    }

    private boolean selectWeaponSlot(Player player, int slot) {
        var weapons = this.vehicleData().defaults().weapons();
        if (slot < 0 || slot >= weapons.size()) {
            return false;
        }
        int seatIndex = this.seatIndexForPassenger(player, this.getPassengers().indexOf(player));
        if (!weapons.get(slot).usableBySeat(seatIndex)) {
            return false;
        }
        if (slot == this.selectedVehicleWeaponIndex()) {
            return false;
        }
        this.cancelWeaponReload();
        this.entityData.set(DATA_SELECTED_WEAPON, slot);
        this.syncSelectedWeaponAmmoState();
        return true;
    }

    private int countRifleAmmo() {
        return this.countAmmo(RIFLE_AMMO);
    }

    private int countAmmo(ResourceLocation ammoId) {
        Item ammo = this.resolveAmmoItem(ammoId);
        if (ammo == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (stack.is(ammo)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean hasAmmo(ResourceLocation ammoId) {
        return this.countAmmo(ammoId) > 0;
    }

    private boolean usesSharedVehicleReloadSystem() {
        ResourceLocation weaponId = this.selectedVehicleWeaponId();
        if (weaponId == null) {
            return false;
        }
        GunStats stats = VehicleWeaponStats.get(weaponId);
        return stats != null && stats.usesMagazine() && !stats.isInventoryFed();
    }

    private int weaponLoadedAmmo(int slot) {
        return Math.max(0, this.loadedAmmoByWeaponSlot.getOrDefault(slot, 0));
    }

    private void setWeaponLoadedAmmo(int slot, int ammo) {
        if (ammo <= 0) {
            this.loadedAmmoByWeaponSlot.remove(slot);
        } else {
            this.loadedAmmoByWeaponSlot.put(slot, ammo);
        }
    }

    private int selectedWeaponLoadedAmmo() {
        return this.weaponLoadedAmmo(this.selectedVehicleWeaponIndex());
    }

    private int selectedWeaponReserveAmmo() {
        VehicleWeaponInfo weapon = this.selectedWeapon();
        return weapon == null ? 0 : this.countAmmo(weapon.ammoId());
    }

    private int selectedWeaponMagazineSize() {
        ResourceLocation weaponId = this.selectedVehicleWeaponId();
        GunStats stats = weaponId == null ? null : VehicleWeaponStats.get(weaponId);
        return stats == null ? 0 : Math.max(0, stats.magazineSize());
    }

    private int selectedWeaponReloadDuration() {
        ResourceLocation weaponId = this.selectedVehicleWeaponId();
        GunStats stats = weaponId == null ? null : VehicleWeaponStats.get(weaponId);
        return stats == null ? 0 : Math.max(0, stats.totalReloadTime());
    }

    private boolean isWeaponReloading() {
        return this.activeReloadWeaponSlot >= 0 && this.activeReloadTicks > 0;
    }

    private boolean canReloadSelectedWeapon() {
        int slot = this.selectedVehicleWeaponIndex();
        if (!this.usesSharedVehicleReloadSystem() || slot < 0 || slot >= this.vehicleData().defaults().weapons().size()) {
            return false;
        }
        int magazineSize = this.selectedWeaponMagazineSize();
        if (magazineSize <= 0 || this.isWeaponReloading()) {
            return false;
        }
        return this.selectedWeaponLoadedAmmo() < magazineSize && this.selectedWeaponReserveAmmo() > 0;
    }

    private void startWeaponReload() {
        if (!this.canReloadSelectedWeapon()) {
            return;
        }
        int reloadDuration = this.selectedWeaponReloadDuration();
        if (reloadDuration <= 0) {
            this.finishWeaponReload(this.selectedVehicleWeaponIndex());
            return;
        }
        this.activeReloadWeaponSlot = this.selectedVehicleWeaponIndex();
        this.activeReloadTicks = reloadDuration;
        this.syncSelectedWeaponAmmoState();
        VehicleWeaponInfo weapon = this.selectedWeapon();
        ResourceLocation weaponId = this.selectedVehicleWeaponId();
        this.playWeaponReloadSound(weapon, weaponId == null ? null : VehicleWeaponStats.get(weaponId), true);
    }

    private void cancelWeaponReload() {
        if (!this.isWeaponReloading()) {
            return;
        }
        this.activeReloadWeaponSlot = -1;
        this.activeReloadTicks = 0;
        this.syncSelectedWeaponAmmoState();
    }

    private void tickWeaponReload() {
        if (this.isWeaponReloading()) {
            if (this.activeReloadTicks > 0) {
                this.activeReloadTicks--;
            }
            if (this.activeReloadTicks <= 0) {
                int slot = this.activeReloadWeaponSlot;
                this.activeReloadWeaponSlot = -1;
                this.activeReloadTicks = 0;
                this.finishWeaponReload(slot);
                return;
            }
            this.syncSelectedWeaponAmmoState();
            return;
        }
        if (this.shouldAutoReloadSelectedWeapon()) {
            this.startWeaponReload();
        }
    }

    private boolean shouldAutoReloadSelectedWeapon() {
        if (!this.usesSharedVehicleReloadSystem()) {
            return false;
        }
        LivingEntity shooter = this.weaponController();
        if (shooter == null) {
            shooter = this.getControllingPassenger() instanceof LivingEntity living ? living : null;
        }
        VehicleWeaponInfo weapon = this.selectedWeapon();
        if (shooter == null || weapon == null || !this.canUseSelectedWeapon(shooter, weapon)) {
            return false;
        }
        return this.selectedWeaponLoadedAmmo() <= 0 && this.canReloadSelectedWeapon();
    }

    private void finishWeaponReload(int slot) {
        var weapons = this.vehicleData().defaults().weapons();
        if (slot < 0 || slot >= weapons.size()) {
            this.syncSelectedWeaponAmmoState();
            return;
        }
        VehicleWeaponInfo weapon = weapons.get(slot);
        GunStats stats = VehicleWeaponStats.get(weapon.weaponId());
        int magazineSize = stats == null ? 0 : Math.max(0, stats.magazineSize());
        int loaded = this.weaponLoadedAmmo(slot);
        int reserve = this.countAmmo(weapon.ammoId());
        int transfer = Math.min(Math.max(0, magazineSize - loaded), reserve);
        if (transfer > 0 && this.consumeAmmo(weapon.ammoId(), transfer)) {
            this.setWeaponLoadedAmmo(slot, loaded + transfer);
        }
        this.playWeaponReloadSound(weapon, stats, false);
        this.syncSelectedWeaponAmmoState();
    }

    private void syncSelectedWeaponAmmoState() {
        if (!this.hasVehicleWeapons()) {
            this.entityData.set(DATA_SELECTED_WEAPON_AMMO, 0);
            this.entityData.set(DATA_SELECTED_WEAPON_RESERVE_AMMO, 0);
            this.entityData.set(DATA_SELECTED_WEAPON_RELOADING, false);
            this.entityData.set(DATA_SELECTED_WEAPON_RELOAD_TICKS, 0);
            return;
        }
        int selectedSlot = this.selectedVehicleWeaponIndex();
        VehicleWeaponInfo weapon = this.selectedWeapon();
        if (!this.usesSharedVehicleReloadSystem()) {
            this.entityData.set(DATA_SELECTED_WEAPON_AMMO, weapon == null ? 0 : this.countAmmo(weapon.ammoId()));
            this.entityData.set(DATA_SELECTED_WEAPON_RESERVE_AMMO, 0);
            this.entityData.set(DATA_SELECTED_WEAPON_RELOADING, false);
            this.entityData.set(DATA_SELECTED_WEAPON_RELOAD_TICKS, 0);
            return;
        }
        this.entityData.set(DATA_SELECTED_WEAPON_AMMO, this.weaponLoadedAmmo(selectedSlot));
        this.entityData.set(DATA_SELECTED_WEAPON_RESERVE_AMMO, this.selectedWeaponReserveAmmo());
        this.entityData.set(DATA_SELECTED_WEAPON_RELOADING, this.isWeaponReloading() && this.activeReloadWeaponSlot == selectedSlot);
        this.entityData.set(DATA_SELECTED_WEAPON_RELOAD_TICKS, this.isWeaponReloading() && this.activeReloadWeaponSlot == selectedSlot ? this.activeReloadTicks : 0);
    }

    private boolean hasEnergy(int amount) {
        return amount <= 0 || this.vehicleEnergy() >= amount;
    }

    private boolean consumeAmmo(ResourceLocation ammoId) {
        return this.consumeAmmo(ammoId, 1);
    }

    private boolean consumeAmmo(ResourceLocation ammoId, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (this.countAmmo(ammoId) < amount) {
            return false;
        }
        Item ammo = this.resolveAmmoItem(ammoId);
        if (ammo == null) {
            return false;
        }
        int remaining = amount;
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.is(ammo)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) {
                this.inventory.setItem(slot, ItemStack.EMPTY);
            }
            remaining -= removed;
            if (remaining == 0) {
                this.inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Item resolveAmmoItem(ResourceLocation ammoId) {
        var modAmmo = ModItems.AMMO.get(ammoId);
        if (modAmmo != null) {
            return modAmmo.get();
        }
        return BuiltInRegistries.ITEM.getOptional(ammoId).orElse(null);
    }

    @Nullable
    private VehicleWeaponInfo selectedWeapon() {
        var weapons = this.vehicleData().defaults().weapons();
        if (weapons.isEmpty()) {
            return null;
        }
        int index = Mth.clamp(this.entityData.get(DATA_SELECTED_WEAPON), 0, weapons.size() - 1);
        return weapons.get(index);
    }

    private boolean selectWeaponFor(Player player, int direction) {
        var weapons = this.vehicleData().defaults().weapons();
        if (weapons.isEmpty()) {
            return false;
        }
        int seatIndex = this.seatIndexForPassenger(player, this.getPassengers().indexOf(player));
        int current = Mth.clamp(this.entityData.get(DATA_SELECTED_WEAPON), 0, weapons.size() - 1);
        int step = direction < 0 ? -1 : 1;
        for (int offset = 1; offset <= weapons.size(); offset++) {
            int next = Mth.positiveModulo(current + offset * step, weapons.size());
            if (weapons.get(next).usableBySeat(seatIndex)) {
                if (next == current) {
                    return false;
                }
                this.cancelWeaponReload();
                this.entityData.set(DATA_SELECTED_WEAPON, next);
                this.syncSelectedWeaponAmmoState();
                return true;
            }
        }
        return false;
    }

    private boolean canUseSelectedWeapon(LivingEntity passenger, VehicleWeaponInfo weapon) {
        return weapon.usableBySeat(this.seatIndexForPassenger(passenger, this.getPassengers().indexOf(passenger)));
    }

    public boolean shouldHidePassenger(Entity passenger) {
        int fallbackIndex = this.getPassengers().indexOf(passenger);
        return fallbackIndex >= 0 && this.seatForPassenger(passenger, fallbackIndex).hidePassenger();
    }

    public boolean shouldBanPassengerHand(Entity passenger) {
        int fallbackIndex = this.getPassengers().indexOf(passenger);
        return fallbackIndex >= 0 && this.seatForPassenger(passenger, fallbackIndex).banHand();
    }

    public Vec3 cameraPositionFor(Entity passenger, float partialTick) {
        int fallbackIndex = this.getPassengers().indexOf(passenger);
        if (fallbackIndex < 0) {
            return passenger.getEyePosition(partialTick);
        }
        SeatInfo seat = this.seatForPassenger(passenger, fallbackIndex);
        Vec3 offset = this.seatOffset(seat, passenger.getEyeHeight(), partialTick);
        return this.interpolatedVehiclePosition(partialTick).add(offset);
    }

    public Vec3 cameraRotationFor(Entity passenger, float partialTick) {
        int fallbackIndex = this.getPassengers().indexOf(passenger);
        if (fallbackIndex >= 0) {
            SeatInfo seat = this.seatForPassenger(passenger, fallbackIndex);
            Vec3 articulatedRotation = this.articulatedCameraRotation(seat, partialTick);
            if (articulatedRotation != null) {
                return articulatedRotation;
            }
        }
        return new Vec3(passenger.getViewYRot(partialTick), passenger.getViewXRot(partialTick), 0.0D);
    }

    @Nullable
    private Vec3 articulatedCameraRotation(SeatInfo seat, float partialTick) {
        if (!this.usesArticulatedSeatTransform(seat)) {
            return null;
        }
        if (this.vehicleDataId().equals(BMP2_ID)) {
            Vec3 barrel = this.bmp2BarrelDirection(partialTick).normalize();
            return new Vec3(this.yRotFromVector(barrel), this.xRotFromVector(barrel), 0.0D);
        }
        Vec3 barrel = this.lav150BarrelDirection(partialTick).normalize();
        return new Vec3(this.yRotFromVector(barrel), this.xRotFromVector(barrel), 0.0D);
    }

    private void tickAutoRepair() {
        if (this.tickLowHealthDecay()) {
            return;
        }
        if (this.repairCooldown > 0) {
            this.repairCooldown--;
            return;
        }
        float repair = this.vehicleData().defaults().autoRepairPerTick();
        if (repair <= 0.0F) {
            return;
        }
        boolean repaired = false;
        if (this.vehicleHealth() < this.maxVehicleHealth()) {
            this.entityData.set(DATA_HEALTH, Math.min(this.maxVehicleHealth(), this.vehicleHealth() + repair));
            repaired = true;
        }
        repaired |= this.autoRepairParts(repair);
        if (repaired) {
            this.hurtMarked = true;
        }
    }

    private boolean autoRepairParts(float repair) {
        boolean repaired = false;
        if (this.leftWheelHealth < PART_MAX_HEALTH) {
            this.leftWheelHealth = Math.min(PART_MAX_HEALTH, this.leftWheelHealth + repair);
            repaired = true;
        }
        if (this.rightWheelHealth < PART_MAX_HEALTH) {
            this.rightWheelHealth = Math.min(PART_MAX_HEALTH, this.rightWheelHealth + repair);
            repaired = true;
        }
        if (this.engineHealth < PART_MAX_HEALTH) {
            this.engineHealth = Math.min(PART_MAX_HEALTH, this.engineHealth + repair);
            repaired = true;
        }
        if (this.turretHealth < PART_MAX_HEALTH) {
            this.turretHealth = Math.min(PART_MAX_HEALTH, this.turretHealth + repair);
            repaired = true;
        }
        if (repaired) {
            this.syncPartDamageFlags();
        }
        return repaired;
    }

    private boolean tickLowHealthDecay() {
        if (this.vehicleHealth() <= 0.0F || this.vehicleHealth() >= this.maxVehicleHealth() * LOW_HEALTH_DECAY_THRESHOLD) {
            return false;
        }
        if (this.tickCount % LOW_HEALTH_DECAY_INTERVAL == 0) {
            float newHealth = this.vehicleHealth() - LOW_HEALTH_DECAY_DAMAGE;
            this.entityData.set(DATA_HEALTH, Math.max(0.0F, newHealth));
            this.hurtMarked = true;
            if (newHealth <= 0.0F) {
                this.destroyVehicle();
            }
        }
        return true;
    }

    private void tickServerMovement() {
        EngineInfo engine = this.vehicleData().defaults().engine();
        if (this.vehicleData().defaults().vehicleType() == VehicleType.ARTILLERY) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (this.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER
                || this.vehicleData().defaults().vehicleType() == VehicleType.AIRCRAFT) {
            this.tickServerAirMovement(engine);
            this.tickEngineSound();
            return;
        }
        if (this.vehicleData().defaults().vehicleType() == VehicleType.BOAT) {
            this.tickServerBoatMovement(engine);
            this.tickEngineSound();
            return;
        }
        Vec3 velocity = this.getDeltaMovement();
        int forwardAxis = this.input.forwardAxis();
        int steeringAxis = this.steeringAxis();
        if (engine.steeringSpeed() > 0.0D) {
            this.tickServerSteeredLandMovement(engine, velocity, forwardAxis, steeringAxis);
            return;
        }
        boolean hasThrottle = this.hasEngineEnergy(engine, forwardAxis != 0 || steeringAxis != 0);
        this.tickBasicLandMovement(engine, velocity, forwardAxis, this.input.strafeAxis(), hasThrottle, true);
    }

    private void tickBasicLandMovement(EngineInfo engine, Vec3 velocity, int forwardAxis, int strafeAxis, boolean hasThrottle, boolean engineSound) {
        boolean grounded = this.onGround();
        double mobility = this.mobilityMultiplier();

        if (hasThrottle && (forwardAxis != 0 || strafeAxis != 0)) {
            Vec3 forward = this.landDriveForward();
            Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
            Vec3 desired = forward.scale(forwardAxis).add(right.scale(strafeAxis * 0.55D));
            if (desired.lengthSqr() > 1.0E-4D) {
                desired = desired.normalize().scale(engine.acceleration() * mobility);
                velocity = velocity.add(desired.x, 0.0D, desired.z);
            }
        }

        double maxSpeed = (forwardAxis < 0 ? engine.maxReverseSpeed() : engine.maxForwardSpeed()) * mobility;
        Vec3 horizontal = new Vec3(velocity.x, 0.0D, velocity.z);
        if (horizontal.length() > maxSpeed) {
            horizontal = horizontal.normalize().scale(maxSpeed);
            velocity = new Vec3(horizontal.x, velocity.y, horizontal.z);
        }

        double friction = this.input.brake() ? 0.55D : engine.friction();
        if (grounded) {
            velocity = new Vec3(velocity.x * friction, velocity.y, velocity.z * friction);
        }
        if (!this.isNoGravity()) {
            velocity = velocity.add(0.0D, -GRAVITY, 0.0D);
        }

        this.moveGroundVehicle(velocity);
        if (engineSound) {
            this.tickEngineSound();
        }
    }

    private void tickClientPredictedLandMovement() {
        if (this.vehicleData().defaults().vehicleType() != VehicleType.LAND) {
            return;
        }
        EngineInfo engine = this.vehicleData().defaults().engine();
        Vec3 velocity = this.getDeltaMovement();
        int forwardAxis = this.input.forwardAxis();
        int strafeAxis = this.input.strafeAxis();
        int steeringAxis = this.steeringAxis();
        if (engine.steeringSpeed() > 0.0D) {
            this.tickSteeredLandMovement(engine, velocity, forwardAxis, steeringAxis, false);
            return;
        }
        this.tickBasicLandMovement(engine, velocity, forwardAxis, strafeAxis, forwardAxis != 0 || strafeAxis != 0, false);
    }

    private boolean hasEngineEnergy(EngineInfo engine, boolean active) {
        int cost = engine.energyCostRate();
        return !active || cost <= 0 || this.vehicleEnergy() >= cost;
    }

    private boolean consumeEngineEnergy(EngineInfo engine, boolean active) {
        int cost = engine.energyCostRate();
        return !active || cost <= 0 || this.consumeEnergy(cost);
    }

    private void tickServerSteeredLandMovement(EngineInfo engine, Vec3 velocity, int forwardAxis, int steeringAxis) {
        this.tickSteeredLandMovement(engine, velocity, forwardAxis, steeringAxis, true);
    }

    private void tickSteeredLandMovement(EngineInfo engine, Vec3 velocity, int forwardAxis, int steeringAxis, boolean engineSound) {
        double mobility = this.mobilityMultiplier();
        Vec3 forward = this.landDriveForward();

        Vec3 horizontal = new Vec3(velocity.x, 0.0D, velocity.z);
        double horizontalSpeed = horizontal.length();
        if (forwardAxis > 0) {
            this.enginePower = Math.min(this.enginePower + (this.enginePower < 0.0D ? engine.acceleration() * 2.0D : engine.acceleration()), 1.0D);
        } else if (forwardAxis < 0) {
            this.enginePower = Math.max(this.enginePower - (this.enginePower > 0.0D ? engine.acceleration() * 2.0D : engine.acceleration()), -1.0D);
        } else {
            this.enginePower *= 0.97D;
        }
        if (this.input.brake()) {
            this.enginePower *= 0.6D;
        }
        if (steeringAxis != 0) {
            this.enginePower *= 0.98D;
        }

        if (engineSound && engine.energyCostRate() > 0) {
            int cost = (int) (engine.energyCostRate() * Math.abs(this.enginePower));
            if (cost > 0 && !this.consumeEnergy(cost)) {
                this.enginePower *= 0.95D;
            }
        }

        if (steeringAxis != 0) {
            this.wheelSteering += steeringAxis * engine.steeringSpeed();
        }
        this.wheelSteering *= Math.max(0.78D - 0.25D * horizontalSpeed, 0.1D);

        if (Math.abs(this.wheelSteering) > 1.0E-4D) {
            double yawDelta = Math.max(12.0D * horizontalSpeed, 0.0D) * this.wheelSteering * (this.enginePower > 0.0D ? 1.0D : -1.0D);
            this.setYRot(this.getYRot() + (float) yawDelta);
            this.yRotO = this.getYRot();
        }

        double targetSpeed = (this.enginePower > 0.0D ? engine.maxForwardSpeed() : engine.maxReverseSpeed()) * mobility;
        velocity = velocity.add(forward.scale(0.15D * targetSpeed * this.enginePower));
        horizontal = new Vec3(velocity.x, 0.0D, velocity.z);
        horizontalSpeed = horizontal.length();

        double maxSpeed = (this.enginePower < 0.0D ? engine.maxReverseSpeed() : engine.maxForwardSpeed()) * mobility;
        if (horizontalSpeed > maxSpeed) {
            horizontal = horizontal.normalize().scale(maxSpeed);
            velocity = new Vec3(horizontal.x, velocity.y, horizontal.z);
        }

        double friction = this.input.brake() ? 0.55D : engine.friction();
        if (this.onGround()) {
            velocity = new Vec3(velocity.x * friction, velocity.y, velocity.z * friction);
        }
        if (!this.isNoGravity()) {
            velocity = velocity.add(0.0D, -GRAVITY, 0.0D);
        }

        this.moveGroundVehicle(velocity);
        if (engineSound) {
            this.tickEngineSound();
        }
    }

    private void moveGroundVehicle(Vec3 velocity) {
        Vec3 before = this.position();
        this.setDeltaMovement(velocity);
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 moved = this.position().subtract(before);
        if (moved.horizontalDistanceSqr() < 1.0E-7D && new Vec3(velocity.x, 0.0D, velocity.z).lengthSqr() > 1.0E-7D) {
            velocity = new Vec3(0.0D, velocity.y, 0.0D);
            this.enginePower *= 0.35D;
            this.wheelSteering *= 0.5D;
        }
        double nextY = velocity.y;
        if (this.onGround() || this.verticalCollisionBelow) {
            nextY = moved.y > 0.0D ? Math.min(moved.y * 0.2D, 0.08D) : Math.min(0.0D, nextY);
        } else if (this.verticalCollision && nextY > 0.0D) {
            nextY = 0.0D;
        }
        this.setDeltaMovement(moved.x * 0.98D, nextY * 0.98D, moved.z * 0.98D);
        if (moved.horizontalDistanceSqr() > 1.0E-7D) {
            this.hurtMarked = true;
            this.hasImpulse = true;
        }
    }

    private Vec3 landDriveForward() {
        return this.horizontalDirection(this.getYRot());
    }

    public double rotateOffsetHeight() {
        return this.articulatedRenderPivotHeight();
    }

    private double articulatedRenderPivotHeight() {
        return this.vehicleDataId().equals(LAV150_ID) ? LAV150_ROTATE_OFFSET_HEIGHT : 0.0D;
    }

    private int steeringAxis() {
        return (this.input.right() ? 1 : 0) - (this.input.left() ? 1 : 0);
    }

    private Vec3 horizontalDirection(float yaw) {
        double yawRadians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
    }

    private void tickEngineSound() {
        if (this.tickCount % 20 != 0) {
            return;
        }
        SoundEvent sound = VehicleSoundHelper.engineSound(this);
        if (sound == null) {
            return;
        }
        VehicleType type = this.vehicleData().defaults().vehicleType();
        Vec3 movement = this.getDeltaMovement();
        boolean active = this.input.forwardAxis() != 0 || this.input.strafeAxis() != 0 || this.input.verticalAxis() != 0;
        if (!active) {
            active = switch (type) {
                case LAND, BOAT -> movement.x * movement.x + movement.z * movement.z > 0.0025D;
                case HELICOPTER, AIRCRAFT -> movement.lengthSqr() > 0.0025D;
                case ARTILLERY -> false;
            };
        }
        if (!active) {
            return;
        }
        this.level().playSound(null, this, sound, SoundSource.AMBIENT, 1.0F, 1.0F);
    }

    private void tickServerAirMovement(EngineInfo engine) {
        Vec3 velocity = this.getDeltaMovement();
        int forwardAxis = this.input.forwardAxis();
        int strafeAxis = this.input.strafeAxis();
        int verticalAxis = this.input.verticalAxis();
        boolean hasThrottle = this.consumeEngineEnergy(engine, forwardAxis != 0 || strafeAxis != 0 || verticalAxis != 0);
        double mobility = this.mobilityMultiplier();

        float yaw = this.getYRot();
        double yawRadians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 desired = forward.scale(forwardAxis).add(right.scale(strafeAxis * 0.65D)).add(0.0D, verticalAxis * 0.8D, 0.0D);
        if (hasThrottle && desired.lengthSqr() > 1.0E-4D) {
            velocity = velocity.add(desired.normalize().scale(engine.acceleration() * mobility));
        }

        double maxSpeed = engine.maxForwardSpeed() * mobility;
        if (velocity.length() > maxSpeed) {
            velocity = velocity.normalize().scale(maxSpeed);
        }
        double drag = this.input.brake() ? 0.82D : 0.94D;
        velocity = velocity.multiply(drag, 0.90D, drag);
        if (verticalAxis == 0) {
            velocity = velocity.add(0.0D, -0.01D, 0.0D);
        }

        this.setDeltaMovement(velocity);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private void tickServerBoatMovement(EngineInfo engine) {
        Vec3 velocity = this.getDeltaMovement();
        int forwardAxis = this.input.forwardAxis();
        int strafeAxis = this.input.strafeAxis();
        boolean inWater = this.isInWater();
        boolean hasThrottle = this.consumeEngineEnergy(engine, forwardAxis != 0 || strafeAxis != 0);
        double mobility = this.mobilityMultiplier();

        if (hasThrottle && (forwardAxis != 0 || strafeAxis != 0)) {
            float yaw = this.getYRot();
            double yawRadians = Math.toRadians(yaw);
            Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
            Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
            Vec3 desired = forward.scale(forwardAxis).add(right.scale(strafeAxis * 0.45D));
            if (desired.lengthSqr() > 1.0E-4D) {
                velocity = velocity.add(desired.normalize().scale(engine.acceleration() * mobility * (inWater ? 1.0D : 0.25D)));
            }
        }

        double maxSpeed = engine.maxForwardSpeed() * mobility * (inWater ? 1.0D : 0.35D);
        Vec3 horizontal = new Vec3(velocity.x, 0.0D, velocity.z);
        if (horizontal.length() > maxSpeed) {
            horizontal = horizontal.normalize().scale(maxSpeed);
            velocity = new Vec3(horizontal.x, velocity.y, horizontal.z);
        }
        if (inWater) {
            velocity = new Vec3(velocity.x * engine.friction(), Math.min(0.05D, velocity.y + 0.02D), velocity.z * engine.friction());
        } else {
            velocity = velocity.add(0.0D, -GRAVITY, 0.0D).multiply(0.92D, 0.98D, 0.92D);
        }

        this.setDeltaMovement(velocity);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private double mobilityMultiplier() {
        double multiplier = this.isEngineDamaged() ? 0.35D : 1.0D;
        if (this.isLeftWheelDamaged()) {
            multiplier *= 0.75D;
        }
        if (this.isRightWheelDamaged()) {
            multiplier *= 0.75D;
        }
        return Math.max(0.2D, multiplier);
    }

    private void applyPassengerYaw() {
        Entity passenger = this.getControllingPassenger();
        if (passenger == null || this.isFreeLookInputDown() || this.vehicleData().defaults().vehicleType() == VehicleType.LAND) {
            return;
        }
        this.setYRot(passenger.getYRot());
        this.yRotO = this.getYRot();
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        int fallbackIndex = this.getPassengers().indexOf(passenger);
        SeatInfo seat = this.seatForPassenger(passenger, fallbackIndex);
        Vec3 offset = this.seatOffset(seat, 0.0D, 1.0F);
        callback.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
    }

    private Vec3 rotateSeatOffset(SeatInfo seat) {
        return this.seatOffset(seat, 0.0D, 1.0F);
    }

    private Vec3 seatOffset(SeatInfo seat, double eyeHeight, float partialTick) {
        if (this.usesArticulatedSeatTransform(seat)) {
            if (this.vehicleDataId().equals(BMP2_ID)) {
                return this.bmp2TurretOffset(seat.x(), seat.y() + eyeHeight, seat.z(), partialTick);
            }
            return this.lav150TurretOffset(seat.x(), seat.y() + eyeHeight, seat.z(), partialTick);
        }
        return this.rotateLocalOffset(seat.x(), seat.y() + eyeHeight, seat.z(), partialTick);
    }

    private boolean usesArticulatedSeatTransform(SeatInfo seat) {
        return (this.vehicleDataId().equals(LAV150_ID) || this.vehicleDataId().equals(BMP2_ID)) && seat.index() == 0;
    }

    private boolean usesArticulatedTurretAim(LivingEntity shooter) {
        VehicleWeaponInfo weapon = this.selectedWeapon();
        if (weapon == null) {
            return false;
        }
        if (this.vehicleDataId().equals(LAV150_ID)) {
            return this.canUseSelectedWeapon(shooter, weapon);
        }
        return this.vehicleDataId().equals(BMP2_ID)
                && !weapon.guided()
                && this.seatIndexForPassenger(shooter, this.getPassengers().indexOf(shooter)) == 0
                && this.canUseSelectedWeapon(shooter, weapon);
    }

    private boolean usesArticulatedTurretMuzzle(VehicleWeaponInfo weapon) {
        if (this.vehicleDataId().equals(LAV150_ID)) {
            return weapon.hasMuzzle();
        }
        return this.vehicleDataId().equals(BMP2_ID) && !weapon.guided() && weapon.hasMuzzle();
    }

    private Vec3 rotateLocalOffset(double localX, double localY, double localZ) {
        return this.rotateLocalOffset(localX, localY, localZ, 1.0F);
    }

    private Vec3 rotateLocalOffset(double localX, double localY, double localZ, float partialTick) {
        double yaw = Math.toRadians(Mth.lerp(partialTick, this.yRotO, this.getYRot()));
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double x = localX * cos - localZ * sin;
        double z = localX * sin + localZ * cos;
        return new Vec3(x, localY, z);
    }

    private Vec3 lav150TurretOffset(double localX, double localY, double localZ, float partialTick) {
        Vec3 turretOrigin = this.rotateLocalOffset(0.0D, LAV150_TURRET_POS_Y, 0.0D, partialTick);
        Vec3 turretLocal = this.rotateLocalOffsetByYaw(localX, localY, localZ, this.lav150WorldTurretYaw(partialTick));
        return turretOrigin.add(turretLocal);
    }

    private Vec3 bmp2TurretOffset(double localX, double localY, double localZ, float partialTick) {
        Vec3 turretOrigin = this.rotateLocalOffset(0.0D, BMP2_TURRET_POS_Y, BMP2_TURRET_POS_Z, partialTick);
        Vec3 turretLocal = this.rotateLocalOffsetByYaw(localX, localY, localZ, this.bmp2WorldTurretYaw(partialTick));
        return turretOrigin.add(turretLocal);
    }

    private Vec3 lav150BarrelPosition(double localX, double localY, double localZ, float partialTick) {
        Vec3 barrelOrigin = this.lav150TurretOffset(LAV150_BARREL_POS_X, LAV150_BARREL_POS_Y, LAV150_BARREL_POS_Z, partialTick);
        return this.interpolatedVehiclePosition(partialTick)
                .add(barrelOrigin)
                .add(this.rotateLocalOffsetByYawAndPitch(localX, localY, localZ, this.lav150WorldTurretYaw(partialTick), this.lav150TurretPitch(partialTick)));
    }

    private Vec3 bmp2BarrelPosition(double localX, double localY, double localZ, float partialTick) {
        Vec3 barrelOrigin = this.bmp2TurretOffset(BMP2_BARREL_POS_X, BMP2_BARREL_POS_Y, BMP2_BARREL_POS_Z, partialTick);
        return this.interpolatedVehiclePosition(partialTick)
                .add(barrelOrigin)
                .add(this.rotateLocalOffsetByYawAndPitch(localX, localY, localZ, this.bmp2WorldTurretYaw(partialTick), this.bmp2TurretPitch(partialTick)));
    }

    private Vec3 lav150BarrelDirection(float partialTick) {
        return this.rotateLocalOffsetByYawAndPitch(0.0D, 0.0D, 1.0D, this.lav150WorldTurretYaw(partialTick), this.lav150TurretPitch(partialTick));
    }

    private Vec3 bmp2BarrelDirection(float partialTick) {
        return this.rotateLocalOffsetByYawAndPitch(0.0D, 0.0D, 1.0D, this.bmp2WorldTurretYaw(partialTick), this.bmp2TurretPitch(partialTick));
    }

    private float lav150WorldTurretYaw(float partialTick) {
        return Mth.lerp(partialTick, this.yRotO, this.getYRot()) - this.turretYaw(partialTick);
    }

    private float bmp2WorldTurretYaw(float partialTick) {
        return Mth.lerp(partialTick, this.yRotO, this.getYRot()) - this.turretYaw(partialTick);
    }

    private float lav150TurretPitch(float partialTick) {
        return this.turretPitch(partialTick);
    }

    private float bmp2TurretPitch(float partialTick) {
        return this.turretPitch(partialTick);
    }

    private Vec3 rotateLocalOffsetByYawAndPitch(double localX, double localY, double localZ, float yaw, float pitch) {
        double pitchRadians = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRadians);
        double sinPitch = Math.sin(pitchRadians);
        double pitchedY = localY * cosPitch - localZ * sinPitch;
        double pitchedZ = localY * sinPitch + localZ * cosPitch;
        return this.rotateLocalOffsetByYaw(localX, pitchedY, pitchedZ, yaw);
    }

    private Vec3 rotateLocalOffsetByYaw(double localX, double localY, double localZ, float yaw) {
        double yawRadians = Math.toRadians(yaw);
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);
        double x = localX * cos - localZ * sin;
        double z = localX * sin + localZ * cos;
        return new Vec3(x, localY, z);
    }

    private float yRotFromVector(Vec3 vec3) {
        return (float) -Math.toDegrees(Math.atan2(vec3.x, vec3.z));
    }

    private float xRotFromVector(Vec3 vec3) {
        return (float) -Math.toDegrees(Math.atan2(vec3.y, vec3.horizontalDistance()));
    }

    private Vec3 interpolatedVehiclePosition(float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, this.xo, this.getX()),
                Mth.lerp(partialTick, this.yo, this.getY()),
                Mth.lerp(partialTick, this.zo, this.getZ())
        );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        output.putString(TAG_VEHICLE_ID, this.entityData.get(DATA_VEHICLE_ID));
        output.putFloat(TAG_HEALTH, this.vehicleHealth());
        output.putInt(TAG_ENERGY, this.vehicleEnergy());
        output.putInt(TAG_REPAIR_COOLDOWN, this.repairCooldown);
        output.putInt(TAG_SELECTED_WEAPON, this.entityData.get(DATA_SELECTED_WEAPON));
        ListTag loadedAmmo = new ListTag();
        for (Map.Entry<Integer, Integer> entry : this.loadedAmmoByWeaponSlot.entrySet()) {
            CompoundTag weaponTag = new CompoundTag();
            weaponTag.putInt(TAG_LOADED_WEAPON_SLOT, entry.getKey());
            weaponTag.putInt(TAG_LOADED_WEAPON_COUNT, entry.getValue());
            loadedAmmo.add(weaponTag);
        }
        output.put(TAG_LOADED_WEAPON_AMMO, loadedAmmo);
        output.putInt(TAG_DECOY_COOLDOWN, this.decoyCooldown);
        output.putFloat(TAG_LEFT_WHEEL_HEALTH, this.leftWheelHealth);
        output.putFloat(TAG_RIGHT_WHEEL_HEALTH, this.rightWheelHealth);
        output.putFloat(TAG_ENGINE_HEALTH, this.engineHealth);
        output.putFloat(TAG_TURRET_HEALTH, this.turretHealth);
        ListTag seats = new ListTag();
        for (Map.Entry<UUID, Integer> assignment : this.seatAssignments.entrySet()) {
            CompoundTag seat = new CompoundTag();
            seat.putUUID(TAG_SEAT_PASSENGER, assignment.getKey());
            seat.putInt(TAG_SEAT_INDEX, assignment.getValue());
            seats.add(seat);
        }
        output.put(TAG_SEAT_ASSIGNMENTS, seats);
        CompoundTag inventoryTag = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryTag, this.inventory.getItems(), this.level().registryAccess());
        output.put(TAG_ITEMS, inventoryTag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        if (input.contains(TAG_VEHICLE_ID)) {
            this.entityData.set(DATA_VEHICLE_ID, input.getString(TAG_VEHICLE_ID));
        }
        float health = input.contains(TAG_HEALTH) ? input.getFloat(TAG_HEALTH) : this.maxVehicleHealth();
        this.entityData.set(DATA_HEALTH, Mth.clamp(health, 0.0F, this.maxVehicleHealth()));
        int energy = input.contains(TAG_ENERGY) ? input.getInt(TAG_ENERGY) : this.maxVehicleEnergy();
        this.entityData.set(DATA_ENERGY, Mth.clamp(energy, 0, this.maxVehicleEnergy()));
        this.repairCooldown = input.getInt(TAG_REPAIR_COOLDOWN);
        this.entityData.set(DATA_SELECTED_WEAPON, this.readSelectedWeapon(input));
        this.readLoadedWeaponAmmo(input);
        this.activeReloadWeaponSlot = -1;
        this.activeReloadTicks = 0;
        this.decoyCooldown = input.getInt(TAG_DECOY_COOLDOWN);
        this.leftWheelHealth = input.contains(TAG_LEFT_WHEEL_HEALTH) ? input.getFloat(TAG_LEFT_WHEEL_HEALTH) : PART_MAX_HEALTH;
        this.rightWheelHealth = input.contains(TAG_RIGHT_WHEEL_HEALTH) ? input.getFloat(TAG_RIGHT_WHEEL_HEALTH) : PART_MAX_HEALTH;
        this.engineHealth = input.contains(TAG_ENGINE_HEALTH) ? input.getFloat(TAG_ENGINE_HEALTH) : PART_MAX_HEALTH;
        this.turretHealth = input.contains(TAG_TURRET_HEALTH) ? input.getFloat(TAG_TURRET_HEALTH) : PART_MAX_HEALTH;
        this.readSeatAssignments(input);
        this.syncPartDamageFlags();
        if (input.contains(TAG_ITEMS)) {
            ContainerHelper.loadAllItems(input.getCompound(TAG_ITEMS), this.inventory.getItems(), this.level().registryAccess());
        }
        this.syncSelectedWeaponAmmoState();
    }

    private int readSelectedWeapon(CompoundTag input) {
        int weaponCount = this.vehicleData().defaults().weapons().size();
        if (!input.contains(TAG_SELECTED_WEAPON) || weaponCount <= 0) {
            return 0;
        }
        return Mth.clamp(input.getInt(TAG_SELECTED_WEAPON), 0, weaponCount - 1);
    }

    private void readLoadedWeaponAmmo(CompoundTag input) {
        this.loadedAmmoByWeaponSlot.clear();
        if (!input.contains(TAG_LOADED_WEAPON_AMMO, Tag.TAG_LIST)) {
            return;
        }
        int weaponCount = this.vehicleData().defaults().weapons().size();
        ListTag loadedAmmo = input.getList(TAG_LOADED_WEAPON_AMMO, Tag.TAG_COMPOUND);
        for (int index = 0; index < loadedAmmo.size(); index++) {
            CompoundTag weaponTag = loadedAmmo.getCompound(index);
            int slot = weaponTag.getInt(TAG_LOADED_WEAPON_SLOT);
            int ammo = weaponTag.getInt(TAG_LOADED_WEAPON_COUNT);
            if (slot >= 0 && slot < weaponCount && ammo > 0) {
                this.loadedAmmoByWeaponSlot.put(slot, ammo);
            }
        }
    }

    private void readSeatAssignments(CompoundTag input) {
        this.seatAssignments.clear();
        if (!input.contains(TAG_SEAT_ASSIGNMENTS, Tag.TAG_LIST)) {
            return;
        }
        int seatCount = this.vehicleData().defaults().seats().size();
        ListTag seats = input.getList(TAG_SEAT_ASSIGNMENTS, Tag.TAG_COMPOUND);
        for (int index = 0; index < seats.size(); index++) {
            CompoundTag seat = seats.getCompound(index);
            if (!seat.hasUUID(TAG_SEAT_PASSENGER)) {
                continue;
            }
            int seatIndex = seat.getInt(TAG_SEAT_INDEX);
            if (seatIndex >= 0 && seatIndex < seatCount) {
                this.seatAssignments.put(seat.getUUID(TAG_SEAT_PASSENGER), seatIndex);
            }
        }
    }

    public CompoundTag saveVehicleContainerState() {
        CompoundTag tag = new CompoundTag();
        this.addAdditionalSaveData(tag);
        return tag;
    }

    public void loadVehicleContainerState(CompoundTag tag) {
        this.readAdditionalSaveData(tag);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved() || this.isInvulnerableTo(source)) {
            return false;
        }
        OBBInfo.Part hitPart = this.estimateHitPart(source);
        ArmorHit armorHit = this.applyVehicleArmor(source, amount, hitPart);
        float finalDamage = this.vehicleData().defaults().damageModifier().apply(armorHit.finalDamage());
        if (finalDamage <= 1.0F) {
            return false;
        }
        this.applyPartDamage(hitPart, finalDamage);
        this.applyPassengerLeakDamage(source, finalDamage, hitPart, armorHit.penetrated());
        this.repairCooldown = this.vehicleData().defaults().autoRepairCooldownTicks();
        float newHealth = this.vehicleHealth() - finalDamage;
        this.entityData.set(DATA_HEALTH, Math.max(0.0F, newHealth));
        this.hurtMarked = true;
        if (newHealth <= 0.0F) {
            this.destroyVehicle();
        }
        return true;
    }

    private void destroyVehicle() {
        this.ejectPassengers();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticleTypes.SMALL_EXPLOSION.get(), this.getX(), this.getY() + 0.6D, this.getZ(), 12, 0.8D, 0.5D, 0.8D, 0.08D);
            var destroy = this.vehicleData().defaults().destroy();
            if (destroy.explodes() && destroy.explosionPower() > 0.0F) {
                this.level().explode(this, this.getX(), this.getY() + 0.4D, this.getZ(), destroy.explosionPower(), ExplosionInteraction.MOB);
            }
        }
        this.discard();
    }

    private ArmorHit applyVehicleArmor(DamageSource source, float amount, OBBInfo.Part hitPart) {
        if (!(source.getDirectEntity() instanceof BulletEntity bullet)) {
            return new ArmorHit(amount, true);
        }
        VehiclePartArmorProfile armor = this.vehicleData().defaults().armor().forPart(hitPart);
        BallisticProtection.IntrinsicArmorProfile intrinsic = new BallisticProtection.IntrinsicArmorProfile(
                armor.rating(),
                armor.undermatchMultiplier(),
                armor.overmatchMultiplier()
        );
        BallisticProtection.BallisticResult result = BallisticProtection.applyToIntrinsicArmor(
                amount,
                bullet.getGunStats(),
                intrinsic,
                BallisticProtection.isRocketDirectHit(bullet.getGunStats())
        );
        return new ArmorHit(result.finalDamage(), !result.armorApplied() || result.overmatched());
    }

    private void applyPassengerLeakDamage(DamageSource source, float finalDamage, OBBInfo.Part hitPart, boolean penetrated) {
        VehiclePartArmorProfile armor = this.vehicleData().defaults().armor().forPart(hitPart);
        float leakMultiplier = armor.passengerLeakMultiplier();
        if (leakMultiplier <= 0.0F || this.getPassengers().isEmpty()) {
            return;
        }
        Entity sourceEntity = source.getEntity();
        Entity directEntity = source.getDirectEntity();
        for (int index = 0; index < this.getPassengers().size(); index++) {
            Entity passenger = this.getPassengers().get(index);
            if (!(passenger instanceof LivingEntity living) || passenger == sourceEntity || passenger == directEntity) {
                continue;
            }
            SeatInfo seat = this.seatForPassenger(passenger, index);
            if (seat.enclosed() && !penetrated) {
                continue;
            }
            living.hurt(source, finalDamage * leakMultiplier);
        }
    }

    private SeatInfo seatForPassenger(int index) {
        var seats = this.vehicleData().defaults().seats();
        if (index >= 0 && index < seats.size()) {
            return seats.get(index);
        }
        return SeatInfo.DRIVER;
    }

    private SeatInfo seatForPassenger(Entity passenger, int fallbackIndex) {
        return this.seatForPassenger(this.seatIndexForPassenger(passenger, fallbackIndex));
    }

    private int seatIndexForPassenger(Entity passenger, int fallbackIndex) {
        int seatCount = this.vehicleData().defaults().seats().size();
        int assignedSeat = this.seatAssignments.getOrDefault(passenger.getUUID(), fallbackIndex);
        if (assignedSeat >= 0 && assignedSeat < seatCount) {
            return assignedSeat;
        }
        return Mth.clamp(fallbackIndex, 0, Math.max(0, seatCount - 1));
    }

    public int getSeatIndex(Entity passenger) {
        return this.seatIndexForPassenger(passenger, this.getPassengers().indexOf(passenger));
    }

    @Nullable
    public Entity passengerForSeat(int seatIndex) {
        for (int index = 0; index < this.getPassengers().size(); index++) {
            Entity passenger = this.getPassengers().get(index);
            if (this.seatIndexForPassenger(passenger, index) == seatIndex) {
                return passenger;
            }
        }
        return null;
    }

    private boolean isSeatOccupied(int seatIndex, @Nullable Entity except) {
        for (int index = 0; index < this.getPassengers().size(); index++) {
            Entity passenger = this.getPassengers().get(index);
            if (passenger != except && this.seatIndexForPassenger(passenger, index) == seatIndex) {
                return true;
            }
        }
        return false;
    }

    private OBBInfo.Part estimateHitPart(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct == null) {
            return OBBInfo.Part.BODY;
        }
        Vec3 local = this.toLocalVehiclePosition(direct.position());
        for (OBBInfo.Box box : this.vehicleData().defaults().obb().boxes()) {
            if (Math.abs(local.x - box.x()) <= box.halfWidth()
                    && Math.abs(local.y - box.y()) <= box.halfHeight()
                    && Math.abs(local.z - box.z()) <= box.halfDepth()) {
                return box.part();
            }
        }
        if (local.y < 0.65D && Math.abs(local.x) > 0.35D) {
            return local.x < 0.0D ? OBBInfo.Part.WHEEL_LEFT : OBBInfo.Part.WHEEL_RIGHT;
        }
        if (local.y < 0.9D && local.z < -0.35D) {
            return OBBInfo.Part.MAIN_ENGINE;
        }
        return OBBInfo.Part.BODY;
    }

    private Vec3 toLocalVehiclePosition(Vec3 worldPosition) {
        double dx = worldPosition.x - this.getX();
        double dy = worldPosition.y - this.getY();
        double dz = worldPosition.z - this.getZ();
        double yaw = Math.toRadians(this.getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        return new Vec3(dx * cos + dz * sin, dy, -dx * sin + dz * cos);
    }

    private void applyPartDamage(OBBInfo.Part hitPart, float finalDamage) {
        VehiclePartArmorProfile armor = this.vehicleData().defaults().armor().forPart(hitPart);
        float partDamage = finalDamage * armor.partDamageMultiplier();
        switch (hitPart) {
            case WHEEL_LEFT -> this.leftWheelHealth = Math.max(0.0F, this.leftWheelHealth - partDamage);
            case WHEEL_RIGHT -> this.rightWheelHealth = Math.max(0.0F, this.rightWheelHealth - partDamage);
            case MAIN_ENGINE, SUB_ENGINE -> this.engineHealth = Math.max(0.0F, this.engineHealth - partDamage);
            case TURRET -> this.turretHealth = Math.max(0.0F, this.turretHealth - partDamage);
            default -> {
            }
        }
        this.syncPartDamageFlags();
    }

    private void syncPartDamageFlags() {
        this.entityData.set(DATA_LEFT_WHEEL_DAMAGED, this.leftWheelHealth <= 0.0F);
        this.entityData.set(DATA_RIGHT_WHEEL_DAMAGED, this.rightWheelHealth <= 0.0F);
        this.entityData.set(DATA_ENGINE_DAMAGED, this.engineHealth <= 0.0F);
        this.entityData.set(DATA_TURRET_DAMAGED, this.turretHealth <= 0.0F);
    }

    private boolean repairWithKit() {
        boolean repaired = false;
        if (this.vehicleHealth() < this.maxVehicleHealth()) {
            this.entityData.set(DATA_HEALTH, Math.min(this.maxVehicleHealth(), this.vehicleHealth() + REPAIR_KIT_HULL_REPAIR));
            repaired = true;
        }
        if (this.leftWheelHealth < PART_MAX_HEALTH) {
            this.leftWheelHealth = Math.min(PART_MAX_HEALTH, this.leftWheelHealth + REPAIR_KIT_PART_REPAIR);
            repaired = true;
        }
        if (this.rightWheelHealth < PART_MAX_HEALTH) {
            this.rightWheelHealth = Math.min(PART_MAX_HEALTH, this.rightWheelHealth + REPAIR_KIT_PART_REPAIR);
            repaired = true;
        }
        if (this.engineHealth < PART_MAX_HEALTH) {
            this.engineHealth = Math.min(PART_MAX_HEALTH, this.engineHealth + REPAIR_KIT_PART_REPAIR);
            repaired = true;
        }
        if (this.turretHealth < PART_MAX_HEALTH) {
            this.turretHealth = Math.min(PART_MAX_HEALTH, this.turretHealth + REPAIR_KIT_PART_REPAIR);
            repaired = true;
        }
        if (repaired) {
            this.repairCooldown = 0;
            this.syncPartDamageFlags();
            this.hurtMarked = true;
        }
        return repaired;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.REPAIR_KIT.get())) {
            if (this.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (this.repairWithKit()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.displayClientMessage(Component.translatable("message.jeg.vehicle.repaired"), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide && player.isShiftKeyDown() && stack.is(ModItems.CROWBAR.get()) && player instanceof ServerPlayer serverPlayer) {
            if (!this.getPassengers().isEmpty()) {
                serverPlayer.displayClientMessage(Component.translatable("message.jeg.vehicle.occupied"), true);
                return InteractionResult.CONSUME;
            }
            ItemStack container = VehicleContainerBlockEntity.createItemFor(this);
            if (!serverPlayer.getInventory().add(container)) {
                serverPlayer.drop(container, false);
            }
            if (!serverPlayer.getAbilities().instabuild) {
                stack.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));
            }
            this.discard();
            return InteractionResult.CONSUME;
        }
        if (!this.level().isClientSide && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this, buffer -> buffer.writeVarInt(this.vehicleContainerSlots()));
            return InteractionResult.CONSUME;
        }
        if (!this.level().isClientSide && !player.isPassenger()) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.jeg.vehicle");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VehicleMenu(containerId, playerInventory, this.inventory, this.vehicleContainerSlots(), this);
    }

    public int vehicleContainerSlots() {
        VehicleContainerType containerType = this.vehicleData().defaults().containerType();
        return Math.min(containerType.slots(), this.inventory.getContainerSize());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.vehicleData().defaults().seats().size();
    }

    @Override
    protected void addPassenger(@NotNull Entity passenger) {
        super.addPassenger(passenger);
        int seatCount = this.vehicleData().defaults().seats().size();
        for (int seat = 0; seat < seatCount; seat++) {
            if (!this.isSeatOccupied(seat, passenger)) {
                this.seatAssignments.put(passenger.getUUID(), seat);
                this.syncSeatAssignments();
                return;
            }
        }
        this.syncSeatAssignments();
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger) {
        boolean controllingPassenger = passenger == this.getControllingPassenger();
        this.seatAssignments.remove(passenger.getUUID());
        if (controllingPassenger) {
            this.cancelWeaponReload();
            this.clearControlState(true);
            if (!this.level().isClientSide && passenger instanceof ServerPlayer player) {
                NetworkHandler.sendForcedVehicleState(player, this);
            }
        }
        super.removePassenger(passenger);
        this.syncSeatAssignments();
        if (controllingPassenger && this.vehicleData().defaults().vehicleType() == VehicleType.LAND) {
            Vec3 passengerMotion = passenger.getDeltaMovement();
            passenger.setDeltaMovement(0.0D, passengerMotion.y, 0.0D);
        }
    }

    public double getPassengersRidingOffset() {
        return 0.45D;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        DismountInfo dismount = this.vehicleData().defaults().dismount();
        Vec3 offset = this.rotateLocalOffset(dismount.x(), dismount.y(), dismount.z());
        Vec3 candidate = this.position().add(offset);
        Vec3 configured = this.findValidDismountPosition(passenger, candidate);
        if (configured != null) {
            return configured;
        }

        Vec3 side = this.findSideDismountPosition(passenger);
        if (side != null) {
            return side;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Nullable
    private Vec3 findSideDismountPosition(LivingEntity passenger) {
        double clearance = (this.getBbWidth() + passenger.getBbWidth()) * 0.5D + 0.2D;
        Vec3[] offsets = {
                this.rotateLocalOffset(clearance, 0.0D, 0.0D),
                this.rotateLocalOffset(-clearance, 0.0D, 0.0D)
        };
        for (Vec3 offset : offsets) {
            Vec3 base = this.position().add(offset);
            Vec3 candidate = this.findGroundedDismountPosition(passenger, base);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private Vec3 findGroundedDismountPosition(LivingEntity passenger, Vec3 base) {
        BlockPos exitPos = BlockPos.containing(base.x, this.getY(), base.z);
        BlockPos floorPos = exitPos.below();
        double exitHeight = this.level().getBlockFloorHeight(exitPos);
        if (DismountHelper.isBlockFloorValid(exitHeight)) {
            Vec3 candidate = this.findValidDismountPosition(passenger, new Vec3(base.x, exitPos.getY() + exitHeight, base.z));
            if (candidate != null) {
                return candidate;
            }
        }
        double floorHeight = this.level().getBlockFloorHeight(floorPos);
        if (DismountHelper.isBlockFloorValid(floorHeight)) {
            return this.findValidDismountPosition(passenger, new Vec3(base.x, floorPos.getY() + floorHeight, base.z));
        }
        return null;
    }

    @Nullable
    private Vec3 findValidDismountPosition(LivingEntity passenger, Vec3 candidate) {
        for (Pose pose : passenger.getDismountPoses()) {
            if (DismountHelper.canDismountTo(this.level(), candidate, passenger, pose)) {
                passenger.setPose(pose);
                return candidate;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity passenger = null;
        boolean hasDriverSeat = false;
        for (int index = 0; index < this.getPassengers().size(); index++) {
            if (this.seatForPassenger(this.getPassengers().get(index), index).driver()) {
                passenger = this.getPassengers().get(index);
                break;
            }
        }
        for (SeatInfo seat : this.vehicleData().defaults().seats()) {
            hasDriverSeat |= seat.driver();
        }
        if (passenger == null && !hasDriverSeat) {
            passenger = this.getFirstPassenger();
        }
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    private record ArmorHit(float finalDamage, boolean penetrated) {
    }

}
