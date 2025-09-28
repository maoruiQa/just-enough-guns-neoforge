package ttv.migami.jeg.world.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Supplier;

public class ScrapOnSimpleDungeonModifier extends LootModifier {
    public static final MapCodec<ScrapOnSimpleDungeonModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item").forGetter(m -> m.item)).apply(inst, ScrapOnSimpleDungeonModifier::new));
    private final Item item;

    protected ScrapOnSimpleDungeonModifier(LootItemCondition[] conditionsIn, Item item) {
        super(conditionsIn);
        this.item = item;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        if(context.getRandom().nextFloat() > 0.25F) { //75% chance of the item spawning in.
            int random = new Random().nextInt(6) + 1; //Min 1, Max 6.
            generatedLoot.add(new ItemStack(item, random));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
