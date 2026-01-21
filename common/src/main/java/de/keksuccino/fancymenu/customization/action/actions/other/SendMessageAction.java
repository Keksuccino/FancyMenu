package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalPlayerUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SendMessageAction extends Action {

    public SendMessageAction() {
        super("sendmessage");
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
        if (value != null) {
            value = StringUtils.convertFormatCodes(value, "§", "&");
            if ((Minecraft.getInstance().level != null) && (Minecraft.getInstance().player != null)) {
                if (value.startsWith("/")) {
                    value = value.substring(1);
                    LocalPlayerUtils.sendPlayerCommand(Minecraft.getInstance().player, value);
                } else {
                    LocalPlayerUtils.sendPlayerChatMessage(Minecraft.getInstance().player, value);
                }
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.sendmessage");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.sendmessage.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.sendmessage.desc.value");
    }

    @Override
    public String getValuePreset() {
        return "This is an example chat message!";
    }

}
