package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@WidgetifiedScreen
@Mixin(DeathScreen.class)
public class MixinDeathScreen extends Screen {

    // dummy constructor
    private MixinDeathScreen() {
        super(Component.empty());
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void after_init_FancyMenu(CallbackInfo info) {

        this.addRenderableWidget(); // add custom widgets here like this

    }

}
