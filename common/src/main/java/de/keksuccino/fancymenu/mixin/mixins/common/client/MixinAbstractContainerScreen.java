package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuInputRouter;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuPointerTracker;
import net.minecraft.client.gui.components.events.GuiEventListener;
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

        GuiEventListener listener = this.pointerTracker_FancyMenu.routeMouseClicked(this.children(), mouseX, mouseY, button);
        if (listener != null) {
            // The manual routing must mirror 1.19.2's ContainerEventHandler focus contract. Edit boxes rely on their parent to focus them.
            this.setFocused(listener);
            if (button == 0) this.setDragging(true);
            info.setReturnValue(true);
        }

    }

    /**
     * @reason Container screens must not process slot releases belonging to a FancyMenu component interaction.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void before_mouseReleased_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {

        if (this.pointerTracker_FancyMenu.dispatchMouseReleased(mouseX, mouseY, button)) {
            if ((button == 0) && this.isDragging()) this.setDragging(false);
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

        if (this.pointerTracker_FancyMenu.dispatchMouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            info.setReturnValue(true);
        }

    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_FancyMenu(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
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
