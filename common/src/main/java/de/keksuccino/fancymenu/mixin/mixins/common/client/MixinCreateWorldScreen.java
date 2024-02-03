package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO übernehmen
@Mixin(CreateWorldScreen.class)
public class MixinCreateWorldScreen extends Screen {

    protected MixinCreateWorldScreen(Component $$0) {
        super($$0);
    }

    //Make the footer buttons unique for better compatibility with the customization system
    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private <T extends LayoutElement> T wrapAddChildInInit_FancyMenu(GridLayout.RowHelper instance, T layoutElement, Operation<T> original) {

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

    //CreateWorldScreen overrides renderDirtBackground, so add back RenderedScreenBackgroundEvent
    @Inject(method = "renderDirtBackground", at = @At("RETURN"))
    private void afterRenderDirtBackgroundInCreateWorldFancyMenu(GuiGraphics graphics, CallbackInfo info) {
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, graphics));
    }

}
