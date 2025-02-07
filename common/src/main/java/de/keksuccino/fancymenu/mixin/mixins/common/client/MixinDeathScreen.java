package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class MixinDeathScreen extends Screen {

    private MixinDeathScreen() {
        super(Component.empty());
    }

    /**
     * @reason Fires the background render event after this screen's custom background render logic
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/DeathScreen;fillGradient(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V", shift = At.Shift.AFTER))
    private void after_fillGradient_in_render_FancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, pose));
    }

}
