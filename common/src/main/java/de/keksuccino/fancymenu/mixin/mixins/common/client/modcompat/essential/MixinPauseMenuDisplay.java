package de.keksuccino.fancymenu.mixin.mixins.common.client.modcompat.essential;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import gg.essential.event.gui.GuiDrawScreenEvent;
import gg.essential.handlers.PauseMenuDisplay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseMenuDisplay.class)
public class MixinPauseMenuDisplay {

    @Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

    /**
     * @reason This makes Essential not show its overlay in the Title screen while keeping the full overlay in the Pause screen and everywhere else.
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void head_init_FancyMenu(Screen screen, CallbackInfo info) {
        LOGGER_FANCYMENU.info("########################### INIT 0");
        if (screen == null) return;
        LOGGER_FANCYMENU.info("########################### INIT 1");
        if ((screen instanceof TitleScreen) && ScreenCustomization.isCustomizationEnabledForScreen(screen)) {
            LOGGER_FANCYMENU.info("########################### INIT 2");
            info.cancel();
        }
    }

    /**
     * @reason This makes Essential not show its overlay in the Title screen while keeping the full overlay in the Pause screen and everywhere else.
     */
    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private void head_drawScreen_FancyMenu(GuiDrawScreenEvent event, CallbackInfo info) {
        LOGGER_FANCYMENU.info("########################### DRAW 0");
        if (event.getScreen() == null) return;
        LOGGER_FANCYMENU.info("########################### DRAW 1");
        if ((event.getScreen() instanceof TitleScreen) && ScreenCustomization.isCustomizationEnabledForScreen(event.getScreen())) {
            LOGGER_FANCYMENU.info("########################### DRAW 2");
            info.cancel();
        }
    }

}
