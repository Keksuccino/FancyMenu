package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.widget.identification.identificationcontext.contexts.TitleScreenWidgetIdentificationContext;
import de.keksuccino.fancymenu.platform.Services;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Upgrades the vanilla widget customizations of layouts saved with an older widget identifier revision.<br><br>
 *
 * Layouts store the widget identifier revision they were saved with, and steps only run for older revisions, because only these
 * layouts can use outdated identifiers. A layout saved with the current revision can lack an entry for a widget on purpose
 * (it simply was never customized), so it must never get migrated again.
 */
public final class WidgetIdentifierMigration {

    /**
     * The widget identifier revision layouts get saved with.<br>
     * Bump it and add a step to {@link #migrate(List, int, Supplier)} whenever a widget that shares an identifier with another widget gets its own one,
     * or whenever a widget identifier gets replaced by another one.
     */
    public static final int CURRENT_REVISION = 2;
    /** The revision of layouts saved before revisions got stored. */
    public static final int UNVERSIONED_REVISION = 0;

    private static final String FORGE_MODS_BUTTON_IDENTIFIER = "forge_titlescreen_mods_button";
    private static final String MOD_MENU_MODS_BUTTON_IDENTIFIER = "modmenu_titlescreen_mods_button";
    /** Widget identifiers that got replaced by another identifier, mapped to their replacement. */
    private static final Map<String, String> REPLACED_IDENTIFIERS = Map.of(
            FORGE_MODS_BUTTON_IDENTIFIER, TitleScreenWidgetIdentificationContext.MODS_BUTTON_IDENTIFIER,
            MOD_MENU_MODS_BUTTON_IDENTIFIER, TitleScreenWidgetIdentificationContext.MODS_BUTTON_IDENTIFIER
    );

    private WidgetIdentifierMigration() {
    }

    public static void migrate(@NotNull List<SerializedElement> serializedWidgetElements, int savedRevision) {
        // Has to stay a lambda: a method reference would initialize Services right away, even for layouts that never need the mod loader.
        migrate(serializedWidgetElements, savedRevision, () -> Services.PLATFORM.getPlatformName());
    }

    /**
     * @param modLoaderName Supplies the name of the running mod loader. Steps only call it when they have to choose between customizations made on different loaders.
     */
    static void migrate(@NotNull List<SerializedElement> serializedWidgetElements, int savedRevision, @NotNull Supplier<String> modLoaderName) {
        // Report Player shared the Open to LAN button's identifier, because pause menus up to Minecraft 26.1 show it in the LAN button's place outside of unpublished singleplayer worlds.
        if (savedRevision < 1) copyCustomizationsToSplitIdentifier(serializedWidgetElements, "pause_share_to_lan_button", "pause_player_reporting_button");
        // The Title screen's Mods button had one identifier on Forge/NeoForge and another one with Mod Menu on Fabric, so its customizations only worked on the loader they were made on.
        if (savedRevision < 2) mergeCustomizationsIntoIdentifier(serializedWidgetElements, TitleScreenWidgetIdentificationContext.MODS_BUTTON_IDENTIFIER, () -> "fabric".equals(modLoaderName.get()) ? MOD_MENU_MODS_BUTTON_IDENTIFIER : FORGE_MODS_BUTTON_IDENTIFIER);
    }

    /**
     * Returns the identifier that replaced the given prefix-free widget identifier, or the given identifier if it never got replaced.<br>
     * Only vanilla widget customizations get migrated, so widget references stored anywhere else, like element anchors, actions, requirements and placeholders,
     * need this to keep finding their widget.
     */
    @NotNull
    public static String getCurrentIdentifier(@NotNull String identifier) {
        return REPLACED_IDENTIFIERS.getOrDefault(identifier, identifier);
    }

    /**
     * Gives the widget split off a shared identifier a copy of every customization of the shared identifier, so it looks the same as before the split.<br>
     * Does nothing if the split identifier is already customized, which happens when a newer FancyMenu version saved the layout before an older one.
     */
    private static void copyCustomizationsToSplitIdentifier(@NotNull List<SerializedElement> serializedWidgetElements, @NotNull String sharedIdentifier, @NotNull String splitIdentifier) {
        List<SerializedElement> copies = new ArrayList<>();
        for (SerializedElement serialized : serializedWidgetElements) {
            String identifier = WidgetIdentifierHandler.getTargetWidgetIdentifier(serialized);
            if (splitIdentifier.equals(identifier)) return;
            if (sharedIdentifier.equals(identifier)) copies.add(copyWithTargetIdentifier(serialized, splitIdentifier));
        }
        serializedWidgetElements.addAll(copies);
    }

    /**
     * Moves the customizations of all identifiers replaced by the merged identifier over to it, keeping their order.<br>
     * A layout edited on different loaders can customize more than one of the replaced identifiers. Stacking these customizations would combine
     * unrelated settings, so only the ones of the preferred identifier get kept. Customizations that already use the merged identifier always win,
     * which happens when a newer FancyMenu version saved the layout before an older one.
     *
     * @param preferredReplacedIdentifier Supplies the replaced identifier to keep. Only gets called if the layout customizes more than one of them.
     */
    private static void mergeCustomizationsIntoIdentifier(@NotNull List<SerializedElement> serializedWidgetElements, @NotNull String mergedIdentifier, @NotNull Supplier<String> preferredReplacedIdentifier) {
        Set<String> customizedReplacedIdentifiers = new HashSet<>();
        boolean mergedIdentifierCustomized = false;
        for (SerializedElement serialized : serializedWidgetElements) {
            String identifier = WidgetIdentifierHandler.getTargetWidgetIdentifier(serialized);
            if (mergedIdentifier.equals(identifier)) {
                mergedIdentifierCustomized = true;
            } else if ((identifier != null) && mergedIdentifier.equals(REPLACED_IDENTIFIERS.get(identifier))) {
                customizedReplacedIdentifiers.add(identifier);
            }
        }
        if (customizedReplacedIdentifiers.isEmpty()) return;
        String keptIdentifier = null;
        if (!mergedIdentifierCustomized) {
            keptIdentifier = (customizedReplacedIdentifiers.size() == 1) ? customizedReplacedIdentifiers.iterator().next() : preferredReplacedIdentifier.get();
        }
        Iterator<SerializedElement> iterator = serializedWidgetElements.iterator();
        while (iterator.hasNext()) {
            SerializedElement serialized = iterator.next();
            String identifier = WidgetIdentifierHandler.getTargetWidgetIdentifier(serialized);
            if ((identifier == null) || !customizedReplacedIdentifiers.contains(identifier)) continue;
            if (identifier.equals(keptIdentifier)) {
                serialized.putProperty("instance_identifier", mergedIdentifier);
            } else {
                iterator.remove();
            }
        }
    }

    @NotNull
    private static SerializedElement copyWithTargetIdentifier(@NotNull SerializedElement serialized, @NotNull String targetIdentifier) {
        SerializedElement copy = new SerializedElement();
        copy.setType(serialized.getType());
        // Copy the raw values, so nothing gets serialized twice.
        copy.getProperties().putAll(serialized.getProperties());
        copy.putProperty("instance_identifier", targetIdentifier);
        return copy;
    }

}
