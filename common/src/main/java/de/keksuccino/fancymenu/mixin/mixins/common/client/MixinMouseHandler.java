package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
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

    @Unique
    private final Minecraft mc_FancyMenu = Minecraft.getInstance();

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z"), cancellable = true)
    private void beforeMouseScrollScreenFancyMenu(long $$0, double $$1, double $$2, CallbackInfo info) {
        double offset = $$2;
        if (Minecraft.ON_OSX && $$2 == 0) {
            offset = $$1;
        }
        double scrollDelta = (this.mc_FancyMenu.options.discreteMouseScroll().get() ? Math.signum(offset) : offset) * this.mc_FancyMenu.options.mouseWheelSensitivity().get();
        double mX = this.xpos * (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth() / (double)this.mc_FancyMenu.getWindow().getScreenWidth();
        double mY = this.ypos * (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight() / (double)this.mc_FancyMenu.getWindow().getScreenHeight();
        ScreenMouseScrollEvent.Pre e = new ScreenMouseScrollEvent.Pre(mc_FancyMenu.screen, mX, mY, scrollDelta);
        EventHandler.INSTANCE.postEvent(e);
        if (e.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z", shift = At.Shift.AFTER))
    private void afterMouseScrollScreenFancyMenu(long $$0, double $$1, double $$2, CallbackInfo info) {
        double offset = $$2;
        if (Minecraft.ON_OSX && $$2 == 0) {
            offset = $$1;
        }
        double scrollDelta = (this.mc_FancyMenu.options.discreteMouseScroll().get() ? Math.signum(offset) : offset) * this.mc_FancyMenu.options.mouseWheelSensitivity().get();
        double mX = this.xpos * (double)this.mc_FancyMenu.getWindow().getGuiScaledWidth() / (double)this.mc_FancyMenu.getWindow().getScreenWidth();
        double mY = this.ypos * (double)this.mc_FancyMenu.getWindow().getGuiScaledHeight() / (double)this.mc_FancyMenu.getWindow().getScreenHeight();
        ScreenMouseScrollEvent.Post e = new ScreenMouseScrollEvent.Post(mc_FancyMenu.screen, mX, mY, scrollDelta);
        EventHandler.INSTANCE.postEvent(e);
    }

    @Inject(method = "onPress", at = @At(value = "HEAD"))
    private void headOnPressFancyMenu(long window, int button, int $$2, int $$3, CallbackInfo info) {
        if (window == Minecraft.getInstance().getWindow().getWindow()) {
            boolean clicked = $$2 == 1;
            if (clicked && (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o)) o.mouseClicked(button);
        }
    }

}
