package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopyToClipboardAction extends Action {

    public CopyToClipboardAction() {
        super("copytoclipboard");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            Minecraft.getInstance().keyboardHandler.setClipboard(value);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.copytoclipboard");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.copytoclipboard.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.copytoclipboard.desc.value");
    }

    @Override
    public String getValueExample() {
        return "This text gets copied to the clipboard!";
    }

}
