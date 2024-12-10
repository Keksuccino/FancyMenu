package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
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
public class MixinMouseHandler {

    @Shadow private double xpos;
    @Shadow private double ypos;

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
    private void headOnPressFancyMenu(long window, int button, int $$2, int $$3, CallbackInfo info) {
        if (window == Minecraft.getInstance().getWindow().getWindow()) {
            boolean clicked = $$2 == 1;
            if (clicked && (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o)) o.mouseClicked(button);
        }
    }

    /**
     * @reason This restores Minecraft's old UI component click logic to not only click the hovered component, but all of them. The old logic is only used for FancyMenu's components.
     */
    @WrapOperation(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"))
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
