package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public abstract class MixinScreen implements CustomizableScreen {

	@Unique private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();
	@Unique private boolean nextFocusPath_called_FancyMenu = false;
    @Unique private int cachedMouseX_FancyMenu = -1;
    @Unique private int cachedMouseY_FancyMenu = -1;
    @Unique private float cachedPartial_FancyMenu = -1.0F;

	@Shadow @Final private List<GuiEventListener> children;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @WrapOperation(method = "renderWithTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void wrap_render_in_renderWithTooltip_FancyMenu(Screen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
        this.cachedMouseX_FancyMenu = mouseX;
        this.cachedMouseY_FancyMenu = mouseY;
        this.cachedPartial_FancyMenu = partial;
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(instance, graphics, mouseX, mouseY, partial));
        original.call(instance, graphics, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(instance, graphics, mouseX, mouseY, partial));
    }

    @WrapOperation(method = "renderWithTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;II)V"))
    private void wrap_renderTooltip_in_renderWithTooltip_FancyMenu(GuiGraphics instance, Font font, List<FormattedCharSequence> tooltipLines, ClientTooltipPositioner tooltipPositioner, int mouseX, int mouseY, Operation<Void> original) {
        if (PiPWindowHandler.INSTANCE.isAnyWindowOpen()) {
            // Makes tooltips render later when PipWindows are open
            RenderingUtils.postPostRenderTask((graphics, mouseX1, mouseY1, partial) -> {
                instance.renderTooltip(font, tooltipLines, tooltipPositioner, mouseX, mouseY);
            });
            return;
        }
        original.call(instance, font, tooltipLines, tooltipPositioner, mouseX, mouseY);
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void return_renderWithTooltip_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        RenderingUtils.executeAndClearDeferredScreenRenderingTasks(graphics, mouseX, mouseY, partial);
    }

    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void before_renderDirtBackground_FancyMenu(GuiGraphics graphics, CallbackInfo info) {
        int x = 0;
        int y = 0;
        int width = this.width;
        int height = this.height;
        Screen currentScreen = Minecraft.getInstance().screen;
        if (SeamlessWorldLoadingHandler.renderLoadingBackgroundIfActive(graphics, x, y, width, height, currentScreen)) {
            info.cancel();
            return;
        }
        RenderableResource customBackground = GlobalCustomizationHandler.getCustomMenuBackgroundTexture();
        if (customBackground == null) return;
        ResourceLocation customLocation = customBackground.getResourceLocation();
        if (customLocation == null) return;
        int textureWidth = customBackground.getWidth();
        int textureHeight = customBackground.getHeight();
        if (textureWidth <= 0 || textureHeight <= 0) return;
        RenderSystem.enableBlend();
        RenderingUtils.blitRepeat(graphics, customLocation, x, y, width, height, textureWidth, textureHeight);
        RenderingUtils.resetShaderColor(graphics);
        RenderSystem.disableBlend();
        info.cancel();
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void before_renderBackground_FancyMenu(GuiGraphics graphics, CallbackInfo info) {
        Screen instance = (Screen)(Object)this;
        // Don't fire the event in the TitleScreen, because it gets handled differently there.
        if (instance instanceof TitleScreen) {
            return;
        }
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(instance);
        if ((layer != null) && ScreenCustomization.isCustomizationEnabledForScreen(instance) && !layer.layoutBase.menuBackgrounds.isEmpty()) {
            RenderSystem.enableBlend();
            graphics.fill(0, 0, instance.width, instance.height, 0);
            RenderingUtils.resetShaderColor(graphics);
            EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(instance, graphics, this.cachedMouseX_FancyMenu, this.cachedMouseY_FancyMenu, this.cachedPartial_FancyMenu));
            info.cancel();
        }
    }

    @Inject(method = "renderBackground", at = @At("RETURN"))
    private void after_renderBackground_FancyMenu(GuiGraphics graphics, CallbackInfo info) {
        Screen instance = (Screen)(Object)this;
        if (instance instanceof TitleScreen) {
            return;
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(instance, graphics, this.cachedMouseX_FancyMenu, this.cachedMouseY_FancyMenu, this.cachedPartial_FancyMenu));
    }

	@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/AbstractContainerEventHandler;nextFocusPath(Lnet/minecraft/client/gui/navigation/FocusNavigationEvent;)Lnet/minecraft/client/gui/ComponentPath;"))
	private void beforeNextFocusPathInKeyPressedFancyMenu(int $$0, int $$1, int $$2, CallbackInfoReturnable<Boolean> info) {
		this.nextFocusPath_called_FancyMenu = true;
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/AbstractContainerEventHandler;nextFocusPath(Lnet/minecraft/client/gui/navigation/FocusNavigationEvent;)Lnet/minecraft/client/gui/ComponentPath;", shift = At.Shift.AFTER))
	private void afterNextFocusPathInKeyPressedFancyMenu(int $$0, int $$1, int $$2, CallbackInfoReturnable<Boolean> info) {
		this.nextFocusPath_called_FancyMenu = false;
	}

	@Inject(method = "setInitialFocus(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("HEAD"))
	private void beforeSetInitialFocusFancyMenu(GuiEventListener $$0, CallbackInfo info) {
		this.nextFocusPath_called_FancyMenu = true;
	}

	@Inject(method = "setInitialFocus(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("RETURN"))
	private void afterSetInitialFocusFancyMenu(GuiEventListener $$0, CallbackInfo info) {
		this.nextFocusPath_called_FancyMenu = false;
	}

	@Inject(method = "children", at = @At("RETURN"), cancellable = true)
	private void atReturnChildrenFancyMenu(CallbackInfoReturnable<List<? extends GuiEventListener>> info) {
		if (this.nextFocusPath_called_FancyMenu) {
			List<GuiEventListener> filtered = new ArrayList<>(this.children);
			filtered.removeIf(guiEventListener -> (guiEventListener instanceof NavigatableWidget n) && (!n.isFocusable() || !n.isNavigatable()));
			info.setReturnValue(filtered);
		}
	}

	@Unique
	@Override
	public @NotNull List<GuiEventListener> removeOnInitChildrenFancyMenu() {
		return this.removeOnInitChildrenFancyMenu;
	}

}
