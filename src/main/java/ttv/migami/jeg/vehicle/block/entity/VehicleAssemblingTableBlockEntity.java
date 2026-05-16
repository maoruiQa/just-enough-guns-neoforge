package ttv.migami.jeg.vehicle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;

public final class VehicleAssemblingTableBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public VehicleAssemblingTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.jeg.vehicle_assembling_table");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VehicleAssemblingMenu(containerId, playerInventory, ContainerLevelAccess.create(this.getLevel(), this.getBlockPos()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
