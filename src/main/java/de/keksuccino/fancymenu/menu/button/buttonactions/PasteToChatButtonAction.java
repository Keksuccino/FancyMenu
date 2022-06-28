package de.keksuccino.fancymenu.menu.button.buttonactions;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.mixin.client.IMixinChatScreen;
import de.keksuccino.fancymenu.mixin.client.IMixinMinecraft;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

public class PasteToChatButtonAction extends ButtonActionContainer {

    public PasteToChatButtonAction() {
        super("fancymenu_buttonaction_paste_to_chat");
    }

    @Override
    public String getAction() {
        return "paste_to_chat";
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(String value) {
        if (value != null) {
            String msg;
            boolean append = true;
            if (value.toLowerCase().startsWith("true:") || value.toLowerCase().startsWith("false:")) {
                msg = value.split("[:]", 2)[1];
                if (value.toLowerCase().startsWith("false:")) {
                    append = false;
                }
            } else {
                msg = value;
            }
            if (Minecraft.getInstance().level != null) {
                if (Minecraft.getInstance().player != null) {
                    Screen s = Minecraft.getInstance().screen;
                    if ((s == null) || !(s instanceof ChatScreen)) {
                        ((IMixinMinecraft)Minecraft.getInstance()).openChatScreenFancyMenu(msg);
                    } else if (s instanceof ChatScreen) {
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
    public String getActionDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.paste_to_chat.desc");
    }

    @Override
    public String getValueDescription() {
        return Locals.localize("fancymenu.helper.buttonaction.paste_to_chat.value.desc");
    }

    @Override
    public String getValueExample() {
        return "true:Hi my name is Fred.";
    }

}
