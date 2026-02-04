package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import java.awt.Color;

public class DarkUITheme extends UITheme {

    public DarkUITheme() {

        super("dark", "fancymenu.ui.themes.dark");

        allow_blur = true;
        pip_docking_overlay_color = DrawableColor.of(new Color(64, 150, 255, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(64, 150, 255, 200));

    }

}
