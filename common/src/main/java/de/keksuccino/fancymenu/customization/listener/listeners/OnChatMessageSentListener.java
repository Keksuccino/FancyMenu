package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnChatMessageSentListener extends AbstractListener {

    @Nullable
    private String lastMessageString;
    @Nullable
    private String lastMessageJson;

    public OnChatMessageSentListener() {
        super("chat_message_sent");
    }

    public void onChatMessageSent(@NotNull Component message) {
        this.lastMessageString = message.getString();
        this.lastMessageJson = this.serializeComponent(message);
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("chat_message_string", () -> this.lastMessageString != null ? this.lastMessageString : "ERROR"));
        list.add(new CustomVariable("chat_message_component", () -> this.lastMessageJson != null ? this.lastMessageJson : "ERROR"));
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
        return Component.translatable("fancymenu.listeners.on_chat_message_sent");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_chat_message_sent.desc"));
    }
}