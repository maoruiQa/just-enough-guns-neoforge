package ttv.migami.jeg.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("overlayMessageString")
    Component jeg$getOverlayMessageString();

    @Accessor("overlayMessageString")
    void jeg$setOverlayMessageString(Component component);

    @Accessor("overlayMessageTime")
    int jeg$getOverlayMessageTime();

    @Accessor("overlayMessageTime")
    void jeg$setOverlayMessageTime(int time);
}
