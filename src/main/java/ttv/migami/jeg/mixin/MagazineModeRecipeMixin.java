package ttv.migami.jeg.mixin;

import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.resources.ResourceLocation;
import com.google.gson.JsonElement;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.gun.MagazineModeRules;
import ttv.migami.jeg.gun.MagazineRecipeAccess;

@Mixin(RecipeManager.class)
public abstract class MagazineModeRecipeMixin implements MagazineRecipeAccess {
    @Shadow public abstract java.util.Collection<RecipeHolder<?>> getRecipes();
    @Shadow public abstract void replaceRecipes(Iterable<RecipeHolder<?>> recipes);
    @Unique private List<RecipeHolder<?>> jeg$originals;
    @Unique private Boolean jeg$mode;
    @Unique private long jeg$generation;
    @Unique private boolean jeg$dirty;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void jeg$captureRecipes(Map<ResourceLocation, JsonElement> entries, ResourceManager resources, ProfilerFiller profiler, CallbackInfo callback) {
        jeg$originals = List.copyOf(getRecipes());
        
        jeg$generation++;
        jeg$dirty = true;
    }

    @Override public long jeg$recipeGeneration() { return jeg$generation; }

    @Override public boolean jeg$applyMagazineMode(boolean mode) {
        if (jeg$originals == null || (!jeg$dirty && Boolean.valueOf(mode).equals(jeg$mode))) return false;
        jeg$mode = mode;
        jeg$dirty = false;
        var allowed = jeg$originals.stream().filter(holder -> MagazineModeRules.allows(holder.id().toString(), mode)).toList();
        replaceRecipes(allowed);
        return true;
    }


}
