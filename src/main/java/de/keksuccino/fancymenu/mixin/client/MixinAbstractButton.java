package de.keksuccino.fancymenu.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetLabelEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class MixinAbstractButton extends AbstractWidget {

    public MixinAbstractButton(int p_93629_, int p_93630_, int p_93631_, int p_93632_, Component p_93633_) {
        super(p_93629_, p_93630_, p_93631_, p_93632_, p_93633_);
    }

    @WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private boolean wrapBlitSpriteInRenderWidgetFancyMenu(GuiGraphics instance, ResourceLocation resourceLocation, int x, int y, int width, int height) {
        RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(instance, this, this.getAlphaFancyMenu());
        try {
            MinecraftForge.EVENT_BUS.post(e);
            this.setAlpha(e.getAlpha());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (!e.isCanceled()) {
            instance.blitSprite(resourceLocation, x, y, width, height);
        }
        try {
            RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(instance, this, this.getAlphaFancyMenu());
            MinecraftForge.EVENT_BUS.post(e2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"), cancellable = true)
    private void beforeRenderLabelFancyMenu(GuiGraphics graphics, int p_282682_, int p_281714_, float p_282542_, CallbackInfo info) {
        RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(graphics, this, this.getAlphaFancyMenu());
        MinecraftForge.EVENT_BUS.post(e3);
        this.setAlpha(e3.getAlpha());
        if (e3.isCanceled()) {
            info.cancel();
        }
    }

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.AFTER))
    private void afterRenderLabelFancyMenu(GuiGraphics graphics, int p_282682_, int p_281714_, float p_282542_, CallbackInfo info) {
        RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(graphics, this, this.getAlphaFancyMenu());
        MinecraftForge.EVENT_BUS.post(e4);
    }

    @Unique
    private float getAlphaFancyMenu() {
        return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
    }

}
