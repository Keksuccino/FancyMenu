package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OnElementSpawnedListener extends AbstractListener {

    protected String elementType;
    protected String elementIdentifier;
    protected String targetScreen;

    public OnElementSpawnedListener() {
        super("element_spawned_via_action");
    }

    public void onElementSpawned(@NotNull String elementType, @NotNull String elementIdentifier, @NotNull String targetScreen) {
        this.elementType = elementType;
        this.elementIdentifier = elementIdentifier;
        this.targetScreen = targetScreen;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("element_type", () -> (this.elementType != null) ? this.elementType : "ERROR"));
        list.add(new CustomVariable("element_identifier", () -> (this.elementIdentifier != null) ? this.elementIdentifier : "ERROR"));
        list.add(new CustomVariable("target_screen", () -> (this.targetScreen != null) ? this.targetScreen : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_element_spawned");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_element_spawned.desc"));
    }

}
