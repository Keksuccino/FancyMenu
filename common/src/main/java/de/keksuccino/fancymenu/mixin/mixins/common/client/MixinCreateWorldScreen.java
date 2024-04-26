package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CreateWorldScreen.class)
public class MixinCreateWorldScreen extends Screen {

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

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private boolean wrapFooterSeparatorRenderingInRender_FancyMenu(GuiGraphics instance, ResourceLocation p_283272_, int p_283605_, int p_281879_, float p_282809_, float p_282942_, int p_281922_, int p_282385_, int p_282596_, int p_281699_) {
        if (ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
            if (layer != null) {
                return layer.layoutBase.renderScrollListFooterShadow;
            }
        }
        return true;
    }

}
