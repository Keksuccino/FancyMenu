package de.keksuccino.fancymenu.customization.layout.editor.widget;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.properties.PropertiesSerializer;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.konkrete.math.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractLayoutEditorWidgetBuilder<T extends AbstractLayoutEditorWidget> {

    public static final File WIDGET_SETTINGS_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/layout_editor/widgets"));
    private static final LayoutEditorScreen DUMMY_LAYOUT_EDITOR = new LayoutEditorScreen(Layout.buildUniversal());

    private final String identifier;

    public AbstractLayoutEditorWidgetBuilder(@NotNull String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public abstract T buildWithSettings(@NotNull LayoutEditorScreen editor, @NotNull WidgetSettings settings);

    @Nullable
    public AbstractLayoutEditorWidget buildWithSettingsInternal(@NotNull LayoutEditorScreen editor) {

        try {

            WidgetSettings settings = this.readSettingsInternal();
            AbstractLayoutEditorWidget widget = Objects.requireNonNull(this.buildWithSettings(Objects.requireNonNull(editor), Objects.requireNonNull(settings)));

            String offsetX = settings.getValue("offset_x");
            if ((offsetX != null) && MathUtils.isInteger(offsetX)) {
                widget.setUnscaledWidgetOffsetX(Integer.parseInt(offsetX), true);
            }

            String offsetY = settings.getValue("offset_y");
            if ((offsetY != null) && MathUtils.isInteger(offsetY)) {
                widget.setUnscaledWidgetOffsetY(Integer.parseInt(offsetY), true);
            }

            String innerWidth = settings.getValue("inner_width");
            if ((innerWidth != null) && MathUtils.isInteger(innerWidth)) {
                widget.setInnerWidth(Integer.parseInt(innerWidth));
            }

            String innerHeight = settings.getValue("inner_height");
            if ((innerHeight != null) && MathUtils.isInteger(innerHeight)) {
                widget.setInnerHeight(Integer.parseInt(innerHeight));
            }

            String snappingSide = settings.getValue("snapping_side");
            if (snappingSide != null) {
                AbstractLayoutEditorWidget.SnappingSide s = AbstractLayoutEditorWidget.SnappingSide.getByName(snappingSide);
                if (s != null) widget.snappingSide = s;
            }

            String expanded = settings.getValue("expanded");
            if (expanded != null) {
                if (expanded.equals("true")) {
                    widget.setExpanded(true);
                } else if (expanded.equals("false")) {
                    widget.setExpanded(false);
                }
            }

            String visible = settings.getValue("visible");
            if (visible != null) {
                if (visible.equals("true")) {
                    widget.setVisible(true);
                } else if (visible.equals("false")) {
                    widget.setVisible(false);
                }
            }

            return widget;

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;

    }

    @NotNull
    public abstract T buildWithoutSettings(@NotNull LayoutEditorScreen editor);

    public abstract void writeSettings(@NotNull WidgetSettings settings, @NotNull T widgetInstance);

    @SuppressWarnings("all")
    public void writeSettingsInternal(@NotNull AbstractLayoutEditorWidget widgetInstance) {
        try {

            WidgetSettings settings = new WidgetSettings();
            settings.putProperty("offset_x", "" + widgetInstance.getUnscaledWidgetOffsetX());
            settings.putProperty("offset_y", "" + widgetInstance.getUnscaledWidgetOffsetY());
            settings.putProperty("inner_width", "" + widgetInstance.getInnerWidth());
            settings.putProperty("inner_height", "" + widgetInstance.getInnerHeight());
            settings.putProperty("snapping_side", "" + widgetInstance.snappingSide.name);
            settings.putProperty("expanded", "" + widgetInstance.isExpanded());
            settings.putProperty("visible", "" + widgetInstance.isVisible());

            this.writeSettings(settings, (T) widgetInstance);

            PropertyContainerSet set = new PropertyContainerSet("layout_editor_widget_settings");
            set.putContainer(settings);

            PropertiesSerializer.serializePropertyContainerSet(set, this.getSettingsFile().getAbsolutePath());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Nullable
    public WidgetSettings readSettingsInternal() {
        try {
            File savedSettingsFile = this.getSettingsFile();
            if (!savedSettingsFile.isFile()) {
                this.writeSettingsInternal(Objects.requireNonNull(this.buildWithoutSettings(DUMMY_LAYOUT_EDITOR)));
            }
            PropertyContainerSet set = PropertiesSerializer.deserializePropertyContainerSet(savedSettingsFile.getAbsolutePath());
            if (set != null) {
                List<PropertyContainer> containers = set.getContainersOfType("settings");
                if (!containers.isEmpty()) {
                    return WidgetSettings.convertContainerToSettings(containers.get(0));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public File getSettingsFile() {
        return new File(WIDGET_SETTINGS_DIR, "/" + this.getIdentifier() + ".lewidget");
    }

    public static class WidgetSettings extends PropertyContainer {

        public WidgetSettings() {
            super("settings");
        }

        @NotNull
        public static WidgetSettings convertContainerToSettings(@NotNull PropertyContainer container) {
            WidgetSettings settings = new WidgetSettings();
            for (Map.Entry<String, String> m : container.getProperties().entrySet()) {
                settings.putProperty(m.getKey(), m.getValue());
            }
            return settings;
        }

    }

}
