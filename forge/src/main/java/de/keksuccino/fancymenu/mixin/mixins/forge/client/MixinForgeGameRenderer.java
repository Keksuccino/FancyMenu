package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.MixinCacheCommon;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinForgeGameRenderer {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", remap = false))
    private void wrapRenderScreenFancyMenu(Screen instance, PoseStack pose, int mouseX, int mouseY, float partial, Operation<Void> original) {
        MixinCacheCommon.current_screen_render_pose_stack = pose;
        GuiGraphics graphics = GuiGraphics.currentGraphics();
        ScreenRenderUtils.executeAllPreRenderTasks(graphics, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(instance, pose, mouseX, mouseY, partial));
        original.call(instance, pose, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(instance, pose, mouseX, mouseY, partial));
        ScreenRenderUtils.executeAllPostRenderTasks(graphics, mouseX, mouseY, partial);
    }

}
