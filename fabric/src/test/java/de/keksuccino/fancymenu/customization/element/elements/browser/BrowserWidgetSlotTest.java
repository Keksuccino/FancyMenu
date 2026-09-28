package de.keksuccino.fancymenu.customization.element.elements.browser;

import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserWidgetSlotTest {

    @Test
    void delayedReadinessReplacesTheReservedSlotAndEnablesInput() {
        AtomicBoolean ready = new AtomicBoolean();
        AtomicInteger creations = new AtomicInteger();
        TrackingWidget browser = new TrackingWidget();
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(ready::get, () -> {
            creations.incrementAndGet();
            return browser;
        });
        TrackingWidget before = new TrackingWidget();
        TrackingWidget after = new TrackingWidget();
        List<GuiEventListener> children = new ArrayList<>(List.of(before, slot.getWidgetToRegister(), after));
        List<GuiEventListener> cleanup = new ArrayList<>(List.of(slot.getWidgetToRegister()));

        assertNull(slot.resolve());
        assertNull(slot.resolve());
        slot.attach(children, cleanup);
        assertEquals(0, creations.get());
        assertFalse(slot.isAttached());
        assertFalse(children.get(1).mouseClicked(new net.minecraft.client.input.MouseButtonEvent(1, 2, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false));
        assertFalse(children.get(1).keyPressed(new net.minecraft.client.input.KeyEvent(65, 0, 0)));

        ready.set(true);
        assertSame(browser, slot.resolve());
        slot.attach(children, cleanup);

        assertEquals(List.of(before, browser, after), children);
        assertEquals(List.of(browser), cleanup);
        assertTrue(slot.isAttached());
        assertTrue(children.get(1).mouseClicked(new net.minecraft.client.input.MouseButtonEvent(1, 2, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false));
        assertTrue(children.get(1).keyPressed(new net.minecraft.client.input.KeyEvent(65, 0, 0)));
        assertEquals(1, browser.clicks);
        assertEquals(1, browser.keys);

        for (int frame = 0; frame < 3; frame++) {
            assertSame(browser, slot.resolve());
            slot.attach(children, cleanup);
        }
        assertEquals(1, creations.get());
        assertEquals(3, children.size());
    }

    @Test
    void immediateReadinessRegistersTheBrowserDirectly() {
        TrackingWidget browser = new TrackingWidget();
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(() -> true, () -> browser);
        assertSame(browser, slot.resolve());
        List<GuiEventListener> children = new ArrayList<>(List.of(slot.getWidgetToRegister()));

        slot.attach(children, null);

        assertEquals(List.of(browser), children);
        assertTrue(slot.isAttached());
    }

    @Test
    void anUnrelatedScreenNeverReceivesTheBrowser() {
        TrackingWidget browser = new TrackingWidget();
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(() -> true, () -> browser);
        List<GuiEventListener> owner = new ArrayList<>(List.of(slot.getWidgetToRegister()));
        List<GuiEventListener> unrelated = new ArrayList<>();
        slot.resolve();

        slot.attach(unrelated, null);
        assertTrue(unrelated.isEmpty());
        assertFalse(slot.isAttached());
        slot.attach(owner, null);
        assertEquals(List.of(browser), owner);
        assertTrue(slot.isAttached());
    }

    @Test
    void absentOrFailedRuntimeNeverCallsTheFactory() {
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(() -> false, () -> {
            throw new AssertionError("Factory must wait for runtime readiness");
        });

        assertNull(slot.resolve());
        assertNull(slot.resolve());
        assertSame(slot, slot.getWidgetToRegister());
        assertFalse(slot.hasFailed());
    }

    @Test
    void nativeCreationFailureIsReportedOnceAndDoesNotRetryEveryFrame() {
        AtomicInteger attempts = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException();
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(() -> true, () -> {
            attempts.incrementAndGet();
            throw failure;
        });

        assertSame(failure, assertThrows(IllegalStateException.class, slot::resolve));
        assertTrue(slot.hasFailed());
        assertNull(slot.resolve());
        assertNull(slot.resolve());
        assertEquals(1, attempts.get());
        assertSame(slot, slot.getWidgetToRegister());
    }

    @Test
    void aNullFactoryResultIsAContainedFailure() {
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(() -> true, () -> null);
        assertThrows(NullPointerException.class, slot::resolve);
        assertTrue(slot.hasFailed());
        assertNull(slot.resolve());
    }

    @Test
    void retiringBeforeReadinessPreventsCreationAndRegistration() {
        AtomicBoolean ready = new AtomicBoolean();
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(ready::get, () -> {
            throw new AssertionError("Retired element must not create a browser");
        });
        List<GuiEventListener> children = new ArrayList<>(List.of(slot.getWidgetToRegister()));
        slot.retire();
        ready.set(true);

        assertNull(slot.resolve());
        slot.attach(children, null);
        assertSame(slot, children.get(0));
        assertFalse(slot.isAttached());
    }

    @Test
    void retiringAfterCreationPreventsLateAttachment() {
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(() -> true, TrackingWidget::new);
        List<GuiEventListener> children = new ArrayList<>(List.of(slot.getWidgetToRegister()));
        assertNotNull(slot.resolve());
        slot.retire();

        assertNull(slot.resolve());
        slot.attach(children, null);
        assertSame(slot, children.get(0));
        assertFalse(slot.isAttached());
    }

    @Test
    void resizedElementsCanReuseTheCachedBrowserWithoutRevivingTheOldSlot() {
        TrackingWidget cachedBrowser = new TrackingWidget();
        BrowserWidgetSlot<TrackingWidget> oldSlot = new BrowserWidgetSlot<>(() -> true, () -> cachedBrowser);
        assertSame(cachedBrowser, oldSlot.resolve());
        oldSlot.retire();
        BrowserWidgetSlot<TrackingWidget> replacement = new BrowserWidgetSlot<>(() -> true, () -> cachedBrowser);
        List<GuiEventListener> children = new ArrayList<>(List.of(replacement.getWidgetToRegister()));

        assertSame(cachedBrowser, replacement.resolve());
        replacement.attach(children, null);

        assertEquals(List.of(cachedBrowser), children);
        assertNull(oldSlot.resolve());
    }

    @Test
    void replacingSeveralPendingBrowsersPreservesTheirOriginalOrder() {
        TrackingWidget first = new TrackingWidget();
        TrackingWidget second = new TrackingWidget();
        BrowserWidgetSlot<TrackingWidget> firstSlot = new BrowserWidgetSlot<>(() -> true, () -> first);
        BrowserWidgetSlot<TrackingWidget> secondSlot = new BrowserWidgetSlot<>(() -> true, () -> second);
        List<GuiEventListener> children = new ArrayList<>(List.of(firstSlot, secondSlot));

        secondSlot.resolve();
        secondSlot.attach(children, null);
        firstSlot.resolve();
        firstSlot.attach(children, null);

        assertEquals(List.of(first, second), children);
    }

    @Test
    void cleanupRemovesTheReplacementOnTheNextScreenInitialization() {
        BrowserWidgetSlot<TrackingWidget> slot = new BrowserWidgetSlot<>(() -> true, TrackingWidget::new);
        List<GuiEventListener> children = new ArrayList<>(List.of(slot.getWidgetToRegister()));
        List<GuiEventListener> cleanup = new ArrayList<>(children);
        slot.resolve();
        slot.attach(children, cleanup);

        children.removeAll(cleanup);

        assertTrue(children.isEmpty());
    }

    private static final class TrackingWidget implements GuiEventListener {

        private int clicks;
        private int keys;

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            this.clicks++;
            return true;
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            this.keys++;
            return true;
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

    }

}
