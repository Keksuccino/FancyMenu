package de.keksuccino.fancymenu.mixin.mixins.fabric.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MixinFabricMouseHandler {

    /**
     * @reason This restores Minecraft's old UI component scroll logic to not only scroll the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z"))
    private boolean wrap_Screen_mouseScrolled_in_onScroll_FancyMenu(Screen instance, double v1, double v2, double v3, Operation<Boolean> original) {
        for (GuiEventListener listener : instance.children()) {
            if (listener instanceof FancyMenuUiComponent) {
                if (listener.mouseScrolled(v1, v2, v3)) {
                    return true;
                }
            }
        }
        return original.call(instance, v1, v2, v3);
    }

}
