package ttv.migami.jeg.vehicle.recipe;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record VehicleAssemblyRecipe(ResourceLocation id, ResourceLocation resultVehicle, List<Ingredient> ingredients) {
    public record Ingredient(ResourceLocation item, int count) {}
}
