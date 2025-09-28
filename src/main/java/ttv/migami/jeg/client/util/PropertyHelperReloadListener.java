package ttv.migami.jeg.client.util;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;

/**
 * Tiny reload listener that refreshes the cached property data whenever client resources reload.
 */
public class PropertyHelperReloadListener extends SimplePreparableReloadListener<Void> {

    @Override
    protected CompletableFuture<Void> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
        PropertyHelper.resetCache();
    }
}
