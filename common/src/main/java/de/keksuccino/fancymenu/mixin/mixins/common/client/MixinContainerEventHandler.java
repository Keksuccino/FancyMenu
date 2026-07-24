package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuInputRouter;
import de.keksuccino.fancymenu.util.rendering.ui.screen.VanillaMouseClickHandlingScreen;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(ContainerEventHandler.class)
public interface MixinContainerEventHandler {

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void head_mouseClicked_FancyMenu(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> info) {
        if ((this instanceof VanillaMouseClickHandlingScreen) || (this instanceof AbstractContainerScreen<?>)) {
            return;
        }
        GuiEventListener listener = FancyMenuInputRouter.routeMouseClicked(this.children(), event, isDoubleClick);
        if (listener != null) {
            if (listener.shouldTakeFocusAfterInteraction()) {
                this.setFocused(listener);
                if (event.button() == 0) {
                    this.setDragging(true);
                }
            }
            info.setReturnValue(true);
        }
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void head_mouseReleased_FancyMenu(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {
        if (this instanceof VanillaMouseClickHandlingScreen) {
            return;
        }
        if (FancyMenuInputRouter.routeMouseReleased(this.children(), this.getFocused(), event, FancyMenuInputRouter.MouseReleaseRouting.BROADCAST_FANCYMENU_COMPONENTS)) {
            if ((event.button() == 0) && this.isDragging()) {
                this.setDragging(false);
            }
            info.setReturnValue(true);
        }
    }

    @Shadow
    List<? extends GuiEventListener> children();

    @Shadow
    @Nullable GuiEventListener getFocused();

    @Shadow
    void setFocused(GuiEventListener focused);

    @Shadow
    boolean isDragging();

    @Shadow
    void setDragging(boolean isDragging);

}
