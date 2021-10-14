package de.keksuccino.fancymenu.mixin.client;

import java.lang.reflect.Field;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import net.minecraft.client.gui.GuiComponent;

@Mixin(value = ImageButton.class)
public abstract class MixinImageButton extends GuiComponent {

	@Shadow @Final private int xTexStart;
	@Shadow @Final private int yTexStart;
	@Shadow @Final private int yDiffTex;
	@Shadow @Final private int textureWidth;
	@Shadow @Final private int textureHeight;
	@Shadow @Final private ResourceLocation resourceLocation;

	@Inject(at = @At(value = "HEAD"), method = "renderButton", cancellable = true)
	private void onRenderButton(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		info.cancel();

		ImageButton b = ((ImageButton)((Object)this));

		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, (AbstractWidget) ((Object)this), this.getAlpha());
		MinecraftForge.EVENT_BUS.post(e);

		if (!e.isCanceled()) {
			RenderUtils.bindTexture(this.resourceLocation);
			int i = this.yTexStart;
			if (b.isHovered()) {
				i += this.yDiffTex;
			}
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, e.getAlpha());
			blit(matrix, b.x, b.y, (float)this.xTexStart, (float)i, b.getWidth(), b.getHeight(), this.textureWidth, this.textureHeight);
		}

		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, (AbstractWidget) ((Object)this), e.getAlpha());
		MinecraftForge.EVENT_BUS.post(e2);

		if (b.isHovered()) {
			b.renderToolTip(matrix, mouseX, mouseY);
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
