package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.old;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
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

public abstract class AbstractLayoutEditorWidgetBuilderOLD<T extends AbstractLayoutEditorWidgetOLD> {

    public static final File WIDGET_SETTINGS_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/layout_editor/widgets"));
    private static final LayoutEditorScreen DUMMY_LAYOUT_EDITOR = new LayoutEditorScreen(Layout.buildUniversal());

    private final String identifier;

    /**
     * @param identifier Needs to be as unique as possible. Only characters [a-z], [0-9], [-] and [_] are supported. No spaces!
     */
    public AbstractLayoutEditorWidgetBuilderOLD(@NotNull String identifier) {
        if (!CharacterFilter.buildBasicFilenameCharacterFilter().isAllowedText(identifier)) {
            throw new RuntimeException("Invalid characters in identifier! Only characters [a-z], [0-9], [-] and [_] are supported. No spaces!");
        }
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public abstract void applySettings(@NotNull LayoutEditorScreen editor, @NotNull WidgetSettings settings, @NotNull T applyTo);

    @SuppressWarnings("all")
    @Nullable
    public AbstractLayoutEditorWidgetOLD buildWithSettingsInternal(@NotNull LayoutEditorScreen editor) {

        try {

            WidgetSettings settings = this.readSettingsInternal();
            AbstractLayoutEditorWidgetOLD widget = this.buildDefaultInstance(Objects.requireNonNull(editor));
            this.applySettings(editor, Objects.requireNonNull(settings), (T) Objects.requireNonNull(widget));

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
                AbstractLayoutEditorWidgetOLD.SnappingSide s = AbstractLayoutEditorWidgetOLD.SnappingSide.getByName(snappingSide);
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
    public abstract T buildDefaultInstance(@NotNull LayoutEditorScreen editor);

    public abstract void writeSettings(@NotNull WidgetSettings settings, @NotNull T widgetInstance);

    @SuppressWarnings("all")
    public void writeSettingsInternal(@NotNull AbstractLayoutEditorWidgetOLD widgetInstance) {
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
                this.writeSettingsInternal(Objects.requireNonNull(this.buildDefaultInstance(DUMMY_LAYOUT_EDITOR)));
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
