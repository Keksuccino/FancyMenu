package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import de.keksuccino.fancymenu.event.events.widget.RenderWidgetBackgroundEvent;
import net.minecraft.client.gui.GuiComponent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ImageButton.class)
public abstract class MixinImageButton extends GuiComponent {

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void beforeRenderWidgetBackgroundFancyMenu(PoseStack matrix, int p_267992_, int p_267950_, float p_268076_, CallbackInfo info) {
		try {
			RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, (AbstractButton)((Object)this), this.getAlpha());
			EventHandler.INSTANCE.postEvent(e);
			((AbstractWidget)((Object)this)).setAlpha(e.getAlpha());
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(method = "renderWidget", at = @At("TAIL"), cancellable = true)
	private void afterRenderWidgetBackgroundFancyMenu(PoseStack matrix, int p_267992_, int p_267950_, float p_268076_, CallbackInfo info) {
		try {
			RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, (AbstractButton)((Object)this), this.getAlpha());
			EventHandler.INSTANCE.postEvent(e2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private float getAlpha() {
		return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
	}
	
}
