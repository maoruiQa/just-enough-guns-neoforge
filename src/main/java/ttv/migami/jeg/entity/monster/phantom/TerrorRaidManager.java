package ttv.migami.jeg.entity.monster.phantom;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.util.LootUtils;

/**
 * Utility that emulates the legacy Terror Phantom post-death raid event.
 */
final class TerrorRaidManager {
    private static final ResourceKey<LootTable> SUPPLY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Reference.id("chests/terror_phantom_supply"));
    private static final ResourceKey<LootTable> REWARD_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Reference.id("chests/terror_phantom_reward"));
    private static final UniformInt SKELETON_WAVE_SIZE = UniformInt.of(3, 5);
    private static final UniformInt PHANTOM_WAVE_SIZE = UniformInt.of(2, 3);
    private static final int[] WAVE_DELAYS = new int[] {0, 120, 240};

    private TerrorRaidManager() {}

    static void triggerGroundRaid(ServerLevel level, BlockPos origin) {
        broadcast(level, Component.translatable("message.jeg.terror_raid.begin"));
        BlockPos raidOrigin = origin.immutable();
        spawnLootCrates(level, raidOrigin);
        spawnFlareBurst(level, raidOrigin, false);
        awardCelebrationXp(level, raidOrigin);
        for (int delay : WAVE_DELAYS) {
            TerrorRaidScheduler.schedule(level, delay, () -> spawnSkeletonWave(level, raidOrigin));
        }
    }

    static void triggerAirRaid(ServerLevel level, BlockPos origin) {
        BlockPos raidOrigin = origin.immutable();
        broadcast(level, Component.translatable("message.jeg.terror_raid.guardian"));
        spawnFlareBurst(level, raidOrigin, true);
        awardCelebrationXp(level, raidOrigin);
        for (int delay : WAVE_DELAYS) {
            TerrorRaidScheduler.schedule(level, delay, () -> spawnPhantomWave(level, raidOrigin));
        }
    }

    static void triggerGuardianAftermath(ServerLevel level, BlockPos origin) {
        broadcast(level, Component.translatable("message.jeg.terror_raid.guardian"));
        spawnPhantomWave(level, origin);
        for (int delay : WAVE_DELAYS) {
            TerrorRaidScheduler.schedule(level, delay + 40, () -> spawnPhantomWave(level, origin));
        }
    }

    private static void broadcast(ServerLevel level, MutableComponent message) {
        level.getServer().getPlayerList().broadcastSystemMessage(message, false);
        level.playSound(null, level.getSharedSpawnPos(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(), SoundSource.HOSTILE, 4.0F, 0.9F);
    }

    private static void spawnLootCrates(ServerLevel level, BlockPos origin) {
        placeBarrelWithLoot(level, findTerrain(level, origin), SUPPLY_LOOT);
        placeBarrelWithLoot(level, findTerrain(level, origin.offset(2, 0, 2)), REWARD_LOOT);
    }

    private static BlockPos findTerrain(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        while (cursor.getY() > level.getMinY() && level.isEmptyBlock(cursor)) {
            cursor.move(0, -1, 0);
        }
        if (!level.getBlockState(cursor).isAir()) {
            cursor.move(0, 1, 0);
        }
        return cursor.immutable();
    }

    private static void placeBarrelWithLoot(ServerLevel level, BlockPos pos, ResourceKey<LootTable> lootTable) {
        level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
        LootUtils.fillContainer(level, pos, lootTable, level.getRandom());
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity randomizable) {
            ResourceKey<LootTable> assigned = randomizable.getLootTable();
            String lootId = assigned != null ? assigned.location().toString() : "none";
            ttv.migami.jeg.JustEnoughGuns.LOGGER.debug("[TerrorRaid] Barrel {} lootTable={} seed={}", pos, lootId, randomizable.getLootTableSeed());
        } else if (blockEntity instanceof net.minecraft.world.Container container) {
            boolean empty = LootUtils.isContainerEmpty(container);
            ttv.migami.jeg.JustEnoughGuns.LOGGER.debug("[TerrorRaid] Barrel {} filled empty={}", pos, empty);
        } else {
            ttv.migami.jeg.JustEnoughGuns.LOGGER.debug("[TerrorRaid] Barrel {} has no container block entity", pos);
        }
    }

    private static void spawnSkeletonWave(ServerLevel level, BlockPos origin) {
        RandomSource random = level.getRandom();
        int mobCount = SKELETON_WAVE_SIZE.sample(random);
        for (int i = 0; i < mobCount; i++) {
            BlockPos spawnPos = sampleGroundPosition(level, origin, random, 6, 12);
            Skeleton skeleton = new Skeleton(net.minecraft.world.entity.EntityType.SKELETON, level);
            skeleton.addTag(GunEvents.SKELETON_GUNNER_TAG);
            skeleton.setPersistenceRequired();
            skeleton.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
            skeleton.setYRot(random.nextFloat() * 360.0F);
            skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.EVENT, (SpawnGroupData) null);
            GunEvents.equipSkeletonWithGun(skeleton, random);
            AbstractTerrorPhantom.prepareSkeletonForDaylight(skeleton);
            assignInitialTarget(level, skeleton, spawnPos, 16.0D);
            level.addFreshEntity(skeleton);
        }
    }

    private static void spawnPhantomWave(ServerLevel level, BlockPos origin) {
        RandomSource random = level.getRandom();
        int mobCount = PHANTOM_WAVE_SIZE.sample(random);
        for (int i = 0; i < mobCount; i++) {
            Vec3 center = sampleAirPosition(level, origin, random, 10, 18, 8, 14);
            PhantomGunner gunner = new PhantomGunner(ModEntities.PHANTOM_GUNNER.get(), level);
            Vec3 spawn = center.add(0.5D, 0.0D, 0.5D);
            gunner.setPos(spawn.x, spawn.y, spawn.z);
            gunner.setYRot(random.nextFloat() * 360.0F);
            gunner.setXRot(-10.0F);
            gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spawn)), EntitySpawnReason.EVENT, (SpawnGroupData) null);
            assignInitialTarget(level, gunner, BlockPos.containing(spawn), 64.0D);
            level.addFreshEntity(gunner);
        }
    }

    private static void assignInitialTarget(ServerLevel level, net.minecraft.world.entity.Mob mob, BlockPos origin, double range) {
        Player target = level.getNearestEntity(Player.class, TargetingConditions.forCombat().range(range), null, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, new AABB(origin).inflate(range));
        if (target != null) {
            mob.setTarget(target);
        }
    }

    private static BlockPos sampleGroundPosition(ServerLevel level, BlockPos origin, RandomSource random, int minRadius, int maxRadius) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double radius = Mth.nextDouble(random, minRadius, maxRadius);
        int x = origin.getX() + Mth.floor(Math.cos(angle) * radius);
        int z = origin.getZ() + Mth.floor(Math.sin(angle) * radius);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static Vec3 sampleAirPosition(ServerLevel level, BlockPos origin, RandomSource random, int minRadius, int maxRadius, int minHeight, int maxHeight) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double radius = Mth.nextDouble(random, minRadius, maxRadius);
        double yOffset = Mth.nextDouble(random, minHeight, maxHeight);
        double x = origin.getX() + 0.5D + Math.cos(angle) * radius;
        double z = origin.getZ() + 0.5D + Math.sin(angle) * radius;
        double y = origin.getY() + yOffset;
        return new Vec3(x, y, z);
    }

    private static void spawnFlareBurst(ServerLevel level, BlockPos origin, boolean terror) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 40; i++) {
            double speedX = (random.nextDouble() - 0.5D) * 0.3D;
            double speedY = random.nextDouble() * 0.2D;
            double speedZ = (random.nextDouble() - 0.5D) * 0.3D;
            level.sendParticles(terror ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, origin.getX() + 0.5D, origin.getY() + 1.5D, origin.getZ() + 0.5D, 1, speedX, speedY, speedZ, 0.02D);
        }
        level.playSound(null, origin, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.HOSTILE, 3.0F, terror ? 0.6F : 1.0F);
    }

    private static void awardCelebrationXp(ServerLevel level, BlockPos origin) {
        for (int i = 0; i < 8; i++) {
            ExperienceOrb.award(level, Vec3.atCenterOf(origin).add(0.0D, 1.0D, 0.0D), 25);
        }
    }
}
