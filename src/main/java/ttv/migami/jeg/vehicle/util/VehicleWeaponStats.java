package ttv.migami.jeg.vehicle.util;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;

public final class VehicleWeaponStats {
    private static final ResourceLocation SMALL_SHELL = Reference.id("small_shell");
    private static final ResourceLocation AUTOCANNON_SHELL = Reference.id("autocannon_shell");
    private static final ResourceLocation RIFLE_AMMO = Reference.id("rifle_ammo");
    private static final ResourceLocation ROCKET = Reference.id("rocket");
    private static final ResourceLocation SMALL_ROCKET = Reference.id("small_rocket");
    private static final ResourceLocation MEDIUM_ANTI_AIR_MISSILE = Reference.id("medium_anti_air_missile");
    private static final ResourceLocation MEDIUM_ANTI_GROUND_MISSILE = Reference.id("medium_anti_ground_missile");
    private static final ResourceLocation LARGE_ANTI_GROUND_MISSILE = Reference.id("large_anti_ground_missile");
    private static final ResourceLocation ROCKET_FIRE = Reference.id("item.rocket_launcher.fire");
    private static final ResourceLocation ROCKET_RELOAD_START = Reference.id("item.rocket_launcher.lid_open");
    private static final ResourceLocation ROCKET_RELOAD_LOAD = Reference.id("item.rocket_launcher.rocket_in");
    private static final ResourceLocation ROCKET_RELOAD_END = Reference.id("item.rocket_launcher.lid_close");
    private static final ResourceLocation LAV150_CANNON_FIRE = Reference.id("vehicle.lav150.cannon_fire");
    private static final ResourceLocation BMP2_CANNON_FIRE = Reference.id("vehicle.bmp2.cannon_fire");
    private static final ResourceLocation BMP2_MISSILE_FIRE = Reference.id("vehicle.bmp2.missile_fire");
    private static final ResourceLocation MACHINE_GUN_FIRE = Reference.id("item.light_machine_gun.fire");

    private static final Map<ResourceLocation, GunStats> STATS = createStats();

    private static Map<ResourceLocation, GunStats> createStats() {
        Map<ResourceLocation, GunStats> stats = new LinkedHashMap<>();
        stats.put(
                Reference.id("vehicle_20mm_cannon"),
                new GunStats(
                        Reference.id("vehicle_20mm_cannon"),
                        SMALL_SHELL,
                        "jeg:magazine",
                        12,
                        50,
                        10,
                        4,
                        45.0F,
                        20.0F,
                        60,
                        true,
                        false,
                        0.5F,
                        1,
                        LAV150_CANNON_FIRE,
                        null,
                        LAV150_CANNON_FIRE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0.12F,
                        0x66FF00,
                        2.0F
                )
        );
        stats.put(
                Reference.id("vehicle_30mm_cannon"),
                new GunStats(
                        Reference.id("vehicle_30mm_cannon"),
                        AUTOCANNON_SHELL,
                        "jeg:magazine",
                        8,
                        60,
                        20,
                        15,
                        60.0F,
                        24.0F,
                        90,
                        true,
                        false,
                        0.35F,
                        1,
                        BMP2_CANNON_FIRE,
                        null,
                        BMP2_CANNON_FIRE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0.16F,
                        0xFFC700,
                        2.8F
                )
        );
        stats.put(
                Reference.id("vehicle_coax_machine_gun"),
                new GunStats(
                        Reference.id("vehicle_coax_machine_gun"),
                        RIFLE_AMMO,
                        "jeg:magazine",
                        40,
                        30,
                        10,
                        2,
                        9.5F,
                        30.0F,
                        50,
                        false,
                        false,
                        0.5F,
                        1,
                        MACHINE_GUN_FIRE,
                        null,
                        MACHINE_GUN_FIRE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        GunStats.STANDARD_BULLET_PROJECTILE_SIZE,
                        0x66FF00,
                        1.5F
                )
        );
        stats.put(
                Reference.id("vehicle_bmp2_missile"),
                new GunStats(
                        Reference.id("vehicle_bmp2_missile"),
                        MEDIUM_ANTI_GROUND_MISSILE,
                        "jeg:magazine",
                        1,
                        120,
                        0,
                        100,
                        600.0F,
                        0.95F,
                        120,
                        false,
                        true,
                        0.0F,
                        1,
                        BMP2_MISSILE_FIRE,
                        null,
                        BMP2_MISSILE_FIRE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0.22F,
                        0xFF7744,
                        3.0F
                )
        );
        stats.put(
                Reference.id("rocket_launcher"),
                new GunStats(
                        Reference.id("rocket_launcher"),
                        ROCKET,
                        "jeg:magazine",
                        1,
                        80,
                        20,
                        18,
                        140.0F,
                        0.9F,
                        120,
                        false,
                        true,
                        0.0F,
                        1,
                        ROCKET_FIRE,
                        null,
                        ROCKET_FIRE,
                        ROCKET_RELOAD_START,
                        ROCKET_RELOAD_LOAD,
                        ROCKET_RELOAD_END,
                        null,
                        null,
                        0.22F,
                        0xFF7744,
                        3.0F
                )
        );
        stats.put(
                Reference.id("vehicle_70mm_rocket"),
                new GunStats(
                        Reference.id("vehicle_70mm_rocket"),
                        SMALL_ROCKET,
                        "jeg:magazine",
                        14,
                        100,
                        0,
                        3,
                        80.0F,
                        16.0F,
                        80,
                        true,
                        true,
                        0.25F,
                        1,
                        ROCKET_FIRE,
                        null,
                        ROCKET_FIRE,
                        ROCKET_RELOAD_START,
                        ROCKET_RELOAD_LOAD,
                        ROCKET_RELOAD_END,
                        null,
                        null,
                        0.16F,
                        0xFF7744,
                        2.0F
                )
        );
        stats.put(
                Reference.id("vehicle_80mm_rocket"),
                new GunStats(
                        Reference.id("vehicle_80mm_rocket"),
                        SMALL_ROCKET,
                        "jeg:magazine",
                        42,
                        200,
                        0,
                        3,
                        85.0F,
                        16.0F,
                        80,
                        true,
                        true,
                        0.25F,
                        1,
                        ROCKET_FIRE,
                        null,
                        ROCKET_FIRE,
                        ROCKET_RELOAD_START,
                        ROCKET_RELOAD_LOAD,
                        ROCKET_RELOAD_END,
                        null,
                        null,
                        0.16F,
                        0xFF7744,
                        2.0F
                )
        );
        stats.put(
                Reference.id("vehicle_9m120_driver_missile"),
                vehicleMissileStats(Reference.id("vehicle_9m120_driver_missile"), MEDIUM_ANTI_GROUND_MISSILE, 4, 240, 100, 650.0F)
        );
        stats.put(
                Reference.id("vehicle_9m120_passenger_missile"),
                vehicleMissileStats(Reference.id("vehicle_9m120_passenger_missile"), MEDIUM_ANTI_GROUND_MISSILE, 8, 240, 100, 650.0F)
        );
        stats.put(
                Reference.id("vehicle_kh39_missile"),
                vehicleMissileStats(Reference.id("vehicle_kh39_missile"), LARGE_ANTI_GROUND_MISSILE, 2, 260, 120, 1100.0F)
        );
        stats.put(
                Reference.id("vehicle_9m336_missile"),
                vehicleMissileStats(Reference.id("vehicle_9m336_missile"), MEDIUM_ANTI_AIR_MISSILE, 4, 260, 120, 260.0F)
        );
        return Map.copyOf(stats);
    }

    private static GunStats vehicleMissileStats(ResourceLocation id, ResourceLocation ammo, int magazineSize, int reloadTime, int fireDelay, float damage) {
        return new GunStats(
                id,
                ammo,
                "jeg:magazine",
                magazineSize,
                reloadTime,
                0,
                fireDelay,
                damage,
                1.0F,
                160,
                false,
                true,
                0.0F,
                1,
                ROCKET_FIRE,
                null,
                ROCKET_FIRE,
                ROCKET_RELOAD_START,
                ROCKET_RELOAD_LOAD,
                ROCKET_RELOAD_END,
                null,
                null,
                0.22F,
                0xFF7744,
                3.0F
        );
    }

    private VehicleWeaponStats() {}

    public static GunStats get(ResourceLocation weaponId) {
        GunStats stats = GunDefinitions.ALL.get(weaponId);
        return stats != null ? stats : STATS.get(weaponId);
    }
}
