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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.ai.AIType;
import ttv.migami.jeg.entity.ai.GunAttackGoal;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModTags;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.faction.GunnerArmorEquiper;
import ttv.migami.jeg.faction.patrol.PatrolEncounterManager;
import ttv.migami.jeg.faction.raid.FactionRaidHooks;
import ttv.migami.jeg.faction.raid.RaidEntity;
import ttv.migami.jeg.entity.monster.phantom.TerrorRaidHooks;

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
            reassessWeaponGoal(mob);
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(EntityTickEvent.Post event) {
        if (!GunMobValues.enabled) {
            return;
        }

        if (!(event.getEntity() instanceof PathfinderMob mob)) {
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

        // Normalize tagged gunners before assigning their weapon.
        if (mob.entityTags().contains("MobGunner") && !(heldItem.getItem() instanceof GunItem)) {
            normalizeGunnerMob(mob);
            if (mob.isBaby()) {
                return;
            }

            GunnerManager manager = new GunnerManager(GunnerManager.getConfigFactions());
            String entityName = mob.getType().getDescriptionId().replace("entity.", "").replace(".", ":");
            Identifier entityTypeLocation = Identifier.tryParse(entityName);
            Faction faction = manager.getFactionForMob(entityTypeLocation);

            if (faction != null) {
                boolean isCloseRange = mob.getRandom().nextBoolean();
                int stopRange = isCloseRange ? 7 : 20;

                Item gun = faction.getRandomGun(isCloseRange, mob.level(), mob.getRandom());
                AIType aiType = AIType.values()[mob.getRandom().nextInt(AIType.values().length)];
                boolean elite = (mob.getRandom().nextFloat() < GunMobValues.eliteChance && GunMobValues.elitesEnabled);
                int aiLevel = faction.getAiLevel() + (elite ? 1 : 0);

                if (elite) {
                    gun = faction.getEliteGun(mob.level(), mob.getRandom());
                    applyEliteAttributes(mob);
                }

                if (!mob.level().isClientSide() && !hasGunAttackGoal(mob)) {
                    // Remove Drowned water-seeking restrictions to allow land combat
                    enableDrownedGunnerCombat(mob);

                    // Add target-finding goal first (priority 1)
                    if (!hasTargetGoal(mob)) {
                        mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, true));
                    }

                    // Then add gun attack goal (priority 2)
                    mob.goalSelector.addGoal(2, new GunAttackGoal<>(mob, stopRange, 1.2F, aiType, aiLevel));
                    mob.addTag("GunAttackAssigned");
                }

                ItemStack modifiedGun = createModifiedGun(mob, gun);
                mob.setItemSlot(EquipmentSlot.MAINHAND, modifiedGun);
                enforceGunnerMainHandLock(mob);

                // Equip armor for all gunners (normal and elite)
                // HELMET PRIORITY: System will prioritize helmets over body armor
                if (elite) {
                    GunnerArmorEquiper.equipGunnerArmor(mob.getRandom(), GunnerArmorEquiper.GunnerArmorContext.elite(mob));
                } else {
                    GunnerArmorEquiper.equipGunnerArmor(mob.getRandom(), GunnerArmorEquiper.GunnerArmorContext.normal(mob));
                }

                extendFollowRange(mob);
            }
        }

        if (heldItem.getItem() instanceof GunItem) {
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

        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        PatrolEncounterManager.recoverPatrolMob(serverLevel, mob);
        FactionRaidHooks.recoverRaidMob(mob);
        TerrorRaidHooks.recoverRaidMob(mob);

        mob.removeTag("GunAttackAssigned");

        // Check if this mob should become a gunner (only for newly spawned mobs)
        if (mob.tickCount <= 5 && !mob.entityTags().contains("MobGunner") && !mob.entityTags().contains(GUNNER_SPAWN_CHECKED_TAG) && mob.getType().builtInRegistryHolder().is(ModTags.Entities.GUNNER)) {
            mob.addTag(GUNNER_SPAWN_CHECKED_TAG);
            if (!mob.isBaby()) {
                double gunnerChance = resolveNaturalGunnerChance(mob);
                if (gunnerChance > 0.0D && mob.getRandom().nextDouble() < gunnerChance) {
                    GunnerManager manager = GunnerManager.getInstance();
                    String entityName = mob.getType().getDescriptionId().replace("entity.", "").replace(".", ":");
                    Identifier entityTypeLocation = Identifier.tryParse(entityName);
                    Faction faction = manager.getFactionForMob(entityTypeLocation);

                    if (faction != null) {
                        normalizeGunnerMob(mob);
                        mob.addTag("MobGunner");
                        ttv.migami.jeg.JustEnoughGuns.LOGGER.info("Created gunner: {} at {}", mob.getType().getDescriptionId(), mob.blockPosition());
                    }
                }
            }
        }

        ItemStack heldItem = mob.getMainHandItem();
        if (heldItem.getItem() instanceof GunItem) {
            enforceGunnerMainHandLock(mob);
            reassessWeaponGoal(mob);
        } else {
            resetFollowRange(mob);
        }
    }

    public static boolean hasGunAttackGoal(PathfinderMob mob) {
        return mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof GunAttackGoal<?>);
    }

    private static double resolveNaturalGunnerChance(PathfinderMob mob) {
        if (!hasReachedMinimumGunnerDay(mob.level())) {
            return 0.0D;
        }
        Identifier entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (PARCHED_ID.equals(entityTypeId)) {
            return Config.parchedGunnerChance();
        }
        if (mob instanceof Husk) {
            return Config.huskGunnerChance();
        }
        if (mob instanceof ZombifiedPiglin) {
            return Config.zombifiedPiglinGunnerChance();
        }
        if (mob instanceof ZombieVillager || mob instanceof Drowned || mob instanceof Zombie) {
            return Config.zombieGunnerChance();
        }
        if (mob instanceof Stray || mob instanceof Skeleton) {
            return Config.skeletonGunnerChance();
        }
        if (mob instanceof WitherSkeleton) {
            return Config.witherSkeletonGunnerChance();
        }
        if (mob instanceof PiglinBrute || mob instanceof Piglin) {
            return Config.piglinGunnerChance();
        }
        if (mob instanceof Vindicator || mob instanceof Pillager) {
            return Config.pillagerGunnerChance(mob.level());
        }
        return legacyNaturalGunnerChance(mob.level());
    }

    private static double legacyNaturalGunnerChance(Level level) {
        int currentDay = (int) (level.getOverworldClockTime() / 24000L);
        if (currentDay < GunMobValues.minDays) {
            return 0.0D;
        }
        int daysOverMin = currentDay - GunMobValues.minDays;
        int currentChance = Math.min(GunMobValues.initialChance + (daysOverMin * GunMobValues.chanceIncrement), GunMobValues.maxChance);
        return currentChance / 100.0D;
    }

    private static boolean hasReachedMinimumGunnerDay(Level level) {
        long currentDay = Math.max(0L, level.getOverworldClockTime() / 24000L);
        return currentDay >= Math.max(0, Config.SPAWN_SCALING_START_DAY.get());
    }

    public static boolean hasTargetGoal(PathfinderMob mob) {
        return mob.targetSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof NearestAttackableTargetGoal<?>);
    }

    /**
     * Allows Drowned gunners to attack players while in water or under shade.
     * Keeps original water-seeking and sun-avoiding behaviors intact, but allows combat
     * when environmental conditions are favorable (in water or shade).
     */
    public static void enableDrownedGunnerCombat(PathfinderMob mob) {
        if (!(mob instanceof net.minecraft.world.entity.monster.zombie.Drowned)) {
            return;
        }

        // Mark this drowned as a gunner so it will attack in favorable conditions
        mob.addTag("DrownedGunner");
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

        mob.goalSelector.addGoal(2, new GunAttackGoal<>(mob, stopRange, 1.2F, aiType, aiDifficulty));
        mob.addTag("GunAttackAssigned");
        extendFollowRange(mob);
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

    private static void applyEliteAttributes(PathfinderMob mob) {
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
