package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import de.keksuccino.fancymenu.events.widget.RenderWidgetBackgroundEvent;
import net.minecraft.client.gui.GuiComponent;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ImageButton.class)
public abstract class MixinImageButton extends GuiComponent {

	@WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ImageButton;renderTexture(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/ResourceLocation;IIIIIIIII)V"))
	private boolean wrapRenderTextureFancyMenu(ImageButton instance, PoseStack pose, ResourceLocation resourceLocation, int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
		ImageButton button = (ImageButton)((Object)this);
		CustomizableWidget customizable = ((CustomizableWidget)this);
		boolean renderVanilla = ((CustomizableWidget)this).renderCustomBackgroundFancyMenu(button, pose, button.getX(), button.getY(), button.getWidth(), button.getHeight());
		//Render custom labels if present
		if (!renderVanilla && (((customizable.getCustomLabelFancyMenu() != null) && !button.isHoveredOrFocused()) || ((customizable.getHoverLabelFancyMenu() != null) && button.isHoveredOrFocused()))) {
			int labelColor = button.active ? 16777215 : 10526880;
			button.renderString(pose, Minecraft.getInstance().font, labelColor | Mth.ceil(((IMixinAbstractWidget)button).getAlphaFancyMenu() * 255.0F) << 24);
		}
		return renderVanilla;
	}

//	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
//	private void beforeRenderWidgetBackgroundFancyMenu(PoseStack matrix, int p_267992_, int p_267950_, float p_268076_, CallbackInfo info) {
//		try {
//			RenderWidgetBackgroundEvent.Pre e = new RenderWidgetBackgroundEvent.Pre(matrix, (AbstractButton)((Object)this), this.getAlpha());
//			EventHandler.INSTANCE.postEvent(e);
//			((AbstractWidget)((Object)this)).setAlpha(e.getAlpha());
//			if (e.isCanceled()) {
//				info.cancel();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

//	@Inject(method = "renderWidget", at = @At("TAIL"))
//	private void afterRenderWidgetBackgroundFancyMenu(PoseStack matrix, int p_267992_, int p_267950_, float p_268076_, CallbackInfo info) {
//		try {
//			RenderWidgetBackgroundEvent.Post e2 = new RenderWidgetBackgroundEvent.Post(matrix, (AbstractButton)((Object)this), this.getAlpha());
//			EventHandler.INSTANCE.postEvent(e2);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

//	private float getAlpha() {
//		return ((IMixinAbstractWidget)this).getAlphaFancyMenu();
//	}
	
}
