package de.keksuccino.fancymenu.mixin.mixins.neoforge.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.BiConsumer;

@Mixin(TitleScreen.class)
public class MixinNeoForgeTitleScreen {

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;renderMainMenu(Lnet/minecraft/client/gui/screens/TitleScreen;Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;III)V", remap = false))
    private boolean cancelForgeWarningAboveLogoRenderingFancyMenu(TitleScreen gui, GuiGraphicsExtractor guiGraphics, Font font, int width, int height, int alpha) {
        return false;
    }

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/BiConsumer;)V", remap = false))
    private boolean cancelForgeBrandingRenderingFancyMenu(boolean includeMC, boolean reverse, BiConsumer<Integer, String> lineConsumer) {
        return false;
    }

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/BiConsumer;)V", remap = false))
    private boolean cancelForgeBrandingAboveCopyrightRenderingFancyMenu(BiConsumer<Integer, String> lineConsumer) {
        return false;
    }

}
