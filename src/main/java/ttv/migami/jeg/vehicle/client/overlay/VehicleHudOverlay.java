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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.client.util.ScreenProjection;
import ttv.migami.jeg.vehicle.client.VehicleClientState;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleGeometry;
import ttv.migami.jeg.vehicle.projectile.VehicleDecoyEntity;
import ttv.migami.jeg.vehicle.util.VehicleMissileProfile;
import ttv.migami.jeg.vehicle.util.VehicleWeaponStats;
import com.mojang.math.Axis;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;

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
    private static final Identifier CROSSHAIR_RU_APC = Reference.id("textures/overlay/vehicle/crosshair/ru_apc.png");
    private static final Identifier CROSSHAIR_DYNAMIC = Reference.id("textures/overlay/vehicle/crosshair/common_dynamic_cross.png");
    private static final Identifier CROSSHAIR_FIXED_POINT = Reference.id("textures/overlay/vehicle/crosshair/common_fixed_point.png");
    private static final Identifier HELICOPTER_CROSSHAIR = Reference.id("textures/overlay/vehicle/helicopter/crosshair_ind.png");
    private static final Identifier FRAME_GREEN = Reference.id("textures/overlay/frame/frame_green.png");
    private static final Identifier FRAME_TARGET = Reference.id("textures/overlay/frame/frame_target.png");
    private static final Identifier FRAME_TARGET_TRIANGLE = Reference.id("textures/overlay/frame/frame_target_triangle.png");
    private static final Identifier FRAME_LOCK = Reference.id("textures/overlay/frame/frame_lock.png");
    private static final Identifier LOCK_IND_1 = Reference.id("textures/overlay/vehicle/aircraft/locking_ind1.png");
    private static final Identifier LOCK_IND_2 = Reference.id("textures/overlay/vehicle/aircraft/locking_ind2.png");
    private static final Identifier LOCK_IND_3 = Reference.id("textures/overlay/vehicle/aircraft/locking_ind3.png");
    private static final Identifier LOCK_IND_4 = Reference.id("textures/overlay/vehicle/aircraft/locking_ind4.png");
    private static final Identifier WEAPON_ICON_CANNON_20MM = Reference.id("textures/overlay/vehicle/weapon/icons/cannon_20mm.png");
    private static final Identifier WEAPON_ICON_COAX_762 = Reference.id("textures/overlay/vehicle/weapon/icons/gun_7_62mm.png");
    private static final Identifier WEAPON_SELECTED = Reference.id("textures/overlay/vehicle/weapon/frame/selected.png");
    private static final Identifier WEAPON_NUMBER = Reference.id("textures/overlay/vehicle/weapon/frame/number.png");
    private static final int WEAPON_ICON_WIDTH = 75;
    private static final int WEAPON_ICON_HEIGHT = 16;
    private static final int WEAPON_ICON_TEXTURE_WIDTH = 300;
    private static final int WEAPON_ICON_TEXTURE_HEIGHT = 64;
    private static final float HELICOPTER_CRITICAL_DAMAGE_WARNING_HEALTH = 20.0F;
    private static final double HELICOPTER_SAFE_DESCENT_SPEED = 0.35D;
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
        if (!vehicle.usesSwControls() && shouldHidePlayerHudLayer(event.getName().getPath())) {
            event.setCanceled(true);
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
        event.setCanceled(!vehicle.usesSwControls() || vehicle.canPassengerUseSelectedVehicleWeapon(player));
    }

    private static boolean shouldReplaceHotbar(LocalPlayer player, VehicleEntity vehicle) {
        return (player == vehicle.getControllingPassenger() && !vehicle.usesSwControls())
                || vehicle.shouldBanPassengerHand(player)
                || vehicle.canPassengerUseSelectedVehicleWeapon(player);
    }

    private static boolean shouldHidePlayerHudLayer(String path) {
        return "player_health".equals(path)
                || "armor_level".equals(path)
                || "food_level".equals(path)
                || "experience_level".equals(path)
                || "contextual_info_bar".equals(path)
                || "contextual_info_bar_background".equals(path);
    }

    private static void render(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        if (vehicle.usesSwControls()) { renderSwHud(guiGraphics, minecraft, vehicle); return; }




        swAlpha = 1.0F;
        boolean focusedSight = isFocusedVehicleSight(vehicle);
        renderLandVehicleStatus(guiGraphics, minecraft, vehicle);
        if (vehicle.hasVehicleWeapons() && vehicle.canPassengerUseSelectedVehicleWeapon(minecraft.player)) {
            renderReticle(guiGraphics, vehicle);
            renderMissileSeekFrames(guiGraphics, minecraft, vehicle);
        }
        renderPassengerInfo(guiGraphics, minecraft, vehicle);
        renderWeaponSelector(guiGraphics, minecraft, vehicle);
        renderSpeedIndicator(guiGraphics, minecraft, vehicle);
        renderVehicleWarning(guiGraphics, minecraft, vehicle);
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
                    boolean locked = vehicle.hasMissileLock();
                    boolean acquiring = seeking && vehicle.missileSeekTicks() > 0 && !locked;
                    Component lock = locked
                            ? Component.translatable("hud.jeg.vehicle.locked")
                            : acquiring
                            ? Component.translatable("hud.jeg.vehicle.locking")
                            : Component.translatable("hud.jeg.vehicle.seek_prompt", KeyBindings.VEHICLE_SEEK.getTranslatedKeyMessage());
                    int lockColor = locked ? 0xFFFF5555 : acquiring ? 0xFFFFDD88 : 0xFFB8E0FF;
                    guiGraphics.text(minecraft.font, lock, (width - minecraft.font.width(lock)) / 2, lineY, lockColor);
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
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, VALUE_FRAME, x + 11, y + 2, 0, 0, 60, 6, 120, 12, 120, 12);
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
        if (vehicle.usesSwControls()) { renderSwWeaponSelector(guiGraphics,minecraft,vehicle); return; }
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
            swAlpha = index == selected ? 1.0F : 0.35F;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WEAPON_FRAMES[Math.min(displayIndex, WEAPON_FRAMES.length - 1)], x, y, 0.0F, 0.0F, 75, 16, 75, 16);
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
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WEAPON_SELECTED, width - 95, y + 4, 0.0F, 0.0F, 8, 8, 8, 8);
            }
            frameIndex++;
        }
        swAlpha = 1.0F;
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

    private static void renderVehicleWarning(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        if (minecraft.player == null) {
            return;
        }
        String warningKey = vehicle.activeWarningMessageKey();
        if (warningKey.isEmpty() && vehicle.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER) {
            warningKey = helicopterWarningKey(vehicle);
        }
        if (warningKey == null || warningKey.isEmpty()) {
            return;
        }
        Component warning = Component.translatable(warningKey);
        int color = (vehicle.tickCount / 10) % 2 == 0 ? 0xFFFF2222 : 0xFF7F0000;
        int x = (guiGraphics.guiWidth() - minecraft.font.width(warning)) / 2;
        int y = guiGraphics.guiHeight() / 2 + 9;
        guiGraphics.text(minecraft.font, warning, x, y, color);
    }

    private static String helicopterWarningKey(VehicleEntity vehicle) {
        if (vehicle.vehicleHealth() < Math.min(HELICOPTER_CRITICAL_DAMAGE_WARNING_HEALTH, vehicle.maxVehicleHealth())) {
            return "message.jeg.vehicle.helicopter_critical_damage_warning";
        }
        if (vehicle.maxVehicleEnergy() > 0 && vehicle.vehicleEnergy() <= 0) {
            return "message.jeg.vehicle.helicopter_power_loss_warning";
        }
        boolean forcedDescent = !vehicle.onGround() && vehicle.getControllingPassenger() == null;
        if (forcedDescent && Math.abs(vehicle.getDeltaMovement().y) <= HELICOPTER_SAFE_DESCENT_SPEED) {
            return "message.jeg.vehicle.helicopter_low_speed_warning";
        }
        return null;
    }

    private static void blitWeaponIcon(GuiGraphicsExtractor guiGraphics, Identifier icon, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, WEAPON_ICON_WIDTH, WEAPON_ICON_HEIGHT, WEAPON_ICON_TEXTURE_WIDTH, WEAPON_ICON_TEXTURE_HEIGHT, WEAPON_ICON_TEXTURE_WIDTH, WEAPON_ICON_TEXTURE_HEIGHT);
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
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WEAPON_NUMBER, x, y, digit * 20, 0, 5, 8, 20, 30, 300, 30);
    }

    private static void renderReticle(GuiGraphicsExtractor guiGraphics, VehicleEntity vehicle) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;
        if (vehicle.selectedVehicleWeaponId(player) == null || !vehicle.canPassengerUseSelectedVehicleWeapon(player)) return;
        CameraType camera = minecraft.options.getCameraType();
        boolean zooming = VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId() && VehicleClientState.zoomDown();
        boolean helicopterPilot = vehicle.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER
                && vehicle.getSeatIndex(player) == 0;
        // SW's separate helicopter HUD draws a small projected indicator for the pilot.
        if (camera == CameraType.THIRD_PERSON_FRONT && !helicopterPilot && !zooming) return;

        Vec3 screen = null;
        if (camera != CameraType.FIRST_PERSON || helicopterPilot || isDynamicReticle(vehicle, player, zooming)) {
            float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 start = vehicle.vehicleHudShootPos(player, partialTick);
            Vec3 direction = vehicle.vehicleHudShootDirection(player, partialTick).normalize();
            if (direction.lengthSqr() > 1.0E-4D) {
                Vec3 end = start.add(direction.scale(512.0D));
                Vec3 hit = minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)).getLocation();
                double distance = start.distanceToSqr(hit);
                for (Entity target : minecraft.level.getEntities(vehicle, new AABB(start, hit).inflate(8.0D),
                        entity -> entity.isAlive() && !entity.isSpectator() && entity != player
                                && entity.getVehicle() != vehicle && (entity instanceof LivingEntity || entity instanceof VehicleEntity))) {
                    Vec3 point = null;
                    if (target instanceof VehicleEntity other) {
                        if (VehicleGeometry.bounds(other).intersects(new AABB(start, hit).inflate(0.3D))) {
                            VehicleGeometry.Hit vehicleHit = VehicleGeometry.clip(other, start, hit);
                            if (vehicleHit != null) point = vehicleHit.position();
                        }
                    } else {
                        point = target.getBoundingBox().inflate(0.3D).clip(start, hit).orElse(null);
                    }
                    if (point != null && start.distanceToSqr(point) < distance) {
                        hit = point;
                        distance = start.distanceToSqr(point);
                    }
                }
                if (ScreenProjection.canSee(hit)) screen = ScreenProjection.worldToScreen(hit);
            }
        }

        if (vehicle.usesSwControls() && (helicopterPilot || reticleTexture(vehicle) != null)) renderSwFeedback(guiGraphics, vehicle, screen,
                helicopterPilot || camera != CameraType.FIRST_PERSON && !zooming || isDynamicReticle(vehicle, player, zooming));

        if (helicopterPilot) {
            if (screen != null) {
                int size = 16;
                boolean helicopterHudTexture = camera == CameraType.FIRST_PERSON || zooming;
                Identifier texture = helicopterHudTexture ? HELICOPTER_CROSSHAIR : CROSSHAIR_THIRD_CAMERA;
                int textureSize = helicopterHudTexture ? 32 : 64;
                coloredReticleBlit(guiGraphics, texture, (float) screen.x - size / 2.0F, (float) screen.y - size / 2.0F,
                        size, size, 0.0F, 0.0F, textureSize, textureSize, textureSize, textureSize, vehicle.usesSwControls() ? helicopterHudTexture ? swHudColor(vehicle) : 0xFFFFFFFF : reticleColor(vehicle));
                if (vehicle.usesSwControls() && !helicopterHudTexture) {
                    guiGraphics.pose().pushMatrix();
                    guiGraphics.pose().rotateAbout(vehicle.roll()*Mth.DEG_TO_RAD, (float) screen.x, (float) screen.y);
                    guiGraphics.pose().translate((float) screen.x, (float) screen.y);
                    guiGraphics.pose().scale(.75F, .75F);
                    String text = swWeaponText(minecraft, vehicle);
                    if (text != null) swText(guiGraphics, minecraft, text, 30, -9, 0xFFFFFFFF);
                    if (vehicle.hasBuiltInDecoy()) {
                        Component status = Component.translatable(vehicle.vehicleDecoyCooldown() > 0 ? "hud.jeg.vehicle.decoy_reloading" : "hud.jeg.vehicle.decoy_ready");
                        swText(guiGraphics, minecraft, status.getString() + (vehicle.vehicleDecoyCooldown() > 0 ? "" : " [" + KeyBindings.VEHICLE_DEPLOY_DECOY.getTranslatedKeyMessage().getString() + "]"), 30, 1, vehicle.vehicleDecoyCooldown() > 0 ? 0xFFFF0000 : 0xFFFFFFFF);
                    }
                    guiGraphics.pose().popMatrix();
                }
            }
        }
        if (camera == CameraType.THIRD_PERSON_FRONT && !zooming) return;
        if (vehicle.usesSwControls() && reticleTexture(vehicle) == null) return;
        if (camera == CameraType.THIRD_PERSON_BACK && !zooming) {
            if (screen != null) preciseBlit(guiGraphics, CROSSHAIR_THIRD_CAMERA, (float) screen.x - 12.0F,
                    (float) screen.y - 12.0F, 24, 24, 0.0F, 0.0F, 64, 64, 64, 64);
            return;
        }
        Identifier texture = reticleTexture(vehicle);
        if (texture == null) return;
        int size = Math.min(guiGraphics.guiWidth(), guiGraphics.guiHeight());
        float x = (guiGraphics.guiWidth() - size) / 2.0F;
        float y = (guiGraphics.guiHeight() - size) / 2.0F;
        if (isDynamicReticle(vehicle, player, zooming)) {
            if (screen != null) coloredReticleBlit(guiGraphics, texture, (float) screen.x - size / 2.0F,
                    (float) screen.y - size / 2.0F, size, size, 0.0F, 0.0F, 512, 512, 512, 512, reticleColor(vehicle));
            if ("speedboat".equals(vehicle.vehicleDataId().getPath())) texture = CROSSHAIR_FIXED_POINT;
            else return;
        }
        coloredReticleBlit(guiGraphics, texture, x, y, size, size, 0.0F, 0.0F, 512, 512, 512, 512, reticleColor(vehicle));
    }

    private static int feedbackVehicle = -1, feedbackHitUntil, feedbackKillUntil;
    private static boolean feedbackCritical, feedbackOnVehicle;

    public static void clearFeedback() {
        feedbackVehicle = -1;
        feedbackHitUntil = feedbackKillUntil = 0;
    }

    public static boolean recordHit(boolean critical, boolean onVehicle, boolean killed) {
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle) || !vehicle.usesSwControls() || !vehicle.canPassengerUseSelectedVehicleWeapon(player)) return false;
        if (feedbackVehicle != vehicle.getId()) clearFeedback();
        feedbackVehicle = vehicle.getId();
        feedbackHitUntil = player.tickCount + 5;
        feedbackCritical = critical; feedbackOnVehicle = onVehicle;
        if (killed) feedbackKillUntil = player.tickCount + 8;
        return true;
    }

    private static void renderSwFeedback(GuiGraphicsExtractor gui, VehicleEntity vehicle, Vec3 screen, boolean projected) {
        var player = Minecraft.getInstance().player;
        if (player == null || feedbackVehicle != vehicle.getId()) return;
        if (projected && screen == null) return;
        float x = (projected ? (float) screen.x : gui.guiWidth() / 2F) - 7.5F;
        float y = (projected ? (float) screen.y : gui.guiHeight() / 2F) - 7.5F;
        int hit = feedbackHitUntil - player.tickCount, kill = feedbackKillUntil - player.tickCount;
        if (hit > 0 && hit <= 5) {
            float hitX = projected ? x : x + (float)(2*(Math.random()-.5));
            float hitY = projected ? y : y + (float)(2*(Math.random()-.5));
            String file = feedbackOnVehicle ? "hit_marker_vehicle" : "hit_marker";
            preciseBlit(gui, Reference.id("textures/overlay/crosshair/" + file + ".png"), hitX, hitY, 16, 16, 0, 0, 16, 16, 16, 16);
            if (feedbackCritical) preciseBlit(gui, Reference.id("textures/overlay/crosshair/headshot_marker.png"), hitX, hitY, 16, 16, 0, 0, 16, 16, 16, 16);
        }
        if (kill > 0 && kill <= 8) {
            float rate = (40 - kill * 5) / 5.5F, first = -2 + rate, second = 2 - rate;
            for (int i = 0; i < 4; i++) preciseBlit(gui, Reference.id("textures/overlay/crosshair/kill_marker_" + (i+1) + ".png"), x + ((i & 1) == 0 ? first : second), y + (i < 2 ? first : second), 16, 16, 0, 0, 16, 16, 16, 16);
        }
    }

    private static boolean isDynamicReticle(VehicleEntity vehicle, Entity passenger, boolean zooming) {
        if (vehicle.usesSwControls()) {
            var weapon = vehicle.selectedVehicleWeaponInfo(passenger);
            String name = weapon == null ? "@Empty" : zooming ? weapon.crosshairZooming() : weapon.crosshair();
            return "@VehicleCommonGunDynamic".equals(name) || "@VehicleDynamicCross".equals(name);
        }
        if (zooming) return false;
        String vehicleId = vehicle.vehicleDataId().getPath();
        Identifier weaponId = vehicle.selectedVehicleWeaponId(passenger);
        return "speedboat".equals(vehicleId) || "mi28".equals(vehicleId)
                && weaponId != null && "light_machine_gun".equals(weaponId.getPath());
    }

    private static void renderMissileSeekFrames(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, VehicleEntity vehicle) {
        LocalPlayer player = minecraft.player;
        if (player == null || !vehicle.isSelectedVehicleWeaponLockOn(player) || minecraft.level == null) {
            return;
        }
        Identifier weaponId = vehicle.selectedVehicleWeaponId(player);
        if (weaponId == null) {
            return;
        }
        VehicleMissileProfile profile = VehicleMissileProfile.get(weaponId);
        if (!profile.usesLockOn()) {
            return;
        }

        double range = vehicle.missileSeekRange();
        int lockTargetId = vehicle.missileLockTargetId();
        boolean locked = vehicle.hasMissileLock();
        int seekTicks = vehicle.missileSeekTicks();
        int seekTime = Math.max(1, vehicle.missileSeekTime());
        boolean seeking = VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.seekDown();

        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (Entity target : minecraft.level.getEntities(vehicle, vehicle.getBoundingBox().inflate(range),
                entity -> entity instanceof LivingEntity
                        || entity instanceof VehicleEntity
                        || entity instanceof VehicleDecoyEntity)) {
            if (!vehicle.isValidMissileSeekCandidate(player, target, profile)) {
                continue;
            }
            Vec3 center = target instanceof LivingEntity living
                    ? living.getEyePosition(partialTick)
                    : target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 screen = ScreenProjection.worldToScreen(center);
            if (screen == null) {
                screen = ScreenProjection.approximateWorldToScreen(center);
            }
            if (screen == null) {
                continue;
            }
            float x = (float) screen.x;
            float y = (float) screen.y;
            boolean isLockTarget = target.getId() == lockTargetId;
            if (isLockTarget && locked) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FRAME_LOCK, Mth.floor(x) - 12, Mth.floor(y) - 12, 0, 0, 24, 24, 24, 24);
            } else if (isLockTarget && seeking && seekTicks > 0) {
                float lockOffset = Mth.clamp((seekTime - seekTicks) * (20.0F / seekTime), 0.0F, 20.0F);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LOCK_IND_1, Mth.floor(x) - 12, Mth.floor(y - lockOffset) - 12, 0, 0, 24, 24, 24, 24);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LOCK_IND_2, Mth.floor(x) - 12, Mth.floor(y + lockOffset) - 12, 0, 0, 24, 24, 24, 24);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LOCK_IND_3, Mth.floor(x - lockOffset) - 12, Mth.floor(y) - 12, 0, 0, 24, 24, 24, 24);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, LOCK_IND_4, Mth.floor(x + lockOffset) - 12, Mth.floor(y) - 12, 0, 0, 24, 24, 24, 24);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FRAME_TARGET, Mth.floor(x) - 12, Mth.floor(y) - 12, 0, 0, 24, 24, 24, 24);
            } else if (isLockTarget) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FRAME_TARGET_TRIANGLE, Mth.floor(x) - 12, Mth.floor(y) - 12, 0, 0, 24, 24, 24, 24);
            } else {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FRAME_GREEN, Mth.floor(x) - 12, Mth.floor(y) - 12, 0, 0, 24, 24, 24, 24);
            }
        }
    }

    private static Identifier reticleTexture(VehicleEntity vehicle) {
        if (vehicle.usesSwControls()) {
            var weapon = vehicle.selectedVehicleWeaponInfo(Minecraft.getInstance().player);
            String name = weapon == null ? "@Empty" : VehicleClientState.zoomDown() ? weapon.crosshairZooming() : weapon.crosshair();
            return switch (name) {
                case "@VehicleUsApc" -> CROSSHAIR_US_APC;
                case "@VehicleRuApc" -> CROSSHAIR_RU_APC;
                case "@VehicleCommonGun", "@VehicleCommonGunDynamic" -> CROSSHAIR_GUN;
                case "@VehicleCommonMissile" -> CROSSHAIR_MISSILE;
                case "@VehicleCommonSeekMissile" -> CROSSHAIR_SEEK_MISSILE;
                case "@VehicleDynamicCross" -> CROSSHAIR_DYNAMIC;
                case "@VehicleFixedPoint" -> CROSSHAIR_FIXED_POINT;
                default -> null;
            };
        }
        String vehiclePath = vehicle.vehicleDataId().getPath();
        Entity passenger = Minecraft.getInstance().player;
        Identifier selectedWeaponId = passenger == null ? vehicle.selectedVehicleWeaponId() : vehicle.selectedVehicleWeaponId(passenger);
        String weaponPath = selectedWeaponId == null ? "" : selectedWeaponId.getPath();
        boolean zooming = VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
        switch (vehiclePath) {
            case "speedboat": return zooming ? CROSSHAIR_GUN : CROSSHAIR_DYNAMIC;
            case "bmp2":
                if ("vehicle_30mm_cannon".equals(weaponPath)) return CROSSHAIR_RU_APC;
                return "vehicle_bmp2_missile".equals(weaponPath) ? CROSSHAIR_MISSILE : CROSSHAIR_GUN;
            case "lav150": return "vehicle_20mm_cannon".equals(weaponPath) ? CROSSHAIR_US_APC : CROSSHAIR_GUN;
            case "mi28":
                if ("light_machine_gun".equals(weaponPath)) return CROSSHAIR_GUN;
                if ("vehicle_9m336_missile".equals(weaponPath) || zooming && "vehicle_kh39_missile".equals(weaponPath)) return CROSSHAIR_SEEK_MISSILE;
                return CROSSHAIR_MISSILE;
            default: break;
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
            guiGraphics.text(minecraft.font, name, 42, y, 0xFF66FF00);
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

    private static int reticleColor(VehicleEntity vehicle) {
        if (vehicle.usesSwControls()) {
            var weapon = vehicle.selectedVehicleWeaponInfo(Minecraft.getInstance().player);
            return weapon == null ? 0xFFFFFFFF : 0xFF000000 | weapon.crosshairColor();
        }
        return switch (vehicle.vehicleDataId().getPath()) {
            case "bmp2", "mi28" -> 0xFFFFC700;
            case "lav150" -> 0xFF66FF00;
            default -> 0xFFFFFFFF;
        };
    }

    private static void coloredReticleBlit(GuiGraphicsExtractor guiGraphics, Identifier texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight, int color) {
        if (width <= 0 || height <= 0) return;
        int pixelsWide = Math.max(1, Math.round(width)), pixelsHigh = Math.max(1, Math.round(height));
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(width / pixelsWide, height / pixelsHigh);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, uOffset, vOffset, pixelsWide, pixelsHigh, Math.round(uWidth), Math.round(vHeight), Math.round(textureWidth), Math.round(textureHeight), color);
        guiGraphics.pose().popMatrix();
    }

    private static void preciseBlit(GuiGraphicsExtractor guiGraphics, Identifier texture, float x, float y, float width, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        coloredReticleBlit(guiGraphics, texture, x, y, width, height, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight, ((int)(swAlpha*255)<<24)|0xFFFFFF);
    }

    private static void blitVehiclePart(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, boolean damaged) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
    }

    private static float swAlpha = 1;
    private static int animatedVehicle = -1, animatedSeat = -1, animatedWeapon = -1, previousAnimatedWeapon = -1;
    private static long weaponChangedAt;
    private static final float[] slotAnimation = new float[9];
    private static final float[] slotStart = new float[9];
    private static int lastTerrainWarningTick = Integer.MIN_VALUE;
    private static float helicopterScope = .7F, helicopterVerticalSpeed;

    private static void renderSwHud(GuiGraphicsExtractor gui, Minecraft mc, VehicleEntity vehicle) {
        if (mc.gui.hud.isHidden() || mc.player == null || mc.player.isSpectator()) return;
        int w=gui.guiWidth(), h=gui.guiHeight();

        swAlpha = 1;
        float tick=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var weapon=vehicle.selectedVehicleWeaponInfo(mc.player);
        if (weapon != null) {
            if (vehicle.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER) renderSwHelicopter(gui,mc,vehicle,tick);
            else if (("bmp2".equals(vehicle.vehicleDataId().getPath()) || "lav150".equals(vehicle.vehicleDataId().getPath()))
                    && vehicle.getSeatIndex(mc.player)==vehicle.vehicleData().defaults().turret().seatIndex()) renderSwLand(gui,mc,vehicle,tick);
            renderReticle(gui,vehicle);
            renderMissileSeekFrames(gui,mc,vehicle);
        }
        if (vehicle.maxVehicleEnergy() > 0) swValueBar(gui,ENERGY_ICON,h-22,(float)vehicle.vehicleEnergy()/vehicle.maxVehicleEnergy());
        swValueBar(gui,ARMOR_ICON,h-13,vehicle.vehicleHealth()/Math.max(1,vehicle.maxVehicleHealth()));
        if (vehicle.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER && mc.player == vehicle.getControllingPassenger()) {
            String hoverKey = vehicle.hoverMode() ? "gui.jeg.vehicle.hover_on" : vehicle.canHover() ? "gui.jeg.vehicle.hover_off" : "gui.jeg.vehicle.hover_unavailable";
            Component hover = Component.translatable(hoverKey, mc.options.keyJump.getTranslatedKeyMessage());
            swText(gui, mc, hover.getString(), 85, h - 13, vehicle.hoverMode() ? swHudColor(vehicle) : vehicle.canHover() ? 0xFFFFFF : 0xAAAAAA);
        }
        renderPassengerInfo(gui,mc,vehicle);
        renderSwWeaponSelector(gui,mc,vehicle);
        if (!vehicle.activeWarningMessageKey().isEmpty()) {
            Component warning=Component.translatable(vehicle.activeWarningMessageKey());
            gui.text(mc.font,warning,(w-mc.font.width(warning))/2,h/2+24,0xFFFF0000,false);
        }
        swAlpha = 1;
    }

    private static void swValueBar(GuiGraphicsExtractor gui, Identifier icon, int y, float ratio) {
        preciseBlit(gui,icon,10,y,8,8,0,0,8,8,8,8);
        preciseBlit(gui,VALUE_FRAME,20,y+1,60,6,0,0,60,6,60,6);
        int fill=(int)(60*Mth.clamp(ratio,0,1));
        if(fill>0) preciseBlit(gui,VALUE_BAR,20,y+1,fill,6,0,0,fill,6,60,6);
    }

    private static void renderSwWeaponSelector(GuiGraphicsExtractor gui, Minecraft mc, VehicleEntity vehicle) {
        if (!vehicle.shouldBanPassengerHand(mc.player)) return;
        var weapons=vehicle.vehicleData().defaults().weapons();
        var indexes=new java.util.ArrayList<Integer>();
        for(int i=0;i<weapons.size() && indexes.size()<9;i++) if(vehicle.canPassengerUseVehicleWeapon(mc.player,i)) indexes.add(i);
        int selected=indexes.indexOf(vehicle.selectedVehicleWeaponIndex(mc.player));
        if(selected<0) return;
        int seat=vehicle.getSeatIndex(mc.player);
        long now=System.nanoTime();
        if(animatedVehicle!=vehicle.getId() || animatedSeat!=seat) {
            java.util.Arrays.fill(slotAnimation,0);
            animatedVehicle=vehicle.getId(); animatedSeat=seat; animatedWeapon=-1;
        }
        if(animatedWeapon!=selected) {
            System.arraycopy(slotAnimation,0,slotStart,0,9);
            previousAnimatedWeapon=animatedWeapon<0?selected:animatedWeapon;
            animatedWeapon=selected; weaponChangedAt=now;
        }
        float time=Mth.clamp((now-weaponChangedAt)/300_000_000.0F,0,1);
        float ease=(float)Math.sqrt(1-(time-1)*(time-1));
        int w=gui.guiWidth(),h=gui.guiHeight();
        for(int i=indexes.size()-1;i>=0;i--) {
            int row=indexes.size()-1-i,slot=indexes.get(i);
            slotAnimation[i]=Mth.lerp(ease,slotStart[i],i==selected?1:0);
            float offset=37*(1-slotAnimation[i]),x=w-85+offset,y=h-row*18-20;
            swAlpha = Mth.lerp(slotAnimation[i],.2F,1);
            preciseBlit(gui,WEAPON_FRAMES[i],x,y,75,16,0,0,75,16,75,16);
            Identifier icon=weapons.get(slot).icon();
            if(icon!=null) preciseBlit(gui,icon,x,y,75,16,0,0,75,16,75,16);
            if(i==selected) {
                float marker=Mth.lerp(ease,h-(indexes.size()-1-previousAnimatedWeapon)*18-16,h-row*18-16);
                preciseBlit(gui,WEAPON_SELECTED,w-95,marker,8,8,0,0,8,8,8,8);
                renderWeaponNumber(gui,vehicle.selectedVehicleWeaponAmmo(mc.player),(int)(w-20+offset),(int)(y+4.5F));
            }
        }
        swAlpha = 1;
    }

    private static int swHudColor(VehicleEntity vehicle) { return "bmp2".equals(vehicle.vehicleDataId().getPath()) || "mi28".equals(vehicle.vehicleDataId().getPath()) ? 0xFFFFC700 : 0xFF66FF00; }
    private static void swBlit(GuiGraphicsExtractor gui,String file,float x,float y,float width,float height,int color) {
        coloredReticleBlit(gui,Reference.id("textures/overlay/vehicle/"+file+".png"),x,y,width,height,0,0,width,height,width,height,color);
    }
    private static void swCompass(GuiGraphicsExtractor gui,float yaw,int y,int color) {
        coloredReticleBlit(gui,COMPASS,gui.guiWidth()/2F-128,y,256,16,128+64F/45*yaw,0,256,16,512,16,color);
    }
    private static void swText(GuiGraphicsExtractor gui,Minecraft mc,String text,int x,int y,int color) { gui.text(mc.font,text,x,y,color|0xFF000000,false); }
    private static String swNumber(double value,String unit) { return String.format(java.util.Locale.ROOT,"%.0f%s",value,unit); }
    private static void swSightFrame(GuiGraphicsExtractor gui) {
        int w=gui.guiWidth(),h=gui.guiHeight(),addW=w/h*48,addH=w/h*27;
        preciseBlit(gui,LAND_FRAME,-addW/2F,-addH/2F,w+addW,h+addH,0,0,w+addW,h+addH,w+addW,h+addH);
    }
    private static void swRange(GuiGraphicsExtractor gui,Minecraft mc,VehicleEntity vehicle,float tick,int color) {
        Vec3 origin=vehicle.vehicleHudShootPos(mc.player,tick),direction=vehicle.vehicleHudShootDirection(mc.player,tick);
        Vec3 hit=mc.level.clip(new ClipContext(origin,origin.add(direction.scale(512)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,mc.player)).getLocation();
        double distance=origin.distanceTo(hit);
        for(Entity target:mc.level.getEntities(vehicle,new AABB(origin,hit).inflate(8),e->e.isAlive() && e!=mc.player && e.getVehicle()!=vehicle)) {
            Vec3 point=target.getBoundingBox().inflate(.1).clip(origin,hit).orElse(null);
            if(target instanceof VehicleEntity other) {
                VehicleGeometry.Hit vehicleHit=VehicleGeometry.clip(other,origin,hit);
                point=vehicleHit==null?null:vehicleHit.position();
            }
            if(point!=null) distance=Math.min(distance,origin.distanceTo(point));
        }
        String range=distance>500?"---m":swNumber(distance," m");
        swText(gui,mc,range,(gui.guiWidth()-mc.font.width(range))/2,gui.guiHeight()-53,color);
    }
    private static String swWeaponText(Minecraft mc,VehicleEntity vehicle) {
        var id=vehicle.selectedVehicleWeaponId(mc.player);
        if(id==null) return null;
        String text=Component.translatable("item."+id.getNamespace()+"."+id.getPath()).getString()+" "+vehicle.selectedVehicleWeaponAmmo(mc.player);
        if(vehicle.selectedVehicleWeaponReloading(mc.player)) text=Component.translatable("hud.jeg.vehicle.reloading").getString()+" "+swNumber(vehicle.selectedVehicleWeaponReloadTicks(mc.player)/20D,"s");
        else if(canManualReloadPrompt(vehicle)) text+=" ["+KeyBindings.RELOAD.getTranslatedKeyMessage().getString()+"]";
        return text;
    }
    private static void swWeaponReadout(GuiGraphicsExtractor gui,Minecraft mc,VehicleEntity vehicle,int color) {
        int w=gui.guiWidth(),h=gui.guiHeight();
        String text=swWeaponText(mc,vehicle);
        if(text==null) return;
        boolean pilot=vehicle.vehicleData().defaults().vehicleType()==VehicleType.HELICOPTER && (!vehicle.vehicleData().defaults().turret().enabled() || vehicle.getSeatIndex(mc.player)!=vehicle.vehicleData().defaults().turret().seatIndex());
        swText(gui,mc,text,pilot?w/2-160:(w-mc.font.width(text))/2,pilot?h/2-59:h-65,color);
        if(vehicle.maxVehicleEnergy()>0 && vehicle.vehicleEnergy()<vehicle.maxVehicleEnergy()*.2) swText(gui,mc,vehicle.vehicleEnergy()<vehicle.maxVehicleEnergy()*.02?"NO POWER!":"LOW POWER",w/2-144,h/2+14,vehicle.vehicleEnergy()<vehicle.maxVehicleEnergy()*.02?0xFFFF0000:0xFFFF6B00);
    }
    private static void swDecoy(GuiGraphicsExtractor gui,Minecraft mc,VehicleEntity vehicle,int y,int color) {
        if(!vehicle.hasBuiltInDecoy() || mc.player!=vehicle.getControllingPassenger()) return;
        Component status=Component.translatable(vehicle.vehicleDecoyCooldown()>0?"hud.jeg.vehicle.decoy_reloading":"hud.jeg.vehicle.decoy_ready");
        swText(gui,mc,status.getString()+(vehicle.vehicleDecoyCooldown()>0?"":" ["+KeyBindings.VEHICLE_DEPLOY_DECOY.getTranslatedKeyMessage().getString()+"]"),gui.guiWidth()/2-(vehicle.vehicleData().defaults().vehicleType()==VehicleType.HELICOPTER?160:165),y,vehicle.vehicleDecoyCooldown()>0?0xFF0000:color);
    }
    private static int swPartColor(int color,float health) {
        // SW interpolates hue in HSV toward red; these HUD colors have full saturation/value.
        float ratio = ((int) (100 - Mth.clamp(health,0,1) * 100)) / 100F;
        float hue = (color & 0xFFFFFF) == 0xFFC700 ? 199F / 1530F : .26666667F;
        return 0xFF000000 | Mth.hsvToRgb(hue * (1-ratio), 1, 1);
    }
    private static void renderSwLand(GuiGraphicsExtractor gui,Minecraft mc,VehicleEntity vehicle,float tick) {
        if(!mc.options.getCameraType().isFirstPerson() && !VehicleClientState.zoomDown()) return;
        int w=gui.guiWidth(),h=gui.guiHeight(),color=swHudColor(vehicle);
        swSightFrame(gui); swCompass(gui,mc.player.getYRot(),10,color);
        swBlit(gui,"helicopter/roll_ind",w/2F-8,30,16,16,color);
        swBlit(gui,"land/line",w/2F-64,h-56,128,1,color);
        swBlit(gui,"land/line",w/2F+112,h-71,1,16,swPartColor(color,vehicle.partHealthFraction(OBBInfo.Part.TURRET)));
        gui.pose().pushMatrix(); gui.pose().rotateAbout(vehicle.turretYaw(tick)*Mth.DEG_TO_RAD,w/2F+112,h-56);
        swBlit(gui,"land/body",w/2F+96,h-72,32,32,swPartColor(color,vehicle.partHealthFraction(OBBInfo.Part.BODY)));
        swBlit(gui,"land/left_wheel",w/2F+96,h-72,32,32,swPartColor(color,vehicle.partHealthFraction(OBBInfo.Part.WHEEL_LEFT)));
        swBlit(gui,"land/right_wheel",w/2F+96,h-72,32,32,swPartColor(color,vehicle.partHealthFraction(OBBInfo.Part.WHEEL_RIGHT)));
        swBlit(gui,"land/engine",w/2F+96,h-72,32,32,swPartColor(color,vehicle.partHealthFraction(OBBInfo.Part.MAIN_ENGINE))); gui.pose().popMatrix();
        swText(gui,mc,swNumber(vehicle.getDeltaMovement().dot(vehicle.getViewVector(tick))*72," km/h"),w/2+160,h/2-48,color);
        swText(gui,mc,swNumber(vehicle.vehicleHealth()/vehicle.maxVehicleHealth()*100,""),w/2-165,h/2-46,color);
        swRange(gui,mc,vehicle,tick,color); swDecoy(gui,mc,vehicle,h/2-36,color); swWeaponReadout(gui,mc,vehicle,color);
    }
    private static void renderSwHelicopter(GuiGraphicsExtractor gui,Minecraft mc,VehicleEntity vehicle,float tick) {
        int w=gui.guiWidth(),h=gui.guiHeight(),color=swHudColor(vehicle);
        boolean sight=mc.options.getCameraType().isFirstPerson() || VehicleClientState.zoomDown();
        if(vehicle.vehicleData().defaults().turret().enabled() && vehicle.getSeatIndex(mc.player)==vehicle.vehicleData().defaults().turret().seatIndex()) {
            if(!VehicleClientState.zoomDown()) return;
            swSightFrame(gui); swCompass(gui,(float)vehicle.cameraRotationFor(mc.player,tick).x,10,color);
            swBlit(gui,"helicopter/roll_ind",w/2F-8,30,16,16,color); swBlit(gui,"land/line",w/2F-64,h-56,128,1,color);
            swText(gui,mc,swNumber(vehicle.getDeltaMovement().dot(vehicle.getViewVector(tick))*72," km/h"),w/2+160,h/2-48,color);
            swText(gui,mc,swNumber(vehicle.getY()," m"),w/2+160,h/2-39,color);
            swRange(gui,mc,vehicle,tick,color); swWeaponReadout(gui,mc,vehicle,color); return;
        }
        if(vehicle.getSeatIndex(mc.player)!=0) return;
        double speed=vehicle.getDeltaMovement().length()*72;
        helicopterVerticalSpeed=(float)Mth.lerp(.021F*tick,helicopterVerticalSpeed,vehicle.getDeltaMovement().y*20);
        if(sight) {
            helicopterScope=Mth.lerp(tick,helicopterScope,1);
            float size=Mth.floor(Math.min(w,h)*helicopterScope),x=(w-size)/2F,y=(h-size)/2F;
            swBlit(gui,"helicopter/heli_base",x,y,size,size,color);
            swBlit(gui,"helicopter/heli_driver_angle",x-vehicle.turretYaw(tick)*.3F,y+(vehicle.turretPitch(tick)-vehicle.getXRot())*.072F,size,size,color);
            swCompass(gui,vehicle.getYRot(),6,color);
            gui.pose().pushMatrix();gui.pose().rotateAbout(-vehicle.roll(tick)*Mth.DEG_TO_RAD,w/2F,h/2F);
            swBlit(gui,"helicopter/heli_line",w/2F-128,h/2F-512-5.475F*Mth.lerp(tick,vehicle.xRotO,vehicle.getXRot()),256,1024,color);gui.pose().popMatrix();
            gui.pose().pushMatrix();gui.pose().rotateAbout(vehicle.roll(tick)*Mth.DEG_TO_RAD,w/2F,h/2F-56);
            swBlit(gui,"helicopter/roll_ind",w/2F-8,h/2F-88,16,16,color);gui.pose().popMatrix();
            swBlit(gui,"helicopter/heli_power_ruler",w/2F+100,h/2F-64,64,128,color);
            float power=(float)vehicle.enginePower()*980;
            if(power>0) swBlit(gui,"helicopter/heli_power",w/2F+130,h/2F+60-power,4,power,color);
            float vyY=h/2F-3-Math.max(helicopterVerticalSpeed,-24)*2.5F;
            swBlit(gui,"helicopter/heli_vy_move",w/2F+138,vyY,8,8,color);
            swText(gui,mc,swNumber(helicopterVerticalSpeed,"m/s"),w/2+146,(int)vyY,helicopterVerticalSpeed<-24?0xFF0000:color);
            swText(gui,mc,swNumber(vehicle.getY(),""),w/2+104,h/2,color);
            swBlit(gui,"helicopter/speed_frame",w/2F-144,h/2F-6,50,18,color);
            swText(gui,mc,swNumber(speed,"km/h"),w/2-140,h/2,color);
            swDecoy(gui,mc,vehicle,h/2-50,color);swWeaponReadout(gui,mc,vehicle,color);
        }
        Vec3 ground=mc.level.clip(new ClipContext(vehicle.position(),vehicle.position().add(0,-100,0),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,vehicle)).getLocation();
        Vec3 ahead=mc.level.clip(new ClipContext(vehicle.position(),vehicle.position().add(vehicle.getDeltaMovement().add(0,.06,0).normalize().scale(100)),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,vehicle)).getLocation();
        String warning=helicopterVerticalSpeed<-16?"SINK RATE, PULL UP!":((helicopterVerticalSpeed<-10 || helicopterVerticalSpeed<-3 && speed>100) && vehicle.position().distanceTo(ground)<36 || speed>72 && vehicle.position().distanceTo(ahead)<72)?"TERRAIN TERRAIN":"";
        if(!warning.isEmpty()) swText(gui,mc,warning,(w-mc.font.width(warning))/2,h/2+24,0xFF0000);
        if(!warning.isEmpty() && mc.player.tickCount % 30 == 0 && lastTerrainWarningTick != mc.player.tickCount) {
            lastTerrainWarningTick = mc.player.tickCount;
            var sound = ttv.migami.jeg.init.ModSounds.ALL.get(Reference.id(helicopterVerticalSpeed < -16 ? "vehicle.pull_up" : "vehicle.terrain"));
            if(sound != null) mc.player.playSound(sound.get(), 3, 1);
        }
    }
    public static void renderArmorValueBar(GuiGraphicsExtractor guiGraphics, int x, int y, float ratio) {
        renderValueBar(guiGraphics, ARMOR_ICON, x, y, ratio);
    }
}
