package de.keksuccino.fancymenu.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;

@Mixin(TitleScreen.class)
public class MixinForgeTitleScreen {

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;renderMainMenu(Lnet/minecraft/client/gui/screens/TitleScreen;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;III)V", remap = false))
    private boolean cancelForgeWarningAboveLogoRenderingFancyMenu(TitleScreen line, PoseStack gui, Font poseStack, int font, int width, int height) {
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
