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

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;setColor(FFFF)V", ordinal = 3, shift = At.Shift.AFTER))
	private void after_renderTopAndBottom_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
		//Render custom header/footer textures after the 1.20.1 vanilla header/footer background.
		EventHandler.INSTANCE.postEvent(new RenderedGuiListHeaderFooterEvent(graphics, (AbstractSelectionList) ((Object)this)));
	}
	
}
