package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class MixinWorldListEntry {

    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"), method = "render")
    private boolean wrapBlitInRenderFancyMenu(GuiGraphics instance, RenderPipeline $$0, ResourceLocation $$1, int $$2, int $$3, float $$4, float $$5, int $$6, int $$7, int $$8, int i) {
        if (i == 32) {
            return FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue();
        }
        return true;
    }

    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), method = "render")
    private boolean wrapFillInRenderFancyMenu(GuiGraphics graphics, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        return FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue();
    }

}
