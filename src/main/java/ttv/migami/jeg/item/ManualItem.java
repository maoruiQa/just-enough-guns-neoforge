package ttv.migami.jeg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import java.util.List;

public class ManualItem extends Item {
    private final List<ResourceKey<Recipe<?>>> recipes;

    public ManualItem(Properties properties, List<ResourceKey<Recipe<?>>> recipes) {
        super(properties);
        this.recipes = List.copyOf(recipes);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Convert ResourceKey list to ResourceLocation list for awardRecipesByKey
            List<net.minecraft.resources.ResourceLocation> recipeIds = recipes.stream()
                    .map(ResourceKey::location)
                    .toList();
            serverPlayer.awardRecipesByKey(recipeIds);
            player.displayClientMessage(Component.translatable("item.jeg.gunsmith_manual.learned").withStyle(ChatFormatting.GREEN), true);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jeg.gunsmith_manual.info").withStyle(ChatFormatting.GRAY));
    }
}
