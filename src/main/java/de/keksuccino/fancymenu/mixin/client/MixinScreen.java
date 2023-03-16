package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.events.GuiInitCompletedEvent;
import de.keksuccino.fancymenu.events.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

//TODO übernehmen 1.19.4
@Mixin(value = Screen.class)
public class MixinScreen {

	@Inject(at = @At("TAIL"), method = "<init>")
	protected void onConstructInstance(Component title, CallbackInfo info) {
		MenuHandlerBase.cachedOriginalMenuTitles.put(this.getClass(), title);
	}

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init()V"))
	private void beforeInitInInit(Minecraft p_96607_, int p_96608_, int p_96609_, CallbackInfo info) {
		InitOrResizeScreenEvent.Pre e = new InitOrResizeScreenEvent.Pre(((Screen)(Object)this));
		MinecraftForge.EVENT_BUS.post(e);
//		if (e.isCanceled()) {
//			info.cancel();
//		}
	}

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;repositionElements()V"))
	private void beforeRepositionElementsInInit(Minecraft p_96607_, int p_96608_, int p_96609_, CallbackInfo info) {
		InitOrResizeScreenEvent.Pre e = new InitOrResizeScreenEvent.Pre(((Screen)(Object)this));
		MinecraftForge.EVENT_BUS.post(e);
//		if (e.isCanceled()) {
//			info.cancel();
//		}
	}

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
	private void afterInitTriggeredBySetScreen(Minecraft minecraft, int width, int height, CallbackInfo info) {
		InitOrResizeScreenEvent.Post e = new InitOrResizeScreenEvent.Post(((Screen)(Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		GuiInitCompletedEvent e2 = new GuiInitCompletedEvent((Screen) ((Object)this));
		MinecraftForge.EVENT_BUS.post(e2);
	}

	@Inject(method = "resize", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;repositionElements()V"))
	private void beforeRepositionElementsInResize(CallbackInfo info) {
		InitOrResizeScreenEvent.Pre e = new InitOrResizeScreenEvent.Pre(Minecraft.getInstance().screen);
		MinecraftForge.EVENT_BUS.post(e);
	}

	@Inject(method = "resize", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;repositionElements()V", shift = At.Shift.AFTER))
	private void afterRepositionElementsInResize(CallbackInfo info) {
		InitOrResizeScreenEvent.Post e = new InitOrResizeScreenEvent.Post(Minecraft.getInstance().screen);
		MinecraftForge.EVENT_BUS.post(e);
		GuiInitCompletedEvent e2 = new GuiInitCompletedEvent(Minecraft.getInstance().screen);
		MinecraftForge.EVENT_BUS.post(e2);
	}

//	@Inject(at = @At(value = "TAIL"), method = "init(Lnet/minecraft/client/Minecraft;II)V")
//	private void onInitCompleted(Minecraft minecraft, int width, int height, CallbackInfo info) {
//		GuiInitCompletedEvent e = new GuiInitCompletedEvent((Screen) ((Object)this));
//		MinecraftForge.EVENT_BUS.post(e);
//	}
	
}
