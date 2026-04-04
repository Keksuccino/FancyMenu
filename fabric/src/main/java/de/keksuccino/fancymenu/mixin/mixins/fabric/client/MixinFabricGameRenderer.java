package de.keksuccino.fancymenu.mixin.mixins.fabric.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.events.screen.AfterScreenRenderingEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinFabricGameRenderer {

    @WrapOperation(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void wrapRenderScreenFancyMenu(Screen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
        ScreenRenderUtils.executeAllPreRenderTasks(graphics, mouseX, mouseY, partial);
        original.call(instance, graphics, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(new AfterScreenRenderingEvent(instance, graphics, mouseX, mouseY, partial));
        ScreenRenderUtils.executeAllPostRenderTasks(graphics, mouseX, mouseY, partial);
    }

}
