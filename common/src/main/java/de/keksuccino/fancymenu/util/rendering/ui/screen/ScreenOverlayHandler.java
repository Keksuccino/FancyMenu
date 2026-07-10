package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.ScreenUtils;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import de.keksuccino.fancymenu.util.VanillaEvents;
import de.keksuccino.fancymenu.util.rendering.ui.Tickable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ScreenOverlayHandler {

    public static final ScreenOverlayHandler INSTANCE = new ScreenOverlayHandler(ScreenUtils::getScreen);
    private static final Renderable PLACEHOLDER_OVERLAY = (graphics, mouseX, mouseY, partial) -> {};

    private final Map<Long, Renderable> overlays = new LinkedHashMap<>();
    private final Map<Long, OverlayVisibilityController> visibilityControllers = new LinkedHashMap<>();
    private final Map<Long, OverlayInputConsumptionController> inputConsumptionControllers = new LinkedHashMap<>();
    private final Map<Integer, GuiEventListener> capturedMouseListeners = new HashMap<>();
    private final Set<Integer> detachedMouseButtons = new HashSet<>();
    private final Set<Integer> pendingMouseButtons = new HashSet<>();
    private final Supplier<@Nullable Screen> currentScreenSupplier;
    private long mouseCaptureGeneration = 0;
    private long id = 0;

    ScreenOverlayHandler(@NotNull Supplier<@Nullable Screen> currentScreenSupplier) {
        this.currentScreenSupplier = currentScreenSupplier;
    }

    public long addOverlay(@NotNull Renderable overlay) {
        id++;
        this.overlays.put(id, overlay);
        return id;
    }

    public long addPlaceholder() {
        id++;
        this.overlays.put(id, PLACEHOLDER_OVERLAY);
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

    public long addPlaceholderFirst() {
        id++;
        LinkedHashMap<Long, Renderable> reordered = new LinkedHashMap<>();
        reordered.put(id, PLACEHOLDER_OVERLAY);
        reordered.putAll(this.overlays);
        this.overlays.clear();
        this.overlays.putAll(reordered);
        return id;
    }

    public void addOverlayWithId(long overlayId, @NotNull Renderable body) {
        if (id <= overlayId) {
            id = (overlayId + 10);
        }
        this.detachMouseCaptureFor(this.overlays.get(overlayId));
        this.overlays.put(overlayId, body);
    }

    public void removeOverlay(long overlayId, boolean preserveIndex, boolean removeController) {
        if (!this.overlays.containsKey(overlayId)) {
            return;
        }
        this.detachMouseCaptureFor(this.overlays.get(overlayId));
        if (removeController) {
            this.visibilityControllers.remove(overlayId);
            this.inputConsumptionControllers.remove(overlayId);
        }
        if (preserveIndex) {
            this.overlays.put(overlayId, PLACEHOLDER_OVERLAY);
        } else {
            this.overlays.remove(overlayId);
        }
    }

    public void clearOverlays() {
        this.detachMouseCaptures();
        this.overlays.clear();
    }

    public void setVisibilityControllerFor(long overlayId, @Nullable OverlayVisibilityController controller) {
        if (controller == null) {
            this.visibilityControllers.remove(overlayId);
            return;
        }
        this.visibilityControllers.put(overlayId, controller);
    }

    public void setInputConsumptionControllerFor(long overlayId, @Nullable OverlayInputConsumptionController controller) {
        if (controller == null) {
            this.inputConsumptionControllers.remove(overlayId);
            return;
        }
        this.inputConsumptionControllers.put(overlayId, controller);
    }

    @Nullable
    public Renderable getOverlay(long overlayId) {
        Renderable overlay = this.overlays.get(overlayId);
        return (overlay == null || isPlaceholder(overlay)) ? null : overlay;
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

    public void renderAll(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        for (Map.Entry<Long, Renderable> entry : overlays.entrySet()) {
            Renderable renderable = entry.getValue();
            if (!isOverlayVisible(entry.getKey(), renderable)) {
                continue;
            }
            renderable.extractRenderState(graphics, mouseX, mouseY, partial);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // A missing release (for example while the window loses focus) must not let an old owner capture a later press.
        this.prepareMousePress(button);
        long captureGeneration = this.mouseCaptureGeneration;
        this.pendingMouseButtons.add(button);
        GuiEventListener listener;
        try {
            listener = this.findEventConsumer(candidate -> candidate.mouseClicked(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), false) || captureGeneration != this.mouseCaptureGeneration);
        } finally {
            this.pendingMouseButtons.remove(button);
        }
        // A screen/global overlay reset during the callback invalidates its old listener even though ownership was not recordable until the callback returned.
        if (captureGeneration != this.mouseCaptureGeneration) return true;
        if (listener == null) return false;
        // Record ownership after the callback. Click handlers are allowed to synchronously close/remove themselves, but still own this physical press through release.
        this.capturedMouseListeners.put(button, listener);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Remove before invoking user code so a reentrant press for the same button can establish a new, independent capture.
        GuiEventListener captured = this.capturedMouseListeners.remove(button);
        if (captured != null) {
            captured.mouseReleased(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button));
            return true;
        }
        if (this.detachedMouseButtons.remove(button)) return true;
        return dispatchBooleanEvent(listener -> listener.mouseReleased(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button)));
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        GuiEventListener captured = this.capturedMouseListeners.get(button);
        if (captured != null) {
            captured.mouseDragged(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), deltaX, deltaY);
            return true;
        }
        if (this.detachedMouseButtons.contains(button)) return true;
        return dispatchBooleanEvent(listener -> listener.mouseDragged(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), deltaX, deltaY));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return dispatchBooleanEvent(listener -> listener.mouseScrolled(mouseX, mouseY, deltaX, deltaY));
    }

    public void mouseMoved(double mouseX, double mouseY) {
        dispatchVoidEvent(listener -> listener.mouseMoved(mouseX, mouseY));
    }

    public boolean keyPressed(int button, int scanCode, int modifiers) {
        return dispatchBooleanEvent(listener -> listener.keyPressed(VanillaEvents.keyEvent(button, scanCode, modifiers)));
    }

    public boolean keyReleased(int button, int scanCode, int modifiers) {
        return dispatchBooleanEvent(listener -> listener.keyReleased(VanillaEvents.keyEvent(button, scanCode, modifiers)));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return dispatchBooleanEvent(listener -> listener.charTyped(VanillaEvents.characterEvent(codePoint, modifiers)));
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

    /**
     * Drops listener references when the overlay or screen stack is replaced, while still consuming the remainder of each active physical press.
     * This prevents events from leaking into the replacement screen without retaining removed UI objects until a potentially missing release.
     */
    public void detachMouseCaptures() {
        this.mouseCaptureGeneration++;
        this.detachedMouseButtons.addAll(this.capturedMouseListeners.keySet());
        this.detachedMouseButtons.addAll(this.pendingMouseButtons);
        this.capturedMouseListeners.clear();
    }

    /**
     * Drops listener references when input focus is lost. Reference-free button sentinels still block a late drag/release, and a new press clears them if GLFW omits the release.
     */
    public void cancelMouseCaptures() {
        this.detachMouseCaptures();
    }

    /**
     * Clears stale ownership at the start of a physical press, before loader hooks can cancel the rest of its dispatch.
     * {@link #mouseClicked(double, double, int)} also calls this so direct callers retain the same missing-release recovery.
     */
    public void prepareMousePress(int button) {
        this.capturedMouseListeners.remove(button);
        this.detachedMouseButtons.remove(button);
    }

    private boolean dispatchBooleanEvent(@NotNull OverlayEvent handler) {
        return this.findEventConsumer(handler) != null;
    }

    @Nullable
    private GuiEventListener findEventConsumer(@NotNull OverlayEvent handler) {
        List<Map.Entry<Long, Renderable>> ordered = new ArrayList<>(overlays.entrySet());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Map.Entry<Long, Renderable> entry = ordered.get(i);
            Renderable overlay = entry.getValue();
            if (!isOverlayVisible(entry.getKey(), overlay)) {
                continue;
            }
            if (!isOverlayInputEnabled(entry.getKey(), overlay)) {
                continue;
            }
            if (overlay instanceof GuiEventListener listener && handler.handle(listener)) {
                return listener;
            }
        }
        return null;
    }

    private void dispatchVoidEvent(@NotNull Consumer<GuiEventListener> handler) {
        List<Map.Entry<Long, Renderable>> ordered = new ArrayList<>(overlays.entrySet());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Map.Entry<Long, Renderable> entry = ordered.get(i);
            Renderable overlay = entry.getValue();
            if (!isOverlayVisible(entry.getKey(), overlay)) {
                continue;
            }
            if (!isOverlayInputEnabled(entry.getKey(), overlay)) {
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
        Screen screen = this.currentScreenSupplier.get();
        if (screen == null) {
            return true;
        }
        OverlayVisibilityController controller = visibilityControllers.get(overlayId);
        if (controller == null) {
            return true;
        }
        return controller.isVisible(screen);
    }

    private boolean isOverlayInputEnabled(long overlayId, @NotNull Renderable overlay) {
        if (isPlaceholder(overlay)) {
            return false;
        }
        Screen screen = this.currentScreenSupplier.get();
        if (screen == null) {
            return true;
        }
        OverlayInputConsumptionController controller = inputConsumptionControllers.get(overlayId);
        if (controller == null) {
            return true;
        }
        return controller.canConsumeInput(screen);
    }

    private boolean isPlaceholder(@NotNull Renderable overlay) {
        return overlay == PLACEHOLDER_OVERLAY;
    }

    private void detachMouseCaptureFor(@Nullable Renderable overlay) {
        if (!(overlay instanceof GuiEventListener listener)) return;
        this.capturedMouseListeners.entrySet().removeIf(entry -> {
            if (entry.getValue() != listener) return false;
            this.detachedMouseButtons.add(entry.getKey());
            return true;
        });
    }

    @FunctionalInterface
    public interface OverlayVisibilityController {
        boolean isVisible(@NotNull Screen screen);
    }

    @FunctionalInterface
    public interface OverlayInputConsumptionController {
        boolean canConsumeInput(@NotNull Screen screen);
    }

    @FunctionalInterface
    private interface OverlayEvent {
        boolean handle(@NotNull GuiEventListener listener);
    }

}
