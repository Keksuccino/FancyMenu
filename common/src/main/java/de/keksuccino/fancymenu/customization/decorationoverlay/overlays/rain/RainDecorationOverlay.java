package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.RainOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class RainDecorationOverlay extends AbstractDecorationOverlay {

    @NotNull
    public String rainColorHex = "#CFE7FF";
    @NotNull
    public String rainIntensity = "1.0";
    @NotNull
    public String rainScale = "1.0";
    @NotNull
    public String rainThunderBrightness = "1.0";
    public boolean rainPuddles = true;
    public boolean rainDrips = true;
    public boolean rainThunder = false;
    protected final RainOverlay overlay = new RainOverlay(0, 0);
    protected String lastRainColorString = null;
    protected String lastRainIntensityString = null;
    protected String lastRainScaleString = null;
    protected String lastRainThunderBrightnessString = null;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Update puddles
        this.overlay.setPuddlesEnabled(this.rainPuddles);
        this.overlay.setDripsEnabled(this.rainDrips);
        this.overlay.setThunderEnabled(this.rainThunder);

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

        String scaleString = PlaceholderParser.replacePlaceholders(this.rainScale);
        if (!Objects.equals(scaleString, this.lastRainScaleString)) {
            this.lastRainScaleString = scaleString;
            float scaleValue;
            if (MathUtils.isFloat(scaleString)) {
                scaleValue = Float.parseFloat(scaleString);
            } else {
                scaleValue = 1.0F;
            }
            this.overlay.setScale(scaleValue);
        }

        String thunderBrightnessString = PlaceholderParser.replacePlaceholders(this.rainThunderBrightness);
        if (!Objects.equals(thunderBrightnessString, this.lastRainThunderBrightnessString)) {
            this.lastRainThunderBrightnessString = thunderBrightnessString;
            float lastThunderBrightness;
            if (MathUtils.isFloat(thunderBrightnessString)) {
                lastThunderBrightness = Float.parseFloat(thunderBrightnessString);
            } else {
                lastThunderBrightness = 1.0F;
            }
            this.overlay.setThunderBrightness(lastThunderBrightness);
        }

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {

        this.overlay.clearCollisionAreas();

        if (isEditor()) return;

        screen.children().forEach(listener -> {
            var c = getAsCollisionBox(listener);
            if (c != null) {
                this.overlay.addCollisionArea(c.x(), c.y(), c.width(), c.height());
            }
        });

        elements.forEach(element -> {
            var c = getAsCollisionBox(element);
            if (c != null) {
                this.overlay.addCollisionArea(c.x(), c.y(), c.width(), c.height());
            }
        });

    }

}
