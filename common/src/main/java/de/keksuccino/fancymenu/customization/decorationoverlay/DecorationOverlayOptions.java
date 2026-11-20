package de.keksuccino.fancymenu.customization.decorationoverlay;

import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Options many decoration overlays share, so they look and behave the same in all overlays: {@link Amount}, {@link Size} and {@link Look}.<br>
 * Create them as fields of the overlay, which registers their property, and add their entries in {@code initConfigMenu}:
 *
 * <pre>{@code
 * public final DecorationOverlayOptions.Amount amount = new DecorationOverlayOptions.Amount(this);
 * public final DecorationOverlayOptions.Size size = new DecorationOverlayOptions.Size(this);
 * public final DecorationOverlayOptions.Look look = new DecorationOverlayOptions.Look(this, DecorationOverlayLook.DETAILED);
 *
 * protected void initConfigMenu(ContextMenu menu, LayoutEditorScreen editor) {
 *     this.amount.addEntry(menu, this);
 *     this.size.addEntry(menu, this);
 *     this.look.addEntry(menu, this);
 * }
 * }</pre>
 *
 * Read them while updating: {@code amount.getFactor()}, {@code size.get()}, {@code look.isSimple()}.
 */
public final class DecorationOverlayOptions {

    private DecorationOverlayOptions() {
    }

    /**
     * How many things an overlay shows, as a percentage from 25% to 300% (a factor from 0.25 to 3.0). Edited with a slider.
     * Serialized as the whole percentage.
     */
    public static final class Amount {

        public static final String DEFAULT_KEY = "amount";
        public static final int MIN_PERCENT = 25;
        public static final int MAX_PERCENT = 300;
        public static final int STEP_PERCENT = 25;
        public static final int DEFAULT_PERCENT = 100;

        @NotNull
        public final Property.IntegerProperty property;
        @NotNull
        private final String descriptionKey;

        public Amount(@NotNull PropertyHolder overlay) {
            this(overlay, DEFAULT_KEY, DEFAULT_PERCENT, "fancymenu.decoration_overlay.amount", "fancymenu.decoration_overlay.amount.desc");
        }

        /**
         * For overlays with more than one amount (like an amount of flowers) or a different default. Keep the label's {@code (%)}.
         */
        public Amount(@NotNull PropertyHolder overlay, @NotNull String key, int defaultPercent, @NotNull String labelKey, @NotNull String descriptionKey) {
            Property.NumericInputBehavior<Integer> behavior = Property.NumericInputBehavior.<Integer>builder().rangeInput(MIN_PERCENT, MAX_PERCENT).step(STEP_PERCENT).build();
            this.property = overlay.putProperty(Property.integerProperty(key, defaultPercent, labelKey, behavior));
            this.descriptionKey = descriptionKey;
        }

        /**
         * The amount in percent, from {@value #MIN_PERCENT} to {@value #MAX_PERCENT}.
         */
        public int getPercent() {
            return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, this.property.getInteger()));
        }

        /**
         * The amount as a factor, from 0.25 to 3.0. 1.0 is the normal amount.
         */
        public float getFactor() {
            return this.getPercent() / 100.0F;
        }

        public void addEntry(@NotNull ContextMenu menu, @NotNull ContextMenuBuilder<? extends PropertyHolder> overlay) {
            this.property.buildContextMenuEntryAndAddTo(menu, overlay)
                    .setIcon(MaterialIcons.DENSITY_MEDIUM)
                    .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(this.descriptionKey)));
        }

    }

    /**
     * A whole-number size factor from 1 to 3 ("1x", "2x", "3x") for overlay sprites, so every art pixel stays square.
     * Cycles through its values in the context menu. Serialized as the number.
     */
    public static final class Size {

        public static final String DEFAULT_KEY = "size";
        public static final int MIN = 1;
        public static final int MAX = 3;
        private static final List<Integer> VALUES = List.of(1, 2, 3);

        @NotNull
        public final Property.IntegerProperty property;
        @NotNull
        private final String descriptionKey;

        public Size(@NotNull PropertyHolder overlay) {
            this(overlay, DEFAULT_KEY, "fancymenu.decoration_overlay.size", "fancymenu.decoration_overlay.size.desc");
        }

        /**
         * @param labelKey needs a {@code %s} for the size, like {@code "Size: %s"}
         */
        public Size(@NotNull PropertyHolder overlay, @NotNull String key, @NotNull String labelKey, @NotNull String descriptionKey) {
            this.property = overlay.putProperty(Property.integerProperty(key, MIN, labelKey));
            this.property.setContextMenuEntrySupplier((property, builder, menu) -> builder.buildGenericCycleContextMenuEntry(menu, "menu_entry_" + key, VALUES,
                    consumes -> consumes.getProperty(key) instanceof Property.IntegerProperty,
                    consumes -> (consumes.getProperty(key) instanceof Property.IntegerProperty resolved) ? clampSize(resolved.getInteger()) : MIN,
                    (holder, value) -> {
                        if ((holder.getProperty(key) instanceof Property.IntegerProperty resolved) && (value != null)) resolved.set(value);
                    },
                    (contextMenu, entry, value) -> Component.translatable(labelKey, Component.translatable("fancymenu.decoration_overlay.size.value", value).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())))));
            this.descriptionKey = descriptionKey;
        }

        /**
         * The size factor, from 1 to 3.
         */
        public int get() {
            return clampSize(this.property.getInteger());
        }

        private static int clampSize(int size) {
            return Math.max(MIN, Math.min(MAX, size));
        }

        public void addEntry(@NotNull ContextMenu menu, @NotNull ContextMenuBuilder<? extends PropertyHolder> overlay) {
            this.property.buildContextMenuEntryAndAddTo(menu, overlay)
                    .setIcon(MaterialIcons.STRAIGHTEN)
                    .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(this.descriptionKey)));
        }

    }

    /**
     * Picks between the {@link DecorationOverlayLook#DETAILED Detailed} and the {@link DecorationOverlayLook#SIMPLE Simple} look of an overlay.
     * Cycles through its values in the context menu. Serialized as {@code detailed} or {@code simple}.
     */
    public static final class Look {

        public static final String DEFAULT_KEY = "look";

        @NotNull
        public final Property<DecorationOverlayLook> property;
        @NotNull
        private final DecorationOverlayLook defaultLook;
        @NotNull
        private final String descriptionKey;

        public Look(@NotNull PropertyHolder overlay, @NotNull DecorationOverlayLook defaultLook) {
            this(overlay, DEFAULT_KEY, defaultLook, "fancymenu.decoration_overlay.look", "fancymenu.decoration_overlay.look.desc");
        }

        /**
         * @param labelKey needs a {@code %s} for the look, like {@code "Look: %s"}
         */
        public Look(@NotNull PropertyHolder overlay, @NotNull String key, @NotNull DecorationOverlayLook defaultLook, @NotNull String labelKey, @NotNull String descriptionKey) {
            this.property = overlay.putProperty(Property.enumProperty(key, defaultLook, labelKey, DecorationOverlayLook::getLocalizationKey));
            this.defaultLook = defaultLook;
            this.descriptionKey = descriptionKey;
        }

        @NotNull
        public DecorationOverlayLook get() {
            return this.property.tryGetNonNullElse(this.defaultLook);
        }

        public boolean isSimple() {
            return this.get() == DecorationOverlayLook.SIMPLE;
        }

        public boolean isDetailed() {
            return this.get() == DecorationOverlayLook.DETAILED;
        }

        public void addEntry(@NotNull ContextMenu menu, @NotNull ContextMenuBuilder<? extends PropertyHolder> overlay) {
            this.property.buildContextMenuEntryAndAddTo(menu, overlay)
                    .setIcon(MaterialIcons.STYLE)
                    .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(this.descriptionKey)));
        }

    }

}
