package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.RainOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class RainDecorationOverlay extends AbstractDecorationOverlay<RainDecorationOverlay> {

    @NotNull
    public String rainIntensity = "1.0";
    @NotNull
    public String rainScale = "1.0";
    @NotNull
    public String rainThunderBrightness = "1.0";
    public boolean rainPuddles = true;
    public boolean rainDrips = true;
    public boolean rainThunder = false;

    public final Property.ColorProperty rainColorHex = putProperty(Property.hexColorProperty("rain_color_hex", "#CFE7FF", true, "fancymenu.decoration_overlays.rain.color"));

    protected final RainOverlay overlay = new RainOverlay(0, 0);
    protected String lastRainColorString = null;
    protected String lastRainIntensityString = null;
    protected String lastRainScaleString = null;
    protected String lastRainThunderBrightnessString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.addToggleContextMenuEntryTo(menu, "rain_puddles", RainDecorationOverlay.class,
                        o -> o.rainPuddles,
                        (o, aBoolean) -> o.rainPuddles = aBoolean,
                        "fancymenu.decoration_overlays.rain.puddles")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.puddles.desc")));

        this.addToggleContextMenuEntryTo(menu, "rain_drips", RainDecorationOverlay.class,
                        o -> o.rainDrips,
                        (o, aBoolean) -> o.rainDrips = aBoolean,
                        "fancymenu.decoration_overlays.rain.drips")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.drips.desc")));

        this.addToggleContextMenuEntryTo(menu, "rain_thunder", RainDecorationOverlay.class,
                        o -> o.rainThunder,
                        (o, aBoolean) -> o.rainThunder = aBoolean,
                        "fancymenu.decoration_overlays.rain.thunder")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.thunder.desc")));

        this.addInputContextMenuEntryTo(menu, "rain_thunder_brightness", RainDecorationOverlay.class,
                        o -> o.rainThunderBrightness,
                        (o, s) -> o.rainThunderBrightness = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.rain.thunder_brightness"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.thunder_brightness.desc")));

        this.rainColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.color.desc")));

        this.addInputContextMenuEntryTo(menu, "rain_intensity", RainDecorationOverlay.class,
                        o -> o.rainIntensity,
                        (o, s) -> o.rainIntensity = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.rain.intensity"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.intensity.desc")));

        this.addInputContextMenuEntryTo(menu, "rain_scale", RainDecorationOverlay.class,
                        o -> o.rainScale,
                        (o, s) -> o.rainScale = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.rain.scale"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.scale.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Update puddles
        this.overlay.setPuddlesEnabled(this.rainPuddles);
        this.overlay.setDripsEnabled(this.rainDrips);
        this.overlay.setThunderEnabled(this.rainThunder);

        // Update rain color
        String colorString = this.rainColorHex.getHex();
        if (!Objects.equals(colorString, this.lastRainColorString)) {
            this.lastRainColorString = colorString;
            this.overlay.setColor(this.rainColorHex.getDrawable().getColorInt());
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

        visitCollisionBoxes(screen, elements, c -> this.overlay.addCollisionArea(c.x(), c.y(), c.width(), c.height()));

    }

}
