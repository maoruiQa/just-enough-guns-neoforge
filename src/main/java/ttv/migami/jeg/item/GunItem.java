package ttv.migami.jeg.item;

import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.Reference;
import net.minecraft.ChatFormatting;
import ttv.migami.jeg.network.NetworkHandler;

public class GunItem extends Item {
    private static final ResourceLocation GRENADE_LAUNCHER_ID = Reference.id("grenade_launcher");
    private static final float GRENADE_BASE_POWER = 4.0F;
    private static final float GRENADE_DAMAGE_FACTOR = 5.0F;
    private static final int GRENADE_FUSE_TICKS = 600;
    private static final Set<String> AUTOMATIC_IDS = Set.of(
            "abstract_gun",
            "assault_rifle",
            "blossom_rifle",
            "burst_rifle",
            "combat_rifle",
            "custom_smg",
            "flamethrower",
            "phantom_smg",
            "hollenfire_mk2",
            "infantry_rifle",
            "light_machine_gun",
            "minigun",
            "service_rifle",
            "soulhunter_mk2",
            "subsonic_rifle"
    );

    // Guns whose GeckoLib animation files are authored as reload_start/reload_loop/reload_stop.
    // Others use a single "reload".
    private static final Set<String> SEGMENTED_RELOAD_ANIM_IDS = Set.of(
            "bolt_action_rifle",
            "double_barrel_shotgun",
            "flare_gun",
            "grenade_launcher",
            "holy_shotgun",
            "pump_shotgun",
            "python",
            "repeating_shotgun",
            "revolver",
            "rocket_launcher",
            "supersonic_shotgun",
            "typhoonee",
            "waterpipe_shotgun"
    );

    private static final int RELOAD_STAGE_NONE = 0;
    private static final int RELOAD_STAGE_START = 1;
    private static final int RELOAD_STAGE_LOOP = 2;
    private static final int RELOAD_STAGE_STOP = 3;
    private static final int SPREAD_THRESHOLD_MS = 300;
    private static final int SPREAD_MAX_COUNT = 10;
    private static final int WATER_COOL_DURATION_TICKS = 60;
    private static final ResourceLocation WATER_COOL_SOUND_ID = Reference.id("item.cooldown_with_water");
    private static final Map<UUID, SpreadTrackerState> SPREAD_TRACKERS = new WeakHashMap<>();
    private static final Map<UUID, Integer> MINIGUN_PENDING_HEAT_SHOTS = new HashMap<>();
    private static final Map<Integer, Integer> MINIGUN_PENDING_HEAT_NUMERATOR = new HashMap<>();
    private static final Map<Integer, Integer> OVERHEAT_PENDING_NUMERATOR = new HashMap<>();
    private static final Map<Integer, Integer> OVERHEAT_COOL_NUMERATOR = new HashMap<>();
    private static final int MINIGUN_HEAT_BATCH_SHOTS = 3;
    private static final Set<String> SHOTGUN_IDS = Set.of(
            "double_barrel_shotgun",
            "holy_shotgun",
            "pump_shotgun",
            "repeating_shotgun",
            "supersonic_shotgun",
            "waterpipe_shotgun"
    );
    private static final Set<String> NON_BULLET_TRAIL_IDS = Set.of(
            "flamethrower",
            "flare_gun",
            "rocket_launcher",
            "grenade_launcher",
            "hypersonic_cannon",
            "typhoonee"
    );
    private static final Set<String> HEAVY_BACKSTEP_IDS = Set.of(
            "light_machine_gun",
            "minigun",
            "rocket_launcher",
            "typhoonee"
    );
    private static final Set<String> ASSAULT_RIFLE_IDS = Set.of(
            "assault_rifle",
            "burst_rifle",
            "combat_rifle",
            "service_rifle",
            "blossom_rifle",
            "infantry_rifle",
            "subsonic_rifle",
            "hollenfire_mk2",
            "soulhunter_mk2"
    );
    private static final float MINIGUN_SPREAD_FLOOR = 0.85F;
    private static final double MOVEMENT_THRESHOLD_SQR = 0.0036D;
    private static final int OVERHEAT_MAX = 200;
    private static final int OVERHEAT_TRACKED_MAX = 280;
    private static final int OVERHEAT_RECOVERY_BUFFER = 80;
    private static final int OVERHEAT_HEAT_NUMERATOR_LMG = 5;
    private static final int OVERHEAT_HEAT_NUMERATOR_MINIGUN = 8;
    private static final int OVERHEAT_HEAT_DENOMINATOR = 6;
    private static final int OVERHEAT_COOL_NUMERATOR_HELD = 2;
    private static final int OVERHEAT_COOL_NUMERATOR_IDLE = 4;
    private static final int OVERHEAT_COOL_DENOMINATOR = 5;

    private final GunStats stats;

    public GunItem(Properties properties, GunStats stats) {
        super(properties);
        this.stats = stats;
    }

    public boolean isEnchantable(ItemStack stack) {
        return stack.getMaxDamage() > 0;
    }

    public int getEnchantmentValue() {
        return 10; // Same as iron tools
    }

    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.IRON_INGOT);
    }

    public GunStats getStats() {
        return this.stats;
    }

    public int magazineSize() {
        return this.stats.magazineSize();
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        if (stats.usesMagazine()) {
            stack.set(ModDataComponents.GUN_AMMO.get(), 0); // 弹匣默认为空
        }
        return stack;
    }

    private void ensureAmmoInitialized(ItemStack stack) {
        if (stats.usesMagazine() && !stack.has(ModDataComponents.GUN_AMMO.get())) {
            stack.set(ModDataComponents.GUN_AMMO.get(), 0); // 弹匣默认为空
        }
    }

    private int getAmmo(ItemStack stack) {
        if (!stats.usesMagazine()) {
            return 0;
        }
        ensureAmmoInitialized(stack);
        return stack.getOrDefault(ModDataComponents.GUN_AMMO.get(), stats.magazineSize());
    }

    public int getMagazineAmmo(ItemStack stack) {
        return stats.usesMagazine() ? getAmmo(stack) : 0;
    }

    public boolean isAutomatic() {
        return isAutomatic(this.stats);
    }

    public static boolean isAutomatic(GunStats stats) {
        return AUTOMATIC_IDS.contains(stats.id().getPath());
    }

    public static boolean isBulletClassWeapon(ResourceLocation gunId) {
        return !NON_BULLET_TRAIL_IDS.contains(gunId.getPath());
    }

    private int shotsPerTrigger() {
        return 1;
    }

    /**
     * Check if this gun fires slow bullets (tracked projectiles) vs fast bullets (instant raycast).
     * Slow bullets: flamethrower, flare gun, rocket launcher, hypersonic cannon, typhoonee
     * Fast bullets: all other guns
     */
    private static boolean isSlowBullet(ResourceLocation gunId) {
        String path = gunId.getPath();
        return path.equals("flamethrower") ||
               path.equals("flare_gun") ||
               path.equals("rocket_launcher") ||
               path.equals("hypersonic_cannon") ||
               path.equals("typhoonee");
    }

    public static boolean isTriggerLocked(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUN_TRIGGER_LOCK.get(), Boolean.FALSE);
    }

    private static void setTriggerLocked(ItemStack stack, boolean locked) {
        if (locked) {
            stack.set(ModDataComponents.GUN_TRIGGER_LOCK.get(), Boolean.TRUE);
        } else {
            stack.remove(ModDataComponents.GUN_TRIGGER_LOCK.get());
        }
    }

    public static void clearTriggerLock(ItemStack stack) {
        setTriggerLocked(stack, false);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) {
            return true;
        }
        return !ItemStack.isSameItem(oldStack, newStack);
    }

    public int countInventoryAmmo(Player player) {
        if (player.getAbilities().instabuild) {
            return Integer.MAX_VALUE;
        }

        Optional<Item> ammoItem = getAmmoItem();
        if (ammoItem.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        Item ammo = ammoItem.get();
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.is(ammo)) {
                total += slot.getCount();
            }
        }
        return total;
    }

    private void setAmmo(ItemStack stack, int value) {
        if (stats.usesMagazine()) {
            stack.set(ModDataComponents.GUN_AMMO.get(), Mth.clamp(value, 0, stats.magazineSize()));
        }
    }

    public boolean usesOverheatMechanic() {
        return isOverheatWeapon(stats.id());
    }

    public int getOverheatPercent(ItemStack stack) {
        if (!usesOverheatMechanic()) {
            return 0;
        }
        return Math.round((Math.min(getTrackedHeat(stack), OVERHEAT_MAX) * 100.0F) / OVERHEAT_MAX);
    }

    public boolean shouldShowWaterCoolingPrompt(ItemStack stack) {
        return usesOverheatMechanic() && isOverheated(stack);
    }

    public static boolean canWaterCool(ItemStack stack) {
        return stack.getItem() instanceof GunItem gun
                && gun.usesOverheatMechanic()
                && getTrackedHeat(stack) > 0;
    }

    public static boolean isCoolingWithWater(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get(), 0) > 0;
    }

    public static int getWaterCoolingProgressPercent(ItemStack stack) {
        int total = stack.getOrDefault(ModDataComponents.GUN_WATER_COOLING_TICKS_TOTAL.get(), 0);
        int remaining = stack.getOrDefault(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get(), 0);
        if (total <= 0 || remaining <= 0) {
            return 0;
        }
        return Mth.clamp(Math.round(((total - remaining) * 100.0F) / total), 0, 100);
    }

    public static boolean tryStartWaterCooling(Level level, Player player, InteractionHand hand) {
        ItemStack coolant = player.getItemInHand(hand);
        ItemStack gunStack = hand == InteractionHand.OFF_HAND ? player.getMainHandItem() : player.getOffhandItem();
        if (!isCoolant(coolant)) {
            return false;
        }
        if (!canWaterCool(gunStack) || isCoolingWithWater(gunStack)) {
            return false;
        }
        startWaterCooling(gunStack);
        return true;
    }

    public static int getWaterCoolingUseDuration(ItemStack coolant, LivingEntity entity) {
        if (isCoolant(coolant)) {
            return WATER_COOL_DURATION_TICKS;
        }
        return 0;
    }

    public static void onWaterCoolingUseTick(Level level, LivingEntity livingEntity, ItemStack coolant, int remainingUseTicks) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        ItemStack gunStack = coolant == player.getOffhandItem() ? player.getMainHandItem() : player.getOffhandItem();
        if (!isActivelyCoolingWithWater(player, coolant)) {
            clearWaterCooling(gunStack);
            player.stopUsingItem();
        }
    }

    public static ItemStack finishWaterCooling(ItemStack coolant, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) {
            return coolant;
        }
        ItemStack currentOffhand = player.getOffhandItem();
        ItemStack currentMainhand = player.getMainHandItem();
        ItemStack gunStack = coolant == currentOffhand ? currentMainhand : currentOffhand;
        if (!isCoolant(coolant) || !canWaterCool(gunStack)) {
            return coolant;
        }
        applyWaterCooling(gunStack, coolant);
        clearWaterCooling(gunStack);
        playWaterCoolingSound(level, player);
        player.awardStat(Stats.ITEM_USED.get(ModItems.COOLANT.get()));
        if (!player.getAbilities().instabuild && coolant == player.getOffhandItem()) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE));
            return player.getOffhandItem();
        }
        return coolant;
    }

    public static boolean releaseWaterCooling(ItemStack coolant, Level level, LivingEntity livingEntity, int timeLeft) {
        if (livingEntity instanceof Player player) {
            int usedTicks = getWaterCoolingUseDuration(coolant, livingEntity) - timeLeft;
            if (usedTicks >= getWaterCoolingUseDuration(coolant, livingEntity)) {
                return false;
            }
            ItemStack gunStack = coolant == player.getOffhandItem() ? player.getMainHandItem() : player.getOffhandItem();
            clearWaterCooling(gunStack);
        }
        return false;
    }

    public static void cancelWaterCoolingIfInvalid(Player player) {
        ItemStack mainhand = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();
        if (isCoolingWithWater(mainhand) && !canWaterCool(mainhand)) {
            clearWaterCooling(mainhand);
        }
        if (isCoolingWithWater(offhand) && !canWaterCool(offhand)) {
            clearWaterCooling(offhand);
        }
    }

    private static boolean isActivelyCoolingWithWater(Player player, ItemStack coolant) {
        ItemStack gunStack = coolant == player.getOffhandItem() ? player.getMainHandItem() : player.getOffhandItem();
        return isCoolant(coolant)
                && ItemStack.isSameItemSameComponents(player.getUseItem(), coolant)
                && canWaterCool(gunStack)
                && isCoolingWithWater(gunStack);
    }

    private static boolean isCoolant(ItemStack stack) {
        return stack.is(ModItems.COOLANT.get()) || stack.is(ModItems.ENHANCED_COOLANT.get());
    }

    private static void startWaterCooling(ItemStack stack) {
        stack.set(ModDataComponents.GUN_WATER_COOLING_TICKS_TOTAL.get(), WATER_COOL_DURATION_TICKS);
        stack.set(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get(), WATER_COOL_DURATION_TICKS);
    }

    private static void clearWaterCooling(ItemStack stack) {
        stack.remove(ModDataComponents.GUN_WATER_COOLING_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get());
    }

    private static int getWaterCoolingAmount(ItemStack gunStack, ItemStack coolantStack) {
        if (!(gunStack.getItem() instanceof GunItem gun)) {
            return 0;
        }
        boolean enhanced = coolantStack.is(ModItems.ENHANCED_COOLANT.get());
        return switch (gun.getStats().id().getPath()) {
            case "minigun" -> Math.round(OVERHEAT_MAX * (enhanced ? 0.60F : 0.30F));
            case "light_machine_gun" -> Math.round(OVERHEAT_MAX * (enhanced ? 0.90F : 0.50F));
            default -> 0;
        };
    }

    private static void applyWaterCooling(ItemStack gunStack, ItemStack coolantStack) {
        if (!canWaterCool(gunStack)) {
            return;
        }
        setTrackedHeat(gunStack, getTrackedHeat(gunStack) - getWaterCoolingAmount(gunStack, coolantStack));
    }

    private static void playWaterCoolingSound(Level level, Player player) {
        SoundEvent sound = ModSounds.ALL.getOrDefault(WATER_COOL_SOUND_ID, null) != null
                ? ModSounds.ALL.get(WATER_COOL_SOUND_ID).get()
                : SoundEvents.FIRE_EXTINGUISH;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean isOverheatWeapon(ResourceLocation gunId) {
        String path = gunId.getPath();
        return "light_machine_gun".equals(path) || "minigun".equals(path);
    }

    private static int getHeatNumeratorPerShot(ResourceLocation gunId) {
        return "minigun".equals(gunId.getPath()) ? OVERHEAT_HEAT_NUMERATOR_MINIGUN : OVERHEAT_HEAT_NUMERATOR_LMG;
    }

    private static int getTrackedHeat(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.GUN_HEAT.get(), 0), 0, OVERHEAT_TRACKED_MAX);
    }

    private static void setTrackedHeat(ItemStack stack, int heat) {
        int clamped = Mth.clamp(heat, 0, OVERHEAT_TRACKED_MAX);
        int before = stack.getOrDefault(ModDataComponents.GUN_HEAT.get(), 0);
        stack.set(ModDataComponents.GUN_HEAT.get(), clamped);
        JustEnoughGuns.LOGGER.info("[debug/heat-set] item={} beforeRaw={} requested={} stored={}", stack.getItem(), before, heat, clamped);
    }

    private static boolean isOverheated(ItemStack stack) {
        return getTrackedHeat(stack) >= OVERHEAT_MAX;
    }

    private static int applyHeatDelta(int current, int delta) {
        int next = Mth.clamp(current + delta, 0, OVERHEAT_TRACKED_MAX);
        if (current < OVERHEAT_MAX && next >= OVERHEAT_MAX) {
            next = Math.max(next, OVERHEAT_MAX + OVERHEAT_RECOVERY_BUFFER);
        }
        return next;
    }

    private static int applyFractionalHeat(ItemStack stack, int numeratorDelta, int denominator) {
        int trackedHeat = getTrackedHeat(stack);
        int stackKey = System.identityHashCode(stack);
        int pendingBefore = OVERHEAT_PENDING_NUMERATOR.getOrDefault(stackKey, 0);
        int scaledNumerator = pendingBefore + numeratorDelta;
        int wholeDelta = scaledNumerator / denominator;
        int pendingAfter = scaledNumerator % denominator;
        if (pendingAfter > 0) {
            OVERHEAT_PENDING_NUMERATOR.put(stackKey, pendingAfter);
        } else {
            OVERHEAT_PENDING_NUMERATOR.remove(stackKey);
        }
        int next = applyHeatDelta(trackedHeat, wholeDelta);
        JustEnoughGuns.LOGGER.info("[debug/overheat] frac item={} trackedHeat={} numeratorDelta={} denominator={} pendingBefore={} wholeDelta={} pendingAfter={} next={}",
                stack.getItem(),
                trackedHeat,
                numeratorDelta,
                denominator,
                pendingBefore,
                wholeDelta,
                pendingAfter,
                next);
        return next;
    }

    private static void addOverheatForShots(ItemStack stack, ResourceLocation gunId, int shotsFired, @Nullable Player shooter) {
        if (shotsFired <= 0 || !isOverheatWeapon(gunId)) {
            return;
        }
        int effectiveShots = shotsFired;
        if ("minigun".equals(gunId.getPath()) && shooter != null) {
            int pending = MINIGUN_PENDING_HEAT_SHOTS.getOrDefault(shooter.getUUID(), 0) + shotsFired;
            int applyShots = pending - (pending % MINIGUN_HEAT_BATCH_SHOTS);
            int remainder = pending % MINIGUN_HEAT_BATCH_SHOTS;
            if (remainder > 0) {
                MINIGUN_PENDING_HEAT_SHOTS.put(shooter.getUUID(), remainder);
            } else {
                MINIGUN_PENDING_HEAT_SHOTS.remove(shooter.getUUID());
            }
            if (applyShots <= 0) {
                return;
            }
            effectiveShots = applyShots;
        }
        int next = applyFractionalHeat(stack, getHeatNumeratorPerShot(gunId) * effectiveShots, OVERHEAT_HEAT_DENOMINATOR);
        setTrackedHeat(stack, next);
    }

    private static void coolOverheat(ItemStack stack, boolean heldInHand) {
        int current = getTrackedHeat(stack);
        if (current <= 0) {
            return;
        }
        int numerator = heldInHand ? OVERHEAT_COOL_NUMERATOR_HELD : OVERHEAT_COOL_NUMERATOR_IDLE;
        int stackKey = ItemStack.hashItemAndComponents(stack);
        int scaledNumerator = OVERHEAT_COOL_NUMERATOR.getOrDefault(stackKey, 0) + numerator;
        int cooling = scaledNumerator / OVERHEAT_COOL_DENOMINATOR;
        int remainder = scaledNumerator % OVERHEAT_COOL_DENOMINATOR;
        if (remainder > 0) {
            OVERHEAT_COOL_NUMERATOR.put(stackKey, remainder);
        } else {
            OVERHEAT_COOL_NUMERATOR.remove(stackKey);
        }
        if (cooling > 0) {
            setTrackedHeat(stack, current - cooling);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (tryStartWaterCooling(level, player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        if (isCoolant(stack)) {
            return InteractionResultHolder.consume(stack);
        }
        return tryShoot(level, player, hand) ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isCoolant(stack) ? UseAnim.NONE : super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        int duration = getWaterCoolingUseDuration(stack, entity);
        return duration > 0 ? duration : super.getUseDuration(stack, entity);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseTicks) {
        onWaterCoolingUseTick(level, livingEntity, stack, remainingUseTicks);
        super.onUseTick(level, livingEntity, stack, remainingUseTicks);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (isCoolant(stack)) {
            int usedTicks = getUseDuration(stack, livingEntity) - timeLeft;
            if (usedTicks >= getWaterCoolingUseDuration(stack, livingEntity)) {
                return;
            }
        }
        releaseWaterCooling(stack, level, livingEntity, timeLeft);
        super.releaseUsing(stack, level, livingEntity, timeLeft);
    }

    public boolean tryShoot(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ensureAmmoInitialized(stack);
        boolean automatic = isAutomatic();

        if (!automatic && isTriggerLocked(stack)) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }

        if (usesOverheatMechanic() && isOverheated(stack)) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.literal("Gun overheated"), true);
            }
            return false;
        }

        if (!hasAmmoAvailable(player, stack)) {
            if (level.isClientSide()) {
                playDryFireSound(level, player);
                Component message = stats.usesMagazine() && !stats.isInventoryFed()
                        ? Component.translatable("item.jeg.gun.empty")
                        : Component.translatable("item.jeg.gun.no_ammo");
                player.displayClientMessage(message, true);
            } else {
                playDryFireSound(level, player);
            }
            return false;
        }

        if (level.isClientSide()) {
            float recoilMultiplier = RecoilProfiles.multiplier(stats.id());
            float recoilKick = stats.recoilKick() * recoilMultiplier;
            addClientShotRecoil(recoilKick);
            float targetPitch = player.getXRot() - recoilKick * getVerticalRecoilPitchMultiplier(stats.id());
            player.setXRot(Mth.clamp(targetPitch, -90.0F, 90.0F));
            if (!automatic) {
                setTriggerLocked(stack, true);
            }

            // Client-side instant trail calculation for fast bullets
            ResourceLocation gunId = stats.id();
            // Removed custom trail rendering - rely on server-sent particles instead
            // which have proper depth testing and don't render through blocks
        } else {
            updateSpreadTracker(player, stats.id());
            int shotsFired = 0;
            int shotsToFire = shotsPerTrigger();
            for (int shot = 0; shot < shotsToFire; shot++) {
                if (!consumeAmmo(level, player, stack)) {
                    if (shot == 0) {
                        return false;
                    }
                    break;
                }
                fireAt(level, player, stack, null);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                shotsFired++;
            }
            if (shotsFired <= 0) {
                return false;
            }

            if (usesOverheatMechanic()) {
                int beforeHeat = getTrackedHeat(stack);
                addOverheatForShots(stack, stats.id(), shotsFired, player);
                JustEnoughGuns.LOGGER.info("[debug/heat] tryShoot server gun={} hand={} shotsFired={} beforeHeat={} afterHeat={} ammoAfter={} cooldown={}",
                        stats.id(),
                        hand,
                        shotsFired,
                        beforeHeat,
                        getTrackedHeat(stack),
                        getAmmo(stack),
                        Math.max(1, stats.fireDelay()));
            }

            if (stack.getItem() instanceof AnimatedGunItem animated) {
                animated.triggerShoot(level, player, stack);
            }

            applyRecoilBackstep(player);

            if (!automatic) {
                setTriggerLocked(stack, true);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(stack.getItem(), Math.max(1, stats.fireDelay()));
        }

        playSound(level, player, stats.fireSoundEvent().or(stats::enchantedFireSoundEvent));
        return true;
    }

    private void applyRecoilBackstep(Player player) {
        if (!Config.recoilBackstepEnabled()) {
            return;
        }
        if (!isHeavyBackstepWeapon(stats.id())) {
            return;
        }

        double force = stats.recoilKick() * RecoilProfiles.multiplier(stats.id()) * 0.20D;
        if (isRocketKnockbackWeapon(stats.id())) {
            force *= 4.2D;
            force = Mth.clamp(force, 0.100D, 0.380D);
        } else if (isShotgun(stats.id())) {
            force *= 2.0D;
            force = Mth.clamp(force, 0.030D, 0.180D);
        } else {
            force = Mth.clamp(force, 0.010D, 0.110D);
        }
        if (player.isCrouching() && player.level().getBlockState(player.getOnPos()).isSolid()) {
            force *= 0.5D;
        }
        double scale = Config.recoilBackstepScale();
        if (scale <= 0.0D) {
            // Treat non-positive scale as an invalid config and fall back to default.
            scale = 0.5D;
        }
        force *= scale;
        if (force <= 0.0D) {
            return;
        }

        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 direction = look.normalize();
        Vec3 recoilVelocity = direction.scale(-force);
        player.setDeltaMovement(player.getDeltaMovement().add(recoilVelocity));
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
    }

    private boolean hasAmmoAvailable(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        if (stats.isInventoryFed() || !stats.usesMagazine()) {
            return hasAmmoInInventory(player);
        }

        ensureAmmoInitialized(stack);
        return getAmmo(stack) > 0;
    }

    private boolean hasAmmoInInventory(Player player) {
        Optional<Item> ammoItem = getAmmoItem();
        if (ammoItem.isEmpty()) {
            return true;
        }

        Item ammo = ammoItem.get();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.is(ammo)) {
                return true;
            }
        }
        return false;
    }

    private boolean consumeAmmo(Level level, Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        if (stats.isInventoryFed()) {
            if (consumeSingleAmmoFromInventory(player)) {
                return true;
            }
            player.displayClientMessage(Component.translatable("item.jeg.gun.no_ammo"), true);
            return false;
        }

        if (!stats.usesMagazine()) {
            return consumeSingleAmmoFromInventory(player);
        }

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            player.displayClientMessage(Component.translatable("item.jeg.gun.empty"), true);
            return false;
        }

        setAmmo(stack, ammo - 1);
        return true;
    }

    private boolean consumeSingleAmmoFromInventory(Player player) {
        Optional<Item> ammoItem = getAmmoItem();
        if (ammoItem.isEmpty()) {
            return true;
        }

        Item ammo = ammoItem.get();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.is(ammo)) {
                slot.shrink(1);
                if (slot.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }

        return false;
    }

    private Optional<Item> getAmmoItem() {
        ResourceLocation ammoId = stats.ammoItem();
        if (ammoId == null || ammoId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "air"))) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(ammoId);
    }

    public void fireAt(Level level, LivingEntity shooter, ItemStack stack, @Nullable LivingEntity target) {
        Vec3 origin = shooter.getEyePosition();
        RandomSource random = shooter.getRandom();
        int pellets = Math.max(1, stats.projectileAmount());
        ResourceLocation gunId = stats.id();

        boolean grenadeLauncher = gunId.equals(GRENADE_LAUNCHER_ID);
        float grenadePower = grenadeLauncher ? GRENADE_BASE_POWER : Math.max(1.8F, stats.damage() / 12.0F + 1.5F);
        int fuseTicks = grenadeLauncher ? GRENADE_FUSE_TICKS : 40;
        Vec3 shooterMotion = shooter.getDeltaMovement();
        if (level instanceof ServerLevel serverLevel) {
            NetworkHandler.sendGunFireFx(serverLevel, shooter.getId(), random.nextFloat());
        }

        for (int i = 0; i < pellets; i++) {
            Vec3 direction = computeDirection(shooter, origin, target, random, stats);
            Vec3 muzzle = origin.add(direction.scale(0.35F));

            if (grenadeLauncher) {
                GrenadeEntity grenade = new GrenadeEntity(level, shooter, grenadePower, fuseTicks, true);
                grenade.initialisePosition(muzzle);
                Vec3 launchVelocity = direction.scale(Math.max(1.2F, stats.projectileSpeed() * 0.9F)).add(shooterMotion);
                grenade.setDeltaMovement(launchVelocity);
                level.addFreshEntity(grenade);
            } else {
                Vec3 velocity = direction.scale(stats.projectileSpeed());
                BulletEntity bullet = new BulletEntity(level, shooter, stats, velocity);
                bullet.initialisePosition(muzzle);
                level.addFreshEntity(bullet);
                if (level instanceof ServerLevel serverLevel && isBulletClassWeapon(stats.id())) {
                    bullet.sendTrailToClients(serverLevel);
                }

                // Add bullet trail particles for all guns EXCEPT flamethrower
                // (flamethrower already has its own particle effects)
                if (!stats.flameTrail() && level instanceof ServerLevel serverLevel) {
                    // Use penetration-aware raycast to spawn particles along actual bullet path
                    spawnBulletTrailParticles(serverLevel, muzzle, direction, stats, shooter);
                }

                // NeoForge 1.21.11: keep fire feedback to recoil-only for first person.
                // Disabling shoot trigger avoids intermittent invisibility caused by per-shot animated state.
            }
        }
    }

    public void fireDirectionally(Level level, LivingEntity shooter, ItemStack stack, Vec3 direction) {
        fireDirectionallyFrom(level, shooter, stack, shooter.getEyePosition(), direction);
    }

    /**
     * Fire using a custom origin (muzzle) instead of {@link LivingEntity#getEyePosition()}.
     * Used by large/animated entities that have visible weapon muzzles away from the eyes.
     */
    public void fireDirectionallyFrom(Level level, LivingEntity shooter, ItemStack stack, Vec3 origin, Vec3 direction) {
        int pellets = Math.max(1, stats.projectileAmount());
        ResourceLocation gunId = stats.id();

        boolean grenadeLauncher = gunId.equals(GRENADE_LAUNCHER_ID);
        float grenadePower = grenadeLauncher ? GRENADE_BASE_POWER : Math.max(1.8F, stats.damage() / 12.0F + 1.5F);
        int fuseTicks = grenadeLauncher ? GRENADE_FUSE_TICKS : 40;
        Vec3 shooterMotion = shooter.getDeltaMovement();
        Vec3 normalized = direction.normalize();
        if (level instanceof ServerLevel serverLevel) {
            NetworkHandler.sendGunFireFx(serverLevel, shooter.getId(), shooter.getRandom().nextFloat());
        }

        for (int i = 0; i < pellets; i++) {
            // Push slightly forward to avoid self-collision while still visually originating from the muzzle.
            Vec3 muzzle = origin.add(normalized.scale(0.10F));

            if (grenadeLauncher) {
                GrenadeEntity grenade = new GrenadeEntity(level, shooter, grenadePower, fuseTicks, true);
                grenade.initialisePosition(muzzle);
                Vec3 launchVelocity = normalized.scale(Math.max(1.2F, stats.projectileSpeed() * 0.9F)).add(shooterMotion);
                grenade.setDeltaMovement(launchVelocity);
                level.addFreshEntity(grenade);
            } else {
                Vec3 velocity = normalized.scale(stats.projectileSpeed());
                BulletEntity bullet = new BulletEntity(level, shooter, stats, velocity);
                bullet.initialisePosition(muzzle);
                level.addFreshEntity(bullet);
                if (level instanceof ServerLevel serverLevel && isBulletClassWeapon(stats.id())) {
                    bullet.sendTrailToClients(serverLevel);
                }

                if (!stats.flameTrail() && level instanceof ServerLevel serverLevel) {
                    // Use penetration-aware raycast to spawn particles along actual bullet path
                    spawnBulletTrailParticles(serverLevel, muzzle, normalized, stats, shooter);
                }

                // NeoForge 1.21.11: keep fire feedback to recoil-only for first person.
                // Disabling shoot trigger avoids intermittent invisibility caused by per-shot animated state.
            }
        }
    }

    private Vec3 computeDirection(LivingEntity shooter, Vec3 origin, @Nullable LivingEntity target, RandomSource random, GunStats stats) {
        Vec3 base = target != null
                ? target.getEyePosition().subtract(origin)
                : shooter.getViewVector(1.0F);
        return applyLegacySpread(shooter, base, stats, random);
    }

    private static Vec3 applyLegacySpread(LivingEntity shooter, Vec3 baseDirection, GunStats stats, RandomSource random) {
        Vec3 forwards = baseDirection.normalize();
        if (forwards.lengthSqr() < 1.0E-6D) {
            forwards = shooter.getViewVector(1.0F);
        }

        float gunSpread = stats.spread();
        if (gunSpread == 0.0F) {
            return forwards.normalize();
        }

        if (shooter instanceof Player player) {
            boolean minigun = isMinigunWeapon(stats.id());
            gunSpread *= getSpreadMultiplier(player, stats.id());
            if (!minigun && NetworkHandler.isAiming(player)) {
                gunSpread *= 0.5F;
            }
            if (!minigun) {
                gunSpread *= getMovementSpreadMultiplier(player, stats);
            } else {
                gunSpread = Math.max(gunSpread, stats.spread() * MINIGUN_SPREAD_FLOOR);
            }
            if (isShotgun(stats.id())) {
                float shotgunFloor = stats.spread() * (NetworkHandler.isAiming(player) ? 0.35F : 0.60F);
                gunSpread = Math.max(gunSpread, shotgunFloor);
            }
        } else {
            float earlySpreadMultiplier = shooter.level().getDifficulty() != Difficulty.HARD ? 10.0F : 5.0F;
            float scaledSpreadMultiplier = Config.scaleGunnerSpreadMultiplier(shooter.level(), earlySpreadMultiplier);
            gunSpread *= scaledSpreadMultiplier;
            if (isShotgun(stats.id())) {
                gunSpread *= (float) Config.gunnerShotgunSpreadMultiplier();
            }
        }

        if (gunSpread <= 0.0F) {
            return forwards.normalize();
        }

        float spreadRadians = Math.min(gunSpread, 170.0F) * 0.5F * Mth.DEG_TO_RAD;
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 sideways = forwards.cross(worldUp);
        if (sideways.lengthSqr() < 1.0E-6D) {
            sideways = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideways = sideways.normalize();
        }
        Vec3 upwards = sideways.cross(forwards).normalize();

        float theta = random.nextFloat() * 2.0F * (float) Math.PI;
        float r = Mth.sqrt(random.nextFloat()) * (float) Math.tan(spreadRadians);
        float a1 = Mth.cos(theta) * r;
        float a2 = Mth.sin(theta) * r;

        return forwards.add(sideways.scale(a1)).add(upwards.scale(a2)).normalize();
    }

    private static void updateSpreadTracker(Player player, ResourceLocation gunId) {
        SpreadTrackerState playerState = SPREAD_TRACKERS.computeIfAbsent(player.getUUID(), ignored -> new SpreadTrackerState());
        SpreadEntry entry = playerState.byGun.computeIfAbsent(gunId, ignored -> new SpreadEntry());
        long now = System.currentTimeMillis();
        if (entry.lastFireMs != -1L) {
            long delta = now - entry.lastFireMs;
            if (delta < SPREAD_THRESHOLD_MS) {
                if (entry.spreadCount < SPREAD_MAX_COUNT) {
                    entry.spreadCount++;
                    if (entry.spreadCount < SPREAD_MAX_COUNT && !NetworkHandler.isAiming(player)) {
                        entry.spreadCount++;
                    }
                    if (isMinigunWeapon(gunId) && entry.spreadCount < SPREAD_MAX_COUNT) {
                        entry.spreadCount++;
                    }
                }
            } else {
                entry.spreadCount = 0;
            }
        }
        entry.lastFireMs = now;
    }

    private static float getSpreadMultiplier(Player player, ResourceLocation gunId) {
        SpreadTrackerState playerState = SPREAD_TRACKERS.get(player.getUUID());
        if (playerState == null) {
            return isMinigunWeapon(gunId) ? MINIGUN_SPREAD_FLOOR : 0.0F;
        }
        SpreadEntry entry = playerState.byGun.get(gunId);
        if (entry == null) {
            return isMinigunWeapon(gunId) ? MINIGUN_SPREAD_FLOOR : 0.0F;
        }
        float tracked = (float) entry.spreadCount / (float) SPREAD_MAX_COUNT;
        if (isMinigunWeapon(gunId)) {
            return Math.max(MINIGUN_SPREAD_FLOOR, tracked);
        }
        return tracked;
    }

    public static boolean isShotgunWeapon(ResourceLocation gunId) {
        return SHOTGUN_IDS.contains(gunId.getPath());
    }

    private static boolean isShotgun(ResourceLocation gunId) {
        return isShotgunWeapon(gunId);
    }

    private static boolean isHeavyBackstepWeapon(ResourceLocation gunId) {
        return HEAVY_BACKSTEP_IDS.contains(gunId.getPath()) || isShotgun(gunId);
    }

    private static boolean isRocketKnockbackWeapon(ResourceLocation gunId) {
        String path = gunId.getPath();
        return "rocket_launcher".equals(path) || "typhoonee".equals(path);
    }

    private static boolean isMinigunWeapon(ResourceLocation gunId) {
        return "minigun".equals(gunId.getPath());
    }

    private static float getMovementSpreadMultiplier(Player player, GunStats stats) {
        if (player.isCrouching() || !isPlayerMoving(player)) {
            return 1.0F;
        }
        if (isAssaultRifleWeapon(stats.id())) {
            return 1.90F;
        }
        ResourceLocation ammoId = stats.ammoItem();
        if (ammoId != null) {
            String ammoPath = ammoId.getPath();
            if ("rifle_ammo".equals(ammoPath)) {
                return 1.65F;
            }
            if ("pistol_ammo".equals(ammoPath)) {
                return 1.35F;
            }
        }
        return 1.15F;
    }

    private static boolean isAssaultRifleWeapon(ResourceLocation gunId) {
        return ASSAULT_RIFLE_IDS.contains(gunId.getPath());
    }

    private static float getVerticalRecoilPitchMultiplier(ResourceLocation gunId) {
        String path = gunId.getPath();
        if ("light_machine_gun".equals(path)) {
            return 5.3F;
        }
        if ("minigun".equals(path)) {
            return 4.9F;
        }
        return 7.5F;
    }

    private static boolean isPlayerMoving(Player player) {
        Vec3 velocity = player.getDeltaMovement();
        return velocity.horizontalDistanceSqr() > MOVEMENT_THRESHOLD_SQR;
    }

    private static final class SpreadTrackerState {
        final Map<ResourceLocation, SpreadEntry> byGun = new WeakHashMap<>();
    }

    private static final class SpreadEntry {
        long lastFireMs = -1L;
        int spreadCount;
    }

    private void playSound(Level level, LivingEntity shooter, Optional<SoundEvent> sound) {
        SoundSource source = shooter instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
        double x = shooter.getX();
        double y = shooter.getY();
        double z = shooter.getZ();
        sound.ifPresentOrElse(
                value -> level.playSound(null, x, y, z, value, source, 7.5F, 1.0F),
                () -> level.playSound(null, x, y, z, SoundEvents.CROSSBOW_SHOOT, source, 7.5F, 1.1F)
        );
    }

    private void playDryFireSound(Level level, LivingEntity shooter) {
        SoundSource source = shooter instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
        double x = shooter.getX();
        double y = shooter.getY();
        double z = shooter.getZ();
        level.playSound(
                shooter instanceof Player ? (Player) shooter : null,
                x,
                y,
                z,
                SoundEvents.LEVER_CLICK,
                source,
                0.6F,
                1.8F
        );
    }

    public boolean tryReload(Level level, Player player, ItemStack stack, boolean notify) {
        if (!stats.usesMagazine()) {
            return false;
        }

        ensureAmmoInitialized(stack);
        int ammo = getAmmo(stack);
        if (ammo >= stats.magazineSize()) {
            if (notify) {
                player.displayClientMessage(Component.translatable("item.jeg.gun.magazine_full"), true);
            }
            return false;
        }

        int needed = stats.magazineSize() - ammo;
        int pulled = player.getAbilities().instabuild ? needed : removeAmmoFromInventory(player, needed);
        if (pulled <= 0) {
            if (notify) {
                player.displayClientMessage(Component.translatable("item.jeg.gun.no_ammo"), true);
            }
            return false;
        }

        setAmmo(stack, ammo + pulled);
        int reloadTicks = Math.max(1, stats.totalReloadTime());
        player.getCooldowns().addCooldown(stack.getItem(), reloadTicks);
        playSound(level, player, stats.reloadStartSoundEvent());

        if (stack.getItem() instanceof AnimatedGunItem animated) {
            if (usesSegmentedReloadAnimation()) {
                animated.triggerReloadStart(level, player, stack);
            } else {
                animated.triggerReload(level, player, stack);
            }
        }

        return true;
    }

    private boolean usesSegmentedReloadAnimation() {
        return SEGMENTED_RELOAD_ANIM_IDS.contains(stats.id().getPath());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide() || !usesOverheatMechanic()) {
            return;
        }
        if (isCoolingWithWater(stack)) {
            int remainingCooling = stack.getOrDefault(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get(), 0);
            if (remainingCooling > 0) {
                stack.set(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get(), remainingCooling - 1);
            }
            if (remainingCooling <= 1) {
                if (entity instanceof Player playerHolder) {
                    ItemStack coolantStack = playerHolder.getOffhandItem().is(ModItems.COOLANT.get()) || playerHolder.getOffhandItem().is(ModItems.ENHANCED_COOLANT.get()) ? playerHolder.getOffhandItem() : ItemStack.EMPTY;
                    if (!coolantStack.isEmpty()) {
                        finishWaterCooling(coolantStack, level, playerHolder);
                        playerHolder.stopUsingItem();
                    } else {
                        clearWaterCooling(stack);
                    }
                } else {
                    clearWaterCooling(stack);
                }
            }
        } else {
            boolean coolingBlockedByFiring = selected
                    && entity instanceof Player playerHolder
                    && playerHolder.getCooldowns().isOnCooldown(stack.getItem());
            if (!coolingBlockedByFiring && (!selected || (level.getGameTime() & 1L) == 0L)) {
                coolOverheat(stack, selected);
            }
        }
        if (entity instanceof Player player) {
            cancelWaterCoolingIfInvalid(player);
        }
    }

    private int removeAmmoFromInventory(Player player, int needed) {
        Optional<Item> ammoItem = getAmmoItem();
        if (ammoItem.isEmpty()) {
            return needed;
        }

        Item ammo = ammoItem.get();
        // Check if this is flamethrower (fire_charge ammo) - 1 fire_charge = 3 ammo
        boolean isFlamethrower = stats.id().equals(Reference.id("flamethrower"));
        int ammoPerItem = isFlamethrower ? 3 : 1;

        int removed = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack invStack = player.getInventory().getItem(slot);
            if (!invStack.isEmpty() && invStack.is(ammo)) {
                int itemsNeeded = (int) Math.ceil((double)(needed - removed) / ammoPerItem);
                int take = Math.min(itemsNeeded, invStack.getCount());
                invStack.shrink(take);
                removed += take * ammoPerItem;
                if (invStack.isEmpty()) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                }
                if (removed >= needed) {
                    break;
                }
            }
        }
        return Math.min(removed, needed); // Cap at needed amount
    }

    /**
     * Spawn bullet trail particles with penetration-aware raycast.
     * Particles have proper depth testing and won't render through blocks.
     */
    private void spawnBulletTrailParticles(ServerLevel level, Vec3 start, Vec3 direction, GunStats stats, LivingEntity shooter) {
        // Muzzle-only black dust with chance per shot to avoid constant spam.
        if (level.random.nextFloat() < 0.25F) {
            int count = level.random.nextFloat() < 0.35F ? 2 : 1;
            level.sendParticles(
                ParticleTypes.SMOKE,
                start.x, start.y, start.z,
                count, 0.02, 0.02, 0.02, 0.01
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        float displayDamage = this.stats.id().equals(GRENADE_LAUNCHER_ID) ? GRENADE_BASE_POWER * GRENADE_DAMAGE_FACTOR : stats.damage();
        tooltip.add(Component.translatable("info.jeg.damage", String.format("%.1f", displayDamage)));

        if (stats.usesMagazine()) {
            tooltip.add(Component.translatable("info.jeg.ammo", getAmmo(stack), stats.magazineSize()));
        }

        if (usesOverheatMechanic()) {
            int heat = getOverheatPercent(stack);
            ChatFormatting color = heat >= 100 ? ChatFormatting.RED : ChatFormatting.GOLD;
            tooltip.add(Component.literal("Overheat: " + heat + "%").withStyle(color));
        }

        // Add ammo type information
        Optional<Item> ammoItem = getAmmoItem();
        if (ammoItem.isPresent()) {
            ItemStack ammoStack = new ItemStack(ammoItem.get());
            Component ammoName = ammoStack.getHoverName();
            tooltip.add(Component.translatable("info.jeg.ammo_type", ammoName));
        }

        double effectiveRange = GunRangeHelper.computeEffectiveRange(this.stats);
        if (effectiveRange > 0.0D) {
            tooltip.add(Component.translatable("info.jeg.range", String.format(Locale.US, "%.0f", effectiveRange)));
        }

        // Add projectile count for shotguns
        if (stats.projectileAmount() > 1) {
            tooltip.add(Component.translatable("info.jeg.projectiles", stats.projectileAmount()));
        }
    }

    private static void addClientDryFireRecoil(float recoilAmount) {
        invokeClientRecoilMethod("addDryFire", recoilAmount);
    }

    private static void addClientShotRecoil(float recoilAmount) {
        invokeClientRecoilMethod("addShot", recoilAmount);
    }

    private static void invokeClientRecoilMethod(String methodName, float recoilAmount) {
        try {
            Class<?> recoilClass = Class.forName("ttv.migami.jeg.client.GunRecoilHandler");
            recoilClass.getDeclaredMethod(methodName, float.class).invoke(null, recoilAmount);
        } catch (Throwable ignored) {
        }
    }
}





