package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.FireflyOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FireflyDecorationOverlay extends AbstractDecorationOverlay {

    @NotNull
    public String fireflyColorHex = "#FFE08A";
    @NotNull
    public String fireflyIntensity = "1.0";
    @NotNull
    public String fireflyGroupSize = "1.0";
    public boolean fireflyFollowMouse = true;
    public boolean fireflyLanding = true;
    protected final FireflyOverlay overlay = new FireflyOverlay(0, 0);
    protected String lastFireflyColorString = null;
    protected String lastFireflyIntensityString = null;
    protected String lastFireflyGroupSizeString = null;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setFollowMouseEnabled(this.fireflyFollowMouse);
        this.overlay.setLandingEnabled(this.fireflyLanding);

        String colorString = PlaceholderParser.replacePlaceholders(this.fireflyColorHex);
        if (!Objects.equals(colorString, this.lastFireflyColorString)) {
            this.lastFireflyColorString = colorString;
            this.overlay.setColor(DrawableColor.of(colorString).getColorInt());
        }

        String intensityString = PlaceholderParser.replacePlaceholders(this.fireflyIntensity);
        if (!Objects.equals(intensityString, this.lastFireflyIntensityString)) {
            this.lastFireflyIntensityString = intensityString;
            float intensityValue;
            if (MathUtils.isFloat(intensityString)) {
                intensityValue = Float.parseFloat(intensityString);
            } else {
                intensityValue = 1.0F;
            }
            this.overlay.setIntensity(intensityValue);
        }

        String groupSizeString = PlaceholderParser.replacePlaceholders(this.fireflyGroupSize);
        if (!Objects.equals(groupSizeString, this.lastFireflyGroupSizeString)) {
            this.lastFireflyGroupSizeString = groupSizeString;
            float sizeValue;
            if (MathUtils.isFloat(groupSizeString)) {
                sizeValue = Float.parseFloat(groupSizeString);
            } else {
                sizeValue = 1.0F;
            }
            this.overlay.setGroupSize(sizeValue);
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
