package ttv.migami.jeg.blockentity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.block.AbstractRecyclerBlock;
import ttv.migami.jeg.crafting.recycling.AbstractRecyclingRecipe;
import ttv.migami.jeg.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class AbstractRecyclerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int SLOT_RESULT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_RECYCLING_PROGRESS = 2;
    public static final int DATA_RECYCLING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    public NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    int litTime;
    int litDuration;
    int recyclingProgress;
    int recyclingTotalTime;
    protected final ContainerData dataAccess = new ContainerData() {
        public int get(int p_58431_) {
            switch (p_58431_) {
                case 0:
                    return AbstractRecyclerBlockEntity.this.litTime;
                case 1:
                    return AbstractRecyclerBlockEntity.this.litDuration;
                case 2:
                    return AbstractRecyclerBlockEntity.this.recyclingProgress;
                case 3:
                    return AbstractRecyclerBlockEntity.this.recyclingTotalTime;
                default:
                    return 0;
            }
        }

        public void set(int p_58433_, int p_58434_) {
            switch (p_58433_) {
                case 0:
                    AbstractRecyclerBlockEntity.this.litTime = p_58434_;
                    break;
                case 1:
                    AbstractRecyclerBlockEntity.this.litDuration = p_58434_;
                    break;
                case 2:
                    AbstractRecyclerBlockEntity.this.recyclingProgress = p_58434_;
                    break;
                case 3:
                    AbstractRecyclerBlockEntity.this.recyclingTotalTime = p_58434_;
            }

        }

        public int getCount() {
            return 4;
        }
    };
    private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();
    private final RecipeType<? extends AbstractRecyclingRecipe> recipeType;

    protected AbstractRecyclerBlockEntity(BlockEntityType<?> p_154991_, BlockPos p_154992_, BlockState p_154993_, RecipeType<? extends AbstractRecyclingRecipe> p_154994_) {
        super(p_154991_, p_154992_, p_154993_);
        this.recipeType = p_154994_;
    }

    /**
     * @deprecated Forge: get burn times by calling ForgeHooks#getBurnTime(ItemStack)
     */
    @Deprecated
    public static Map<Item, Integer> getFuel() {
        Map<Item, Integer> map = Maps.newLinkedHashMap();
        add(map, Items.LAVA_BUCKET, 20000);
        add(map, Blocks.COAL_BLOCK, 16000);
        add(map, Items.BLAZE_ROD, 2400);
        add(map, Items.COAL, 1600);
        add(map, Items.CHARCOAL, 1600);
        add(map, ItemTags.LOGS, 300);
        add(map, ItemTags.PLANKS, 300);
        add(map, ItemTags.WOODEN_STAIRS, 300);
        add(map, ItemTags.WOODEN_SLABS, 150);
        add(map, ItemTags.WOODEN_TRAPDOORS, 300);
        add(map, ItemTags.WOODEN_PRESSURE_PLATES, 300);
        add(map, Blocks.OAK_FENCE, 300);
        add(map, Blocks.BIRCH_FENCE, 300);
        add(map, Blocks.SPRUCE_FENCE, 300);
        add(map, Blocks.JUNGLE_FENCE, 300);
        add(map, Blocks.DARK_OAK_FENCE, 300);
        add(map, Blocks.ACACIA_FENCE, 300);
        add(map, Blocks.OAK_FENCE_GATE, 300);
        add(map, Blocks.BIRCH_FENCE_GATE, 300);
        add(map, Blocks.SPRUCE_FENCE_GATE, 300);
        add(map, Blocks.JUNGLE_FENCE_GATE, 300);
        add(map, Blocks.DARK_OAK_FENCE_GATE, 300);
        add(map, Blocks.ACACIA_FENCE_GATE, 300);
        add(map, Blocks.NOTE_BLOCK, 300);
        add(map, Blocks.BOOKSHELF, 300);
        add(map, Blocks.LECTERN, 300);
        add(map, Blocks.JUKEBOX, 300);
        add(map, Blocks.CHEST, 300);
        add(map, Blocks.TRAPPED_CHEST, 300);
        add(map, Blocks.CRAFTING_TABLE, 300);
        add(map, Blocks.DAYLIGHT_DETECTOR, 300);
        add(map, ItemTags.BANNERS, 300);
        add(map, Items.BOW, 300);
        add(map, Items.FISHING_ROD, 300);
        add(map, Blocks.LADDER, 300);
        add(map, ItemTags.SIGNS, 200);
        add(map, Items.WOODEN_SHOVEL, 200);
        add(map, Items.WOODEN_SWORD, 200);
        add(map, Items.WOODEN_HOE, 200);
        add(map, Items.WOODEN_AXE, 200);
        add(map, Items.WOODEN_PICKAXE, 200);
        add(map, ItemTags.WOODEN_DOORS, 200);
        add(map, ItemTags.BOATS, 1200);
        add(map, ItemTags.WOOL, 100);
        add(map, ItemTags.WOODEN_BUTTONS, 100);
        add(map, Items.STICK, 100);
        add(map, ItemTags.SAPLINGS, 100);
        add(map, Items.BOWL, 100);
        add(map, ItemTags.WOOL_CARPETS, 67);
        add(map, Blocks.DRIED_KELP_BLOCK, 4001);
        add(map, Items.CROSSBOW, 300);
        add(map, Blocks.BAMBOO, 50);
        add(map, Blocks.DEAD_BUSH, 100);
        add(map, Blocks.SCAFFOLDING, 400);
        add(map, Blocks.LOOM, 300);
        add(map, Blocks.BARREL, 300);
        add(map, Blocks.CARTOGRAPHY_TABLE, 300);
        add(map, Blocks.FLETCHING_TABLE, 300);
        add(map, Blocks.SMITHING_TABLE, 300);
        add(map, Blocks.COMPOSTER, 300);
        add(map, Blocks.AZALEA, 100);
        add(map, Blocks.FLOWERING_AZALEA, 100);
        return map;
    }

    private static boolean isNeverARecyclerFuel(Item p_58398_) {
        return p_58398_.builtInRegistryHolder().is(ItemTags.NON_FLAMMABLE_WOOD);
    }

    private static void add(Map<Item, Integer> p_204303_, TagKey<Item> p_204304_, int p_204305_) {
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(p_204304_)) {
            if (!isNeverARecyclerFuel(holder.value())) {
                p_204303_.put(holder.value(), p_204305_);
            }
        }

    }

    private static void add(Map<Item, Integer> p_58375_, ItemLike p_58376_, int p_58377_) {
        Item item = p_58376_.asItem();
        if (isNeverARecyclerFuel(item)) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw Util.pauseInIde(new IllegalStateException("A developer tried to explicitly make fire resistant item " + item.getName(null).getString() + " a recycler fuel. That will not work!"));
            }
        } else {
            p_58375_.put(item, p_58377_);
        }
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    public void load(CompoundTag p_155025_) {
        super.load(p_155025_);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(p_155025_, this.items);
        this.litTime = p_155025_.getInt("BurnTime");
        this.recyclingProgress = p_155025_.getInt("CookTime");
        this.recyclingTotalTime = p_155025_.getInt("CookTimeTotal");
        this.litDuration = this.getBurnDuration(this.items.get(1));
        CompoundTag compoundtag = p_155025_.getCompound("RecipesUsed");

        for (String s : compoundtag.getAllKeys()) {
            this.recipesUsed.put(new ResourceLocation(s), compoundtag.getInt(s));
        }

    }

    protected void saveAdditional(CompoundTag p_187452_) {
        super.saveAdditional(p_187452_);
        p_187452_.putInt("BurnTime", this.litTime);
        p_187452_.putInt("CookTime", this.recyclingProgress);
        p_187452_.putInt("CookTimeTotal", this.recyclingTotalTime);
        ContainerHelper.saveAllItems(p_187452_, this.items);
        CompoundTag compoundtag = new CompoundTag();
        this.recipesUsed.forEach((p_187449_, p_187450_) -> {
            compoundtag.putInt(p_187449_.toString(), p_187450_);
        });
        p_187452_.put("RecipesUsed", compoundtag);
    }

    public static void serverTick(Level p_155014_, BlockPos p_155015_, BlockState p_155016_, AbstractRecyclerBlockEntity blockEntity) {
        boolean flag = blockEntity.isLit();
        boolean flag1 = false;
        Recipe<?> recipe = p_155014_.getRecipeManager().getRecipeFor((RecipeType<AbstractRecyclingRecipe>) blockEntity.recipeType, blockEntity, p_155014_).orElse(null);
        int i = blockEntity.getMaxStackSize();
        ItemStack itemstackInput = blockEntity.items.get(0);
        //if (blockEntity.isLit() && !itemstackInput.isEmpty() && blockEntity.canBurn(recipe, blockEntity.items, i))
        if (blockEntity.isLit())
        {
            --blockEntity.litTime;
            if (blockEntity.items.get(0).is(ModItems.GRENADE.get()) ||
                    blockEntity.items.get(0).is(ModItems.MOLOTOV_COCKTAIL.get()) ||
                    blockEntity.items.get(0).is(ModItems.ROCKET.get()) ||
                    blockEntity.items.get(0).is(ModItems.EXPLOSIVE_CHARGE.get())) {
                blockEntity.items.set(0, ItemStack.EMPTY);

                p_155014_.explode(
                        null,
                        p_155015_.getX() + 0.5, p_155015_.getY() + 0.5, p_155015_.getZ() + 0.5, // Explosion position
                        4.0F,
                        true,
                        Level.ExplosionInteraction.NONE
                );
            }
        }

        ItemStack itemstack = blockEntity.items.get(1);
        if (blockEntity.isLit() || !itemstack.isEmpty() && !blockEntity.items.get(0).isEmpty()) {
            if (!blockEntity.isLit() && blockEntity.canBurn(p_155014_.registryAccess(), recipe, blockEntity.items, i)) {
                blockEntity.litTime = blockEntity.getBurnDuration(itemstack);
                blockEntity.litDuration = blockEntity.litTime;
                if (blockEntity.isLit()) {
                    flag1 = true;
                    if (itemstack.hasCraftingRemainingItem())
                        blockEntity.items.set(1, itemstack.getCraftingRemainingItem());
                    else if (!itemstack.isEmpty()) {
                        Item item = itemstack.getItem();
                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            blockEntity.items.set(1, itemstack.getCraftingRemainingItem());
                        }
                    }
                }
            }

            if (blockEntity.isLit() && blockEntity.canBurn(p_155014_.registryAccess(), recipe, blockEntity.items, i)) {
                ++blockEntity.recyclingProgress;
                if (blockEntity.recyclingProgress == blockEntity.recyclingTotalTime) {
                    blockEntity.recyclingProgress = 0;
                    blockEntity.recyclingTotalTime = getTotalRecyclingTime(p_155014_, blockEntity.recipeType, blockEntity);
                    if (blockEntity.burn(p_155014_.registryAccess(), recipe, blockEntity.items, i)) {
                        blockEntity.setRecipeUsed(recipe);
                    }

                    flag1 = true;
                }
            } else {
                blockEntity.recyclingProgress = 0;
            }
        } else if (!blockEntity.isLit() && blockEntity.recyclingProgress > 0) {
            blockEntity.recyclingProgress = Mth.clamp(blockEntity.recyclingProgress - 2, 0, blockEntity.recyclingTotalTime);
        }

        if (flag != blockEntity.isLit()) {
            flag1 = true;
            p_155016_ = p_155016_.setValue(AbstractRecyclerBlock.LIT, Boolean.valueOf(blockEntity.isLit()));
            p_155014_.setBlock(p_155015_, p_155016_, 3);
        }

        if (flag1) {
            setChanged(p_155014_, p_155015_, p_155016_);
        }

    }

    private boolean canBurn(RegistryAccess access, @Nullable Recipe<?> p_155006_, NonNullList<ItemStack> p_155007_, int p_155008_) {
        if (!p_155007_.get(0).isEmpty() && p_155006_ != null) {
            ItemStack itemstack = ((Recipe<WorldlyContainer>) p_155006_).assemble(this, access);
            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack itemstack1 = p_155007_.get(2);
                if (itemstack1.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItem(itemstack1, itemstack)) {
                    return false;
                } else if (itemstack1.getCount() + itemstack.getCount() <= p_155008_ && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()) { // Forge fix: make recycler respect stack sizes in recycler recipes
                    return true;
                } else {
                    return itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize(); // Forge fix: make recycler respect stack sizes in recycler recipes
                }
            }
        } else {
            return false;
        }
    }

    private boolean burn(RegistryAccess access, @Nullable Recipe<?> p_155027_, NonNullList<ItemStack> p_155028_, int p_155029_) {
        if (p_155027_ != null && this.canBurn(access, p_155027_, p_155028_, p_155029_)) {
            ItemStack itemstack = p_155028_.get(0);
            ItemStack itemstack1 = ((Recipe<WorldlyContainer>) p_155027_).assemble(this, access);
            ItemStack itemstack2 = p_155028_.get(2);
            if (itemstack2.isEmpty()) {
                p_155028_.set(2, itemstack1.copy());
            } else if (itemstack2.is(itemstack1.getItem())) {
                itemstack2.grow(itemstack1.getCount());
            }

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !p_155028_.get(1).isEmpty() && p_155028_.get(1).is(Items.BUCKET)) {
                p_155028_.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getBurnDuration(ItemStack p_58343_) {
        if (p_58343_.isEmpty()) {
            return 0;
        } else {
            Item item = p_58343_.getItem();
            return net.minecraftforge.common.ForgeHooks.getBurnTime(p_58343_, this.recipeType);
        }
    }

    private static int getTotalRecyclingTime(Level p_155010_, RecipeType<? extends AbstractRecyclingRecipe> p_155011_, Container p_155012_) {
        return p_155010_.getRecipeManager().getRecipeFor((RecipeType<AbstractRecyclingRecipe>) p_155011_, p_155012_, p_155010_).map(AbstractRecyclingRecipe::getRecyclingTime).orElse(200);
    }

    public static boolean isFuel(ItemStack p_58400_) {
        return net.minecraftforge.common.ForgeHooks.getBurnTime(p_58400_, null) > 0;
    }

    public int[] getSlotsForFace(Direction p_58363_) {
        if (p_58363_ == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return p_58363_ == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    public boolean canPlaceItemThroughFace(int p_58336_, ItemStack p_58337_, @Nullable Direction p_58338_) {
        return this.canPlaceItem(p_58336_, p_58337_);
    }

    public boolean canTakeItemThroughFace(int p_58392_, ItemStack p_58393_, Direction p_58394_) {
        if (p_58394_ == Direction.DOWN && p_58392_ == 1) {
            return p_58393_.is(Items.WATER_BUCKET) || p_58393_.is(Items.BUCKET);
        } else {
            return true;
        }
    }

    public int getContainerSize() {
        return this.items.size();
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public ItemStack getItem(int p_58328_) {
        return this.items.get(p_58328_);
    }

    public ItemStack removeItem(int p_58330_, int p_58331_) {
        return ContainerHelper.removeItem(this.items, p_58330_, p_58331_);
    }

    public ItemStack removeItemNoUpdate(int p_58387_) {
        return ContainerHelper.takeItem(this.items, p_58387_);
    }

    public void setItem(int p_58333_, ItemStack p_58334_) {
        ItemStack itemstack = this.items.get(p_58333_);
        boolean flag = !p_58334_.isEmpty() && ItemStack.isSameItemSameTags(itemstack, p_58334_);
        this.items.set(p_58333_, p_58334_);
        if (p_58334_.getCount() > this.getMaxStackSize()) {
            p_58334_.setCount(this.getMaxStackSize());
        }

        if (p_58333_ == 0 && !flag) {
            this.recyclingTotalTime = getTotalRecyclingTime(this.level, this.recipeType, this);
            this.recyclingProgress = 0;
            this.setChanged();
        }

    }

    public boolean stillValid(Player p_58340_) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return p_58340_.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    public boolean canPlaceItem(int p_58389_, ItemStack p_58390_) {
        if (p_58389_ == 2) {
            return false;
        } else if (p_58389_ != 1) {
            return true;
        } else {
            ItemStack itemstack = this.items.get(1);
            return net.minecraftforge.common.ForgeHooks.getBurnTime(p_58390_, this.recipeType) > 0 || p_58390_.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
        }
    }

    public void clearContent() {
        this.items.clear();
    }

    public void setRecipeUsed(@Nullable RecipeHolder<?> recipeHolder) {
        if (recipeHolder != null) {
            ResourceKey<Recipe<?>> recipeKey = (ResourceKey<Recipe<?>>) recipeHolder.id();
            this.recipesUsed.addTo(recipeKey, 1);
        }

    }

    @Nullable
    public RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    public void awardUsedRecipes(Player p_58396_) {
    }

    public void awardUsedRecipesAndPopExperience(ServerPlayer p_155004_) {
        List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(p_155004_.serverLevel(), p_155004_.position());
        p_155004_.awardRecipes(list);
        this.recipesUsed.clear();
    }

    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel serverLevel, Vec3 origin) {
        List<RecipeHolder<?>> list = Lists.newArrayList();

        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : this.recipesUsed.reference2IntEntrySet()) {
            serverLevel.recipeAccess().byKey(entry.getKey()).ifPresent(recipeHolder -> {
                list.add((RecipeHolder<?>) recipeHolder);
                createExperience(serverLevel, origin, entry.getIntValue(), ((AbstractRecyclingRecipe) recipeHolder.value()).getExperience());
            });
        }

        return list;
    }

    private static void createExperience(ServerLevel serverLevel, Vec3 origin, int recipeCount, float experience) {
        int i = Mth.floor(recipeCount * experience);
        float f = Mth.frac(recipeCount * experience);
        if (f != 0.0F && Math.random() < (double) f) {
            ++i;
        }

        ExperienceOrb.award(serverLevel, origin, i);
    }

    public void fillStackedContents(StackedItemContents stackedContents) {
        for (ItemStack itemstack : this.items) {
            stackedContents.accountStack(itemstack);
        }

    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
    }
}
