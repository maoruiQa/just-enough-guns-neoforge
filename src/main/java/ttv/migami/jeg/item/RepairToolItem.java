package ttv.migami.jeg.item;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.item.RepairToolItemRenderer;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public class RepairToolItem extends Item implements GeoItem {
    private static final String CONTROLLER = "controller";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.repair_tool.idle");
    private static final double RANGE = 3.0D;
    private static final int USE_COOLDOWN_TICKS = 4;
    private static final int MAX_USES = 2000;
    private static final float VEHICLE_DAMAGE = 0.5F;
    private static final float LIVING_DAMAGE = 3.0F;
    private static final float VEHICLE_PART_REPAIR = 0.5F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RepairToolItem() {
        super(new Properties().rarity(Rarity.COMMON).stacksTo(1).durability(MAX_USES));
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            useRepairTool(serverLevel, player, hand, stack);
        }
        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void useRepairTool(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F).normalize();
        Vec3 end = start.add(direction.scale(RANGE));
        HitResult hit = trace(level, player, start, end);
        if (hit.getType() == HitResult.Type.MISS) {
            playSound(level, player.position());
            return;
        }

        boolean applied = false;
        if (hit instanceof EntityHitResult entityHit) {
            applied = hitEntity(level, player, entityHit.getEntity());
        } else if (hit instanceof BlockHitResult blockHit) {
            BlockState state = level.getBlockState(blockHit.getBlockPos());
            summonHitParticles(level, hit.getLocation(), direction.scale(-1.0D), state);
            applied = true;
        }

        playSound(level, hit.getLocation());
        if (applied && !player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
    }

    public boolean useOnVehicle(ServerLevel level, Player player, InteractionHand hand, ItemStack stack, VehicleEntity vehicle) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        boolean applied = hitVehicle(level, player, vehicle);
        playSound(level, vehicle.position());
        summonHitParticles(level, vehicle.position().add(0.0D, vehicle.getBbHeight() * 0.5D, 0.0D), player.getViewVector(1.0F).scale(-1.0D), null);
        if (applied && !player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        player.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    private boolean hitEntity(ServerLevel level, Player player, Entity target) {
        Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        boolean applied = false;
        if (target instanceof VehicleEntity vehicle) {
            applied = hitVehicle(level, player, vehicle);
        } else if (target instanceof LivingEntity living) {
            applied = living.hurt(ModDamageTypes.causeRepairToolDamage(level.registryAccess(), player), LIVING_DAMAGE);
            living.invulnerableTime = 0;
        }
        summonHitParticles(level, hitPos, player.getViewVector(1.0F).scale(-1.0D), null);
        return applied;
    }

    private boolean hitVehicle(ServerLevel level, Player player, VehicleEntity vehicle) {
        if (player.isShiftKeyDown()) {
            return vehicle.hurtWithRepairTool(ModDamageTypes.causeRepairToolDamage(level.registryAccess(), player), VEHICLE_DAMAGE);
        }
        float hullRepair = VEHICLE_DAMAGE + 0.0025F * vehicle.maxVehicleHealth();
        return vehicle.repairWithTool(hullRepair, VEHICLE_PART_REPAIR);
    }

    private HitResult trace(ServerLevel level, Player player, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistanceSqr = blockHit.getType() == HitResult.Type.MISS ? RANGE * RANGE : blockHit.getLocation().distanceToSqr(start);
        AABB searchBox = player.getBoundingBox().expandTowards(player.getViewVector(1.0F).scale(RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                entity -> entity.isAlive() && !entity.isSpectator() && entity != player && entity != player.getVehicle(),
                maxDistanceSqr
        );
        return entityHit != null ? entityHit : blockHit;
    }

    private void summonHitParticles(ServerLevel level, Vec3 pos, Vec3 direction, @Nullable BlockState state) {
        RandomSource random = level.random;
        Vec3 normal = direction.normalize();
        if (state != null) {
            BlockParticleOption blockParticle = new BlockParticleOption(ParticleTypes.BLOCK, state);
            Vec3 velocity = randomVector(random, normal, 0.08D);
            level.sendParticles(blockParticle, pos.x, pos.y, pos.z, 2, velocity.x, velocity.y, velocity.z, 0.02D);
        }
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 3, 0.03D, 0.03D, 0.03D, 0.01D);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 2, 0.02D, 0.02D, 0.02D, 0.01D);
    }

    private Vec3 randomVector(RandomSource random, Vec3 direction, double spread) {
        return direction.scale(0.05D).add(
                (random.nextDouble() - 0.5D) * spread,
                (random.nextDouble() - 0.5D) * spread,
                (random.nextDouble() - 0.5D) * spread
        );
    }

    private void playSound(ServerLevel level, Vec3 pos) {
        SoundEvent sound = ModSounds.ALL.containsKey(Reference.id("repairing"))
                ? ModSounds.ALL.get(Reference.id("repairing")).get()
                : SoundEvents.FLINTANDSTEEL_USE;
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, 0.7F, 0.95F + level.random.nextFloat() * 0.1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.jeg.repair_tool.vehicle").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("info.jeg.repair_tool.shift").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level instanceof ServerLevel serverLevel) {
            GeoItem.getOrAssignId(stack, serverLevel);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(
                this,
                CONTROLLER,
                0,
                state -> state.setAndContinue(IDLE)
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private RepairToolItemRenderer renderer;

            @Override
            public RepairToolItemRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new RepairToolItemRenderer();
                }
                return renderer;
            }
        });
    }
}
