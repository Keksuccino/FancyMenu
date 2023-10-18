package de.keksuccino.fancymenu.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.events.RenderScreenEvent;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO übernehmen
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    private GuiGraphics cachedGuiGraphics = null;
    private int cachedMouseX = 0;
    private int cachedMouseY = 0;
    private float cachedPartial = 0.0F;

    //I'm wrapping this to easily cache the render() params
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private boolean wrapRenderScreenFancyMenu(Screen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.cachedGuiGraphics = graphics;
        this.cachedMouseX = mouseX;
        this.cachedMouseY = mouseY;
        this.cachedPartial = partial;
        Konkrete.getEventHandler().callEventsFor(new RenderScreenEvent.Pre(Minecraft.getInstance().screen, graphics, mouseX, mouseY, partial));
        return true;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void afterRenderScreenFancyMenu(float $$0, long $$1, boolean $$2, CallbackInfo info) {
        Konkrete.getEventHandler().callEventsFor(new RenderScreenEvent.Post(Minecraft.getInstance().screen, this.cachedGuiGraphics, this.cachedMouseX, this.cachedMouseY, this.cachedPartial));
    }

}
