package ttv.migami.jeg.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModEntities;

public class BulletEntity extends Projectile {
    private static final EntityDataAccessor<String> DATA_GUN = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRAIL_COLOR = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TRAIL_LENGTH = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final ResourceLocation FLAMETHROWER_ID = Reference.id("flamethrower");
    private static final ResourceLocation FLARE_GUN_ID = Reference.id("flare_gun");
    private static final ResourceLocation ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher");
    private static final ResourceLocation HYPERSONIC_ID = Reference.id("hypersonic_cannon");
    private static final ResourceLocation TYPHOONEE_ID = Reference.id("typhoonee");
    private static final int FLARE_DETONATE_TICKS = 40; // 2 seconds (20 ticks per second)
    private static final byte FLARE_DETONATION_EVENT = (byte) 98;
    private static final double SKY_RANGE_THRESHOLD = 6.0D;
    private static final double SKY_RANGE_MULTIPLIER = 8.0D;
    private static final int[] FLARE_BLAST_COLORS = new int[] {
        0xFF1A1A, // vivid red
        0x2AFF4C, // neon green
        0x3D7CFF, // electric blue
        0xFFE066, // golden yellow
        0xFF66E5, // magenta accent
        0x66FFF5  // cyan accent
    };
    private static final EntityDataAccessor<Integer> DATA_TICKS_LIVED = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public BulletEntity(Level level, LivingEntity shooter, GunStats stats, Vec3 velocity) {
        this(ModEntities.BULLET.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        this.entityData.set(DATA_GUN, stats.id().toString());
        this.entityData.set(DATA_DAMAGE, stats.damage());
        this.entityData.set(DATA_LIFE, stats.projectileLife());
        this.entityData.set(DATA_TRAIL_COLOR, stats.trailColor());
        this.entityData.set(DATA_TRAIL_LENGTH, stats.clampedTrailLength());
        this.entityData.set(DATA_SIZE, stats.clampedProjectileSize());
        this.setVelocityAndRotation(velocity);
        // Apply gravity to flamethrower and flare gun for realistic projectile physics
        if (stats.id().equals(FLAMETHROWER_ID) || stats.id().equals(FLARE_GUN_ID)) {
            this.setNoGravity(false);
        } else {
            this.setNoGravity(!stats.gravity());
        }
        // DO NOT use noPhysics for flare gun - it prevents proper ticking
        this.refreshDimensions();
        this.setOldPosAndRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_GUN, Reference.id("assault_rifle").toString());
        builder.define(DATA_DAMAGE, 4.0F);
        builder.define(DATA_LIFE, 40);
        builder.define(DATA_TRAIL_COLOR, 0xFFFFFFFF);
        builder.define(DATA_TRAIL_LENGTH, 1.0F);
        builder.define(DATA_SIZE, 0.05F);
        builder.define(DATA_TICKS_LIVED, 0);
    }

    @Override
    public void tick() {
        super.tick();

        ResourceLocation gunId = ResourceLocation.parse(this.entityData.get(DATA_GUN));

        // FLARE GUN: Simple timer-based detonation using entity age (tickCount)
        if (gunId.equals(FLARE_GUN_ID)) {
            // Debug logging to verify tickCount is incrementing
            if (this.tickCount % 10 == 0) {
                System.out.println("[" + (this.level().isClientSide ? "CLIENT" : "SERVER") + "] Flare tickCount: " + this.tickCount + "/" + FLARE_DETONATE_TICKS);
            }

            Vec3 motion = this.getDeltaMovement();
            HitResult collisionResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (collisionResult.getType() != HitResult.Type.MISS) {
                if (!this.level().isClientSide && this.isAlive()) {
                    detonateFlare((ServerLevel)this.level());
                    this.discard();
                }
                return;
            }

            // Entity tickCount is automatically incremented by Minecraft
            if (this.tickCount >= FLARE_DETONATE_TICKS) {
                if (!this.level().isClientSide) {
                    System.out.println("[SERVER] Flare detonating at tick " + this.tickCount);
                    detonateFlare((ServerLevel) this.level());
                    this.discard();
                }
                return;
            }

            // Continue normal movement for flare
            if (this.level().isClientSide) {
                spawnFlareParticles();
            }

            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            if (!this.isNoGravity()) {
                this.setDeltaMovement(motion.x, motion.y - 0.08, motion.z);
            }
            return; // Skip all other bullet logic for flare gun
        }

        // Normal bullet logic below (not flare gun)
        // Increment ticksLived using synced data
        int ticksLived = this.entityData.get(DATA_TICKS_LIVED);
        this.entityData.set(DATA_TICKS_LIVED, ticksLived + 1);

        Vec3 motion = this.getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }

        if (this.isAlive()) {
            // Spawn particles along bullet trail BEFORE moving (so particles spawn along the path)
            if (this.level().isClientSide) {
                if (gunId.equals(FLAMETHROWER_ID)) {
                    spawnFlameParticles();
                } else if (gunId.equals(FLARE_GUN_ID)) {
                    spawnFlareParticles();
                } else {
                    // Spawn particles along the entire movement path
                    spawnBulletTrailParticlesAlongPath(motion);
                }
            }

            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            if (!this.isNoGravity()) {
                // Enhanced gravity for flamethrower and flare gun to simulate realistic projectile drop
                double gravityAccel;
                if (gunId.equals(FLAMETHROWER_ID)) {
                    gravityAccel = 0.1;  // Reduced for minimal drop and maximum range
                } else if (gunId.equals(FLARE_GUN_ID)) {
                    gravityAccel = 0.08; // Flare gun has realistic arc trajectory
                } else {
                    gravityAccel = 0.05;
                }
                this.setDeltaMovement(motion.x, motion.y - gravityAccel, motion.z);
            }
        }

        if (this.entityData.get(DATA_TICKS_LIVED) > this.entityData.get(DATA_LIFE)) {
            // Special effects when projectile lifetime ends
            if (!this.level().isClientSide) {
                if (gunId.equals(FLAMETHROWER_ID)) {
                    igniteArea(this.blockPosition());
                }
            }
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Entity owner = this.getOwner();

        // Flare gun only detonates on timer, not on collision
        ResourceLocation gunId = ResourceLocation.parse(this.entityData.get(DATA_GUN));
        if (gunId.equals(FLARE_GUN_ID)) {
            if (!this.level().isClientSide && this.isAlive()) {
                detonateFlare((ServerLevel) this.level());
                this.discard();
            }
            return;
        }

        if (handleSpecialImpact(result)) {
            this.discard();
            return;
        }
        if (!this.level().isClientSide) {
            float damage = this.entityData.get(DATA_DAMAGE);
            DamageSource source;
            if (owner instanceof LivingEntity livingOwner) {
                source = this.damageSources().mobProjectile(this, livingOwner);
            } else {
                source = this.damageSources().thrown(this, owner);
            }

            if (entity instanceof LivingEntity living) {
                living.hurtServer((ServerLevel)this.level(), source, damage);
            } else {
                entity.hurt(source, damage);
            }
        }

        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Flare gun only detonates on timer, not on collision
        // Check BEFORE calling super to prevent premature discard
        ResourceLocation gunId = ResourceLocation.parse(this.entityData.get(DATA_GUN));
        if (gunId.equals(FLARE_GUN_ID)) {
            // Flare gun projectiles pass through blocks without exploding
            // Do NOT call super.onHitBlock() to prevent entity from being discarded
            return;
        }

        super.onHitBlock(result);

        if (handleSpecialImpact(result)) {
            this.discard();
            return;
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("GunId", this.entityData.get(DATA_GUN));
        output.putFloat("Damage", this.entityData.get(DATA_DAMAGE));
        output.putInt("Life", this.entityData.get(DATA_LIFE));
        output.putInt("TrailColor", this.entityData.get(DATA_TRAIL_COLOR));
        output.putFloat("TrailLength", this.entityData.get(DATA_TRAIL_LENGTH));
        output.putFloat("ProjectileSize", this.entityData.get(DATA_SIZE));
        output.putInt("Ticks", this.entityData.get(DATA_TICKS_LIVED));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_GUN, input.getStringOr("GunId", Reference.id("assault_rifle").toString()));
        this.entityData.set(DATA_DAMAGE, input.getFloatOr("Damage", this.entityData.get(DATA_DAMAGE)));
        this.entityData.set(DATA_LIFE, input.getIntOr("Life", this.entityData.get(DATA_LIFE)));
        this.entityData.set(DATA_TRAIL_COLOR, input.getIntOr("TrailColor", this.entityData.get(DATA_TRAIL_COLOR)));
        this.entityData.set(DATA_TRAIL_LENGTH, input.getFloatOr("TrailLength", this.entityData.get(DATA_TRAIL_LENGTH)));
        this.entityData.set(DATA_SIZE, input.getFloatOr("ProjectileSize", this.entityData.get(DATA_SIZE)));
        this.entityData.set(DATA_TICKS_LIVED, input.getIntOr("Ticks", this.entityData.get(DATA_TICKS_LIVED)));
        this.refreshDimensions();
        this.setVelocityAndRotation(this.getDeltaMovement());
    }

    public GunStats getGunStats() {
        ResourceLocation id = ResourceLocation.parse(this.entityData.get(DATA_GUN));
        GunStats stats = GunDefinitions.ALL.get(id);
        if (stats == null) {
            return new GunStats(id, null, "jeg:mag_fed", 1, 20, 0, 10, this.entityData.get(DATA_DAMAGE), 4F, this.entityData.get(DATA_LIFE), false, 0F, 1F, 1, null, null, null, null, null, null, null, null, this.entityData.get(DATA_SIZE), this.entityData.get(DATA_TRAIL_COLOR), this.entityData.get(DATA_TRAIL_LENGTH));
        }
        return stats;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    public int getTrailColor() {
        return this.entityData.get(DATA_TRAIL_COLOR);
    }

    public float getTrailLengthMultiplier() {
        return this.entityData.get(DATA_TRAIL_LENGTH);
    }

    public float getProjectileSize() {
        return this.entityData.get(DATA_SIZE);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float diameter = Mth.clamp(this.getProjectileSize(), 0.05F, 1.0F);
        return EntityDimensions.scalable(diameter, diameter).withEyeHeight(0.0F);
    }

    public void initialisePosition(Vec3 position) {
        this.setPos(position);
        this.setOldPosAndRot();
    }

    private void setVelocityAndRotation(Vec3 velocity) {
        this.setDeltaMovement(velocity);
        double length = velocity.length();
        if (length <= 1.0E-5D) {
            return;
        }

        Vec3 normalized = velocity.scale(1.0D / length);
        float yaw = (float)(Mth.atan2(normalized.x, normalized.z) * (180F / Math.PI));
        float pitch = (float)(Mth.atan2(normalized.y, Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z)) * (180F / Math.PI));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    private boolean handleSpecialImpact(HitResult result) {
        GunStats stats = getGunStats();
        ResourceLocation id = stats.id();

        if (id.equals(FLAMETHROWER_ID)) {
            if (!this.level().isClientSide) {
                if (result instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
                    Entity hitEntity = entityHit.getEntity();
                    if (hitEntity instanceof LivingEntity living) {
                        living.igniteForSeconds(6);
                    } else {
                        hitEntity.igniteForSeconds(6);
                    }
                }
                igniteArea(BlockPos.containing(result.getLocation()));
            }
            return true;
        }

        if (id.equals(ROCKET_LAUNCHER_ID) || id.equals(HYPERSONIC_ID) || id.equals(TYPHOONEE_ID)) {
            if (!this.level().isClientSide) {
                float power = 2.5F;
                if (id.equals(HYPERSONIC_ID)) {
                    power = 3.5F;
                } else if (id.equals(TYPHOONEE_ID)) {
                    power = 3.1F;
                }
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), power, ExplosionInteraction.MOB);
            }
            return true;
        }

        return false;
    }

    private void igniteArea(BlockPos center) {
        Level level = this.level();
        if (level.isClientSide()) {
            return;
        }

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 1, 1))) {
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            if (!Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                continue;
            }
            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        }
    }

    private void spawnFlameParticles() {
        Level level = this.level();
        if (!level.isClientSide()) {
            return;
        }

        // Spawn flame and smoke particles along the flamethrower stream
        for (int i = 0; i < 3; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            level.addParticle(ParticleTypes.FLAME,
                this.getX() + offsetX,
                this.getY() + offsetY,
                this.getZ() + offsetZ,
                0, 0.02, 0);
        }

        // Add occasional smoke
        if (level.random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                this.getX(),
                this.getY(),
                this.getZ(),
                0, 0.01, 0);
        }

        // Add large smoke particles less frequently
        if (level.random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                this.getX(),
                this.getY(),
                this.getZ(),
                0, 0.015, 0);
        }
    }

    private void spawnBulletTrailParticlesAlongPath(Vec3 motion) {
        Level level = this.level();
        if (!level.isClientSide()) {
            return;
        }

        // Only spawn 1 smoke particle at current position per tick
        // This creates a thin continuous trail as the bullet moves
        // The BulletRenderer handles the visual trail line
        level.addParticle(ParticleTypes.SMOKE,
            this.getX(),
            this.getY(),
            this.getZ(),
            0, 0, 0);
    }

    private void spawnFlareParticles() {
        Level level = this.level();
        if (!level.isClientSide()) {
            return;
        }

        // Spawn bright colored particles along flare trajectory
        for (int i = 0; i < 2; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.1;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.1;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.1;

            level.addParticle(ParticleTypes.FLAME,
                this.getX() + offsetX,
                this.getY() + offsetY,
                this.getZ() + offsetZ,
                0, 0.01, 0);
        }

        // Add smoke trail
        if (level.random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                this.getX(),
                this.getY(),
                this.getZ(),
                0, 0.005, 0);
        }
    }

    private void detonateFlare(ServerLevel serverLevel) {
    	Vec3 pos = this.position();
    	Entity owner = this.getOwner();

    	System.out.println("spawnFlareFireworks broadcast on SERVER at " + pos);

    	// 1) 仍然广播实体事件给正在跟踪该实体的客户端（保持现有机制）
    	serverLevel.broadcastEntityEvent(this, FLARE_DETONATION_EVENT);

    	// Debug: 统计附近（64 块范围）玩家数，便于判定 sendParticles 是否会有人接收
    	//int nearbyPlayers = serverLevel.getPlayers(p -> p.distanceToSqr(pos.x, pos.y, pos.z) <= 64.0D * 64.0D).size();
	//System.out.println("[SERVER] nearby players within 64 blocks: " + nearbyPlayers);


    	// 2) 额外在服务器端向附近玩家发送一些基础粒子和声音（保证可见性）
    	try {
    	    // 爆炸发射器：瞬间的爆炸视觉（常见、可靠）
        	serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            	pos.x, pos.y, pos.z,
            	1, 0.0D, 0.0D, 0.0D, 0.0D);

        	// 大量火花/烟用 FIREWORK + EXPLOSION（这些类型对视觉效果更明显）
        	serverLevel.sendParticles(ParticleTypes.FIREWORK,
        	    pos.x, pos.y, pos.z,
        	    80, 6.0D, 6.0D, 6.0D, 0.15D);

        	serverLevel.sendParticles(ParticleTypes.EXPLOSION,
        	    pos.x, pos.y, pos.z,
        	    12, 3.0D, 3.0D, 3.0D, 0.0D);

        	// 保留火焰与烟雾（补充细节）
        	serverLevel.sendParticles(ParticleTypes.FLAME,
        	    pos.x, pos.y, pos.z,
        	    40, 1.6D, 1.6D, 1.6D, 0.04D);
	
        	serverLevel.sendParticles(ParticleTypes.SMOKE,
        	    pos.x, pos.y, pos.z,
        	    30, 2.0D, 2.0D, 2.0D, 0.02D);

        	// 有色尘埃保留，但数量减小（避免网络/客户端抑制）
        	for (int color : FLARE_BLAST_COLORS) {
        	    serverLevel.sendParticles(new DustParticleOptions(color, 1.2F),
        	        pos.x, pos.y, pos.z,
        	        4, 2.0D, 2.0D, 2.0D, 0.06D);
        	}

        	// 声音：广播给附近玩家
        	serverLevel.playSound(null, pos.x, pos.y, pos.z,
        	    net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
        	    net.minecraft.sounds.SoundSource.PLAYERS,
        	    4.0F, 0.9F + serverLevel.random.nextFloat() * 0.2F);

    	} catch (Exception ex) {
    	    ex.printStackTrace();
    	}

    	// 3) 最后应用服务器端的伤害逻辑（和你原来的实现一致）
    	applyFlareDamage(serverLevel, pos, owner);
	}	



    	private void applyFlareDamage(ServerLevel serverLevel, Vec3 pos, @Nullable Entity owner) {
    	    final float explosionDamage = 12.0F;
    	    final float explosionRadius = 4.0F;

    	    for (Entity entity : serverLevel.getEntities(null, new net.minecraft.world.phys.AABB(
    	            pos.x - explosionRadius, pos.y - explosionRadius, pos.z - explosionRadius,
    	            pos.x + explosionRadius, pos.y + explosionRadius, pos.z + explosionRadius))) {
    	        if (entity instanceof LivingEntity living && entity != owner) {
    	            double distance = entity.position().distanceTo(pos);
    	            if (distance <= explosionRadius) {
    	                float damage = explosionDamage * (1.0F - (float)(distance / explosionRadius));
    	                DamageSource source;
    	                if (owner instanceof LivingEntity livingOwner) {
    	                    source = this.damageSources().mobProjectile(this, livingOwner);
    	                } else {
   	                     source = this.damageSources().thrown(this, owner);
       	             }
       	             living.hurtServer(serverLevel, source, damage);
       	             living.igniteForSeconds(2);
       	         }
       	     }
       	 }
	
        System.out.println("SERVER: Damage applied");
    	}

    	private void spawnFlareExplosionEffectsClient() {
    	    Level level = this.level();
    	    if (!level.isClientSide()) {
    	        return;
    	    }
	
	        Vec3 pos = this.position();
	
	        // Multi-stage burst with different colors for visual clarity
			for (int i = 0; i < 1400; i++) {
            double speed = 0.65 + level.random.nextDouble() * 3.6;
            double angle = level.random.nextDouble() * Math.PI * 2;
            double verticalAngle = (level.random.nextDouble() - 0.5) * Math.PI;

            double offsetX = Math.cos(angle) * Math.cos(verticalAngle) * speed;
            double offsetY = Math.sin(verticalAngle) * speed;
            double offsetZ = Math.sin(angle) * Math.cos(verticalAngle) * speed;

            double spawnScale = 2.2 + level.random.nextDouble() * 1.8;
            double jitterScale = 3.0;
            double spawnX = pos.x + offsetX * spawnScale + (level.random.nextDouble() - 0.5) * jitterScale;
            double spawnY = pos.y + offsetY * spawnScale + (level.random.nextDouble() - 0.5) * jitterScale;
            double spawnZ = pos.z + offsetZ * spawnScale + (level.random.nextDouble() - 0.5) * jitterScale;

            level.addParticle(ParticleTypes.FIREWORK,
               spawnX, spawnY, spawnZ,
               offsetX, offsetY, offsetZ);
	
            if (i % 2 == 0) {
                level.addParticle(ParticleTypes.FLAME,
                    spawnX, spawnY, spawnZ,
                    offsetX * 0.8, offsetY * 0.8, offsetZ * 0.8);
            }

            if (i % 3 == 0) {
                level.addParticle(ParticleTypes.LAVA,
                    spawnX, spawnY, spawnZ,
                    offsetX * 0.6, offsetY * 0.6, offsetZ * 0.6);
            }

            if (i % 4 == 0) {
                level.addParticle(ParticleTypes.END_ROD,
                    spawnX, spawnY, spawnZ,
                    offsetX * 0.9, offsetY * 0.9, offsetZ * 0.9);
            }

            if (i % 5 == 0) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    spawnX, spawnY, spawnZ,
                    offsetX * 0.7, offsetY * 0.7, offsetZ * 0.7);
            }

            if (i % 10 == 0) {
                level.addParticle(ParticleTypes.EXPLOSION,
                    spawnX + offsetX * 0.5, spawnY + offsetY * 0.5, spawnZ + offsetZ * 0.5,
                    0, 0, 0);
            }

            // Add four distinct colored dust streaks for visibility
            if (i % 5 == 0) {
                spawnColoredDustBurst(level, spawnX, spawnY, spawnZ, offsetX, offsetY, offsetZ);
            }

            if (i % 12 == 0) {
                level.addParticle(ParticleTypes.GLOW,
                    spawnX, spawnY, spawnZ,
                    offsetX * 0.45, offsetY * 0.45, offsetZ * 0.45);
            }

            if (i % 14 == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    spawnX, spawnY, spawnZ,
                    offsetX * 0.6, offsetY * 0.6, offsetZ * 0.6);
            }
        }

        // Secondary halo shell so aerial bursts read from a distance
        for (int halo = 0; halo < 420; halo++) {
            double radius = 7.5 + level.random.nextDouble() * 6.5;
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = (level.random.nextDouble() - 0.5) * Math.PI;
            double haloX = pos.x + radius * Math.cos(theta) * Math.cos(phi);
            double haloY = pos.y + radius * Math.sin(phi);
            double haloZ = pos.z + radius * Math.sin(theta) * Math.cos(phi);

            int color = FLARE_BLAST_COLORS[level.random.nextInt(FLARE_BLAST_COLORS.length)];
            level.addParticle(new DustParticleOptions(color, 2.4F),
                haloX, haloY, haloZ,
                0, 0, 0);

            if (halo % 3 == 0) {
                level.addParticle(ParticleTypes.GLOW,
                    haloX, haloY, haloZ,
                    0, 0, 0);
            }
        }

        // Cascading sparks to emphasize vertical visibility
        for (int column = 0; column < 320; column++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double radius = 0.6 + level.random.nextDouble() * 4.0;
            double startX = pos.x + Math.cos(angle) * radius;
            double startZ = pos.z + Math.sin(angle) * radius;
            double startY = pos.y + level.random.nextDouble() * 3.5;
            double velocityX = (level.random.nextDouble() - 0.5) * 0.32;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.32;

            level.addParticle(ParticleTypes.END_ROD,
                startX, startY, startZ,
                velocityX, -0.45 - level.random.nextDouble() * 0.38, velocityZ);

            if (column % 3 == 0) {
                int color = FLARE_BLAST_COLORS[level.random.nextInt(FLARE_BLAST_COLORS.length)];
                level.addParticle(new DustParticleOptions(color, 2.4F),
                    startX, startY, startZ,
                    velocityX * 0.85, -0.38 - level.random.nextDouble() * 0.34, velocityZ * 0.85);
            }
        }

        // Outer ripple to enhance visibility for distant observers
        for (int ring = 0; ring < 220; ring++) {
            double radius = 9.0 + level.random.nextDouble() * 5.0;
            double theta = level.random.nextDouble() * Math.PI * 2;
            double ringX = pos.x + Math.cos(theta) * radius;
            double ringZ = pos.z + Math.sin(theta) * radius;
            double ringY = pos.y + (level.random.nextDouble() - 0.5) * 2.0;

            level.addParticle(ParticleTypes.GLOW,
                ringX, ringY, ringZ,
                0, 0, 0);

            if (ring % 2 == 0) {
                int color = FLARE_BLAST_COLORS[level.random.nextInt(FLARE_BLAST_COLORS.length)];
                level.addParticle(new DustParticleOptions(color, 2.8F),
                    ringX, ringY, ringZ,
                    0, 0, 0);
            }
        }

        // Distant shell for high-altitude flares
        for (int distant = 0; distant < 260; distant++) {
            double radius = 12.0 + level.random.nextDouble() * 6.0;
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = (level.random.nextDouble() - 0.5) * Math.PI;
            double outerX = pos.x + radius * Math.cos(theta) * Math.cos(phi);
            double outerY = pos.y + radius * Math.sin(phi) * 0.7;
            double outerZ = pos.z + radius * Math.sin(theta) * Math.cos(phi);

            int color = FLARE_BLAST_COLORS[level.random.nextInt(FLARE_BLAST_COLORS.length)];
            level.addParticle(new DustParticleOptions(color, 2.2F),
                outerX, outerY, outerZ,
                0, 0, 0);

            if (distant % 4 == 0) {
                level.addParticle(ParticleTypes.EXPLOSION,
                    outerX, outerY, outerZ,
                    0, 0, 0);
            }
        }

        level.playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
            net.minecraft.sounds.SoundSource.PLAYERS,
            4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F), false);

        level.playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
            net.minecraft.sounds.SoundSource.PLAYERS,
            4.0F, 0.9F + level.random.nextFloat() * 0.2F, false);
    }

    @Override
    public void handleEntityEvent(byte event) {
    	if (event == FLARE_DETONATION_EVENT) {
    	    // Debug: 打印客户端是否真收到事件（方便排查网络/追踪问题）
    	    if (this.level().isClientSide) {
    	        System.out.println("[CLIENT] BulletEntity received FLARE_DETONATION_EVENT id="
    	            + this.getId() + " pos=" + this.position());
    	    }
    	    spawnFlareExplosionEffectsClient();
    	    return;
    	}
    	super.handleEntityEvent(event);
    }


    private void spawnColoredDustBurst(Level level, double originX, double originY, double originZ, double dirX, double dirY, double dirZ) {
        double spread = 2.8;
        for (int color : FLARE_BLAST_COLORS) {
            for (int i = 0; i < 5; i++) {
                double velocityScale = 0.9 + level.random.nextDouble() * 2.2;
                level.addParticle(new DustParticleOptions(color, 2.6F),
                    originX + (level.random.nextDouble() - 0.5) * spread,
                    originY + (level.random.nextDouble() - 0.5) * spread,
                    originZ + (level.random.nextDouble() - 0.5) * spread,
                    dirX * velocityScale,
                    dirY * velocityScale,
                    dirZ * velocityScale);
            }
        }
    }
}
