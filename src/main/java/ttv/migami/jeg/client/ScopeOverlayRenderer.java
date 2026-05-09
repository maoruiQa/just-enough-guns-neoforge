package ttv.migami.jeg.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.GunItem;

public final class ScopeOverlayRenderer {
    private static final Identifier SCOPE_OVERLAY = Reference.id("textures/scope_long_overlay.png");
    private static final Identifier BOLT_ACTION_RIFLE = Reference.id("bolt_action_rifle");
    private static final int TEXTURE_SIZE = 256;

    private ScopeOverlayRenderer() {
    }

    public static void render(GuiGraphicsExtractor guiGraphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)
                || !BOLT_ACTION_RIFLE.equals(gun.getStats().id())
                || !GunScopeSupport.isBoltActionRifleScopeEnabled()) {
            return;
        }

        float ads = AimingHandler.get().getNormalisedAdsProgress(partialTick);
        if (ads <= 0.5F) {
            return;
        }

        int alpha = Mth.clamp(Math.round((ads - 0.5F) * 2.0F * 255.0F), 0, 255);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCOPE_OVERLAY, 0, 0, 0.0F, 0.0F,
                guiGraphics.guiWidth(), guiGraphics.guiHeight(), TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE,
                (alpha << 24) | 0x00FFFFFF);
    }
}
