package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.BiConsumer;

@Mixin(TitleScreen.class)
public class MixinForgeTitleScreen {

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;renderMainMenu(Lnet/minecraft/client/gui/screens/TitleScreen;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;III)V", remap = false))
    private boolean cancelForgeWarningAboveLogoRenderingFancyMenu(TitleScreen gui, GuiGraphics guiGraphics, Font font, int width, int height, int alpha) {
        return !ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/BiConsumer;)V", remap = false))
    private boolean cancelForgeBrandingRenderingFancyMenu(boolean includeMC, boolean reverse, BiConsumer<Integer, String> lineConsumer) {
        return !ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/BiConsumer;)V", remap = false))
    private boolean cancelForgeBrandingAboveCopyrightRenderingFancyMenu(BiConsumer<Integer, String> lineConsumer) {
        return !ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this));
    }

}
