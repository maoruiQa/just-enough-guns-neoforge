package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.client.network.ClientPlayHandler;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;
import ttv.migami.jeg.network.BufferUtil;

/**
 * Author: MrCrayfish
 */
public class S2CMessageBulletTrail
{
    private final int[] entityIds;
    private final Vec3[] positions;
    private final Vec3[] motions;
    private final ItemStack item;
    private final int trailColor;
    private final double trailLengthMultiplier;
    private final int life;
    private final double gravity;
    private final int shooterId;
    private final boolean enchanted;
    private final ParticleOptions particleData;
    private final boolean isVisible;

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageBulletTrail> CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeInt(message.entityIds.length);
                for(int i = 0; i < message.entityIds.length; i++)
                {
                    buffer.writeInt(message.entityIds[i]);
                    BufferUtil.writeVec3(buffer, message.positions[i]);
                    BufferUtil.writeVec3(buffer, message.motions[i]);
                }
                buffer.writeItem(message.item);
                buffer.writeVarInt(message.trailColor);
                buffer.writeDouble(message.trailLengthMultiplier);
                buffer.writeInt(message.life);
                buffer.writeDouble(message.gravity);
                buffer.writeInt(message.shooterId);
                buffer.writeBoolean(message.enchanted);
                buffer.writeId(BuiltInRegistries.PARTICLE_TYPE, message.particleData.getType());
                buffer.writeBoolean(message.isVisible);
                message.particleData.writeToNetwork(buffer);
            },
            buffer -> {
                int size = buffer.readInt();
                int[] entityIds = new int[size];
                Vec3[] positions = new Vec3[size];
                Vec3[] motions = new Vec3[size];
                for(int i = 0; i < size; i++)
                {
                    entityIds[i] = buffer.readInt();
                    positions[i] = BufferUtil.readVec3(buffer);
                    motions[i] = BufferUtil.readVec3(buffer);
                }
                ItemStack item = buffer.readItem();
                int trailColor = buffer.readVarInt();
                double trailLengthMultiplier = buffer.readDouble();
                int life = buffer.readInt();
                double gravity = buffer.readDouble();
                int shooterId = buffer.readInt();
                boolean enchanted = buffer.readBoolean();
                ParticleType<?> type = buffer.readById(BuiltInRegistries.PARTICLE_TYPE);
                if (type == null)
                {
                    type = ParticleTypes.CRIT;
                }
                boolean isVisible = buffer.readBoolean();
                ParticleOptions particleData = readParticle(buffer, type);
                return new S2CMessageBulletTrail(entityIds, positions, motions, item, trailColor, trailLengthMultiplier, life, gravity, shooterId, enchanted, particleData, isVisible);
            }
    );

    public static void handle(S2CMessageBulletTrail message, PlayMessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleMessageBulletTrail(message));
        context.setHandled(true);
    }

    public S2CMessageBulletTrail(ProjectileEntity[] spawnedProjectiles, Gun.Projectile projectileProps, int shooterId, ParticleOptions particleData)
    {
        this(spawnedProjectiles, projectileProps, shooterId, particleData, false);
    }

    public S2CMessageBulletTrail(ProjectileEntity[] spawnedProjectiles, Gun.Projectile projectileProps, int shooterId, ParticleOptions particleData, boolean isVisible)
    {
        this.positions = new Vec3[spawnedProjectiles.length];
        this.motions = new Vec3[spawnedProjectiles.length];
        this.entityIds = new int[spawnedProjectiles.length];
        for(int i = 0; i < spawnedProjectiles.length; i++)
        {
            ProjectileEntity projectile = spawnedProjectiles[i];
            this.positions[i] = projectile.position();
            this.motions[i] = projectile.getDeltaMovement();
            this.entityIds[i] = projectile.getId();
        }
        this.item = spawnedProjectiles[0].getItem();
        this.enchanted = spawnedProjectiles[0].getWeapon().isEnchanted();
        this.trailColor = this.enchanted ? 0x9C71FF : projectileProps.getTrailColor();
        this.trailLengthMultiplier = projectileProps.getTrailLengthMultiplier();
        this.life = projectileProps.getLife();
        this.gravity = spawnedProjectiles[0].getModifiedGravity();
        this.shooterId = shooterId;
        this.particleData = particleData;
        this.isVisible = isVisible;
    }

    public S2CMessageBulletTrail(int[] entityIds, Vec3[] positions, Vec3[] motions, ItemStack item, int trailColor, double trailLengthMultiplier, int life, double gravity, int shooterId, boolean enchanted, ParticleOptions particleData, boolean isVisible)
    {
        this.entityIds = entityIds;
        this.positions = positions;
        this.motions = motions;
        this.item = item;
        this.trailColor = trailColor;
        this.trailLengthMultiplier = trailLengthMultiplier;
        this.life = life;
        this.gravity = gravity;
        this.shooterId = shooterId;
        this.enchanted = enchanted;
        this.particleData = particleData;
        this.isVisible = isVisible;
    }

    private static <T extends ParticleOptions> T readParticle(RegistryFriendlyByteBuf buffer, ParticleType<T> type)
    {
        return type.getDeserializer().fromNetwork(type, buffer);
    }

    public int getCount()
    {
        return this.entityIds.length;
    }

    public int[] getEntityIds()
    {
        return this.entityIds;
    }

    public Vec3[] getPositions()
    {
        return this.positions;
    }

    public Vec3[] getMotions()
    {
        return this.motions;
    }

    public int getTrailColor()
    {
        return this.trailColor;
    }

    public double getTrailLengthMultiplier()
    {
        return this.trailLengthMultiplier;
    }

    public int getLife()
    {
        return this.life;
    }

    public ItemStack getItem()
    {
        return this.item;
    }

    public double getGravity()
    {
        return this.gravity;
    }

    public int getShooterId()
    {
        return this.shooterId;
    }

    public boolean isEnchanted()
    {
        return this.enchanted;
    }

    public boolean isVisible()
    {
        return this.isVisible;
    }

    public ParticleOptions getParticleData()
    {
        return this.particleData;
    }
}
