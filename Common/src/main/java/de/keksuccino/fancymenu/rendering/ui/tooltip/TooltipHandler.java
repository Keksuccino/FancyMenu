package de.keksuccino.fancymenu.rendering.ui.tooltip;

import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import net.minecraft.client.gui.components.AbstractWidget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class TooltipHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final TooltipHandler INSTANCE = new TooltipHandler();

    private final List<HandledTooltip> tooltips = new ArrayList<>();
    private final Map<AbstractWidget, HandledTooltip> widgetTooltips = new HashMap<>();

    public TooltipHandler() {
        EventHandler.INSTANCE.registerListenersOf(this);
    }

    @EventListener(priority = -1000)
    public void onScreenRenderPost(RenderScreenEvent.Post e) {
        for (HandledTooltip t : new ArrayList<>(this.tooltips)) {
            if (t.shouldRender.getAsBoolean()) {
                t.tooltip.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
            }
            if (t.removeAfterScreenRender) {
                t.remove();
            }
        }
    }

    @EventListener(priority = 1000)
    public void onScreenInitResizePre(InitOrResizeScreenEvent.Pre e) {
        for (HandledTooltip t : new ArrayList<>(this.tooltips)) {
            if (t.removeOnScreenInitOrResize) t.remove();
        }
    }

    public HandledTooltip addWidgetTooltip(@NotNull AbstractWidget widget, @NotNull Tooltip tooltip) {
        return this.addWidgetTooltip(widget, tooltip, true, false);
    }

    public HandledTooltip addWidgetTooltip(@NotNull AbstractWidget widget, @NotNull Tooltip tooltip, boolean removeOnScreenInitOrResize, boolean removeAfterScreenRender) {
        if (this.widgetTooltips.containsKey(widget)) {
            this.removeTooltip(this.widgetTooltips.get(widget));
        }
        HandledTooltip t = this.addTooltip(tooltip, widget::isHovered, removeOnScreenInitOrResize, removeAfterScreenRender);
        t.widget = widget;
        this.widgetTooltips.put(widget, t);
        return t;
    }

    public HandledTooltip addTooltip(@NotNull Tooltip tooltip, @NotNull BooleanSupplier shouldRender, boolean removeOnScreenInitOrResize, boolean removeAfterScreenRender) {
        HandledTooltip t = new HandledTooltip(this, tooltip, shouldRender, removeOnScreenInitOrResize, removeAfterScreenRender);
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
        public final boolean removeOnScreenInitOrResize;
        public final boolean removeAfterScreenRender;
        protected AbstractWidget widget = null;

        private HandledTooltip(TooltipHandler parent, Tooltip tooltip, BooleanSupplier shouldRender, boolean removeOnScreenInitOrResize, boolean removeAfterScreenRender) {
            this.parent = parent;
            this.tooltip = tooltip;
            this.shouldRender = shouldRender;
            this.removeOnScreenInitOrResize = removeOnScreenInitOrResize;
            this.removeAfterScreenRender = removeAfterScreenRender;
        }

        /** Removes the tooltip from its handler. **/
        public void remove() {
            this.parent.removeTooltip(this);
        }

    }

}
