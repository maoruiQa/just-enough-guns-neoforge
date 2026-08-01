package ttv.migami.jeg.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.init.ModDataComponents;

public final class SpecialExplosiveItem extends Item {
    public enum Kind { C4, CLAYMORE, TM_62 }

    private final Kind kind;

    public SpecialExplosiveItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Vec3 position = context.getClickLocation().add(Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(0.05D));
        if (!place(level, context.getPlayer(), context.getItemInHand(), position, null)) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.kind != Kind.C4) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            Entity target = level.getEntities(player, new AABB(eye, eye.add(look.scale(5.0D))).inflate(1.0D), Entity::isPickable)
                    .stream()
                    .filter(entity -> entity.distanceToSqr(player) <= 25.0D)
                    .min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                    .orElse(null);
            if (target != null) {
                place(serverLevel, player, stack, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), target);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private boolean place(ServerLevel level, Player player, ItemStack stack, Vec3 position, Entity attached) {
        PlacedExplosiveEntity explosive = new PlacedExplosiveEntity(level, this.kind, player, position, player.getYRot());
        if (this.kind == Kind.C4) {
            explosive.setRemote(stack.getOrDefault(ModDataComponents.C4_REMOTE.get(), false));
            explosive.attachTo(attached);
        } else if (this.kind == Kind.TM_62 && player.isShiftKeyDown()) {
            explosive.setTimed(true);
        }
        if (!level.addFreshEntity(explosive)) {
            return false;
        }
        stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }
}
