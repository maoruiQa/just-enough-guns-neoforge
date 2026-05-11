package ttv.migami.jeg.vehicle.client.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
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
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleWeaponStats;
import org.joml.Matrix4f;

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
    private static final ResourceLocation WEAPON_ICON_CANNON_20MM = Reference.id("textures/overlay/vehicle/weapon/icons/cannon_20mm.png");
    private static final ResourceLocation WEAPON_ICON_COAX_762 = Reference.id("textures/overlay/vehicle/weapon/icons/gun_7_62mm.png");
    private static final ResourceLocation WEAPON_SELECTED = Reference.id("textures/overlay/vehicle/weapon/frame/selected.png");
    private static final ResourceLocation WEAPON_NUMBER = Reference.id("textures/overlay/vehicle/weapon/frame/number.png");
    private static final int WEAPON_ICON_WIDTH = 75;
    private static final int WEAPON_ICON_HEIGHT = 16;
    private static final int WEAPON_ICON_TEXTURE_WIDTH = 300;
    private static final int WEAPON_ICON_TEXTURE_HEIGHT = 64;
    private static final ResourceLocation[] WEAPON_FRAMES = {
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_1.png"),
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_2.png"),
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_3.png"),
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_4.png"),
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_5.png"),
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_6.png"),
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_7.png"),
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_8.png"),
            Reference.id("textures/overlay/vehicle/weapon/frame/frame_9.png")
    };

    private VehicleHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName() == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if ("hotbar".equals(event.getName().getPath())) {
            event.setCanceled(true);
            return;
        }
        if (!"crosshair".equals(event.getName().getPath())) {
            return;
        }
        render(event.getGuiGraphics(), minecraft, vehicle);
        event.setCanceled(true);
    }

    private static void render(GuiGraphics guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        boolean focusedSight = isFocusedVehicleSight(vehicle);
        renderLandVehicleStatus(guiGraphics, minecraft, vehicle);
        if (vehicle.hasVehicleWeapons()) {
            renderReticle(guiGraphics, vehicle);
        }
        renderPassengerInfo(guiGraphics, minecraft, vehicle);
        renderWeaponSelector(guiGraphics, minecraft, vehicle);
        if (focusedSight) {
            return;
        }
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
        if (!focusedSight && vehicle.hasVehicleWeapons()) {
            Component weaponName = Component.translatable("item." + vehicle.selectedVehicleWeaponId().getNamespace() + "." + vehicle.selectedVehicleWeaponId().getPath());
            Component ammo = vehicleUsesLoadedAmmo(vehicle)
                    ? vehicleAmmoComponent(vehicle, weaponName)
                    : Component.translatable("hud.jeg.vehicle.weapon", weaponName, String.valueOf(vehicle.selectedVehicleWeaponAmmo()));
            renderSelectedWeaponIcon(guiGraphics, vehicle, width / 2 - 128, lineY - 4);
            guiGraphics.drawString(minecraft.font, ammo, (width - minecraft.font.width(ammo)) / 2, lineY, 0xFFFFDD88);
            lineY += 11;
            if (vehicle.selectedVehicleWeaponReloading()) {
                int reloadSeconds = Math.max(1, Math.ceilDiv(vehicle.selectedVehicleWeaponReloadTicks(), 20));
                Component reload = Component.literal(Component.translatable("subtitle.jeg.reload").getString() + " " + reloadSeconds + "s");
                guiGraphics.drawString(minecraft.font, reload, (width - minecraft.font.width(reload)) / 2, lineY, 0xFFFFAA55);
                lineY += 11;
            } else if (canManualReloadPrompt(vehicle)) {
                Component prompt = Component.literal("Press " + KeyBindings.RELOAD.getTranslatedKeyMessage().getString() + " to reload");
                guiGraphics.drawString(minecraft.font, prompt, (width - minecraft.font.width(prompt)) / 2, lineY, 0xFFB8E0FF);
                lineY += 11;
            }
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
        }
        if (showDecoyStatus(vehicle)) {
            Component decoy = vehicle.hasBuiltInDecoy()
                    ? Component.translatable("hud.jeg.vehicle.decoy_smoke", vehicle.vehicleDecoyCooldown() == 0 ? Component.translatable("hud.jeg.vehicle.ready") : Component.literal(Math.ceilDiv(vehicle.vehicleDecoyCooldown(), 20) + "s"))
                    : Component.translatable("hud.jeg.vehicle.decoy", vehicle.vehicleFlareAmmo(), Math.ceilDiv(vehicle.vehicleDecoyCooldown(), 20));
            guiGraphics.drawString(minecraft.font, decoy, (width - minecraft.font.width(decoy)) / 2, lineY, 0xFFB8E0FF);
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

    private static void renderSelectedWeaponIcon(GuiGraphics guiGraphics, VehicleEntity vehicle, int x, int y) {
        ResourceLocation icon = switch (vehicle.selectedVehicleWeaponId().getPath()) {
            case "vehicle_20mm_cannon" -> WEAPON_ICON_CANNON_20MM;
            case "vehicle_30mm_cannon" -> Reference.id("textures/overlay/vehicle/weapon/icons/cannon_30mm.png");
            case "vehicle_coax_machine_gun", "light_machine_gun" -> WEAPON_ICON_COAX_762;
            default -> null;
        };
        if (icon != null) {
            blitWeaponIcon(guiGraphics, icon, x, y);
        }
    }

    private static void renderWeaponSelector(GuiGraphics guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        if (!hasWeaponSelectorHud(vehicle)) {
            return;
        }
        var weapons = vehicle.vehicleData().defaults().weapons();
        int selected = vehicle.selectedVehicleWeaponIndex();
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int frameIndex = 0;
        for (int index = weapons.size() - 1; index >= 0 && index < 9; index--) {
            int x = width - 85;
            int y = height - frameIndex * 18 - 20;
            ResourceLocation icon = weaponIcon(weapons.get(index).weaponId());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, index == selected ? 1.0F : 0.35F);
            guiGraphics.blit(WEAPON_FRAMES[Math.min(index, WEAPON_FRAMES.length - 1)], x, y, 0.0F, 0.0F, 75, 16, 75, 16);
            if (icon != null) {
                blitWeaponIcon(guiGraphics, icon, x, y);
            } else {
                Component name = Component.translatable("item." + weapons.get(index).weaponId().getNamespace() + "." + weapons.get(index).weaponId().getPath());
                guiGraphics.drawString(minecraft.font, name, x + 4, y + 4, 0xFFFFFFFF, false);
            }
            if (index == selected) {
                renderWeaponNumber(guiGraphics, vehicle.selectedVehicleWeaponAmmo(), width - 20, y + 4);
                if (vehicle.selectedVehicleWeaponReloading()) {
                    Component reload = Component.literal("R");
                    guiGraphics.drawString(minecraft.font, reload, x + 4, y + 5, 0xFFFFAA55, false);
                }
            } else {
                Component slotNumber = Component.literal(String.valueOf(index + 1));
                guiGraphics.drawString(minecraft.font, slotNumber, width - 20 - minecraft.font.width(slotNumber), y + 5, 0xFFFFFFFF, false);
            }
            if (index == selected) {
                guiGraphics.blit(WEAPON_SELECTED, width - 95, y + 4, 0.0F, 0.0F, 8, 8, 8, 8);
            }
            frameIndex++;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation weaponIcon(ResourceLocation weaponId) {
        return switch (weaponId.getPath()) {
            case "vehicle_20mm_cannon" -> WEAPON_ICON_CANNON_20MM;
            case "vehicle_30mm_cannon" -> Reference.id("textures/overlay/vehicle/weapon/icons/cannon_30mm.png");
            case "vehicle_coax_machine_gun", "light_machine_gun" -> WEAPON_ICON_COAX_762;
            default -> null;
        };
    }

    private static boolean isRifleAmmoWeapon(ResourceLocation weaponId) {
        return switch (weaponId.getPath()) {
            case "vehicle_coax_machine_gun", "light_machine_gun" -> true;
            default -> false;
        };
    }

    private static Component vehicleAmmoComponent(VehicleEntity vehicle, Component weaponName) {
        String ammoCounts = vehicle.selectedVehicleWeaponAmmo() + "/" + vehicle.selectedVehicleWeaponReserveAmmo();
        return isRifleAmmoWeapon(vehicle.selectedVehicleWeaponId())
                ? Component.translatable("hud.jeg.vehicle.rifle_ammo", ammoCounts)
                : Component.translatable("hud.jeg.vehicle.weapon", weaponName, ammoCounts);
    }

    private static boolean vehicleUsesLoadedAmmo(VehicleEntity vehicle) {
        var stats = VehicleWeaponStats.get(vehicle.selectedVehicleWeaponId());
        return stats != null && stats.usesMagazine() && !stats.isInventoryFed();
    }

    private static boolean canManualReloadPrompt(VehicleEntity vehicle) {
        return vehicleUsesLoadedAmmo(vehicle)
                && vehicle.selectedVehicleWeaponAmmo() > 0
                && vehicle.selectedVehicleWeaponAmmo() < vehicle.selectedVehicleWeaponReserveAmmo() + vehicle.selectedVehicleWeaponAmmo();
    }

    private static boolean showDecoyStatus(VehicleEntity vehicle) {
        return vehicle.hasBuiltInDecoy() || vehicle.vehicleFlareAmmo() > 0 || vehicle.vehicleDecoyCooldown() > 0;
    }

    private static void blitWeaponIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y) {
        guiGraphics.blit(icon, x, y, WEAPON_ICON_WIDTH, WEAPON_ICON_HEIGHT, 0.0F, 0.0F, WEAPON_ICON_TEXTURE_WIDTH, WEAPON_ICON_TEXTURE_HEIGHT, WEAPON_ICON_TEXTURE_WIDTH, WEAPON_ICON_TEXTURE_HEIGHT);
    }

    private static void renderWeaponNumber(GuiGraphics guiGraphics, int number, int rightX, int y) {
        int clamped = Math.max(0, number);
        if (clamped == 0) {
            blitWeaponDigit(guiGraphics, 0, rightX - 5, y);
            return;
        }
        int offset = 0;
        while (clamped > 0) {
            int digit = clamped % 10;
            blitWeaponDigit(guiGraphics, digit, rightX - 5 - offset * 5, y);
            clamped /= 10;
            offset++;
        }
    }

    private static void blitWeaponDigit(GuiGraphics guiGraphics, int digit, int x, int y) {
        guiGraphics.blit(WEAPON_NUMBER, x, y, 5, 8, digit * 20.0F, 0.0F, 20, 30, 300, 30);
    }

    private static void renderReticle(GuiGraphics guiGraphics, VehicleEntity vehicle) {
        int size = Math.min(guiGraphics.guiWidth(), guiGraphics.guiHeight());
        int x = (guiGraphics.guiWidth() - size) / 2;
        int y = (guiGraphics.guiHeight() - size) / 2;
        ResourceLocation texture = reticleTexture(vehicle);
        preciseBlit(guiGraphics, texture, x, y, size, size, 0.0F, 0.0F, 512.0F, 512.0F, 512.0F, 512.0F);
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
            Entity passenger = vehicle.passengerForSeat(seat);
            Component name = passenger == null ? Component.literal("---") : passenger.getName();
            String number = "[" + (seat + 1) + "]";
            guiGraphics.drawString(minecraft.font, number, 25 - minecraft.font.width(number), y, 0xFF66FF00);
            guiGraphics.blit(seat == 0 ? DRIVER_ICON : PASSENGER_ICON, 30, y, 0.0F, 0.0F, 8, 8, 8, 8);
            guiGraphics.drawString(minecraft.font, name, 42, y, passenger == null ? 0xFF4F8740 : 0xFF66FF00);
        }
    }

    private static void renderLandVehicleStatus(GuiGraphics guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        if (vehicle.vehicleData().defaults().vehicleType() != VehicleType.LAND) {
            return;
        }

        String vehiclePath = vehicle.vehicleDataId().getPath();
        boolean focusedSight = isFocusedVehicleSight(vehicle);
        if (focusedSight && hasApcFocusedSightFrame(vehiclePath)) {
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            int addW = (screenWidth / screenHeight) * 48;
            int addH = (screenWidth / screenHeight) * 27;
            preciseBlit(guiGraphics, LAND_FRAME, -addW / 2.0F, -addH / 2.0F, screenWidth + addW, screenHeight + addH, 0.0F, 0.0F, 1920.0F, 1080.0F, 1920.0F, 1080.0F);
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

        int speed = (int) Math.round(Math.sqrt(vehicle.distanceToSqr(vehicle.xOld, vehicle.getY(), vehicle.zOld)) * 72.0D);
        Component speedText = Component.literal(speed + " km/h");
        guiGraphics.drawString(minecraft.font, speedText, x + 64, guiGraphics.guiHeight() / 2 - 48, 0xFF66FF00);
    }

    private static boolean hasApcWeaponHud(VehicleEntity vehicle) {
        String vehiclePath = vehicle.vehicleDataId().getPath();
        return "lav150".equals(vehiclePath) || "bmp2".equals(vehiclePath);
    }

    private static boolean hasWeaponSelectorHud(VehicleEntity vehicle) {
        return vehicle.hasVehicleWeapons() && (hasApcWeaponHud(vehicle) || vehicle.vehicleData().defaults().weapons().size() > 1);
    }

    private static boolean hasApcFocusedSightFrame(String vehiclePath) {
        return "lav150".equals(vehiclePath) || "bmp2".equals(vehiclePath);
    }

    private static boolean isFocusedVehicleSight(VehicleEntity vehicle) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && player.getVehicle() == vehicle
                && vehicle.passengerForSeat(0) == player
                && hasApcFocusedSightFrame(vehicle.vehicleDataId().getPath())
                && VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
    }

    private static void preciseBlit(GuiGraphics guiGraphics, ResourceLocation texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float minU = uOffset / textureWidth;
        float maxU = (uOffset + uWidth) / textureWidth;
        float minV = vOffset / textureHeight;
        float maxV = (vOffset + vHeight) / textureHeight;
        buffer.addVertex(matrix, x, y, 0.0F).setUv(minU, minV);
        buffer.addVertex(matrix, x, y + height, 0.0F).setUv(minU, maxV);
        buffer.addVertex(matrix, x + width, y + height, 0.0F).setUv(maxU, maxV);
        buffer.addVertex(matrix, x + width, y, 0.0F).setUv(maxU, minV);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void blitVehiclePart(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, boolean damaged) {
        if (damaged) {
            RenderSystem.setShaderColor(1.0F, 0.25F, 0.2F, 1.0F);
        }
        guiGraphics.blit(texture, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
