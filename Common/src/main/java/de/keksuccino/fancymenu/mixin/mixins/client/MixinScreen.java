package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.events.screen.RenderedScreenBackgroundEvent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class MixinScreen {

	@Unique private static final Logger LOGGER = LogManager.getLogger();

	@Inject(method = "renderBackground", at = @At(value = "RETURN"))
	private void afterRenderScreenBackgroundFancyMenu(PoseStack matrix, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), matrix));
	}

	//TODO remove debug
	@Inject(method = "addWidget", at = @At(value = "HEAD", remap = false), remap = false)
	private <T extends GuiEventListener & NarratableEntry> void onAddWidgetFancyMenu(T widget, CallbackInfoReturnable<T> info) {
		if (widget instanceof AbstractWidget a) {
			LOGGER.info("################ ADDING WIDGET: " + a + " | " + a.getMessage());
		} else {
			LOGGER.info("################ ADDING WIDGET: " + widget);
		}
	}

}
