package ttv.migami.jeg.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.processing.AnimationController;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.common.*;
import ttv.migami.jeg.compat.PlayerReviveHelper;
import ttv.migami.jeg.event.GunFireEvent;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.*;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * Author: MrCrayfish
 */
public class ShootingHandler
{
    private static ShootingHandler instance;
    private int fireTimer;
    private int holdFire;
    private int overheatTimer;
    private boolean previouslyPressed = false;
    private boolean overheated = false;
    private boolean previouslyDecoupled = false;

    public static ShootingHandler get()
    {
        if(instance == null)
        {
            instance = new ShootingHandler();
        }
        return instance;
    }

    private boolean shooting;

    private ShootingHandler() {}

    public int getFireTimer() {
        return fireTimer;
    }

    public int getHoldFire() {
        return holdFire;
    }

    public int getOverheatTimer() {
        return overheatTimer;
    }

    public boolean isShooting() {
        return  shooting;
    }

    private boolean isInGame()
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.getOverlay() != null)
            return false;
        if(mc.screen != null)
            return false;
        if(!mc.mouseHandler.isMouseGrabbed())
            return false;
        return mc.isWindowActive();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMouseClick(InputEvent.InteractionKeyMappingTriggered event)
    {
        if(event.isCanceled())
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if(player == null)
            return;

        if(PlayerReviveHelper.isBleeding(player))
            return;

        if(event.isAttack())
        {
            ItemStack heldItem = player.getMainHandItem();
            if(heldItem.getItem() instanceof GunItem gunItem)
            {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        }
        else if(event.isUseItem())
        {
            ItemStack heldItem = player.getMainHandItem();
            if(heldItem.getItem() instanceof GunItem gunItem)
            {
                if(event.getHand() == InteractionHand.OFF_HAND)
                {
                    // Allow shields to be used if weapon is one-handed
                    if(player.getOffhandItem().getItem() instanceof ShieldItem)
                    {
                        Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                        if(modifiedGun.getGeneral().getGripType() == GripType.ONE_HANDED)
                        {
                            return;
                        }
                    }
                    event.setCanceled(true);
                    event.setSwingHand(false);
                    return;
                }
                if(AimingHandler.get().isZooming() && AimingHandler.get().isLookingAtInteractableBlock())
                {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                }
            }
        }
    }

    @SubscribeEvent
    public void onHandleShooting(ClientTickEvent.Pre event)
    {
        if(!this.isInGame())
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if(player != null)
        {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof GunItem gunItem && KeyBinds.getShootMapping().isDown() && !Gun.hasAmmo(heldItem) && !PlayerReviveHelper.isBleeding(player)) {
                Gun gun = gunItem.getModifiedGun(heldItem);

                if (heldItem.hasTag() && heldItem.getTag() != null && heldItem.getTag().getInt("AmmoCount") <= 0 && !player.isCreative()) {
                    if (!gun.getReloads().getReloadType().equals(ReloadType.INVENTORY_FED)) {
                        ReloadHandler.get().setReloading(true);
                    }
                }
            }
            if(heldItem.getItem() instanceof GunItem gunItem && (Gun.hasAmmo(heldItem) || player.isCreative()) && !PlayerReviveHelper.isBleeding(player))
            {
                Gun gun = gunItem.getModifiedGun(heldItem);

                boolean shooting = (KeyBinds.getShootMapping().isDown() || (ModSyncedDataKeys.BURST_COUNT.getValue(player) > 0 && gun.getGeneral().getFireMode() == FireMode.BURST));
                if(JustEnoughGuns.shoulderSurfingLoaded)
                {
                    // Shoulder Surfing is optional; without the mod present we do not adjust the custom camera.
                    if(!shooting && this.previouslyDecoupled)
                    {
                        this.previouslyDecoupled = false;
                    }
                }
                if(JustEnoughGuns.controllableLoaded)
                {
                }
                if(shooting)
                {
                    if(!this.shooting)
                    {
                        this.shooting = true;
                        if (gun.getGeneral().getFireMode() == FireMode.BURST) {
                            PacketHandler.getPlayChannel().sendToServer(new C2SMessageBurst());
                        }
                        PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(true));
                    }
                }
                else if(this.shooting)
                {
                    this.shooting = false;
                    PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(false));
                }
            }
            else if(this.shooting)
            {
                this.shooting = false;
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageShooting(false));
            }
        }
        else
        {
            this.shooting = false;
        }
    }

    // Props to Moon-404 for the double-tap fix!
    @SubscribeEvent
    public void onPostClientTick(ClientTickEvent.Post event)
    {
        if(!isInGame())
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if(player != null)
        {
            if(PlayerReviveHelper.isBleeding(player))
                return;

            if(!KeyBinds.getShootMapping().isDown())
            {
                fireTimer = 0;
                if (!previouslyPressed) {
                    holdFire = 0;
                } else if (holdFire > 0) {
                    holdFire--;
                }
                if (overheatTimer > 0) {
                    overheatTimer--;
                }
            }
            if (overheated && overheatTimer <= 0) {
                overheated = false;
            }
            if (player.isUnderWater()) {
                overheatTimer = 0;
                overheated = false;
            }

            if (overheated) {
                KeyBinds.getShootMapping().setDown(false);
            }

            ItemStack heldItem = player.getMainHandItem();
            if(heldItem.getItem() instanceof GunItem gunItem)
            {
                Gun gun = ((GunItem) heldItem.getItem()).getModifiedGun(heldItem);

                if (gun.getGeneral().getOverheatTimer() == 0) {
                    overheated = false;
                }

                if (!KeyBinds.getShootMapping().isDown() && gun.getGeneral().getFireMode() == FireMode.RELEASE_FIRE) {
                    if (holdFire > 5 && previouslyPressed) {
                        this.fire(player, heldItem);
                        ChargeTracker.updateChargeTime(player, heldItem, false);
                    }
                }

                if(KeyBinds.getShootMapping().isDown() || (ModSyncedDataKeys.BURST_COUNT.getValue(player) > 0 && gun.getGeneral().getFireMode() == FireMode.BURST))
                {
                    if (gun.getGeneral().getFireMode() != FireMode.RELEASE_FIRE && gun.getGeneral().getFireMode() != FireMode.SEMI_AUTO && gun.getGeneral().getFireMode() != FireMode.PULSE)
                    {
                        this.fire(player, heldItem);
                        boolean doAutoFire = gun.getGeneral().getFireMode() == FireMode.AUTOMATIC || gun.getGeneral().getFireMode() == FireMode.BURST;
                        if(!doAutoFire)
                        {
                            KeyBinds.getShootMapping().setDown(false);
                        }
                    }

                }

                if (!Gun.hasAmmo(heldItem) && !player.isCreative()) {
                    KeyBinds.getShootMapping().setDown(false);
                    ModSyncedDataKeys.BURST_COUNT.setValue(player, 0);
                }

                if(KeyBinds.getShootMapping().isDown())
                {
                    ItemCooldowns tracker = player.getCooldowns();
                    if(gun.getGeneral().getOverheatTimer() != 0 && overheatTimer < gun.getGeneral().getOverheatTimer()) {
                        if (heldItem.getItem() instanceof AnimatedGunItem animatedGunItem) {
                            final long id = GeoItem.getId(player.getMainHandItem());
                            AnimationController<AnimatedGunItem> animationController = animatedGunItem.getAnimatableInstanceCache().getManagerForId(id).getAnimationControllers().get("Controller");
                            if (animationController != null && animationController.getCurrentAnimation() != null && !animationController.getCurrentAnimation().animation().name().matches("draw")) {
                                overheatTimer++;
                            }
                        } else {
                            overheatTimer++;
                        }
                        if (overheatTimer >= gun.getGeneral().getOverheatTimer()) {
                            tracker.addCooldown(heldItem.getItem(), 80);
                            if (heldItem.is(ModItems.FLAMETHROWER.get())) {
                                PacketHandler.getPlayChannel().sendToServer(new C2SMessageBurnPlayer());
                            }
                            overheated = true;
                            KeyBinds.getShootMapping().setDown(false);
                            PacketHandler.getPlayChannel().sendToServer(new C2SMessageOverheat());
                        }
                    }
                    if(gun.getGeneral().getMaxHoldFire() != 0) {
                        if(holdFire < gun.getGeneral().getMaxHoldFire() && !tracker.isOnCooldown(heldItem.getItem())) {
                            ChargeTracker.updateChargeTime(player, heldItem, true);
                            previouslyPressed = true;
                            holdFire++;
                        }
                    }
                    if(gun.getGeneral().getFireTimer() != 0)
                    {
                        if(fireTimer < gun.getGeneral().getFireTimer() && !tracker.isOnCooldown(heldItem.getItem())) {
                            if (fireTimer == 2)
                            {
                                PacketHandler.getPlayChannel().sendToServer(new C2SMessagePreFireSound(player));
                            }
                            fireTimer++;
                            if (player.isUnderWater()) {
                                fireTimer++;
                            }
                        } else {
                            // Execute after preFire timer ends
                            this.fire(player, heldItem);
                            if (gun.getGeneral().getFireMode() == FireMode.SEMI_AUTO || gun.getGeneral().getFireMode() == FireMode.PULSE)
                            {
                                KeyBinds.getShootMapping().setDown(false);
                                fireTimer = 0;
                            }
                        }
                    }
                    else {
                        if (gun.getGeneral().getFireMode() != FireMode.RELEASE_FIRE) {
                            this.fire(player, heldItem);
                        }
                        if(gun.getGeneral().getFireMode() == FireMode.SEMI_AUTO && gun.getGeneral().getFireMode() != FireMode.RELEASE_FIRE)
                        {
                            KeyBinds.getShootMapping().setDown(false);
                        }
                    }
                }
            } else {
                previouslyPressed = false;
                ChargeTracker.updateChargeTime(player, heldItem, false);
            }
        }
    }

    private boolean isEmpty(Player player, ItemStack heldItem)
    {
        if(!(heldItem.getItem() instanceof GunItem))
            return false;

        return !Gun.hasAmmo(heldItem) && !player.isCreative();
    }

    public void fire(Player player, ItemStack heldItem)
    {
        if(!(heldItem.getItem() instanceof GunItem))
            return;

        if(!Gun.hasAmmo(heldItem) && !player.isCreative())
            return;
        
        if(player.isSpectator())
            return;

        if(player.getUseItem().getItem() instanceof ShieldItem)
            return;

        if(isEmpty(player, heldItem))
        {
            if (ModSyncedDataKeys.BURST_COUNT.getValue(player)>0)
                ModSyncedDataKeys.BURST_COUNT.setValue(player, 0);
            return;
        }

        holdFire = 0;
        previouslyPressed = false;

        ItemCooldowns tracker = player.getCooldowns();
        int maxDamage = heldItem.getMaxDamage();
        int currentDamage = heldItem.getDamageValue();
        if(!tracker.isOnCooldown(heldItem.getItem()))
        {
            GunItem gunItem = (GunItem) heldItem.getItem();
            Gun modifiedGun = gunItem.getModifiedGun(heldItem);
            ItemStack stack = player.getMainHandItem();

            {
                var pre = new GunFireEvent.Pre(player, heldItem);
                NeoForge.EVENT_BUS.post(pre);
                if (pre.isCanceled())
                    return;
            }

            int rate = GunEnchantmentHelper.getRate(heldItem, modifiedGun);
            rate = GunModifierHelper.getModifiedRate(heldItem, rate);

            if(!stack.isDamageableItem() || currentDamage < (maxDamage - 1))
            {
                tracker.addCooldown(heldItem.getItem(), rate);

                // Burst code by NineZero!
                int gunBurstCount = modifiedGun.getGeneral().getBurstAmount();
                if (modifiedGun.getGeneral().getFireMode() == FireMode.BURST)
                {
                    // No shots left
                    if (ModSyncedDataKeys.BURST_COUNT.getValue(player) == 1) {
                        tracker.addCooldown(heldItem.getItem(), modifiedGun.getGeneral().getBurstDelay());
                    }

                    // Burst has not begun yet:
                    if (ModSyncedDataKeys.BURST_COUNT.getValue(player) <= 0)
                        ModSyncedDataKeys.BURST_COUNT.setValue(player, gunBurstCount - 1);
                    else
                        // When there are shots remaining in burst:
                        if (ModSyncedDataKeys.BURST_COUNT.getValue(player) > 0)
                            ModSyncedDataKeys.BURST_COUNT.setValue(player, ModSyncedDataKeys.BURST_COUNT.getValue(player) - 1);
                }

                PacketHandler.getPlayChannel().sendToServer(new C2SMessageShoot(player));
                NeoForge.EVENT_BUS.post(new GunFireEvent.Post(player, heldItem));
                ModSyncedDataKeys.RELOADING.setValue(player, false);
            }
        }
    }
}
