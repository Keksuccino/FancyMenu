package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetEvent;
import de.keksuccino.konkrete.Konkrete;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;

@Mixin(AbstractWidget.class)
public abstract class MixinAbstractWidget {

	@Shadow float alpha;

	@Inject(at = @At(value = "HEAD"), method = "render", cancellable = true)
	private void onRenderPre(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		try {
			RenderWidgetEvent.Pre e = new RenderWidgetEvent.Pre(graphics, (AbstractWidget)((Object)this), this.alpha);
			Konkrete.getEventHandler().callEventsFor(e);
			this.alpha = e.getAlpha();
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "render")
	private void onRenderPost(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		try {
			RenderWidgetEvent.Post e = new RenderWidgetEvent.Post(graphics, (AbstractWidget)((Object)this), this.alpha);
			Konkrete.getEventHandler().callEventsFor(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(at = @At(value = "HEAD"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPre(SoundManager manager, CallbackInfo info) {
		try {
			PlayWidgetClickSoundEvent.Pre e = new PlayWidgetClickSoundEvent.Pre((AbstractWidget)((Object)this));
			Konkrete.getEventHandler().callEventsFor(e);
			if (e.isCanceled()) {
				info.cancel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "playDownSound", cancellable = true)
	private void onButtonClickSoundPost(SoundManager manager, CallbackInfo info) {
		try {
			PlayWidgetClickSoundEvent.Post e = new PlayWidgetClickSoundEvent.Post((AbstractWidget)((Object)this));
			Konkrete.getEventHandler().callEventsFor(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
