package de.keksuccino.fancymenu.networking.packets.commands.variable.command;

import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.networking.Packet;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Objects;

public class VariableCommandPacket extends Packet {

    private static final Logger LOGGER = LogManager.getLogger();

    public boolean set;
    public String variable_name;
    public String set_to_value;
    public boolean feedback;

    @Override
    public boolean processPacket() {
        if (this.set) {
            return this.setVariable();
        } else {
            return this.getVariable();
        }
    }

    protected boolean getVariable() {
        try {
            String s = VariableHandler.variableExists(Objects.requireNonNull(this.variable_name)) ? Objects.requireNonNull(VariableHandler.getVariable(this.variable_name)).getValue() : null;
            if (s != null) {
                this.sendChatFeedback(Component.translatable("fancymenu.commands.variable.get.success", s), false);
                return true;
            } else {
                this.sendChatFeedback(Component.translatable("fancymenu.commands.variable.not_found"), true);
            }
        } catch (Exception ex) {
            this.sendChatFeedback(Component.translatable("fancymenu.commands.variable.get.error"), true);
            LOGGER.error("[FANCYMENU] Failed to get variable via /fmvariable command!", ex);
        }
        return false;
    }

    protected boolean setVariable() {
        try {
            VariableHandler.setVariable(Objects.requireNonNull(this.variable_name), Objects.requireNonNull(this.set_to_value));
            if (this.feedback) {
                this.sendChatFeedback(Component.translatable("fancymenu.commands.variable.set.success", this.set_to_value), false);
            }
            return true;
        } catch (Exception ex) {
            this.sendChatFeedback(Component.translatable("fancymenu.commands.variable.set.error"), true);
            LOGGER.error("[FANCYMENU] Failed to set variable via /fmvariable command!", ex);
        }
        return false;
    }

}
