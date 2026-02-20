package ttv.migami.jeg.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

/**
 * Ranged combat goal for piglins wielding gun items.
 */
public class PiglinGunAttackGoal extends Goal {
    private final Piglin piglin;
    private int fireCooldown;
    private int reloadTicks;
    private int magazine;
    private int aimTicks;

    public PiglinGunAttackGoal(Piglin piglin) {
        this.piglin = piglin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!hasGun()) {
            return false;
        }
        LivingEntity target = piglin.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (!hasGun()) {
            return false;
        }
        LivingEntity target = piglin.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.fireCooldown = 8;
        initialiseMagazine();
    }

    @Override
    public void stop() {
        this.fireCooldown = 0;
        this.reloadTicks = 0;
        piglin.setAggressive(false);
    }

    @Override
    public void tick() {
        ItemStack stack = piglin.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        LivingEntity target = piglin.getTarget();
        if (target == null) {
            piglin.setAggressive(false);
            return;
        }

        piglin.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = piglin.distanceToSqr(target);
        boolean canSee = ttv.migami.jeg.gun.BulletPenetrationHelper.hasLineOfSightThroughPenetrable(piglin, target);

        if (!canSee || distance > 900.0D) {
            piglin.getNavigation().moveTo(target, 1.1D);
            // Don't set aggressive to false during movement - this interferes with targeting
            if (aimTicks > 0) {
                piglin.setAggressive(false);
                aimTicks = 0;
            }
        }

        // Handle reloading
        if (reloadTicks > 0) {
            reloadTicks--;
            piglin.setAggressive(false);
            if (reloadTicks == 0) {
                refillMagazine(stack, gun.getStats());
            }
            return;
        }

        // Handle fire cooldown
        if (fireCooldown > 0) {
            fireCooldown--;
            if (aimTicks > 0) {
                piglin.setAggressive(true);
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
        piglin.setAggressive(true);
        if (aimTicks < 8) {
            aimTicks++;
            return;
        }

        // Fire the gun
        Level level = piglin.level();
        GunStats stats = gun.getStats();
        gun.fireAt(level, piglin, stack, target);
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> level.playSound(null, piglin, sound, SoundSource.HOSTILE, 1.0F, 0.95F + piglin.getRandom().nextFloat() * 0.1F),
                () -> level.playSound(null, piglin, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.95F + piglin.getRandom().nextFloat() * 0.1F)
        );
        stack.hurtAndBreak(1, piglin, EquipmentSlot.MAINHAND);

        if (stats.usesMagazine()) {
            magazine = Math.max(0, magazine - 1);
            stack.set(ModDataComponents.GUN_AMMO.get(), magazine);
            if (magazine <= 0) {
                beginReload(stats, stack);
                aimTicks = 0;
            } else {
                fireCooldown = Math.max(4, stats.fireDelay());
                aimTicks = 8;
            }
        } else {
            fireCooldown = Math.max(6, stats.fireDelay());
            aimTicks = 8;
        }
    }

    private boolean hasGun() {
        ItemStack stack = piglin.getMainHandItem();
        return stack.getItem() instanceof GunItem;
    }

    private void initialiseMagazine() {
        ItemStack stack = piglin.getMainHandItem();
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
        stats.reloadEndSoundEvent().ifPresent(sound -> piglin.level().playSound(null, piglin, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
        fireCooldown = Math.max(6, stats.fireDelay());
    }

    private void beginReload(GunStats stats, ItemStack stack) {
        reloadTicks = Math.max(22, stats.totalReloadTime());
        stack.set(ModDataComponents.GUN_AMMO.get(), 0);
        stats.reloadStartSoundEvent().ifPresent(sound -> piglin.level().playSound(null, piglin, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
    }
}
