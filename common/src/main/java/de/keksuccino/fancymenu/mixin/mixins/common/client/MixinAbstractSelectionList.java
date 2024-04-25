package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.events.widget.RenderGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.keksuccino.fancymenu.events.widget.RenderGuiListBackgroundEvent;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Unique private boolean shouldFireRenderHeaderFooterEvents_FancyMenu;

	@Inject(method = "renderListBackground", at = @At("HEAD"))
	private void head_renderListBackground_FancyMenu(GuiGraphics graphics, CallbackInfo info) {
		RenderGuiListBackgroundEvent.Pre e = new RenderGuiListBackgroundEvent.Pre(graphics, (AbstractSelectionList) ((Object)this));
		EventHandler.INSTANCE.postEvent(e);
	}

	@Inject(method = "renderListBackground", at = @At("RETURN"))
	private void return_renderListBackground_FancyMenu(GuiGraphics graphics, CallbackInfo info) {
		RenderGuiListBackgroundEvent.Post e = new RenderGuiListBackgroundEvent.Post(graphics, (AbstractSelectionList) ((Object)this));
		EventHandler.INSTANCE.postEvent(e);
		//TODO experimental
		RenderGuiListHeaderFooterEvent.Pre e2 = new RenderGuiListHeaderFooterEvent.Pre(graphics, (AbstractSelectionList) ((Object)this));
		EventHandler.INSTANCE.postEvent(e2);
		RenderGuiListHeaderFooterEvent.Post e3 = new RenderGuiListHeaderFooterEvent.Post(graphics, (AbstractSelectionList) ((Object)this));
		EventHandler.INSTANCE.postEvent(e3);
		//--------------------------
	}
	
}
