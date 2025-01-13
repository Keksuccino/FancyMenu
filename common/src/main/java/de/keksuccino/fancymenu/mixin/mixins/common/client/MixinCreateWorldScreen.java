package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Function;

@Mixin(CreateWorldScreen.class)
public class MixinCreateWorldScreen extends Screen {

    @Unique private boolean reInitialized_FancyMenu = false;

    protected MixinCreateWorldScreen(Component $$0) {
        super($$0);
    }

    //Make the footer buttons unique for better compatibility with the customization system
    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private <T extends LayoutElement> T wrapAddChildInInit_FancyMenu(LinearLayout instance, T layoutElement, Operation<T> original) {

        if (layoutElement instanceof Button b) {
            if (b.getMessage() instanceof MutableComponent c) {
                if (c.getContents() instanceof TranslatableContents t) {

                    //Create World button
                    if ("selectWorld.create".equals(t.getKey())) {
                        ((UniqueWidget)b).setWidgetIdentifierFancyMenu("create_world_button");
                    }

                    //Cancel button
                    if ("gui.cancel".equals(t.getKey())) {
                        ((UniqueWidget)b).setWidgetIdentifierFancyMenu("cancel_button");
                    }

                }
            }
        }

        //Plugin shows param error, but should work (generic T makes plugin go brr)
        return original.call(instance, layoutElement);

    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private boolean wrapFooterSeparatorRenderingInRender_FancyMenu(GuiGraphics instance, Function<ResourceLocation, RenderType> $$0, ResourceLocation $$1, int $$2, int $$3, float $$4, float $$5, int $$6, int $$7, int $$8, int $$9) {
        if (ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
            if (layer != null) {
                return layer.layoutBase.renderScrollListFooterShadow;
            }
        }
        return true;
    }

    /**
     * @reason This fixes FM's menu bar not being clickable until you resize the window in this screen. Yes, it's hacky af, but works.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void head_render_FancyMenu(CallbackInfo info) {
        if (!this.reInitialized_FancyMenu) {
            this.reInitialized_FancyMenu = true;
            Minecraft.getInstance().resizeDisplay();
            info.cancel();
        }
    }

}
