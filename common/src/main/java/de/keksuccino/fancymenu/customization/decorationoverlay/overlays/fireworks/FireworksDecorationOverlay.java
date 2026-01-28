package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.FireworksOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class FireworksDecorationOverlay extends AbstractDecorationOverlay<FireworksDecorationOverlay> {

    public final Property.FloatProperty fireworksScale = putProperty(Property.floatProperty("fireworks_scale", 1.0F, "fancymenu.decoration_overlays.fireworks.scale"));
    public final Property.FloatProperty fireworksExplosionSize = putProperty(Property.floatProperty("fireworks_explosion_size", 1.0F, "fancymenu.decoration_overlays.fireworks.explosion_size"));
    public final Property.FloatProperty fireworksAmount = putProperty(Property.floatProperty("fireworks_amount", 1.0F, "fancymenu.decoration_overlays.fireworks.amount"));
    public final Property<Boolean> fireworksShowRockets = putProperty(Property.booleanProperty("fireworks_show_rockets", true, "fancymenu.decoration_overlays.fireworks.show_rockets"));

    protected final FireworksOverlay overlay = new FireworksOverlay(0, 0);

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.fireworksShowRockets.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.ROCKET_LAUNCH)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.show_rockets.desc")));

        this.fireworksScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.scale.desc")));

        this.fireworksExplosionSize.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.EXPLOSION)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size.desc")));

        this.fireworksAmount.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.FORMAT_LIST_NUMBERED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.amount.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.overlay.setRocketTrailEnabled(this.fireworksShowRockets.tryGetNonNullElse(true));

        this.overlay.setScale(this.fireworksScale.getFloat());
        this.overlay.setExplosionScale(this.fireworksExplosionSize.getFloat());
        this.overlay.setAmountMultiplier(this.fireworksAmount.getFloat());

        this.overlay.setWidth(getScreenWidth());
        this.overlay.setHeight(getScreenHeight());
        this.overlay.render(graphics, mouseX, mouseY, partial);

    }

}
