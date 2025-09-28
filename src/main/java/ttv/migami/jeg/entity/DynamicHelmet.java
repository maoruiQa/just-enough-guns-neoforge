package ttv.migami.jeg.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.ValueInput;
import net.minecraft.nbt.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.init.ModEntities;

public class DynamicHelmet extends Entity {
    private ItemStack storedStack = Items.TURTLE_HELMET.getDefaultInstance();

    public DynamicHelmet(EntityType<?> type, Level level) {
        super(type, level);
    }

    public DynamicHelmet(Level level, double x, double y, double z, ItemStack hatStack) {
        this(ModEntities.DYNAMIC_HELMET.get(), level);
        this.setPos(x, y, z);
        this.setStoredStackData(hatStack.copy());
        this.storedStack = hatStack.copy();
        this.setNoGravity(false);
    }

    private static final EntityDataAccessor<ItemStack> STORED_STACK = SynchedEntityData.defineId(DynamicHelmet.class, EntityDataSerializers.ITEM_STACK);

    public void setStoredStackData(ItemStack stack) {
        this.entityData.set(STORED_STACK, stack.copy());
    }

    public ItemStack getStoredStackData() {
        return this.entityData.get(STORED_STACK);
    }

    public ItemStack getStoredStack() {
        return this.storedStack;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STORED_STACK, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String snbt = input.getStringOr("HatStackNBT", "");
        if (!snbt.isEmpty()) {
            try {
                CompoundTag tag = TagParser.parseTag(snbt);
                ItemStack loadedStack = ItemStack.of(tag);
                this.setStoredStackData(loadedStack);
                this.storedStack = loadedStack.copy();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        CompoundTag tag = this.getStoredStack().save(new CompoundTag());
        output.putString("HatStackNBT", tag.toString());
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.onGround()) {
            if (!level().isClientSide) {
                ItemEntity drop = new ItemEntity(level(), getX(), getY(), getZ(), storedStack);
                level().addFreshEntity(drop);
                this.discard();
            }
        }
    }
}