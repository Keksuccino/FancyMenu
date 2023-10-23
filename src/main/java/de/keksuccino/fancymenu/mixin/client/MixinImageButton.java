package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ImageButton.class)
public abstract class MixinImageButton {

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void beforeRenderWidgetBackground(GuiGraphics graphics, int p_281473_, int p_283021_, float p_282518_, CallbackInfo info) {
		try {
			RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(graphics, (AbstractButton)((Object)this), this.getAlpha());
			MinecraftForge.EVENT_BUS.post(e);
			((AbstractWidget)((Object)this)).setAlpha(e.getAlpha());
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(method = "renderWidget", at = @At("TAIL"))
	private void afterRenderWidgetBackground(GuiGraphics graphics, int p_281473_, int p_283021_, float p_282518_, CallbackInfo info) {
		try {
			RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(graphics, (AbstractButton)((Object)this), this.getAlpha());
			MinecraftForge.EVENT_BUS.post(e2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private float getAlpha() {
		return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
	}
	
}
