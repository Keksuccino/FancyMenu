package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
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

    @Unique private final ContainerWidgetPointerTracker<GuiEventListener> widgetPointerOwnership_FancyMenu = new ContainerWidgetPointerTracker<>();

    // Dummy constructor
    private MixinAbstractContainerScreen() {
        super(Component.empty());
    }

    /**
     * @reason Container screens need explicit routing for FancyMenu widgets so their interactions take precedence over slot handling.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void before_mouseClicked_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {

        this.widgetPointerOwnership_FancyMenu.clear(button);
        for (GuiEventListener listener : this.children()) {
            if ((listener instanceof FancyMenuWidget) && this.canClickWidget_FancyMenu(listener, mouseX, mouseY)) {
                if (listener.mouseClicked(mouseX, mouseY, button)) {
                    // The manual routing must mirror 1.19.2's ContainerEventHandler focus contract. Edit boxes rely on their parent to focus them.
                    this.widgetPointerOwnership_FancyMenu.capture(button, listener);
                    this.setFocused(listener);
                    if (button == 0) {
                        this.setDragging(true);
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
    private void before_mouseReleased_FancyMenu(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {

        GuiEventListener clickedWidget = this.widgetPointerOwnership_FancyMenu.release(button);
        if (clickedWidget == null) {
            return;
        }
        if ((button == 0) && this.isDragging()) {
            this.setDragging(false);
        }
        clickedWidget.mouseReleased(mouseX, mouseY, button);
        info.setReturnValue(true);

    }

    /**
     * @reason Container screens must not start slot dragging while a FancyMenu widget owns the active pointer interaction.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void before_mouseDragged_FancyMenu(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> info) {

        GuiEventListener clickedWidget = this.widgetPointerOwnership_FancyMenu.get(button);
        if (clickedWidget == null) {
            return;
        }
        clickedWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        info.setReturnValue(true);

    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_FancyMenu(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        Slot hoveredSlot = this.hoveredSlot;
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            Listeners.ON_ITEM_HOVERED_IN_INVENTORY.clearCurrentItem();
            return;
        }
        Listeners.ON_ITEM_HOVERED_IN_INVENTORY.onItemHovered(hoveredSlot, hoveredSlot.getItem());
    }

    @Unique
    private boolean canClickWidget_FancyMenu(@NotNull GuiEventListener listener, double mouseX, double mouseY) {
        if (listener instanceof AbstractWidget w) {
            return w.isActive() && w.isMouseOver(mouseX, mouseY);
        }
        return false;
    }

}
