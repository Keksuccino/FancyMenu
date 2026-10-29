package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * {@link ContextMenu} entry label that prefixes a base label with a ✔ (enabled) or ✖ (disabled) state indicator,
 * colored with the success or error color of the active {@link UITheme}.
 * <p>
 * Context menus query entry labels several times per frame (rendering, entry width and tooltip icon position).
 * The supplier keeps returning the same {@link Component} instance until the state or the indicator color changes,
 * because the UI font caches text measurements per component instance. Building a new component on every call
 * would make every measurement a cache miss and push other entries out of that cache.
 */
public class StateIndicatorLabelSupplier implements ContextMenu.Supplier<Component> {

    @NotNull
    private final Component label;
    @NotNull
    private final BooleanSupplier stateSupplier;
    @NotNull
    private final Supplier<UITheme> themeSupplier;
    @Nullable
    private Component cachedLabel;
    private boolean cachedState;
    private int cachedIndicatorColor;

    /**
     * @param label the label to show next to the state indicator
     * @param stateSupplier returns {@code true} while the thing the entry represents is enabled
     */
    public StateIndicatorLabelSupplier(@NotNull Component label, @NotNull BooleanSupplier stateSupplier) {
        this(label, stateSupplier, UIBase::getUITheme);
    }

    StateIndicatorLabelSupplier(@NotNull Component label, @NotNull BooleanSupplier stateSupplier, @NotNull Supplier<UITheme> themeSupplier) {
        this.label = Objects.requireNonNull(label);
        this.stateSupplier = Objects.requireNonNull(stateSupplier);
        this.themeSupplier = Objects.requireNonNull(themeSupplier);
    }

    @Override
    public Component get(ContextMenu menu, ContextMenu.ContextMenuEntry<?> entry) {
        boolean state = this.stateSupplier.getAsBoolean();
        UITheme theme = this.themeSupplier.get();
        int indicatorColor = state ? theme.success_color.getColorInt() : theme.error_color.getColorInt();
        if ((this.cachedLabel == null) || (state != this.cachedState) || (indicatorColor != this.cachedIndicatorColor)) {
            this.cachedLabel = buildLabel(this.label, state, indicatorColor);
            this.cachedState = state;
            this.cachedIndicatorColor = indicatorColor;
        }
        return this.cachedLabel;
    }

    @NotNull
    private static MutableComponent buildLabel(@NotNull Component label, boolean state, int indicatorColor) {
        // The empty root keeps the indicator color from leaking into the label, since siblings inherit their parent's style
        MutableComponent stateLabel = Component.empty();
        stateLabel.append(Component.literal(state ? "✔" : "✖").withStyle(Style.EMPTY.withColor(indicatorColor)));
        stateLabel.append(Component.literal(" "));
        stateLabel.append(label);
        return stateLabel;
    }

}
