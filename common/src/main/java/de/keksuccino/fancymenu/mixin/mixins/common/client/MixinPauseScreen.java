package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.mixin.support.client.PauseScreenWidgetIdentifierResolver;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class MixinPauseScreen extends Screen {

    private MixinPauseScreen(Component $$0) {
        super($$0);
    }

    /**
     * @reason Give unique identifiers to all Pause screen widgets
     */
    @Inject(method = "createPauseMenu", at = @At("RETURN"))
    private void after_createPauseMenu_FancyMenu(CallbackInfo info) {

        boolean hasSendFeedbackButton = false;
        boolean hasReportBugsButton = false;
        for (GuiEventListener guiEventListener : this.children()) {
            if (guiEventListener instanceof AbstractWidget widget && widget.getMessage().getContents() instanceof TranslatableContents contents) {
                if (contents.getKey().equals("menu.sendFeedback")) hasSendFeedbackButton = true;
                if (contents.getKey().equals("menu.reportBugs")) hasReportBugsButton = true;
            }
        }

        for (GuiEventListener guiEventListener : this.children()) {
            if (guiEventListener instanceof AbstractWidget w) {
                if (w.getMessage().getContents() instanceof TranslatableContents c) {
                    UniqueWidget u = ((UniqueWidget)w);
                    String identifier = PauseScreenWidgetIdentifierResolver.resolve(c.getKey(), hasSendFeedbackButton, hasReportBugsButton);
                    if (identifier != null) u.setWidgetIdentifierFancyMenu(identifier);
                }
            }
        }

    }

}
