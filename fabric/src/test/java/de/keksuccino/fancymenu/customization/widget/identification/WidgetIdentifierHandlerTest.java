package de.keksuccino.fancymenu.customization.widget.identification;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetIdentifierHandlerTest {

    private static final String CONTEXT_IDENTIFIER = "fancymenu_options_context_button";

    @Test
    void contextualOptionsVariantsShareAnIdentifierWithoutChangingTheirMessages() {
        List<Component> messages = List.of(Component.translatable("options.online"), Component.translatable("options.worldOptions.button"), difficultyMessage(1), difficultyMessage(2));
        for (Component message : messages) {
            TestWidget widget = new TestWidget(message);
            TestWidget fov = new TestWidget(CommonComponents.optionNameValue(Component.translatable("options.fov"), Component.literal("70")));

            WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(fov, widget));

            assertEquals(CONTEXT_IDENTIFIER, widget.getWidgetIdentifierFancyMenu());
            assertNotEquals(CONTEXT_IDENTIFIER, fov.getWidgetIdentifierFancyMenu());
            assertSame(message, widget.getMessage());
        }
    }

    @Test
    void contextualOptionsVariantsAcceptLegacySemanticAndNumericIdentifiers() {
        for (Component message : List.of(Component.translatable("options.online"), Component.translatable("options.worldOptions.button"), difficultyMessage(1))) {
            TestWidget widget = new TestWidget(message);
            WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));
            WidgetMeta meta = createIdentityOnlyMeta(widget, 505166L);

            for (String identifier : List.of(CONTEXT_IDENTIFIER, "fancymenu_options_options.online", "fancymenu_options_options.worldoptions.button", "505166")) {
                for (String prefix : List.of("", "vanillabtn:", "button_compatibility_id:")) {
                    assertTrue(WidgetIdentifierHandler.isIdentifierOfWidget(prefix + identifier, meta));
                }
            }
            assertFalse(WidgetIdentifierHandler.isIdentifierOfWidget("fancymenu_options_options.generic_value", meta));
        }
    }

    @Test
    void coexistingOptionsVariantsKeepDistinctIdentifiers() {
        TestWidget online = new TestWidget(Component.translatable("options.online"));
        TestWidget world = new TestWidget(Component.translatable("options.worldOptions.button"));

        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(online, world));

        assertEquals("fancymenu_options_options.online", online.getWidgetIdentifierFancyMenu());
        assertEquals("fancymenu_options_options.worldoptions.button", world.getWidgetIdentifierFancyMenu());
        assertFalse(WidgetIdentifierHandler.isIdentifierOfWidget(world.getWidgetIdentifierFancyMenu(), createIdentityOnlyMeta(online, 1L)));
        assertFalse(WidgetIdentifierHandler.isIdentifierOfWidget(online.getWidgetIdentifierFancyMenu(), createIdentityOnlyMeta(world, 2L)));
    }

    @Test
    void explicitIdentifiersCannotBeClaimedByContextualAliases() {
        for (String occupiedIdentifier : List.of(CONTEXT_IDENTIFIER, "fancymenu_options_options.online", "fancymenu_options_options.worldoptions.button")) {
            TestWidget world = new TestWidget(Component.translatable("options.worldOptions.button"));
            TestWidget explicit = new TestWidget(Component.empty());
            explicit.setWidgetIdentifierFancyMenu(occupiedIdentifier);

            WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(world, explicit));

            assertNotEquals(CONTEXT_IDENTIFIER, world.getWidgetIdentifierFancyMenu());
            assertEquals(occupiedIdentifier, explicit.getWidgetIdentifierFancyMenu());
            assertFalse(WidgetIdentifierHandler.isIdentifierOfWidget("fancymenu_options_options.online", createIdentityOnlyMeta(world, 1L)));
        }
    }

    @Test
    void generatedIdentifierCollisionsCannotBeClaimedByContextualAliases() {
        for (String key : List.of("context_button", "options.worldoptions.button", "OPTIONS.ONLINE")) {
            TestWidget online = new TestWidget(Component.translatable("options.online"));
            TestWidget other = new TestWidget(Component.translatable(key));

            WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(online, other));

            assertNotEquals(CONTEXT_IDENTIFIER, online.getWidgetIdentifierFancyMenu());
            assertFalse(WidgetIdentifierHandler.isIdentifierOfWidget("fancymenu_options_options.worldoptions.button", createIdentityOnlyMeta(online, 1L)));
        }
    }

    @Test
    void contextualIdentifierSurvivesReassignmentAndRetiresWhenWidgetChangesRole() {
        TestWidget widget = new TestWidget(Component.translatable("options.online"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));
        widget.setMessage(Component.translatable("options.worldOptions.button"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));
        assertEquals(CONTEXT_IDENTIFIER, widget.getWidgetIdentifierFancyMenu());

        widget.setMessage(Component.translatable("options.video"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));

        assertEquals("fancymenu_options_options.video", widget.getWidgetIdentifierFancyMenu());
        assertFalse(WidgetIdentifierHandler.isIdentifierOfWidget("fancymenu_options_options.online", createIdentityOnlyMeta(widget, 1L)));
    }

    @Test
    void explicitReplacementAndUnownedContextIdentifiersDoNotAcquireAliases() {
        TestWidget widget = new TestWidget(Component.translatable("options.worldOptions.button"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(widget));
        widget.setWidgetIdentifierFancyMenu("explicit_world_button");
        assertFalse(WidgetIdentifierHandler.isIdentifierOfWidget("fancymenu_options_options.online", createIdentityOnlyMeta(widget, 1L)));

        TestWidget unrelated = new TestWidget(Component.empty());
        unrelated.setWidgetIdentifierFancyMenu(CONTEXT_IDENTIFIER);
        assertFalse(WidgetIdentifierHandler.isIdentifierOfWidget("fancymenu_options_options.online", createIdentityOnlyMeta(unrelated, 2L)));
    }

    @Test
    void introducingAnotherVariantRevokesTheSharedIdentifier() {
        TestWidget online = new TestWidget(Component.translatable("options.online"));
        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(online));
        assertEquals(CONTEXT_IDENTIFIER, online.getWidgetIdentifierFancyMenu());
        TestWidget world = new TestWidget(Component.translatable("options.worldOptions.button"));

        WidgetIdentifierHandler.assignStableOptionsWidgetIdentifiers(List.of(world, online));

        assertEquals("fancymenu_options_options.online", online.getWidgetIdentifierFancyMenu());
        assertEquals("fancymenu_options_options.worldoptions.button", world.getWidgetIdentifierFancyMenu());
    }

    private static Component difficultyMessage(int value) {
        return CommonComponents.optionNameValue(Component.translatable("options.difficulty"), Component.literal(Integer.toString(value)));
    }

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

        widget.x = 900;
        widget.y = 700;
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
        public void updateNarration(NarrationElementOutput output) {
        }

    }

}
