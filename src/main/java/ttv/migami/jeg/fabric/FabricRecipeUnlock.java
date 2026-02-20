package ttv.migami.jeg.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.JustEnoughGuns;

public final class FabricRecipeUnlock {
    private FabricRecipeUnlock() {}

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            // Ensure we run on the server thread so recipe-book sync happens reliably.
            server.execute(() -> unlockAllRecipes(server, player));
        });
    }

    private static void unlockAllRecipes(MinecraftServer server, ServerPlayer player) {
        RecipeManager recipeManager = server.getRecipeManager();

        // Award all loaded recipes under the "jeg" namespace. This avoids relying on a
        // hardcoded list and ensures the client recipe book actually syncs.
        java.util.ArrayList<RecipeHolder<?>> holders = new java.util.ArrayList<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.id().getNamespace().equals(Reference.MOD_ID)) {
                holders.add(holder);
            }
        }

        if (!holders.isEmpty()) {
            player.awardRecipes(holders);
            JustEnoughGuns.LOGGER.info("Awarded {} {} recipes to {}", holders.size(), Reference.MOD_ID, player.getName().getString());
        } else {
            JustEnoughGuns.LOGGER.warn("No {} recipes found to award; verify data pack paths", Reference.MOD_ID);
        }
    }
}
