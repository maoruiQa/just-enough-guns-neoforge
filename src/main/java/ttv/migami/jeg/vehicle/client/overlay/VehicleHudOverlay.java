package ttv.migami.jeg.vehicle.client.overlay;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehicleHudOverlay {
    private static final Identifier ARMOR_ICON = Reference.id("textures/overlay/vehicle/base/armor.png");
    private static final Identifier ENERGY_ICON = Reference.id("textures/overlay/vehicle/base/energy.png");
    private static final Identifier COMPASS = Reference.id("textures/overlay/vehicle/base/compass.png");
    private static final Identifier DRIVER_ICON = Reference.id("textures/overlay/vehicle/base/driver.png");
    private static final Identifier PASSENGER_ICON = Reference.id("textures/overlay/vehicle/base/passenger.png");
    private static final Identifier VALUE_BAR = Reference.id("textures/overlay/vehicle/base/value_bar.png");
    private static final Identifier VALUE_FRAME = Reference.id("textures/overlay/vehicle/base/value_frame.png");
    private static final Identifier ROLL_INDICATOR = Reference.id("textures/overlay/vehicle/helicopter/roll_ind.png");
    private static final Identifier LAND_FRAME = Reference.id("textures/overlay/vehicle/land/tv_frame.png");
    private static final Identifier LAND_BODY = Reference.id("textures/overlay/vehicle/land/body.png");
    private static final Identifier LAND_LEFT_WHEEL = Reference.id("textures/overlay/vehicle/land/left_wheel.png");
    private static final Identifier LAND_RIGHT_WHEEL = Reference.id("textures/overlay/vehicle/land/right_wheel.png");
    private static final Identifier LAND_ENGINE = Reference.id("textures/overlay/vehicle/land/engine.png");
    private static final Identifier LAND_LINE = Reference.id("textures/overlay/vehicle/land/line.png");
    private static final Identifier CROSSHAIR_GUN = Reference.id("textures/overlay/vehicle/crosshair/common_gun.png");
    private static final Identifier CROSSHAIR_CANNON = Reference.id("textures/overlay/vehicle/crosshair/common_cannon.png");
    private static final Identifier CROSSHAIR_CANNON_ZOOMING = Reference.id("textures/overlay/vehicle/crosshair/common_cannon_zooming.png");
    private static final Identifier CROSSHAIR_CN_HPJ_ZOOMING = Reference.id("textures/overlay/vehicle/crosshair/cn_hpj_zooming.png");
    private static final Identifier CROSSHAIR_LASER_CANNON = Reference.id("textures/overlay/vehicle/crosshair/laser_cannon.png");
    private static final Identifier CROSSHAIR_MISSILE = Reference.id("textures/overlay/vehicle/crosshair/common_missile.png");
    private static final Identifier CROSSHAIR_SEEK_MISSILE = Reference.id("textures/overlay/vehicle/crosshair/common_seek_missile.png");
    private static final Identifier CROSSHAIR_THIRD_CAMERA = Reference.id("textures/overlay/vehicle/crosshair/third_camera.png");
    private static final Identifier CROSSHAIR_US_APC = Reference.id("textures/overlay/vehicle/crosshair/us_apc.png");
    private static final Identifier WEAPON_ICON_CANNON_20MM = Reference.id("textures/overlay/vehicle/weapon/icons/cannon_20mm.png");
    private static final Identifier WEAPON_ICON_COAX_762 = Reference.id("textures/overlay/vehicle/weapon/icons/gun_7_62mm.png");
    private static final Identifier WEAPON_SELECTED = Reference.id("textures/overlay/vehicle/weapon/frame/selected.png");
    private static final Identifier WEAPON_NUMBER = Reference.id("textures/overlay/vehicle/weapon/frame/number.png");
    private static final int WEAPON_ICON_WIDTH = 75;
    private static final int WEAPON_ICON_HEIGHT = 16;
    private static final int WEAPON_ICON_TEXTURE_WIDTH = 300;
    private static final int WEAPON_ICON_TEXTURE_HEIGHT = 64;
    private static final Identifier[] WEAPON_FRAMES = {
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
        if ("hotbar".equals(event.getName().getPath()) && shouldReplaceHotbar(player, vehicle)) {
            event.setCanceled(true);
            return;
        }
        if (!"crosshair".equals(event.getName().getPath())) {
            return;
        }
        render(event.getGuiGraphics(), minecraft, vehicle);
        event.setCanceled(true);
    }

    private static boolean shouldReplaceHotbar(LocalPlayer player, VehicleEntity vehicle) {
        return player == vehicle.getControllingPassenger()
                || vehicle.shouldBanPassengerHand(player)
                || vehicle.canPassengerUseSelectedVehicleWeapon(player);
    }

    private static void render(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        boolean focusedSight = isFocusedVehicleSight(vehicle);
        renderLandVehicleStatus(guiGraphics, minecraft, vehicle);
        if (vehicle.hasVehicleWeapons() && vehicle.canPassengerUseSelectedVehicleWeapon(minecraft.player)) {
            renderReticle(guiGraphics, vehicle);
        }
        renderPassengerInfo(guiGraphics, minecraft, vehicle);
        renderWeaponSelector(guiGraphics, minecraft, vehicle);
        renderSpeedIndicator(guiGraphics, minecraft, vehicle);
        boolean showCompactVehicleInfo = !focusedSight || vehicle.vehicleData().defaults().vehicleType() == VehicleType.BOAT;
        if (!showCompactVehicleInfo) {
            return;
        }
        int width = guiGraphics.guiWidth();
        int y = guiGraphics.guiHeight() - 80;
        float healthRatio = vehicle.maxVehicleHealth() <= 0.0F ? 0.0F : Mth.clamp(vehicle.vehicleHealth() / vehicle.maxVehicleHealth(), 0.0F, 1.0F);
        int barWidth = 60;
        int x = width / 2 - barWidth / 2;

        Component title = Component.translatable("entity." + vehicle.vehicleDataId().getNamespace() + "." + vehicle.vehicleDataId().getPath());
        guiGraphics.text(minecraft.font, title, (width - minecraft.font.width(title)) / 2, y - 12, 0xFFFFFFFF);
        renderValueBar(guiGraphics, ARMOR_ICON, x - 12, y - 1, healthRatio);

        Component health = Component.translatable("hud.jeg.vehicle.health", Math.round(vehicle.vehicleHealth()), Math.round(vehicle.maxVehicleHealth()));
        guiGraphics.text(minecraft.font, health, (width - minecraft.font.width(health)) / 2, y + 11, 0xFFE6E6E6);
        int lineY = y + 22;
        if (vehicle.maxVehicleEnergy() > 0) {
            float energyRatio = Mth.clamp((float) vehicle.vehicleEnergy() / (float) vehicle.maxVehicleEnergy(), 0.0F, 1.0F);
            renderValueBar(guiGraphics, ENERGY_ICON, x - 12, lineY - 1, energyRatio);
            Component energy = Component.translatable("hud.jeg.vehicle.energy", vehicle.vehicleEnergy(), vehicle.maxVehicleEnergy());
            guiGraphics.text(minecraft.font, energy, (width - minecraft.font.width(energy)) / 2, lineY + 8, 0xFF8FC7FF);
            lineY += 19;
        }
        boolean showWeaponInfo = vehicle.hasVehicleWeapons()
                && vehicle.canPassengerUseSelectedVehicleWeapon(minecraft.player)
                && (!focusedSight || vehicle.vehicleData().defaults().vehicleType() == VehicleType.BOAT);
        if (showWeaponInfo) {
            Identifier selectedWeaponId = vehicle.selectedVehicleWeaponId(minecraft.player);
            if (selectedWeaponId != null) {
                Component ammo = vehicleUsesLoadedAmmo(vehicle, minecraft.player)
                        ? vehicleAmmoComponent(vehicle, minecraft.player)
                        : Component.translatable("hud.jeg.vehicle.weapon", Component.translatable("item." + selectedWeaponId.getNamespace() + "." + selectedWeaponId.getPath()), String.valueOf(vehicle.selectedVehicleWeaponAmmo(minecraft.player)));
                renderSelectedWeaponIcon(guiGraphics, vehicle, minecraft.player, width / 2 - 128, lineY - 4);
                guiGraphics.text(minecraft.font, ammo, (width - minecraft.font.width(ammo)) / 2, lineY, 0xFFFFDD88);
                lineY += 11;
                if (vehicle.selectedVehicleWeaponReloading(minecraft.player)) {
                    int reloadSeconds = Math.max(1, Math.ceilDiv(vehicle.selectedVehicleWeaponReloadTicks(minecraft.player), 20));
                    Component reload = Component.literal(Component.translatable("subtitle.jeg.reload").getString() + " " + reloadSeconds + "s");
                    guiGraphics.text(minecraft.font, reload, (width - minecraft.font.width(reload)) / 2, lineY, 0xFFFFAA55);
                    lineY += 11;
                }
                if (vehicle.isSelectedVehicleWeaponLockOn(minecraft.player)) {
                    boolean seeking = VehicleClientState.isRidingVehicle()
                            && VehicleClientState.vehicleId() == vehicle.getId()
                            && VehicleClientState.seekDown();
                    Component lock = seeking
                            ? Component.translatable(vehicle.hasMissileLock() ? "hud.jeg.vehicle.locked" : "hud.jeg.vehicle.locking")
                            : Component.translatable("hud.jeg.vehicle.seek_prompt", KeyBindings.VEHICLE_SEEK.getTranslatedKeyMessage());
                    guiGraphics.text(minecraft.font, lock, (width - minecraft.font.width(lock)) / 2, lineY, seeking ? vehicle.hasMissileLock() ? 0xFFFF5555 : 0xFFFFDD88 : 0xFFB8E0FF);
                    lineY += 11;
                }
            }
        }
        if (showDecoyStatus(vehicle)) {
            Component decoy = builtInDecoyUsesSmoke(vehicle)
                    ? Component.translatable("hud.jeg.vehicle.decoy_smoke", KeyBindings.VEHICLE_DEPLOY_DECOY.getTranslatedKeyMessage(), decoyStatus(vehicle))
                    : Component.translatable("hud.jeg.vehicle.decoy_flare", KeyBindings.VEHICLE_DEPLOY_DECOY.getTranslatedKeyMessage(), decoyStatus(vehicle));
            guiGraphics.text(minecraft.font, decoy, (width - minecraft.font.width(decoy)) / 2, lineY, 0xFFB8E0FF);
            lineY += 11;
        }
        if (vehicle.isEngineDamaged() || vehicle.isLeftWheelDamaged() || vehicle.isRightWheelDamaged() || vehicle.isTurretDamaged()) {
            Component damage = Component.translatable("hud.jeg.vehicle.parts_compact");
            guiGraphics.text(minecraft.font, damage, (width - minecraft.font.width(damage)) / 2, lineY, 0xFFFF7777);
        }
    }

    private static void renderValueBar(GuiGraphicsExtractor guiGraphics, Identifier icon, int x, int y, float ratio) {
        int filled = Mth.clamp(Math.round(60.0F * ratio), 0, 60);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, 10, 10, 10, 10);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, VALUE_FRAME, x + 11, y + 2, 60, 6, 0, 0, 120, 12, 120, 12);
        if (filled > 0) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, VALUE_BAR, x + 11, y + 2, 0, 0, filled, 6, 60, 6);
        }
    }

    private static void renderSelectedWeaponIcon(GuiGraphicsExtractor guiGraphics, VehicleEntity vehicle, Entity passenger, int x, int y) {
        Identifier selectedWeaponId = vehicle.selectedVehicleWeaponId(passenger);
        if (selectedWeaponId == null) {
            return;
        }
        Identifier icon = switch (selectedWeaponId.getPath()) {
            case "vehicle_20mm_cannon" -> WEAPON_ICON_CANNON_20MM;
            case "vehicle_30mm_cannon" -> Reference.id("textures/overlay/vehicle/weapon/icons/cannon_30mm.png");
            case "vehicle_coax_machine_gun", "light_machine_gun" -> WEAPON_ICON_COAX_762;
            case "vehicle_70mm_rocket", "vehicle_80mm_rocket" -> Reference.id("textures/overlay/vehicle/weapon/icons/small_rocket.png");
            case "vehicle_bmp2_missile" -> Reference.id("textures/overlay/vehicle/weapon/icons/missile_9m113.png");
            case "vehicle_9m120_driver_missile", "vehicle_9m120_passenger_missile" -> Reference.id("textures/overlay/vehicle/weapon/icons/missile_9m120.png");
            case "vehicle_kh39_missile" -> Reference.id("textures/overlay/vehicle/weapon/icons/kh_39.png");
            case "vehicle_9m336_missile" -> Reference.id("textures/overlay/vehicle/weapon/icons/ru_9m336.png");
            default -> null;
        };
        if (icon != null) {
            blitWeaponIcon(guiGraphics, icon, x, y);
        }
    }

    private static void renderWeaponSelector(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        if (!hasWeaponSelectorHud(vehicle)) {
            return;
        }
        var weapons = vehicle.vehicleData().defaults().weapons();
        int selected = vehicle.selectedVehicleWeaponIndex(minecraft.player);
        int[] usableWeaponIndexes = new int[Math.min(weapons.size(), 9)];
        int usableWeaponCount = 0;
        for (int index = 0; index < weapons.size() && usableWeaponCount < usableWeaponIndexes.length; index++) {
            if (vehicle.canPassengerUseVehicleWeapon(minecraft.player, index)) {
                usableWeaponIndexes[usableWeaponCount++] = index;
            }
        }
        if (usableWeaponCount == 0) {
            return;
        }
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int frameIndex = 0;
        for (int displayIndex = usableWeaponCount - 1; displayIndex >= 0; displayIndex--) {
            int index = usableWeaponIndexes[displayIndex];
            int x = width - 85;
            int y = height - frameIndex * 18 - 20;
            Identifier icon = weaponIcon(weapons.get(index).weaponId());
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WEAPON_FRAMES[Math.min(displayIndex, WEAPON_FRAMES.length - 1)], x, y, 0, 0, 75, 16, 75, 16);
            if (icon != null) {
                blitWeaponIcon(guiGraphics, icon, x, y);
            } else {
                Component name = Component.translatable("item." + weapons.get(index).weaponId().getNamespace() + "." + weapons.get(index).weaponId().getPath());
                guiGraphics.text(minecraft.font, name, x + 4, y + 4, 0xFFFFFFFF, false);
            }
            if (index == selected) {
                renderWeaponNumber(guiGraphics, vehicle.selectedVehicleWeaponAmmo(minecraft.player), width - 20, y + 4);
                if (vehicle.selectedVehicleWeaponReloading(minecraft.player)) {
                    Component reload = Component.literal("R");
                    guiGraphics.text(minecraft.font, reload, x + 4, y + 5, 0xFFFFAA55, false);
                }
            } else {
                Component slotNumber = Component.literal(String.valueOf(displayIndex + 1));
                guiGraphics.text(minecraft.font, slotNumber, width - 20 - minecraft.font.width(slotNumber), y + 5, 0xFFFFFFFF, false);
            }
            if (index == selected) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WEAPON_SELECTED, width - 95, y + 4, 0, 0, 8, 8, 8, 8);
            }
            frameIndex++;
        }
    }

    private static Identifier weaponIcon(Identifier weaponId) {
        return switch (weaponId.getPath()) {
            case "vehicle_20mm_cannon" -> WEAPON_ICON_CANNON_20MM;
            case "vehicle_30mm_cannon" -> Reference.id("textures/overlay/vehicle/weapon/icons/cannon_30mm.png");
            case "vehicle_coax_machine_gun", "light_machine_gun" -> WEAPON_ICON_COAX_762;
            case "vehicle_70mm_rocket", "vehicle_80mm_rocket" -> Reference.id("textures/overlay/vehicle/weapon/icons/small_rocket.png");
            case "vehicle_bmp2_missile" -> Reference.id("textures/overlay/vehicle/weapon/icons/missile_9m113.png");
            case "vehicle_9m120_driver_missile", "vehicle_9m120_passenger_missile" -> Reference.id("textures/overlay/vehicle/weapon/icons/missile_9m120.png");
            case "vehicle_kh39_missile" -> Reference.id("textures/overlay/vehicle/weapon/icons/kh_39.png");
            case "vehicle_9m336_missile" -> Reference.id("textures/overlay/vehicle/weapon/icons/ru_9m336.png");
            default -> null;
        };
    }

    private static Component vehicleAmmoComponent(VehicleEntity vehicle, Entity passenger) {
        int slot = vehicle.selectedVehicleWeaponIndex(passenger);
        var weapons = vehicle.vehicleData().defaults().weapons();
        if (slot < 0 || slot >= weapons.size()) {
            return Component.empty();
        }
        String ammoCounts = vehicle.selectedVehicleWeaponAmmo(passenger) + "/" + vehicle.selectedVehicleWeaponReserveAmmo(passenger);
        Identifier ammoId = weapons.get(slot).ammoId();
        return Component.translatable("item." + ammoId.getNamespace() + "." + ammoId.getPath())
                .append(Component.literal(": " + ammoCounts));
    }

    private static boolean vehicleUsesLoadedAmmo(VehicleEntity vehicle, Entity passenger) {
        Identifier selectedWeaponId = vehicle.selectedVehicleWeaponId(passenger);
        if (selectedWeaponId == null) {
            return false;
        }
        var stats = VehicleWeaponStats.get(selectedWeaponId);
        return stats != null && stats.usesMagazine() && !stats.isInventoryFed();
    }

    private static boolean canManualReloadPrompt(VehicleEntity vehicle) {
        Entity passenger = Minecraft.getInstance().player;
        return passenger != null && vehicle.canReloadSelectedVehicleWeapon(passenger) && vehicle.selectedVehicleWeaponAmmo(passenger) < vehicle.selectedVehicleWeaponMagazineSize(passenger);
    }

    private static boolean showDecoyStatus(VehicleEntity vehicle) {
        return vehicle.hasBuiltInDecoy() || vehicle.vehicleFlareAmmo() > 0 || vehicle.vehicleDecoyCooldown() > 0;
    }

    private static boolean builtInDecoyUsesSmoke(VehicleEntity vehicle) {
        VehicleType type = vehicle.vehicleData().defaults().vehicleType();
        return vehicle.hasBuiltInDecoy() && type != VehicleType.HELICOPTER && type != VehicleType.AIRCRAFT;
    }

    private static Component decoyStatus(VehicleEntity vehicle) {
        return vehicle.vehicleDecoyCooldown() == 0
                ? Component.translatable("hud.jeg.vehicle.ready")
                : Component.literal(Math.ceilDiv(vehicle.vehicleDecoyCooldown(), 20) + "s");
    }

    private static int vehicleSpeedKmh(VehicleEntity vehicle) {
        return (int) Math.round(Math.sqrt(vehicle.distanceToSqr(vehicle.xOld, vehicle.getY(), vehicle.zOld)) * 72.0D);
    }

    private static void renderSpeedIndicator(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        Component speed = Component.literal(vehicleSpeedKmh(vehicle) + " km/h");
        int x = guiGraphics.guiWidth() - minecraft.font.width(speed) - 16;
        int y = 16;
        guiGraphics.text(minecraft.font, speed, x, y, 0xFF66FF00);
    }

    private static void blitWeaponIcon(GuiGraphicsExtractor guiGraphics, Identifier icon, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, WEAPON_ICON_WIDTH, WEAPON_ICON_HEIGHT, 0, 0, WEAPON_ICON_TEXTURE_WIDTH, WEAPON_ICON_TEXTURE_HEIGHT, WEAPON_ICON_TEXTURE_WIDTH, WEAPON_ICON_TEXTURE_HEIGHT);
    }

    private static void renderWeaponNumber(GuiGraphicsExtractor guiGraphics, int number, int rightX, int y) {
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

    private static void blitWeaponDigit(GuiGraphicsExtractor guiGraphics, int digit, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WEAPON_NUMBER, x, y, 5, 8, digit * 20, 0, 20, 30, 300, 30);
    }

    private static void renderReticle(GuiGraphicsExtractor guiGraphics, VehicleEntity vehicle) {
        int size = Math.min(guiGraphics.guiWidth(), guiGraphics.guiHeight());
        int x = (guiGraphics.guiWidth() - size) / 2;
        int y = (guiGraphics.guiHeight() - size) / 2;
        Identifier texture = reticleTexture(vehicle);
        preciseBlit(guiGraphics, texture, x, y, size, size, 0.0F, 0.0F, 512.0F, 512.0F, 512.0F, 512.0F);
    }

    private static Identifier reticleTexture(VehicleEntity vehicle) {
        String vehiclePath = vehicle.vehicleDataId().getPath();
        Entity passenger = Minecraft.getInstance().player;
        Identifier selectedWeaponId = passenger == null ? vehicle.selectedVehicleWeaponId() : vehicle.selectedVehicleWeaponId(passenger);
        String weaponPath = selectedWeaponId == null ? "" : selectedWeaponId.getPath();
        boolean zooming = VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
        if (!zooming && Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            return CROSSHAIR_THIRD_CAMERA;
        }
        if (zooming && "hpj11".equals(vehiclePath)) {
            return CROSSHAIR_CN_HPJ_ZOOMING;
        }
        if (passenger != null && vehicle.isSelectedVehicleWeaponLockOn(passenger)) {
            return CROSSHAIR_SEEK_MISSILE;
        }
        if (passenger != null && vehicle.isSelectedVehicleWeaponGuided(passenger)) {
            return CROSSHAIR_MISSILE;
        }
        if ("hypersonic_cannon".equals(weaponPath) || "grenade_launcher".equals(weaponPath)) {
            return zooming ? CROSSHAIR_CANNON_ZOOMING : CROSSHAIR_CANNON;
        }
        if ("laser_tower".equals(vehiclePath) || "waveforce_tower".equals(vehiclePath)) {
            return CROSSHAIR_LASER_CANNON;
        }
        if (vehicle.hasFocusedDriverSightHud()) {
            return CROSSHAIR_US_APC;
        }
        return CROSSHAIR_GUN;
    }

    private static void renderPassengerInfo(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        int seatCount = vehicle.vehicleData().defaults().seats().size();
        int screenHeight = guiGraphics.guiHeight();
        for (int seat = seatCount - 1; seat >= 0; seat--) {
            int row = seatCount - 1 - seat;
            int y = screenHeight - 35 - row * 12;
            Entity passenger = vehicle.passengerForSeat(seat);
            Component name = passenger == null ? Component.literal("---") : passenger.getName();
            String number = "[" + (seat + 1) + "]";
            guiGraphics.text(minecraft.font, number, 25 - minecraft.font.width(number), y, 0xFF66FF00);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, seat == 0 ? DRIVER_ICON : PASSENGER_ICON, 30, y, 0.0F, 0.0F, 8, 8, 8, 8);
            guiGraphics.text(minecraft.font, name, 42, y, passenger == null ? 0xFF4F8740 : 0xFF66FF00);
        }
    }

    private static void renderLandVehicleStatus(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        boolean focusedSight = isFocusedVehicleSight(vehicle);
        if (vehicle.vehicleData().defaults().vehicleType() != VehicleType.LAND && !focusedSight) {
            return;
        }

        if (focusedSight) {
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            int addW = (screenWidth / screenHeight) * 48;
            int addH = (screenWidth / screenHeight) * 27;
            preciseBlit(guiGraphics, LAND_FRAME, -addW / 2.0F, -addH / 2.0F, screenWidth + addW, screenHeight + addH, 0.0F, 0.0F, 1920.0F, 1080.0F, 1920.0F, 1080.0F);
            int compassOffset = Mth.floor(128.0F + 64.0F / 45.0F * minecraft.player.getYRot());
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, COMPASS, screenWidth / 2 - 128, 10, compassOffset, 0.0F, 256, 16, 1024, 32);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ROLL_INDICATOR, screenWidth / 2 - 8, 30, 0.0F, 0.0F, 16, 16, 16, 16);
        }

        if (vehicle.vehicleData().defaults().vehicleType() != VehicleType.LAND) {
            return;
        }

        int x = guiGraphics.guiWidth() / 2 + 96;
        int y = guiGraphics.guiHeight() - 72;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LAND_LINE, guiGraphics.guiWidth() / 2 - 64, guiGraphics.guiHeight() - 56, 0.0F, 0.0F, 128, 1, 128, 1);
        blitVehiclePart(guiGraphics, LAND_BODY, x, y, vehicle.vehicleHealth() <= vehicle.maxVehicleHealth() * 0.35F);
        blitVehiclePart(guiGraphics, LAND_LEFT_WHEEL, x, y, vehicle.isLeftWheelDamaged());
        blitVehiclePart(guiGraphics, LAND_RIGHT_WHEEL, x, y, vehicle.isRightWheelDamaged());
        blitVehiclePart(guiGraphics, LAND_ENGINE, x, y, vehicle.isEngineDamaged());
        if (vehicle.isTurretDamaged()) {
            guiGraphics.fill(guiGraphics.guiWidth() / 2 + 112, guiGraphics.guiHeight() - 71, guiGraphics.guiWidth() / 2 + 113, guiGraphics.guiHeight() - 55, 0xFFFF4033);
        }
    }

    private static boolean hasApcWeaponHud(VehicleEntity vehicle) {
        return vehicle.hasFocusedDriverSightHud();
    }

    private static boolean hasWeaponSelectorHud(VehicleEntity vehicle) {
        return vehicle.hasVehicleWeapons() && (hasApcWeaponHud(vehicle) || vehicle.vehicleData().defaults().weapons().size() > 1 || vehicle.vehicleData().defaults().vehicleType() == VehicleType.BOAT);
    }

    private static boolean hasApcFocusedSightFrame(VehicleEntity vehicle) {
        return vehicle.hasFocusedDriverSightHud();
    }

    private static boolean isFocusedVehicleSight(VehicleEntity vehicle) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && player.getVehicle() == vehicle
                && vehicle.hasFocusedSightHud(player)
                && VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
    }

    private static void preciseBlit(GuiGraphicsExtractor guiGraphics, Identifier texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, Math.round(x), Math.round(y), uOffset, vOffset, Math.round(width), Math.round(height), Math.round(textureWidth), Math.round(textureHeight), Math.round(textureWidth), Math.round(textureHeight));
    }

    private static void blitVehiclePart(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, boolean damaged) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
    }
}
