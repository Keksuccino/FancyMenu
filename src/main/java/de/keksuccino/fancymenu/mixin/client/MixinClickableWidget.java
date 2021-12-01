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
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

@Mixin(value = ClickableWidget.class)
public abstract class MixinClickableWidget extends DrawableHelper {

	@Shadow int height;
	@Shadow int width;
	@Shadow float alpha;
	
	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", ordinal = 0, shift = Shift.AFTER), method = "renderButton", cancellable = true)
	private void onRenderButton(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		ClickableWidget bt = (ClickableWidget)((Object)this);
		if (bt instanceof PressableWidget) {
			info.cancel();
			
			PressableWidget b = (PressableWidget) bt;
			MinecraftClient mc = MinecraftClient.getInstance();
			TextRenderer font = mc.textRenderer;
			
			RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, b, this.alpha);
			Konkrete.getEventHandler().callEventsFor(e);
			this.alpha = e.getAlpha();
			if (!e.isCanceled()) {
				int i = this.getYImage(b.isHovered());
				this.drawTexture(matrix, b.x, b.y, 0, 46 + i * 20, this.width / 2, this.height);
				this.drawTexture(matrix, b.x + this.width / 2, b.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
			}
			RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, b, this.alpha);
			Konkrete.getEventHandler().callEventsFor(e2);
			
			this.renderBackground(matrix, mc, mouseX, mouseY);
			
			RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(matrix, b, this.alpha);
			Konkrete.getEventHandler().callEventsFor(e3);
			this.alpha = e3.getAlpha();
			if (!e3.isCanceled()) {
				drawCenteredText(matrix, font, b.getMessage(), b.x + this.width / 2, b.y + (this.height - 8) / 2, this.getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
			}
			RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(matrix, b, this.alpha);
			Konkrete.getEventHandler().callEventsFor(e4);
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
	private void onRenderPre(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		ClickableWidget bt = (ClickableWidget)((Object)this);
		if (bt instanceof PressableWidget) {
			RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(matrix, (PressableWidget)((Object)this), this.alpha);
			Konkrete.getEventHandler().callEventsFor(e);
			this.alpha = e.getAlpha();
			if (e.isCanceled()) {
				info.cancel();
			}
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "render", cancellable = true)
	private void onRenderPost(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		ClickableWidget bt = (ClickableWidget)((Object)this);
		if (bt instanceof PressableWidget) {
			RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(matrix, (PressableWidget)((Object)this), this.alpha);
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPre(SoundManager handler, CallbackInfo info) {
		ClickableWidget bt = (ClickableWidget)((Object)this);
		if (bt instanceof PressableWidget) {
			PlayWidgetClickSoundEvent.Pre e = new PlayWidgetClickSoundEvent.Pre((PressableWidget)((Object)this));
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPost(SoundManager handler, CallbackInfo info) {
		ClickableWidget bt = (ClickableWidget)((Object)this);
		if (bt instanceof PressableWidget) {
			PlayWidgetClickSoundEvent.Post e = new PlayWidgetClickSoundEvent.Post((PressableWidget)((Object)this));
			Konkrete.getEventHandler().callEventsFor(e);
		}
	}
	
	@Shadow protected abstract int getYImage(boolean isHovered);
	
	@Shadow protected abstract void renderBackground(MatrixStack matrixStack, MinecraftClient minecraft, int mouseX, int mouseY);
	
	private int getFGColor() {
		return ((ClickableWidget)((Object)this)).active ? 16777215 : 10526880;
	}
	
}
