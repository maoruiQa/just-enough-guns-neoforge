package ttv.migami.jeg.vehicle.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModMenuTypes;

public final class VehicleChargingStationMenu extends AbstractContainerMenu {
    public static final int DATA_ENERGY_LOW = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_MAX_ENERGY_LOW = 2;
    public static final int DATA_MAX_ENERGY_HIGH = 3;
    public static final int DATA_CHARGING = 4;
    public static final int DATA_COUNT = 5;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    public VehicleChargingStationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public VehicleChargingStationMenu(int containerId, Inventory playerInventory, ContainerData data) {
        this(containerId, playerInventory, data, ContainerLevelAccess.NULL);
    }

    public VehicleChargingStationMenu(int containerId, Inventory playerInventory, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.VEHICLE_CHARGING_STATION_MENU.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.access = access;
        this.addDataSlots(data);
    }

    public int vehicleEnergy() {
        return decodeInt(DATA_ENERGY_LOW, DATA_ENERGY_HIGH);
    }

    public int maxVehicleEnergy() {
        return decodeInt(DATA_MAX_ENERGY_LOW, DATA_MAX_ENERGY_HIGH);
    }

    public boolean isCharging() {
        return this.data.get(DATA_CHARGING) > 0;
    }

    private int decodeInt(int lowIndex, int highIndex) {
        return (this.data.get(lowIndex) & 0xFFFF) | ((this.data.get(highIndex) & 0xFFFF) << 16);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, ModBlocks.VEHICLE_CHARGING_STATION.get());
    }
}
