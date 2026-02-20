package ttv.migami.jeg.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import ttv.migami.jeg.item.GunItem;

/**
 * Standard melee attack goal for regular zombies (non-gunners).
 * Ensures zombies use their natural melee behavior without interference from gunner AI.
 */
public class ZombieMeleeAttackGoal extends Goal {
    private final Zombie zombie;
    private int raiseArmTicks;
    private int attackCooldown;

    public ZombieMeleeAttackGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (zombie.getMainHandItem().getItem() instanceof GunItem) {
            return false;
        }
        LivingEntity target = zombie.getTarget();
        return target != null && target.isAlive() && zombie.distanceToSqr(target) <= 36.0D; // 6 blocks
    }

    @Override
    public boolean canContinueToUse() {
        if (zombie.getMainHandItem().getItem() instanceof GunItem) {
            return false;
        }
        LivingEntity target = zombie.getTarget();
        return target != null && target.isAlive() && zombie.distanceToSqr(target) <= 36.0D;
    }

    @Override
    public void start() {
        this.raiseArmTicks = 0;
        this.attackCooldown = 0;
    }

    @Override
    public void stop() {
        this.raiseArmTicks = 0;
        zombie.setAggressive(false);
    }

    @Override
    public void tick() {
        LivingEntity target = zombie.getTarget();
        if (target == null) {
            return;
        }

        zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = zombie.distanceToSqr(target);

        // Move towards target if too far
        if (distance > 9.0D) { // 3 blocks
            zombie.getNavigation().moveTo(target, 1.0D);
        } else {
            zombie.getNavigation().stop();
        }

        // Handle attack animation and cooldown
        this.raiseArmTicks++;
        if (this.raiseArmTicks >= 5 && attackCooldown <= 0) {
            if (distance <= 9.0D) {
                // Use hurt method instead of doHurtTarget to fix compilation issue
                float damage = (float) zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                target.hurt(zombie.damageSources().mobAttack(zombie), damage);
                attackCooldown = 20; // 1 second cooldown
            }
            this.raiseArmTicks = 0;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Set aggressive state for visual feedback
        zombie.setAggressive(distance <= 36.0D && target.isAlive());
    }
}
