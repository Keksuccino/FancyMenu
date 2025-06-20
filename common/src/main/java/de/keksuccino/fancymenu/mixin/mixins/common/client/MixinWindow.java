package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.window.FancyWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class MixinWindow implements FancyWindow {

    @Unique double preciseScale_FancyMenu = -1;

    @Inject(method = "setGuiScale", at = @At("HEAD"))
    private void void_before_setGuiScale_FancyMenu(int scale, CallbackInfo info) {
        // Reset precise scale when setting scale via Vanilla method
        this.preciseScale_FancyMenu = -1;
    }

    @Shadow public abstract int getGuiScale();

    @Unique
    @Override
    public double getPreciseGuiScale_FancyMenu() {
        if (this.preciseScale_FancyMenu <= 0) return this.getGuiScale();
        return this.preciseScale_FancyMenu;
    }

    @Unique
    @Override
    public void setPreciseGuiScale_FancyMenu(double scale) {
        this.preciseScale_FancyMenu = scale;
    }

}
