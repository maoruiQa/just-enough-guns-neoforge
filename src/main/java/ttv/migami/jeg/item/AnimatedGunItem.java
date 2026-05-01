package ttv.migami.jeg.item;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.network.NetworkHandler;

public final class AnimatedGunItem extends GunItem implements GeoItem {
    public static final String CONTROLLER = "controller";
    public static final String ANIM_SHOOT = "shoot";
    public static final String ANIM_RELOAD = "reload";
    public static final String ANIM_RELOAD_START = "reload_start";
    public static final String ANIM_RELOAD_LOOP = "reload_loop";
    public static final String ANIM_RELOAD_STOP = "reload_stop";
    public static final String ANIM_AIM_SHOOT = "aim_shoot";
    public static final String ANIM_SPRINT = "sprint";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().then(ANIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation AIM_SHOOT = RawAnimation.begin().then(ANIM_AIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RELOAD = RawAnimation.begin().then(ANIM_RELOAD, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_START = RawAnimation.begin().then(ANIM_RELOAD_START, Animation.LoopType.PLAY_ONCE).thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_STOP = RawAnimation.begin().then(ANIM_RELOAD_STOP, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation SPRINT = RawAnimation.begin().thenLoop(ANIM_SPRINT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AnimatedGunItem(Properties properties, GunStats stats) {
        super(properties, stats);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                CONTROLLER,
                0,
                this::animationPredicate
        )
                .triggerableAnim(ANIM_SHOOT, SHOOT)
                .triggerableAnim(ANIM_AIM_SHOOT, AIM_SHOOT)
                .triggerableAnim(ANIM_RELOAD, RELOAD)
                .triggerableAnim(ANIM_RELOAD_START, RELOAD_START)
                .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
                .triggerableAnim(ANIM_RELOAD_STOP, RELOAD_STOP)
                .triggerableAnim(ANIM_SPRINT, SPRINT));
    }

    private PlayState animationPredicate(AnimationState<AnimatedGunItem> state) {
        if (state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
        if (isFirstPersonRender(state, stack)) {
            var player = software.bernie.geckolib.util.ClientUtil.getClientPlayer();
            if (player != null && player.isSprinting() && !AimingHandler.get().isAiming()) {
                return state.setAndContinue(SPRINT);
            }
        }

        return state.setAndContinue(IDLE);
    }

    private static boolean isFirstPersonRender(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (perspective != null && perspective.firstPerson()) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(stack, minecraft.player.getMainHandItem())
                || ItemStack.isSameItemSameComponents(stack, minecraft.player.getOffhandItem())
                || ItemStack.isSameItem(stack, minecraft.player.getMainHandItem())
                || ItemStack.isSameItem(stack, minecraft.player.getOffhandItem());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private AnimatedGunRenderer renderer;

            @Override
            public AnimatedGunRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new AnimatedGunRenderer();
                }
                return renderer;
            }
        });
    }

    private void trigger(Level level, Entity triggerEntity, ItemStack stack, String animation) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long id = GeoItem.getOrAssignId(stack, serverLevel);
        triggerAnim(triggerEntity, id, CONTROLLER, animation);
    }

    public void triggerShoot(Level level, Entity triggerEntity, ItemStack stack) {
        boolean aiming = triggerEntity instanceof net.minecraft.world.entity.player.Player player && NetworkHandler.isAiming(player);
        trigger(level, triggerEntity, stack, aiming ? ANIM_AIM_SHOOT : ANIM_SHOOT);
    }

    public void triggerReload(Level level, Entity triggerEntity, ItemStack stack) {
        trigger(level, triggerEntity, stack, ANIM_RELOAD);
    }

    public void triggerReloadStart(Level level, Entity triggerEntity, ItemStack stack) {
        trigger(level, triggerEntity, stack, ANIM_RELOAD_START);
    }

    public void triggerReloadLoop(Level level, Entity triggerEntity, ItemStack stack) {
        trigger(level, triggerEntity, stack, ANIM_RELOAD_LOOP);
    }

    public void triggerReloadStop(Level level, Entity triggerEntity, ItemStack stack) {
        trigger(level, triggerEntity, stack, ANIM_RELOAD_STOP);
    }
}

