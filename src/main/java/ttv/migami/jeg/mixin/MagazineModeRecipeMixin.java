package ttv.migami.jeg.mixin;

import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
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
    @Shadow private RecipeMap recipes;
    @Unique private List<RecipeHolder<?>> jeg$originals;
    @Unique private Boolean jeg$mode;
    @Unique private long jeg$generation;
    @Unique private boolean jeg$dirty;

    @Inject(method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void jeg$captureRecipes(RecipeMap entries, ResourceManager resources, ProfilerFiller profiler, CallbackInfo callback) {
        jeg$originals = List.copyOf(recipes.values());
        
        jeg$generation++;
        jeg$dirty = true;
    }

    @Override public long jeg$recipeGeneration() { return jeg$generation; }

    @Override public boolean jeg$applyMagazineMode(boolean mode) {
        if (jeg$originals == null || (!jeg$dirty && Boolean.valueOf(mode).equals(jeg$mode))) return false;
        jeg$mode = mode;
        jeg$dirty = false;
        var allowed = jeg$originals.stream().filter(holder -> MagazineModeRules.allows(holder.id().identifier().toString(), mode)).toList();
        recipes = RecipeMap.create(allowed);
        return true;
    }

    // The holder overload can otherwise return a recipe cached before the mode changed.
    @ModifyVariable(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
            at = @At("HEAD"), argsOnly = true)
    private RecipeHolder<?> jeg$discardDisabledCache(RecipeHolder<?> holder) {
        return holder != null && jeg$mode != null && !MagazineModeRules.allows(holder.id().identifier().toString(), jeg$mode) ? null : holder;
    }
}
