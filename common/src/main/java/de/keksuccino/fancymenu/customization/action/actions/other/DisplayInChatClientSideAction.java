package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.rendering.text.ComponentParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisplayInChatClientSideAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public DisplayInChatClientSideAction() {
        super("display_in_chat_client_side");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if ((value == null) || value.isBlank()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Component message = this.deserializeComponent(value.trim());
        minecraft.gui.getChat().addMessage(message);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.display_in_chat_client_side");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.display_in_chat_client_side.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.display_in_chat_client_side.value");
    }

    @Override
    public String getValuePreset() {
        return "&aThis text only shows up for you!";
    }

    private Component deserializeComponent(@NotNull String raw) {
        return ComponentParser.fromJsonOrPlainText(raw);
    }

}
