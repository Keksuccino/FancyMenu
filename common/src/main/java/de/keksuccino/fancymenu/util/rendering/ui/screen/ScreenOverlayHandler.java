package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ScreenOverlayHandler extends AbstractContainerEventHandler {

    public static final ScreenOverlayHandler INSTANCE = new ScreenOverlayHandler();

    private final Map<Long, Renderable> overlays = new LinkedHashMap<>();
    private final List<GuiEventListener> children = new ArrayList<>();
    private long id = 0;

    private ScreenOverlayHandler() {
    }

    public long addOverlay(@NotNull Renderable overlay) {
        id++;
        this.overlays.put(id, overlay);
        if ((overlay instanceof GuiEventListener l) && !this.children.contains(l)) this.children.add(l);
        return id;
    }

    public long addOverlayFirst(@NotNull Renderable overlay) {
        id++;
        LinkedHashMap<Long, Renderable> reordered = new LinkedHashMap<>();
        if ((overlay instanceof GuiEventListener l) && !this.children.contains(l)) this.children.add(l);
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
        if (old instanceof GuiEventListener l) this.children.removeIf(listener -> (l == listener)); // remove all possible occurrences of the overlay
        this.overlays.put(idToReplace, body);
    }

    public void removeOverlay(long id) {
        Renderable toRemove = this.overlays.get(id);
        if (toRemove instanceof GuiEventListener l) this.children.removeIf(listener -> (l == listener)); // remove all possible occurrences of the overlay
        this.overlays.remove(id);
    }

    public void clearOverlays() {
        this.overlays.clear();
        this.children.clear();
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

    public void renderAllOverlays(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        overlays.values().forEach(Renderable -> Renderable.render(graphics, mouseX, mouseY, partial));
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.children;
    }

}
