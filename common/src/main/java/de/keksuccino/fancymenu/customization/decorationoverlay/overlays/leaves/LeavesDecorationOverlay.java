package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.LeavesOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class LeavesDecorationOverlay extends AbstractDecorationOverlay {

    @NotNull
    public String leavesColorStartHex = "#7BA84F";
    @NotNull
    public String leavesColorEndHex = "#D58A3B";
    @NotNull
    public String leavesDensity = "1.0";
    @NotNull
    public String leavesWindIntensity = "1.0";
    public boolean leavesWindBlows = true;
    @NotNull
    public String leavesFallSpeed = "1.0";
    @NotNull
    public String leavesScale = "1.0";
    protected final LeavesOverlay overlay = new LeavesOverlay(0, 0);
    protected String lastLeavesColorStartString = null;
    protected String lastLeavesColorEndString = null;
    protected String lastLeavesDensityString = null;
    protected String lastLeavesWindIntensityString = null;
    protected String lastLeavesFallSpeedString = null;
    protected String lastLeavesScaleString = null;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        String startColorString = PlaceholderParser.replacePlaceholders(this.leavesColorStartHex);
        String endColorString = PlaceholderParser.replacePlaceholders(this.leavesColorEndHex);
        if (!Objects.equals(startColorString, this.lastLeavesColorStartString) || !Objects.equals(endColorString, this.lastLeavesColorEndString)) {
            this.lastLeavesColorStartString = startColorString;
            this.lastLeavesColorEndString = endColorString;
            this.overlay.setColorRange(DrawableColor.of(startColorString).getColorInt(), DrawableColor.of(endColorString).getColorInt());
        }

        String densityString = PlaceholderParser.replacePlaceholders(this.leavesDensity);
        if (!Objects.equals(densityString, this.lastLeavesDensityString)) {
            this.lastLeavesDensityString = densityString;
            float densityValue;
            if (MathUtils.isFloat(densityString)) {
                densityValue = Float.parseFloat(densityString);
            } else {
                densityValue = 1.0F;
            }
            this.overlay.setIntensity(densityValue);
        }

        String windString = PlaceholderParser.replacePlaceholders(this.leavesWindIntensity);
        if (!Objects.equals(windString, this.lastLeavesWindIntensityString)) {
            this.lastLeavesWindIntensityString = windString;
            float windValue;
            if (MathUtils.isFloat(windString)) {
                windValue = Float.parseFloat(windString);
            } else {
                windValue = 1.0F;
            }
            this.overlay.setWindStrength(windValue);
        }

        this.overlay.setWindBlowsEnabled(this.leavesWindBlows);

        String fallSpeedString = PlaceholderParser.replacePlaceholders(this.leavesFallSpeed);
        if (!Objects.equals(fallSpeedString, this.lastLeavesFallSpeedString)) {
            this.lastLeavesFallSpeedString = fallSpeedString;
            float fallSpeedValue;
            if (MathUtils.isFloat(fallSpeedString)) {
                fallSpeedValue = Float.parseFloat(fallSpeedString);
            } else {
                fallSpeedValue = 1.0F;
            }
            this.overlay.setFallSpeedMultiplier(fallSpeedValue);
        }

        String scaleString = PlaceholderParser.replacePlaceholders(this.leavesScale);
        if (!Objects.equals(scaleString, this.lastLeavesScaleString)) {
            this.lastLeavesScaleString = scaleString;
            float scaleValue;
            if (MathUtils.isFloat(scaleString)) {
                scaleValue = Float.parseFloat(scaleString);
            } else {
                scaleValue = 1.0F;
            }
            this.overlay.setScale(scaleValue);
        }

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {

        this.overlay.clearCollisionAreas();

        visitCollisionBoxes(screen, elements, c -> this.overlay.addCollisionArea(c.x(), c.y(), c.width(), c.height()));

    }

}