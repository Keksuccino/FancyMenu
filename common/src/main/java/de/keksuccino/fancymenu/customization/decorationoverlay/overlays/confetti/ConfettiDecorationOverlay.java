package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.ConfettiOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class ConfettiDecorationOverlay extends AbstractDecorationOverlay {

    public static final String DEFAULT_SETTLED_CAP = "500";

    @NotNull
    public String confettiScale = "1.0";
    @NotNull
    public String confettiFallSpeed = "1.0";
    @NotNull
    public String confettiBurstDensity = "1.0";
    @NotNull
    public String confettiBurstAmount = "1.0";
    public boolean confettiColorMixMode = true;
    @NotNull
    public String confettiColorHex = "#FFFFFF";
    @NotNull
    public String confettiParticleCap = DEFAULT_SETTLED_CAP;
    public boolean confettiMouseClickMode = false;
    protected final ConfettiOverlay overlay = new ConfettiOverlay(0, 0);
    protected String lastScaleString = null;
    protected String lastFallSpeedString = null;
    protected String lastDensityString = null;
    protected String lastAmountString = null;
    protected String lastCapString = null;
    protected String lastColorString = null;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setColorMixEnabled(this.confettiColorMixMode);
        this.overlay.setAutoSpawnEnabled(!this.confettiMouseClickMode);

        String colorString = PlaceholderParser.replacePlaceholders(this.confettiColorHex);
        if (!Objects.equals(colorString, this.lastColorString)) {
            this.lastColorString = colorString;
            this.overlay.setBaseColor(DrawableColor.of(colorString).getColorInt());
        }

        String scaleString = PlaceholderParser.replacePlaceholders(this.confettiScale);
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

        String fallSpeedString = PlaceholderParser.replacePlaceholders(this.confettiFallSpeed);
        if (!Objects.equals(fallSpeedString, this.lastFallSpeedString)) {
            this.lastFallSpeedString = fallSpeedString;
            float fallSpeedValue;
            if (MathUtils.isFloat(fallSpeedString)) {
                fallSpeedValue = Float.parseFloat(fallSpeedString);
            } else {
                fallSpeedValue = 1.0F;
            }
            this.overlay.setFallSpeedMultiplier(fallSpeedValue);
        }

        String densityString = PlaceholderParser.replacePlaceholders(this.confettiBurstDensity);
        if (!Objects.equals(densityString, this.lastDensityString)) {
            this.lastDensityString = densityString;
            float densityValue;
            if (MathUtils.isFloat(densityString)) {
                densityValue = Float.parseFloat(densityString);
            } else {
                densityValue = 1.0F;
            }
            this.overlay.setBurstDensity(densityValue);
        }

        String amountString = PlaceholderParser.replacePlaceholders(this.confettiBurstAmount);
        if (!Objects.equals(amountString, this.lastAmountString)) {
            this.lastAmountString = amountString;
            float amountValue;
            if (MathUtils.isFloat(amountString)) {
                amountValue = Float.parseFloat(amountString);
            } else {
                amountValue = 1.0F;
            }
            this.overlay.setBurstAmount(amountValue);
        }

        String capString = PlaceholderParser.replacePlaceholders(this.confettiParticleCap);
        if (!Objects.equals(capString, this.lastCapString)) {
            this.lastCapString = capString;
            int capValue;
            if (MathUtils.isInteger(capString)) {
                capValue = Integer.parseInt(capString);
            } else {
                capValue = Integer.parseInt(DEFAULT_SETTLED_CAP);
            }
            this.overlay.setSettledCapOverride(capValue);
        }

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.showOverlay) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.confettiMouseClickMode && button == 0) {
            this.overlay.triggerBurstAt((float)mouseX, (float)mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {

        this.overlay.clearCollisionAreas();

        visitCollisionBoxes(screen, elements, c -> this.overlay.addCollisionArea(c.x(), c.y(), c.width(), c.height()));

    }

}
