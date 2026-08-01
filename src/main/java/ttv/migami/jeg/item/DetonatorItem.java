package ttv.migami.jeg.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;

public final class DetonatorItem extends Item {
    public DetonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            int detonated = 0;
            for (PlacedExplosiveEntity explosive : serverLevel.getEntitiesOfClass(
                    PlacedExplosiveEntity.class, player.getBoundingBox().inflate(512.0D),
                    entity -> entity.isRemoteC4OwnedBy(player.getUUID()))) {
                explosive.detonate();
                detonated++;
            }
            if (detonated > 0) {
                player.getCooldowns().addCooldown(this, 10);
                player.awardStat(Stats.ITEM_USED.get(this));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
