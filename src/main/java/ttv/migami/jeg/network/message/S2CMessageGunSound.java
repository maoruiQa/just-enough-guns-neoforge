package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import ttv.migami.jeg.client.network.ClientPlayHandler;

/**
 * Author: MrCrayfish
 */
public record S2CMessageGunSound(ResourceLocation id, SoundSource category, float x, float y, float z,
                                 float volume, float pitch, int shooterId, boolean muzzle, boolean reload)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageGunSound> CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeResourceLocation(message.id);
                buffer.writeEnum(message.category);
                buffer.writeFloat(message.x);
                buffer.writeFloat(message.y);
                buffer.writeFloat(message.z);
                buffer.writeFloat(message.volume);
                buffer.writeFloat(message.pitch);
                buffer.writeInt(message.shooterId);
                buffer.writeBoolean(message.muzzle);
                buffer.writeBoolean(message.reload);
            },
            buffer -> new S2CMessageGunSound(
                    buffer.readResourceLocation(),
                    buffer.readEnum(SoundSource.class),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            )
    );

    public static void handle(S2CMessageGunSound message, PlayMessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleMessageGunSound(message));
        context.setHandled(true);
    }

    public ResourceLocation getId()
    {
        return this.id;
    }

    public SoundSource getCategory()
    {
        return this.category;
    }

    public float getX()
    {
        return this.x;
    }

    public float getY()
    {
        return this.y;
    }

    public float getZ()
    {
        return this.z;
    }

    public float getVolume()
    {
        return this.volume;
    }

    public float getPitch()
    {
        return this.pitch;
    }

    public int getShooterId()
    {
        return this.shooterId;
    }

    public boolean showMuzzleFlash()
    {
        return this.muzzle;
    }

    public boolean isReload()
    {
        return this.reload;
    }
}
