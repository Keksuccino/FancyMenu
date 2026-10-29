package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StateIndicatorLabelSupplierTest {

    private static final Component BASE_LABEL = Component.literal("Base Label");

    @Test
    void enabledStateUsesSuccessColorAndKeepsBaseLabel() {
        UITheme theme = createTheme(new Color(10, 200, 30), new Color(220, 40, 50));
        StateIndicatorLabelSupplier supplier = new StateIndicatorLabelSupplier(BASE_LABEL, () -> true, () -> theme);

        List<Component> parts = supplier.get(null, null).getSiblings();

        assertEquals(3, parts.size());
        assertIndicatorColor(theme.success_color, parts.get(0));
        assertSame(BASE_LABEL, parts.get(2));
    }

    @Test
    void disabledStateUsesErrorColorAndKeepsBaseLabel() {
        UITheme theme = createTheme(new Color(10, 200, 30), new Color(220, 40, 50));
        StateIndicatorLabelSupplier supplier = new StateIndicatorLabelSupplier(BASE_LABEL, () -> false, () -> theme);

        List<Component> parts = supplier.get(null, null).getSiblings();

        assertEquals(3, parts.size());
        assertIndicatorColor(theme.error_color, parts.get(0));
        assertSame(BASE_LABEL, parts.get(2));
    }

    @Test
    void indicatorColorDoesNotLeakIntoBaseLabel() {
        UITheme theme = createTheme(new Color(10, 200, 30), new Color(220, 40, 50));
        StateIndicatorLabelSupplier supplier = new StateIndicatorLabelSupplier(BASE_LABEL, () -> true, () -> theme);

        assertEquals(Style.EMPTY, supplier.get(null, null).getStyle());
    }

    @Test
    void enabledAndDisabledStatesUseDifferentIndicators() {
        UITheme theme = createTheme(new Color(10, 200, 30), new Color(220, 40, 50));

        Component enabledIndicator = new StateIndicatorLabelSupplier(BASE_LABEL, () -> true, () -> theme).get(null, null).getSiblings().get(0);
        Component disabledIndicator = new StateIndicatorLabelSupplier(BASE_LABEL, () -> false, () -> theme).get(null, null).getSiblings().get(0);

        assertNotEquals(enabledIndicator.getString(), disabledIndicator.getString());
    }

    @Test
    void returnsSameInstanceWhileStateAndColorStayUnchanged() {
        UITheme theme = createTheme(new Color(10, 200, 30), new Color(220, 40, 50));
        StateIndicatorLabelSupplier supplier = new StateIndicatorLabelSupplier(BASE_LABEL, () -> true, () -> theme);

        Component first = supplier.get(null, null);

        assertSame(first, supplier.get(null, null));
        assertSame(first, supplier.get(null, null));
    }

    @Test
    void followsStateChangesInBothDirections() {
        UITheme theme = createTheme(new Color(10, 200, 30), new Color(220, 40, 50));
        AtomicBoolean state = new AtomicBoolean(false);
        StateIndicatorLabelSupplier supplier = new StateIndicatorLabelSupplier(BASE_LABEL, state::get, () -> theme);

        Component disabled = supplier.get(null, null);
        state.set(true);
        Component enabled = supplier.get(null, null);
        state.set(false);
        Component disabledAgain = supplier.get(null, null);

        assertNotSame(disabled, enabled);
        assertIndicatorColor(theme.error_color, disabled.getSiblings().get(0));
        assertIndicatorColor(theme.success_color, enabled.getSiblings().get(0));
        assertIndicatorColor(theme.error_color, disabledAgain.getSiblings().get(0));
        assertEquals(disabled.getString(), disabledAgain.getString());
    }

    @Test
    void rebuildsWhenActiveThemeChangesIndicatorColor() {
        UITheme firstTheme = createTheme(new Color(10, 200, 30), new Color(220, 40, 50));
        UITheme secondTheme = createTheme(new Color(40, 120, 250), new Color(250, 150, 20));
        AtomicReference<UITheme> activeTheme = new AtomicReference<>(firstTheme);
        StateIndicatorLabelSupplier supplier = new StateIndicatorLabelSupplier(BASE_LABEL, () -> true, activeTheme::get);

        Component firstLabel = supplier.get(null, null);
        activeTheme.set(secondTheme);
        Component secondLabel = supplier.get(null, null);

        assertNotSame(firstLabel, secondLabel);
        assertIndicatorColor(secondTheme.success_color, secondLabel.getSiblings().get(0));
        assertSame(secondLabel, supplier.get(null, null));
    }

    @Test
    void rebuildsWhenColorOfActiveThemeGetsReplaced() {
        UITheme theme = createTheme(new Color(10, 200, 30), new Color(220, 40, 50));
        StateIndicatorLabelSupplier supplier = new StateIndicatorLabelSupplier(BASE_LABEL, () -> false, () -> theme);

        Component firstLabel = supplier.get(null, null);
        theme.error_color = DrawableColor.of(new Color(120, 0, 200));
        Component secondLabel = supplier.get(null, null);

        assertNotSame(firstLabel, secondLabel);
        assertIndicatorColor(theme.error_color, secondLabel.getSiblings().get(0));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new StateIndicatorLabelSupplier(null, () -> true));
        assertThrows(NullPointerException.class, () -> new StateIndicatorLabelSupplier(BASE_LABEL, null));
        assertThrows(NullPointerException.class, () -> new StateIndicatorLabelSupplier(BASE_LABEL, () -> true, null));
    }

    private static UITheme createTheme(Color successColor, Color errorColor) {
        UITheme theme = new UITheme("test_theme", "Test Theme");
        theme.success_color = DrawableColor.of(successColor);
        theme.error_color = DrawableColor.of(errorColor);
        return theme;
    }

    private static void assertIndicatorColor(DrawableColor expected, Component indicator) {
        TextColor color = indicator.getStyle().getColor();
        assertNotNull(color);
        assertEquals(TextColor.fromRgb(expected.getColorInt()).getValue(), color.getValue());
    }

}
