package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.events.screen.AfterScreenRenderingEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinForgeGameRenderer {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V", remap = false))
    private void wrapRenderScreenFancyMenu(Screen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
        RenderingUtils.executeAllPreRenderTasks(graphics, mouseX, mouseY, partial);
        original.call(instance, graphics, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(new AfterScreenRenderingEvent(instance, graphics, mouseX, mouseY, partial));
        ScreenOverlayHandler.INSTANCE.renderAll(graphics, mouseX, mouseY, partial);
        RenderingUtils.executeAllPostRenderTasks(graphics, mouseX, mouseY, partial);
    }

}
