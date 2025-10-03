package ttv.migami.jeg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class ManualItem extends Item {
    private final List<ResourceKey<Recipe<?>>> recipes;

    public ManualItem(Properties properties, List<ResourceKey<Recipe<?>>> recipes) {
        super(properties);
        this.recipes = List.copyOf(recipes);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardRecipesByKey(recipes);
            player.displayClientMessage(Component.translatable("item.jeg.gunsmith_manual.learned").withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("item.jeg.gunsmith_manual.info").withStyle(ChatFormatting.GRAY));
    }
}
