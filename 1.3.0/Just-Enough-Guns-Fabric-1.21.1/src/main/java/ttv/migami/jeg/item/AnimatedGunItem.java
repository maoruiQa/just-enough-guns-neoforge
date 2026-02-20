package ttv.migami.jeg.item;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.gun.GunStats;

public final class AnimatedGunItem extends GunItem implements GeoItem {
    public static final String CONTROLLER = "controller";
    public static final String ANIM_SHOOT = "shoot";
    public static final String ANIM_RELOAD = "reload";
    public static final String ANIM_RELOAD_START = "reload_start";
    public static final String ANIM_RELOAD_LOOP = "reload_loop";
    public static final String ANIM_RELOAD_STOP = "reload_stop";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private transient Object cachedGeoItemRenderer;

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
                state -> {
                    if (!state.getController().isPlayingTriggeredAnimation()) {
                        state.setAndContinue(IDLE);
                    }
                    return PlayState.CONTINUE;
                }
        )
                .triggerableAnim(ANIM_SHOOT, RawAnimation.begin().thenPlay(ANIM_SHOOT))
                .triggerableAnim(ANIM_RELOAD, RawAnimation.begin().thenPlay(ANIM_RELOAD))
                .triggerableAnim(ANIM_RELOAD_START, RawAnimation.begin().thenPlay(ANIM_RELOAD_START))
                .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
                .triggerableAnim(ANIM_RELOAD_STOP, RawAnimation.begin().thenPlay(ANIM_RELOAD_STOP)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void createGeoRenderer(Consumer consumer) {
        try {
            Class<?> providerClass = Class.forName("software.bernie.geckolib.animatable.client.GeoRenderProvider");
            Object provider = Proxy.newProxyInstance(
                    providerClass.getClassLoader(),
                    new Class<?>[] {providerClass},
                    (proxy, method, args) -> {
                        if ("getGeoItemRenderer".equals(method.getName())) {
                            if (cachedGeoItemRenderer == null) {
                                Class<?> rendererClass = Class.forName("ttv.migami.jeg.client.render.gun.AnimatedGunRenderer");
                                cachedGeoItemRenderer = rendererClass.getDeclaredConstructor().newInstance();
                            }
                            return cachedGeoItemRenderer;
                        }
                        return null;
                    }
            );
            consumer.accept(provider);
        } catch (ClassNotFoundException ignored) {
            // Dedicated server: no client renderer classes available.
        } catch (Exception e) {
            throw new RuntimeException("Failed to create AnimatedGunRenderer provider", e);
        }
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
