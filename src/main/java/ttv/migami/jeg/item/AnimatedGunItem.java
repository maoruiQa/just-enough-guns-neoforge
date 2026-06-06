package ttv.migami.jeg.item;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
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
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
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
    public static final String ANIM_RELOAD_ALT = "reload_alt";
    public static final String ANIM_AIM_SHOOT = "aim_shoot";
    public static final String ANIM_SPRINT = "sprint";
    public static final String ANIM_DRAW = "draw";
    public static final String ANIM_MELEE = "melee";
    public static final String ANIM_BAYONET = "bayonet";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().then(ANIM_SHOOT, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation AIM_SHOOT = RawAnimation.begin().then(ANIM_AIM_SHOOT, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation DRAW = RawAnimation.begin().then(ANIM_DRAW, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD = RawAnimation.begin().then(ANIM_RELOAD, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_ALT = RawAnimation.begin().then(ANIM_RELOAD_ALT, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_START = RawAnimation.begin().then(ANIM_RELOAD_START, LoopType.PLAY_ONCE).thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_STOP = RawAnimation.begin().then(ANIM_RELOAD_STOP, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation SPRINT = RawAnimation.begin().then(ANIM_SPRINT, LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation MELEE = RawAnimation.begin().then(ANIM_MELEE, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation BAYONET = RawAnimation.begin().then(ANIM_BAYONET, LoopType.PLAY_ONCE).thenLoop("idle");
    private static final long CLIENT_SHOOT_TRIGGER_WINDOW_NANOS = 250_000_000L;
    private static final long CLIENT_SPRINT_AFTER_SHOOT_SUPPRESSION_NANOS = 250_000_000L;
    private static final long CLIENT_DRAW_VISUAL_NANOS = 1_700_000_000L;

    private static final int RELOAD_STAGE_NONE = 0;
    private static final int RELOAD_STAGE_START = 1;
    private static final int RELOAD_STAGE_LOOP = 2;
    private static final int RELOAD_STAGE_STOP = 3;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
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
    private static ItemStack clientSprintSuppressedDrawStack = ItemStack.EMPTY;
    private static long clientSprintSuppressedDrawDeadlineNanos;
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
                .triggerableAnim(ANIM_DRAW, DRAW)
                .triggerableAnim(ANIM_RELOAD, RELOAD)
                .triggerableAnim(ANIM_RELOAD_ALT, RELOAD_ALT)
                .triggerableAnim(ANIM_RELOAD_START, RELOAD_START)
                .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
                .triggerableAnim(ANIM_RELOAD_STOP, RELOAD_STOP)
                .triggerableAnim(ANIM_SPRINT, SPRINT)
                .triggerableAnim(ANIM_MELEE, MELEE)
                .triggerableAnim(ANIM_BAYONET, BAYONET));
    }

    private PlayState animationPredicate(AnimationTest<AnimatedGunItem> test) {
        ItemStack renderStack = rendererItemStack(test);
        ItemStack stack = animationStateStack(test, renderStack);
        clearStaleRenderReloadVisualState(renderStack, stack);

        if (suppressDrawForSprint(test.controller(), stack)) {
            return setSprintOrBayonetSprintAnimation(test, stack);
        }
        RawAnimation pendingDrawAnimation = pendingClientDrawAnimationFor(test, stack);
        if (pendingDrawAnimation != null) {
            return test.setAndContinue(pendingDrawAnimation);
        }

        if (triggerPendingClientMelee(test, stack)) {
            return PlayState.CONTINUE;
        }
        if (shouldContinueMeleeAnimation(test.controller())) {
            return PlayState.CONTINUE;
        }

        if (triggerPendingClientShoot(test, stack)) {
            return PlayState.CONTINUE;
        }

        clearInterruptedReloadAnimation(test.controller(), stack);

        if (shouldContinueReloadAnimation(test.controller(), stack)) {
            return PlayState.CONTINUE;
        }

        RawAnimation drawAnimation = drawAnimationFor(stack);
        if (drawAnimation != null && resetDrawAnimationIfRequested(test.controller(), stack)) {
            return test.setAndContinue(drawAnimation);
        }
        if (shouldContinueDrawAnimation(test.controller(), stack)) {
            return PlayState.CONTINUE;
        }
        drawAnimation = drawAnimationFor(stack);
        if (drawAnimation != null) {
            return test.setAndContinue(drawAnimation);
        }

        RawAnimation reloadAnimation = reloadAnimationFor(stack);
        if (reloadAnimation != null) {
            return test.setAndContinue(reloadAnimation);
        }

        if (finishTriggeredShootAnimation(test.controller())) {
            return test.setAndContinue(IDLE);
        }

        if (test.controller().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        if (isFirstPersonRender(test, stack)) {
            var player = com.geckolib.util.ClientUtil.getClientPlayer();
            if (player != null && player.isSprinting() && !isClientAiming() && !isLocalAttackDown(player)
                    && !isClientSprintAnimationBlocked() && canApplySprintingAnimation(stack)) {
                return setSprintOrBayonetSprintAnimation(test, stack);
            }
        }

        return test.setAndContinue(IDLE);
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
        if (isSprintingFirstPerson(stack)) {
            clearRecentDrawAnimation();
            return null;
        }
        if (hasSprintSuppressedDraw(stack)) {
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

    private static PlayState setSprintAnimation(AnimationTest<AnimatedGunItem> test) {
        if (hasAnimation(test.controller().getCurrentRawAnimation(), ANIM_SPRINT)) {
            return PlayState.CONTINUE;
        }
        return test.setAndContinue(SPRINT);
    }

    private static PlayState setSprintOrBayonetSprintAnimation(AnimationTest<AnimatedGunItem> test, ItemStack stack) {
        if (!hasBayonet(stack)) {
            return setSprintAnimation(test);
        }
        if (hasAnimation(test.controller().getCurrentRawAnimation(), ANIM_BAYONET)) {
            return PlayState.CONTINUE;
        }
        test.controller().reset();
        return test.setAndContinue(BAYONET);
    }

    private static ItemStack animationStateStack(AnimationTest<AnimatedGunItem> test, ItemStack renderStack) {
        ItemStack liveStack = matchingLiveHeldStack(test, renderStack);
        if (!liveStack.isEmpty()) {
            return liveStack;
        }
        if (renderStack != null && !renderStack.isEmpty()) {
            return renderStack;
        }
        return resolveRenderStack(test);
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

    private static ItemStack matchingLiveHeldStack(AnimationTest<AnimatedGunItem> test, ItemStack renderStack) {
        Object minecraft = minecraftInstance();
        Object player = clientPlayer(minecraft);
        if (minecraft == null || player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack perspectiveStack = heldStackForPerspective(player, test.getData(DataTickets.ITEM_RENDER_PERSPECTIVE));
        if (matchesHeldStack(renderStack, perspectiveStack) || isStaleRenderCopyOf(renderStack, perspectiveStack)) {
            return perspectiveStack;
        }

        ItemStack mainHand = clientMainHand(player);
        if (matchesHeldStack(renderStack, mainHand) || isStaleRenderCopyOf(renderStack, mainHand)) {
            return mainHand;
        }

        ItemStack offHand = clientOffHand(player);
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

    private static boolean shouldContinueReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        RawAnimation current = controller.getCurrentRawAnimation();
        if (!isReloadAnimation(current) || stack == null || stack.isEmpty()) {
            return false;
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            controller.reset();
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

    private static void clearInterruptedReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!isReloadAnimation(controller.getCurrentRawAnimation())) {
            return;
        }
        if (stack != null && !stack.isEmpty()
                && stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0) {
            return;
        }
        controller.reset();
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

        test.controller().reset();
        String animation = clientShootAiming ? ANIM_AIM_SHOOT : ANIM_SHOOT;
        if (!test.controller().triggerAnimation(animation)) {
            clearPendingClientShoot();
            return false;
        }
        clearPendingClientShoot();
        return true;
    }

    private static boolean triggerPendingClientMelee(AnimationTest<AnimatedGunItem> test, ItemStack renderStack) {
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

        test.controller().reset();
        if (!test.controller().triggerAnimation(clientMeleeBayonet ? ANIM_BAYONET : ANIM_MELEE)) {
            clearPendingClientMelee();
            return false;
        }
        clearPendingClientMelee();
        return true;
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

    public static void suppressSprintAnimationBriefly() {
        clientSprintAnimationBlockedUntilNanos = System.nanoTime() + CLIENT_SPRINT_AFTER_SHOOT_SUPPRESSION_NANOS;
    }

    static void restartDrawAnimation(ItemStack stack) {
        clearRecentDrawAnimation();
        rememberDrawAnimation(stack);
    }

    static void restartDrawAnimationAfterReloadCancel(ItemStack stack) {
        clearReloadVisualState(stack);
        clearRecentDrawAnimation();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        clientDrawStack = stack.copy();
        clientDrawAnimationDeadlineNanos = System.nanoTime() + CLIENT_DRAW_VISUAL_NANOS;
        requestDrawAnimationReset(stack);
        queuePendingClientDraw(stack);
    }

    private static void clearReloadVisualState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_RELOAD_STAGE.get());
    }

    private static void rememberDrawAnimation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            clearRecentDrawAnimation();
            return;
        }
        clientDrawStack = stack.copy();
        clientDrawAnimationDeadlineNanos = System.nanoTime() + CLIENT_DRAW_VISUAL_NANOS;
        requestDrawAnimationReset(stack);
        queuePendingClientDraw(stack);
    }

    static void clearRecentDrawAnimation() {
        clientDrawStack = ItemStack.EMPTY;
        clientDrawAnimationDeadlineNanos = 0L;
        clearDrawAnimationReset();
        clearPendingClientDraw();
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

    private static void rememberSprintSuppressedDraw(ItemStack stack) {
        clientSprintSuppressedDrawStack = stack.copy();
        clientSprintSuppressedDrawDeadlineNanos = System.nanoTime() + CLIENT_DRAW_VISUAL_NANOS;
    }

    private static boolean hasSprintSuppressedDraw(ItemStack stack) {
        if (clientSprintSuppressedDrawStack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() > clientSprintSuppressedDrawDeadlineNanos) {
            clearSprintSuppressedDraw();
            return false;
        }
        return matchesHeldStack(stack, clientSprintSuppressedDrawStack);
    }

    private static void clearSprintSuppressedDraw() {
        clientSprintSuppressedDrawStack = ItemStack.EMPTY;
        clientSprintSuppressedDrawDeadlineNanos = 0L;
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
        if (System.nanoTime() > clientDrawResetDeadlineNanos || !matchesHeldStack(stack, clientDrawResetStack)) {
            clearDrawAnimationReset();
            return false;
        }
        controller.reset();
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

    private static RawAnimation pendingClientDrawAnimationFor(AnimationTest<AnimatedGunItem> test, ItemStack renderStack) {
        if (clientDrawTriggerStack.isEmpty()) {
            return null;
        }
        if (isSprintingFirstPerson(renderStack) || hasSprintSuppressedDraw(renderStack)) {
            clearPendingClientDraw();
            clearDrawAnimationReset();
            return null;
        }
        if (System.nanoTime() > clientDrawTriggerDeadlineNanos) {
            clearPendingClientDraw();
            return null;
        }
        if (!matchesHeldStack(renderStack, clientDrawTriggerStack)) {
            return null;
        }
        if (renderStack != null && !renderStack.isEmpty()
                && renderStack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0) {
            return null;
        }
        test.controller().reset();
        clearDrawAnimationReset();
        clearPendingClientDraw();
        return DRAW;
    }

    private static void clearPendingClientDraw() {
        clientDrawTriggerStack = ItemStack.EMPTY;
        clientDrawTriggerDeadlineNanos = 0L;
    }

    private static boolean shouldContinueDrawAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!hasAnimation(controller.getCurrentRawAnimation(), ANIM_DRAW) || stack == null || stack.isEmpty()) {
            return false;
        }
        if (suppressDrawForSprint(controller, stack)) {
            return false;
        }
        boolean drawActive = stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0) > 0
                || hasRecentDrawAnimation(stack);
        if (drawActive && !controller.hasAnimationFinished() && !isClientAiming()) {
            return true;
        }
        clearRecentDrawAnimation();
        return false;
    }

    private static boolean suppressDrawForSprint(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!isSprintingFirstPerson(stack)) {
            return false;
        }
        boolean hadDrawState = !clientDrawTriggerStack.isEmpty()
                || !clientDrawStack.isEmpty()
                || !clientDrawResetStack.isEmpty()
                || stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0) > 0
                || hasAnimation(controller.getCurrentRawAnimation(), ANIM_DRAW);
        if (!hadDrawState) {
            return false;
        }
        rememberSprintSuppressedDraw(stack);
        clearRecentDrawAnimation();
        stack.remove(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get());
        controller.reset();
        return true;
    }

    private static boolean isClientSprintAnimationBlocked() {
        return System.nanoTime() < clientSprintAnimationBlockedUntilNanos;
    }

    private static boolean shouldDiscardReleasedAutomaticShot(ItemStack renderStack) {
        if (!(renderStack.getItem() instanceof GunItem gun) || !gun.isAutomatic()) {
            return false;
        }

        Object minecraft = minecraftInstance();
        Object player = clientPlayer(minecraft);
        if (minecraft == null || player == null || keyAttackDown(minecraft)) {
            return false;
        }

        return matchesHeldStack(renderStack, clientMainHand(player))
                || matchesHeldStack(renderStack, clientOffHand(player));
    }

    private static boolean isReloadAnimation(RawAnimation animation) {
        return hasAnyAnimation(animation, ANIM_RELOAD, ANIM_RELOAD_START, ANIM_RELOAD_LOOP, ANIM_RELOAD_STOP);
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

    private static boolean shouldContinueMeleeAnimation(AnimationController<AnimatedGunItem> controller) {
        return isMeleeAnimation(controller.getCurrentRawAnimation()) && !controller.hasAnimationFinished();
    }

    private static boolean isFirstPersonRender(AnimationTest<AnimatedGunItem> test, ItemStack stack) {
        var perspective = test.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (perspective != null && perspective.firstPerson()) {
            return true;
        }

        Object minecraft = minecraftInstance();
        Object player = clientPlayer(minecraft);
        if (minecraft == null || player == null || !isFirstPersonCamera(minecraft)) {
            return false;
        }

        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return matchesHeldStack(stack, clientMainHand(player))
                || matchesHeldStack(stack, clientOffHand(player));
    }

    private static boolean isSprintingFirstPerson(ItemStack stack) {
        Object minecraft = minecraftInstance();
        Object player = clientPlayer(minecraft);
        if (minecraft == null || player == null || !isFirstPersonCamera(minecraft)) {
            return false;
        }
        if (!matchesHeldStack(stack, clientMainHand(player)) && !matchesHeldStack(stack, clientOffHand(player))) {
            return false;
        }
        try {
            Method isSprintingMethod = player.getClass().getMethod("isSprinting");
            return Boolean.TRUE.equals(isSprintingMethod.invoke(player)) && !isClientAiming() && canApplySprintingAnimation(stack);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static ItemStack resolveRenderStack(AnimationTest<AnimatedGunItem> test) {
        ItemStack stack = rendererItemStack(test);
        if (!stack.isEmpty()) {
            return stack;
        }

        Object minecraft = minecraftInstance();
        Object player = clientPlayer(minecraft);
        if (minecraft == null || player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = clientMainHand(player);
        if (matchesAnimatedGun(test.animatable(), mainHand)) {
            return mainHand;
        }

        ItemStack offHand = clientOffHand(player);
        if (matchesAnimatedGun(test.animatable(), offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack rendererItemStack(AnimationTest<AnimatedGunItem> test) {
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
        Object minecraft = minecraftInstance();
        return minecraft != null && clientPlayer(minecraft) == entity && keyAttackDown(minecraft);
    }

    public static void triggerClientShoot(Entity entity, boolean aiming) {
        Object minecraft = minecraftInstance();
        Object player = clientPlayer(minecraft);
        if (!(entity instanceof Player) || minecraft == null || player != entity) {
            return;
        }

        ItemStack mainHand = clientMainHand(player);
        ItemStack offHand = clientOffHand(player);
        ItemStack stack = mainHand.getItem() instanceof AnimatedGunItem ? mainHand : offHand;
        if (!(stack.getItem() instanceof AnimatedGunItem)) {
            clearPendingClientShoot();
            return;
        }

        suppressSprintAnimationBriefly();
        clearReloadVisualState(stack);
        clearRecentDrawAnimation();
        clientShootStack = stack;
        clientShootAiming = aiming;
        clientShootTriggerDeadlineNanos = System.nanoTime() + CLIENT_SHOOT_TRIGGER_WINDOW_NANOS;
    }

    public static void triggerClientMelee(Entity entity) {
        Object minecraft = minecraftInstance();
        Object player = clientPlayer(minecraft);
        if (!(entity instanceof Player) || minecraft == null || player != entity) {
            return;
        }

        ItemStack stack = clientMainHand(player);
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

    private static Object minecraftInstance() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            return minecraftClass.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object clientPlayer(Object minecraft) {
        if (minecraft == null) {
            return null;
        }
        try {
            return minecraft.getClass().getField("player").get(minecraft);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static ItemStack clientMainHand(Object player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        try {
            Object stack = player.getClass().getMethod("getMainHandItem").invoke(player);
            return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack clientOffHand(Object player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        try {
            Object stack = player.getClass().getMethod("getOffhandItem").invoke(player);
            return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean isFirstPersonCamera(Object minecraft) {
        try {
            Object options = minecraft.getClass().getField("options").get(minecraft);
            Object cameraType = options.getClass().getMethod("getCameraType").invoke(options);
            Object firstPerson = cameraType.getClass().getMethod("isFirstPerson").invoke(cameraType);
            return firstPerson instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean keyAttackDown(Object minecraft) {
        try {
            Object options = minecraft.getClass().getField("options").get(minecraft);
            Object keyAttack = options.getClass().getField("keyAttack").get(options);
            Object down = keyAttack.getClass().getMethod("isDown").invoke(keyAttack);
            return down instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
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

    public void triggerMelee(Level level, Entity triggerEntity, ItemStack stack) {
        trigger(level, triggerEntity, stack, hasBayonet(stack) ? ANIM_BAYONET : ANIM_MELEE);
    }

    private static boolean hasBayonet(ItemStack stack) {
        return GunAttachments.stack(stack, AttachmentType.BARREL)
                .filter(attachment -> attachment.is(ItemTags.SWORDS))
                .isPresent();
    }
}
