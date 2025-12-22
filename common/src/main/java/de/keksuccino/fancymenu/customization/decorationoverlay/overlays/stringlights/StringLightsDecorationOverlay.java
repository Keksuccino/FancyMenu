package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.StringLightsOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StringLightsDecorationOverlay extends AbstractDecorationOverlay {

    @NotNull
    public String stringLightsColorHex = "#FFD27A";
    @NotNull
    public String stringLightsScale = "1.0";
    @NotNull
    public String stringLightsWindStrength = "1.0";
    @NotNull
    public String stringLightsFlickerSpeed = "1.0";
    public boolean stringLightsChristmasMode = false;
    public boolean stringLightsLeftCenterToTopCenter = true;
    public boolean stringLightsRightCenterToTopCenter = true;
    public boolean stringLightsBottomLeftToTopCenter = false;
    public boolean stringLightsTopLeftToTopRight = true;
    public boolean stringLightsBottomLeftToBottomRight = false;
    public boolean stringLightsLooseLeftTop = false;
    public boolean stringLightsLooseRightTop = false;
    protected final StringLightsOverlay overlay = new StringLightsOverlay(0, 0);
    protected String lastColorString = null;
    protected String lastScaleString = null;
    protected String lastWindStrengthString = null;
    protected String lastFlickerSpeedString = null;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        String colorString = PlaceholderParser.replacePlaceholders(this.stringLightsColorHex);
        if (!Objects.equals(colorString, this.lastColorString)) {
            this.lastColorString = colorString;
            this.overlay.setColor(DrawableColor.of(colorString).getColorInt());
        }

        String scaleString = PlaceholderParser.replacePlaceholders(this.stringLightsScale);
        if (!Objects.equals(scaleString, this.lastScaleString)) {
            this.lastScaleString = scaleString;
            float scaleValue;
            if (MathUtils.isFloat(scaleString)) {
                scaleValue = Float.parseFloat(scaleString);
            } else {
                scaleValue = 1.0F;
            }
            this.overlay.setScale(scaleValue);
        }

        String windStrengthString = PlaceholderParser.replacePlaceholders(this.stringLightsWindStrength);
        if (!Objects.equals(windStrengthString, this.lastWindStrengthString)) {
            this.lastWindStrengthString = windStrengthString;
            float windValue;
            if (MathUtils.isFloat(windStrengthString)) {
                windValue = Float.parseFloat(windStrengthString);
            } else {
                windValue = 1.0F;
            }
            this.overlay.setWindStrength(windValue);
        }

        String flickerSpeedString = PlaceholderParser.replacePlaceholders(this.stringLightsFlickerSpeed);
        if (!Objects.equals(flickerSpeedString, this.lastFlickerSpeedString)) {
            this.lastFlickerSpeedString = flickerSpeedString;
            float speedValue;
            if (MathUtils.isFloat(flickerSpeedString)) {
                speedValue = Float.parseFloat(flickerSpeedString);
            } else {
                speedValue = 1.0F;
            }
            this.overlay.setFlickerSpeed(speedValue);
        }

        this.overlay.setChristmasMode(this.stringLightsChristmasMode);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LEFT_CENTER_TO_TOP_CENTER, this.stringLightsLeftCenterToTopCenter);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.RIGHT_CENTER_TO_TOP_CENTER, this.stringLightsRightCenterToTopCenter);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_TOP_CENTER, this.stringLightsBottomLeftToTopCenter);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.TOP_LEFT_TO_TOP_RIGHT, this.stringLightsTopLeftToTopRight);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.BOTTOM_LEFT_TO_BOTTOM_RIGHT, this.stringLightsBottomLeftToBottomRight);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LOOSE_LEFT_TOP, this.stringLightsLooseLeftTop);
        this.overlay.setPositionEnabled(StringLightsOverlay.StringLightsPosition.LOOSE_RIGHT_TOP, this.stringLightsLooseRightTop);

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }
}
