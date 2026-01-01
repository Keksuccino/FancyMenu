package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.overlay.FireworksOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FireworksDecorationOverlay extends AbstractDecorationOverlay<FireworksDecorationOverlay> {

    @NotNull
    public String fireworksScale = "1.0";
    @NotNull
    public String fireworksExplosionSize = "1.0";
    @NotNull
    public String fireworksAmount = "1.0";
    public boolean fireworksShowRockets = true;
    protected final FireworksOverlay overlay = new FireworksOverlay(0, 0);
    protected String lastScaleString = null;
    protected String lastExplosionSizeString = null;
    protected String lastAmountString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.addToggleContextMenuEntryTo(menu, "fireworks_show_rockets", FireworksDecorationOverlay.class,
                        o -> o.fireworksShowRockets,
                        (o, aBoolean) -> o.fireworksShowRockets = aBoolean,
                        "fancymenu.decoration_overlays.fireworks.show_rockets")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.show_rockets.desc")));

        this.addInputContextMenuEntryTo(menu, "fireworks_scale", FireworksDecorationOverlay.class,
                        o -> o.fireworksScale,
                        (o, s) -> o.fireworksScale = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.fireworks.scale"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.scale.desc")));

        this.addInputContextMenuEntryTo(menu, "fireworks_explosion_size", FireworksDecorationOverlay.class,
                        o -> o.fireworksExplosionSize,
                        (o, s) -> o.fireworksExplosionSize = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size.desc")));

        this.addInputContextMenuEntryTo(menu, "fireworks_amount", FireworksDecorationOverlay.class,
                        o -> o.fireworksAmount,
                        (o, s) -> o.fireworksAmount = s,
                        null, false, true,
                        Component.translatable("fancymenu.decoration_overlays.fireworks.amount"),
                        true, "1.0", null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.amount.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setRocketTrailEnabled(this.fireworksShowRockets);

        String scaleString = PlaceholderParser.replacePlaceholders(this.fireworksScale);
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

        String explosionSizeString = PlaceholderParser.replacePlaceholders(this.fireworksExplosionSize);
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

        String amountString = PlaceholderParser.replacePlaceholders(this.fireworksAmount);
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
