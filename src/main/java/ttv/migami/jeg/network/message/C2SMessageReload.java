package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import ttv.migami.jeg.event.GunReloadEvent;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.AnimatedGunItem;

/**
 * Author: MrCrayfish
 */
public record C2SMessageReload(boolean reload)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageReload> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeBoolean(message.reload),
            buffer -> new C2SMessageReload(buffer.readBoolean())
    );

    public static void handle(C2SMessageReload message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null && !player.isSpectator())
            {
                ModSyncedDataKeys.RELOADING.setValue(player, message.reload); // keep vanilla logic
                if(!message.reload)
                {
                    return;
                }

                ItemStack gun = player.getMainHandItem();
                var pre = new GunReloadEvent.Pre(player, gun);
                NeoForge.EVENT_BUS.post(pre);
                if (pre.isCanceled())
                {
                    ModSyncedDataKeys.RELOADING.setValue(player, false);
                    return;
                }

                CompoundTag tag = gun.getTag();
                if (gun.getItem() instanceof AnimatedGunItem && tag != null)
                {
                    tag.putBoolean("IsReloading", true);
                }

                NeoForge.EVENT_BUS.post(new GunReloadEvent.Post(player, gun));
            }
        });
        context.setHandled(true);
    }
}
