package recipecheck;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;

public final class FabricClientCheck implements ClientModInitializer {
    private static int sequence;
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientCheck::tick);
        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> {
            var ids = recipes.recipes().stream().map(h -> h.id().identifier().toString()).sorted().toList();
            CommonCheck.write(ClientCheck.output(), "sync-" + (++sequence) + ".json", ids);
        });
    }
}
