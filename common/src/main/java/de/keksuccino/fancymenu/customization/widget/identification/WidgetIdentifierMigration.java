package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Upgrades the vanilla widget customizations of layouts saved before a widget got its own identifier.<br><br>
 *
 * Layouts store the widget identifier revision they were saved with, and steps only run for older revisions, because only these
 * layouts used one identifier for both widgets. A layout saved with the current revision can lack an entry for one of the split
 * widgets on purpose (it simply was never customized), so it must never get migrated again.
 */
public final class WidgetIdentifierMigration {

    /**
     * The widget identifier revision layouts get saved with.<br>
     * Bump it and add a step to {@link #migrate(List, int)} whenever a widget that shares an identifier with another widget gets its own one.
     */
    public static final int CURRENT_REVISION = 1;
    /** The revision of layouts saved before revisions got stored. */
    public static final int UNVERSIONED_REVISION = 0;

    private WidgetIdentifierMigration() {
    }

    public static void migrate(@NotNull List<SerializedElement> serializedWidgetElements, int savedRevision) {
        // Report Player shared the Open to LAN button's identifier, because pause menus up to Minecraft 26.1 show it in the LAN button's place outside of unpublished singleplayer worlds.
        if (savedRevision < 1) copyCustomizationsToSplitIdentifier(serializedWidgetElements, "pause_share_to_lan_button", "pause_player_reporting_button");
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
