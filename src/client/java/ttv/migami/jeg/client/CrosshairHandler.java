package ttv.migami.jeg.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.item.GunItem;

public final class CrosshairHandler {
    private static final ResourceLocation DEFAULT = Reference.id("textures/crosshair/better_default.png");
    private static final ResourceLocation TECH = Reference.id("textures/crosshair/tech.png");
    private static final ResourceLocation DOT = Reference.id("textures/crosshair/dot.png");
    private static final ResourceLocation DYNAMIC_H = Reference.id("textures/crosshair/dynamic_horizontal.png");
    private static final ResourceLocation DYNAMIC_V = Reference.id("textures/crosshair/dynamic_vertical.png");
    private static final ResourceLocation HIT = Reference.id("textures/crosshair/special_hit_marker.png");
    private static final ResourceLocation CRIT_HIT = Reference.id("textures/crosshair/special_crit_hit_marker.png");
    private static final int CROSSHAIR_TEXTURE_SIZE = 16;
    private static final int DYNAMIC_TEXTURE_SIZE = 9;
    private static final float DYNAMIC_LINE_LENGTH = 4.0F;
    private static final float DYNAMIC_LINE_GAP = 3.5F;
    private static final float DYNAMIC_SPREAD_PIXELS_PER_DEGREE = 0.85F;
    private static final float DYNAMIC_MAX_SPREAD_PIXELS = 28.0F;
    private static final int HIT_MARKER_MAX_TIME = 6;

    private static float techScale;
    private static float prevTechScale;
    private static float techRotation;
    private static float prevTechRotation;
    private static boolean playingHitMarker;
    private static boolean criticalHitMarker;
    private static int hitMarkerTime;
    private static int prevHitMarkerTime;

    private CrosshairHandler() {}

    public static void tick() {
        prevTechRotation = techRotation;
        prevTechScale = techScale;
        techRotation += 4.0F;
        techScale *= 0.75F;

        prevHitMarkerTime = hitMarkerTime;
        if (playingHitMarker) {
            hitMarkerTime++;
            if (hitMarkerTime > HIT_MARKER_MAX_TIME) {
                playingHitMarker = false;
                hitMarkerTime = 0;
                prevHitMarkerTime = 0;
            }
        }
    }

    public static void onGunFired() {
        techScale = 1.5F;
    }

    public static void playHitMarker(boolean critical) {
        if (!Config.showHitmarker() || !ClientUiConfig.showHitFeedback()) {
            return;
        }
        playingHitMarker = true;
        criticalHitMarker = critical;
        hitMarkerTime = 1;
        prevHitMarkerTime = 0;
    }

    public static void reset() {
        techScale = 0.0F;
        prevTechScale = 0.0F;
        techRotation = 0.0F;
        prevTechRotation = 0.0F;
        playingHitMarker = false;
        criticalHitMarker = false;
        hitMarkerTime = 0;
        prevHitMarkerTime = 0;
    }

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        renderHitMarker(guiGraphics, width, height, partialTick);
        if (!ClientUiConfig.showCrosshair()) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (GunItem.isRocketLauncher(stack)) {
            return;
        }
        if (!(stack.getItem() instanceof GunItem gun) || minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        if (Reference.id("bolt_action_rifle").equals(gun.getStats().id())) {
            return;
        }
        if (AimingHandler.get().getNormalisedAdsProgress(partialTick) > 0.5F) {
            return;
        }

        ResourceLocation id = configuredCrosshairId();
        if (id.getNamespace().equals("minecraft") && id.getPath().equals("default")) {
            drawTexture(guiGraphics, DEFAULT, width / 2.0F, height / 2.0F, 15.0F, 1.0F);
        } else if (id.equals(Reference.id("tech"))) {
            renderTech(guiGraphics, width, height, partialTick);
        } else if (id.equals(Reference.id("dynamic"))) {
            renderDynamic(guiGraphics, player, gun, width, height, partialTick);
        } else {
            drawTexture(guiGraphics, crosshairTexture(id), width / 2.0F, height / 2.0F, 15.0F, 1.0F);
        }
    }

    private static void renderHitMarker(GuiGraphics guiGraphics, int width, int height, float partialTick) {
        if (!playingHitMarker || !Config.showHitmarker() || !ClientUiConfig.showHitFeedback()) {
            return;
        }
        float progress = Mth.clamp(Mth.lerp(partialTick, prevHitMarkerTime, hitMarkerTime) / HIT_MARKER_MAX_TIME, 0.0F, 1.0F);
        float alpha = 1.0F - progress * 0.35F;
        float size = 15.0F + progress * 7.0F;
        drawTexture(guiGraphics, criticalHitMarker ? CRIT_HIT : HIT, width / 2.0F, height / 2.0F, size, alpha);
    }

    private static void renderTech(GuiGraphics guiGraphics, int width, int height, float partialTick) {
        float alpha = 1.0F - AimingHandler.get().getNormalisedAdsProgress(partialTick);
        drawTexture(guiGraphics, DOT, width / 2.0F, height / 2.0F, 8.0F, alpha);

        float scale = 1.0F + Mth.lerp(partialTick, prevTechScale, techScale);
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(width / 2.0F, height / 2.0F, 0.0F);
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, prevTechRotation, techRotation)));
        blitCentered(guiGraphics, TECH, 8.0F, alpha);
        pose.popPose();
    }

    private static void renderDynamic(GuiGraphics guiGraphics, LocalPlayer player, GunItem gun, int width, int height, float partialTick) {
        float aiming = AimingHandler.get().getNormalisedAdsProgress(partialTick);
        float spread = GunItem.getClientSpreadDegrees(player, gun.getStats(), aiming > 0.5F);
        float spreadPixels = Mth.clamp(spread * DYNAMIC_SPREAD_PIXELS_PER_DEGREE, 0.0F, DYNAMIC_MAX_SPREAD_PIXELS);
        float centerX = Math.round(width / 2.0F) - 0.5F;
        float centerY = Math.round(height / 2.0F) - 0.5F;
        float offset = DYNAMIC_LINE_GAP + spreadPixels;
        float halfLength = DYNAMIC_LINE_LENGTH / 2.0F;

        RenderSystem.enableBlend();
        drawDynamicPart(guiGraphics, DYNAMIC_H, centerX - offset - halfLength, centerY, DYNAMIC_LINE_LENGTH, 1.0F, 0, 0, 4, 1);
        drawDynamicPart(guiGraphics, DYNAMIC_H, centerX + offset + halfLength, centerY, DYNAMIC_LINE_LENGTH, 1.0F, 5, 8, 4, 1);
        drawDynamicPart(guiGraphics, DYNAMIC_V, centerX, centerY - offset - halfLength, 1.0F, DYNAMIC_LINE_LENGTH, 0, 0, 1, 4);
        drawDynamicPart(guiGraphics, DYNAMIC_V, centerX, centerY + offset + halfLength, 1.0F, DYNAMIC_LINE_LENGTH, 8, 5, 1, 4);
        RenderSystem.defaultBlendFunc();
    }

    private static ResourceLocation configuredCrosshairId() {
        ResourceLocation id = ResourceLocation.tryParse(Config.crosshair());
        return id != null ? id : Reference.id("dynamic");
    }

    private static ResourceLocation crosshairTexture(ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/crosshair/" + id.getPath() + ".png");
    }

    private static void drawDynamicPart(GuiGraphics guiGraphics, ResourceLocation texture, float x, float y, float width, float height,
                                        int sourceX, int sourceY, int sourceWidth, int sourceHeight) {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        guiGraphics.blit(texture, Math.round(-width / 2.0F), Math.round(-height / 2.0F), sourceX, sourceY, sourceWidth, sourceHeight, DYNAMIC_TEXTURE_SIZE, DYNAMIC_TEXTURE_SIZE);
        pose.popPose();
    }

    private static void drawTexture(GuiGraphics guiGraphics, ResourceLocation texture, float centerX, float centerY, float size, float alpha) {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0.0F);
        blitCentered(guiGraphics, texture, size, alpha);
        pose.popPose();
    }

    private static void blitCentered(GuiGraphics guiGraphics, ResourceLocation texture, float size, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        int intSize = Math.round(size);
        guiGraphics.blit(texture, -intSize / 2, -intSize / 2, intSize, intSize, 0.0F, 0.0F,
                CROSSHAIR_TEXTURE_SIZE, CROSSHAIR_TEXTURE_SIZE, CROSSHAIR_TEXTURE_SIZE, CROSSHAIR_TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }
}
