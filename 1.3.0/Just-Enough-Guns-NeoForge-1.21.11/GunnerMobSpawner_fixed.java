package ttv.migami.jeg.faction;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.ai.AIType;
import ttv.migami.jeg.entity.ai.GunAttackGoal;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.gun.GunStats;

import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class GunnerMobSpawner {

    // Default values - will be replaced by config system later
    private static final boolean ENABLED = true;
    private static final int MIN_DAYS = 4;
    private static final int INITIAL_CHANCE = 10;
    private static final int CHANCE_INCREMENT = 1;
    private static final int MAX_CHANCE = 50;
    private static final float ELITE_CHANCE = 0.3F;
    private static final boolean ELITES_ENABLED = true;

    public static final ResourceLocation GUN_FOLLOW_RANGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("jeg", "gun_follow_range");

    // Fixed: Use specific subclass instead of abstract MobSpawnEvent
    @SubscribeEvent
    public static void onSpecialSpawn(MobSpawnEvent.SpawnPlacement event) {
        if (!ENABLED) {
            return;
        }

        // Check if this is a natural spawn (not summoned by commands, spawners, etc.)
        if (!event.isNaturalSpawn()) {
            return;
        }

        try {
            // Rest of the original logic...
            // [Keep the existing implementation here]

        } catch (Exception e) {
            // Log error but don't crash the game
            System.err.println("Error in GunnerMobSpawner.onSpecialSpawn: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        try {
            // Original implementation with error handling
            // [Keep existing logic]
        } catch (Exception e) {
            System.err.println("Error in GunnerMobSpawner.onLivingEquipmentChange: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        try {
            // Original implementation with error handling
            // [Keep existing logic]
        } catch (Exception e) {
            System.err.println("Error in GunnerMobSpawner.onEntityJoinWorld: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(EntityTickEvent.Post event) {
        try {
            // Original implementation with error handling
            // [Keep existing logic]
        } catch (Exception e) {
            System.err.println("Error in GunnerMobSpawner.onLivingUpdate: " + e.getMessage());
        }
    }

    // Rest of the original class implementation...
    // [Keep all existing methods but add error handling]
}