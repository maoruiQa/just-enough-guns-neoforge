package ttv.migami.jeg.crafting.workbench;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Objects;

/**
 * Represents an ingredient in one of the custom workbench recipes, along with the amount required.
 */
public class WorkbenchIngredient {
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WorkbenchIngredient::ingredient,
            ByteBufCodecs.VAR_INT, WorkbenchIngredient::getCount,
            WorkbenchIngredient::of
    );

    private final Ingredient ingredient;
    private final JsonElement definition;
    private final int count;

    private WorkbenchIngredient(Ingredient ingredient, JsonElement definition, int count) {
        this.ingredient = ingredient;
        this.definition = definition;
        this.count = count;
    }

    private static WorkbenchIngredient of(Ingredient ingredient, int count) {
        JsonElement definition = encodeIngredient(ingredient).result().orElseGet(JsonObject::new);
        return new WorkbenchIngredient(ingredient, definition, count);
    }

    public static WorkbenchIngredient fromJson(JsonObject object) {
        int count = GsonHelper.getAsInt(object, "count", 1);
        JsonElement base = object.deepCopy();
        base.getAsJsonObject().remove("count");
        Ingredient ingredient = decodeIngredient(base);
        return new WorkbenchIngredient(ingredient, base, count);
    }

    public static WorkbenchIngredient of(ItemLike provider, int count) {
        Objects.requireNonNull(provider, "provider");
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(provider.asItem());
        if (id == null) {
            throw new IllegalArgumentException("Unregistered item: " + provider);
        }
        JsonObject json = new JsonObject();
        json.addProperty("item", id.toString());
        return new WorkbenchIngredient(decodeIngredient(json), json, count);
    }

    public static WorkbenchIngredient of(ItemStack stack, int count) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            throw new IllegalArgumentException("Unregistered item stack: " + stack);
        }
        JsonObject json = new JsonObject();
        json.addProperty("item", id.toString());
        return new WorkbenchIngredient(decodeIngredient(json), json, count);
    }

    public static WorkbenchIngredient of(TagKey<Item> tag, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", tag.location().toString());
        return new WorkbenchIngredient(decodeIngredient(json), json, count);
    }

    public static WorkbenchIngredient of(ResourceLocation id, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("item", id.toString());
        return new WorkbenchIngredient(decodeIngredient(json), json, count);
    }

    public int getCount() {
        return this.count;
    }

    public Ingredient ingredient() {
        return this.ingredient;
    }

    public boolean test(ItemStack stack) {
        return this.ingredient.test(stack);
    }

    public ItemStack[] getItems() {
        return this.ingredient.items()
                .map(Holder::value)
                .map(Item::getDefaultInstance)
                .map(ItemStack::copy)
                .toArray(ItemStack[]::new);
    }

    public JsonElement toJson() {
        JsonElement base = this.definition.deepCopy();
        if (base.isJsonObject()) {
            base.getAsJsonObject().addProperty("count", this.count);
            return base;
        }
        JsonObject wrapper = new JsonObject();
        wrapper.add("ingredients", base);
        wrapper.addProperty("count", this.count);
        return wrapper;
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        if (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
            throw new IllegalStateException("Expected RegistryFriendlyByteBuf for workbench ingredient serialization");
        }
        STREAM_CODEC.encode(registryBuffer, this);
    }

    public static WorkbenchIngredient fromNetwork(FriendlyByteBuf buffer) {
        if (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
            throw new IllegalStateException("Expected RegistryFriendlyByteBuf for workbench ingredient deserialization");
        }
        return STREAM_CODEC.decode(registryBuffer);
    }

    private static Ingredient decodeIngredient(JsonElement element) {
        DataResult<Ingredient> result = Ingredient.CODEC.parse(JsonOps.INSTANCE, element);
        return result.getOrThrow(false, msg -> { throw new IllegalStateException("Failed to parse ingredient: " + msg); });
    }

    private static DataResult<JsonElement> encodeIngredient(Ingredient ingredient) {
        return Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient);
    }
}
