package ttv.migami.jeg.init;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.item.SpecialExplosiveItem;

/**
 * Superb Warfare style dispenser throws for C4 / Claymore / TM-62.
 */
public final class SpecialEquipmentDispenseBehaviors {
    private SpecialEquipmentDispenseBehaviors() {}

    public static void register() {
        DispenserBlock.registerBehavior(ModItems.C4_BOMB.get(), new ExplosiveDispense(SpecialExplosiveItem.Kind.C4));
        DispenserBlock.registerBehavior(ModItems.CLAYMORE_MINE.get(), new ExplosiveDispense(SpecialExplosiveItem.Kind.CLAYMORE));
        DispenserBlock.registerBehavior(ModItems.TM_62.get(), new ExplosiveDispense(SpecialExplosiveItem.Kind.TM_62));
    }

    private static final class ExplosiveDispense extends DefaultDispenseItemBehavior {
        private final SpecialExplosiveItem.Kind kind;

        private ExplosiveDispense(SpecialExplosiveItem.Kind kind) {
            this.kind = kind;
        }

        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            if (!(source.level() instanceof ServerLevel level)) {
                return stack;
            }
            Position position = DispenserBlock.getDispensePosition(source);
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            boolean remote = this.kind == SpecialExplosiveItem.Kind.C4
                    && stack.getOrDefault(ModDataComponents.C4_REMOTE.get(), false);

            PlacedExplosiveEntity entity = new PlacedExplosiveEntity(
                    level, this.kind, null, new Vec3(position.x(), position.y(), position.z()), 0.0F
            );
            if (this.kind == SpecialExplosiveItem.Kind.C4) {
                entity.setRemote(remote);
            }

            double pX = direction.getStepX();
            double pY = direction.getStepY() + (this.kind == SpecialExplosiveItem.Kind.TM_62 ? 0.0D : 0.1D);
            double pZ = direction.getStepZ();
            double speed = this.kind == SpecialExplosiveItem.Kind.TM_62 ? 0.2D : 0.05D;
            if (this.kind == SpecialExplosiveItem.Kind.CLAYMORE) {
                speed = 0.05D;
            }
            Vec3 motion = new Vec3(pX, pY, pZ).normalize().scale(speed);
            entity.setDeltaMovement(motion);
            double horizontal = motion.horizontalDistance();
            entity.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI)));
            entity.setXRot((float) (Mth.atan2(motion.y, horizontal) * (180.0D / Math.PI)));
            entity.yRotO = entity.getYRot();
            entity.xRotO = entity.getXRot();
            level.addFreshEntity(entity);
            stack.shrink(1);
            return stack;
        }
    }
}
