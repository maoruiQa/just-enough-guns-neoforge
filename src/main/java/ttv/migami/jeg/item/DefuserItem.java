package ttv.migami.jeg.item;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;

/**
 * C4 defuser: hold right-click for ~5s (no arm bob). On complete, swing once then disarm.
 * Taking damage cancels the channel.
 */
public final class DefuserItem extends Item {
    public static final int DEFAULT_DEFUSE_PROGRESS = 80;
    private static final double REACH = 4.0D;

    public DefuserItem(Properties properties) {
        super(properties);
    }

    /** Cancel channel when the user takes damage. */
    public static void interruptIfDefusing(LivingEntity entity) {
        if (entity == null || !entity.isUsingItem()) {
            return;
        }
        if (entity.getUseItem().getItem() instanceof DefuserItem) {
            entity.stopUsingItem();
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (findC4InSight(player) != null) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // No continuous arm animation while channeling.
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        PlacedExplosiveEntity target = findC4InSight(player);
        if (target == null) {
            if (!level.isClientSide) {
                player.stopUsingItem();
            }
            return;
        }

        int useTick = stack.getUseDuration(player) - remainingUseDuration;
        if (!level.isClientSide) {
            double remainingSeconds = (DEFAULT_DEFUSE_PROGRESS - useTick) / 20.0D;
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "%.1fs", Math.max(0.0D, remainingSeconds)))
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }

        if (useTick >= DEFAULT_DEFUSE_PROGRESS && level instanceof ServerLevel) {
            InteractionHand hand = player.getUsedItemHand();
            player.stopUsingItem();
            player.swing(hand, true);
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, player, handToSlot(hand));
            }
            target.defuse();
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Nullable
    private static PlacedExplosiveEntity findC4InSight(Player player) {
        Entity target = findLookingEntity(player, REACH);
        if (target instanceof PlacedExplosiveEntity explosive
                && explosive.kind() == SpecialExplosiveItem.Kind.C4
                && !explosive.isDetonating()) {
            return explosive;
        }
        return null;
    }

    @Nullable
    private static Entity findLookingEntity(Player player, double reach) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));
        double maxDistanceSqr = reach * reach;
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                searchBox,
                entity -> entity.isAlive()
                        && !entity.isSpectator()
                        && entity != player
                        && entity.isPickable(),
                maxDistanceSqr
        );
        return hit != null ? hit.getEntity() : null;
    }

    private static EquipmentSlot handToSlot(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.jeg.defuser").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("des.jeg.defuser.howto").withStyle(ChatFormatting.DARK_GRAY));
    }
}
