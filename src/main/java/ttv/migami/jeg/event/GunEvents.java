package ttv.migami.jeg.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.util.Map;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.StreamSupport;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.event.MainThreadLevelActionScheduler;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantom;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.faction.GunnerProgression;

public final class GunEvents {
    private static final String MANUAL_GRANTED_TAG = "jeg_manual_granted";
    private static final String FINGER_GUN_RECIPE_GRANTED_TAG = "jeg_finger_gun_recipe_granted";

    // Tags for JEG faction gunners (replacing old individual gunner tags)
    public static final String JEG_GUNNER_TAG = "MobGunner";
    public static final String JEG_ELITE_GUNNER_TAG = "EliteGunner";
    private static final double TERROR_PHANTOM_EXCLUSION_RADIUS = 128.0D;
    private static final long NATURAL_TERROR_PHANTOM_COOLDOWN_TICKS = 10L * 24000L;
    private static final Map<MinecraftServer, Long> LAST_NATURAL_TERROR_PHANTOM_SPAWN = new WeakHashMap<>();

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
        boolean manualGranted = player.entityTags().contains(MANUAL_GRANTED_TAG);
        if (!manualGranted) {
            player.awardRecipesByKey(ModItems.unlockGunRecipeKeys());
            player.addTag(MANUAL_GRANTED_TAG);
        }

        if (!player.entityTags().contains(FINGER_GUN_RECIPE_GRANTED_TAG)) {
            player.awardRecipesByKey(java.util.List.of(
                    ResourceKey.create(Registries.RECIPE, Reference.id("finger_gun"))
            ));
            player.addTag(FINGER_GUN_RECIPE_GRANTED_TAG);
        }
    }

    private static void sendAvailableCommands(ServerPlayer player) {
        Component header = Component.empty()
                .append(Component.literal("[JEG] ").withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)))
                .append(Component.literal("Commands").withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true)));
        Component divider = Component.literal("------------------------------")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY));
        Component unlockLine = Component.empty()
                .append(Component.literal("/justEnoughGuns unlockGunRecipes")
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(true)))
                .append(Component.literal(" - Unlock all JEG recipes")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));

        player.sendSystemMessage(header);
        player.sendSystemMessage(divider);
        player.sendSystemMessage(unlockLine);

        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            Component configLine = Component.empty()
                    .append(Component.literal("/justEnoughGuns config")
                            .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true)))
                    .append(Component.literal(" - Configure patrol, mob spawn rates, and combat")
                            .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));
            player.sendSystemMessage(configLine);
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

        MainThreadLevelActionScheduler.scheduleNextTick(serverLevel, () -> handlePillagerFinalize(pillager, serverLevel));
    }

    static void equipPillagerWithGun(Pillager pillager, Identifier gunId) {
        var holder = ModItems.GUNS.get(gunId);
        if (holder == null) {
            return;
        }

        ItemStack stack = new ItemStack(holder.get());
        pillager.setItemInHand(InteractionHand.MAIN_HAND, stack);
        GunnerProgression.prepareDroppedWeapon(pillager, stack);
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

        MainThreadLevelActionScheduler.scheduleNextTick(serverLevel, () -> handlePhantomFinalize(phantom, serverLevel, event.getDifficulty(), (net.minecraft.world.entity.EntitySpawnReason) event.getSpawnType(), event.getSpawnData()));
    }

    public static void handlePillagerFinalize(Pillager pillager, ServerLevel serverLevel) {
        if (!pillager.isAlive() || pillager.level() != serverLevel || isHoldingGun(pillager)) {
            return;
        }

        double conversionChance = Config.pillagerGunnerChance(serverLevel);
        if (conversionChance <= 0.0D || pillager.getRandom().nextDouble() >= conversionChance) {
            return;
        }

        pillager.addTag("jeg_pillager_gunner");
        Identifier selected = selectRandomGun(serverLevel, pillager.getRandom());
        if (selected != null) {
            equipPillagerWithGun(pillager, selected);
        }
    }

    public static void handlePhantomFinalize(Phantom phantom, ServerLevel serverLevel, DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason spawnType, SpawnGroupData spawnData) {
        if (!phantom.isAlive() || phantom.level() != serverLevel) {
            return;
        }
        if (spawnType != net.minecraft.world.entity.EntitySpawnReason.NATURAL) {
            return;
        }
        if (phantom instanceof TerrorPhantom || phantom instanceof PhantomGunner) {
            return;
        }
        if (!serverLevel.getEntitiesOfClass(TerrorPhantom.class, phantom.getBoundingBox().inflate(1.0D), existing -> existing.isAlive()).isEmpty()) {
            phantom.discard();
            return;
        }
        if (!serverLevel.getEntitiesOfClass(PhantomGunner.class, phantom.getBoundingBox().inflate(1.0D), existing -> existing.isAlive()).isEmpty()) {
            phantom.discard();
            return;
        }
        if (phantom.isRemoved()) {
            return;
        }

        double terrorChance = Config.terrorPhantomChance(serverLevel);
        if (terrorChance > 0.0D
                && canSpawnNaturalTerrorPhantom(serverLevel, phantom)
                && phantom.getRandom().nextDouble() < terrorChance) {
            TerrorPhantom terror = new TerrorPhantom(ModEntities.TERROR_PHANTOM.get(), serverLevel);
            if (terror != null) {
                terror.setPos(phantom.getX(), phantom.getY(), phantom.getZ());
                terror.setYRot(phantom.getYRot());
                terror.setXRot(phantom.getXRot());
                terror.setDeltaMovement(phantom.getDeltaMovement());
                terror.finalizeSpawn(serverLevel, difficulty, net.minecraft.world.entity.EntitySpawnReason.EVENT, spawnData);
                if (serverLevel.addFreshEntity(terror)) {
                    recordNaturalTerrorPhantomSpawn(serverLevel);
                    phantom.discard();
                    return;
                }
            }
        }

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
        gunner.finalizeSpawn(serverLevel, difficulty, net.minecraft.world.entity.EntitySpawnReason.EVENT, spawnData);
        if (serverLevel.addFreshEntity(gunner)) {
            phantom.discard();
        }
    }

    private static boolean canSpawnNaturalTerrorPhantom(ServerLevel level, Phantom sourcePhantom) {
        if (!isNaturalTerrorPhantomCooldownReady(level)) {
            return false;
        }

        AABB checkArea = sourcePhantom.getBoundingBox().inflate(TERROR_PHANTOM_EXCLUSION_RADIUS, 96.0D, TERROR_PHANTOM_EXCLUSION_RADIUS);
        // Guardian (Bound Terror Phantom) extends TerrorPhantom, so this single query covers both variants.
        return level.getEntitiesOfClass(TerrorPhantom.class, checkArea, existing -> existing.isAlive()).isEmpty();
    }

    private static boolean isNaturalTerrorPhantomCooldownReady(ServerLevel level) {
        MinecraftServer server = level.getServer();
        Long lastSpawn = LAST_NATURAL_TERROR_PHANTOM_SPAWN.get(server);
        if (lastSpawn == null) {
            return true;
        }
        return level.getGameTime() - lastSpawn >= NATURAL_TERROR_PHANTOM_COOLDOWN_TICKS;
    }

    private static void recordNaturalTerrorPhantomSpawn(ServerLevel level) {
        LAST_NATURAL_TERROR_PHANTOM_SPAWN.put(level.getServer(), level.getGameTime());
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        handleGunnerDrops(event.getEntity(), event.getDrops());
    }

    public static void spawnGunnerDrops(net.minecraft.world.entity.LivingEntity entity) {
        List<ItemEntity> drops = new java.util.ArrayList<>();
        handleGunnerDrops(entity, drops);
        for (ItemEntity drop : drops) {
            entity.level().addFreshEntity(drop);
        }
    }

    public static void handleGunnerDrops(net.minecraft.world.entity.LivingEntity entity, List<ItemEntity> drops) {
        // Handle JEG faction gunners and special gunner types
        if (!isGunner(entity)) {
            return;
        }

        ItemStack held = entity.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            // Remove vanilla ranged weapons from gunner drops
            if (entity instanceof net.minecraft.world.entity.monster.skeleton.Skeleton) {
                drops.removeIf(drop -> drop.getItem().is(Items.BOW) || drop.getItem().is(Items.ARROW));
            }
            return;
        }

        // Remove vanilla ranged weapons from all gunner types
        if (entity instanceof net.minecraft.world.entity.monster.skeleton.Skeleton) {
            drops.removeIf(drop -> drop.getItem().is(Items.BOW) || drop.getItem().is(Items.ARROW));
        } else if (entity instanceof net.minecraft.world.entity.monster.zombie.Zombie || entity instanceof net.minecraft.world.entity.monster.zombie.Husk) {
            drops.removeIf(drop -> drop.getItem().is(Items.IRON_SHOVEL) || drop.getItem().is(Items.IRON_SWORD));
        }

        RandomSource random = entity.getRandom();
        GunStats stats = gunItem.getStats();

        if (random.nextFloat() < 0.06F) {
            ItemStack dropGun = held.copy();
            GunnerProgression.damageWeaponToLowDurability(dropGun, random);
            addDrop(drops, entity, dropGun);
            return;
        }

        // Reduced ammo drop chance to 60% (was 100%)
        if (random.nextFloat() < 0.60F) {
            ItemStack ammo = buildAmmoDrop(stats, random);
            if (!ammo.isEmpty()) {
                addDrop(drops, entity, ammo);
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

    private static void addDrop(List<ItemEntity> drops, net.minecraft.world.entity.LivingEntity entity, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
        item.setDefaultPickUpDelay();
        drops.add(item);
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
        List<Item> guns = new java.util.ArrayList<>();
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
