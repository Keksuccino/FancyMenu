package de.keksuccino.fancymenu.mixin.client;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Mixin(value = TexturedButtonWidget.class)
public abstract class MixinTexturedButtonWidget extends DrawableHelper {

	@Shadow private int u;
	@Shadow private int v;
	@Shadow private int hoveredVOffset;
	@Shadow private int textureWidth;
	@Shadow private int textureHeight;
	@Shadow private Identifier texture;

//	@Inject(at = @At(value = "HEAD"), method = "renderButton", cancellable = true)
	@Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V", ordinal = 0, shift = At.Shift.AFTER), method = "renderButton", cancellable = true)
	private void onRenderButton(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		info.cancel();
		
		TexturedButtonWidget b = ((TexturedButtonWidget)((Object)this));

		RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, (AbstractButtonWidget)((Object)this), this.getAlpha());
		Konkrete.getEventHandler().callEventsFor(e);
		
		if (!e.isCanceled()) {
			RenderUtils.bindTexture(this.texture);
			int i = this.v;
			if (b.isHovered()) {
				i += this.hoveredVOffset;
			}
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, e.getAlpha());
			drawTexture(matrix, b.x, b.y, (float)this.u, (float)i, b.getWidth(), b.getHeight(), this.textureWidth, this.textureHeight);
		}
		
		RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, (AbstractButtonWidget)((Object)this), e.getAlpha());
		Konkrete.getEventHandler().callEventsFor(e2);

		if (b.isHovered()) {
			b.renderToolTip(matrix, mouseX, mouseY);
		}

	}

	private float getAlpha() {
		try {
			Field f = ReflectionHelper.findField(AbstractButtonWidget.class, "alpha", "field_22765");
			return f.getFloat(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1.0F;
	}

}
