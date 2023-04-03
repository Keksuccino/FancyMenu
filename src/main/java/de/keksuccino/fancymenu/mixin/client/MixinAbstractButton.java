package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetLabelEvent;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO übernehmen 1.19.4 (neue klasse)
@Mixin(AbstractButton.class)
public class MixinAbstractButton {

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;blitNineSliced(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIIIIIII)V"))
    private void redirectRenderWidgetBackground(PoseStack matrix, int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, w, this.getAlpha());
        try {
            Konkrete.getEventHandler().callEventsFor(e);
            w.setAlpha(e.getAlpha());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (!e.isCanceled()) {
            GuiComponent.blitNineSliced(matrix, i1, i2, i3, i4, i5, i6, i7, i8, i9, i10);
        }
        try {
            RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, w, this.getAlpha());
            Konkrete.getEventHandler().callEventsFor(e2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;I)V"), cancellable = true)
    private void beforeRenderLabel(PoseStack matrix, int p_275505_, int p_275674_, float p_275696_, CallbackInfo info) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(matrix, w, this.getAlpha());
        Konkrete.getEventHandler().callEventsFor(e3);
        w.setAlpha(e3.getAlpha());
        if (e3.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.AFTER))
    private void afterRenderLabel(PoseStack matrix, int p_275505_, int p_275674_, float p_275696_, CallbackInfo info) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(matrix, w, this.getAlpha());
        Konkrete.getEventHandler().callEventsFor(e4);
    }

    private float getAlpha() {
        return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
    }

}
