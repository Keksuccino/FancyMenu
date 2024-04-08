package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.MixinCacheCommon;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public class MixinScreen implements CustomizableScreen {

	@Unique private static final Logger LOGGER = LogManager.getLogger();

	@Unique
	private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();

	@Inject(method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;fillGradient(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V", shift = At.Shift.AFTER))
	private void afterFillGradientInRenderScreenBackgroundFancyMenu(PoseStack pose, int i, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), pose));
	}

	@Inject(method = "renderDirtBackground", at = @At(value = "RETURN"))
	private void afterRenderDirtBackgroundFancyMenu(int i, CallbackInfo info) {
		if (MixinCacheCommon.current_screen_render_pose_stack != null) {
			EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen) ((Object) this), MixinCacheCommon.current_screen_render_pose_stack));
		}
	}

	@Unique
	@Override
	public @NotNull List<GuiEventListener> removeOnInitChildrenFancyMenu() {
		return this.removeOnInitChildrenFancyMenu;
	}

}
