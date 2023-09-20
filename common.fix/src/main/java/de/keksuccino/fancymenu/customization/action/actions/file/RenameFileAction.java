package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class RenameFileAction extends Action {

    public RenameFileAction() {
        super("renamefile");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            if (value.contains(";")) {
                try {
                    String path = cleanPath(value.split(";", 2)[0]);
                    String name = value.split(";", 2)[1];
                    File f = new File(path);
                    if (f.exists()) {
                        String parent = f.getParent();
                        File renameTo = new File(name);
                        if (parent != null) {
                            renameTo = new File(parent + "/" + name);
                        }
                        if (renameTo.exists()) return;
                        if (f.isFile()) {
                            FileUtils.moveFile(f, renameTo);
                        } else if (f.isDirectory()) {
                            FileUtils.moveDirectory(f, renameTo);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.renamefile");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.renamefile.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.renamefile.desc.value");
    }

    @Override
    public String getValueExample() {
        return "some/path/example.png;new_name.png";
    }

}
