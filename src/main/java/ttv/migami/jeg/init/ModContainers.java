package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.common.container.*;
import ttv.migami.jeg.common.container.recycler.RecyclerMenu;
import ttv.migami.jeg.blockentity.ScrapWorkbenchBlockEntity;
import ttv.migami.jeg.blockentity.GunmetalWorkbenchBlockEntity;
import ttv.migami.jeg.blockentity.GunniteWorkbenchBlockEntity;
import ttv.migami.jeg.blockentity.BlueprintWorkbenchBlockEntity;

/**
 * Author: MrCrayfish
 */
public class ModContainers {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, Reference.MOD_ID);

    // Workbench-style menus (will be removed later if migrating fully to vanilla crafting)
    public static final RegistryObject<MenuType<ScrapWorkbenchContainer>> SCRAP_WORKBENCH = REGISTER.register("scrap_workbench", () -> IMenuTypeExtension.create((int windowId, Inventory inv, FriendlyByteBuf buf) -> {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof ScrapWorkbenchBlockEntity workbench) {
            return new ScrapWorkbenchContainer(windowId, inv, workbench);
        }
        return null;
    }));
    public static final RegistryObject<MenuType<GunmetalWorkbenchContainer>> GUNMETAL_WORKBENCH = REGISTER.register("gunmetal_workbench", () -> IMenuTypeExtension.create((int windowId, Inventory inv, FriendlyByteBuf buf) -> {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof GunmetalWorkbenchBlockEntity workbench) {
            return new GunmetalWorkbenchContainer(windowId, inv, workbench);
        }
        return null;
    }));
    public static final RegistryObject<MenuType<GunniteWorkbenchContainer>> GUNNITE_WORKBENCH = REGISTER.register("gunnite_workbench", () -> IMenuTypeExtension.create((int windowId, Inventory inv, FriendlyByteBuf buf) -> {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof GunniteWorkbenchBlockEntity workbench) {
            return new GunniteWorkbenchContainer(windowId, inv, workbench);
        }
        return null;
    }));
    public static final RegistryObject<MenuType<BlueprintWorkbenchContainer>> BLUEPRINT_WORKBENCH = REGISTER.register("blueprint_workbench", () -> IMenuTypeExtension.create((int windowId, Inventory inv, FriendlyByteBuf buf) -> {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof BlueprintWorkbenchBlockEntity workbench) {
            return new BlueprintWorkbenchContainer(windowId, inv, workbench);
        }
        return null;
    }));

    public static final RegistryObject<MenuType<SchematicStationMenu>> SCHEMATIC_STATION = register("schematic_station", SchematicStationMenu::new);

    public static final RegistryObject<MenuType<AttachmentContainer>> ATTACHMENTS = register("attachments", AttachmentContainer::new);

    public static final RegistryObject<MenuType<RecyclerMenu>> RECYCLER = register("recycler", RecyclerMenu::new);

    public static final RegistryObject<MenuType<AmmoBoxMenu>> AMMO_BOX = register("ammo_box", AmmoBoxMenu::new);

    /*public static final RegistryObject<MenuType<BasicTurretContainer>> BASIC_TURRET_CONTAINER =
            REGISTER.register("basic_turret_container", () -> IForgeMenuType.create(BasicTurretContainer::new));*/

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> register(String id, MenuType.MenuSupplier<T> factory) {
        return REGISTER.register(id, () -> new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS));
    }
}
