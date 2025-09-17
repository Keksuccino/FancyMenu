package de.keksuccino.fancymenu.customization.listener.listeners;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class OnKeyPressedListener extends AbstractListener {

    @Nullable
    private String lastKeyName;
    @Nullable
    private Integer lastKeycode;
    @Nullable
    private Integer lastScancode;
    @Nullable
    private Integer lastModifiers;

    public OnKeyPressedListener() {

        super("keyboard_key_pressed");

        EventHandler.INSTANCE.registerListenersOf(this);

    }

    @EventListener
    public void onKeyPressed(@NotNull ScreenKeyPressedEvent event) {

        this.lastKeyName = InputConstants.getKey(event.getKeycode(), event.getScancode()).getDisplayName().getString();
        this.lastKeycode = event.getKeycode();
        this.lastScancode = event.getScancode();
        this.lastModifiers = event.getModifiers();

        this.notifyAllInstances();

    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {

        list.add(new CustomVariable("key_name", () -> this.formatString(this.lastKeyName)));
        list.add(new CustomVariable("key_keycode", () -> this.formatInteger(this.lastKeycode)));
        list.add(new CustomVariable("key_scancode", () -> this.formatInteger(this.lastScancode)));
        list.add(new CustomVariable("key_modifiers", () -> this.formatInteger(this.lastModifiers)));

    }

    private String formatString(@Nullable String value) {
        if (value == null || value.isBlank()) return "ERROR";
        return value;
    }

    private String formatInteger(@Nullable Integer value) {
        if (value == null) return "ERROR";
        return Integer.toString(value);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_key_pressed");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_key_pressed.desc"));
    }

}

