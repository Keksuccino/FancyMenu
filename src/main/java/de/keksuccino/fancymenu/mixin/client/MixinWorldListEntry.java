package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class MixinWorldListEntry {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"), method = "render")
    private void onBlitInRender(GuiGraphics instance, ResourceLocation p_283272_, int p_283605_, int p_281879_, float p_282809_, float p_282942_, int p_281922_, int p_282385_, int p_282596_, int p_281699_) {
        if (p_281699_ == 32) {
            if (FancyMenu.config.getOrDefault("show_world_icons", true)) {
                instance.blit(p_283272_, p_283605_, p_281879_, p_282809_, p_282942_, p_281922_, p_282385_, p_282596_, p_281699_);
            }
        } else {
            instance.blit(p_283272_, p_283605_, p_281879_, p_282809_, p_282942_, p_281922_, p_282385_, p_282596_, p_281699_);
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), method = "render")
    private void onFillInRender(GuiGraphics p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        if (FancyMenu.config.getOrDefault("show_world_icons", true)) {
            p_93173_.fill(p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
        }
    }

}
