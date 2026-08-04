package ttv.migami.jeg.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.faction.BomberGunnerHelper;

/**
 * Combat / detonation AI for bomber gunners.
 * <ul>
 *   <li>Inactive without a target so normal wander / idle goals can run.</li>
 *   <li>C4 beeps only while the bomber has line of sight on its target.</li>
 *   <li>Within range: 1.5s accelerating beeps (movable), then the final C4 sound — still movable
 *       for the first 1.5s of that sound, frozen for the remainder, then detonation.</li>
 * </ul>
 */
public class BomberDetonateGoal extends Goal {
    private final PathfinderMob mob;
    /** -1 = not armed; 0+ = fuse progress after entering detonation range. */
    private int fuseTicks = -1;
    private int sightBeepCooldown;

    public BomberDetonateGoal(PathfinderMob mob) {
        this.mob = mob;
        // MOVE is claimed only while this goal is active (has target or mid-fuse).
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!BomberGunnerHelper.isBomber(this.mob)
                || BomberGunnerHelper.hasDetonated(this.mob)
                || !BomberGunnerHelper.wearingC4Vest(this.mob)) {
            return false;
        }
        // Stay active through an armed fuse even if the target briefly drops.
        if (this.fuseTicks >= 0 || BomberGunnerHelper.isArmed(this.mob)) {
            return true;
        }
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (BomberGunnerHelper.hasDetonated(this.mob) || !BomberGunnerHelper.wearingC4Vest(this.mob)) {
            return false;
        }
        if (this.fuseTicks >= 0 || BomberGunnerHelper.isArmed(this.mob)) {
            return true;
        }
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        // Do not reset an in-progress fuse if the goal restarts mid-sequence.
        if (this.fuseTicks < 0 && !BomberGunnerHelper.isArmed(this.mob)) {
            this.fuseTicks = -1;
        }
        this.sightBeepCooldown = 0;
    }

    @Override
    public void stop() {
        // Only stop pathing if we were the ones driving the chase; free wander resumes when inactive.
        if (this.fuseTicks < 0) {
            this.mob.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        if (BomberGunnerHelper.hasDetonated(this.mob)) {
            return;
        }

        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (this.fuseTicks < 0) {
            tickChase(target);
            return;
        }

        this.fuseTicks++;

        // 1.5s accelerating beep phase before the final sound — fully mobile.
        if (!BomberGunnerHelper.isInFinalPhase(this.fuseTicks)) {
            int interval = BomberGunnerHelper.accelBeepInterval(this.fuseTicks);
            if (this.fuseTicks % interval == 0) {
                BomberGunnerHelper.playBeep(this.mob);
            }
            moveToward(target);
            return;
        }

        // Final C4 sound: first 1.5s still mobile, then freeze until detonation.
        if (this.fuseTicks == BomberGunnerHelper.ACCEL_TICKS) {
            BomberGunnerHelper.playFinalBeep(this.mob);
        }

        if (BomberGunnerHelper.isFrozen(this.fuseTicks)) {
            this.mob.getNavigation().stop();
            Vec3 motion = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(0.0D, motion.y, 0.0D);
        } else {
            moveToward(target);
        }

        if (BomberGunnerHelper.shouldDetonate(this.fuseTicks)) {
            BomberGunnerHelper.detonateAndKill(this.mob);
        }
    }

    private void moveToward(LivingEntity target) {
        if (target != null && target.isAlive()) {
            this.mob.getNavigation().moveTo(target, BomberGunnerHelper.RUSH_SPEED);
        }
    }

    private void tickChase(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        boolean canSee = this.mob.hasLineOfSight(target);

        // Medium-rate beeps only while the bomber can actually see the player.
        if (canSee) {
            if (this.sightBeepCooldown-- <= 0) {
                BomberGunnerHelper.playBeep(this.mob);
                this.sightBeepCooldown = BomberGunnerHelper.IDLE_BEEP_INTERVAL;
            }
        } else {
            this.sightBeepCooldown = 0;
        }

        double distanceSq = this.mob.distanceToSqr(target);
        double range = BomberGunnerHelper.DETONATE_RANGE;
        // Arm only when close enough and still has eyes on the target.
        if (canSee && distanceSq <= range * range && BomberGunnerHelper.wearingC4Vest(this.mob)) {
            startFuse();
            return;
        }

        // Chase when we have a target; free wander is used only when this goal is inactive (no target).
        this.mob.getNavigation().moveTo(target, BomberGunnerHelper.RUSH_SPEED);
    }

    private void startFuse() {
        this.fuseTicks = 0;
        BomberGunnerHelper.markArmed(this.mob);
        BomberGunnerHelper.playBeep(this.mob);
    }

    public boolean isFusing() {
        return this.fuseTicks >= 0;
    }
}
