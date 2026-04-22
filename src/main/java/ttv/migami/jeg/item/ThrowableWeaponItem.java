package ttv.migami.jeg.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.TimedThrowableItemProjectile;
import ttv.migami.jeg.init.ModSounds;

public abstract class ThrowableWeaponItem extends Item {
    protected static final int MIN_THROW_TICKS = 10;
    private static final int MAX_HOLD_DURATION = 72000;

    private final int maxCookTime;

    protected ThrowableWeaponItem(Properties properties, int maxCookTime) {
        super(properties);
        this.maxCookTime = maxCookTime;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return this.canCook() ? this.maxCookTime : MAX_HOLD_DURATION;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (this.handleSpecialUse(level, player, hand)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseTicks) {
        if (!this.canCook()) {
            return;
        }

        int duration = this.getChargeDuration(stack, livingEntity, remainingUseTicks);
        if (duration == MIN_THROW_TICKS) {
            this.playPrimeSound(level, livingEntity);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (this.canCook() && !level.isClientSide() && !livingEntity.isUnderWater()) {
            TimedThrowableItemProjectile projectile = this.createProjectile(level, livingEntity, 0);
            projectile.initialisePosition(livingEntity.getEyePosition());
            level.addFreshEntity(projectile);
            projectile.explodeNow();
            this.consumeAndAward(stack, livingEntity);
        }
        return stack;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (level.isClientSide() || livingEntity.isUnderWater()) {
            return false;
        }

        int duration = this.getChargeDuration(stack, livingEntity, timeLeft);
        if (duration < MIN_THROW_TICKS) {
            return false;
        }

        TimedThrowableItemProjectile projectile = this.createProjectile(level, livingEntity, this.getRemainingFuseTicks(duration));
        Vec3 eyePos = livingEntity.getEyePosition();
        Vec3 look = livingEntity.getLookAngle();
        projectile.initialisePosition(eyePos.add(look.scale(0.35D)));
        projectile.shootFromRotation(livingEntity, livingEntity.getXRot(), livingEntity.getYRot(), 0.0F, Math.min(1.0F, duration / 10.0F), 1.0F);
        level.addFreshEntity(projectile);
        this.onThrown(level, livingEntity, projectile, stack);
        this.consumeAndAward(stack, livingEntity);
        return true;
    }

    protected boolean canCook() {
        return true;
    }

    protected int getRemainingFuseTicks(int useDuration) {
        return Math.max(5, this.maxCookTime - useDuration);
    }

    protected void onThrown(Level level, LivingEntity livingEntity, TimedThrowableItemProjectile projectile, ItemStack stack) {
    }

    protected boolean handleSpecialUse(Level level, Player player, InteractionHand hand) {
        return false;
    }

    protected void playPrimeSound(Level level, LivingEntity livingEntity) {
        SoundEvent sound = this.resolveSound("item.grenade.pin", net.minecraft.sounds.SoundEvents.SNOWBALL_THROW);
        level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    protected final int getChargeDuration(ItemStack stack, LivingEntity livingEntity, int remainingUseTicks) {
        int usedTicks = this.getUseDuration(stack, livingEntity) - remainingUseTicks;
        return this.canCook() ? usedTicks : Math.min(usedTicks, this.maxCookTime);
    }

    protected final SoundEvent resolveSound(String path, SoundEvent fallback) {
        var holder = ModSounds.ALL.get(Reference.id(path));
        return holder != null ? holder.get() : fallback;
    }

    private void consumeAndAward(ItemStack stack, LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return;
        }
        stack.shrink(1);
    }

    protected abstract TimedThrowableItemProjectile createProjectile(Level level, LivingEntity livingEntity, int fuseTicks);
}
