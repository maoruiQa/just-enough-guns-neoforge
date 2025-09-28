package ttv.migami.jeg.network.message;

import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.client.network.ClientPlayHandler;
import ttv.migami.jeg.common.CustomGun;
import ttv.migami.jeg.common.CustomGunLoader;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.NetworkGunManager;

/**
 * Author: MrCrayfish
 */
public record S2CMessageUpdateGuns(ImmutableMap<ResourceLocation, Gun> registeredGuns,
                                   ImmutableMap<ResourceLocation, CustomGun> customGuns)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageUpdateGuns> CODEC = StreamCodec.of(
            (buffer, message) -> {
                writeRegisteredGuns(buffer, message.registeredGuns);
                writeCustomGuns(buffer, message.customGuns);
            },
            buffer -> new S2CMessageUpdateGuns(
                    NetworkGunManager.readRegisteredGuns(buffer),
                    CustomGunLoader.readCustomGuns(buffer)
            )
    );

    public static void handle(S2CMessageUpdateGuns message, PlayMessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleUpdateGuns(message));
        context.setHandled(true);
    }

    public ImmutableMap<ResourceLocation, Gun> getRegisteredGuns()
    {
        return this.registeredGuns;
    }

    public ImmutableMap<ResourceLocation, CustomGun> getCustomGuns()
    {
        return this.customGuns;
    }

    private static void writeRegisteredGuns(RegistryFriendlyByteBuf buffer, ImmutableMap<ResourceLocation, Gun> guns)
    {
        buffer.writeVarInt(guns.size());
        guns.forEach((id, gun) -> {
            buffer.writeResourceLocation(id);
            buffer.writeNbt(gun.serializeNBT());
        });
    }

    private static void writeCustomGuns(RegistryFriendlyByteBuf buffer, ImmutableMap<ResourceLocation, CustomGun> customGuns)
    {
        buffer.writeVarInt(customGuns.size());
        customGuns.forEach((id, customGun) -> {
            buffer.writeResourceLocation(id);
            buffer.writeNbt(customGun.serializeNBT());
        });
    }
}
