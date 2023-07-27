package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class ExecuteTerminalCommandAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public ExecuteTerminalCommandAction() {
        super("runcmd");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            runCommand(value);
        }
    }

    private static void runCommand(String command) {
        try {
            if (command != null) {
                command = command.replace("];", "%e%;");
                command = command.replace("[windows:", "%s%windows:");
                command = command.replace("[macos:", "%s%macos:");
                command = command.replace("[linux:", "%s%linux:");
                if (command.contains("%s%") && command.contains("%e%;")) {
                    String s = null;
                    if (isMacOS()) {
                        if (command.contains("%s%macos:")) {
                            s = command.split("%s%macos:", 2)[1];
                        }
                    } else if (isWindows()) {
                        if (command.contains("%s%windows:")) {
                            s = command.split("%s%windows:", 2)[1];
                        }
                    } else {
                        if (command.contains("%s%linux:")) {
                            s = command.split("%s%linux:", 2)[1];
                        }
                    }
                    if (s != null) {
                        if (s.contains("%e%;")) {
                            s = s.split("%e%;", 2)[0];
                            Runtime.getRuntime().exec(s);
                        }
                    } else {
                        LOGGER.error("[FANCYMENU] Failed to execute CMD/Terminal command! Missing OS name!");
                    }
                } else {
                    Runtime.getRuntime().exec(command);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static boolean isMacOS() {
        return Minecraft.ON_OSX;
    }

    private static boolean isWindows() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        return (s.contains("win"));
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.runcmd");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.runcmd.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.runcmd.desc.value");
    }

    @Override
    public String getValueExample() {
        return "[windows:run.bat]; [macos:./Run]; [linux:./run.sh];";
    }

}
