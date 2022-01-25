package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldListWidget.Entry.class)
public class MixinWorldListEntry {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawableHelper;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIFFIIII)V"), method = "render")
    private void onBlitInRender(MatrixStack p_93134_, int p_93135_, int p_93136_, float p_93137_, float p_93138_, int p_93139_, int p_93140_, int p_93141_, int p_93142_) {
        if (p_93142_ == 32) {
            if (FancyMenu.config.getOrDefault("show_world_icons", true)) {
                DrawableHelper.drawTexture(p_93134_, p_93135_, p_93136_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
            }
        } else {
            DrawableHelper.drawTexture(p_93134_, p_93135_, p_93136_, p_93137_, p_93138_, p_93139_, p_93140_, p_93141_, p_93142_);
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawableHelper;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V"), method = "render")
    private void onFillInRender(MatrixStack p_93173_, int p_93174_, int p_93175_, int p_93176_, int p_93177_, int p_93178_) {
        if (FancyMenu.config.getOrDefault("show_world_icons", true)) {
            DrawableHelper.fill(p_93173_, p_93174_, p_93175_, p_93176_, p_93177_, p_93178_);
        }
    }

}
