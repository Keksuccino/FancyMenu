package de.keksuccino.fancymenu.util.rendering.gui;

import org.joml.Vector2ic;

public interface ClientTooltipPositioner {
    Vector2ic positionTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY, int tooltipWidth, int tooltipHeight);
}
