package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetEvent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = GuiButtonImage.class)
public abstract class MixinGuiButtonImage extends Gui {

	@Shadow private int xTexStart;
	@Shadow private int yTexStart;
	@Shadow private int yDiffText;
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableDepth()V", ordinal = 0, shift = Shift.AFTER), method = "drawButton", cancellable = true)
	private void onRenderButton(CallbackInfo info) {
		if (!FancyMenu.isKonkreteLoaded()) {
			return;
		}

		info.cancel();
		
		RenderWidgetEvent.Pre ep = new RenderWidgetEvent.Pre((GuiButton)((Object)this), 1.0F);
		MinecraftForge.EVENT_BUS.post(ep);
		if (ep.isCanceled()) {
			return;
		}
		
		GuiButtonImage b = ((GuiButtonImage)((Object)this));
		
		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre((GuiButton)((Object)this), ep.getAlpha());
		MinecraftForge.EVENT_BUS.post(e);
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, e.getAlpha());
		if (!e.isCanceled()) {
			int i = this.xTexStart;
            int j = this.yTexStart;

            if (b.isMouseOver()) {
                j += this.yDiffText;
            }
            this.drawTexturedModalRect(b.x, b.y, i, j, b.width, b.height);
		}
		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post((GuiButton)((Object)this), e.getAlpha());
		MinecraftForge.EVENT_BUS.post(e2);
		
		GlStateManager.enableDepth();
		
	}
	
}
