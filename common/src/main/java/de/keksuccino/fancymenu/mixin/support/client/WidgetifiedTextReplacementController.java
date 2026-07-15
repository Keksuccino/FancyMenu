package de.keksuccino.fancymenu.mixin.support.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Coordinates a widgetified text replacement with its matching Vanilla draw call.
 *
 * <p>The render decision must reflect the replacement created during the latest screen initialization.
 * Looking up customization state again while rendering can otherwise produce either two text draws or no text
 * when that state changes before the screen is initialized again.</p>
 */
public final class WidgetifiedTextReplacementController<T> {

    @Nullable private T replacement;

    /**
     * Clears the previous replacement and creates a new one only when customization is enabled.
     * A factory that cannot provide a replacement leaves Vanilla rendering active as a safe fallback.
     */
    public @Nullable T initialize(boolean customizationEnabled, @NotNull Supplier<@Nullable T> replacementFactory) {
        this.replacement = null;
        if (!customizationEnabled) return null;
        this.replacement = replacementFactory.get();
        return this.replacement;
    }

    public boolean shouldRenderVanillaText() {
        return this.replacement == null;
    }

}
