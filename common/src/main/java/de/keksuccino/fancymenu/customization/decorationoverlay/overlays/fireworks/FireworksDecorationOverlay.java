package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.overlay.FireworksOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
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

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "fireworks_show_rockets",
                        () -> this.fireworksShowRockets,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.fireworksShowRockets = aBoolean;
                        },
                        "fancymenu.decoration_overlays.fireworks.show_rockets")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.show_rockets.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "fireworks_scale", Component.translatable("fancymenu.decoration_overlays.fireworks.scale"),
                        () -> this.fireworksScale,
                        s -> {
                            editor.history.saveSnapshot();
                            this.fireworksScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.scale.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "fireworks_explosion_size", Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size"),
                        () -> this.fireworksExplosionSize,
                        s -> {
                            editor.history.saveSnapshot();
                            this.fireworksExplosionSize = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "fireworks_amount", Component.translatable("fancymenu.decoration_overlays.fireworks.amount"),
                        () -> this.fireworksAmount,
                        s -> {
                            editor.history.saveSnapshot();
                            this.fireworksAmount = s;
                        }, true,
                        "1.0", null, false, true, null, null)
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
