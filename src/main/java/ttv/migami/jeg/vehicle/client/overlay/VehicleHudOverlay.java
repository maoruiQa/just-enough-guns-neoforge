package ttv.migami.jeg.vehicle.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.vehicle.client.VehicleClientState;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehicleHudOverlay {
    private static final ResourceLocation ARMOR_ICON = Reference.id("textures/overlay/vehicle/base/armor.png");
    private static final ResourceLocation ENERGY_ICON = Reference.id("textures/overlay/vehicle/base/energy.png");
    private static final ResourceLocation COMPASS = Reference.id("textures/overlay/vehicle/base/compass.png");
    private static final ResourceLocation DRIVER_ICON = Reference.id("textures/overlay/vehicle/base/driver.png");
    private static final ResourceLocation PASSENGER_ICON = Reference.id("textures/overlay/vehicle/base/passenger.png");
    private static final ResourceLocation VALUE_BAR = Reference.id("textures/overlay/vehicle/base/value_bar.png");
    private static final ResourceLocation VALUE_FRAME = Reference.id("textures/overlay/vehicle/base/value_frame.png");
    private static final ResourceLocation ROLL_INDICATOR = Reference.id("textures/overlay/vehicle/helicopter/roll_ind.png");
    private static final ResourceLocation LAND_FRAME = Reference.id("textures/overlay/vehicle/land/tv_frame.png");
    private static final ResourceLocation LAND_BODY = Reference.id("textures/overlay/vehicle/land/body.png");
    private static final ResourceLocation LAND_LEFT_WHEEL = Reference.id("textures/overlay/vehicle/land/left_wheel.png");
    private static final ResourceLocation LAND_RIGHT_WHEEL = Reference.id("textures/overlay/vehicle/land/right_wheel.png");
    private static final ResourceLocation LAND_ENGINE = Reference.id("textures/overlay/vehicle/land/engine.png");
    private static final ResourceLocation LAND_LINE = Reference.id("textures/overlay/vehicle/land/line.png");
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
        renderPassengerInfo(guiGraphics, minecraft, vehicle);
        renderLandVehicleStatus(guiGraphics, minecraft, vehicle);
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
        Component decoy = vehicle.hasBuiltInDecoy()
                ? Component.translatable("hud.jeg.vehicle.decoy_smoke", vehicle.vehicleDecoyCooldown() == 0 ? Component.translatable("hud.jeg.vehicle.ready") : Component.literal(Math.ceilDiv(vehicle.vehicleDecoyCooldown(), 20) + "s"))
                : Component.translatable("hud.jeg.vehicle.decoy", vehicle.vehicleFlareAmmo(), Math.ceilDiv(vehicle.vehicleDecoyCooldown(), 20));
        guiGraphics.drawString(minecraft.font, decoy, (width - minecraft.font.width(decoy)) / 2, lineY, 0xFFB8E0FF);
        lineY += 11;
        if (vehicle.isSelectedVehicleWeaponGuided()) {
            boolean seeking = VehicleClientState.isRidingVehicle()
                    && VehicleClientState.vehicleId() == vehicle.getId()
                    && VehicleClientState.seekDown();
            Component lock = seeking
                    ? Component.translatable(vehicle.hasMissileLock() ? "hud.jeg.vehicle.locked" : "hud.jeg.vehicle.locking")
                    : Component.translatable("hud.jeg.vehicle.seek_prompt", KeyBindings.VEHICLE_SEEK.getTranslatedKeyMessage());
            guiGraphics.drawString(minecraft.font, lock, (width - minecraft.font.width(lock)) / 2, lineY, seeking ? vehicle.hasMissileLock() ? 0xFFFF5555 : 0xFFFFDD88 : 0xFFB8E0FF);
            lineY += 11;
        }
        if (vehicle.isEngineDamaged() || vehicle.isLeftWheelDamaged() || vehicle.isRightWheelDamaged() || vehicle.isTurretDamaged()) {
            Component damage = Component.translatable("hud.jeg.vehicle.parts_compact");
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

    private static void renderPassengerInfo(GuiGraphics guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        int seatCount = vehicle.vehicleData().defaults().seats().size();
        int screenHeight = guiGraphics.guiHeight();
        for (int seat = seatCount - 1; seat >= 0; seat--) {
            int row = seatCount - 1 - seat;
            int y = screenHeight - 35 - row * 12;
            Entity passenger = seat < vehicle.getPassengers().size() ? vehicle.getPassengers().get(seat) : null;
            Component name = passenger == null ? Component.literal("---") : passenger.getName();
            String number = "[" + (seat + 1) + "]";
            guiGraphics.drawString(minecraft.font, number, 25 - minecraft.font.width(number), y, 0xFF66FF00);
            guiGraphics.blit(seat == 0 ? DRIVER_ICON : PASSENGER_ICON, 30, y, 0.0F, 0.0F, 8, 8, 8, 8);
            guiGraphics.drawString(minecraft.font, name, 42, y, passenger == null ? 0xFF4F8740 : 0xFF66FF00);
        }
    }

    private static void renderLandVehicleStatus(GuiGraphics guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        String vehiclePath = vehicle.vehicleDataId().getPath();
        if (!"lav150".equals(vehiclePath) && !"bmp2".equals(vehiclePath)) {
            return;
        }

        boolean focusedSight = Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON
                || (VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown());
        if (focusedSight && "lav150".equals(vehiclePath)) {
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            int frameWidth = Math.max(screenWidth, screenHeight * 16 / 9);
            int frameHeight = Math.max(screenHeight, screenWidth * 9 / 16);
            guiGraphics.blit(LAND_FRAME, (screenWidth - frameWidth) / 2, (screenHeight - frameHeight) / 2, frameWidth, frameHeight, 0.0F, 0.0F, 1920, 1080, 1920, 1080);
            int compassOffset = Mth.floor(128.0F + 64.0F / 45.0F * minecraft.player.getYRot());
            guiGraphics.blit(COMPASS, screenWidth / 2 - 128, 10, compassOffset, 0.0F, 256, 16, 1024, 32);
            guiGraphics.blit(ROLL_INDICATOR, screenWidth / 2 - 8, 30, 0.0F, 0.0F, 16, 16, 16, 16);
        }

        int x = guiGraphics.guiWidth() / 2 + 96;
        int y = guiGraphics.guiHeight() - 72;
        guiGraphics.blit(LAND_LINE, guiGraphics.guiWidth() / 2 - 64, guiGraphics.guiHeight() - 56, 0.0F, 0.0F, 128, 1, 128, 1);
        blitVehiclePart(guiGraphics, LAND_BODY, x, y, vehicle.vehicleHealth() <= vehicle.maxVehicleHealth() * 0.35F);
        blitVehiclePart(guiGraphics, LAND_LEFT_WHEEL, x, y, vehicle.isLeftWheelDamaged());
        blitVehiclePart(guiGraphics, LAND_RIGHT_WHEEL, x, y, vehicle.isRightWheelDamaged());
        blitVehiclePart(guiGraphics, LAND_ENGINE, x, y, vehicle.isEngineDamaged());
        if (vehicle.isTurretDamaged()) {
            guiGraphics.fill(guiGraphics.guiWidth() / 2 + 112, guiGraphics.guiHeight() - 71, guiGraphics.guiWidth() / 2 + 113, guiGraphics.guiHeight() - 55, 0xFFFF4033);
        }

        int speed = (int) Math.round(vehicle.getDeltaMovement().horizontalDistance() * 72.0D);
        Component speedText = Component.literal(speed + " km/h");
        guiGraphics.drawString(minecraft.font, speedText, x + 64, guiGraphics.guiHeight() / 2 - 48, 0xFF66FF00);
    }

    private static void blitVehiclePart(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, boolean damaged) {
        if (damaged) {
            RenderSystem.setShaderColor(1.0F, 0.25F, 0.2F, 1.0F);
        }
        guiGraphics.blit(texture, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
