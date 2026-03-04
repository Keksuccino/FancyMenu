package de.keksuccino.fancymenu.networking.packets.placeholders.gamerule;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerSideServerGameruleValueRequestPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerPlayer sender, @NotNull ServerGameruleValueRequestPacket packet) {
        if ((packet.placeholder == null) || packet.placeholder.isEmpty()) {
            LOGGER.warn("[FANCYMENU] Received malformed gamerule placeholder request without placeholder string.");
            return false;
        }

        String result = "";
        try {
            if (sender.hasPermissions(Commands.LEVEL_GAMEMASTERS) && (sender.getServer() != null)) {
                String gameruleName = normalizeGameruleName(packet.gamerule);
                if (gameruleName != null) {
                    String gameruleValue = getGameruleValue(sender.getServer().getGameRules(), gameruleName);
                    if (gameruleValue != null) {
                        result = gameruleValue;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to resolve server-side gamerule placeholder.", ex);
        }

        PacketHandler.sendToClient(sender, new ServerGameruleValueResponsePacket(packet.placeholder, result));
        return true;
    }

    @Nullable
    private static String getGameruleValue(@NotNull GameRules gameRules, @NotNull String gameruleName) {
        final String[] value = new String[1];
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                if (value[0] != null) {
                    return;
                }
                if (key.getId().equalsIgnoreCase(gameruleName)) {
                    T rule = gameRules.getRule(key);
                    if (rule != null) {
                        value[0] = rule.serialize();
                    }
                }
            }
        });
        return value[0];
    }

    @Nullable
    private static String normalizeGameruleName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
