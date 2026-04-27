package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.browser;

import de.keksuccino.fancymenu.customization.overlay.ScreenOverlays;
import de.keksuccino.fancymenu.util.VanillaEvents;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BrowserOverlayInputConsumers {

    private static final Dispatcher DISPATCHER = new Dispatcher();
    private static final Map<String, GuiEventListener> CONSUMERS = new LinkedHashMap<>();

    static {
        ScreenOverlayHandler.INSTANCE.addOverlayWithId(ScreenOverlays.BROWSER_DECORATION_OVERLAY_INPUT_CONSUMERS, DISPATCHER);
    }

    private BrowserOverlayInputConsumers() {
    }

    public static void register(@NotNull String key, @NotNull GuiEventListener consumer) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(consumer);
        CONSUMERS.put(key, consumer);
    }

    public static void unregister(@NotNull String key) {
        Objects.requireNonNull(key);
        CONSUMERS.remove(key);
    }

    public static void clear() {
        CONSUMERS.clear();
    }

    private static boolean dispatchBoolean(@NotNull BooleanEvent event) {
        List<GuiEventListener> ordered = new ArrayList<>(CONSUMERS.values());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            GuiEventListener listener = ordered.get(i);
            if (event.handle(listener)) {
                return true;
            }
        }
        return false;
    }

    private static final class Dispatcher implements Renderable, GuiEventListener {

        private boolean focused = false;

        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            // Input-only dispatcher layer.
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            return dispatchBoolean(listener -> listener.mouseClicked(event, isDoubleClick));
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return dispatchBoolean(listener -> listener.mouseClicked(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), false));
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            return dispatchBoolean(listener -> listener.mouseReleased(event));
        }
        
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return dispatchBoolean(listener -> listener.mouseReleased(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button)));
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            return dispatchBoolean(listener -> listener.mouseDragged(event, dragX, dragY));
        }
        
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return dispatchBoolean(listener -> listener.mouseDragged(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), dragX, dragY));
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            return dispatchBoolean(listener -> listener.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY));
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            return dispatchBoolean(listener -> listener.keyPressed(event));
        }
        
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return dispatchBoolean(listener -> listener.keyPressed(VanillaEvents.keyEvent(keyCode, scanCode, modifiers)));
        }

        @Override
        public boolean keyReleased(KeyEvent event) {
            return dispatchBoolean(listener -> listener.keyReleased(event));
        }
        
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            return dispatchBoolean(listener -> listener.keyReleased(VanillaEvents.keyEvent(keyCode, scanCode, modifiers)));
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            return dispatchBoolean(listener -> listener.charTyped(event));
        }
        
        public boolean charTyped(char codePoint, int modifiers) {
            return dispatchBoolean(listener -> listener.charTyped(VanillaEvents.characterEvent(codePoint, modifiers)));
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return true;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }
    }

    @FunctionalInterface
    private interface BooleanEvent {
        boolean handle(@NotNull GuiEventListener listener);
    }
}
