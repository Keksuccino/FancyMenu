package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuInputRouter;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuPointerTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
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
    @Unique private boolean itemHoverTrackingWasDormant_FancyMenu;

    // Dummy constructor
    private MixinAbstractContainerScreen() {
        super(Component.empty());
    }

    /**
     * @reason Container screens need explicit routing for FancyMenu widgets so their interactions take precedence over slot handling.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void before_mouseClicked_FancyMenu(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> info) {

        GuiEventListener listener = this.pointerTracker_FancyMenu.routeMouseClicked(this.children(), event, isDoubleClick);
        if (listener != null) {
            // Track the actual consumer instead of inferring release ownership from focus; some FancyMenu controls intentionally never take focus.
            if (listener.shouldTakeFocusAfterInteraction()) {
                this.setFocused(listener);
                if (event.button() == 0) {
                    this.setDragging(true);
                }
            }
            info.setReturnValue(true);
        }

    }

    /**
     * @reason Container screens must not process slot releases belonging to a FancyMenu widget interaction.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void before_mouseReleased_FancyMenu(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {

        if (!this.pointerTracker_FancyMenu.dispatchMouseReleased(event)) {
            if (FancyMenuInputRouter.routeMouseReleased(this.children(), null, event, FancyMenuInputRouter.MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY)) {
                // Container release logic runs before its super call, so route orphaned pointer captures before slot handling.
                if ((event.button() == 0) && this.isDragging()) this.setDragging(false);
                info.setReturnValue(true);
            }
            return;
        }
        if ((event.button() == 0) && this.isDragging()) {
            this.setDragging(false);
        }
        info.setReturnValue(true);

    }

    /**
     * @reason Container screens must not start slot dragging while a FancyMenu widget owns the active pointer interaction.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void before_mouseDragged_FancyMenu(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> info) {

        if (this.pointerTracker_FancyMenu.dispatchMouseDragged(event, dragX, dragY)) info.setReturnValue(true);

    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void after_render_FancyMenu(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
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

}
