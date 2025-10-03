package ttv.migami.jeg.event;

import net.minecraft.resources.ResourceLocation;
import java.util.stream.StreamSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.GunnerEntity;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;

public final class GunEvents {
    private static final String MANUAL_GRANTED_TAG = "jeg_manual_granted";
    private static final String SKELETON_GUNNER_TAG = "jeg_gunner";
    private static final float PILLAGER_GUNNER_CHANCE = 0.2F;
    private static final float SKELETON_GUNNER_CHANCE = 1.0F / 7.0F;
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

        if (serverLevel.random.nextFloat() > PILLAGER_GUNNER_CHANCE) {
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

        if (skeleton.getRandom().nextFloat() < SKELETON_GUNNER_CHANCE) {
            skeleton.addTag(SKELETON_GUNNER_TAG);
        }
    }

    private static ResourceLocation selectRandomGun(RandomSource random) {
        if (DEFAULT_PILLAGER_GUNS.length == 0) {
            return null;
        }
        return DEFAULT_PILLAGER_GUNS[random.nextInt(DEFAULT_PILLAGER_GUNS.length)];
    }

    private static void equipSkeletonWithGun(net.minecraft.world.entity.monster.Skeleton skeleton, RandomSource random) {
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
        skeleton.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.05F);
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
}
