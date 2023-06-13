package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetLabelEvent;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractButton.class)
public class MixinAbstractButton {

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitNineSliced(Lnet/minecraft/resources/ResourceLocation;IIIIIIIIII)V"))
    private void redirectRenderWidgetBackground(GuiGraphics instance, ResourceLocation p_282543_, int p_281513_, int p_281865_, int p_282482_, int p_282661_, int p_282068_, int p_281294_, int p_281681_, int p_281957_, int p_282300_, int p_282769_) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(instance, w, this.getAlpha());
        try {
            Konkrete.getEventHandler().callEventsFor(e);
            w.setAlpha(e.getAlpha());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (!e.isCanceled()) {
            instance.blitNineSliced(p_282543_, p_281513_, p_281865_, p_282482_, p_282661_, p_282068_, p_281294_, p_281681_, p_281957_, p_282300_, p_282769_);
        }
        try {
            RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(instance, w, this.getAlpha());
            Konkrete.getEventHandler().callEventsFor(e2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"), cancellable = true)
    private void beforeRenderLabel(GuiGraphics graphics, int p_282682_, int p_281714_, float p_282542_, CallbackInfo info) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(graphics, w, this.getAlpha());
        Konkrete.getEventHandler().callEventsFor(e3);
        w.setAlpha(e3.getAlpha());
        if (e3.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.AFTER))
    private void afterRenderLabel(GuiGraphics graphics, int p_282682_, int p_281714_, float p_282542_, CallbackInfo info) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(graphics, w, this.getAlpha());
        Konkrete.getEventHandler().callEventsFor(e4);
    }

    private float getAlpha() {
        return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
    }

}
