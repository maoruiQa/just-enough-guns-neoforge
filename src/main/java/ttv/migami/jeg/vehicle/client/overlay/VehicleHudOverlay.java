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
        event.setCanceled(true);
    }

    private static void render(GuiGraphics guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        renderReticle(guiGraphics, vehicle);
        int width = guiGraphics.guiWidth();
        int y = guiGraphics.guiHeight() - 58;
        float healthRatio = vehicle.maxVehicleHealth() <= 0.0F ? 0.0F : Mth.clamp(vehicle.vehicleHealth() / vehicle.maxVehicleHealth(), 0.0F, 1.0F);
        int barWidth = 112;
        int filled = Math.round(barWidth * healthRatio);
        int x = width / 2 - barWidth / 2;

        Component title = Component.translatable("entity." + vehicle.vehicleDataId().getNamespace() + "." + vehicle.vehicleDataId().getPath());
        guiGraphics.drawString(minecraft.font, title, (width - minecraft.font.width(title)) / 2, y - 12, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + barWidth, y + 7, 0xAA101010);
        guiGraphics.fill(x + 1, y + 1, x + Math.max(1, filled - 1), y + 6, 0xFF42B35A);

        Component health = Component.translatable("hud.jeg.vehicle.health", Math.round(vehicle.vehicleHealth()), Math.round(vehicle.maxVehicleHealth()));
        guiGraphics.drawString(minecraft.font, health, (width - minecraft.font.width(health)) / 2, y + 11, 0xFFE6E6E6);
        int lineY = y + 22;
        if (vehicle.maxVehicleEnergy() > 0) {
            Component energy = Component.translatable("hud.jeg.vehicle.energy", vehicle.vehicleEnergy(), vehicle.maxVehicleEnergy());
            guiGraphics.drawString(minecraft.font, energy, (width - minecraft.font.width(energy)) / 2, lineY, 0xFF8FC7FF);
            lineY += 11;
        }
        Component weaponName = Component.translatable("item." + vehicle.selectedVehicleWeaponId().getNamespace() + "." + vehicle.selectedVehicleWeaponId().getPath());
        Component ammo = Component.translatable("hud.jeg.vehicle.weapon", weaponName, vehicle.selectedVehicleWeaponAmmo());
        guiGraphics.drawString(minecraft.font, ammo, (width - minecraft.font.width(ammo)) / 2, lineY, 0xFFFFDD88);
        lineY += 11;
        Component decoy = Component.translatable("hud.jeg.vehicle.decoy", vehicle.vehicleFlareAmmo(), Math.ceilDiv(vehicle.vehicleDecoyCooldown(), 20));
        guiGraphics.drawString(minecraft.font, decoy, (width - minecraft.font.width(decoy)) / 2, lineY, 0xFFB8E0FF);
        lineY += 11;
        if (vehicle.isSelectedVehicleWeaponGuided()) {
            Component lock = Component.translatable(vehicle.hasMissileLock() ? "hud.jeg.vehicle.locked" : "hud.jeg.vehicle.locking");
            guiGraphics.drawString(minecraft.font, lock, (width - minecraft.font.width(lock)) / 2, lineY, vehicle.hasMissileLock() ? 0xFFFF5555 : 0xFFFFDD88);
            lineY += 11;
        }
        if (vehicle.isEngineDamaged() || vehicle.isLeftWheelDamaged() || vehicle.isRightWheelDamaged()) {
            Component damage = Component.translatable(
                    "hud.jeg.vehicle.parts",
                    vehicle.isEngineDamaged() ? "!" : "-",
                    vehicle.isLeftWheelDamaged() ? "!" : "-",
                    vehicle.isRightWheelDamaged() ? "!" : "-"
            );
            guiGraphics.drawString(minecraft.font, damage, (width - minecraft.font.width(damage)) / 2, lineY, 0xFFFF7777);
        }
    }

    private static void renderReticle(GuiGraphics guiGraphics, VehicleEntity vehicle) {
        int centerX = guiGraphics.guiWidth() / 2;
        int centerY = guiGraphics.guiHeight() / 2;
        int color = vehicle.isSelectedVehicleWeaponGuided()
                ? (vehicle.hasMissileLock() ? 0xFFFF5555 : 0xFFFFDD88)
                : 0xFFE6E6E6;
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 1, centerY + 1, color);
        guiGraphics.fill(centerX - 13, centerY, centerX - 5, centerY + 1, color);
        guiGraphics.fill(centerX + 5, centerY, centerX + 13, centerY + 1, color);
        guiGraphics.fill(centerX, centerY - 13, centerX + 1, centerY - 5, color);
        guiGraphics.fill(centerX, centerY + 5, centerX + 1, centerY + 13, color);
        if (vehicle.isSelectedVehicleWeaponGuided()) {
            guiGraphics.fill(centerX - 18, centerY - 18, centerX - 10, centerY - 17, color);
            guiGraphics.fill(centerX - 18, centerY - 18, centerX - 17, centerY - 10, color);
            guiGraphics.fill(centerX + 10, centerY - 18, centerX + 18, centerY - 17, color);
            guiGraphics.fill(centerX + 17, centerY - 18, centerX + 18, centerY - 10, color);
            guiGraphics.fill(centerX - 18, centerY + 17, centerX - 10, centerY + 18, color);
            guiGraphics.fill(centerX - 18, centerY + 10, centerX - 17, centerY + 18, color);
            guiGraphics.fill(centerX + 10, centerY + 17, centerX + 18, centerY + 18, color);
            guiGraphics.fill(centerX + 17, centerY + 10, centerX + 18, centerY + 18, color);
        }
    }
}
