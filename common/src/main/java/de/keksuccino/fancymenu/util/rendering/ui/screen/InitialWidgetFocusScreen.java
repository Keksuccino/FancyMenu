package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.ObjectHolder;
import net.minecraft.client.gui.components.AbstractWidget;
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
                // In 1.19.2, Screen#setFocused only takes the focus away from the widget that had it before (see MixinAbstractContainerEventHandler)
                // and never gives it to the new one, so the widget gets told here, unless the screen refused the focus because it's not focusable
                if ((s.getFocused() == l) && (l instanceof AbstractWidget widget)) widget.setFocused(true);
            }
        }
    }

}
