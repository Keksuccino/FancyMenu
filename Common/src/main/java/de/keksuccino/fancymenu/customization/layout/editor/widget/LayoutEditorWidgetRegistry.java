package de.keksuccino.fancymenu.customization.layout.editor.widget;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LayoutEditorWidgetRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Class<? extends AbstractLayoutEditorWidget>> WIDGET_CLASSES = new ArrayList<>();

    /**
     * {@link AbstractLayoutEditorWidget}s need a constructor that takes only one parameter of type {@link LayoutEditorScreen}.
     */
    public static void register(@NotNull Class<? extends AbstractLayoutEditorWidget> widgetClass) {
        if (!isWidgetRegistered(widgetClass)) {
            WIDGET_CLASSES.add(widgetClass);
        } else {
            LOGGER.error("[FANCYMENU] Failed to register AbstractLayoutEditorWidget! Already registered!");
        }
    }

    @Nullable
    public static Class<? extends AbstractLayoutEditorWidget> getWidget(@NotNull String classPath) {
        for (Class<? extends AbstractLayoutEditorWidget> c : WIDGET_CLASSES) {
            if (c.getName().equals(classPath)) return c;
        }
        return null;
    }

    @NotNull
    public static List<Class<? extends AbstractLayoutEditorWidget>> getWidgets() {
        return new ArrayList<>(WIDGET_CLASSES);
    }

    public static boolean isWidgetRegistered(@NotNull Class<? extends AbstractLayoutEditorWidget> widgetClass) {
        return WIDGET_CLASSES.contains(widgetClass);
    }

    public static boolean isWidgetRegistered(@NotNull String classPath) {
        return getWidget(classPath) != null;
    }

    @NotNull
    public static List<AbstractLayoutEditorWidget> buildWidgetInstances(@NotNull LayoutEditorScreen editor) {
        List<AbstractLayoutEditorWidget> instances = new ArrayList<>();
        for (Class<? extends AbstractLayoutEditorWidget> c : WIDGET_CLASSES) {
            try {
                instances.add(c.getDeclaredConstructor(LayoutEditorScreen.class).newInstance(Objects.requireNonNull(editor)));
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to create AbstractLayoutEditorWidget instance of " + c.getName() + "!");
                ex.printStackTrace();
            }
        }
        return instances;
    }

}
