package de.keksuccino.fancymenu.networking.neoforge.packets.variablesuggestions;

import de.keksuccino.fancymenu.commands.server.ServerVariableCommand;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerboundVariableSuggestionsPacketPayloadHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ServerboundVariableSuggestionsPacketPayloadHandler INSTANCE = new ServerboundVariableSuggestionsPacketPayloadHandler();

    public static ServerboundVariableSuggestionsPacketPayloadHandler getInstance() {
        return INSTANCE;
    }

    public void handleData(final ServerboundVariableSuggestionsPacketPayload data, final PlayPayloadContext context) {

        if (context.player().isPresent()) {
            if (context.player().get() instanceof ServerPlayer sender) {
                context.workHandler().submitAsync(() -> {
                    String uuid = sender.getUUID().toString();
                    ServerVariableCommand.cachedVariableArguments.put(uuid, data.variableSuggestions());
                });
            } else {
                LOGGER.error("[FANCYMENU] Failed to handle VariableSuggestionPacketPayload!", new IllegalStateException("Player of context is not a ServerPlayer!"));
            }
        } else {
            LOGGER.error("[FANCYMENU] Failed to handle VariableSuggestionPacketPayload!", new NullPointerException("Player not present in context!"));
        }

    }

}
