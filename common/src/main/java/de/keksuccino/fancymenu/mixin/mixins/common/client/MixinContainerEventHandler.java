package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.screen.VanillaMouseClickHandlingScreen;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
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
    private void head_mouseClicked_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (this instanceof VanillaMouseClickHandlingScreen) {
            return;
        }
        for (GuiEventListener listener : this.children()) {
            if (listener instanceof FancyMenuUiComponent) {
                if (listener.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(listener);
                    if (button == 0) {
                        this.setDragging(true);
                    }
                    info.cancel();
                    return;
                }
            }
        }
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void head_mouseReleased_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (this instanceof VanillaMouseClickHandlingScreen) {
            return;
        }
        for (GuiEventListener listener : this.children()) {
            if (listener instanceof FancyMenuUiComponent) {
                listener.mouseReleased(mouseX, mouseY, button); // Call mouseReleased for ALL FM listeners
            }
        }
        if (this.getFocused() instanceof FancyMenuUiComponent) {
            if ((button == 0) && this.isDragging()) {
                this.setDragging(false);
            }
            info.cancel();
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
