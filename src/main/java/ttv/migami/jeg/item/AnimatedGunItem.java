package ttv.migami.jeg.item;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
import software.bernie.geckolib.animation.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.client.render.gun.GunPoseProfile;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModSounds;
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
    private static final long CLIENT_DRAW_VISUAL_NANOS = 1_700_000_000L;
    private static final ResourceLocation GUN_RUSTLE_SOUND = Reference.id("item.gun_rustle");
    private static final ResourceLocation GUN_SCREW_SOUND = Reference.id("item.gun_screw");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static volatile boolean loggedRendererProvider;
    private static ItemStack clientShootStack = ItemStack.EMPTY;
    private static boolean clientShootAiming;
    private static long clientShootTriggerDeadlineNanos;
    private static ItemStack clientDrawStack = ItemStack.EMPTY;
    private static long clientDrawAnimationDeadlineNanos;
    private static ItemStack clientDrawResetStack = ItemStack.EMPTY;
    private static long clientDrawResetDeadlineNanos;

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
        ).setSoundKeyframeHandler(this::soundListener)
                .receiveTriggeredAnimations()
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
        if (isNonFirstPersonPerspective(state)) {
            return PlayState.STOP;
        }
        if (clearInterruptedReloadAnimation(state.getController(), stack)) {
            RawAnimation drawAnimation = drawAnimationFor(stack);
            resetDrawAnimationIfRequested(state.getController(), stack);
            return state.setAndContinue(drawAnimation != null ? drawAnimation : IDLE);
        }
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

        if (drawAnimation != null) {
            return state.setAndContinue(drawAnimation);
        }

        RawAnimation reloadAnimation = reloadAnimationFor(stack);
        if (reloadAnimation != null) {
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

    private static boolean isReloadAnimation(RawAnimation animation) {
        return hasAnyAnimation(animation, ANIM_RELOAD, ANIM_RELOAD_START, ANIM_RELOAD_LOOP, ANIM_RELOAD_STOP);
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
        if (System.nanoTime() <= clientDrawAnimationDeadlineNanos && matchesHeldStack(stack, clientDrawStack)) {
            return true;
        }
        clearRecentDrawAnimation();
        return false;
    }

    private static void rememberDrawAnimation(ItemStack stack) {
        clientDrawStack = stack.copy();
        clientDrawAnimationDeadlineNanos = System.nanoTime() + CLIENT_DRAW_VISUAL_NANOS;
        requestDrawAnimationReset(stack);
    }

    static void clearRecentDrawAnimation() {
        clientDrawStack = ItemStack.EMPTY;
        clientDrawAnimationDeadlineNanos = 0L;
        clearDrawAnimationReset();
    }

    static void restartDrawAnimation(ItemStack stack) {
        clearRecentDrawAnimation();
        rememberDrawAnimation(stack);
    }

    static void restartDrawAnimationAfterReloadCancel(ItemStack stack) {
        clearReloadVisualState(stack);
        restartDrawAnimation(stack);
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
        return System.nanoTime() <= clientDrawAnimationDeadlineNanos && matchesHeldStack(stack, clientDrawStack);
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
        controller.forceAnimationReset();
        controller.stop();
        clearDrawAnimationReset();
        return true;
    }

    private static void clearDrawAnimationReset() {
        clientDrawResetStack = ItemStack.EMPTY;
        clientDrawResetDeadlineNanos = 0L;
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

    private static boolean isNonFirstPersonPerspective(AnimationState<AnimatedGunItem> state) {
        var perspective = state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        return perspective != null && !perspective.firstPerson();
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

    private void soundListener(SoundKeyframeEvent<AnimatedGunItem> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        Player player = minecraft.player;
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
