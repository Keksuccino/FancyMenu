package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
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

    @Unique private final ContainerWidgetPointerRouter<GuiEventListener> widgetPointerRouter_FancyMenu = new ContainerWidgetPointerRouter<>();
    @Unique private boolean itemHoverTrackingWasDormant_FancyMenu;

    // Dummy constructor
    private MixinAbstractContainerScreen() {
        super(Component.empty());
    }

    /**
     * @reason Container screens need explicit routing for FancyMenu widgets so their interactions take precedence over slot handling.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void before_mouseClicked_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        boolean handled = this.widgetPointerRouter_FancyMenu.mouseClicked(button, this.children(), listener -> (listener instanceof FancyMenuWidget) && this.canClickWidget_FancyMenu(listener, mouseX, mouseY), listener -> listener.mouseClicked(mouseX, mouseY, button), this::setFocused, () -> this.setDragging(true), () -> this.setDragging(false));
        if (handled) info.setReturnValue(true);
    }

    /**
     * @reason Container screens must not process slot releases belonging to a FancyMenu widget interaction.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void before_mouseReleased_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        boolean handled = this.widgetPointerRouter_FancyMenu.mouseReleased(button, listener -> listener.mouseReleased(mouseX, mouseY, button), () -> this.setDragging(false));
        if (handled) info.setReturnValue(true);
    }

    /**
     * @reason Container screens must not start slot dragging while a FancyMenu widget owns the active pointer interaction.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void before_mouseDragged_FancyMenu(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> info) {
        boolean handled = this.widgetPointerRouter_FancyMenu.mouseDragged(button, listener -> listener.mouseDragged(mouseX, mouseY, button, dragX, dragY));
        if (handled) info.setReturnValue(true);
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

    @Unique
    private boolean canClickWidget_FancyMenu(@NotNull GuiEventListener listener, double mouseX, double mouseY) {
        if (listener instanceof AbstractWidget w) {
            return w.isActive() && w.visible && w.isMouseOver(mouseX, mouseY);
        }
        return false;
    }

}
