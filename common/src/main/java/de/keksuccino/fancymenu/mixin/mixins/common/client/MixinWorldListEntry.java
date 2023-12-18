package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class MixinWorldListEntry {

    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIFFIIII)V"), method = "render")
    private boolean wrapBlitInRenderFancyMenu(PoseStack p_93134_, int p_93135_, int p_93136_, float p_93137_, float p_93138_, int p_93139_, int p_93140_, int p_93141_, int p_93142_) {
        if (p_93142_ == 32) {
            return FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue();
        }
        return true;
    }

    @WrapWithCondition(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"), method = "render")
    private boolean wrapFillInRenderFancyMenu(PoseStack p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        return FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue();
    }

}
