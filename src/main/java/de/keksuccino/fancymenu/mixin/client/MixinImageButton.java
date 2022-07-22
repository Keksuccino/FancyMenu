package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = ImageButton.class)
public abstract class MixinImageButton extends AbstractGui {

	@Shadow private int xTexStart;
	@Shadow private int yTexStart;
	@Shadow private int yDiffTex;
	@Shadow private int textureWidth;
	@Shadow private int textureHeight;
	
	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", ordinal = 0, shift = Shift.AFTER), method = "renderButton", cancellable = true)
	private void onRenderButton(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		info.cancel();
		
		ImageButton b = ((ImageButton)((Object)this));
		
		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, (Widget)((Object)this), this.getAlpha());
		MinecraftForge.EVENT_BUS.post(e);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, e.getAlpha());
		if (!e.isCanceled()) {
			int i = this.yTexStart;
			if (b.isHovered()) {
				i += this.yDiffTex;
			}
			blit(matrix, b.x, b.y, (float)this.xTexStart, (float)i, b.getWidth(), b.getHeight(), this.textureWidth, this.textureHeight);
		}
		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, (Widget)((Object)this), e.getAlpha());
		MinecraftForge.EVENT_BUS.post(e2);
		
		if (b.isHovered()) {
			b.renderToolTip(matrix, mouseX, mouseY);
		}
		
	}
	
	private float getAlpha() {
		return ((IMixinWidget)this).getAlphaFancyMenu();
	}
	
}
