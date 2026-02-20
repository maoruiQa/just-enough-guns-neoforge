package ttv.migami.jeg.faction.raid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.init.ModEntities;

public class RaidEntity extends Entity {
    private static final int ACTIVE_RADIUS = 64;
    private static final int MAX_ACTIVE_MOBS = 10;
    private static final int WAVE_MOBS = 20;
    private static final int DEFAULT_TOTAL_WAVES = 3;
    private static final int SPAWN_INTERVAL_TICKS = 30;
    private static final int WAVE_COOLDOWN_TICKS = 120;
    private static final int MAX_DESPAWN_TICKS = 600;

    private final ServerBossEvent bossBar;
    private final Set<UUID> activeMobIds = new HashSet<>();
    private final Set<UUID> spawnedThisWave = new HashSet<>();
    private final Set<UUID> activePlayerIds = new HashSet<>();

    private String factionName = "night_of_the_undead";
    private boolean forceGuns = true;
    private int totalWaves = DEFAULT_TOTAL_WAVES;
    private int currentWave = 0;
    private int waveCooldown = 20;
    private int spawnCooldown = SPAWN_INTERVAL_TICKS;
    private int despawnTicks = MAX_DESPAWN_TICKS;
    private boolean spawningWave;
    private boolean finished;
    private boolean victory;
    private boolean defeat;
    private boolean resultAnnounced;

    public RaidEntity(EntityType<? extends RaidEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
        this.bossBar = new ServerBossEvent(
                Component.translatable("raid.jeg"),
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS
        );
        this.bossBar.setVisible(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        refreshActiveMobs(serverLevel);
        refreshPlayers(serverLevel);

        if (!this.finished) {
            if (serverLevel.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
                finishDefeat();
            } else {
                tickActiveRaid(serverLevel);
            }
        }

        updateBossBar();

        if (this.finished) {
            tickFinished(serverLevel);
        }
    }

    private void tickActiveRaid(ServerLevel level) {
        if (!this.spawningWave && this.spawnedThisWave.isEmpty()) {
            if (--this.waveCooldown <= 0) {
                if (this.currentWave >= this.totalWaves) {
                    finishVictory();
                } else {
                    startWave(level);
                }
            }
            return;
        }

        if (this.spawningWave) {
            if (--this.spawnCooldown <= 0) {
                this.spawnCooldown = SPAWN_INTERVAL_TICKS;
                spawnWaveBatch(level);
                if (this.spawnedThisWave.size() >= WAVE_MOBS) {
                    this.spawningWave = false;
                }
            }
        }

        boolean waveSpawnedAll = this.spawnedThisWave.size() >= WAVE_MOBS;
        if (!this.spawningWave && waveSpawnedAll && this.activeMobIds.isEmpty()) {
            if (this.currentWave >= this.totalWaves) {
                finishVictory();
            } else {
                this.spawnedThisWave.clear();
                this.waveCooldown = WAVE_COOLDOWN_TICKS;
                level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("broadcast.jeg.raid.next_wave"), false);
            }
        }
    }

    private void startWave(ServerLevel level) {
        this.currentWave++;
        this.spawningWave = true;
        this.spawnCooldown = 1;
        playHorn(level);
    }

    private void spawnWaveBatch(ServerLevel level) {
        Faction faction = resolveFaction();
        if (faction == null) {
            return;
        }

        int attempts = 0;
        while (this.activeMobIds.size() < MAX_ACTIVE_MOBS && this.spawnedThisWave.size() < WAVE_MOBS && attempts++ < 8) {
            Mob mob = FactionSpawnHelper.spawnRaidMember(level, faction, this.blockPosition(), pickPreferredTarget(level));
            if (mob == null) {
                continue;
            }
            this.activeMobIds.add(mob.getUUID());
            this.spawnedThisWave.add(mob.getUUID());
        }
    }

    private void refreshActiveMobs(ServerLevel level) {
        this.activeMobIds.removeIf(id -> {
            Entity entity = level.getEntity(id);
            return !(entity instanceof Mob mob) || mob.isRemoved() || !mob.isAlive();
        });
    }

    private void refreshPlayers(ServerLevel level) {
        AABB zone = this.getBoundingBox().inflate(ACTIVE_RADIUS, ACTIVE_RADIUS, ACTIVE_RADIUS);
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class, zone, player -> !player.isSpectator() && !player.isDeadOrDying());

        Set<UUID> nearbyIds = new HashSet<>();
        for (ServerPlayer player : nearby) {
            nearbyIds.add(player.getUUID());
            this.bossBar.addPlayer(player);
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && !nearbyIds.contains(player.getUUID())) {
                this.bossBar.removePlayer(player);
            }
        }

        this.activePlayerIds.clear();
        this.activePlayerIds.addAll(nearbyIds);
    }

    private void updateBossBar() {
        if (this.finished) {
            this.bossBar.setProgress(this.victory ? 0.0F : 1.0F);
            return;
        }

        float progress;
        if (this.spawningWave || !this.spawnedThisWave.isEmpty()) {
            int defeated = this.spawnedThisWave.size() - this.activeMobIds.size();
            int remaining = Math.max(0, WAVE_MOBS - defeated);
            progress = (float) remaining / (float) WAVE_MOBS;
        } else {
            progress = 1.0F - ((float) this.waveCooldown / (float) WAVE_COOLDOWN_TICKS);
        }
        this.bossBar.setProgress(Mth.clamp(progress, 0.0F, 1.0F));

        MutableComponent factionLang = Component.translatable("faction.jeg." + this.factionName);
        MutableComponent raidLang = Component.translatable("raid.jeg");
        MutableComponent waveLang = Component.translatable("raid.jeg.wave");
        this.bossBar.setName(Component.literal(
                factionLang.getString() + " " + raidLang.getString() + " | " + waveLang.getString() + " : " + this.currentWave + "/" + this.totalWaves
        ));
    }

    private void tickFinished(ServerLevel level) {
        if (!this.resultAnnounced) {
            announceResult(level);
            this.resultAnnounced = true;
        }

        if (this.victory && this.tickCount % 20 == 0) {
            playCelebrationHorn(level);
        }
        if (this.defeat && this.tickCount % 60 == 0) {
            playHorn(level);
        }

        if (--this.despawnTicks <= 0) {
            this.bossBar.removeAllPlayers();
            this.discard();
        }
    }

    private void announceResult(ServerLevel level) {
        MutableComponent factionLang = Component.translatable("faction.jeg." + this.factionName);
        if (this.victory) {
            Component message = Component.translatable("broadcast.jeg.raid.victory", factionLang).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GOLD);
            level.getServer().getPlayerList().broadcastSystemMessage(message, false);
            return;
        }
        if (this.defeat) {
            Component defeatMessage = Component.translatable("broadcast.jeg.raid.defeat", factionLang).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.RED);
            level.getServer().getPlayerList().broadcastSystemMessage(defeatMessage, false);
        }
    }

    private void finishVictory() {
        this.finished = true;
        this.victory = true;
        this.defeat = false;
        this.spawningWave = false;
    }

    private void finishDefeat() {
        this.finished = true;
        this.defeat = true;
        this.victory = false;
        this.spawningWave = false;
    }

    @Nullable
    private Player pickPreferredTarget(ServerLevel level) {
        for (UUID playerId : this.activePlayerIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null && player.level() == level && !player.isSpectator()) {
                return player;
            }
        }

        ServerPlayer nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level || player.isSpectator() || player.isDeadOrDying()) {
                continue;
            }

            double distanceSq = player.distanceToSqr(this.getX(), this.getY(), this.getZ());
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = player;
            }
        }
        return nearest;
    }

    @Nullable
    private Faction resolveFaction() {
        GunnerManager manager = GunnerManager.getInstance();
        Faction faction = manager.getFactionByName(this.factionName);
        if (faction == null) {
            faction = FactionSpawnHelper.getRandomFaction();
            if (faction != null) {
                this.factionName = faction.getName();
            }
        }
        return faction;
    }

    private void playHorn(ServerLevel level) {
        int x = level.random.nextInt(-25, 25);
        int z = level.random.nextInt(-25, 25);
        level.playSound(null, BlockPos.containing(this.position().add(x, 24, z)), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.HOSTILE, 4.0F, 1.0F);
    }

    private void playCelebrationHorn(ServerLevel level) {
        int x = level.random.nextInt(-25, 25);
        int z = level.random.nextInt(-25, 25);
        int soundIndex = level.random.nextBoolean() ? 1 : 2;
        level.playSound(null, BlockPos.containing(this.position().add(x, 24, z)), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(soundIndex).value(), SoundSource.HOSTILE, 4.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        output.putString("FactionName", this.factionName);
        output.putBoolean("Finished", this.finished);
        output.putBoolean("Victory", this.victory);
        output.putBoolean("Defeat", this.defeat);
        output.putInt("CurrentWave", this.currentWave);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        if (input.contains("FactionName")) {
            this.factionName = input.getString("FactionName");
        }
        this.finished = input.getBoolean("Finished");
        this.victory = input.getBoolean("Victory");
        this.defeat = input.getBoolean("Defeat");
        this.currentWave = input.getInt("CurrentWave");
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    public static boolean hasActiveRaidNear(ServerLevel level, BlockPos pos, double radius) {
        AABB area = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius, pos.getY() + radius, pos.getZ() + radius
        );
        return !level.getEntitiesOfClass(RaidEntity.class, area, raid -> !raid.finished).isEmpty();
    }

    public static void summonRaidEntity(ServerLevel level, Faction faction, Vec3 startPos, boolean forceGuns) {
        if (faction == null || hasActiveRaidNear(level, BlockPos.containing(startPos), ACTIVE_RADIUS)) {
            return;
        }

        RaidEntity raidEntity = new RaidEntity(ModEntities.RAID_ENTITY.get(), level);
        raidEntity.setPos(startPos);
        raidEntity.factionName = faction.getName();
        raidEntity.forceGuns = forceGuns;
        level.addFreshEntity(raidEntity);

        Component message = Component.translatable(
                "broadcast.jeg.raid",
                Component.translatable("faction.jeg." + faction.getName()),
                BlockPos.containing(startPos).toShortString()
        ).withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
        level.getServer().getPlayerList().broadcastSystemMessage(message, false);
    }
}
