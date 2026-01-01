package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.overlay.FireflyOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
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

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "firefly_follow_mouse",
                        () -> this.fireflyFollowMouse,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.fireflyFollowMouse = aBoolean;
                        },
                        "fancymenu.decoration_overlays.fireflies.follow_mouse")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.follow_mouse.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "firefly_landing",
                        () -> this.fireflyLanding,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.fireflyLanding = aBoolean;
                        },
                        "fancymenu.decoration_overlays.fireflies.landing")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.landing.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_color", Component.translatable("fancymenu.decoration_overlays.fireflies.color"),
                        () -> this.fireflyColorHex,
                        s -> {
                            editor.history.saveSnapshot();
                            this.fireflyColorHex = s;
                        }, true,
                        "#FFE08A", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_group_amount", Component.translatable("fancymenu.decoration_overlays.fireflies.group_amount"),
                        () -> this.fireflyGroupAmount,
                        s -> {
                            editor.history.saveSnapshot();
                            this.fireflyGroupAmount = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_amount.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_group_density", Component.translatable("fancymenu.decoration_overlays.fireflies.intensity"),
                        () -> this.fireflyGroupDensity,
                        s -> {
                            editor.history.saveSnapshot();
                            this.fireflyGroupDensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.intensity.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_group_size", Component.translatable("fancymenu.decoration_overlays.fireflies.group_size"),
                        () -> this.fireflyGroupSize,
                        s -> {
                            editor.history.saveSnapshot();
                            this.fireflyGroupSize = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_size.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_scale", Component.translatable("fancymenu.decoration_overlays.fireflies.scale"),
                        () -> this.fireflyScale,
                        s -> {
                            editor.history.saveSnapshot();
                            this.fireflyScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
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
