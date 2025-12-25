package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.ui.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FireworksDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<FireworksDecorationOverlay> {

    private static final String FIREWORKS_SCALE_KEY = "fireworks_scale";
    private static final String FIREWORKS_EXPLOSION_SIZE_KEY = "fireworks_explosion_size";
    private static final String FIREWORKS_AMOUNT_KEY = "fireworks_amount";
    private static final String FIREWORKS_SHOW_ROCKETS_KEY = "fireworks_show_rockets";

    public FireworksDecorationOverlayBuilder() {
        super("fireworks");
    }

    @Override
    public @NotNull FireworksDecorationOverlay buildDefaultInstance() {
        return new FireworksDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull FireworksDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.fireworksScale = Objects.requireNonNullElse(deserializeFrom.getValue(FIREWORKS_SCALE_KEY), instanceToWrite.fireworksScale);
        instanceToWrite.fireworksExplosionSize = Objects.requireNonNullElse(deserializeFrom.getValue(FIREWORKS_EXPLOSION_SIZE_KEY), instanceToWrite.fireworksExplosionSize);
        instanceToWrite.fireworksAmount = Objects.requireNonNullElse(deserializeFrom.getValue(FIREWORKS_AMOUNT_KEY), instanceToWrite.fireworksAmount);
        instanceToWrite.fireworksShowRockets = deserializeBoolean(instanceToWrite.fireworksShowRockets, deserializeFrom.getValue(FIREWORKS_SHOW_ROCKETS_KEY));

    }

    @Override
    protected void serialize(@NotNull FireworksDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(FIREWORKS_SCALE_KEY, instanceToSerialize.fireworksScale);
        serializeTo.putProperty(FIREWORKS_EXPLOSION_SIZE_KEY, instanceToSerialize.fireworksExplosionSize);
        serializeTo.putProperty(FIREWORKS_AMOUNT_KEY, instanceToSerialize.fireworksAmount);
        serializeTo.putProperty(FIREWORKS_SHOW_ROCKETS_KEY, instanceToSerialize.fireworksShowRockets);

    }

    @Override
    protected void buildConfigurationMenu(@NotNull FireworksDecorationOverlay instance, @NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "fireworks_show_rockets",
                        () -> instance.fireworksShowRockets,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.fireworksShowRockets = aBoolean;
                        },
                        "fancymenu.decoration_overlays.fireworks.show_rockets")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.show_rockets.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "fireworks_scale", Component.translatable("fancymenu.decoration_overlays.fireworks.scale"),
                        () -> instance.fireworksScale,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.fireworksScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.scale.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "fireworks_explosion_size", Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size"),
                        () -> instance.fireworksExplosionSize,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.fireworksExplosionSize = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "fireworks_amount", Component.translatable("fancymenu.decoration_overlays.fireworks.amount"),
                        () -> instance.fireworksAmount,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.fireworksAmount = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.amount.desc")));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.fireworks");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
