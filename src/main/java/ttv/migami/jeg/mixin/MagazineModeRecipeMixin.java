package ttv.migami.jeg.mixin;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
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
    @Shadow  @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable private RecipeMap recipes;
    @Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable private java.util.Collection<RecipeHolder<?>> learnableRecipes;
    @Unique private List<RecipeHolder<?>> jeg$learnable;
    @Unique private HolderLookup.RegistryLookup<Recipe<?>> jeg$originals;
    @Unique private Boolean jeg$mode;
    @Unique private long jeg$generation;
    @Unique private boolean jeg$dirty;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jeg$captureRecipes(HolderLookup.Provider registries, CallbackInfo callback) {
        jeg$originals = registries.lookupOrThrow(Registries.RECIPE);
        jeg$learnable = List.copyOf(learnableRecipes);
        jeg$generation++;
        jeg$dirty = true;
    }

    @Override public long jeg$recipeGeneration() { return jeg$generation; }

    @Override public boolean jeg$applyMagazineMode(boolean mode) {
        if (jeg$originals == null || (!jeg$dirty && Boolean.valueOf(mode).equals(jeg$mode))) return false;
        jeg$mode = mode;
        jeg$dirty = false;
        var original = jeg$originals;
        // The factory also initializes recipe indexes injected by the loader and other mods.
        recipes = RecipeMap.create(new HolderLookup.RegistryLookup.Delegate<Recipe<?>>() {
            @Override public HolderLookup.RegistryLookup<Recipe<?>> parent() { return original; }
            @Override public Stream<Holder.Reference<Recipe<?>>> listElements() {
                return original.listElements().filter(holder ->
                        MagazineModeRules.allows(holder.key().identifier().toString(), mode));
            }
        });
        learnableRecipes = jeg$learnable.stream().filter(holder -> MagazineModeRules.allows(holder.id().identifier().toString(), mode)).toList();
        return true;
    }

    // The holder overload can otherwise return a recipe cached before the mode changed.
    @ModifyVariable(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
            at = @At("HEAD"), argsOnly = true)
    private RecipeHolder<?> jeg$discardDisabledCache(RecipeHolder<?> holder) {
        return holder != null && jeg$mode != null && !MagazineModeRules.allows(holder.id().identifier().toString(), jeg$mode) ? null : holder;
    }
}
