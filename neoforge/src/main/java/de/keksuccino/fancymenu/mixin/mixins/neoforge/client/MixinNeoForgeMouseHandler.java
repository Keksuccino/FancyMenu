package de.keksuccino.fancymenu.mixin.mixins.neoforge.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinNeoForgeMouseHandler {

    @Shadow private double xpos;
    @Shadow private double ypos;

    @Unique private int cachedMouseButton_FancyMenu = -1;

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

    @Inject(method = "onPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getScreenWidth()I"))
    private void before_getScreenWidth_in_onPress_FancyMenu(long windowPointer, int button, int action, int modifiers, CallbackInfo info) {
        this.cachedMouseButton_FancyMenu = button;
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapWithCondition(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private boolean wrap_Screen_mouseClicked_in_onPress_FancyMenu(Runnable runnable, String message, String className) {
        if ("mouseClicked event handler".equals(message)) {
            Minecraft minecraft = Minecraft.getInstance();
            Screen instance = minecraft.screen;
            if (instance != null) {
                double d = this.xpos * (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
                double e = this.ypos * (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
                for (GuiEventListener listener : instance.children()) {
                    if (listener instanceof FancyMenuUiComponent) {
                        if (listener.mouseClicked(d, e, this.cachedMouseButton_FancyMenu)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapWithCondition(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private boolean wrap_Screen_mouseReleased_in_onPress_FancyMenu(Runnable runnable, String message, String className) {
        if ("mouseReleased event handler".equals(message)) {
            Minecraft minecraft = Minecraft.getInstance();
            Screen instance = minecraft.screen;
            if (instance != null) {
                double d = this.xpos * (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
                double e = this.ypos * (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
                for (GuiEventListener listener : instance.children()) {
                    if (listener instanceof FancyMenuUiComponent) {
                        if (listener.mouseReleased(d, e, this.cachedMouseButton_FancyMenu)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

}
