package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.events.screen.ScreenMouseMoveEvent;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Unique private final Minecraft mc_FancyMenu = Minecraft.getInstance();

    /**
     * @reason This restores Minecraft's old UI component scroll logic to not only scroll the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean wrap_Screen_mouseScrolled_in_onScroll_FancyMenu(Screen instance, double mouseX, double mouseY, double scrollX, double scrollY, Operation<Boolean> original) {
        this.beforeMouseScrollScreen_FancyMenu(scrollX, scrollY);
        for (GuiEventListener listener : instance.children()) {
            if (listener instanceof FancyMenuUiComponent) {
                if (listener.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    this.afterMouseScrollScreen_FancyMenu(scrollX, scrollY);
                    return true;
                }
            }
        }
        boolean b = original.call(instance, mouseX, mouseY, scrollX, scrollY);
        this.afterMouseScrollScreen_FancyMenu(scrollX, scrollY);
        return b;
    }

    @Unique
    private void beforeMouseScrollScreen_FancyMenu(double scrollX, double scrollY) {
        boolean isDiscrete = mc_FancyMenu.options.discreteMouseScroll().get();
        double wheelSensitivity = mc_FancyMenu.options.mouseWheelSensitivity().get();
        double scrollDeltaX = (isDiscrete ? Math.signum(scrollX) : scrollX) * wheelSensitivity;
        double scrollDeltaY = (isDiscrete ? Math.signum(scrollY) : scrollY) * wheelSensitivity;
        double mX = this.xpos * (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth() / (double)this.mc_FancyMenu.getWindow().getScreenWidth();
        double mY = this.ypos * (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight() / (double)this.mc_FancyMenu.getWindow().getScreenHeight();
        EventHandler.INSTANCE.postEvent(new ScreenMouseScrollEvent.Pre(mc_FancyMenu.screen, mX, mY, scrollDeltaX, scrollDeltaY));
    }

    @Unique
    private void afterMouseScrollScreen_FancyMenu(double scrollX, double scrollY) {
        boolean isDiscrete = mc_FancyMenu.options.discreteMouseScroll().get();
        double wheelSensitivity = mc_FancyMenu.options.mouseWheelSensitivity().get();
        double scrollDeltaX = (isDiscrete ? Math.signum(scrollX) : scrollX) * wheelSensitivity;
        double scrollDeltaY = (isDiscrete ? Math.signum(scrollY) : scrollY) * wheelSensitivity;
        double mX = this.xpos * (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth() / (double)this.mc_FancyMenu.getWindow().getScreenWidth();
        double mY = this.ypos * (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight() / (double)this.mc_FancyMenu.getWindow().getScreenHeight();
        ScreenMouseScrollEvent.Post e = new ScreenMouseScrollEvent.Post(mc_FancyMenu.screen, mX, mY, scrollDeltaX, scrollDeltaY);
        EventHandler.INSTANCE.postEvent(e);
    }

    @Inject(method = "onPress", at = @At(value = "HEAD"))
    private void headOnPressFancyMenu(long window, int button, int action, int modifiers, CallbackInfo info) {
        if (window == Minecraft.getInstance().getWindow().getWindow()) {
            boolean clicked = action == GLFW.GLFW_PRESS;
            if (clicked && (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o)) o.mouseClicked(button);
        }
    }

    /**
     * @reason Fire FancyMenu's mouse button listeners after vanilla processing so they run once per press/release.
     */
    @Inject(method = "onPress", at = @At("RETURN"))
    private void triggerMouseButtonListeners_FancyMenu(long window, int button, int action, int modifiers, CallbackInfo info) {
        if (window != this.mc_FancyMenu.getWindow().getWindow()) {
            return;
        }

        double guiWidth = (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = (double)this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = (double)this.mc_FancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;

        if (action == GLFW.GLFW_PRESS) {
            Listeners.ON_MOUSE_BUTTON_CLICKED.onMouseButtonClicked(button, mouseX, mouseY);
        } else if (action == GLFW.GLFW_RELEASE) {
            Listeners.ON_MOUSE_BUTTON_RELEASED.onMouseButtonReleased(button, mouseX, mouseY);
        }
    }

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void wrap_mouseMoved_in_handleAccumulatedMovement_FancyMenu(Screen instance, double mouseX, double mouseY, Operation<Void> original) {
        double guiWidth = (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = (double)this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = (double)this.mc_FancyMenu.getWindow().getScreenHeight();
        double deltaX = this.accumulatedDX * guiWidth / screenWidth;
        double deltaY = this.accumulatedDY * guiHeight / screenHeight;
        EventHandler.INSTANCE.postEvent(new ScreenMouseMoveEvent(this.mc_FancyMenu.screen, mouseX, mouseY, deltaX, deltaY));
        if (MCEFUtil.isMCEFLoaded()) BrowserHandler.mouseMoved(mouseX, mouseY);
        original.call(instance, mouseX, mouseY);
    }

}
