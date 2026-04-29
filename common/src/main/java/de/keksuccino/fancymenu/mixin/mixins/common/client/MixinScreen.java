package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.mixin.MixinCacheCommon;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
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
import java.util.ListIterator;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(Screen.class)
public abstract class MixinScreen implements CustomizableScreen, ContainerEventHandler {

    @Unique private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();
    @Unique private boolean initialized_FancyMenu = false;
    @Unique private boolean nextFocusPath_called_FancyMenu = false;

    @Shadow @Final private List<GuiEventListener> children;

    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
    private void return_init_FancyMenu(Minecraft minecraft, int width, int height, CallbackInfo info) {
        this.initialized_FancyMenu = true;
    }

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void head_renderTooltipInternal_FancyMenu(PoseStack pose, List<ClientTooltipComponent> clientTooltipComponents, int mouseX, int mouseY, CallbackInfo info) {
        if (RenderingUtils.isTooltipRenderingBlocked()) info.cancel();
    }

    @WrapOperation(method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;fillGradient(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
    private void wrap_fillGradient_in_renderBackground_FancyMenu(Screen instance, PoseStack pose, int i1, int i2, int i3, int i4, int i5, int i6, Operation<Void> original) {
        this.renderBackgroundWrappedInEvent_FancyMenu(() -> original.call(instance, pose, i1, i2, i3, i4, i5, i6));
    }

    @WrapMethod(method = "renderDirtBackground")
    private void wrap_renderDirtBackground_FancyMenu(int vOffset, Operation<Void> original) {
        this.renderBackgroundWrappedInEvent_FancyMenu(() -> this.renderCustomOrVanillaDirtBackground_FancyMenu(vOffset, original));
    }

    @Unique
    private void renderCustomOrVanillaDirtBackground_FancyMenu(int vOffset, @NotNull Operation<Void> original) {
        GuiGraphics graphics = GuiGraphics.currentGraphics();
        Screen currentScreen = Minecraft.getInstance().screen;
        if (SeamlessWorldLoadingHandler.renderLoadingBackgroundIfActive(graphics, 0, 0, this.width, this.height, currentScreen)) {
            return;
        }
        RenderableResource customBackground = GlobalCustomizationHandler.getCustomMenuBackgroundTexture();
        if (customBackground == null) {
            original.call(vOffset);
            return;
        }
        ResourceLocation customLocation = customBackground.getResourceLocation();
        int textureWidth = customBackground.getWidth();
        int textureHeight = customBackground.getHeight();
        if (customLocation == null || textureWidth <= 0 || textureHeight <= 0) {
            original.call(vOffset);
            return;
        }
        RenderSystem.enableBlend();
        RenderingUtils.blitRepeat(graphics, customLocation, 0, 0, this.width, this.height, textureWidth, textureHeight);
        RenderingUtils.resetShaderColor(graphics);
        RenderSystem.disableBlend();
    }

    @Unique
    private void renderBackgroundWrappedInEvent_FancyMenu(@NotNull Runnable original) {
        Screen instance = (Screen)(Object)this;
        GuiGraphics graphics = GuiGraphics.currentGraphics();
        int mouseX = MixinCacheCommon.cached_screen_render_mouseX;
        int mouseY = MixinCacheCommon.cached_screen_render_mouseY;
        float partial = MixinCacheCommon.cached_screen_render_partial;

        if ((instance instanceof TitleScreen) || (instance instanceof CustomGuiBaseScreen)) {
            original.run();
            return;
        }

        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(instance);
        if ((layer != null) && ScreenCustomization.isCustomizationEnabledForScreen(instance) && layer.shouldReplaceVanillaScreenBackground()) {
            RenderSystem.enableBlend();
            graphics.fill(0, 0, instance.width, instance.height, 0);
            RenderingUtils.resetShaderColor(graphics);
        } else {
            original.run();
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(instance, graphics, mouseX, mouseY, partial));
    }

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;changeFocus(Z)Z"))
    private boolean wrap_changeFocus_in_keyPressed_FancyMenu(Screen instance, boolean focus, Operation<Boolean> original) {
        this.nextFocusPath_called_FancyMenu = true;
        boolean changed = original.call(instance, focus);
        this.nextFocusPath_called_FancyMenu = false;
        return changed;
    }

    @Override
    public void setInitialFocus(@Nullable GuiEventListener eventListener) {
        this.nextFocusPath_called_FancyMenu = true;
        this.setFocused(eventListener);
        if (eventListener != null) {
            eventListener.changeFocus(true);
        }
        this.nextFocusPath_called_FancyMenu = false;
    }

    @Override
    public boolean changeFocus(boolean forward) {
        this.nextFocusPath_called_FancyMenu = true;
        GuiEventListener current = this.getFocused();
        boolean hadFocused = current != null;

        if (hadFocused && current.changeFocus(forward)) {
            this.nextFocusPath_called_FancyMenu = false;
            return true;
        }

        List<? extends GuiEventListener> list = this.children();
        int start = hadFocused ? list.indexOf(current) + (forward ? 1 : 0) : (forward ? 0 : list.size());
        ListIterator<? extends GuiEventListener> it = list.listIterator(start);
        BooleanSupplier hasNext = forward ? it::hasNext : it::hasPrevious;
        Supplier<? extends GuiEventListener> next = forward ? it::next : it::previous;

        while (hasNext.getAsBoolean()) {
            GuiEventListener candidate = next.get();
            if (candidate.changeFocus(forward)) {
                this.setFocused(candidate);
                this.nextFocusPath_called_FancyMenu = false;
                return true;
            }
        }

        this.setFocused(null);
        this.nextFocusPath_called_FancyMenu = false;
        return false;
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
    public boolean get_initialized_FancyMenu() {
        return this.initialized_FancyMenu;
    }

}
