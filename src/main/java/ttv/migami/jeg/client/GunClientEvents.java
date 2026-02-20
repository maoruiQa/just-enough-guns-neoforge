package ttv.migami.jeg.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.opengl.GL11;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.item.GunItem;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GunClientEvents {
    private static final float AIM_FOV_MULTIPLIER = 0.8F;
    private static final int OVERHEAT_BAR_WIDTH = 82;
    private static final int OVERHEAT_BAR_HEIGHT = 4;
    private static final int OVERHEAT_BAR_Y_OFFSET = 58;
    private static int hudTicker;
    private static String lastHudText = "";

    private GunClientEvents() {}

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        event.setNewFovModifier(Math.max(0.1F, event.getNewFovModifier() * AIM_FOV_MULTIPLIER));
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.getHand() == null) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack held = player.getItemInHand(event.getHand());
        if (event.isUseItem() && held.getItem() instanceof GunItem) {
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GunRecoilHandler.tick();
        // Tick bullet trail renderer to age and remove old trails
        ttv.migami.jeg.client.render.BulletTrailRenderer.tick();

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            hudTicker = 0;
            lastHudText = "";
            return;
        }

        ItemStack heldMain = player.getMainHandItem();
        ItemStack heldOff = player.getOffhandItem();
        if (heldMain.getItem() instanceof GunItem || heldOff.getItem() instanceof GunItem) {
            player.attackAnim = 0.0F;
            player.oAttackAnim = 0.0F;
            player.swingTime = 0;
            player.swinging = false;
        }

        hudTicker++;
        if (hudTicker % 4 != 0) {
            return;
        }

        ItemStack held = heldMain;
        if (!(held.getItem() instanceof GunItem gun)) {
            lastHudText = "";
            return;
        }

        String hudText = buildAmmoHudText(player, held, gun);
        if (!hudText.isEmpty() && (!hudText.equals(lastHudText) || hudTicker % 20 == 0)) {
            player.displayClientMessage(Component.literal(hudText), true);
            lastHudText = hudText;
        }
    }

    private static String buildAmmoHudText(LocalPlayer player, ItemStack stack, GunItem gun) {
        GunStats stats = gun.getStats();
        int reserve = gun.countInventoryAmmo(player);
        boolean infinite = reserve == Integer.MAX_VALUE;
        String reserveText = infinite ? "∞" : Integer.toString(Math.max(0, reserve));

        if (stats.usesMagazine()) {
            int magazine = gun.getMagazineAmmo(stack);
            return "Ammo " + magazine + "/" + stats.magazineSize() + " | Reserve " + reserveText;
        }

        return "Ammo " + reserveText;
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            return;
        }
        renderOverheatBar(event.getGuiGraphics());
    }

    private static void renderOverheatBar(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gun) || !gun.usesOverheatMechanic()) {
            return;
        }

        int percent = gun.getOverheatPercent(held);
        if (percent <= 0) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = (screenWidth - OVERHEAT_BAR_WIDTH) / 2;
        int y = screenHeight - OVERHEAT_BAR_Y_OFFSET;

        guiGraphics.fill(x - 1, y - 1, x + OVERHEAT_BAR_WIDTH + 1, y + OVERHEAT_BAR_HEIGHT + 1, 0xAA000000);
        guiGraphics.fill(x, y, x + OVERHEAT_BAR_WIDTH, y + OVERHEAT_BAR_HEIGHT, 0x66000000);

        float ratio = Math.max(0.0F, Math.min(1.0F, percent / 100.0F));
        int fill = Math.max(1, Math.round(OVERHEAT_BAR_WIDTH * ratio));
        guiGraphics.fill(x, y, x + fill, y + OVERHEAT_BAR_HEIGHT, overheatColor(ratio));
    }

    private static int overheatColor(float ratio) {
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        int red;
        int green;
        if (clamped < 0.5F) {
            float t = clamped / 0.5F;
            red = Math.round(255.0F * t);
            green = 255;
        } else {
            float t = (clamped - 0.5F) / 0.5F;
            red = 255;
            green = Math.round(255.0F * (1.0F - t));
        }
        int blue = 48;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.hasSingleplayerServer()) {
            return;
        }

        flushRenderQueue();
        // Clear bullet trails when logging out
        ttv.migami.jeg.client.render.BulletTrailRenderer.clear();
    }

    private static void flushRenderQueue() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        Runnable finish = () -> {
            if (org.lwjgl.opengl.GL.getCapabilities() != null) {
                GL11.glFinish();
            }
        };

        if (RenderSystem.isOnRenderThread()) {
            finish.run();
            return;
        }

        minecraft.submit(finish).join();
    }

}
