package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetIdentifierHandlerTest {

    @Test
    void assignsUniqueUntranslatedOptionsLocalizationKey() {
        TestWidget widget = new TestWidget(Component.translatable("button.cosmetica.home"));

        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));

        assertEquals("fancymenu_options_button.cosmetica.home", widget.getWidgetIdentifierFancyMenu());
    }

    @Test
    void preservesExplicitIdentifiersAndRejectsGeneratedCollisions() {
        TestWidget explicit = new TestWidget(Component.translatable("options.video"));
        explicit.setWidgetIdentifierFancyMenu("existing_video_identifier");
        TestWidget colliding = new TestWidget(Component.translatable("options:video"));
        TestWidget occupied = new TestWidget(Component.literal("occupied"));
        occupied.setWidgetIdentifierFancyMenu("fancymenu_options_options_video");

        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(explicit, colliding, occupied));

        assertEquals("existing_video_identifier", explicit.getWidgetIdentifierFancyMenu());
        assertNull(colliding.getWidgetIdentifierFancyMenu());
        assertEquals("fancymenu_options_options_video", occupied.getWidgetIdentifierFancyMenu());
    }

    @Test
    void duplicateAndLiteralLabelsRetainNumericFallback() {
        TestWidget first = new TestWidget(Component.translatable("options.duplicate"));
        TestWidget second = new TestWidget(Component.translatable("options.duplicate"));
        TestWidget literal = new TestWidget(Component.literal("Changing label"));

        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(first, second, literal));

        assertNull(first.getWidgetIdentifierFancyMenu());
        assertNull(second.getWidgetIdentifierFancyMenu());
        assertNull(literal.getWidgetIdentifierFancyMenu());
    }

    @Test
    void repeatedAssignmentRemovesGeneratedIdentifierWhenKeyStopsBeingUnique() {
        TestWidget first = new TestWidget(Component.translatable("options.dynamic"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(first));
        assertEquals("fancymenu_options_options.dynamic", first.getWidgetIdentifierFancyMenu());

        TestWidget second = new TestWidget(Component.translatable("options.dynamic"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(first, second));

        assertNull(first.getWidgetIdentifierFancyMenu());
        assertNull(second.getWidgetIdentifierFancyMenu());
    }

    @Test
    void repeatedAssignmentIsStableAcrossPositionChanges() {
        TestWidget widget = new TestWidget(Component.translatable("options.stable"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));
        String identifier = widget.getWidgetIdentifierFancyMenu();

        widget.setX(900);
        widget.setY(700);
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));

        assertEquals(identifier, widget.getWidgetIdentifierFancyMenu());
    }

    @Test
    void legacyNumericIdentifiersRemainAliasesForSemanticWidget() {
        TestWidget widget = new TestWidget(Component.translatable("button.cosmetica.home"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));
        WidgetMeta meta = createIdentityOnlyMeta(widget, 34691L);

        assertEquals("fancymenu_options_button.cosmetica.home", meta.getIdentifier());
        assertTrue(WidgetIdentifierHandler.isIdentifierOfWidget("34691", meta));
        assertTrue(WidgetIdentifierHandler.isIdentifierOfWidget("vanillabtn:34691", meta));
        assertTrue(WidgetIdentifierHandler.isIdentifierOfWidget("button_compatibility_id:34691", meta));
        assertTrue(WidgetIdentifierHandler.isIdentifierOfWidget("fancymenu_options_button.cosmetica.home", meta));
    }

    @SuppressWarnings("DataFlowIssue")
    private static WidgetMeta createIdentityOnlyMeta(AbstractWidget widget, long numericIdentifier) {
        // Identifier matching never reads the parent screen; null avoids entering Minecraft-dependent constructors.
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
