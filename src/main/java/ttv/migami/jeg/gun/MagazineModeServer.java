package ttv.migami.jeg.gun;

import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.network.MagazineModePayload;
import ttv.migami.jeg.network.NetworkHandler;

/** Keeps all entry points (login, config editor and data reload) on the same server rules. */
public final class MagazineModeServer {
    private MagazineModeServer() {}

    public static void refresh(MinecraftServer server) {
        if (server.overworld() == null) return;
        MagazineModeData data = MagazineModeData.get(server);
        boolean changed = data.update(Config.magazineFeedEnabled());
        var recipes = server.getRecipeManager();
        var access = (MagazineRecipeAccess) recipes;
        if (!changed && data.appliedRecipes == recipes && data.appliedGeneration == access.jeg$recipeGeneration()) return;
        data.appliedRecipes = recipes;
        data.appliedGeneration = access.jeg$recipeGeneration();
        JustEnoughGuns.LOGGER.info("Loaded {} jeg recipes ({} feed, revision {})",
                recipes.getRecipes().stream().filter(holder -> holder.id().getNamespace().equals("jeg")).count(),
                data.magazineFeed ? "magazine" : "direct", data.revision);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.invoker().onSyncDataPackContents(player, false);
            player.connection.send(new ClientboundUpdateRecipesPacket(recipes.getRecipes()));
            ArrayList<RecipeHolder<?>> allowed = new ArrayList<>();
            for (RecipeHolder<?> holder : recipes.getRecipes()) {
                if (holder.id().getNamespace().equals("jeg")) allowed.add(holder);
            }
            player.awardRecipes(allowed);
            player.getRecipeBook().sendInitialRecipeBook(player);
            player.inventoryMenu.slotsChanged(player.inventoryMenu.getCraftSlots());
            if (player.containerMenu instanceof CraftingMenu menu) menu.slotsChanged(menu.getSlot(1).container);
            player.containerMenu.broadcastChanges();
            if (changed) notify(player);
        }
    }

    public static void login(ServerPlayer player) {
        refresh(player.level().getServer());
        notify(player);
    }

    private static void notify(ServerPlayer player) {
        MagazineModeData data = MagazineModeData.get(player.level().getServer());
        String id = player.getUUID().toString();
        if (Objects.equals(data.confirmed.get(id), data.revision)) return;
        if (Objects.equals(data.pending.put(player.getUUID(), data.revision), data.revision)) return;
        NetworkHandler.sendMagazineMode(player, new MagazineModePayload(data.magazineFeed, data.revision, data.confirmed.containsKey(id)));
    }

    public static void confirm(ServerPlayer player, long revision) {
        MagazineModeData data = MagazineModeData.get(player.level().getServer());
        if (revision != data.revision || !Objects.equals(data.pending.get(player.getUUID()), revision)) return;
        data.confirmed.put(player.getUUID().toString(), revision);
        data.pending.remove(player.getUUID());
        data.setDirty();
    }

    public static void logout(ServerPlayer player) {
        MagazineModeData.get(player.level().getServer()).pending.remove(player.getUUID());
    }
}
