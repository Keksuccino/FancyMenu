package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraftforge.common.MinecraftForge;

@SuppressWarnings("rawtypes")
@Mixin(value = AbstractList.class)
public abstract class MixinAbstractList {

	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;color4f(FFFF)V", ordinal = 0, shift = Shift.BEFORE), method = "render", cancellable = false)
	private void onRenderListBackgroundPre(int mouseX, int mouseY, float partial, CallbackInfo info) {
		
		RenderGuiListBackgroundEvent.Pre e = new RenderGuiListBackgroundEvent.Pre((AbstractList)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V", ordinal = 0, shift = Shift.AFTER), method = "render", cancellable = false)
	private void onRenderListBackgroundPost(int mouseX, int mouseY, float partial, CallbackInfo info) {
		
		RenderGuiListBackgroundEvent.Post e = new RenderGuiListBackgroundEvent.Post((AbstractList)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		
	}
	
}
