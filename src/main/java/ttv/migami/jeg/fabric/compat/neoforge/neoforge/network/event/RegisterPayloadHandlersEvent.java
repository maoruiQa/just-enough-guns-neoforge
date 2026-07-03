package ttv.migami.jeg.fabric.compat.neoforge.neoforge.network.event;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.network.handling.IPayloadContext;

import java.util.function.BiConsumer;

public class RegisterPayloadHandlersEvent {
    public Registrar registrar(String namespace) {
        return new Registrar();
    }

    public static class Registrar {
        public <T extends CustomPacketPayload> Registrar playToServer(
                CustomPacketPayload.Type<T> type,
                StreamCodec<RegistryFriendlyByteBuf, T> codec,
                BiConsumer<T, IPayloadContext> handler
        ) {
            return this;
        }

        public <T extends CustomPacketPayload> Registrar playToClient(
                CustomPacketPayload.Type<T> type,
                StreamCodec<RegistryFriendlyByteBuf, T> codec,
                BiConsumer<T, IPayloadContext> handler
        ) {
            return this;
        }
    }
}
