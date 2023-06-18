package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.events.widget.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.widget.RenderWidgetEvent;
import net.minecraft.client.gui.GuiComponent;

@Mixin(value = AbstractWidget.class)
public abstract class MixinAbstractWidget extends GuiComponent {

	@Shadow protected float alpha;
	
	@Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
	private void beforeRenderWidgetFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		try {
			RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(matrix, (AbstractWidget)((Object)this), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
			this.alpha = e.getAlpha();
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "render")
	private void afterRenderWidgetFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		try {
			RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(matrix, (AbstractWidget)((Object)this), this.alpha);
			EventHandler.INSTANCE.postEvent(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "playDownSound", cancellable = true)
	private void beforeWidgetClickSoundFancyMenu(SoundManager manager, CallbackInfo info) {
		try {
			PlayWidgetClickSoundEvent.Pre e = new PlayWidgetClickSoundEvent.Pre((AbstractWidget)((Object)this));
			EventHandler.INSTANCE.postEvent(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Inject(at = @At(value = "TAIL"), method = "playDownSound")
	private void afterWidgetClickSoundFancyMenu(SoundManager manager, CallbackInfo info) {
		try {
			PlayWidgetClickSoundEvent.Post e = new PlayWidgetClickSoundEvent.Post((AbstractWidget)((Object)this));
			EventHandler.INSTANCE.postEvent(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
