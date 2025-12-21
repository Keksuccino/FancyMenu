package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.RainOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RainDecorationOverlay extends AbstractDecorationOverlay {

    @NotNull
    public String rainColorHex = "#CFE7FF";
    @NotNull
    public String rainIntensity = "1.0";
    public boolean rainPuddles = true;
    public boolean rainDrips = true;
    protected final RainOverlay overlay = new RainOverlay(0, 0);
    protected String lastRainColorString = null;
    protected String lastRainIntensityString = null;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Update puddles
        this.overlay.setPuddlesEnabled(this.rainPuddles);
        this.overlay.setDripsEnabled(this.rainDrips);

        // Update rain color
        String colorString = PlaceholderParser.replacePlaceholders(this.rainColorHex);
        if (!Objects.equals(colorString, this.lastRainColorString)) {
            this.lastRainColorString = colorString;
            this.overlay.setColor(DrawableColor.of(colorString).getColorInt());
        }

        // Update rain intensity
        String intensityString = PlaceholderParser.replacePlaceholders(this.rainIntensity);
        if (!Objects.equals(intensityString, this.lastRainIntensityString)) {
            this.lastRainIntensityString = intensityString;
            float lastRainIntensity;
            if (MathUtils.isFloat(intensityString)) {
                lastRainIntensity = Float.parseFloat(intensityString);
            } else {
                lastRainIntensity = 1.0f;
            }
            this.overlay.setIntensity(lastRainIntensity);
        }

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen) {

        this.overlay.clearCollisionAreas();

        if (isEditor()) return;

        screen.children().forEach(listener -> {
            if ((listener instanceof Button b) && !(listener instanceof PlainTextButton)) {
                this.overlay.addCollisionArea(b.getX(), b.getY(), b.getWidth(), b.getHeight());
            }
        });

    }

}
