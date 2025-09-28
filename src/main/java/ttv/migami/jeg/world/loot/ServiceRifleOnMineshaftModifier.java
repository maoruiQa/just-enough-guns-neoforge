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

import java.util.function.Supplier;

public class ServiceRifleOnMineshaftModifier extends LootModifier {
    public static final MapCodec<ServiceRifleOnMineshaftModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item").forGetter(m -> m.item)).apply(inst, ServiceRifleOnMineshaftModifier::new));
    private final Item item;

    protected ServiceRifleOnMineshaftModifier(LootItemCondition[] conditionsIn, Item item) {
        super(conditionsIn);
        this.item = item;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        if(context.getRandom().nextFloat() > 0.80F) { // 20% chance of the item spawning in.
            ItemStack itemStack = item.getDefaultInstance();
            itemStack.setDamageValue(itemStack.getMaxDamage() - 1);
            generatedLoot.add(itemStack);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
