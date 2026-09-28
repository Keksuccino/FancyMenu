package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContext;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.WidgetIdentificationContextRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

public class WidgetIdentifierHandler {

    private static final String GENERATED_OPTIONS_WIDGET_PREFIX = "fancymenu_options_";
    private static final String CONTEXTUAL_OPTIONS_WIDGET_IDENTIFIER = GENERATED_OPTIONS_WIDGET_PREFIX + "context_button";
    private static final Set<String> CONTEXTUAL_OPTIONS_LOCALIZATION_KEYS = Set.of("options.online", "options.worldOptions.button", "options.difficulty");
    private static final Set<String> CONTEXTUAL_OPTIONS_LEGACY_IDENTIFIERS = CONTEXTUAL_OPTIONS_LOCALIZATION_KEYS.stream().map(WidgetIdentifierHandler::buildOptionsWidgetIdentifier).collect(Collectors.toUnmodifiableSet());
    // Access is confined to client-thread screen discovery; weak keys prevent preview screens from being retained.
    private static final Map<AbstractWidget, String> GENERATED_OPTIONS_WIDGET_IDENTIFIERS = new WeakHashMap<>();

    public static boolean isIdentifierOfWidget(@NotNull String widgetIdentifier, @NotNull WidgetMeta meta) {
        return getIdentifierMatchPriority(widgetIdentifier, meta) > 0;
    }

    /**
     * Zero means no match. Direct identifiers (3) outrank the active Options variant's old identifier (2), which outranks
     * another variant's compatibility alias (1). Compare these priorities within one layout, never across stacked layouts.
     */
    public static int getIdentifierMatchPriority(@NotNull String widgetIdentifier, @NotNull WidgetMeta meta) {
        widgetIdentifier = widgetIdentifier.replace("button_compatibility_id:", "");
        widgetIdentifier = widgetIdentifier.replace("vanillabtn:", "");
        if ((meta.getWidget() instanceof UniqueWidget u) && widgetIdentifier.equals(u.getWidgetIdentifierFancyMenu())) return 3;
        if (MathUtils.isLong(widgetIdentifier)) {
            return widgetIdentifier.equals("" + meta.getLongIdentifier()) ? 3 : 0;
        }
        if (widgetIdentifier.equals(meta.getUniversalIdentifier())) return 3;
        // Only our unambiguous Options discovery may grant these aliases; explicit IDs on other widgets remain exact-only.
        if (CONTEXTUAL_OPTIONS_WIDGET_IDENTIFIER.equals(meta.getUniversalIdentifier()) && CONTEXTUAL_OPTIONS_WIDGET_IDENTIFIER.equals(GENERATED_OPTIONS_WIDGET_IDENTIFIERS.get(meta.getWidget())) && CONTEXTUAL_OPTIONS_LEGACY_IDENTIFIERS.contains(widgetIdentifier)) {
            // Use the discovery snapshot because customization may already have replaced the live widget message.
            String originalKey = getContextualOptionsLocalizationKey(meta.label);
            return originalKey != null && widgetIdentifier.equals(buildOptionsWidgetIdentifier(originalKey)) ? 2 : 1;
        }
        return 0;
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
     * Online, World Options and the older difficulty selector share one slot only when exactly one variant is present.
     */
    public static void assignStableOptionsWidgetIdentifiers(@NotNull List<? extends AbstractWidget> widgets) {
        Set<String> occupiedIdentifiers = new HashSet<>();
        Map<String, Integer> localizationKeyCounts = new HashMap<>();
        Map<String, Integer> generatedIdentifierCounts = new HashMap<>();
        int contextualWidgetCount = 0;
        for (AbstractWidget widget : widgets) {
            UniqueWidget uniqueWidget = (UniqueWidget)widget;
            String previouslyGenerated = GENERATED_OPTIONS_WIDGET_IDENTIFIERS.remove(widget);
            if (previouslyGenerated != null && previouslyGenerated.equals(uniqueWidget.getWidgetIdentifierFancyMenu())) {
                uniqueWidget.setWidgetIdentifierFancyMenu(null);
            }
            String explicitIdentifier = uniqueWidget.getWidgetIdentifierFancyMenu();
            if (explicitIdentifier != null) occupiedIdentifiers.add(explicitIdentifier);
            if (getContextualOptionsLocalizationKey(widget.getMessage()) != null) contextualWidgetCount++;
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
            if (localizationKey == null) continue;
            String generatedIdentifier = buildOptionsWidgetIdentifier(localizationKey);
            if (contextualWidgetCount == 1 && getContextualOptionsLocalizationKey(widget.getMessage()) != null && canAssignContextualOptionsIdentifier(generatedIdentifier, occupiedIdentifiers, generatedIdentifierCounts)) {
                generatedIdentifier = CONTEXTUAL_OPTIONS_WIDGET_IDENTIFIER;
            } else if (localizationKeyCounts.getOrDefault(localizationKey, 0) != 1 || generatedIdentifierCounts.getOrDefault(generatedIdentifier, 0) != 1 || occupiedIdentifiers.contains(generatedIdentifier)) {
                continue;
            }
            uniqueWidget.setWidgetIdentifierFancyMenu(generatedIdentifier);
            GENERATED_OPTIONS_WIDGET_IDENTIFIERS.put(widget, generatedIdentifier);
            occupiedIdentifiers.add(generatedIdentifier);
        }
    }

    private static boolean canAssignContextualOptionsIdentifier(@NotNull String originalIdentifier, @NotNull Set<String> occupiedIdentifiers, @NotNull Map<String, Integer> generatedIdentifierCounts) {
        if (occupiedIdentifiers.contains(CONTEXTUAL_OPTIONS_WIDGET_IDENTIFIER) || generatedIdentifierCounts.containsKey(CONTEXTUAL_OPTIONS_WIDGET_IDENTIFIER)) return false;
        // Reserve the legacy names as well, so an alias can never steal an explicit or generated ID from another widget.
        for (String legacyIdentifier : CONTEXTUAL_OPTIONS_LEGACY_IDENTIFIERS) {
            int ownOccurrences = legacyIdentifier.equals(originalIdentifier) ? 1 : 0;
            if (occupiedIdentifiers.contains(legacyIdentifier) || generatedIdentifierCounts.getOrDefault(legacyIdentifier, 0) > ownOccurrences) return false;
        }
        return true;
    }

    @Nullable
    private static String getContextualOptionsLocalizationKey(@NotNull Component message) {
        if (!(message.getContents() instanceof TranslatableContents contents)) return null;
        if (CONTEXTUAL_OPTIONS_LOCALIZATION_KEYS.contains(contents.getKey())) return contents.getKey();
        // CycleButton wraps the difficulty caption and its changing value in options.generic_value. Inspect only the
        // caption: treating that generic key itself as an alias would also capture the neighboring FOV slider.
        if (contents.getKey().equals("options.generic_value") && contents.getArgs().length == 2 && contents.getArgs()[0] instanceof Component caption && caption.getContents() instanceof TranslatableContents captionContents && captionContents.getKey().equals("options.difficulty")) {
            return captionContents.getKey();
        }
        return null;
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
