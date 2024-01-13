package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.thread.MainThreadTaskExecutor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.keksuccino.fancymenu.mainwindow.WindowHandler;
import net.minecraft.client.Minecraft;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	//TODO übernehmen 2.14.9
	@Inject(method = "<init>", at = @At("RETURN"))
	private void setCustomWindowIconFancyMenu(GameConfig p_91084_, CallbackInfo info) {
		WindowHandler.updateWindowIcon();
	}

	@Inject(method = "setScreen", at = @At("HEAD"))
	private void beforeSetScreen(Screen screen, CallbackInfo info) {
		//Reset GUI scale in case some layout changed it
		Window m = Minecraft.getInstance().getWindow();
		m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onClientTickPre(CallbackInfo info) {
		for (Runnable r : MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK)) {
			try {
				r.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onClientTickPost(CallbackInfo info) {
		for (Runnable r : MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK)) {
			try {
				r.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	//TODO übernehmen 2.14.9
	@Inject(at = @At(value = "HEAD"), method = "createTitle", cancellable = true)
	private void setCustomWindowTitleFancyMenu(CallbackInfoReturnable<String> info) {
		WindowHandler.readCustomWindowTitleFromConfig();
		String title = WindowHandler.getCustomWindowTitle();
		if (title != null) {
			info.setReturnValue(title);
		}
	}
	
}
