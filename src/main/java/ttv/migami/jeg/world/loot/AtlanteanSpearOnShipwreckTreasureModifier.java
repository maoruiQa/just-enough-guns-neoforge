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

public class AtlanteanSpearOnShipwreckTreasureModifier extends LootModifier {
    public static final MapCodec<AtlanteanSpearOnShipwreckTreasureModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item").forGetter(m -> m.item)).apply(inst, AtlanteanSpearOnShipwreckTreasureModifier::new));
    private final Item item;

    protected AtlanteanSpearOnShipwreckTreasureModifier(LootItemCondition[] conditionsIn, Item item) {
        super(conditionsIn);
        this.item = item;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        if(context.getRandom().nextFloat() > 0.66F) { // 33% chance of the item spawning in.
            int random = new Random().nextInt(1) + 1; //Min 1, Max 1.
            generatedLoot.add(new ItemStack(item, random));
        }

        //Vec3 origin = context.getParam(LootContextParams.ORIGIN); // this is nullable!

        /*if (origin != null) {
            if (context.getLevel() != null) {
                BlockPos pos = BlockPos.containing(origin);
                Holder<Biome> biomeHolder = context.getLevel().getBiome(pos);

                // Get the biome's ResourceLocation (e.g., "minecraft:warm_ocean")
                Optional<ResourceKey<Biome>> biomeKey = context.getLevel().registryAccess()
                        .registryOrThrow(Registries.BIOME)
                        .getResourceKey(biomeHolder.value());

                if (biomeKey.isPresent() && biomeKey.get().location().toString().equals("minecraft:warm_ocean")) {
                    if (context.getRandom().nextFloat() > 0.66F) {
                        generatedLoot.add(new ItemStack(item));
                    }
                }
            }
        }*/

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
