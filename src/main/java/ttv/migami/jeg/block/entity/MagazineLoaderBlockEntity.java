package ttv.migami.jeg.block.entity;

import javax.annotation.Nullable;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.menu.MagazineLoaderMenu;

public final class MagazineLoaderBlockEntity extends BlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_PROGRESS = "Progress";
    public static final int LOAD_PROGRESS_REQUIRED = 38;
    public static final int LOAD_PROGRESS_PER_TICK = 5;
    private static final int[] INPUT_SLOTS = {MagazineLoaderMenu.SLOT_AMMO, MagazineLoaderMenu.SLOT_MAGAZINE};
    private static final int[] OUTPUT_SLOTS = {MagazineLoaderMenu.SLOT_OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(MagazineLoaderMenu.SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == MagazineLoaderMenu.DATA_PROGRESS ? MagazineLoaderBlockEntity.this.progress : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == MagazineLoaderMenu.DATA_PROGRESS) {
                MagazineLoaderBlockEntity.this.progress = value;
            }
        }

        @Override
        public int getCount() {
            return MagazineLoaderMenu.DATA_COUNT;
        }
    };

    public MagazineLoaderBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MAGAZINE_LOADER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagazineLoaderBlockEntity loader) {
        if (loader.tryMoveOutputReady()) {
            loader.progress = 0;
            loader.setChanged();
            return;
        }

        if (!loader.canLoadCurrentInput()) {
            if (loader.progress != 0) {
                loader.progress = 0;
                loader.setChanged();
            }
            return;
        }

        loader.progress += LOAD_PROGRESS_PER_TICK;
        if (loader.progress >= LOAD_PROGRESS_REQUIRED) {
            loader.progress -= LOAD_PROGRESS_REQUIRED;
            loader.loadOneRound();
        }
        loader.setChanged();
    }

    private boolean tryMoveOutputReady() {
        ItemStack magazineStack = this.items.get(MagazineLoaderMenu.SLOT_MAGAZINE);
        if (!(magazineStack.getItem() instanceof MagazineItem magazine) || !this.items.get(MagazineLoaderMenu.SLOT_OUTPUT).isEmpty()) {
            return false;
        }

        int ammoCount = magazine.getAmmoCount(magazineStack);
        boolean full = ammoCount >= magazine.getCapacity();
        boolean conflictingAmmo = ammoCount > 0 && this.hasConflictingAmmoInput(magazine, magazineStack);
        if (!full && !conflictingAmmo) {
            return false;
        }

        this.items.set(MagazineLoaderMenu.SLOT_OUTPUT, magazineStack);
        this.items.set(MagazineLoaderMenu.SLOT_MAGAZINE, ItemStack.EMPTY);
        return true;
    }

    private boolean canLoadCurrentInput() {
        ItemStack ammoStack = this.items.get(MagazineLoaderMenu.SLOT_AMMO);
        ItemStack magazineStack = this.items.get(MagazineLoaderMenu.SLOT_MAGAZINE);
        if (!(magazineStack.getItem() instanceof MagazineItem magazine) || ammoStack.isEmpty()) {
            return false;
        }
        return magazine.getAmmoCount(magazineStack) < magazine.getCapacity() && magazine.getLoadPromptMessage(magazineStack, ammoStack) == null;
    }

    private void loadOneRound() {
        ItemStack ammoStack = this.items.get(MagazineLoaderMenu.SLOT_AMMO);
        ItemStack magazineStack = this.items.get(MagazineLoaderMenu.SLOT_MAGAZINE);
        if (!(magazineStack.getItem() instanceof MagazineItem magazine) || ammoStack.isEmpty()) {
            return;
        }

        Identifier ammoId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem());
        int ammoCount = magazine.getAmmoCount(magazineStack);
        magazineStack.set(ModDataComponents.MAGAZINE_AMMO_ITEM.get(), ammoId.toString());
        magazineStack.set(ModDataComponents.MAGAZINE_AMMO_COUNT.get(), ammoCount + 1);
        ammoStack.shrink(1);
        if (ammoStack.isEmpty()) {
            this.items.set(MagazineLoaderMenu.SLOT_AMMO, ItemStack.EMPTY);
        }
    }

    private boolean hasConflictingAmmoInput(MagazineItem magazine, ItemStack magazineStack) {
        ItemStack ammoStack = this.items.get(MagazineLoaderMenu.SLOT_AMMO);
        if (ammoStack.isEmpty()) {
            return false;
        }
        return magazine.getLoadPromptMessage(magazineStack, ammoStack) != null;
    }

    public static boolean isAmmo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier ammoId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (MagazineItem.MagazineType type : MagazineItem.MagazineType.values()) {
            if (type.supports(ammoId)) {
                return true;
            }
        }
        return false;
    }

    public ContainerData data() {
        return this.data;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return slot != MagazineLoaderMenu.SLOT_OUTPUT && this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && slot == MagazineLoaderMenu.SLOT_OUTPUT;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case MagazineLoaderMenu.SLOT_AMMO -> isAmmo(stack);
            case MagazineLoaderMenu.SLOT_MAGAZINE -> stack.getItem() instanceof MagazineItem;
            default -> false;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.clearItems();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.jeg.magazine_loader");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MagazineLoaderMenu(containerId, playerInventory, this, this.data, ContainerLevelAccess.create(this.getLevel(), this.getBlockPos()));
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.getBlockPos();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.clearItems();
        input.child(TAG_ITEMS).ifPresent(itemsInput -> ContainerHelper.loadAllItems(itemsInput, this.items));
        this.progress = input.getIntOr(TAG_PROGRESS, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output.child(TAG_ITEMS), this.items);
        output.putInt(TAG_PROGRESS, this.progress);
    }

    private void clearItems() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
    }
}
