package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.ConfettiOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

public class ConfettiDecorationOverlay extends AbstractDecorationOverlay<ConfettiDecorationOverlay> {

    public static final String DEFAULT_SETTLED_CAP = "500";

    public final Property.StringProperty confettiScale = putProperty(Property.stringProperty("confetti_scale", "1.0", false, true, "fancymenu.decoration_overlays.confetti.scale"));
    public final Property.StringProperty confettiFallSpeed = putProperty(Property.stringProperty("confetti_fall_speed", "1.0", false, true, "fancymenu.decoration_overlays.confetti.fall_speed"));
    public final Property.StringProperty confettiBurstDensity = putProperty(Property.stringProperty("confetti_burst_density", "1.0", false, true, "fancymenu.decoration_overlays.confetti.density"));
    public final Property.StringProperty confettiBurstAmount = putProperty(Property.stringProperty("confetti_burst_amount", "1.0", false, true, "fancymenu.decoration_overlays.confetti.amount"));
    public final Property.StringProperty confettiParticleCap = putProperty(Property.stringProperty("confetti_particle_cap", DEFAULT_SETTLED_CAP, false, true, "fancymenu.decoration_overlays.confetti.particle_cap"));
    public final Property<Boolean> confettiColorMixMode = putProperty(Property.booleanProperty("confetti_color_mix_mode", true, "fancymenu.decoration_overlays.confetti.color_mix"));
    public final Property<Boolean> confettiMouseClickMode = putProperty(Property.booleanProperty("confetti_mouse_click_mode", false, "fancymenu.decoration_overlays.confetti.mouse_click_mode"));
    public final Property.ColorProperty confettiColorHex = putProperty(Property.hexColorProperty("confetti_color_hex", "#FFFFFF", true, "fancymenu.decoration_overlays.confetti.color"));

    protected final ConfettiOverlay overlay = new ConfettiOverlay(0, 0);
    protected String lastScaleString = null;
    protected String lastFallSpeedString = null;
    protected String lastDensityString = null;
    protected String lastAmountString = null;
    protected String lastCapString = null;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.confettiColorMixMode.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SHUFFLE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.color_mix.desc")));

        this.confettiColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.color.desc")));

        this.confettiScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.scale.desc")));

        this.confettiFallSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.fall_speed.desc")));

        this.confettiBurstDensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.DENSITY_MEDIUM)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.density.desc")));

        this.confettiBurstAmount.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.FORMAT_LIST_NUMBERED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.amount.desc")));

        this.confettiParticleCap.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.CHECKLIST)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.particle_cap.desc")));

        this.confettiMouseClickMode.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.MOUSE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.mouse_click_mode.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setColorMixEnabled(this.confettiColorMixMode.tryGetNonNullElse(true));
        this.overlay.setAutoSpawnEnabled(!this.confettiMouseClickMode.tryGetNonNullElse(false));

        this.overlay.setBaseColor(this.confettiColorHex.getDrawable().getColorInt());

        String scaleString = this.confettiScale.getString();
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

        String fallSpeedString = this.confettiFallSpeed.getString();
        if (fallSpeedString == null) fallSpeedString = "1.0";
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

        String densityString = this.confettiBurstDensity.getString();
        if (densityString == null) densityString = "1.0";
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

        String amountString = this.confettiBurstAmount.getString();
        if (amountString == null) amountString = "1.0";
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

        String capString = this.confettiParticleCap.getString();
        if (capString == null) capString = DEFAULT_SETTLED_CAP;
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
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.confettiMouseClickMode.tryGetNonNullElse(false) && button == 0) {
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
