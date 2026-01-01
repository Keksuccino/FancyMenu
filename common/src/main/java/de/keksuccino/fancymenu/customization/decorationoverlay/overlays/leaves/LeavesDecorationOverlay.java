package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.LeavesOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class LeavesDecorationOverlay extends AbstractDecorationOverlay<LeavesDecorationOverlay> {

    @NotNull
    public String leavesColorStartHex = "#7BA84F";
    @NotNull
    public String leavesColorEndHex = "#D58A3B";
    @NotNull
    public String leavesDensity = "1.0";
    @NotNull
    public String leavesWindIntensity = "1.0";
    public boolean leavesWindBlows = true;
    @NotNull
    public String leavesFallSpeed = "1.0";
    @NotNull
    public String leavesScale = "1.0";
    protected final LeavesOverlay overlay = new LeavesOverlay(0, 0);
    protected String lastLeavesColorStartString = null;
    protected String lastLeavesColorEndString = null;
    protected String lastLeavesDensityString = null;
    protected String lastLeavesWindIntensityString = null;
    protected String lastLeavesFallSpeedString = null;
    protected String lastLeavesScaleString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_color_start", Component.translatable("fancymenu.decoration_overlays.leaves.color_start"),
                        () -> this.leavesColorStartHex,
                        s -> {
                            editor.history.saveSnapshot();
                            this.leavesColorStartHex = s;
                        }, true,
                        "#7BA84F", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.color_start.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_color_end", Component.translatable("fancymenu.decoration_overlays.leaves.color_end"),
                        () -> this.leavesColorEndHex,
                        s -> {
                            editor.history.saveSnapshot();
                            this.leavesColorEndHex = s;
                        }, true,
                        "#D58A3B", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.color_end.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_density", Component.translatable("fancymenu.decoration_overlays.leaves.density"),
                        () -> this.leavesDensity,
                        s -> {
                            editor.history.saveSnapshot();
                            this.leavesDensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.density.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_wind_intensity", Component.translatable("fancymenu.decoration_overlays.leaves.wind"),
                        () -> this.leavesWindIntensity,
                        s -> {
                            editor.history.saveSnapshot();
                            this.leavesWindIntensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.wind.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "leaves_wind_blows",
                        () -> this.leavesWindBlows,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.leavesWindBlows = aBoolean;
                        },
                        "fancymenu.decoration_overlays.leaves.wind_blows")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.wind_blows.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_fall_speed", Component.translatable("fancymenu.decoration_overlays.leaves.fall_speed"),
                        () -> this.leavesFallSpeed,
                        s -> {
                            editor.history.saveSnapshot();
                            this.leavesFallSpeed = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.fall_speed.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "leaves_scale", Component.translatable("fancymenu.decoration_overlays.leaves.scale"),
                        () -> this.leavesScale,
                        s -> {
                            editor.history.saveSnapshot();
                            this.leavesScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.scale.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        String startColorString = PlaceholderParser.replacePlaceholders(this.leavesColorStartHex);
        String endColorString = PlaceholderParser.replacePlaceholders(this.leavesColorEndHex);
        if (!Objects.equals(startColorString, this.lastLeavesColorStartString) || !Objects.equals(endColorString, this.lastLeavesColorEndString)) {
            this.lastLeavesColorStartString = startColorString;
            this.lastLeavesColorEndString = endColorString;
            this.overlay.setColorRange(DrawableColor.of(startColorString).getColorInt(), DrawableColor.of(endColorString).getColorInt());
        }

        String densityString = PlaceholderParser.replacePlaceholders(this.leavesDensity);
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

        String windString = PlaceholderParser.replacePlaceholders(this.leavesWindIntensity);
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

        this.overlay.setWindBlowsEnabled(this.leavesWindBlows);

        String fallSpeedString = PlaceholderParser.replacePlaceholders(this.leavesFallSpeed);
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

        String scaleString = PlaceholderParser.replacePlaceholders(this.leavesScale);
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
