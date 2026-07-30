package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuInputRouter;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
     * @reason FancyMenu components must receive clicks before overlapping vanilla children. Container screens use a dedicated path that also blocks slot handling.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void head_mouseClicked_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (this instanceof AbstractContainerScreen<?>) return;
        GuiEventListener listener = FancyMenuInputRouter.routeMouseClicked(this.children(), mouseX, mouseY, button);
        if (listener == null) return;
        this.setFocused(listener);
        if (button == 0) this.setDragging(true);
        info.setReturnValue(true);
    }

    /**
     * @reason Captured FancyMenu releases must remain consumed after pointer movement or focus changes.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void head_mouseReleased_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (!FancyMenuInputRouter.routeMouseReleased(this.children(), this.getFocused(), mouseX, mouseY, button, FancyMenuInputRouter.MouseReleaseRouting.BROADCAST_FANCYMENU_COMPONENTS)) return;
        if ((button == 0) && this.isDragging()) this.setDragging(false);
        info.setReturnValue(true);
    }

    @Shadow
    List<? extends GuiEventListener> children();

    @Shadow
    @Nullable
    GuiEventListener getFocused();

    @Shadow
    void setFocused(@Nullable GuiEventListener focused);

    @Shadow
    boolean isDragging();

    @Shadow
    void setDragging(boolean dragging);

}
