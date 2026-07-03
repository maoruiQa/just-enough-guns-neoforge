package ttv.migami.jeg.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import ttv.migami.jeg.fabric.compat.neoforge.bus.api.SubscribeEvent;
import ttv.migami.jeg.fabric.compat.neoforge.fml.common.EventBusSubscriber;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.player.PlayerEvent;
import ttv.migami.jeg.Reference;

/**
 * Automatically unlocks all JEG mod recipes when a player joins the game.
 * This ensures players have immediate access to all crafting recipes without
 * needing to discover them through gameplay.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public final class RecipeUnlockHandler {
    private RecipeUnlockHandler() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Get all recipes from the server's recipe manager
        List<RecipeHolder<?>> jegRecipes = new ArrayList<>();
        for (RecipeHolder<?> holder : player.getServer().getRecipeManager().getRecipes()) {
            ResourceLocation recipeId = holder.id();
            // Only unlock recipes from our mod (jeg namespace)
            if (recipeId.getNamespace().equals(Reference.MOD_ID)) {
                jegRecipes.add(holder);
            }
        }

        // Award all JEG recipes to the player
        if (!jegRecipes.isEmpty()) {
            player.awardRecipes(jegRecipes);
        }
    }
}
