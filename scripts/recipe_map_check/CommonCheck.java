package recipecheck;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.*;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.config.ServerConfigEditor;
import ttv.migami.jeg.gun.MagazineModeRules;

/** Loaded only by the standalone regression fixture, never by the published mod. */
public final class CommonCheck {
    private static String last = "";
    private static int ticks;
    private static boolean ready;

    public static Path output() { return Path.of(System.getProperty("recipecheck.server")); }

    public static void write(Path directory, String name, Object value) {
        try {
            Files.createDirectories(directory);
            Path temporary = directory.resolve(name + ".tmp");
            Files.writeString(temporary, new Gson().toJson(value));
            Files.move(temporary, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { throw new AssertionError("Cannot write recipe evidence", e); }
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % 10 != 0) return;
        try {
            Path directory = output();
            if (!ready) {
                write(directory, "ready.json", Map.of("ticks", ticks));
                ready = true;
            }
            Path control = directory.resolve("control.txt");
            if (!Files.exists(control)) return;
            String command = Files.readString(control).strip();
            if (command.isEmpty() || command.equals(last)) return;
            last = command;
            String[] parts = command.split("\\|");
            String label = parts[0];
            if (parts[1].equals("mode")) {
                ServerConfigEditor.apply(server, Map.of("combat.magazineFeed", parts[2]));
            }
            if (parts[1].equals("reload")) {
                server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((result, error) ->
                    server.execute(() -> {
                        if (error != null) fail(error);
                        else verify(server, label);
                    }));
            } else verify(server, label);
        } catch (Throwable error) { fail(error); }
    }

    private static void fail(Throwable error) {
        write(output(), "failure.json", error.toString());
        throw new AssertionError("Recipe regression failed", error);
    }

    private static List<String> ids(Collection<RecipeHolder<?>> recipes) {
        return recipes.stream().map(h -> h.id().identifier().toString()).sorted().toList();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void verify(MinecraftServer server, String label) {
        var raw = server.reloadableRegistries().lookup().lookupOrThrow(Registries.RECIPE).listElements().toList();
        boolean mode = Config.magazineFeedEnabled();
        var expected = raw.stream().filter(h -> MagazineModeRules.allows(h.key().identifier().toString(), mode)).toList();
        var manager = server.getRecipeManager();
        var expectedIds = expected.stream().map(h -> h.key().identifier().toString()).sorted().toList();
        require(ids(manager.getRecipes()).equals(expectedIds), "Full recipe set differs from filtered registry");
        var controlled = new HashSet<>(MagazineModeRules.CAPACITY_RECIPES);
        controlled.addAll(MagazineModeRules.MAGAZINE_RECIPES);
        var rawIds = raw.stream().map(h -> h.key().identifier().toString()).collect(Collectors.toSet());
        require(rawIds.containsAll(controlled), "Missing one of the 14 controlled recipes");
        require(rawIds.contains("minecraft:crafting_table"), "Missing vanilla control recipe");
        require(expectedIds.stream().anyMatch(id -> id.startsWith("jeg:") && !controlled.contains(id)), "Missing ordinary JEG recipes");
        for (var holder : raw) {
            require(manager.byKey(holder.key()).isPresent() == expectedIds.contains(holder.key().identifier().toString()),
                    "byKey disagrees for " + holder.key());
        }
        var learnable = expected.stream().filter(h -> !h.value().isSpecial()).map(h -> h.key().identifier().toString()).sorted().toList();
        require(ids(manager.getLearnableRecipes()).equals(learnable), "Learnable recipes differ");
        var shapeless = manager.getRecipes().stream().filter(h -> h.value().getSerializer() == ShapelessRecipe.SERIALIZER).toList();
        require(!shapeless.isEmpty(), "No shapeless recipes to synchronize");
        var displays = new TreeSet<Integer>();
        for (var player : server.getPlayerList().getPlayers()) {
            for (var recipe : manager.getRecipes()) {
                if (player.getRecipeBook().contains(recipe.id())) {
                    manager.listDisplaysForRecipe(recipe.id(), entry -> displays.add(entry.id().index()));
                }
            }
        }
        write(output(), label + ".json", Map.of("mode", mode, "all", expectedIds, "shapeless", ids(shapeless),
                "learnable", learnable, "players", server.getPlayerList().getPlayerCount(), "displays", displays));
        System.out.println("RECIPE_CHECK " + label + " mode=" + mode + " recipes=" + expectedIds.size());
    }
}
