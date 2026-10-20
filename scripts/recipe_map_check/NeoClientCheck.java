package recipecheck;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod(value = "recipe_map_check", dist = Dist.CLIENT)
public final class NeoClientCheck {
    public NeoClientCheck() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientCheck.tick(Minecraft.getInstance()));
    }
}
