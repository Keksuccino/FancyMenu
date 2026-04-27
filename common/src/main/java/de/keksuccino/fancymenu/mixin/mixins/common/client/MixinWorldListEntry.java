package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class MixinWorldListEntry {

    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"), method = "renderContent")
    private boolean wrapBlitInRenderFancyMenu(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier loc, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        if (textureHeight == 32) {
            return FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue();
        }
        return true;
    }

    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), method = "renderContent")
    private boolean wrapFillInRenderFancyMenu(GuiGraphicsExtractor graphics, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        return FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue();
    }

}
