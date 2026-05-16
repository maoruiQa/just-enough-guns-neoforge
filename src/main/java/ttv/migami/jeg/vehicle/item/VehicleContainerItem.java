package ttv.migami.jeg.vehicle.item;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public final class VehicleContainerItem extends BlockItem {
    private static final String TAG_ENTITY_TYPE = "EntityType";
    private static final String TAG_ENTITY = "Entity";
    private static final String TAG_VEHICLE_ID = "VehicleDataId";
    private static final String TAG_HEALTH = "Health";

    public VehicleContainerItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag blockEntityTag = blockEntityTag(stack);
        if (blockEntityTag == null) {
            return super.getName(stack);
        }
        String vehicleId = containedVehicleId(blockEntityTag);
        if (vehicleId.isBlank()) {
            return super.getName(stack);
        }
        return Component.translatable(
                "item.jeg.vehicle_container.named",
                Component.translatable("entity." + vehicleId.replace(':', '.'))
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipAdder, flag);
        CompoundTag blockEntityTag = blockEntityTag(stack);
        if (blockEntityTag == null) {
            tooltipAdder.accept(Component.translatable("tooltip.jeg.vehicle_container.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        String vehicleId = containedVehicleId(blockEntityTag);
        if (!vehicleId.isBlank()) {
            tooltipAdder.accept(Component.translatable(
                    "tooltip.jeg.vehicle_container.vehicle",
                    Component.translatable("entity." + vehicleId.replace(':', '.'))
            ).withStyle(ChatFormatting.GRAY));
        }
        CompoundTag entityTag = blockEntityTag.getCompoundOrEmpty(TAG_ENTITY);
        if (entityTag.contains(TAG_HEALTH)) {
            tooltipAdder.accept(Component.translatable(
                    "tooltip.jeg.vehicle_container.health",
                    String.format(Locale.US, "%.0f", entityTag.getFloatOr(TAG_HEALTH, 0.0F))
            ).withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    private static String containedVehicleId(CompoundTag blockEntityTag) {
        CompoundTag entityTag = blockEntityTag.getCompoundOrEmpty(TAG_ENTITY);
        if (entityTag.contains(TAG_VEHICLE_ID)) {
            return entityTag.getStringOr(TAG_VEHICLE_ID, "");
        }
        if (blockEntityTag.contains(TAG_ENTITY_TYPE)) {
            return blockEntityTag.getStringOr(TAG_ENTITY_TYPE, "");
        }
        return "";
    }

    private static CompoundTag blockEntityTag(ItemStack stack) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTagWithoutId();
    }
}
