package ttv.migami.jeg.client;

import net.minecraft.client.CameraType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.audio.DroneEngineSoundInstance;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.util.ScreenProjection;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GuidedLauncherItem;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

/**
 * Client lock, FPV drone control, and SW-style layered HUDs for special equipment.
 */
public final class SpecialEquipmentClientEvents {
    private static final double LOCK_RANGE = 256.0D;
    private static final double JAVELIN_MAX_TARGET_HEIGHT = 64.0D;
    private static final double IGLA_MIN_TARGET_HEIGHT = 16.0D;

    private static final Identifier JAVELIN_HUD = Reference.id("textures/overlay/javelin/javelin_hud.png");
    private static final Identifier JAVELIN_DIR = Reference.id("textures/overlay/javelin/dir.png");
    private static final Identifier JAVELIN_TOP = Reference.id("textures/overlay/javelin/top.png");
    private static final Identifier JAVELIN_MISSILE_GREEN = Reference.id("textures/overlay/javelin/missile_green.png");
    private static final Identifier JAVELIN_MISSILE_RED = Reference.id("textures/overlay/javelin/missile_red.png");
    private static final Identifier JAVELIN_SEEK = Reference.id("textures/overlay/javelin/seek.png");
    private static final Identifier FRAME = Reference.id("textures/overlay/frame/frame.png");
    private static final Identifier FRAME_TARGET = Reference.id("textures/overlay/frame/frame_target_triangle.png");
    private static final Identifier FRAME_LOCK = Reference.id("textures/overlay/frame/frame_lock.png");
    private static final Identifier IGLA_HUD = Reference.id("textures/overlay/igla_9k38/igla_scope.png");
    private static final Identifier IGLA_HOLD = Reference.id("textures/overlay/igla_9k38/hold.png");
    private static final Identifier IGLA_SHOOT = Reference.id("textures/overlay/igla_9k38/shoot.png");
    private static final Identifier IGLA_PART_1 = Reference.id("textures/overlay/igla_9k38/part_1.png");
    private static final Identifier IGLA_PART_2 = Reference.id("textures/overlay/igla_9k38/part_2.png");
    private static final Identifier IGLA_PART_3 = Reference.id("textures/overlay/igla_9k38/part_3.png");
    private static final Identifier IGLA_PART_4 = Reference.id("textures/overlay/igla_9k38/part_4.png");
    private static final Identifier DRONE_FOV = Reference.id("textures/overlay/drone/drone_fov.png");
    private static final Identifier DRONE_FOV_MOVE = Reference.id("textures/overlay/drone/drone_fov_move.png");
    private static final Identifier DRONE_CROSSHAIR = Reference.id("textures/overlay/vehicle/crosshair/third_camera.png");
    private static final Identifier TV_FRAME = Reference.id("textures/overlay/vehicle/land/tv_frame.png");

    private static int lockTarget = Integer.MIN_VALUE;
    private static int lockTicks;
    private static int droneId = -1;
    private static int droneRange;
    private static float droneZoom = 1.0F;
    private static float playerYaw;
    private static float playerPitch;
    private static boolean payloadHeld;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    /** All active drone engine loops (FPV + airborne world drones). */
    private static final java.util.Map<Integer, DroneEngineSoundInstance> DRONE_ENGINE_SOUNDS = new java.util.HashMap<>();

    private SpecialEquipmentClientEvents() {}

    private static boolean registered;

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
    }

    public static void setDroneControl(int entityId, boolean active, int maxRange) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active) {
            int previous = droneId;
            droneId = -1;
            droneRange = 0;
            droneZoom = 1.0F;
            // Drop FPV mode; world loop may keep running if still airborne
            DroneEngineSoundInstance existing = DRONE_ENGINE_SOUNDS.get(previous);
            if (existing != null) {
                existing.setFpvMode(false);
            }
            if (minecraft.player != null) {
                minecraft.setCameraEntity(minecraft.player);
                minecraft.options.setCameraType(previousCameraType);
            }
            return;
        }
        droneId = entityId;
        droneRange = maxRange;
        if (minecraft.player != null) {
            playerYaw = minecraft.player.getYRot();
            playerPitch = minecraft.player.getXRot();
            previousCameraType = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    public static boolean isControllingDrone() {
        return droneId >= 0;
    }

    /**
     * Engine should hum while FPV-controlling (including hands-off hover) or while airborne in the world.
     */
    private static boolean shouldPlayDroneEngine(DroneEntity drone) {
        if (!drone.isAlive() || drone.isRemoved()) {
            return false;
        }
        if (drone.getId() == droneId) {
            return true; // FPV: always, including pure hover
        }
        // Uncontrolled / not in FPV: still hum while in the air (hover / flight)
        return !drone.onGround();
    }

    private static void tickDroneEngineAudio(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            DRONE_ENGINE_SOUNDS.values().forEach(DroneEngineSoundInstance::beginStop);
            DRONE_ENGINE_SOUNDS.clear();
            return;
        }

        SoundEvent engine = BuiltInRegistries.SOUND_EVENT.getOptional(Reference.id("entity.drone.engine")).orElse(null);
        java.util.Set<Integer> keep = new java.util.HashSet<>();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof DroneEntity drone)) {
                continue;
            }
            int id = drone.getId();
            if (!shouldPlayDroneEngine(drone)) {
                continue;
            }
            keep.add(id);
            boolean fpv = id == droneId;
            DroneEngineSoundInstance existing = DRONE_ENGINE_SOUNDS.get(id);
            if (existing != null && minecraft.getSoundManager().isActive(existing)) {
                existing.setFpvMode(fpv);
                existing.keepAlive();
                continue;
            }
            if (engine == null) {
                continue;
            }
            if (existing != null) {
                minecraft.getSoundManager().stop(existing);
            }
            DroneEngineSoundInstance instance = new DroneEngineSoundInstance(engine, id, fpv);
            DRONE_ENGINE_SOUNDS.put(id, instance);
            minecraft.getSoundManager().play(instance);
        }

        DRONE_ENGINE_SOUNDS.entrySet().removeIf(entry -> {
            if (keep.contains(entry.getKey()) && minecraft.getSoundManager().isActive(entry.getValue())) {
                return false;
            }
            entry.getValue().beginStop();
            if (!minecraft.getSoundManager().isActive(entry.getValue())) {
                minecraft.getSoundManager().stop(entry.getValue());
            }
            return true;
        });
    }

    public static void onClientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetLock();
            DRONE_ENGINE_SOUNDS.values().forEach(DroneEngineSoundInstance::beginStop);
            DRONE_ENGINE_SOUNDS.clear();
            return;
        }

        tickLauncher(player);
        while (KeyBindings.LAUNCHER_MODE.consumeClick()) {
            if (player.getMainHandItem().getItem() instanceof GuidedLauncherItem launcher && !launcher.airOnly()) {
                ClientNetworkHandler.sendToggleLauncherMode();
            }
        }

        // Always maintain world/FPV engine loops (including hands-off hover)
        tickDroneEngineAudio(player);

        if (droneId < 0) {
            return;
        }
        Entity camera = minecraft.level.getEntity(droneId);
        if (!(camera instanceof DroneEntity drone) || !drone.isAlive()) {
            setDroneControl(-1, false, 0);
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (!main.is(ModItems.MONITOR.get()) || !drone.getUUID().toString().equals(main.get(ModDataComponents.DRONE_LINK.get()))) {
            setDroneControl(-1, false, 0);
            return;
        }

        minecraft.setCameraEntity(drone);

        // Capture mouse delta then lock player look (SW free-look on drone)
        float yawDelta = Mth.wrapDegrees(player.getYRot() - playerYaw);
        float pitchDelta = player.getXRot() - playerPitch;
        player.setYRot(playerYaw);
        player.setXRot(playerPitch);

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

        // Client prediction: SW travel (do not double-apply mouse here; travel uses 0.5 * mouse)
        drone.clientPredictInput(inputs, yawDelta, pitchDelta);
        ClientNetworkHandler.sendDroneInput(droneId, inputs, yawDelta, pitchDelta);
        player.input.keyPresses = net.minecraft.world.entity.player.Input.EMPTY;
        player.xxa = 0.0F;
        player.zza = 0.0F;
        // movement already zeroed via xxa/zza/keyPresses
    }

    private static void tickLauncher(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GuidedLauncherItem launcher) || !AimingHandler.get().isAiming()) {
            resetLock();
            return;
        }
        int candidate = findTarget(player, launcher);
        ClientNetworkHandler.sendGuidedLock(net.minecraft.world.InteractionHand.MAIN_HAND, candidate);
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
            return -1; // ground-point lock
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        double minimumDot = Math.cos(Math.toRadians(launcher.lockAngleDegrees()));
        Entity best = null;
        double bestDot = minimumDot;
        AABB area = player.getBoundingBox().inflate(LOCK_RANGE);
        for (Entity target : player.level().getEntities(player, area, entity -> entity instanceof LivingEntity || entity instanceof VehicleEntity)) {
            if (!target.isAlive() || player.distanceToSqr(target) > LOCK_RANGE * LOCK_RANGE) continue;
            if (target.getVehicle() != null) continue; // SW noVehicle(): not riding something
            if (ttv.migami.jeg.util.SmokeUtil.isSmokeBlockingLock(player, target)) continue;
            double heightDelta = target.getY() - player.getY();
            if (launcher.airOnly()) {
                boolean airVehicle = target instanceof VehicleEntity vehicle
                        && (vehicle.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER
                        || vehicle.vehicleData().defaults().vehicleType() == VehicleType.AIRCRAFT);
                boolean airborne = !target.onGround() && !target.isInWater();
                if (!airVehicle && !airborne) continue;
                if (heightDelta < IGLA_MIN_TARGET_HEIGHT && !airVehicle && target.onGround()) continue;
            } else {
                // SW ground seek: living on ground / surface vehicles
                if (target instanceof VehicleEntity vehicle) {
                    VehicleType type = vehicle.vehicleData().defaults().vehicleType();
                    if (type == VehicleType.HELICOPTER || type == VehicleType.AIRCRAFT) continue;
                } else if (!(target instanceof LivingEntity) || (!target.onGround() && !target.isInWater())) {
                    continue;
                }
                if (heightDelta > JAVELIN_MAX_TARGET_HEIGHT) continue;
            }
            Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(eye).normalize();
            double dot = look.dot(aim);
            if (dot > bestDot && player.hasLineOfSight(target)) {
                bestDot = dot;
                best = target;
            }
        }
        return best == null ? Integer.MIN_VALUE : best.getId();
    }

    public static void onRenderHud(GuiGraphicsExtractor guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        if (droneId >= 0 && minecraft.level != null && minecraft.level.getEntity(droneId) instanceof DroneEntity drone) {
            renderDroneHud(guiGraphics, minecraft, player, drone);
            return;
        }

        float ads = AimingHandler.get().getRenderAdsProgress();
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GuidedLauncherItem launcher && ads > 0.8F
                && minecraft.options.getCameraType().isFirstPerson()) {
            if (launcher.airOnly()) {
                renderIglaHud(guiGraphics, minecraft, player, launcher, stack);
            } else {
                renderJavelinHud(guiGraphics, minecraft, player, launcher, stack);
            }
        }
    }

    private static void renderJavelinHud(GuiGraphicsExtractor g, Minecraft minecraft, LocalPlayer player, GuidedLauncherItem launcher, ItemStack stack) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        float f = Math.min(w, h);
        float scale = Math.min(w / f, h / f) * 1.35F;
        float size = Mth.floor(f * scale);
        float left = (w - size) / 2.0F;
        float top = (h - size) / 2.0F;
        float right = left + size;
        float bottom = top + size;

        blit(g, JAVELIN_HUD, left, top, size, size, 512);
        blit(g, GuidedLauncherItem.launcherMode(stack) == 0 ? JAVELIN_DIR : JAVELIN_TOP, left, top, size, size, 512);
        boolean hasAmmo = stack.getOrDefault(ModDataComponents.GUN_AMMO.get(), 0) > 0;
        blit(g, hasAmmo ? JAVELIN_MISSILE_GREEN : JAVELIN_MISSILE_RED, left, top, size, size, 512);
        if (lockTicks > 1 && lockTicks < launcher.lockTicks()) {
            blit(g, JAVELIN_SEEK, left, top, size, size, 512);
        }

        // SW black side bars
        g.fill( 0, Mth.floor(top), Mth.floor(left) + 3, Mth.floor(bottom), 0xFF000000);
        g.fill( Mth.floor(right), Mth.floor(top), w, Mth.floor(bottom), 0xFF000000);

        // Target frames in FOV (SW SeekTool list)
        drawSeekFrames(g, player, launcher, false);

        int x = w / 2;
        int y = h / 2;
        int progress = Mth.floor(100.0F * lockTicks / Math.max(1, launcher.lockTicks()));
        String status = lockTarget == Integer.MIN_VALUE ? "NO TARGET" : progress >= 100 ? "LOCKED" : "LOCK " + progress + "%";
        g.text(minecraft.font, status, x - minecraft.font.width(status) / 2, y + 48, progress >= 100 ? 0xFF55FF55 : 0xFFFFAA00, false);
    }

    private static void renderIglaHud(GuiGraphicsExtractor g, Minecraft minecraft, LocalPlayer player, GuidedLauncherItem launcher, ItemStack stack) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        float f = Math.min(w, h);
        float scale = Math.min(w / f, h / f) * 1.35F;
        float size = Mth.floor(f * scale);
        float left = (w - size) / 2.0F;
        float top = (h - size) / 2.0F;
        blit(g, IGLA_HUD, left - size * 0.5F, top - size * 0.5F, size * 2.0F, size * 2.0F, 1024);

        float progress = lockTicks / (float) Math.max(1, launcher.lockTicks());
        int cx = w / 2;
        int cy = h / 2;
        if (progress > 0.15F) blitCentered(g, IGLA_PART_1, cx, cy, 32, 32);
        if (progress > 0.4F) blitCentered(g, IGLA_PART_2, cx, cy, 32, 32);
        if (progress > 0.65F) blitCentered(g, IGLA_PART_3, cx, cy, 32, 32);
        if (progress > 0.9F) blitCentered(g, IGLA_PART_4, cx, cy, 32, 32);
        blitCentered(g, progress >= 1.0F ? IGLA_SHOOT : IGLA_HOLD, cx, cy + 40, 48, 16);

        drawSeekFrames(g, player, launcher, true);

        int pct = Mth.floor(100.0F * progress);
        String status = lockTarget == Integer.MIN_VALUE ? "NO TARGET" : pct >= 100 ? "LOCKED" : "LOCK " + pct + "%";
        g.text(minecraft.font, status, cx - minecraft.font.width(status) / 2, cy + 56, pct >= 100 ? 0xFF55FF55 : 0xFFFFAA00, false);
    }

    private static void drawSeekFrames(GuiGraphicsExtractor g, LocalPlayer player, GuidedLauncherItem launcher, boolean airOnly) {
        Vec3 eye = player.getEyePosition();
        double minDot = Math.cos(Math.toRadians(launcher.lockAngleDegrees() + 5.0D));
        for (Entity target : player.level().getEntities(player, player.getBoundingBox().inflate(LOCK_RANGE),
                e -> e instanceof LivingEntity || e instanceof VehicleEntity)) {
            if (!target.isAlive() || player.distanceToSqr(target) > LOCK_RANGE * LOCK_RANGE) continue;
            if (target.getVehicle() != null) continue;
            boolean airborne = !target.onGround() && !target.isInWater();
            boolean airVehicle = target instanceof VehicleEntity v
                    && (v.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER
                    || v.vehicleData().defaults().vehicleType() == VehicleType.AIRCRAFT);
            if (airOnly) {
                if (!airVehicle && !airborne) continue;
            } else {
                if (airVehicle || (airborne && !(target instanceof VehicleEntity))) continue;
            }
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (!ScreenProjection.canSee(center)) continue;
            Vec3 to = center.subtract(eye).normalize();
            if (player.getViewVector(1.0F).dot(to) < minDot) continue;
            Vec3 screen = ScreenProjection.worldToScreen(center);
            if (screen == null) {
                screen = ScreenProjection.approximateWorldToScreen(center);
            }
            if (screen == null) continue;
            boolean locked = target.getId() == lockTarget && lockTicks >= launcher.lockTicks();
            boolean nearest = target.getId() == lockTarget;
            Identifier frame = locked ? FRAME_LOCK : nearest ? FRAME_TARGET : FRAME;
            int x = Mth.floor(screen.x) - 12;
            int y = Mth.floor(screen.y) - 12;
            g.blit(RenderPipelines.GUI_TEXTURED, frame, x, y, 0.0F, 0.0F, 24, 24, 24, 24, 24, 24);
        }
    }

    private static void renderDroneHud(GuiGraphicsExtractor g, Minecraft minecraft, LocalPlayer player, DroneEntity drone) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int x = w / 2;
        int y = h / 2;
        int distance = Mth.floor(player.distanceTo(drone));

        // SW TV frame + center crosshair (stretch 256 texture to full GUI)
        g.blit(RenderPipelines.GUI_TEXTURED, TV_FRAME, 0, 0, 0.0F, 0.0F, w, h, 256, 256, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, DRONE_CROSSHAIR, x - 16, y - 16, 0.0F, 0.0F, 32, 32, 32, 32, 32, 32);

        // FOV scale bar
        int barX = x + 100;
        int barY = y - 64;
        g.blit(RenderPipelines.GUI_TEXTURED, DRONE_FOV, barX, barY, 0.0F, 0.0F, 64, 129, 64, 129, 64, 129);
        float zoomT = (droneZoom - 1.0F) / 5.0F;
        int moveY = barY + Mth.floor((1.0F - zoomT) * 110.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, DRONE_FOV_MOVE, barX, moveY, 0.0F, 0.0F, 64, 16, 64, 16, 64, 129);
        g.text(minecraft.font, String.format("%.0fx", droneZoom), barX + 44, moveY + 4, 0xFFFFFFFF, false);

        int color = 0xFFFFFFFF;
        if (droneRange > 0) {
            if (distance >= droneRange - 16) color = 0xFFFF0000;
            else if (distance >= droneRange - 48) color = 0xFFFFFF00;
        }
        if (droneRange > 0 && distance >= droneRange - 48) {
            g.text(minecraft.font, Component.translatable("message.jeg.drone.range_warning"), x - 18, y - 47, 0xFFFF0000, false);
        }
        g.text(minecraft.font, Component.translatable("des.jeg.drone.distance", distance + "m"), x + 10, y + 33, color, false);
        g.text(minecraft.font, Component.translatable("des.jeg.drone.health",
                Mth.ceil(drone.health()) + " / 5"), x - 77, y + 33, 0xFFFFFFFF, false);
        String payload = drone.payloadName();
        if ("C4".equals(payload)) {
            g.text(minecraft.font, Component.translatable("des.jeg.drone.kamikaze"), x + 12, y - 37, 0xFFFF0000, false);
        } else {
            g.text(minecraft.font, Component.translatable("des.jeg.drone.ammo", payload), x + 12, y - 37, 0xFFFFFFFF, false);
        }

        // Look range (block / entity) like SW
        Vec3 cam = drone.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(drone.getXRot(), drone.getYRot());
        BlockHitResult hit = drone.level().clip(new ClipContext(cam, cam.add(look.scale(512.0D)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, drone));
        double blockRange = cam.distanceTo(hit.getLocation());
        Entity looking = drone.level().getEntities(drone, new AABB(cam, cam.add(look.scale(64.0D))).inflate(1.0D), Entity::isPickable)
                .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(drone))).orElse(null);
        if (looking != null) {
            g.text(minecraft.font, Component.translatable("des.jeg.drone.range",
                    String.format("%.0fm %s", looking.distanceTo(drone), looking.getName().getString())), x + 12, y - 28, color, false);
        } else if (blockRange > 500.0D || hit.getType() == HitResult.Type.MISS) {
            g.text(minecraft.font, Component.translatable("des.jeg.drone.range", "---m"), x + 12, y - 28, color, false);
        } else {
            g.text(minecraft.font, Component.translatable("des.jeg.drone.range", String.format("%.0fm", blockRange)), x + 12, y - 28, color, false);
        }

        // Nearby entity frames
        for (Entity e : drone.level().getEntities(drone, drone.getBoundingBox().inflate(256.0D), ent -> ent instanceof LivingEntity || ent instanceof VehicleEntity)) {
            if (!e.isAlive() || e.distanceTo(drone) > 256.0D) continue;
            Vec3 center = e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D);
            if (!ScreenProjection.canSee(center)) continue;
            Vec3 screen = ScreenProjection.worldToScreen(center);
            if (screen == null) screen = ScreenProjection.approximateWorldToScreen(center);
            if (screen == null) continue;
            g.blit(RenderPipelines.GUI_TEXTURED, FRAME, Mth.floor(screen.x) - 12, Mth.floor(screen.y) - 12, 0.0F, 0.0F, 24, 24, 24, 24, 24, 24);
        }
    }

    /**
     * Stretch a full {@code sourceSize}x{@code sourceSize} texture into dest rect (w,h).
     * 26.x 11-arg blit: dest size + source region + texture size (same pattern as vehicle reticle).
     */
    private static void blit(GuiGraphicsExtractor g, Identifier texture, float x, float y, float w, float h, int sourceSize) {
        int ix = Mth.floor(x);
        int iy = Mth.floor(y);
        int iw = Math.max(1, Mth.floor(w));
        int ih = Math.max(1, Mth.floor(h));
        g.blit(RenderPipelines.GUI_TEXTURED, texture, ix, iy, 0.0F, 0.0F, iw, ih, sourceSize, sourceSize, sourceSize, sourceSize);
    }

    private static void blitCentered(GuiGraphicsExtractor g, Identifier texture, int cx, int cy, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, cx - w / 2, cy - h / 2, 0.0F, 0.0F, w, h, w, h, w, h);
    }

    /** FOV divisor when piloting drone (1 = no zoom). */
    public static float droneFovDivisor() {
        return droneId >= 0 ? Math.max(1.0F, droneZoom) : 1.0F;
    }

    public static boolean isDroneControlling() {
        return droneId >= 0;
    }

    /** @return true if scroll was consumed for drone zoom. */
    public static boolean handleMouseScroll(double scrollY) {
        if (droneId < 0 || scrollY == 0.0D) {
            return false;
        }
        droneZoom = Mth.clamp(droneZoom + (scrollY > 0.0D ? 1.0F : -1.0F), 1.0F, 6.0F);
        return true;
    }

    private static void playLocal(LocalPlayer player, Identifier id) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (sound != null) {
            player.playSound(sound, 0.7F, 1.0F);
        }
    }

    private static void resetLock() {
        lockTarget = Integer.MIN_VALUE;
        lockTicks = 0;
    }
}
