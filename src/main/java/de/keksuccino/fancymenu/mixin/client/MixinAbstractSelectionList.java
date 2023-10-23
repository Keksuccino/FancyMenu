package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import net.minecraftforge.common.MinecraftForge;

@SuppressWarnings("rawtypes")
@Mixin(value = AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;isMouseOver(DD)Z", shift = Shift.AFTER), method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
	private void onRenderListBackgroundPre(GuiGraphics graphics, int p_283242_, int p_282891_, float p_283683_, CallbackInfo info) {
		
		RenderGuiListBackgroundEvent.Pre e = new RenderGuiListBackgroundEvent.Pre(graphics, (AbstractSelectionList) ((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getRowLeft()I"), method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
	private void onRenderListBackgroundPost(GuiGraphics graphics, int p_283242_, int p_282891_, float p_283683_, CallbackInfo info) {
		
		RenderGuiListBackgroundEvent.Post e = new RenderGuiListBackgroundEvent.Post(graphics, (AbstractSelectionList) ((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		
	}
	
}
