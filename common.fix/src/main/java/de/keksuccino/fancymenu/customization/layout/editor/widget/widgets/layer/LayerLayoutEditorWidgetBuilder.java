package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import org.jetbrains.annotations.NotNull;

public class LayerLayoutEditorWidgetBuilder extends AbstractLayoutEditorWidgetBuilder<LayerLayoutEditorWidget> {

    public LayerLayoutEditorWidgetBuilder() {
        super("element_layer_control");
    }

    @Override
    public void applySettings(@NotNull LayoutEditorScreen editor, @NotNull WidgetSettings settings, @NotNull LayerLayoutEditorWidget applyTo) {
    }

    @Override
    public @NotNull LayerLayoutEditorWidget buildDefaultInstance(@NotNull LayoutEditorScreen editor) {
        LayerLayoutEditorWidget w = new LayerLayoutEditorWidget(editor, this);
        w.setBodyWidth(200);
        w.setBodyHeight(300);
        return w;
    }

    @Override
    public void writeSettings(@NotNull WidgetSettings settings, @NotNull LayerLayoutEditorWidget widgetInstance) {
    }

}
