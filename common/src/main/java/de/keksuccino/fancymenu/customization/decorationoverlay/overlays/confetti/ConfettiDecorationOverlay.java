package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.ConfettiOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class ConfettiDecorationOverlay extends AbstractDecorationOverlay<ConfettiDecorationOverlay> {

    public static final String DEFAULT_SETTLED_CAP = "500";

    @NotNull
    public String confettiScale = "1.0";
    @NotNull
    public String confettiFallSpeed = "1.0";
    @NotNull
    public String confettiBurstDensity = "1.0";
    @NotNull
    public String confettiBurstAmount = "1.0";
    public boolean confettiColorMixMode = true;
    @NotNull
    public String confettiParticleCap = DEFAULT_SETTLED_CAP;
    public boolean confettiMouseClickMode = false;

    public final Property.ColorProperty confettiColorHex = putProperty(Property.hexColorProperty("confetti_color_hex", "#FFFFFF", true, "fancymenu.decoration_overlays.confetti.color"));

    protected final ConfettiOverlay overlay = new ConfettiOverlay(0, 0);
    protected String lastScaleString = null;
    protected String lastFallSpeedString = null;
    protected String lastDensityString = null;
    protected String lastAmountString = null;
    protected String lastCapString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "confetti_color_mix_mode",
                        () -> this.confettiColorMixMode,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.confettiColorMixMode = aBoolean;
                        },
                        "fancymenu.decoration_overlays.confetti.color_mix")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.color_mix.desc")));

        this.confettiColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_scale", Component.translatable("fancymenu.decoration_overlays.confetti.scale"),
                        () -> this.confettiScale,
                        s -> {
                            editor.history.saveSnapshot();
                            this.confettiScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.scale.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_fall_speed", Component.translatable("fancymenu.decoration_overlays.confetti.fall_speed"),
                        () -> this.confettiFallSpeed,
                        s -> {
                            editor.history.saveSnapshot();
                            this.confettiFallSpeed = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.fall_speed.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_density", Component.translatable("fancymenu.decoration_overlays.confetti.density"),
                        () -> this.confettiBurstDensity,
                        s -> {
                            editor.history.saveSnapshot();
                            this.confettiBurstDensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.density.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_amount", Component.translatable("fancymenu.decoration_overlays.confetti.amount"),
                        () -> this.confettiBurstAmount,
                        s -> {
                            editor.history.saveSnapshot();
                            this.confettiBurstAmount = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.amount.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_particle_cap", Component.translatable("fancymenu.decoration_overlays.confetti.particle_cap"),
                        () -> this.confettiParticleCap,
                        s -> {
                            editor.history.saveSnapshot();
                            this.confettiParticleCap = s;
                        }, true,
                        ConfettiDecorationOverlay.DEFAULT_SETTLED_CAP, null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.particle_cap.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "confetti_mouse_click_mode",
                        () -> this.confettiMouseClickMode,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            this.confettiMouseClickMode = aBoolean;
                        },
                        "fancymenu.decoration_overlays.confetti.mouse_click_mode")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.mouse_click_mode.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setColorMixEnabled(this.confettiColorMixMode);
        this.overlay.setAutoSpawnEnabled(!this.confettiMouseClickMode);

        this.overlay.setBaseColor(this.confettiColorHex.getDrawable().getColorInt());

        String scaleString = PlaceholderParser.replacePlaceholders(this.confettiScale);
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

        String fallSpeedString = PlaceholderParser.replacePlaceholders(this.confettiFallSpeed);
        if (!Objects.equals(fallSpeedString, this.lastFallSpeedString)) {
            this.lastFallSpeedString = fallSpeedString;
            float fallSpeedValue;
            if (MathUtils.isFloat(fallSpeedString)) {
                fallSpeedValue = Float.parseFloat(fallSpeedString);
            } else {
                fallSpeedValue = 1.0F;
            }
            this.overlay.setFallSpeedMultiplier(fallSpeedValue);
        }

        String densityString = PlaceholderParser.replacePlaceholders(this.confettiBurstDensity);
        if (!Objects.equals(densityString, this.lastDensityString)) {
            this.lastDensityString = densityString;
            float densityValue;
            if (MathUtils.isFloat(densityString)) {
                densityValue = Float.parseFloat(densityString);
            } else {
                densityValue = 1.0F;
            }
            this.overlay.setBurstDensity(densityValue);
        }

        String amountString = PlaceholderParser.replacePlaceholders(this.confettiBurstAmount);
        if (!Objects.equals(amountString, this.lastAmountString)) {
            this.lastAmountString = amountString;
            float amountValue;
            if (MathUtils.isFloat(amountString)) {
                amountValue = Float.parseFloat(amountString);
            } else {
                amountValue = 1.0F;
            }
            this.overlay.setBurstAmount(amountValue);
        }

        String capString = PlaceholderParser.replacePlaceholders(this.confettiParticleCap);
        if (!Objects.equals(capString, this.lastCapString)) {
            this.lastCapString = capString;
            int capValue;
            if (MathUtils.isInteger(capString)) {
                capValue = Integer.parseInt(capString);
            } else {
                capValue = Integer.parseInt(DEFAULT_SETTLED_CAP);
            }
            this.overlay.setSettledCapOverride(capValue);
        }

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.showOverlay) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.confettiMouseClickMode && button == 0) {
            this.overlay.triggerBurstAt((float)mouseX, (float)mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {

        this.overlay.clearCollisionAreas();

        visitCollisionBoxes(screen, elements, c -> this.overlay.addCollisionArea(c.x(), c.y(), c.width(), c.height()));

    }

}