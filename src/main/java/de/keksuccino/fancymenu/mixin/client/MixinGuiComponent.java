package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import de.keksuccino.konkrete.annotations.OptifineFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiComponent.class)
public abstract class MixinGuiComponent {

    //TODO übernehmen
//    private static final int LOGO_BACKGROUND_COLOR = FastColor.ARGB32.color(255, 239, 50, 61);

    //TODO übernehmen
    @OptifineFix
    @ModifyVariable(at = @At("HEAD"), method = "fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V", argsOnly = true, index = 5)
    private static int modifyColor(int color) {
        if (FancyMenu.config == null) {
            return color;
        }
        if (Minecraft.getInstance().getOverlay() == null) {
            MixinCache.isSplashScreenRendering = false;
        }
        //TODO übernehmen
        if (MixinCache.isSplashScreenRendering || ((Minecraft.getInstance().getOverlay() != null) && FancyMenu.isOptifineCompatibilityMode() && !FancyMenu.isDrippyLoadingScreenLoaded())) {
            //TODO übernehmen
            int backColor = color;
            int alpha = MixinCache.currentSplashAlpha;

            Screen current = Minecraft.getInstance().screen;
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
