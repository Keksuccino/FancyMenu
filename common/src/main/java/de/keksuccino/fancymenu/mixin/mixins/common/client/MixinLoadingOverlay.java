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
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Objects;

@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private void before_renderScreen_in_render_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire RenderPre event for current screen in loading overlay
		if (ScreenUtils.getScreen() != null) {
			ScreenRenderUtils.executeAllPreRenderTasks(graphics, mouseX, mouseY, partial);
			EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(ScreenUtils.getScreen(), graphics, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
	private void after_renderScreen_in_render_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire RenderPost event for current screen in loading overlay
		if (ScreenUtils.getScreen() != null) {
			EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(ScreenUtils.getScreen(), graphics, mouseX, mouseY, partial));
			ScreenRenderUtils.executeAllPostRenderTasks(graphics, mouseX, mouseY, partial);
		}
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V"))
	private void wrap_initScreen_in_tick_FancyMenu(Screen instance, int width, int height, Operation<Void> original) {

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

        Minecraft mc = Minecraft.getInstance();
		//Use window.getGuiScaledWidth/Height here to respect GUI scale modifications made in Init.Pre events
		original.call(instance, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

		ScrollScreenNormalizer.normalizeScrollableScreen(instance);

		//Fire Post Screen Init events, because they normally don't get fired in the loading overlay
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(Objects.requireNonNull(instance), InitOrResizeScreenEvent.InitializationPhase.INIT));
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(Objects.requireNonNull(instance), InitOrResizeScreenEvent.InitializationPhase.INIT));

	}

}

