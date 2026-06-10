package ttv.migami.jeg.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ttv.migami.jeg.Reference;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class RecipeUnlockHandler {
    private RecipeUnlockHandler() {}

    public static final List<ResourceKey<Recipe<?>>> BULLETPROOF_RECIPES = List.of(
            key("bulletproof_vest_i"),
            key("bulletproof_vest_ii"),
            key("bulletproof_vest_iii"),
            key("bulletproof_vest_iv"),
            key("bulletproof_vest_v"),
            key("bulletproof_vest_vi"),
            key("bulletproof_helmet_i"),
            key("bulletproof_helmet_ii"),
            key("bulletproof_helmet_iii"),
            key("bulletproof_helmet_iv"),
            key("bulletproof_helmet_v"),
            key("bulletproof_helmet_vi"));

    private static ResourceKey<Recipe<?>> key(String path) {
        Identifier id = Reference.id(path);
        return ResourceKey.create(Registries.RECIPE, id);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        List<RecipeHolder<?>> jegRecipes = new ArrayList<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (holder.id().identifier().getNamespace().equals(Reference.MOD_ID)) {
                jegRecipes.add(holder);
            }
        }

        if (!jegRecipes.isEmpty()) {
            player.awardRecipes(jegRecipes);
        }
    }
}
