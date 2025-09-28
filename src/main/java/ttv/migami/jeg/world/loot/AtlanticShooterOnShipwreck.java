package ttv.migami.jeg.world.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.Reference;

import java.util.function.Supplier;

public class AtlanticShooterOnShipwreck extends LootModifier {
    public static final MapCodec<AtlanticShooterOnShipwreck> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item").forGetter(m -> m.item)).apply(inst, AtlanticShooterOnShipwreck::new));
    private final Item item;

    protected AtlanticShooterOnShipwreck(LootItemCondition[] conditionsIn, Item item) {
        super(conditionsIn);
        this.item = item;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        if (context.getRandom().nextFloat() > 0.0F) {
            ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            Enchantment atlanticShooter = BuiltInRegistries.ENCHANTMENT.get(new ResourceLocation(Reference.MOD_ID, "atlantic_shooter"));
            if (atlanticShooter != null) {
                EnchantedBookItem.addEnchantment(enchantedBook, new EnchantmentInstance(atlanticShooter, 1));
            }
            generatedLoot.add(enchantedBook);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}