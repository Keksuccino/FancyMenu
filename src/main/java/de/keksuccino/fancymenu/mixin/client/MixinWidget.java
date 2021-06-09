package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetEvent;
import de.keksuccino.fancymenu.events.RenderWidgetLabelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = Widget.class)
public abstract class MixinWidget extends AbstractGui {

	@Shadow int height;
	@Shadow int width;
	@Shadow float alpha;
	
	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;defaultBlendFunc()V", ordinal = 0, shift = Shift.AFTER), method = "renderButton", cancellable = true)
	private void onRenderButton(int mouseX, int mouseY, float partial, CallbackInfo info) {
		info.cancel();
		
		Minecraft mc = Minecraft.getInstance();
		FontRenderer font = mc.fontRenderer;
		Widget b = (Widget)((Object)this);
		
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
		
		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
		this.alpha = e.getAlpha();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
		if (!e.isCanceled()) {
			int i = this.getYImage(b.isHovered());
			this.blit(b.x, b.y, 0, 46 + i * 20, this.width / 2, this.height);
			this.blit(b.x + this.width / 2, b.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
		}
		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e2);
		
		this.renderBg(mc, mouseX, mouseY);
		
		RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e3);
		this.alpha = e3.getAlpha();
		if (!e3.isCanceled()) {
			drawCenteredString(font, ((Widget)((Object)this)).getMessage(), b.x + this.width / 2, b.y + (this.height - 8) / 2, b.getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
		}
		RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e4);
	}
	
	@Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
	private void onRenderPre(int mouseX, int mouseY, float partial, CallbackInfo info) {
		RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre((Widget)((Object)this), this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
		this.alpha = e.getAlpha();
		if (e.isCanceled()) {
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "render", cancellable = true)
	private void onRenderPost(int mouseX, int mouseY, float partial, CallbackInfo info) {
		RenderWidgetEvent.Post e = new RenderWidgetEvent.Post((Widget)((Object)this), this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
	}
	
	@Inject(at = @At(value = "HEAD"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPre(SoundHandler handler, CallbackInfo info) {
		PlayWidgetClickSoundEvent.Pre e = new PlayWidgetClickSoundEvent.Pre((Widget)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		if (e.isCanceled()) {
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPost(SoundHandler handler, CallbackInfo info) {
		PlayWidgetClickSoundEvent.Post e = new PlayWidgetClickSoundEvent.Post((Widget)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
	}
	
	@Shadow protected abstract int getYImage(boolean isHovered);
	
	@Shadow protected abstract void renderBg(Minecraft minecraft, int mouseX, int mouseY);
	
}
