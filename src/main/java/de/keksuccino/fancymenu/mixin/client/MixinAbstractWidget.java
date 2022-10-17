package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetEvent;
import de.keksuccino.fancymenu.events.RenderWidgetLabelEvent;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.Mth;

@Mixin(AbstractWidget.class)
public abstract class MixinAbstractWidget extends GuiComponent {

	@Shadow int height;
	@Shadow int width;
	@Shadow float alpha;
	
	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", ordinal = 0, shift = Shift.AFTER), method = "renderButton", cancellable = true)
	private void onRenderButton(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		try {
			AbstractWidget bt = (AbstractWidget)((Object)this);
			if (bt instanceof AbstractButton) {
				info.cancel();

				AbstractButton b = (AbstractButton) bt;
				Minecraft mc = Minecraft.getInstance();
				Font font = mc.font;

				RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, b, this.alpha);
				Konkrete.getEventHandler().callEventsFor(e);
				this.alpha = e.getAlpha();
				if (!e.isCanceled()) {
					int i = this.getYImage(b.isHoveredOrFocused());
					this.blit(matrix, b.x, b.y, 0, 46 + i * 20, this.width / 2, this.height);
					this.blit(matrix, b.x + this.width / 2, b.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
				}
				RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, b, this.alpha);
				Konkrete.getEventHandler().callEventsFor(e2);

				this.renderBg(matrix, mc, mouseX, mouseY);

				RenderWidgetLabelEvent.Pre e3 = new RenderWidgetLabelEvent.Pre(matrix, b, this.alpha);
				Konkrete.getEventHandler().callEventsFor(e3);
				this.alpha = e3.getAlpha();
				if (!e3.isCanceled()) {
					drawCenteredString(matrix, font, b.getMessage(), b.x + this.width / 2, b.y + (this.height - 8) / 2, this.getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
				}
				RenderWidgetLabelEvent.Post e4 = new RenderWidgetLabelEvent.Post(matrix, b, this.alpha);
				Konkrete.getEventHandler().callEventsFor(e4);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
	private void onRenderPre(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		try {
			AbstractWidget bt = (AbstractWidget)((Object)this);
			if (bt instanceof AbstractButton) {
				RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(matrix, (AbstractButton)((Object)this), this.alpha);
				Konkrete.getEventHandler().callEventsFor(e);
				this.alpha = e.getAlpha();
				if (e.isCanceled()) {
					info.cancel();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "render", cancellable = true)
	private void onRenderPost(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		try {
			AbstractWidget bt = (AbstractWidget)((Object)this);
			if (bt instanceof AbstractButton) {
				RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(matrix, (AbstractButton)((Object)this), this.alpha);
				Konkrete.getEventHandler().callEventsFor(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPre(SoundManager handler, CallbackInfo info) {
		try {
			AbstractWidget bt = (AbstractWidget)((Object)this);
			if (bt instanceof AbstractButton) {
				PlayWidgetClickSoundEvent.Pre e = new PlayWidgetClickSoundEvent.Pre((AbstractButton)((Object)this));
				Konkrete.getEventHandler().callEventsFor(e);
				if (e.isCanceled()) {
					info.cancel();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPost(SoundManager handler, CallbackInfo info) {
		try {
			AbstractWidget bt = (AbstractWidget)((Object)this);
			if (bt instanceof AbstractButton) {
				PlayWidgetClickSoundEvent.Post e = new PlayWidgetClickSoundEvent.Post((AbstractButton)((Object)this));
				Konkrete.getEventHandler().callEventsFor(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Shadow protected abstract int getYImage(boolean isHovered);
	
	@Shadow protected abstract void renderBg(PoseStack matrixStack, Minecraft minecraft, int mouseX, int mouseY);
	
	private int getFGColor() {
		return ((AbstractWidget)((Object)this)).active ? 16777215 : 10526880;
	}
	
}
