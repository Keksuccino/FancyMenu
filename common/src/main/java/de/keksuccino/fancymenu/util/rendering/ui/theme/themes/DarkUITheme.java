package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import java.awt.Color;

public class DarkUITheme extends UITheme {

    public DarkUITheme() {

        super("dark", "fancymenu.ui.themes.dark");

        allow_blur = true;
        ui_interface_background_color = DrawableColor.of(new Color(33, 33, 33));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(64, 64, 64));
        layout_editor_grid_color_center = DrawableColor.of(new Color(90, 90, 90));
        pip_docking_overlay_color = DrawableColor.of(new Color(64, 150, 255, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(64, 150, 255, 200));

    }

}
