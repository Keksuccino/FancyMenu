package de.keksuccino.fancymenu.mixin.mixins.fabric.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import de.keksuccino.fancymenu.events.screen.ScreenCharTypedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinFabricKeyboardHandler {

    @Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

    @Unique int cached_key_FancyMenu;
    @Unique int cached_scanCode_FancyMenu;
    @Unique int cached_action_FancyMenu;
    @Unique int cached_modifiers_FancyMenu;

    @Unique long cached_char_windowPointer_FancyMenu;
    @Unique int cached_char_codePoint_FancyMenu;
    @Unique int cached_char_modifiers_FancyMenu;

    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private void before_wrapScreenError_FancyMenu(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo info) {
        this.cached_key_FancyMenu = key;
        this.cached_scanCode_FancyMenu = scanCode;
        this.cached_action_FancyMenu = action;
        this.cached_modifiers_FancyMenu = modifiers;
    }

    @Inject(method = "charTyped", at = @At(value = "INVOKE", target = "Ljava/lang/Character;charCount(I)I"))
    private void before_charCount_FancyMenu(long windowPointer, int codePoint, int modifiers, CallbackInfo info) {
        this.cached_char_windowPointer_FancyMenu = windowPointer;
        this.cached_char_codePoint_FancyMenu = codePoint;
        this.cached_char_modifiers_FancyMenu = modifiers;
    }

    /**
     * @reason This handles the CharTypedEvent.
     */
    @WrapWithCondition(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private boolean wrap_screen_charTyped_in_charTyped_FancyMenu(Runnable runnable, String message, String className) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            // Fire CharTypedEvent
            EventHandler.INSTANCE.postEvent(new ScreenCharTypedEvent(screen, (char) this.cached_char_codePoint_FancyMenu));
        }
        return true;
    }

}
