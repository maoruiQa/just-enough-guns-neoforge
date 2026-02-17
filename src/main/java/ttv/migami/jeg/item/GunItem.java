package ttv.migami.jeg.item;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import ttv.migami.jeg.client.GunRecoilHandler;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import net.minecraft.ChatFormatting;

public class GunItem extends Item {
    private static final ResourceLocation GRENADE_LAUNCHER_ID = Reference.id("grenade_launcher");
    private static final float GRENADE_BASE_POWER = 4.0F;
    private static final float GRENADE_DAMAGE_FACTOR = 5.0F;
    private static final int GRENADE_FUSE_TICKS = 600;
    private static final String MINIGUN_PATH = "minigun";
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

    private int shotsPerTrigger() {
        return MINIGUN_PATH.equals(this.stats.id().getPath()) ? 5 : 1;
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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ensureAmmoInitialized(stack);
        boolean automatic = isAutomatic();

        if (!automatic && isTriggerLocked(stack)) {
            return InteractionResult.FAIL;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        if (!hasAmmoAvailable(player, stack)) {
            if (level.isClientSide()) {
                float recoilMultiplier = RecoilProfiles.multiplier(stats.id());
                GunRecoilHandler.addDryFire(stats.recoilKick() * recoilMultiplier * 0.25F);
                playDryFireSound(level, player);
                Component message = stats.usesMagazine() && !stats.isInventoryFed()
                        ? Component.translatable("item.jeg.gun.empty")
                        : Component.translatable("item.jeg.gun.no_ammo");
                player.displayClientMessage(message, true);
            } else {
                playDryFireSound(level, player);
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            float recoilMultiplier = RecoilProfiles.multiplier(stats.id());
            float recoilKick = stats.recoilKick() * recoilMultiplier;
            GunRecoilHandler.addShot(recoilKick);
            float targetPitch = player.getXRot() - recoilKick * 6.0F;
            player.setXRot(Mth.clamp(targetPitch, -90.0F, 90.0F));
            if (!automatic) {
                setTriggerLocked(stack, true);
            }

            // Client-side instant trail calculation for fast bullets
            ResourceLocation gunId = stats.id();
            // Removed custom trail rendering - rely on server-sent particles instead
            // which have proper depth testing and don't render through blocks
        } else {
            int shotsFired = 0;
            int shotsToFire = shotsPerTrigger();
            for (int shot = 0; shot < shotsToFire; shot++) {
                if (!consumeAmmo(level, player, stack)) {
                    if (shot == 0) {
                        return InteractionResult.FAIL;
                    }
                    break;
                }
                fireAt(level, player, stack, null);
                stack.hurtAndBreak(1, player, hand);
                shotsFired++;
            }
            if (shotsFired <= 0) {
                return InteractionResult.FAIL;
            }

            if (!automatic) {
                setTriggerLocked(stack, true);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(stack, Math.max(1, stats.fireDelay()));
        }

        playSound(level, player, stats.fireSoundEvent().or(stats::enchantedFireSoundEvent));
        return InteractionResult.SUCCESS;
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

    private static GunCategory resolveCategory(GunStats stats) {
        return GunCategory.fromStats(stats);
    }

    public void fireAt(Level level, LivingEntity shooter, ItemStack stack, @Nullable LivingEntity target) {
        Vec3 origin = shooter.getEyePosition();
        RandomSource random = shooter.getRandom();
        int pellets = Math.max(1, stats.projectileAmount());
        ResourceLocation gunId = stats.id();

        boolean grenadeLauncher = gunId.equals(GRENADE_LAUNCHER_ID);
        boolean flamethrower = gunId.equals(Reference.id("flamethrower"));
        float grenadePower = grenadeLauncher ? GRENADE_BASE_POWER : Math.max(1.8F, stats.damage() / 12.0F + 1.5F);
        int fuseTicks = grenadeLauncher ? GRENADE_FUSE_TICKS : 40;
        Vec3 shooterMotion = shooter.getDeltaMovement();

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
                if (flamethrower) {
                    // Apply gravity to flamethrower projectiles
                    velocity = velocity.add(0, -0.05, 0);
                }
                BulletEntity bullet = new BulletEntity(level, shooter, stats, velocity);
                bullet.initialisePosition(muzzle);
                level.addFreshEntity(bullet);

                // Add bullet trail particles for all guns EXCEPT flamethrower
                // (flamethrower already has its own particle effects)
                if (!flamethrower && level instanceof ServerLevel serverLevel) {
                    // Use penetration-aware raycast to spawn particles along actual bullet path
                    spawnBulletTrailParticles(serverLevel, muzzle, direction, stats, shooter);
                }
            }
        }
    }

    public void fireDirectionally(Level level, LivingEntity shooter, ItemStack stack, Vec3 direction) {
        Vec3 origin = shooter.getEyePosition();
        int pellets = Math.max(1, stats.projectileAmount());
        ResourceLocation gunId = stats.id();

        boolean grenadeLauncher = gunId.equals(GRENADE_LAUNCHER_ID);
        boolean flamethrower = gunId.equals(Reference.id("flamethrower"));
        float grenadePower = grenadeLauncher ? GRENADE_BASE_POWER : Math.max(1.8F, stats.damage() / 12.0F + 1.5F);
        int fuseTicks = grenadeLauncher ? GRENADE_FUSE_TICKS : 40;
        Vec3 shooterMotion = shooter.getDeltaMovement();
        Vec3 normalized = direction.normalize();

        for (int i = 0; i < pellets; i++) {
            Vec3 muzzle = origin.add(normalized.scale(0.35F));

            if (grenadeLauncher) {
                GrenadeEntity grenade = new GrenadeEntity(level, shooter, grenadePower, fuseTicks, true);
                grenade.initialisePosition(muzzle);
                Vec3 launchVelocity = normalized.scale(Math.max(1.2F, stats.projectileSpeed() * 0.9F)).add(shooterMotion);
                grenade.setDeltaMovement(launchVelocity);
                level.addFreshEntity(grenade);
            } else {
                Vec3 velocity = normalized.scale(stats.projectileSpeed());
                if (flamethrower) {
                    velocity = velocity.add(0, -0.05, 0);
                }
                BulletEntity bullet = new BulletEntity(level, shooter, stats, velocity);
                bullet.initialisePosition(muzzle);
                level.addFreshEntity(bullet);

                if (!flamethrower && level instanceof ServerLevel serverLevel) {
                    // Use penetration-aware raycast to spawn particles along actual bullet path
                    spawnBulletTrailParticles(serverLevel, muzzle, normalized, stats, shooter);
                }
            }
        }
    }

    private Vec3 computeDirection(LivingEntity shooter, Vec3 origin, @Nullable LivingEntity target, RandomSource random, GunStats stats) {
        Vec3 base = target != null
                ? target.getEyePosition().subtract(origin)
                : shooter.getViewVector(1.0F);

        GunCategory category = resolveCategory(stats);
        float baseSpread = stats.spread();
        float multiplier = shooter.isCrouching() ? category.crouchMultiplier() : category.hipMultiplier();
        float actualSpread = Math.max(0.0F, baseSpread * multiplier);

        if (!(shooter instanceof Player)) {
            actualSpread += 2.5F;
            if (shooter instanceof Skeleton) {
                // Check if this skeleton was converted from a pillager (reduced spread for better accuracy)
                if (shooter.getTags().contains("jeg_pillager_converted")) {
                    actualSpread += 1.5F; // Moderately reduced spread for pillager gunners (was 2.5F)
                } else {
                    actualSpread += 2.5F; // Normal spread for other gunners
                }
            }
            if (shooter instanceof PhantomGunner) {
                actualSpread += 3.0F;
            }
        }

        if (shooter.isCrouching() && "light_machine_gun".equals(stats.id().getPath())) {
            actualSpread *= 1.85F;
        }

        return applySpread(base, actualSpread, random);
    }

    private Vec3 applySpread(Vec3 direction, float spreadDeg, RandomSource random) {
        Vec3 normalized = direction.normalize();
        if (spreadDeg <= 0.0F) {
            return normalized;
        }

        double deviation = Math.tan(Math.toRadians(spreadDeg));
        double offsetX = random.triangle(0.0D, deviation);
        double offsetY = random.triangle(0.0D, deviation * 0.5D);
        double offsetZ = random.triangle(0.0D, deviation);
        Vec3 jitter = new Vec3(offsetX, offsetY, offsetZ);
        return normalized.add(jitter).normalize();
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
        player.getCooldowns().addCooldown(stack, reloadTicks);
        playSound(level, player, stats.reloadStartSoundEvent());
        return true;
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
        double maxRange = GunRangeHelper.computeEffectiveRange(stats);
        Vec3 motion = direction.scale(maxRange);
        Vec3 searchStart = start;

        int maxIterations = 10;
        for (int i = 0; i < maxIterations; i++) {
            Vec3 searchEnd = searchStart.add(motion);

            // Check block collision
            ClipContext clipContext = new ClipContext(
                searchStart,
                searchEnd,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                shooter
            );
            net.minecraft.world.phys.BlockHitResult blockHit = level.clip(clipContext);

            if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                // No collision - spawn particles to max range
                spawnParticleSegment(level, searchStart, searchEnd);
                return;
            }

            BlockPos hitPos = blockHit.getBlockPos();
            net.minecraft.world.level.block.state.BlockState hitState = level.getBlockState(hitPos);
            Vec3 hitLocation = blockHit.getLocation();
            boolean isPenetrable = ttv.migami.jeg.gun.BulletPenetrationHelper.isPenetrable(level, hitState);

            if (isPenetrable) {
                // Calculate exit point
                Vec3 dir = motion.normalize();
                Vec3 exitPoint = new Vec3(
                    hitPos.getX() + 0.5 + dir.x * 0.6,
                    hitPos.getY() + 0.5 + dir.y * 0.6,
                    hitPos.getZ() + 0.5 + dir.z * 0.6
                );

                spawnParticleSegment(level, searchStart, exitPoint);

                // Continue from exit point
                double distanceToHit = searchStart.distanceTo(hitLocation);
                double remainingDistance = searchStart.distanceTo(searchEnd) - distanceToHit;
                searchStart = exitPoint;
                motion = dir.scale(remainingDistance);
            } else {
                // Hit solid block - spawn particles and stop
                spawnParticleSegment(level, searchStart, hitLocation);
                return;
            }
        }
    }

    /**
     * Spawn particle trail from start to end position.
     */
    private void spawnParticleSegment(ServerLevel level, Vec3 start, Vec3 end) {
        double distance = start.distanceTo(end);
        int particleCount = Math.min(20, Math.max(3, (int) (distance / 2.0)));

        for (int i = 1; i < particleCount; i++) { // Start from 1 instead of 0 to skip muzzle position
            double fraction = (double) i / particleCount;
            Vec3 pos = start.add(end.subtract(start).scale(fraction));

            if (stats.gravity()) {
                // Fire particles for gravity-affected bullets only
                level.sendParticles(
                    ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z,
                    1, 0.01, 0.01, 0.01, 0.005
                );
            }

            // Smoke particles
            level.sendParticles(
                ParticleTypes.SMOKE,
                pos.x, pos.y, pos.z,
                1, 0.01, 0.01, 0.01, 0.005
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        float displayDamage = this.stats.id().equals(GRENADE_LAUNCHER_ID) ? GRENADE_BASE_POWER * GRENADE_DAMAGE_FACTOR : stats.damage();
        tooltipAdder.accept(Component.translatable("info.jeg.damage", String.format("%.1f", displayDamage)));

        if (stats.usesMagazine()) {
            tooltipAdder.accept(Component.translatable("info.jeg.ammo", getAmmo(stack), stats.magazineSize()));
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
