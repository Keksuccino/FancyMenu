package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeHooksClient.class)
public class MixinForgeHooksClient {

    @Inject(method = "drawScreen", at = @At(value = "HEAD"), remap = false)
    private static void before_drawScreen_FancyMenu(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        ScreenRenderUtils.executeAllPreRenderTasks(graphics, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(screen, graphics, mouseX, mouseY, partial));
    }

    @Inject(method = "drawScreen", at = @At(value = "RETURN"), remap = false)
    private static void after_drawScreen_FancyMenu(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(screen, graphics, mouseX, mouseY, partial));
        ScreenRenderUtils.executeAllPostRenderTasks(graphics, mouseX, mouseY, partial);
    }

}
