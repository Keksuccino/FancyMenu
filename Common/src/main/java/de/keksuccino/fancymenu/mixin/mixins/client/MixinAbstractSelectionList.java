package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.events.widget.RenderGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.keksuccino.fancymenu.events.widget.RenderGuiListBackgroundEvent;

@Mixin(value = AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Shadow private boolean renderTopAndBottom;

	@Unique private boolean shouldFireRenderHeaderFooterEvents;

	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;isMouseOver(DD)Z", shift = Shift.AFTER))
	private void beforeRenderListBackgroundFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		RenderGuiListBackgroundEvent.Pre e = new RenderGuiListBackgroundEvent.Pre(matrix, (AbstractSelectionList) ((Object)this));
		EventHandler.INSTANCE.postEvent(e);
	}
	
	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getRowLeft()I"))
	private void afterRenderListBackgroundFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		RenderGuiListBackgroundEvent.Post e = new RenderGuiListBackgroundEvent.Post(matrix, (AbstractSelectionList) ((Object)this));
		EventHandler.INSTANCE.postEvent(e);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;disableScissor()V", shift = Shift.AFTER))
	private void beforeRenderListHeaderFooterFancyMenu(PoseStack pose, int $$1, int $$2, float $$3, CallbackInfo info) {
		this.shouldFireRenderHeaderFooterEvents = this.renderTopAndBottom;
		if (this.shouldFireRenderHeaderFooterEvents) {
			RenderGuiListHeaderFooterEvent.Pre e = new RenderGuiListHeaderFooterEvent.Pre(pose, (AbstractSelectionList) ((Object)this));
			EventHandler.INSTANCE.postEvent(e);
			if (e.isCanceled()) this.renderTopAndBottom = false;
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getMaxScroll()I"))
	private void afterRenderListHeaderFooterFancyMenu(PoseStack pose, int $$1, int $$2, float $$3, CallbackInfo info) {
		if (this.shouldFireRenderHeaderFooterEvents) {
			RenderGuiListHeaderFooterEvent.Post e = new RenderGuiListHeaderFooterEvent.Post(pose, (AbstractSelectionList) ((Object)this));
			EventHandler.INSTANCE.postEvent(e);
			this.renderTopAndBottom = true;
		}
	}
	
}
