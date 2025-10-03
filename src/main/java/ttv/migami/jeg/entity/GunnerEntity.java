package ttv.migami.jeg.entity;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;

public class GunnerEntity extends Monster {
    private static final double ATTACK_RANGE = 30.0D;
    private static final ResourceLocation DEFAULT_GUN = Reference.id("assault_rifle");
    private static final ResourceLocation DEFAULT_AMMO = Reference.id("rifle_ammo");

    private GunStats cachedStats;
    private int magazine;
    private int reloadTicks;
    private int fireCooldown;

    public GunnerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        this.magazine = 0;
        this.reloadTicks = 0;
        this.fireCooldown = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new GunnerShootGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        ItemStack gunStack = ItemStack.EMPTY;
        DeferredHolder<Item, GunItem> holder = ModItems.GUNS.get(DEFAULT_GUN);
        if (holder != null) {
            gunStack = new ItemStack(holder.get());
            configureLoadout(holder.get().getStats(), gunStack);
        }
        this.setItemInHand(InteractionHand.MAIN_HAND, gunStack);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        int bulletDrop = this.random.nextInt(3);
        if (bulletDrop > 0) {
            DeferredHolder<Item, Item> ammoHolder = ModItems.AMMO.get(DEFAULT_AMMO);
            Item ammoItem = ammoHolder != null
                    ? ammoHolder.get()
                    : BuiltInRegistries.ITEM.getOptional(DEFAULT_AMMO).orElse(null);
            if (ammoItem != null) {
                this.spawnAtLocation(level, new ItemStack(ammoItem, bulletDrop));
            }
        }

        float chance = 0.02F;
        if (this.random.nextFloat() < chance) {
            DeferredHolder<Item, GunItem> holder = ModItems.GUNS.get(DEFAULT_GUN);
            if (holder != null) {
                ItemStack gun = new ItemStack(holder.get());
                int maxDamage = gun.getMaxDamage();
                int remaining = Math.max(1, maxDamage / 12);
                gun.setDamageValue(Math.max(1, maxDamage - remaining));
                this.spawnAtLocation(level, gun);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        tickCombatTimers();
    }

    public void shootAt(LivingEntity target) {
        ItemStack gunStack = this.getMainHandItem();
        if (!(gunStack.getItem() instanceof GunItem gun)) {
            return;
        }

        GunStats stats = getEquippedGunStats().orElseGet(gun::getStats);
        gun.fireAt(this.level(), this, gunStack, target);
        playGunshotSound(stats);
        gunStack.hurtAndBreak(1, this, InteractionHand.MAIN_HAND);
        this.gameEvent(GameEvent.ENTITY_ACTION);

        if (stats.usesMagazine()) {
            this.magazine = Math.max(0, this.magazine - 1);
            gunStack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
            if (this.magazine <= 0) {
                startReload(stats, gunStack);
            }
        } else {
            this.fireCooldown = Math.max(6, stats.fireDelay());
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return data;
    }

    private static class GunnerShootGoal extends Goal {
        private final GunnerEntity mob;
        private int cooldown;

        private GunnerShootGoal(GunnerEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.cooldown = 10;
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) {
                return;
            }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = mob.distanceToSqr(target);
            boolean canSee = mob.getSensing().hasLineOfSight(target);

            if (canSee && distance < ATTACK_RANGE * ATTACK_RANGE) {
                if (mob.isReloading()) {
                    return;
                }

                if (mob.readyToShoot() && --cooldown <= 0) {
                    mob.shootAt(target);
                    cooldown = Math.max(6, mob.getCurrentFireDelay());
                }
            } else {
                mob.getNavigation().moveTo(target, 1.1D);
                cooldown = Math.max(cooldown, 6);
            }
        }
    }

    private void configureLoadout(GunStats stats, ItemStack stack) {
        this.cachedStats = stats;
        if (stats.usesMagazine()) {
            this.magazine = stats.magazineSize();
            stack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
        } else {
            this.magazine = Math.max(1, stats.projectileAmount());
        }
        this.fireCooldown = 0;
        this.reloadTicks = 0;
    }

    private Optional<GunStats> getEquippedGunStats() {
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

    public void equipGun(ResourceLocation id) {
        DeferredHolder<Item, GunItem> holder = ModItems.GUNS.get(id);
        if (holder == null) {
            return;
        }

        ItemStack stack = new ItemStack(holder.get());
        configureLoadout(holder.get().getStats(), stack);
        this.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private void startReload(GunStats stats, ItemStack stack) {
        int reload = Math.max(20, stats.totalReloadTime());
        this.reloadTicks = reload;
        stats.reloadStartSoundEvent().ifPresent(sound -> this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.HOSTILE, 1.0F, 1.0F));
        this.fireCooldown = Math.max(this.fireCooldown, reload / 4);
        this.magazine = Math.max(0, this.magazine);
        stack.set(ModDataComponents.GUN_AMMO.get(), 0);
    }

    private void finishReload(ItemStack stack) {
        GunStats stats = getEquippedGunStats().orElse(null);
        if (stats == null) {
            return;
        }

        if (stats.usesMagazine()) {
            this.magazine = stats.magazineSize();
            stack.set(ModDataComponents.GUN_AMMO.get(), this.magazine);
        } else {
            this.magazine = Math.max(1, stats.projectileAmount());
        }
        stats.reloadEndSoundEvent().ifPresent(sound -> this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.HOSTILE, 1.0F, 1.0F));
    }

    private void playGunshotSound(GunStats stats) {
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> this.level().playSound(null, this, sound, SoundSource.HOSTILE, 1.0F, 0.9F + this.random.nextFloat() * 0.2F),
                () -> this.level().playSound(null, this, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.9F + this.random.nextFloat() * 0.2F)
        );
    }

    private void tickCombatTimers() {
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

    private boolean isReloading() {
        return this.reloadTicks > 0;
    }

    private boolean readyToShoot() {
        if (this.isReloading()) {
            return false;
        }
        if (this.fireCooldown > 0) {
            return false;
        }
        if (this.cachedStats == null) {
            return true;
        }
        if (!this.cachedStats.usesMagazine()) {
            return true;
        }
        return this.magazine > 0;
    }

    private int getCurrentFireDelay() {
        GunStats stats = getEquippedGunStats().orElse(null);
        if (stats == null) {
            return 20;
        }
        int delay = Math.max(4, stats.fireDelay());
        this.fireCooldown = delay;
        return delay;
    }
}
