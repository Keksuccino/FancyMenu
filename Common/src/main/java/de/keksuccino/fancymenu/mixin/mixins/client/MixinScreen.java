package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenBackgroundEvent;
import de.keksuccino.fancymenu.customization.menuhandler.MenuHandlerBase;
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
		MenuHandlerBase.cachedOriginalMenuTitles.put(this.getClass(), title);
	}

	@Inject(method = "renderBackground", at = @At(value = "RETURN"))
	private void afterRenderScreenBackgroundFancyMenu(PoseStack matrix, CallbackInfo info) {
		RenderScreenBackgroundEvent.Post e = new RenderScreenBackgroundEvent.Post((Screen)((Object)this), matrix);
		EventHandler.INSTANCE.postEvent(e);
	}

//	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
//	private void beforeInit(Minecraft minecraft, int width, int height, CallbackInfo info) {
//		InitOrResizeScreenEvent.Pre e = new InitOrResizeScreenEvent.Pre(((Screen)(Object)this));
//		EventHandler.INSTANCE.postEvent(e);
//	}
//
//	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
//	private void afterInit(Minecraft minecraft, int width, int height, CallbackInfo info) {
//		InitOrResizeScreenEvent.Post e = new InitOrResizeScreenEvent.Post(((Screen)(Object)this));
//		EventHandler.INSTANCE.postEvent(e);
//		GuiInitCompletedEvent e2 = new GuiInitCompletedEvent((Screen) ((Object)this));
//		EventHandler.INSTANCE.postEvent(e2);
//	}
//
//	@Inject(method = "resize", at = @At(value = "HEAD"))
//	private void beforeResize(CallbackInfo info) {
//		InitOrResizeScreenEvent.Pre e = new InitOrResizeScreenEvent.Pre(Minecraft.getInstance().screen);
//		EventHandler.INSTANCE.postEvent(e);
//	}
//
//	@Inject(method = "resize", at = @At(value = "TAIL"))
//	private void afterResize(CallbackInfo info) {
//		InitOrResizeScreenEvent.Post e = new InitOrResizeScreenEvent.Post(Minecraft.getInstance().screen);
//		EventHandler.INSTANCE.postEvent(e);
//		GuiInitCompletedEvent e2 = new GuiInitCompletedEvent(Minecraft.getInstance().screen);
//		EventHandler.INSTANCE.postEvent(e2);
//	}

}
