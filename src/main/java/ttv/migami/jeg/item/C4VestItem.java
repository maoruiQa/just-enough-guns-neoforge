package ttv.migami.jeg.item;

import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.util.SpecialExplosion;

/**
 * C4 vest (leather chest armor). Only detonates while worn.
 * First wearer claims ownership if none is set. Power = two remote C4 blasts.
 */
public final class C4VestItem extends ArmorItem {
    private static final Holder<ArmorMaterial> MATERIAL = Holder.direct(new ArmorMaterial(
            new EnumMap<>(ArmorMaterials.LEATHER.value().defense()),
            ArmorMaterials.LEATHER.value().enchantmentValue(),
            SoundEvents.ARMOR_EQUIP_LEATHER,
            ArmorMaterials.LEATHER.value().repairIngredient(),
            List.of(new ArmorMaterial.Layer(Reference.id("c4_vest"))),
            ArmorMaterials.LEATHER.value().toughness(),
            ArmorMaterials.LEATHER.value().knockbackResistance()
    ));

    public C4VestItem(Properties properties) {
        super(MATERIAL, Type.CHESTPLATE, properties.stacksTo(1).durability(80));
    }

    public static void bindOwner(ItemStack stack, Player player) {
        stack.set(ModDataComponents.ITEM_OWNER.get(), player.getUUID().toString());
        stack.set(ModDataComponents.ITEM_OWNER_NAME.get(), player.getGameProfile().getName());
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
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player player)) {
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
            @NotNull TooltipContext context,
            @NotNull List<Component> tooltip,
            @NotNull TooltipFlag flag
    ) {
        String name = stack.get(ModDataComponents.ITEM_OWNER_NAME.get());
        if (name != null && !name.isBlank()) {
            tooltip.add(Component.translatable("des.jeg.c4_vest.owner", name).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("des.jeg.c4_vest.no_owner").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("des.jeg.c4_vest").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("des.jeg.c4_vest.power").withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("des.jeg.c4_vest.howto").withStyle(ChatFormatting.DARK_GRAY));
    }
}
