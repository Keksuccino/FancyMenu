package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

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

public class ConfettiDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<ConfettiDecorationOverlay> {

    private static final String CONFETTI_SCALE_KEY = "confetti_scale";
    private static final String CONFETTI_DENSITY_KEY = "confetti_burst_density";
    private static final String CONFETTI_AMOUNT_KEY = "confetti_burst_amount";
    private static final String CONFETTI_PARTICLE_CAP_KEY = "confetti_particle_cap";
    private static final String CONFETTI_COLOR_MIX_KEY = "confetti_color_mix_mode";
    private static final String CONFETTI_COLOR_KEY = "confetti_color_hex";
    private static final String CONFETTI_MOUSE_CLICK_KEY = "confetti_mouse_click_mode";

    public ConfettiDecorationOverlayBuilder() {
        super("confetti");
    }

    @Override
    public @NotNull ConfettiDecorationOverlay buildDefaultInstance() {
        return new ConfettiDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull ConfettiDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.confettiScale = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_SCALE_KEY), instanceToWrite.confettiScale);
        instanceToWrite.confettiBurstDensity = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_DENSITY_KEY), instanceToWrite.confettiBurstDensity);
        instanceToWrite.confettiBurstAmount = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_AMOUNT_KEY), instanceToWrite.confettiBurstAmount);
        instanceToWrite.confettiParticleCap = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_PARTICLE_CAP_KEY), instanceToWrite.confettiParticleCap);
        instanceToWrite.confettiColorMixMode = deserializeBoolean(instanceToWrite.confettiColorMixMode, deserializeFrom.getValue(CONFETTI_COLOR_MIX_KEY));
        instanceToWrite.confettiColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(CONFETTI_COLOR_KEY), instanceToWrite.confettiColorHex);
        instanceToWrite.confettiMouseClickMode = deserializeBoolean(instanceToWrite.confettiMouseClickMode, deserializeFrom.getValue(CONFETTI_MOUSE_CLICK_KEY));

    }

    @Override
    protected void serialize(@NotNull ConfettiDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(CONFETTI_SCALE_KEY, instanceToSerialize.confettiScale);
        serializeTo.putProperty(CONFETTI_DENSITY_KEY, instanceToSerialize.confettiBurstDensity);
        serializeTo.putProperty(CONFETTI_AMOUNT_KEY, instanceToSerialize.confettiBurstAmount);
        serializeTo.putProperty(CONFETTI_PARTICLE_CAP_KEY, instanceToSerialize.confettiParticleCap);
        serializeTo.putProperty(CONFETTI_COLOR_MIX_KEY, instanceToSerialize.confettiColorMixMode);
        serializeTo.putProperty(CONFETTI_COLOR_KEY, instanceToSerialize.confettiColorHex);
        serializeTo.putProperty(CONFETTI_MOUSE_CLICK_KEY, instanceToSerialize.confettiMouseClickMode);

    }

    @Override
    protected void buildConfigurationMenu(@NotNull ConfettiDecorationOverlay instance, @NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "confetti_color_mix_mode",
                        () -> instance.confettiColorMixMode,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.confettiColorMixMode = aBoolean;
                        },
                        "fancymenu.decoration_overlays.confetti.color_mix")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.color_mix.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_color", Component.translatable("fancymenu.decoration_overlays.confetti.color"),
                        () -> instance.confettiColorHex,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.confettiColorHex = s;
                        }, true,
                        "#FFFFFF", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_scale", Component.translatable("fancymenu.decoration_overlays.confetti.scale"),
                        () -> instance.confettiScale,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.confettiScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.scale.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_density", Component.translatable("fancymenu.decoration_overlays.confetti.density"),
                        () -> instance.confettiBurstDensity,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.confettiBurstDensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.density.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_amount", Component.translatable("fancymenu.decoration_overlays.confetti.amount"),
                        () -> instance.confettiBurstAmount,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.confettiBurstAmount = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.amount.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "confetti_particle_cap", Component.translatable("fancymenu.decoration_overlays.confetti.particle_cap"),
                        () -> instance.confettiParticleCap,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.confettiParticleCap = s;
                        }, true,
                        ConfettiDecorationOverlay.DEFAULT_SETTLED_CAP, null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.particle_cap.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "confetti_mouse_click_mode",
                        () -> instance.confettiMouseClickMode,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.confettiMouseClickMode = aBoolean;
                        },
                        "fancymenu.decoration_overlays.confetti.mouse_click_mode")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.mouse_click_mode.desc")));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.confetti");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
