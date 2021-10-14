package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.events.GuiInitCompletedEvent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = GuiScreen.class)
public class MixinGuiScreen {

	@Inject(at = @At(value = "TAIL"), method = "setWorldAndResolution", cancellable = false)
	private void onInitCompleted(CallbackInfo info) {
		if (!FancyMenu.isKonkreteLoaded()) {
			return;
		}

		GuiInitCompletedEvent e = new GuiInitCompletedEvent((GuiScreen)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
	}
	
}
