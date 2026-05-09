package ttv.migami.jeg.vehicle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.menu.VehicleChargingStationMenu;

public final class VehicleChargingStationBlockEntity extends BlockEntity implements MenuProvider {
    private static final int CHARGE_INTERVAL = 20;
    private static final int CHARGE_PER_INTERVAL = 8;
    private int chargeTick;
    private int lastVehicleEnergy;
    private int lastVehicleMaxEnergy;
    private boolean charging;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case VehicleChargingStationMenu.DATA_ENERGY -> VehicleChargingStationBlockEntity.this.lastVehicleEnergy;
                case VehicleChargingStationMenu.DATA_MAX_ENERGY -> VehicleChargingStationBlockEntity.this.lastVehicleMaxEnergy;
                case VehicleChargingStationMenu.DATA_CHARGING -> VehicleChargingStationBlockEntity.this.charging ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == VehicleChargingStationMenu.DATA_ENERGY) {
                VehicleChargingStationBlockEntity.this.lastVehicleEnergy = value;
            } else if (index == VehicleChargingStationMenu.DATA_MAX_ENERGY) {
                VehicleChargingStationBlockEntity.this.lastVehicleMaxEnergy = value;
            } else if (index == VehicleChargingStationMenu.DATA_CHARGING) {
                VehicleChargingStationBlockEntity.this.charging = value > 0;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public VehicleChargingStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VEHICLE_CHARGING_STATION.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VehicleChargingStationBlockEntity station) {
        station.chargeTick++;
        boolean shouldCharge = station.chargeTick >= CHARGE_INTERVAL;
        if (shouldCharge) {
            station.chargeTick = 0;
        }
        station.charging = false;
        station.lastVehicleEnergy = 0;
        station.lastVehicleMaxEnergy = 0;
        AABB range = new AABB(pos).inflate(2.0D, 1.0D, 2.0D).expandTowards(0.0D, 2.0D, 0.0D);
        for (VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class, range)) {
            var energy = vehicle.getCapability(Capabilities.EnergyStorage.ENTITY, null);
            if (energy == null || energy.getMaxEnergyStored() <= 0) {
                continue;
            }
            station.lastVehicleEnergy = energy.getEnergyStored();
            station.lastVehicleMaxEnergy = energy.getMaxEnergyStored();
            if (shouldCharge && energy.receiveEnergy(CHARGE_PER_INTERVAL, false) > 0) {
                station.lastVehicleEnergy = energy.getEnergyStored();
                station.charging = true;
                station.setChanged();
            }
            return;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.jeg.vehicle_charging_station");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VehicleChargingStationMenu(containerId, playerInventory, this.data, ContainerLevelAccess.create(this.getLevel(), this.getBlockPos()));
    }
}
