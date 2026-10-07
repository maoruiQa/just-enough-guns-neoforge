package ttv.migami.jeg.client;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MonitorDistanceTooltip {
    private MonitorDistanceTooltip() {}

    public static void append(Consumer<Component> tooltipAdder, float x, float y, float z) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        double dist = player.position().distanceTo(new Vec3(x, y, z));
        tooltipAdder.accept(Component.translatable("des.jeg.monitor.distance", String.format("%.1fm", dist)).withStyle(ChatFormatting.GRAY));
    }
}
