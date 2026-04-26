package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.events.screen.ScreenMouseMoveEvent;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.VanillaEvents;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.input.ClicksPerSecondTracker;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.glsl.GlslRuntimeEventTracker;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.jetbrains.annotations.Nullable;
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
    @Shadow @Nullable private MouseButtonInfo activeButton;

    @Unique private final Minecraft mc_FancyMenu = Minecraft.getInstance();
    @Unique private int fakeRightMouse_FancyMenu = 0;
    @Unique private int mappedButtonOnPress_FancyMenu = -1;

    @Inject(method = "onButton", at = @At("HEAD"))
    private void head_onButton_FancyMenu(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo info) {
        if (window != WindowHandler.getWindowHandle()) return;

        boolean pressed = (action == GLFW.GLFW_PRESS);
        int mappedButton = buttonInfo.button();
        int modifiers = buttonInfo.modifiers();
        // Mirror vanilla macOS fake right click behavior (Ctrl + Left Click).
        if (InputQuirks.SIMULATE_RIGHT_CLICK_WITH_LONG_LEFT_CLICK && (mappedButton == GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
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
        VanillaEvents.updateLatestVanillaMouseButtonInfo(new MouseButtonInfo(mappedButton, modifiers));

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
        if (windowPointer == WindowHandler.getWindowHandle()) {
            boolean isDiscrete = mc_FancyMenu.options.discreteMouseScroll().get();
            double wheelSensitivity = mc_FancyMenu.options.mouseWheelSensitivity().get();
            double scrollDeltaX = (isDiscrete ? Math.signum(scrollX) : scrollX) * wheelSensitivity;
            double scrollDeltaY = (isDiscrete ? Math.signum(scrollY) : scrollY) * wheelSensitivity;
            double mX = this.xpos * (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth() / (double)this.mc_FancyMenu.getWindow().getScreenWidth();
            double mY = this.ypos * (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight() / (double)this.mc_FancyMenu.getWindow().getScreenHeight();
            GlslRuntimeEventTracker.onMouseScrolled(mX, mY, scrollDeltaX, scrollDeltaY);
            if (ScreenOverlayHandler.INSTANCE.mouseScrolled(mX, mY, scrollDeltaX, scrollDeltaY)) info.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"), cancellable = true)
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

    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;"), cancellable = true)
    private void before_getOverlay_in_onButton_FancyMenu(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo info) {
        int button = (this.mappedButtonOnPress_FancyMenu != -1) ? this.mappedButtonOnPress_FancyMenu : buttonInfo.button();

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
    @Inject(method = "onButton", at = @At("RETURN"))
    private void return_onButton_FancyMenu(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo info) {
        if (window != WindowHandler.getWindowHandle()) {
            return;
        }
        double guiWidth = this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = this.mc_FancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;
        int mappedButton = (this.mappedButtonOnPress_FancyMenu != -1) ? this.mappedButtonOnPress_FancyMenu : buttonInfo.button();
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

    @Inject(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void before_mouseMoved_in_handleAccumulatedMovement_FancyMenu(CallbackInfo info) {

        double guiWidth = this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = this.mc_FancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;
        double deltaX = this.accumulatedDX * guiWidth / screenWidth;
        double deltaY = this.accumulatedDY * guiHeight / screenHeight;

        EventHandler.INSTANCE.postEvent(new ScreenMouseMoveEvent(this.mc_FancyMenu.screen, mouseX, mouseY, deltaX, deltaY));
        GlslRuntimeEventTracker.onMouseMoved(mouseX, mouseY, deltaX, deltaY);
        if (MCEFUtil.isMCEFLoaded()) BrowserHandler.mouseMoved(mouseX, mouseY);
        ScreenOverlayHandler.INSTANCE.mouseMoved(mouseX, mouseY);
    }

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"))
    private boolean wrap_mouseDragged_in_handleAccumulatedMovement_FancyMenu(Screen instance, MouseButtonEvent event, double dragX, double dragY, Operation<Boolean> original) {
        VanillaEvents.updateLatestVanillaMouseButtonInfo(event.buttonInfo());

        double guiWidth = this.mc_FancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = this.mc_FancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = this.mc_FancyMenu.getWindow().getScreenWidth();
        double screenHeight = this.mc_FancyMenu.getWindow().getScreenHeight();
        double mouseX = this.xpos * guiWidth / screenWidth;
        double mouseY = this.ypos * guiHeight / screenHeight;
        double deltaX = this.accumulatedDX * guiWidth / screenWidth;
        double deltaY = this.accumulatedDY * guiHeight / screenHeight;
        int button = (this.activeButton != null) ? this.activeButton.button() : 0;
        if (ScreenOverlayHandler.INSTANCE.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        return original.call(instance, event, dragX, dragY);
    }

}
