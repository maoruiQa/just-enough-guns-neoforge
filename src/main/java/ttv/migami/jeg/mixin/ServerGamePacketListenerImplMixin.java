package ttv.migami.jeg.mixin;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "handleRecipeBookSeenRecipePacket", at = @At("TAIL"))
    private void jeg$recipeViewed(ServerboundRecipeBookSeenRecipePacket packet, CallbackInfo ci) {
        var entry = this.player.level().getServer().getRecipeManager().getRecipeFromDisplay(packet.recipe());
        if (entry != null && entry.parent().id().identifier().getNamespace().equals("jeg")) ttv.migami.jeg.advancement.GameplayActions.action(this.player, "recipe_view");
    }

    @Inject(method = "handleRecipeBookChangeSettingsPacket", at = @At("TAIL"))
    private void jeg$recipeBookOpened(ServerboundRecipeBookChangeSettingsPacket packet, CallbackInfo ci) {
        if (!packet.isOpen() || packet.getBookType() != RecipeBookType.CRAFTING
                || !(this.player.containerMenu instanceof InventoryMenu || this.player.containerMenu instanceof CraftingMenu)) return;
        if (this.player.level().getServer().getRecipeManager().getRecipes().stream().anyMatch(recipe -> recipe.id().identifier().getNamespace().equals("jeg")
                && this.player.getRecipeBook().contains(recipe.id()))) {
            ttv.migami.jeg.advancement.GameplayActions.action(this.player, "recipe_view");
        }
    }
}
