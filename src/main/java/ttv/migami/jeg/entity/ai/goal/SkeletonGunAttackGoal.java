package ttv.migami.jeg.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

/**
 * Simplified ranged combat goal for skeletons wielding gun items.
 */
public class SkeletonGunAttackGoal extends Goal {
    private final Skeleton skeleton;
    private int fireCooldown;
    private int reloadTicks;
    private int magazine;

    public SkeletonGunAttackGoal(Skeleton skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!hasGun()) {
            return false;
        }
        LivingEntity target = skeleton.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (!hasGun()) {
            return false;
        }
        LivingEntity target = skeleton.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.fireCooldown = 10;
        initialiseMagazine();
    }

    @Override
    public void stop() {
        this.fireCooldown = 0;
        this.reloadTicks = 0;
    }

    @Override
    public void tick() {
        ItemStack stack = skeleton.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        LivingEntity target = skeleton.getTarget();
        if (target == null) {
            return;
        }

        skeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = skeleton.distanceToSqr(target);
        boolean canSee = skeleton.getSensing().hasLineOfSight(target);

        if (!canSee || distance > 900.0D) {
            skeleton.getNavigation().moveTo(target, 1.05D);
        }

        if (reloadTicks > 0) {
            reloadTicks--;
            if (reloadTicks == 0) {
                refillMagazine(stack, gun.getStats());
            }
            return;
        }

        if (fireCooldown > 0) {
            fireCooldown--;
            return;
        }

        if (!canSee || distance > 900.0D) {
            return;
        }

        Level level = skeleton.level();
        GunStats stats = gun.getStats();
        gun.fireAt(level, skeleton, stack, target);
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> level.playSound(null, skeleton, sound, SoundSource.HOSTILE, 1.0F, 0.95F + skeleton.getRandom().nextFloat() * 0.1F),
                () -> level.playSound(null, skeleton, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.95F + skeleton.getRandom().nextFloat() * 0.1F)
        );
        stack.hurtAndBreak(1, skeleton, EquipmentSlot.MAINHAND);

        if (stats.usesMagazine()) {
            magazine = Math.max(0, magazine - 1);
            stack.set(ModDataComponents.GUN_AMMO.get(), magazine);
            if (magazine <= 0) {
                beginReload(stats, stack);
            } else {
                fireCooldown = Math.max(4, stats.fireDelay());
            }
        } else {
            fireCooldown = Math.max(6, stats.fireDelay());
        }
    }

    private boolean hasGun() {
        ItemStack stack = skeleton.getMainHandItem();
        return stack.getItem() instanceof GunItem;
    }

    private void initialiseMagazine() {
        ItemStack stack = skeleton.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }
        GunStats stats = gun.getStats();
        if (stats.usesMagazine()) {
            magazine = stats.magazineSize();
            stack.set(ModDataComponents.GUN_AMMO.get(), magazine);
        } else {
            magazine = Math.max(1, stats.projectileAmount());
        }
    }

    private void refillMagazine(ItemStack stack, GunStats stats) {
        if (stats.usesMagazine()) {
            magazine = stats.magazineSize();
            stack.set(ModDataComponents.GUN_AMMO.get(), magazine);
        } else {
            magazine = Math.max(1, stats.projectileAmount());
        }
        stats.reloadEndSoundEvent().ifPresent(sound -> skeleton.level().playSound(null, skeleton, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
        fireCooldown = Math.max(6, stats.fireDelay());
    }

    private void beginReload(GunStats stats, ItemStack stack) {
        reloadTicks = Math.max(20, stats.totalReloadTime());
        stack.set(ModDataComponents.GUN_AMMO.get(), 0);
        stats.reloadStartSoundEvent().ifPresent(sound -> skeleton.level().playSound(null, skeleton, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
    }
}
