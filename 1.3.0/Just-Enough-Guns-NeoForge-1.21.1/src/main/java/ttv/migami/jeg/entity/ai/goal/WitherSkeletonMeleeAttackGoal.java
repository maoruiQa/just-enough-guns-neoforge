package ttv.migami.jeg.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.WitherSkeleton;
import ttv.migami.jeg.item.GunItem;

/**
 * Standard melee attack goal for regular Wither Skeletons (non-gunners).
 * Ensures they use natural melee behavior without interference from gunner AI.
 */
public class WitherSkeletonMeleeAttackGoal extends Goal {
    private final WitherSkeleton entity;
    private int raiseArmTicks;
    private int attackCooldown;

    public WitherSkeletonMeleeAttackGoal(WitherSkeleton entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (entity.getMainHandItem().getItem() instanceof GunItem) {
            return false;
        }
        LivingEntity target = entity.getTarget();
        return target != null && target.isAlive() && entity.distanceToSqr(target) <= 36.0D; // 6 blocks
    }

    @Override
    public boolean canContinueToUse() {
        if (entity.getMainHandItem().getItem() instanceof GunItem) {
            return false;
        }
        LivingEntity target = entity.getTarget();
        return target != null && target.isAlive() && entity.distanceToSqr(target) <= 36.0D;
    }

    @Override
    public void start() {
        this.raiseArmTicks = 0;
        this.attackCooldown = 0;
    }

    @Override
    public void stop() {
        this.raiseArmTicks = 0;
        entity.setAggressive(false);
    }

    @Override
    public void tick() {
        LivingEntity target = entity.getTarget();
        if (target == null) {
            return;
        }

        entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = entity.distanceToSqr(target);

        // Move towards target if too far
        if (distance > 9.0D) { // 3 blocks
            entity.getNavigation().moveTo(target, 1.0D);
        } else {
            entity.getNavigation().stop();
        }

        // Handle attack animation and cooldown
        this.raiseArmTicks++;
        if (this.raiseArmTicks >= 5 && attackCooldown <= 0) {
            if (distance <= 9.0D) {
                // Use hurt method instead of doHurtTarget to fix compilation issue
                float damage = (float) entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                target.hurt(entity.damageSources().mobAttack(entity), damage);
                attackCooldown = 20; // 1 second cooldown
            }
            this.raiseArmTicks = 0;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Set aggressive state for visual feedback
        entity.setAggressive(distance <= 36.0D && target.isAlive());
    }
}
