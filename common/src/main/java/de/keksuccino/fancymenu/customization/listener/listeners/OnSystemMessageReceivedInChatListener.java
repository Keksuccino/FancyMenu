package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnSystemMessageReceivedInChatListener extends AbstractListener {

    @Nullable
    private String lastFeedbackString;
    @Nullable
    private String lastFeedbackJson;

    public OnSystemMessageReceivedInChatListener() {
        super("system_message_received_in_chat");
    }

    public void onSystemMessageReceivedInChat(@NotNull Component message) {
        this.lastFeedbackString = message.getString();
        this.lastFeedbackJson = this.serializeComponent(message);
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("system_message_string", () -> this.lastFeedbackString != null ? this.lastFeedbackString : "ERROR"));
        list.add(new CustomVariable("system_message_component", () -> this.lastFeedbackJson != null ? this.lastFeedbackJson : "ERROR"));
    }

    @NotNull
    private String serializeComponent(@NotNull Component component) {
        RegistryAccess registryAccess = RegistryAccess.EMPTY;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            registryAccess = minecraft.level.registryAccess();
        } else if ((minecraft.getConnection() != null) && (minecraft.getConnection().registryAccess() != null)) {
            registryAccess = minecraft.getConnection().registryAccess();
        }
        return Component.Serializer.toJson(component, registryAccess);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_system_message_received_in_chat");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_system_message_received_in_chat.desc"));
    }
}
