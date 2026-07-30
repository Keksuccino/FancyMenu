package de.keksuccino.fancymenu.util.rendering.ui;

import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared input routing rules for FancyMenu components registered in vanilla screens. */
public final class FancyMenuInputRouter {

    public enum MouseReleaseRouting {
        BROADCAST_FANCYMENU_COMPONENTS,
        CAPTURED_COMPONENTS_ONLY
    }

    private FancyMenuInputRouter() {
    }

    @Nullable
    public static GuiEventListener routeMouseClicked(@NotNull Iterable<? extends GuiEventListener> listeners, double mouseX, double mouseY, int button) {
        for (GuiEventListener listener : listeners) {
            if ((listener instanceof FancyMenuUiComponent) && listener.mouseClicked(mouseX, mouseY, button)) return listener;
        }
        return null;
    }

    @Nullable
    public static GuiEventListener routeContainerMouseClicked(@NotNull Iterable<? extends GuiEventListener> listeners, double mouseX, double mouseY, int button) {
        for (GuiEventListener listener : listeners) {
            boolean fancyMenuWidget = listener instanceof FancyMenuWidget;
            if (fancyMenuWidget && !listener.isMouseOver(mouseX, mouseY)) continue;
            if (!fancyMenuWidget && !(listener instanceof FancyMenuUiComponent) && !(listener instanceof MouseButtonCaptureOwner)) continue;
            if (listener.mouseClicked(mouseX, mouseY, button)) return listener;
        }
        return null;
    }

    public static boolean routeMouseScrolled(@NotNull Iterable<? extends GuiEventListener> listeners, double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        for (GuiEventListener listener : listeners) {
            if ((listener instanceof FancyMenuUiComponent) && listener.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        }
        return false;
    }

    public static boolean routeMouseReleased(@NotNull Iterable<? extends GuiEventListener> listeners, @Nullable GuiEventListener focused, double mouseX, double mouseY, int button, @NotNull MouseReleaseRouting routing) {
        if (routing == MouseReleaseRouting.CAPTURED_COMPONENTS_ONLY) {
            boolean dispatched = false;
            for (GuiEventListener listener : listeners) {
                if ((listener instanceof FancyMenuUiComponent) && (listener instanceof MouseButtonCaptureOwner owner) && owner.hasMouseButtonCapture(button)) {
                    listener.mouseReleased(mouseX, mouseY, button);
                    dispatched = true;
                }
            }
            return dispatched;
        }
        boolean consumeRelease = shouldConsumeMouseRelease(listeners, focused, button);
        for (GuiEventListener listener : listeners) {
            if (listener instanceof FancyMenuUiComponent) listener.mouseReleased(mouseX, mouseY, button);
        }
        return consumeRelease;
    }

    private static boolean shouldConsumeMouseRelease(@NotNull Iterable<? extends GuiEventListener> listeners, @Nullable GuiEventListener focused, int button) {
        // Captured releases remain owned even if another component took screen focus after the press.
        for (GuiEventListener listener : listeners) {
            if ((listener instanceof FancyMenuUiComponent) && (listener instanceof MouseButtonCaptureOwner owner) && owner.hasMouseButtonCapture(button)) return true;
        }
        if (!(focused instanceof FancyMenuUiComponent)) return false;
        return !(focused instanceof MouseButtonCaptureOwner owner) || owner.hasMouseButtonCapture(button);
    }

}
