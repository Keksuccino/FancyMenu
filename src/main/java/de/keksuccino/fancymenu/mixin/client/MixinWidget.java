package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.Mth;
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
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = AbstractWidget.class)
public abstract class MixinWidget extends GuiComponent {

	@Shadow int height;
	@Shadow int width;
	@Shadow float alpha;
	
	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", ordinal = 0, shift = Shift.AFTER), method = "renderButton", cancellable = true)
	private void onRenderButton(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		info.cancel();
		
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		AbstractWidget b = (AbstractWidget) ((Object)this);
		
		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
		this.alpha = e.getAlpha();
		if (!e.isCanceled()) {
			int i = this.getYImage(b.isHoveredOrFocused());
			this.blit(matrix, b.x, b.y, 0, 46 + i * 20, this.width / 2, this.height);
			this.blit(matrix, b.x + this.width / 2, b.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
		}
		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e2);
		
		this.renderBg(matrix, mc, mouseX, mouseY);
		
		RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(matrix, b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e3);
		this.alpha = e3.getAlpha();
		if (!e3.isCanceled()) {
			drawCenteredString(matrix, font, ((AbstractWidget)((Object)this)).getMessage(), b.x + this.width / 2, b.y + (this.height - 8) / 2, b.getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
		}
		RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(matrix, b, this.alpha);
		MinecraftForge.EVENT_BUS.post(e4);
	}
	
	@Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
	private void onRenderPre(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(matrix, (AbstractWidget)((Object)this), this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
		this.alpha = e.getAlpha();
		if (e.isCanceled()) {
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "render", cancellable = true)
	private void onRenderPost(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(matrix, (AbstractWidget)((Object)this), this.alpha);
		MinecraftForge.EVENT_BUS.post(e);
	}
	
	@Inject(at = @At(value = "HEAD"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPre(SoundManager manager, CallbackInfo info) {
		PlayWidgetClickSoundEvent.Pre e = new PlayWidgetClickSoundEvent.Pre((AbstractWidget)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
		if (e.isCanceled()) {
			info.cancel();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPost(SoundManager manager, CallbackInfo info) {
		PlayWidgetClickSoundEvent.Post e = new PlayWidgetClickSoundEvent.Post((AbstractWidget)((Object)this));
		MinecraftForge.EVENT_BUS.post(e);
	}
	
	@Shadow protected abstract int getYImage(boolean isHovered);
	
	@Shadow protected abstract void renderBg(PoseStack matrixStack, Minecraft minecraft, int mouseX, int mouseY);
	
}
