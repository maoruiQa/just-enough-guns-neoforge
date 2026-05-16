package ttv.migami.jeg.vehicle.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class VehicleAssemblyRecipeSerializer {
    private VehicleAssemblyRecipeSerializer() {}

    public static VehicleAssemblyRecipe fromJson(ResourceLocation id, JsonObject object) {
        ResourceLocation resultVehicle = ResourceLocation.parse(object.get("result_vehicle").getAsString());
        List<VehicleAssemblyRecipe.Ingredient> ingredients = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray("ingredients")) {
            JsonObject ingredient = element.getAsJsonObject();
            ingredients.add(new VehicleAssemblyRecipe.Ingredient(
                    ResourceLocation.parse(ingredient.get("item").getAsString()),
                    ingredient.get("count").getAsInt()
            ));
        }
        return new VehicleAssemblyRecipe(id, resultVehicle, List.copyOf(ingredients));
    }
}
