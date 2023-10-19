package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.RenderUtils;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.keksuccino.fancymenu.events.RenderListBackgroundEvent;

//TODO übernehmen
@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Inject(method = "render", at = @At("HEAD"))
	private void beforeRenderListBackgroundFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {

		Konkrete.getEventHandler().callEventsFor(new RenderListBackgroundEvent.Pre(graphics, (AbstractSelectionList<?>)((Object)this)));

	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;enableScissor(Lnet/minecraft/client/gui/GuiGraphics;)V", shift = At.Shift.AFTER))
	private void afterRenderListBackgroundFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {

		Konkrete.getEventHandler().callEventsFor(new RenderListBackgroundEvent.Post(graphics, (AbstractSelectionList<?>)((Object)this)));
		RenderUtils.bindTexture(Screen.BACKGROUND_LOCATION);

	}

}
