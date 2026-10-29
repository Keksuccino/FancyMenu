package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContext;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContextRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class WidgetIdentifierHandler {

    private static final String GENERATED_OPTIONS_WIDGET_PREFIX = "fancymenu_options_";
    // Access is confined to client-thread screen discovery; weak keys prevent preview screens from being retained.
    private static final Map<AbstractWidget, String> GENERATED_OPTIONS_WIDGET_IDENTIFIERS = new WeakHashMap<>();

    public static boolean isIdentifierOfWidget(@NotNull String widgetIdentifier, @NotNull WidgetMeta meta) {
        widgetIdentifier = stripIdentifierPrefixes(widgetIdentifier);
        if ((meta.getWidget() instanceof UniqueWidget u) && widgetIdentifier.equals(u.getWidgetIdentifierFancyMenu())) return true;
        if (MathUtils.isLong(widgetIdentifier)) {
            return widgetIdentifier.equals("" + meta.getLongIdentifier());
        }
        return widgetIdentifier.equals(meta.getUniversalIdentifier());
    }

    /**
     * Returns the prefix-free identifier of the widget a serialized vanilla widget customization targets, or NULL if it has none.
     */
    @Nullable
    public static String getTargetWidgetIdentifier(@NotNull SerializedElement serializedWidgetElement) {
        String identifier = serializedWidgetElement.getValue("instance_identifier");
        // Very old layouts store it as action ID; element deserialization falls back to it the same way.
        if (identifier == null) identifier = serializedWidgetElement.getValue("actionid");
        return (identifier != null) ? stripIdentifierPrefixes(identifier) : null;
    }

    /**
     * Returns the serialized vanilla widget customizations whose stable identifier matches none of the given widgets.<br>
     * Some widgets only exist in certain contexts, like buttons that only show up in singleplayer or only in multiplayer. An editor that only
     * re-serializes the widgets of its current screen instance has to keep these entries, otherwise editing a layout in one context deletes
     * the customizations made in the other one.<br>
     * Numeric identifiers are never kept, because they are derived from widget positions, so an unmatched one is indistinguishable from an outdated one.
     */
    @NotNull
    public static List<SerializedElement> getCustomizationsOfAbsentWidgets(@NotNull List<SerializedElement> serializedWidgetElements, @NotNull List<WidgetMeta> currentWidgets) {
        List<SerializedElement> absentWidgetCustomizations = new ArrayList<>();
        for (SerializedElement serialized : serializedWidgetElements) {
            String identifier = getTargetWidgetIdentifier(serialized);
            if ((identifier == null) || MathUtils.isLong(identifier) || currentWidgets.stream().anyMatch(meta -> isIdentifierOfWidget(identifier, meta))) continue;
            absentWidgetCustomizations.add(serialized);
        }
        return absentWidgetCustomizations;
    }

    @Nullable
    public static String getUniversalIdentifierForWidgetMeta(@NotNull WidgetMeta meta) {
        if ((meta.getWidget() instanceof UniqueWidget u) && (u.getWidgetIdentifierFancyMenu() != null)) return u.getWidgetIdentifierFancyMenu();
        try {
            WidgetIdentificationContext c = WidgetIdentificationContextRegistry.getContextForScreen(meta.getScreen().getClass());
            if (c != null) {
                return c.getUniversalIdentifierForWidget(meta);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void setUniversalIdentifierOfWidgetMeta(@NotNull WidgetMeta meta) {
        meta.setUniversalIdentifier(getUniversalIdentifierForWidgetMeta(meta));
    }

    /**
     * Gives Options-screen widgets stable identifiers without replacing their legacy coordinate identifiers. Generated
     * identifiers are limited to untranslated localization keys that occur exactly once in the current screen instance.
     */
    public static void assignStableOptionsWidgetIdentifiers(@NotNull List<? extends AbstractWidget> widgets) {
        Set<String> occupiedIdentifiers = new HashSet<>();
        Map<String, Integer> localizationKeyCounts = new HashMap<>();
        Map<String, Integer> generatedIdentifierCounts = new HashMap<>();
        for (AbstractWidget widget : widgets) {
            UniqueWidget uniqueWidget = (UniqueWidget)widget;
            String previouslyGenerated = GENERATED_OPTIONS_WIDGET_IDENTIFIERS.remove(widget);
            if (previouslyGenerated != null && previouslyGenerated.equals(uniqueWidget.getWidgetIdentifierFancyMenu())) {
                uniqueWidget.setWidgetIdentifierFancyMenu(null);
            }
            String explicitIdentifier = uniqueWidget.getWidgetIdentifierFancyMenu();
            if (explicitIdentifier != null) occupiedIdentifiers.add(explicitIdentifier);
            String localizationKey = getUntranslatedLocalizationKey(widget);
            if (localizationKey != null) {
                localizationKeyCounts.merge(localizationKey, 1, Integer::sum);
                generatedIdentifierCounts.merge(buildOptionsWidgetIdentifier(localizationKey), 1, Integer::sum);
            }
        }

        for (AbstractWidget widget : widgets) {
            UniqueWidget uniqueWidget = (UniqueWidget)widget;
            if (uniqueWidget.getWidgetIdentifierFancyMenu() != null) continue;
            String localizationKey = getUntranslatedLocalizationKey(widget);
            if (localizationKey == null || localizationKeyCounts.getOrDefault(localizationKey, 0) != 1) continue;
            String generatedIdentifier = buildOptionsWidgetIdentifier(localizationKey);
            if (generatedIdentifierCounts.getOrDefault(generatedIdentifier, 0) != 1 || occupiedIdentifiers.contains(generatedIdentifier)) continue;
            uniqueWidget.setWidgetIdentifierFancyMenu(generatedIdentifier);
            GENERATED_OPTIONS_WIDGET_IDENTIFIERS.put(widget, generatedIdentifier);
            occupiedIdentifiers.add(generatedIdentifier);
        }
    }

    @NotNull
    private static String stripIdentifierPrefixes(@NotNull String widgetIdentifier) {
        return widgetIdentifier.replace("button_compatibility_id:", "").replace("vanillabtn:", "");
    }

    @Nullable
    static String getUntranslatedLocalizationKey(@NotNull AbstractWidget widget) {
        if (widget.getMessage().getContents() instanceof TranslatableContents contents) return contents.getKey();
        return null;
    }

    @NotNull
    static String buildOptionsWidgetIdentifier(@NotNull String localizationKey) {
        StringBuilder sanitized = new StringBuilder(localizationKey.length());
        for (int i = 0; i < localizationKey.length(); i++) {
            char character = Character.toLowerCase(localizationKey.charAt(i));
            sanitized.append((character >= 'a' && character <= 'z') || (character >= '0' && character <= '9') || character == '.' || character == '_' || character == '-' ? character : '_');
        }
        return GENERATED_OPTIONS_WIDGET_PREFIX + sanitized.toString().toLowerCase(Locale.ROOT);
    }

}
