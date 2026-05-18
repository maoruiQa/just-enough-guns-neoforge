package ttv.migami.jeg.vehicle.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;

public final class VehicleAssemblyRecipeManager {
    private static final Gson GSON = new Gson();
    private static final Identifier TEST_WHEEL_RECIPE = Reference.id("test_wheel_vehicle");
    private static final Set<Identifier> ENABLED_ASSEMBLY_VEHICLES = Set.of(
            Reference.id("lav150"),
            Reference.id("bmp2"),
            Reference.id("truck"),
            Reference.id("speedboat"),
            Reference.id("ah6"),
            Reference.id("mi28")
    );
    private static volatile Map<Identifier, VehicleAssemblyRecipe> recipes = defaultsOnly();

    private VehicleAssemblyRecipeManager() {}

    public static Loader reloadListener() {
        return new Loader();
    }

    public static VehicleAssemblyRecipe testWheelRecipe() {
        return recipes.get(TEST_WHEEL_RECIPE);
    }

    public static VehicleAssemblyRecipe get(Identifier id) {
        VehicleAssemblyRecipe recipe = recipes.get(id);
        return recipe != null && isEnabledAssemblyVehicle(recipe) ? recipe : null;
    }

    public static List<VehicleAssemblyRecipe> recipes() {
        return recipes.values().stream()
                .filter(VehicleAssemblyRecipeManager::isEnabledAssemblyVehicle)
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
    }

    private static boolean isEnabledAssemblyVehicle(VehicleAssemblyRecipe recipe) {
        return ENABLED_ASSEMBLY_VEHICLES.contains(recipe.resultVehicle());
    }

    private static Map<Identifier, VehicleAssemblyRecipe> defaultsOnly() {
        Identifier lightCombatRecipe = Reference.id("light_combat_vehicle");
        Identifier testHelicopterRecipe = Reference.id("test_helicopter");
        Identifier testBoatRecipe = Reference.id("test_boat");
        Identifier testArtilleryRecipe = Reference.id("test_artillery");
        Identifier testAircraftRecipe = Reference.id("test_aircraft");
        return Map.of(
                TEST_WHEEL_RECIPE, new VehicleAssemblyRecipe(
                        TEST_WHEEL_RECIPE,
                        Reference.id("test_wheel_vehicle"),
                        List.of(
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("iron_ingot"), 16),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("redstone"), 4)
                        )
                ),
                lightCombatRecipe, new VehicleAssemblyRecipe(
                        lightCombatRecipe,
                        Reference.id("light_combat_vehicle"),
                        List.of(
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("iron_ingot"), 32),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("copper_ingot"), 12),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("redstone"), 8)
                        )
                ),
                testHelicopterRecipe, new VehicleAssemblyRecipe(
                        testHelicopterRecipe,
                        Reference.id("test_helicopter"),
                        List.of(
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("iron_ingot"), 24),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("copper_ingot"), 16),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("redstone"), 10),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("feather"), 8)
                        )
                ),
                testBoatRecipe, new VehicleAssemblyRecipe(
                        testBoatRecipe,
                        Reference.id("test_boat"),
                        List.of(
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("iron_ingot"), 16),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("oak_planks"), 12),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("redstone"), 4)
                        )
                ),
                testArtilleryRecipe, new VehicleAssemblyRecipe(
                        testArtilleryRecipe,
                        Reference.id("test_artillery"),
                        List.of(
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("iron_ingot"), 28),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("copper_ingot"), 8),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("redstone"), 8)
                        )
                ),
                testAircraftRecipe, new VehicleAssemblyRecipe(
                        testAircraftRecipe,
                        Reference.id("test_aircraft"),
                        List.of(
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("iron_ingot"), 28),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("copper_ingot"), 18),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("redstone"), 12),
                                new VehicleAssemblyRecipe.Ingredient(Identifier.withDefaultNamespace("feather"), 12)
                        )
                )
        );
    }

    public static final class Loader extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> implements IdentifiableResourceReloadListener {
        private static final FileToIdConverter LISTER = FileToIdConverter.json("vehicle_assembly");

        @Override
        public Identifier getFabricId() {
            return Reference.id("vehicle_assembly");
        }

        @Override
        protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<Identifier, JsonElement> objects = new HashMap<>();
            LISTER.listMatchingResources(manager).forEach((fileId, resource) -> {
                Identifier id = LISTER.fileToId(fileId);
                try (var reader = resource.openAsReader()) {
                    objects.put(id, GsonHelper.fromJson(GSON, reader, JsonElement.class));
                } catch (IOException | RuntimeException exception) {
                    JustEnoughGuns.LOGGER.error("Failed to read vehicle assembly recipe {}: {}", id, exception.getMessage());
                }
            });
            return objects;
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            Map<Identifier, VehicleAssemblyRecipe> loaded = new HashMap<>(defaultsOnly());
            for (Map.Entry<Identifier, JsonElement> entry : objects.entrySet()) {
                try {
                    loaded.put(entry.getKey(), VehicleAssemblyRecipeSerializer.fromJson(entry.getKey(), entry.getValue().getAsJsonObject()));
                } catch (RuntimeException exception) {
                    JustEnoughGuns.LOGGER.error("Failed to load vehicle assembly recipe {}: {}", entry.getKey(), exception.getMessage());
                }
            }
            recipes = Map.copyOf(loaded);
            JustEnoughGuns.LOGGER.info("Loaded {} JEG vehicle assembly recipes", recipes.size());
        }
    }
}
