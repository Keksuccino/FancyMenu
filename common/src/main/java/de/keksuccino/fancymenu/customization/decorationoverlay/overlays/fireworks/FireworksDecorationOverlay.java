package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.FireworksOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class FireworksDecorationOverlay extends AbstractDecorationOverlay<FireworksDecorationOverlay> {

    public final Property.StringProperty fireworksScale = putProperty(Property.stringProperty("fireworks_scale", "1.0", false, true, "fancymenu.decoration_overlays.fireworks.scale"));
    public final Property.StringProperty fireworksExplosionSize = putProperty(Property.stringProperty("fireworks_explosion_size", "1.0", false, true, "fancymenu.decoration_overlays.fireworks.explosion_size"));
    public final Property.StringProperty fireworksAmount = putProperty(Property.stringProperty("fireworks_amount", "1.0", false, true, "fancymenu.decoration_overlays.fireworks.amount"));
    public final Property<Boolean> fireworksShowRockets = putProperty(Property.booleanProperty("fireworks_show_rockets", true, "fancymenu.decoration_overlays.fireworks.show_rockets"));

    protected final FireworksOverlay overlay = new FireworksOverlay(0, 0);
    protected String lastScaleString = null;
    protected String lastExplosionSizeString = null;
    protected String lastAmountString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.fireworksShowRockets.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.show_rockets.desc")));

        this.fireworksScale.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.scale.desc")));

        this.fireworksExplosionSize.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size.desc")));

        this.fireworksAmount.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.amount.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setRocketTrailEnabled(this.fireworksShowRockets.tryGetNonNullElse(true));

        String scaleString = this.fireworksScale.getString();
        if (scaleString == null) scaleString = "1.0";
        if (!Objects.equals(scaleString, this.lastScaleString)) {
            this.lastScaleString = scaleString;
            float scaleValue;
            if (MathUtils.isFloat(scaleString)) {
                scaleValue = Float.parseFloat(scaleString);
            } else {
                scaleValue = 1.0F;
            }
            this.overlay.setScale(scaleValue);
        }

        String explosionSizeString = this.fireworksExplosionSize.getString();
        if (explosionSizeString == null) explosionSizeString = "1.0";
        if (!Objects.equals(explosionSizeString, this.lastExplosionSizeString)) {
            this.lastExplosionSizeString = explosionSizeString;
            float sizeValue;
            if (MathUtils.isFloat(explosionSizeString)) {
                sizeValue = Float.parseFloat(explosionSizeString);
            } else {
                sizeValue = 1.0F;
            }
            this.overlay.setExplosionScale(sizeValue);
        }

        String amountString = this.fireworksAmount.getString();
        if (amountString == null) amountString = "1.0";
        if (!Objects.equals(amountString, this.lastAmountString)) {
            this.lastAmountString = amountString;
            float amountValue;
            if (MathUtils.isFloat(amountString)) {
                amountValue = Float.parseFloat(amountString);
            } else {
                amountValue = 1.0F;
            }
            this.overlay.setAmountMultiplier(amountValue);
        }

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

}
