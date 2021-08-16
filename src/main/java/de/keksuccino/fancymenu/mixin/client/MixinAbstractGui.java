package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.TransformationMatrix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractGui.class)
public abstract class MixinAbstractGui {

    private static final int SPLASH_BACKGROUND_COLOR = -1;

    @Inject(at = @At("HEAD"), method = "fill(IIIII)V", cancellable = true)
    private static void onFill(int minX, int minY, int maxX, int maxY, int color, CallbackInfo info) {

        if (Minecraft.getInstance().loadingGui == null) {
            MixinCache.isSplashScreenRendering = false;
        }

        if (MixinCache.isSplashScreenRendering) {

            info.cancel();

            int backColor = SPLASH_BACKGROUND_COLOR;
            int alpha = MixinCache.currentSplashAlpha;

            Screen current = Minecraft.getInstance().currentScreen;
            if ((current != null) && (MenuCustomization.isMenuCustomizable(current) || FancyMenu.config.getOrDefault("preloadanimations", true))) {
                alpha = 255;
            }

            fill(TransformationMatrix.identity().getMatrix(), minX, minY, maxX, maxY, withAlpha(backColor, alpha));

        }

    }

    @Shadow private static void fill(Matrix4f matrix, int minX, int minY, int maxX, int maxY, int color){}

    private static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

}
