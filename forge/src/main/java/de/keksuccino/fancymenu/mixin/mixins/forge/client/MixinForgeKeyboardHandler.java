package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import de.keksuccino.fancymenu.events.screen.ScreenCharTypedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.input.Utf16CodeUnitDispatcher;
import de.keksuccino.fancymenu.util.rinku.WrappedRinkuBrowser;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinForgeKeyboardHandler {

    @Unique long cached_char_windowPointer_FancyMenu;
    @Unique int cached_char_codePoint_FancyMenu;
    @Unique int cached_char_modifiers_FancyMenu;

    /**
     * @reason A focused browser owns handled key input. Cancel the whole callback so loader-level raw-key hooks cannot observe and act on the same key afterward.
     */
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"), cancellable = true)
    private void before_wrapScreenError_FancyMenu(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo info) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof WrappedRinkuBrowser) {
                    boolean b = false;
                    if (action == 1 || action == 2) {
                        b = listener.keyPressed(key, scanCode, modifiers);
                    } else if (action == 0) {
                        b = listener.keyReleased(key, scanCode, modifiers);
                    }
                    if (b) {
                        info.cancel();
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "charTyped", at = @At(value = "INVOKE", target = "Ljava/lang/Character;charCount(I)I"))
    private void before_charCount_FancyMenu(long windowPointer, int codePoint, int modifiers, CallbackInfo info) {
        this.cached_char_windowPointer_FancyMenu = windowPointer;
        this.cached_char_codePoint_FancyMenu = codePoint;
        this.cached_char_modifiers_FancyMenu = modifiers;
    }

    /**
     * @reason This adds special char typed handling for FancyMenu's {@link WrappedRinkuBrowser}.
     *         It also handles the CharTypedEvent.
     */
    @WrapWithCondition(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
    private boolean wrap_screen_charTyped_in_charTyped_FancyMenu(Runnable runnable, String message, String className) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            // Fire CharTypedEvent
            EventHandler.INSTANCE.postEvent(new ScreenCharTypedEvent(screen, (char) this.cached_char_codePoint_FancyMenu));

            // Handle browser typing logic
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof WrappedRinkuBrowser) {
                    if (Utf16CodeUnitDispatcher.dispatch(this.cached_char_codePoint_FancyMenu, this.cached_char_modifiers_FancyMenu, listener::charTyped)) return false;
                }
            }
        }
        return true;
    }

}
