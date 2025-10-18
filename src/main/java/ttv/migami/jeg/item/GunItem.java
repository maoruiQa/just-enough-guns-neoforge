package ttv.migami.jeg.item;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import ttv.migami.jeg.client.GunRecoilHandler;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.entity.GrenadeEntity;
import ttv.migami.jeg.entity.GunnerEntity;
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import net.minecraft.ChatFormatting;

public class GunItem extends Item {
    private static final ResourceLocation GRENADE_LAUNCHER_ID = Reference.id("grenade_launcher");
    private static final Set<String> AUTOMATIC_IDS = Set.of(
            "abstract_gun",
            "assault_rifle",
            "blossom_rifle",
            "burst_rifle",
            "combat_rifle",
            "custom_smg",
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
            stack.set(ModDataComponents.GUN_AMMO.get(), stats.magazineSize());
        }
        return stack;
    }

    private void ensureAmmoInitialized(ItemStack stack) {
        if (stats.usesMagazine() && !stack.has(ModDataComponents.GUN_AMMO.get())) {
            stack.set(ModDataComponents.GUN_AMMO.get(), stats.magazineSize());
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
                GunRecoilHandler.addDryFire(stats.recoilKick() * 0.25F);
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
            GunRecoilHandler.addShot(stats.recoilKick());
            float targetPitch = player.getXRot() - stats.recoilKick() * 6.0F;
            player.setXRot(Mth.clamp(targetPitch, -90.0F, 90.0F));
            if (!automatic) {
                setTriggerLocked(stack, true);
            }
        } else {
            if (!consumeAmmo(level, player, stack)) {
                return InteractionResult.FAIL;
            }

            fireAt(level, player, stack, null);
            if (!automatic) {
                setTriggerLocked(stack, true);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.getCooldowns().addCooldown(stack, Math.max(1, stats.fireDelay()));
            stack.hurtAndBreak(1, player, hand);
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
        if (ammoId == null) {
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
        float grenadePower = Math.max(1.8F, stats.damage() / 12.0F + 1.5F);
        int fuseTicks = grenadeLauncher ? Math.max(40, stats.projectileLife() / 2) : 40;
        Vec3 shooterMotion = shooter.getDeltaMovement();

        for (int i = 0; i < pellets; i++) {
            Vec3 direction = computeDirection(shooter, origin, target, random, stats);
            Vec3 muzzle = origin.add(direction.scale(0.35F));

            if (grenadeLauncher) {
                GrenadeEntity grenade = new GrenadeEntity(level, shooter, grenadePower, fuseTicks, true);
                grenade.initialisePosition(muzzle);
                Vec3 launchVelocity = direction.scale(Math.max(1.2F, stats.projectileSpeed() * 0.8F)).add(shooterMotion);
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
                    // Estimate target position for particle trail
                    Vec3 targetPos = target != null ? target.getEyePosition() : muzzle.add(direction.scale(50.0D));
                    double distance = muzzle.distanceTo(targetPos);

                    // Spawn flame and smoke particles along the bullet path
                    // Scale particle count based on distance (max 20 particles)
                    int particleCount = Math.min(20, (int) (distance / 2.0D));
                    for (int j = 0; j < particleCount; j++) {
                        double fraction = (double) j / particleCount;
                        Vec3 particlePos = muzzle.add(direction.scale(distance * fraction));

                        // Fire particles (reduced from phantom code for subtler effect)
                        serverLevel.sendParticles(
                            ParticleTypes.FLAME,
                            particlePos.x, particlePos.y, particlePos.z,
                            1, 0.01D, 0.01D, 0.01D, 0.005D
                        );

                        // Smoke particles
                        serverLevel.sendParticles(
                            ParticleTypes.SMOKE,
                            particlePos.x, particlePos.y, particlePos.z,
                            1, 0.01D, 0.01D, 0.01D, 0.005D
                        );
                    }
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
        float grenadePower = Math.max(1.8F, stats.damage() / 12.0F + 1.5F);
        int fuseTicks = grenadeLauncher ? Math.max(40, stats.projectileLife() / 2) : 40;
        Vec3 shooterMotion = shooter.getDeltaMovement();
        Vec3 normalized = direction.normalize();

        for (int i = 0; i < pellets; i++) {
            Vec3 muzzle = origin.add(normalized.scale(0.35F));

            if (grenadeLauncher) {
                GrenadeEntity grenade = new GrenadeEntity(level, shooter, grenadePower, fuseTicks, true);
                grenade.initialisePosition(muzzle);
                Vec3 launchVelocity = normalized.scale(Math.max(1.2F, stats.projectileSpeed() * 0.8F)).add(shooterMotion);
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
                    Vec3 targetPos = muzzle.add(normalized.scale(50.0D));
                    double distance = muzzle.distanceTo(targetPos);
                    int particleCount = Math.min(20, (int) (distance / 2.0D));
                    for (int j = 0; j < particleCount; j++) {
                        double fraction = (double) j / particleCount;
                        Vec3 particlePos = muzzle.add(normalized.scale(distance * fraction));
                        serverLevel.sendParticles(
                                ParticleTypes.FLAME,
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.01D, 0.01D, 0.01D, 0.005D
                        );
                        serverLevel.sendParticles(
                                ParticleTypes.SMOKE,
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.01D, 0.01D, 0.01D, 0.005D
                        );
                    }
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
            if (shooter instanceof Skeleton || shooter instanceof GunnerEntity) {
                actualSpread += 2.5F;
            }
            if (shooter instanceof PhantomGunner) {
                actualSpread += 3.0F;
            }
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
                value -> level.playSound(null, x, y, z, value, source, 8.0F, 1.0F),
                () -> level.playSound(null, x, y, z, SoundEvents.CROSSBOW_SHOOT, source, 8.0F, 1.1F)
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

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("info.jeg.damage", String.format("%.1f", stats.damage())));

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
    }
}
