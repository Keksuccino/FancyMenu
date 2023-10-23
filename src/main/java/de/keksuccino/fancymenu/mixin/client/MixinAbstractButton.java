package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetLabelEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public class MixinAbstractButton {

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void redirectRenderWidgetBackground(GuiGraphics instance, ResourceLocation resourceLocation, int x, int y, int width, int height) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(instance, w, this.getAlpha());
        try {
            MinecraftForge.EVENT_BUS.post(e);
            w.setAlpha(e.getAlpha());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (!e.isCanceled()) {
            instance.blitSprite(resourceLocation, x, y, width, height);
        }
        try {
            RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(instance, w, this.getAlpha());
            MinecraftForge.EVENT_BUS.post(e2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"), cancellable = true)
    private void beforeRenderLabel(GuiGraphics graphics, int p_282682_, int p_281714_, float p_282542_, CallbackInfo info) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(graphics, w, this.getAlpha());
        MinecraftForge.EVENT_BUS.post(e3);
        w.setAlpha(e3.getAlpha());
        if (e3.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.AFTER))
    private void afterRenderLabel(GuiGraphics graphics, int p_282682_, int p_281714_, float p_282542_, CallbackInfo info) {
        AbstractWidget w = ((AbstractWidget)((Object)this));
        RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(graphics, w, this.getAlpha());
        MinecraftForge.EVENT_BUS.post(e4);
    }

    private float getAlpha() {
        return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
    }

}
