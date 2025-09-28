package ttv.migami.jeg.common;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEnchantments;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("unused")
// @Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ReloadTracker
{
    private static final Map<Player, ReloadTracker> RELOAD_TRACKER_MAP = new WeakHashMap<>();

    private final int startTick;
    private final int slot;
    private final ItemStack stack;
    private final Gun gun;
    private boolean firstReload;

    private ReloadTracker(Player player)
    {
        this.startTick = player.tickCount;
        this.slot = player.getInventory().selected;
        this.stack = player.getInventory().getSelected();
        this.gun = ((GunItem) stack.getItem()).getModifiedGun(stack);
        this.firstReload = true;
    }

    /**
     * Tests if the current item the player is holding is the same as the one being reloaded
     *
     * @param player the player to check
     * @return True if it's the same weapon and slot
     */
    private boolean isSameWeapon(Player player)
    {
        return !this.stack.isEmpty() && player.getInventory().selected == this.slot && player.getInventory().getSelected() == this.stack;
    }

    /**
     * @return
     */
    private boolean isWeaponFull()
    {
        CompoundTag tag = this.stack.getOrCreateTag();
        return tag.getInt("AmmoCount") >= GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun);
    }

    private boolean isWeaponEmpty()
    {
        CompoundTag tag = this.stack.getOrCreateTag();
        return tag.getInt("AmmoCount") == 0;
    }

    private boolean hasNoAmmo(Player player)
    {
        if (gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
            return Gun.findAmmo(player, this.gun.getReloads().getReloadItem()).stack().isEmpty();
        }
        return Gun.findAmmo(player, this.gun.getProjectile().getItem()).stack().isEmpty();
    }

    private boolean canReload(Player player)
    {
        GunItem gunItem = (GunItem) stack.getItem();

        {
            int deltaTicks = player.tickCount - this.startTick;
            if (deltaTicks == 4) {
                ResourceLocation reloadSound = this.gun.getSounds().getReloadStart();
                playReloadSound(reloadSound, player);
            }

            int interval;
            if(this.isWeaponEmpty())
            {
                interval = GunEnchantmentHelper.getReloadInterval(stack, gun.getReloads().getReloadTimer() + gun.getReloads().getAdditionalReloadTimer());
            }
            else
            {
                interval = GunEnchantmentHelper.getReloadInterval(stack, gun.getReloads().getReloadTimer());
            }

            if(gun.getReloads().getReloadType() == ReloadType.MAG_FED || gun.getReloads().getReloadType() == ReloadType.INVENTORY_FED ||
                    gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM)
            {
                int quickHandsLevel = player.getMainHandItem().getEnchantmentLevel(ModEnchantments.QUICK_HANDS.get());



                if (quickHandsLevel == 1) {
                    interval = Math.max(1, Math.round(interval * 0.75F));
                } else if (quickHandsLevel >= 2) {
                    interval = Math.max(1, Math.round(interval * 0.5F));
                }

                if (gun.getReloads().getReloadType() != ReloadType.INVENTORY_FED) {
                    if (deltaTicks == interval / 2) {
                        ResourceLocation reloadSound = this.gun.getSounds().getReloadLoad();
                        playReloadSound(reloadSound, player);
                    }

                    if (deltaTicks == interval - 5) {
                        ResourceLocation reloadSound = this.gun.getSounds().getEjectorPull();
                        playReloadSound(reloadSound, player);
                    }
                }

                return deltaTicks > interval;
            }
            else
            {
                if (this.firstReload) {
                    interval += gun.getReloads().getAdditionalReloadTimer();
                }
                return deltaTicks > 0 && deltaTicks % interval == 0;
            }
        }
    }

    public static int ammoInInventory(ItemStack[] ammoStack)
    {
        int result = 0;
        for (ItemStack x: ammoStack)
            result+=x.getCount();
        return result;
    }

    private void shrinkFromAmmoPool(ItemStack[] ammoStack, Player player, int shrinkAmount)
    {
        // Cancels the event if the gun is enchanted with Infinity
        if (player.getMainHandItem().getEnchantmentLevel(ModEnchantments.INFINITY.get()) != 0) {
            return;
        }

        int shrinkAmt = shrinkAmount;
        ArrayList<ItemStack> stacks = new ArrayList<>();

        for (ItemStack x: ammoStack)
        {
            if(!x.isEmpty())
            {
                int max = Math.min(shrinkAmt, x.getCount());
                x.shrink(max);
                shrinkAmt-=max;
            }
            if(shrinkAmt==0)
                return;
        }
    }

    private void increaseMagAmmo(Player player)
    {
        ItemStack[] ammoStack = Gun.findAmmoStack(player, this.gun.getProjectile().getItem());
        if(ammoStack.length > 0)
        {
            CompoundTag tag = this.stack.getTag();
            int ammoAmount = Math.min(ammoInInventory(ammoStack), GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun));
            int currentAmmo = tag.getInt("AmmoCount");
            int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun);
            int amount = maxAmmo - currentAmmo;
            if(tag != null)
            {
                if (ammoAmount < amount) {
                    tag.putInt("AmmoCount", currentAmmo + ammoAmount);
                    this.shrinkFromAmmoPool(ammoStack, player, ammoAmount);
                } else {
                    tag.putInt("AmmoCount", maxAmmo);
                    this.shrinkFromAmmoPool(ammoStack, player, amount);
                }
            }
        }

        ResourceLocation reloadSound = this.gun.getSounds().getEjectorRelease();
        playReloadSound(reloadSound, player);
    }

    private void reloadItem(Player player) {
        AmmoContext context = Gun.findAmmo(player, this.gun.getReloads().getReloadItem());
        ItemStack ammo = context.stack();
        if (!ammo.isEmpty()) {
            CompoundTag tag = this.stack.getTag();
            if (tag != null) {
                int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun);
                tag.putInt("AmmoCount", maxAmmo);
                ammo.shrink(1);
            }

            // Trigger that the container changed
            Container container = context.container();
            if (container != null) {
                container.setChanged();
            }
        }

        Item waterBucket = Items.WATER_BUCKET;
        Item lavaBucket = Items.LAVA_BUCKET;
        ResourceLocation waterBucketLocation = BuiltInRegistries.ITEM.getKey(waterBucket);
        ResourceLocation lavaBucketLocation = BuiltInRegistries.ITEM.getKey(lavaBucket);
        if (this.gun.getReloads().getReloadItem().equals(waterBucketLocation) ||
                this.gun.getReloads().getReloadItem().equals(lavaBucketLocation)) {
            Item bucket = Items.BUCKET;
            ResourceLocation bucketLocation = BuiltInRegistries.ITEM.getKey(bucket);
            Item item = BuiltInRegistries.ITEM.get(bucketLocation);
            if (item != null && this.stack.getEnchantmentLevel(ModEnchantments.INFINITY.get()) == 0) {
                ItemStack itemStack = new ItemStack(item);
                player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), itemStack.copy()));
            }
        }

        ResourceLocation reloadSound = this.gun.getSounds().getReloadLoad();
        playReloadSound(reloadSound, player);
    }

    public static void inventoryFeed(Player player, Gun gun) {
        AmmoContext context = Gun.findAmmo(player, gun.getProjectile().getItem());
        ItemStack ammo = context.stack();
        CompoundTag tag = player.getMainHandItem().getTag();
        if(tag != null)
        {
            if(!ammo.isEmpty())
            {
                tag.putInt("AmmoCount", tag.getInt("AmmoCount") + 1);
                ammo.shrink(1);
            }
        }
    }

    private void increaseAmmo(Player player)
    {
        AmmoContext context = Gun.findAmmo(player, this.gun.getProjectile().getItem());
        ItemStack ammo = context.stack();
        if(!ammo.isEmpty())
        {
            int amount = Math.min(ammo.getCount(), this.gun.getReloads().getReloadAmount());
            CompoundTag tag = this.stack.getTag();
            if(tag != null)
            {
                int maxAmmo = GunModifierHelper.getModifiedAmmoCapacity(this.stack, this.gun);
                amount = Math.min(amount, maxAmmo - tag.getInt("AmmoCount"));
                tag.putInt("AmmoCount", tag.getInt("AmmoCount") + amount);

                if (ammo.is(ModItems.TERROR_ARMADA_FLARE.get())) {
                    player.displayClientMessage(Component.translatable("chat.jeg.raid_flare_loaded").withStyle(ChatFormatting.RED), true);
                    tag.putBoolean("HasTerrorRaid", true);
                }
                if (ammo.getTag() != null) {
                    if (ammo.getTag().getBoolean("HasRaid")) {
                        if (ammo.getTag().contains("Raid")) {
                            tag.putString("Raid", ammo.getTag().getString("Raid"));
                        }

                        player.displayClientMessage(Component.translatable("chat.jeg.raid_flare_loaded").withStyle(ChatFormatting.RED), true);
                        tag.putBoolean("HasRaid", true);
                    }
                }
            }
            ammo.shrink(amount);

            ResourceLocation reloadSound = gun.getSounds().getReloadLoad();
            playReloadSound(reloadSound, player);

            // Trigger that the container changed
            Container container = context.container();
            if(container != null)
            {
                container.setChanged();
            }
        }

        ResourceLocation reloadSound = this.gun.getSounds().getReloadLoad();
        playReloadSound(reloadSound, player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if(event.phase == TickEvent.Phase.START && !event.player.level().isClientSide)
        {
            Player player = event.player;
            CompoundTag tag = player.getMainHandItem().getTag();

            if(ModSyncedDataKeys.RELOADING.getValue(player))
            {
                if(!RELOAD_TRACKER_MAP.containsKey(player))
                {
                    if(!(player.getInventory().getSelected().getItem() instanceof GunItem))
                    {
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                        return;
                    }
                    RELOAD_TRACKER_MAP.put(player, new ReloadTracker(player));
                }
                ReloadTracker tracker = RELOAD_TRACKER_MAP.get(player);
                if(!tracker.isSameWeapon(player) || tracker.isWeaponFull() || tracker.hasNoAmmo(player))
                {
                    RELOAD_TRACKER_MAP.remove(player);
                    ModSyncedDataKeys.RELOADING.setValue(player, false);
                    return;
                }
                if(tracker.canReload(player))
                {
                    final Player finalPlayer = player;
                    final Gun gun = tracker.gun;
                    {
                        if(gun.getReloads().getReloadType() == ReloadType.MAG_FED) {
                            tracker.increaseMagAmmo(player);
                        }
                        else if(gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
                            tracker.reloadItem(player);
                        }
                        else if(gun.getReloads().getReloadType() == ReloadType.MANUAL) {
                            tracker.increaseAmmo(player);
                            tracker.firstReload = false;
                        } else if(gun.getReloads().getReloadType() == ReloadType.INVENTORY_FED) {
                            tracker.increaseAmmo(player);
                        }
                    }

                    if(tracker.isWeaponFull() || tracker.hasNoAmmo(player))
                    {
                        RELOAD_TRACKER_MAP.remove(player);
                        ModSyncedDataKeys.RELOADING.setValue(player, false);
                    }
                }
            }
            else if(RELOAD_TRACKER_MAP.containsKey(player)) {
                RELOAD_TRACKER_MAP.remove(player);
            }
        }
    }

    private static void playReloadSound(ResourceLocation location, Player player) {
        if (location == null) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof GunItem gunItem)) {
            return;
        }

        /*double radius = Config.SERVER.reloadMaxDistance.get();
        double soundX = player.getX();
        double soundY = player.getY() + 1.0;
        double soundZ = player.getZ();

        S2CMessageGunSound message = new S2CMessageGunSound(location, SoundSource.PLAYERS, (float) soundX, (float) soundY, (float) soundZ, 1.0F, 1.0F, player.getId(), false, true);
        PacketHandler.getPlayChannel().sendToNearbyPlayers(() -> LevelLocation.create(player.level(), soundX, soundY, soundZ, radius), message);*/

        Gun gun = gunItem.getModifiedGun(player.getMainHandItem());
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(location);

        if (sound == null) {
            return;
        }

        CompoundTag nbt = heldItem.getTag();
        boolean isFirstPerson = nbt != null && nbt.getBoolean("IsFirstPersonReload");

        if (player.level() instanceof ServerLevel serverLevel) {
            if (isFirstPerson) {
                serverLevel.playSound(player, player.getOnPos(), sound, SoundSource.PLAYERS, 1, 1);
            } else {
                serverLevel.playSound(null, player.getOnPos(), sound, SoundSource.PLAYERS, 1, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerEvent.PlayerLoggedOutEvent event)
    {
        MinecraftServer server = event.getEntity().getServer();
        if(server != null)
        {
            server.execute(() -> RELOAD_TRACKER_MAP.remove(event.getEntity()));
        }
    }
}
