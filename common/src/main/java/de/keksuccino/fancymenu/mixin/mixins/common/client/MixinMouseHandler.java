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
import de.keksuccino.fancymenu.util.rendering.ui.screen.VanillaMouseClickHandlingScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.VanillaMouseScrollHandlingScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Unique private final Minecraft mcFancyMenu = Minecraft.getInstance();

    /**
     * @reason This restores Minecraft's old UI component scroll logic to not only scroll the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean wrap_Screen_mouseScrolled_in_onScroll_FancyMenu(Screen instance, double d1, double d2, double d3, double d4, Operation<Boolean> original) {
        if (instance instanceof VanillaMouseScrollHandlingScreen) {
            return original.call(instance, d1, d2, d3, d4);
        }
        List<GuiEventListener> fmListeners = new ArrayList<>();
        for (GuiEventListener listener : instance.children()) {
            fmListeners.add(listener);
            if (listener instanceof FancyMenuUiComponent) {
                if (listener.mouseScrolled(d1, d2, d3, d4)) {
                    return true;
                }
            }
        }
        // Temporarily remove FM widgets from screen children to not call their event twice
        List<GuiEventListener> originalChildren = new ArrayList<>(instance.children());
        instance.children().removeIf(fmListeners::contains);
        // Call screen event and store result
        boolean b = original.call(instance, d1, d2, d3, d4);;
        // Restore original children
        instance.children().clear();
        ((IMixinScreen)instance).getChildrenFancyMenu().addAll(originalChildren);
        // Return screen event result
        return b;
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"))
    private boolean wrap_mouseClicked_in_onPress_FancyMenu(@NotNull Screen instance, double mouseX, double mouseY, int button, Operation<Boolean> original, long windowPointer, int button2, int action, int modifiers) {
        if (instance instanceof VanillaMouseClickHandlingScreen) {
            return original.call(instance, mouseX, mouseY, button);
        }
        boolean cancel = false;
        long now = Util.getMillis();
        List<GuiEventListener> fmListeners = new ArrayList<>();
        for (GuiEventListener listener : instance.children()) {
            if (listener instanceof FancyMenuUiComponent) {
                fmListeners.add(listener);
                if (listener.mouseClicked(mouseX, mouseY, button)) {
                    instance.setFocused(listener);
                    if (button == 0) {
                        instance.setDragging(true);
                    }
                    cancel = true;
                    break;
                }
            }
        }
        if (cancel) return true;
        // Temporarily remove FM widgets from screen children to not call their event twice
        List<GuiEventListener> originalChildren = new ArrayList<>(instance.children());
        instance.children().removeIf(fmListeners::contains);
        // Call screen event and store result
        boolean b = original.call(instance, mouseX, mouseY, button);
        // Restore original children
        instance.children().clear();
        ((IMixinScreen)instance).getChildrenFancyMenu().addAll(originalChildren);
        // Return screen event result
        return b;
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(DDI)Z"))
    private boolean wrap_mouseReleased_in_onPress_FancyMenu(Screen instance, double mouseX, double mouseY, int button, Operation<Boolean> original, long windowPointer, int button2, int action, int modifiers) {
        if (instance instanceof VanillaMouseClickHandlingScreen) {
            return original.call(instance, mouseX, mouseY, button);
        }
        boolean cancel = false;
        List<GuiEventListener> fmListeners = new ArrayList<>();
        for (GuiEventListener listener : instance.children()) {
            if (listener instanceof FancyMenuUiComponent) {
                fmListeners.add(listener);
                if (listener.mouseReleased(mouseX, mouseY, button)) {
                    if ((button == 0) && instance.isDragging()) {
                        instance.setDragging(false);
                    }
                    cancel = true;
                    break;
                }
            }
        }
        if (cancel) return true;
        // Temporarily remove FM widgets from screen children to not call their event twice
        List<GuiEventListener> originalChildren = new ArrayList<>(instance.children());
        instance.children().removeIf(fmListeners::contains);
        // Call screen event and store result
        boolean b = original.call(instance, mouseX, mouseY, button);
        // Restore original children
        instance.children().clear();
        ((IMixinScreen)instance).getChildrenFancyMenu().addAll(originalChildren);
        // Return screen event result
        return b;
    }

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

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void wrap_mouseMoved_in_handleAccumulatedMovement_FancyMenu(Screen instance, double mouseX, double mouseY, Operation<Void> original) {
        double guiWidth = (double)this.mcFancyMenu.getWindow().getGuiScaledWidth();
        double guiHeight = (double)this.mcFancyMenu.getWindow().getGuiScaledHeight();
        double screenWidth = (double)this.mcFancyMenu.getWindow().getScreenWidth();
        double screenHeight = (double)this.mcFancyMenu.getWindow().getScreenHeight();
        double deltaX = this.accumulatedDX * guiWidth / screenWidth;
        double deltaY = this.accumulatedDY * guiHeight / screenHeight;
        EventHandler.INSTANCE.postEvent(new ScreenMouseMoveEvent(this.mcFancyMenu.screen, mouseX, mouseY, deltaX, deltaY));
        if (MCEFUtil.isMCEFLoaded()) BrowserHandler.mouseMoved(mouseX, mouseY);
        original.call(instance, mouseX, mouseY);
    }

}
