package ttv.migami.jeg.crafting.workbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import net.neoforged.neoforge.common.crafting.conditions.ICondition;
import net.neoforged.neoforge.registries.ForgeRegistries;
import ttv.migami.jeg.init.ModRecipeSerializers;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AbstractWorkbenchRecipeBuilder {
    @Nullable
    private final RecipeCategory category;
    private final Item result;
    private final int count;
    private final List<WorkbenchIngredient> ingredients;
    private final Advancement.Builder advancementBuilder;
    private final List<ICondition> conditions = new ArrayList<>();

    protected AbstractWorkbenchRecipeBuilder(@Nullable RecipeCategory category, Item item, int count) {
        this.category = category;
        this.result = item.asItem();
        this.count = count;
        this.ingredients = new ArrayList<>();
        this.advancementBuilder = Advancement.Builder.advancement();
    }

    public static AbstractWorkbenchRecipeBuilder crafting(Item item) {
        return new AbstractWorkbenchRecipeBuilder(null, item, 1);
    }

    public static AbstractWorkbenchRecipeBuilder crafting(Item item, int count) {
        return new AbstractWorkbenchRecipeBuilder(null, item, count);
    }

    public static AbstractWorkbenchRecipeBuilder crafting(@Nullable RecipeCategory category, Item item) {
        return new AbstractWorkbenchRecipeBuilder(category, item, 1);
    }

    public static AbstractWorkbenchRecipeBuilder crafting(@Nullable RecipeCategory category, Item item, int count) {
        return new AbstractWorkbenchRecipeBuilder(category, item, count);
    }

    public AbstractWorkbenchRecipeBuilder addIngredient(Item item, int count) {
        this.ingredients.add(WorkbenchIngredient.of(item, count));
        return this;
    }

    public AbstractWorkbenchRecipeBuilder addIngredient(WorkbenchIngredient ingredient) {
        this.ingredients.add(ingredient);
        return this;
    }

    public AbstractWorkbenchRecipeBuilder addCriterion(String name, CriterionTriggerInstance criterionIn) {
        this.advancementBuilder.addCriterion(name, criterionIn);
        return this;
    }

    public AbstractWorkbenchRecipeBuilder addCondition(ICondition condition) {
        this.conditions.add(condition);
        return this;
    }

    public void build(Consumer<FinishedRecipe> consumer) {
        ResourceLocation resourcelocation = ForgeRegistries.ITEMS.getKey(this.result);
        this.build(consumer, resourcelocation);
    }

    public void build(Consumer<FinishedRecipe> consumer, ResourceLocation id) {
        this.validate(id);
        this.advancementBuilder.parent(new ResourceLocation("recipes/root"))
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(RequirementsStrategy.OR);

        consumer.accept(new AbstractWorkbenchRecipeBuilder.Result(id, this.result, this.count, this.ingredients, this.conditions, this.advancementBuilder, new ResourceLocation(id.getNamespace(), "recipes/" + (this.category != null ? this.category.getFolderName() : "") + "/" + id.getPath())));
    }

    private void validate(ResourceLocation id) {
        if (this.advancementBuilder.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    public static class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final Item item;
        private final int count;
        private final List<WorkbenchIngredient> ingredients;
        private final List<ICondition> conditions;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;

        public Result(ResourceLocation id, Item item, int count, List<WorkbenchIngredient> ingredients, List<ICondition> conditions, Advancement.Builder advancement, ResourceLocation advancementId) {
            this.id = id;
            this.item = item.asItem();
            this.count = count;
            this.ingredients = ingredients;
            this.conditions = conditions;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            JsonArray conditions = new JsonArray();
            this.conditions.forEach(condition -> conditions.add(CraftingHelper.serialize(condition)));
            if (!conditions.isEmpty()) {
                json.add("conditions", conditions);
            }

            JsonArray materials = new JsonArray();
            this.ingredients.forEach(ingredient -> materials.add(ingredient.toJson()));
            json.add("materials", materials);

            JsonObject resultObject = new JsonObject();
            resultObject.addProperty("item", ForgeRegistries.ITEMS.getKey(this.item).toString());
            if (this.count > 1) {
                resultObject.addProperty("count", this.count);
            }
            json.add("result", resultObject);
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ModRecipeSerializers.SCRAP_WORKBENCH.get();
        }

        @Override
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        @Override
        public ResourceLocation getAdvancementId() {
            return this.advancementId;
        }
    }
}