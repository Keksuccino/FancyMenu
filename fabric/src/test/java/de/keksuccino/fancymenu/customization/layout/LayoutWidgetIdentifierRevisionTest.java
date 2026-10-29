package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierMigration;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LayoutWidgetIdentifierRevisionTest {

    private static final String LAN_IDENTIFIER = "pause_share_to_lan_button";
    private static final String REPORTING_IDENTIFIER = "pause_player_reporting_button";

    @Test
    void savedLayoutStoresCurrentRevisionAndKeepsLanOnlyCustomizationOffPlayerReporting() {
        Layout layout = new Layout("pause_screen");
        layout.serializedVanillaButtonElements.add(vanillaWidgetEntry(LAN_IDENTIFIER));

        PropertyContainerSet saved = layout.serialize();
        Layout reloaded = Layout.deserialize(saved, null);

        PropertyContainer meta = saved.getFirstContainerOfType("layout-meta");
        assertNotNull(meta);
        assertEquals("" + WidgetIdentifierMigration.CURRENT_REVISION, meta.getValue("widget_identifier_revision"));
        assertNotNull(reloaded);
        assertEquals(List.of(LAN_IDENTIFIER), targetIdentifiers(reloaded));
    }

    @Test
    void unversionedLayoutIsMigratedOnLoadAndNotAgainAfterSaving() {
        PropertyContainerSet unversioned = new PropertyContainerSet("fancymenu_layout");
        PropertyContainer meta = new PropertyContainer("layout-meta");
        meta.putProperty("identifier", "pause_screen");
        unversioned.putContainer(meta);
        unversioned.putContainer(vanillaWidgetEntry(LAN_IDENTIFIER));

        Layout loaded = Layout.deserialize(unversioned, null);
        assertNotNull(loaded);
        assertEquals(List.of(LAN_IDENTIFIER, REPORTING_IDENTIFIER), targetIdentifiers(loaded));

        loaded.serializedVanillaButtonElements.remove(1);
        assertEquals(List.of(LAN_IDENTIFIER), targetIdentifiers(loaded.copy()));
    }

    private static SerializedElement vanillaWidgetEntry(String identifier) {
        SerializedElement serialized = new SerializedElement();
        serialized.setType("vanilla_button");
        serialized.putProperty("element_type", "vanilla_button");
        serialized.putProperty("instance_identifier", identifier);
        serialized.putProperty("is_hidden", "true");
        return serialized;
    }

    private static List<String> targetIdentifiers(Layout layout) {
        return layout.serializedVanillaButtonElements.stream().map(WidgetIdentifierHandler::getTargetWidgetIdentifier).toList();
    }

}
