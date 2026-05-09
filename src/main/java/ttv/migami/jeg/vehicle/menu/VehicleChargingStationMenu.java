package ttv.migami.jeg.vehicle.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.init.ModMenuTypes;

public final class VehicleChargingStationMenu extends AbstractContainerMenu {
    public static final int DATA_ENERGY = 0;
    public static final int DATA_MAX_ENERGY = 1;
    public static final int DATA_CHARGING = 2;
    private static final int DATA_COUNT = 3;

    private final ContainerData data;

    public VehicleChargingStationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(DATA_COUNT));
    }

    public VehicleChargingStationMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.VEHICLE_CHARGING_STATION_MENU.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.addDataSlots(data);
    }

    public int vehicleEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    public int maxVehicleEnergy() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public boolean isCharging() {
        return this.data.get(DATA_CHARGING) > 0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }
}
