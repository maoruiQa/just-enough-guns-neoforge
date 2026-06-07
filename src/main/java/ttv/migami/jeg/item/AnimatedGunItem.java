package ttv.migami.jeg.item;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.network.NetworkHandler;

public final class AnimatedGunItem extends GunItem implements GeoItem {
    public static final String CONTROLLER = "controller";
    public static final String ANIM_SHOOT = "shoot";
    public static final String ANIM_RELOAD = "reload";
    public static final String ANIM_RELOAD_START = "reload_start";
    public static final String ANIM_RELOAD_LOOP = "reload_loop";
    public static final String ANIM_RELOAD_STOP = "reload_stop";
    public static final String ANIM_AIM_SHOOT = "aim_shoot";
    public static final String ANIM_DRAW = "draw";
    public static final String ANIM_SPRINT = "sprint";
    public static final String ANIM_MELEE = "melee";
    public static final String ANIM_BAYONET = "bayonet";
    private static final int RELOAD_STAGE_NONE = 0;
    private static final int RELOAD_STAGE_START = 1;
    private static final int RELOAD_STAGE_LOOP = 2;
    private static final int RELOAD_STAGE_STOP = 3;
    private static final int DRAW_TICKS = 14;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().then(ANIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation AIM_SHOOT = RawAnimation.begin().then(ANIM_AIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RELOAD = RawAnimation.begin().then(ANIM_RELOAD, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_START = RawAnimation.begin().then(ANIM_RELOAD_START, Animation.LoopType.PLAY_ONCE).thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_STOP = RawAnimation.begin().then(ANIM_RELOAD_STOP, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation DRAW = RawAnimation.begin().then(ANIM_DRAW, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation SPRINT = RawAnimation.begin().then(ANIM_SPRINT, Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation MELEE = RawAnimation.begin().then(ANIM_MELEE, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation BAYONET = RawAnimation.begin().then(ANIM_BAYONET, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final long CLIENT_SHOOT_TRIGGER_WINDOW_NANOS = 250_000_000L;
    private static final long CLIENT_DRAW_VISUAL_NANOS = 1_700_000_000L;
    private static final ResourceLocation GUN_RUSTLE_SOUND = Reference.id("item.gun_rustle");
    private static final ResourceLocation GUN_SCREW_SOUND = Reference.id("item.gun_screw");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private transient Object cachedGeoItemRenderer;
    private static ItemStack clientShootStack = ItemStack.EMPTY;
    private static boolean clientShootAiming;
    private static long clientShootTriggerDeadlineNanos;
    private static ItemStack clientMeleeStack = ItemStack.EMPTY;
    private static boolean clientMeleeBayonet;
    private static long clientMeleeTriggerDeadlineNanos;
    private static ItemStack clientDrawTriggerStack = ItemStack.EMPTY;
    private static long clientDrawTriggerDeadlineNanos;
    private static ItemStack clientDrawStack = ItemStack.EMPTY;
    private static long clientDrawAnimationDeadlineNanos;
    private static ItemStack clientDrawResetStack = ItemStack.EMPTY;
    private static long clientDrawResetDeadlineNanos;
    private static String lastFirstPersonGunId = "";

    public AnimatedGunItem(Properties properties, GunStats stats) {
        super(properties, stats);
        GeoItem.registerSyncedAnimatable(this);
    }

    private static final class GunAnimationController extends AnimationController<AnimatedGunItem> {
        private GunAnimationController(AnimatedGunItem animatable, String name, int transitionLength,
                                       AnimationController.AnimationStateHandler<AnimatedGunItem> stateHandler) {
            super(animatable, name, transitionLength, stateHandler);
        }

        private boolean stopTriggeredAnimationIfActive() {
            return stopTriggeredAnimation();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new GunAnimationController(
                this,
                CONTROLLER,
                0,
                this::animationPredicate
        ).setSoundKeyframeHandler(this::soundListener)
                .receiveTriggeredAnimations()
                .triggerableAnim(ANIM_SHOOT, SHOOT)
                .triggerableAnim(ANIM_AIM_SHOOT, AIM_SHOOT)
                .triggerableAnim(ANIM_RELOAD, RELOAD)
                .triggerableAnim(ANIM_RELOAD_START, RELOAD_START)
                .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
                .triggerableAnim(ANIM_RELOAD_STOP, RELOAD_STOP)
                .triggerableAnim(ANIM_DRAW, DRAW)
                .triggerableAnim(ANIM_SPRINT, SPRINT)
                .triggerableAnim(ANIM_MELEE, MELEE)
                .triggerableAnim(ANIM_BAYONET, BAYONET));
    }

    private PlayState animationPredicate(AnimationState<AnimatedGunItem> state) {
        if (isNonFirstPersonPerspective(state)) {
            return PlayState.STOP;
        }

        ItemStack renderStack = state.getData(DataTickets.ITEMSTACK);
        ItemStack stack = animationStateStack(state, renderStack);
        clearStaleRenderReloadVisualState(renderStack, stack);
        resetControllerOnGunChange(state.getController(), stack);

        if (triggerPendingClientDraw(state, stack)) {
            return PlayState.CONTINUE;
        }

        if (triggerPendingClientMelee(state, stack)) {
            return PlayState.CONTINUE;
        }
        if (shouldContinueMeleeAnimation(state.getController())) {
            return PlayState.CONTINUE;
        }

        RawAnimation shootAnimation = pendingClientShootAnimationFor(stack);
        if (shootAnimation != null) {
            state.getController().forceAnimationReset();
            clearPendingClientShoot();
            return state.setAndContinue(shootAnimation);
        }

        clearInterruptedReloadAnimation(state.getController(), stack);
        if (shouldContinueReloadAnimation(state.getController(), stack)) {
            return PlayState.CONTINUE;
        }

        RawAnimation drawAnimation = drawAnimationFor(stack);
        if (drawAnimation != null && resetDrawAnimationIfRequested(state.getController(), stack)) {
            return state.setAndContinue(drawAnimation);
        }
        if (shouldContinueDrawAnimation(state.getController(), stack)) {
            return PlayState.CONTINUE;
        }
        drawAnimation = drawAnimationFor(stack);
        if (drawAnimation != null) {
            return state.setAndContinue(drawAnimation);
        }

        RawAnimation reloadAnimation = reloadAnimationFor(stack);
        if (reloadAnimation != null) {
            return state.setAndContinue(reloadAnimation);
        }

        stopFinishedDrawAnimation(state.getController(), stack);
        stopFinishedReloadAnimation(state.getController(), stack);
        if (shouldContinueTriggeredAnimation(state.getController())) {
            return PlayState.CONTINUE;
        }

        if (shouldPlaySprintAnimation(state, stack)) {
            return setSprintOrBayonetSprintAnimation(state, stack);
        }

        clearFinishedDrawAnimation(state.getController(), stack);
        clearFinishedReloadAnimation(state.getController(), stack);
        if (isIdleOnlyAnimation(state.getController().getCurrentRawAnimation())
                && !state.getController().hasAnimationFinished()) {
            return PlayState.CONTINUE;
        }
        return state.setAndContinue(IDLE);
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

    private static RawAnimation drawAnimationFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (hasRecentDrawAnimation(stack)) {
            return DRAW;
        }
        if (stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0) <= 0) {
            return null;
        }
        rememberDrawAnimation(stack);
        return DRAW;
    }

    private static void resetControllerOnGunChange(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof AnimatedGunItem gun)) {
            lastFirstPersonGunId = "";
            return;
        }
        String currentId = gun.getStats().id().toString();
        if (!lastFirstPersonGunId.isEmpty() && !currentId.equals(lastFirstPersonGunId)) {
            controller.forceAnimationReset();
            controller.stop();
        }
        lastFirstPersonGunId = currentId;
    }

    private static PlayState setSprintOrBayonetSprintAnimation(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        if (hasBayonet(stack)) {
            return state.setAndContinue(IDLE);
        }
        return setSprintAnimation(state, stack);
    }

    private static PlayState setSprintAnimation(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        if (hasAnimation(state.getController().getCurrentRawAnimation(), ANIM_SPRINT)
                && !state.getController().hasAnimationFinished()) {
            return PlayState.CONTINUE;
        }
        clearFinishedDrawAnimation(state.getController(), stack);
        clearFinishedReloadAnimation(state.getController(), stack);
        state.getController().forceAnimationReset();
        return state.setAndContinue(SPRINT);
    }

    private static boolean shouldContinueReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        RawAnimation current = controller.getCurrentRawAnimation();
        if (!isReloadAnimation(current) || stack == null || stack.isEmpty()) {
            return false;
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            controller.forceAnimationReset();
            controller.stop();
            return false;
        }

        int stage = stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE);
        return switch (stage) {
            case RELOAD_STAGE_START -> hasAnimation(current, ANIM_RELOAD_START);
            case RELOAD_STAGE_LOOP -> hasAnimation(current, ANIM_RELOAD_LOOP);
            case RELOAD_STAGE_STOP -> hasAnimation(current, ANIM_RELOAD_STOP);
            default -> hasAnimation(current, ANIM_RELOAD);
        };
    }

    private static boolean clearInterruptedReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!isReloadAnimation(controller.getCurrentRawAnimation())) {
            return false;
        }
        if (stack != null && !stack.isEmpty()
                && stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0) {
            return false;
        }
        controller.forceAnimationReset();
        controller.stop();
        return true;
    }

    private static boolean shouldContinueTriggeredAnimation(AnimationController<AnimatedGunItem> controller) {
        if (!controller.isPlayingTriggeredAnimation()) {
            return false;
        }
        RawAnimation current = controller.getCurrentRawAnimation();
        if (hasAnyAnimation(current, ANIM_SHOOT, ANIM_AIM_SHOOT, ANIM_RELOAD, ANIM_RELOAD_START,
                ANIM_RELOAD_LOOP, ANIM_RELOAD_STOP, ANIM_MELEE, ANIM_BAYONET)) {
            return true;
        }
        controller.forceAnimationReset();
        controller.stop();
        return false;
    }

    private static void stopFinishedDrawAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!(controller instanceof GunAnimationController gunController)
                || !hasAnimation(controller.getCurrentRawAnimation(), ANIM_DRAW)
                || !controller.hasAnimationFinished()) {
            return;
        }

        gunController.stopTriggeredAnimationIfActive();
        clearClientDrawAnimationState(stack);
    }

    private static void clearFinishedDrawAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!hasAnimation(controller.getCurrentRawAnimation(), ANIM_DRAW) || !controller.hasAnimationFinished()) {
            return;
        }

        if (controller instanceof GunAnimationController gunController) {
            gunController.stopTriggeredAnimationIfActive();
        }
        controller.forceAnimationReset();
        controller.stop();
        clearClientDrawAnimationState(stack);
    }

    private static void stopFinishedReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!(controller instanceof GunAnimationController gunController)
                || !isReloadAnimation(controller.getCurrentRawAnimation())
                || !controller.hasAnimationFinished()
                || isReloadActive(stack)) {
            return;
        }

        gunController.stopTriggeredAnimationIfActive();
        clearReloadVisualState(stack);
    }

    private static void clearFinishedReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!isReloadAnimation(controller.getCurrentRawAnimation())
                || !controller.hasAnimationFinished()
                || isReloadActive(stack)) {
            return;
        }

        if (controller instanceof GunAnimationController gunController) {
            gunController.stopTriggeredAnimationIfActive();
        }
        controller.forceAnimationReset();
        controller.stop();
        clearReloadVisualState(stack);
    }

    private static boolean isReloadActive(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0;
    }

    private static boolean isReloadAnimation(RawAnimation animation) {
        return hasAnyAnimation(animation, ANIM_RELOAD, ANIM_RELOAD_START, ANIM_RELOAD_LOOP, ANIM_RELOAD_STOP);
    }

    private static boolean isIdleOnlyAnimation(RawAnimation animation) {
        return animation != null
                && animation.getAnimationStages().size() == 1
                && hasAnimation(animation, "idle");
    }

    private static boolean isMeleeAnimation(RawAnimation animation) {
        return hasAnyAnimation(animation, ANIM_MELEE, ANIM_BAYONET);
    }

    private static boolean hasAnimation(RawAnimation animation, String animationName) {
        return hasAnyAnimation(animation, animationName);
    }

    private static boolean hasAnyAnimation(RawAnimation animation, String... animationNames) {
        if (animation == null) {
            return false;
        }
        return animation.getAnimationStages().stream()
                .map(RawAnimation.Stage::animationName)
                .anyMatch(name -> {
                    for (String animationName : animationNames) {
                        if (animationName.equals(name)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    private static boolean shouldContinueDrawAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!hasAnimation(controller.getCurrentRawAnimation(), ANIM_DRAW) || stack == null || stack.isEmpty()) {
            return false;
        }
        boolean drawActive = stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0) > 0
                || hasRecentDrawAnimation(stack);
        if (drawActive && !controller.hasAnimationFinished() && !isClientAiming()) {
            return true;
        }
        clearClientDrawAnimationState(stack);
        return false;
    }

    private static void rememberDrawAnimation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            clearClientDrawAnimationState(stack);
            return;
        }
        clientDrawStack = stack.copy();
        clientDrawAnimationDeadlineNanos = System.nanoTime() + CLIENT_DRAW_VISUAL_NANOS;
    }

    private static void startDrawAnimation(ItemStack stack) {
        rememberDrawAnimation(stack);
        requestDrawAnimationReset(stack);
        queuePendingClientDraw(stack);
    }

    private static boolean hasRecentDrawAnimation(ItemStack stack) {
        if (clientDrawStack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() > clientDrawAnimationDeadlineNanos) {
            clearRecentDrawAnimation();
            return false;
        }
        return matchesHeldStack(stack, clientDrawStack);
    }

    private static void clearRecentDrawAnimation() {
        clientDrawStack = ItemStack.EMPTY;
        clientDrawAnimationDeadlineNanos = 0L;
    }

    private static void clearClientDrawAnimationState(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            stack.remove(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get());
        }
        clearRecentDrawAnimation();
        clearDrawAnimationReset();
        clearPendingClientDraw();
    }

    private static void requestDrawAnimationReset(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            clearDrawAnimationReset();
            return;
        }
        clientDrawResetStack = stack.copy();
        clientDrawResetDeadlineNanos = System.nanoTime() + CLIENT_DRAW_VISUAL_NANOS;
    }

    private static boolean resetDrawAnimationIfRequested(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (clientDrawResetStack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() > clientDrawResetDeadlineNanos) {
            clearDrawAnimationReset();
            return false;
        }
        if (!matchesHeldStack(stack, clientDrawResetStack)) {
            return false;
        }
        controller.forceAnimationReset();
        controller.stop();
        clearDrawAnimationReset();
        return true;
    }

    private static void clearDrawAnimationReset() {
        clientDrawResetStack = ItemStack.EMPTY;
        clientDrawResetDeadlineNanos = 0L;
    }

    static void restartDrawAnimation(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            clearRecentDrawAnimation();
            resetClientAnimationInstance(stack);
            stack.set(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), DRAW_TICKS);
            startDrawAnimation(stack);
        }
    }

    static void restartDrawAnimationAfterReloadCancel(ItemStack stack) {
        clearReloadVisualState(stack);
        restartDrawAnimation(stack);
    }

    private static void resetClientAnimationInstance(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof AnimatedGunItem gun)) {
            return;
        }

        var manager = gun.getAnimatableInstanceCache().getManagerForId(GeoItem.getId(stack));
        var controller = manager.getAnimationControllers().get(CONTROLLER);
        if (controller != null) {
            controller.forceAnimationReset();
            controller.stop();
        }
        manager.clearSnapshotCache();
    }

    private static void clearReloadVisualState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_RELOAD_STAGE.get());
    }

    private static void queuePendingClientDraw(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            clearPendingClientDraw();
            return;
        }
        clearPendingClientMelee();
        clientDrawTriggerStack = stack.copy();
        clientDrawTriggerDeadlineNanos = System.nanoTime() + CLIENT_DRAW_VISUAL_NANOS;
    }

    private static boolean triggerPendingClientDraw(AnimationState<AnimatedGunItem> state, ItemStack renderStack) {
        if (clientDrawTriggerStack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() > clientDrawTriggerDeadlineNanos) {
            clearPendingClientDraw();
            return false;
        }
        if (!matchesHeldStack(renderStack, clientDrawTriggerStack)) {
            return false;
        }
        if (renderStack != null && !renderStack.isEmpty()
                && renderStack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0) {
            return false;
        }

        state.getController().forceAnimationReset();
        boolean triggered = state.getController().tryTriggerAnimation(ANIM_DRAW);
        clearDrawAnimationReset();
        if (triggered) {
            clearPendingClientDraw();
        }
        return triggered;
    }

    private static void clearPendingClientDraw() {
        clientDrawTriggerStack = ItemStack.EMPTY;
        clientDrawTriggerDeadlineNanos = 0L;
    }

    private static RawAnimation pendingClientShootAnimationFor(ItemStack renderStack) {
        if (clientShootStack.isEmpty()) {
            return null;
        }
        if (System.nanoTime() > clientShootTriggerDeadlineNanos) {
            clearPendingClientShoot();
            return null;
        }
        if (!matchesHeldStack(renderStack, clientShootStack)) {
            return null;
        }

        return clientShootAiming ? AIM_SHOOT : SHOOT;
    }

    private static boolean triggerPendingClientMelee(AnimationState<AnimatedGunItem> state, ItemStack renderStack) {
        if (clientMeleeStack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() > clientMeleeTriggerDeadlineNanos) {
            clearPendingClientMelee();
            return false;
        }
        if (!matchesHeldStack(renderStack, clientMeleeStack)) {
            return false;
        }

        state.getController().forceAnimationReset();
        boolean triggered = state.getController().tryTriggerAnimation(clientMeleeBayonet ? ANIM_BAYONET : ANIM_MELEE);
        if (triggered) {
            clearPendingClientMelee();
        }
        return triggered;
    }

    private static void clearPendingClientShoot() {
        clientShootStack = ItemStack.EMPTY;
        clientShootAiming = false;
        clientShootTriggerDeadlineNanos = 0L;
    }

    private static void clearPendingClientMelee() {
        clientMeleeStack = ItemStack.EMPTY;
        clientMeleeBayonet = false;
        clientMeleeTriggerDeadlineNanos = 0L;
    }

    private static boolean shouldContinueMeleeAnimation(AnimationController<AnimatedGunItem> controller) {
        return isMeleeAnimation(controller.getCurrentRawAnimation()) && !controller.hasAnimationFinished();
    }

    private static boolean isNonFirstPersonPerspective(AnimationState<AnimatedGunItem> state) {
        var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        return perspective != null && !perspective.firstPerson();
    }

    private ItemStack animationStateStack(AnimationState<AnimatedGunItem> state, ItemStack renderStack) {
        ItemStack liveStack = matchingLiveHeldStack(state, renderStack);
        if (!liveStack.isEmpty()) {
            return liveStack;
        }
        ItemStack localHeldStack = localHeldStackForItem(this);
        if (!localHeldStack.isEmpty()) {
            return localHeldStack;
        }
        if (renderStack != null && !renderStack.isEmpty()) {
            return renderStack;
        }
        return ItemStack.EMPTY;
    }

    private static void clearStaleRenderReloadVisualState(ItemStack renderStack, ItemStack liveStack) {
        if (renderStack == null || renderStack.isEmpty() || liveStack == null || liveStack.isEmpty() || renderStack == liveStack) {
            return;
        }
        if (liveStack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) <= 0
                && renderStack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0) {
            clearReloadVisualState(renderStack);
        }
    }

    private static ItemStack matchingLiveHeldStack(AnimationState<AnimatedGunItem> state, ItemStack renderStack) {
        Object player = clientPlayer();
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack perspectiveStack = heldStackForPerspective(player, state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE));
        if (matchesHeldStack(renderStack, perspectiveStack) || isStaleRenderCopyOf(renderStack, perspectiveStack)) {
            return perspectiveStack;
        }

        ItemStack mainHand = mainHandItem(player);
        if (matchesHeldStack(renderStack, mainHand) || isStaleRenderCopyOf(renderStack, mainHand)) {
            return mainHand;
        }

        ItemStack offHand = offHandItem(player);
        if (matchesHeldStack(renderStack, offHand) || isStaleRenderCopyOf(renderStack, offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static boolean isStaleRenderCopyOf(ItemStack renderStack, ItemStack heldStack) {
        if (renderStack == null || renderStack.isEmpty() || heldStack == null || heldStack.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItem(renderStack, heldStack)
                && renderStack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0
                && heldStack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) <= 0;
    }

    private static ItemStack heldStackForPerspective(Object player, Object perspective) {
        if (!(player instanceof Player playerEntity)) {
            return ItemStack.EMPTY;
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return playerEntity.getMainArm() == HumanoidArm.RIGHT ? playerEntity.getMainHandItem() : playerEntity.getOffhandItem();
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return playerEntity.getMainArm() == HumanoidArm.LEFT ? playerEntity.getMainHandItem() : playerEntity.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack localHeldStackForItem(AnimatedGunItem item) {
        Object player = clientPlayer();
        if (!(player instanceof Player playerEntity)) {
            return ItemStack.EMPTY;
        }
        ItemStack mainHand = playerEntity.getMainHandItem();
        if (matchesAnimatedGun(item, mainHand)) {
            return mainHand;
        }
        ItemStack offHand = playerEntity.getOffhandItem();
        if (matchesAnimatedGun(item, offHand)) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchesAnimatedGun(AnimatedGunItem item, ItemStack stack) {
        return item != null && stack != null && !stack.isEmpty() && stack.getItem() == item;
    }

    private boolean shouldPlaySprintAnimation(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != this) {
            return false;
        }
        if (!isFirstPersonRender(state, stack)) {
            return false;
        }
        if (isClientAiming()) {
            return false;
        }
        if (hasBayonet(stack)) {
            return false;
        }
        if ("minigun".equals(this.getStats().id().getPath())) {
            return false;
        }
        return isClientPlayerSprinting();
    }

    private static boolean isFirstPersonRender(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (perspective != null && perspective.firstPerson()) {
            return true;
        }

        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return false;
            }
            Object options = minecraftClass.getField("options").get(minecraft);
            Object cameraType = options.getClass().getMethod("getCameraType").invoke(options);
            Object firstPerson = cameraType.getClass().getMethod("isFirstPerson").invoke(cameraType);
            if (!(firstPerson instanceof Boolean bool) || !bool) {
                return false;
            }

            Object player = minecraftClass.getField("player").get(minecraft);
            if (player == null || stack == null || stack.isEmpty()) {
                return false;
            }
            ItemStack mainHand = (ItemStack) player.getClass().getMethod("getMainHandItem").invoke(player);
            ItemStack offHand = (ItemStack) player.getClass().getMethod("getOffhandItem").invoke(player);
            return ItemStack.isSameItemSameComponents(stack, mainHand)
                    || ItemStack.isSameItemSameComponents(stack, offHand)
                    || ItemStack.isSameItem(stack, mainHand)
                    || ItemStack.isSameItem(stack, offHand);
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
        if (!ItemStack.isSameItem(renderStack, heldStack)) {
            return false;
        }

        ItemStack renderCopy = renderStack.copy();
        ItemStack heldCopy = heldStack.copy();
        clearAnimationMatchState(renderCopy);
        clearAnimationMatchState(heldCopy);
        return ItemStack.isSameItemSameComponents(renderCopy, heldCopy);
    }

    private static void clearAnimationMatchState(ItemStack stack) {
        clearReloadVisualState(stack);
        stack.remove(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_SCOPE_ATTACHMENT_DAMAGE.get());
        stack.remove(ModDataComponents.GUN_BARREL_ATTACHMENT_DAMAGE.get());
        stack.remove(ModDataComponents.GUN_STOCK_ATTACHMENT_DAMAGE.get());
        stack.remove(ModDataComponents.GUN_UNDER_BARREL_ATTACHMENT_DAMAGE.get());
    }

    public static void triggerClientShoot(Entity entity, boolean aiming) {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return;
            }
            Object player = minecraftClass.getField("player").get(minecraft);
            if (player != entity) {
                return;
            }

            ItemStack mainHand = (ItemStack) player.getClass().getMethod("getMainHandItem").invoke(player);
            ItemStack offHand = (ItemStack) player.getClass().getMethod("getOffhandItem").invoke(player);
            ItemStack stack = mainHand.getItem() instanceof AnimatedGunItem ? mainHand : offHand;
            if (!(stack.getItem() instanceof AnimatedGunItem)) {
                clearPendingClientShoot();
                return;
            }

            clearReloadVisualState(stack);
            clearClientDrawAnimationState(stack);
            clientShootStack = stack;
            clientShootAiming = aiming;
            clientShootTriggerDeadlineNanos = System.nanoTime() + CLIENT_SHOOT_TRIGGER_WINDOW_NANOS;
        } catch (Throwable ignored) {
            clearPendingClientShoot();
        }
    }

    public static void triggerClientMelee(Entity entity) {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return;
            }
            Object player = minecraftClass.getField("player").get(minecraft);
            if (player != entity) {
                return;
            }

            ItemStack stack = mainHandItem(player);
            if (!(stack.getItem() instanceof AnimatedGunItem)) {
                clearPendingClientMelee();
                return;
            }

            clearReloadVisualState(stack);
            clearClientDrawAnimationState(stack);
            clientMeleeStack = stack.copy();
            clientMeleeBayonet = hasBayonet(stack);
            clientMeleeTriggerDeadlineNanos = System.nanoTime() + CLIENT_SHOOT_TRIGGER_WINDOW_NANOS;
        } catch (Throwable ignored) {
            clearPendingClientMelee();
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

    public void triggerMelee(Level level, Entity triggerEntity, ItemStack stack) {
        trigger(level, triggerEntity, stack, hasBayonet(stack) ? ANIM_BAYONET : ANIM_MELEE);
    }

    private static boolean hasBayonet(ItemStack stack) {
        return GunAttachments.stack(stack, AttachmentType.BARREL)
                .filter(AnimatedGunItem::isBayonetStack)
                .isPresent();
    }

    private static boolean isBayonetStack(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS) || stack.getItem() instanceof SwordItem) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().endsWith("_sword");
    }

    private void soundListener(SoundKeyframeEvent<AnimatedGunItem> event) {
        Player player = clientPlayer();
        if (player == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        GunItem gun = stack.getItem() instanceof GunItem mainHandGun ? mainHandGun : null;
        if (gun == null) {
            stack = player.getOffhandItem();
            gun = stack.getItem() instanceof GunItem offHandGun ? offHandGun : null;
        }
        if (gun == null) {
            return;
        }

        SoundEvent sound = soundForKeyframe(event.getKeyframeData().getSound(), gun.getStats());
        if (sound != null) {
            player.playSound(sound, 1.0F, 1.0F);
        }
    }

    private static Player clientPlayer() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            return minecraft == null ? null : (Player) minecraftClass.getField("player").get(minecraft);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ItemStack mainHandItem(Object player) {
        return player instanceof Player playerEntity ? playerEntity.getMainHandItem() : ItemStack.EMPTY;
    }

    private static ItemStack offHandItem(Object player) {
        return player instanceof Player playerEntity ? playerEntity.getOffhandItem() : ItemStack.EMPTY;
    }

    private static SoundEvent soundForKeyframe(String key, GunStats stats) {
        return switch (key) {
            case "rustle" -> resolveSound(GUN_RUSTLE_SOUND);
            case "screw" -> resolveSound(GUN_SCREW_SOUND);
            case "reload_mag_out" -> stats.reloadStartSoundEvent().orElse(null);
            case "reload_mag_in" -> stats.reloadLoadSoundEvent().orElse(null);
            case "reload_end" -> stats.reloadEndSoundEvent().orElse(null);
            case "ejector_pull" -> resolveSound(stats.ejectorPullSound());
            case "ejector_release" -> resolveSound(stats.ejectorReleaseSound());
            case "jammed" -> SoundEvents.ANVIL_LAND;
            default -> null;
        };
    }

    private static SoundEvent resolveSound(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        var holder = ModSounds.ALL.get(id);
        if (holder != null) {
            return holder.get();
        }
        return BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null);
    }
}
