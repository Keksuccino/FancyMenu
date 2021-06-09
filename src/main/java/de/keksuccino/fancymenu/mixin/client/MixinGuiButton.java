package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetEvent;
import de.keksuccino.fancymenu.events.RenderWidgetLabelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = GuiButton.class)
public abstract class MixinGuiButton extends Gui {
	
	protected float alpha = 1.0F;
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableBlend()V", ordinal = 0, shift = Shift.AFTER), method = "drawButton", cancellable = true)
	private void onRenderButton(Minecraft mc, int mouseX, int mouseY, float partial, CallbackInfo info) {
		info.cancel();
		
		FontRenderer font = mc.fontRenderer;
		GuiButton b = (GuiButton)((Object)this);
		
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1.0F, 1.0F, 1.0F, this.alpha);
		
		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
		this.alpha = e.getAlpha();
		if (!e.isCanceled()) {
			GlStateManager.color(1.0F, 1.0F, 1.0F, this.alpha);
			int i = this.getHoverState(b.isMouseOver());
			this.drawTexturedModalRect(b.x, b.y, 0, 46 + i * 20, b.width / 2, b.height);
            this.drawTexturedModalRect(b.x + b.width / 2, b.y, 200 - b.width / 2, 46 + i * 20, b.width / 2, b.height);
		}
		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e2);
		
		this.mouseDragged(mc, mouseX, mouseY);
		
		RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e3);
		this.alpha = e3.getAlpha();
		if (!e3.isCanceled()) {
			int j = 14737632;
			if (b.packedFGColour != 0) {
                j = b.packedFGColour;
            } else if (!b.enabled) {
                j = 10526880;
            } else if (b.isMouseOver()) {
                j = 16777120;
            }

            this.drawCenteredString(font, b.displayString, b.x + b.width / 2, b.y + (b.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0F) << 24);
		}
		RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e4);
	}
	
	@Inject(at = @At(value = "HEAD"), method = "drawButton", cancellable = true)
	private void onRenderPre(Minecraft mc, int mouseX, int mouseY, float partial, CallbackInfo info) {
		RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre((GuiButton)((Object)this), this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
		this.alpha = e.getAlpha();
		if (e.isCanceled()) {
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "drawButton", cancellable = true)
	private void onRenderPost(Minecraft mc, int mouseX, int mouseY, float partial, CallbackInfo info) {
		RenderWidgetEvent.Post e = new RenderWidgetEvent.Post((GuiButton)((Object)this), this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
	}
	
	@Inject(at = @At(value = "HEAD"), method = "playPressSound", cancellable = true)
	private void onButtonClickSoundPre(SoundHandler handler, CallbackInfo info) {
		PlayWidgetClickSoundEvent.Pre e = new PlayWidgetClickSoundEvent.Pre((GuiButton)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		if (e.isCanceled()) {
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "playPressSound", cancellable = true)
	private void onButtonClickSoundPost(SoundHandler handler, CallbackInfo info) {
		PlayWidgetClickSoundEvent.Post e = new PlayWidgetClickSoundEvent.Post((GuiButton)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
	}
	
	@Shadow protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);
	
	@Shadow protected abstract int getHoverState(boolean mouseOver);
	
}
