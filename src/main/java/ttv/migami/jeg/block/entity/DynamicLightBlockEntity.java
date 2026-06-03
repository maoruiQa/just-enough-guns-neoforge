package ttv.migami.jeg.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_DELAY)) {
            this.delay = tag.getDouble(TAG_DELAY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble(TAG_DELAY, this.delay);
    }
}
