package de.keksuccino.fancymenu.customization.widget.identification.identificationcontext;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link WidgetIdentificationContext}s can help FancyMenu to better handle screens if their default widget layout changes,
 * like when a mod is adding a new widget, moves default widgets, or changes their size.<br>
 * Normally this would break layouts and the user would need to fix them, but if widgets have a universal identifier, they
 * should still be detectable by FancyMenu, even if their default position or size changes.<br><br>
 *
 * Keep in mind that this does not add support for widgets that are added to screens in a non-vanilla way,
 * which means they are not registered to the screen's {@code renderables} list.
 */
public abstract class WidgetIdentificationContext {

    protected final List<ConsumingSupplier<WidgetMeta, String>> identifierProviders = new ArrayList<>();

    @NotNull
    public abstract Class<? extends Screen> getTargetScreen();

    /**
     * Providers return a {@link String} in case they consume a matching {@link WidgetMeta}.<br>
     * They return NULL if the given {@link WidgetMeta} does not match.
     **/
    public void addUniversalIdentifierProvider(@NotNull ConsumingSupplier<WidgetMeta, String> provider) {
        this.identifierProviders.add(Objects.requireNonNull(provider));
    }

    @Nullable
    public String getUniversalIdentifierForWidget(@NotNull WidgetMeta meta) {
        for (ConsumingSupplier<WidgetMeta, String> provider : this.identifierProviders) {
            String universal = provider.get(meta);
            if (universal != null) return universal;
        }
        return null;
    }

}
