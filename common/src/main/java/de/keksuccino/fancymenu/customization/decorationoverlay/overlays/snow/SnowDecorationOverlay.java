package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.SnowfallOverlay;
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
    public String snowIntensity = "1.0";
    @NotNull
    public String snowScale = "1.0";
    @NotNull
    public String snowSpeed = "1.0";
    public boolean snowAccumulation = true;

    public final Property.ColorProperty snowColorHex = putProperty(Property.hexColorProperty("snow_color_hex", "#FFFFFF", true, "fancymenu.decoration_overlays.snow.color"));

    protected final SnowfallOverlay overlay = new SnowfallOverlay(0, 0);
    protected String lastSnowColorString = null;
    protected String lastSnowIntensityString = null;
    protected String lastSnowScaleString = null;
    protected String lastSnowSpeedString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.addToggleContextMenuEntryTo(menu, "accumulate_snow", SnowDecorationOverlay.class,
                        o -> o.snowAccumulation,
                        (o, aBoolean) -> o.snowAccumulation = aBoolean,
                        "fancymenu.decoration_overlays.snow.accumulate_snow")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.accumulate_snow.desc")));

        this.snowColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.color.desc")));

        this.addInputContextMenuEntryTo(menu, "snow_intensity", SnowDecorationOverlay.class,
                        o -> o.snowIntensity,
                        (o, s) -> o.snowIntensity = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.snow.intensity"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.intensity.desc")));

        this.addInputContextMenuEntryTo(menu, "snow_scale", SnowDecorationOverlay.class,
                        o -> o.snowScale,
                        (o, s) -> o.snowScale = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.snow.scale"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.scale.desc")));

        this.addInputContextMenuEntryTo(menu, "snow_speed", SnowDecorationOverlay.class,
                        o -> o.snowSpeed,
                        (o, s) -> o.snowSpeed = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.snow.speed"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.speed.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Update snow accumulation
        this.overlay.setAccumulationEnabled(this.snowAccumulation);

        // Update snow color
        String colorString = this.snowColorHex.getHex();
        if (!Objects.equals(colorString, this.lastSnowColorString)) {
            this.lastSnowColorString = colorString;
            this.overlay.setColor(this.snowColorHex.getDrawable().getColorInt());
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
