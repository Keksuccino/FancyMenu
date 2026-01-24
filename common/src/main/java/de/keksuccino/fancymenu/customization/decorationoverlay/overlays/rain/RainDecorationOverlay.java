package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.RainOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class RainDecorationOverlay extends AbstractDecorationOverlay<RainDecorationOverlay> {

    public final Property.StringProperty rainIntensity = putProperty(Property.stringProperty("rain_intensity", "1.0", false, true, "fancymenu.decoration_overlays.rain.intensity"));
    public final Property.StringProperty rainScale = putProperty(Property.stringProperty("rain_scale", "1.0", false, true, "fancymenu.decoration_overlays.rain.scale"));
    public final Property.StringProperty rainThunderBrightness = putProperty(Property.stringProperty("rain_thunder_brightness", "1.0", false, true, "fancymenu.decoration_overlays.rain.thunder_brightness"));
    public final Property<Boolean> rainPuddles = putProperty(Property.booleanProperty("rain_puddles", true, "fancymenu.decoration_overlays.rain.puddles"));
    public final Property<Boolean> rainDrips = putProperty(Property.booleanProperty("rain_drips", true, "fancymenu.decoration_overlays.rain.drips"));
    public final Property<Boolean> rainThunder = putProperty(Property.booleanProperty("rain_thunder", false, "fancymenu.decoration_overlays.rain.thunder"));
    public final Property.ColorProperty rainColorHex = putProperty(Property.hexColorProperty("rain_color_hex", "#CFE7FF", true, "fancymenu.decoration_overlays.rain.color"));

    protected final RainOverlay overlay = new RainOverlay(0, 0);
    protected String lastRainColorString = null;
    protected String lastRainIntensityString = null;
    protected String lastRainScaleString = null;
    protected String lastRainThunderBrightnessString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.rainPuddles.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.WATER)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.puddles.desc")));

        this.rainDrips.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.WATER_DROP)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.drips.desc")));

        this.rainThunder.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.THUNDERSTORM)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.thunder.desc")));

        this.rainThunderBrightness.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.BRIGHTNESS_6)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.thunder_brightness.desc")));

        this.rainColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.color.desc")));

        this.rainIntensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.intensity.desc")));

        this.rainScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.scale.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Update puddles
        this.overlay.setPuddlesEnabled(this.rainPuddles.tryGetNonNullElse(true));
        this.overlay.setDripsEnabled(this.rainDrips.tryGetNonNullElse(true));
        this.overlay.setThunderEnabled(this.rainThunder.tryGetNonNullElse(false));

        // Update rain color
        String colorString = this.rainColorHex.getHex();
        if (!Objects.equals(colorString, this.lastRainColorString)) {
            this.lastRainColorString = colorString;
            this.overlay.setColor(this.rainColorHex.getDrawable().getColorInt());
        }

        // Update rain intensity
        String intensityString = this.rainIntensity.getString();
        if (intensityString == null) intensityString = "1.0";
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

        String scaleString = this.rainScale.getString();
        if (scaleString == null) scaleString = "1.0";
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

        String thunderBrightnessString = this.rainThunderBrightness.getString();
        if (thunderBrightnessString == null) thunderBrightnessString = "1.0";
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

        visitCollisionBoxes(screen, elements, c -> this.overlay.addCollisionArea(c.x(), c.y(), c.width(), c.height()));

    }

}
