package ttv.migami.jeg.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.entity.GrenadeEntity;

public class GrenadeItem extends Item {
    private static final int DEFAULT_FUSE = 60;
    private static final float DEFAULT_POWER = 2.4F;
    // Reduced velocity to ensure throw range is less than grenade launcher
    // Grenade launcher uses projectileSpeed * 1.6 (3 * 1.6 = 4.8), so 0.8 is much less
    private static final float THROW_VELOCITY = 0.8F;
    private static final float THROW_INACCURACY = 0.08F;

    public GrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.9F + level.random.nextFloat() * 0.2F);

        if (!level.isClientSide) {
            GrenadeEntity grenade = new GrenadeEntity(level, player, DEFAULT_POWER, DEFAULT_FUSE, false);
            grenade.setItem(stack.copyWithCount(1));
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_VELOCITY, THROW_INACCURACY);
            level.addFreshEntity(grenade);
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
