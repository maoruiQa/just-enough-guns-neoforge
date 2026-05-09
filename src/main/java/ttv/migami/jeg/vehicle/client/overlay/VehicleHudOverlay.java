package ttv.migami.jeg.vehicle.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehicleHudOverlay {
    private VehicleHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName() == null || !"crosshair".equals(event.getName().getPath())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        render(event.getGuiGraphics(), minecraft, vehicle);
    }

    private static void render(GuiGraphics guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        int width = guiGraphics.guiWidth();
        int y = guiGraphics.guiHeight() - 58;
        float healthRatio = vehicle.maxVehicleHealth() <= 0.0F ? 0.0F : Mth.clamp(vehicle.vehicleHealth() / vehicle.maxVehicleHealth(), 0.0F, 1.0F);
        int barWidth = 112;
        int filled = Math.round(barWidth * healthRatio);
        int x = width / 2 - barWidth / 2;

        Component title = Component.translatable("hud.jeg.vehicle.test_wheel_vehicle");
        guiGraphics.drawString(minecraft.font, title, (width - minecraft.font.width(title)) / 2, y - 12, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + barWidth, y + 7, 0xAA101010);
        guiGraphics.fill(x + 1, y + 1, x + Math.max(1, filled - 1), y + 6, 0xFF42B35A);

        Component health = Component.translatable("hud.jeg.vehicle.health", Math.round(vehicle.vehicleHealth()), Math.round(vehicle.maxVehicleHealth()));
        guiGraphics.drawString(minecraft.font, health, (width - minecraft.font.width(health)) / 2, y + 11, 0xFFE6E6E6);
        if (vehicle.maxVehicleEnergy() > 0) {
            Component energy = Component.translatable("hud.jeg.vehicle.energy", vehicle.vehicleEnergy(), vehicle.maxVehicleEnergy());
            guiGraphics.drawString(minecraft.font, energy, (width - minecraft.font.width(energy)) / 2, y + 22, 0xFF8FC7FF);
        }
        Component ammo = Component.translatable("hud.jeg.vehicle.rifle_ammo", vehicle.vehicleRifleAmmo());
        guiGraphics.drawString(minecraft.font, ammo, (width - minecraft.font.width(ammo)) / 2, y + 33, 0xFFFFDD88);
        if (vehicle.isEngineDamaged() || vehicle.isLeftWheelDamaged() || vehicle.isRightWheelDamaged()) {
            Component damage = Component.translatable(
                    "hud.jeg.vehicle.parts",
                    vehicle.isEngineDamaged() ? "!" : "-",
                    vehicle.isLeftWheelDamaged() ? "!" : "-",
                    vehicle.isRightWheelDamaged() ? "!" : "-"
            );
            guiGraphics.drawString(minecraft.font, damage, (width - minecraft.font.width(damage)) / 2, y + 44, 0xFFFF7777);
        }
    }
}
