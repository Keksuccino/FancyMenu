//TODO übernehmen
package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class MixinWorldListEntry {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIFFIIII)V"), method = "render")
    private void onBlitInRender(PoseStack p_93134_, int p_93135_, int p_93136_, float p_93137_, float p_93138_, int p_93139_, int p_93140_, int p_93141_, int p_93142_) {
        if (p_93142_ == 32) {
            if (FancyMenu.config.getOrDefault("show_world_icons", true)) {
                GuiComponent.blit(p_93134_, p_93135_, p_93136_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
            }
        } else {
            GuiComponent.blit(p_93134_, p_93135_, p_93136_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"), method = "render")
    private void onFillInRender(PoseStack p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        if (FancyMenu.config.getOrDefault("show_world_icons", true)) {
            GuiComponent.fill(p_93173_, p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
        }
    }

}
