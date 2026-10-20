package recipecheck;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class FabricCheck implements ModInitializer {
    public void onInitialize() {
        RecipeSynchronization.synchronizeRecipeSerializer(ShapelessRecipe.SERIALIZER);
        ServerTickEvents.END_SERVER_TICK.register(CommonCheck::tick);
    }
}
