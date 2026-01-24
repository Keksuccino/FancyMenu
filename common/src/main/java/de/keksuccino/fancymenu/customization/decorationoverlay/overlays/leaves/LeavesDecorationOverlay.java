package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.LeavesOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class LeavesDecorationOverlay extends AbstractDecorationOverlay<LeavesDecorationOverlay> {

    public final Property.StringProperty leavesDensity = putProperty(Property.stringProperty("leaves_density", "1.0", false, true, "fancymenu.decoration_overlays.leaves.density"));
    public final Property.StringProperty leavesWindIntensity = putProperty(Property.stringProperty("leaves_wind_intensity", "1.0", false, true, "fancymenu.decoration_overlays.leaves.wind"));
    public final Property<Boolean> leavesWindBlows = putProperty(Property.booleanProperty("leaves_wind_blows", true, "fancymenu.decoration_overlays.leaves.wind_blows"));
    public final Property.StringProperty leavesFallSpeed = putProperty(Property.stringProperty("leaves_fall_speed", "1.0", false, true, "fancymenu.decoration_overlays.leaves.fall_speed"));
    public final Property.StringProperty leavesScale = putProperty(Property.stringProperty("leaves_scale", "1.0", false, true, "fancymenu.decoration_overlays.leaves.scale"));
    public final Property.ColorProperty leavesColorStartHex = putProperty(Property.hexColorProperty("leaves_color_start_hex", "#7BA84F", true, "fancymenu.decoration_overlays.leaves.color_start"));
    public final Property.ColorProperty leavesColorEndHex = putProperty(Property.hexColorProperty("leaves_color_end_hex", "#D58A3B", true, "fancymenu.decoration_overlays.leaves.color_end"));

    protected final LeavesOverlay overlay = new LeavesOverlay(0, 0);
    protected String lastLeavesColorStartString = null;
    protected String lastLeavesColorEndString = null;
    protected String lastLeavesDensityString = null;
    protected String lastLeavesWindIntensityString = null;
    protected String lastLeavesFallSpeedString = null;
    protected String lastLeavesScaleString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.leavesColorStartHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.color_start.desc")));

        this.leavesColorEndHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.color_end.desc")));

        this.leavesDensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.DENSITY_MEDIUM)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.density.desc")));

        this.leavesWindIntensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.AIR)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.wind.desc")));

        this.leavesWindBlows.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.AIR)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.wind_blows.desc")));

        this.leavesFallSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.fall_speed.desc")));

        this.leavesScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.scale.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        String startColorString = this.leavesColorStartHex.getHex();
        String endColorString = this.leavesColorEndHex.getHex();
        if (!Objects.equals(startColorString, this.lastLeavesColorStartString) || !Objects.equals(endColorString, this.lastLeavesColorEndString)) {
            this.lastLeavesColorStartString = startColorString;
            this.lastLeavesColorEndString = endColorString;
            this.overlay.setColorRange(this.leavesColorStartHex.getDrawable().getColorInt(), this.leavesColorEndHex.getDrawable().getColorInt());
        }

        String densityString = this.leavesDensity.getString();
        if (densityString == null) densityString = "1.0";
        if (!Objects.equals(densityString, this.lastLeavesDensityString)) {
            this.lastLeavesDensityString = densityString;
            float densityValue;
            if (MathUtils.isFloat(densityString)) {
                densityValue = Float.parseFloat(densityString);
            } else {
                densityValue = 1.0F;
            }
            this.overlay.setIntensity(densityValue);
        }

        String windString = this.leavesWindIntensity.getString();
        if (windString == null) windString = "1.0";
        if (!Objects.equals(windString, this.lastLeavesWindIntensityString)) {
            this.lastLeavesWindIntensityString = windString;
            float windValue;
            if (MathUtils.isFloat(windString)) {
                windValue = Float.parseFloat(windString);
            } else {
                windValue = 1.0F;
            }
            this.overlay.setWindStrength(windValue);
        }

        this.overlay.setWindBlowsEnabled(this.leavesWindBlows.tryGetNonNullElse(true));

        String fallSpeedString = this.leavesFallSpeed.getString();
        if (fallSpeedString == null) fallSpeedString = "1.0";
        if (!Objects.equals(fallSpeedString, this.lastLeavesFallSpeedString)) {
            this.lastLeavesFallSpeedString = fallSpeedString;
            float fallSpeedValue;
            if (MathUtils.isFloat(fallSpeedString)) {
                fallSpeedValue = Float.parseFloat(fallSpeedString);
            } else {
                fallSpeedValue = 1.0F;
            }
            this.overlay.setFallSpeedMultiplier(fallSpeedValue);
        }

        String scaleString = this.leavesScale.getString();
        if (scaleString == null) scaleString = "1.0";
        if (!Objects.equals(scaleString, this.lastLeavesScaleString)) {
            this.lastLeavesScaleString = scaleString;
            float scaleValue;
            if (MathUtils.isFloat(scaleString)) {
                scaleValue = Float.parseFloat(scaleString);
            } else {
                scaleValue = 1.0F;
            }
            this.overlay.setScale(scaleValue);
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
