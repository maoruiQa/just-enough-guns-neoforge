package ttv.migami.jeg.client.render.gun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.GuidedLauncherItem;

/**
 * SuperbWarfare-style first-person walk/sprint root motion for javelin and igla_9k38.
 * <p>
 * SW advances {@code handleWeaponMove/Sway} every render frame (camera event), not once per
 * client tick. Updating only on 20 TPS ticks makes the bob look slow and stuttery; this class
 * advances from the first-person render path with wall-clock delta (game-tick units).
 */
public final class SwGuidedLauncherMotion {
    public record RootTransform(float posX, float posY, float posZ, float rotX, float rotY, float rotZ) {
        public static final RootTransform IDENTITY = new RootTransform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static final float JAVELIN_CUSTOM_X = 4.0F;
    private static final float JAVELIN_CUSTOM_Y = 0.0F;
    private static final float JAVELIN_CUSTOM_Z = 2.0F;
    private static final float IGLA_CUSTOM_X = 1.0F;
    private static final float IGLA_CUSTOM_Y = 0.0F;
    private static final float IGLA_CUSTOM_Z = 2.0F;
    private static final float JAVELIN_WEIGHT = 10.0F;
    private static final float IGLA_WEIGHT = 8.0F;

    /** Floor so hitch/pause does not freeze motion for a frame. */
    private static final float MIN_DELTA_TICKS = 0.05F;
    /** Cap ~1 game tick so freezes do not explode the state. */
    private static final float MAX_DELTA_TICKS = 1.0F;
    /** Ignore re-entry within the same render frame (multi-pass). */
    private static final long MIN_UPDATE_NANOS = 1_500_000L; // 1.5ms

    private static double swayTime;
    private static double swayX;
    private static double swayY;
    private static double moveTime;
    private static double sprintTime;
    private static double movePosX;
    private static double movePosY;
    private static double moveRotZ;
    private static double sprintBasicRotX;
    private static double sprintBasicRotY;
    private static double sprintBasicRotZ;
    private static double sprintPosX;
    private static double sprintPosY;
    private static double sprintBasicPosX;
    private static double sprintBasicPosY;
    private static double sprintBasicPosZ;
    private static double movePosHorizon;
    private static double velocityY;
    private static double moveFadeTime;
    private static double sprintFadeTime;
    private static float noSprintTicks;
    private static final double[] turnRot = {0.0D, 0.0D, 0.0D};
    private static float lastYRot;
    private static long lastUpdateNanos;
    private static boolean active;

    private SwGuidedLauncherMotion() {
    }

    /**
     * Lightweight tick: aiming gate + drop state when not holding a guided launcher.
     * Heavy motion advances from {@link #computeRoot} every render frame.
     */
    public static void tick(LocalPlayer player) {
        if (player == null) {
            reset();
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GuidedLauncherItem) || !isGuidedLauncherPath(pathOf(stack))) {
            reset();
            return;
        }
        active = true;
        if (AimingHandler.get().isAiming()) {
            noSprintTicks = 5.0F;
        } else if (noSprintTicks > 0.0F) {
            noSprintTicks = Math.max(0.0F, noSprintTicks - 1.0F);
        }
    }

    public static void reset() {
        swayTime = 0.0D;
        swayX = 0.0D;
        swayY = 0.0D;
        moveTime = 0.0D;
        sprintTime = 0.0D;
        movePosX = 0.0D;
        movePosY = 0.0D;
        moveRotZ = 0.0D;
        sprintBasicRotX = 0.0D;
        sprintBasicRotY = 0.0D;
        sprintBasicRotZ = 0.0D;
        sprintPosX = 0.0D;
        sprintPosY = 0.0D;
        sprintBasicPosX = 0.0D;
        sprintBasicPosY = 0.0D;
        sprintBasicPosZ = 0.0D;
        movePosHorizon = 0.0D;
        velocityY = 0.0D;
        moveFadeTime = 0.0D;
        sprintFadeTime = 0.0D;
        noSprintTicks = 0.0F;
        turnRot[0] = 0.0D;
        turnRot[1] = 0.0D;
        turnRot[2] = 0.0D;
        lastYRot = 0.0F;
        lastUpdateNanos = 0L;
        active = false;
    }

    public static RootTransform computeRoot(String path, ItemStack stack, float partialTick, float zoomTime) {
        if (!isGuidedLauncherPath(path)) {
            return RootTransform.IDENTITY;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc != null ? mc.player : null;
        if (player != null && stack.getItem() instanceof GuidedLauncherItem) {
            advanceFrame(player, stack, zoomTime);
        }
        float drawTime = GunItem.getClientDrawTime(stack, partialTick);
        float customX = "javelin".equals(path) ? JAVELIN_CUSTOM_X : IGLA_CUSTOM_X;
        float customY = "javelin".equals(path) ? JAVELIN_CUSTOM_Y : IGLA_CUSTOM_Y;
        float customZ = "javelin".equals(path) ? JAVELIN_CUSTOM_Z : IGLA_CUSTOM_Z;
        return gunRootMove(customX, customY, customZ, drawTime, zoomTime);
    }

    public static boolean isGuidedLauncherPath(String path) {
        return "javelin".equals(path) || "igla_9k38".equals(path);
    }

    private static String pathOf(ItemStack stack) {
        if (stack.getItem() instanceof GunItem gun) {
            return gun.getStats().id().getPath();
        }
        return "";
    }

    /**
     * Per-render-frame advance (SW camera-event cadence). Uses wall-clock delta converted to
     * game-tick units so bob stays smooth at any FPS and is not doubled by multi-pass renders.
     */
    private static void advanceFrame(LocalPlayer player, ItemStack stack, float zoomTime) {
        long now = System.nanoTime();
        if (lastUpdateNanos != 0L && now - lastUpdateNanos < MIN_UPDATE_NANOS) {
            return;
        }
        float deltaTicks;
        if (lastUpdateNanos == 0L) {
            deltaTicks = MIN_DELTA_TICKS;
        } else {
            double seconds = (now - lastUpdateNanos) / 1_000_000_000.0D;
            // 20 game ticks per second
            deltaTicks = (float) (seconds * 20.0D);
        }
        lastUpdateNanos = now;
        deltaTicks = Mth.clamp(deltaTicks, MIN_DELTA_TICKS, MAX_DELTA_TICKS);
        active = true;

        updateTurnRot(player, zoomTime);
        handleWeaponSway(player, zoomTime, deltaTicks);
        handleWeaponMove(player, stack, zoomTime, deltaTicks);
    }

    private static void updateTurnRot(LocalPlayer player, float zoomTime) {
        float yRot = player.getYRot();
        float fromY = lastYRot;
        float toY = yRot;
        if (fromY > 135.0F && toY < -135.0F) {
            toY += 360.0F;
        }
        if (fromY < -135.0F && toY > 135.0F) {
            fromY += 360.0F;
        }
        float yRotOffset = toY - fromY;
        lastYRot = yRot;
        // Scale look offset by frame density so high FPS does not amplify turn kick
        turnRot[0] = Mth.clamp(0.05D * yRotOffset, -5.0D, 5.0D) * (1.0D - 0.75D * zoomTime);
        turnRot[1] = Mth.clamp(0.05D * yRotOffset, -10.0D, 10.0D) * (1.0D - 0.75D * zoomTime);
        turnRot[2] = Mth.clamp(0.1D * yRotOffset, -10.0D, 10.0D) * (1.0D - zoomTime);
    }

    private static void handleWeaponSway(LocalPlayer player, float zoomTime, float deltaTicks) {
        float times = 2.0F * Math.min(deltaTicks, 0.8F);
        double pose = player.isShiftKeyDown() ? 0.85D : 1.0D;
        swayTime += 0.05D * times;
        swayX = pose * -0.008D * Math.sin(swayTime) * (1.0D - 0.95D * zoomTime);
        swayY = pose * 0.125D * Math.sin(swayTime - 1.585D) * (1.0D - 0.95D * zoomTime) - 3.0D * moveRotZ;
    }

    private static void handleWeaponMove(LocalPlayer player, ItemStack stack, float zoomTime, float deltaTicks) {
        Minecraft mc = Minecraft.getInstance();
        float times = 3.7F * Math.min(deltaTicks, 0.8F);
        // Prefer live velocity; fall back so bob still runs when velocity is briefly zero mid-strafe
        double moveSpeed = player.getDeltaMovement().horizontalDistance();
        if (isMoving(mc, player) && moveSpeed < 0.08D) {
            moveSpeed = player.isSprinting() ? 0.28D : 0.18D;
        }
        double animSpeed = player.onGround() ? (player.isSprinting() ? 1.8D : 2.0D) : 0.005D;

        String path = pathOf(stack);
        float customWeight = Mth.clamp("javelin".equals(path) ? JAVELIN_WEIGHT : IGLA_WEIGHT, 1.0F, 50.0F);
        boolean reloading = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0;
        // Do not treat attack-hold as fire for guided launchers: javelin lock uses hold-fire and
        // would freeze sprint bob. Only block when actually reloading / ADS gate applies.
        boolean fireBusy = false;

        if (!player.isSprinting() && mc.options.keyUp.isDown() && !fireBusy) {
            moveRotZ = Mth.lerp(0.2F * times, (float) moveRotZ, 0.14F) * (1.0F - zoomTime);
        } else {
            moveRotZ = Mth.lerp(0.2F * times, (float) moveRotZ, 0.0F) * (1.0F - zoomTime);
        }

        if (player.isSprinting()
                && !reloading
                && !fireBusy
                && noSprintTicks == 0.0F
                && zoomTime < 0.1F) {
            sprintBasicRotX = Mth.clamp(Mth.lerp(0.3F * times / (customWeight + 4.0F), (float) sprintBasicRotX, 1.0F), 0.0F, 1.0F);
            sprintBasicRotY = Mth.clamp(Mth.lerp(0.18F * times / (customWeight + 4.0F), (float) sprintBasicRotY, 1.0F), 0.0F, 1.0F);
            sprintBasicRotZ = Mth.clamp(Mth.lerp(0.3F * times / (customWeight + 4.0F), (float) sprintBasicRotZ, 1.0F), 0.0F, 1.0F);
            sprintBasicPosX = Mth.clamp(Mth.lerp(0.8F * times / (customWeight + 4.0F), (float) sprintBasicPosX, 1.0F), 0.0F, 1.0F);
            sprintBasicPosY = Mth.clamp(Mth.lerp(0.25F * times / (customWeight + 4.0F), (float) sprintBasicPosY, 1.0F), 0.0F, 1.0F);
            sprintBasicPosZ = Mth.clamp(Mth.lerp(0.8F * times / (customWeight + 4.0F), (float) sprintBasicPosZ, 1.0F), 0.0F, 1.0F);
        } else {
            sprintBasicRotX = Mth.clamp(Mth.lerp(1.4F * times / customWeight, (float) sprintBasicRotX, 0.0F), 0.0F, 1.0F);
            sprintBasicRotY = Mth.clamp(Mth.lerp(0.96F * times / customWeight, (float) sprintBasicRotY, 0.0F), 0.0F, 1.0F);
            sprintBasicRotZ = Mth.clamp(Mth.lerp(1.4F * times / customWeight, (float) sprintBasicRotZ, 0.0F), 0.0F, 1.0F);
            sprintBasicPosX = Mth.clamp(Mth.lerp(0.8F * times / customWeight, (float) sprintBasicPosX, 0.0F), 0.0F, 1.0F);
            sprintBasicPosY = Mth.clamp(Mth.lerp(0.8F * times / customWeight, (float) sprintBasicPosY, 0.0F), 0.0F, 1.0F);
            sprintBasicPosZ = Mth.clamp(Mth.lerp(0.8F * times / customWeight, (float) sprintBasicPosZ, 0.0F), 0.0F, 1.0F);
        }

        if (isMoving(mc, player)) {
            double fireScale = fireBusy ? 0.4D : 1.0D;
            // Full sprint bob immediately: do not gate phase by sprintBasicPosX (that made early
            // sprint frames crawl). Pose still eases in via sprintBasic* lerps.
            moveTime += 0.15D * animSpeed * times * moveSpeed * fireScale;
            sprintTime += 0.15D * animSpeed * times * moveSpeed * fireScale;
            moveFadeTime = Mth.lerp(0.13D * times, moveFadeTime, 1.0D);
        } else {
            moveFadeTime = Mth.lerp(0.1D * times, moveFadeTime, 0.0D);
        }

        if (player.isSprinting() && !reloading && !fireBusy && noSprintTicks == 0.0F) {
            if (player.onGround()) {
                sprintFadeTime = Mth.lerp(0.08D * times, sprintFadeTime, 1.0D);
            } else {
                sprintFadeTime = Mth.lerp(0.15D * times, sprintFadeTime, 0.0D);
            }
            sprintPosX = 2.0D * Math.sin(Math.PI * sprintTime) * (1.0D - 0.95D * zoomTime) * sprintFadeTime;
            sprintPosY = 1.0D * Math.sin(2.0D * Math.PI * sprintTime) * (1.0D - 0.95D * zoomTime) * sprintFadeTime;
        } else {
            sprintPosX = Mth.lerp(0.1D * times, sprintPosX, 0.0D);
            sprintPosY = Mth.lerp(0.1D * times, sprintPosY, 0.0D);
            sprintFadeTime = Mth.lerp(0.1D * times, sprintFadeTime, 0.0D);
        }

        movePosX = 0.2D * Math.sin(Math.PI * moveTime) * (1.0D - 0.95D * zoomTime) * moveFadeTime;
        movePosY = -0.135D * Math.sin(2.0D * Math.PI * (moveTime - 0.25D)) * (1.0D - 0.95D * zoomTime) * moveFadeTime;

        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        double pos = 0.0D;
        if (left) {
            pos = -0.04D;
        }
        if (right) {
            pos = 0.04D;
        }
        if (left && right) {
            pos = 0.0D;
        }
        movePosHorizon = Mth.lerp(0.1F * times, (float) movePosHorizon, (float) (pos * (1.0D - zoomTime)));

        double velocity = player.getDeltaMovement().y() + 0.078D;
        velocityY = Mth.clamp(Mth.lerp(0.23F * times, (float) velocityY, (float) velocity) * (1.0F - 0.8F * zoomTime), -0.8F, 0.8F);
    }

    private static RootTransform gunRootMove(
            float customX,
            float customY,
            float customZ,
            float drawTime,
            float zoomTime
    ) {
        float walkPosX = (float) movePosX;
        float walkPosY = (float) (swayY + movePosY);
        float walkPosZ = 0.0F;
        float walkRotX = (float) swayX;
        float walkRotY = (float) (0.2F * movePosX);
        float walkRotZ = (float) (0.2F * movePosX);

        // useCustomAnim = false => sprint pose enabled
        float basicSprintPosX = (float) (sprintBasicPosX * (1.5D + customX));
        float basicSprintPosY = (float) (sprintBasicPosY * (-2.35D + customY - 8.0D * parabola(sprintBasicPosY)));
        float basicSprintPosZ = (float) (sprintBasicPosZ * (-0.55D + customZ));
        float basicSprintRotX = (float) (sprintBasicRotX * 39.0D * Mth.DEG_TO_RAD);
        float basicSprintRotY = (float) (sprintBasicRotY * 35.6D * Mth.DEG_TO_RAD);
        float basicSprintRotZ = (float) (sprintBasicRotZ * 34.7D * Mth.DEG_TO_RAD);

        float zoomFade = 1.0F - zoomTime;
        float gunPosX = (float) (walkPosX + basicSprintPosX + sprintPosX + 20.0D * drawTime + 9.3D * movePosHorizon) * zoomFade;
        float gunPosY = (float) (walkPosY + basicSprintPosY + sprintPosY - 40.0D * drawTime - 2.0D * velocityY) * zoomFade;
        float gunPosZ = (walkPosZ + basicSprintPosZ) * zoomFade;
        float gunRotX = (float) ((walkRotX + basicSprintRotX - Mth.DEG_TO_RAD * 60.0D * drawTime - 0.15D * velocityY) * zoomFade
                + Mth.DEG_TO_RAD * turnRot[0]);
        float gunRotY = (float) ((walkRotY + basicSprintRotY + 0.2D * sprintBasicPosX + Mth.DEG_TO_RAD * 300.0D * drawTime) * zoomFade
                + Mth.DEG_TO_RAD * turnRot[1]);
        float gunRotZ = (float) ((walkRotZ + basicSprintRotZ + moveRotZ + Mth.DEG_TO_RAD * 90.0D * drawTime + 2.7D * movePosHorizon) * zoomFade
                + Mth.DEG_TO_RAD * turnRot[2]);

        return new RootTransform(gunPosX, gunPosY, gunPosZ, gunRotX, gunRotY, gunRotZ);
    }

    private static boolean isMoving(Minecraft mc, LocalPlayer player) {
        return mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown()
                || mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || player.isSprinting();
    }

    /** SW AnimationCurves.PARABOLA: peaks at 1 when x=0.5. */
    private static double parabola(double x) {
        x = Mth.clamp(x, 0.0D, 1.0D);
        return -Math.pow(2.0D * x - 1.0D, 2.0D) + 1.0D;
    }
}
