package ttv.migami.jeg.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin extends TargetGoal {
    protected NearestAttackableTargetGoalMixin(Mob mob, boolean mustSee) {
        super(mob, mustSee);
    }

    @Inject(method = "getTargetConditions", at = @At("RETURN"), cancellable = true)
    private void jeg$getTargetConditions(CallbackInfoReturnable<TargetingConditions> cir) {
        if (!this.mob.entityTags().contains("MobGunner")) {
            return;
        }

        TargetingConditions original = cir.getReturnValue();
        cir.setReturnValue(
            original.copy()
                .ignoreLineOfSight()
                .selector((candidate, level) -> !(candidate instanceof Player player && player.isInvisible())
                    && jeg$canNoticeTargetThroughSolidBlocks(this.mob, candidate, 3))
        );
    }

    @Unique
    private static boolean jeg$canNoticeTargetThroughSolidBlocks(Mob mob, LivingEntity target, int maxSolidBlocks) {
        if (maxSolidBlocks == 0) {
            return true;
        }

        Level level = mob.level();
        Vec3 from = mob.getEyePosition();
        Vec3 to = target.getEyePosition();
        double distance = from.distanceTo(to);
        if (distance <= 0.0001D) {
            return true;
        }

        int steps = Math.max(1, (int) Math.ceil(distance * 5.0D));
        int solidBlocks = 0;
        BlockPos startPos = BlockPos.containing(from);
        BlockPos endPos = BlockPos.containing(to);
        BlockPos lastPos = null;

        for (int i = 1; i < steps; i++) {
            double t = (double) i / (double) steps;
            Vec3 point = from.lerp(to, t);
            BlockPos pos = BlockPos.containing(point);
            if (pos.equals(lastPos)) {
                continue;
            }
            lastPos = pos;
            if (pos.equals(startPos) || pos.equals(endPos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isSolidRender()) {
                solidBlocks++;
                if (solidBlocks > maxSolidBlocks) {
                    return false;
                }
            }
        }

        return true;
    }
}
