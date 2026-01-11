package de.keksuccino.fancymenu.util.rendering.ui.tooltip;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class TooltipHandler implements Renderable {

    public static final TooltipHandler INSTANCE = new TooltipHandler();

    private final List<HandledTooltip> tooltips = new ArrayList<>();
    private final Map<AbstractWidget, HandledTooltip> widgetTooltips = new HashMap<>();

    private TooltipHandler() {
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        HandledTooltip renderTooltip = null;
        for (HandledTooltip t : new ArrayList<>(this.tooltips)) {
            if (t.shouldRender.getAsBoolean()) {
                renderTooltip = t;
            }
            t.remove();
        }
        if (renderTooltip != null) {
            renderTooltip.tooltip.render(graphics, mouseX, mouseY, partial);
        }
    }

    @Deprecated
    public HandledTooltip addWidgetTooltip(@NotNull AbstractWidget widget, @NotNull Tooltip tooltip, boolean unusedBoolean1, boolean unusedBoolean2) {
        return addRenderTickWidgetTooltip(widget, tooltip);
    }

    public HandledTooltip addRenderTickWidgetTooltip(@NotNull AbstractWidget widget, @NotNull Tooltip tooltip) {
        if (this.widgetTooltips.containsKey(widget)) {
            this.removeTooltip(this.widgetTooltips.get(widget));
        }
        HandledTooltip t = this.addRenderTickTooltip(tooltip, () -> widget.isHovered() && widget.visible);
        t.widget = widget;
        this.widgetTooltips.put(widget, t);
        return t;
    }

    @Deprecated
    public HandledTooltip addTooltip(@NotNull Tooltip tooltip, @NotNull BooleanSupplier shouldRender, boolean unusedBoolean1, boolean unusedBoolean2) {
        return addRenderTickTooltip(tooltip, shouldRender);
    }

    public HandledTooltip addRenderTickTooltip(@NotNull Tooltip tooltip, @NotNull BooleanSupplier shouldRender) {
        HandledTooltip t = new HandledTooltip(this, tooltip, shouldRender);
        this.tooltips.add(t);
        return t;
    }

    public void removeTooltip(HandledTooltip tooltip) {
        this.tooltips.remove(tooltip);
        if (tooltip.widget != null) {
            this.widgetTooltips.remove(tooltip.widget);
        }
    }

    public static class HandledTooltip {

        private final TooltipHandler parent;
        public final Tooltip tooltip;
        public final BooleanSupplier shouldRender;
        protected AbstractWidget widget = null;

        private HandledTooltip(TooltipHandler parent, Tooltip tooltip, BooleanSupplier shouldRender) {
            this.parent = parent;
            this.tooltip = tooltip;
            this.shouldRender = shouldRender;
        }

        /** Removes the tooltip from its handler. **/
        public void remove() {
            this.parent.removeTooltip(this);
        }

    }

}
