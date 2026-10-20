package recipecheck;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;

public final class ClientCheck {
    private static int ticks;
    private static Set<Integer> lastDisplays;
    public static Path output() { return Path.of(System.getProperty("recipecheck.client")); }
    public static void tick(Minecraft client) {
        if (Files.exists(output().resolve("stop"))) { client.stop(); return; }
        if (client.player == null || client.level == null || ++ticks % 10 != 0) return;
        client.options.pauseOnLostFocus = false;
        if (client.gui.screen() != null && client.gui.screen().getClass().getName().contains("MagazineModeClient")) {
            client.gui.screen().onClose();
        }
        var displays = new TreeSet<Integer>();
        for (var collection : client.player.getRecipeBook().getCollections()) {
            for (var entry : collection.getRecipes()) displays.add(entry.id().index());
        }
        if (displays.equals(lastDisplays)) return;
        lastDisplays = displays;
        CommonCheck.write(output(), "joined-" + ticks + ".json", Map.of("player", client.player.getUUID().toString(),
                "ticks", ticks, "displays", displays));
    }
}
