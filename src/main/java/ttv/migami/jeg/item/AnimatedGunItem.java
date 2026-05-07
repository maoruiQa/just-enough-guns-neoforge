package ttv.migami.jeg.item;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;

public final class AnimatedGunItem extends GunItem implements GeoItem {
    public static final String CONTROLLER = "controller";
    public static final String ANIM_SHOOT = "shoot";
    public static final String ANIM_RELOAD = "reload";
    public static final String ANIM_RELOAD_START = "reload_start";
    public static final String ANIM_RELOAD_LOOP = "reload_loop";
    public static final String ANIM_RELOAD_STOP = "reload_stop";
    public static final String ANIM_RELOAD_ALT = "reload_alt";
    public static final String ANIM_AIM_SHOOT = "aim_shoot";
    public static final String ANIM_SPRINT = "sprint";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().then(ANIM_SHOOT, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation AIM_SHOOT = RawAnimation.begin().then(ANIM_AIM_SHOOT, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD = RawAnimation.begin().then(ANIM_RELOAD, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_ALT = RawAnimation.begin().then(ANIM_RELOAD_ALT, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_START = RawAnimation.begin().then(ANIM_RELOAD_START, LoopType.PLAY_ONCE).thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_STOP = RawAnimation.begin().then(ANIM_RELOAD_STOP, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation SPRINT = RawAnimation.begin().then(ANIM_SPRINT, LoopType.HOLD_ON_LAST_FRAME);
    private static final long CLIENT_SHOOT_TRIGGER_WINDOW_NANOS = 250_000_000L;
    private static final long CLIENT_SPRINT_AFTER_SHOOT_SUPPRESSION_NANOS = 250_000_000L;

    private static final int RELOAD_STAGE_NONE = 0;
    private static final int RELOAD_STAGE_START = 1;
    private static final int RELOAD_STAGE_LOOP = 2;
    private static final int RELOAD_STAGE_STOP = 3;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static ItemStack clientShootStack = ItemStack.EMPTY;
    private static boolean clientShootAiming;
    private static long clientShootTriggerDeadlineNanos;
    private static long clientSprintAnimationBlockedUntilNanos;

    public AnimatedGunItem(Properties properties, GunStats stats) {
        super(properties, stats);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                CONTROLLER,
                0,
                this::animationPredicate
        ).receiveTriggeredAnimations()
                .triggerableAnim(ANIM_SHOOT, SHOOT)
                .triggerableAnim(ANIM_AIM_SHOOT, AIM_SHOOT)
                .triggerableAnim(ANIM_RELOAD, RELOAD)
                .triggerableAnim(ANIM_RELOAD_ALT, RELOAD_ALT)
                .triggerableAnim(ANIM_RELOAD_START, RELOAD_START)
                .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
                .triggerableAnim(ANIM_RELOAD_STOP, RELOAD_STOP)
                .triggerableAnim(ANIM_SPRINT, SPRINT));
    }

    private PlayState animationPredicate(AnimationTest<AnimatedGunItem> test) {
        ItemStack renderStack = resolveRenderStack(test);
        if (shouldContinueReloadAnimation(test.controller(), renderStack)) {
            return PlayState.CONTINUE;
        }

        RawAnimation reloadAnimation = reloadAnimationFor(renderStack);
        if (reloadAnimation != null) {
            return test.setAndContinue(reloadAnimation);
        }

        if (triggerPendingClientShoot(test, renderStack)) {
            return PlayState.CONTINUE;
        }

        if (finishTriggeredShootAnimation(test.controller())) {
            return test.setAndContinue(IDLE);
        }

        if (test.controller().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        if (isFirstPersonRender(test)) {
            var player = com.geckolib.util.ClientUtil.getClientPlayer();
            if (player != null && player.isSprinting() && !isClientAiming() && !isLocalAttackDown(player)
                    && !isClientSprintAnimationBlocked() && canApplySprintingAnimation(renderStack)) {
                return test.setAndContinue(SPRINT);
            }
        }

        return test.setAndContinue(IDLE);
    }

    private static RawAnimation reloadAnimationFor(ItemStack stack) {
        if (stack.isEmpty()) {
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

    private static boolean shouldContinueReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!isReloadAnimation(controller.getCurrentRawAnimation())) {
            return false;
        }
        if (!stack.isEmpty() && stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0) {
            return true;
        }

        var point = controller.getCurrentAnimationPoint();
        if (point == null || point.animation() == null || point.hasFinished()) {
            return false;
        }
        String animationName = point.animation().name();
        return ANIM_RELOAD.equals(animationName)
                || ANIM_RELOAD_START.equals(animationName)
                || ANIM_RELOAD_STOP.equals(animationName);
    }

    private static boolean finishTriggeredShootAnimation(AnimationController<AnimatedGunItem> controller) {
        if (!controller.isTriggeredAnimation(ANIM_SHOOT) && !controller.isTriggeredAnimation(ANIM_AIM_SHOOT)) {
            return false;
        }

        var point = controller.getCurrentAnimationPoint();
        if (point == null || point.animation() == null || !"idle".equals(point.animation().name())) {
            return false;
        }

        controller.stopTriggeredAnimation();
        return true;
    }

    private static boolean triggerPendingClientShoot(AnimationTest<AnimatedGunItem> test, ItemStack renderStack) {
        if (clientShootStack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() > clientShootTriggerDeadlineNanos) {
            clearPendingClientShoot();
            return false;
        }
        if (!matchesHeldStack(renderStack, clientShootStack)) {
            return false;
        }
        if (shouldDiscardReleasedAutomaticShot(renderStack)) {
            clearPendingClientShoot();
            return false;
        }

        String animation = clientShootAiming ? ANIM_AIM_SHOOT : ANIM_SHOOT;
        if (!test.controller().triggerAnimation(animation)) {
            clearPendingClientShoot();
            return false;
        }
        clearPendingClientShoot();
        return true;
    }

    private static void clearPendingClientShoot() {
        clientShootStack = ItemStack.EMPTY;
        clientShootAiming = false;
        clientShootTriggerDeadlineNanos = 0L;
    }

    public static void suppressSprintAnimationBriefly() {
        clientSprintAnimationBlockedUntilNanos = System.nanoTime() + CLIENT_SPRINT_AFTER_SHOOT_SUPPRESSION_NANOS;
    }

    private static boolean isClientSprintAnimationBlocked() {
        return System.nanoTime() < clientSprintAnimationBlockedUntilNanos;
    }

    private static boolean shouldDiscardReleasedAutomaticShot(ItemStack renderStack) {
        if (!(renderStack.getItem() instanceof GunItem gun) || !gun.isAutomatic()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.options.keyAttack.isDown()) {
            return false;
        }

        return matchesHeldStack(renderStack, minecraft.player.getMainHandItem())
                || matchesHeldStack(renderStack, minecraft.player.getOffhandItem());
    }

    private static boolean isReloadAnimation(RawAnimation animation) {
        if (animation == null) {
            return false;
        }
        return animation.getAnimationStages().stream()
                .map(RawAnimation.Stage::animationName)
                .anyMatch(name -> ANIM_RELOAD.equals(name)
                        || ANIM_RELOAD_START.equals(name)
                        || ANIM_RELOAD_LOOP.equals(name)
                        || ANIM_RELOAD_STOP.equals(name));
    }

    private static boolean isFirstPersonRender(AnimationTest<AnimatedGunItem> test) {
        var perspective = test.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (perspective != null && perspective.firstPerson()) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }

        ItemStack stack = resolveRenderStack(test);
        if (stack.isEmpty()) {
            return false;
        }

        return matchesHeldStack(stack, minecraft.player.getMainHandItem())
                || matchesHeldStack(stack, minecraft.player.getOffhandItem());
    }

    private static ItemStack resolveRenderStack(AnimationTest<AnimatedGunItem> test) {
        ItemStack stack = getItemStackFromRenderer(test);
        if (!stack.isEmpty()) {
            return stack;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (matchesAnimatedGun(test.animatable(), mainHand)) {
            return mainHand;
        }

        ItemStack offHand = minecraft.player.getOffhandItem();
        if (matchesAnimatedGun(test.animatable(), offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getItemStackFromRenderer(AnimationTest<AnimatedGunItem> test) {
        try {
            Class<?> rendererClass = Class.forName("ttv.migami.jeg.client.render.gun.AnimatedGunRenderer");
            java.lang.reflect.Field itemStackField = rendererClass.getDeclaredField("ITEM_STACK");
            itemStackField.setAccessible(true);
            Object dataTicket = itemStackField.get(null);
            java.lang.reflect.Method getDataMethod = test.getClass().getMethod("getDataOrDefault",
                    Class.forName("com.geckolib.constant.DataTicket"), Object.class);
            return (ItemStack) getDataMethod.invoke(test, dataTicket, ItemStack.EMPTY);
        } catch (ReflectiveOperationException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean matchesAnimatedGun(AnimatedGunItem item, ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == item;
    }

    private static boolean matchesHeldStack(ItemStack renderStack, ItemStack heldStack) {
        if (renderStack == heldStack) {
            return true;
        }
        if (renderStack.isEmpty() || heldStack.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(renderStack, heldStack)
                || ItemStack.isSameItem(renderStack, heldStack);
    }

    private static boolean canApplySprintingAnimation(ItemStack stack) {
        if (!(stack.getItem() instanceof AnimatedGunItem gun)) {
            return true;
        }
        return !"minigun".equals(gun.getStats().id().getPath());
    }

    private static boolean isClientAiming() {
        try {
            Class<?> handlerClass = Class.forName("ttv.migami.jeg.client.handler.AimingHandler");
            Method getMethod = handlerClass.getMethod("get");
            Object handler = getMethod.invoke(null);
            Method isAimingMethod = handlerClass.getMethod("isAiming");
            return Boolean.TRUE.equals(isAimingMethod.invoke(handler));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isLocalAttackDown(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.player == entity && minecraft.options.keyAttack.isDown();
    }

    public static void triggerClientShoot(Entity entity, boolean aiming) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(entity instanceof Player) || minecraft == null || minecraft.player != entity) {
            return;
        }

        ItemStack mainHand = minecraft.player.getMainHandItem();
        ItemStack offHand = minecraft.player.getOffhandItem();
        ItemStack stack = mainHand.getItem() instanceof AnimatedGunItem ? mainHand : offHand;
        if (!(stack.getItem() instanceof AnimatedGunItem)) {
            clearPendingClientShoot();
            return;
        }

        suppressSprintAnimationBriefly();
        clientShootStack = stack;
        clientShootAiming = aiming;
        clientShootTriggerDeadlineNanos = System.nanoTime() + CLIENT_SHOOT_TRIGGER_WINDOW_NANOS;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        GeoItem.getOrAssignId(stack, level);
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<?> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    try {
                        Class<?> clazz = Class.forName("ttv.migami.jeg.client.render.gun.AnimatedGunRenderer");
                        renderer = (GeoItemRenderer<?>) clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create AnimatedGunRenderer", e);
                    }
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
        boolean aiming = triggerEntity instanceof Player player && NetworkHandler.isAiming(player);
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
