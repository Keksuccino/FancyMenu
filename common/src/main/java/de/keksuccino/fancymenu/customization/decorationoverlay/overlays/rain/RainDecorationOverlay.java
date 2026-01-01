package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.RainOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
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
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "rain_puddles",
                        () -> this.rainPuddles,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.rainPuddles = aBoolean;
                        },
                        "fancymenu.decoration_overlays.rain.puddles")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.puddles.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "rain_drips",
                        () -> this.rainDrips,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.rainDrips = aBoolean;
                        },
                        "fancymenu.decoration_overlays.rain.drips")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.drips.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "rain_thunder",
                        () -> this.rainThunder,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.rainThunder = aBoolean;
                        },
                        "fancymenu.decoration_overlays.rain.thunder")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.thunder.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "rain_thunder_brightness", Component.translatable("fancymenu.decoration_overlays.rain.thunder_brightness"),
                        () -> this.rainThunderBrightness,
                        s -> {
                            editor.history.saveSnapshot();
                            this.rainThunderBrightness = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.thunder_brightness.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "rain_color", Component.translatable("fancymenu.decoration_overlays.rain.color"),
                        () -> this.rainColorHex,
                        s -> {
                            editor.history.saveSnapshot();
                            this.rainColorHex = s;
                        }, true,
                        "#CFE7FF", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "rain_intensity", Component.translatable("fancymenu.decoration_overlays.rain.intensity"),
                        () -> this.rainIntensity,
                        s -> {
                            editor.history.saveSnapshot();
                            this.rainIntensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.intensity.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "rain_scale", Component.translatable("fancymenu.decoration_overlays.rain.scale"),
                        () -> this.rainScale,
                        s -> {
                            editor.history.saveSnapshot();
                            this.rainScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.scale.desc")));

    }

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

        visitCollisionBoxes(screen, elements, c -> this.overlay.addCollisionArea(c.x(), c.y(), c.width(), c.height()));

    }

}
