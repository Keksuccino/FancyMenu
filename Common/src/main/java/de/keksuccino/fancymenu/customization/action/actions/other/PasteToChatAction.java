package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinChatScreen;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinMinecraft;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PasteToChatAction extends Action {

    public PasteToChatAction() {
        super("paste_to_chat");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            String msg;
            boolean append = true;
            if (value.toLowerCase().startsWith("true:") || value.toLowerCase().startsWith("false:")) {
                msg = value.split(":", 2)[1];
                if (value.toLowerCase().startsWith("false:")) {
                    append = false;
                }
            } else {
                msg = value;
            }
            msg = StringUtils.convertFormatCodes(msg, "§", "&");
            if (Minecraft.getInstance().level != null) {
                if (Minecraft.getInstance().player != null) {
                    Screen s = Minecraft.getInstance().screen;
                    if (!(s instanceof ChatScreen)) {
                        ((IMixinMinecraft)Minecraft.getInstance()).openChatScreenFancyMenu(msg);
                    } else {
                        if (append) {
                            ((IMixinChatScreen)s).getInputFancyMenu().insertText(msg);
                        } else {
                            ((IMixinChatScreen)s).getInputFancyMenu().setValue(msg);
                        }
                    }
                }
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.helper.buttonaction.paste_to_chat");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.helper.buttonaction.paste_to_chat.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.helper.buttonaction.paste_to_chat.value.desc");
    }

    @Override
    public String getValueExample() {
        return "true:Hi my name is Fred.";
    }

}
