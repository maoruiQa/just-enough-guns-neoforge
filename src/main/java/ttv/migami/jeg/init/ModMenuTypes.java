package ttv.migami.jeg.init;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.menu.MagazineLoaderMenu;
import ttv.migami.jeg.menu.AttachmentMenu;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;
import ttv.migami.jeg.vehicle.menu.VehicleChargingStationMenu;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;

public final class ModMenuTypes {
    private ModMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, Reference.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AttachmentMenu>> ATTACHMENT_MENU = REGISTER.register(
            "attachment_menu",
            () -> new MenuType<>(AttachmentMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleMenu>> VEHICLE_MENU = REGISTER.register(
            "vehicle_menu",
            () -> new ExtendedMenuType<>(
                    (windowId, inventory, slotCount) -> new VehicleMenu(windowId, inventory, slotCount),
                    ByteBufCodecs.VAR_INT
            )
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleAssemblingMenu>> VEHICLE_ASSEMBLING_MENU = REGISTER.register(
            "vehicle_assembling_menu",
            () -> new ExtendedMenuType<>((windowId, inventory, pos) -> new VehicleAssemblingMenu(
                    windowId,
                    inventory,
                    ContainerLevelAccess.create(inventory.player.level(), pos)
            ), BlockPos.STREAM_CODEC)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleChargingStationMenu>> VEHICLE_CHARGING_STATION_MENU = REGISTER.register(
            "vehicle_charging_station_menu",
            () -> new ExtendedMenuType<>((windowId, inventory, pos) -> new VehicleChargingStationMenu(
                    windowId,
                    inventory,
                    new net.minecraft.world.inventory.SimpleContainerData(VehicleChargingStationMenu.DATA_COUNT),
                    ContainerLevelAccess.create(inventory.player.level(), pos)
            ), BlockPos.STREAM_CODEC)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<MagazineLoaderMenu>> MAGAZINE_LOADER_MENU = REGISTER.register(
            "magazine_loader_menu",
            () -> new ExtendedMenuType<>((windowId, inventory, pos) -> new MagazineLoaderMenu(
                    windowId,
                    inventory,
                    new net.minecraft.world.SimpleContainer(MagazineLoaderMenu.SLOT_COUNT),
                    new net.minecraft.world.inventory.SimpleContainerData(MagazineLoaderMenu.DATA_COUNT),
                    ContainerLevelAccess.create(inventory.player.level(), pos)
            ), BlockPos.STREAM_CODEC)
    );
}
