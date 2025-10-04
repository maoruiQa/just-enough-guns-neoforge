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
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.entity.GrenadeEntity;

public class GrenadeItem extends Item {
    private static final int DEFAULT_FUSE = 60;
    private static final float DEFAULT_POWER = 2.4F;
    // Reduced velocity to ensure throw range is less than grenade launcher
    // Grenade launcher uses projectileSpeed * 0.8, so we use 1.0 for moderate throwing range
    private static final float THROW_VELOCITY = 1.0F;

    public GrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.9F + level.random.nextFloat() * 0.2F);

        if (!level.isClientSide) {
            // Use grenade launcher approach: calculate direction and set velocity directly
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getLookAngle();
            Vec3 throwPos = eyePos.add(lookVec.scale(0.35));

            GrenadeEntity grenade = new GrenadeEntity(level, player, DEFAULT_POWER, DEFAULT_FUSE, true);
            grenade.setItem(stack.copyWithCount(1));
            grenade.initialisePosition(throwPos);

            // Calculate throw velocity: direction + player motion
            Vec3 playerMotion = player.getDeltaMovement();
            Vec3 throwVelocity = lookVec.scale(THROW_VELOCITY).add(playerMotion);
            grenade.setDeltaMovement(throwVelocity);

            level.addFreshEntity(grenade);
            player.awardStat(Stats.ITEM_USED.get(this));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS_SERVER;
    }
}
