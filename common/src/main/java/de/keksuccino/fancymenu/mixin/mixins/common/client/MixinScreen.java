package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
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
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
public abstract class MixinScreen implements CustomizableScreen, ContainerEventHandler {

    @Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

    @Unique private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();
    @Unique private boolean initialized_FancyMenu = false;
    @Unique private boolean nextFocusPath_called_FancyMenu = false;

    @Shadow @Final private List<GuiEventListener> children;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
    private void return_init_FancyMenu(Minecraft minecraft, int width, int height, CallbackInfo info) {
        this.initialized_FancyMenu = true;
    }

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void head_renderTooltipInternal_FancyMenu(PoseStack pose, List<ClientTooltipComponent> clientTooltipComponents, int mouseX, int mouseY, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    @WrapMethod(method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V")
    private void wrap_renderBackground_FancyMenu(PoseStack poseStack, int vOffset, Operation<Void> original) {
        Screen instance = ((Screen)(Object)this);
        GuiGraphics graphics = GuiGraphics.currentGraphics();
        int mouseX = MixinCacheCommon.cached_screen_render_mouseX;
        int mouseY = MixinCacheCommon.cached_screen_render_mouseY;
        float partial = MixinCacheCommon.cached_screen_render_partial;
        //Don't fire the event in the TitleScreen, because it gets handled differently there
        if (instance instanceof TitleScreen) {
            original.call(poseStack, vOffset);
            return;
        }
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(instance);
        if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(instance)) {
            if (!l.layoutBase.menuBackgrounds.isEmpty()) {
                RenderSystem.enableBlend();
                //Render a black background before the custom background gets rendered
                graphics.fill(0, 0, instance.width, instance.height, 0);
                RenderingUtils.resetShaderColor(graphics);
            } else {
                original.call(poseStack, vOffset);
            }
        } else {
            original.call(poseStack, vOffset);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(instance, graphics, mouseX, mouseY, partial));
    }

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;changeFocus(Z)Z"))
    private boolean wrap_changeFocus_in_keyPressed_FancyMenu(Screen instance, boolean focus, Operation<Boolean> original) {
        this.nextFocusPath_called_FancyMenu = true;
        boolean b = original.call(instance, focus);
        this.nextFocusPath_called_FancyMenu = false;
        return b;
    }

    @Override
    public void setInitialFocus(@Nullable GuiEventListener eventListener) {
        this.nextFocusPath_called_FancyMenu = true;
        ContainerEventHandler.super.setInitialFocus(eventListener);
        this.nextFocusPath_called_FancyMenu = false;
    }

    @Override
    public boolean changeFocus(boolean focus) {
        this.nextFocusPath_called_FancyMenu = true;
        boolean b = ContainerEventHandler.super.changeFocus(focus);
        this.nextFocusPath_called_FancyMenu = false;
        return b;
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

    @Unique
    @Override
    public boolean isScreenInitialized_FancyMenu() {
        return this.initialized_FancyMenu;
    }

}