package ttv.migami.jeg.vehicle.ai;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.item.EnemyVehicleSpawnItem;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class EnemyVehicleSpawner {
    private static final Identifier LAV150 = Reference.id("lav150");
    private static final Identifier BMP2 = Reference.id("bmp2");
    private static final Identifier AH6 = Reference.id("ah6");
    private static final Identifier MI28 = Reference.id("mi28");
    private static final int RAID_VEHICLE_BATCH_WEIGHT = 4;

    private EnemyVehicleSpawner() {}

    public static boolean canSpawnNaturally(ServerLevel level) {
        return Config.enemyVehicleSpawningEnabled()
                && Config.currentGunnerDay(level) >= Config.enemyVehicleStartDay()
                && Config.enemyVehicleConversionChance() > 0.0D;
    }

    public static boolean tryReplaceNaturalGunner(ServerLevel level, PathfinderMob mob) {
        if (!canSpawnNaturally(level)
                || mob.isPassenger()
                || !isOpenSky(level, mob.blockPosition())
                || mob.getRandom().nextDouble() >= Config.enemyVehicleConversionChance()) {
            return false;
        }

        Identifier vehicleId = pickVehicleId(level, mob.blockPosition(), mob.getRandom());
        return spawnVehicle(level, mob.blockPosition(), mob.getYRot(), vehicleId, mob) != null;
    }

    @Nullable
    public static VehicleEntity trySpawnRaidVehicle(ServerLevel level, BlockPos origin, @Nullable BlockPos burstCenter, @Nullable Player target) {
        if (!canSpawnNaturally(level)) {
            return null;
        }

        BlockPos center = burstCenter != null ? burstCenter : origin;
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos pos = randomGroundPos(level, center, level.getRandom(), 8 + attempt * 2);
            if (!isOpenSky(level, pos)) {
                continue;
            }
            Identifier vehicleId = pickVehicleId(level, pos, level.getRandom());
            float yaw = target == null ? level.getRandom().nextFloat() * 360.0F : yawToward(pos, target.position());
            VehicleEntity vehicle = spawnVehicle(level, pos, yaw, vehicleId);
            if (vehicle != null) {
                if (target != null) {
                    EnemyVehicleController.rememberTarget(vehicle, target, 20 * 30);
                }
                return vehicle;
            }
        }
        return null;
    }

    public static int raidVehicleBatchWeight() {
        return RAID_VEHICLE_BATCH_WEIGHT;
    }

    @Nullable
    private static VehicleEntity spawnVehicle(ServerLevel level, BlockPos pos, float yaw, Identifier vehicleId) {
        return spawnVehicle(level, pos, yaw, vehicleId, null);
    }

    @Nullable
    private static VehicleEntity spawnVehicle(ServerLevel level, BlockPos pos, float yaw, Identifier vehicleId, @Nullable PathfinderMob existingCrew) {
        VehicleEntity vehicle = EnemyVehicleSpawnItem.spawnVehicle(level, pos, vehicleType(vehicleId), vehicleId, null, null, false, existingCrew);
        if (vehicle != null) {
            vehicle.setYRot(yaw);
            EnemyVehicleController.setAnchor(vehicle, vehicle.position());
        }
        return vehicle;
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends VehicleEntity> vehicleType(Identifier vehicleId) {
        return (EntityType<? extends VehicleEntity>) switch (vehicleId.getPath()) {
            case "bmp2" -> ModEntities.BMP2.get();
            case "ah6" -> ModEntities.AH6.get();
            case "mi28" -> ModEntities.MI28.get();
            default -> ModEntities.LAV150.get();
        };
    }

    private static Identifier pickVehicleId(ServerLevel level, BlockPos pos, RandomSource random) {
        boolean jungle = isJungle(level, pos);
        int roll = random.nextInt(100);
        if (jungle) {
            return roll < 62 ? AH6 : MI28;
        }
        if (roll < 34) {
            return LAV150;
        }
        if (roll < 64) {
            return BMP2;
        }
        if (roll < 84) {
            return AH6;
        }
        return MI28;
    }

    private static BlockPos randomGroundPos(ServerLevel level, BlockPos center, RandomSource random, int radius) {
        int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
        int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static boolean isOpenSky(ServerLevel level, BlockPos pos) {
        BlockPos skyPos = pos.above();
        return level.canSeeSky(skyPos) && level.isEmptyBlock(pos) && level.isEmptyBlock(skyPos);
    }

    private static boolean isJungle(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(key -> key.toString().contains("jungle")).orElse(false);
    }

    private static float yawToward(BlockPos pos, Vec3 target) {
        double dx = target.x - (pos.getX() + 0.5D);
        double dz = target.z - (pos.getZ() + 0.5D);
        return (float) -Math.toDegrees(Math.atan2(dx, dz));
    }
}
