package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ImageButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.konkrete.Konkrete;

@Mixin(ImageButton.class)
public abstract class MixinImageButton extends GuiComponent {

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ImageButton;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIFFIIII)V"),method = "renderButton")
	private void redirectBlitInRenderButton(PoseStack poseStack, int x, int y, float texX, float texY, int width, int height, int texWidth, int texHeight) {
		try {
			RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(poseStack, (AbstractButton)((Object)this), this.getAlpha());
			Konkrete.getEventHandler().callEventsFor(e);
			if (!e.isCanceled()) {
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, e.getAlpha());
				blit(poseStack, x, y, texX, texY, width, height, texWidth, texHeight);
			}
			RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(poseStack, (AbstractButton)((Object)this), e.getAlpha());
			Konkrete.getEventHandler().callEventsFor(e2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private float getAlpha() {
		return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
	}

}
