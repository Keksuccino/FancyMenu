package de.keksuccino.fancymenu.mixin.mixins.neoforge.client;

import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NeoForgeLoadingOverlay.class)
public class MixinNeoForgeLoadingOverlay {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void beforeRenderScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        //Fire RenderPre event for current screen in loading overlay
        if (ScreenUtils.getScreen() != null) {
            ScreenRenderUtils.executeAllPreRenderTasks(graphics, mouseX, mouseY, partial);
            EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(ScreenUtils.getScreen(), graphics, mouseX, mouseY, partial));
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void afterRenderScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        //Fire RenderPost event for current screen in loading overlay
        if (ScreenUtils.getScreen() != null) {
            EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(ScreenUtils.getScreen(), graphics, mouseX, mouseY, partial));
            ScreenRenderUtils.executeAllPostRenderTasks(graphics, mouseX, mouseY, partial);
        }
    }

}
