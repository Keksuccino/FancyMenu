package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.SnowfallOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class SnowDecorationOverlay extends AbstractDecorationOverlay<SnowDecorationOverlay> {

    public final Property.FloatProperty snowIntensity = putProperty(Property.floatProperty("snow_intensity", 1.0F, "fancymenu.decoration_overlays.snow.intensity"));
    public final Property.FloatProperty snowScale = putProperty(Property.floatProperty("snow_scale", 1.0F, "fancymenu.decoration_overlays.snow.scale"));
    public final Property.FloatProperty snowSpeed = putProperty(Property.floatProperty("snow_speed", 1.0F, "fancymenu.decoration_overlays.snow.speed"));
    public final Property<Boolean> snowAccumulation = putProperty(Property.booleanProperty("snow_accumulation", true, "fancymenu.decoration_overlays.snow.accumulate_snow"));
    public final Property.ColorProperty snowColorHex = putProperty(Property.hexColorProperty("snow_color_hex", "#FFFFFF", true, "fancymenu.decoration_overlays.snow.color"));

    protected final SnowfallOverlay overlay = new SnowfallOverlay(0, 0);
    protected String lastSnowColorString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.snowAccumulation.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SNOWING)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.accumulate_snow.desc")));

        this.snowColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.color.desc")));

        this.snowIntensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.DENSITY_MEDIUM)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.intensity.desc")));

        this.snowScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.scale.desc")));

        this.snowSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.speed.desc")));

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
        this.overlay.setIntensity(this.snowIntensity.getFloat());

        // Update snow scale
        this.overlay.setScale(this.snowScale.getFloat());

        // Update snow speed
        this.overlay.setFallSpeedMultiplier(this.snowSpeed.getFloat());

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
