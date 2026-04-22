package ttv.migami.jeg.item;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ModSpawnEggItem extends SpawnEggItem {
    private final Supplier<EntityType<? extends Mob>> typeSupplier;

    public ModSpawnEggItem(EntityType<? extends Mob> type, Properties properties) {
        super(properties);
        this.typeSupplier = () -> type;
    }

    private EntityType<?> getSpawnType(ItemStack stack) {
        return this.typeSupplier.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(clickedPos);

        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if (blockEntity instanceof SpawnerBlockEntity spawner) {
            EntityType<?> entityType = getSpawnType(stack);
            spawner.getSpawner().setEntityId(entityType, level, level.getRandom(), clickedPos);
            level.sendBlockUpdated(clickedPos, state, state, 3);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, clickedPos);
            stack.consume(1, player);
            return InteractionResult.SUCCESS;
        }

        BlockPos spawnPos = state.getCollisionShape(level, clickedPos).isEmpty()
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());

        EntityType<?> rawType = getSpawnType(stack);
        Mob mob = resolveMobType(rawType).spawn(
                (ServerLevel) level,
                stack,
                player,
                spawnPos,
                net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE,
                true,
                !Objects.equals(clickedPos, spawnPos) && context.getClickedFace() == Direction.UP
        );

        if (mob == null) {
            return InteractionResult.FAIL;
        }

        postSpawn(level, mob, stack, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos blockPos = hitResult.getBlockPos();
        if (!(level.getBlockState(blockPos).getBlock() instanceof LiquidBlock)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            if (!level.mayInteract(player, blockPos) || !player.mayUseItemAt(blockPos, hitResult.getDirection(), stack)) {
                return InteractionResult.FAIL;
            }

            EntityType<?> rawType = getSpawnType(stack);
            Mob mob = resolveMobType(rawType).spawn(
                    (ServerLevel) level,
                    stack,
                    player,
                    blockPos,
                    net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE,
                    false,
                    false
            );

            if (mob == null) {
                return InteractionResult.FAIL;
            }

            postSpawn(level, mob, stack, player);
        }

        return InteractionResult.SUCCESS;
    }

    protected void postSpawn(Level level, Mob mob, ItemStack stack, Player player) {
        mob.applyComponentsFromItemStack(stack);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, mob.position());
        stack.consume(1, player);
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }

    @SuppressWarnings("unchecked")
    private EntityType<? extends Mob> resolveMobType(EntityType<?> type) {
        return (EntityType<? extends Mob>) type;
    }
}
