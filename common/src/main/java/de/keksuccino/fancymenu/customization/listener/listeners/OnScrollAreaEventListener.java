package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * A listener for one of the things that happen to the content of a Scrollable Area element: it moved, it reached its start or it reached its end.
 * All of them tell their actions which area it was and where its content is.
 */
public class OnScrollAreaEventListener extends AbstractListener {

    @NotNull
    private final String localizationKey;
    @Nullable
    private String areaIdentifier;
    private int scrollX;
    private int scrollY;
    private float scrollPercent;

    /**
     * @param localizationKey The key of the name of the listener. The key of its description is this one with ".desc" behind it.
     */
    public OnScrollAreaEventListener(@NotNull String identifier, @NotNull String localizationKey) {
        super(identifier);
        this.localizationKey = localizationKey;
    }

    /**
     * @param scrollX How far the content is scrolled sideways, in pixels.
     * @param scrollY How far the content is scrolled downwards, in pixels.
     * @param scrollPercent How far the content is scrolled in the direction the area mainly scrolls in, between 0 and 100.
     */
    public void onScrollAreaEvent(@NotNull String areaIdentifier, int scrollX, int scrollY, float scrollPercent) {
        if (!this.hasInstancesListening()) return;
        this.areaIdentifier = areaIdentifier;
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        this.scrollPercent = scrollPercent;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("element_identifier", () -> (this.areaIdentifier != null) ? this.areaIdentifier : "ERROR"));
        list.add(new CustomVariable("scroll_x", () -> String.valueOf(this.scrollX)));
        list.add(new CustomVariable("scroll_y", () -> String.valueOf(this.scrollY)));
        list.add(new CustomVariable("scroll_percent", () -> String.valueOf(this.scrollPercent)));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable(this.localizationKey);
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines(this.localizationKey + ".desc"));
    }

}
