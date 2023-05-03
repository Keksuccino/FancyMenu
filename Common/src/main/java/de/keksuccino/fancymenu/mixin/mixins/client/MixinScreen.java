package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void onConstructFancyMenu(Component title, CallbackInfo info) {
		ScreenCustomizationLayer.cachedOriginalMenuTitles.put(this.getClass(), title);
	}

	@Inject(method = "renderBackground", at = @At(value = "RETURN"))
	private void afterRenderScreenBackgroundFancyMenu(PoseStack matrix, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), matrix));
	}

}
