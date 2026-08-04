package ttv.migami.jeg.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.Husk;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantom;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.C4VestItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.faction.BomberGunnerHelper;
import ttv.migami.jeg.faction.GunnerProgression;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class GunEvents {
    private static final String MANUAL_GRANTED_TAG = "jeg_manual_granted";
    private static final String FINGER_GUN_RECIPE_GRANTED_TAG = "jeg_finger_gun_recipe_granted";
    private static final int MAX_SAVED_ITEM_COUNT = 99;
    private static final String VEHICLE_RETURN_TAG = "jeg_vehicle_return";
    private static final String VEHICLE_RETURN_DIMENSION_TAG = "Dimension";
    private static final String VEHICLE_RETURN_UUID_TAG = "Vehicle";
    private static final String VEHICLE_RETURN_SEAT_TAG = "Seat";

    // Tags for JEG faction gunners (replacing old individual gunner tags)
    public static final String JEG_GUNNER_TAG = "MobGunner";
    public static final String JEG_ELITE_GUNNER_TAG = "EliteGunner";

    private static final Identifier[] DEFAULT_PILLAGER_GUNS = new Identifier[] {
            Reference.id("assault_rifle"),
            Reference.id("burst_rifle"),
            Reference.id("service_rifle"),
            Reference.id("light_machine_gun"),
            Reference.id("semi_auto_rifle"),
            Reference.id("combat_rifle"),
            Reference.id("infantry_rifle"),
            Reference.id("pump_shotgun"),
            Reference.id("repeating_shotgun"),
            Reference.id("minigun")
    };

    private GunEvents() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            grantStartingManual(serverPlayer);
            sendAvailableCommands(serverPlayer);
            NetworkHandler.sendUiConfig(serverPlayer);
            NetworkHandler.sendVehicleData(serverPlayer);
            restoreVehicleSeat(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            saveVehicleSeat(serverPlayer);
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

        // Remove old gunner system - only handle pillager gunners here (non-JEG system)
        if (event.getEntity() instanceof Pillager pillager) {
            if (pillager.entityTags().contains("jeg_pillager_gunner")) {
                // Only equip if pillager doesn't already have a gun
                if (!isHoldingGun(pillager)) {
                    Identifier selected = selectRandomGun(pillager.level(), pillager.getRandom());
                    if (selected != null) {
                        equipPillagerWithGun(pillager, selected);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        long count = StreamSupport.stream(event.getServer().getRecipeManager().getRecipes().spliterator(), false)
                .filter(holder -> holder.id().identifier().getNamespace().equals(Reference.MOD_ID))
                .count();
        if (count == 0) {
            ttv.migami.jeg.JustEnoughGuns.LOGGER.warn("No {} recipes were loaded; verify data pack paths", Reference.MOD_ID);
        } else {
            ttv.migami.jeg.JustEnoughGuns.LOGGER.info("Loaded {} {} recipes", count, Reference.MOD_ID);
        }
    }

    private static void grantStartingManual(ServerPlayer player) {
        player.awardRecipesByKey(ModItems.unlockGunRecipeKeys());

        boolean manualGranted = player.getPersistentData().getBoolean(MANUAL_GRANTED_TAG).orElse(false);
        if (!manualGranted) {
            player.awardRecipesByKey(ModItems.manualRecipes());

            player.awardRecipesByKey(java.util.List.of(
                    ResourceKey.create(Registries.RECIPE, Reference.id("armored_joy_harness")),
                    ResourceKey.create(Registries.RECIPE, Reference.id("finger_gun"))
            ));

            player.getPersistentData().putBoolean(MANUAL_GRANTED_TAG, true);
        }

        if (!player.getPersistentData().getBoolean(FINGER_GUN_RECIPE_GRANTED_TAG).orElse(false)) {
            player.awardRecipesByKey(java.util.List.of(
                    ResourceKey.create(Registries.RECIPE, Reference.id("finger_gun"))
            ));
            player.getPersistentData().putBoolean(FINGER_GUN_RECIPE_GRANTED_TAG, true);
        }
    }

    private static void saveVehicleSeat(ServerPlayer player) {
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
            player.getPersistentData().remove(VEHICLE_RETURN_TAG);
            return;
        }
        int seatIndex = vehicle.getSeatIndex(player);
        if (seatIndex < 0) {
            player.getPersistentData().remove(VEHICLE_RETURN_TAG);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString(VEHICLE_RETURN_DIMENSION_TAG, player.level().dimension().identifier().toString());
        tag.putString(VEHICLE_RETURN_UUID_TAG, vehicle.getUUID().toString());
        tag.putInt(VEHICLE_RETURN_SEAT_TAG, seatIndex);
        player.getPersistentData().put(VEHICLE_RETURN_TAG, tag);
        vehicle.preserveSeatAssignment(player);
    }

    public static void clearVehicleSeatReturn(ServerPlayer player) {
        player.getPersistentData().remove(VEHICLE_RETURN_TAG);
    }

    private static void restoreVehicleSeat(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(VEHICLE_RETURN_TAG);
        player.getPersistentData().remove(VEHICLE_RETURN_TAG);
        if (tag.isEmpty() || !tag.contains(VEHICLE_RETURN_UUID_TAG)) {
            return;
        }
        Identifier dimensionId = Identifier.tryParse(tag.getStringOr(VEHICLE_RETURN_DIMENSION_TAG, ""));
        if (dimensionId == null || !player.level().dimension().identifier().equals(dimensionId)) {
            return;
        }
        UUID vehicleUuid;
        try {
            vehicleUuid = UUID.fromString(tag.getStringOr(VEHICLE_RETURN_UUID_TAG, ""));
        } catch (IllegalArgumentException exception) {
            return;
        }
        Entity entity = findEntityByUUID((ServerLevel) player.level(), vehicleUuid);
        if (!(entity instanceof VehicleEntity vehicle) || vehicle.isRemoved() || player.isPassenger()) {
            return;
        }
        int seatIndex = tag.getIntOr(VEHICLE_RETURN_SEAT_TAG, -1);
        vehicle.rememberSeatAssignment(player, seatIndex);
        player.startRiding(vehicle, true, true);
    }

    private static Entity findEntityByUUID(ServerLevel level, UUID uuid) {
        for (Entity entity : level.getAllEntities()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    private static void sendAvailableCommands(ServerPlayer player) {
        Component header = Component.empty()
                .append(Component.translatable("message.jeg.intro.prefix").withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)))
                .append(Component.translatable("commands.jeg.help.commands").withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true)));
        Component divider = Component.translatable("message.jeg.intro.divider")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY));
        Component unlockLine = Component.empty()
                .append(Component.translatable("message.jeg.intro.unlock_recipes.command")
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(true)))
                .append(Component.translatable("message.jeg.intro.unlock_recipes.description")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));

        player.sendSystemMessage(header);
        player.sendSystemMessage(divider);
        player.sendSystemMessage(unlockLine);

        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            Component configLine = Component.empty()
                    .append(Component.translatable("message.jeg.intro.config.command")
                            .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true)))
                    .append(Component.translatable("message.jeg.intro.config.description")
                            .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
            player.sendSystemMessage(configLine);

            Component enemyVehicleLine = Component.empty()
                    .append(Component.translatable("message.jeg.intro.disable_enemy_vehicles.command")
                            .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true)))
                    .append(Component.translatable("message.jeg.intro.disable_enemy_vehicles.description")
                            .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
            player.sendSystemMessage(enemyVehicleLine);
        }
        player.sendSystemMessage(divider);
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

        double conversionChance = Config.pillagerGunnerChance(serverLevel);
        if (conversionChance <= 0.0D || pillager.getRandom().nextDouble() >= conversionChance) {
            return;
        }

        // Since GunnerEntity was removed, we just tag the pillager instead
        pillager.addTag("jeg_pillager_gunner");
        Identifier selected = selectRandomGun(serverLevel, pillager.getRandom());
        if (selected != null) {
            equipPillagerWithGun(pillager, selected);
        }
    }

    static void equipPillagerWithGun(Pillager pillager, Identifier gunId) {
        var holder = ModItems.GUNS.get(gunId);
        if (holder == null) {
            return;
        }

        ItemStack stack = new ItemStack(holder.get());
        pillager.setItemInHand(InteractionHand.MAIN_HAND, stack);
        GunnerProgression.prepareDroppedWeapon(pillager, stack);
        pillager.setCanPickUpLoot(false);
        // Pillagers use the default crossbow AI which works reasonably well for guns
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

        if (event.getSpawnType() != net.minecraft.world.entity.EntitySpawnReason.NATURAL) {
            return;
        }

        // Free-roaming Terror Phantom natural conversion is soft-disabled.
        // Bound Terror Phantom (guardian) and spawn eggs remain available.

        double gunnerChance = Config.phantomGunnerChance(serverLevel);
        if (gunnerChance <= 0.0D || phantom.getRandom().nextDouble() >= gunnerChance) {
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

    @SubscribeEvent
    public static void onBomberDeath(LivingDeathEvent event) {
        if (BomberGunnerHelper.isArmed(event.getEntity()) && !BomberGunnerHelper.hasDetonated(event.getEntity())) {
            BomberGunnerHelper.explodeVest(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        net.minecraft.world.entity.LivingEntity entity = event.getEntity();

        // Handle JEG faction gunners and special gunner types
        if (!isGunner(entity)) {
            return;
        }

        if (BomberGunnerHelper.isBomber(entity)) {
            event.getDrops().removeIf(drop -> drop.getItem().getItem() instanceof C4VestItem);
            addDrop(event, entity, BomberGunnerHelper.rollGunpowderDrop(entity));
        }

        ItemStack held = entity.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            // Remove vanilla ranged weapons from gunner drops
            if (entity instanceof net.minecraft.world.entity.monster.skeleton.Skeleton) {
                event.getDrops().removeIf(drop -> drop.getItem().is(Items.BOW) || drop.getItem().is(Items.ARROW));
            }
            return;
        }

        // Remove vanilla ranged weapons from all gunner types
        if (entity instanceof net.minecraft.world.entity.monster.skeleton.Skeleton) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.BOW) || drop.getItem().is(Items.ARROW));
        } else if (entity instanceof net.minecraft.world.entity.monster.zombie.Zombie || entity instanceof net.minecraft.world.entity.monster.zombie.Husk) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.IRON_SHOVEL) || drop.getItem().is(Items.IRON_SWORD));
        }

        RandomSource random = entity.getRandom();
        GunStats stats = gunItem.getStats();
        boolean skeletonSniper = isSkeletonSniper(entity, held);
        if (skeletonSniper) {
            event.getDrops().removeIf(drop -> isBoltActionRifle(drop.getItem()));
        }

        if (!skeletonSniper && random.nextFloat() < 0.06F) {
            ItemStack dropGun = held.copy();
            GunnerProgression.damageWeaponToLowDurability(dropGun, random);
            addDrop(event, entity, dropGun);
            return;
        }

        float ammoDropChance = 0.60F;
        if (random.nextFloat() < ammoDropChance) {
            ItemStack ammo = buildAmmoDrop(stats, random);
            if (!ammo.isEmpty()) {
                addDrop(event, entity, ammo);
            }
        }
    }

    private static boolean isGunner(net.minecraft.world.entity.LivingEntity entity) {
        // Check for JEG faction gunners
        if (entity.entityTags().contains(JEG_GUNNER_TAG) || entity.entityTags().contains(JEG_ELITE_GUNNER_TAG)) {
            return true;
        }

        // Check for special gunner types (phantom gunner, pillager gunner)
        return entity.entityTags().contains("jeg_pillager_gunner") ||
               entity instanceof PhantomGunner ||
               entity instanceof TerrorPhantom;
    }

    private static boolean isSkeletonSniper(net.minecraft.world.entity.LivingEntity entity, ItemStack stack) {
        return entity instanceof net.minecraft.world.entity.monster.skeleton.Skeleton && isBoltActionRifle(stack);
    }

    private static boolean isBoltActionRifle(ItemStack stack) {
        return "bolt_action_rifle".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }

    private static void addDrop(LivingDropsEvent event, net.minecraft.world.entity.LivingEntity entity, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        int remaining = stack.getCount();
        int maxDropCount = Math.max(1, Math.min(stack.getMaxStackSize(), MAX_SAVED_ITEM_COUNT));
        while (remaining > 0) {
            int dropCount = Math.min(remaining, maxDropCount);
            ItemEntity item = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack.copyWithCount(dropCount));
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
            remaining -= dropCount;
        }
    }

    private static ItemStack buildAmmoDrop(GunStats stats, RandomSource random) {
        Identifier ammoId = stats.ammoItem();
        if (ammoId != null) {
            var ammoHolder = ModItems.AMMO.get(ammoId);
            Item ammo = ammoHolder != null ? ammoHolder.get() : BuiltInRegistries.ITEM.getOptional(ammoId).orElse(null);
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

    private static Identifier selectRandomGun(Level level, RandomSource random) {
        java.util.List<Item> guns = new java.util.ArrayList<>();
        for (Identifier gunId : DEFAULT_PILLAGER_GUNS) {
            var holder = ModItems.GUNS.get(gunId);
            if (holder != null) {
                guns.add(holder.get());
            }
        }
        if (guns.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(GunnerProgression.selectGun(guns, level, random));
    }

    /**
     * Checks if an entity is holding a gun in their main hand
     * @param entity The entity to check
     * @return true if the entity is holding a GunItem, false otherwise
     */
    private static boolean isHoldingGun(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            net.minecraft.world.item.ItemStack stack = livingEntity.getMainHandItem();
            return stack.getItem() instanceof ttv.migami.jeg.item.GunItem;
        }
        return false;
    }
}
