package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.events.screen.ScreenMouseMoveEvent;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
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

    @Unique private final Minecraft mcFancyMenu = Minecraft.getInstance();

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"), cancellable = true)
    private void beforeMouseScrollScreenFancyMenu(long $$0, double scrollX, double scrollY, CallbackInfo info) {
        boolean isDiscrete = mcFancyMenu.options.discreteMouseScroll().get();
        double wheelSensitivity = mcFancyMenu.options.mouseWheelSensitivity().get();
        double scrollDeltaX = (isDiscrete ? Math.signum(scrollX) : scrollX) * wheelSensitivity;
        double scrollDeltaY = (isDiscrete ? Math.signum(scrollY) : scrollY) * wheelSensitivity;
        double mX = this.xpos * (double)this.mcFancyMenu.getWindow().getGuiScaledWidth() / (double)this.mcFancyMenu.getWindow().getScreenWidth();
        double mY = this.ypos * (double)this.mcFancyMenu.getWindow().getGuiScaledHeight() / (double)this.mcFancyMenu.getWindow().getScreenHeight();
        ScreenMouseScrollEvent.Pre e = new ScreenMouseScrollEvent.Pre(mcFancyMenu.screen, mX, mY, scrollDeltaX, scrollDeltaY);
        EventHandler.INSTANCE.postEvent(e);
        if (e.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z", shift = At.Shift.AFTER))
    private void afterMouseScrollScreenFancyMenu(long $$0, double scrollX, double scrollY, CallbackInfo info) {
        boolean isDiscrete = mcFancyMenu.options.discreteMouseScroll().get();
        double wheelSensitivity = mcFancyMenu.options.mouseWheelSensitivity().get();
        double scrollDeltaX = (isDiscrete ? Math.signum(scrollX) : scrollX) * wheelSensitivity;
        double scrollDeltaY = (isDiscrete ? Math.signum(scrollY) : scrollY) * wheelSensitivity;
        double mX = this.xpos * (double)this.mcFancyMenu.getWindow().getGuiScaledWidth() / (double)this.mcFancyMenu.getWindow().getScreenWidth();
        double mY = this.ypos * (double)this.mcFancyMenu.getWindow().getGuiScaledHeight() / (double)this.mcFancyMenu.getWindow().getScreenHeight();
        ScreenMouseScrollEvent.Post e = new ScreenMouseScrollEvent.Post(mcFancyMenu.screen, mX, mY, scrollDeltaX, scrollDeltaY);
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
        if (window != this.mcFancyMenu.getWindow().getWindow()) {
            return;
        }

        double guiWidth = (double)this.mcFancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = (double)this.mcFancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = (double)this.mcFancyMenu.getWindow().getScreenWidth();
        double screenHeight = (double)this.mcFancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;

        if (action == GLFW.GLFW_PRESS) {
            Listeners.ON_MOUSE_BUTTON_CLICKED.onMouseButtonClicked(button, mouseX, mouseY);
        } else if (action == GLFW.GLFW_RELEASE) {
            Listeners.ON_MOUSE_BUTTON_RELEASED.onMouseButtonReleased(button, mouseX, mouseY);
        }
    }

    @Inject(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private void before_Screen_mouseMoved_FancyMenu(CallbackInfo info) {
        double guiWidth = (double)this.mcFancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = (double)this.mcFancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = (double)this.mcFancyMenu.getWindow().getScreenWidth();
        double screenHeight = (double)this.mcFancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;
        double deltaX = this.accumulatedDX * guiWidth / screenWidth;
        double deltaY = this.accumulatedDY * guiHeight / screenHeight;
        EventHandler.INSTANCE.postEvent(new ScreenMouseMoveEvent(this.mcFancyMenu.screen, mouseX, mouseY, deltaX, deltaY));
        if (MCEFUtil.isMCEFLoaded()) BrowserHandler.mouseMoved(mouseX, mouseY);
        original.call(instance, mouseX, mouseY);
    }

}
