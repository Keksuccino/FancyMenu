package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.overlay.FireworksOverlay;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FireworksDecorationOverlay extends AbstractDecorationOverlay {

    @NotNull
    public String fireworksScale = "1.0";
    @NotNull
    public String fireworksExplosionSize = "1.0";
    @NotNull
    public String fireworksAmount = "1.0";
    public boolean fireworksShowRockets = true;
    protected final FireworksOverlay overlay = new FireworksOverlay(0, 0);
    protected String lastScaleString = null;
    protected String lastExplosionSizeString = null;
    protected String lastAmountString = null;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setRocketTrailEnabled(this.fireworksShowRockets);

        String scaleString = PlaceholderParser.replacePlaceholders(this.fireworksScale);
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

        String explosionSizeString = PlaceholderParser.replacePlaceholders(this.fireworksExplosionSize);
        if (!Objects.equals(explosionSizeString, this.lastExplosionSizeString)) {
            this.lastExplosionSizeString = explosionSizeString;
            float sizeValue;
            if (MathUtils.isFloat(explosionSizeString)) {
                sizeValue = Float.parseFloat(explosionSizeString);
            } else {
                sizeValue = 1.0F;
            }
            this.overlay.setExplosionScale(sizeValue);
        }

        String amountString = PlaceholderParser.replacePlaceholders(this.fireworksAmount);
        if (!Objects.equals(amountString, this.lastAmountString)) {
            this.lastAmountString = amountString;
            float amountValue;
            if (MathUtils.isFloat(amountString)) {
                amountValue = Float.parseFloat(amountString);
            } else {
                amountValue = 1.0F;
            }
            this.overlay.setAmountMultiplier(amountValue);
        }

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

}
