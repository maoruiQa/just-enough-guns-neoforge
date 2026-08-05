package ttv.migami.jeg.vehicle.util;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

/**
 * Guided missile combat profile.
 * <p>
 * Damage model mirrors SuperbWarfare guns/vehicles:
 * {@code directHitDamage} = SW {@code Damage},
 * {@code explosionDamage} = SW {@code ExplosionDamage} (unified for all entities),
 * {@code blastRadius} = SW {@code ExplosionRadius}.
 */
public record VehicleMissileProfile(
        GuidanceMode guidanceMode,
        TargetMode targetMode,
        int lifeTicks,
        double maxSpeed,
        double turnRate,
        float explosionPower,
        double blastRadius,
        float explosionDamage,
        float directHitDamage,
        /** Ballistic armor piercing for vehicle direct hits (same system as rockets/guns). */
        float armorPiercing
) {
    /** Ground missiles may only lock living entities at or above this max health. Air missiles ignore this. */
    private static final float MIN_GROUND_MISSILE_LIVING_MAX_HEALTH = 55.0F;

    private static final VehicleMissileProfile DEFAULT = new VehicleMissileProfile(
            GuidanceMode.LOCK_ON,
            TargetMode.ANY_LIVING,
            120,
            0.95D,
            0.16D,
            2.4F,
            3.0D,
            18.0F,
            18.0F,
            10.0F
    );

    private static final Map<Identifier, VehicleMissileProfile> PROFILES = Map.of(
            // SW BMP-2 / TOW: Damage 600, ExplosionDamage 60, Radius 6
            Reference.id("vehicle_bmp2_missile"), new VehicleMissileProfile(
                    GuidanceMode.WIRE_GUIDED,
                    TargetMode.GROUND_ENTITY,
                    120,
                    0.95D,
                    0.16D,
                    3.0F,
                    6.0D,
                    60.0F,
                    600.0F,
                    12.0F
            ),
            // SW Mi-28 9M120: Damage 650, ExplosionDamage 80, Radius 7
            Reference.id("vehicle_9m120_driver_missile"), new VehicleMissileProfile(
                    GuidanceMode.WIRE_GUIDED,
                    TargetMode.GROUND_ENTITY,
                    140,
                    1.05D,
                    0.14D,
                    3.5F,
                    7.0D,
                    80.0F,
                    650.0F,
                    13.0F
            ),
            Reference.id("vehicle_9m120_passenger_missile"), new VehicleMissileProfile(
                    GuidanceMode.WIRE_GUIDED,
                    TargetMode.GROUND_ENTITY,
                    140,
                    1.05D,
                    0.14D,
                    3.5F,
                    7.0D,
                    80.0F,
                    650.0F,
                    13.0F
            ),
            // SW KH-39: Damage 1100, ExplosionDamage 180, Radius 12
            Reference.id("vehicle_kh39_missile"), new VehicleMissileProfile(
                    GuidanceMode.LOCK_ON,
                    TargetMode.GROUND_ENTITY,
                    180,
                    1.10D,
                    0.12D,
                    5.5F,
                    12.0D,
                    180.0F,
                    1100.0F,
                    15.0F
            ),
            // SW 9M336 AA: Damage 260, ExplosionDamage 90, Radius 6
            Reference.id("vehicle_9m336_missile"), new VehicleMissileProfile(
                    GuidanceMode.LOCK_ON,
                    TargetMode.AIR_ENTITY,
                    160,
                    1.25D,
                    0.18D,
                    3.2F,
                    6.0D,
                    90.0F,
                    260.0F,
                    11.0F
            ),
            // SW Javelin: Damage 500, ExplosionDamage 120, Radius 9
            Reference.id("javelin"), new VehicleMissileProfile(
                    GuidanceMode.LOCK_ON,
                    TargetMode.GROUND_ENTITY,
                    240,
                    1.35D,
                    0.16D,
                    4.2F,
                    9.0D,
                    120.0F,
                    500.0F,
                    14.0F
            ),
            // SW Igla: Damage 260, ExplosionDamage 90, Radius 6
            Reference.id("igla_9k38"), new VehicleMissileProfile(
                    GuidanceMode.LOCK_ON,
                    TargetMode.AIR_ENTITY,
                    220,
                    1.55D,
                    0.20D,
                    3.4F,
                    6.0D,
                    90.0F,
                    260.0F,
                    11.0F
            )
    );

    public static VehicleMissileProfile get(Identifier weaponId) {
        return PROFILES.getOrDefault(weaponId, DEFAULT);
    }

    public boolean usesLockOn() {
        return this.guidanceMode == GuidanceMode.LOCK_ON;
    }

    public boolean usesWireGuidance() {
        return this.guidanceMode == GuidanceMode.WIRE_GUIDED;
    }

    public boolean canLock(Entity candidate, LivingEntity shooter, VehicleEntity shooterVehicle) {
        if (!candidate.isAlive() || candidate == shooter || candidate == shooterVehicle
                || shooterVehicle != null && candidate.getVehicle() == shooterVehicle) {
            return false;
        }
        return this.targetMode.canLock(candidate);
    }

    public boolean canContinueTracking(Entity target, Entity owner, Entity ownerVehicle) {
        if (!target.isAlive() || target == owner || target == ownerVehicle
                || ownerVehicle != null && target.getVehicle() == ownerVehicle) {
            return false;
        }
        return this.targetMode.canLock(target);
    }

    public enum GuidanceMode {
        DIRECT,
        WIRE_GUIDED,
        LOCK_ON
    }

    public enum TargetMode {
        ANY_LIVING {
            @Override
            boolean canLock(Entity target) {
                return target instanceof LivingEntity;
            }
        },
        SURFACE_VEHICLE {
            @Override
            boolean canLock(Entity target) {
                return target instanceof VehicleEntity vehicle && isSurfaceVehicle(vehicle);
            }
        },
        GROUND_ENTITY {
            @Override
            boolean canLock(Entity target) {
                if (target instanceof VehicleEntity vehicle) {
                    return isGroundVehicle(vehicle);
                }
                return target instanceof LivingEntity living
                        && isGroundedEntity(target)
                        && living.getMaxHealth() >= MIN_GROUND_MISSILE_LIVING_MAX_HEALTH;
            }
        },
        AIR_ENTITY {
            @Override
            boolean canLock(Entity target) {
                if (target instanceof VehicleEntity vehicle) {
                    return isAirVehicle(vehicle);
                }
                if (isExplicitAntiAirTarget(target)) {
                    return true;
                }
                return target instanceof LivingEntity && !isGroundedEntity(target);
            }
        };

        abstract boolean canLock(Entity target);
    }

    private static boolean isGroundedEntity(Entity target) {
        return target.onGround() || target.isInWater();
    }

    private static boolean isExplicitAntiAirTarget(Entity target) {
        return target instanceof AbstractTerrorPhantom
                || target instanceof EnderDragon
                || target instanceof WitherBoss;
    }

    private static boolean isSurfaceVehicle(VehicleEntity vehicle) {
        VehicleType type = vehicle.vehicleData().defaults().vehicleType();
        return type == VehicleType.LAND || type == VehicleType.BOAT || type == VehicleType.ARTILLERY;
    }

    private static boolean isGroundVehicle(VehicleEntity vehicle) {
        VehicleType type = vehicle.vehicleData().defaults().vehicleType();
        return type == VehicleType.LAND || type == VehicleType.BOAT || type == VehicleType.ARTILLERY;
    }

    private static boolean isAirVehicle(VehicleEntity vehicle) {
        VehicleType type = vehicle.vehicleData().defaults().vehicleType();
        return type == VehicleType.HELICOPTER || type == VehicleType.AIRCRAFT;
    }
}
