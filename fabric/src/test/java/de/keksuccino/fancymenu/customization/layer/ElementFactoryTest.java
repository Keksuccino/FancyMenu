package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElementBuilder;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementFactoryTest {

    private static final String LAN_IDENTIFIER = "pause_share_to_lan_button";
    private static final String REPORTING_IDENTIFIER = "pause_player_reporting_button";
    // Keep the real serializer/deserializer and factory; only suppress the post-construction lookup of Minecraft's active screen.
    private static final VanillaWidgetElementBuilder HEADLESS_BUILDER = new VanillaWidgetElementBuilder() {
        @Override
        public VanillaWidgetElement buildDefaultInstance() {
            return new VanillaWidgetElement(this) {
                @Override
                public void afterConstruction() {
                }
            };
        }
    };
    private final ElementFactory factory = new ElementFactory() {};

    @Test
    void pauseLanAndPlayerReportingCustomizationsStayIndependentAcrossContextsAndEditorSaves() {
        WidgetMeta lan = pauseMeta("menu.shareToLan", LAN_IDENTIFIER);
        WidgetMeta reporting = pauseMeta("menu.playerReporting", REPORTING_IDENTIFIER);
        Layout layout = layout(serialize(customized(LAN_IDENTIFIER, 37)));

        VanillaWidgetElement boundReporting = resolve(List.of(layout), reporting);
        assertSame(ElementAnchorPoints.VANILLA, boundReporting.anchorPoint);
        assertEquals(REPORTING_IDENTIFIER, boundReporting.getInstanceIdentifier());
        boundReporting.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        boundReporting.posOffsetX = 11;
        layout = saveLikeEditor(layout, boundReporting, reporting);

        VanillaWidgetElement boundLan = resolve(List.of(layout), lan);
        assertEquals(37, boundLan.posOffsetX);
        assertEquals(LAN_IDENTIFIER, boundLan.getInstanceIdentifier());
        boundLan.posOffsetX = 41;
        layout = saveLikeEditor(layout, boundLan, lan);

        assertEquals(11, resolve(List.of(layout), reporting).posOffsetX);
        assertEquals(41, resolve(List.of(layout), lan).posOffsetX);
        assertEquals(2, layout.serializedVanillaButtonElements.size());
    }

    @Test
    void unversionedPauseLayoutKeepsSharedCustomizationOnBothButtons() {
        VanillaWidgetElement hidden = customized(LAN_IDENTIFIER, 23);
        hidden.setHidden(true);
        Layout loaded = Layout.deserialize(unversionedLayoutWith(serialize(hidden)), null);
        assertNotNull(loaded);
        Layout layout = layout(loaded.serializedVanillaButtonElements.toArray(new SerializedElement[0]));

        for (WidgetMeta meta : List.of(pauseMeta("menu.shareToLan", LAN_IDENTIFIER), pauseMeta("menu.playerReporting", REPORTING_IDENTIFIER))) {
            VanillaWidgetElement bound = resolve(List.of(layout), meta);
            assertEquals(23, bound.posOffsetX);
            assertTrue(bound.isHidden());
            assertEquals(meta.getIdentifier(), bound.getInstanceIdentifier());
        }
    }

    // Serializes like LayoutEditorScreen#serializeElementInstancesToLayoutInstance for a screen instance that only contains the given widget.
    private static Layout saveLikeEditor(Layout layout, VanillaWidgetElement bound, WidgetMeta current) {
        List<SerializedElement> entries = new ArrayList<>();
        entries.add(serialize(bound));
        entries.addAll(WidgetIdentifierHandler.getCustomizationsOfAbsentWidgets(layout.serializedVanillaButtonElements, List.of(current)));
        return layout(entries.toArray(new SerializedElement[0]));
    }

    private static PropertyContainerSet unversionedLayoutWith(SerializedElement vanillaWidgetEntry) {
        PropertyContainerSet set = new PropertyContainerSet("fancymenu_layout");
        PropertyContainer meta = new PropertyContainer("layout-meta");
        meta.putProperty("identifier", "pause_screen");
        set.putContainer(meta);
        set.putContainer(vanillaWidgetEntry);
        return set;
    }

    private VanillaWidgetElement resolve(List<Layout> layouts, WidgetMeta meta) {
        List<VanillaWidgetElement> widgets = new ArrayList<>();
        this.factory.constructElementInstances("pause_screen", List.of(meta), layouts, new Layout.OrderedElementCollection(), widgets);
        assertEquals(1, widgets.size());
        return widgets.get(0);
    }

    private static Layout layout(SerializedElement... elements) {
        Layout layout = new Layout() {
            @Override
            public List<VanillaWidgetElement> buildVanillaButtonElementInstances() {
                List<VanillaWidgetElement> deserialized = new ArrayList<>();
                for (SerializedElement element : this.serializedVanillaButtonElements) {
                    VanillaWidgetElement instance = HEADLESS_BUILDER.deserializeElementInternal(element);
                    assertNotNull(instance);
                    deserialized.add(instance);
                }
                return deserialized;
            }
        };
        layout.serializedVanillaButtonElements.addAll(List.of(elements));
        return layout;
    }

    private static VanillaWidgetElement customized(String identifier, int x) {
        VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        element.setInstanceIdentifier(identifier);
        element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        element.posOffsetX = x;
        element.posOffsetY = 47;
        element.baseWidth = 123;
        element.baseHeight = 20;
        return element;
    }

    private static SerializedElement serialize(VanillaWidgetElement element) {
        SerializedElement serialized = VanillaWidgetElementBuilder.INSTANCE.serializeElementInternal(element);
        assertNotNull(serialized);
        return serialized;
    }

    @SuppressWarnings("DataFlowIssue")
    private static WidgetMeta pauseMeta(String key, String identifier) {
        TestWidget widget = new TestWidget(Component.translatable(key));
        // Mirrors MixinPauseScreen, which assigns these identifiers by translation key.
        widget.setWidgetIdentifierFancyMenu(identifier);
        // Element construction and serialization do not need a live Minecraft screen.
        return new WidgetMeta(widget, 505166L, null);
    }

    private static class TestWidget extends AbstractWidget implements UniqueWidget {

        @Nullable private String identifier;

        private TestWidget(Component message) {
            super(504, 30, 150, 20, message);
        }

        @Override
        public AbstractWidget setWidgetIdentifierFancyMenu(@Nullable String identifier) {
            this.identifier = identifier;
            return this;
        }

        @Override
        public String getWidgetIdentifierFancyMenu() {
            return this.identifier;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

    }

}
