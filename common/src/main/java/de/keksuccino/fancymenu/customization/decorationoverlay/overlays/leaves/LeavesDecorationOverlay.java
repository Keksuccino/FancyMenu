package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
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

    public final Property.FloatProperty leavesDensity = putProperty(Property.floatProperty("leaves_density", 1.0F, "fancymenu.decoration_overlays.leaves.density"));
    public final Property.FloatProperty leavesWindIntensity = putProperty(Property.floatProperty("leaves_wind_intensity", 1.0F, "fancymenu.decoration_overlays.leaves.wind"));
    public final Property<Boolean> leavesWindBlows = putProperty(Property.booleanProperty("leaves_wind_blows", true, "fancymenu.decoration_overlays.leaves.wind_blows"));
    public final Property.FloatProperty leavesFallSpeed = putProperty(Property.floatProperty("leaves_fall_speed", 1.0F, "fancymenu.decoration_overlays.leaves.fall_speed"));
    public final Property.FloatProperty leavesScale = putProperty(Property.floatProperty("leaves_scale", 1.0F, "fancymenu.decoration_overlays.leaves.scale"));
    public final Property.ColorProperty leavesColorStartHex = putProperty(Property.hexColorProperty("leaves_color_start_hex", "#7BA84F", true, "fancymenu.decoration_overlays.leaves.color_start"));
    public final Property.ColorProperty leavesColorEndHex = putProperty(Property.hexColorProperty("leaves_color_end_hex", "#D58A3B", true, "fancymenu.decoration_overlays.leaves.color_end"));

    protected final LeavesOverlay overlay = new LeavesOverlay(0, 0);
    protected String lastLeavesColorStartString = null;
    protected String lastLeavesColorEndString = null;

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

        this.overlay.setIntensity(this.leavesDensity.getFloat());

        this.overlay.setWindStrength(this.leavesWindIntensity.getFloat());

        this.overlay.setWindBlowsEnabled(this.leavesWindBlows.tryGetNonNullElse(true));

        this.overlay.setFallSpeedMultiplier(this.leavesFallSpeed.getFloat());

        this.overlay.setScale(this.leavesScale.getFloat());

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
