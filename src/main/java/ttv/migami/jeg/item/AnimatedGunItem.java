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
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.client.render.gun.GunPoseProfile;
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
    public static final String ANIM_DRAW = "draw";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().then(ANIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation AIM_SHOOT = RawAnimation.begin().then(ANIM_AIM_SHOOT, Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DRAW = RawAnimation.begin().then(ANIM_DRAW, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD = RawAnimation.begin().then(ANIM_RELOAD, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation RELOAD_START = RawAnimation.begin().then(ANIM_RELOAD_START, Animation.LoopType.PLAY_ONCE).thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_LOOP = RawAnimation.begin().thenLoop(ANIM_RELOAD_LOOP);
    private static final RawAnimation RELOAD_STOP = RawAnimation.begin().then(ANIM_RELOAD_STOP, Animation.LoopType.PLAY_ONCE).thenLoop("idle");
    private static final RawAnimation SPRINT = RawAnimation.begin().then(ANIM_SPRINT, Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final long CLIENT_SHOOT_TRIGGER_WINDOW_NANOS = 250_000_000L;
    private static final long CLIENT_RELOAD_COMPONENT_GRACE_NANOS = 250_000_000L;
    private static final long CLIENT_DRAW_VISUAL_NANOS = 1_700_000_000L;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static volatile boolean loggedRendererProvider;
    private static volatile long nextClientReloadDebugNanos;
    private static ItemStack clientShootStack = ItemStack.EMPTY;
    private static boolean clientShootAiming;
    private static long clientShootTriggerDeadlineNanos;
    private static ItemStack clientReloadStack = ItemStack.EMPTY;
    private static RawAnimation clientReloadAnimation;
    private static long clientReloadAnimationDeadlineNanos;
    private static ItemStack clientDrawStack = ItemStack.EMPTY;
    private static long clientDrawAnimationDeadlineNanos;

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
        ).receiveTriggeredAnimations()
                .triggerableAnim(ANIM_SHOOT, SHOOT)
                .triggerableAnim(ANIM_AIM_SHOOT, AIM_SHOOT)
                .triggerableAnim(ANIM_DRAW, DRAW)
                .triggerableAnim(ANIM_RELOAD, RELOAD)
                .triggerableAnim(ANIM_RELOAD_START, RELOAD_START)
                .triggerableAnim(ANIM_RELOAD_LOOP, RELOAD_LOOP)
                .triggerableAnim(ANIM_RELOAD_STOP, RELOAD_STOP)
                .triggerableAnim(ANIM_SPRINT, SPRINT));
    }

    private PlayState animationPredicate(AnimationState<AnimatedGunItem> state) {
        ItemStack stack = state.getData(DataTickets.ITEMSTACK);
        if (shouldContinueReloadAnimation(state.getController(), stack)) {
            return PlayState.CONTINUE;
        }
        if (shouldContinueDrawAnimation(state.getController(), stack)) {
            return PlayState.CONTINUE;
        }

        RawAnimation drawAnimation = drawAnimationFor(stack);
        if (drawAnimation != null) {
            return state.setAndContinue(drawAnimation);
        }

        RawAnimation reloadAnimation = reloadAnimationFor(stack);
        if (reloadAnimation != null) {
            debugClientReloadPredicate(stack);
            return state.setAndContinue(reloadAnimation);
        }

        if (triggerPendingClientShoot(state, stack)) {
            return PlayState.CONTINUE;
        }

        if (state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        if (isFirstPersonRender(state, stack)) {
            var player = software.bernie.geckolib.util.ClientUtil.getClientPlayer();
            if (player != null && player.isSprinting() && !AimingHandler.get().isAiming()) {
                if (stack.getItem() instanceof AnimatedGunItem gun) {
                    var profile = GunPoseProfile.forGun(gun.getStats().id());
                    if (profile.canApplySprintingAnimation()) {
                        if (hasAnimation(state.getController().getCurrentRawAnimation(), ANIM_SPRINT)) {
                            return PlayState.CONTINUE;
                        }
                        return state.setAndContinue(SPRINT);
                    }
                }
            }
        }

        if (hasAnimation(state.getController().getCurrentRawAnimation(), "idle")) {
            return PlayState.CONTINUE;
        }
        return state.setAndContinue(IDLE);
    }

    private static RawAnimation drawAnimationFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0) <= 0) {
            return null;
        }
        if (!matchesHeldStack(stack, clientDrawStack) || System.nanoTime() > clientDrawAnimationDeadlineNanos) {
            rememberDrawAnimation(stack);
        }
        return DRAW;
    }

    private static RawAnimation reloadAnimationFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            return recentReloadAnimationFor(stack);
        }

        int stage = stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE);
        RawAnimation animation = switch (stage) {
            case RELOAD_STAGE_START -> RELOAD_START;
            case RELOAD_STAGE_LOOP -> RELOAD_LOOP;
            case RELOAD_STAGE_STOP -> RELOAD_STOP;
            default -> RELOAD;
        };
        rememberReloadAnimation(stack, animation);
        return animation;
    }

    private static boolean shouldContinueReloadAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        RawAnimation current = controller.getCurrentRawAnimation();
        if (!isReloadAnimation(current) || stack == null || stack.isEmpty()) {
            return false;
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            return recentReloadAnimationFor(stack) != null;
        }

        int stage = stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE);
        return switch (stage) {
            case RELOAD_STAGE_START -> hasAnimation(current, ANIM_RELOAD_START);
            case RELOAD_STAGE_LOOP -> hasAnimation(current, ANIM_RELOAD_LOOP);
            case RELOAD_STAGE_STOP -> hasAnimation(current, ANIM_RELOAD_STOP);
            default -> hasAnimation(current, ANIM_RELOAD);
        };
    }

    private static boolean isReloadAnimation(RawAnimation animation) {
        return hasAnyAnimation(animation, ANIM_RELOAD, ANIM_RELOAD_START, ANIM_RELOAD_LOOP, ANIM_RELOAD_STOP);
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

    private static void rememberReloadAnimation(ItemStack stack, RawAnimation animation) {
        clientReloadStack = stack.copy();
        clientReloadAnimation = animation;
        clientReloadAnimationDeadlineNanos = System.nanoTime() + CLIENT_RELOAD_COMPONENT_GRACE_NANOS;
    }

    private static RawAnimation recentReloadAnimationFor(ItemStack stack) {
        if (clientReloadAnimation == null || System.nanoTime() > clientReloadAnimationDeadlineNanos) {
            clearRecentReloadAnimation();
            return null;
        }
        if (!matchesHeldStack(stack, clientReloadStack)) {
            return null;
        }
        return clientReloadAnimation;
    }

    private static void clearRecentReloadAnimation() {
        clientReloadStack = ItemStack.EMPTY;
        clientReloadAnimation = null;
        clientReloadAnimationDeadlineNanos = 0L;
    }

    private static boolean shouldContinueDrawAnimation(AnimationController<AnimatedGunItem> controller, ItemStack stack) {
        if (!hasAnimation(controller.getCurrentRawAnimation(), ANIM_DRAW) || stack == null || stack.isEmpty()) {
            return false;
        }
        if (System.nanoTime() <= clientDrawAnimationDeadlineNanos && matchesHeldStack(stack, clientDrawStack)) {
            return true;
        }
        clearRecentDrawAnimation();
        return false;
    }

    private static void rememberDrawAnimation(ItemStack stack) {
        clientDrawStack = stack.copy();
        clientDrawAnimationDeadlineNanos = System.nanoTime() + CLIENT_DRAW_VISUAL_NANOS;
    }

    private static void clearRecentDrawAnimation() {
        clientDrawStack = ItemStack.EMPTY;
        clientDrawAnimationDeadlineNanos = 0L;
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

    private static void clearPendingClientShoot() {
        clientShootStack = ItemStack.EMPTY;
        clientShootAiming = false;
        clientShootTriggerDeadlineNanos = 0L;
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

    private static boolean matchesHeldStack(ItemStack renderStack, ItemStack heldStack) {
        if (renderStack == heldStack) {
            return true;
        }
        if (renderStack == null || renderStack.isEmpty() || heldStack == null || heldStack.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(renderStack, heldStack)
                || ItemStack.isSameItem(renderStack, heldStack);
    }

    public static void triggerClientShoot(Entity entity, boolean aiming) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player != entity) {
            return;
        }

        ItemStack mainHand = minecraft.player.getMainHandItem();
        ItemStack offHand = minecraft.player.getOffhandItem();
        ItemStack stack = mainHand.getItem() instanceof AnimatedGunItem ? mainHand : offHand;
        if (!(stack.getItem() instanceof AnimatedGunItem)) {
            clearPendingClientShoot();
            return;
        }

        clientShootStack = stack;
        clientShootAiming = aiming;
        clientShootTriggerDeadlineNanos = System.nanoTime() + CLIENT_SHOOT_TRIGGER_WINDOW_NANOS;
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
            private AnimatedGunRenderer renderer;

            @Override
            public AnimatedGunRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new AnimatedGunRenderer();
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
        JustEnoughGuns.LOGGER.info(
                "[JEG_RELOAD_DEBUG] trigger animation={} id={} item={} entity={} remaining={} stage={}",
                animation,
                id,
                stack.getItem(),
                triggerEntity.getType().getDescriptionId(),
                stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0),
                stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE)
        );
        triggerAnim(triggerEntity, id, CONTROLLER, animation);
    }

    private static void debugClientReloadPredicate(ItemStack stack) {
        long now = System.nanoTime();
        if (now < nextClientReloadDebugNanos) {
            return;
        }
        nextClientReloadDebugNanos = now + 1_000_000_000L;
        JustEnoughGuns.LOGGER.info(
                "[JEG_RELOAD_DEBUG] client predicate item={} remaining={} total={} stage={}",
                stack.getItem(),
                stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0),
                stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get(), 0),
                stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE)
        );
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
