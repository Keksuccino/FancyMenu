package de.keksuccino.fancymenu.mixin.mixins.neoforge.client;

import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinNeoForgeKeyboardHandler {

    /**
     * @reason This adds special key press handling for FancyMenu's {@link WrappedMCEFBrowser}.
     */
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"), cancellable = true)
    private void before_keyPressed_in_keyPress_FancyMenu(long window, int action, KeyEvent event, CallbackInfo info) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof WrappedMCEFBrowser) {
                    boolean b = false;
                    if (action == 1 || action == 2) {
                        b = listener.keyPressed(event);
                    } else if (action == 0) {
                        b = listener.keyReleased(event);
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
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"), cancellable = true)
    private void before_keyReleased_in_keyPress_FancyMenu(long window, int action, KeyEvent event, CallbackInfo info) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof WrappedMCEFBrowser) {
                    boolean b = false;
                    if (action == 1 || action == 2) {
                        b = listener.keyPressed(event);
                    } else if (action == 0) {
                        b = listener.keyReleased(event);
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
    private void head_charTyped_FancyMenu(long window, CharacterEvent event, CallbackInfo info) {

        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen != null) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof WrappedMCEFBrowser) {
                    boolean b = false;
                    if (Character.charCount(event.codepoint()) == 1) {
                        b = listener.charTyped(event);
                    } else {
                        for (char c : Character.toChars(event.codepoint())) {
                            b = !b ? listener.charTyped(event) : true;
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

}