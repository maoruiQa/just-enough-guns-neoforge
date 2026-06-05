package ttv.migami.jeg.item.attachment;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import ttv.migami.jeg.init.ModItems;

public class PaintJobCanItem extends Item {
    private final String paintJob;

    public PaintJobCanItem(Properties properties, String paintJob) {
        super(properties);
        this.paintJob = paintJob;
    }

    public String paintJob() {
        return this.paintJob;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        if (stack.is(ModItems.TOY_SPRAY_CAN.get())) {
            tooltipAdder.accept(Component.translatable("info.jeg.tooltip_item.toy_paint_job_can").withStyle(ChatFormatting.GOLD));
        } else if (stack.is(ModItems.CLASSIC_SPRAY_CAN.get())) {
            tooltipAdder.accept(Component.translatable("info.jeg.tooltip_item.classic_paint_job_can").withStyle(ChatFormatting.GOLD));
        } else {
            tooltipAdder.accept(Component.translatable("info.jeg.tooltip_item.paint_job_can").withStyle(ChatFormatting.GOLD));
        }

        tooltipAdder.accept(Component.literal(""));
        tooltipAdder.accept(Component.translatable("info.jeg.tooltip_item.paint_job_can.disclaimer").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("info.jeg.tooltip_item.paint_job_can.disclaimer_2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getRarity().equals(Rarity.EPIC);
    }
}
