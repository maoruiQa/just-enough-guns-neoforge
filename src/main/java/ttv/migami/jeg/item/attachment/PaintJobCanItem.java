package ttv.migami.jeg.item.attachment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.is(ModItems.TOY_SPRAY_CAN.get())) {
            tooltip.add(Component.translatable("info.jeg.tooltip_item.toy_paint_job_can").withStyle(ChatFormatting.GOLD));
        } else if (stack.is(ModItems.CLASSIC_SPRAY_CAN.get())) {
            tooltip.add(Component.translatable("info.jeg.tooltip_item.classic_paint_job_can").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("info.jeg.tooltip_item.paint_job_can").withStyle(ChatFormatting.GOLD));
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("info.jeg.tooltip_item.paint_job_can.disclaimer").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("info.jeg.tooltip_item.paint_job_can.disclaimer_2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getRarity().equals(Rarity.EPIC);
    }
}
