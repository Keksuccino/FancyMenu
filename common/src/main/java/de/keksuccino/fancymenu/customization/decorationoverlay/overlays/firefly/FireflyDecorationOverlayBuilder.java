package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

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

public class FireflyDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<FireflyDecorationOverlay> {

    private static final String FIREFLY_COLOR_KEY = "firefly_color_hex";
    private static final String FIREFLY_INTENSITY_KEY = "firefly_intensity";
    private static final String FIREFLY_GROUP_DENSITY_KEY = "firefly_group_density";
    private static final String FIREFLY_GROUP_AMOUNT_KEY = "firefly_group_amount";
    private static final String FIREFLY_GROUP_SIZE_KEY = "firefly_group_size";
    private static final String FIREFLY_SCALE_KEY = "firefly_scale";
    private static final String FIREFLY_FOLLOW_MOUSE_KEY = "firefly_follow_mouse";
    private static final String FIREFLY_LANDING_KEY = "firefly_landing";

    public FireflyDecorationOverlayBuilder() {
        super("fireflies");
    }

    @Override
    public @NotNull FireflyDecorationOverlay buildDefaultInstance() {
        return new FireflyDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull FireflyDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {

        instanceToWrite.fireflyColorHex = Objects.requireNonNullElse(deserializeFrom.getValue(FIREFLY_COLOR_KEY), instanceToWrite.fireflyColorHex);
        String legacyIntensity = deserializeFrom.getValue(FIREFLY_INTENSITY_KEY);
        String groupDensity = deserializeFrom.getValue(FIREFLY_GROUP_DENSITY_KEY);
        String groupAmount = deserializeFrom.getValue(FIREFLY_GROUP_AMOUNT_KEY);
        instanceToWrite.fireflyGroupDensity = Objects.requireNonNullElse(groupDensity, Objects.requireNonNullElse(legacyIntensity, instanceToWrite.fireflyGroupDensity));
        instanceToWrite.fireflyGroupAmount = Objects.requireNonNullElse(groupAmount, Objects.requireNonNullElse(legacyIntensity, instanceToWrite.fireflyGroupAmount));
        instanceToWrite.fireflyGroupSize = Objects.requireNonNullElse(deserializeFrom.getValue(FIREFLY_GROUP_SIZE_KEY), instanceToWrite.fireflyGroupSize);
        instanceToWrite.fireflyScale = Objects.requireNonNullElse(deserializeFrom.getValue(FIREFLY_SCALE_KEY), instanceToWrite.fireflyScale);
        instanceToWrite.fireflyFollowMouse = deserializeBoolean(instanceToWrite.fireflyFollowMouse, deserializeFrom.getValue(FIREFLY_FOLLOW_MOUSE_KEY));
        instanceToWrite.fireflyLanding = deserializeBoolean(instanceToWrite.fireflyLanding, deserializeFrom.getValue(FIREFLY_LANDING_KEY));

    }

    @Override
    protected void serialize(@NotNull FireflyDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {

        serializeTo.putProperty(FIREFLY_COLOR_KEY, instanceToSerialize.fireflyColorHex);
        serializeTo.putProperty(FIREFLY_GROUP_DENSITY_KEY, instanceToSerialize.fireflyGroupDensity);
        serializeTo.putProperty(FIREFLY_GROUP_AMOUNT_KEY, instanceToSerialize.fireflyGroupAmount);
        serializeTo.putProperty(FIREFLY_GROUP_SIZE_KEY, instanceToSerialize.fireflyGroupSize);
        serializeTo.putProperty(FIREFLY_SCALE_KEY, instanceToSerialize.fireflyScale);
        serializeTo.putProperty(FIREFLY_FOLLOW_MOUSE_KEY, instanceToSerialize.fireflyFollowMouse);
        serializeTo.putProperty(FIREFLY_LANDING_KEY, instanceToSerialize.fireflyLanding);

    }

    @Override
    protected void buildConfigurationMenu(@NotNull FireflyDecorationOverlay instance, @NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "firefly_follow_mouse",
                        () -> instance.fireflyFollowMouse,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.fireflyFollowMouse = aBoolean;
                        },
                        "fancymenu.decoration_overlays.fireflies.follow_mouse")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.follow_mouse.desc")));

        ContextMenuUtils.addToggleContextMenuEntryTo(menu, "firefly_landing",
                        () -> instance.fireflyLanding,
                        aBoolean -> {
                            editor.history.saveSnapshot();
                            instance.fireflyLanding = aBoolean;
                        },
                        "fancymenu.decoration_overlays.fireflies.landing")
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.landing.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_color", Component.translatable("fancymenu.decoration_overlays.fireflies.color"),
                        () -> instance.fireflyColorHex,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.fireflyColorHex = s;
                        }, true,
                        "#FFE08A", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.color.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_group_amount", Component.translatable("fancymenu.decoration_overlays.fireflies.group_amount"),
                        () -> instance.fireflyGroupAmount,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.fireflyGroupAmount = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_amount.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_group_density", Component.translatable("fancymenu.decoration_overlays.fireflies.intensity"),
                        () -> instance.fireflyGroupDensity,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.fireflyGroupDensity = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.intensity.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_group_size", Component.translatable("fancymenu.decoration_overlays.fireflies.group_size"),
                        () -> instance.fireflyGroupSize,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.fireflyGroupSize = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_size.desc")));

        ContextMenuUtils.addInputContextMenuEntryTo(menu, "firefly_scale", Component.translatable("fancymenu.decoration_overlays.fireflies.scale"),
                        () -> instance.fireflyScale,
                        s -> {
                            editor.history.saveSnapshot();
                            instance.fireflyScale = s;
                        }, true,
                        "1.0", null, false, true, null, null)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.scale.desc")));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.fireflies");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
