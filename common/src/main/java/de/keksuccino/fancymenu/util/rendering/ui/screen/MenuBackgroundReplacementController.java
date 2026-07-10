package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Coordinates the full-screen menu-background replacement across a screen's legacy rendering paths.
 */
public interface MenuBackgroundReplacementController {

    boolean ensureMenuBackgroundReplacementFancyMenu(@NotNull GuiGraphics graphics);

    boolean isMenuBackgroundReplacementRenderedFancyMenu();

}
