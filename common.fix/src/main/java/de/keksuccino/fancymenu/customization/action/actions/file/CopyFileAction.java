package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

public class CopyFileAction extends Action {

    public CopyFileAction() {
        super("copyfile");
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
                if (fromFile.isFile()) {
                    FileUtils.copyFile(fromFile, toFile);
                } else if (fromFile.isDirectory()) {
                    FileUtils.copyDirectory(fromFile, toFile);
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
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.copyfile");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.copyfile.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.copyfile.desc.value");
    }

    @Override
    public String getValueExample() {
        return "some/path/example.png;target/path/example.png";
    }

}
