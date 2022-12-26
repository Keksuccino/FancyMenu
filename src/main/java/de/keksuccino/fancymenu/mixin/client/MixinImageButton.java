package de.keksuccino.fancymenu.mixin.client;

import java.lang.reflect.Field;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import net.minecraft.client.gui.GuiComponent;

@Mixin(value = ImageButton.class)
public abstract class MixinImageButton extends GuiComponent {

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ImageButton;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIFFIIII)V"),method = "renderButton")
	private void redirectBlitInRenderButton(PoseStack poseStack, int x, int y, float texX, float texY, int width, int height, int texWidth, int texHeight) {
		try {
			RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(poseStack, (AbstractButton)((Object)this), this.getAlpha());
			MinecraftForge.EVENT_BUS.post(e);
			if (!e.isCanceled()) {
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, e.getAlpha());
				blit(poseStack, x, y, texX, texY, width, height, texWidth, texHeight);
			}
			RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(poseStack, (AbstractButton)((Object)this), e.getAlpha());
			MinecraftForge.EVENT_BUS.post(e2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private float getAlpha() {
		try {
			Field f = ReflectionHelper.findField(AbstractWidget.class, "f_93625_"); //alpha
			return f.getFloat(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1.0F;
	}
	
}
