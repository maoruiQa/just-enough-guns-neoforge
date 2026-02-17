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
import net.minecraft.world.level.block.Blocks;
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
    private static final int RAID_PLAYER_RANGE = 128;
    private static final int RAID_DUPLICATE_GUARD_TICKS = 200;
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
            BlockPos spawnPos = sampleGroundPosition(level, origin, random, 6, 12);
            net.minecraft.world.entity.Mob mob = createUndeadForWave(level, random, waveNumber);

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
            assignDirectTarget(level, mob, targetPlayer, spawnPos, 64.0D);

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
            // Set random ammo count between 0 and magazine size using stack.set()
            gunStack.set(ttv.migami.jeg.init.ModDataComponents.GUN_AMMO.get(),
                        mob.getRandom().nextInt(stats.magazineSize()));
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
            assignDirectTarget(level, gunner, targetPlayer, BlockPos.containing(spawn), 128.0D);

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

    private static void assignDirectTarget(ServerLevel level, net.minecraft.world.entity.Mob mob, @Nullable Player targetPlayer, BlockPos spawnPos, double range) {
        Player target = level.getNearestEntity(Player.class, TargetingConditions.forCombat().range(range), null,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D, new AABB(spawnPos).inflate(range));
        if (target != null && !isValidRaidTarget(target)) {
            target = null;
        }
        if (target == null && isValidRaidTarget(targetPlayer)) {
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
        RaidContext raid = new RaidContext(level, origin, initialTarget, airRaid, totalWaves);
        ACTIVE_RAIDS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(raid);
        RECENT_RAID_TRIGGERS.computeIfAbsent(level, ignored -> new HashMap<>()).put(origin.immutable(), level.getGameTime());
        return raid;
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
            boolean inRange = serverPlayer.level() == level && serverPlayer.blockPosition().distManhattan(raid.origin) <= RAID_PLAYER_RANGE;
            if (inRange) {
                raid.bossBar.addPlayer(serverPlayer);
            } else {
                raid.bossBar.removePlayer(serverPlayer);
            }
        }
    }

    private static void maintainRaidTargets(ServerLevel level, RaidContext raid) {
        Player preferred = resolvePreferredTarget(level, raid);
        for (UUID mobId : raid.activeMobIds) {
            var entity = level.getEntity(mobId);
            if (!(entity instanceof net.minecraft.world.entity.Mob mob) || !mob.isAlive()) {
                continue;
            }
            if (!mob.getTags().contains(TERROR_RAID_MOB_TAG)) {
                continue;
            }

            LivingEntity current = mob.getTarget();
            if (current instanceof Player player && isValidRaidTarget(player)) {
                continue;
            }
            if (!(current instanceof Player)) {
                mob.setTarget(null);
            }
            if (preferred != null) {
                mob.setTarget(preferred);
                mob.setAggressive(true);
            }
        }
    }

    private static @Nullable Player resolvePreferredTarget(ServerLevel level, RaidContext raid) {
        if (raid.targetPlayerId != null) {
            Player preferred = level.getServer().getPlayerList().getPlayer(raid.targetPlayerId);
            if (isValidRaidTarget(preferred) && preferred.level() == level) {
                return preferred;
            }
        }

        AABB search = new AABB(raid.origin).inflate(RAID_PLAYER_RANGE);
        Player nearest = level.getNearestEntity(Player.class, TargetingConditions.forCombat().range(RAID_PLAYER_RANGE), null,
                raid.origin.getX() + 0.5D, raid.origin.getY() + 0.5D, raid.origin.getZ() + 0.5D, search);
        return isValidRaidTarget(nearest) ? nearest : null;
    }

    private static boolean isValidRaidTarget(@Nullable Player player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
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
        private final UUID raidId = UUID.randomUUID();
        private final BlockPos origin;
        private final @Nullable UUID targetPlayerId;
        private final int totalWaves;
        private final Set<UUID> activeMobIds = new HashSet<>();
        private final ServerBossEvent bossBar;
        private int spawnedTotal;
        private int wavesSpawned;
        private boolean completed;

        private RaidContext(ServerLevel level, BlockPos origin, @Nullable Player targetPlayer, boolean airRaid, int totalWaves) {
            this.origin = origin.immutable();
            this.targetPlayerId = targetPlayer != null ? targetPlayer.getUUID() : null;
            this.totalWaves = Math.max(1, totalWaves);
            Component title = airRaid
                    ? Component.translatable("message.jeg.terror_raid.guardian")
                    : Component.translatable("message.jeg.terror_raid.begin");
            this.bossBar = new ServerBossEvent(title, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setProgress(1.0F);
        }

        private void trackSpawn(net.minecraft.world.entity.Mob mob) {
            this.activeMobIds.add(mob.getUUID());
            this.spawnedTotal++;
        }

        private void refreshActiveMobs(ServerLevel level) {
            this.activeMobIds.removeIf(uuid -> {
                var entity = level.getEntity(uuid);
                return !(entity instanceof net.minecraft.world.entity.Mob mob) || !mob.isAlive();
            });
        }

        private float computeProgress() {
            if (this.spawnedTotal <= 0) {
                return 1.0F;
            }
            int defeated = Math.max(0, this.spawnedTotal - this.activeMobIds.size());
            return 1.0F - ((float) defeated / (float) this.spawnedTotal);
        }
    }

    private static BlockPos sampleGroundPosition(ServerLevel level, BlockPos origin, RandomSource random, int minRadius, int maxRadius) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double radius = Mth.nextDouble(random, minRadius, maxRadius);
        int x = origin.getX() + Mth.floor(Math.cos(angle) * radius);
        int z = origin.getZ() + Mth.floor(Math.sin(angle) * radius);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos spawnPos = new BlockPos(x, y, z);

        // Check if the spawn position is valid (must be empty space for mob to spawn)
        boolean validSpawn = level.isEmptyBlock(spawnPos) && level.getWorldBorder().isWithinBounds(spawnPos);

        // If position is not valid (solid block), try to find air above
        if (!validSpawn) {
            // Try to find air space above the ground position
            BlockPos checkPos = spawnPos.above();
            int attempts = 0;
            while (attempts < 5 && !level.isEmptyBlock(checkPos)) {
                checkPos = checkPos.above();
                attempts++;
            }

            if (level.isEmptyBlock(checkPos) && level.getWorldBorder().isWithinBounds(checkPos)) {
                return checkPos;
            }
        }

        return spawnPos;
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


