package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;

@Mixin(value = ImageButton.class)
public abstract class MixinTexturedButtonWidget extends GuiComponent {

	@Shadow private ResourceLocation resourceLocation;
	@Shadow private int xTexStart;
	@Shadow private int yTexStart;
	@Shadow private int yDiffTex;
	@Shadow private int textureWidth;
	@Shadow private int textureHeight;

	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", ordinal = 0, shift = At.Shift.AFTER), method = "renderButton", cancellable = true)
	private void onRenderButton(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		info.cancel();
		
		ImageButton b = ((ImageButton)((Object)this));

		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, (AbstractWidget)((Object)this), this.getAlpha());
		Konkrete.getEventHandler().callEventsFor(e);
		
		if (!e.isCanceled()) {
			RenderUtils.bindTexture(this.resourceLocation);
			int i = this.yTexStart;
			if (b.isHovered()) {
				i += this.yDiffTex;
			}
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, e.getAlpha());
			blit(matrix, b.x, b.y, (float)this.xTexStart, (float)i, b.getWidth(), b.getHeight(), this.textureWidth, this.textureHeight);
		}
		
		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, (AbstractWidget)((Object)this), e.getAlpha());
		Konkrete.getEventHandler().callEventsFor(e2);

		if (b.isHovered()) {
			b.renderToolTip(matrix, mouseX, mouseY);
		}

	}

	private float getAlpha() {
		return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
	}

}
