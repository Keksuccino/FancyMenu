package de.keksuccino.fancymenu.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.widget.RenderWidgetBackgroundEvent;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractButton.class)
public class MixinAbstractButton {

    @WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;blitNineSliced(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIIIIIII)V"))
    private boolean beforeRenderWidgetBackgroundFancyMenu(PoseStack matrix, int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, w, this.getAlpha());
        try {
            EventHandler.INSTANCE.postEvent(e);
            w.setAlpha(e.getAlpha());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (!e.isCanceled()) {
            GuiComponent.blitNineSliced(matrix, i1, i2, i3, i4, i5, i6, i7, i8, i9, i10);
        }
        try {
            RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, w, this.getAlpha());
            EventHandler.INSTANCE.postEvent(e2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private float getAlpha() {
        return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
    }

}
