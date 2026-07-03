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
import net.minecraft.core.particles.SimpleParticleType;
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
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.block.DynamicLightBlock;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.gun.BallisticProtection;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunHeadshotHelper;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.attachment.AttachmentModifiers;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.Reference;
import net.minecraft.ChatFormatting;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.util.HudMessageHelper;

public class GunItem extends Item {
    private static final ResourceLocation GRENADE_LAUNCHER_ID = Reference.id("grenade_launcher");
    private static final ResourceLocation ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher");
    private static final ResourceLocation FLARE_GUN_ID = Reference.id("flare_gun");
    private static final ResourceLocation SHOTGUN_SHELL_ID = Reference.id("shotgun_shell");
    private static final ResourceLocation HANDMADE_SHELL_ID = Reference.id("handmade_shell");
    private static final ResourceLocation SPECTRE_ROUND_ID = Reference.id("spectre_round");
    private static final ResourceLocation BLAZE_ROUND_ID = Reference.id("blaze_round");
    private static final int ROCKET_LAUNCHER_HOLD_TICKS = 7;
    private static final float GRENADE_BASE_POWER = 4.0F;
    private static final float GRENADE_DAMAGE_FACTOR = 5.0F;
    private static final int FORGE_YELLOW_TRAIL_COLOR = 0xFFFFFF00;
    private static final int GRENADE_FUSE_TICKS = 40;
    private static final double LOW_DURABILITY_JAM_THRESHOLD = 1.75D;
    private static final double INCREASED_JAM_THRESHOLD = 2.25D;
    private static final float JAM_CHANCE = 0.025F;
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
            "holy_shotgun",
            "pump_shotgun",
            "python",
            "repeating_shotgun",
            "revolver",
            "rocket_launcher",
            "supersonic_shotgun",
            "typhoonee"
    );

    static final int RELOAD_STAGE_NONE = 0;
    static final int RELOAD_STAGE_START = 1;
    static final int RELOAD_STAGE_LOOP = 2;
    static final int RELOAD_STAGE_STOP = 3;
    private static final int DEFAULT_DRAW_TICKS = 30;
    private static final double DRAW_OPERATION_LOCK_FRACTION = 0.85D;
    private static final Map<String, Integer> DRAW_ANIMATION_TICKS = Map.ofEntries(
            Map.entry("abstract_gun", 30),
            Map.entry("assault_rifle", 30),
            Map.entry("blossom_rifle", 32),
            Map.entry("bolt_action_rifle", 30),
            Map.entry("burst_rifle", 25),
            Map.entry("combat_pistol", 14),
            Map.entry("combat_rifle", 25),
            Map.entry("compound_bow", 16),
            Map.entry("custom_smg", 30),
            Map.entry("double_barrel_shotgun", 22),
            Map.entry("finger_gun", 26),
            Map.entry("flamethrower", 24),
            Map.entry("flare_gun", 20),
            Map.entry("grenade_launcher", 35),
            Map.entry("hollenfire_mk2", 30),
            Map.entry("holy_shotgun", 25),
            Map.entry("hypersonic_cannon", 17),
            Map.entry("infantry_rifle", 30),
            Map.entry("light_machine_gun", 54),
            Map.entry("minigun", 30),
            Map.entry("phantom_smg", 30),
            Map.entry("primitive_bow", 16),
            Map.entry("pump_shotgun", 25),
            Map.entry("python", 26),
            Map.entry("repeating_shotgun", 34),
            Map.entry("revolver", 26),
            Map.entry("rocket_launcher", 41),
            Map.entry("semi_auto_pistol", 14),
            Map.entry("semi_auto_rifle", 30),
            Map.entry("service_rifle", 30),
            Map.entry("soulhunter_mk2", 30),
            Map.entry("subsonic_rifle", 32),
            Map.entry("supersonic_shotgun", 25),
            Map.entry("typhoonee", 41),
            Map.entry("waterpipe_shotgun", 35)
    );
    private static final int ROCKET_RELOAD_START_TICKS = 46;
    private static final int ROCKET_RELOAD_LOOP_TICKS = 29;
    private static final int ROCKET_RELOAD_STOP_TICKS = 38;
    private static final Map<String, Integer> RELOAD_ANIMATION_MIN_TICKS = Map.ofEntries(
            Map.entry("abstract_gun", 70),
            Map.entry("assault_rifle", 70),
            Map.entry("blossom_rifle", 96),
            Map.entry("burst_rifle", 51),
            Map.entry("combat_pistol", 79),
            Map.entry("combat_rifle", 51),
            Map.entry("custom_smg", 60),
            Map.entry("flamethrower", 205),
            Map.entry("grenade_launcher", 54),
            Map.entry("hollenfire_mk2", 120),
            Map.entry("hypersonic_cannon", 90),
            Map.entry("infantry_rifle", 57),
            Map.entry("light_machine_gun", 130),
            Map.entry("minigun", 43),
            Map.entry("phantom_smg", 60),
            Map.entry("semi_auto_pistol", 79),
            Map.entry("semi_auto_rifle", 60),
            Map.entry("service_rifle", 51),
            Map.entry("soulhunter_mk2", 130),
            Map.entry("subsonic_rifle", 52),
            Map.entry("waterpipe_shotgun", 56)
    );
    private static final Map<String, Integer> RELOAD_START_ANIMATION_MIN_TICKS = Map.ofEntries(
            Map.entry("bolt_action_rifle", 20),
            Map.entry("double_barrel_shotgun", 38),
            Map.entry("flare_gun", 21),
            Map.entry("holy_shotgun", 10),
            Map.entry("pump_shotgun", 10),
            Map.entry("python", 43),
            Map.entry("repeating_shotgun", 12),
            Map.entry("revolver", 43),
            Map.entry("rocket_launcher", ROCKET_RELOAD_START_TICKS),
            Map.entry("supersonic_shotgun", 10),
            Map.entry("typhoonee", ROCKET_RELOAD_START_TICKS)
    );
    private static final Map<String, Integer> RELOAD_STOP_ANIMATION_MIN_TICKS = Map.ofEntries(
            Map.entry("bolt_action_rifle", 22),
            Map.entry("double_barrel_shotgun", 16),
            Map.entry("flare_gun", 15),
            Map.entry("holy_shotgun", 20),
            Map.entry("pump_shotgun", 20),
            Map.entry("python", 15),
            Map.entry("repeating_shotgun", 29),
            Map.entry("revolver", 15),
            Map.entry("rocket_launcher", ROCKET_RELOAD_STOP_TICKS),
            Map.entry("supersonic_shotgun", 20),
            Map.entry("typhoonee", ROCKET_RELOAD_STOP_TICKS)
    );
    private static final int SPREAD_THRESHOLD_MS = 300;
    private static final int SPREAD_MAX_COUNT = 10;
    private static final int WATER_COOL_DURATION_TICKS = 60;
    private static final ResourceLocation WATER_COOL_SOUND_ID = Reference.id("item.cooldown_with_water");
    private static final Map<UUID, SpreadTrackerState> SPREAD_TRACKERS = new WeakHashMap<>();
    private static final Map<UUID, Integer> MINIGUN_PENDING_HEAT_SHOTS = new HashMap<>();
    private static final Map<Integer, Integer> MINIGUN_PENDING_HEAT_NUMERATOR = new HashMap<>();
    private static final Map<Integer, Integer> OVERHEAT_PENDING_NUMERATOR = new HashMap<>();
    private static final Map<Integer, Integer> OVERHEAT_COOL_NUMERATOR = new HashMap<>();
    private static final Map<UUID, PendingReload> PENDING_RELOADS = new HashMap<>();
    private static final Map<UUID, QueuedReloadCancelDraw> SERVER_RELOAD_CANCEL_DRAW_STATES = new HashMap<>();
    private static final Map<UUID, QueuedReloadCancelDraw> CLIENT_RELOAD_CANCEL_DRAW_STATES = new HashMap<>();
    private static final Map<UUID, HeldGunState> HELD_DRAW_STATES = new HashMap<>();
    private static final Map<UUID, HeldGunState> CLIENT_HELD_DRAW_STATES = new HashMap<>();
    private static final Map<UUID, HeldGunState> CLIENT_RELOAD_VISUAL_STATES = new HashMap<>();
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
    private static final Set<String> FORGE_YELLOW_TRAIL_IDS = Set.of(
            "abstract_gun",
            "finger_gun",
            "revolver",
            "waterpipe_shotgun",
            "custom_smg",
            "double_barrel_shotgun",
            "semi_auto_pistol",
            "semi_auto_rifle",
            "assault_rifle",
            "pump_shotgun",
            "combat_pistol",
            "burst_rifle",
            "combat_rifle",
            "bolt_action_rifle",
            "bubble_cannon",
            "repeating_shotgun",
            "infantry_rifle",
            "service_rifle",
            "hollenfire_mk2",
            "subsonic_rifle",
            "supersonic_shotgun",
            "hypersonic_cannon",
            "rocket_launcher",
            "grenade_launcher",
            "light_machine_gun",
            "minigun"
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
    private static final float BOLT_ACTION_PLAYER_HIP_SPREAD = 8.0F;
    private static final float BOLT_ACTION_GUNNER_SPREAD_MULTIPLIER = 1.20F;
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

    private record MagazineInventoryScan(int loadedMagazineCount, int emptyMagazineCount, int bestMagazineSlot, int bestMagazineCapacity) {}

    private record PendingReload(
            InteractionHand hand,
            int selectedSlot,
            ItemStack stack,
            ItemStack stackSnapshot,
            int magazineSlot,
            @Nullable ResourceLocation magazineItemId,
            @Nullable ResourceLocation magazineAmmoId,
            int magazineAmmoCount
    ) {}

    private record PendingMagazineSwap(
            int slot,
            ResourceLocation magazineItemId,
            ResourceLocation ammoItemId,
            int ammoCount,
            int magazineCapacity
    ) {}

    private record HeldGunState(InteractionHand hand, int selectedSlot, int inventorySlot, int stackIdentity, ItemStack stackSnapshot) {
        private boolean matchesStack(ItemStack stack, int slot) {
            return this.inventorySlot == slot
                    && (this.stackIdentity == System.identityHashCode(stack)
                    || isSameStackIgnoringAnimationState(stack, this.stackSnapshot));
        }
    }

    private record QueuedReloadCancelDraw(int inventorySlot, int stackIdentity, ItemStack stackSnapshot) {
        private boolean matchesStack(ItemStack stack, int slot) {
            return this.inventorySlot == slot
                    && (this.stackIdentity == System.identityHashCode(stack)
                    || isSameStackIgnoringAnimationState(stack, this.stackSnapshot));
        }
    }

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
        return repair.is(ModItems.REPAIR_KIT.get());
    }

    public GunStats getStats() {
        return this.stats;
    }

    public int magazineSize() {
        return this.stats.magazineSize();
    }

    public int magazineSize(ItemStack stack) {
        return modifiedMagazineSize(stack);
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
        return "minigun".equals(path) || "repeating_shotgun".equals(path);
    }

    public boolean usesMagazineSwapReload() {
        return Config.magazineFeedEnabled()
                && usesLoadedAmmo()
                && (isThirtyRoundRifle() || isCustomSmg() || isMagazineSwapPistol() || isMagazineSwapShotgun() || isMachineGunMagazineSwap());
    }

    public boolean usesMagazineSwapReload(ItemStack stack) {
        return usesMagazineSwapReload();
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
        int maxAmmo = modifiedMagazineSize(stack);
        return Mth.clamp(stack.getOrDefault(ModDataComponents.GUN_AMMO.get(), maxAmmo), 0, maxAmmo);
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

    public static boolean isBulletClassWeapon(ResourceLocation gunId) {
        return !NON_BULLET_TRAIL_IDS.contains(gunId.getPath());
    }

    public static boolean isRocketLauncher(ItemStack stack) {
        return stack.getItem() instanceof GunItem gun && isRocketLauncher(gun.getStats().id());
    }

    public static boolean isRocketLauncher(ResourceLocation gunId) {
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

    public static boolean isOperationLocked(ItemStack stack) {
        return isReloading(stack) || isDrawOperationLocked(stack);
    }

    public static boolean isReloading(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0) > 0;
    }

    public static boolean isDrawing(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0) > 0;
    }

    public static boolean isDrawOperationLocked(ItemStack stack) {
        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            return false;
        }
        int totalTicks = getDrawAnimationTicks(stack);
        int elapsedTicks = Math.max(0, totalTicks - remainingTicks);
        return elapsedTicks < getDrawOperationLockTicks(totalTicks);
    }

    static int getDrawAnimationTicks(ItemStack stack) {
        if (stack.getItem() instanceof GunItem gun) {
            return DRAW_ANIMATION_TICKS.getOrDefault(gun.getStats().id().getPath(), DEFAULT_DRAW_TICKS);
        }
        return DEFAULT_DRAW_TICKS;
    }

    private static int getDrawOperationLockTicks(int totalTicks) {
        return Math.max(1, (int) Math.ceil(totalTicks * DRAW_OPERATION_LOCK_FRACTION));
    }

    static int getDrawOperationLockTicks(ItemStack stack) {
        return getDrawOperationLockTicks(getDrawAnimationTicks(stack));
    }

    public static void tickPendingReloads(Player player) {
        PendingReload pending = PENDING_RELOADS.get(player.getUUID());
        if (pending == null) {
            return;
        }

        ItemStack current = player.getItemInHand(pending.hand());
        if (pending.hand() == InteractionHand.MAIN_HAND && player.getInventory().selected != pending.selectedSlot()) {
            cancelPendingReloadForSwitch(player, pending, current, "selectedSlotChanged currentSlot=" + player.getInventory().selected);
            return;
        }
        if (current.isEmpty() || !isSameStackIgnoringAnimationState(current, pending.stackSnapshot())) {
            cancelPendingReloadForSwitch(player, pending, current, "stackMismatch");
        }
    }

    private static void cancelPendingReloadForSwitch(Player player, PendingReload pending, ItemStack current, String reason) {
        PENDING_RELOADS.remove(player.getUUID());
        SERVER_RELOAD_CANCEL_DRAW_STATES.remove(player.getUUID());
        if (!pending.stack().isEmpty()) {
            clearReloadVisualState(pending.stack());
            queueDrawAfterReloadCancel(player, pending.stack(), pending.selectedSlot(), true);
        }
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged && newStack.getItem() instanceof GunItem) {
            return false;
        }
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
        if (usesLoadedAmmo()) {
            stack.set(ModDataComponents.GUN_AMMO.get(), Mth.clamp(value, 0, modifiedMagazineSize(stack)));
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
            case "minigun" -> Math.round(OVERHEAT_MAX * (enhanced ? 0.90F : 0.60F));
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

    private static int getHeatDenominatorPerShot(ResourceLocation gunId) {
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

        if (isOperationLocked(stack)) {
            return false;
        }
        clearDrawState(stack);

        if (!automatic && isTriggerLocked(stack)) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }

        if (usesOverheatMechanic() && isOverheated(stack)) {
            if (level.isClientSide()) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.overheated"));
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
            ResourceLocation gunId = stats.id();
            // Removed custom trail rendering - rely on server-sent particles instead
            // which have proper depth testing and don't render through blocks
        } else {
            if (shouldJamBeforeShot(level, player, stack)) {
                return false;
            }
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
                if (Config.gunDurabilityEnabled()) {
                    stack.hurtAndBreak(durabilityDamagePerShot(stack), player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    GunAttachments.damageOnShot(stack, level, player);
                }
                shotsFired++;
            }
            if (shotsFired <= 0) {
                return false;
            }

            if (usesOverheatMechanic()) {
                addOverheatForShots(stack, stats.id(), shotsFired, player);
            }

            if (stack.getItem() instanceof AnimatedGunItem animated) {
                animated.triggerShoot(level, player, stack);
            }

            applyRecoilBackstep(player, stack);

            if (!automatic) {
                setTriggerLocked(stack, true);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(stack.getItem(), Math.max(1, stats.fireDelay()));
        }

        playFireSound(level, player, stack, fireSoundFor(stack));
        playAttachmentFireSounds(level, player, stack);
        return true;
    }

    private Optional<SoundEvent> fireSoundFor(ItemStack stack) {
        if (GunAttachments.modifiers(stack).silenced()) {
            return stats.silencedFireSoundEvent().or(stats::fireSoundEvent).or(stats::enchantedFireSoundEvent);
        }
        return stats.fireSoundEvent().or(stats::enchantedFireSoundEvent);
    }

    private void playAttachmentFireSounds(Level level, LivingEntity shooter, ItemStack stack) {
        GunAttachments.id(stack, AttachmentType.BARREL)
                .map(ResourceLocation::getPath)
                .ifPresent(path -> {
                    if ("trumpet".equals(path)) {
                        playSound(level, shooter, Optional.ofNullable(resolveSound(Reference.id("item.doot"))));
                        if (stats.projectileAmount() > 3) {
                            applyTrumpetSoundwave(level, shooter);
                        }
                    } else if ("explosive_muzzle".equals(path)) {
                        playSound(level, shooter, Optional.of(SoundEvents.FIRECHARGE_USE));
                    }
                });
    }

    private void applyTrumpetSoundwave(Level level, LivingEntity shooter) {
        if (level.isClientSide()) {
            return;
        }

        Vec3 look = shooter.getLookAngle();
        Vec3 origin = shooter.position();
        double attackRange = 8.0D;
        double maxDistance = 10.0D;
        double sweepAngle = Math.toRadians(100.0D);
        double pushStrength = 2.0D;

        shooter.push(-look.x, -look.y, -look.z);
        shooter.fallDistance = 0.0F;
        ServerLevel serverLevel = (ServerLevel) level;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, shooter.getBoundingBox().inflate(attackRange))) {
            if (entity == shooter) {
                continue;
            }

            Vec3 entityOffset = entity.position().subtract(origin);
            double distance = entityOffset.length();
            if (!isInsideSoundwaveCone(entityOffset, look, sweepAngle)) {
                continue;
            }

            double distanceMultiplier = 1.0D - Math.min(distance / maxDistance, 1.0D);
            entity.push(
                    look.x * pushStrength * distanceMultiplier,
                    look.y * pushStrength * distanceMultiplier,
                    look.z * pushStrength * distanceMultiplier
            );
        }

        Vec3 particlePos = shooter.getEyePosition().add(look.scale(1.8D));
        emitTrumpetSoundwaveParticles(serverLevel, particlePos, look);
        serverLevel.sendParticles(
                ParticleTypes.SONIC_BOOM,
                particlePos.x,
                particlePos.y,
                particlePos.z,
                1,
                look.x,
                look.y,
                look.z,
                0.0D
        );
    }

    private static void emitTrumpetSoundwaveParticles(ServerLevel level, Vec3 particlePos, Vec3 look) {
        level.sendParticles(
                ModParticleTypes.BIG_SONIC_RING.get(),
                particlePos.x,
                particlePos.y,
                particlePos.z,
                0,
                look.x * 0.9D,
                look.y * 0.9D,
                look.z * 0.9D,
                0.0D
        );
        level.sendParticles(
                ModParticleTypes.BIG_SONIC_RING.get(),
                particlePos.x,
                particlePos.y,
                particlePos.z,
                0,
                look.x * 0.45D,
                look.y * 0.45D,
                look.z * 0.45D,
                0.0D
        );
        level.sendParticles(
                ModParticleTypes.BIG_SONIC_RING.get(),
                particlePos.x,
                particlePos.y,
                particlePos.z,
                0,
                look.x * 1.2D,
                look.y * 1.2D,
                look.z * 1.2D,
                0.0D
        );
        level.sendParticles(
                ModParticleTypes.SONIC_RING.get(),
                particlePos.x,
                particlePos.y,
                particlePos.z,
                5,
                0.0D,
                0.0D,
                0.0D,
                0.2D
        );
        level.sendParticles(
                ModParticleTypes.BIG_SONIC_RING.get(),
                particlePos.x,
                particlePos.y,
                particlePos.z,
                2,
                0.0D,
                0.0D,
                0.0D,
                0.1D
        );
    }

    private static boolean isInsideSoundwaveCone(Vec3 offset, Vec3 look, double sweepAngle) {
        if (offset.lengthSqr() < 1.0E-6D) {
            return false;
        }
        return Math.acos(offset.normalize().dot(look.normalize())) < sweepAngle * 0.5D;
    }

    private int durabilityDamagePerShot(ItemStack stack) {
        return GunAttachments.modifiers(stack).explosiveAmmo() ? 5 : 1;
    }

    private boolean shouldJamBeforeShot(Level level, Player player, ItemStack stack) {
        if (!Config.gunDurabilityEnabled() || !stack.isDamageableItem()) {
            return false;
        }

        int damageAmount = durabilityDamagePerShot(stack);
        if (stack.getDamageValue() >= stack.getMaxDamage() - damageAmount) {
            level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(stack.getItem(), Math.max(1, stats.fireDelay()));
            return true;
        }

        if (!Config.gunJammingEnabled()) {
            return false;
        }

        double threshold = GunAttachments.modifiers(stack).increasedJamming()
                ? INCREASED_JAM_THRESHOLD
                : LOW_DURABILITY_JAM_THRESHOLD;
        if (stack.getDamageValue() < stack.getMaxDamage() / threshold) {
            return false;
        }
        if (player.getRandom().nextFloat() >= JAM_CHANCE) {
            return false;
        }

        playSound(level, player, Optional.ofNullable(resolveSound(Reference.id("item.pistol.cock"))), 1.0F);
        player.displayClientMessage(Component.translatable("chat.jeg.jam").withStyle(ChatFormatting.GRAY), true);
        player.getCooldowns().addCooldown(stack.getItem(), Mth.clamp(stats.fireDelay() * 10, 1, 60));
        return true;
    }

    @Nullable
    private SoundEvent resolveSound(ResourceLocation soundId) {
        var holder = ModSounds.ALL.get(soundId);
        return holder != null ? holder.get() : null;
    }

    private void applyRecoilBackstep(Player player, ItemStack stack) {
        if (!Config.recoilBackstepEnabled()) {
            return;
        }
        if (!isHeavyBackstepWeapon(stats.id())) {
            return;
        }

        AttachmentModifiers modifiers = GunAttachments.modifiers(stack);
        double force = stats.recoilKick() * modifiers.kickMultiplier() * RecoilProfiles.multiplier(stats.id()) * 0.20D;
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
        AttachmentModifiers modifiers = GunAttachments.modifiers(stack);
        float damage = modifiedDamage(stats, modifiers);

        boolean grenadeLauncher = gunId.equals(GRENADE_LAUNCHER_ID);
        float grenadePower = grenadeLauncher ? GRENADE_BASE_POWER : Math.max(1.8F, damage / 12.0F + 1.5F);
        int fuseTicks = grenadeLauncher ? GRENADE_FUSE_TICKS : 40;
        Vec3 shooterMotion = shooter.getDeltaMovement();
        if (level instanceof ServerLevel serverLevel && !(shooter instanceof ttv.migami.jeg.entity.monster.phantom.PhantomGunner)) {
            NetworkHandler.sendGunFireFx(serverLevel, shooter.getId(), random.nextFloat());
            refreshGunfireLight(serverLevel, shooter, stack);
            ejectCasing(serverLevel, shooter);
        }

        for (int i = 0; i < pellets; i++) {
            Vec3 direction = computeDirection(shooter, origin, target, random, stats, stack);
            Vec3 muzzle = origin.add(direction.scale(0.35F));

            if (grenadeLauncher) {
                GrenadeEntity grenade = new GrenadeEntity(level, shooter, grenadePower, fuseTicks, true);
                grenade.initialisePosition(muzzle);
                Vec3 launchVelocity = direction.scale(Math.max(1.2F, stats.projectileSpeed() * 0.9F)).add(shooterMotion);
                grenade.setDeltaMovement(launchVelocity);
                level.addFreshEntity(grenade);
            } else {
                Vec3 velocity = direction.scale(stats.projectileSpeed());
                BulletEntity bullet = createBullet(level, shooter, stack, stats, velocity);
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
        AttachmentModifiers modifiers = GunAttachments.modifiers(stack);
        float damage = modifiedDamage(stats, modifiers);

        boolean grenadeLauncher = gunId.equals(GRENADE_LAUNCHER_ID);
        float grenadePower = grenadeLauncher ? GRENADE_BASE_POWER : Math.max(1.8F, damage / 12.0F + 1.5F);
        int fuseTicks = grenadeLauncher ? GRENADE_FUSE_TICKS : 40;
        Vec3 shooterMotion = shooter.getDeltaMovement();
        Vec3 normalized = direction.normalize();
        if (level instanceof ServerLevel serverLevel && !(shooter instanceof ttv.migami.jeg.entity.monster.phantom.PhantomGunner)) {
            NetworkHandler.sendGunFireFx(serverLevel, shooter.getId(), shooter.getRandom().nextFloat());
            refreshGunfireLight(serverLevel, shooter, stack);
            ejectCasing(serverLevel, shooter);
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
                BulletEntity bullet = createBullet(level, shooter, stack, stats, velocity);
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

    private void refreshGunfireLight(ServerLevel level, LivingEntity shooter, ItemStack stack) {
        if (!(shooter instanceof Player) || shouldSkipGunfireLight(stack)) {
            return;
        }

        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle();
        BlockPos[] candidates = new BlockPos[] {
                BlockPos.containing(eye.add(look.scale(0.45D))),
                BlockPos.containing(eye),
                BlockPos.containing(eye.add(0.0D, 0.35D, 0.0D))
        };

        for (BlockPos pos : candidates) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.DYNAMIC_LIGHT.get())) {
                DynamicLightBlock.setDelay(level, pos, 0.5D);
                return;
            }
            if (state.isAir()) {
                level.setBlock(pos, ModBlocks.DYNAMIC_LIGHT.get().defaultBlockState(), 3);
                DynamicLightBlock.setDelay(level, pos, 0.5D);
                return;
            }
            if (state.is(Blocks.WATER)) {
                BlockState dynamicLight = ModBlocks.DYNAMIC_LIGHT.get()
                        .defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, true);
                level.setBlock(pos, dynamicLight, 3);
                DynamicLightBlock.setDelay(level, pos, 0.5D);
                return;
            }
        }
    }

    private boolean shouldSkipGunfireLight(ItemStack stack) {
        if (GunAttachments.modifiers(stack).silenced()) {
            return true;
        }
        String path = stats.id().getPath();
        return "finger_gun".equals(path)
                || "typhoonee".equals(path)
                || "atlantean_spear".equals(path)
                || path.endsWith("bow")
                || path.endsWith("blowpipe");
    }

    private void ejectCasing(ServerLevel level, LivingEntity shooter) {
        SimpleParticleType particle = casingParticle();
        if (particle == null) {
            return;
        }

        Vec3 look = shooter.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 1.0E-6D) {
            forward = Vec3.ZERO;
        } else {
            forward = forward.normalize();
        }

        double divisor = shooter instanceof Player player && NetworkHandler.isAiming(player) ? 0.4D : 0.5D;
        Vec3 particlePos = shooter.position()
                .add(right.scale(divisor))
                .add(forward.scale(divisor))
                .add(0.0D, shooter.getEyeHeight() - 0.4D, 0.0D);
        Vec3 velocity = right.scale(0.05D).add(0.0D, 0.02D, 0.0D);

        level.sendParticles(
                particle,
                particlePos.x,
                particlePos.y,
                particlePos.z,
                0,
                velocity.x,
                velocity.y,
                velocity.z,
                1.0D
        );
    }

    @Nullable
    private SimpleParticleType casingParticle() {
        ResourceLocation ammo = stats.ammoItem();
        if (ammo == null) {
            return null;
        }

        String path = stats.id().getPath();
        if ("finger_gun".equals(path)
                || "typhoonee".equals(path)
                || "atlantean_spear".equals(path)
                || path.endsWith("bow")
                || path.endsWith("blowpipe")) {
            return null;
        }

        if (SPECTRE_ROUND_ID.equals(ammo) || BLAZE_ROUND_ID.equals(ammo)) {
            return ModParticleTypes.SPECTRE_CASING_PARTICLE.get();
        }
        if (SHOTGUN_SHELL_ID.equals(ammo) || HANDMADE_SHELL_ID.equals(ammo) || "grenade".equals(ammo.getPath()) || "flare".equals(ammo.getPath())) {
            return ModParticleTypes.SHELL_PARTICLE.get();
        }
        if ("fire_charge".equals(ammo.getPath())) {
            return null;
        }
        return ModParticleTypes.CASING_PARTICLE.get();
    }

    public static BulletEntity createBullet(Level level, LivingEntity shooter, ItemStack stack, GunStats stats, Vec3 velocity) {
        AttachmentModifiers modifiers = GunAttachments.modifiers(stack);
        BulletEntity bullet = new BulletEntity(
                level,
                shooter,
                stats,
                velocity,
                modifiedDamage(stats, modifiers),
                modifiers.explosiveAmmo()
        );
        GunAttachments.id(stack, AttachmentType.KILL_EFFECT).ifPresent(bullet::setKillEffect);
        bullet.setMedalsEnabled(GunAttachments.areMedalsEnabled(stack));
        if (shooter instanceof Player player && !player.isCreative()
                && stack.getItem() instanceof GunItem gun && gun.usesLoadedAmmo()) {
            bullet.setJustEnoughAmmoMedal(gun.getAmmo(stack) < 1);
        }
        applyFlareDye(stack, stats, bullet);
        applyForgeTrailColor(stats, bullet);
        return bullet;
    }

    private static void applyForgeTrailColor(GunStats stats, BulletEntity bullet) {
        if (FORGE_YELLOW_TRAIL_IDS.contains(stats.id().getPath())) {
            bullet.setTrailColor(FORGE_YELLOW_TRAIL_COLOR);
        }
    }

    private static void applyFlareDye(ItemStack stack, GunStats stats, BulletEntity bullet) {
        if (!FLARE_GUN_ID.equals(stats.id())) {
            return;
        }
        GunAttachments.cosmeticItem(stack, AttachmentType.DYE)
                .filter(DyeItem.class::isInstance)
                .map(DyeItem.class::cast)
                .map(dye -> dye.getDyeColor().getFireworkColor())
                .ifPresent(bullet::setFlareColor);
    }

    private Vec3 computeDirection(LivingEntity shooter, Vec3 origin, @Nullable LivingEntity target, RandomSource random, GunStats stats, ItemStack stack) {
        Vec3 base = target != null
                ? target.getEyePosition().subtract(origin)
                : shooter.getViewVector(1.0F);
        return applyLegacySpread(shooter, base, stats, random, stack);
    }

    private static Vec3 applyLegacySpread(LivingEntity shooter, Vec3 baseDirection, GunStats stats, RandomSource random, ItemStack stack) {
        Vec3 forwards = baseDirection.normalize();
        if (forwards.lengthSqr() < 1.0E-6D) {
            forwards = shooter.getViewVector(1.0F);
        }

        float baseSpread = modifiedSpread(stats, stack);
        float gunSpread = baseSpread;
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
                gunSpread += getMovementSpreadDegrees(player, stats, NetworkHandler.isAiming(player), baseSpread);
            } else {
                gunSpread = Math.max(gunSpread, baseSpread * MINIGUN_SPREAD_FLOOR);
            }
            if (isBoltActionRifle(stats.id()) && !NetworkHandler.isAiming(player)) {
                gunSpread = Math.max(gunSpread, BOLT_ACTION_PLAYER_HIP_SPREAD);
            }
            if (isShotgun(stats.id())) {
                float shotgunFloor = baseSpread * (NetworkHandler.isAiming(player) ? 0.35F : 0.60F);
                gunSpread = Math.max(gunSpread, shotgunFloor);
            }
        } else if (isShotgun(stats.id())) {
            gunSpread = baseSpread * 0.60F;
        } else {
            float earlySpreadMultiplier = shooter.level().getDifficulty() != Difficulty.HARD ? 10.0F : 5.0F;
            float scaledSpreadMultiplier = Config.scaleGunnerSpreadMultiplier(shooter.level(), earlySpreadMultiplier);
            gunSpread *= scaledSpreadMultiplier;
            if (isBoltActionRifle(stats.id())) {
                gunSpread *= BOLT_ACTION_GUNNER_SPREAD_MULTIPLIER;
            }
        }

        if (gunSpread <= 0.0F) {
            return forwards.normalize();
        }

        float spreadRadians = Math.min(gunSpread, 170.0F) * Mth.DEG_TO_RAD;
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

    public static void recordClientShotSpread(Player player, GunStats stats) {
        updateSpreadTracker(player, stats.id());
    }

    private static float getSpreadMultiplier(Player player, GunStats stats) {
        SpreadTrackerState playerState = SPREAD_TRACKERS.get(player.getUUID());
        ResourceLocation gunId = stats.id();
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
        return getClientSpreadDegrees(player, ItemStack.EMPTY, stats, aiming);
    }

    public static float getClientSpreadDegrees(Player player, ItemStack stack, GunStats stats, boolean aiming) {
        float baseSpread = modifiedSpread(stats, stack);
        float gunSpread = baseSpread;
        if (gunSpread <= 0.0F) {
            return 0.0F;
        }

        boolean minigun = isMinigunWeapon(stats.id());
        gunSpread *= getSpreadMultiplier(player, stats);
        if (!minigun && aiming) {
            gunSpread *= 0.5F;
        }
        if (!minigun) {
            gunSpread += getMovementSpreadDegrees(player, stats, aiming, baseSpread);
        } else {
            gunSpread = Math.max(gunSpread, baseSpread * MINIGUN_SPREAD_FLOOR);
        }
        if (isBoltActionRifle(stats.id()) && !aiming) {
            gunSpread = Math.max(gunSpread, BOLT_ACTION_PLAYER_HIP_SPREAD);
        }
        if (isShotgun(stats.id())) {
            float shotgunFloor = baseSpread * (aiming ? 0.35F : 0.60F);
            gunSpread = Math.max(gunSpread, shotgunFloor);
        }
        return Math.max(0.0F, gunSpread);
    }

    public static boolean isShotgunWeapon(ResourceLocation gunId) {
        return SHOTGUN_IDS.contains(gunId.getPath());
    }

    public static boolean isShotgun(ResourceLocation gunId) {
        return isShotgunWeapon(gunId);
    }

    public static boolean hasFlameTrail(ResourceLocation gunId) {
        GunStats stats = GunDefinitions.ALL.get(gunId);
        return stats != null && stats.flameTrail();
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

    private static boolean isBoltActionRifle(ResourceLocation gunId) {
        return "bolt_action_rifle".equals(gunId.getPath());
    }

    private static float getMovementSpreadDegrees(Player player, GunStats stats, boolean aiming, float baseSpread) {
        if (player.isCrouching() || !isPlayerMoving(player)) {
            return 0.0F;
        }

        float multiplier = getMovementSpreadMultiplier(player, stats);
        if (aiming) {
            multiplier *= 0.65F;
        }
        return baseSpread * multiplier;
    }

    private static float modifiedDamage(GunStats stats, AttachmentModifiers modifiers) {
        return Math.max(0.0F, stats.damage() * modifiers.damageMultiplier());
    }

    private static float modifiedSpread(GunStats stats, ItemStack stack) {
        return Math.max(0.0F, stats.spread() * GunAttachments.modifiers(stack).spreadMultiplier());
    }

    private int modifiedMagazineSize(ItemStack stack) {
        int baseCapacity = Math.max(0, stats.magazineSize());
        if (Config.magazineFeedEnabled()) {
            return usesMagazineSwapReload() ? getLoadedMagazineCapacity(stack, baseCapacity) : baseCapacity;
        }

        double multiplier = GunAttachments.modifiers(stack).magazineCapacityMultiplier();
        if (multiplier <= 1.0D) {
            return baseCapacity;
        }
        if ("infantry_rifle".equals(stats.id().getPath())) {
            return GunAttachments.id(stack, AttachmentType.MAGAZINE)
                    .map(ResourceLocation::getPath)
                    .map(path -> switch (path) {
                        case "extended_mag" -> 20;
                        case "drum_mag" -> 40;
                        default -> baseCapacity;
                    })
                    .orElse(baseCapacity);
        }
        return Math.max(baseCapacity, (int) (baseCapacity * multiplier));
    }

    private int getLoadedMagazineCapacity(ItemStack stack, int fallbackCapacity) {
        MagazineItem loadedMagazine = getLoadedMagazineItem(stack);
        if (loadedMagazine != null && isCompatibleMagazine(loadedMagazine)) {
            return loadedMagazine.getCapacity();
        }

        MagazineItem baseMagazine = getCompatibleMagazineItemUnchecked();
        return baseMagazine != null ? baseMagazine.getCapacity() : fallbackCapacity;
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
        final Map<ResourceLocation, SpreadEntry> byGun = new WeakHashMap<>();
    }

    private static final class SpreadEntry {
        long lastFireMs = -1L;
        int spreadCount;
    }

    private void playSound(Level level, LivingEntity shooter, Optional<SoundEvent> sound) {
        playSound(level, shooter, sound, 7.5F);
    }

    private void playFireSound(Level level, LivingEntity shooter, ItemStack stack, Optional<SoundEvent> sound) {
        float volume = (float) (7.5D * GunAttachments.modifiers(stack).fireSoundRadiusMultiplier());
        playSound(level, shooter, sound, Math.max(0.0F, volume));
    }

    private void playSound(Level level, LivingEntity shooter, Optional<SoundEvent> sound, float volume) {
        SoundSource source = shooter instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
        double x = shooter.getX();
        double y = shooter.getY();
        double z = shooter.getZ();
        sound.ifPresentOrElse(
                value -> level.playSound(null, x, y, z, value, source, volume, 1.0F),
                () -> level.playSound(null, x, y, z, SoundEvents.CROSSBOW_SHOOT, source, volume, 1.1F)
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
        InteractionHand hand = stack == player.getOffhandItem() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return tryReload(level, player, hand, notify);
    }

    public boolean tryReload(Level level, Player player, InteractionHand hand, boolean notify) {
        ItemStack stack = player.getItemInHand(hand);
        if (!usesLoadedAmmo()) {
            return false;
        }
        if (isOperationLocked(stack) || PENDING_RELOADS.containsKey(player.getUUID())) {
            return false;
        }
        clearDrawState(stack);

        if (usesMagazineSwapReload(stack)) {
            return tryStartReloadWithMagazineSwap(level, player, hand, stack, notify);
        }
        return tryStartReloadWithLooseAmmo(level, player, hand, stack, notify);
    }

    private boolean tryStartReloadWithMagazineSwap(Level level, Player player, InteractionHand hand, ItemStack stack, boolean notify) {
        ensureAmmoInitialized(stack);
        int ammo = getAmmo(stack);
        int maxAmmo = modifiedMagazineSize(stack);

        if (player.getAbilities().instabuild) {
            if (ammo >= maxAmmo) {
                if (notify) {
                    HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.magazine_full"));
                }
                return false;
            }
            return startPendingReload(level, player, hand, stack, null);
        }

        PendingMagazineSwap selectedMagazine = findReloadMagazine(player, stack);
        if (selectedMagazine == null) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.no_compatible_magazine"));
            }
            return false;
        }

        if (ammo >= maxAmmo && isSameLoadedMagazineType(stack, selectedMagazine.magazineItemId())) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.magazine_full"));
            }
            return false;
        }

        return startPendingReload(level, player, hand, stack, selectedMagazine);
    }

    private boolean completeReloadWithMagazineSwap(Level level, Player player, ItemStack stack, @Nullable PendingReload pending) {
        ensureAmmoInitialized(stack);
        int maxAmmo = modifiedMagazineSize(stack);
        if (player.getAbilities().instabuild) {
            setAmmo(stack, maxAmmo);
            return true;
        }

        if (pending == null || pending.magazineSlot() < 0 || pending.magazineItemId() == null || pending.magazineAmmoId() == null || pending.magazineAmmoCount() <= 0) {
            return false;
        }

        ItemStack magazineStack = player.getInventory().getItem(pending.magazineSlot());
        if (!(magazineStack.getItem() instanceof MagazineItem magazine)) {
            return false;
        }

        ResourceLocation magazineItemId = BuiltInRegistries.ITEM.getKey(magazineStack.getItem());
        if (!pending.magazineItemId().equals(magazineItemId) || !isCompatibleMagazine(magazine)) {
            return false;
        }

        ResourceLocation ammoId = magazine.getAmmoItemId(magazineStack);
        if (!pending.magazineAmmoId().equals(ammoId)) {
            return false;
        }

        int newAmmo = magazine.getAmmoCount(magazineStack);
        if (newAmmo != pending.magazineAmmoCount() || newAmmo <= 0) {
            return false;
        }

        int ammo = getAmmo(stack);
        ItemStack oldMagazine = createStoredMagazineStack(stack, ammo);
        int oldMagazineCapacity = oldMagazine.getItem() instanceof MagazineItem oldMagazineItem ? oldMagazineItem.getCapacity() : 0;
        magazineStack.shrink(1);
        if (magazineStack.isEmpty()) {
            player.getInventory().setItem(pending.magazineSlot(), ItemStack.EMPTY);
        }

        returnStoredMagazine(player, oldMagazine);
        returnExcessStoredAmmo(player, Math.max(0, ammo - oldMagazineCapacity));
        setLoadedMagazineItem(stack, magazineItemId);
        setAmmo(stack, newAmmo);
        return true;
    }

    private boolean tryStartReloadWithLooseAmmo(Level level, Player player, InteractionHand hand, ItemStack stack, boolean notify) {
        ensureAmmoInitialized(stack);
        int ammo = getAmmo(stack);
        int maxAmmo = modifiedMagazineSize(stack);
        if (ammo >= maxAmmo) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.magazine_full"));
            }
            return false;
        }

        int needed = maxAmmo - ammo;
        int available = player.getAbilities().instabuild ? needed : countInventoryAmmo(player);
        if (available <= 0) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.gun.no_ammo"));
            }
            return false;
        }

        return startPendingReload(level, player, hand, stack, null);
    }

    private boolean completeReloadWithLooseAmmo(Player player, ItemStack stack) {
        ensureAmmoInitialized(stack);
        int ammo = getAmmo(stack);
        int maxAmmo = modifiedMagazineSize(stack);
        if (ammo >= maxAmmo) {
            return false;
        }

        int needed = maxAmmo - ammo;
        int pulled = player.getAbilities().instabuild ? needed : removeAmmoFromInventory(player, needed);
        if (pulled <= 0) {
            return false;
        }

        setAmmo(stack, ammo + pulled);
        return true;
    }

    private boolean startPendingReload(Level level, Player player, InteractionHand hand, ItemStack stack, @Nullable PendingMagazineSwap pendingMagazine) {
        int reloadTicks = Math.max(1, stats.totalReloadTime());
        playSound(level, player, stats.reloadStartSoundEvent());
        PENDING_RELOADS.put(player.getUUID(), new PendingReload(
                hand,
                player.getInventory().selected,
                stack,
                stack.copy(),
                pendingMagazine != null ? pendingMagazine.slot() : -1,
                pendingMagazine != null ? pendingMagazine.magazineItemId() : null,
                pendingMagazine != null ? pendingMagazine.ammoItemId() : null,
                pendingMagazine != null ? pendingMagazine.ammoCount() : 0
        ));

        if (stack.getItem() instanceof AnimatedGunItem animated) {
            startReloadVisualState(stack, reloadTicks, pendingMagazine);
            if ("rocket_launcher".equals(stats.id().getPath())) {
                return true;
            }
            if (usesSegmentedReloadAnimation()) {
                animated.triggerReloadStart(level, player, stack);
            } else {
                animated.triggerReload(level, player, stack);
            }
        }
        return true;
    }

    private MagazineInventoryScan scanCompatibleMagazines(Player player, ItemStack stack) {
        if (!usesMagazineSwapReload(stack)) {
            return new MagazineInventoryScan(0, 0, -1, 0);
        }
        return scanCompatibleMagazines(player);
    }

    private MagazineInventoryScan scanCompatibleMagazines(Player player) {
        MagazineItem.MagazineType compatibleType = getCompatibleMagazineType();
        ResourceLocation ammoId = getCompatibleAmmoId();
        if (compatibleType == null || ammoId == null) {
            return new MagazineInventoryScan(0, 0, -1, 0);
        }

        int loadedCount = 0;
        int emptyCount = 0;
        int bestSlot = -1;
        int bestAmmoCount = -1;
        int bestMagazineCapacity = 0;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!(candidate.getItem() instanceof MagazineItem magazine) || !magazine.type().isVariantOf(compatibleType)) {
                continue;
            }

            int ammoCount = magazine.getAmmoCount(candidate);
            if (ammoCount <= 0) {
                emptyCount++;
                continue;
            }

            ResourceLocation storedAmmoId = magazine.getAmmoItemId(candidate);
            if (!ammoId.equals(storedAmmoId)) {
                continue;
            }

            loadedCount++;
            if (ammoCount > bestAmmoCount) {
                bestAmmoCount = ammoCount;
                bestSlot = slot;
                bestMagazineCapacity = magazine.getCapacity();
            }
        }

        return new MagazineInventoryScan(loadedCount, emptyCount, bestSlot, bestMagazineCapacity);
    }

    @Nullable
    private PendingMagazineSwap findReloadMagazine(Player player, ItemStack stack) {
        if (!usesMagazineSwapReload(stack)) {
            return null;
        }

        MagazineItem.MagazineType compatibleType = getCompatibleMagazineType();
        ResourceLocation ammoId = getCompatibleAmmoId();
        if (compatibleType == null || ammoId == null) {
            return null;
        }

        boolean preferDifferentType = getAmmo(stack) >= modifiedMagazineSize(stack);
        PendingMagazineSwap best = null;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!(candidate.getItem() instanceof MagazineItem magazine) || !magazine.type().isVariantOf(compatibleType)) {
                continue;
            }

            int ammoCount = magazine.getAmmoCount(candidate);
            if (ammoCount <= 0 || !ammoId.equals(magazine.getAmmoItemId(candidate))) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(candidate.getItem());
            PendingMagazineSwap swap = new PendingMagazineSwap(slot, itemId, ammoId, ammoCount, magazine.getCapacity());
            boolean swapDifferentType = !isSameLoadedMagazineType(stack, swap.magazineItemId());
            boolean bestDifferentType = best != null && !isSameLoadedMagazineType(stack, best.magazineItemId());
            if (best == null
                    || (preferDifferentType && swapDifferentType && !bestDifferentType)
                    || (swapDifferentType == bestDifferentType && swap.ammoCount() > best.ammoCount())) {
                best = swap;
            }
        }
        return best;
    }

    @Nullable
    private MagazineItem.MagazineType getCompatibleMagazineType() {
        if (!usesMagazineSwapReload()) {
            return null;
        }
        return getCompatibleMagazineTypeUnchecked();
    }

    @Nullable
    private MagazineItem getCompatibleMagazineItemUnchecked() {
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
    private MagazineItem.MagazineType getCompatibleMagazineTypeUnchecked() {
        MagazineItem magazine = getCompatibleMagazineItemUnchecked();
        return magazine != null ? magazine.type() : null;
    }

    private boolean isCompatibleMagazine(MagazineItem magazine) {
        MagazineItem.MagazineType type = getCompatibleMagazineTypeUnchecked();
        return type != null && magazine.type().isVariantOf(type);
    }

    @Nullable
    private ResourceLocation getCompatibleAmmoId() {
        ResourceLocation ammoId = stats.ammoItem();
        if (ammoId == null || ammoId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "air"))) {
            return null;
        }
        return ammoId;
    }

    private ItemStack createStoredMagazineStack(ItemStack gunStack, int ammoCount) {
        MagazineItem compatibleMagazine = getLoadedMagazineItem(gunStack);
        if (compatibleMagazine == null || !isCompatibleMagazine(compatibleMagazine)) {
            compatibleMagazine = getCompatibleMagazineItemUnchecked();
        }
        if (compatibleMagazine == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(compatibleMagazine);
        ResourceLocation ammoId = getCompatibleAmmoId();
        if (ammoId != null) {
            stack.set(ModDataComponents.MAGAZINE_AMMO_ITEM.get(), ammoId.toString());
        }
        stack.set(ModDataComponents.MAGAZINE_AMMO_COUNT.get(), Mth.clamp(ammoCount, 0, compatibleMagazine.getCapacity()));
        return stack;
    }

    @Nullable
    private MagazineItem getLoadedMagazineItem(ItemStack gunStack) {
        String stored = gunStack.get(ModDataComponents.GUN_LOADED_MAGAZINE_ITEM.get());
        if (stored == null || stored.isBlank()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(stored);
        if (id == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item instanceof MagazineItem magazine ? magazine : null;
    }

    private void setLoadedMagazineItem(ItemStack gunStack, @Nullable ResourceLocation id) {
        if (id == null || !BuiltInRegistries.ITEM.getOptional(id).map(MagazineItem.class::isInstance).orElse(false)) {
            gunStack.remove(ModDataComponents.GUN_LOADED_MAGAZINE_ITEM.get());
            return;
        }
        gunStack.set(ModDataComponents.GUN_LOADED_MAGAZINE_ITEM.get(), id.toString());
    }

    private boolean isSameLoadedMagazineType(ItemStack gunStack, ResourceLocation id) {
        ResourceLocation storedId = getLoadedMagazineItemId(gunStack);
        if (storedId == null) {
            MagazineItem baseMagazine = getCompatibleMagazineItemUnchecked();
            return baseMagazine != null && BuiltInRegistries.ITEM.getKey(baseMagazine).equals(id);
        }
        return id.equals(storedId);
    }

    @Nullable
    private ResourceLocation getLoadedMagazineItemId(ItemStack gunStack) {
        String stored = gunStack.get(ModDataComponents.GUN_LOADED_MAGAZINE_ITEM.get());
        if (stored == null || stored.isBlank()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(stored);
        if (id == null || !BuiltInRegistries.ITEM.getOptional(id).map(MagazineItem.class::isInstance).orElse(false)) {
            return null;
        }
        return id;
    }

    private void returnStoredMagazine(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void returnExcessStoredAmmo(Player player, int ammoCount) {
        if (ammoCount <= 0) {
            return;
        }
        ResourceLocation ammoId = getCompatibleAmmoId();
        if (ammoId == null) {
            return;
        }
        Item ammoItem = BuiltInRegistries.ITEM.getOptional(ammoId).orElse(null);
        if (ammoItem == null) {
            return;
        }
        int remaining = ammoCount;
        int maxStack = Math.max(1, ammoItem.getDefaultMaxStackSize());
        while (remaining > 0) {
            int count = Math.min(remaining, maxStack);
            ItemStack ammoStack = new ItemStack(ammoItem, count);
            if (!player.getInventory().add(ammoStack)) {
                player.drop(ammoStack, false);
            }
            remaining -= count;
        }
    }

    private boolean usesSegmentedReloadAnimation() {
        return SEGMENTED_RELOAD_ANIM_IDS.contains(stats.id().getPath());
    }

    private void startReloadVisualState(ItemStack stack, int reloadTicks, @Nullable PendingMagazineSwap pendingMagazine) {
        int totalTicks = Math.max(1, getReloadVisualTicks(reloadTicks));
        stack.set(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get(), totalTicks);
        stack.set(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), totalTicks);
        stack.set(ModDataComponents.GUN_RELOAD_STAGE.get(), usesSegmentedReloadAnimation() ? RELOAD_STAGE_START : RELOAD_STAGE_NONE);
        if (usesMagazineSwapReload(stack)) {
            setReloadMagazineVisuals(stack, pendingMagazine);
        }
    }

    private void updateReloadVisualState(Level level, Player player, ItemStack stack, int slot, boolean held) {
        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            clearReloadVisualState(stack);
            return;
        }

        if (!held || !isPendingReloadStillHeld(player, stack)) {
            cancelPendingReload(player, stack, slot);
            return;
        }

        remainingTicks--;
        if (remainingTicks <= 0) {
            completePendingReload(level, player, stack);
            clearReloadVisualState(stack);
            return;
        }

        stack.set(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), remainingTicks);
        if (usesSegmentedReloadAnimation()) {
            int oldStage = stack.getOrDefault(ModDataComponents.GUN_RELOAD_STAGE.get(), RELOAD_STAGE_NONE);
            int newStage = computeSegmentedReloadStage(stack, remainingTicks);
            stack.set(ModDataComponents.GUN_RELOAD_STAGE.get(), newStage);
            if (newStage != oldStage && stack.getItem() instanceof AnimatedGunItem animated
                    && !"rocket_launcher".equals(stats.id().getPath())) {
                triggerSegmentedReloadStage(animated, level, player, stack, newStage);
            }
        }
    }

    private boolean isPendingReloadStillHeld(Player player, ItemStack stack) {
        PendingReload pending = PENDING_RELOADS.get(player.getUUID());
        if (pending == null) {
            return false;
        }

        if (pending.hand() == InteractionHand.MAIN_HAND && player.getInventory().selected != pending.selectedSlot()) {
            return false;
        }

        ItemStack current = player.getItemInHand(pending.hand());
        return current == stack && isSameStackIgnoringAnimationState(current, pending.stackSnapshot());
    }

    private void cancelPendingReload(Player player, ItemStack stack, int slot) {
        PENDING_RELOADS.remove(player.getUUID());
        clearReloadVisualState(stack);
        queueDrawAfterReloadCancel(player, stack, slot, true);
    }

    public static void cancelReloadForImmediateAction(Player player, ItemStack stack) {
        UUID playerId = player.getUUID();
        PENDING_RELOADS.remove(playerId);
        SERVER_RELOAD_CANCEL_DRAW_STATES.remove(playerId);
        CLIENT_RELOAD_CANCEL_DRAW_STATES.remove(playerId);
        HELD_DRAW_STATES.remove(playerId);
        CLIENT_HELD_DRAW_STATES.remove(playerId);
        CLIENT_RELOAD_VISUAL_STATES.remove(playerId);
        clearReloadVisualState(stack);
        clearDrawState(stack);
    }

    private void completePendingReload(Level level, Player player, ItemStack stack) {
        PendingReload pending = PENDING_RELOADS.remove(player.getUUID());
        if (usesMagazineSwapReload(stack)) {
            completeReloadWithMagazineSwap(level, player, stack, pending);
        } else {
            completeReloadWithLooseAmmo(player, stack);
        }
    }

    private void updateDrawState(Player player, ItemStack stack, int slot, boolean selected, boolean held) {
        UUID playerId = player.getUUID();
        HeldGunState currentState = held ? heldGunState(player, stack, slot, selected) : null;
        if (currentState == null) {
            if (hasQueuedReloadCancelDraw(SERVER_RELOAD_CANCEL_DRAW_STATES, playerId, stack, slot)) {
                forgetHeldGunState(HELD_DRAW_STATES, playerId, stack, slot);
                return;
            }
            clearDrawState(stack);
            forgetHeldGunState(HELD_DRAW_STATES, playerId, stack, slot);
            return;
        }
        if (isReloading(stack)) {
            HELD_DRAW_STATES.put(playerId, currentState);
            return;
        }

        HeldGunState previousState = HELD_DRAW_STATES.get(playerId);
        if (consumeQueuedReloadCancelDraw(SERVER_RELOAD_CANCEL_DRAW_STATES, playerId, stack, slot)) {
            HELD_DRAW_STATES.put(playerId, currentState);
            stack.set(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), getDrawAnimationTicks(stack));
            return;
        }
        if (!isSameHeldGunState(currentState, previousState)) {
            HELD_DRAW_STATES.put(playerId, currentState);
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0);
        if (remainingTicks > 0) {
            stack.set(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), remainingTicks - 1);
        }
    }

    private void updateClientReloadVisualState(Player player, ItemStack stack, int slot, boolean selected, boolean held) {
        UUID playerId = player.getUUID();
        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        if (remainingTicks <= 0) {
            forgetHeldGunState(CLIENT_RELOAD_VISUAL_STATES, playerId, stack, slot);
            return;
        }

        HeldGunState currentState = held ? heldGunState(player, stack, slot, selected) : null;
        if (currentState == null) {
            clearReloadVisualState(stack);
            queueDrawAfterReloadCancel(player, stack, slot, true);
            forgetHeldGunState(CLIENT_RELOAD_VISUAL_STATES, playerId, stack, slot);
            return;
        }

        HeldGunState previousState = CLIENT_RELOAD_VISUAL_STATES.get(playerId);
        if (previousState == null) {
            CLIENT_RELOAD_VISUAL_STATES.put(playerId, currentState);
        } else if (!isSameHeldGunState(currentState, previousState) && currentState.stackIdentity() == previousState.stackIdentity()) {
            clearReloadVisualState(stack);
            queueDrawAfterReloadCancel(player, stack, slot, true);
            CLIENT_RELOAD_VISUAL_STATES.remove(playerId);
            return;
        }
        CLIENT_RELOAD_VISUAL_STATES.put(playerId, currentState);

        remainingTicks--;
        if (remainingTicks <= 0) {
            clearReloadVisualState(stack);
            forgetHeldGunState(CLIENT_RELOAD_VISUAL_STATES, playerId, stack, slot);
            return;
        }

        stack.set(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), remainingTicks);
        if (usesSegmentedReloadAnimation()) {
            stack.set(ModDataComponents.GUN_RELOAD_STAGE.get(), computeSegmentedReloadStage(stack, remainingTicks));
        }
    }

    private void updateClientDrawState(Player player, ItemStack stack, int slot, boolean selected, boolean held) {
        UUID playerId = player.getUUID();
        HeldGunState currentState = held ? heldGunState(player, stack, slot, selected) : null;
        if (currentState == null) {
            if (hasQueuedReloadCancelDraw(CLIENT_RELOAD_CANCEL_DRAW_STATES, playerId, stack, slot)) {
                forgetHeldGunState(CLIENT_HELD_DRAW_STATES, playerId, stack, slot);
                return;
            }
            clearDrawState(stack);
            forgetHeldGunState(CLIENT_HELD_DRAW_STATES, playerId, stack, slot);
            return;
        }

        if (consumeQueuedReloadCancelDraw(CLIENT_RELOAD_CANCEL_DRAW_STATES, playerId, stack, slot)) {
            CLIENT_HELD_DRAW_STATES.put(playerId, currentState);
            clearReloadVisualState(stack);
            stack.set(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), getDrawAnimationTicks(stack));
            if (stack.getItem() instanceof AnimatedGunItem) {
                AnimatedGunItem.restartDrawAnimationAfterReloadCancel(stack);
            }
            return;
        }

        if (isReloading(stack)) {
            CLIENT_HELD_DRAW_STATES.put(playerId, currentState);
            return;
        }

        HeldGunState previousState = CLIENT_HELD_DRAW_STATES.get(playerId);
        if (!isSameHeldGunState(currentState, previousState)) {
            CLIENT_HELD_DRAW_STATES.put(playerId, currentState);
            clearReloadVisualState(stack);
            return;
        }

        int remainingTicks = stack.getOrDefault(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0);
        if (remainingTicks > 0) {
            stack.set(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), remainingTicks - 1);
        }
    }

    private static void queueDrawAfterReloadCancel(Player player, ItemStack stack, int slot, boolean preserveUntilHeld) {
        HELD_DRAW_STATES.remove(player.getUUID());
        CLIENT_HELD_DRAW_STATES.remove(player.getUUID());
        if (preserveUntilHeld) {
            reloadCancelDrawStates(player).put(player.getUUID(), new QueuedReloadCancelDraw(slot, System.identityHashCode(stack), stack.copy()));
        } else {
            stack.set(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), getDrawAnimationTicks(stack));
        }
    }

    private static Map<UUID, QueuedReloadCancelDraw> reloadCancelDrawStates(Player player) {
        return player.level().isClientSide() ? CLIENT_RELOAD_CANCEL_DRAW_STATES : SERVER_RELOAD_CANCEL_DRAW_STATES;
    }

    private static boolean hasQueuedReloadCancelDraw(Map<UUID, QueuedReloadCancelDraw> states, UUID playerId, ItemStack stack, int slot) {
        QueuedReloadCancelDraw queued = states.get(playerId);
        if (queued == null) {
            return false;
        }
        if (queued.matchesStack(stack, slot)) {
            return true;
        }
        if (queued.inventorySlot() == slot) {
            states.remove(playerId);
        }
        return false;
    }

    private static boolean consumeQueuedReloadCancelDraw(Map<UUID, QueuedReloadCancelDraw> states, UUID playerId, ItemStack stack, int slot) {
        QueuedReloadCancelDraw queued = states.get(playerId);
        if (queued == null) {
            return false;
        }
        if (!queued.matchesStack(stack, slot)) {
            if (queued.inventorySlot() == slot) {
                states.remove(playerId);
            }
            return false;
        }
        states.remove(playerId);
        return true;
    }

    private static HeldGunState heldGunState(Player player, ItemStack stack, int slot, boolean selected) {
        InteractionHand hand = heldHand(player, stack, selected);
        if (hand == null) {
            return null;
        }
        int selectedSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        return new HeldGunState(hand, selectedSlot, slot, System.identityHashCode(stack), stack.copy());
    }

    private static boolean isSameHeldGunState(HeldGunState currentState, HeldGunState previousState) {
        if (currentState == previousState) {
            return true;
        }
        if (currentState == null || previousState == null) {
            return false;
        }
        return currentState.hand() == previousState.hand()
                && currentState.selectedSlot() == previousState.selectedSlot()
                && currentState.inventorySlot() == previousState.inventorySlot()
                && isSameStackIgnoringAnimationState(currentState.stackSnapshot(), previousState.stackSnapshot());
    }

    private static InteractionHand heldHand(Player player, ItemStack stack, boolean selected) {
        if (stack == player.getOffhandItem()) {
            return InteractionHand.OFF_HAND;
        }
        if (selected || stack == player.getMainHandItem()) {
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private static void forgetHeldGunState(Map<UUID, HeldGunState> states, UUID playerId, ItemStack stack, int slot) {
        HeldGunState previousState = states.get(playerId);
        if (previousState != null && previousState.matchesStack(stack, slot)) {
            states.remove(playerId);
        }
    }

    private static void triggerSegmentedReloadStage(AnimatedGunItem animated, Level level, Player player, ItemStack stack, int stage) {
        if (stage == RELOAD_STAGE_START) {
            animated.triggerReloadStart(level, player, stack);
        } else if (stage == RELOAD_STAGE_LOOP) {
            animated.triggerReloadLoop(level, player, stack);
        } else if (stage == RELOAD_STAGE_STOP) {
            animated.triggerReloadStop(level, player, stack);
        }
    }

    private int computeSegmentedReloadStage(ItemStack stack, int remainingTicks) {
        int totalTicks = Math.max(1, stack.getOrDefault(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get(), stats.totalReloadTime()));
        int elapsedTicks = totalTicks - remainingTicks;
        int startTicks = getSegmentedReloadStartTicks(totalTicks);
        int stopTicks = getSegmentedReloadStopTicks(totalTicks);
        if (remainingTicks <= stopTicks) {
            return RELOAD_STAGE_STOP;
        }
        if (elapsedTicks >= startTicks) {
            return RELOAD_STAGE_LOOP;
        }
        return RELOAD_STAGE_START;
    }

    private int getSegmentedReloadStartTicks(int totalTicks) {
        if ("rocket_launcher".equals(stats.id().getPath())) {
            return ROCKET_RELOAD_START_TICKS;
        }
        int defaultTicks = Mth.clamp(totalTicks / 4, 4, 12);
        return Math.max(defaultTicks, RELOAD_START_ANIMATION_MIN_TICKS.getOrDefault(stats.id().getPath(), defaultTicks));
    }

    private int getSegmentedReloadStopTicks(int totalTicks) {
        if ("rocket_launcher".equals(stats.id().getPath())) {
            return ROCKET_RELOAD_STOP_TICKS;
        }
        int defaultTicks = Mth.clamp(totalTicks / 4, 4, 12);
        return Math.max(defaultTicks, RELOAD_STOP_ANIMATION_MIN_TICKS.getOrDefault(stats.id().getPath(), defaultTicks));
    }

    private int getReloadVisualTicks(int reloadTicks) {
        if ("rocket_launcher".equals(stats.id().getPath()) && usesSegmentedReloadAnimation()) {
            int minVisualTicks = ROCKET_RELOAD_START_TICKS + ROCKET_RELOAD_LOOP_TICKS + ROCKET_RELOAD_STOP_TICKS;
            return Math.max(reloadTicks, minVisualTicks);
        }
        if (usesSegmentedReloadAnimation()) {
            String gunId = stats.id().getPath();
            int minVisualTicks = RELOAD_START_ANIMATION_MIN_TICKS.getOrDefault(gunId, 0)
                    + RELOAD_STOP_ANIMATION_MIN_TICKS.getOrDefault(gunId, 0);
            return Math.max(reloadTicks, minVisualTicks);
        }
        return Math.max(reloadTicks, RELOAD_ANIMATION_MIN_TICKS.getOrDefault(stats.id().getPath(), reloadTicks));
    }

    private static void clearReloadVisualState(ItemStack stack) {
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_RELOAD_STAGE.get());
        stack.remove(ModDataComponents.GUN_RELOAD_FROM_MAGAZINE_ITEM.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TO_MAGAZINE_ITEM.get());
    }

    private void setReloadMagazineVisuals(ItemStack stack, @Nullable PendingMagazineSwap pendingMagazine) {
        ResourceLocation from = getLoadedMagazineItemId(stack);
        if (from == null) {
            MagazineItem baseMagazine = getCompatibleMagazineItemUnchecked();
            if (baseMagazine != null) {
                from = BuiltInRegistries.ITEM.getKey(baseMagazine);
            }
        }

        ResourceLocation to = pendingMagazine != null ? pendingMagazine.magazineItemId() : from;
        setReloadMagazineVisual(stack, ModDataComponents.GUN_RELOAD_FROM_MAGAZINE_ITEM.get(), from);
        setReloadMagazineVisual(stack, ModDataComponents.GUN_RELOAD_TO_MAGAZINE_ITEM.get(), to);
    }

    private static void setReloadMagazineVisual(ItemStack stack, net.minecraft.core.component.DataComponentType<String> component, @Nullable ResourceLocation id) {
        if (id == null) {
            stack.remove(component);
            return;
        }
        stack.set(component, id.toString());
    }

    private static void clearDrawState(ItemStack stack) {
        stack.remove(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get());
    }

    private static boolean isSameStackIgnoringAnimationState(ItemStack first, ItemStack second) {
        if (first == second) {
            return true;
        }
        if (first == null || first.isEmpty() || second == null || second.isEmpty()) {
            return false;
        }
        if (ItemStack.isSameItemSameComponents(first, second)) {
            return true;
        }
        if (!ItemStack.isSameItem(first, second)) {
            return false;
        }

        ItemStack firstCopy = first.copy();
        ItemStack secondCopy = second.copy();
        clearHeldGunMatchState(firstCopy);
        clearHeldGunMatchState(secondCopy);
        return ItemStack.isSameItemSameComponents(firstCopy, secondCopy);
    }

    private static void clearHeldGunMatchState(ItemStack stack) {
        clearReloadVisualState(stack);
        clearDrawState(stack);
        stack.remove(ModDataComponents.GUN_AMMO.get());
        stack.remove(ModDataComponents.GUN_HEAT.get());
        stack.remove(ModDataComponents.GUN_TRIGGER_LOCK.get());
        stack.remove(ModDataComponents.GUN_WATER_COOLING_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_FLASHLIGHT_POWERED.get());
        stack.remove(ModDataComponents.GUN_FLASHLIGHT_BATTERY.get());
    }

    public static void startClientDrawAnimationForSwitch(Player player, ItemStack stack) {
        if (player == null || !player.level().isClientSide() || stack.isEmpty() || !(stack.getItem() instanceof AnimatedGunItem)) {
            return;
        }

        UUID playerId = player.getUUID();
        int slot = player.getInventory().selected;
        boolean hadReloadVisual = isReloading(stack);
        clearReloadVisualState(stack);

        HeldGunState currentState = heldGunState(player, stack, slot, true);
        if (currentState != null) {
            CLIENT_HELD_DRAW_STATES.put(playerId, currentState);
        } else {
            CLIENT_HELD_DRAW_STATES.remove(playerId);
        }
        CLIENT_RELOAD_VISUAL_STATES.remove(playerId);

        stack.set(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), getDrawAnimationTicks(stack));
        if (hadReloadVisual) {
            AnimatedGunItem.restartDrawAnimationAfterReloadCancel(stack);
        } else {
            AnimatedGunItem.restartDrawAnimation(stack);
        }
    }

    public static void cancelClientReloadVisualForSwitch(Player player, ItemStack stack, int slot) {
        if (player == null || !player.level().isClientSide() || stack.isEmpty() || !(stack.getItem() instanceof AnimatedGunItem)) {
            return;
        }
        if (!isReloading(stack)) {
            return;
        }

        UUID playerId = player.getUUID();
        clearReloadVisualState(stack);
        if (slot >= 0) {
            queueDrawAfterReloadCancel(player, stack, slot, true);
        }
        CLIENT_RELOAD_VISUAL_STATES.remove(playerId);
        forgetHeldGunState(CLIENT_HELD_DRAW_STATES, playerId, stack, slot);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (entity instanceof Player player && stack.getItem() instanceof AnimatedGunItem) {
            boolean held = selected || stack == player.getMainHandItem() || stack == player.getOffhandItem();
            if (level.isClientSide()) {
                updateClientReloadVisualState(player, stack, slot, selected, held);
                updateClientDrawState(player, stack, slot, selected, held);
            } else {
                updateDrawState(player, stack, slot, selected, held);
                updateReloadVisualState(level, player, stack, slot, held);
            }
        }
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        AttachmentModifiers modifiers = GunAttachments.modifiers(stack);
        float displayDamage = this.stats.id().equals(GRENADE_LAUNCHER_ID)
                ? GRENADE_BASE_POWER * GRENADE_DAMAGE_FACTOR
                : modifiedDamage(stats, modifiers);
        tooltip.add(Component.translatable("info.jeg.damage", String.format(Locale.US, "%.1f", displayDamage)));

        if (usesLoadedAmmo()) {
            tooltip.add(Component.translatable("info.jeg.ammo", getAmmo(stack), modifiedMagazineSize(stack)));
        }

        if (usesOverheatMechanic()) {
            int heat = getOverheatPercent(stack);
            ChatFormatting color = heat >= 100 ? ChatFormatting.RED : ChatFormatting.GOLD;
            tooltip.add(Component.translatable("info.jeg.overheat", heat).withStyle(color));
        }

        // Add ammo type information
        Optional<Item> ammoItem = getAmmoItem();
        if (ammoItem.isPresent()) {
            ItemStack ammoStack = new ItemStack(ammoItem.get());
            Component ammoName = ammoStack.getHoverName();
            tooltip.add(Component.translatable("info.jeg.ammo_type", ammoName));
        }

        float armorPiercing = BallisticProtection.effectiveArmorPiercing(
                this.stats,
                BallisticProtection.isRocketDirectHit(this.stats),
                modifiers.explosiveAmmo() ? 0.75F : 1.0F
        );
        tooltip.add(Component.translatable("info.jeg.armor_piercing", String.format(Locale.US, "%.2f", armorPiercing)));
        tooltip.add(Component.translatable("info.jeg.headshot_multiplier", String.format(Locale.US, "%.2fx", GunHeadshotHelper.headshotMultiplier(this.stats))));

        double effectiveRange = GunRangeHelper.computeFullDamageRange(this.stats);
        if (effectiveRange > 0.0D) {
            tooltip.add(Component.translatable("info.jeg.range", String.format(Locale.US, "%.0f", effectiveRange)));
        }

        // Add projectile count for shotguns
        if (stats.projectileAmount() > 1) {
            tooltip.add(Component.translatable("info.jeg.projectiles", stats.projectileAmount()));
        }
        tooltip.add(Component.translatable("info.jeg.open_attachments_z").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("info.jeg.inspect_help", Component.translatable("key.jeg.inspect")).withStyle(ChatFormatting.YELLOW));
    }

    private static void addClientDryFireRecoil(float recoilAmount) {
        invokeClientRecoilMethod("addDryFire", recoilAmount);
    }

    private static void invokeClientRecoilMethod(String methodName, float recoilAmount) {
        try {
            Class<?> recoilClass = Class.forName("ttv.migami.jeg.client.GunRecoilHandler");
            recoilClass.getDeclaredMethod(methodName, float.class).invoke(null, recoilAmount);
        } catch (Throwable ignored) {
        }
    }
}
