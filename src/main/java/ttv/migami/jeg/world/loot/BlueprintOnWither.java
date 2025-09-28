package ttv.migami.jeg.world.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.Reference;

import java.util.function.Supplier;

public class BlueprintOnWither extends LootModifier {
    public static final MapCodec<BlueprintOnWither> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item").forGetter(m -> m.item)).apply(inst, BlueprintOnWither::new));
    private final Item item;

    protected BlueprintOnWither(LootItemCondition[] conditionsIn, Item item) {
        super(conditionsIn);
        this.item = item;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        if(context.getRandom().nextFloat() > 0.0F) { // 100% chance of the item spawning in.

            // Array of possible item paths
            String[] itemPaths = {"explosive_charge", "rocket_launcher", "explosive_muzzle"};

            // Randomly select one of the items
            String selectedItemPath = itemPaths[context.getRandom().nextInt(itemPaths.length)];

            ItemStack itemStack = item.getDefaultInstance();
            CompoundTag tag = new CompoundTag();
            tag.putString("Namespace", Reference.MOD_ID);
            tag.putString("Path", selectedItemPath);
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            generatedLoot.add(itemStack);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
