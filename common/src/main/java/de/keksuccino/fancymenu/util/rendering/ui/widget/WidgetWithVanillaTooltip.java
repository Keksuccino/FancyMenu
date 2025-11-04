package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.rendering.gui.VanillaTooltip;
import org.jetbrains.annotations.Nullable;

public interface WidgetWithVanillaTooltip {

    @Nullable
    public VanillaTooltip getVanillaTooltip_FancyMenu();

    public void setVanillaTooltip_FancyMenu(@Nullable VanillaTooltip tooltip);

}
