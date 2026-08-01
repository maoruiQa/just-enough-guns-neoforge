package ttv.migami.jeg.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.faction.GunnerFactionRelations;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.SpecialExplosiveItem;
import ttv.migami.jeg.util.SpecialExplosion;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class PlacedExplosiveEntity extends Entity {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REMOTE = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TIMED = SynchedEntityData.defineId(PlacedExplosiveEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable private UUID ownerId;
    @Nullable private UUID attachedEntityId;

    public PlacedExplosiveEntity(EntityType<? extends PlacedExplosiveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public PlacedExplosiveEntity(ServerLevel level, SpecialExplosiveItem.Kind kind, @Nullable Player owner, Vec3 position, float yaw) {
        this(ModEntities.PLACED_EXPLOSIVE.get(), level);
        this.entityData.set(KIND, kind.ordinal());
        this.ownerId = owner == null ? null : owner.getUUID();
        this.setPos(position);
        this.setYRot(yaw);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, SpecialExplosiveItem.Kind.C4.ordinal());
        builder.define(REMOTE, false);
        builder.define(TIMED, false);
    }

    public SpecialExplosiveItem.Kind kind() {
        int value = this.entityData.get(KIND);
        SpecialExplosiveItem.Kind[] values = SpecialExplosiveItem.Kind.values();
        return value >= 0 && value < values.length ? values[value] : SpecialExplosiveItem.Kind.C4;
    }

    public void setRemote(boolean remote) {
        this.entityData.set(REMOTE, remote);
    }

    public void setTimed(boolean timed) {
        this.entityData.set(TIMED, timed);
    }

    public void attachTo(@Nullable Entity entity) {
        this.attachedEntityId = entity == null ? null : entity.getUUID();
    }

    public boolean isRemoteC4OwnedBy(UUID owner) {
        return this.kind() == SpecialExplosiveItem.Kind.C4
                && this.entityData.get(REMOTE)
                && owner.equals(this.ownerId);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.attachedEntityId != null && this.level() instanceof ServerLevel serverLevel) {
            Entity attached = serverLevel.getEntity(this.attachedEntityId);
            if (attached == null || !attached.isAlive()) {
                this.attachedEntityId = null;
            } else {
                this.setPos(attached.position().add(0.0D, attached.getBbHeight() * 0.5D, 0.0D));
            }
        }

        switch (this.kind()) {
            case C4 -> {
                if (!this.entityData.get(REMOTE) && this.tickCount >= 514) {
                    this.detonate();
                }
            }
            case CLAYMORE -> this.tickClaymore();
            case TM_62 -> this.tickTm62();
        }
    }

    private void tickClaymore() {
        if (this.tickCount >= 12000) {
            this.discard();
            return;
        }
        if (this.tickCount < 40) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        Vec3 center = this.position().add(forward.scale(1.5D)).add(0.0D, 0.8D, 0.0D);
        AABB area = new AABB(center, center).inflate(1.25D, 1.1D, 1.25D);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area, this::canTriggerClaymore)) {
            if (target.position().subtract(this.position()).dot(forward) > 0.0D) {
                this.detonate();
                return;
            }
        }
    }

    private boolean canTriggerClaymore(LivingEntity target) {
        if (!target.isAlive() || target.isShiftKeyDown() || target.getUUID().equals(this.ownerId)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        LivingEntity owner = this.ownerEntity();
        return owner == null || (!owner.isAlliedTo(target) && !GunnerFactionRelations.areSameFactionGunners(owner, target));
    }

    private void tickTm62() {
        if (this.entityData.get(TIMED)) {
            if (this.tickCount >= 100) {
                this.detonate();
            }
            return;
        }
        if (this.tickCount < 20) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(0.8D, 0.35D, 0.8D);
        boolean pressed = !this.level().getEntities(this, area, target -> target instanceof VehicleEntity
                || target instanceof LivingEntity living
                && living.isAlive()
                && (living.getBbWidth() >= 0.9F || living.getDeltaMovement().y < -0.15D)).isEmpty();
        if (pressed) {
            this.detonate();
        }
    }

    public void detonate() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.isRemoved()) {
            return;
        }
        Entity owner = this.ownerEntity();
        switch (this.kind()) {
            case C4 -> SpecialExplosion.explode(serverLevel, this, owner, 300.0F, 10.0D);
            case CLAYMORE -> SpecialExplosion.explode(serverLevel, this, owner, 140.0F, 4.0D);
            case TM_62 -> SpecialExplosion.explode(serverLevel, this, owner, 450.0F, 13.0D);
        }
        this.discard();
    }

    @Nullable
    private LivingEntity ownerEntity() {
        if (this.ownerId == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity owner = serverLevel.getEntity(this.ownerId);
        return owner instanceof LivingEntity living ? living : null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player.isShiftKeyDown() && player.getUUID().equals(this.ownerId)) {
            ItemStack stack = this.pickupStack();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            this.discard();
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.kind() == SpecialExplosiveItem.Kind.CLAYMORE) {
            if (this.level() instanceof ServerLevel serverLevel) {
                SpecialExplosion.explode(serverLevel, this, source.getEntity(), 28.0F, 4.0D);
            }
            this.discard();
        }
        return true;
    }

    public ItemStack pickupStack() {
        return switch (this.kind()) {
            case C4 -> {
                ItemStack stack = new ItemStack(ModItems.C4_BOMB.get());
                stack.set(ModDataComponents.C4_REMOTE.get(), this.entityData.get(REMOTE));
                yield stack;
            }
            case CLAYMORE -> new ItemStack(ModItems.CLAYMORE_MINE.get());
            case TM_62 -> new ItemStack(ModItems.TM_62.get());
        };
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(KIND, tag.getInt("Kind"));
        this.entityData.set(REMOTE, tag.getBoolean("Remote"));
        this.entityData.set(TIMED, tag.getBoolean("Timed"));
        if (tag.hasUUID("Owner")) this.ownerId = tag.getUUID("Owner");
        if (tag.hasUUID("Attached")) this.attachedEntityId = tag.getUUID("Attached");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", this.entityData.get(KIND));
        tag.putBoolean("Remote", this.entityData.get(REMOTE));
        tag.putBoolean("Timed", this.entityData.get(TIMED));
        if (this.ownerId != null) tag.putUUID("Owner", this.ownerId);
        if (this.attachedEntityId != null) tag.putUUID("Attached", this.attachedEntityId);
    }
}
