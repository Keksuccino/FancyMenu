package de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.widget.identification.WidgetIdentifierHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VanillaWidgetElementIdentifierTest {

    @Test
    void configuredNumericIdentifierRemainsSerializationPrimaryAfterSemanticBinding() {
        WidgetMeta meta = semanticMeta("button.cosmetica.home", 34691L);
        VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        element.setInstanceIdentifier("34691");

        element.setVanillaWidget(meta, false);
        SerializedElement serialized = VanillaWidgetElementBuilder.INSTANCE.serializeElementInternal(element);

        assertEquals("34691", element.getInstanceIdentifier());
        assertNotNull(serialized);
        assertEquals("34691", serialized.getValue("instance_identifier"));
    }

    @Test
    void configuredPrefixedIdentifierIsPreservedInNormalizedForm() {
        WidgetMeta meta = semanticMeta("button.cosmetica.home", 34691L);
        VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        element.setInstanceIdentifier("vanillabtn:34691");

        element.setVanillaWidget(meta, false);

        assertEquals("34691", element.getInstanceIdentifier());
    }

    @Test
    void generatedDefaultUsesSemanticPrimary() {
        WidgetMeta meta = semanticMeta("button.cosmetica.home", 34691L);
        VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();

        element.setVanillaWidget(meta, false);

        assertEquals("fancymenu_options_button.cosmetica.home", element.getInstanceIdentifier());
    }

    @Test
    void rebindingClearsStaleSerializedIdentifier() {
        WidgetMeta firstMeta = semanticMeta("button.cosmetica.home", 34691L);
        WidgetMeta secondMeta = semanticMeta("options.video", 99123L);
        VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        element.setInstanceIdentifier("34691");
        element.setVanillaWidget(firstMeta, false);

        element.setVanillaWidget(secondMeta, false);

        assertEquals("fancymenu_options_options.video", element.getInstanceIdentifier());
    }

    @Test
    void stackingCarriesConfiguredPrimaryIdentifier() {
        WidgetMeta meta = semanticMeta("button.cosmetica.home", 34691L);
        VanillaWidgetElement configured = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        configured.setInstanceIdentifier("button_compatibility_id:34691");
        configured.setVanillaWidget(meta, false);

        VanillaWidgetElement stacked = VanillaWidgetElementBuilder.INSTANCE.stackElementsInternal(VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance(), configured);

        assertNotNull(stacked);
        assertEquals("34691", stacked.getInstanceIdentifier());
    }

    @Test
    void stackingUsesLastSourcePrimaryDeterministically() {
        WidgetMeta meta = semanticMeta("button.cosmetica.home", 34691L);
        VanillaWidgetElement configured = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        configured.setInstanceIdentifier("34691");
        configured.setVanillaWidget(meta, false);
        VanillaWidgetElement semantic = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
        semantic.setVanillaWidget(meta, false);

        VanillaWidgetElement semanticLast = VanillaWidgetElementBuilder.INSTANCE.stackElementsInternal(VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance(), configured, semantic);
        VanillaWidgetElement configuredLast = VanillaWidgetElementBuilder.INSTANCE.stackElementsInternal(VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance(), semantic, configured);

        assertNotNull(semanticLast);
        assertNotNull(configuredLast);
        assertEquals("fancymenu_options_button.cosmetica.home", semanticLast.getInstanceIdentifier());
        assertEquals("34691", configuredLast.getInstanceIdentifier());
    }

    @SuppressWarnings("DataFlowIssue")
    private static WidgetMeta semanticMeta(String translationKey, long numericIdentifier) {
        TestWidget widget = new TestWidget(Component.translatable(translationKey));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));
        // These tests bind and serialize widget identity only; none of those paths reads the parent screen.
        return new WidgetMeta(widget, numericIdentifier, null);
    }

    private static class TestWidget extends AbstractWidget implements UniqueWidget {

        @Nullable private String identifier;

        private TestWidget(Component message) {
            super(0, 0, 150, 20, message);
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
