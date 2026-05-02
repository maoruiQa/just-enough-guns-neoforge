package ttv.migami.jeg.entity.monster.phantom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Terror Phantom / Bound Terror Phantom summoned Phantom Gunner variant.
 * Same behaviour as {@link PhantomGunner}, but with lower max health.
 */
public class PhantomGunnerMinion extends PhantomGunner {
    public static final double MAX_HEALTH = 40.0D;
    private static final int SUPPORT_TIMEOUT_TICKS = 20 * 120;
    private static final double SUPPORT_PATROL_RADIUS = 24.0D;
    private static final double SUPPORT_TARGET_SEARCH_RADIUS = 48.0D;
    private static final double SUPPORT_TARGET_SEARCH_HEIGHT = 40.0D;
    private static final double SUPPORT_SPEED = 0.55D;

    private TerrorPhantomGuardian supportGuardian;
    private Vec3 supportCenter;
    private Vec3 supportPatrolPoint;
    private long supportLastRefreshTick;
    private int supportPatrolCooldown;
    private int supportTargetScanCooldown;

    public PhantomGunnerMinion(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
    }

    public void assignGuardianSupport(TerrorPhantomGuardian guardian, Vec3 targetCenter) {
        this.supportGuardian = guardian;
        this.supportCenter = targetCenter;
        this.supportPatrolPoint = null;
        this.supportLastRefreshTick = this.level().getGameTime();
        this.supportPatrolCooldown = 0;
        this.supportTargetScanCooldown = 0;
    }

    public boolean isGuardianSupportFor(TerrorPhantomGuardian guardian) {
        return this.supportGuardian == guardian;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.level() instanceof ServerLevel serverLevel) {
            tickGuardianSupportMission(serverLevel);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Keep everything identical to PhantomGunner except MAX_HEALTH.
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    private void tickGuardianSupportMission(ServerLevel level) {
        if (this.supportCenter == null) {
            return;
        }

        if (this.supportGuardian == null || this.supportGuardian.isRemoved() || !this.supportGuardian.isAlive()) {
            this.discard();
            return;
        }

        if (level.getGameTime() - this.supportLastRefreshTick > SUPPORT_TIMEOUT_TICKS) {
            this.discard();
            return;
        }

        if (this.supportTargetScanCooldown-- <= 0) {
            this.supportTargetScanCooldown = 20;
            acquireSupportTarget(level);
        }

        if (this.supportPatrolPoint == null
                || this.position().distanceToSqr(this.supportPatrolPoint) < 9.0D
                || this.supportPatrolCooldown-- <= 0) {
            this.supportPatrolCooldown = 60 + this.random.nextInt(40);
            this.supportPatrolPoint = chooseSupportPatrolPoint();
        }

        steerTowardSupportPoint();
    }

    private void acquireSupportTarget(ServerLevel level) {
        AABB searchArea = new AABB(
                this.supportCenter.x - SUPPORT_TARGET_SEARCH_RADIUS,
                this.supportCenter.y - SUPPORT_TARGET_SEARCH_HEIGHT,
                this.supportCenter.z - SUPPORT_TARGET_SEARCH_RADIUS,
                this.supportCenter.x + SUPPORT_TARGET_SEARCH_RADIUS,
                this.supportCenter.y + SUPPORT_TARGET_SEARCH_HEIGHT,
                this.supportCenter.z + SUPPORT_TARGET_SEARCH_RADIUS
        );
        LivingEntity target = level.getNearestEntity(
                Player.class,
                TargetingConditions.DEFAULT,
                this,
                this.supportCenter.x,
                this.supportCenter.y,
                this.supportCenter.z,
                searchArea
        );
        if (target != null && this.canAttack(target)) {
            this.setTarget(target);
        }
    }

    private Vec3 chooseSupportPatrolPoint() {
        double angle = this.random.nextDouble() * Mth.TWO_PI;
        double radius = 8.0D + this.random.nextDouble() * SUPPORT_PATROL_RADIUS;
        double yOffset = 18.0D + this.random.nextDouble() * 16.0D;
        return new Vec3(
                this.supportCenter.x + Math.cos(angle) * radius,
                this.supportCenter.y + yOffset,
                this.supportCenter.z + Math.sin(angle) * radius
        );
    }

    private void steerTowardSupportPoint() {
        if (this.supportPatrolPoint == null) {
            return;
        }

        Vec3 toTarget = this.supportPatrolPoint.subtract(this.position());
        double distance = toTarget.length();
        if (distance < 0.001D) {
            return;
        }

        Vec3 desiredMotion = toTarget.scale(SUPPORT_SPEED / distance);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.75D).add(desiredMotion.scale(0.25D)));
        this.getLookControl().setLookAt(this.supportPatrolPoint.x, this.supportPatrolPoint.y, this.supportPatrolPoint.z, 30.0F, 30.0F);
    }
}
