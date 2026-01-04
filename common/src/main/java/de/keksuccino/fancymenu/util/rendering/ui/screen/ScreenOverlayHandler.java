package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import de.keksuccino.fancymenu.util.rendering.ui.Tickable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ScreenOverlayHandler {

    public static final ScreenOverlayHandler INSTANCE = new ScreenOverlayHandler();
    private static final Renderable PLACEHOLDER_OVERLAY = (graphics, mouseX, mouseY, partial) -> {};

    private final Map<Long, Renderable> overlays = new LinkedHashMap<>();
    private final Map<Long, OverlayVisibilityController> visibilityControllers = new LinkedHashMap<>();
    private long id = 0;

    private ScreenOverlayHandler() {
    }

    public long addOverlay(@NotNull Renderable overlay) {
        id++;
        this.overlays.put(id, overlay);
        return id;
    }

    public long addOverlay(@NotNull Renderable overlay, @NotNull OverlayVisibilityController controller) {
        long overlayId = addOverlay(overlay);
        addVisibilityController(overlayId, controller);
        return overlayId;
    }

    public long addOverlayFirst(@NotNull Renderable overlay) {
        id++;
        LinkedHashMap<Long, Renderable> reordered = new LinkedHashMap<>();
        reordered.put(id, overlay);
        reordered.putAll(this.overlays);
        this.overlays.clear();
        this.overlays.putAll(reordered);
        return id;
    }

    public long addOverlayFirst(@NotNull Renderable overlay, @NotNull OverlayVisibilityController controller) {
        long overlayId = addOverlayFirst(overlay);
        addVisibilityController(overlayId, controller);
        return overlayId;
    }

    public void addOverlayWithId(long overlayId, @NotNull Renderable body) {
        if (id <= overlayId) {
            id = (overlayId + 10);
        }
        this.overlays.put(overlayId, body);
    }

    public void removeOverlay(long overlayId, boolean preserveIndex, boolean removeController) {
        if (!this.overlays.containsKey(overlayId)) {
            return;
        }
        if (removeController) {
            this.visibilityControllers.remove(overlayId);
        }
        if (preserveIndex) {
            this.overlays.put(overlayId, PLACEHOLDER_OVERLAY);
        } else {
            this.overlays.remove(overlayId);
        }
    }

    public void clearOverlays() {
        this.overlays.clear();
    }

    public void addVisibilityController(long overlayId, @NotNull OverlayVisibilityController controller) {
        this.visibilityControllers.put(overlayId, controller);
    }

    public void removeVisibilityController(long overlayId) {
        this.visibilityControllers.remove(overlayId);
    }

    @NotNull
    public List<Renderable> getOverlays() {
        List<Renderable> filtered = new ArrayList<>();
        for (Renderable overlay : overlays.values()) {
            if (!isPlaceholder(overlay)) {
                filtered.add(overlay);
            }
        }
        return filtered;
    }

    @NotNull
    public List<Renderable> getOverlays(@NotNull Predicate<Renderable> filter) {
        List<Renderable> filtered = new ArrayList<>();
        for (Renderable overlay : overlays.values()) {
            if (!isPlaceholder(overlay) && filter.test(overlay)) {
                filtered.add(overlay);
            }
        }
        return filtered;
    }

    public void renderAll(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        for (Map.Entry<Long, Renderable> entry : overlays.entrySet()) {
            Renderable renderable = entry.getValue();
            if (!isOverlayVisible(entry.getKey(), renderable)) {
                continue;
            }
            renderable.render(graphics, mouseX, mouseY, partial);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return dispatchBooleanEvent(listener -> listener.mouseClicked(mouseX, mouseY, button));
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return dispatchBooleanEvent(listener -> listener.mouseReleased(mouseX, mouseY, button));
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return dispatchBooleanEvent(listener -> listener.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return dispatchBooleanEvent(listener -> listener.mouseScrolled(mouseX, mouseY, deltaX, deltaY));
    }

    public void mouseMoved(double mouseX, double mouseY) {
        dispatchVoidEvent(listener -> listener.mouseMoved(mouseX, mouseY));
    }

    public boolean keyPressed(int button, int scanCode, int modifiers) {
        return dispatchBooleanEvent(listener -> listener.keyPressed(button, scanCode, modifiers));
    }

    public boolean keyReleased(int button, int scanCode, int modifiers) {
        return dispatchBooleanEvent(listener -> listener.keyReleased(button, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return dispatchBooleanEvent(listener -> listener.charTyped(codePoint, modifiers));
    }

    public void tick() {
        List<Map.Entry<Long, Renderable>> ordered = new ArrayList<>(overlays.entrySet());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Map.Entry<Long, Renderable> entry = ordered.get(i);
            Renderable overlay = entry.getValue();
            if (!isOverlayVisible(entry.getKey(), overlay)) {
                continue;
            }
            if (overlay instanceof Tickable tickable) {
                tickable.tick();
            }
        }
    }

    private boolean dispatchBooleanEvent(@NotNull OverlayEvent handler) {
        List<Map.Entry<Long, Renderable>> ordered = new ArrayList<>(overlays.entrySet());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Map.Entry<Long, Renderable> entry = ordered.get(i);
            Renderable overlay = entry.getValue();
            if (!isOverlayVisible(entry.getKey(), overlay)) {
                continue;
            }
            if (overlay instanceof GuiEventListener listener && handler.handle(listener)) {
                return true;
            }
        }
        return false;
    }

    private void dispatchVoidEvent(@NotNull Consumer<GuiEventListener> handler) {
        List<Map.Entry<Long, Renderable>> ordered = new ArrayList<>(overlays.entrySet());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Map.Entry<Long, Renderable> entry = ordered.get(i);
            Renderable overlay = entry.getValue();
            if (!isOverlayVisible(entry.getKey(), overlay)) {
                continue;
            }
            if (overlay instanceof GuiEventListener listener) {
                handler.accept(listener);
            }
        }
    }

    private boolean isOverlayVisible(long overlayId, @NotNull Renderable overlay) {
        if (isPlaceholder(overlay)) {
            return false;
        }
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return true;
        }
        OverlayVisibilityController controller = visibilityControllers.get(overlayId);
        if (controller == null) {
            return true;
        }
        return controller.isVisible(screen);
    }

    private boolean isPlaceholder(@NotNull Renderable overlay) {
        return overlay == PLACEHOLDER_OVERLAY;
    }

    @FunctionalInterface
    public interface OverlayVisibilityController {
        boolean isVisible(@NotNull Screen screen);
    }

    @FunctionalInterface
    private interface OverlayEvent {
        boolean handle(@NotNull GuiEventListener listener);
    }

}
