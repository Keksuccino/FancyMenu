package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
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

    public final Property.StringProperty snowIntensity = putProperty(Property.stringProperty("snow_intensity", "1.0", false, true, "fancymenu.decoration_overlays.snow.intensity"));
    public final Property.StringProperty snowScale = putProperty(Property.stringProperty("snow_scale", "1.0", false, true, "fancymenu.decoration_overlays.snow.scale"));
    public final Property.StringProperty snowSpeed = putProperty(Property.stringProperty("snow_speed", "1.0", false, true, "fancymenu.decoration_overlays.snow.speed"));
    public final Property<Boolean> snowAccumulation = putProperty(Property.booleanProperty("snow_accumulation", true, "fancymenu.decoration_overlays.snow.accumulate_snow"));
    public final Property.ColorProperty snowColorHex = putProperty(Property.hexColorProperty("snow_color_hex", "#FFFFFF", true, "fancymenu.decoration_overlays.snow.color"));

    protected final SnowfallOverlay overlay = new SnowfallOverlay(0, 0);
    protected String lastSnowColorString = null;
    protected String lastSnowIntensityString = null;
    protected String lastSnowScaleString = null;
    protected String lastSnowSpeedString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.snowAccumulation.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.accumulate_snow.desc")));

        this.snowColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.color.desc")));

        this.snowIntensity.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.intensity.desc")));

        this.snowScale.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.scale.desc")));

        this.snowSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.speed.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Update snow accumulation
        this.overlay.setAccumulationEnabled(this.snowAccumulation.tryGetNonNullElse(true));

        // Update snow color
        String colorString = this.snowColorHex.getHex();
        if (!Objects.equals(colorString, this.lastSnowColorString)) {
            this.lastSnowColorString = colorString;
            this.overlay.setColor(this.snowColorHex.getDrawable().getColorInt());
        }

        // Update snow intensity
        String intensityString = this.snowIntensity.getString();
        if (intensityString == null) intensityString = "1.0";
        if (!Objects.equals(intensityString, this.lastSnowIntensityString)) {
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
        String scaleString = this.snowScale.getString();
        if (scaleString == null) scaleString = "1.0";
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
        String speedString = this.snowSpeed.getString();
        if (speedString == null) speedString = "1.0";
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
