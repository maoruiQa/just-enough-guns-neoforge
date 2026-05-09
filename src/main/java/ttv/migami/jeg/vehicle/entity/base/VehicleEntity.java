package ttv.migami.jeg.vehicle.entity.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.gun.BallisticProtection;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;
import ttv.migami.jeg.vehicle.data.DefaultVehicleData;
import ttv.migami.jeg.vehicle.data.VehiclePartArmorProfile;
import ttv.migami.jeg.vehicle.data.VehicleData;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.data.subdata.EngineInfo;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeatInfo;
import ttv.migami.jeg.vehicle.data.subdata.VehicleWeaponInfo;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;
import ttv.migami.jeg.vehicle.projectile.VehicleDecoyEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;

public class VehicleEntity extends Entity implements MenuProvider {
    private static final EntityDataAccessor<String> DATA_VEHICLE_ID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RIFLE_AMMO = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SELECTED_WEAPON = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SELECTED_WEAPON_AMMO = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LEFT_WHEEL_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RIGHT_WHEEL_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENGINE_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation RIFLE_AMMO = Reference.id("rifle_ammo");
    private static final ResourceLocation FLARE_AMMO = Reference.id("flare");
    private static final int DECOY_COOLDOWN_TICKS = 60;
    private static final int REDSTONE_ENERGY_VALUE = 20;
    private static final int ENERGY_RECHARGE_INTERVAL = 20;
    private static final int LOW_HEALTH_DECAY_INTERVAL = 40;
    private static final String TAG_VEHICLE_ID = "VehicleDataId";
    private static final String TAG_HEALTH = "Health";
    private static final String TAG_ENERGY = "Energy";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_REPAIR_COOLDOWN = "RepairCooldown";
    private static final String TAG_LEFT_WHEEL_HEALTH = "LeftWheelHealth";
    private static final String TAG_RIGHT_WHEEL_HEALTH = "RightWheelHealth";
    private static final String TAG_ENGINE_HEALTH = "EngineHealth";
    private static final float PART_MAX_HEALTH = 10.0F;
    private static final float REPAIR_KIT_HULL_REPAIR = 12.0F;
    private static final float REPAIR_KIT_PART_REPAIR = 5.0F;
    private static final float LOW_HEALTH_DECAY_THRESHOLD = 0.15F;
    private static final float LOW_HEALTH_DECAY_DAMAGE = 0.25F;
    private static final double GRAVITY = 0.08D;

    private final SimpleContainer inventory = new SimpleContainer(VehicleMenu.VEHICLE_SLOT_COUNT);
    private VehicleInput input = VehicleInput.EMPTY;
    private int repairCooldown;
    private int fireCooldown;
    private int decoyCooldown;
    private int energyRechargeTick;
    private float leftWheelHealth = PART_MAX_HEALTH;
    private float rightWheelHealth = PART_MAX_HEALTH;
    private float engineHealth = PART_MAX_HEALTH;

    public VehicleEntity(EntityType<? extends VehicleEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VEHICLE_ID, DefaultVehicleData.TEST_WHEEL.id().toString());
        builder.define(DATA_HEALTH, DefaultVehicleData.TEST_WHEEL.maxHealth());
        builder.define(DATA_ENERGY, DefaultVehicleData.TEST_WHEEL.maxEnergy());
        builder.define(DATA_RIFLE_AMMO, 0);
        builder.define(DATA_SELECTED_WEAPON, 0);
        builder.define(DATA_SELECTED_WEAPON_AMMO, 0);
        builder.define(DATA_LEFT_WHEEL_DAMAGED, false);
        builder.define(DATA_RIGHT_WHEEL_DAMAGED, false);
        builder.define(DATA_ENGINE_DAMAGED, false);
    }

    public VehicleData vehicleData() {
        return VehicleDataManager.get(ResourceLocation.parse(this.entityData.get(DATA_VEHICLE_ID)));
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
        return this.entityData.get(DATA_SELECTED_WEAPON_AMMO);
    }

    public ResourceLocation selectedVehicleWeaponId() {
        return this.selectedWeapon().weaponId();
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

    public boolean isFreeLookInputDown() {
        return this.input.freeLook();
    }

    public void processInput(ServerPlayer player, VehicleInput input) {
        if (player != this.getControllingPassenger()) {
            return;
        }
        if (input.switchWeapon()) {
            int weaponCount = this.vehicleData().defaults().weapons().size();
            if (weaponCount > 0) {
                this.entityData.set(DATA_SELECTED_WEAPON, (this.entityData.get(DATA_SELECTED_WEAPON) + 1) % weaponCount);
            }
        }
        if (input.deployDecoy()) {
            this.tryDeployDecoy(player);
        }
        this.input = input;
    }

    @Override
    public void tick() {
        super.tick();
        this.applyPassengerYaw();
        if (!this.level().isClientSide) {
            this.tickServerMovement();
            this.tickServerWeapon();
            this.tickDecoyCooldown();
            this.tickInventoryEnergyRecharge();
            this.tickAutoRepair();
            this.entityData.set(DATA_RIFLE_AMMO, this.countRifleAmmo());
            this.entityData.set(DATA_SELECTED_WEAPON_AMMO, this.countAmmo(this.selectedWeapon().ammoId()));
        }
        this.updateRiderPosition();
    }

    private void tickServerWeapon() {
        if (this.fireCooldown > 0) {
            this.fireCooldown--;
        }
        if (!this.input.fire() || this.fireCooldown > 0 || !(this.getControllingPassenger() instanceof LivingEntity shooter)) {
            return;
        }
        VehicleWeaponInfo weapon = this.selectedWeapon();
        GunStats stats = GunDefinitions.ALL.get(weapon.weaponId());
        if (stats == null || !this.consumeEnergy(weapon.energyCost()) || !this.consumeAmmo(weapon.ammoId())) {
            return;
        }
        Vec3 direction = shooter.getViewVector(1.0F).normalize();
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }
        if (weapon.guided()) {
            this.launchMissile(shooter, direction, stats);
            return;
        }
        Vec3 muzzle = this.position().add(0.0D, 0.9D, 0.0D).add(direction.scale(1.15D));
        BulletEntity bullet = new BulletEntity(this.level(), shooter, stats, direction.scale(stats.projectileSpeed()));
        bullet.initialisePosition(muzzle);
        this.level().addFreshEntity(bullet);
        if (this.level() instanceof ServerLevel serverLevel) {
            bullet.sendTrailToClients(serverLevel);
        }
        this.fireCooldown = Math.max(1, stats.fireDelay());
    }

    private void launchMissile(LivingEntity shooter, Vec3 direction, GunStats stats) {
        Vec3 muzzle = this.position().add(0.0D, 0.95D, 0.0D).add(direction.scale(1.25D));
        Vec3 velocity = direction.scale(0.72D).add(this.getDeltaMovement().scale(0.15D));
        this.level().addFreshEntity(new VehicleMissileEntity(this.level(), shooter, this.findLookTarget(shooter, direction, 64.0D), muzzle, velocity));
        this.fireCooldown = Math.max(1, stats.fireDelay());
    }

    @Nullable
    private Entity findLookTarget(LivingEntity shooter, Vec3 direction, double range) {
        Vec3 eye = shooter.getEyePosition();
        Entity bestTarget = null;
        double bestScore = 0.985D;
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

    private void tickDecoyCooldown() {
        if (this.decoyCooldown > 0) {
            this.decoyCooldown--;
        }
    }

    private void tryDeployDecoy(ServerPlayer player) {
        if (this.decoyCooldown > 0 || !this.consumeAmmo(FLARE_AMMO)) {
            return;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
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

    private boolean consumeInventoryItem(net.minecraft.world.item.Item item, int count) {
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

    private int countRifleAmmo() {
        return this.countAmmo(RIFLE_AMMO);
    }

    private int countAmmo(ResourceLocation ammoId) {
        var ammo = ModItems.AMMO.get(ammoId);
        if (ammo == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (stack.is(ammo.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean consumeAmmo(ResourceLocation ammoId) {
        var ammo = ModItems.AMMO.get(ammoId);
        if (ammo == null) {
            return false;
        }
        for (int slot = 0; slot < this.inventory.getContainerSize(); slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (!stack.is(ammo.get())) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                this.inventory.setItem(slot, ItemStack.EMPTY);
            }
            this.inventory.setChanged();
            return true;
        }
        return false;
    }

    private VehicleWeaponInfo selectedWeapon() {
        var weapons = this.vehicleData().defaults().weapons();
        if (weapons.isEmpty()) {
            return DefaultVehicleData.TEST_WHEEL.weapons().getFirst();
        }
        int index = Mth.clamp(this.entityData.get(DATA_SELECTED_WEAPON), 0, weapons.size() - 1);
        return weapons.get(index);
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
        if (repair <= 0.0F || this.vehicleHealth() >= this.maxVehicleHealth()) {
            return;
        }
        this.entityData.set(DATA_HEALTH, Math.min(this.maxVehicleHealth(), this.vehicleHealth() + repair));
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
        if (this.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER
                || this.vehicleData().defaults().vehicleType() == VehicleType.AIRCRAFT) {
            this.tickServerAirMovement(engine);
            return;
        }
        if (this.vehicleData().defaults().vehicleType() == VehicleType.BOAT) {
            this.tickServerBoatMovement(engine);
            return;
        }
        Vec3 velocity = this.getDeltaMovement();
        int forwardAxis = this.input.forwardAxis();
        int strafeAxis = this.input.strafeAxis();
        boolean grounded = this.onGround();
        double mobility = this.mobilityMultiplier();

        if (forwardAxis != 0 || strafeAxis != 0) {
            float yaw = this.getYRot();
            double yawRadians = Math.toRadians(yaw);
            Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
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

        this.setDeltaMovement(velocity);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.98D, 0.98D, 0.98D));
    }

    private void tickServerAirMovement(EngineInfo engine) {
        Vec3 velocity = this.getDeltaMovement();
        int forwardAxis = this.input.forwardAxis();
        int strafeAxis = this.input.strafeAxis();
        int verticalAxis = this.input.verticalAxis();
        double mobility = this.mobilityMultiplier();

        float yaw = this.getYRot();
        double yawRadians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 desired = forward.scale(forwardAxis).add(right.scale(strafeAxis * 0.65D)).add(0.0D, verticalAxis * 0.8D, 0.0D);
        if (desired.lengthSqr() > 1.0E-4D) {
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
        double mobility = this.mobilityMultiplier();

        if (forwardAxis != 0 || strafeAxis != 0) {
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
        if (passenger == null || this.input.freeLook()) {
            return;
        }
        this.setYRot(passenger.getYRot());
        this.yRotO = this.getYRot();
    }

    private void updateRiderPosition() {
        for (int index = 0; index < this.getPassengers().size(); index++) {
            Entity passenger = this.getPassengers().get(index);
            SeatInfo seat = this.seatForPassenger(index);
            Vec3 offset = this.rotateSeatOffset(seat);
            passenger.setPos(this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
        }
    }

    private Vec3 rotateSeatOffset(SeatInfo seat) {
        double yaw = Math.toRadians(this.getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double x = seat.x() * cos - seat.z() * sin;
        double z = seat.x() * sin + seat.z() * cos;
        return new Vec3(x, seat.y(), z);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        output.putString(TAG_VEHICLE_ID, this.entityData.get(DATA_VEHICLE_ID));
        output.putFloat(TAG_HEALTH, this.vehicleHealth());
        output.putInt(TAG_ENERGY, this.vehicleEnergy());
        output.putInt(TAG_REPAIR_COOLDOWN, this.repairCooldown);
        output.putFloat(TAG_LEFT_WHEEL_HEALTH, this.leftWheelHealth);
        output.putFloat(TAG_RIGHT_WHEEL_HEALTH, this.rightWheelHealth);
        output.putFloat(TAG_ENGINE_HEALTH, this.engineHealth);
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
        this.leftWheelHealth = input.contains(TAG_LEFT_WHEEL_HEALTH) ? input.getFloat(TAG_LEFT_WHEEL_HEALTH) : PART_MAX_HEALTH;
        this.rightWheelHealth = input.contains(TAG_RIGHT_WHEEL_HEALTH) ? input.getFloat(TAG_RIGHT_WHEEL_HEALTH) : PART_MAX_HEALTH;
        this.engineHealth = input.contains(TAG_ENGINE_HEALTH) ? input.getFloat(TAG_ENGINE_HEALTH) : PART_MAX_HEALTH;
        this.syncPartDamageFlags();
        if (input.contains(TAG_ITEMS)) {
            ContainerHelper.loadAllItems(input.getCompound(TAG_ITEMS), this.inventory.getItems(), this.level().registryAccess());
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
        float finalDamage = armorHit.finalDamage();
        if (finalDamage <= 0.0F) {
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
            SeatInfo seat = this.seatForPassenger(index);
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
            default -> {
            }
        }
        this.syncPartDamageFlags();
    }

    private void syncPartDamageFlags() {
        this.entityData.set(DATA_LEFT_WHEEL_DAMAGED, this.leftWheelHealth <= 0.0F);
        this.entityData.set(DATA_RIGHT_WHEEL_DAMAGED, this.rightWheelHealth <= 0.0F);
        this.entityData.set(DATA_ENGINE_DAMAGED, this.engineHealth <= 0.0F);
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
            ItemStack container = VehicleContainerBlockEntity.createItemFor(this);
            if (!serverPlayer.getInventory().add(container)) {
                serverPlayer.drop(container, false);
            }
            stack.hurtAndBreak(1, serverPlayer, LivingEntity.getSlotForHand(hand));
            this.discard();
            return InteractionResult.CONSUME;
        }
        if (!this.level().isClientSide && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this);
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
        return new VehicleMenu(containerId, playerInventory, this.inventory);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.vehicleData().defaults().seats().size();
    }

    public double getPassengersRidingOffset() {
        return 0.45D;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity passenger = null;
        for (int index = 0; index < this.getPassengers().size(); index++) {
            if (this.seatForPassenger(index).driver()) {
                passenger = this.getPassengers().get(index);
                break;
            }
        }
        if (passenger == null) {
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
