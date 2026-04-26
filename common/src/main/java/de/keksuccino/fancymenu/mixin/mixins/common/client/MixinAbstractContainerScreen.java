package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlStateManager;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
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
import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen extends Screen {

    @Unique private static final List<GuiEventListener> CLICKED_WIDGETS_FANCYMENU = new ArrayList<>();

    @Unique private int cached_mouseX_FancyMenu;
    @Unique private int cached_mouseY_FancyMenu;
    @Unique private float cached_partial_FancyMenu;

    @Shadow @Nullable protected Slot hoveredSlot;

    // Dummy constructor
    private MixinAbstractContainerScreen() {
        super(Component.empty());
    }

    /**
     * @reason This is to make widgets work correctly in Inventory Container screens.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void head_mouseClicked_FancyMenu(MouseButtonEvent event, boolean isDoubleClick, CallbackInfoReturnable<Boolean> info) {

        for (GuiEventListener l : this.children()) {
            if ((l instanceof FancyMenuWidget) && this.canClickWidget_FancyMenu(l)) {
                CLICKED_WIDGETS_FANCYMENU.add(l);
                if (l.mouseClicked(event, isDoubleClick)) {
                    info.setReturnValue(true);
                    break;
                }
            }
        }

    }

    /**
     * @reason This is to make widgets work correctly in Inventory Container screens.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void head_mouseReleased_FancyMenu(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {

        for (GuiEventListener l : this.children()) {
            if ((l instanceof FancyMenuWidget) && CLICKED_WIDGETS_FANCYMENU.contains(l)) {
                if (l.mouseReleased(event)) {
                    info.setReturnValue(true);
                    break;
                }
            }
        }

        CLICKED_WIDGETS_FANCYMENU.clear();

    }

    /**
     * @reason This is to make widgets work correctly in Inventory Container screens.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void head_mouseDragged_FancyMenu(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> info) {

        for (GuiEventListener l : this.children()) {
            if ((l instanceof FancyMenuWidget) && CLICKED_WIDGETS_FANCYMENU.contains(l)) {
                if (l.mouseDragged(event, dragX, dragY)) {
                    info.setReturnValue(true);
                    break;
                }
            }
        }

    }

    @Inject(method = "render", at = @At("TAIL"))
    private void after_render_FancyMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        Slot hoveredSlot = this.hoveredSlot;
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            Listeners.ON_ITEM_HOVERED_IN_INVENTORY.clearCurrentItem();
            return;
        }
        Listeners.ON_ITEM_HOVERED_IN_INVENTORY.onItemHovered(hoveredSlot, hoveredSlot.getItem());
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
    @WrapOperation(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void wrap_renderBackground_in_renderBackground_FancyMenu(AbstractContainerScreen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(instance);
        if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(instance)) {
            if (!l.layoutBase.menuBackgrounds.isEmpty()) {
                GlStateManager._enableBlend();
                //Render a black background before the custom background gets rendered
                graphics.fill(0, 0, instance.width, instance.height, 0);
                RenderingUtils.resetShaderColor(graphics);
            } else {
                original.call(instance, graphics, mouseX, mouseY, partial);
            }
        } else {
            original.call(instance, graphics, mouseX, mouseY, partial);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(instance, graphics, this.cached_mouseX_FancyMenu, this.cached_mouseY_FancyMenu, this.cached_partial_FancyMenu));
    }

    @Unique
    private boolean canClickWidget_FancyMenu(@NotNull GuiEventListener listener) {
        if (listener instanceof AbstractWidget w) {
            return (w.isHovered() && w.isActive() && w.visible);
        }
        return false;
    }

}
