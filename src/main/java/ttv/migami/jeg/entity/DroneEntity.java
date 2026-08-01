package ttv.migami.jeg.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.SpecialExplosiveItem;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.util.SpecialExplosion;

public final class DroneEntity extends Entity {
    public static final int FORWARD = 1;
    public static final int BACK = 1 << 1;
    public static final int LEFT = 1 << 2;
    public static final int RIGHT = 1 << 3;
    public static final int UP = 1 << 4;
    public static final int DOWN = 1 << 5;
    public static final int ACTION_PAYLOAD = 1 << 6;
    public static final int ACTION_INTERACT = 1 << 7;

    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PAYLOAD = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);

    @Nullable private UUID ownerId;
    @Nullable private UUID controllerId;
    private int actionCooldown;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public DroneEntity(ServerLevel level, @Nullable Player owner, Vec3 position) {
        this(ModEntities.DRONE.get(), level);
        this.ownerId = owner == null ? null : owner.getUUID();
        this.setPos(position);
        this.setYRot(owner == null ? 0.0F : owner.getYRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEALTH, 5.0F);
        builder.define(PAYLOAD, 0);
    }

    public float health() {
        return this.entityData.get(HEALTH);
    }

    public String payloadName() {
        return switch (this.entityData.get(PAYLOAD)) {
            case 1 -> "C4";
            case 2 -> "TM-62";
            default -> "EMPTY";
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (this.actionCooldown > 0) this.actionCooldown--;
        if (!this.level().isClientSide) {
            this.validateController();
            if (this.isInWater() && this.tickCount % 20 == 0) {
                this.damage(1.0F, this.damageSources().drown());
            }
        }
        Vec3 before = this.getDeltaMovement();
        this.move(MoverType.SELF, before);
        if (!this.level().isClientSide && (this.horizontalCollision || this.verticalCollision) && before.lengthSqr() > 0.36D) {
            this.damage((float) (before.length() * 2.0D), this.damageSources().flyIntoWall());
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.86D));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.MONITOR.get())) {
            if (!this.level().isClientSide) {
                stack.set(ModDataComponents.DRONE_LINK.get(), this.getUUID().toString());
                player.displayClientMessage(Component.translatable("message.jeg.drone.linked"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.entityData.get(PAYLOAD) == 0 && stack.is(ModItems.C4_BOMB.get())) {
            if (!this.level().isClientSide) {
                this.entityData.set(PAYLOAD, 1);
                stack.consume(1, player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.entityData.get(PAYLOAD) == 0 && stack.is(ModItems.TM_62.get())) {
            if (!this.level().isClientSide) {
                this.entityData.set(PAYLOAD, 2);
                stack.consume(1, player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.level().isClientSide && player.isShiftKeyDown() && player.getUUID().equals(this.ownerId)) {
            this.stopControl(null);
            this.spawnAtLocation(ModItems.DRONE.get());
            this.dropPayload();
            this.discard();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void startControl(ServerPlayer player, ItemStack monitor) {
        if (this.ownerId != null && !this.ownerId.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.jeg.drone.not_owner"), true);
            return;
        }
        if (this.controllerId != null && !this.controllerId.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.jeg.drone.in_use"), true);
            return;
        }
        this.controllerId = player.getUUID();
        monitor.set(ModDataComponents.DRONE_CONTROLLING.get(), true);
        NetworkHandler.sendDroneControl(player, this.getId(), true, this.controlRange(player));
    }

    public void stopControl(@Nullable ServerPlayer expected) {
        if (this.controllerId == null || expected != null && !this.controllerId.equals(expected.getUUID())) {
            return;
        }
        ServerPlayer player = this.controller();
        if (player != null) {
            ItemStack monitor = linkedMonitor(player);
            if (!monitor.isEmpty()) monitor.remove(ModDataComponents.DRONE_CONTROLLING.get());
            NetworkHandler.sendDroneControl(player, this.getId(), false, 0);
        }
        this.controllerId = null;
        this.setDeltaMovement(Vec3.ZERO);
    }

    public void processInput(ServerPlayer player, int inputs, float yawDelta, float pitchDelta) {
        if (!player.getUUID().equals(this.controllerId) || linkedMonitor(player).isEmpty()) {
            return;
        }
        this.setYRot(this.getYRot() + Mth.clamp(yawDelta, -30.0F, 30.0F));
        this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(pitchDelta, -20.0F, 20.0F), -80.0F, 80.0F));

        Vec3 forward = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 desired = Vec3.ZERO;
        if ((inputs & FORWARD) != 0) desired = desired.add(forward);
        if ((inputs & BACK) != 0) desired = desired.subtract(forward);
        if ((inputs & RIGHT) != 0) desired = desired.add(right);
        if ((inputs & LEFT) != 0) desired = desired.subtract(right);
        if ((inputs & UP) != 0) desired = desired.add(0.0D, 1.0D, 0.0D);
        if ((inputs & DOWN) != 0) desired = desired.add(0.0D, -1.0D, 0.0D);
        if (desired.lengthSqr() > 0.0D) {
            desired = desired.normalize().scale(0.42D);
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.35D).add(desired.scale(0.65D)));

        if (this.actionCooldown == 0 && (inputs & ACTION_INTERACT) != 0) {
            this.remoteInteract(player);
            this.actionCooldown = 13;
        }
        if (this.actionCooldown == 0 && (inputs & ACTION_PAYLOAD) != 0) {
            this.activatePayload();
            this.actionCooldown = 13;
        }
    }

    private void remoteInteract(ServerPlayer player) {
        Vec3 start = this.position().add(0.0D, 0.15D, 0.0D);
        Vec3 end = start.add(Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(2.0D));
        Entity target = this.level().getEntities(this, new AABB(start, end).inflate(0.4D), Entity::isPickable)
                .stream().min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(this))).orElse(null);
        if (target != null) {
            player.attack(target);
            return;
        }
        BlockHitResult hit = this.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            this.level().getBlockState(hit.getBlockPos()).useWithoutItem(this.level(), player, hit);
        }
    }

    private void activatePayload() {
        int payload = this.entityData.get(PAYLOAD);
        if (payload == 1 && this.level() instanceof ServerLevel serverLevel) {
            SpecialExplosion.explode(serverLevel, this, this.ownerEntity(), 300.0F, 10.0D);
            this.discard();
        } else if (payload == 2 && this.level() instanceof ServerLevel serverLevel) {
            PlacedExplosiveEntity mine = new PlacedExplosiveEntity(serverLevel, SpecialExplosiveItem.Kind.TM_62, this.ownerPlayer(), this.position(), this.getYRot());
            serverLevel.addFreshEntity(mine);
            this.entityData.set(PAYLOAD, 0);
        }
    }

    private void validateController() {
        ServerPlayer player = this.controller();
        if (player == null) {
            this.controllerId = null;
            return;
        }
        int range = this.controlRange(player);
        if (!player.isAlive() || player.level() != this.level() || player.distanceToSqr(this) > (double) range * range || linkedMonitor(player).isEmpty()) {
            this.stopControl(player);
        }
    }

    private int controlRange(ServerPlayer player) {
        return Math.max(64, player.getServer().getPlayerList().getSimulationDistance() * 16);
    }

    private ItemStack linkedMonitor(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.MONITOR.get()) && this.getUUID().toString().equals(stack.get(ModDataComponents.DRONE_LINK.get()))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private ServerPlayer controller() {
        return this.controllerId != null && this.level() instanceof ServerLevel serverLevel
                ? serverLevel.getServer().getPlayerList().getPlayer(this.controllerId)
                : null;
    }

    @Nullable
    private Player ownerPlayer() {
        Entity owner = this.ownerEntity();
        return owner instanceof Player player ? player : null;
    }

    @Nullable
    private Entity ownerEntity() {
        return this.ownerId != null && this.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(this.ownerId) : null;
    }

    private void damage(float amount, DamageSource source) {
        float remaining = this.health() - amount;
        this.entityData.set(HEALTH, remaining);
        if (remaining <= 0.0F) {
            this.stopControl(null);
            this.dropPayload();
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && amount > 0.0F) {
            this.damage(amount, source);
        }
        return true;
    }

    private void dropPayload() {
        if (this.entityData.get(PAYLOAD) == 2 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(new PlacedExplosiveEntity(serverLevel, SpecialExplosiveItem.Kind.TM_62, this.ownerPlayer(), this.position(), this.getYRot()));
        } else if (this.entityData.get(PAYLOAD) == 1) {
            this.spawnAtLocation(ModItems.C4_BOMB.get());
        }
        this.entityData.set(PAYLOAD, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(HEALTH, tag.getFloat("Health"));
        this.entityData.set(PAYLOAD, tag.getInt("Payload"));
        if (tag.hasUUID("Owner")) this.ownerId = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", this.health());
        tag.putInt("Payload", this.entityData.get(PAYLOAD));
        if (this.ownerId != null) tag.putUUID("Owner", this.ownerId);
    }
}
