package ttv.migami.jeg.entity.monster.phantom;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;

/**
 * Shared behaviour for Terror Phantom variants.
 */
public abstract class AbstractTerrorPhantom extends Phantom implements GeoEntity {
    private static final float MIN_DAMAGE_TO_HURT = 2.0F;
    private static final int RAPID_FIRE_RESISTANCE_RESET_TICKS = 25;
    private static final int RAPID_FIRE_RESISTANCE_WARMUP_HITS = 10;
    private static final float RAPID_FIRE_MINIGUN_DAMAGE_MULTIPLIER = 0.18F;
    private static final float RAPID_FIRE_LMG_DAMAGE_MULTIPLIER = 0.35F;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTerrorPhantom.class);
    private static final int SUMMON_INTERVAL_TICKS = 200;
    private static final int MAX_ACTIVE_GUNNERS = 6; // Increased from 3 to 6 for more intense battles
    private static final double SUMMON_RANGE = 48.0D;
    private static final int PHANTOM_SIZE = 4;
    private static final int IDLE_HEAL_DELAY_TICKS = 20 * 60;
    private static final int IDLE_HEAL_INTERVAL_TICKS = 40;
    private static final float IDLE_HEAL_AMOUNT = 1.0F;
    private static final int DEFAULT_TERROR_PHANTOM_PROJECTILE_PROTECTION_LEVEL = 5;
    private static final long DESPAWN_IF_UNHIT_TICKS = 24000L;
    private static final String TAG_LAST_ATTACKED_GAME_TIME = "LastAttackedGameTime";
    private static final int TARGET_REACQUIRE_INTERVAL_TICKS = 20;
    private static final double TARGET_REACQUIRE_RANGE = 96.0D;
    private int summonCooldown = SUMMON_INTERVAL_TICKS;
    private final ServerBossEvent bossInfo = new ServerBossEvent(
            java.util.UUID.randomUUID(),
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );
    private GunStats cachedStats;
    protected int magazine;
    private int reloadTicks;
    protected int fireCooldown;
    private int sustainedFireShots;
    private int ticksSinceLastDamage = IDLE_HEAL_DELAY_TICKS;
    private long lastAttackedGameTime = Long.MIN_VALUE;
    private long lastRapidFireResistanceHitTick = Long.MIN_VALUE;
    private int rapidFireResistanceHitCount;

    private static final String GECKO_CONTROLLER = "Idle";
    private static final RawAnimation GECKO_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation GECKO_DYING = RawAnimation.begin().thenPlay("dying");

    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractTerrorPhantom(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
        this.xpReward = 35;
        this.setPhantomSize(PHANTOM_SIZE);
        this.bossInfo.setDarkenScreen(false);
        equipDefaultGun();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                GECKO_CONTROLLER,
                0,
                state -> {
                    if (isDying()) {
                        return state.setAndContinue(GECKO_DYING);
                    }
                    state.setAndContinue(GECKO_IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    protected Identifier defaultGunId() {
        return Reference.id("light_machine_gun");
    }

    protected boolean canPerformBombing() {
        return true;
    }

    protected float bombExplosionPower() {
        return 3.0F;
    }

    protected int bombFuseTicks() {
        return 80;
    }

    protected void equipDefaultGun() {
        Identifier gunId = defaultGunId();
        if (gunId == null) {
            return;
        }
        var holder = ModItems.GUNS.get(gunId);
        if (holder == null) {
            return;
        }
        GunItem gun = holder.get();
        ItemStack stack = new ItemStack(gun);
        configureLoadout(gun.getStats(), stack);
        this.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private void configureLoadout(GunStats stats, ItemStack stack) {
        this.cachedStats = stats;
        if (usesLoadedAmmo(stack, stats)) {
            this.magazine = getLoadedAmmoCapacity(stats);
            stack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
        } else {
            this.magazine = Math.max(1, stats.projectileAmount());
        }
        this.reloadTicks = 0;
        this.fireCooldown = 0;
        this.sustainedFireShots = 0;
    }

    protected int getLoadedAmmoCapacity(GunStats stats) {
        return stats.magazineSize();
    }

    protected Optional<GunStats> getEquippedGunStats() {
        if (this.cachedStats != null) {
            return Optional.of(this.cachedStats);
        }
        ItemStack stack = this.getMainHandItem();
        if (stack.getItem() instanceof GunItem gun) {
            this.cachedStats = gun.getStats();
            return Optional.of(this.cachedStats);
        }
        return Optional.empty();
    }

    protected void shootAt(LivingEntity target) {
        ItemStack stack = this.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        GunStats stats = getEquippedGunStats().orElseGet(gun::getStats);

        // Add spread to phantom shooting for balance (compensates for high fire rate)
        Vec3 targetPos = target.getEyePosition();
        Vec3 myPos = this.getEyePosition();
        Vec3 aimDirection = targetPos.subtract(myPos).normalize();

        // Apply random spread - increase spread for more challenging combat
        double spreadAmount = 0.03D; // Slightly reduced spread for tighter volleys
        double spreadX = (this.random.nextDouble() - 0.5) * spreadAmount;
        double spreadY = (this.random.nextDouble() - 0.5) * spreadAmount;
        double spreadZ = (this.random.nextDouble() - 0.5) * spreadAmount;

        Vec3 spread = new Vec3(spreadX, spreadY, spreadZ);
        Vec3 finalDirection = aimDirection.add(spread).normalize();

        gun.fireDirectionally(this.level(), this, stack, finalDirection);

        // Spawn bullet trail particles (fire + smoke) for 6 ticks (0.3 seconds)
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 start = this.getEyePosition();
            Vec3 end = target.getEyePosition();
            Vec3 direction = end.subtract(start).normalize();

            // Spawn particles along the bullet path
            double distance = start.distanceTo(end);
            int particleCount = Math.min(20, (int) (distance / 2.0D));
            for (int i = 0; i < particleCount; i++) {
                double fraction = (double) i / particleCount;
                Vec3 particlePos = start.add(direction.scale(distance * fraction));

                // Fire particles
                serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.02D, 0.02D, 0.02D, 0.01D
                );

                // Smoke particles
                serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.02D, 0.02D, 0.02D, 0.01D
                );
            }
        }

        playGunshotSound(stats);
        stack.hurtAndBreak(1, this, InteractionHand.MAIN_HAND);
        this.gameEvent(GameEvent.ENTITY_ACTION);

        boolean startedReload = false;
        if (usesLoadedAmmo(stack, stats)) {
            this.magazine = Math.max(0, this.magazine - 1);
            stack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
            if (this.magazine <= 0) {
                this.sustainedFireShots = 0;
                startReload(stats, stack);
                startedReload = true;
            }
        }
        if (!startedReload) {
            applyPostShotCooldown();
        }
    }

    protected void playGunshotSound(GunStats stats) {
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> this.level().playSound(null, this, sound, SoundSource.HOSTILE, 7.5F, 0.9F + this.random.nextFloat() * 0.2F),
                () -> this.level().playSound(null, this, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 7.5F, 0.9F + this.random.nextFloat() * 0.2F)
        );
    }

    protected boolean isReloading() {
        return this.reloadTicks > 0;
    }

    protected boolean readyToShoot() {
        if (this.isReloading()) {
            return false;
        }
        if (this.fireCooldown > 0) {
            return false;
        }
        ItemStack stack = this.getMainHandItem();
        GunStats stats = getEquippedGunStats().orElse(null);
        if (!usesLoadedAmmo(stack, stats)) {
            return true;
        }
        return this.magazine > 0;
    }

    protected int getCurrentFireDelay() {
        return 4;
    }

    protected int getSustainedFireShotLimit() {
        return 0;
    }

    protected int getSustainedFireCooldown() {
        return getCurrentFireDelay();
    }

    protected void applyPostShotCooldown() {
        int shotLimit = getSustainedFireShotLimit();
        if (shotLimit > 0 && ++this.sustainedFireShots >= shotLimit) {
            this.sustainedFireShots = 0;
            this.fireCooldown = getSustainedFireCooldown();
            return;
        }
        this.fireCooldown = getCurrentFireDelay();
    }

    protected void startReload(GunStats stats, ItemStack stack) {
        this.reloadTicks = Math.max(20, stats.totalReloadTime());
        stack.set(ModDataComponents.GUN_AMMO.get(), 0);
        stats.reloadStartSoundEvent().ifPresent(sound -> this.level().playSound(null, this, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
    }

    protected void finishReload(ItemStack stack) {
        GunStats stats = getEquippedGunStats().orElse(null);
        if (stats == null) {
            return;
        }
        if (usesLoadedAmmo(stack, stats)) {
            this.magazine = getLoadedAmmoCapacity(stats);
            stack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
        } else {
            this.magazine = Math.max(1, stats.projectileAmount());
        }
        this.sustainedFireShots = 0;
        stats.reloadEndSoundEvent().ifPresent(sound -> this.level().playSound(null, this, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
        this.fireCooldown = Math.max(6, stats.fireDelay());
    }

    protected boolean usesLoadedAmmo(ItemStack stack, @Nullable GunStats stats) {
        return stats != null && stack.getItem() instanceof GunItem gun && gun.usesLoadedAmmo();
    }

    protected void tickCombatTimers() {
        if (this.fireCooldown > 0) {
            this.fireCooldown--;
        }
        if (this.reloadTicks > 0) {
            this.reloadTicks--;
            if (this.reloadTicks == 0) {
                finishReload(this.getMainHandItem());
            }
        }
    }

    protected void throwGrenadeAt(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        GrenadeEntity grenade = new GrenadeEntity(serverLevel, this, bombExplosionPower(), bombFuseTicks(), true);
        Vec3 origin = this.position().add(this.getForward().scale(2.0D)).add(0.0D, -0.5D, 0.0D);
        grenade.initialisePosition(origin);

        Vec3 aim = target.getEyePosition().subtract(origin);
        if (!aim.equals(Vec3.ZERO)) {
            grenade.setDeltaMovement(aim.normalize().scale(0.7D));
        }

        serverLevel.addFreshEntity(grenade);
        serverLevel.playSound(null, this, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.2F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(160.0D);
    }

    public static AttributeSupplier.Builder createAttributes(double maxHealth) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, maxHealth)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 80.0D) // Increased from 52 to 80 for better player detection
                .add(Attributes.ARMOR, 6.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        long gameTime = serverLevel.getGameTime();
        if (this.lastAttackedGameTime == Long.MIN_VALUE) {
            this.lastAttackedGameTime = gameTime;
        }

        if (this.getPhantomSize() != PHANTOM_SIZE) {
            this.setPhantomSize(PHANTOM_SIZE);
        }

        if (this.summonCooldown > 0) {
            this.summonCooldown--;
        }

        if (this.ticksSinceLastDamage < Integer.MAX_VALUE) {
            this.ticksSinceLastDamage++;
        }

        if (this.ticksSinceLastDamage >= IDLE_HEAL_DELAY_TICKS && this.tickCount % IDLE_HEAL_INTERVAL_TICKS == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(IDLE_HEAL_AMOUNT);
        }

        if (this.getType() == ModEntities.TERROR_PHANTOM.get() && gameTime - this.lastAttackedGameTime >= DESPAWN_IF_UNHIT_TICKS) {
            this.discard();
            return;
        }

        refreshCombatTarget(serverLevel);
        LivingEntity target = this.getTarget();
        if (target != null) {
            trySummonReinforcements(serverLevel, target);
        }
    }

    private void refreshCombatTarget(ServerLevel level) {
        LivingEntity current = this.getTarget();
        if (!isInvalidCombatTarget(current)) {
            return;
        }

        if (current != null) {
            super.setTarget(null);
        }

        if (this.tickCount % TARGET_REACQUIRE_INTERVAL_TICKS != 0) {
            return;
        }

        Player reacquired = findClosestAttackablePlayer(level);
        if (reacquired != null) {
            super.setTarget(reacquired);
        }
    }

    private boolean isInvalidCombatTarget(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return true;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        return !this.canAttack(target);
    }

    @Nullable
    private Player findClosestAttackablePlayer(ServerLevel level) {
        Player bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (Player candidate : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(TARGET_REACQUIRE_RANGE))) {
            if (isInvalidCombatTarget(candidate)) {
                continue;
            }

            double distance = this.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.getType() == ModEntities.TERROR_PHANTOM.get() && amount < MIN_DAMAGE_TO_HURT) {
            return false;
        }
        float adjustedAmount = applyDefaultProjectileProtectionReduction(source, amount);
        adjustedAmount = applyRapidFireResistance(level, source, adjustedAmount);
        boolean damaged = super.hurtServer(level, source, adjustedAmount);
        if (damaged && amount > 0.0F) {
            this.ticksSinceLastDamage = 0;
            this.lastAttackedGameTime = level.getGameTime();
        }
        return damaged;
    }

    private float applyDefaultProjectileProtectionReduction(DamageSource source, float amount) {
        if (this.getType() != ModEntities.TERROR_PHANTOM.get()) {
            return amount;
        }
        if (isRocketDirectDamageSource(source)) {
            return amount;
        }
        if (!source.is(DamageTypeTags.IS_PROJECTILE)) {
            return amount;
        }

        // Match vanilla projectile protection scaling: EPF = level * 2, capped at 20; reduction = EPF / 25.
        int epf = Math.min(20, DEFAULT_TERROR_PHANTOM_PROJECTILE_PROTECTION_LEVEL * 2);
        float reduction = (float) epf / 25.0F;
        return amount * (1.0F - reduction);
    }

    private static boolean isRocketDirectDamageSource(DamageSource source) {
        if (!(source.getDirectEntity() instanceof BulletEntity bullet)) {
            return false;
        }
        return "rocket_launcher".equals(bullet.getGunStats().id().getPath());
    }

    private float applyRapidFireResistance(ServerLevel level, DamageSource source, float amount) {
        if (amount <= 0.0F || !isRapidFireResistanceTarget()) {
            return amount;
        }
        if (!(source.getDirectEntity() instanceof BulletEntity bullet)) {
            return amount;
        }
        if (!(bullet.getOwner() instanceof Player player) || player.isCreative() || player.isSpectator()) {
            return amount;
        }

        String gunPath = bullet.getGunStats().id().getPath();
        float multiplier;
        if ("minigun".equals(gunPath)) {
            multiplier = RAPID_FIRE_MINIGUN_DAMAGE_MULTIPLIER;
        } else if ("light_machine_gun".equals(gunPath)) {
            multiplier = RAPID_FIRE_LMG_DAMAGE_MULTIPLIER;
        } else {
            return amount;
        }

        long gameTime = level.getGameTime();
        if (this.lastRapidFireResistanceHitTick == Long.MIN_VALUE
                || gameTime - this.lastRapidFireResistanceHitTick > RAPID_FIRE_RESISTANCE_RESET_TICKS) {
            this.rapidFireResistanceHitCount = 0;
        }
        this.lastRapidFireResistanceHitTick = gameTime;
        this.rapidFireResistanceHitCount++;

        if (this.rapidFireResistanceHitCount <= RAPID_FIRE_RESISTANCE_WARMUP_HITS) {
            return amount;
        }
        return amount * multiplier;
    }

    private boolean isRapidFireResistanceTarget() {
        return this.getType() == ModEntities.TERROR_PHANTOM.get()
                || this.getType() == ModEntities.TERROR_PHANTOM_GUARDIAN.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong(TAG_LAST_ATTACKED_GAME_TIME, this.lastAttackedGameTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lastAttackedGameTime = input.getLongOr(TAG_LAST_ATTACKED_GAME_TIME, Long.MIN_VALUE);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (shouldSpawnGunnerSkeletonsOnDeath()) {
            spawnGunnerSkeletons(serverLevel);
        }

        if (shouldSpawnPhantomGunnersOnDeath()) {
            spawnPhantomGunners(serverLevel);
        }

        if (shouldTriggerRaidOnDeath()) {
            onDefeated(serverLevel, source);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor accessor, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData spawnGroupData = super.finalizeSpawn(accessor, difficulty, reason, data);
        if (accessor instanceof ServerLevel serverLevel) {
            this.onSpawned(serverLevel, this.blockPosition());
            this.lastAttackedGameTime = serverLevel.getGameTime();
        }
        this.setPersistenceRequired();
        return spawnGroupData;
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        tickCombatTimers();
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossInfo.setName(this.getDisplayName());
    }

    protected void trySummonReinforcements(ServerLevel level, LivingEntity target) {
        if (this.summonCooldown > 0) {
            return;
        }

        List<PhantomGunner> existing = level.getEntitiesOfClass(
                PhantomGunner.class,
                new AABB(this.blockPosition()).inflate(SUMMON_RANGE)
        );
        if (existing.size() >= MAX_ACTIVE_GUNNERS) {
            this.summonCooldown = Mth.nextInt(this.random, 120, 240);
            return;
        }

        PhantomGunner gunner = new PhantomGunnerMinion(ModEntities.PHANTOM_GUNNER_MINION.get(), level);
        if (gunner == null) {
            this.summonCooldown = Mth.nextInt(this.random, 120, 200);
            return;
        }

        Vec3 spawnPos = this.position().add(
                (this.random.nextDouble() - 0.5D) * 12.0D,
                -4.0D,
                (this.random.nextDouble() - 0.5D) * 12.0D
        );
        gunner.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        gunner.setYRot(this.getYRot());
        gunner.setXRot(this.getXRot());
        gunner.yRotO = gunner.getYRot();
        gunner.xRotO = gunner.getXRot();
        gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spawnPos)), EntitySpawnReason.EVENT, null);
        gunner.setTarget(target);
        level.addFreshEntity(gunner);
        this.summonCooldown = Mth.nextInt(this.random, 200, 320);
    }

    private void spawnGunnerSkeletons(ServerLevel level) {
        int count = 2 + this.random.nextInt(2);
        BlockPos origin = this.blockPosition();

        // Find and target nearest player
        LivingEntity target = level.getNearestEntity(Player.class, TargetingConditions.DEFAULT, null,
            origin.getX(), origin.getY(), origin.getZ(), new AABB(origin).inflate(64.0D));

        for (int i = 0; i < count; i++) {
            Skeleton skeleton = new Skeleton(EntityTypes.SKELETON, level);
            if (skeleton == null) {
                continue;
            }
            BlockPos spawnPos = findSupportPosition(level, origin, this.random);
            skeleton.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
            skeleton.setYRot(this.random.nextFloat() * 360.0F);
            skeleton.yRotO = skeleton.getYRot();
            skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.EVENT, null);
            skeleton.addTag(GunEvents.JEG_GUNNER_TAG);            // JEG faction system will handle gun equipping automatically
            prepareSkeletonForDaylight(skeleton);

            // Set target to nearest player
            if (target != null) {
                skeleton.setTarget(target);
            }

            level.addFreshEntity(skeleton);
        }
        playSummonEffects(level, origin.above());
    }

    private BlockPos findSupportPosition(ServerLevel level, BlockPos origin, RandomSource random) {
        int dx = random.nextInt(7) - 3;
        int dz = random.nextInt(7) - 3;
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX() + dx, origin.getZ() + dz);
        return new BlockPos(origin.getX() + dx, surfaceY, origin.getZ() + dz);
    }

    protected abstract boolean shouldSpawnGunnerSkeletonsOnDeath();

    protected boolean shouldSpawnPhantomGunnersOnDeath() {
        return false;
    }

    protected boolean shouldTriggerRaidOnDeath() {
        return false;
    }

    protected void onDefeated(ServerLevel level, DamageSource source) {
        // hook for subclasses
    }

    protected static void prepareSkeletonForDaylight(Skeleton skeleton) {
        skeleton.setPersistenceRequired();
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        skeleton.setItemSlot(EquipmentSlot.HEAD, helmet);
        skeleton.setDropChance(EquipmentSlot.HEAD, 0.0F);
        skeleton.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 120, 0, false, false));
        skeleton.extinguishFire();
    }

    private void spawnPhantomGunners(ServerLevel level) {
        int count = 1 + this.random.nextInt(2);
        BlockPos origin = this.blockPosition();

        // Find and target nearest player
        LivingEntity target = level.getNearestEntity(Player.class, TargetingConditions.DEFAULT, null,
            origin.getX(), origin.getY(), origin.getZ(), new AABB(origin).inflate(64.0D));

        for (int i = 0; i < count; i++) {
            PhantomGunner gunner = new PhantomGunnerMinion(ModEntities.PHANTOM_GUNNER_MINION.get(), level);
            if (gunner == null) {
                continue;
            }

            Vec3 offset = Vec3.directionFromRotation(0.0F, this.random.nextFloat() * 360.0F).scale(6.0D + this.random.nextDouble() * 4.0D);
            Vec3 spawnCenter = this.position().add(offset.x, 4.0D + this.random.nextInt(4), offset.z);
            BlockPos spawnPos = BlockPos.containing(spawnCenter);
            gunner.setPos(spawnCenter.x, spawnCenter.y, spawnCenter.z);
            gunner.setYRot(this.random.nextFloat() * 360.0F);
            gunner.yRotO = gunner.getYRot();
            gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.EVENT, null);

            // Set target to nearest player
            if (target != null) {
                gunner.setTarget(target);
            }

            level.addFreshEntity(gunner);
        }

        playSummonEffects(level, this.blockPosition().above(4));
    }

    private void playSummonEffects(ServerLevel level, BlockPos center) {
        level.playSound(null, center, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.8F, 0.6F + this.random.nextFloat() * 0.3F);
        for (int i = 0; i < 12; i++) {
            double dx = center.getX() + 0.5D + (this.random.nextDouble() - 0.5D) * 4.0D;
            double dy = center.getY() + this.random.nextDouble() * 2.0D;
            double dz = center.getZ() + 0.5D + (this.random.nextDouble() - 0.5D) * 4.0D;
            level.sendParticles(ParticleTypes.SMOKE, dx, dy, dz, 1, 0.0D, 0.05D, 0.0D, 0.02D);
        }
    }

    protected void onSpawned(ServerLevel level, BlockPos spawnPos) {
        // hook for subclasses
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !this.canAttack(target)) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // Don't attack creative mode players
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        // Don't attack own summoned Phantom Gunners
        if (target instanceof PhantomGunner) {
            return false;
        }

        return super.canAttack(target);
    }

    protected abstract Identifier getVariantTexture();

    protected float getModelScale() {
        return 1.6F;
    }

    public Identifier getRenderTexture() {
        return getVariantTexture();
    }

    public float getRenderScale() {
        return getModelScale();
    }

    public final Identifier getGeoTexture() {
        return getVariantTexture();
    }

    public final float getGeoScale() {
        return getModelScale();
    }

    public abstract boolean isRolling();

    public abstract boolean isDying();

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossInfo.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide()) {
            this.bossInfo.removeAllPlayers();
        }
    }
}
