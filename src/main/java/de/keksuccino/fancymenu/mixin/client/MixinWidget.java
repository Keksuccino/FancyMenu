package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.DrawWidgetBackgroundEvent;
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

	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", ordinal = 0, shift = Shift.AFTER), method = "renderButton", cancellable = true)
	private void onRenderButton(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {

		info.cancel();

		try {
			Minecraft mc = Minecraft.getInstance();
			FontRenderer font = mc.font;
			Widget b = (Widget)((Object)this);

			DrawWidgetBackgroundEvent.Pre e = new DrawWidgetBackgroundEvent.Pre(matrix, b, this.alpha);
			MinecraftForge.EVENT_BUS.post(e);
			this.alpha = e.getAlpha();
			if (!e.isCanceled()) {
				int i = this.getYImage(b.isHovered());
				this.blit(matrix, b.x, b.y, 0, 46 + i * 20, this.width / 2, this.height);
				this.blit(matrix, b.x + this.width / 2, b.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
			}
			DrawWidgetBackgroundEvent.Post e2 = new DrawWidgetBackgroundEvent.Post(matrix, b, this.alpha);
			MinecraftForge.EVENT_BUS.post(e2);

			this.renderBg(matrix, mc, mouseX, mouseY);

			RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(matrix, b, this.alpha);
			MinecraftForge.EVENT_BUS.post(e3);
			this.alpha = e3.getAlpha();
			if (!e3.isCanceled()) {
				drawCenteredString(matrix, font, ((Widget)((Object)this)).getMessage(), b.x + this.width / 2, b.y + (this.height - 8) / 2, b.getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24);
			}
			RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(matrix, b, this.alpha);
			MinecraftForge.EVENT_BUS.post(e4);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Inject(at = @At(value = "HEAD"), method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V", cancellable = true)
	private void onRenderPre(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {

		try {
			RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(matrix, (Widget)((Object)this), this.alpha);
			MinecraftForge.EVENT_BUS.post(e);
			this.alpha = e.getAlpha();
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V", cancellable = true)
	private void onRenderPost(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {

		try {
			RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(matrix, (Widget)((Object)this), this.alpha);
			MinecraftForge.EVENT_BUS.post(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(at = @At(value = "HEAD"), method = "playDownSound(Lnet/minecraft/client/audio/SoundHandler;)V", cancellable = true)
	private void onButtonClickSoundPre(SoundHandler handler, CallbackInfo info) {

		try {
			PlayWidgetClickSoundEvent.Pre e = new PlayWidgetClickSoundEvent.Pre((Widget)((Object)this));
			MinecraftForge.EVENT_BUS.post(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "playDownSound(Lnet/minecraft/client/audio/SoundHandler;)V", cancellable = true)
	private void onButtonClickSoundPost(SoundHandler handler, CallbackInfo info) {

		try {
			PlayWidgetClickSoundEvent.Post e = new PlayWidgetClickSoundEvent.Post((Widget)((Object)this));
			MinecraftForge.EVENT_BUS.post(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Shadow protected abstract int getYImage(boolean isHovered);
	
	@Shadow protected abstract void renderBg(MatrixStack matrixStack, Minecraft minecraft, int mouseX, int mouseY);
	
}
