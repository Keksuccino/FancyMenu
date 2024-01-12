package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.events.widget.RenderGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.keksuccino.fancymenu.events.widget.RenderGuiListBackgroundEvent;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Shadow private boolean renderBackground;

	@Unique private boolean shouldFireRenderHeaderFooterEvents;

	@Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;isMouseOver(DD)Z", shift = Shift.AFTER))
	private void beforeRenderListBackgroundFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
		RenderGuiListBackgroundEvent.Pre e = new RenderGuiListBackgroundEvent.Pre(graphics, (AbstractSelectionList) ((Object)this));
		EventHandler.INSTANCE.postEvent(e);
	}
	
	@Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getRowLeft()I"))
	private void afterRenderListBackgroundFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
		RenderGuiListBackgroundEvent.Post e = new RenderGuiListBackgroundEvent.Post(graphics, (AbstractSelectionList) ((Object)this));
		EventHandler.INSTANCE.postEvent(e);
	}

	@Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;disableScissor()V", shift = Shift.AFTER))
	private void beforeRenderListHeaderFooterFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
		this.shouldFireRenderHeaderFooterEvents = this.renderBackground;
		if (this.shouldFireRenderHeaderFooterEvents) {
			RenderGuiListHeaderFooterEvent.Pre e = new RenderGuiListHeaderFooterEvent.Pre(graphics, (AbstractSelectionList) ((Object)this));
			EventHandler.INSTANCE.postEvent(e);
			if (e.isCanceled()) this.renderBackground = false;
		}
	}

	@Inject(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getMaxScroll()I"))
	private void afterRenderListHeaderFooterFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
		if (this.shouldFireRenderHeaderFooterEvents) {
			RenderGuiListHeaderFooterEvent.Post e = new RenderGuiListHeaderFooterEvent.Post(graphics, (AbstractSelectionList) ((Object)this));
			EventHandler.INSTANCE.postEvent(e);
			this.renderBackground = true;
		}
	}
	
}
