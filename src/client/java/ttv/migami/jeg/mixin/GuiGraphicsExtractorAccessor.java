package ttv.migami.jeg.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
    @Accessor("guiRenderState")
    GuiRenderState jeg$getGuiRenderState();

    @Accessor("scissorStack")
    ScissorStackAccess jeg$getScissorStack();

    @Mixin(targets = "net.minecraft.client.gui.GuiGraphicsExtractor$ScissorStack")
    interface ScissorStackAccess {
        @Invoker("peek")
        ScreenRectangle jeg$peek();
    }
}
