package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.ComponentParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.UUID;

public class OnChatMessageReceivedListener extends AbstractListener {

    @Nullable
    private String lastMessageString;
    @Nullable
    private String lastMessageJson;
    @Nullable
    private UUID lastSenderUuid;
    @Nullable
    private String lastSenderName;

    public OnChatMessageReceivedListener() {
        super("chat_message_received");
    }

    public void onChatMessageReceived(@NotNull Component message, @Nullable UUID senderUuid, @Nullable Component senderName) {
        this.lastMessageString = message.getString();
        this.lastMessageJson = this.serializeComponent(message);
        this.lastSenderUuid = senderUuid;
        this.lastSenderName = (senderName != null) ? senderName.getString() : null;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("chat_message_string", () -> this.lastMessageString != null ? this.lastMessageString : "ERROR"));
        list.add(new CustomVariable("chat_message_component", () -> this.lastMessageJson != null ? this.lastMessageJson : "ERROR"));
        list.add(new CustomVariable("sender_uuid", () -> this.lastSenderUuid != null ? this.lastSenderUuid.toString() : "ERROR"));
        list.add(new CustomVariable("sender_name", () -> this.lastSenderName != null ? this.lastSenderName : "ERROR"));
    }

    @NotNull
    private String serializeComponent(@NotNull Component component) {
        return ComponentParser.toJson(component);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_chat_message_received");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_chat_message_received.desc"));
    }
}