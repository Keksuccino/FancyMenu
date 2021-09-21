package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.BackgroundHelper;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawableHelper.class)
public class MixinDrawableHelper {

    private static final int SPLASH_BACKGROUND_COLOR = BackgroundHelper.ColorMixer.getArgb(255, 239, 50, 61);

    @ModifyVariable(at = @At("HEAD"), method = "fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V", argsOnly = true, index = 5)
    private static int modifyColor(int color) {
        if (FancyMenu.config == null) {
            return color;
        }
        if (MinecraftClient.getInstance().getOverlay() == null) {
            MixinCache.isSplashScreenRendering = false;
        }
        if (MixinCache.isSplashScreenRendering) {
            int backColor = SPLASH_BACKGROUND_COLOR;
            int alpha = MixinCache.currentSplashAlpha;

            Screen current = MinecraftClient.getInstance().currentScreen;
            if ((current != null) && (MenuCustomization.isMenuCustomizable(current) || FancyMenu.config.getOrDefault("preloadanimations", true))) {
                alpha = 255;
            }
            return withAlpha(backColor, alpha);
        }

        return color;
    }

    private static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

}
