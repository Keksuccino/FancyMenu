package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

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
            handleCommandValue(value);
        }
    }

    protected static void handleCommandValue(@NotNull String commandValue) {
        try {
            if ((commandValue.contains("[windows:") || commandValue.contains("[macos:") || commandValue.contains("[linux:")) && commandValue.contains("];")) {
                String command;
                if (isMacOS()) {
                    command = getOSCommand(commandValue, "macos");
                } else if (isWindows()) {
                    command = getOSCommand(commandValue, "windows");
                } else {
                    command = getOSCommand(commandValue, "linux");
                }
                if (command != null) {
                    executeTerminalCommand(command);
                } else {
                    LOGGER.error("[FANCYMENU] Failed to execute Terminal/CMD command via action! Missing OS name!");
                }
            } else {
                executeTerminalCommand(commandValue);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Nullable
    protected static String getOSCommand(@NotNull String value, @NotNull String osId) {
        value = value.replace("];", "&%e%&;");
        value = value.replace("[" + osId + ":", "&%s%&" + osId + ":");
        if (value.contains("&%s%&") && value.contains("&%e%&;")) {
            if (value.contains("&%s%&" + osId + ":")) {
                String s = value.split("&%s%&" + osId + ":", 2)[1];
                if (s.contains("&%e%&;")) {
                    s = s.split("&%e%&;", 2)[0];
                    if (s.replace(" ", "").isEmpty()) return null;
                    return s;
                }
            }
        }
        return null;
    }

    protected static void executeTerminalCommand(@NotNull String command) {
        new Thread(() -> {
            BufferedReader reader = null;
            try {
                Logger logger = LogManager.getLogger();
                Process process = Runtime.getRuntime().exec(command);
                if (process != null) {
                    logger.info("[FANCYMENU] Executing Terminal/CMD command via action: " + command);
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String output;
                    while ((output = reader.readLine()) != null) {
                        logger.info("TERMINAL OUT: " + output);
                    }
                    logger.info("[FANCYMENU] Finished executing Terminal/CMD command.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            CloseableUtils.closeQuietly(reader);
        }).start();
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

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {
        ExecuteTerminalCommandActionValueScreen s = new ExecuteTerminalCommandActionValueScreen(Objects.requireNonNullElse(instance.value, this.getValueExample()), value -> {
            if (value != null) {
                instance.value = value;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public static class ExecuteTerminalCommandActionValueScreen extends StringBuilderScreen {

        @NotNull
        protected String windowsCommand;
        @NotNull
        protected String macOsCommand;
        @NotNull
        protected String linuxCommand;

        @SuppressWarnings("all")
        protected ExecuteTerminalCommandActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.editor.actions.execute_terminal_command.edit"), callback);
            this.windowsCommand = getOSCommand(value, "windows");
            this.macOsCommand = getOSCommand(value, "macos");
            this.linuxCommand = getOSCommand(value, "linux");
            if ((this.windowsCommand == null) && (this.macOsCommand == null) && (this.linuxCommand == null)) {
                this.windowsCommand = value;
                this.macOsCommand = value;
                this.linuxCommand = value;
            }
            if (this.windowsCommand == null) this.windowsCommand = "";
            if (this.macOsCommand == null) this.macOsCommand = "";
            if (this.linuxCommand == null) this.linuxCommand = "";
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            this.addLabelCell(Component.translatable("fancymenu.editor.actions.execute_terminal_command.edit.desc.line1"));
            this.addLabelCell(Component.translatable("fancymenu.editor.actions.execute_terminal_command.edit.desc.line2"));
            this.addLabelCell(Component.translatable("fancymenu.editor.actions.execute_terminal_command.edit.desc.line3"));

            this.addDescriptionEndSeparatorCell();

            this.addLabelCell(Component.translatable("fancymenu.editor.actions.execute_terminal_command.windows"));
            this.addTextInputCell(null, true, true).setEditListener(s -> this.windowsCommand = s).setText(this.windowsCommand);

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.editor.actions.execute_terminal_command.macos"));
            this.addTextInputCell(null, true, true).setEditListener(s -> this.macOsCommand = s).setText(this.macOsCommand);

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.editor.actions.execute_terminal_command.linux"));
            this.addTextInputCell(null, true, true).setEditListener(s -> this.linuxCommand = s).setText(this.linuxCommand);

            this.addSpacerCell(20);

        }

        @Override
        public @NotNull String buildString() {
            return "[windows:" + this.windowsCommand + "]; [macos:" + this.macOsCommand + "]; [linux:" + this.linuxCommand + "];";
        }

    }

}
