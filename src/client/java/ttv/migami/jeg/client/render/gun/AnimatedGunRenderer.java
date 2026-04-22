package ttv.migami.jeg.client.render.gun;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import ttv.migami.jeg.client.render.gun.layer.GunFirstPersonArmsLayer;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class AnimatedGunRenderer extends GeoItemRenderer<AnimatedGunItem> {
    public AnimatedGunRenderer() {
        super(new AnimatedGunGeoModel());
        this.addRenderLayer(new GunFirstPersonArmsLayer(this));
    }

    public ItemDisplayContext currentPerspective() {
        return this.renderPerspective;
    }

    public boolean isFirstPersonContext() {
        ItemDisplayContext ctx = this.renderPerspective;
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return true;
        }

        ItemStack stack = this.getCurrentItemStack();
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return false;
        }

        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        return matchesHeldStack(stack, main) || matchesHeldStack(stack, off);
    }

    public HumanoidArm resolveRenderedHand() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return HumanoidArm.RIGHT;
        }

        ItemDisplayContext ctx = this.renderPerspective;
        HumanoidArm mainArm = mc.player.getMainArm();
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return mainArm == HumanoidArm.LEFT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        }
        if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return mainArm == HumanoidArm.RIGHT ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        }

        ItemStack stack = this.getCurrentItemStack();
        ItemStack main = mc.player.getMainHandItem();
        if (matchesHeldStack(stack, main)) {
            return mainArm;
        }
        return mainArm == HumanoidArm.LEFT ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
    }

    private static boolean matchesHeldStack(ItemStack renderStack, ItemStack heldStack) {
        if (renderStack == heldStack) {
            return true;
        }
        if (renderStack == null || renderStack.isEmpty() || heldStack == null || heldStack.isEmpty()) {
            return false;
        }
        if (ItemStack.isSameItemSameComponents(renderStack, heldStack)) {
            return true;
        }
        return ItemStack.isSameItem(renderStack, heldStack);
    }
}
