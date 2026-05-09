package ttv.migami.jeg.vehicle.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;

public final class VehicleAssemblyRecipeManager {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation TEST_WHEEL_RECIPE = Reference.id("test_wheel_vehicle");
    private static volatile Map<ResourceLocation, VehicleAssemblyRecipe> recipes = defaultsOnly();

    private VehicleAssemblyRecipeManager() {}

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static VehicleAssemblyRecipe testWheelRecipe() {
        return recipes.get(TEST_WHEEL_RECIPE);
    }

    public static VehicleAssemblyRecipe get(ResourceLocation id) {
        return recipes.get(id);
    }

    private static Map<ResourceLocation, VehicleAssemblyRecipe> defaultsOnly() {
        ResourceLocation lightCombatRecipe = Reference.id("light_combat_vehicle");
        return Map.of(
                TEST_WHEEL_RECIPE, new VehicleAssemblyRecipe(
                        TEST_WHEEL_RECIPE,
                        Reference.id("test_wheel_vehicle"),
                        List.of(
                                new VehicleAssemblyRecipe.Ingredient(ResourceLocation.withDefaultNamespace("iron_ingot"), 16),
                                new VehicleAssemblyRecipe.Ingredient(ResourceLocation.withDefaultNamespace("redstone"), 4)
                        )
                ),
                lightCombatRecipe, new VehicleAssemblyRecipe(
                        lightCombatRecipe,
                        Reference.id("light_combat_vehicle"),
                        List.of(
                                new VehicleAssemblyRecipe.Ingredient(ResourceLocation.withDefaultNamespace("iron_ingot"), 32),
                                new VehicleAssemblyRecipe.Ingredient(ResourceLocation.withDefaultNamespace("copper_ingot"), 12),
                                new VehicleAssemblyRecipe.Ingredient(ResourceLocation.withDefaultNamespace("redstone"), 8)
                        )
                )
        );
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private Loader() {
            super(GSON, "vehicle_assembly");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, VehicleAssemblyRecipe> loaded = new HashMap<>(defaultsOnly());
            for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
                try {
                    loaded.put(entry.getKey(), parse(entry.getKey(), entry.getValue().getAsJsonObject()));
                } catch (RuntimeException exception) {
                    JustEnoughGuns.LOGGER.error("Failed to load vehicle assembly recipe {}: {}", entry.getKey(), exception.getMessage());
                }
            }
            recipes = Map.copyOf(loaded);
            JustEnoughGuns.LOGGER.info("Loaded {} JEG vehicle assembly recipes", recipes.size());
        }
    }

    private static VehicleAssemblyRecipe parse(ResourceLocation id, JsonObject object) {
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
