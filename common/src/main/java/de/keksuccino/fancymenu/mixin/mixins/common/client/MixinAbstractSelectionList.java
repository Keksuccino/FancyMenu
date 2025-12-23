package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.events.widget.RenderedGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Inject(method = "renderListBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private void before_blit_in_renderListBackground_FancyMenu(GuiGraphics graphics, CallbackInfo info) {
		//In 1.20.5+, lists don't really render headers/footers, so lets just render custom header/footer textures after the background
		EventHandler.INSTANCE.postEvent(new RenderedGuiListHeaderFooterEvent(graphics, (AbstractSelectionList) ((Object)this)));
	}
	
}
