package de.keksuccino.fancymenu.customization.layout.editor.widget.widgets;

import de.keksuccino.fancymenu.customization.layout.editor.widget.LayoutEditorWidgetRegistry;
import de.keksuccino.fancymenu.customization.layout.editor.widget.widgets.layer.LayerLayoutEditorWidgetBuilder;

public class LayoutEditorWidgets {

    public static final LayerLayoutEditorWidgetBuilder LAYER_WIDGET_BUILDER = new LayerLayoutEditorWidgetBuilder();

    public static void registerAll() {

        LayoutEditorWidgetRegistry.register(LAYER_WIDGET_BUILDER);

    }

}
