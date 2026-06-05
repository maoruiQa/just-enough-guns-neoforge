package ttv.migami.jeg.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import ttv.migami.jeg.init.ModBlockEntities;

public final class DynamicLightBlockEntity extends BlockEntity {
    private static final String TAG_DELAY = "Delay";
    private double delay = 5.0D;

    public DynamicLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DYNAMIC_LIGHT.get(), pos, state);
    }

    public double delay() {
        return this.delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
        this.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.delay = input.getDoubleOr(TAG_DELAY, this.delay);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble(TAG_DELAY, this.delay);
    }
}
