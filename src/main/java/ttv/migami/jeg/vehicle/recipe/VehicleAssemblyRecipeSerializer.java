package ttv.migami.jeg.vehicle.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class VehicleAssemblyRecipeSerializer {
    private VehicleAssemblyRecipeSerializer() {}

    public static VehicleAssemblyRecipe fromJson(Identifier id, JsonObject object) {
        Identifier resultVehicle = Identifier.parse(object.get("result_vehicle").getAsString());
        List<VehicleAssemblyRecipe.Ingredient> ingredients = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray("ingredients")) {
            JsonObject ingredient = element.getAsJsonObject();
            ingredients.add(new VehicleAssemblyRecipe.Ingredient(
                    Identifier.parse(ingredient.get("item").getAsString()),
                    ingredient.get("count").getAsInt()
            ));
        }
        return new VehicleAssemblyRecipe(id, resultVehicle, List.copyOf(ingredients));
    }
}
