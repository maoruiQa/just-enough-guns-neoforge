package ttv.migami.jeg.vehicle.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.menu.VehicleChargingStationMenu;

public final class VehicleChargingStationBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {
    private static final int CHARGE_INTERVAL = 20;
    private static final int CHARGE_PER_INTERVAL = 960;
    private int chargeTick;
    private int lastVehicleEnergy;
    private int lastVehicleMaxEnergy;
    private boolean charging;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case VehicleChargingStationMenu.DATA_ENERGY_LOW -> lowBits(VehicleChargingStationBlockEntity.this.lastVehicleEnergy);
                case VehicleChargingStationMenu.DATA_ENERGY_HIGH -> highBits(VehicleChargingStationBlockEntity.this.lastVehicleEnergy);
                case VehicleChargingStationMenu.DATA_MAX_ENERGY_LOW -> lowBits(VehicleChargingStationBlockEntity.this.lastVehicleMaxEnergy);
                case VehicleChargingStationMenu.DATA_MAX_ENERGY_HIGH -> highBits(VehicleChargingStationBlockEntity.this.lastVehicleMaxEnergy);
                case VehicleChargingStationMenu.DATA_CHARGING -> VehicleChargingStationBlockEntity.this.charging ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == VehicleChargingStationMenu.DATA_ENERGY_LOW || index == VehicleChargingStationMenu.DATA_ENERGY_HIGH) {
                VehicleChargingStationBlockEntity.this.lastVehicleEnergy = setBits(
                        VehicleChargingStationBlockEntity.this.lastVehicleEnergy,
                        index == VehicleChargingStationMenu.DATA_ENERGY_LOW,
                        value
                );
            } else if (index == VehicleChargingStationMenu.DATA_MAX_ENERGY_LOW || index == VehicleChargingStationMenu.DATA_MAX_ENERGY_HIGH) {
                VehicleChargingStationBlockEntity.this.lastVehicleMaxEnergy = setBits(
                        VehicleChargingStationBlockEntity.this.lastVehicleMaxEnergy,
                        index == VehicleChargingStationMenu.DATA_MAX_ENERGY_LOW,
                        value
                );
            } else if (index == VehicleChargingStationMenu.DATA_CHARGING) {
                VehicleChargingStationBlockEntity.this.charging = value > 0;
            }
        }

        @Override
        public int getCount() {
            return VehicleChargingStationMenu.DATA_COUNT;
        }
    };

    public VehicleChargingStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VEHICLE_CHARGING_STATION.get(), pos, blockState);
    }

    private static int lowBits(int value) {
        return value & 0xFFFF;
    }

    private static int highBits(int value) {
        return (value >>> 16) & 0xFFFF;
    }

    private static int setBits(int current, boolean low, int value) {
        int bits = value & 0xFFFF;
        return low ? (current & 0xFFFF0000) | bits : (current & 0x0000FFFF) | (bits << 16);
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
            if (vehicle.maxVehicleEnergy() <= 0) {
                continue;
            }
            station.lastVehicleEnergy = vehicle.vehicleEnergy();
            station.lastVehicleMaxEnergy = vehicle.maxVehicleEnergy();
            if (shouldCharge && vehicle.addEnergy(CHARGE_PER_INTERVAL)) {
                station.lastVehicleEnergy = vehicle.vehicleEnergy();
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
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.getBlockPos();
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VehicleChargingStationMenu(containerId, playerInventory, this.data, ContainerLevelAccess.create(this.getLevel(), this.getBlockPos()));
    }
}
