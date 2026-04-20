package ttv.migami.jeg.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import java.util.stream.StreamSupport;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.TerrorPhantom;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.gun.GunStats;

public final class GunEvents {
    private static final String MANUAL_GRANTED_TAG = "jeg_manual_granted";
    private static final String FINGER_GUN_RECIPE_GRANTED_TAG = "jeg_finger_gun_recipe_granted";

    // Tags for JEG faction gunners (replacing old individual gunner tags)
    public static final String JEG_GUNNER_TAG = "MobGunner";
    public static final String JEG_ELITE_GUNNER_TAG = "EliteGunner";

    private static final ResourceLocation[] DEFAULT_PILLAGER_GUNS = new ResourceLocation[] {
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
            if (pillager.getTags().contains("jeg_pillager_gunner")) {
                // Only equip if pillager doesn't already have a gun
                if (!isHoldingGun(pillager)) {
                    ResourceLocation selected = selectRandomGun(event.getLevel().getRandom());
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
                .filter(holder -> holder.id().getNamespace().equals(Reference.MOD_ID))
                .count();
        if (count == 0) {
            ttv.migami.jeg.JustEnoughGuns.LOGGER.warn("No {} recipes were loaded; verify data pack paths", Reference.MOD_ID);
        } else {
            ttv.migami.jeg.JustEnoughGuns.LOGGER.info("Loaded {} {} recipes", count, Reference.MOD_ID);
        }
    }

    private static void grantStartingManual(ServerPlayer player) {
        boolean manualGranted = player.getPersistentData().contains(MANUAL_GRANTED_TAG) && player.getPersistentData().getBoolean(MANUAL_GRANTED_TAG);
        if (!manualGranted) {
            player.awardRecipesByKey(ModItems.manualRecipes().stream().map(ResourceKey::location).toList());
            player.awardRecipesByKey(ModItems.unlockGunRecipeKeys().stream().map(ResourceKey::location).toList());

            player.getPersistentData().putBoolean(MANUAL_GRANTED_TAG, true);
        }

        boolean fingerGunGranted = player.getPersistentData().contains(FINGER_GUN_RECIPE_GRANTED_TAG) && player.getPersistentData().getBoolean(FINGER_GUN_RECIPE_GRANTED_TAG);
        if (!fingerGunGranted) {
            player.awardRecipesByKey(java.util.List.of(Reference.id("finger_gun")));
            player.getPersistentData().putBoolean(FINGER_GUN_RECIPE_GRANTED_TAG, true);
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
                .append(Component.literal(" - Unlock all gun recipes")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(true)));

        player.sendSystemMessage(header);
        player.sendSystemMessage(divider);
        player.sendSystemMessage(unlockLine);

        if (player.hasPermissions(2)) {
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

        double conversionChance = Config.pillagerGunnerChance();

        // Replaced serverLevel.random with pillager.getRandom()
        if (conversionChance <= 0.0D || pillager.getRandom().nextDouble() >= conversionChance) {
            return;
        }

        // Since GunnerEntity was removed, we just tag the pillager instead
        pillager.addTag("jeg_pillager_gunner");

        // Replaced serverLevel.random with pillager.getRandom()
        ResourceLocation selected = selectRandomGun(pillager.getRandom());
        if (selected != null) {
            equipPillagerWithGun(pillager, selected);
        }
    }

    static void equipPillagerWithGun(Pillager pillager, ResourceLocation gunId) {
        var holder = ModItems.GUNS.get(gunId);
        if (holder == null) {
            return;
        }

        ItemStack stack = new ItemStack(holder.get());
        pillager.setItemInHand(InteractionHand.MAIN_HAND, stack);
        pillager.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
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

        if (event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.NATURAL) {
            double terrorChance = Config.terrorPhantomChance();
            if (terrorChance > 0.0D && phantom.getRandom().nextDouble() < terrorChance) {
                TerrorPhantom terror = new TerrorPhantom(ModEntities.TERROR_PHANTOM.get(), serverLevel);
                if (terror != null) {
                    terror.setPos(phantom.getX(), phantom.getY(), phantom.getZ());
                    terror.setYRot(phantom.getYRot());
                    terror.setXRot(phantom.getXRot());
                    terror.setDeltaMovement(phantom.getDeltaMovement());
                    terror.finalizeSpawn(serverLevel, event.getDifficulty(), net.minecraft.world.entity.MobSpawnType.EVENT, event.getSpawnData());
                    serverLevel.addFreshEntity(terror);
                    phantom.discard();
                    return;
                }
            }

            double gunnerChance = Config.phantomGunnerChance();
            if (gunnerChance <= 0.0D || phantom.getRandom().nextDouble() >= gunnerChance) {
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
        gunner.finalizeSpawn(serverLevel, event.getDifficulty(), net.minecraft.world.entity.MobSpawnType.EVENT, event.getSpawnData());
        serverLevel.addFreshEntity(gunner);
        phantom.discard();
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        net.minecraft.world.entity.LivingEntity entity = event.getEntity();

        // Handle JEG faction gunners and special gunner types
        if (!isGunner(entity)) {
            return;
        }

        ItemStack held = entity.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            // Remove vanilla ranged weapons from gunner drops
            if (entity instanceof net.minecraft.world.entity.monster.Skeleton) {
                event.getDrops().removeIf(drop -> drop.getItem().is(Items.BOW) || drop.getItem().is(Items.ARROW));
            }
            return;
        }

        // Remove vanilla ranged weapons from all gunner types
        if (entity instanceof net.minecraft.world.entity.monster.Skeleton) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.BOW) || drop.getItem().is(Items.ARROW));
        } else if (entity instanceof net.minecraft.world.entity.monster.Zombie || entity instanceof net.minecraft.world.entity.monster.Husk) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.IRON_SHOVEL) || drop.getItem().is(Items.IRON_SWORD));
        }

        RandomSource random = entity.getRandom();
        GunStats stats = gunItem.getStats();

        // Reduced gun drop chance from 12% to 8% for better balance
        if (random.nextFloat() < 0.08F) {
            ItemStack dropGun = held.copy();
            if (dropGun.isDamageableItem()) {
                int maxDamage = dropGun.getMaxDamage();
                int damage = Mth.nextInt(random, Math.max(1, (int) (maxDamage * 0.65F)), Math.max(1, (int) (maxDamage * 0.9F)));
                dropGun.setDamageValue(Math.min(maxDamage - 1, damage));
            }
            addDrop(event, entity, dropGun);
            return;
        }

        // Reduced ammo drop chance to 60% (was 100%)
        if (random.nextFloat() < 0.60F) {
            ItemStack ammo = buildAmmoDrop(stats, random);
            if (!ammo.isEmpty()) {
                addDrop(event, entity, ammo);
            }
        }
    }

    private static boolean isGunner(net.minecraft.world.entity.LivingEntity entity) {
        // Check for JEG faction gunners
        if (entity.getTags().contains(JEG_GUNNER_TAG) || entity.getTags().contains(JEG_ELITE_GUNNER_TAG)) {
            return true;
        }

        // Check for special gunner types (phantom gunner, pillager gunner)
        return entity.getTags().contains("jeg_pillager_gunner") ||
               entity instanceof PhantomGunner ||
               entity instanceof TerrorPhantom;
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

    private static ResourceLocation selectRandomGun(RandomSource random) {
        if (DEFAULT_PILLAGER_GUNS.length == 0) {
            return null;
        }
        return DEFAULT_PILLAGER_GUNS[random.nextInt(DEFAULT_PILLAGER_GUNS.length)];
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
