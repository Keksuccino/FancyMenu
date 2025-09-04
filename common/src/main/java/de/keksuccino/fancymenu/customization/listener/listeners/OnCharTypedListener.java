package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.events.screen.ScreenCharTypedEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class OnCharTypedListener extends AbstractListener {

    protected Character lastTypedChar = null;

    public OnCharTypedListener() {

        super("keyboard_char_typed");

        // Register @EventListeners to the EventHandler
        EventHandler.INSTANCE.registerListenersOf(this);

    }

    @EventListener
    private void onCharTyped(ScreenCharTypedEvent e) {

        // Update cached typed char before notifying instances, so they can use the up-to-date char
        this.lastTypedChar = e.getCharacter();

        this.notifyAllInstances();

    }

    @Override
    protected void registerCustomVariables(List<CustomVariable> registry) {

        // $$char
        registry.add(new CustomVariable("char", () -> {
            if (this.lastTypedChar == null) return "ERROR";
            return this.lastTypedChar.toString();
        }));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_char_typed");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_char_typed.desc"));
    }

}
