package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuInputRouter;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuPointerTracker;
import de.keksuccino.fancymenu.util.rendering.ui.widget.interaction.ContainerWidgetInteractionRouter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import javax.annotation.Nullable;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen extends Screen {

    @Shadow @Nullable protected Slot hoveredSlot;

    @Unique private final FancyMenuPointerTracker pointerTracker_FancyMenu = new FancyMenuPointerTracker();
    @Unique private int cached_mouseX_FancyMenu;
    @Unique private int cached_mouseY_FancyMenu;
    @Unique private float cached_partial_FancyMenu;
    @Unique private boolean itemHoverTrackingWasDormant_FancyMenu;

    // Dummy constructor
    private MixinAbstractContainerScreen() {
        super(Component.empty());
    }

    /**
     * @reason Container screens need explicit routing for FancyMenu widgets and browsers so their interactions take precedence over slot handling.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void before_mouseClicked_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {

        if (ContainerWidgetInteractionRouter.mouseClicked(this, this.pointerTracker_FancyMenu, this.children(), mouseX, mouseY, button)) {
            info.setReturnValue(true);
        }

    }

    /**
     * @reason Container screens must not process slot releases belonging to a FancyMenu component interaction.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void before_mouseReleased_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {

        if (ContainerWidgetInteractionRouter.mouseReleased(this, this.pointerTracker_FancyMenu, mouseX, mouseY, button)) {
            info.setReturnValue(true);
            return;
        }
        if (FancyMenuInputRouter.routeMouseReleased(this.children(), null, mouseX, mouseY, button, FancyMenuInputRouter.MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY)) {
            // Container release logic runs before its super call, so route orphaned pointer captures before slot handling.
            if ((button == 0) && this.isDragging()) this.setDragging(false);
            info.setReturnValue(true);
        }

    }

    /**
     * @reason Container screens must not start slot dragging while a FancyMenu component owns the active pointer interaction.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void before_mouseDragged_FancyMenu(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> info) {

        if (ContainerWidgetInteractionRouter.mouseDragged(this.pointerTracker_FancyMenu, mouseX, mouseY, button, dragX, dragY)) {
            info.setReturnValue(true);
        }

    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_FancyMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        if (!Listeners.ON_ITEM_HOVERED_IN_INVENTORY.hasInstancesListening()) {
            this.itemHoverTrackingWasDormant_FancyMenu = true;
            return;
        }
        Slot hoveredSlot = this.hoveredSlot;
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            Listeners.ON_ITEM_HOVERED_IN_INVENTORY.clearCurrentItem();
            this.itemHoverTrackingWasDormant_FancyMenu = false;
            return;
        }
        Listeners.ON_ITEM_HOVERED_IN_INVENTORY.onItemHovered(hoveredSlot, hoveredSlot.getItem(), !this.itemHoverTrackingWasDormant_FancyMenu);
        this.itemHoverTrackingWasDormant_FancyMenu = false;
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void head_renderBackground_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        this.cached_mouseX_FancyMenu = mouseX;
        this.cached_mouseY_FancyMenu = mouseY;
        this.cached_partial_FancyMenu = partial;
    }

    /**
     * @reason Custom handling for FancyMenu's background render event in container screens.
     */
    @WrapOperation(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderTransparentBackground(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void wrap_renderTransparentBackground_in_renderBackground_FancyMenu(AbstractContainerScreen instance, GuiGraphics graphics, Operation<Void> original) {
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(instance);
        if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(instance)) {
            if (!l.layoutBase.menuBackgrounds.isEmpty()) {
                RenderSystem.enableBlend();
                //Render a black background before the custom background gets rendered
                graphics.fill(0, 0, instance.width, instance.height, 0);
                RenderingUtils.resetShaderColor(graphics);
            } else {
                original.call(instance, graphics);
            }
        } else {
            original.call(instance, graphics);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(instance, graphics, this.cached_mouseX_FancyMenu, this.cached_mouseY_FancyMenu, this.cached_partial_FancyMenu));
    }

}
