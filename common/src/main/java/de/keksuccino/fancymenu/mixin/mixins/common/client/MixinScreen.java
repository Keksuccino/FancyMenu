package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.mixin.MixinCacheCommon;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import net.minecraft.client.Minecraft;
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

	@Unique private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();
	@Unique private boolean screenInitialized_FancyMenu = false;

	@WrapOperation(method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;fillGradient(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
	private void wrap_fillGradient_in_renderBackground_FancyMenu(Screen instance, PoseStack pose, int i1, int i2, int i3, int i4, int i5, int i6, Operation<Void> original) {
		ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen((Screen)((Object)this));
		if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this.getScreen_FancyMenu())) {
			if (!l.layoutBase.menuBackgrounds.isEmpty()) {
				RenderSystem.enableBlend();
				//Render a black background before the custom background gets rendered
				GuiGraphics.currentGraphics().fill(0, 0, this.getScreen_FancyMenu().width, this.getScreen_FancyMenu().height, 0);
				RenderingUtils.resetShaderColor(GuiGraphics.currentGraphics());
			} else {
				original.call(instance, pose, i1, i2, i3, i4, i5, i6);
			}
		} else {
			original.call(instance, pose, i1, i2, i3, i4, i5, i6);
		}
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this.getScreen_FancyMenu(), pose));
	}

	@WrapOperation(method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderDirtBackground(I)V"))
	private void wrap_renderDirtBackground_in_renderBackground_FancyMenu(Screen instance, int $$0, Operation<Void> original) {
		ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen((Screen)((Object)this));
		if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this.getScreen_FancyMenu()) && (MixinCacheCommon.current_screen_render_pose_stack != null)) {
			if (!l.layoutBase.menuBackgrounds.isEmpty()) {
				RenderSystem.enableBlend();
				//Render a black background before the custom background gets rendered
				GuiGraphics.currentGraphics().fill(0, 0, this.getScreen_FancyMenu().width, this.getScreen_FancyMenu().height, 0);
				RenderingUtils.resetShaderColor(GuiGraphics.currentGraphics());
			} else {
				original.call(instance, $$0);
			}
		} else {
			original.call(instance, $$0);
		}
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this.getScreen_FancyMenu(), MixinCacheCommon.current_screen_render_pose_stack));
	}

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
	private void head_init_FancyMenu(Minecraft $$0, int $$1, int $$2, CallbackInfo info) {
		this.screenInitialized_FancyMenu = true;
	}

	@Unique
	@Override
	public @NotNull List<GuiEventListener> removeOnInitChildrenFancyMenu() {
		return this.removeOnInitChildrenFancyMenu;
	}

	@Unique
	@Override
	public boolean isScreenInitialized_FancyMenu() {
		return this.screenInitialized_FancyMenu;
	}

	@Unique
	private Screen getScreen_FancyMenu() {
		return (Screen)((Object)this);
	}

}