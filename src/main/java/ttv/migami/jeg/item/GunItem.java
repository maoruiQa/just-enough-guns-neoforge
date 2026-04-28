package ttv.migami.jeg.item;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.Reference;
import net.minecraft.ChatFormatting;
// NOTE: AnimatedGunItem extends GunItem and adds GeckoLib animation triggers.
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.util.HudMessageHelper;

public class GunItem extends Item {
    private static final Identifier GRENADE_LAUNCHER_ID = Reference.id("grenade_launcher");
    private static final Identifier ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher");
    private static final int ROCKET_LAUNCHER_HOLD_TICKS = 7;
    private static final float GRENADE_BASE_POWER = 4.0F;
    private static final float GRENADE_DAMAGE_FACTOR = 5.0F;
    private static final int GRENADE_FUSE_TICKS = 600;
    private static final Set<String> AUTOMATIC_IDS = Set.of(
            "abstract_gun",
            "assault_rifle",
            "blossom_rifle",
            "burst_rifle",
            "combat_pistol",
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
    private static final Identifier WATER_COOL_SOUND_ID = Reference.id("item.cooldown_with_water");
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
            "typhoonee",
            "compound_bow",
            "primitive_bow"
    );
    private static final Set<String> HEAVY_BACKSTEP_IDS = Set.of(
            "light_machine_gun",
            "minigun",
            "rocket_launcher",
            "typhoonee"
    );
    private static final float MINIGUN_SPREAD_FLOOR = 0.85F;
    private static final double MOVEMENT_THRESHOLD_SQR = 0.0036D;
    private static final float RIFLE_FIRE_SPREAD_CAP = 2.70F;
    private static final float SIDEARM_FIRE_SPREAD_CAP = 1.725F;
    private static final float DEFAULT_FIRE_SPREAD_CAP = 1.0F;
    private static final int OVERHEAT_MAX = 200;
    private static final int OVERHEAT_TRACKED_MAX = 280;
    private static final int OVERHEAT_RECOVERY_BUFFER = 80;
    private static final int OVERHEAT_HEAT_NUMERATOR_LMG = 13;
    private static final int OVERHEAT_HEAT_NUMERATOR_MINIGUN = 6;
    private static final int OVERHEAT_HEAT_DENOMINATOR = 6;
    private static final int OVERHEAT_HEAT_DENOMINATOR_LMG = 12;
    private static final int OVERHEAT_COOL_NUMERATOR_HELD = 2;
    private static final int OVERHEAT_COOL_NUMERATOR_IDLE = 4;
    private static final int OVERHEAT_COOL_DENOMINATOR = 5;

    private final GunStats stats;

    public record MagazineInventorySummary(int loadedMagazineCount, int emptyMagazineCount) {}

    private record MagazineInventoryScan(int loadedMagazineCount, int emptyMagazineCount, int bestMagazineSlot) {}

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
        if (usesLoadedAmmo()) {
            stack.set(ModDataComponents.GUN_AMMO.get(), 0); // 弹匣默认为空
        }
        return stack;
    }

    private void ensureAmmoInitialized(ItemStack stack) {
        if (usesLoadedAmmo() && !stack.has(ModDataComponents.GUN_AMMO.get())) {
            stack.set(ModDataComponents.GUN_AMMO.get(), 0); // 弹匣默认为空
        }
    }

    public boolean usesLoadedAmmo() {
        return !isInventoryFedGun();
    }

    public boolean isInventoryFedGun() {
        String path = stats.id().getPath();
        return "minigun".equals(path) || "rocket_launcher".equals(path);
    }

    public boolean usesMagazineSwapReload() {
        return Config.magazineFeedEnabled()
                && usesLoadedAmmo()
                && (isThirtyRoundRifle() || isCustomSmg() || isMagazineSwapPistol() || isMagazineSwapShotgun() || isMachineGunMagazineSwap());
    }

    public boolean usesLegacyLoadedReload() {
        return usesLoadedAmmo() && !usesMagazineSwapReload();
    }

    private boolean isThirtyRoundRifle() {
        String path = stats.id().getPath();
        return ("semi_auto_rifle".equals(path) || GunCategory.fromStats(stats) == GunCategory.RIFLE) && stats.magazineSize() == 30;
    }

    private boolean isCustomSmg() {
        return "custom_smg".equals(stats.id().getPath());
    }

    private boolean isMagazineSwapPistol() {
        return GunCategory.fromStats(stats) == GunCategory.PISTOL && stats.magazineSize() == 12;
    }

    private boolean isMagazineSwapShotgun() {
        return GunCategory.fromStats(stats) == GunCategory.SHOTGUN && stats.magazineSize() == 8;
    }

    private boolean isMachineGunMagazineSwap() {
        return "light_machine_gun".equals(stats.id().getPath()) && stats.magazineSize() == 150;
    }

    private int getAmmo(ItemStack stack) {
        if (!usesLoadedAmmo()) {
            return 0;
        }
        ensureAmmoInitialized(stack);
        return stack.getOrDefault(ModDataComponents.GUN_AMMO.get(), stats.magazineSize());
    }

    public int getMagazineAmmo(ItemStack stack) {
        return usesLoadedAmmo() ? getAmmo(stack) : 0;
    }

    public MagazineInventorySummary getMagazineInventorySummary(Player player) {
        MagazineInventoryScan scan = scanCompatibleMagazines(player);
        return new MagazineInventorySummary(scan.loadedMagazineCount(), scan.emptyMagazineCount());
    }

    public boolean isAutomatic() {
        return isAutomatic(this.stats);
    }

    public static boolean isAutomatic(GunStats stats) {
        return AUTOMATIC_IDS.contains(stats.id().getPath());
    }

    public static boolean isBulletClassWeapon(Identifier gunId) {
        return !NON_BULLET_TRAIL_IDS.contains(gunId.getPath());
    }

    public static boolean isRocketLauncher(ItemStack stack) {
        return stack.getItem() instanceof GunItem gun && isRocketLauncher(gun.getStats().id());
    }

    public static boolean isRocketLauncher(Identifier gunId) {
        return ROCKET_LAUNCHER_ID.equals(gunId);
    }

    public static boolean isHoldToFireWeapon(ItemStack stack) {
        return isRocketLauncher(stack);
    }

    public static int holdToFireTicks(ItemStack stack) {
        return isHoldToFireWeapon(stack) ? ROCKET_LAUNCHER_HOLD_TICKS : 0;
    }

    private int shotsPerTrigger() {
        return 1;
    }

    /**
     * Check if this gun fires slow bullets (tracked projectiles) vs fast bullets (instant raycast).
     * Slow bullets: flamethrower, flare gun, rocket launcher, hypersonic cannon, typhoonee
     * Fast bullets: all other guns
     */
    private static boolean isSlowBullet(Identifier gunId) {
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
        if (usesLoadedAmmo()) {
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

    private static boolean isOverheatWeapon(Identifier gunId) {
        String path = gunId.getPath();
        return "light_machine_gun".equals(path) || "minigun".equals(path);
    }

    private static int getHeatNumeratorPerShot(Identifier gunId) {
        return "minigun".equals(gunId.getPath()) ? OVERHEAT_HEAT_NUMERATOR_MINIGUN : OVERHEAT_HEAT_NUMERATOR_LMG;
    }

    private static int getHeatDenominatorPerShot(Identifier gunId) {
        return "light_machine_gun".equals(gunId.getPath()) ? OVERHEAT_HEAT_DENOMINATOR_LMG : OVERHEAT_HEAT_DENOMINATOR;
    }

    private static int getTrackedHeat(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.GUN_HEAT.get(), 0), 0, OVERHEAT_TRACKED_MAX);
    }

    private static void setTrackedHeat(ItemStack stack, int heat) {
        int clamped = Mth.clamp(heat, 0, OVERHEAT_TRACKED_MAX);
        stack.set(ModDataComponents.GUN_HEAT.get(), clamped);
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

    private static void addOverheatForShots(ItemStack stack, Identifier gunId, int shotsFired, @Nullable Player shooter) {
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
        int next = applyFractionalHeat(stack, getHeatNumeratorPerShot(gunId) * effectiveShots, getHeatDenominatorPerShot(gunId));
        setTrackedHeat(stack, next);
    }

    private static void coolOverheat(ItemStack stack, boolean heldInHand) {
        int current = getTrackedHeat(stack);
        if (current <= 0) {
            return;
        }
        int numerator = getOverheatCoolNumerator(stack, heldInHand);
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

    private static int getOverheatCoolNumerator(ItemStack stack, boolean heldInHand) {
        int numerator = heldInHand ? OVERHEAT_COOL_NUMERATOR_HELD : OVERHEAT_COOL_NUMERATOR_IDLE;
        if (stack.getItem() instanceof GunItem gun && "light_machine_gun".equals(gun.getStats().id().getPath())) {
            return Math.max(1, numerator / 2);
        }
        return numerator;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (tryStartWaterCooling(level, player, hand)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        // Right-click is reserved for ADS. Shooting is driven by left-click C2S shoot packets.
        return isCoolant(stack) ? InteractionResult.CONSUME : InteractionResult.CONSUME;
    }

    @Override
    public net.minecraft.world.item.ItemUseAnimation getUseAnimation(ItemStack stack) {
        return isCoolant(stack) ? net.minecraft.world.item.ItemUseAnimation.NONE : super.getUseAnimation(stack);
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
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (isCoolant(stack)) {
            int usedTicks = getUseDuration(stack, livingEntity) - timeLeft;
            if (usedTicks >= getWaterCoolingUseDuration(stack, livingEntity)) {
                return false;
            }
        }
        if (releaseWaterCooling(stack, level, livingEntity, timeLeft)) {
            return true;
        }
        return super.releaseUsing(stack, level, livingEntity, timeLeft);
    }

    public boolean tryShoot(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ensureAmmoInitialized(stack);
        boolean automatic = isAutomatic();

        if (!automatic && isTriggerLocked(stack)) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }

        if (usesOverheatMechanic() && isOverheated(stack)) {
            if (level.isClientSide()) {
                HudMessageHelper.showActionBar(player, Component.literal("Gun overheated"));
            }
            return false;
        }

        if (!hasAmmoAvailable(player, stack)) {
            if (level.isClientSide()) {
                playDryFireSound(level, player);
                Component message = usesLoadedAmmo() && !isInventoryFedGun()
                        ? Component.translatable("item.jeg.gun.empty")
                        : Component.translatable("item.jeg.gun.no_ammo");
                HudMessageHelper.showActionBar(player, message);
            } else {
                playDryFireSound(level, player);
            }
            return false;
        }

        if (level.isClientSide()) {
            if (!automatic) {
                setTriggerLocked(stack, true);
            }

            // Client-side instant trail calculation for fast bullets
            Identifier gunId = stats.id();
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
            player.getCooldowns().addCooldown(stack, Math.max(1, stats.fireDelay()));
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
        } else if (isShotgunWeapon(stats.id())) {
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

        if (isInventoryFedGun()) {
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

        if (isInventoryFedGun()) {
            if (consumeSingleAmmoFromInventory(player)) {
                return true;
            }
            HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.no_ammo"));
            return false;
        }

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.empty"));
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
        Identifier ammoId = stats.ammoItem();
        if (ammoId == null || ammoId.equals(Identifier.fromNamespaceAndPath("minecraft", "air"))) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(ammoId);
    }

    public void fireAt(Level level, LivingEntity shooter, ItemStack stack, @Nullable LivingEntity target) {
        Vec3 origin = shooter.getEyePosition();
        RandomSource random = shooter.getRandom();
        int pellets = Math.max(1, stats.projectileAmount());
        Identifier gunId = stats.id();

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
        Identifier gunId = stats.id();

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
            gunSpread *= getSpreadMultiplier(player, stats);
            if (!minigun && NetworkHandler.isAiming(player)) {
                gunSpread *= 0.5F;
            }
            if (!minigun) {
                gunSpread += getMovementSpreadDegrees(player, stats, NetworkHandler.isAiming(player));
            } else {
                gunSpread = Math.max(gunSpread, stats.spread() * MINIGUN_SPREAD_FLOOR);
            }
            if (isShotgunWeapon(stats.id())) {
                float shotgunFloor = stats.spread() * (NetworkHandler.isAiming(player) ? 0.35F : 0.60F);
                gunSpread = Math.max(gunSpread, shotgunFloor);
            }
        } else {
            float earlySpreadMultiplier = shooter.level().getDifficulty() != Difficulty.HARD ? 10.0F : 5.0F;
            float scaledSpreadMultiplier = Config.scaleGunnerSpreadMultiplier(shooter.level(), earlySpreadMultiplier);
            gunSpread *= scaledSpreadMultiplier;
            if (isShotgunWeapon(stats.id())) {
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

    private static void updateSpreadTracker(Player player, Identifier gunId) {
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

    public static void recordClientShotSpread(Player player, GunStats stats) {
        updateSpreadTracker(player, stats.id());
    }

    private static float getSpreadMultiplier(Player player, GunStats stats) {
        SpreadTrackerState playerState = SPREAD_TRACKERS.get(player.getUUID());
        Identifier gunId = stats.id();
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
        return tracked * getFireSpreadCap(stats);
    }

    public static float getClientSpreadDegrees(Player player, GunStats stats, boolean aiming) {
        float gunSpread = stats.spread();
        if (gunSpread <= 0.0F) {
            return 0.0F;
        }

        boolean minigun = isMinigunWeapon(stats.id());
        gunSpread *= getSpreadMultiplier(player, stats);
        if (!minigun && aiming) {
            gunSpread *= 0.5F;
        }
        if (!minigun) {
            gunSpread += getMovementSpreadDegrees(player, stats, aiming);
        } else {
            gunSpread = Math.max(gunSpread, stats.spread() * MINIGUN_SPREAD_FLOOR);
        }
        if (isShotgunWeapon(stats.id())) {
            float shotgunFloor = stats.spread() * (aiming ? 0.35F : 0.60F);
            gunSpread = Math.max(gunSpread, shotgunFloor);
        }
        return Math.max(0.0F, gunSpread);
    }

    public static boolean isShotgunWeapon(Identifier gunId) {
        return SHOTGUN_IDS.contains(gunId.getPath());
    }

    private static boolean isHeavyBackstepWeapon(Identifier gunId) {
        return HEAVY_BACKSTEP_IDS.contains(gunId.getPath()) || isShotgunWeapon(gunId);
    }

    private static boolean isRocketKnockbackWeapon(Identifier gunId) {
        String path = gunId.getPath();
        return "rocket_launcher".equals(path) || "typhoonee".equals(path);
    }

    private static boolean isMinigunWeapon(Identifier gunId) {
        return "minigun".equals(gunId.getPath());
    }

    private static float getMovementSpreadDegrees(Player player, GunStats stats, boolean aiming) {
        if (player.isCrouching() || !isPlayerMoving(player)) {
            return 0.0F;
        }

        float multiplier = getMovementSpreadMultiplier(player, stats);
        if (aiming) {
            multiplier *= 0.65F;
        }
        return stats.spread() * multiplier;
    }

    private static float getMovementSpreadMultiplier(Player player, GunStats stats) {
        boolean sprinting = player.isSprinting();
        if (isLargeMovementSpreadWeapon(stats)) {
            return sprinting ? 5.50F : 3.30F;
        }
        if (isSidearmMovementSpreadWeapon(stats)) {
            return sprinting ? 2.475F : 1.425F;
        }
        return sprinting ? 1.25F : 0.65F;
    }

    private static float getFireSpreadCap(GunStats stats) {
        if (isLargeMovementSpreadWeapon(stats)) {
            return RIFLE_FIRE_SPREAD_CAP;
        }
        if (isSidearmMovementSpreadWeapon(stats)) {
            return SIDEARM_FIRE_SPREAD_CAP;
        }
        return DEFAULT_FIRE_SPREAD_CAP;
    }

    private static boolean isLargeMovementSpreadWeapon(GunStats stats) {
        GunCategory category = GunCategory.fromStats(stats);
        return category == GunCategory.RIFLE
                || category == GunCategory.SNIPER
                || "light_machine_gun".equals(stats.id().getPath());
    }

    private static boolean isSidearmMovementSpreadWeapon(GunStats stats) {
        GunCategory category = GunCategory.fromStats(stats);
        return category == GunCategory.PISTOL || category == GunCategory.SMG;
    }

    private static boolean isPlayerMoving(Player player) {
        Vec3 velocity = player.getDeltaMovement();
        return velocity.horizontalDistanceSqr() > MOVEMENT_THRESHOLD_SQR;
    }

    private static final class SpreadTrackerState {
        final Map<Identifier, SpreadEntry> byGun = new WeakHashMap<>();
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
        if (!usesLoadedAmmo()) {
            return false;
        }

        if (usesMagazineSwapReload()) {
            return tryReloadWithMagazineSwap(level, player, stack, notify);
        }
        return tryReloadWithLooseAmmo(level, player, stack, notify);
    }

    private boolean tryReloadWithMagazineSwap(Level level, Player player, ItemStack stack, boolean notify) {
        ensureAmmoInitialized(stack);
        int ammo = getAmmo(stack);
        if (ammo >= stats.magazineSize()) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.magazine_full"));
            }
            return false;
        }

        if (player.getAbilities().instabuild) {
            setAmmo(stack, stats.magazineSize());
            finishReload(level, player, stack);
            return true;
        }

        MagazineInventoryScan scan = scanCompatibleMagazines(player);
        if (scan.bestMagazineSlot() < 0) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.no_ammo"));
            }
            return false;
        }

        ItemStack magazineStack = player.getInventory().getItem(scan.bestMagazineSlot());
        if (!(magazineStack.getItem() instanceof MagazineItem magazine)) {
            return false;
        }

        int newAmmo = magazine.getAmmoCount(magazineStack);
        if (newAmmo <= 0) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.no_ammo"));
            }
            return false;
        }

        ItemStack oldMagazine = createStoredMagazineStack(ammo);
        magazineStack.shrink(1);
        if (magazineStack.isEmpty()) {
            player.getInventory().setItem(scan.bestMagazineSlot(), ItemStack.EMPTY);
        }

        returnStoredMagazine(player, oldMagazine);
        setAmmo(stack, newAmmo);
        finishReload(level, player, stack);
        return true;
    }

    private boolean tryReloadWithLooseAmmo(Level level, Player player, ItemStack stack, boolean notify) {
        ensureAmmoInitialized(stack);
        int ammo = getAmmo(stack);
        if (ammo >= stats.magazineSize()) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.magazine_full"));
            }
            return false;
        }

        int needed = stats.magazineSize() - ammo;
        int pulled = player.getAbilities().instabuild ? needed : removeAmmoFromInventory(player, needed);
        if (pulled <= 0) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.no_ammo"));
            }
            return false;
        }

        setAmmo(stack, ammo + pulled);
        finishReload(level, player, stack);
        return true;
    }

    private void finishReload(Level level, Player player, ItemStack stack) {
        int reloadTicks = Math.max(1, stats.totalReloadTime());
        player.getCooldowns().addCooldown(stack, reloadTicks);
        playSound(level, player, stats.reloadStartSoundEvent());

        // Track reload progress on the stack so we can drive segmented reload animations over time.
        stack.set(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_START);
        stack.set(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get(), reloadTicks);
        stack.set(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), reloadTicks);
        if (level instanceof ServerLevel serverLevel) {
            stack.set(ModDataComponents.GUN_RELOAD_END_TICK.get(), serverLevel.getGameTime() + reloadTicks);
        }

        if (stack.getItem() instanceof AnimatedGunItem animated) {
            if (usesSegmentedReloadAnimation()) {
                animated.triggerReloadStart(level, player, stack);
            } else {
                animated.triggerReload(level, player, stack);
            }
        }
    }

    private MagazineInventoryScan scanCompatibleMagazines(Player player) {
        MagazineItem compatibleMagazine = getCompatibleMagazineItem();
        Identifier ammoId = getCompatibleAmmoId();
        if (!usesMagazineSwapReload() || compatibleMagazine == null || ammoId == null) {
            return new MagazineInventoryScan(0, 0, -1);
        }

        int loadedCount = 0;
        int emptyCount = 0;
        int bestSlot = -1;
        int bestAmmoCount = -1;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!candidate.is(compatibleMagazine)) {
                continue;
            }

            int ammoCount = compatibleMagazine.getAmmoCount(candidate);
            if (ammoCount <= 0) {
                emptyCount++;
                continue;
            }

            Identifier storedAmmoId = compatibleMagazine.getAmmoItemId(candidate);
            if (!ammoId.equals(storedAmmoId)) {
                continue;
            }

            loadedCount++;
            if (ammoCount > bestAmmoCount) {
                bestAmmoCount = ammoCount;
                bestSlot = slot;
            }
        }

        return new MagazineInventoryScan(loadedCount, emptyCount, bestSlot);
    }

    @Nullable
    private MagazineItem getCompatibleMagazineItem() {
        if (!usesMagazineSwapReload()) {
            return null;
        }
        if (isThirtyRoundRifle()) {
            return ModItems.RIFLE_MAGAZINE.get();
        }
        if (isCustomSmg()) {
            return ModItems.SMG_MAGAZINE.get();
        }
        if (isMagazineSwapPistol()) {
            return ModItems.PISTOL_MAGAZINE.get();
        }
        if (isMagazineSwapShotgun()) {
            return ModItems.SHOTGUN_MAGAZINE.get();
        }
        if (isMachineGunMagazineSwap()) {
            return ModItems.MACHINE_GUN_MAGAZINE.get();
        }
        return null;
    }

    @Nullable
    private Identifier getCompatibleAmmoId() {
        Identifier ammoId = stats.ammoItem();
        if (ammoId == null || ammoId.equals(Identifier.fromNamespaceAndPath("minecraft", "air"))) {
            return null;
        }
        return ammoId;
    }

    private ItemStack createStoredMagazineStack(int ammoCount) {
        MagazineItem compatibleMagazine = getCompatibleMagazineItem();
        if (compatibleMagazine == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(compatibleMagazine);
        Identifier ammoId = getCompatibleAmmoId();
        if (ammoId != null) {
            stack.set(ModDataComponents.MAGAZINE_AMMO_ITEM.get(), ammoId.toString());
        }
        stack.set(ModDataComponents.MAGAZINE_AMMO_COUNT.get(), Mth.clamp(ammoCount, 0, compatibleMagazine.getCapacity()));
        return stack;
    }

    private void returnStoredMagazine(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private boolean usesSegmentedReloadAnimation() {
        return SEGMENTED_RELOAD_ANIM_IDS.contains(stats.id().getPath());
    }

    private static void clearReloadState(ItemStack stack) {
        stack.remove(ModDataComponents.GUN_RELOAD_STAGE.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_RELOAD_END_TICK.get());
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (usesOverheatMechanic()) {
            boolean heldInHand = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
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
                boolean coolingBlockedByFiring = heldInHand
                        && entity instanceof Player playerHolder
                        && playerHolder.getCooldowns().isOnCooldown(stack);
                if (!coolingBlockedByFiring && (!heldInHand || (level.getGameTime() & 1L) == 0L)) {
                    coolOverheat(stack, heldInHand);
                }
            }
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            return;
        }

        cancelWaterCoolingIfInvalid(player);

        long endTick = stack.getOrDefault(ModDataComponents.GUN_RELOAD_END_TICK.get(), 0L);
        int remaining = endTick > 0L
                ? (int) Math.max(0L, endTick - level.getGameTime())
                : stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remaining <= 0) {
            // Ensure stale state isn't kept around indefinitely.
            if (stack.has(ModDataComponents.GUN_RELOAD_STAGE.get())) {
                clearReloadState(stack);
            }
            return;
        }

        int total = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get(), remaining);
        int stage = stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE);

        stack.set(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), remaining);

        if (!(stack.getItem() instanceof AnimatedGunItem animated)) {
            if (remaining == 0) {
                clearReloadState(stack);
            }
            return;
        }

        if (usesSegmentedReloadAnimation()) {
            int startTicks = Math.max(3, total / 5);
            int stopTicks = Math.max(3, total / 5);
            int loopStartRemaining = Math.max(0, total - startTicks);

            // START -> LOOP once the start phase elapses.
            if (stage == RELOAD_STAGE_START && remaining <= loopStartRemaining) {
                stage = RELOAD_STAGE_LOOP;
                stack.set(ModDataComponents.GUN_RELOAD_STAGE.get(), stage);
                animated.triggerReloadLoop(level, player, stack);
            }

            // LOOP/START -> STOP near the end.
            if (stage != RELOAD_STAGE_STOP && remaining <= stopTicks) {
                stage = RELOAD_STAGE_STOP;
                stack.set(ModDataComponents.GUN_RELOAD_STAGE.get(), stage);
                animated.triggerReloadStop(level, player, stack);
            }
        }

        if (remaining == 0) {
            clearReloadState(stack);
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
        if ("minigun".equals(stats.id().getPath())) {
            if (shooter.getRandom().nextFloat() < 0.15F) {
                level.sendParticles(
                    ParticleTypes.SMOKE,
                    start.x, start.y, start.z,
                    1, 0.02, 0.02, 0.02, 0.01
                );
            }
            return;
        }

        // Muzzle-only black dust with chance per shot to avoid constant spam.
        if (shooter.getRandom().nextFloat() < 0.25F) {
            int count = shooter.getRandom().nextFloat() < 0.35F ? 2 : 1;
            level.sendParticles(
                ParticleTypes.SMOKE,
                start.x, start.y, start.z,
                count, 0.02, 0.02, 0.02, 0.01
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        float displayDamage = this.stats.id().equals(GRENADE_LAUNCHER_ID) ? GRENADE_BASE_POWER * GRENADE_DAMAGE_FACTOR : stats.damage();
        tooltipAdder.accept(Component.translatable("info.jeg.damage", String.format("%.1f", displayDamage)));

        if (usesLoadedAmmo()) {
            tooltipAdder.accept(Component.translatable("info.jeg.ammo", getAmmo(stack), stats.magazineSize()));
        }

        if (usesOverheatMechanic()) {
            int heat = getOverheatPercent(stack);
            ChatFormatting color = heat >= 100 ? ChatFormatting.RED : ChatFormatting.GOLD;
            tooltipAdder.accept(Component.literal("Overheat: " + heat + "%").withStyle(color));
        }

        // Add ammo type information
        Optional<Item> ammoItem = getAmmoItem();
        if (ammoItem.isPresent()) {
            ItemStack ammoStack = new ItemStack(ammoItem.get());
            Component ammoName = ammoStack.getHoverName();
            tooltipAdder.accept(Component.translatable("info.jeg.ammo_type", ammoName));
        }

        double effectiveRange = GunRangeHelper.computeEffectiveRange(this.stats);
        if (effectiveRange > 0.0D) {
            tooltipAdder.accept(Component.translatable("info.jeg.range", String.format(Locale.US, "%.0f", effectiveRange)));
        }

        // Add projectile count for shotguns
        if (stats.projectileAmount() > 1) {
            tooltipAdder.accept(Component.translatable("info.jeg.projectiles", stats.projectileAmount()));
        }
    }
}

