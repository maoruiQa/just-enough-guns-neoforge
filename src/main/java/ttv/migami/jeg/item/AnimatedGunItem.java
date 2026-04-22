package ttv.migami.jeg.item;

import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.gun.GunStats;

public final class AnimatedGunItem extends GunItem implements GeoItem {
    public static final String CONTROLLER = "controller";
    public static final String ANIM_SHOOT = "shoot";
    public static final String ANIM_RELOAD = "reload";
    public static final String ANIM_RELOAD_START = "reload_start";
    public static final String ANIM_RELOAD_LOOP = "reload_loop";
    public static final String ANIM_RELOAD_STOP = "reload_stop";
    public static final String ANIM_RELOAD_ALT = "reload_alt";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AnimatedGunItem(Properties properties, GunStats stats) {
        super(properties, stats);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                CONTROLLER,
                0,
                state -> {
                    // Triggered animations (reload/shoot/etc) are driven by server-side triggerAnim calls.
                    // Do not overwrite them every tick with idle, or they will never be visible.
                    if (!state.controller().isPlayingTriggeredAnimation()) {
                        state.setAndContinue(IDLE);
                    }
                    return PlayState.CONTINUE;
                }
        ).receiveTriggeredAnimations()
         .triggerableAnim(ANIM_SHOOT, RawAnimation.begin().thenPlay(ANIM_SHOOT))
         .triggerableAnim(ANIM_RELOAD, RawAnimation.begin().thenPlay(ANIM_RELOAD))
         .triggerableAnim(ANIM_RELOAD_START, RawAnimation.begin().thenPlay(ANIM_RELOAD_START))
         .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
         .triggerableAnim(ANIM_RELOAD_STOP, RawAnimation.begin().thenPlay(ANIM_RELOAD_STOP))
         .triggerableAnim(ANIM_RELOAD_ALT, RawAnimation.begin().thenPlay(ANIM_RELOAD_ALT)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
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
        trigger(level, triggerEntity, stack, ANIM_SHOOT);
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
