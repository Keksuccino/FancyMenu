package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MixinForgeMouseHandler {

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/event/ForgeEventFactoryClient;onScreenMouseClicked(Lnet/minecraft/client/gui/screens/Screen;DDI)Z"))
    private boolean wrap_Screen_mouseClicked_in_onPress_FancyMenu(Screen instance, double mouseX, double mouseY, int button, Operation<Boolean> original) {
        for (GuiEventListener listener : instance.children()) {
            if (listener instanceof FancyMenuUiComponent) {
                if (listener.mouseClicked(mouseX, mouseY, button)) {
                    instance.setFocused(listener);
                    if (button == 0) {
                        instance.setDragging(true);
                    }
                    return true;
                }
            }
        }
        return original.call(instance, mouseX, mouseY, button);
    }

}
