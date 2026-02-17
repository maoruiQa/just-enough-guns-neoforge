package ttv.migami.jeg.gun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;

/**
 * Helper class for handling bullet penetration and block destruction mechanics.
 */
public class BulletPenetrationHelper {

    // Block tags for different bulletproof tiers
    public static final TagKey<Block> TIER_1_BLOCKS = TagKey.create(
        BuiltInRegistries.BLOCK.key(),
        Reference.id("bulletproof_tier_1")
    );

    public static final TagKey<Block> TIER_2_BLOCKS = TagKey.create(
        BuiltInRegistries.BLOCK.key(),
        Reference.id("bulletproof_tier_2")
    );

    public static final TagKey<Block> TIER_3_BLOCKS = TagKey.create(
        BuiltInRegistries.BLOCK.key(),
        Reference.id("bulletproof_tier_3")
    );

    public static final TagKey<Block> PENETRABLE_BLOCKS = TagKey.create(
        BuiltInRegistries.BLOCK.key(),
        Reference.id("penetrable")
    );

    // Bullet type destructive power multipliers
    private static final float PISTOL_POWER = 0.5F;     // 手枪弹破坏力最低
    private static final float SHOTGUN_POWER = 1.0F;    // 霰弹枪中等
    private static final float RIFLE_POWER = 1.5F;      // 步枪破坏力最高
    private static final float DEFAULT_POWER = 0.8F;    // 默认破坏力

    /**
     * Check if a block can be penetrated by bullets (like leaves).
     */
    public static boolean isPenetrable(Level level, BlockState state) {
        return state.is(PENETRABLE_BLOCKS) || state.isAir();
    }

    /**
     * Determine the bulletproof tier of a block.
     * @return 1-3 for tiers, 4 for indestructible, 0 for penetrable
     */
    public static int getBlockTier(Level level, BlockState state) {
        if (isPenetrable(level, state)) {
            return 0; // Penetrable
        }
        if (state.is(TIER_1_BLOCKS)) {
            return 1;
        }
        if (state.is(TIER_2_BLOCKS)) {
            return 2;
        }
        if (state.is(TIER_3_BLOCKS)) {
            return 3;
        }
        return 4; // Indestructible (tier 4)
    }

    /**
     * Get the destructive power multiplier based on ammo type.
     */
    public static float getBulletPower(GunStats stats) {
        if (stats.ammoItem() == null) {
            return DEFAULT_POWER;
        }

        String ammoPath = stats.ammoItem().getPath();

        // Pistol ammo
        if (ammoPath.contains("pistol")) {
            return PISTOL_POWER;
        }

        // Shotgun ammo
        if (ammoPath.contains("shell") || ammoPath.contains("shotgun")) {
            return SHOTGUN_POWER;
        }

        // Rifle ammo
        if (ammoPath.contains("rifle")) {
            return RIFLE_POWER;
        }

        // Special ammo types - treat as rifle power
        if (ammoPath.contains("round") || ammoPath.contains("rocket")) {
            return RIFLE_POWER;
        }

        return DEFAULT_POWER;
    }

    /**
     * Calculate the destruction probability based on tier and bullet power.
     * @param tier Block tier (1-3)
     * @param bulletPower Bullet destructive power multiplier
     * @return Probability between 0.0 and 1.0
     */
    public static float getDestructionProbability(int tier, float bulletPower) {
        return switch (tier) {
            case 1 -> 1.0F; // Tier 1: Always destroyed (100%)
            case 2 -> 0.15F * bulletPower * 0.4F; // Tier 2: 3%-9% based on bullet type (reduced by 60%)
            case 3 -> 0.08F * bulletPower * 0.4F; // Tier 3: 1.6%-4.8% based on bullet type (reduced by 60%)
            default -> 0.0F; // Tier 4 or penetrable: Never destroyed
        };
    }

    /**
     * Attempt to destroy a block based on bullet type and block tier.
     * This should only be called on server side.
     */
    public static void tryDestroyBlock(ServerLevel level, BlockPos pos, GunStats stats) {
        BlockState state = level.getBlockState(pos);

        // Skip if already air or penetrable (should be checked before calling this)
        if (state.isAir() || isPenetrable(level, state)) {
            return;
        }

        int tier = getBlockTier(level, state);

        // Tier 4 blocks are indestructible
        if (tier == 4) {
            return;
        }

        float bulletPower = getBulletPower(stats);
        float destroyChance = getDestructionProbability(tier, bulletPower);

        // Roll for destruction
        if (level.random.nextFloat() < destroyChance) {
            // Destroy the block without dropping items
            level.destroyBlock(pos, false);
        }
    }

    /**
     * Check if shooter has line of sight to target, ignoring penetrable blocks (leaves, vines, etc).
     * This allows gun-wielding mobs to see through foliage and attack players.
     *
     * @param shooter The entity performing the line of sight check
     * @param target The target entity to check visibility for
     * @return true if shooter can see target through penetrable blocks, false otherwise
     */
    public static boolean hasLineOfSightThroughPenetrable(LivingEntity shooter, LivingEntity target) {
        Level level = shooter.level();
        Vec3 shooterEye = shooter.getEyePosition();
        Vec3 targetEye = target.getEyePosition();

        // Perform raycast from shooter to target
        ClipContext clipContext = new ClipContext(
            shooterEye,
            targetEye,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            shooter
        );

        BlockHitResult result = level.clip(clipContext);

        // If no block collision, line of sight is clear
        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        // Check if hit block is penetrable
        BlockPos hitPos = result.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);

        // If we hit a penetrable block (leaves, vines, etc.), consider it as clear line of sight
        return isPenetrable(level, hitState);
    }
}
