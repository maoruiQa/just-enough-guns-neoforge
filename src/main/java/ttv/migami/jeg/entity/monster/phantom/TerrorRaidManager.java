package ttv.migami.jeg.entity.monster.phantom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.BossEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.ai.AIType;
import ttv.migami.jeg.entity.ai.GunAttackGoal;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.faction.GunnerArmorEquiper;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.faction.GroupedGunnerRecovery;
import ttv.migami.jeg.util.LootUtils;

/**
 * Utility that emulates the legacy Terror Phantom post-death raid event.
 */
final class TerrorRaidManager {
    private static final ResourceKey<LootTable> SUPPLY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Reference.id("chests/terror_phantom_supply"));
    private static final ResourceKey<LootTable> REWARD_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Reference.id("chests/terror_phantom_reward"));
    private static final UniformInt GROUND_RAID_WAVE_SIZE = UniformInt.of(18, 22);
    private static final UniformInt PHANTOM_WAVE_SIZE = UniformInt.of(4, 7);
    private static final ResourceLocation GUN_FOLLOW_RANGE_MODIFIER_ID = Reference.id("gun_follow_range_modifier");
    private static final String TERROR_RAID_MOB_TAG = "TerrorRaidMob";
    private static final String RAID_ID_TAG_PREFIX = "JEGTerrorRaidId:";
    private static final String RAID_ORIGIN_TAG_PREFIX = "JEGTerrorRaidOrigin:";
    private static final String RAID_TOTAL_WAVES_TAG_PREFIX = "JEGTerrorRaidTotalWaves:";
    private static final String RAID_WAVES_SPAWNED_TAG_PREFIX = "JEGTerrorRaidWavesSpawned:";
    private static final String RAID_AIR_TAG_PREFIX = "JEGTerrorRaidAir:";
    private static final String RAID_TARGET_TAG_PREFIX = "JEGTerrorRaidTarget:";
    private static final int RAID_PLAYER_RANGE = 128;
    private static final int RAID_DUPLICATE_GUARD_TICKS = 200;
    private static final int LEGACY_RECOVERY_MERGE_DISTANCE = 192;
    private static final int UNREACHABLE_REPATH_TICKS = 3;
    private static final int UNREACHABLE_RELOCATE_TICKS = 8;
    private static final int UNREACHABLE_CLEANUP_TICKS = 24;
    private static final int STATIONARY_STUCK_TICKS = 120;
    private static final int SPIN_STUCK_TICKS = 120;
    private static final int DISTANCE_STALL_TICKS = 120;
    private static final double STATIONARY_MOVEMENT_THRESHOLD_SQ = 0.25D;
    private static final float SPIN_STUCK_YAW_DELTA_DEGREES = 35.0F;
    private static final double DISTANCE_STALL_THRESHOLD = 1.5D;
    private static final int MAX_GROUND_SAMPLE_ATTEMPTS = 12;
    private static final double RAID_NAVIGATION_SPEED = 1.2D;
    private static final Map<ServerLevel, List<RaidContext>> ACTIVE_RAIDS = new HashMap<>();
    private static final Map<ServerLevel, Map<BlockPos, Long>> RECENT_RAID_TRIGGERS = new HashMap<>();

    private TerrorRaidManager() {}

    static void triggerGroundRaid(ServerLevel level, BlockPos origin, @Nullable Player targetPlayer) {
        BlockPos raidOrigin = origin.immutable();
        if (isDuplicateTrigger(level, raidOrigin)) {
            return;
        }
        broadcast(level, raidOrigin, Component.translatable("message.jeg.terror_raid.begin"));
        spawnLootCrates(level, raidOrigin);
        spawnFlareBurst(level, raidOrigin, false);
        awardCelebrationXp(level, raidOrigin);
        int waveCount = Config.terrorRaidGroundWaveCount();
        int intervalTicks = Config.terrorRaidWaveIntervalSeconds() * 20;
        RaidContext raid = createRaid(level, raidOrigin, targetPlayer, false, waveCount);
        for (int i = 0; i < waveCount; i++) {
            int waveNumber = i;
            TerrorRaidScheduler.schedule(level, i * intervalTicks, () -> spawnGroundRaidWave(level, raidOrigin, waveNumber, targetPlayer, raid));
        }
        scheduleRaidTick(level, raid);
    }

    static void triggerAirRaid(ServerLevel level, BlockPos origin, @Nullable Player targetPlayer) {
        BlockPos raidOrigin = origin.immutable();
        if (isDuplicateTrigger(level, raidOrigin)) {
            return;
        }
        broadcast(level, raidOrigin, Component.translatable("message.jeg.terror_raid.guardian"));
        spawnFlareBurst(level, raidOrigin, true);
        awardCelebrationXp(level, raidOrigin);
        int waveCount = Config.terrorRaidAirWaveCount();
        int intervalTicks = Config.terrorRaidWaveIntervalSeconds() * 20;
        RaidContext raid = createRaid(level, raidOrigin, targetPlayer, true, waveCount);
        for (int wave = 0; wave < waveCount; wave++) {
            int delay = wave * intervalTicks;
            TerrorRaidScheduler.schedule(level, delay, () -> spawnPhantomWave(level, raidOrigin, targetPlayer, raid));
        }
        scheduleRaidTick(level, raid);
    }

    static void triggerGuardianAftermath(ServerLevel level, BlockPos origin, @Nullable Player targetPlayer) {
        triggerAirRaid(level, origin, targetPlayer);
    }
    static void recoverRaidMob(PathfinderMob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!mob.getTags().contains(TERROR_RAID_MOB_TAG)) {
            return;
        }

        UUID raidId = parseUuid(readTagValue(mob, RAID_ID_TAG_PREFIX));
        BlockPos origin = parseOrigin(readTagValue(mob, RAID_ORIGIN_TAG_PREFIX), mob.blockPosition());
        int totalWaves = Math.max(1, parseInt(readTagValue(mob, RAID_TOTAL_WAVES_TAG_PREFIX), 1));
        boolean airRaid = Boolean.parseBoolean(readTagValue(mob, RAID_AIR_TAG_PREFIX));
        UUID targetPlayerId = parseUuid(readTagValue(mob, RAID_TARGET_TAG_PREFIX));

        List<RaidContext> raids = ACTIVE_RAIDS.computeIfAbsent(level, ignored -> new ArrayList<>());
        RaidContext raid = raidId != null ? findContextById(raids, raidId) : null;
        if (raid == null) {
            raid = findCompatibleRecoveryContext(raids, origin, airRaid, targetPlayerId);
        }
        if (raid == null) {
            if (raidId == null) {
                raidId = UUID.randomUUID();
            }
            raid = new RaidContext(raidId, origin, targetPlayerId, airRaid, totalWaves);
            // Scheduler state is in-memory only; after relog treat current mobs as the remaining phase.
            raid.wavesSpawned = raid.totalWaves;
            raids.add(raid);
            scheduleRaidTick(level, raid);
        }

        raid.trackRecoveredMob(mob);
    }

    private static void broadcast(ServerLevel level, BlockPos soundPos, MutableComponent message) {
        level.getServer().getPlayerList().broadcastSystemMessage(message, false);
        level.playSound(null, soundPos, SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(), SoundSource.HOSTILE, 4.0F, 0.9F);
    }

    private static void spawnLootCrates(ServerLevel level, BlockPos origin) {
        placeBarrelWithLoot(level, findTerrain(level, origin), SUPPLY_LOOT);
        placeBarrelWithLoot(level, findTerrain(level, origin.offset(2, 0, 2)), REWARD_LOOT);
    }

    private static BlockPos findTerrain(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        while (cursor.getY() > level.getMinBuildHeight() && level.isEmptyBlock(cursor)) {
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
            // Barrel successfully placed with loot table
        } else if (blockEntity instanceof net.minecraft.world.Container container) {
            // Barrel successfully filled as container
        } else {
            // Barrel placed without container block entity
        }
    }

    private static void spawnGroundRaidWave(ServerLevel level, BlockPos origin, int waveNumber, @Nullable Player targetPlayer, RaidContext raid) {
        RandomSource random = level.getRandom();
        int mobCount = GROUND_RAID_WAVE_SIZE.sample(random);
        raid.wavesSpawned = Math.max(raid.wavesSpawned, waveNumber + 1);

        int successfulSpawns = 0;
        for (int i = 0; i < mobCount; i++) {
            net.minecraft.world.entity.Mob mob = createUndeadForWave(level, random, waveNumber);
            BlockPos spawnPos = null;
            for (int attempt = 0; attempt < MAX_GROUND_SAMPLE_ATTEMPTS; attempt++) {
                BlockPos candidate = sampleGroundPosition(level, origin, random, 6, 12);
                if (!isSafeGroundPosition(level, candidate)) {
                    continue;
                }
                mob.setPos(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                if (!level.noCollision(mob)) {
                    continue;
                }
                spawnPos = candidate;
                break;
            }
            if (spawnPos == null) {
                continue;
            }

            // CRITICAL: Set mob position to spawn position BEFORE adding to world
            mob.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);

            mob.addTag(GunEvents.JEG_GUNNER_TAG);
            mob.addTag(TERROR_RAID_MOB_TAG);
            mob.setPersistenceRequired();
            equipGunnerImmediately(mob);
            GunnerArmorEquiper.GunnerArmorContext armorContext = GunnerArmorEquiper.GunnerArmorContext.special((PathfinderMob) mob);
            GunnerArmorEquiper.equipGunnerArmor(random, armorContext);
            if (mob instanceof Skeleton) {
                AbstractTerrorPhantom.prepareSkeletonForDaylight((Skeleton) mob);
            }

            // Set target to player who killed Terror Phantom, or fallback to nearby player
            assignDirectTarget(level, mob, targetPlayer, spawnPos, 64.0D, raid.origin);

            if (!mob.isRemoved()) {
                boolean added = level.addFreshEntity(mob);
                if (added) {
                    successfulSpawns++;
                    raid.trackSpawn(mob);
                }
            }
        }
    }

    /**
     * Equips a gunner mob with a gun immediately during spawn, bypassing the timing race condition
     * that prevents GunnerMobSpawner from equipping guns on terror raid mobs.
     */
    private static void equipGunnerImmediately(net.minecraft.world.entity.Mob mob) {
        // Prevent baby entities from getting guns during terror raids
        if (mob.isBaby()) {
            return;
        }

        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        ItemStack heldItem = pathfinderMob.getMainHandItem();

        if (!(heldItem.getItem() instanceof GunItem)) {
            GunnerManager manager = new GunnerManager(GunnerManager.getConfigFactions());
            String entityName = mob.getType().getDescriptionId().replace("entity.", "").replace(".", ":");
            ResourceLocation entityTypeLocation = ResourceLocation.tryParse(entityName);
            Faction faction = manager.getFactionForMob(entityTypeLocation);

            if (faction != null) {
                boolean isCloseRange = mob.getRandom().nextBoolean();
                int stopRange = isCloseRange ? 7 : 20;

                Item gun = faction.getRandomGun(isCloseRange);
                AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
                boolean elite = (mob.getRandom().nextFloat() < GunMobValues.eliteChance && GunMobValues.elitesEnabled);
                int aiLevel = faction.getAiLevel() + (elite ? 1 : 0);

                if (elite) {
                    gun = faction.getEliteGun();
                    applyEliteAttributes(pathfinderMob);
                }

                if (!mob.level().isClientSide() && !hasGunAttackGoal(pathfinderMob)) {
                    pathfinderMob.goalSelector.addGoal(2, new GunAttackGoal<>(pathfinderMob, stopRange, 1.2F, aiType, aiLevel));
                    mob.addTag("GunAttackAssigned");
                }

                ItemStack modifiedGun = createModifiedGun(pathfinderMob, gun);
                mob.setItemSlot(EquipmentSlot.MAINHAND, modifiedGun);
                extendFollowRange(pathfinderMob);
            }
        }
    }

    private static boolean hasGunAttackGoal(PathfinderMob mob) {
        return mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof GunAttackGoal<?>);
    }

    private static void applyEliteAttributes(PathfinderMob mob) {
        mob.addTag("EliteGunner");
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        // Use the bulletproof armor system instead of manually equipping turtle helmet
        // Elite armor will be equipped through the armor equiper system in equipGunnerImmediately
        // Using alternative effect since DAMAGE_BOOST doesn't exist in 1.21.10
        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 1, false, true));
        mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, -1, 0, false, false));
    }

    private static ItemStack createModifiedGun(PathfinderMob mob, Item gun) {
        ItemStack gunStack = new ItemStack(gun);
        if (gun instanceof GunItem gunItem) {
            GunStats stats = gunItem.getStats();
            // Ground-raid gunners need a loaded weapon on spawn or they can stall before their first reload.
            gunStack.set(ttv.migami.jeg.init.ModDataComponents.GUN_AMMO.get(),
                        Math.max(1, stats.magazineSize()));
        }
        return gunStack;
    }

    private static void extendFollowRange(PathfinderMob mob) {
        AttributeInstance attribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (attribute != null) {
            double additionalRange = 64 - attribute.getBaseValue();
            AttributeModifier modifier = new AttributeModifier(
                    GUN_FOLLOW_RANGE_MODIFIER_ID,
                    additionalRange,
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!attribute.hasModifier(GUN_FOLLOW_RANGE_MODIFIER_ID)) {
                attribute.addPermanentModifier(modifier);
            }
        }
    }

    private static net.minecraft.world.entity.Mob createUndeadForWave(ServerLevel level, RandomSource random, int waveNumber) {
        // Wave-based undead diversity matching JEG 1.20.1
        int roll = random.nextInt(100);

        net.minecraft.world.entity.Mob mob;
        String mobType;

        if (waveNumber == 0) {
            // Wave 1: zombies, zombie_villagers, husks with lower-tier guns
            if (roll < 40) {
                mob = new Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, level);
                mobType = "Zombie";
            } else if (roll < 70) {
                mob = new ZombieVillager(net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER, level);
                mobType = "ZombieVillager";
            } else if (roll < 85) {
                mob = new Husk(net.minecraft.world.entity.EntityType.HUSK, level);
                mobType = "Husk";
            } else {
                mob = new Skeleton(net.minecraft.world.entity.EntityType.SKELETON, level);
                mobType = "Skeleton";
            }
        } else if (waveNumber == 1) {
            // Wave 2: mix of zombies, skeletons, strays with mid-tier guns
            if (roll < 35) {
                mob = new Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, level);
                mobType = "Zombie";
            } else if (roll < 65) {
                mob = new Skeleton(net.minecraft.world.entity.EntityType.SKELETON, level);
                mobType = "Skeleton";
            } else if (roll < 85) {
                mob = new Stray(net.minecraft.world.entity.EntityType.STRAY, level);
                mobType = "Stray";
            } else {
                mob = new ZombieVillager(net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER, level);
                mobType = "ZombieVillager";
            }
        } else {
            // Wave 3: all types with higher-tier guns
            if (roll < 25) {
                mob = new Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, level);
                mobType = "Zombie";
            } else if (roll < 45) {
                mob = new ZombieVillager(net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER, level);
                mobType = "ZombieVillager";
            } else if (roll < 60) {
                mob = new Stray(net.minecraft.world.entity.EntityType.STRAY, level);
                mobType = "Stray";
            } else if (roll < 80) {
                mob = new Skeleton(net.minecraft.world.entity.EntityType.SKELETON, level);
                mobType = "Skeleton";
            } else {
                mob = new Husk(net.minecraft.world.entity.EntityType.HUSK, level);
                mobType = "Husk";
            }
        }

        return mob;
    }
    private static void spawnPhantomWave(ServerLevel level, BlockPos origin, @Nullable Player targetPlayer, RaidContext raid) {
        RandomSource random = level.getRandom();
        int mobCount = PHANTOM_WAVE_SIZE.sample(random);
        raid.wavesSpawned = Math.min(raid.totalWaves, raid.wavesSpawned + 1);
        for (int i = 0; i < mobCount; i++) {
            Vec3 center = sampleAirPosition(level, origin, random, 10, 18, 8, 14);
            PhantomGunner gunner = new PhantomGunnerMinion(ModEntities.PHANTOM_GUNNER_MINION.get(), level);
            Vec3 spawn = center.add(0.5D, 0.0D, 0.5D);
            gunner.setPos(spawn.x, spawn.y, spawn.z);
            gunner.setYRot(random.nextFloat() * 360.0F);
            gunner.setXRot(-10.0F);
            gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spawn)), MobSpawnType.EVENT, (SpawnGroupData) null);
            gunner.addTag(TERROR_RAID_MOB_TAG);
            gunner.setPersistenceRequired();

            // Set target to player who killed Terror Phantom, or fallback to nearby player
            assignDirectTarget(level, gunner, targetPlayer, BlockPos.containing(spawn), 128.0D, raid.origin);

            if (level.addFreshEntity(gunner)) {
                raid.trackSpawn(gunner);
            }
        }
    }

    private static void assignInitialTarget(ServerLevel level, net.minecraft.world.entity.Mob mob, BlockPos origin, double range) {
        Player target = level.getNearestEntity(Player.class, TargetingConditions.forCombat().range(range), null, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, new AABB(origin).inflate(range));
        if (target != null) {
            mob.setTarget(target);
        }
    }

    private static void assignDirectTarget(ServerLevel level, net.minecraft.world.entity.Mob mob, @Nullable Player targetPlayer, BlockPos spawnPos, double range, BlockPos raidOrigin) {
        Player target = level.getNearestEntity(Player.class, TargetingConditions.forCombat().range(range), null,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D, new AABB(spawnPos).inflate(range));
        if (target != null && !isValidRaidTarget(target, level, raidOrigin)) {
            target = null;
        }
        if (target == null && isValidRaidTarget(targetPlayer, level, raidOrigin)) {
            target = targetPlayer;
        }

        // Set aggressive target with extended follow range
        if (target != null) {
            mob.setTarget(target);

            // Increase follow range to ensure mobs can track the player over long distances
            if (mob instanceof PathfinderMob pathfinderMob) {
                AttributeInstance followRange = pathfinderMob.getAttribute(Attributes.FOLLOW_RANGE);
                if (followRange != null && !followRange.hasModifier(GUN_FOLLOW_RANGE_MODIFIER_ID)) {
                    extendFollowRange(pathfinderMob);
                }
            }
        }
    }

    private static RaidContext createRaid(ServerLevel level, BlockPos origin, @Nullable Player initialTarget, boolean airRaid, int totalWaves) {
        RaidContext raid = new RaidContext(UUID.randomUUID(), origin, initialTarget != null ? initialTarget.getUUID() : null, airRaid, totalWaves);
        ACTIVE_RAIDS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(raid);
        RECENT_RAID_TRIGGERS.computeIfAbsent(level, ignored -> new HashMap<>()).put(origin.immutable(), level.getGameTime());
        return raid;
    }

    @Nullable
    private static RaidContext findContextById(List<RaidContext> raids, UUID raidId) {
        for (RaidContext raid : raids) {
            if (raid.raidId.equals(raidId)) {
                return raid;
            }
        }
        return null;
    }

    @Nullable
    private static RaidContext findCompatibleRecoveryContext(List<RaidContext> raids, BlockPos origin, boolean airRaid, @Nullable UUID targetPlayerId) {
        RaidContext best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (RaidContext raid : raids) {
            if (raid.airRaid != airRaid) {
                continue;
            }
            if (targetPlayerId != null && raid.targetPlayerId != null && !targetPlayerId.equals(raid.targetPlayerId)) {
                continue;
            }

            int distance = raid.origin.distManhattan(origin);
            if (distance > LEGACY_RECOVERY_MERGE_DISTANCE || distance >= bestDistance) {
                continue;
            }

            best = raid;
            bestDistance = distance;
        }
        return best;
    }

    @Nullable
    private static String readTagValue(net.minecraft.world.entity.Mob mob, String prefix) {
        for (String tag : mob.getTags()) {
            if (tag.startsWith(prefix)) {
                return tag.substring(prefix.length());
            }
        }
        return null;
    }

    @Nullable
    private static UUID parseUuid(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int parseInt(@Nullable String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static BlockPos parseOrigin(@Nullable String value, BlockPos fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String[] parts = value.split(",");
        if (parts.length != 3) {
            return fallback;
        }

        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void replaceTagValue(net.minecraft.world.entity.Mob mob, String prefix, @Nullable String value) {
        List<String> stale = new ArrayList<>();
        for (String tag : mob.getTags()) {
            if (tag.startsWith(prefix)) {
                stale.add(tag);
            }
        }

        for (String tag : stale) {
            mob.removeTag(tag);
        }

        if (value != null && !value.isBlank()) {
            mob.addTag(prefix + value);
        }
    }

    private static void scheduleRaidTick(ServerLevel level, RaidContext raid) {
        TerrorRaidScheduler.schedule(level, 20, () -> tickRaid(level, raid));
    }

    private static void tickRaid(ServerLevel level, RaidContext raid) {
        if (raid.completed) {
            cleanupRaid(level, raid);
            return;
        }

        raid.refreshActiveMobs(level);
        maintainRaidTargets(level, raid);
        refreshBossBarPlayers(level, raid);
        float progress = raid.computeProgress();
        raid.bossBar.setProgress(Mth.clamp(progress, 0.0F, 1.0F));

        int remaining = raid.activeMobIds.size();
        raid.bossBar.setName(Component.literal("Terror Raid " + raid.wavesSpawned + "/" + raid.totalWaves + " - Remaining " + remaining));

        boolean allWavesDone = raid.wavesSpawned >= raid.totalWaves;
        if (allWavesDone && remaining <= 0) {
            raid.completed = true;
            cleanupRaid(level, raid);
            return;
        }

        scheduleRaidTick(level, raid);
    }

    private static void cleanupRaid(ServerLevel level, RaidContext raid) {
        raid.bossBar.removeAllPlayers();
        List<RaidContext> raids = ACTIVE_RAIDS.get(level);
        if (raids != null) {
            raids.remove(raid);
            if (raids.isEmpty()) {
                ACTIVE_RAIDS.remove(level);
            }
        }
    }

    private static void refreshBossBarPlayers(ServerLevel level, RaidContext raid) {
        PlayerList playerList = level.getServer().getPlayerList();
        for (Player player : playerList.getPlayers()) {
            if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                continue;
            }
            boolean inRange = serverPlayer.level() == level && isWithinRaidRange(serverPlayer.blockPosition(), raid.origin);
            if (inRange) {
                raid.bossBar.addPlayer(serverPlayer);
            } else {
                raid.bossBar.removePlayer(serverPlayer);
            }
        }
    }

    private static void maintainRaidTargets(ServerLevel level, RaidContext raid) {
        Player preferred = resolvePreferredTarget(level, raid);
        if (preferred == null) {
            return;
        }

        Set<UUID> timedOut = new HashSet<>();
        for (UUID mobId : new HashSet<>(raid.activeMobIds)) {
            var entity = level.getEntity(mobId);
            if (!(entity instanceof net.minecraft.world.entity.Mob mob) || !mob.isAlive()) {
                raid.clearMobTracking(mobId);
                continue;
            }
            if (!mob.getTags().contains(TERROR_RAID_MOB_TAG)) {
                raid.clearMobTracking(mobId);
                continue;
            }

            LivingEntity current = mob.getTarget();
            if (!(current instanceof Player player) || !isValidRaidTarget(player, level, raid.origin)) {
                mob.setTarget(preferred);
                mob.setAggressive(true);
            }

            if (checkUnreachableAndRecover(level, raid, mobId, mob, preferred)) {
                timedOut.add(mobId);
            }
        }

        if (!timedOut.isEmpty()) {
            raid.activeMobIds.removeAll(timedOut);
            for (UUID mobId : timedOut) {
                raid.clearMobTracking(mobId);
            }
        }
    }

    private static boolean checkUnreachableAndRecover(ServerLevel level, RaidContext raid, UUID mobId, net.minecraft.world.entity.Mob mob, Player preferred) {
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            raid.clearMobTracking(mobId);
            return false;
        }

        int stationaryTicks = updateStationaryTicks(raid, mobId, mob);
        int spinTicks = updateSpinTicks(raid, mobId, mob);
        int distanceStallTicks = updateDistanceStallTicks(raid, mobId, mob, preferred);
        int stuckSeverity = Math.max(stationaryTicks, Math.max(spinTicks, distanceStallTicks));
        if (stuckSeverity < 60) {
            return false;
        }
        if (stuckSeverity < STATIONARY_STUCK_TICKS) {
            forceRepath(pathfinderMob, preferred);
            return false;
        }

        int eligibleCount = Math.min(3, countEligibleRecoveryMobs(raid, level, preferred));
        boolean recovered;
        if (raid.airRaid) {
            Vec3 relocate = sampleAirPosition(level, preferred.blockPosition(), level.getRandom(), 4, 8, 6, 10);
            recovered = GroupedGunnerRecovery.tryRecoverAirMob(level, "terror-raid:" + raid.raidId, pathfinderMob, preferred, relocate, eligibleCount);
        } else {
            recovered = GroupedGunnerRecovery.tryRecoverGroundMob(
                    level,
                    "terror-raid:" + raid.raidId,
                    raid.origin,
                    pathfinderMob,
                    (net.minecraft.server.level.ServerPlayer) preferred,
                    4,
                    8,
                    4,
                    10,
                    12,
                    eligibleCount
            );
        }
        if (recovered) {
            raid.incrementRelocationCount(mobId);
            raid.clearMobTracking(mobId);
            return false;
        }

        forceRepath(pathfinderMob, preferred);
        return false;
    }

    private static int updateStationaryTicks(RaidContext raid, UUID mobId, net.minecraft.world.entity.Mob mob) {
        Vec3 currentPos = mob.position();
        Vec3 lastPos = raid.lastPositions.put(mobId, currentPos);
        if (lastPos == null || lastPos.distanceToSqr(currentPos) >= STATIONARY_MOVEMENT_THRESHOLD_SQ) {
            raid.stationaryTicks.remove(mobId);
            return 0;
        }
        return raid.stationaryTicks.merge(mobId, 20, Integer::sum);
    }

    private static int updateSpinTicks(RaidContext raid, UUID mobId, net.minecraft.world.entity.Mob mob) {
        float currentYaw = mob.getYRot();
        Float lastYaw = raid.lastYaws.put(mobId, currentYaw);
        if (lastYaw == null) {
            raid.spinTicks.remove(mobId);
            return 0;
        }
        float delta = Math.abs(Mth.wrapDegrees(currentYaw - lastYaw));
        if (delta < SPIN_STUCK_YAW_DELTA_DEGREES) {
            raid.spinTicks.remove(mobId);
            return 0;
        }
        return raid.spinTicks.merge(mobId, 20, Integer::sum);
    }

    private static int updateDistanceStallTicks(RaidContext raid, UUID mobId, net.minecraft.world.entity.Mob mob, Player preferred) {
        double distanceSq = mob.distanceToSqr(preferred);
        Double lastDistanceSq = raid.lastTargetDistanceSq.put(mobId, distanceSq);
        if (lastDistanceSq == null || lastDistanceSq - distanceSq > DISTANCE_STALL_THRESHOLD) {
            raid.distanceStallTicks.remove(mobId);
            return 0;
        }
        return raid.distanceStallTicks.merge(mobId, 20, Integer::sum);
    }

    private static int countEligibleRecoveryMobs(RaidContext raid, ServerLevel level, Player preferred) {
        int eligible = 0;
        for (UUID id : raid.activeMobIds) {
            var entity = level.getEntity(id);
            if (!(entity instanceof net.minecraft.world.entity.Mob mob) || !(mob instanceof PathfinderMob)) {
                continue;
            }
            if (preferred.hasLineOfSight(mob)) {
                continue;
            }
            int severity = Math.max(
                    raid.stationaryTicks.getOrDefault(id, 0),
                    Math.max(raid.spinTicks.getOrDefault(id, 0), raid.distanceStallTicks.getOrDefault(id, 0))
            );
            if (severity >= STATIONARY_STUCK_TICKS) {
                eligible++;
                if (eligible >= 3) {
                    return eligible;
                }
            }
        }
        return eligible;
    }

    private static void forceRepath(PathfinderMob mob, Player preferred) {
        if (mob.getNavigation().moveTo(preferred, RAID_NAVIGATION_SPEED)) {
            return;
        }
        var path = mob.getNavigation().createPath(preferred, 0);
        if (path != null) {
            mob.getNavigation().moveTo(path, RAID_NAVIGATION_SPEED);
        }
    }

    private static void relocateRaidMob(ServerLevel level, RaidContext raid, PathfinderMob mob, Player preferred) {
        if (raid.airRaid) {
            Vec3 relocate = sampleAirPosition(level, preferred.blockPosition(), level.getRandom(), 4, 8, 6, 10);
            mob.teleportTo(relocate.x, relocate.y, relocate.z);
        } else {
            BlockPos relocate = sampleGroundPosition(level, preferred.blockPosition(), level.getRandom(), 4, 10);
            if (!isSafeGroundPosition(level, relocate)) {
                return;
            }
            mob.teleportTo(relocate.getX() + 0.5D, relocate.getY(), relocate.getZ() + 0.5D);
        }

        mob.getNavigation().stop();
        mob.setTarget(preferred);
        mob.setAggressive(true);
        forceRepath(mob, preferred);
    }

    private static @Nullable Player resolvePreferredTarget(ServerLevel level, RaidContext raid) {
        if (raid.targetPlayerId != null) {
            Player preferred = level.getServer().getPlayerList().getPlayer(raid.targetPlayerId);
            if (isValidRaidTarget(preferred, level, raid.origin)) {
                return preferred;
            }
        }

        AABB search = new AABB(raid.origin).inflate(RAID_PLAYER_RANGE);
        Player nearest = level.getNearestEntity(Player.class, TargetingConditions.forCombat().range(RAID_PLAYER_RANGE), null,
                raid.origin.getX() + 0.5D, raid.origin.getY() + 0.5D, raid.origin.getZ() + 0.5D, search);
        return isValidRaidTarget(nearest, level, raid.origin) ? nearest : null;
    }

    private static boolean isValidRaidTarget(@Nullable Player player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private static boolean isValidRaidTarget(@Nullable Player player, ServerLevel level, BlockPos raidOrigin) {
        return isValidRaidTarget(player) && player.level() == level && isWithinRaidRange(player.blockPosition(), raidOrigin);
    }

    private static boolean isWithinRaidRange(BlockPos playerPos, BlockPos raidOrigin) {
        return playerPos.distManhattan(raidOrigin) <= RAID_PLAYER_RANGE;
    }

    private static boolean isDuplicateTrigger(ServerLevel level, BlockPos origin) {
        long now = level.getGameTime();
        Map<BlockPos, Long> recent = RECENT_RAID_TRIGGERS.computeIfAbsent(level, ignored -> new HashMap<>());
        recent.entrySet().removeIf(entry -> now - entry.getValue() > RAID_DUPLICATE_GUARD_TICKS);

        for (Map.Entry<BlockPos, Long> entry : recent.entrySet()) {
            if (entry.getKey().distManhattan(origin) <= 16 && now - entry.getValue() <= RAID_DUPLICATE_GUARD_TICKS) {
                return true;
            }
        }
        return false;
    }

    private static final class RaidContext {
        private final UUID raidId;
        private final BlockPos origin;
        private final @Nullable UUID targetPlayerId;
        private final boolean airRaid;
        private final int totalWaves;
        private final Set<UUID> activeMobIds = new HashSet<>();
        private final Map<UUID, Integer> unreachableTicks = new HashMap<>();
        private final Map<UUID, Integer> relocationCounts = new HashMap<>();
        private final Map<UUID, Vec3> lastPositions = new HashMap<>();
        private final Map<UUID, Integer> stationaryTicks = new HashMap<>();
        private final Map<UUID, Float> lastYaws = new HashMap<>();
        private final Map<UUID, Integer> spinTicks = new HashMap<>();
        private final Map<UUID, Double> lastTargetDistanceSq = new HashMap<>();
        private final Map<UUID, Integer> distanceStallTicks = new HashMap<>();
        private final ServerBossEvent bossBar;
        private int spawnedTotal;
        private int wavesSpawned;
        private boolean completed;

        private RaidContext(UUID raidId, BlockPos origin, @Nullable UUID targetPlayerId, boolean airRaid, int totalWaves) {
            this.raidId = raidId;
            this.origin = origin.immutable();
            this.targetPlayerId = targetPlayerId;
            this.airRaid = airRaid;
            this.totalWaves = Math.max(1, totalWaves);
            Component title = airRaid
                    ? Component.translatable("message.jeg.terror_raid.guardian")
                    : Component.translatable("message.jeg.terror_raid.begin");
            this.bossBar = new ServerBossEvent(title, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setProgress(1.0F);
        }

                private void trackSpawn(net.minecraft.world.entity.Mob mob) {
            if (this.activeMobIds.add(mob.getUUID())) {
                this.spawnedTotal++;
            }
            clearMobTracking(mob.getUUID());
            applyRaidTags(mob);
        }

        private void trackRecoveredMob(net.minecraft.world.entity.Mob mob) {
            if (this.activeMobIds.add(mob.getUUID())) {
                this.spawnedTotal = Math.max(this.spawnedTotal, this.activeMobIds.size());
            }
            clearMobTracking(mob.getUUID());
            applyRaidTags(mob);
        }

        private void applyRaidTags(net.minecraft.world.entity.Mob mob) {
            mob.addTag(TERROR_RAID_MOB_TAG);
            replaceTagValue(mob, RAID_ID_TAG_PREFIX, this.raidId.toString());
            replaceTagValue(mob, RAID_ORIGIN_TAG_PREFIX, this.origin.getX() + "," + this.origin.getY() + "," + this.origin.getZ());
            replaceTagValue(mob, RAID_TOTAL_WAVES_TAG_PREFIX, Integer.toString(this.totalWaves));
            replaceTagValue(mob, RAID_WAVES_SPAWNED_TAG_PREFIX, Integer.toString(this.wavesSpawned));
            replaceTagValue(mob, RAID_AIR_TAG_PREFIX, Boolean.toString(this.airRaid));
            replaceTagValue(mob, RAID_TARGET_TAG_PREFIX, this.targetPlayerId != null ? this.targetPlayerId.toString() : null);
        }

        private void refreshActiveMobs(ServerLevel level) {
            Set<UUID> removed = new HashSet<>();
            this.activeMobIds.removeIf(uuid -> {
                var entity = level.getEntity(uuid);
                boolean missing = !(entity instanceof net.minecraft.world.entity.Mob mob) || !mob.isAlive();
                if (missing) {
                    removed.add(uuid);
                }
                return missing;
            });
            for (UUID mobId : removed) {
                clearMobTracking(mobId);
            }
        }

        private float computeProgress() {
            if (this.spawnedTotal <= 0) {
                return 1.0F;
            }
            int defeated = Math.max(0, this.spawnedTotal - this.activeMobIds.size());
            return 1.0F - ((float) defeated / (float) this.spawnedTotal);
        }

        private int incrementUnreachableTicks(UUID mobId) {
            return this.unreachableTicks.merge(mobId, 1, Integer::sum);
        }

        private void incrementRelocationCount(UUID mobId) {
            this.relocationCounts.merge(mobId, 1, Integer::sum);
        }

        private int getRelocationCount(UUID mobId) {
            return this.relocationCounts.getOrDefault(mobId, 0);
        }

        private void clearMobTracking(UUID mobId) {
            this.unreachableTicks.remove(mobId);
            this.relocationCounts.remove(mobId);
            this.lastPositions.remove(mobId);
            this.stationaryTicks.remove(mobId);
            this.lastYaws.remove(mobId);
            this.spinTicks.remove(mobId);
            this.lastTargetDistanceSq.remove(mobId);
            this.distanceStallTicks.remove(mobId);
        }
    }

    private static BlockPos sampleGroundPosition(ServerLevel level, BlockPos origin, RandomSource random, int minRadius, int maxRadius) {
        BlockPos fallback = resolveGroundSpawn(level, origin.getX(), origin.getZ());
        for (int attempt = 0; attempt < MAX_GROUND_SAMPLE_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radius = Mth.nextDouble(random, minRadius, maxRadius);
            int x = origin.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * radius);
            BlockPos candidate = resolveGroundSpawn(level, x, z);
            if (isSafeGroundPosition(level, candidate)) {
                return candidate;
            }
            fallback = candidate;
        }
        return fallback;
    }

    private static BlockPos resolveGroundSpawn(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos spawnPos = new BlockPos(x, y, z);
        if (level.isEmptyBlock(spawnPos)) {
            return spawnPos;
        }

        BlockPos checkPos = spawnPos.above();
        for (int attempt = 0; attempt < 5; attempt++) {
            if (level.isEmptyBlock(checkPos)) {
                return checkPos;
            }
            checkPos = checkPos.above();
        }
        return spawnPos;
    }

    private static boolean isSafeGroundPosition(ServerLevel level, BlockPos spawnPos) {
        if (!level.getWorldBorder().isWithinBounds(spawnPos)) {
            return false;
        }

        BlockState feetState = level.getBlockState(spawnPos);
        BlockState headState = level.getBlockState(spawnPos.above());
        BlockState groundState = level.getBlockState(spawnPos.below());
        if (isLeafBlock(feetState) || isLeafBlock(headState) || isLeafBlock(groundState)) {
            return false;
        }
        if (!feetState.getCollisionShape(level, spawnPos).isEmpty()) {
            return false;
        }
        if (!headState.getCollisionShape(level, spawnPos.above()).isEmpty()) {
            return false;
        }
        if (groundState.isAir()) {
            return false;
        }
        if (!level.getFluidState(spawnPos).isEmpty() || !level.getFluidState(spawnPos.above()).isEmpty()) {
            return false;
        }
        return true;
    }

    private static boolean isLeafBlock(BlockState state) {
        return state.is(BlockTags.LEAVES);
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




