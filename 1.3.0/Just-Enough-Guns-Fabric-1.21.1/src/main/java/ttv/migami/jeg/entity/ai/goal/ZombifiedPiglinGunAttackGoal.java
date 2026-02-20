package ttv.migami.jeg.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

/**
 * Ranged combat goal for zombified piglins wielding gun items.
 */
public class ZombifiedPiglinGunAttackGoal extends Goal {
    private final ZombifiedPiglin zombifiedPiglin;
    private int fireCooldown;
    private int reloadTicks;
    private int magazine;
    private int aimTicks;

    public ZombifiedPiglinGunAttackGoal(ZombifiedPiglin zombifiedPiglin) {
        this.zombifiedPiglin = zombifiedPiglin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!hasGun()) {
            return false;
        }
        LivingEntity target = zombifiedPiglin.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (!hasGun()) {
            return false;
        }
        LivingEntity target = zombifiedPiglin.getTarget();
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
        zombifiedPiglin.setAggressive(false);
    }

    @Override
    public void tick() {
        ItemStack stack = zombifiedPiglin.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        LivingEntity target = zombifiedPiglin.getTarget();
        if (target == null) {
            zombifiedPiglin.setAggressive(false);
            return;
        }

        zombifiedPiglin.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = zombifiedPiglin.distanceToSqr(target);
        boolean canSee = ttv.migami.jeg.gun.BulletPenetrationHelper.hasLineOfSightThroughPenetrable(zombifiedPiglin, target);

        if (!canSee || distance > 900.0D) {
            zombifiedPiglin.getNavigation().moveTo(target, 1.1D);
            // Don't set aggressive to false during movement - this interferes with targeting
            if (aimTicks > 0) {
                zombifiedPiglin.setAggressive(false);
                aimTicks = 0;
            }
        }

        // Handle reloading
        if (reloadTicks > 0) {
            reloadTicks--;
            zombifiedPiglin.setAggressive(false);
            if (reloadTicks == 0) {
                refillMagazine(stack, gun.getStats());
            }
            return;
        }

        // Handle fire cooldown
        if (fireCooldown > 0) {
            fireCooldown--;
            if (aimTicks > 0) {
                zombifiedPiglin.setAggressive(true);
            }
            return;
        }

        // Ready to shoot
        if (!canSee || distance > 900.0D) {
            // Don't clear aggressive state during line of sight check
            // Just skip shooting and continue with movement
            return;
        }

        // Start aiming animation
        zombifiedPiglin.setAggressive(true);
        if (aimTicks < 10) {
            aimTicks++;
            return;
        }

        // Fire the gun
        Level level = zombifiedPiglin.level();
        GunStats stats = gun.getStats();
        gun.fireAt(level, zombifiedPiglin, stack, target);
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> level.playSound(null, zombifiedPiglin, sound, SoundSource.HOSTILE, 1.0F, 0.95F + zombifiedPiglin.getRandom().nextFloat() * 0.1F),
                () -> level.playSound(null, zombifiedPiglin, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.95F + zombifiedPiglin.getRandom().nextFloat() * 0.1F)
        );
        stack.hurtAndBreak(1, zombifiedPiglin, EquipmentSlot.MAINHAND);

        if (stats.usesMagazine()) {
            magazine = Math.max(0, magazine - 1);
            stack.set(ModDataComponents.GUN_AMMO.get(), magazine);
            if (magazine <= 0) {
                beginReload(stats, stack);
                aimTicks = 0;
            } else {
                fireCooldown = Math.max(5, stats.fireDelay());
                aimTicks = 10;
            }
        } else {
            fireCooldown = Math.max(7, stats.fireDelay());
            aimTicks = 10;
        }
    }

    private boolean hasGun() {
        ItemStack stack = zombifiedPiglin.getMainHandItem();
        return stack.getItem() instanceof GunItem;
    }

    private void initialiseMagazine() {
        ItemStack stack = zombifiedPiglin.getMainHandItem();
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
        stats.reloadEndSoundEvent().ifPresent(sound -> zombifiedPiglin.level().playSound(null, zombifiedPiglin, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
        fireCooldown = Math.max(7, stats.fireDelay());
    }

    private void beginReload(GunStats stats, ItemStack stack) {
        reloadTicks = Math.max(25, stats.totalReloadTime());
        stack.set(ModDataComponents.GUN_AMMO.get(), 0);
        stats.reloadStartSoundEvent().ifPresent(sound -> zombifiedPiglin.level().playSound(null, zombifiedPiglin, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
    }
}
