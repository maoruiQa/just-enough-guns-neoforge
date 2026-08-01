package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.item.GuidedLauncherItem;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class SpecialEquipmentClientEvents {
    private static final double LOCK_RANGE = 256.0D;
    private static final ResourceLocation JAVELIN_HUD = Reference.id("textures/overlay/javelin/javelin_hud.png");
    private static final ResourceLocation IGLA_HUD = Reference.id("textures/overlay/igla_9k38/igla_scope.png");
    private static final ResourceLocation DRONE_FOV = Reference.id("textures/overlay/drone/drone_fov.png");
    private static int lockTarget = Integer.MIN_VALUE;
    private static int lockTicks;
    private static int droneId = -1;
    private static int droneRange;
    private static float droneZoom = 1.0F;
    private static float playerYaw;
    private static float playerPitch;
    private static boolean payloadHeld;

    private SpecialEquipmentClientEvents() {}

    public static void setDroneControl(int entityId, boolean active, int maxRange) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active) {
            droneId = -1;
            droneRange = 0;
            droneZoom = 1.0F;
            if (minecraft.player != null) minecraft.setCameraEntity(minecraft.player);
            return;
        }
        droneId = entityId;
        droneRange = maxRange;
        if (minecraft.player != null) {
            playerYaw = minecraft.player.getYRot();
            playerPitch = minecraft.player.getXRot();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetLock();
            return;
        }

        tickLauncher(player);
        while (KeyBindings.LAUNCHER_MODE.consumeClick()) {
            if (player.getMainHandItem().getItem() instanceof GuidedLauncherItem launcher && !launcher.airOnly()) {
                NetworkHandler.sendToggleLauncherMode();
            }
        }

        if (droneId < 0) {
            return;
        }
        Entity camera = minecraft.level.getEntity(droneId);
        if (!(camera instanceof DroneEntity drone) || !drone.isAlive()) {
            setDroneControl(-1, false, 0);
            return;
        }
        minecraft.setCameraEntity(drone);

        float yawDelta = Mth.wrapDegrees(player.getYRot() - playerYaw);
        float pitchDelta = player.getXRot() - playerPitch;
        player.setYRot(playerYaw);
        player.setXRot(playerPitch);
        drone.setYRot(drone.getYRot() + yawDelta);
        drone.setXRot(Mth.clamp(drone.getXRot() + pitchDelta, -80.0F, 80.0F));

        int inputs = 0;
        if (minecraft.options.keyUp.isDown()) inputs |= DroneEntity.FORWARD;
        if (minecraft.options.keyDown.isDown()) inputs |= DroneEntity.BACK;
        if (minecraft.options.keyLeft.isDown()) inputs |= DroneEntity.LEFT;
        if (minecraft.options.keyRight.isDown()) inputs |= DroneEntity.RIGHT;
        if (minecraft.options.keyJump.isDown()) inputs |= DroneEntity.UP;
        if (minecraft.options.keyShift.isDown()) inputs |= DroneEntity.DOWN;
        boolean payload = minecraft.options.keyAttack.isDown();
        if (payload && !payloadHeld) inputs |= DroneEntity.ACTION_PAYLOAD;
        payloadHeld = payload;
        while (KeyBindings.DRONE_INTERACT.consumeClick()) inputs |= DroneEntity.ACTION_INTERACT;

        NetworkHandler.sendDroneInput(droneId, inputs, yawDelta, pitchDelta);
        player.input.leftImpulse = 0.0F;
        player.input.forwardImpulse = 0.0F;
        player.input.jumping = false;
        player.input.shiftKeyDown = false;
    }

    private static void tickLauncher(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GuidedLauncherItem launcher) || !AimingHandler.get().isAiming()) {
            resetLock();
            return;
        }
        int candidate = findTarget(player, launcher);
        NetworkHandler.sendGuidedLock(net.minecraft.world.InteractionHand.MAIN_HAND, candidate);
        int previousTicks = lockTicks;
        if (candidate == lockTarget) {
            lockTicks = Math.min(launcher.lockTicks(), lockTicks + 1);
        } else {
            lockTarget = candidate;
            lockTicks = candidate == Integer.MIN_VALUE ? 0 : 1;
            if (candidate != Integer.MIN_VALUE) {
                playLocal(player, Reference.id("item." + launcher.getStats().id().getPath() + ".locking"));
            }
        }
        if (previousTicks < launcher.lockTicks() && lockTicks == launcher.lockTicks()) {
            playLocal(player, Reference.id("item." + launcher.getStats().id().getPath() + ".locked"));
        }
    }

    private static int findTarget(LocalPlayer player, GuidedLauncherItem launcher) {
        if (!launcher.airOnly() && player.isShiftKeyDown()) {
            return -1;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        double minimumDot = Math.cos(Math.toRadians(launcher.airOnly() ? 20.0D : 10.0D));
        Entity best = null;
        double bestDot = minimumDot;
        AABB area = player.getBoundingBox().inflate(LOCK_RANGE);
        for (Entity target : player.level().getEntities(player, area, entity -> entity instanceof LivingEntity || entity instanceof VehicleEntity)) {
            if (!target.isAlive() || player.distanceToSqr(target) > LOCK_RANGE * LOCK_RANGE) continue;
            boolean airborne = !target.onGround() && !target.isInWater();
            if (launcher.airOnly() != airborne) continue;
            Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(eye).normalize();
            double dot = look.dot(aim);
            if (dot > bestDot && player.hasLineOfSight(target)) {
                bestDot = dot;
                best = target;
            }
        }
        return best == null ? Integer.MIN_VALUE : best.getId();
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onRenderHud(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || event.getName() == null || !"crosshair".equals(event.getName().getPath())) return;
        if (droneId >= 0 && minecraft.level != null && minecraft.level.getEntity(droneId) instanceof DroneEntity drone) {
            int x = event.getGuiGraphics().guiWidth() / 2;
            int y = event.getGuiGraphics().guiHeight() / 2;
            int distance = Mth.floor(player.distanceTo(drone));
            event.getGuiGraphics().blit(DRONE_FOV, event.getGuiGraphics().guiWidth() - 38, y - 120, 31, 240, 0.0F, 0.0F, 82, 256, 82, 256);
            event.getGuiGraphics().drawCenteredString(minecraft.font, "DRONE  " + drone.payloadName(), x, 12, 0xFF55FF55);
            event.getGuiGraphics().drawCenteredString(minecraft.font, "HP " + Mth.ceil(drone.health()) + "  DIST " + distance + "  ZOOM " + (int) droneZoom + "x", x, 24, 0xFFFFFFFF);
            if (droneRange > 0 && distance >= droneRange - 48) {
                event.getGuiGraphics().drawCenteredString(minecraft.font, Component.translatable("message.jeg.drone.range_warning"), x, y + 40, 0xFFFF5555);
            }
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GuidedLauncherItem launcher && AimingHandler.get().isAiming()) {
            int x = event.getGuiGraphics().guiWidth() / 2;
            int y = event.getGuiGraphics().guiHeight() / 2;
            int size = Math.min(event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight());
            int left = (event.getGuiGraphics().guiWidth() - size) / 2;
            int top = (event.getGuiGraphics().guiHeight() - size) / 2;
            ResourceLocation texture = launcher.airOnly() ? IGLA_HUD : JAVELIN_HUD;
            int sourceSize = launcher.airOnly() ? 1024 : 512;
            event.getGuiGraphics().blit(texture, left, top, size, size, 0.0F, 0.0F, sourceSize, sourceSize, sourceSize, sourceSize);
            int progress = Mth.floor(100.0F * lockTicks / launcher.lockTicks());
            String status = lockTarget == Integer.MIN_VALUE ? "NO TARGET" : progress >= 100 ? "LOCKED" : "LOCK " + progress + "%";
            event.getGuiGraphics().drawCenteredString(minecraft.font, status, x, y + 35, progress >= 100 ? 0xFF55FF55 : 0xFFFFAA00);
            if (!launcher.airOnly()) {
                String mode = GuidedLauncherItem.launcherMode(stack) == 0 ? "DIRECT" : "TOP";
                event.getGuiGraphics().drawCenteredString(minecraft.font, mode, x, y + 47, 0xFFFFFFFF);
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (droneId >= 0) event.setNewFovModifier(event.getNewFovModifier() / droneZoom);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (droneId >= 0 && event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (droneId < 0 || event.getScrollDeltaY() == 0.0D) return;
        droneZoom = Mth.clamp(droneZoom + (event.getScrollDeltaY() > 0.0D ? 1.0F : -1.0F), 1.0F, 6.0F);
        event.setCanceled(true);
    }

    private static void playLocal(LocalPlayer player, ResourceLocation id) {
        var sound = BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound != null) player.playSound(sound, 0.7F, 1.0F);
    }

    private static void resetLock() {
        lockTarget = Integer.MIN_VALUE;
        lockTicks = 0;
    }
}
