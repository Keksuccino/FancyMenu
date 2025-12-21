package de.keksuccino.fancymenu.customization.screenoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.screenoverlay.AbstractOverlay;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.SnowfallOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class SnowOverlay extends AbstractOverlay {

    @NotNull
    public String snowColorHex = "#FFFFFF";
    @NotNull
    public String snowIntensity = "1.0";
    public boolean snowAccumulation = true;
    protected final SnowfallOverlay overlay = new SnowfallOverlay(0, 0);
    protected String lastSnowColorString = null;
    protected String lastSnowIntensityString = null;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Update snow accumulation
        this.overlay.setAccumulationEnabled(this.snowAccumulation);

        // Update snow color
        String colorString = PlaceholderParser.replacePlaceholders(this.snowColorHex);
        if (!Objects.equals(colorString, this.lastSnowColorString)) {
            this.lastSnowColorString = colorString;
            this.overlay.setColor(DrawableColor.of(colorString).getColorInt());
        }

        // Update snow intensity
        String intensityString = PlaceholderParser.replacePlaceholders(this.snowIntensity);
        if (!Objects.equals(colorString, this.lastSnowIntensityString)) {
            this.lastSnowIntensityString = intensityString;
            float lastSnowIntensity;
            if (MathUtils.isFloat(intensityString)) {
                lastSnowIntensity = Float.parseFloat(intensityString);
            } else {
                lastSnowIntensity = 1.0f;
            }
            this.overlay.setIntensity(lastSnowIntensity);
        }

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

}
