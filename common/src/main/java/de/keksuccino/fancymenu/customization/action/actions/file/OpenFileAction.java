package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

public class OpenFileAction extends Action {

    public OpenFileAction() {
        super("openfile");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            File f = new File(value.replace("\\", "/"));
            if (f.exists()) {
                FileUtils.openFile(f);
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.openfile");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.openfile.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.openfile.desc.value");
    }

    @Override
    public String getValueExample() {
        return "config/fancymenu/example.txt";
    }

}
