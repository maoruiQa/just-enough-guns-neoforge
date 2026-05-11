package ttv.migami.jeg.vehicle.util;

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
    private static final ResourceLocation MISSILE = Reference.id("missile");
    private static final ResourceLocation LAV150_CANNON_FIRE = Reference.id("vehicle.lav150.cannon_fire");
    private static final ResourceLocation BMP2_CANNON_FIRE = Reference.id("vehicle.bmp2.cannon_fire");
    private static final ResourceLocation MACHINE_GUN_FIRE = Reference.id("item.light_machine_gun.fire");

    private static final Map<ResourceLocation, GunStats> STATS = Map.of(
            Reference.id("vehicle_20mm_cannon"), new GunStats(
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
            ),
            Reference.id("vehicle_30mm_cannon"), new GunStats(
                    Reference.id("vehicle_30mm_cannon"),
                    AUTOCANNON_SHELL,
                    "jeg:magazine",
                    8,
                    60,
                    20,
                    10,
                    78.0F,
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
            ),
            Reference.id("vehicle_coax_machine_gun"), new GunStats(
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
            ),
            Reference.id("vehicle_bmp2_missile"), new GunStats(
                    Reference.id("vehicle_bmp2_missile"),
                    MISSILE,
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
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0.22F,
                    0xFF7744,
                    3.0F
            ),
            Reference.id("rocket_launcher"), new GunStats(
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
                    null,
                    null,
                    null,
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

    private VehicleWeaponStats() {}

    public static GunStats get(ResourceLocation weaponId) {
        GunStats stats = GunDefinitions.ALL.get(weaponId);
        return stats != null ? stats : STATS.get(weaponId);
    }
}
