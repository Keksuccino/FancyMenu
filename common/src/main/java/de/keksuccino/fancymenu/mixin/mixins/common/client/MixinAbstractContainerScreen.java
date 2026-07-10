package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
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
import java.util.HashMap;
import java.util.Map;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen extends Screen {

    @Shadow @Nullable protected Slot hoveredSlot;

    @Unique private final Map<Integer, GuiEventListener> clickedWidgetsByButton_FancyMenu = new HashMap<>();
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

        this.clickedWidgetsByButton_FancyMenu.remove(event.button());
        for (GuiEventListener listener : this.children()) {
            if ((listener instanceof FancyMenuWidget) && this.canClickWidget_FancyMenu(listener, event)) {
                if (listener.mouseClicked(event, isDoubleClick)) {
                    // The manual routing must mirror ContainerEventHandler's focus contract. Edit boxes rely on their parent to focus them.
                    this.clickedWidgetsByButton_FancyMenu.put(event.button(), listener);
                    if (listener.shouldTakeFocusAfterInteraction()) {
                        this.setFocused(listener);
                        if (event.button() == 0) {
                            this.setDragging(true);
                        }
                    }
                    info.setReturnValue(true);
                    return;
                }
            }
        }

    }

    /**
     * @reason Container screens must not process slot releases belonging to a FancyMenu widget interaction.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void before_mouseReleased_FancyMenu(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {

        GuiEventListener clickedWidget = this.clickedWidgetsByButton_FancyMenu.remove(event.button());
        if (clickedWidget == null) {
            return;
        }
        if ((event.button() == 0) && this.isDragging()) {
            this.setDragging(false);
        }
        clickedWidget.mouseReleased(event);
        info.setReturnValue(true);

    }

    /**
     * @reason Container screens must not start slot dragging while a FancyMenu widget owns the active pointer interaction.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void before_mouseDragged_FancyMenu(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> info) {

        GuiEventListener clickedWidget = this.clickedWidgetsByButton_FancyMenu.get(event.button());
        if (clickedWidget == null) {
            return;
        }
        clickedWidget.mouseDragged(event, dragX, dragY);
        info.setReturnValue(true);

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

    @Unique
    private boolean canClickWidget_FancyMenu(@NotNull GuiEventListener listener, @NotNull MouseButtonEvent event) {
        if (listener instanceof AbstractWidget w) {
            return w.isMouseOver(event.x(), event.y());
        }
        return false;
    }

}
