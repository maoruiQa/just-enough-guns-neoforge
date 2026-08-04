package ttv.migami.jeg.faction;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import ttv.migami.jeg.fabric.compat.neoforge.bus.api.SubscribeEvent;
import ttv.migami.jeg.fabric.compat.neoforge.fml.common.Mod;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.EntityJoinLevelEvent;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.tick.EntityTickEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.ai.AIType;
import ttv.migami.jeg.entity.ai.GunAttackGoal;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.faction.GunnerArmorEquiper;
import ttv.migami.jeg.faction.patrol.PatrolEncounterManager;
import ttv.migami.jeg.faction.raid.FactionRaidHooks;
import ttv.migami.jeg.faction.raid.RaidEntity;
import ttv.migami.jeg.entity.monster.phantom.TerrorRaidHooks;
import ttv.migami.jeg.mixin.MobAccessor;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleController;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleSpawner;

import java.util.UUID;

public class GunnerMobSpawner {
    public static final UUID GUN_FOLLOW_RANGE_MODIFIER_UUID = UUID.randomUUID();
    public static final Identifier GUN_FOLLOW_RANGE_MODIFIER_ID = Reference.id("gun_follow_range_modifier");
    private static final Identifier PARCHED_ID = Identifier.parse("minecraft:parched");
    private static final String GUNNER_SPAWN_CHECKED_TAG = "jeg_gunner_spawn_checked";

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        ItemStack heldItem = mob.getMainHandItem();

        if (heldItem.getItem() instanceof GunItem) {
            enforceGunnerMainHandLock(mob);
            LivingEntity currentTarget = mob.getTarget();
            if (currentTarget instanceof AbstractTerrorPhantom) {
                mob.setTarget(null);
            }
            reassessWeaponGoal(mob);
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        GunnerFriendlyFireEvents.clearIgnoredVehicleStrikeTarget(mob);

        if (!GunMobValues.enabled) {
            return;
        }

        if (mob.entityTags().contains(EnemyVehicleController.ENEMY_VEHICLE_CREW_TAG)) {
            return;
        }

        if (mob.tickCount >= 2) {
            return;
        }

        ItemStack heldItem = mob.getMainHandItem();

        // Alternative approach for piglin zombification immunity - since PiglinAi.rideInBoat() doesn't exist
        if (mob.entityTags().contains("MobGunner") && mob instanceof AbstractPiglin abstractPiglin && abstractPiglin.level().dimension() == Level.OVERWORLD) {
            // Give piglin fire resistance to prevent zombification in overworld
            abstractPiglin.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60, 0, false, true));
        }

        // Normalize tagged gunners before assigning their weapon / bomber kit.
        if (mob.entityTags().contains("MobGunner")
                && !(heldItem.getItem() instanceof GunItem)
                && !BomberGunnerHelper.isBomber(mob)
                && !mob.entityTags().contains("GunAttackAssigned")) {
            normalizeGunnerMob(mob);
            if (mob.isBaby()) {
                return;
            }

            GunnerManager manager = new GunnerManager(GunnerManager.getConfigFactions());
            String entityName = mob.getType().getDescriptionId().replace("entity.", "").replace(".", ":");
            Identifier entityTypeLocation = Identifier.tryParse(entityName);
            Faction faction = manager.getFactionForMob(entityTypeLocation);

            if (faction != null) {
                String gunnerType = GunnerType.keyFor(mob);
                boolean bomber = Config.shouldGunnerBecomeBomber(mob.level(), gunnerType, mob.getRandom());
                boolean elite = GunMobValues.rollElite(mob.level(), mob.getRandom());
                if (elite) {
                    applyEliteAttributes(mob);
                }

                if (elite) {
                    GunnerArmorEquiper.equipGunnerArmor(mob.getRandom(), GunnerArmorEquiper.GunnerArmorContext.elite(mob));
                } else {
                    GunnerArmorEquiper.equipGunnerArmor(mob.getRandom(), GunnerArmorEquiper.GunnerArmorContext.normal(mob));
                }

                if (bomber) {
                    if (!mob.level().isClientSide()) {
                        if (!hasTargetGoal(mob)) {
                            getTargetSelector(mob).addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, true));
                        }
                        BomberGunnerHelper.applyBomberKit(mob, getGoalSelector(mob));
                        mob.addTag("GunAttackAssigned");
                    }
                    extendFollowRange(mob);
                    return;
                }

                boolean isCloseRange = mob.getRandom().nextBoolean();
                int stopRange = isCloseRange ? 7 : 20;
                Item gun = faction.getRandomGun(isCloseRange, mob.level(), mob.getRandom(), gunnerType, true);
                AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
                int aiLevel = faction.getAiLevel() + (elite ? 1 : 0);

                if (elite) {
                    gun = faction.getEliteGun(mob.level(), mob.getRandom(), gunnerType, true);
                }

                if (gun == null) {
                    return;
                }

                if (!mob.level().isClientSide() && !hasGunAttackGoal(mob)) {
                    if (!hasTargetGoal(mob)) {
                        getTargetSelector(mob).addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, true));
                    }
                    getGoalSelector(mob).addGoal(2, new GunAttackGoal<>(mob, stopRange, 1.2F, aiType, aiLevel));
                    mob.addTag("GunAttackAssigned");
                }

                ItemStack modifiedGun = createModifiedGun(mob, gun);
                mob.setItemSlot(EquipmentSlot.MAINHAND, modifiedGun);
                suppressSkeletonSniperDrop(mob, gun);
                enforceGunnerMainHandLock(mob);
                extendFollowRange(mob);
            }
        }

        if (BomberGunnerHelper.isBomber(mob)) {
            BomberGunnerHelper.ensureBomberGoal(mob, getGoalSelector(mob));
        } else if (heldItem.getItem() instanceof GunItem) {
            LivingEntity currentTarget = mob.getTarget();
            if (currentTarget instanceof AbstractTerrorPhantom) {
                mob.setTarget(null);
            }
            reassessWeaponGoal(mob);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof RaidEntity raidEntity) {
            FactionRaidHooks.recoverRaidAnchor(raidEntity);
        }

        if (!GunMobValues.enabled) {
            return;
        }

        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (mob.entityTags().contains(EnemyVehicleController.ENEMY_VEHICLE_CREW_TAG)) {
            return;
        }

        if (mob.level() instanceof ServerLevel serverLevel) {
            PatrolEncounterManager.recoverPatrolMob(serverLevel, mob);
            FactionRaidHooks.recoverRaidMob(mob);
            TerrorRaidHooks.recoverRaidMob(mob);
        }

        mob.removeTag("GunAttackAssigned");

        // Check if this mob should become a gunner (only for newly spawned mobs)
        if (mob.tickCount <= 5 && !mob.entityTags().contains("MobGunner") && !mob.entityTags().contains(GUNNER_SPAWN_CHECKED_TAG) && getFactionForMob(mob) != null) {
            mob.addTag(GUNNER_SPAWN_CHECKED_TAG);
            if (!mob.isBaby()) {
                double gunnerChance = resolveNaturalGunnerChance(mob);
                if (gunnerChance > 0.0D && mob.getRandom().nextDouble() < gunnerChance) {
                    Faction faction = getFactionForMob(mob);

                    if (faction != null) {
                        normalizeGunnerMob(mob);
                        mob.addTag("MobGunner");
                        if (mob.level() instanceof ServerLevel naturalLevel && EnemyVehicleSpawner.tryReplaceNaturalGunner(naturalLevel, mob)) {
                            return;
                        }
                    }
                }
            }
        }

        ItemStack heldItem = mob.getMainHandItem();
        if (BomberGunnerHelper.isBomber(mob)) {
            BomberGunnerHelper.ensureBomberGoal(mob, getGoalSelector(mob));
            extendFollowRange(mob);
        } else if (heldItem.getItem() instanceof GunItem) {
            enforceGunnerMainHandLock(mob);
            reassessWeaponGoal(mob);
        } else {
            resetFollowRange(mob);
        }
    }

    public static boolean hasGunAttackGoal(PathfinderMob mob) {
        return getGoalSelector(mob).getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof GunAttackGoal<?>);
    }

    private static Faction getFactionForMob(PathfinderMob mob) {
        Identifier entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return entityTypeId != null ? GunnerManager.getInstance().getFactionForMob(entityTypeId) : null;
    }

    private static double resolveNaturalGunnerChance(PathfinderMob mob) {
        Identifier entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        String gunnerType = GunnerType.keyFor(mob);
        if (PARCHED_ID.equals(entityTypeId)) {
            return Config.gunnerSpawnChance(mob.level(), gunnerType, Config.parchedGunnerChance());
        }
        if (mob instanceof Husk) {
            return Config.gunnerSpawnChance(mob.level(), gunnerType, Config.huskGunnerChance());
        }
        if (mob instanceof ZombifiedPiglin) {
            return Config.gunnerSpawnChance(mob.level(), gunnerType, Config.zombifiedPiglinGunnerChance());
        }
        if (mob instanceof ZombieVillager || mob instanceof Drowned || mob instanceof Zombie) {
            return Config.gunnerSpawnChance(mob.level(), gunnerType, Config.zombieGunnerChance());
        }
        if (mob instanceof Stray || mob instanceof Skeleton) {
            return Config.gunnerSpawnChance(mob.level(), gunnerType, Config.skeletonGunnerChance());
        }
        if (mob instanceof WitherSkeleton) {
            return Config.gunnerSpawnChance(mob.level(), gunnerType, Config.witherSkeletonGunnerChance());
        }
        if (mob instanceof PiglinBrute || mob instanceof Piglin) {
            return Config.gunnerSpawnChance(mob.level(), gunnerType, Config.piglinGunnerChance());
        }
        if (mob instanceof Vindicator || mob instanceof Pillager) {
            return Config.gunnerSpawnChance(mob.level(), gunnerType, Config.pillagerGunnerChance());
        }
        return Config.gunnerSpawnChance(mob.level(), gunnerType, legacyNaturalGunnerChance(mob.level()));
    }

    private static double legacyNaturalGunnerChance(Level level) {
        int currentDay = (int) Config.currentGunnerDay(level);
        if (currentDay < GunMobValues.minDays) {
            return 0.0D;
        }
        int daysOverMin = currentDay - GunMobValues.minDays;
        int currentChance = Math.min(GunMobValues.initialChance + (daysOverMin * GunMobValues.chanceIncrement), GunMobValues.maxChance);
        return currentChance / 100.0D;
    }

    public static boolean hasTargetGoal(PathfinderMob mob) {
        return getTargetSelector(mob).getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?>);
    }

    public static void reassessWeaponGoal(PathfinderMob mob) {
        if (mob.level().isClientSide() || hasGunAttackGoal(mob)) {
            return;
        }

        normalizeGunnerMob(mob);
        if (mob.isBaby()) {
            return;
        }

        enforceGunnerMainHandLock(mob);

        AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
        boolean isCloseRange = mob.getRandom().nextBoolean();
        int stopRange = isCloseRange ? 7 : 20;
        int aiDifficulty = mob.getRandom().nextInt(4) + 1;

        getGoalSelector(mob).addGoal(2, new GunAttackGoal<>(mob, stopRange, 1.2F, aiType, aiDifficulty));
        mob.addTag("GunAttackAssigned");
        extendFollowRange(mob);
    }

    private static GoalSelector getGoalSelector(PathfinderMob mob) {
        return ((MobAccessor) mob).jeg$getGoalSelector();
    }

    private static GoalSelector getTargetSelector(PathfinderMob mob) {
        return ((MobAccessor) mob).jeg$getTargetSelector();
    }

    public static void normalizeGunnerMob(Mob mob) {
        if (mob instanceof Zombie zombie && zombie.isBaby()) {
            zombie.setBaby(false);
        }
        if (mob instanceof Piglin piglin && piglin.isBaby()) {
            piglin.setBaby(false);
        }
        if (mob instanceof AbstractPiglin abstractPiglin) {
            abstractPiglin.setImmuneToZombification(true);
        }
        if (mob.isOnFire()) {
            mob.extinguishFire();
        }
    }

    private static void enforceGunnerMainHandLock(PathfinderMob mob) {
        if (mob.getMainHandItem().getItem() instanceof GunItem) {
            mob.setCanPickUpLoot(false);
        }
    }

    public static void suppressSkeletonSniperDrop(PathfinderMob mob, Item gun) {
        if (mob instanceof Skeleton && "bolt_action_rifle".equals(BuiltInRegistries.ITEM.getKey(gun).getPath())) {
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    public static void applyEliteAttributes(PathfinderMob mob) {
        mob.addTag("EliteGunner");
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        // Replace turtle helmet with bulletproof armor using GunnerArmorEquiper
        GunnerArmorEquiper.equipGunnerArmor(mob.getRandom(), GunnerArmorEquiper.GunnerArmorContext.elite(mob));
        // Using alternative effect since DAMAGE_BOOST doesn't exist in 1.21.10
        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 1, false, true));
        mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, -1, 0, false, false));
    }

    private static ItemStack createModifiedGun(PathfinderMob mob, Item gun) {
        ItemStack gunStack = new ItemStack(gun);
        if (gun instanceof GunItem gunItem) {
            GunStats stats = gunItem.getStats();
            // Gunners need a loaded weapon on spawn or they can stall before their first reload.
            gunStack.set(ttv.migami.jeg.init.ModDataComponents.GUN_AMMO.get(),
                        Math.max(1, stats.magazineSize()));
        }
        GunnerProgression.prepareDroppedWeapon(mob, gunStack);
        return gunStack;
    }

    public static void extendFollowRange(PathfinderMob mob) {
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

    public static void resetFollowRange(PathfinderMob mob) {
        AttributeInstance attribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (attribute != null) {
            attribute.removeModifier(GUN_FOLLOW_RANGE_MODIFIER_ID);
        }
    }
}

