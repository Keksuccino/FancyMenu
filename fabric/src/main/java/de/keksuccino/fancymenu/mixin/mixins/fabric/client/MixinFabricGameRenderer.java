package de.keksuccino.fancymenu.mixin.mixins.fabric.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinFabricGameRenderer {

    @Unique
    private GuiGraphics cachedGraphicsFancyMenu = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
    @Unique
    private int cachedMouseXFancyMenu = 0;
    @Unique
    private int cachedMouseYFancyMenu = 0;
    @Unique
    private float cachedPartialFancyMenu = 0.0F;

    //I'm wrapping this to easily cache the render() params
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private boolean wrapRenderScreenFancyMenu(Screen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (Minecraft.getInstance().screen == null) return true;
        this.cachedGraphicsFancyMenu = graphics;
        this.cachedMouseXFancyMenu = mouseX;
        this.cachedMouseYFancyMenu = mouseY;
        this.cachedPartialFancyMenu = partial;
        RenderScreenEvent.Pre e = new RenderScreenEvent.Pre(Minecraft.getInstance().screen, graphics, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(e);
        return true;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void afterRenderScreenFancyMenu(float $$0, long $$1, boolean $$2, CallbackInfo info) {
        if (Minecraft.getInstance().screen == null) return;
        RenderScreenEvent.Post e = new RenderScreenEvent.Post(Minecraft.getInstance().screen, this.cachedGraphicsFancyMenu, this.cachedMouseXFancyMenu, this.cachedMouseYFancyMenu, this.cachedPartialFancyMenu);
        EventHandler.INSTANCE.postEvent(e);
    }

}
