package ttv.migami.jeg.faction.raid;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class RaidEntity extends Entity {
    private static final int ACTIVE_RADIUS = 64;
    private static final int WAVE_MOBS = 20;
    private static final int WAVE_COOLDOWN_TICKS = 120;
    private static final int MAX_DESPAWN_TICKS = 600;
    private static final int BOUNDARY_PARTICLE_STEP_DEGREES = 8;
    private static final int BOUNDARY_PARTICLE_INTERVAL_TICKS = 8;
    private static final int BOUNDARY_VERTICAL_MIN_OFFSET = -10;
    private static final int BOUNDARY_VERTICAL_MAX_OFFSET = 10;
    private static final int BOUNDARY_VERTICAL_STEP = 2;
    private static final int BOUNDARY_PARTICLE_COUNT = 3;

    private final ServerBossEvent bossBar;
    private String factionName = "night_of_the_undead";
    private @Nullable UUID raidId;
    private boolean forceGuns = true;
    private int totalWaves = 3;
    private int currentWave;
    private int waveCooldown = 20;
    private int activeMobCount;
    private int spawnedThisWaveCount;
    private boolean spawningWave;
    private boolean finished;
    private boolean victory;
    private boolean defeat;
    private boolean failedNoTargets;
    private boolean rewardGranted;
    private boolean resultAnnounced;
    private int despawnTicks = MAX_DESPAWN_TICKS;

    public RaidEntity(EntityType<? extends RaidEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
        this.bossBar = new ServerBossEvent(
                UUID.randomUUID(),
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

        refreshPlayers(serverLevel);
        updateBossBar();
        if (!this.finished && this.tickCount % BOUNDARY_PARTICLE_INTERVAL_TICKS == 0) {
            summonParticleVolume(serverLevel);
        }
        if (this.finished) {
            tickFinished(serverLevel);
        }
    }

    public void bindToRaid(@Nullable UUID raidId, String factionName, boolean forceGuns, int totalWaves, int currentWave) {
        this.raidId = raidId;
        this.factionName = factionName;
        this.forceGuns = forceGuns;
        this.totalWaves = totalWaves;
        this.currentWave = currentWave;
    }

    public void syncFromManager(String factionName, int currentWave, int totalWaves, boolean spawningWave,
                                int spawnedThisWaveCount, int activeMobCount, int waveCooldown,
                                boolean finished, boolean victory, boolean defeat, boolean failedNoTargets, boolean rewardGranted) {
        this.factionName = factionName;
        this.currentWave = currentWave;
        this.totalWaves = totalWaves;
        this.spawningWave = spawningWave;
        this.spawnedThisWaveCount = spawnedThisWaveCount;
        this.activeMobCount = activeMobCount;
        this.waveCooldown = waveCooldown;
        this.finished = finished;
        this.victory = victory;
        this.defeat = defeat;
        this.failedNoTargets = failedNoTargets;
        this.rewardGranted = rewardGranted;
        if (finished) {
            this.despawnTicks = Math.min(this.despawnTicks, MAX_DESPAWN_TICKS);
        }
    }

    public @Nullable UUID getRaidId() {
        return this.raidId;
    }

    public String getFactionName() {
        return this.factionName;
    }

    public boolean isForceGuns() {
        return this.forceGuns;
    }

    public int getTotalWaves() {
        return this.totalWaves;
    }

    public int getCurrentWave() {
        return this.currentWave;
    }

    public int getWaveCooldown() {
        return this.waveCooldown;
    }

    public int getSpawnedThisWaveCount() {
        return this.spawnedThisWaveCount;
    }

    public boolean isSpawningWave() {
        return this.spawningWave;
    }

    public boolean isFinishedState() {
        return this.finished;
    }

    public boolean isVictoryState() {
        return this.victory;
    }

    public boolean isDefeatState() {
        return this.defeat;
    }

    public boolean isFailedNoTargetsState() {
        return this.failedNoTargets;
    }

    public boolean isRewardGrantedState() {
        return this.rewardGranted;
    }

    private void refreshPlayers(ServerLevel level) {
        if (this.finished) {
            this.bossBar.removeAllPlayers();
            return;
        }

        AABB zone = this.getBoundingBox().inflate(ACTIVE_RADIUS, ACTIVE_RADIUS, ACTIVE_RADIUS);
        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class, zone, player -> !player.isSpectator() && !player.isDeadOrDying());

        for (ServerPlayer player : nearby) {
            this.bossBar.addPlayer(player);
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && !nearby.contains(player)) {
                this.bossBar.removePlayer(player);
            }
        }
    }

    private void updateBossBar() {
        if (this.finished) {
            this.bossBar.setProgress(this.victory ? 0.0F : 1.0F);
            return;
        }

        float progress;
        if (this.spawningWave || this.spawnedThisWaveCount > 0) {
            int defeated = Math.max(0, this.spawnedThisWaveCount - this.activeMobCount);
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
            Component defeatMessage = this.failedNoTargets
                    ? Component.translatable("broadcast.jeg.raid.no_players").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.RED)
                    : Component.translatable("broadcast.jeg.raid.defeat", factionLang).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.RED);
            level.getServer().getPlayerList().broadcastSystemMessage(defeatMessage, false);
        }
    }

    private void playHorn(ServerLevel level) {
        int x = level.getRandom().nextInt(-25, 25);
        int z = level.getRandom().nextInt(-25, 25);
        level.playSound(null, BlockPos.containing(this.position().add(x, 24, z)), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.HOSTILE, 4.0F, 1.0F);
    }

    private void playCelebrationHorn(ServerLevel level) {
        int x = level.getRandom().nextInt(-25, 25);
        int z = level.getRandom().nextInt(-25, 25);
        int soundIndex = level.getRandom().nextBoolean() ? 1 : 2;
        level.playSound(null, BlockPos.containing(this.position().add(x, 24, z)), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(soundIndex).value(), SoundSource.HOSTILE, 4.0F, 1.0F);
    }

    private void summonParticleVolume(ServerLevel level) {
        for (int yOffset = BOUNDARY_VERTICAL_MIN_OFFSET; yOffset <= BOUNDARY_VERTICAL_MAX_OFFSET; yOffset += BOUNDARY_VERTICAL_STEP) {
            for (int angleDegrees = 0; angleDegrees < 360; angleDegrees += BOUNDARY_PARTICLE_STEP_DEGREES) {
                double angle = Math.toRadians(angleDegrees);
                double xOffset = Math.cos(angle) * ACTIVE_RADIUS;
                double zOffset = Math.sin(angle) * ACTIVE_RADIUS;
                level.sendParticles(
                        ParticleTypes.FLAME,
                        true,
                        false,
                        this.getX() + xOffset,
                        this.getY() + yOffset,
                        this.getZ() + zOffset,
                        BOUNDARY_PARTICLE_COUNT,
                        0.08D,
                        0.08D,
                        0.08D,
                        0.0D
                );
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (this.raidId != null) {
            output.putString("RaidId", this.raidId.toString());
        }
        output.putString("FactionName", this.factionName);
        output.putBoolean("ForceGuns", this.forceGuns);
        output.putBoolean("Finished", this.finished);
        output.putBoolean("Victory", this.victory);
        output.putBoolean("Defeat", this.defeat);
        output.putBoolean("FailedNoTargets", this.failedNoTargets);
        output.putBoolean("RewardGranted", this.rewardGranted);
        output.putBoolean("SpawningWave", this.spawningWave);
        output.putInt("CurrentWave", this.currentWave);
        output.putInt("TotalWaves", this.totalWaves);
        output.putInt("WaveCooldown", this.waveCooldown);
        output.putInt("SpawnedThisWaveCount", this.spawnedThisWaveCount);
        output.putInt("ActiveMobCount", this.activeMobCount);
        output.putInt("DespawnTicks", this.despawnTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String raidIdValue = input.getStringOr("RaidId", "");
        this.raidId = raidIdValue.isBlank() ? null : UUID.fromString(raidIdValue);
        this.factionName = input.getStringOr("FactionName", this.factionName);
        this.forceGuns = input.getBooleanOr("ForceGuns", true);
        this.finished = input.getBooleanOr("Finished", false);
        this.victory = input.getBooleanOr("Victory", false);
        this.defeat = input.getBooleanOr("Defeat", false);
        this.failedNoTargets = input.getBooleanOr("FailedNoTargets", false);
        this.rewardGranted = input.getBooleanOr("RewardGranted", false);
        this.spawningWave = input.getBooleanOr("SpawningWave", false);
        this.currentWave = input.getIntOr("CurrentWave", 0);
        this.totalWaves = input.getIntOr("TotalWaves", 3);
        this.waveCooldown = input.getIntOr("WaveCooldown", 20);
        this.spawnedThisWaveCount = input.getIntOr("SpawnedThisWaveCount", 0);
        this.activeMobCount = input.getIntOr("ActiveMobCount", 0);
        this.despawnTicks = input.getIntOr("DespawnTicks", MAX_DESPAWN_TICKS);
    }

    public static void notifyRaidMobFired(Mob mob) {
        FactionRaidManager.notifyRaidMobFired(mob);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
