package ttv.migami.jeg.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import java.util.stream.StreamSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.GunnerEntity;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantom;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.gun.GunStats;

public final class GunEvents {
    private static final String MANUAL_GRANTED_TAG = "jeg_manual_granted";
    public static final String SKELETON_GUNNER_TAG = "jeg_gunner";
    private static final ResourceLocation[] DEFAULT_PILLAGER_GUNS = new ResourceLocation[] {
            Reference.id("assault_rifle"),
            Reference.id("burst_rifle"),
            Reference.id("service_rifle"),
            Reference.id("light_machine_gun"),
            Reference.id("semi_auto_rifle")
    };

    private GunEvents() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            grantStartingManual(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            grantStartingManual(serverPlayer);
            return;
        }

        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Skeleton skeleton) {
            if (skeleton.getTags().contains(SKELETON_GUNNER_TAG)) {
                equipSkeletonWithGun(skeleton, event.getLevel().getRandom());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        long count = StreamSupport.stream(event.getServer().getRecipeManager().getRecipes().spliterator(), false)
                .filter(holder -> holder.id().location().getNamespace().equals(Reference.MOD_ID))
                .count();
        if (count == 0) {
            ttv.migami.jeg.JustEnoughGuns.LOGGER.warn("No {} recipes were loaded; verify data pack paths", Reference.MOD_ID);
        } else {
            ttv.migami.jeg.JustEnoughGuns.LOGGER.info("Loaded {} {} recipes", count, Reference.MOD_ID);
        }
    }

    private static void grantStartingManual(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(MANUAL_GRANTED_TAG).orElse(false)) {
            return;
        }

        ItemStack manual = new ItemStack(ModItems.GUNSMITH_MANUAL.get());
        boolean added = player.getInventory().add(manual);
        if (!added) {
            player.drop(manual, false);
        }

        player.awardRecipesByKey(ModItems.manualRecipes());

        // Grant armored harness and armor plate recipes
        player.awardRecipesByKey(java.util.List.of(
                ResourceKey.create(Registries.RECIPE, Reference.id("armored_joy_harness")),
                ResourceKey.create(Registries.RECIPE, Reference.id("joyous_armor_plate"))
        ));

        player.getPersistentData().putBoolean(MANUAL_GRANTED_TAG, true);
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        boolean reloaded = tryReload(player.level(), player, player.getMainHandItem(), InteractionHand.MAIN_HAND);
        reloaded |= tryReload(player.level(), player, player.getOffhandItem(), InteractionHand.OFF_HAND);

        if (reloaded) {
            event.setCanceled(true);
        }
    }

    private static boolean tryReload(Level level, Player player, ItemStack stack, InteractionHand hand) {
        if (!(stack.getItem() instanceof GunItem gun)) {
            return false;
        }

        boolean reloaded = gun.tryReload(level, player, stack, true);
        if (reloaded && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.swing(hand, true);
        }
        return reloaded;
    }

    @SubscribeEvent
    public static void onPillagerFinalize(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Pillager pillager)) {
            return;
        }

        Level level = pillager.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double conversionChance = Config.pillagerGunnerChance();
        if (conversionChance <= 0.0D || serverLevel.random.nextDouble() >= conversionChance) {
            return;
        }

        GunnerEntity gunner = ModEntities.GUNNER.get().create(serverLevel, event.getSpawnType());
        if (gunner == null) {
            return;
        }

        gunner.setPos(pillager.getX(), pillager.getY(), pillager.getZ());
        gunner.setYRot(pillager.getYRot());
        gunner.setXRot(pillager.getXRot());
        gunner.finalizeSpawn(serverLevel, event.getDifficulty(), event.getSpawnType(), event.getSpawnData());
        ResourceLocation selected = selectRandomGun(serverLevel.random);
        if (selected != null) {
            gunner.equipGun(selected);
        }
        serverLevel.addFreshEntity(gunner);
        pillager.discard();
    }

    @SubscribeEvent
    public static void onSkeletonFinalize(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Skeleton skeleton)) {
            return;
        }

        if (skeleton.level().isClientSide()) {
            return;
        }

        if (net.minecraft.world.entity.EntitySpawnReason.isSpawner(event.getSpawnType())) {
            return;
        }

        double skeletonChance = Config.skeletonGunnerChance();
        if (skeletonChance > 0.0D && skeleton.getRandom().nextDouble() < skeletonChance) {
            skeleton.addTag(SKELETON_GUNNER_TAG);
        }
    }

    @SubscribeEvent
    public static void onPhantomFinalize(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Phantom phantom)) {
            return;
        }

        Level level = phantom.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (event.getSpawnType() == net.minecraft.world.entity.EntitySpawnReason.NATURAL) {
            double terrorChance = Config.terrorPhantomChance();
            if (terrorChance > 0.0D && serverLevel.random.nextDouble() < terrorChance) {
                TerrorPhantom terror = new TerrorPhantom(ModEntities.TERROR_PHANTOM.get(), serverLevel);
                if (terror != null) {
                    terror.setPos(phantom.getX(), phantom.getY(), phantom.getZ());
                    terror.setYRot(phantom.getYRot());
                    terror.setXRot(phantom.getXRot());
                    terror.setDeltaMovement(phantom.getDeltaMovement());
                    terror.finalizeSpawn(serverLevel, event.getDifficulty(), net.minecraft.world.entity.EntitySpawnReason.EVENT, event.getSpawnData());
                    serverLevel.addFreshEntity(terror);
                    phantom.discard();
                    return;
                }
            }

            double gunnerChance = Config.phantomGunnerChance();
            if (gunnerChance <= 0.0D || serverLevel.random.nextDouble() >= gunnerChance) {
                return;
            }
        } else {
            return;
        }

        PhantomGunner gunner = new PhantomGunner(ModEntities.PHANTOM_GUNNER.get(), serverLevel);
        if (gunner == null) {
            return;
        }

        gunner.setPos(phantom.getX(), phantom.getY(), phantom.getZ());
        gunner.setYRot(phantom.getYRot());
        gunner.setXRot(phantom.getXRot());
        gunner.yRotO = phantom.yRotO;
        gunner.xRotO = phantom.xRotO;
        gunner.setDeltaMovement(phantom.getDeltaMovement());
        gunner.finalizeSpawn(serverLevel, event.getDifficulty(), net.minecraft.world.entity.EntitySpawnReason.EVENT, event.getSpawnData());
        serverLevel.addFreshEntity(gunner);
        phantom.discard();
    }

    private static ResourceLocation selectRandomGun(RandomSource random) {
        if (DEFAULT_PILLAGER_GUNS.length == 0) {
            return null;
        }
        return DEFAULT_PILLAGER_GUNS[random.nextInt(DEFAULT_PILLAGER_GUNS.length)];
    }

    public static void equipSkeletonWithGun(net.minecraft.world.entity.monster.Skeleton skeleton, RandomSource random) {
        // Guns available for skeleton spawning (excludes trumpet and other non-combat items)
        ResourceLocation[] guns = new ResourceLocation[] {
                Reference.id("service_rifle"),
                Reference.id("infantry_rifle"),
                Reference.id("subsonic_rifle"),
                Reference.id("burst_rifle"),
                Reference.id("assault_rifle"),
                Reference.id("semi_auto_rifle"),
                Reference.id("bolt_action_rifle"),
                Reference.id("combat_rifle")
        };
        ResourceLocation choice = guns[random.nextInt(guns.length)];
        var holder = ModItems.GUNS.get(choice);
        if (holder == null) {
            return;
        }

        ItemStack stack = new ItemStack(holder.get());
        skeleton.setItemInHand(InteractionHand.MAIN_HAND, stack);
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        skeleton.reassessWeaponGoal();
        addGunGoal(skeleton);
    }

    private static void addGunGoal(net.minecraft.world.entity.monster.Skeleton skeleton) {
        boolean hasGoal = skeleton.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof ttv.migami.jeg.entity.ai.goal.SkeletonGunAttackGoal);
        if (!hasGoal) {
            skeleton.goalSelector.addGoal(2, new ttv.migami.jeg.entity.ai.goal.SkeletonGunAttackGoal(skeleton));
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Skeleton skeleton)) {
            return;
        }

        if (!skeleton.getTags().contains(SKELETON_GUNNER_TAG)) {
            return;
        }

        ItemStack held = skeleton.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.BOW) || drop.getItem().is(Items.ARROW));
            return;
        }

        event.getDrops().removeIf(drop -> drop.getItem().is(Items.BOW) || drop.getItem().is(Items.ARROW));

        RandomSource random = skeleton.getRandom();
        GunStats stats = gunItem.getStats();
        if (random.nextFloat() < 0.12F) {
            ItemStack dropGun = held.copy();
            if (dropGun.isDamageableItem()) {
                int maxDamage = dropGun.getMaxDamage();
                int damage = Mth.nextInt(random, Math.max(1, (int) (maxDamage * 0.65F)), Math.max(1, (int) (maxDamage * 0.9F)));
                dropGun.setDamageValue(Math.min(maxDamage - 1, damage));
            }
            addDrop(event, skeleton, dropGun);
            return;
        }

        ItemStack ammo = buildAmmoDrop(stats, random);
        if (!ammo.isEmpty()) {
            addDrop(event, skeleton, ammo);
        }
    }

    private static void addDrop(LivingDropsEvent event, net.minecraft.world.entity.LivingEntity entity, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
        item.setDefaultPickUpDelay();
        event.getDrops().add(item);
    }

    private static ItemStack buildAmmoDrop(GunStats stats, RandomSource random) {
        ResourceLocation ammoId = stats.ammoItem();
        if (ammoId != null) {
            var ammoItem = ModItems.AMMO.get(ammoId);
            Item ammo = ammoItem != null ? ammoItem.get() : BuiltInRegistries.ITEM.getOptional(ammoId).orElse(null);
            if (ammo != null) {
                int lower = Math.max(6, stats.usesMagazine() ? stats.magazineSize() / 2 : 8);
                int upper = Math.max(lower, stats.usesMagazine() ? stats.magazineSize() : lower + 8);
                int amount = Mth.nextInt(random, lower, upper);
                return new ItemStack(ammo, amount);
            }
        }
        int fallback = Mth.nextInt(random, 6, 14);
        return new ItemStack(Items.GUNPOWDER, fallback);
    }
}
