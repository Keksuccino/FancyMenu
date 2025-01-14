package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class MixinDisconnectedScreen extends Screen {

    private MixinDisconnectedScreen(Component $$0) {
        super($$0);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void at_return_screen_init_FancyMenu(CallbackInfo info) {

        boolean backToMenuButton = false;
        boolean title = false;
        boolean reason = false;
        for (GuiEventListener l : this.children()) {
            if ((l instanceof Button b) && !backToMenuButton) {
                ((UniqueWidget)b).setWidgetIdentifierFancyMenu("back_to_menu_button");
                backToMenuButton = true;
            }
            if ((l instanceof StringWidget w) && !title) {
                ((UniqueWidget)w).setWidgetIdentifierFancyMenu("screen_title");
                title = true;
            }
            if ((l instanceof MultiLineTextWidget w) && !reason) {
                ((UniqueWidget)w).setWidgetIdentifierFancyMenu("disconnect_reason");
                reason = true;
            }
        }

    }

}
