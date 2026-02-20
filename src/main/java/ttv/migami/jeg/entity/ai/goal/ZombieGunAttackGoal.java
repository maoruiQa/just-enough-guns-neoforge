package ttv.migami.jeg.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.entity.ai.GunCombatHelper;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

/**
 * Ranged combat goal for zombies wielding gun items.
 */
public class ZombieGunAttackGoal extends Goal {
    private final Zombie zombie;
    private int fireCooldown;
    private int reloadTicks;
    private int magazine;
    private int aimTicks;
    private double attackRange;
    private double retreatRange;

    public ZombieGunAttackGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!hasGun()) {
            return false;
        }
        LivingEntity target = zombie.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (!hasGun()) {
            return false;
        }
        LivingEntity target = zombie.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.fireCooldown = 10;
        this.aimTicks = 0;
        updateRanges();
        initialiseMagazine();
    }

    @Override
    public void stop() {
        this.fireCooldown = 0;
        this.reloadTicks = 0;
        zombie.setAggressive(false);
    }

    @Override
    public void tick() {
        ItemStack stack = zombie.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        LivingEntity target = zombie.getTarget();
        if (target == null) {
            zombie.setAggressive(false);
            return;
        }

        zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = zombie.distanceToSqr(target);
        boolean canSee = ttv.migami.jeg.gun.BulletPenetrationHelper.hasLineOfSightThroughPenetrable(zombie, target);

        if (!canSee || distance > attackRange * attackRange) {
            zombie.getNavigation().moveTo(target, 1.0D);
            // Don't set aggressive to false during movement - this interferes with targeting
            if (aimTicks > 0) {
                zombie.setAggressive(false);
                aimTicks = 0;
            }
        } else if (zombie.getNavigation().isInProgress()) {
            zombie.getNavigation().stop();
        }

        // Handle reloading
        if (reloadTicks > 0) {
            reloadTicks--;
            zombie.setAggressive(false);
            if (reloadTicks == 0) {
                refillMagazine(stack, gun.getStats());
            }
            return;
        }

        // Handle fire cooldown
        if (fireCooldown > 0) {
            fireCooldown--;
            if (aimTicks > 0) {
                zombie.setAggressive(true);
            }
            return;
        }

        // Ready to shoot
        if (!canSee || distance > attackRange * attackRange) {
            // Don't clear aggressive state during line of sight check
            // Just skip shooting and continue with movement
            return;
        }

        // Start aiming animation
        zombie.setAggressive(true);
        if (aimTicks < 10) {
            aimTicks++;
            return;
        }

        // Fire the gun
        Level level = zombie.level();
        GunStats stats = gun.getStats();
        gun.fireAt(level, zombie, stack, target);
        stats.fireSoundEvent().or(stats::silencedFireSoundEvent).ifPresentOrElse(
                sound -> level.playSound(null, zombie, sound, SoundSource.HOSTILE, 1.0F, 0.95F + zombie.getRandom().nextFloat() * 0.1F),
                () -> level.playSound(null, zombie, SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.95F + zombie.getRandom().nextFloat() * 0.1F)
        );
        stack.hurtAndBreak(1, zombie, EquipmentSlot.MAINHAND);

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

        if (distance < retreatRange * retreatRange) {
            Vec3 fallback = GunCombatHelper.computeRetreatPosition(zombie, target, retreatRange);
            zombie.getNavigation().moveTo(fallback.x, fallback.y, fallback.z, 1.0D);
        }
    }

    private boolean hasGun() {
        ItemStack stack = zombie.getMainHandItem();
        return stack.getItem() instanceof GunItem;
    }

    private void initialiseMagazine() {
        ItemStack stack = zombie.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }
        GunStats stats = gun.getStats();
        updateRanges(stats);
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
        stats.reloadEndSoundEvent().ifPresent(sound -> zombie.level().playSound(null, zombie, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
        fireCooldown = Math.max(7, stats.fireDelay());
        updateRanges(stats);
    }

    private void beginReload(GunStats stats, ItemStack stack) {
        reloadTicks = Math.max(25, stats.totalReloadTime());
        stack.set(ModDataComponents.GUN_AMMO.get(), 0);
        stats.reloadStartSoundEvent().ifPresent(sound -> zombie.level().playSound(null, zombie, sound, SoundSource.HOSTILE, 1.0F, 1.0F));
    }

    private void updateRanges() {
        ItemStack stack = zombie.getMainHandItem();
        if (stack.getItem() instanceof GunItem gun) {
            updateRanges(gun.getStats());
        } else {
            this.attackRange = GunCombatHelper.resolveAttackRange(null);
            this.retreatRange = GunCombatHelper.resolveRetreatRange(null, attackRange);
        }
    }

    private void updateRanges(GunStats stats) {
        double resolved = GunCombatHelper.resolveAttackRange(stats);
        if (resolved <= 0.0D || Double.isNaN(resolved) || Double.isInfinite(resolved)) {
            resolved = GunCombatHelper.resolveAttackRange(null);
        }
        this.attackRange = resolved;
        this.retreatRange = GunCombatHelper.resolveRetreatRange(stats, resolved);
    }
}
