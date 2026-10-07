package ttv.migami.jeg.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.gun.MagazineRecipeAccess;

@Mixin(MinecraftServer.class)
public abstract class MagazineModeServerMixin {
    @Inject(method = "getRecipeManager", at = @At("RETURN"))
    private void jeg$filterRecipes(CallbackInfoReturnable<RecipeManager> callback) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        // Server configs have loaded once the overworld exists; raw recipes remain available before then.
        if (server.overworld() != null && ((MagazineRecipeAccess) callback.getReturnValue()).jeg$applyMagazineMode(Config.magazineFeedEnabled())) {
            
        }
    }
}
