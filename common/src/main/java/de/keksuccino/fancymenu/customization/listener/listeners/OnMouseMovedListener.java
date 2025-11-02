package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.events.screen.ScreenMouseMoveEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class OnMouseMovedListener extends AbstractListener {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private Double lastMouseX;
    @Nullable
    private Double lastMouseY;
    @Nullable
    private Double lastDeltaX;
    @Nullable
    private Double lastDeltaY;

    public OnMouseMovedListener() {

        super("mouse_moved");

        EventHandler.INSTANCE.registerListenersOf(this);

    }

    @EventListener
    public void onMouseMoved(@NotNull ScreenMouseMoveEvent event) {

        // Cache latest data before notifying listeners
        this.lastMouseX = event.getMouseX();
        this.lastMouseY = event.getMouseY();
        this.lastDeltaX = event.getDeltaX();
        this.lastDeltaY = event.getDeltaY();

        this.notifyAllInstances();

    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {

        list.add(new CustomVariable("mouse_pos_x", () -> this.formatValue(this.lastMouseX)));
        list.add(new CustomVariable("mouse_pos_y", () -> this.formatValue(this.lastMouseY)));
        list.add(new CustomVariable("mouse_move_delta_x", () -> this.formatValue(this.lastDeltaX)));
        list.add(new CustomVariable("mouse_move_delta_y", () -> this.formatValue(this.lastDeltaY)));

    }

    private String formatValue(@Nullable Double value) {
        if (value == null) return "ERROR";
        return Double.toString(value);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_mouse_moved");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_mouse_moved.desc"));
    }

}

