package ttv.migami.jeg.init;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.menu.AttachmentMenu;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;
import ttv.migami.jeg.vehicle.menu.VehicleChargingStationMenu;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;

public final class ModMenuTypes {
    private ModMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, Reference.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AttachmentMenu>> ATTACHMENT_MENU = REGISTER.register(
            "attachment_menu",
            () -> new MenuType<>(AttachmentMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleMenu>> VEHICLE_MENU = REGISTER.register(
            "vehicle_menu",
            () -> new ExtendedScreenHandlerType<>(
                    (containerId, inventory, vehicleSlotCount) -> new VehicleMenu(containerId, inventory, vehicleSlotCount),
                    ByteBufCodecs.VAR_INT
            )
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleAssemblingMenu>> VEHICLE_ASSEMBLING_MENU = REGISTER.register(
            "vehicle_assembling_menu",
            () -> new MenuType<>(VehicleAssemblingMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleChargingStationMenu>> VEHICLE_CHARGING_STATION_MENU = REGISTER.register(
            "vehicle_charging_station_menu",
            () -> new ExtendedScreenHandlerType<>(
                    (containerId, inventory, pos) -> new VehicleChargingStationMenu(
                            containerId,
                            inventory,
                            new SimpleContainerData(VehicleChargingStationMenu.DATA_COUNT),
                            ContainerLevelAccess.create(inventory.player.level(), pos)
                    ),
                    BlockPos.STREAM_CODEC
            )
    );
}
