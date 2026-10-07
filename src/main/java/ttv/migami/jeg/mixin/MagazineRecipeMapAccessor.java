package ttv.migami.jeg.mixin;

import java.util.Map;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeMap.class)
public interface MagazineRecipeMapAccessor {
    @Invoker("<init>")
    static RecipeMap jeg$create(Multimap<RecipeType<?>, RecipeHolder<?>> byType,
            Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey) { throw new AssertionError(); }
}
