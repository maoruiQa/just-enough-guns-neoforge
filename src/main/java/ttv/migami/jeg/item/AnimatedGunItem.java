package ttv.migami.jeg.item;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
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
    private static final int RELOAD_STAGE_NONE = 0;
    private static final int RELOAD_STAGE_START = 1;
    private static final int RELOAD_STAGE_LOOP = 2;
    private static final int RELOAD_STAGE_STOP = 3;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().then(ANIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation AIM_SHOOT = RawAnimation.begin().then(ANIM_AIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RELOAD = RawAnimation.begin().then(ANIM_RELOAD, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_START = RawAnimation.begin().then(ANIM_RELOAD_START, Animation.LoopType.PLAY_ONCE).thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_STOP = RawAnimation.begin().then(ANIM_RELOAD_STOP, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation SPRINT = RawAnimation.begin().then(ANIM_SPRINT, Animation.LoopType.HOLD_ON_LAST_FRAME);

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
                    if (state.getController().isPlayingTriggeredAnimation()) {
                        return PlayState.CONTINUE;
                    }

                    ItemStack stack = state.getData(DataTickets.ITEMSTACK);
                    RawAnimation reloadAnimation = reloadAnimationFor(stack);
                    if (reloadAnimation != null) {
                        return state.setAndContinue(reloadAnimation);
                    }

                    if (shouldPlaySprintAnimation(state, stack)) {
                        return state.setAndContinue(SPRINT);
                    }

                    return state.setAndContinue(IDLE);
                }
        ).receiveTriggeredAnimations()
                .triggerableAnim(ANIM_SHOOT, SHOOT)
                .triggerableAnim(ANIM_AIM_SHOOT, AIM_SHOOT)
                .triggerableAnim(ANIM_RELOAD, RELOAD)
                .triggerableAnim(ANIM_RELOAD_START, RELOAD_START)
                .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
                .triggerableAnim(ANIM_RELOAD_STOP, RELOAD_STOP)
                .triggerableAnim(ANIM_SPRINT, SPRINT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
        }
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
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

    private static RawAnimation reloadAnimationFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            return null;
        }

        int stage = stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE);
        return switch (stage) {
            case RELOAD_STAGE_START -> RELOAD_START;
            case RELOAD_STAGE_LOOP -> RELOAD_LOOP;
            case RELOAD_STAGE_STOP -> RELOAD_STOP;
            default -> RELOAD;
        };
    }

    private boolean shouldPlaySprintAnimation(Object state, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != this) {
            return false;
        }
        if (!isFirstPersonPerspective(state)) {
            return false;
        }
        if (isClientAiming()) {
            return false;
        }
        if ("minigun".equals(this.getStats().id().getPath())) {
            return false;
        }
        return isClientPlayerSprinting();
    }

    private static boolean isFirstPersonPerspective(Object state) {
        try {
            Method getData = state.getClass().getMethod("getData", Object.class);
            Object perspective = getData.invoke(state, DataTickets.ITEM_RENDER_PERSPECTIVE);
            if (perspective == null) {
                return false;
            }
            Method firstPerson = perspective.getClass().getMethod("firstPerson");
            Object value = firstPerson.invoke(perspective);
            return value instanceof Boolean bool && bool;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isClientPlayerSprinting() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return false;
            }
            Object player = minecraftClass.getField("player").get(minecraft);
            if (player == null) {
                return false;
            }
            Object value = player.getClass().getMethod("isSprinting").invoke(player);
            return value instanceof Boolean bool && bool;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isClientAiming() {
        try {
            Class<?> aimingHandlerClass = Class.forName("ttv.migami.jeg.client.handler.AimingHandler");
            Object handler = aimingHandlerClass.getMethod("get").invoke(null);
            Object value = aimingHandlerClass.getMethod("isAiming").invoke(handler);
            return value instanceof Boolean bool && bool;
        } catch (Throwable ignored) {
            return false;
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
