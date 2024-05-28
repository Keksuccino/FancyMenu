package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Objects;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void onConstructFancyMenu(Minecraft mc, ReloadInstance reloadInstance, Consumer<?> consumer, boolean b, CallbackInfo info) {
		//TODO übernehmen
//		//Preload animation frames to avoid lagging when rendering them for the first time
//		if (FancyMenu.getOptions().preLoadAnimations.getValue() && !AnimationHandler.preloadingCompleted()) {
//			AnimationHandler.preloadAnimations(false);
//		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private void beforeRenderScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire RenderPre event for current screen in loading overlay
		if (ScreenUtils.getScreen() != null) {
			EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(ScreenUtils.getScreen(), graphics, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
	private void afterRenderScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire RenderPost event for current screen in loading overlay
		if (ScreenUtils.getScreen() != null) {
			EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(ScreenUtils.getScreen(), graphics, mouseX, mouseY, partial));
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V"))
	private void wrapInitScreenFancyMenu(Screen instance, Minecraft mc, int width, int height, Operation<Void> original) {

		if (!GameIntroHandler.introPlayed && GameIntroHandler.shouldPlayIntro()) {
			GameIntroHandler.introPlayed = true;
			PlayableResource intro = GameIntroHandler.getIntro();
			if (intro != null) {
				Minecraft.getInstance().setOverlay(new GameIntroOverlay(instance, intro));
				return;
			}
		}

		ScreenCustomization.setIsNewMenu(true);

		ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(instance);
		if (layer != null) layer.resetLayer();

		//Fire Pre Screen Init events, because they normally don't get fired in the loading overlay
		RenderingUtils.resetGuiScale();
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(Objects.requireNonNull(instance), InitOrResizeScreenEvent.InitializationPhase.INIT));
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(Objects.requireNonNull(instance), InitOrResizeScreenEvent.InitializationPhase.INIT));

		//Use window.getGuiScaledWidth/Height here to respect GUI scale modifications made in Init.Pre events
		original.call(instance, mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

		//Fire Post Screen Init events, because they normally don't get fired in the loading overlay
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(Objects.requireNonNull(instance), InitOrResizeScreenEvent.InitializationPhase.INIT));
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(Objects.requireNonNull(instance), InitOrResizeScreenEvent.InitializationPhase.INIT));

	}

}

