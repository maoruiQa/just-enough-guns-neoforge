package recipecheck;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("recipe_map_check")
public final class NeoCheck {
    public NeoCheck() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> CommonCheck.tick(event.getServer()));
    }
}
