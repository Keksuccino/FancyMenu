package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.events.screen.ScreenMouseMoveEvent;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.input.ClicksPerSecondTracker;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslRuntimeEventTracker;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 2147483647)
public class MixinMouseHandler {

    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Shadow private int activeButton;

    @Unique private final Minecraft mc_FancyMenu = Minecraft.getInstance();
    @Unique private int fakeRightMouse_FancyMenu = 0;
    @Unique private int mappedButtonOnPress_FancyMenu = -1;
    @Unique private double rawMoveX_FancyMenu;
    @Unique private double rawMoveY_FancyMenu;

    @Inject(method = "onMove", at = @At("HEAD"))
    private void head_onMove_FancyMenu(long windowPointer, double xpos, double ypos, CallbackInfo info) {
        this.rawMoveX_FancyMenu = xpos;
        this.rawMoveY_FancyMenu = ypos;
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void head_onPress_FancyMenu(long window, int button, int action, int modifiers, CallbackInfo info) {
        if (window != this.mc_FancyMenu.getWindow().getWindow()) return;

        boolean pressed = (action == GLFW.GLFW_PRESS);
        int mappedButton = button;
        // Mirror vanilla macOS fake right click behavior (Ctrl + Left Click).
        if (Minecraft.ON_OSX && (mappedButton == GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
            if (pressed) {
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL) {
                    mappedButton = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
                    this.fakeRightMouse_FancyMenu++;
                }
            } else if (this.fakeRightMouse_FancyMenu > 0) {
                mappedButton = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
                this.fakeRightMouse_FancyMenu--;
            }
        }

        double guiWidth = this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = this.mc_FancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;
        MouseUtil.cacheMousePosition(mouseX, mouseY);
        MouseUtil.cacheMouseButtonState(mappedButton, action);
        this.mappedButtonOnPress_FancyMenu = mappedButton;
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void head_onScroll_FancyMenu(long windowPointer, double scrollX, double scrollY, CallbackInfo info) {
        if (windowPointer == Minecraft.getInstance().getWindow().getWindow()) {
            boolean isDiscrete = mc_FancyMenu.options.discreteMouseScroll().get();
            double wheelSensitivity = mc_FancyMenu.options.mouseWheelSensitivity().get();
            double scrollDeltaX = (isDiscrete ? Math.signum(scrollX) : scrollX) * wheelSensitivity;
            double scrollDeltaY = (isDiscrete ? Math.signum(scrollY) : scrollY) * wheelSensitivity;
            double mX = this.xpos * (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth() / (double)this.mc_FancyMenu.getWindow().getScreenWidth();
            double mY = this.ypos * (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight() / (double)this.mc_FancyMenu.getWindow().getScreenHeight();
            GlslRuntimeEventTracker.onMouseScrolled(mX, mY, scrollDeltaX, scrollDeltaY);
            if (ScreenOverlayHandler.INSTANCE.mouseScrolled(mX, mY, scrollDeltaY)) info.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z"), cancellable = true)
    private void before_mouseScrolled_in_onScroll_FancyMenu(long windowPointer, double scrollX, double scrollY, CallbackInfo info) {
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

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z", shift = At.Shift.AFTER))
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

        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) {
            // Consume all mouse button events while the intro overlay is active.
            if (clicked) {
                int mappedButton = (this.mappedButtonOnPress_FancyMenu != -1) ? this.mappedButtonOnPress_FancyMenu : button;
                o.mouseClicked(mappedButton);
            }
            info.cancel();
            return;
        }

        boolean cancel = false;
        if (clicked) {
            if (ScreenOverlayHandler.INSTANCE.mouseClicked(mouseX, mouseY, button)) cancel = true;
        } else {
            if (ScreenOverlayHandler.INSTANCE.mouseReleased(mouseX, mouseY, button)) cancel = true;
        }
        if (cancel) {
            info.cancel();
            return;
        }

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
        int mappedButton = (this.mappedButtonOnPress_FancyMenu != -1) ? this.mappedButtonOnPress_FancyMenu : button;
        this.mappedButtonOnPress_FancyMenu = -1;
        if (action == GLFW.GLFW_PRESS) {
            ClicksPerSecondTracker.recordClick(mappedButton);
            Listeners.ON_MOUSE_BUTTON_CLICKED.onMouseButtonClicked(mappedButton, mouseX, mouseY);
            MouseUtil.onMouseButtonPressed(mappedButton, mouseX, mouseY);
            GlslRuntimeEventTracker.onMouseButtonPressed(mappedButton, mouseX, mouseY);
        } else if (action == GLFW.GLFW_RELEASE) {
            Listeners.ON_MOUSE_BUTTON_RELEASED.onMouseButtonReleased(mappedButton, mouseX, mouseY);
            MouseUtil.onMouseButtonReleased(mappedButton, mouseX, mouseY);
            GlslRuntimeEventTracker.onMouseButtonReleased(mappedButton, mouseX, mouseY);
        }
    }

    @WrapOperation(method = "onMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private void wrap_wrapScreenError_FancyMenu(Runnable action, String errorDesc, String screenName, Operation<Void> original) {

        double guiWidth = this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = this.mc_FancyMenu.getWindow().getScreenHeight();
        double mouseX = this.rawMoveX_FancyMenu * guiWidth / screenWidth;
        double mouseY = this.rawMoveY_FancyMenu * guiHeight / screenHeight;
        double deltaX = (this.rawMoveX_FancyMenu - this.xpos) * guiWidth / screenWidth;
        double deltaY = (this.rawMoveY_FancyMenu - this.ypos) * guiHeight / screenHeight;

        if ("mouseMoved event handler".equals(errorDesc)) {
            EventHandler.INSTANCE.postEvent(new ScreenMouseMoveEvent(this.mc_FancyMenu.screen, mouseX, mouseY, deltaX, deltaY));
            GlslRuntimeEventTracker.onMouseMoved(mouseX, mouseY, deltaX, deltaY);
            if (MCEFUtil.isMCEFLoaded()) BrowserHandler.mouseMoved(mouseX, mouseY);
            ScreenOverlayHandler.INSTANCE.mouseMoved(mouseX, mouseY);
        } else if ("mouseDragged event handler".equals(errorDesc)) {
            if (ScreenOverlayHandler.INSTANCE.mouseDragged(mouseX, mouseY, this.activeButton, deltaX, deltaY)) return;
        }

        original.call(action, errorDesc, screenName);

    }

}
