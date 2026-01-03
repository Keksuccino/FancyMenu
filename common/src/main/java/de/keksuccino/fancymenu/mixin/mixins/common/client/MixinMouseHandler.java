package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.events.screen.ScreenMouseMoveEvent;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.input.ClicksPerSecondTracker;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
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
    @Shadow private int activeButton;

    @Unique private final Minecraft mc_FancyMenu = Minecraft.getInstance();

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void head_onScroll_FancyMenu(long windowPointer, double xOffset, double yOffset, CallbackInfo info) {
        if (windowPointer == Minecraft.getInstance().getWindow().getWindow()) {
            Minecraft mc = Minecraft.getInstance();
            boolean bl = mc.options.discreteMouseScroll().get();
            double d = mc.options.mouseWheelSensitivity().get();
            double e = (bl ? Math.signum(xOffset) : xOffset) * d;
            double f = (bl ? Math.signum(yOffset) : yOffset) * d;
            double g = this.xpos * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
            double h = this.ypos * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
            if (PiPWindowHandler.mouseScrolled(g, h, e, f)) info.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"), cancellable = true)
    private void before_mouseScrolled_in_onScroll_FancyMenu(long $$0, double scrollX, double scrollY, CallbackInfo info) {
        boolean isDiscrete = mc_FancyMenu.options.discreteMouseScroll().get();
        double wheelSensitivity = mc_FancyMenu.options.mouseWheelSensitivity().get();
        double scrollDeltaX = (isDiscrete ? Math.signum(scrollX) : scrollX) * wheelSensitivity;
        double scrollDeltaY = (isDiscrete ? Math.signum(scrollY) : scrollY) * wheelSensitivity;
        double mX = this.xpos * (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth() / (double)this.mc_FancyMenu.getWindow().getScreenWidth();
        double mY = this.ypos * (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight() / (double)this.mc_FancyMenu.getWindow().getScreenHeight();
        ScreenMouseScrollEvent.Pre e = new ScreenMouseScrollEvent.Pre(mc_FancyMenu.screen, mX, mY, scrollDeltaX, scrollDeltaY);
        EventHandler.INSTANCE.postEvent(e);
        if (e.isCanceled()) info.cancel();
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z", shift = At.Shift.AFTER))
    private void after_mouseScrolled_in_onScroll_FancyMenu(long $$0, double scrollX, double scrollY, CallbackInfo info) {
        boolean isDiscrete = mc_FancyMenu.options.discreteMouseScroll().get();
        double wheelSensitivity = mc_FancyMenu.options.mouseWheelSensitivity().get();
        double scrollDeltaX = (isDiscrete ? Math.signum(scrollX) : scrollX) * wheelSensitivity;
        double scrollDeltaY = (isDiscrete ? Math.signum(scrollY) : scrollY) * wheelSensitivity;
        double mX = this.xpos * (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth() / (double)this.mc_FancyMenu.getWindow().getScreenWidth();
        double mY = this.ypos * (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight() / (double)this.mc_FancyMenu.getWindow().getScreenHeight();
        ScreenMouseScrollEvent.Post e = new ScreenMouseScrollEvent.Post(mc_FancyMenu.screen, mX, mY, scrollDeltaX, scrollDeltaY);
        EventHandler.INSTANCE.postEvent(e);
    }

    @Inject(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;"), cancellable = true)
    private void before_getOverlay_in_onPress_FancyMenu(long window, int button, int action, int modifiers, CallbackInfo info) {

        boolean clicked = (action == GLFW.GLFW_PRESS);
        double mouseX = this.xpos * (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / (double)Minecraft.getInstance().getWindow().getScreenWidth();
        double mouseY = this.ypos * (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getScreenHeight();

        boolean cancel = false;
        if (clicked) {
            if (PiPWindowHandler.mouseClicked(mouseX, mouseY, button)) cancel = true;
        } else {
            if (PiPWindowHandler.mouseReleased(mouseX, mouseY, button)) cancel = true;
        }
        if (cancel) {
            info.cancel();
            return;
        }

        if (clicked && (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o)) o.mouseClicked(button);

    }

    /**
     * @reason Fire FancyMenu's mouse button listeners after vanilla processing so they run once per press/release.
     */
    @Inject(method = "onPress", at = @At("RETURN"))
    private void return_onPress_FancyMenu(long window, int button, int action, int modifiers, CallbackInfo info) {
        if (window != this.mc_FancyMenu.getWindow().getWindow()) {
            return;
        }
        double guiWidth = this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = this.mc_FancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;
        if (action == GLFW.GLFW_PRESS) {
            ClicksPerSecondTracker.recordClick(button);
            Listeners.ON_MOUSE_BUTTON_CLICKED.onMouseButtonClicked(button, mouseX, mouseY);
        } else if (action == GLFW.GLFW_RELEASE) {
            Listeners.ON_MOUSE_BUTTON_RELEASED.onMouseButtonReleased(button, mouseX, mouseY);
        }
    }

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private void wrap_wrapScreenError_FancyMenu(Runnable action, String errorDesc, String screenName, Operation<Void> original) {

        double guiWidth = this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = this.mc_FancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;
        double deltaX = this.accumulatedDX * guiWidth / screenWidth;
        double deltaY = this.accumulatedDY * guiHeight / screenHeight;

        if ("mouseMoved event handler".equals(errorDesc)) {
            EventHandler.INSTANCE.postEvent(new ScreenMouseMoveEvent(this.mc_FancyMenu.screen, mouseX, mouseY, deltaX, deltaY));
            if (MCEFUtil.isMCEFLoaded()) BrowserHandler.mouseMoved(mouseX, mouseY);
            PiPWindowHandler.mouseMoved(mouseX, mouseY);
        } else if ("mouseDragged event handler".equals(errorDesc)) {
            if (PiPWindowHandler.mouseDragged(mouseX, mouseY, this.activeButton, deltaX, deltaY)) return;
        }

        original.call(action, errorDesc, screenName);

    }

}
