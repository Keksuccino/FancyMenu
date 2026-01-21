package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExecuteCommandAsIntegratedServerAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public ExecuteCommandAsIntegratedServerAction() {
        super("execute_command_as_integrated_server");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            if (minecraft.level != null) {
                LOGGER.error("[FANCYMENU] ExecuteCommandAsIntegratedServerAction can only be used in singleplayer (integrated server). Refusing to run command on multiplayer server: " + value);
            }
            return;
        }
        if (server.isPublished()) {
            LOGGER.error("[FANCYMENU] ExecuteCommandAsIntegratedServerAction does not work while the world is opened to LAN. Refusing to run command: " + value);
            return;
        }

        String command = value;
        server.execute(() -> {
            CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
            server.getCommands().performPrefixedCommand(source, command);
        });
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.execute_command_as_integrated_server");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.execute_command_as_integrated_server.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.execute_command_as_integrated_server.desc.value");
    }

    @Override
    public String getValuePreset() {
        return "/give @p minecraft:diamond 1";
    }

}
