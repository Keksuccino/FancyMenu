package de.keksuccino.fancymenu.customization.element.elements.browser;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Reserves the browser's input order while its native runtime is still starting. */
final class BrowserWidgetSlot<T extends GuiEventListener> implements GuiEventListener, NarratableEntry {

    private final BooleanSupplier ready;
    private final Supplier<T> factory;
    @Nullable private T widget;
    private boolean retired;
    private boolean failed;
    private boolean attached;

    BrowserWidgetSlot(@NotNull BooleanSupplier ready, @NotNull Supplier<T> factory) {
        this.ready = ready;
        this.factory = factory;
    }

    @Nullable
    T resolve() {
        if (this.retired) return null;
        if (this.widget == null && !this.failed && this.ready.getAsBoolean()) {
            try {
                this.widget = Objects.requireNonNull(this.factory.get());
            } catch (RuntimeException ex) {
                // A native creation failure must not cause another creation attempt and error log every frame.
                this.failed = true;
                throw ex;
            }
        }
        return this.widget;
    }

    @NotNull
    GuiEventListener getWidgetToRegister() {
        return this.widget != null ? this.widget : this;
    }

    void attach(@NotNull List<GuiEventListener> children, @Nullable List<GuiEventListener> removeOnInit) {
        if (this.retired || this.attached || this.widget == null) return;
        int index = children.indexOf(this);
        if (index >= 0) {
            // Replace in place: appending would change overlapping elements' input priority. The real browser must
            // remain a direct screen child because the loader keyboard hooks recognize WrappedRinkuBrowser.
            children.set(index, this.widget);
        } else if (!children.contains(this.widget)) {
            return;
        }
        if (removeOnInit != null) {
            int cleanupIndex = removeOnInit.indexOf(this);
            if (cleanupIndex >= 0) removeOnInit.set(cleanupIndex, this.widget);
        }
        this.attached = true;
    }

    boolean isAttached() {
        return this.attached;
    }

    boolean hasFailed() {
        return this.failed;
    }

    void retire() {
        // A resized screen may reuse the cached native browser; retiring this element must not close that browser.
        this.retired = true;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput output) {
    }

}
