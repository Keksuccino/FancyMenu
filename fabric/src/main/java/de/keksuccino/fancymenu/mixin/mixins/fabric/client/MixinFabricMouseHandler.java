package de.keksuccino.fancymenu.mixin.mixins.fabric.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MixinFabricMouseHandler {

    /**
     * @reason This restores Minecraft's old UI component scroll logic to not only scroll the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean wrap_Screen_mouseScrolled_in_onScroll_FancyMenu(Screen instance, double d1, double d2, double d3, double d4, Operation<Boolean> original) {
        for (GuiEventListener listener : instance.children()) {
            if (listener instanceof FancyMenuUiComponent) {
                if (listener.mouseScrolled(d1, d2, d3, d4)) {
                    return true;
                }
            }
        }
        return original.call(instance, d1, d2, d3, d4);
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean wrap_Screen_mouseClicked_in_onButton_FancyMenu(Screen instance, MouseButtonEvent mouseButtonEvent, boolean isDoubleClick, Operation<Boolean> original) {
        boolean cancel = false;
        if (instance != null) {
            for (GuiEventListener listener : instance.children()) {
                if (listener instanceof FancyMenuUiComponent) {
                    if (listener.mouseClicked(mouseButtonEvent, isDoubleClick)) {
                        instance.setFocused(listener);
                        if (mouseButtonEvent.button() == 0) {
                            instance.setDragging(true);
                        }
                        cancel = true;
                        break;
                    }
                }
            }
        }
        return !cancel ? original.call(instance, mouseButtonEvent, isDoubleClick) : true;
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean wrap_Screen_mouseReleased_in_onButton_FancyMenu(Screen instance, MouseButtonEvent mouseButtonEvent, Operation<Boolean> original) {
        boolean cancel = false;
        if (instance != null) {
            for (GuiEventListener listener : instance.children()) {
                if (listener instanceof FancyMenuUiComponent) {
                    if (listener.mouseReleased(mouseButtonEvent)) {
                        if ((mouseButtonEvent.button() == 0) && instance.isDragging()) {
                            instance.setDragging(false);
                        }
                        cancel = true;
                        break;
                    }
                }
            }
        }
        return !cancel ? original.call(instance, mouseButtonEvent) : true;
    }

}
