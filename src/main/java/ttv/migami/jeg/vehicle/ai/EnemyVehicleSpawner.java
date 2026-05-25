package ttv.migami.jeg.vehicle.ai;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
    private static final ResourceLocation LAV150 = Reference.id("lav150");
    private static final ResourceLocation BMP2 = Reference.id("bmp2");
    private static final ResourceLocation AH6 = Reference.id("ah6");
    private static final ResourceLocation MI28 = Reference.id("mi28");
    private static final int RAID_VEHICLE_BATCH_WEIGHT = 4;
    private static final int RAID_SPAWN_MIN_DISTANCE = 44;
    private static final int RAID_SPAWN_MAX_DISTANCE = 82;
    private static final int AH6_SPAWN_ALTITUDE = 28;
    private static final int MI28_SPAWN_ALTITUDE = 42;

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

        ResourceLocation vehicleId = pickVehicleId(level, mob.blockPosition(), mob.getRandom());
        BlockPos spawnPos = spawnPosForVehicle(level, mob.blockPosition(), vehicleId);
        return spawnVehicle(level, spawnPos, mob.getYRot(), vehicleId, mob, Vec3.atCenterOf(mob.blockPosition())) != null;
    }

    @Nullable
    public static VehicleEntity trySpawnRaidVehicle(ServerLevel level, BlockPos origin, @Nullable BlockPos burstCenter, @Nullable Player target) {
        if (!canSpawnNaturally(level)) {
            return null;
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos groundPos = randomRaidGroundPos(level, origin, burstCenter, target, level.getRandom(), attempt);
            if (!isOpenSky(level, groundPos)) {
                continue;
            }
            ResourceLocation vehicleId = pickVehicleId(level, groundPos, level.getRandom());
            BlockPos spawnPos = spawnPosForVehicle(level, groundPos, vehicleId);
            if (!isVehicleSpawnClear(level, spawnPos, vehicleId)) {
                continue;
            }
            float yaw = target == null ? level.getRandom().nextFloat() * 360.0F : yawToward(spawnPos, target.position());
            VehicleEntity vehicle = spawnVehicle(level, spawnPos, yaw, vehicleId, null, Vec3.atCenterOf(groundPos));
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
    private static VehicleEntity spawnVehicle(ServerLevel level, BlockPos pos, float yaw, ResourceLocation vehicleId) {
        return spawnVehicle(level, pos, yaw, vehicleId, null, Vec3.atCenterOf(pos));
    }

    @Nullable
    private static VehicleEntity spawnVehicle(ServerLevel level, BlockPos pos, float yaw, ResourceLocation vehicleId, @Nullable PathfinderMob existingCrew, Vec3 anchor) {
        VehicleEntity vehicle = EnemyVehicleSpawnItem.spawnVehicle(level, pos, vehicleType(vehicleId), vehicleId, null, null, false, existingCrew);
        if (vehicle != null) {
            vehicle.setYRot(yaw);
            EnemyVehicleController.setAnchor(vehicle, anchor);
        }
        return vehicle;
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends VehicleEntity> vehicleType(ResourceLocation vehicleId) {
        return (EntityType<? extends VehicleEntity>) switch (vehicleId.getPath()) {
            case "bmp2" -> ModEntities.BMP2.get();
            case "ah6" -> ModEntities.AH6.get();
            case "mi28" -> ModEntities.MI28.get();
            default -> ModEntities.LAV150.get();
        };
    }

    private static ResourceLocation pickVehicleId(ServerLevel level, BlockPos pos, RandomSource random) {
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

    private static BlockPos randomRaidGroundPos(ServerLevel level, BlockPos origin, @Nullable BlockPos burstCenter, @Nullable Player target, RandomSource random, int attempt) {
        if (target != null) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = RAID_SPAWN_MIN_DISTANCE + random.nextInt(RAID_SPAWN_MAX_DISTANCE - RAID_SPAWN_MIN_DISTANCE + 1);
            int x = Mth.floor(target.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(target.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            return new BlockPos(x, y, z);
        }
        BlockPos center = burstCenter != null ? burstCenter : origin;
        return randomGroundPos(level, center, random, 12 + attempt * 3);
    }

    private static BlockPos spawnPosForVehicle(ServerLevel level, BlockPos groundPos, ResourceLocation vehicleId) {
        if (!isAirVehicle(vehicleId)) {
            return groundPos;
        }
        int altitude = "mi28".equals(vehicleId.getPath()) ? MI28_SPAWN_ALTITUDE : AH6_SPAWN_ALTITUDE;
        int y = Mth.clamp(groundPos.getY() + altitude, level.getMinBuildHeight() + 4, level.getMaxBuildHeight() - 8);
        return new BlockPos(groundPos.getX(), y, groundPos.getZ());
    }

    private static boolean isVehicleSpawnClear(ServerLevel level, BlockPos pos, ResourceLocation vehicleId) {
        if (!isAirVehicle(vehicleId)) {
            return isOpenSky(level, pos);
        }
        return level.canSeeSky(pos)
                && level.isEmptyBlock(pos)
                && level.isEmptyBlock(pos.above())
                && level.isEmptyBlock(pos.below());
    }

    private static boolean isAirVehicle(ResourceLocation vehicleId) {
        String path = vehicleId.getPath();
        return "ah6".equals(path) || "mi28".equals(path);
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
