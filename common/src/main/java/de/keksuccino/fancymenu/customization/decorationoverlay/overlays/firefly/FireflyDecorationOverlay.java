package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.FireflyOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class FireflyDecorationOverlay extends AbstractDecorationOverlay<FireflyDecorationOverlay> {

    @NotNull
    public String fireflyColorHex = "#FFE08A";
    @NotNull
    public String fireflyGroupDensity = "1.0";
    public String fireflyGroupAmount = "1.0";
    @NotNull
    public String fireflyGroupSize = "1.0";
    @NotNull
    public String fireflyScale = "1.0";
    public boolean fireflyFollowMouse = true;
    public boolean fireflyLanding = true;
    protected final FireflyOverlay overlay = new FireflyOverlay(0, 0);
    protected String lastFireflyColorString = null;
    protected String lastFireflyGroupDensityString = null;
    protected String lastFireflyGroupAmountString = null;
    protected String lastFireflyGroupSizeString = null;
    protected String lastFireflyScaleString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.addToggleContextMenuEntryTo(menu, "firefly_follow_mouse", FireflyDecorationOverlay.class,
                        o -> o.fireflyFollowMouse,
                        (o, aBoolean) -> o.fireflyFollowMouse = aBoolean,
                        "fancymenu.decoration_overlays.fireflies.follow_mouse")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.follow_mouse.desc")));

        this.addToggleContextMenuEntryTo(menu, "firefly_landing", FireflyDecorationOverlay.class,
                        o -> o.fireflyLanding,
                        (o, aBoolean) -> o.fireflyLanding = aBoolean,
                        "fancymenu.decoration_overlays.fireflies.landing")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.landing.desc")));

        this.addInputContextMenuEntryTo(menu, "firefly_color", FireflyDecorationOverlay.class,
                        o -> o.fireflyColorHex,
                        (o, s) -> o.fireflyColorHex = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.fireflies.color"),
                        true, "#FFE08A", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.color.desc")));

        this.addInputContextMenuEntryTo(menu, "firefly_group_amount", FireflyDecorationOverlay.class,
                        o -> o.fireflyGroupAmount,
                        (o, s) -> o.fireflyGroupAmount = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.fireflies.group_amount"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_amount.desc")));

        this.addInputContextMenuEntryTo(menu, "firefly_group_density", FireflyDecorationOverlay.class,
                        o -> o.fireflyGroupDensity,
                        (o, s) -> o.fireflyGroupDensity = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.fireflies.intensity"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.intensity.desc")));

        this.addInputContextMenuEntryTo(menu, "firefly_group_size", FireflyDecorationOverlay.class,
                        o -> o.fireflyGroupSize,
                        (o, s) -> o.fireflyGroupSize = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.fireflies.group_size"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_size.desc")));

        this.addInputContextMenuEntryTo(menu, "firefly_scale", FireflyDecorationOverlay.class,
                        o -> o.fireflyScale,
                        (o, s) -> o.fireflyScale = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.fireflies.scale"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.scale.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setFollowMouseEnabled(this.fireflyFollowMouse);
        this.overlay.setLandingEnabled(this.fireflyLanding);

        String colorString = PlaceholderParser.replacePlaceholders(this.fireflyColorHex);
        if (!Objects.equals(colorString, this.lastFireflyColorString)) {
            this.lastFireflyColorString = colorString;
            this.overlay.setColor(DrawableColor.of(colorString).getColorInt());
        }

        String densityString = PlaceholderParser.replacePlaceholders(this.fireflyGroupDensity);
        if (!Objects.equals(densityString, this.lastFireflyGroupDensityString)) {
            this.lastFireflyGroupDensityString = densityString;
            float densityValue;
            if (MathUtils.isFloat(densityString)) {
                densityValue = Float.parseFloat(densityString);
            } else {
                densityValue = 1.0F;
            }
            this.overlay.setGroupDensity(densityValue);
        }

        String amountString = PlaceholderParser.replacePlaceholders(this.fireflyGroupAmount);
        if (!Objects.equals(amountString, this.lastFireflyGroupAmountString)) {
            this.lastFireflyGroupAmountString = amountString;
            float amountValue;
            if (MathUtils.isFloat(amountString)) {
                amountValue = Float.parseFloat(amountString);
            } else {
                amountValue = 1.0F;
            }
            this.overlay.setGroupAmount(amountValue);
        }

        String groupSizeString = PlaceholderParser.replacePlaceholders(this.fireflyGroupSize);
        if (!Objects.equals(groupSizeString, this.lastFireflyGroupSizeString)) {
            this.lastFireflyGroupSizeString = groupSizeString;
            float sizeValue;
            if (MathUtils.isFloat(groupSizeString)) {
                sizeValue = Float.parseFloat(groupSizeString);
            } else {
                sizeValue = 1.0F;
            }
            this.overlay.setGroupSize(sizeValue);
        }

        String scaleString = PlaceholderParser.replacePlaceholders(this.fireflyScale);
        if (!Objects.equals(scaleString, this.lastFireflyScaleString)) {
            this.lastFireflyScaleString = scaleString;
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
