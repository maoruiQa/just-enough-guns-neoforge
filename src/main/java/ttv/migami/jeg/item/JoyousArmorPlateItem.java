package ttv.migami.jeg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class JoyousArmorPlateItem extends Item {
    private static final float RESTORE_AMOUNT = 20.0F;

    public JoyousArmorPlateItem(Properties properties) {
        super(properties
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
                .component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull net.minecraft.world.entity.LivingEntity target, @NotNull InteractionHand hand) {
        if (!(target instanceof HappyGhast ghast) || ghast.isBaby()) {
            return InteractionResult.PASS;
        }

        ItemStack harness = ghast.getItemBySlot(EquipmentSlot.BODY);
        if (!HappyGhastArmorHelper.isArmoredHarness(harness)) {
            return InteractionResult.PASS;
        }

        float plating = HappyGhastArmorHelper.getPlating(harness);
        float max = HappyGhastArmorHelper.getMaxPlating(harness);
        if (max <= 0.0F || plating >= max) {
            return InteractionResult.PASS;
        }

        float restored = Math.min(max - plating, RESTORE_AMOUNT);
        if (restored <= 0.0F) {
            return InteractionResult.PASS;
        }

        Level level = player.level();
        if (!level.isClientSide()) {
            HappyGhastArmorHelper.setPlating(harness, plating + restored);
            HappyGhastArmorHelper.syncAbsorption(ghast);
            level.playSound(null, ghast.getX(), ghast.getY(), ghast.getZ(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.8F, 1.4F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            HappyGhastArmorEvents.notifyPassengers(ghast);
            Component statusMessage = Component.translatable(
                    "tooltip.jeg.harness_status",
                    Math.round(HappyGhastArmorHelper.getPlating(harness)),
                    Math.round(max)
            ).withStyle(ChatFormatting.AQUA);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(statusMessage, true);
            } else {
                player.sendSystemMessage(statusMessage);
            }
            player.swing(hand, true);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }
}
