package ttv.migami.jeg.item;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
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

public class AnimatedGunItem extends GunItem implements GeoItem {
    public static final String CONTROLLER = "controller";
    public static final String ANIM_SHOOT = "shoot";
    public static final String ANIM_RELOAD = "reload";
    public static final String ANIM_RELOAD_START = "reload_start";
    public static final String ANIM_RELOAD_LOOP = "reload_loop";
    public static final String ANIM_RELOAD_STOP = "reload_stop";
    public static final String ANIM_AIM_SHOOT = "aim_shoot";
    public static final String ANIM_SPRINT = "sprint";
    public static final String ANIM_DRAW = "draw";
    public static final String ANIM_MELEE = "melee";
    public static final String ANIM_BAYONET = "bayonet";
    public static final String ANIM_INSPECT = "inspect";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().then(ANIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation AIM_SHOOT = RawAnimation.begin().then(ANIM_AIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DRAW = RawAnimation.begin().then(ANIM_DRAW, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD = RawAnimation.begin().then(ANIM_RELOAD, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_START = RawAnimation.begin().then(ANIM_RELOAD_START, Animation.LoopType.PLAY_ONCE).thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_STOP = RawAnimation.begin().then(ANIM_RELOAD_STOP, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation SPRINT = RawAnimation.begin().then(ANIM_SPRINT, Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation MELEE = RawAnimation.begin().then(ANIM_MELEE, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation BAYONET = RawAnimation.begin().then(ANIM_BAYONET, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation DRAW_TRIGGER = RawAnimation.begin().then(ANIM_DRAW, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RELOAD_TRIGGER = RawAnimation.begin().then(ANIM_RELOAD, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RELOAD_STOP_TRIGGER = RawAnimation.begin().then(ANIM_RELOAD_STOP, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation MELEE_TRIGGER = RawAnimation.begin().then(ANIM_MELEE, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation BAYONET_TRIGGER = RawAnimation.begin().then(ANIM_BAYONET, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation INSPECT_TRIGGER = RawAnimation.begin().then(ANIM_INSPECT, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final long CLIENT_SHOOT_TRIGGER_WINDOW_NANOS = 250_000_000L;
    private static final long CLIENT_DRAW_VISUAL_NANOS = 3_000_000_000L;
    private static final long CLIENT_TICK_NANOS = 50_000_000L;
    private static final ResourceLocation GUN_RUSTLE_SOUND = Reference.id("item.gun_rustle");
    private static final ResourceLocation GUN_SCREW_SOUND = Reference.id("item.gun_screw");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static volatile boolean loggedRendererProvider;
    private static ItemStack clientShootStack = ItemStack.EMPTY;
    private static boolean clientShootAiming;
    private static long clientShootTriggerDeadlineNanos;
    private static ItemStack clientMeleeStack = ItemStack.EMPTY;
    private static boolean clientMeleeBayonet;
    private static long clientMeleeTriggerDeadlineNanos;
    private static ItemStack clientInspectStack = ItemStack.EMPTY;
    private static long clientInspectTriggerDeadlineNanos;
    private static ItemStack clientDrawTriggerStack = ItemStack.EMPTY;
    private static long clientDrawTriggerDeadlineNanos;
    private static ItemStack clientDrawStack = ItemStack.EMPTY;
    private static long clientDrawAnimationDeadlineNanos;
    private static ItemStack clientDrawLockStack = ItemStack.EMPTY;
    private static long clientDrawLockDeadlineNanos;
    private static ItemStack clientDrawResetStack = ItemStack.EMPTY;
    private static long clientDrawResetDeadlineNanos;
    private static String lastFirstPersonGunId = "";

    public AnimatedGunItem(Properties properties, GunStats stats) {
        super(properties, stats);
        GeoItem.registerSyncedAnimatable(this);
    }

    private static final class GunAnimationController extends AnimationController<AnimatedGunItem> {
        private GunAnimationController(AnimatedGunItem animatable, String name, int transitionLength,
                                       AnimationStateHandler<AnimatedGunItem> stateHandler) {
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
                .triggerableAnim(ANIM_DRAW, DRAW_TRIGGER)
                .triggerableAnim(ANIM_MELEE, MELEE_TRIGGER)
                .triggerableAnim(ANIM_BAYONET, BAYONET_TRIGGER)
                .triggerableAnim(ANIM_RELOAD, RELOAD_TRIGGER)
                .triggerableAnim(ANIM_RELOAD_START, RELOAD_START)
                .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
                .triggerableAnim(ANIM_RELOAD_STOP, RELOAD_STOP_TRIGGER)
                .triggerableAnim(ANIM_SPRINT, SPRINT)
                .triggerableAnim(ANIM_INSPECT, INSPECT_TRIGGER));
    }

    private PlayState animationPredicate(AnimationState<AnimatedGunItem> state) {
        ItemStack renderStack = state.getData(DataTickets.ITEMSTACK);
        if (isNonFirstPersonPerspective(state)) {
            return PlayState.STOP;
        }
        ItemStack stack = animationStateStack(state, renderStack);
        clearStaleRenderReloadVisualState(renderStack, stack);
        resetControllerOnGunChange(state.getController(), stack);

        if (triggerPendingClientShoot(state, stack)) {
            return PlayState.CONTINUE;
        }

        if (prioritizeReloadOverDraw(state.getController(), stack)) {
            return PlayState.CONTINUE;
        }
        stopStaleTriggeredReloadAnimation(state.getController(), stack);

        RawAnimation drawAnimation = drawAnimationFor(stack);
        if (drawAnimation != null && resetDrawAnimationIfRequested(state.getController(), stack)) {
            return state.setAndContinue(drawAnimation);
        }
        if (shouldContinueDrawAnimation(state.getController(), stack)) {
            return PlayState.CONTINUE;
        }
        if (triggerPendingClientDraw(state, stack)) {
            return PlayState.CONTINUE;
        }
        drawAnimation = drawAnimationFor(stack);

        if (drawAnimation != null) {
            return state.setAndContinue(drawAnimation);
        }

        if (clearInterruptedReloadAnimation(state.getController(), stack)) {
            resetDrawAnimationIfRequested(state.getController(), stack);
            return state.setAndContinue(IDLE);
        }
        if (shouldContinueReloadAnimation(state.getController(), stack)) {
            return PlayState.CONTINUE;
        }

        RawAnimation reloadAnimation = reloadAnimationFor(stack);
        if (reloadAnimation != null) {
            return state.setAndContinue(reloadAnimation);
        }

        if (shouldBayonetSprintInterruptInspect(state, stack)) {
            clearPendingClientInspect();
            state.getController().forceAnimationReset();
            state.getController().stop();
            return setSprintOrBayonetSprintAnimation(state, stack);
        }

        if (triggerPendingClientInspect(state, stack)) {
            return PlayState.CONTINUE;
        }
        if (shouldContinueInspectAnimation(state.getController())) {
            return PlayState.CONTINUE;
        }

        if (triggerPendingClientMelee(state, stack)) {
            return PlayState.CONTINUE;
        }
        if (shouldContinueMeleeAnimation(state.getController())) {
            return PlayState.CONTINUE;
        }

        stopFinishedTriggeredAnimation(state.getController(), stack);
        if (state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        if (isFirstPersonRender(state, stack)) {
            Player player = clientPlayer();
            if (player != null && player.isSprinting() && !isClientAiming() && canApplySprintingAnimation(stack)) {
                return setSprintOrBayonetSprintAnimation(state, stack);
            }
        }
        if (isIdleOnlyAnimation(state.getController().getCurrentRawAnimation())) {
            return PlayState.CONTINUE;
        }
        return state.setAndContinue(IDLE);
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

    private static RawAnimation reloadAnimationFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            return null;
        }

        int stage = stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE);
        RawAnimation animation = switch (stage) {
            case RELOAD_STAGE_START -> RELOAD_START;
            case RELOAD_STAGE_LOOP -> RELOAD_LOOP;
            case RELOAD_STAGE_STOP -> RELOAD_STOP;
            default -> RELOAD;
        };
        return animation;
    }

    private static boolean prioritizeReloadOverDraw(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        boolean reloadTriggered = controller.isPlayingTriggeredAnimation()
                && isReloadAnimation(controller.getTriggeredAnimation());
        boolean reloadActive = reloadAnimationFor(stack) != null;
        boolean drawActive = hasAnimation(controller.getCurrentRawAnimation(), ANIM_DRAW)
                || (stack != null && !stack.isEmpty()
                && (stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0) > 0
                || hasRecentDrawAnimation(stack)));
        if ((!reloadTriggered && !reloadActive) || !drawActive) {
            return false;
        }

        clearClientDrawAnimationState(stack);
        if (reloadTriggered) {
            return true;
        }
        if (controller instanceof GunAnimationController gunController) {
            gunController.stopTriggeredAnimationIfActive();
        }
        controller.forceAnimationReset();
        controller.stop();
        return false;
    }

    private static ItemStack animationStateStack(AnimationState<AnimatedGunItem> state, ItemStack renderStack) {
        ItemStack liveStack = matchingLiveHeldStack(state, renderStack);
        return liveStack.isEmpty() ? renderStack : liveStack;
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
        Object minecraft = minecraftInstance();
        Object player = minecraftPlayer(minecraft);
        if (minecraft == null || player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack perspectiveStack = heldStackForPerspective(player, state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE));
        if (matchesHeldStack(renderStack, perspectiveStack)) {
            return perspectiveStack;
        }
        if (isStaleRenderCopyOf(renderStack, perspectiveStack)) {
            return perspectiveStack;
        }

        ItemStack mainHand = mainHandItem(player);
        if (matchesHeldStack(renderStack, mainHand)) {
            return mainHand;
        }
        if (isStaleRenderCopyOf(renderStack, mainHand)) {
            return mainHand;
        }

        ItemStack offHand = offHandItem(player);
        if (matchesHeldStack(renderStack, offHand)) {
            return offHand;
        }
        if (isStaleRenderCopyOf(renderStack, offHand)) {
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

    private static void stopStaleTriggeredReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!(controller instanceof GunAnimationController gunController) || !controller.isPlayingTriggeredAnimation()) {
            return;
        }
        if (!isReloadAnimation(controller.getTriggeredAnimation())) {
            return;
        }
        if (stack != null && !stack.isEmpty()
                && stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0) {
            return;
        }

        gunController.stopTriggeredAnimationIfActive();
        controller.forceAnimationReset();
        controller.stop();
    }

    private static void stopFinishedTriggeredAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!(controller instanceof GunAnimationController gunController)
                || !controller.isPlayingTriggeredAnimation()
                || !controller.hasAnimationFinished()) {
            return;
        }

        gunController.stopTriggeredAnimationIfActive();
        controller.forceAnimationReset();
        controller.stop();
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
        return false;
    }

    private static boolean isReloadAnimation(RawAnimation animation) {
        return hasAnyAnimation(animation, ANIM_RELOAD, ANIM_RELOAD_START, ANIM_RELOAD_LOOP, ANIM_RELOAD_STOP);
    }

    private static boolean isMeleeAnimation(RawAnimation animation) {
        return hasAnyAnimation(animation, ANIM_MELEE, ANIM_BAYONET);
    }

    private static boolean isIdleOnlyAnimation(RawAnimation animation) {
        return animation != null
                && animation.getAnimationStages().size() == 1
                && "idle".equals(animation.getAnimationStages().getFirst().animationName());
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
        if (drawActive && !controller.hasAnimationFinished()) {
            return true;
        }
        clearRecentDrawAnimation();
        return false;
    }

    private static PlayState setSprintAnimation(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        if (hasAnimation(state.getController().getCurrentRawAnimation(), ANIM_SPRINT)) {
            return PlayState.CONTINUE;
        }

        return state.setAndContinue(SPRINT);
    }

    private static PlayState setSprintOrBayonetSprintAnimation(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        if (hasBayonet(stack)) {
            return state.setAndContinue(IDLE);
        }
        return setSprintAnimation(state, stack);
    }

    private static boolean shouldContinueMeleeAnimation(AnimationController<AnimatedGunItem> controller) {
        return isMeleeAnimation(controller.getCurrentRawAnimation()) && !controller.hasAnimationFinished();
    }

    private static boolean shouldContinueInspectAnimation(AnimationController<AnimatedGunItem> controller) {
        return hasAnimation(controller.getCurrentRawAnimation(), ANIM_INSPECT) && !controller.hasAnimationFinished();
    }

    private static boolean shouldBayonetSprintInterruptInspect(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        if (!hasBayonet(stack) || !shouldContinueInspectAnimation(state.getController()) || !isFirstPersonRender(state, stack)) {
            return false;
        }
        Player player = clientPlayer();
        return player != null && player.isSprinting() && !isClientAiming();
    }

    private static void rememberDrawAnimation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            clearRecentDrawAnimation();
            clearClientDrawOperationLock();
            return;
        }
        long nowNanos = System.nanoTime();
        clientDrawStack = stack.copy();
        clientDrawAnimationDeadlineNanos = nowNanos + CLIENT_DRAW_VISUAL_NANOS;
        rememberClientDrawOperationLock(stack, nowNanos);
        requestDrawAnimationReset(stack);
        queuePendingClientDraw(stack);
    }

    static void clearRecentDrawAnimation() {
        clientDrawStack = ItemStack.EMPTY;
        clientDrawAnimationDeadlineNanos = 0L;
    }

    private static void rememberClientDrawOperationLock(ItemStack stack, long nowNanos) {
        clientDrawLockStack = stack.copy();
        clientDrawLockDeadlineNanos = nowNanos + (long) GunItem.getDrawOperationLockTicks(stack) * CLIENT_TICK_NANOS;
    }

    private static void clearClientDrawOperationLock() {
        clientDrawLockStack = ItemStack.EMPTY;
        clientDrawLockDeadlineNanos = 0L;
    }

    public static boolean isClientDrawOperationLocked(ItemStack stack) {
        if (GunItem.isDrawOperationLocked(stack)) {
            return true;
        }
        if (clientDrawLockStack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() >= clientDrawLockDeadlineNanos) {
            clearClientDrawOperationLock();
            return false;
        }
        return matchesHeldStack(stack, clientDrawLockStack);
    }

    private static void clearClientDrawAnimationState() {
        clearRecentDrawAnimation();
        clearClientDrawOperationLock();
        clearDrawAnimationReset();
        clearPendingClientDraw();
    }

    private static void clearClientDrawAnimationState(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            stack.remove(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get());
        }
        clearClientDrawAnimationState();
    }

    static void restartDrawAnimation(ItemStack stack) {
        clearRecentDrawAnimation();
        resetClientAnimationInstance(stack, "restartDrawAnimation");
        rememberDrawAnimation(stack);
    }

    static void restartDrawAnimationAfterReloadCancel(ItemStack stack) {
        clearReloadVisualState(stack);
        clearRecentDrawAnimation();
        clearClientDrawOperationLock();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        resetClientAnimationInstance(stack, "restartDrawAnimationAfterReloadCancel");
        long nowNanos = System.nanoTime();
        clientDrawStack = stack.copy();
        clientDrawAnimationDeadlineNanos = nowNanos + CLIENT_DRAW_VISUAL_NANOS;
        rememberClientDrawOperationLock(stack, nowNanos);
        requestDrawAnimationReset(stack);
        queuePendingClientDraw(stack);
    }

    private static void resetClientAnimationInstance(ItemStack stack, String reason) {
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
        state.setAndContinue(DRAW);
        return true;
    }

    private static void clearPendingClientDraw() {
        clientDrawTriggerStack = ItemStack.EMPTY;
        clientDrawTriggerDeadlineNanos = 0L;
    }

    private static boolean triggerPendingClientShoot(AnimationState<AnimatedGunItem> state, ItemStack renderStack) {
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

        state.getController().forceAnimationReset();
        boolean triggered = state.getController().tryTriggerAnimation(clientShootAiming ? ANIM_AIM_SHOOT : ANIM_SHOOT);
        if (triggered) {
            clearPendingClientShoot();
        }
        return triggered;
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

    private static boolean triggerPendingClientInspect(AnimationState<AnimatedGunItem> state, ItemStack renderStack) {
        if (clientInspectStack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() > clientInspectTriggerDeadlineNanos) {
            clearPendingClientInspect();
            return false;
        }
        if (!matchesHeldStack(renderStack, clientInspectStack)) {
            return false;
        }

        state.getController().forceAnimationReset();
        boolean triggered = state.getController().tryTriggerAnimation(ANIM_INSPECT);
        if (triggered) {
            clearPendingClientInspect();
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

    private static void clearPendingClientInspect() {
        clientInspectStack = ItemStack.EMPTY;
        clientInspectTriggerDeadlineNanos = 0L;
    }

    private static boolean isNonFirstPersonPerspective(AnimationState<AnimatedGunItem> state) {
        var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        return perspective != null && !perspective.firstPerson();
    }

    private static boolean isFirstPersonRender(AnimationState<AnimatedGunItem> state, ItemStack stack) {
        var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (perspective != null && perspective.firstPerson()) {
            return true;
        }

        Object minecraft = minecraftInstance();
        Object player = minecraftPlayer(minecraft);
        if (minecraft == null || player == null || !isFirstPersonCamera(minecraft)) {
            return false;
        }
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemStack mainHand = mainHandItem(player);
        ItemStack offHand = offHandItem(player);
        return ItemStack.isSameItemSameComponents(stack, mainHand)
                || ItemStack.isSameItemSameComponents(stack, offHand)
                || ItemStack.isSameItem(stack, mainHand)
                || ItemStack.isSameItem(stack, offHand);
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
        stack.remove(ModDataComponents.GUN_AMMO.get());
        stack.remove(ModDataComponents.GUN_HEAT.get());
    }

    public static void triggerClientShoot(Entity entity, boolean aiming) {
        Object minecraft = minecraftInstance();
        Object player = minecraftPlayer(minecraft);
        if (!(entity instanceof Player) || minecraft == null || player != entity) {
            return;
        }

        ItemStack mainHand = mainHandItem(player);
        ItemStack offHand = offHandItem(player);
        ItemStack stack = mainHand.getItem() instanceof AnimatedGunItem ? mainHand : offHand;
        if (!(stack.getItem() instanceof AnimatedGunItem)) {
            clearPendingClientShoot();
            return;
        }

        clearReloadVisualState(stack);
        stack.remove(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get());
        clearClientDrawAnimationState();
        clientShootStack = stack;
        clientShootAiming = aiming;
        clientShootTriggerDeadlineNanos = System.nanoTime() + CLIENT_SHOOT_TRIGGER_WINDOW_NANOS;
    }

    public static void triggerClientMelee(Entity entity) {
        Object minecraft = minecraftInstance();
        Object player = minecraftPlayer(minecraft);
        if (!(entity instanceof Player) || minecraft == null || player != entity) {
            return;
        }

        ItemStack stack = mainHandItem(player);
        if (!(stack.getItem() instanceof AnimatedGunItem)) {
            clearPendingClientMelee();
            return;
        }

        clearReloadVisualState(stack);
        clearRecentDrawAnimation();
        clientMeleeStack = stack.copy();
        clientMeleeBayonet = hasBayonet(stack);
        clientMeleeTriggerDeadlineNanos = System.nanoTime() + CLIENT_SHOOT_TRIGGER_WINDOW_NANOS;
    }

    public static void triggerClientInspect(Entity entity) {
        Object minecraft = minecraftInstance();
        Object player = minecraftPlayer(minecraft);
        if (!(entity instanceof Player) || minecraft == null || player != entity) {
            return;
        }

        ItemStack stack = mainHandItem(player);
        if (!(stack.getItem() instanceof AnimatedGunItem)) {
            clearPendingClientInspect();
            return;
        }

        clientInspectStack = stack.copy();
        clientInspectTriggerDeadlineNanos = System.nanoTime() + CLIENT_SHOOT_TRIGGER_WINDOW_NANOS;
    }

    private static boolean hasBayonet(ItemStack stack) {
        return GunAttachments.stack(stack, AttachmentType.BARREL)
                .map(ItemStack::getItem)
                .filter(SwordItem.class::isInstance)
                .isPresent();
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
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = createClientGunRenderer();
                }
                if (!loggedRendererProvider) {
                    loggedRendererProvider = true;
                    // JustEnoughGuns.LOGGER.info("[JEG_RENDER_DEBUG] GeoRenderProvider supplied AnimatedGunRenderer");
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

    public void triggerMelee(Level level, Entity triggerEntity, ItemStack stack) {
        trigger(level, triggerEntity, stack, hasBayonet(stack) ? ANIM_BAYONET : ANIM_MELEE);
    }

    public void triggerInspect(Level level, Entity triggerEntity, ItemStack stack) {
        trigger(level, triggerEntity, stack, ANIM_INSPECT);
    }

    private static Object minecraftInstance() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            return minecraftClass.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Object minecraftPlayer(Object minecraft) {
        if (minecraft == null) {
            return null;
        }
        try {
            return minecraft.getClass().getField("player").get(minecraft);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Player clientPlayer() {
        Object player = minecraftPlayer(minecraftInstance());
        return player instanceof Player playerEntity ? playerEntity : null;
    }

    private static ItemStack mainHandItem(Object player) {
        return player instanceof Player playerEntity ? playerEntity.getMainHandItem() : ItemStack.EMPTY;
    }

    private static ItemStack offHandItem(Object player) {
        return player instanceof Player playerEntity ? playerEntity.getOffhandItem() : ItemStack.EMPTY;
    }

    private static boolean isFirstPersonCamera(Object minecraft) {
        try {
            Object options = minecraft.getClass().getField("options").get(minecraft);
            Object cameraType = options.getClass().getMethod("getCameraType").invoke(options);
            return (Boolean) cameraType.getClass().getMethod("isFirstPerson").invoke(cameraType);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean isClientAiming() {
        try {
            Class<?> aimingHandlerClass = Class.forName("ttv.migami.jeg.client.handler.AimingHandler");
            Object handler = aimingHandlerClass.getMethod("get").invoke(null);
            return (Boolean) aimingHandlerClass.getMethod("isAiming").invoke(handler);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean canApplySprintingAnimation(ItemStack stack) {
        if (!(stack.getItem() instanceof AnimatedGunItem gun)) {
            return false;
        }
        try {
            Class<?> profileClass = Class.forName("ttv.migami.jeg.client.render.gun.GunPoseProfile");
            Object profile = profileClass.getMethod("forGun", ResourceLocation.class).invoke(null, gun.getStats().id());
            return (Boolean) profileClass.getMethod("canApplySprintingAnimation").invoke(profile);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static BlockEntityWithoutLevelRenderer createClientGunRenderer() {
        try {
            Class<?> rendererClass = Class.forName("ttv.migami.jeg.client.render.gun.AnimatedGunRenderer");
            return (BlockEntityWithoutLevelRenderer) rendererClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create animated gun renderer", e);
        }
    }
}
