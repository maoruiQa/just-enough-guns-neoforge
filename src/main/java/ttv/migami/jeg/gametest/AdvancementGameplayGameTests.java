package ttv.migami.jeg.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.advancement.FireWeaponAttribution;
import ttv.migami.jeg.advancement.GameplayActions;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.SpecialExplosiveItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.menu.AttachmentMenu;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleController;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdvancementGameplayGameTests {
    private AdvancementGameplayGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void nativeTriggerMatchesAndPersists(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        AdvancementHolder advancement = advancement(helper, "guide/gunsmith/install_flashlight");
        helper.assertTrue(advancement.value().criteria().get("done").trigger() == GameplayActions.ACTION.get(),
                "Loaded criterion must reference the registered native trigger");
        GameplayActions.action(player, "attachment_install", "jeg:laser_pointer");
        GameplayActions.action(player, "attachment_use", "jeg:flashlight");
        helper.assertFalse(done(player, advancement), "Wrong subject or action must not match");

        helper.assertFalse(player.isCreative(), "Mock player must leave creative mode");
        helper.assertFalse(player.isSpectator(), "Mock player must leave spectator mode");
        GameplayActions.action(player, "attachment_install", "jeg:flashlight");
        helper.assertTrue(done(player, advancement), "Survival action and subject must match the native criterion");
        player.getAdvancements().save();
        player.getAdvancements().reload(helper.getLevel().getServer().getAdvancements());
        helper.assertTrue(done(player, advancement), "Awarded criterion must survive advancement save and reload");

        ServerPlayer creative = player(helper, GameType.CREATIVE);
        GameplayActions.action(creative, "attachment_install", "jeg:flashlight");
        helper.assertFalse(done(creative, advancement), "Creative players must not earn action criteria");
        ServerPlayer spectator = player(helper, GameType.SPECTATOR);
        GameplayActions.action(spectator, "attachment_install", "jeg:flashlight");
        helper.assertFalse(done(spectator, advancement), "Spectators must not earn action criteria");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hostileFilterAndFiredWeaponSnapshot(GameTestHelper helper) {
        ServerPlayer shooter = player(helper, GameType.SURVIVAL);
        var cow = helper.spawn(EntityType.COW, new Vec3(1.0D, 2.0D, 1.0D));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(2.0D, 2.0D, 1.0D));
        helper.assertFalse(GameplayActions.hostile(cow, shooter), "Passive animals are not hostile targets");
        helper.assertFalse(GameplayActions.hostile(shooter, shooter), "Players cannot be their own hostile target");
        helper.assertTrue(GameplayActions.hostile(zombie, shooter), "Hostile monsters count as valid targets");

        var ownVehicle = ModEntities.BMP2.get().create(helper.getLevel());
        helper.assertTrue(ownVehicle != null, "BMP-2 must exist for the self-vehicle filter");
        ownVehicle.setPos(helper.absoluteVec(new Vec3(4.0D, 2.0D, 1.0D)));
        ownVehicle.addTag(EnemyVehicleController.ENEMY_VEHICLE_TAG);
        Zombie vehicleShooter = new Zombie(helper.getLevel()) {
            @Override
            public Entity getVehicle() {
                return ownVehicle;
            }
        };
        helper.assertFalse(GameplayActions.hostile(ownVehicle, vehicleShooter), "Own vehicle must not count as hostile even with an enemy tag");

        ItemStack rifle = new ItemStack(ModItems.GUNS.get(Reference.id("assault_rifle")).get());
        shooter.setItemInHand(InteractionHand.MAIN_HAND, rifle);
        BulletEntity bullet = new BulletEntity(helper.getLevel(), shooter, ((GunItem) rifle.getItem()).getStats(), new Vec3(0.0D, 0.0D, 1.0D));
        shooter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUNS.get(Reference.id("revolver")).get()));
        helper.assertValueEqual(GameplayActions.weapon(bullet), "jeg:assault_rifle", "Hit credit must use the weapon fired, not the newly held gun");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void existingGunInventoryIsRecheckedAtLogin(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        AdvancementHolder obtain = advancement(helper, "guide/arsenal/obtain_assault_rifle");
        player.getInventory().setItem(0, new ItemStack(ModItems.GUNS.get(Reference.id("assault_rifle")).get()));
        player.getAdvancements().revoke(obtain, "obtained");
        helper.assertFalse(done(player, obtain), "Old inventory should start with the obtain criterion missing");
        GameplayActions.login(player);
        helper.assertTrue(done(player, obtain), "Login inventory recheck must recognize a gun already owned");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void openingKnownRecipeBookCreditsViewedRecipe(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        AdvancementHolder viewed = advancement(helper, "guide/training/recipes");
        var knownRecipe = player.server.getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.id().getNamespace().equals(Reference.MOD_ID)
                        && player.getRecipeBook().contains(recipe.id()))
                .findFirst();
        helper.assertTrue(knownRecipe.isPresent(), "Player must have a loaded, unlocked JEG recipe");
        player.getAdvancements().revoke(viewed, "done");
        helper.assertFalse(done(player, viewed), "Recipe view criterion must start unearned");
        player.connection.handleRecipeBookChangeSettingsPacket(
                new ServerboundRecipeBookChangeSettingsPacket(RecipeBookType.CRAFTING, true, false));
        helper.assertTrue(done(player, viewed), "Opening a known crafting recipe book must credit recipe view");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void successfulInstallAndTimedBlastCredit(GameTestHelper helper) {
        ServerPlayer owner = player(helper, GameType.SURVIVAL);
        ServerPlayer other = player(helper, GameType.SURVIVAL);
        AdvancementHolder install = advancement(helper, "guide/gunsmith/install_flashlight");
        AdvancementHolder timed = advancement(helper, "guide/special/timed_c4");

        ItemStack rifle = new ItemStack(ModItems.GUNS.get(Reference.id("assault_rifle")).get());
        owner.setItemInHand(InteractionHand.MAIN_HAND, rifle);
        AttachmentMenu menu = new AttachmentMenu(0, owner.getInventory());
        Slot special = menu.getSlot(AttachmentType.SPECIAL.ordinal());
        helper.assertFalse(special.mayPlace(new ItemStack(Items.STONE)), "Invalid item must be rejected by the attachment slot");
        helper.assertFalse(done(owner, install), "Rejected installation must not award progress");
        ItemStack flashlight = new ItemStack(ModItems.FLASHLIGHT.get());
        helper.assertTrue(special.mayPlace(flashlight), "Assault rifle must accept the flashlight");
        special.set(flashlight);
        helper.assertTrue(done(owner, install), "Written attachment must award the successful install");

        PlacedExplosiveEntity c4 = PlacedExplosiveEntity.placeSettled(helper.getLevel(), SpecialExplosiveItem.Kind.C4,
                owner, helper.absoluteVec(new Vec3(4.0D, 2.0D, 1.0D)), 0.0F, false);
        helper.getLevel().addFreshEntity(c4);
        c4.setBombTick(514);
        c4.tick();
        helper.assertTrue(c4.isRemoved(), "Timed C4 must actually detonate");
        helper.assertTrue(done(owner, timed), "Timed blast must credit its owner");
        helper.assertFalse(done(other, timed), "Timed blast must not credit another player");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void delayedFireCreditsRealDamageButNotFriendlyOrImmuneTargets(GameTestHelper helper) {
        ServerPlayer owner = player(helper, GameType.SURVIVAL);
        ServerPlayer filteredOwner = player(helper, GameType.SURVIVAL);
        AdvancementHolder use = advancement(helper, "guide/arsenal/use_flamethrower");
        BulletEntity flame = flame(helper, owner);
        BulletEntity filteredFlame = flame(helper, filteredOwner);
        var hostile = helper.spawn(EntityType.RAVAGER, new Vec3(2.0D, 2.0D, 1.0D));
        hostile.setNoAi(true);
        hostile.igniteForSeconds(2.0F);
        int previousFire = hostile.getRemainingFireTicks();
        hostile.igniteForSeconds(6.0F);
        helper.assertTrue(hostile.getRemainingFireTicks() > previousFire, "The JEG flame must actually extend ignition");
        FireWeaponAttribution.remember(hostile, flame, owner);
        helper.assertFalse(done(owner, use), "Ignition alone must not grant a weapon hit");
        helper.assertTrue(hostile.hurt(helper.getLevel().damageSources().onFire(), 2.0F), "Delayed fire must deal real damage");
        helper.assertTrue(done(owner, use), "Delayed fire damage must retain the fired flamethrower");

        var cow = helper.spawn(EntityType.COW, new Vec3(4.0D, 2.0D, 1.0D));
        cow.igniteForSeconds(6.0F);
        FireWeaponAttribution.remember(cow, filteredFlame, filteredOwner);
        cow.hurt(helper.getLevel().damageSources().onFire(), 2.0F);
        var blaze = helper.spawn(EntityType.BLAZE, new Vec3(6.0D, 2.0D, 1.0D));
        blaze.igniteForSeconds(6.0F);
        FireWeaponAttribution.remember(blaze, filteredFlame, filteredOwner);
        blaze.hurt(helper.getLevel().damageSources().onFire(), 2.0F);
        helper.assertFalse(done(filteredOwner, use), "Friendly and fire-immune targets must not grant a hit");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void lethalFireCreditsWeaponKillAndExpiredTagsDoNot(GameTestHelper helper) {
        ServerPlayer killer = player(helper, GameType.SURVIVAL);
        ServerPlayer expiredOwner = player(helper, GameType.SURVIVAL);
        AdvancementHolder kill = advancement(helper, "guide/training/kill");
        AdvancementHolder category = advancement(helper, "guide/arsenal/category_lmg");
        AdvancementHolder use = advancement(helper, "guide/arsenal/use_flamethrower");

        var doomed = helper.spawn(EntityType.ZOMBIE, new Vec3(2.0D, 2.0D, 1.0D));
        doomed.setNoAi(true);
        doomed.addTag(GunEvents.JEG_GUNNER_TAG);
        doomed.setHealth(1.0F);
        doomed.igniteForSeconds(6.0F);
        FireWeaponAttribution.remember(doomed, flame(helper, killer), killer);
        doomed.hurt(helper.getLevel().damageSources().onFire(), 3.0F);
        helper.assertTrue(done(killer, kill), "Lethal fire must award the real shooter");
        helper.assertTrue(done(killer, category), "Lethal fire must retain the gun category");
        helper.assertFalse(doomed.getTags().stream().anyMatch(tag -> tag.startsWith("jeg_fire_credit|")), "Death must consume source tags once");

        var survivor = helper.spawn(EntityType.RAVAGER, new Vec3(5.0D, 2.0D, 1.0D));
        survivor.setNoAi(true);
        survivor.igniteForSeconds(10.0F);
        FireWeaponAttribution.remember(survivor, flame(helper, expiredOwner), expiredOwner);
        survivor.setInvulnerable(true);
        long ignitionTime = helper.getLevel().getGameTime();
        helper.runAfterDelay(145, () -> {
            helper.assertTrue(helper.getLevel().getGameTime() >= ignitionTime + 120, "Game time must pass the source expiry");
            FireWeaponAttribution.onDamage(survivor, helper.getLevel().damageSources().onFire(), false, survivor.getHealth());
            helper.assertFalse(survivor.getTags().stream().anyMatch(tag -> tag.startsWith("jeg_fire_credit|")), "Expired fire source must be removed");
            survivor.setInvulnerable(false);
            survivor.hurt(helper.getLevel().damageSources().onFire(), 2.0F);
            helper.assertFalse(done(expiredOwner, use), "Fire after source expiry must not count as a gun hit");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void discardedC4SourceCreditsRealDamageToEnemyVehicle(GameTestHelper helper) {
        ServerPlayer owner = player(helper, GameType.SURVIVAL);
        owner.setInvulnerable(true);
        AdvancementHolder effect = advancement(helper, "guide/special/c4_effect");
        var bmp = ModEntities.BMP2.get().create(helper.getLevel());
        helper.assertTrue(bmp != null, "BMP-2 must be registered for the live damage test");
        bmp.setPos(helper.absoluteVec(new Vec3(4.0D, 2.0D, 1.0D)));
        bmp.addTag(EnemyVehicleController.ENEMY_VEHICLE_TAG);
        helper.getLevel().addFreshEntity(bmp);
        float healthBefore = bmp.vehicleHealth();
        PlacedExplosiveEntity c4 = PlacedExplosiveEntity.placeSettled(helper.getLevel(), SpecialExplosiveItem.Kind.C4,
                owner, helper.absoluteVec(new Vec3(2.0D, 2.0D, 1.0D)), 0.0F, true);
        helper.getLevel().addFreshEntity(c4);
        c4.detonate();
        helper.assertTrue(c4.isRemoved(), "Remote C4 must finish detonation before source attribution");
        // GameTestHooks suppresses world explosions; use the same source retained by the real blast.
        helper.assertTrue(bmp.hurt(helper.getLevel().damageSources().explosion(c4, owner), 200.0F),
                "Discarded C4 source must deal real server-side vehicle damage");
        helper.assertTrue(bmp.vehicleHealth() < healthBefore, "Damage must lower the BMP-2 hull");
        helper.assertTrue(done(owner, effect), "Vehicle damage must retain the discarded C4 weapon source");
        helper.succeed();
    }

    private static BulletEntity flame(GameTestHelper helper, ServerPlayer owner) {
        ItemStack gun = new ItemStack(ModItems.GUNS.get(Reference.id("flamethrower")).get());
        return new BulletEntity(helper.getLevel(), owner, ((GunItem) gun.getItem()).getStats(), new Vec3(0.0D, 0.0D, 1.0D));
    }

    private static AdvancementHolder advancement(GameTestHelper helper, String path) {
        AdvancementHolder result = helper.getLevel().getServer().getAdvancements().get(Reference.id(path));
        helper.assertTrue(result != null, "Advancement data must load: " + path);
        return result;
    }

    private static boolean done(ServerPlayer player, AdvancementHolder advancement) {
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static ServerPlayer player(GameTestHelper helper, GameType mode) {
        UUID id = UUID.randomUUID();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(id, "adv-" + id.toString().substring(0, 8)), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(mode);
        return player;
    }
}
