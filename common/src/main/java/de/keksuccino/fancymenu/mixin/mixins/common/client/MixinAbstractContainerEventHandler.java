package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerEventHandler.class)
public abstract class MixinAbstractContainerEventHandler implements ContainerEventHandler {

    @Inject(method = "setFocused", at = @At("HEAD"), cancellable = true)
    private void beforeSetFocusedFancyMenu(GuiEventListener guiEventListener, CallbackInfo info) {
        if ((guiEventListener instanceof NavigatableWidget n) && !n.isFocusable()) {
            info.cancel();
        }
    }

}
