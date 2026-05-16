package ttv.migami.jeg.vehicle.recipe;

import java.util.List;
import net.minecraft.resources.Identifier;

public record VehicleAssemblyRecipe(Identifier id, Identifier resultVehicle, List<Ingredient> ingredients) {
    public record Ingredient(Identifier item, int count) {}
}
