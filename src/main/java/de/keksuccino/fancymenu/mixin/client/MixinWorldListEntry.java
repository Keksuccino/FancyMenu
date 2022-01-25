package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiListWorldSelectionEntry.class)
public class MixinWorldListEntry {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawModalRectWithCustomSizedTexture(IIFFIIFF)V"), method = "drawEntry")
    private void onBlitInRender(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        if (textureHeight == 32.0F) {
            if (FancyMenu.config.getOrDefault("show_world_icons", true)) {
                Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
            }
        } else {
            Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V"), method = "drawEntry")
    private void onFillInRender(int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        if (FancyMenu.config.getOrDefault("show_world_icons", true)) {
            Gui.drawRect(p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
        }
    }

}
