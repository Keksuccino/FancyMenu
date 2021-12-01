package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;

@SuppressWarnings("rawtypes")
@Mixin(value = EntryListWidget.class)
public abstract class MixinEntryListWidget {

	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", ordinal = 0, shift = Shift.BEFORE), method = "render", cancellable = false)
	private void onRenderListBackgroundPre(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		
		RenderGuiListBackgroundEvent.Pre e = new RenderGuiListBackgroundEvent.Pre(matrix, (EntryListWidget)((Object)this));
		Konkrete.getEventHandler().callEventsFor(e);
		
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;draw()V", ordinal = 0, shift = Shift.AFTER), method = "render", cancellable = false)
	private void onRenderListBackgroundPost(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		
		RenderGuiListBackgroundEvent.Post e = new RenderGuiListBackgroundEvent.Post(matrix, (EntryListWidget)((Object)this));
		Konkrete.getEventHandler().callEventsFor(e);
		
	}
	
}
