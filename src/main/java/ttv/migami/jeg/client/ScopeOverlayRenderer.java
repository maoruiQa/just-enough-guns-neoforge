package ttv.migami.jeg.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.GunItem;

public final class ScopeOverlayRenderer {
    private static final ResourceLocation SCOPE_OVERLAY = Reference.id("textures/scope_long_overlay.png");
    private static final ResourceLocation BOLT_ACTION_RIFLE = Reference.id("bolt_action_rifle");
    private static final int TEXTURE_SIZE = 256;

    private ScopeOverlayRenderer() {
    }

    public static void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)
                || !BOLT_ACTION_RIFLE.equals(gun.getStats().id())
                || !GunScopeSupport.isBoltActionRifleScopeEnabled(stack)) {
            return;
        }

        float ads = AimingHandler.get().getNormalisedAdsProgress(partialTick);
        if (ads <= 0.5F) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Mth.clamp((ads - 0.5F) * 2.0F, 0.0F, 1.0F));
        guiGraphics.blit(SCOPE_OVERLAY, 0, 0, width, height, 0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }
}
