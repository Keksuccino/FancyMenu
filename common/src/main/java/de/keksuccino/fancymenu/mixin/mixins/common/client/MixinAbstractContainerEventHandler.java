package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuInputRouter;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerEventHandler.class)
public abstract class MixinAbstractContainerEventHandler implements ContainerEventHandler {

    /**
     * Mixin 0.8.5 cannot inject into the default methods on the ContainerEventHandler interface. Declaring a concrete
     * override on its vanilla base class keeps the normal default implementation available as the fallback while
     * letting FancyMenu children consume input before overlapping vanilla children. Inventory screens have a dedicated
     * path that must run before slot handling, so they intentionally skip the early route here.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // The Object cast reflects the runtime target type; the mixin class itself cannot extend both vanilla base classes for Java's instanceof analysis.
        if (!((Object)this instanceof AbstractContainerScreen<?>)) {
            GuiEventListener listener = FancyMenuInputRouter.routeMouseClicked(this.children(), mouseX, mouseY, button);
            if (listener != null) {
                this.setFocused(listener);
                if (button == 0) this.setDragging(true);
                return true;
            }
        }
        return ContainerEventHandler.super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Keeps captured FancyMenu releases consumed after pointer movement or focus changes, then falls back to 1.20.1's
     * hovered-child release routing when no FancyMenu component owns the event.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (FancyMenuInputRouter.routeMouseReleased(this.children(), this.getFocused(), mouseX, mouseY, button, FancyMenuInputRouter.MouseReleaseRouting.BROADCAST_FANCYMENU_COMPONENTS)) {
            if ((button == 0) && this.isDragging()) this.setDragging(false);
            return true;
        }
        return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, button);
    }

    @Inject(method = "setFocused", at = @At("HEAD"), cancellable = true)
    private void beforeSetFocusedFancyMenu(GuiEventListener guiEventListener, CallbackInfo info) {
        if ((guiEventListener instanceof NavigatableWidget n) && !n.isFocusable()) {
            info.cancel();
        }
    }

}
