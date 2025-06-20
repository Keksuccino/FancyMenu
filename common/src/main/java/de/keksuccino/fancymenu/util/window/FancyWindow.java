package de.keksuccino.fancymenu.util.window;

import com.mojang.blaze3d.platform.Window;

/**
 * Gets applied to {@link Window} via Mixin to allow for the good old precise GUI scaling like before MC 1.21.6.
 */
public interface FancyWindow {

    /**
     * Returns the current precise scale or the Vanilla scale if no precise scale is set.
     * <p>
     * Only gets updated when setting scales via {@link WindowHandler#setGuiScale(double)}.<br>
     * Gets reset when calling {@link Window#setGuiScale(int)}.
     */
    public double getPreciseGuiScale_FancyMenu();

    /**
     * This should NEVER get called manually! Use {@link WindowHandler#setGuiScale(double)} instead!
     */
    public void setPreciseGuiScale_FancyMenu(double scale);

}
