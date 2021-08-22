package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import net.minecraft.client.gui.GuiSlot;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = GuiSlot.class)
public abstract class MixinGuiSlot {

	@Inject(at = @At(value = "HEAD"), method = "drawContainerBackground", cancellable = false, remap = false)
	private void onRenderListBackgroundPre(CallbackInfo info) {
		if (!FancyMenu.isKonkreteLoaded()) {
			return;
		}
		
		RenderGuiListBackgroundEvent.Pre e = new RenderGuiListBackgroundEvent.Pre((GuiSlot)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
	}

	@Inject(at = @At(value = "TAIL"), method = "drawContainerBackground", cancellable = false, remap = false)
	private void onRenderListBackgroundPost(CallbackInfo info) {
		if (!FancyMenu.isKonkreteLoaded()) {
			return;
		}

		RenderGuiListBackgroundEvent.Post e = new RenderGuiListBackgroundEvent.Post((GuiSlot)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		
	}
	
}
