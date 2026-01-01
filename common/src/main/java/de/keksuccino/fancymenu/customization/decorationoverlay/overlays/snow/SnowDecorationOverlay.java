package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.SnowfallOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class SnowDecorationOverlay extends AbstractDecorationOverlay<SnowDecorationOverlay> {

    @NotNull
    public String snowColorHex = "#FFFFFF";
    @NotNull
    public String snowIntensity = "1.0";
    @NotNull
    public String snowScale = "1.0";
    @NotNull
    public String snowSpeed = "1.0";
    public boolean snowAccumulation = true;
    protected final SnowfallOverlay overlay = new SnowfallOverlay(0, 0);
    protected String lastSnowColorString = null;
    protected String lastSnowIntensityString = null;
    protected String lastSnowScaleString = null;
    protected String lastSnowSpeedString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "accumulate_snow",
                        () -> this.snowAccumulation,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.snowAccumulation = aBoolean;
                        },
                        "fancymenu.decoration_overlays.snow.accumulate_snow")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.accumulate_snow.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_color", Component.translatable("fancymenu.decoration_overlays.snow.color"),
                        () -> this.snowColorHex,
                        s -> {
                            editor.history.saveSnapshot();
                            this.snowColorHex = s;
                        }, true,
                        "#FFFFFF", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_intensity", Component.translatable("fancymenu.decoration_overlays.snow.intensity"),
                        () -> this.snowIntensity,
                        s -> {
                            editor.history.saveSnapshot();
                            this.snowIntensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.intensity.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_scale", Component.translatable("fancymenu.decoration_overlays.snow.scale"),
                        () -> this.snowScale,
                        s -> {
                            editor.history.saveSnapshot();
                            this.snowScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.scale.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "snow_speed", Component.translatable("fancymenu.decoration_overlays.snow.speed"),
                        () -> this.snowSpeed,
                        s -> {
                            editor.history.saveSnapshot();
                            this.snowSpeed = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.speed.desc")));

    }

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

        // Update snow scale
        String scaleString = PlaceholderParser.replacePlaceholders(this.snowScale);
        if (!Objects.equals(scaleString, this.lastSnowScaleString)) {
            this.lastSnowScaleString = scaleString;
            float scaleValue;
            if (MathUtils.isFloat(scaleString)) {
                scaleValue = Float.parseFloat(scaleString);
            } else {
                scaleValue = 1.0F;
            }
            this.overlay.setScale(scaleValue);
        }

        // Update snow speed
        String speedString = PlaceholderParser.replacePlaceholders(this.snowSpeed);
        if (!Objects.equals(speedString, this.lastSnowSpeedString)) {
            this.lastSnowSpeedString = speedString;
            float speedValue;
            if (MathUtils.isFloat(speedString)) {
                speedValue = Float.parseFloat(speedString);
            } else {
                speedValue = 1.0F;
            }
            this.overlay.setFallSpeedMultiplier(speedValue);
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
