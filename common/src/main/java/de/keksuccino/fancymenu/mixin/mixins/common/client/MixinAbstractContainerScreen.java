package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen extends Screen {

    @Unique private static final List<GuiEventListener> CLICKED_WIDGETS_FANCYMENU = new ArrayList<>();

    // Dummy constructor
    private MixinAbstractContainerScreen() {
        super(Component.empty());
    }

    /**
     * @reason This is to make widgets work correctly in Inventory Container screens.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void head_mouseClicked_FancyMenu(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> info) {

        for (GuiEventListener l : this.children()) {
            if ((l instanceof FancyMenuWidget) && this.canClickWidget_FancyMenu(l)) {
                CLICKED_WIDGETS_FANCYMENU.add(l);
                if (l.mouseClicked(event, isDoubleClick)) {
                    info.setReturnValue(true);
                    break;
                }
            }
        }

    }

    /**
     * @reason This is to make widgets work correctly in Inventory Container screens.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void head_mouseReleased_FancyMenu(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {

        for (GuiEventListener l : this.children()) {
            if ((l instanceof FancyMenuWidget) && CLICKED_WIDGETS_FANCYMENU.contains(l)) {
                if (l.mouseReleased(event)) {
                    info.setReturnValue(true);
                    break;
                }
            }
        }

        CLICKED_WIDGETS_FANCYMENU.clear();

    }

    /**
     * @reason This is to make widgets work correctly in Inventory Container screens.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void head_mouseDragged_FancyMenu(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> info) {

        for (GuiEventListener l : this.children()) {
            if ((l instanceof FancyMenuWidget) && CLICKED_WIDGETS_FANCYMENU.contains(l)) {
                if (l.mouseDragged(event, dragX, dragY)) {
                    info.setReturnValue(true);
                    break;
                }
            }
        }

    }

    @Unique
    private boolean canClickWidget_FancyMenu(@NotNull GuiEventListener listener) {
        if (listener instanceof AbstractWidget w) {
            return (w.isHovered() && w.isActive() && w.visible);
        }
        return false;
    }

}
