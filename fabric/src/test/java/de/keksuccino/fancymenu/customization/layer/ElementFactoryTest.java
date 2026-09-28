package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElementBuilder;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementFactoryTest {

    private static final String CONTEXT_IDENTIFIER = "fancymenu_options_context_button";
    private static final String ONLINE_IDENTIFIER = "fancymenu_options_options.online";
    private static final String WORLD_IDENTIFIER = "fancymenu_options_options.worldoptions.button";
    private static final List<String> CONTEXT_KEYS = List.of("options.online", "options.worldOptions.button", "options.difficulty");
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

    @ParameterizedTest
    @ValueSource(strings = {"options.online", "options.worldOptions.button", "options.difficulty"})
    void customizedContextWidgetSurvivesEveryTransitionAndSaveReload(String initialKey) {
        WidgetMeta initial = meta(initialKey);
        VanillaWidgetElement element = customized(initial.getIdentifier(), 37);
        element.setVanillaWidget(initial, false);
        Layout layout = layout(serialize(element));

        for (String key : CONTEXT_KEYS) {
            WidgetMeta current = meta(key);
            VanillaWidgetElement bound = resolve(List.of(layout), current);
            assertSame(current.getWidget(), bound.getWidget());
            assertSame(ElementAnchorPoints.TOP_LEFT, bound.anchorPoint);
            assertEquals(37, bound.posOffsetX);
            assertEquals(47, bound.posOffsetY);
            assertEquals(123, bound.baseWidth);
            assertEquals(CONTEXT_IDENTIFIER, bound.getInstanceIdentifier());
            layout = layout(serialize(bound));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {ONLINE_IDENTIFIER, WORLD_IDENTIFIER, "505166", "vanillabtn:fancymenu_options_options.online", "button_compatibility_id:fancymenu_options_options.worldoptions.button"})
    void legacyIdentifiersKeepCustomizationsAcrossContextsAndResaving(String identifier) {
        Layout layout = layout(serialize(customized(identifier, 29)));

        for (String key : CONTEXT_KEYS) {
            VanillaWidgetElement bound = resolve(List.of(layout), meta(key));
            assertSame(ElementAnchorPoints.TOP_LEFT, bound.anchorPoint);
            assertEquals(29, bound.posOffsetX);
            assertEquals(identifier.replace("vanillabtn:", "").replace("button_compatibility_id:", ""), bound.getInstanceIdentifier());
            layout = layout(serialize(bound));
        }
    }

    @Test
    void currentVariantWinsOverConflictingLegacyAliasInEitherOrder() {
        for (String key : List.of("options.online", "options.worldOptions.button")) {
            String exact = key.equals("options.online") ? ONLINE_IDENTIFIER : WORLD_IDENTIFIER;
            String alias = key.equals("options.online") ? WORLD_IDENTIFIER : ONLINE_IDENTIFIER;
            VanillaWidgetElement otherVariant = customized(alias, 11);
            otherVariant.setHidden(true);
            SerializedElement exactEntry = serialize(customized(exact, 83));
            SerializedElement aliasEntry = serialize(otherVariant);

            for (Layout layout : List.of(layout(exactEntry, aliasEntry), layout(aliasEntry, exactEntry))) {
                VanillaWidgetElement bound = resolve(List.of(layout), meta(key));
                assertEquals(83, bound.posOffsetX);
                assertFalse(bound.isHidden());
                assertEquals(exact, bound.getInstanceIdentifier());
            }
        }
    }

    @Test
    void originalCaptionDeterminesLegacyPreferenceAfterMessageCustomization() {
        WidgetMeta current = meta("options.worldOptions.button");
        current.getWidget().setMessage(Component.empty());
        Layout layout = layout(serialize(customized(WORLD_IDENTIFIER, 83)), serialize(customized(ONLINE_IDENTIFIER, 11)));

        VanillaWidgetElement bound = resolve(List.of(layout), current);

        assertEquals(83, bound.posOffsetX);
    }

    @Test
    void sharedIdentifierWinsOverLegacyVariantInEitherOrder() {
        SerializedElement shared = serialize(customized(CONTEXT_IDENTIFIER, 61));
        VanillaWidgetElement legacyElement = customized(WORLD_IDENTIFIER, 15);
        legacyElement.setHidden(true);
        SerializedElement legacy = serialize(legacyElement);

        for (Layout layout : List.of(layout(shared, legacy), layout(legacy, shared))) {
            VanillaWidgetElement bound = resolve(List.of(layout), meta("options.worldOptions.button"));
            assertEquals(61, bound.posOffsetX);
            assertFalse(bound.isHidden());
        }
    }

    @Test
    void aliasPreferenceDoesNotOverrideLayoutStackingOrder() {
        Layout lower = layout(serialize(customized(WORLD_IDENTIFIER, 13)));
        Layout upper = layout(serialize(customized(ONLINE_IDENTIFIER, 79)));

        VanillaWidgetElement bound = resolve(List.of(lower, upper), meta("options.worldOptions.button"));

        assertEquals(79, bound.posOffsetX);
    }

    @Test
    void unrelatedWidgetEntriesStillStackNormally() {
        String identifier = "fancymenu_options_options.video";
        VanillaWidgetElement hidden = customized(identifier, 17);
        hidden.setHidden(true);
        Layout layout = layout(serialize(hidden), serialize(customized(identifier, 71)));

        VanillaWidgetElement bound = resolve(List.of(layout), meta("options.video"));

        assertEquals(71, bound.posOffsetX);
        assertTrue(bound.isHidden());
    }

    private VanillaWidgetElement resolve(List<Layout> layouts, WidgetMeta meta) {
        List<VanillaWidgetElement> widgets = new ArrayList<>();
        this.factory.constructElementInstances("options_screen", List.of(meta), layouts, new Layout.OrderedElementCollection(), widgets);
        assertEquals(1, widgets.size());
        return widgets.get(0);
    }

    private static Layout layout(SerializedElement... elements) {
        return new Layout() {
            @Override
            public List<VanillaWidgetElement> buildVanillaButtonElementInstances() {
                List<VanillaWidgetElement> deserialized = new ArrayList<>();
                for (SerializedElement element : elements) {
                    VanillaWidgetElement instance = HEADLESS_BUILDER.deserializeElementInternal(element);
                    assertNotNull(instance);
                    deserialized.add(instance);
                }
                return deserialized;
            }
        };
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
    private static WidgetMeta meta(String key) {
        Component message = key.equals("options.difficulty") ? CommonComponents.optionNameValue(Component.translatable(key), Component.literal("1")) : Component.translatable(key);
        TestWidget widget = new TestWidget(message);
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));
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
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

    }

}
