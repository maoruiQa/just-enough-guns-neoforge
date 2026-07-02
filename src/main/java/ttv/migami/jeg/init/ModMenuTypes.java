package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.menu.AttachmentMenu;
import ttv.migami.jeg.menu.MagazineLoaderMenu;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;
import ttv.migami.jeg.vehicle.menu.VehicleChargingStationMenu;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;

public final class ModMenuTypes {
    private ModMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, Reference.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AttachmentMenu>> ATTACHMENTS = REGISTER.register(
            "attachments",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new AttachmentMenu(windowId, inventory))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleMenu>> VEHICLE_MENU = REGISTER.register(
            "vehicle_menu",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new VehicleMenu(windowId, inventory, data.readVarInt()))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleAssemblingMenu>> VEHICLE_ASSEMBLING_MENU = REGISTER.register(
            "vehicle_assembling_menu",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new VehicleAssemblingMenu(
                    windowId,
                    inventory,
                    ContainerLevelAccess.create(inventory.player.level(), data.readBlockPos())
            ))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleChargingStationMenu>> VEHICLE_CHARGING_STATION_MENU = REGISTER.register(
            "vehicle_charging_station_menu",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new VehicleChargingStationMenu(
                    windowId,
                    inventory,
                    new net.minecraft.world.inventory.SimpleContainerData(3),
                    ContainerLevelAccess.create(inventory.player.level(), data.readBlockPos())
            ))
    );

    public static final DeferredHolder<MenuType<?>, MenuType<MagazineLoaderMenu>> MAGAZINE_LOADER_MENU = REGISTER.register(
            "magazine_loader_menu",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new MagazineLoaderMenu(
                    windowId,
                    inventory,
                    new net.minecraft.world.SimpleContainer(MagazineLoaderMenu.SLOT_COUNT),
                    new net.minecraft.world.inventory.SimpleContainerData(MagazineLoaderMenu.DATA_COUNT),
                    ContainerLevelAccess.create(inventory.player.level(), data.readBlockPos())
            ))
    );
}
