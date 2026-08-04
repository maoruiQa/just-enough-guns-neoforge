package ttv.migami.jeg.entity;

import java.util.UUID;
import javax.annotation.Nullable;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.SpecialExplosiveItem;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.util.SpecialExplosion;

public final class DroneEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
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
    private int holdTickX;
    private int holdTickY;
    private int holdTickZ;
    private double lastTickSpeed;
    private double lastTickVerticalSpeed;
    /** SW-style power / bank / pitch-rate state (mirrored client for prediction). */
    private float power = 0.02F;
    private float deltaRot;
    private float deltaXRot;
    /** SW body pitch (W/S tilt), separate from camera {@link #getXRot()}. */
    private float bodyPitch;
    private float bodyPitchO;
    private int pendingInputs;
    private float pendingMouseYaw;
    private float pendingMousePitch;
    private boolean hasPendingInput;
    /**
     * True while this client is FPV-controlling and predicting this drone.
     * When set, vanilla entity track packets must not snap the predicted pose (SW local-control rule).
     */
    private boolean clientLocalControl;
    /** SW-style soft network reconciliation for non-controller clients. */
    private static final int NETWORK_LERP_STEPS = 10;
    /** Hard-correct only if FPV prediction drifts this far from server (blocks^2). */
    private static final double LOCAL_CONTROL_SNAP_DISTANCE_SQR = 16.0D;
    private int networkLerpSteps;
    private double networkLerpX;
    private double networkLerpY;
    private double networkLerpZ;
    private float networkLerpYRot;
    private float networkLerpXRot;

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
            case 3 -> "Grenade";
            default -> "EMPTY";
        };
    }

    /** Required for right-click link / projectile hits (Entity default is false). */
    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // SW handleClientSync: soft-pull non-controllers; skip while local FPV prediction owns the pose
        this.handleNetworkReconcile();
        if (this.actionCooldown > 0) {
            this.actionCooldown--;
        }
        this.lastTickSpeed = this.getDeltaMovement().length();
        this.lastTickVerticalSpeed = this.getDeltaMovement().y;

        // SW baseTick: body pitch decays independently of camera pitch
        this.bodyPitchO = this.bodyPitch;
        this.bodyPitch *= 0.9F;

        boolean controlled = this.controllerId != null
                || (this.level().isClientSide() && (this.clientLocalControl || this.hasPendingInput));
        if (!this.level().isClientSide()) {
            this.validateController();
            controlled = this.controllerId != null;
            if (this.isInWater() && this.tickCount % 4 == 0) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 0.6D, 0.6D));
                this.damage(0.25F + (float) (2.0D * this.lastTickSpeed), this.damageSources().drown());
            }
        }

        // SW: flight physics every tick while controlled (or coast with drag when not)
        if (controlled && this.hasPendingInput) {
            this.applySuperbTravel(this.pendingInputs, this.pendingMouseYaw, this.pendingMousePitch);
            // Mouse is per-packet delta - apply once. Keys stay held until next packet.
            this.pendingMouseYaw = 0.0F;
            this.pendingMousePitch = 0.0F;
            this.pendingInputs &= ~(ACTION_PAYLOAD | ACTION_INTERACT);
        } else if (!controlled) {
            this.holdTickX = this.holdTickY = this.holdTickZ = 0;
            this.deltaRot *= 0.7F;
            this.deltaXRot *= 0.7F;
            this.power = Math.max(this.power - 0.005F, 0.0F);
            if (!this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.965D, 0.7D, 0.965D));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.8D, 1.0D, 0.8D));
                this.bodyPitch *= 0.7F;
            }
        }

        Vec3 before = this.getDeltaMovement();
        this.move(MoverType.SELF, before);
        if (!this.level().isClientSide() && (this.horizontalCollision || this.verticalCollision) && before.lengthSqr() > 0.04D) {
            // SW-ish collision damage
            if (this.verticalCollision && Math.abs(this.lastTickVerticalSpeed) > 1.0D) {
                this.damage((float) (20.0D * (Math.abs(this.lastTickVerticalSpeed) - 1.0D)
                        * (this.lastTickSpeed - 0.2D) * (this.lastTickSpeed - 0.2D)), this.damageSources().flyIntoWall());
            } else if (this.horizontalCollision && this.lastTickSpeed > 0.2D) {
                this.damage((float) (10.0D * (this.lastTickSpeed - 0.2D) * (this.lastTickSpeed - 0.2D)), this.damageSources().flyIntoWall());
            }
        }
    }

    /**
     * Client prediction: same input path as server packets so FPV feels responsive.
     * Marks this entity as locally controlled so network lerp cannot rubber-band the FPV camera.
     */
    public void clientPredictInput(int inputs, float mouseYaw, float mousePitch) {
        if (!this.level().isClientSide()) {
            return;
        }
        this.pendingInputs = inputs;
        this.pendingMouseYaw = mouseYaw;
        this.pendingMousePitch = mousePitch;
        this.hasPendingInput = true;
        this.clientLocalControl = true;
        this.networkLerpSteps = 0;
    }

    /**
     * Called when local FPV control ends so ghost prediction and lerp-suppression stop.
     */
    public void clearClientControl() {
        if (!this.level().isClientSide()) {
            return;
        }
        this.clientLocalControl = false;
        this.hasPendingInput = false;
        this.pendingInputs = 0;
        this.pendingMouseYaw = 0.0F;
        this.pendingMousePitch = 0.0F;
        this.holdTickX = this.holdTickY = this.holdTickZ = 0;
    }

    private boolean isLocallyPredictedControl() {
        return this.level().isClientSide() && this.clientLocalControl;
    }

    /**
     * SW-style soft reconcile for spectators / world view; no-op while FPV predicts locally.
     */
    private void handleNetworkReconcile() {
        if (!this.level().isClientSide()) {
            return;
        }
        if (this.clientLocalControl) {
            this.networkLerpSteps = 0;
            return;
        }
        if (this.networkLerpSteps <= 0) {
            return;
        }
        double x = this.getX() + (this.networkLerpX - this.getX()) / (double) this.networkLerpSteps;
        double y = this.getY() + (this.networkLerpY - this.getY()) / (double) this.networkLerpSteps;
        double z = this.getZ() + (this.networkLerpZ - this.getZ()) / (double) this.networkLerpSteps;
        float yRot = this.getYRot() + Mth.wrapDegrees(this.networkLerpYRot - this.getYRot()) / (float) this.networkLerpSteps;
        float xRot = this.getXRot() + (this.networkLerpXRot - this.getXRot()) / (float) this.networkLerpSteps;
        this.setPos(x, y, z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.networkLerpSteps--;
    }

    /**
     * Root hitch fix (26.x API): while FPV is predicting, ignore vanilla track snaps.
     * Otherwise soft-store the server pose for multi-step pull-in (SW-style).
     */
    @Override
    protected void lerpPositionAndRotationStep(int steps, double x, double y, double z, double yRot, double xRot) {
        if (!this.level().isClientSide()) {
            super.lerpPositionAndRotationStep(steps, x, y, z, yRot, xRot);
            return;
        }
        if (this.isLocallyPredictedControl()) {
            double dx = x - this.getX();
            double dy = y - this.getY();
            double dz = z - this.getZ();
            if (dx * dx + dy * dy + dz * dz > LOCAL_CONTROL_SNAP_DISTANCE_SQR) {
                this.setPos(x, y, z);
                this.setYRot((float) yRot);
                this.setXRot((float) xRot);
                this.setOldPosAndRot();
                this.networkLerpSteps = 0;
            }
            return;
        }
        this.networkLerpX = x;
        this.networkLerpY = y;
        this.networkLerpZ = z;
        this.networkLerpYRot = (float) yRot;
        this.networkLerpXRot = (float) xRot;
        this.networkLerpSteps = NETWORK_LERP_STEPS;
    }

    @Override
    public void lerpMotion(Vec3 motion) {
        if (this.isLocallyPredictedControl()) {
            return;
        }
        super.lerpMotion(motion);
    }

    /**
     * Superb Warfare-style interaction (main hand only):
     * - Monitor: link / sneak-unlink
     * - Sneak + empty/crowbar: dismantle drone, return drone + payload items
     * - Empty hand: unload one payload
     * - C4 / TM-62 / grenade: load when empty, or replace a different payload (old item returned)
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        // SW always reads main hand for load/dismantle/monitor
        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModItems.MONITOR.get())) {
            if (!this.level().isClientSide()) {
                if (!player.isShiftKeyDown()) {
                    String existing = stack.get(ModDataComponents.DRONE_LINK.get());
                    if (existing != null && !existing.equals(this.getUUID().toString())) {
                        player.sendSystemMessage(Component.translatable("message.jeg.monitor.already_linked"));
                    } else if (existing != null && existing.equals(this.getUUID().toString())) {
                        player.sendSystemMessage(Component.translatable("message.jeg.drone.already_linked"));
                    } else {
                        stack.set(ModDataComponents.DRONE_LINK.get(), this.getUUID().toString());
                        this.ownerId = player.getUUID();
                        player.sendSystemMessage(Component.translatable("message.jeg.drone.linked"));
                    }
                } else {
                    if (this.getUUID().toString().equals(stack.get(ModDataComponents.DRONE_LINK.get()))) {
                        stack.remove(ModDataComponents.DRONE_LINK.get());
                        stack.remove(ModDataComponents.DRONE_CONTROLLING.get());
                        if (player instanceof ServerPlayer serverPlayer) {
                            this.stopControl(serverPlayer);
                        }
                        player.sendSystemMessage(Component.translatable("message.jeg.drone.unlinked"));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.jeg.drone.already_linked"));
                    }
                }
            }
        } else if (player.isShiftKeyDown()) {
            // SW: crouch + empty hand or crowbar dismantles (no owner gate)
            if (stack.isEmpty() || stack.is(ModItems.CROWBAR.get())) {
                if (!this.level().isClientSide()) {
                    this.stopControl(null);
                    if (!player.getAbilities().instabuild) {
                        giveOrDrop(player, new ItemStack(ModItems.DRONE.get()));
                        ItemStack payload = this.payloadAsItem();
                        if (!payload.isEmpty()) {
                            giveOrDrop(player, payload);
                        }
                    }
                    this.unlinkMonitors();
                    this.entityData.set(PAYLOAD, 0);
                    this.discard();
                }
            }
        } else if (stack.isEmpty()) {
            // SW: empty hand unloads one ammo unit
            if (!this.level().isClientSide() && this.entityData.get(PAYLOAD) != 0) {
                ItemStack payload = this.payloadAsItem();
                String unloadedName = this.payloadName();
                this.entityData.set(PAYLOAD, 0);
                if (!player.getAbilities().instabuild && !payload.isEmpty()) {
                    giveOrDrop(player, payload);
                }
                player.sendSystemMessage(Component.translatable("message.jeg.drone.payload_unloaded", unloadedName));
            }
        } else {
            int newType = payloadTypeOf(stack);
            if (newType != 0) {
                // Load when empty; if another payload type is held, swap (return old item)
                if (!this.level().isClientSide()) {
                    int current = this.entityData.get(PAYLOAD);
                    if (current == 0) {
                        this.entityData.set(PAYLOAD, newType);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        player.sendSystemMessage(Component.translatable("message.jeg.drone.payload_loaded", this.payloadName()));
                    } else if (current != newType) {
                        ItemStack old = this.payloadAsItem();
                        String oldName = this.payloadName();
                        this.entityData.set(PAYLOAD, newType);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            if (!old.isEmpty()) {
                                giveOrDrop(player, old);
                            }
                        }
                        player.sendSystemMessage(Component.translatable("message.jeg.drone.payload_swapped", oldName, this.payloadName()));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.jeg.drone.payload_full"));
                    }
                }
            }
        }

        // SW always consumes the interaction
        return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private ItemStack payloadAsItem() {
        return switch (this.entityData.get(PAYLOAD)) {
            case 1 -> {
                ItemStack c4 = new ItemStack(ModItems.C4_BOMB.get());
                c4.set(ModDataComponents.C4_REMOTE.get(), true);
                yield c4;
            }
            case 2 -> new ItemStack(ModItems.TM_62.get());
            case 3 -> new ItemStack(ModItems.AMMO.get(Reference.id("grenade")).get());
            default -> ItemStack.EMPTY;
        };
    }

    private static int payloadTypeOf(ItemStack stack) {
        if (stack.is(ModItems.C4_BOMB.get())) {
            return 1;
        }
        if (stack.is(ModItems.TM_62.get())) {
            return 2;
        }
        var grenade = ModItems.AMMO.get(Reference.id("grenade"));
        if (grenade != null && stack.is(grenade.get())) {
            return 3;
        }
        return 0;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void unlinkMonitors() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String id = this.getUUID().toString();
        for (ServerPlayer online : serverLevel.getServer().getPlayerList().getPlayers()) {
            for (int i = 0; i < online.getInventory().getContainerSize(); i++) {
                ItemStack inv = online.getInventory().getItem(i);
                if (inv.is(ModItems.MONITOR.get()) && id.equals(inv.get(ModDataComponents.DRONE_LINK.get()))) {
                    inv.remove(ModDataComponents.DRONE_LINK.get());
                    inv.remove(ModDataComponents.DRONE_CONTROLLING.get());
                }
            }
        }
    }

    public void startControl(ServerPlayer player, ItemStack monitor) {
        if (this.ownerId != null && !this.ownerId.equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.jeg.drone.not_owner"));
            return;
        }
        if (this.controllerId != null && !this.controllerId.equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.jeg.drone.in_use"));
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
        this.hasPendingInput = false;
        this.pendingInputs = 0;
        this.holdTickX = this.holdTickY = this.holdTickZ = 0;
        this.setDeltaMovement(Vec3.ZERO);
    }

    public void processInput(ServerPlayer player, int inputs, float yawDelta, float pitchDelta) {
        if (!player.getUUID().equals(this.controllerId) || linkedMonitor(player).isEmpty()) {
            return;
        }
        this.pendingInputs = inputs;
        this.pendingMouseYaw = yawDelta;
        this.pendingMousePitch = pitchDelta;
        this.hasPendingInput = true;

        // One-shot actions on packet (server authoritative)
        if (this.actionCooldown == 0 && (inputs & ACTION_INTERACT) != 0) {
            this.remoteInteract(player);
            this.actionCooldown = 13;
        }
        if (this.actionCooldown == 0 && (inputs & ACTION_PAYLOAD) != 0) {
            this.activatePayload();
            this.actionCooldown = 13;
        }
    }

    /**
     * Superb Warfare bank/pitch rates + mouse look, with true hover when no vertical input.
     * Camera pitch ({@link #getXRot()}) never steers thrust; body tilt is {@link #bodyPitch}.
     */
    private void applySuperbTravel(int inputs, float mouseYaw, float mousePitch) {
        boolean forward = (inputs & FORWARD) != 0;
        boolean back = (inputs & BACK) != 0;
        boolean left = (inputs & LEFT) != 0;
        boolean right = (inputs & RIGHT) != 0;
        boolean up = (inputs & UP) != 0;
        boolean down = (inputs & DOWN) != 0;
        boolean verticalControl = up || down;

        if (!this.onGround()) {
            // left / right -> bank rate (DELTA_ROT)
            if (right) {
                this.holdTickX = Math.min(this.holdTickX + 1, 5);
                this.deltaRot -= 0.3F * this.holdTickX;
            } else if (left) {
                this.holdTickX = Math.min(this.holdTickX + 1, 5);
                this.deltaRot += 0.3F * this.holdTickX;
            } else {
                this.holdTickX = 0;
            }

            // forward / back -> pitch rate (DELTA_X_ROT)
            if (forward) {
                this.holdTickZ = Math.min(this.holdTickZ + 1, 5);
                this.deltaXRot -= 0.3F * this.holdTickZ;
            } else if (back) {
                this.holdTickZ = Math.min(this.holdTickZ + 1, 5);
                this.deltaXRot += 0.3F * this.holdTickZ;
            } else {
                this.holdTickZ = 0;
            }

            // Horizontal air drag; vertical handled separately for hover
            Vec3 motion = this.getDeltaMovement();
            if (verticalControl) {
                this.setDeltaMovement(motion.multiply(0.965D, 0.7D, 0.965D));
            } else {
                // Hover: keep XZ drag, zero vertical drift from previous frame
                this.setDeltaMovement(motion.x * 0.965D, 0.0D, motion.z * 0.965D);
            }
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8D, 1.0D, 0.8D));
            this.setXRot(this.getXRot() * 0.7F);
            this.bodyPitch *= 0.7F;
            this.holdTickX = 0;
            this.holdTickZ = 0;
        }

        // vertical control only when player presses up/down
        if (up) {
            this.holdTickY = Math.min(this.holdTickY + 1, 5);
            this.power = Math.min(this.power + 0.01F * this.holdTickY, 0.2F);
            this.setDeltaMovement(new Vec3(
                    this.getDeltaMovement().x,
                    0.05D * this.holdTickY,
                    this.getDeltaMovement().z
            ));
        } else if (down) {
            this.holdTickY = Math.min(this.holdTickY + 1, 5);
            // Stronger power cut than stock SW 0.02/0.06 so Shift actually sinks
            this.power = Math.max(this.power - 0.03F * this.holdTickY, this.onGround() ? 0.0F : 0.03F);
            this.setDeltaMovement(new Vec3(
                    this.getDeltaMovement().x,
                    // 1.33x prior max descent (-0.05 * holdTickY)
                    -0.0665D * this.holdTickY,
                    this.getDeltaMovement().z
            ));
        } else {
            this.holdTickY = 0;
            this.power = 0.0F;
        }

        // decay bank/pitch rates; integrate residual into body pitch (SW setBodyXRot)
        this.deltaRot *= 0.7F;
        this.deltaXRot *= 0.7F;
        this.bodyPitch = Mth.clamp(this.bodyPitch - this.deltaXRot, -30.0F, 30.0F);

        // Rotor lift only while climbing/descending - hover has no free lift (prevents drift)
        if (verticalControl) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, this.power * 0.6D, 0.0D));
        }

        // SW getRightDirection: (cos(-yaw), 0, sin(yaw)) - NOT MC "yaw+90"
        // right key makes DELTA_ROT negative -> * this vector = strafe right in camera space
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 swRight = new Vec3(Mth.cos(-yawRad), 0.0D, Mth.sin(yawRad));
        Vec3 side = swRight.scale(this.deltaRot * 0.017D);
        this.setDeltaMovement(this.getDeltaMovement().add(side.x, 0.0D, side.z));

        // SW getForwardDirection: always horizontal — camera pitch does not tilt thrust
        Vec3 swForward = new Vec3(Mth.sin(-yawRad), 0.0D, Mth.cos(yawRad));
        Vec3 thrust = swForward.scale(-this.deltaXRot * 0.017D);
        this.setDeltaMovement(this.getDeltaMovement().add(thrust.x, 0.0D, thrust.z));
        if (!verticalControl) {
            // hard lock altitude while hovering (no up/down keys)
            Vec3 m = this.getDeltaMovement();
            this.setDeltaMovement(m.x, 0.0D, m.z);
        }

        // mouse look only (SW: 0.5 * mouse speed, pitch clamp -10..90) — does not affect flight
        this.setYRot(this.getYRot() + 0.5F * Mth.clamp(mouseYaw, -40.0F, 40.0F));
        this.setXRot(Mth.clamp(this.getXRot() + 0.5F * Mth.clamp(mousePitch, -40.0F, 40.0F), -10.0F, 90.0F));
    }

    /** SW body pitch used for model tilt (W/S), not FPV camera pitch. */
    public float getBodyPitch() {
        return this.bodyPitch;
    }

    public float getBodyPitch(float partialTick) {
        return Mth.lerp(0.6F * partialTick, this.bodyPitchO, this.bodyPitch);
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
            // C4 kamikaze: consume payload first so blast death does not drop C4
            this.entityData.set(PAYLOAD, 0);
            ServerPlayer controller = this.controller();
            if (controller != null) {
                this.stopControl(controller);
            }
            SpecialExplosion.explode(
                    serverLevel,
                    this,
                    this.ownerEntity(),
                    PlacedExplosiveEntity.C4_REMOTE_DAMAGE,
                    PlacedExplosiveEntity.C4_REMOTE_RADIUS,
                    SpecialExplosion.Tier.HUGE
            );
            this.discard();
        } else if (payload == 2 && this.level() instanceof ServerLevel serverLevel) {
            this.entityData.set(PAYLOAD, 0);
            serverLevel.addFreshEntity(PlacedExplosiveEntity.placeSettled(
                    serverLevel, SpecialExplosiveItem.Kind.TM_62, this.ownerPlayer(), this.position(), this.getYRot(), false));
        } else if (payload == 3 && this.level() instanceof ServerLevel serverLevel) {
            // Drop a thrown hand grenade along look direction (left-click payload).
            this.entityData.set(PAYLOAD, 0);
            LivingEntity owner = this.ownerPlayer();
            if (owner == null) {
                owner = this.controller();
            }
            GrenadeEntity grenade = owner != null
                    ? new GrenadeEntity(serverLevel, owner, 4.0F, 60, false)
                    : new GrenadeEntity(ModEntities.GRENADE.get(), serverLevel);
            if (owner == null) {
                grenade.setExplosionPower(4.0F);
                grenade.setFuse(60);
            }
            Vec3 look = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
            Vec3 spawn = this.position().add(0.0D, 0.1D, 0.0D).add(look.scale(0.35D));
            grenade.initialisePosition(spawn);
            grenade.setDeltaMovement(look.scale(1.25D).add(this.getDeltaMovement().scale(0.5D)));
            grenade.setYRot(this.getYRot());
            grenade.setXRot(this.getXRot());
            serverLevel.addFreshEntity(grenade);
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
        return Math.max(64, player.level().getServer().getPlayerList().getViewDistance() * 16);
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (amount > 0.0F) {
            this.damage(amount, source);
        }
        return true;
    }

    /** On destroy: drop payload as items (not place live mines). */
    private void dropPayload() {
        ItemStack payload = this.payloadAsItem();
        if (!payload.isEmpty() && this.level() instanceof ServerLevel serverLevel) {
            this.spawnAtLocation(serverLevel, payload);
        }
        this.entityData.set(PAYLOAD, 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(HEALTH, input.getFloatOr("Health", 5.0F));
        this.entityData.set(PAYLOAD, input.getIntOr("Payload", 0));
        this.ownerId = input.read("Owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Health", this.health());
        output.putInt("Payload", this.entityData.get(PAYLOAD));
        if (this.ownerId != null) {
            output.store("Owner", net.minecraft.core.UUIDUtil.CODEC, this.ownerId);
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
