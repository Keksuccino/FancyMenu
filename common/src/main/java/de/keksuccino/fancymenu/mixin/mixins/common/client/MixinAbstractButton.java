package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.Function;

@SuppressWarnings("unused")
@Mixin(AbstractButton.class)
public class MixinAbstractButton {

    @WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIII)V"))
    private boolean wrapBlitSpriteFancyMenu(GuiGraphics instance, Function<ResourceLocation, RenderType> $$0, ResourceLocation $$1, int $$2, int $$3, int $$4, int $$5, int $$6) {

        AbstractButton button = (AbstractButton)((Object)this);
        return ((CustomizableWidget)this).renderCustomBackgroundFancyMenu(button, instance, button.getX(), button.getY(), button.getWidth(), button.getHeight());

    }

}
