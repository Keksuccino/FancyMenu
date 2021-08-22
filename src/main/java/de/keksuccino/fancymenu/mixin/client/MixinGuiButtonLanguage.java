package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetEvent;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = GuiButtonLanguage.class)
public class MixinGuiButtonLanguage {
	
	private static final ResourceLocation BUTTON_TEXTURES = new ResourceLocation("textures/gui/widgets.png");
	 
	@Inject(at = @At(value = "HEAD"), method = "drawButton", cancellable = true)
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
		
		GuiButtonLanguage b = ((GuiButtonLanguage)((Object)this));
		int mouseX = MouseInput.getMouseX();
		int mouseY = MouseInput.getMouseY();
		boolean hovered = mouseX >= b.x && mouseY >= b.y && mouseX < b.x + b.width && mouseY < b.y + b.height;

		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre((GuiButton)((Object)this), ep.getAlpha());
		MinecraftForge.EVENT_BUS.post(e);
		if (!e.isCanceled()) {
			if (b.visible) {
	            Minecraft.getMinecraft().getTextureManager().bindTexture(BUTTON_TEXTURES);
	            GlStateManager.enableBlend();
	            GlStateManager.color(1.0F, 1.0F, 1.0F, e.getAlpha());
	            int i = 106;

	            if (hovered) {
	                i += b.height;
	            }

	            b.drawTexturedModalRect(b.x, b.y, 0, i, b.width, b.height);
	        }
		}
		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post((GuiButton)((Object)this), e.getAlpha());
		MinecraftForge.EVENT_BUS.post(e2);
		
	}
	
}
