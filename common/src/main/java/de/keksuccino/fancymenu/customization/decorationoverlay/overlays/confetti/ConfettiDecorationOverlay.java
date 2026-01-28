package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.ConfettiOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class ConfettiDecorationOverlay extends AbstractDecorationOverlay<ConfettiDecorationOverlay> {

    public static final int DEFAULT_SETTLED_CAP = 500;

    public final Property.FloatProperty confettiScale = putProperty(Property.floatProperty("confetti_scale", 1.0F, "fancymenu.decoration_overlays.confetti.scale"));
    public final Property.FloatProperty confettiFallSpeed = putProperty(Property.floatProperty("confetti_fall_speed", 1.0F, "fancymenu.decoration_overlays.confetti.fall_speed"));
    public final Property.FloatProperty confettiBurstDensity = putProperty(Property.floatProperty("confetti_burst_density", 1.0F, "fancymenu.decoration_overlays.confetti.density"));
    public final Property.FloatProperty confettiBurstAmount = putProperty(Property.floatProperty("confetti_burst_amount", 1.0F, "fancymenu.decoration_overlays.confetti.amount"));
    public final Property.IntegerProperty confettiParticleCap = putProperty(Property.integerProperty("confetti_particle_cap", DEFAULT_SETTLED_CAP, "fancymenu.decoration_overlays.confetti.particle_cap"));
    public final Property<Boolean> confettiColorMixMode = putProperty(Property.booleanProperty("confetti_color_mix_mode", true, "fancymenu.decoration_overlays.confetti.color_mix"));
    public final Property<Boolean> confettiMouseClickMode = putProperty(Property.booleanProperty("confetti_mouse_click_mode", false, "fancymenu.decoration_overlays.confetti.mouse_click_mode"));
    public final Property.ColorProperty confettiColorHex = putProperty(Property.hexColorProperty("confetti_color_hex", "#FFFFFF", true, "fancymenu.decoration_overlays.confetti.color"));

    protected final ConfettiOverlay overlay = new ConfettiOverlay(0, 0);

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

        this.overlay.setScale(this.confettiScale.getFloat());
        this.overlay.setFallSpeedMultiplier(this.confettiFallSpeed.getFloat());
        this.overlay.setBurstDensity(this.confettiBurstDensity.getFloat());
        this.overlay.setBurstAmount(this.confettiBurstAmount.getFloat());
        this.overlay.setSettledCapOverride(this.confettiParticleCap.getInteger());

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
