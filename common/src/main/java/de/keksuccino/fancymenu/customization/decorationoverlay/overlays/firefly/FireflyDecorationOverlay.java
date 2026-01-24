package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.FireflyOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class FireflyDecorationOverlay extends AbstractDecorationOverlay<FireflyDecorationOverlay> {

    public final Property.StringProperty fireflyGroupDensity = putProperty(Property.stringProperty("firefly_group_density", "1.0", false, true, "fancymenu.decoration_overlays.fireflies.intensity"));
    public final Property.StringProperty fireflyGroupAmount = putProperty(Property.stringProperty("firefly_group_amount", "1.0", false, true, "fancymenu.decoration_overlays.fireflies.group_amount"));
    public final Property.StringProperty fireflyGroupSize = putProperty(Property.stringProperty("firefly_group_size", "1.0", false, true, "fancymenu.decoration_overlays.fireflies.group_size"));
    public final Property.StringProperty fireflyScale = putProperty(Property.stringProperty("firefly_scale", "1.0", false, true, "fancymenu.decoration_overlays.fireflies.scale"));
    public final Property<Boolean> fireflyFollowMouse = putProperty(Property.booleanProperty("firefly_follow_mouse", true, "fancymenu.decoration_overlays.fireflies.follow_mouse"));
    public final Property<Boolean> fireflyLanding = putProperty(Property.booleanProperty("firefly_landing", true, "fancymenu.decoration_overlays.fireflies.landing"));
    public final Property.ColorProperty fireflyColorHex = putProperty(Property.hexColorProperty("firefly_color_hex", "#FFE08A", true, "fancymenu.decoration_overlays.fireflies.color"));

    protected final FireflyOverlay overlay = new FireflyOverlay(0, 0);
    protected String lastFireflyColorString = null;
    protected String lastFireflyGroupDensityString = null;
    protected String lastFireflyGroupAmountString = null;
    protected String lastFireflyGroupSizeString = null;
    protected String lastFireflyScaleString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.fireflyFollowMouse.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.MOUSE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.follow_mouse.desc")));

        this.fireflyLanding.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.FLIGHT_LAND)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.landing.desc")));

        this.fireflyColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.color.desc")));

        this.fireflyGroupAmount.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.GROUPS)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_amount.desc")));

        this.fireflyGroupDensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.DENSITY_MEDIUM)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.intensity.desc")));

        this.fireflyGroupSize.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_size.desc")));

        this.fireflyScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.ZOOM_OUT_MAP)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.scale.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setFollowMouseEnabled(this.fireflyFollowMouse.tryGetNonNullElse(true));
        this.overlay.setLandingEnabled(this.fireflyLanding.tryGetNonNullElse(true));

        String colorString = this.fireflyColorHex.getHex();
        if (!Objects.equals(colorString, this.lastFireflyColorString)) {
            this.lastFireflyColorString = colorString;
            this.overlay.setColor(this.fireflyColorHex.getDrawable().getColorInt());
        }

        String densityString = this.fireflyGroupDensity.getString();
        if (densityString == null) densityString = "1.0";
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

        String amountString = this.fireflyGroupAmount.getString();
        if (amountString == null) amountString = "1.0";
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

        String groupSizeString = this.fireflyGroupSize.getString();
        if (groupSizeString == null) groupSizeString = "1.0";
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

        String scaleString = this.fireflyScale.getString();
        if (scaleString == null) scaleString = "1.0";
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
