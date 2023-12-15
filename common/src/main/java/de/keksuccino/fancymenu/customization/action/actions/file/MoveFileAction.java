package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

public class MoveFileAction extends Action {

    public MoveFileAction() {
        super("movefile");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if ((value != null) && value.contains(";")) {
            try {
                String from = cleanPath(value.split(";", 2)[0]);
                String to = cleanPath(value.split(";", 2)[1]);
                File toFile = new File(to);
                File fromFile = new File(from);
                if (toFile.exists()) return;
                if (fromFile.isFile()) {
                    FileUtils.moveFile(fromFile, toFile);
                } else if (fromFile.isDirectory()) {
                    FileUtils.moveDirectory(fromFile, toFile);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Removes all spaces from the beginning of the path and replaces all backslash characters with normal slash characters.
     */
    private static String cleanPath(String path) {
        int i = 0;
        for (char c : path.toCharArray()) {
            if (c == ' ') {
                i++;
            } else {
                break;
            }
        }
        if (i <= path.length()) {
            return path.substring(i).replace("\\", "/");
        }
        return "";
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Components.translatable("fancymenu.editor.custombutton.config.actiontype.movefile");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.movefile.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Components.translatable("fancymenu.editor.custombutton.config.actiontype.movefile.desc.value");
    }

    @Override
    public String getValueExample() {
        return "old/path/example.png;new/path/example.png";
    }

}
