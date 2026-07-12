package de.keksuccino.fancymenu.customization.widget;

import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenWidgetDiscovererTest {

    @Test
    void semanticIdentifiersReconcileReorderedRebuiltWidgets() {
        WidgetMeta stableFirst = meta(widget("first"), 101L);
        WidgetMeta stableSecond = meta(widget("second"), 202L);
        WidgetMeta finalSecond = meta(widget("second"), 22L);
        WidgetMeta finalFirst = meta(widget("first"), 11L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stableFirst, stableSecond), List.of(finalSecond, finalFirst), false);

        assertSame(finalSecond.getWidget(), reconciled.get(0).getWidget());
        assertEquals(202L, reconciled.get(0).getLongIdentifier());
        assertSame(finalFirst.getWidget(), reconciled.get(1).getWidget());
        assertEquals(101L, reconciled.get(1).getLongIdentifier());
    }

    @Test
    void countChangesRetainMatchedAndUnmatchedFinalWidgets() {
        WidgetMeta stable = meta(widget("first"), 101L);
        WidgetMeta finalMatched = meta(widget("first"), 11L);
        WidgetMeta finalUnmatched = meta(widget("added"), 22L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stable), List.of(finalMatched, finalUnmatched), false);

        assertEquals(2, reconciled.size());
        assertEquals(101L, reconciled.get(0).getLongIdentifier());
        assertEquals(22L, reconciled.get(1).getLongIdentifier());
    }

    @Test
    void semanticModeDoesNotTransferNumericIdBetweenUnidentifiedWidgets() {
        WidgetMeta stableAnchor = meta(widget("anchor"), 101L);
        WidgetMeta stableLiteral = meta(new TestWidget(Component.literal("old")), 202L);
        WidgetMeta finalLiteral = meta(new TestWidget(Component.literal("new")), 22L);
        WidgetMeta finalAnchor = meta(widget("anchor"), 11L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stableAnchor, stableLiteral), List.of(finalLiteral, finalAnchor), false);

        assertEquals(22L, reconciled.getFirst().getLongIdentifier());
        assertEquals(101L, reconciled.get(1).getLongIdentifier());
    }

    @Test
    void alignedSemanticAnchorsPermitLiteralCompatibilityFallback() {
        WidgetMeta stableAnchor = meta(widget("anchor"), 101L);
        WidgetMeta stableLiteral = meta(new TestWidget(Component.literal("old")), 202L);
        WidgetMeta finalAnchor = meta(widget("anchor"), 11L);
        WidgetMeta finalLiteral = meta(new TestWidget(Component.literal("new")), 22L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stableAnchor, stableLiteral), List.of(finalAnchor, finalLiteral), false);

        assertEquals(101L, reconciled.get(0).getLongIdentifier());
        assertEquals(202L, reconciled.get(1).getLongIdentifier());
    }

    @Test
    void unmatchedNumericCollisionCannotDisplaceReservedStableAlias() {
        WidgetMeta stableAnchor = meta(widget("anchor"), 22L);
        WidgetMeta stableLiteral = meta(new TestWidget(Component.literal("old")), 202L);
        WidgetMeta finalLiteral = meta(new TestWidget(Component.literal("new")), 22L);
        WidgetMeta finalAnchor = meta(widget("anchor"), 11L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stableAnchor, stableLiteral), List.of(finalLiteral, finalAnchor), false);

        assertEquals(221L, reconciled.get(0).getLongIdentifier());
        assertEquals(22L, reconciled.get(1).getLongIdentifier());
    }

    @Test
    void removedStableAliasIsNotReassignedToNewWidget() {
        WidgetMeta removedStable = meta(widget("removed"), 22L);
        WidgetMeta addedFinal = meta(widget("added"), 22L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(removedStable), List.of(addedFinal), false);

        assertEquals(221L, reconciled.getFirst().getLongIdentifier());
    }

    @Test
    void adjacentUnidentifiedRebuildsDoNotExchangeStableAliases() {
        WidgetMeta stableFirstAnchor = meta(widget("first_anchor"), 101L);
        WidgetMeta stableFirstLiteral = meta(new TestWidget(Component.literal("first_old")), 202L);
        WidgetMeta stableSecondLiteral = meta(new TestWidget(Component.literal("second_old")), 303L);
        WidgetMeta stableSecondAnchor = meta(widget("second_anchor"), 404L);
        WidgetMeta finalFirstAnchor = meta(widget("first_anchor"), 11L);
        WidgetMeta finalSecondLiteral = meta(new TestWidget(Component.literal("second_new")), 22L);
        WidgetMeta finalFirstLiteral = meta(new TestWidget(Component.literal("first_new")), 33L);
        WidgetMeta finalSecondAnchor = meta(widget("second_anchor"), 44L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stableFirstAnchor, stableFirstLiteral, stableSecondLiteral, stableSecondAnchor), List.of(finalFirstAnchor, finalSecondLiteral, finalFirstLiteral, finalSecondAnchor), false);

        assertEquals(101L, reconciled.get(0).getLongIdentifier());
        assertEquals(22L, reconciled.get(1).getLongIdentifier());
        assertEquals(33L, reconciled.get(2).getLongIdentifier());
        assertEquals(404L, reconciled.get(3).getLongIdentifier());
    }

    @Test
    void objectIdentitySafelyRetainsStableNumericId() {
        TestWidget widget = new TestWidget(Component.literal("same"));
        WidgetMeta stable = meta(widget, 101L);
        WidgetMeta finalMeta = meta(widget, 11L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stable), List.of(finalMeta), false);

        assertEquals(101L, reconciled.getFirst().getLongIdentifier());
    }

    @Test
    void legacyPositionalFallbackRemainsAvailableForOtherScreens() {
        WidgetMeta stable = meta(new TestWidget(Component.literal("old")), 101L);
        WidgetMeta finalMeta = meta(new TestWidget(Component.literal("new")), 11L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stable), List.of(finalMeta), true);

        assertEquals(101L, reconciled.getFirst().getLongIdentifier());
    }

    @Test
    void legacyPositionalFallbackIgnoresSemanticReorderingForOtherScreens() {
        WidgetMeta stableFirst = meta(widget("first"), 101L);
        WidgetMeta stableSecond = meta(widget("second"), 202L);
        WidgetMeta finalSecond = meta(widget("second"), 22L);
        WidgetMeta finalFirst = meta(widget("first"), 11L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stableFirst, stableSecond), List.of(finalSecond, finalFirst), true);

        assertEquals(101L, reconciled.get(0).getLongIdentifier());
        assertEquals(202L, reconciled.get(1).getLongIdentifier());
    }

    @Test
    void legacyPositionalFallbackRetainsEmptyResultWhenCountsDiffer() {
        WidgetMeta stable = meta(widget("stable"), 101L);
        WidgetMeta finalFirst = meta(widget("first"), 11L);
        WidgetMeta finalSecond = meta(widget("second"), 22L);

        List<WidgetMeta> reconciled = ScreenWidgetDiscoverer.reconcileDiscoveryPasses(List.of(stable), List.of(finalFirst, finalSecond), true);

        assertEquals(List.of(), reconciled);
    }

    private static TestWidget widget(String identifier) {
        TestWidget widget = new TestWidget(Component.literal(identifier));
        widget.setWidgetIdentifierFancyMenu(identifier);
        return widget;
    }

    @SuppressWarnings("DataFlowIssue")
    private static WidgetMeta meta(AbstractWidget widget, long numericIdentifier) {
        // Reconciliation only needs widget identity and identifiers; it never dereferences the parent screen.
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
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

    }

}
