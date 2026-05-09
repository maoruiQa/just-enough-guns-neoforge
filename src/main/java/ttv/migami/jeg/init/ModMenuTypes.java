package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;

public final class ModMenuTypes {
    private ModMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, Reference.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<VehicleMenu>> VEHICLE_MENU = REGISTER.register(
            "vehicle_menu",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new VehicleMenu(windowId, inventory))
    );
}
