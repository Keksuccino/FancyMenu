package de.keksuccino.fancymenu.mixin.mixins.neoforge.client;

import de.keksuccino.fancymenu.events.screen.ScreenCharTypedEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
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
import java.util.Objects;

@Mixin(KeyboardHandler.class)
public class MixinNeoForgeKeyboardHandler {

    @Unique
    private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

    /**
     * @reason This adds special key press handling for FancyMenu's {@link WrappedMCEFBrowser}.
     */
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"), cancellable = true)
    private void before_keyPressed_in_keyPress_FancyMenu(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo info) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof WrappedMCEFBrowser) {
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

    /**
     * @reason This adds special key press handling for FancyMenu's {@link WrappedMCEFBrowser}.
     */
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(III)Z"), cancellable = true)
    private void before_keyReleased_in_keyPress_FancyMenu(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo info) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof WrappedMCEFBrowser) {
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

    /**
     * @reason This adds special char typed handling for FancyMenu's {@link WrappedMCEFBrowser}.
     */
    @Inject(method = "charTyped", at = @At(value = "HEAD"), cancellable = true)
    private void head_charTyped_FancyMenu(long windowPointer, int codePoint, int modifiers, CallbackInfo info) {

        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof WrappedMCEFBrowser) {
                    boolean b = false;
                    if (Character.charCount(codePoint) == 1) {
                        b = listener.charTyped((char) codePoint, modifiers);
                    } else {
                        for (char c : Character.toChars(codePoint)) {
                            b = !b ? listener.charTyped(c, modifiers) : true;
                        }
                    }
                    if (b) {
                        info.cancel();
                        return;
                    }
                }
            }
        }

    }

    @Inject(method = "charTyped", at = @At(value = "INVOKE", target = "Ljava/lang/Character;isBmpCodePoint(I)Z"))
    private void before_Screen_charTyped_in_charTyped_FancyMenu(long windowPointer, int codePoint, int modifiers, CallbackInfo info) {
        EventHandler.INSTANCE.postEvent(new ScreenCharTypedEvent(Objects.requireNonNull(Minecraft.getInstance().screen), (char) codePoint));
    }

}