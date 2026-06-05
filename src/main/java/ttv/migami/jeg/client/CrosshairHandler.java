package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.item.GunItem;

public final class CrosshairHandler {
    private static final Identifier DEFAULT = Reference.id("textures/crosshair/better_default.png");
    private static final Identifier TECH = Reference.id("textures/crosshair/tech.png");
    private static final Identifier DOT = Reference.id("textures/crosshair/dot.png");
    private static final Identifier HIT = Reference.id("textures/crosshair/special_hit_marker.png");
    private static final Identifier CRIT_HIT = Reference.id("textures/crosshair/special_crit_hit_marker.png");
    private static final int CROSSHAIR_TEXTURE_SIZE = 16;
    private static final float DYNAMIC_LINE_LENGTH = 4.0F;
    private static final float DYNAMIC_LINE_GAP = 3.5F;
    private static final float DYNAMIC_SPREAD_PIXELS_PER_DEGREE = 0.85F;
    private static final float DYNAMIC_MAX_SPREAD_PIXELS = 28.0F;
    private static final int HIT_MARKER_MAX_TIME = 6;

    private static float techScale;
    private static float prevTechScale;
    private static boolean playingHitMarker;
    private static boolean criticalHitMarker;
    private static int hitMarkerTime;
    private static int prevHitMarkerTime;

    private CrosshairHandler() {}

    public static void tick() {
        prevTechScale = techScale;
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
        playingHitMarker = false;
        criticalHitMarker = false;
        hitMarkerTime = 0;
        prevHitMarkerTime = 0;
    }

    public static void render(GuiGraphicsExtractor guiGraphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        renderHitMarker(guiGraphics, width, height, partialTick);
        if (!ClientUiConfig.showCrosshair()) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (GunItem.isRocketLauncher(stack)) {
            return;
        }
        if (!(stack.getItem() instanceof GunItem gun) || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (Reference.id("bolt_action_rifle").equals(gun.getStats().id())) {
            return;
        }
        if (AimingHandler.get().getNormalisedAdsProgress(partialTick) > 0.5F) {
            return;
        }

        Identifier id = configuredCrosshairId();
        if ("minecraft".equals(id.getNamespace()) && "default".equals(id.getPath())) {
            drawTexture(guiGraphics, DEFAULT, width / 2.0F, height / 2.0F, 15.0F, 1.0F);
        } else if (id.equals(Reference.id("tech"))) {
            renderTech(guiGraphics, width, height, partialTick);
        } else if (id.equals(Reference.id("dynamic"))) {
            renderDynamic(guiGraphics, player, gun, width, height, partialTick);
        } else {
            drawTexture(guiGraphics, crosshairTexture(id), width / 2.0F, height / 2.0F, 15.0F, 1.0F);
        }
    }

    private static void renderHitMarker(GuiGraphicsExtractor guiGraphics, int width, int height, float partialTick) {
        if (!playingHitMarker || !Config.showHitmarker() || !ClientUiConfig.showHitFeedback()) {
            return;
        }
        float progress = Mth.clamp(Mth.lerp(partialTick, prevHitMarkerTime, hitMarkerTime) / HIT_MARKER_MAX_TIME, 0.0F, 1.0F);
        float alpha = 1.0F - progress * 0.35F;
        float size = 15.0F + progress * 7.0F;
        drawTexture(guiGraphics, criticalHitMarker ? CRIT_HIT : HIT, width / 2.0F, height / 2.0F, size, alpha);
    }

    private static void renderTech(GuiGraphicsExtractor guiGraphics, int width, int height, float partialTick) {
        float alpha = 1.0F - AimingHandler.get().getNormalisedAdsProgress(partialTick);
        drawTexture(guiGraphics, DOT, width / 2.0F, height / 2.0F, 8.0F, alpha);
        float scale = 1.0F + Mth.lerp(partialTick, prevTechScale, techScale);
        drawTexture(guiGraphics, TECH, width / 2.0F, height / 2.0F, 16.0F * scale, alpha);
    }

    private static void renderDynamic(GuiGraphicsExtractor guiGraphics, LocalPlayer player, GunItem gun, int width, int height, float partialTick) {
        float aiming = AimingHandler.get().getNormalisedAdsProgress(partialTick);
        float spread = GunItem.getClientSpreadDegrees(player, player.getMainHandItem(), gun.getStats(), aiming > 0.5F);
        float spreadPixels = Mth.clamp(spread * DYNAMIC_SPREAD_PIXELS_PER_DEGREE, 0.0F, DYNAMIC_MAX_SPREAD_PIXELS);
        int centerX = Math.round(width / 2.0F);
        int centerY = Math.round(height / 2.0F);
        int offset = Math.round(DYNAMIC_LINE_GAP + spreadPixels);
        int halfLength = Math.round(DYNAMIC_LINE_LENGTH / 2.0F);
        int color = 0xFFFFFFFF;

        guiGraphics.fill(centerX - offset - halfLength, centerY, centerX - offset + halfLength, centerY + 1, color);
        guiGraphics.fill(centerX + offset - halfLength, centerY, centerX + offset + halfLength, centerY + 1, color);
        guiGraphics.fill(centerX, centerY - offset - halfLength, centerX + 1, centerY - offset + halfLength, color);
        guiGraphics.fill(centerX, centerY + offset - halfLength, centerX + 1, centerY + offset + halfLength, color);
        if (shouldRenderDynamicDot(spread)) {
            drawTexture(guiGraphics, DOT, centerX, centerY, 5.0F, 1.0F);
        }
    }

    private static boolean shouldRenderDynamicDot(float spread) {
        return switch (Config.dynamicCrosshairDotMode()) {
            case "never" -> false;
            case "always" -> true;
            case "threshold" -> spread <= Config.dynamicCrosshairDotThreshold();
            default -> spread <= 0.01F;
        };
    }

    private static Identifier configuredCrosshairId() {
        Identifier id = Identifier.tryParse(Config.crosshair());
        return id != null ? id : Reference.id("dynamic");
    }

    private static Identifier crosshairTexture(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/crosshair/" + id.getPath() + ".png");
    }

    private static void drawTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, float centerX, float centerY, float size, float alpha) {
        int intSize = Math.round(size);
        int x = Math.round(centerX - intSize / 2.0F);
        int y = Math.round(centerY - intSize / 2.0F);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, intSize, intSize, CROSSHAIR_TEXTURE_SIZE, CROSSHAIR_TEXTURE_SIZE, CROSSHAIR_TEXTURE_SIZE, CROSSHAIR_TEXTURE_SIZE);
    }
}
