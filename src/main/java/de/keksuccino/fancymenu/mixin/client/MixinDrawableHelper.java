package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.BackgroundHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawableHelper.class)
public class MixinDrawableHelper {

    private static final int SPLASH_BACKGROUND_COLOR = BackgroundHelper.ColorMixer.getArgb(255, 239, 50, 61);

    @Inject(at = @At("HEAD"), method = "fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V", cancellable = true)
    private static void onFill(MatrixStack matrixStack, int minX, int minY, int maxX, int maxY, int color, CallbackInfo info) {

        if (MinecraftClient.getInstance().getOverlay() == null) {
            MixinCache.isSplashScreenRendering = false;
        }

        if (MixinCache.isSplashScreenRendering) {

            info.cancel();

            int backColor = SPLASH_BACKGROUND_COLOR;
            int alpha = MixinCache.currentSplashAlpha;

            Screen current = MinecraftClient.getInstance().currentScreen;
            if ((current != null) && (MenuCustomization.isMenuCustomizable(current) || FancyMenu.config.getOrDefault("preloadanimations", true))) {
                alpha = 255;
            }

            fill(matrixStack.peek().getModel(), minX, minY, maxX, maxY, withAlpha(backColor, alpha));

        }

    }

    @Shadow private static void fill(Matrix4f matrix, int minX, int minY, int maxX, int maxY, int color){}

    private static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

}
