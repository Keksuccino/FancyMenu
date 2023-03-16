package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import net.minecraftforge.common.MinecraftForge;

//TODO übernehmen 1.19.4
@SuppressWarnings("rawtypes")
@Mixin(value = AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;isMouseOver(DD)Z", shift = Shift.AFTER), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V")
	private void onRenderListBackgroundPre(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		
		RenderGuiListBackgroundEvent.Pre e = new RenderGuiListBackgroundEvent.Pre(matrix, (AbstractSelectionList) ((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getRowLeft()I"), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V")
	private void onRenderListBackgroundPost(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		
		RenderGuiListBackgroundEvent.Post e = new RenderGuiListBackgroundEvent.Post(matrix, (AbstractSelectionList) ((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		
	}
	
}
