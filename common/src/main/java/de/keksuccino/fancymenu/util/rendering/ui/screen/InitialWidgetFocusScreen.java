package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.ObjectHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public interface InitialWidgetFocusScreen {

    final ObjectHolder<Boolean> doInitialWidgetFocusAction = ObjectHolder.of(false);
    final ObjectHolder<Screen> parentScreenOfInitialFocusWidget = ObjectHolder.of(null);
    final ObjectHolder<GuiEventListener> initialFocusWidget = ObjectHolder.of(null);

    default void setupInitialFocusWidget(@NotNull Screen parentScreen, @NotNull GuiEventListener widget) {
        parentScreenOfInitialFocusWidget.set(Objects.requireNonNull(parentScreen));
        initialFocusWidget.set(Objects.requireNonNull(widget));
        doInitialWidgetFocusAction.set(true);
    }

    default void performInitialWidgetFocusActionInRender() {
        if (doInitialWidgetFocusAction.get()) {
            doInitialWidgetFocusAction.set(false);
            Screen s = parentScreenOfInitialFocusWidget.get();
            GuiEventListener l = initialFocusWidget.get();
            if ((s != null) && (l != null)) {
                s.setFocused(l);
            }
        }
    }

}
