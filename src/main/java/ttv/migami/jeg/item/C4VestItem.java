package ttv.migami.jeg.item;

import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.util.SpecialExplosion;

/**
 * C4 vest (chest equippable). Only detonates while worn.
 * First wearer claims ownership if none is set. Power = two remote C4 blasts.
 */
public final class C4VestItem extends Item {
    public C4VestItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .durability(80)
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(EquipmentSlot.CHEST)
                                .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                                .setDamageOnHurt(true)
                                .build()
                ));
    }

    public static void bindOwner(ItemStack stack, Player player) {
        stack.set(ModDataComponents.ITEM_OWNER.get(), player.getUUID().toString());
        stack.set(ModDataComponents.ITEM_OWNER_NAME.get(), player.getGameProfile().name());
    }

    @Nullable
    public static UUID ownerId(ItemStack stack) {
        String raw = stack.get(ModDataComponents.ITEM_OWNER.get());
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean isOwnedBy(ItemStack stack, UUID playerId) {
        UUID owner = ownerId(stack);
        return owner != null && owner.equals(playerId);
    }

    /** Claim ownership when worn with no owner. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) {
            return;
        }
        ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
        if (worn != stack) {
            return;
        }
        if (ownerId(stack) == null) {
            bindOwner(stack, player);
        }
    }

    /**
     * Detonate worn C4 vests owned by {@code owner} (chest slot only).
     *
     * @return number of vests detonated
     */
    public static int detonateOwnedWornVests(ServerLevel level, ServerPlayer owner) {
        int count = 0;
        for (ServerPlayer target : level.getServer().getPlayerList().getPlayers()) {
            if (target.level() != level) {
                continue;
            }
            ItemStack chest = target.getItemBySlot(EquipmentSlot.CHEST);
            if (!(chest.getItem() instanceof C4VestItem) || !isOwnedBy(chest, owner.getUUID())) {
                continue;
            }
            explodeTwice(level, owner, target);
            target.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            count++;
        }
        return count;
    }

    private static void explodeTwice(ServerLevel level, ServerPlayer owner, ServerPlayer at) {
        for (int i = 0; i < 2; i++) {
            SpecialExplosion.explodeAt(
                    level,
                    at.position(),
                    owner,
                    PlacedExplosiveEntity.C4_REMOTE_DAMAGE,
                    PlacedExplosiveEntity.C4_REMOTE_RADIUS,
                    SpecialExplosion.Tier.HUGE
            );
        }
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Item.@NotNull TooltipContext context,
            @NotNull TooltipDisplay display,
            @NotNull Consumer<Component> tooltipAdder,
            @NotNull TooltipFlag flag
    ) {
        String name = stack.get(ModDataComponents.ITEM_OWNER_NAME.get());
        if (name != null && !name.isBlank()) {
            tooltipAdder.accept(Component.translatable("des.jeg.c4_vest.owner", name).withStyle(ChatFormatting.GOLD));
        } else {
            tooltipAdder.accept(Component.translatable("des.jeg.c4_vest.no_owner").withStyle(ChatFormatting.YELLOW));
        }
        tooltipAdder.accept(Component.translatable("des.jeg.c4_vest").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("des.jeg.c4_vest.power").withStyle(ChatFormatting.DARK_GREEN));
        tooltipAdder.accept(Component.translatable("des.jeg.c4_vest.howto").withStyle(ChatFormatting.DARK_GRAY));
    }
}
