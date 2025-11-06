package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.events.screen.ScreenMouseScrollEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class OnMouseScrolledListener extends AbstractListener {

    @Nullable
    private Double lastScrollDeltaY;

    public OnMouseScrolledListener() {

        super("mouse_scrolled");

        EventHandler.INSTANCE.registerListenersOf(this);

    }

    @EventListener
    public void onMouseScrolled(@NotNull ScreenMouseScrollEvent.Pre event) {

        this.lastScrollDeltaY = event.getScrollDeltaY();

        this.notifyAllInstances();

    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {

        list.add(new CustomVariable("scroll_delta_y", () -> this.formatValue(this.lastScrollDeltaY)));

    }

    private String formatValue(@Nullable Double value) {
        if (value == null) return "ERROR";
        return Double.toString(value);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_mouse_scrolled");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_mouse_scrolled.desc"));
    }

}

