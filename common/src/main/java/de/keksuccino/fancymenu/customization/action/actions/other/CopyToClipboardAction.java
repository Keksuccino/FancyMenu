package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
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
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.copy_to_clipboard");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.copy_to_clipboard.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.copy_to_clipboard.desc.value");
    }

    @Override
    public String getValuePreset() {
        return "This text gets copied to the clipboard!";
    }

}
