package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.fancymenu.events.ResourceLoadingFadeScreenPostRenderEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = ResourceLoadProgressGui.class)
public class MixinResourceLoadProgressGui {
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V", shift = Shift.BY, by = 1), method = "render", cancellable = true)
	protected void onRender(MatrixStack matrix, int i, int i2, float f, CallbackInfo info) {
		if (Minecraft.getInstance().currentScreen != null) {
			ResourceLoadingFadeScreenPostRenderEvent e = new ResourceLoadingFadeScreenPostRenderEvent(Minecraft.getInstance().currentScreen, matrix);
			MinecraftForge.EVENT_BUS.post(e);
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
	protected void onRenderPre(MatrixStack matrix, int i, int i2, float f, CallbackInfo info) {
		MainMenuHandler.isLoadingScreen = true;
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init(Lnet/minecraft/client/Minecraft;II)V"), method = "render", cancellable = true)
	protected void onRenderInitCurrentScreen(MatrixStack matrix, int i, int i2, float f, CallbackInfo info) {
		if (Minecraft.getInstance().currentScreen != null) {
			MainMenuHandler.isLoadingScreen = false;
		}
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init(Lnet/minecraft/client/Minecraft;II)V", shift = Shift.AFTER), method = "render", cancellable = true)
	protected void onRenderInitCurrentScreenPost(MatrixStack matrix, int i, int i2, float f, CallbackInfo info) {
		if (Minecraft.getInstance().currentScreen != null) {
			CustomizationHelper.reloadSystemAndMenu();
		}
	}
	
}
