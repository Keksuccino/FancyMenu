package de.keksuccino.fancymenu.networking.packets.commands.variable.command;

import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ClientSideVariableCommandPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull VariableCommandPacket packet) {
        if (packet.set) {
            return setVariable(packet);
        } else {
            return getVariable(packet);
        }
    }

    protected static boolean getVariable(@NotNull VariableCommandPacket packet) {
        try {
            String s = VariableHandler.variableExists(Objects.requireNonNull(packet.variable_name)) ? Objects.requireNonNull(VariableHandler.getVariable(packet.variable_name)).getValue() : null;
            if (s != null) {
                packet.sendChatFeedback(Components.translatable("fancymenu.commands.variable.get.success", s), false);
                return true;
            } else {
                packet.sendChatFeedback(Components.translatable("fancymenu.commands.variable.not_found"), true);
            }
        } catch (Exception ex) {
            packet.sendChatFeedback(Components.translatable("fancymenu.commands.variable.get.error"), true);
            LOGGER.error("[FANCYMENU] Failed to get variable via /fmvariable command!", ex);
        }
        return false;
    }

    protected static boolean setVariable(@NotNull VariableCommandPacket packet) {
        try {
            VariableHandler.setVariable(Objects.requireNonNull(packet.variable_name), Objects.requireNonNull(packet.set_to_value));
            if (packet.feedback) {
                packet.sendChatFeedback(Components.translatable("fancymenu.commands.variable.set.success", packet.set_to_value), false);
            }
            return true;
        } catch (Exception ex) {
            packet.sendChatFeedback(Components.translatable("fancymenu.commands.variable.set.error"), true);
            LOGGER.error("[FANCYMENU] Failed to set variable via /fmvariable command!", ex);
        }
        return false;
    }

}
