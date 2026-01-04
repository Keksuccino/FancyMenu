package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import de.keksuccino.fancymenu.util.rendering.ui.Tickable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ScreenOverlayHandler {

    public static final ScreenOverlayHandler INSTANCE = new ScreenOverlayHandler();

    private final Map<Long, Renderable> overlays = new LinkedHashMap<>();
    private long id = 0;

    private ScreenOverlayHandler() {
    }

    public long addOverlay(@NotNull Renderable overlay) {
        id++;
        this.overlays.put(id, overlay);
        return id;
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

    public void replaceOverlay(long idToReplace, @NotNull Renderable body) {
        if (id <= idToReplace) {
            id = (idToReplace + 1);
        }
        Renderable old = this.overlays.get(idToReplace);
        this.overlays.put(idToReplace, body);
    }

    public void removeOverlay(long id) {
        this.overlays.remove(id);
    }

    public void clearOverlays() {
        this.overlays.clear();
    }

    @NotNull
    public List<Renderable> getOverlays() {
        return new ArrayList<>(overlays.values());
    }

    @NotNull
    public List<Renderable> getOverlays(@NotNull Predicate<Renderable> filter) {
        List<Renderable> filtered = new ArrayList<>();
        for (Renderable body : overlays.values()) {
            if (filter.test(body)) {
                filtered.add(body);
            }
        }
        return filtered;
    }

    public void reorderOverlays(@NotNull Comparator<Map.Entry<Long, Renderable>> comparator) {
        List<Map.Entry<Long, Renderable>> entries = new ArrayList<>(overlays.entrySet());
        entries.sort(comparator);
        overlays.clear();
        for (Map.Entry<Long, Renderable> entry : entries) {
            overlays.put(entry.getKey(), entry.getValue());
        }
    }

    public void renderAll(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        overlays.values().forEach(renderable -> renderable.render(graphics, mouseX, mouseY, partial));
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
        List<Renderable> ordered = new ArrayList<>(overlays.values());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Renderable overlay = ordered.get(i);
            if (overlay instanceof Tickable tickable) {
                tickable.tick();
            }
        }
    }

    private boolean dispatchBooleanEvent(@NotNull OverlayEvent handler) {
        List<Renderable> ordered = new ArrayList<>(overlays.values());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Renderable overlay = ordered.get(i);
            if (overlay instanceof GuiEventListener listener && handler.handle(listener)) {
                return true;
            }
        }
        return false;
    }

    private void dispatchVoidEvent(@NotNull Consumer<GuiEventListener> handler) {
        List<Renderable> ordered = new ArrayList<>(overlays.values());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Renderable overlay = ordered.get(i);
            if (overlay instanceof GuiEventListener listener) {
                handler.accept(listener);
            }
        }
    }

    @FunctionalInterface
    private interface OverlayEvent {
        boolean handle(@NotNull GuiEventListener listener);
    }

}
