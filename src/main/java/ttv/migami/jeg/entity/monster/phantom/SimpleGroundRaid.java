package ttv.migami.jeg.entity.monster.phantom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.faction.GunnerArmorEquiper;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple ground raid system that spawns gunners directly without complex scheduling
 */
public class SimpleGroundRaid {

    public static void triggerGroundRaid(ServerLevel level, BlockPos origin, LivingEntity attacker) {
        JustEnoughGuns.LOGGER.info("[SIMPLE_GROUND_RAID] Triggering ground raid at {}", origin);

        // Broadcast raid message
        Component message = Component.translatable("message.jeg.terror_raid.begin");
        level.getServer().getPlayerList().broadcastSystemMessage(message, false);

        // Play raid sound
        level.playSound(null, origin, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 0.8F);

        // Spawn raid waves directly
        spawnRaidWaves(level, origin, attacker);
    }

    private static void spawnRaidWaves(ServerLevel level, BlockPos origin, LivingEntity attacker) {
        RandomSource random = level.getRandom();

        // Find nearest player as target
        Player target = level.getNearestPlayer(origin.getX(), origin.getY(), origin.getZ(), 100.0D, false);
        if (target == null) {
            JustEnoughGuns.LOGGER.warn("[SIMPLE_GROUND_RAID] No player found for raid targeting");
            return;
        }

        JustEnoughGuns.LOGGER.info("[SIMPLE_GROUND_RAID] Target player: {} at {}", target.getName().getString(), target.blockPosition());

        // Spawn 3 waves with delays
        spawnWave(level, origin, target, 0, random);
        JustEnoughGuns.LOGGER.info("[SIMPLE_GROUND_RAID] Delaying wave 2 for 120 ticks");
        scheduleWave(level, origin, target, 120, random, 1);
        scheduleWave(level, origin, target, 240, random, 2);
    }

    private static void spawnWave(ServerLevel level, BlockPos origin, Player target, int waveNumber, RandomSource random) {
        int mobCount = 18 + random.nextInt(5); // 18-22 mobs per wave

        JustEnoughGuns.LOGGER.info("[SIMPLE_GROUND_RAID] Spawning wave {} with {} mobs", waveNumber, mobCount);

        for (int i = 0; i < mobCount; i++) {
            try {
                // Find spawn position around player
                BlockPos spawnPos = findSpawnPosition(level, target.blockPosition(), random);
                if (spawnPos == null) {
                    JustEnoughGuns.LOGGER.warn("[SIMPLE_GROUND_RAID] Could not find spawn position for mob {}", i);
                    continue;
                }

                // Create mob based on wave
                Monster mob = createMobForWave(level, waveNumber, random);
                if (mob == null) {
                    continue;
                }

                // Position and initialize
                mob.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

                // Make it a gunner
                mob.addTag(GunEvents.JEG_GUNNER_TAG);

                // Equip armor
                GunnerArmorEquiper.equipGunnerArmor(random, GunnerArmorEquiper.GunnerArmorContext.special(mob));

                // Set target
                mob.setTarget(target);

                // Add to world
                level.addFreshEntity(mob);

                JustEnoughGuns.LOGGER.info("[SIMPLE_GROUND_RAID] Spawned {} at {}", mob.getType().getDescriptionId(), spawnPos);

            } catch (Exception e) {
                JustEnoughGuns.LOGGER.error("[SIMPLE_GROUND_RAID] Failed to spawn mob {}: {}", i, e.getMessage());
            }
        }

        JustEnoughGuns.LOGGER.info("[SIMPLE_GROUND_RAID] Wave {} completed", waveNumber);
    }

    private static void scheduleWave(ServerLevel level, BlockPos origin, Player target, int delay, RandomSource random, int waveNumber) {
        JustEnoughGuns.LOGGER.info("[SIMPLE_GROUND_RAID] Scheduling wave {} in {} ticks", waveNumber, delay);

        // Use the existing TerrorRaidScheduler system
        ttv.migami.jeg.entity.monster.phantom.TerrorRaidScheduler.schedule(level, delay, () -> {
            spawnWave(level, origin, target, waveNumber, random);
        });
    }

    private static BlockPos findSpawnPosition(ServerLevel level, BlockPos center, RandomSource random) {
        // Try to find a valid spawn position around the center
        for (int attempt = 0; attempt < 10; attempt++) {
            int angle = random.nextInt(360);
            int distance = 15 + random.nextInt(20); // 15-35 blocks away

            double radians = Math.toRadians(angle);
            int x = center.getX() + (int)(Math.cos(radians) * distance);
            int z = center.getZ() + (int)(Math.sin(radians) * distance);

            // Find ground level
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);

            BlockPos pos = new BlockPos(x, y + 1, z); // Spawn 1 block above ground

            // Check if position is valid (air space for mob)
            if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                return pos;
            }
        }

        return null; // No valid position found
    }

    private static Monster createMobForWave(ServerLevel level, int waveNumber, RandomSource random) {
        int roll = random.nextInt(100);

        if (waveNumber == 0) {
            // Wave 1: Mix of zombies and husks
            if (roll < 50) {
                return new Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE, level);
            } else if (roll < 85) {
                return new Husk(net.minecraft.world.entity.EntityTypes.HUSK, level);
            } else {
                return new ZombieVillager(net.minecraft.world.entity.EntityTypes.ZOMBIE_VILLAGER, level);
            }
        } else if (waveNumber == 1) {
            // Wave 2: Mix with skeletons
            if (roll < 35) {
                return new Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE, level);
            } else if (roll < 70) {
                return new Skeleton(net.minecraft.world.entity.EntityTypes.SKELETON, level);
            } else if (roll < 90) {
                return new Stray(net.minecraft.world.entity.EntityTypes.STRAY, level);
            } else {
                return new ZombieVillager(net.minecraft.world.entity.EntityTypes.ZOMBIE_VILLAGER, level);
            }
        } else {
            // Wave 3: All types
            if (roll < 25) {
                return new Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE, level);
            } else if (roll < 45) {
                return new Skeleton(net.minecraft.world.entity.EntityTypes.SKELETON, level);
            } else if (roll < 60) {
                return new Stray(net.minecraft.world.entity.EntityTypes.STRAY, level);
            } else if (roll < 80) {
                return new ZombieVillager(net.minecraft.world.entity.EntityTypes.ZOMBIE_VILLAGER, level);
            } else if (roll < 92) {
                return new Husk(net.minecraft.world.entity.EntityTypes.HUSK, level);
            } else {
                return new Drowned(net.minecraft.world.entity.EntityTypes.DROWNED, level);
            }
        }
    }
}