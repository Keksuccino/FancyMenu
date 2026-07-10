package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.MenuBackgroundReplacementController;
import de.keksuccino.fancymenu.util.rendering.ui.screen.MenuBackgroundReplacementPolicy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.ExperimentsScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ExperimentsScreen.class)
public abstract class MixinExperimentsScreen {

    /**
     * @reason The legacy screen draws a bounded dirt panel after its full-screen background, which would cover the replacement.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private boolean cancel_boundedBackgroundBlit_FancyMenu(GuiGraphics graphics, ResourceLocation location, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        Screen screen = (Screen)(Object)this;
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
        boolean replacementRendered = ((Object)this) instanceof MenuBackgroundReplacementController controller && controller.isMenuBackgroundReplacementRenderedFancyMenu();
        boolean hasScreenMenuBackgrounds = (layer != null) && ScreenCustomization.isCustomizationEnabledForScreen(screen) && !layer.layoutBase.menuBackgrounds.isEmpty();
        return MenuBackgroundReplacementPolicy.shouldRenderLegacyBoundedPanel(replacementRendered, hasScreenMenuBackgrounds);
    }

}
