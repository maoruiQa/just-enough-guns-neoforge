package ttv.migami.jeg.vehicle.client.overlay;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.client.VehicleClientState;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehicleHudOverlay {
    private static final ResourceLocation ARMOR_ICON = Reference.id("textures/overlay/vehicle/base/armor.png");
    private static final ResourceLocation ENERGY_ICON = Reference.id("textures/overlay/vehicle/base/energy.png");
    private static final ResourceLocation VALUE_BAR = Reference.id("textures/overlay/vehicle/base/value_bar.png");
    private static final ResourceLocation VALUE_FRAME = Reference.id("textures/overlay/vehicle/base/value_frame.png");
    private static final ResourceLocation CROSSHAIR_GUN = Reference.id("textures/overlay/vehicle/crosshair/common_gun.png");
    private static final ResourceLocation CROSSHAIR_CANNON = Reference.id("textures/overlay/vehicle/crosshair/common_cannon.png");
    private static final ResourceLocation CROSSHAIR_CANNON_ZOOMING = Reference.id("textures/overlay/vehicle/crosshair/common_cannon_zooming.png");
    private static final ResourceLocation CROSSHAIR_CN_HPJ_ZOOMING = Reference.id("textures/overlay/vehicle/crosshair/cn_hpj_zooming.png");
    private static final ResourceLocation CROSSHAIR_LASER_CANNON = Reference.id("textures/overlay/vehicle/crosshair/laser_cannon.png");
    private static final ResourceLocation CROSSHAIR_SEEK_MISSILE = Reference.id("textures/overlay/vehicle/crosshair/common_seek_missile.png");
    private static final ResourceLocation CROSSHAIR_THIRD_CAMERA = Reference.id("textures/overlay/vehicle/crosshair/third_camera.png");
    private static final ResourceLocation CROSSHAIR_US_APC = Reference.id("textures/overlay/vehicle/crosshair/us_apc.png");

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
        int barWidth = 60;
        int x = width / 2 - barWidth / 2;

        Component title = Component.translatable("entity." + vehicle.vehicleDataId().getNamespace() + "." + vehicle.vehicleDataId().getPath());
        guiGraphics.drawString(minecraft.font, title, (width - minecraft.font.width(title)) / 2, y - 12, 0xFFFFFFFF);
        renderValueBar(guiGraphics, ARMOR_ICON, x - 12, y - 1, healthRatio);

        Component health = Component.translatable("hud.jeg.vehicle.health", Math.round(vehicle.vehicleHealth()), Math.round(vehicle.maxVehicleHealth()));
        guiGraphics.drawString(minecraft.font, health, (width - minecraft.font.width(health)) / 2, y + 11, 0xFFE6E6E6);
        int lineY = y + 22;
        if (vehicle.maxVehicleEnergy() > 0) {
            float energyRatio = Mth.clamp((float) vehicle.vehicleEnergy() / (float) vehicle.maxVehicleEnergy(), 0.0F, 1.0F);
            renderValueBar(guiGraphics, ENERGY_ICON, x - 12, lineY - 1, energyRatio);
            Component energy = Component.translatable("hud.jeg.vehicle.energy", vehicle.vehicleEnergy(), vehicle.maxVehicleEnergy());
            guiGraphics.drawString(minecraft.font, energy, (width - minecraft.font.width(energy)) / 2, lineY + 8, 0xFF8FC7FF);
            lineY += 19;
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
        if (vehicle.isEngineDamaged() || vehicle.isLeftWheelDamaged() || vehicle.isRightWheelDamaged() || vehicle.isTurretDamaged()) {
            Component damage = Component.translatable(
                    "hud.jeg.vehicle.parts",
                    vehicle.isEngineDamaged() ? "!" : "-",
                    vehicle.isLeftWheelDamaged() ? "!" : "-",
                    vehicle.isRightWheelDamaged() ? "!" : "-",
                    vehicle.isTurretDamaged() ? "!" : "-"
            );
            guiGraphics.drawString(minecraft.font, damage, (width - minecraft.font.width(damage)) / 2, lineY, 0xFFFF7777);
        }
    }

    private static void renderValueBar(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y, float ratio) {
        int filled = Mth.clamp(Math.round(60.0F * ratio), 0, 60);
        guiGraphics.blit(icon, x, y, 0.0F, 0.0F, 10, 10, 10, 10);
        guiGraphics.blit(VALUE_FRAME, x + 11, y + 2, 60, 6, 0.0F, 0.0F, 120, 12, 120, 12);
        if (filled > 0) {
            guiGraphics.blit(VALUE_BAR, x + 11, y + 2, 0.0F, 0.0F, filled, 6, 60, 6);
        }
    }

    private static void renderReticle(GuiGraphics guiGraphics, VehicleEntity vehicle) {
        int size = Math.min(guiGraphics.guiWidth(), guiGraphics.guiHeight());
        int x = (guiGraphics.guiWidth() - size) / 2;
        int y = (guiGraphics.guiHeight() - size) / 2;
        ResourceLocation texture = reticleTexture(vehicle);
        guiGraphics.blit(texture, x, y, size, size, 0.0F, 0.0F, 512, 512, 512, 512);
    }

    private static ResourceLocation reticleTexture(VehicleEntity vehicle) {
        String vehiclePath = vehicle.vehicleDataId().getPath();
        String weaponPath = vehicle.selectedVehicleWeaponId().getPath();
        boolean zooming = VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
        if (!zooming && Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            return CROSSHAIR_THIRD_CAMERA;
        }
        if (zooming && "hpj11".equals(vehiclePath)) {
            return CROSSHAIR_CN_HPJ_ZOOMING;
        }
        if (vehicle.isSelectedVehicleWeaponGuided()) {
            return CROSSHAIR_SEEK_MISSILE;
        }
        if ("hypersonic_cannon".equals(weaponPath) || "grenade_launcher".equals(weaponPath)) {
            return zooming ? CROSSHAIR_CANNON_ZOOMING : CROSSHAIR_CANNON;
        }
        if ("laser_tower".equals(vehiclePath) || "waveforce_tower".equals(vehiclePath)) {
            return CROSSHAIR_LASER_CANNON;
        }
        if ("lav150".equals(vehiclePath) || "bmp2".equals(vehiclePath)) {
            return CROSSHAIR_US_APC;
        }
        return CROSSHAIR_GUN;
    }
}
