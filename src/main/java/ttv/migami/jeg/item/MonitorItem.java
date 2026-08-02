package ttv.migami.jeg.item;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.init.ModDataComponents;

public final class MonitorItem extends Item {
    public MonitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // SW only uses main hand monitor for control
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            String linked = stack.get(ModDataComponents.DRONE_LINK.get());
            if (linked == null || linked.isEmpty() || "none".equals(linked)) {
                player.displayClientMessage(Component.translatable("message.jeg.drone.not_linked"), true);
                return InteractionResultHolder.fail(stack);
            }
            Entity entity;
            try {
                entity = serverLevel.getEntity(UUID.fromString(linked));
            } catch (IllegalArgumentException ignored) {
                entity = null;
            }
            if (!(entity instanceof DroneEntity drone) || !drone.isAlive()) {
                stack.remove(ModDataComponents.DRONE_LINK.get());
                stack.remove(ModDataComponents.DRONE_CONTROLLING.get());
                player.displayClientMessage(Component.translatable("message.jeg.drone.lost"), true);
                return InteractionResultHolder.fail(stack);
            }
            if (stack.getOrDefault(ModDataComponents.DRONE_CONTROLLING.get(), false)) {
                drone.stopControl(serverPlayer);
            } else {
                drone.startControl(serverPlayer, stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!isSelected && stack.getOrDefault(ModDataComponents.DRONE_CONTROLLING.get(), false)) {
            stack.remove(ModDataComponents.DRONE_CONTROLLING.get());
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                String linked = stack.get(ModDataComponents.DRONE_LINK.get());
                if (linked != null && level instanceof ServerLevel serverLevel) {
                    try {
                        Entity drone = serverLevel.getEntity(UUID.fromString(linked));
                        if (drone instanceof DroneEntity droneEntity) {
                            droneEntity.stopControl(serverPlayer);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        // Update last known position for tooltip
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            String linked = stack.get(ModDataComponents.DRONE_LINK.get());
            if (linked != null) {
                try {
                    Entity drone = serverLevel.getEntity(UUID.fromString(linked));
                    if (drone instanceof DroneEntity) {
                        stack.set(ModDataComponents.DRONE_POS_X.get(), (float) drone.getX());
                        stack.set(ModDataComponents.DRONE_POS_Y.get(), (float) drone.getY());
                        stack.set(ModDataComponents.DRONE_POS_Z.get(), (float) drone.getZ());
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.jeg.monitor.howto").withStyle(ChatFormatting.GRAY));
        String linked = stack.get(ModDataComponents.DRONE_LINK.get());
        if (linked == null || linked.isEmpty()) {
            tooltipComponents.add(Component.translatable("des.jeg.monitor.unlinked").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltipComponents.add(Component.translatable("des.jeg.monitor.linked").withStyle(ChatFormatting.GREEN));
        Float x = stack.get(ModDataComponents.DRONE_POS_X.get());
        Float y = stack.get(ModDataComponents.DRONE_POS_Y.get());
        Float z = stack.get(ModDataComponents.DRONE_POS_Z.get());
        if (x == null || y == null || z == null) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            double dist = player.position().distanceTo(new Vec3(x, y, z));
            tooltipComponents.add(Component.translatable("des.jeg.monitor.distance", String.format("%.1fm", dist)).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.literal(String.format("X: %.1f Y: %.1f Z: %.1f", x, y, z)).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
