package de.keksuccino.fancymenu.networking.packets.placeholders.gamerule;

import de.keksuccino.fancymenu.networking.PacketHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
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
            MinecraftServer server = sender.level().getServer();
            if (server != null) {
                String gameruleName = normalizeGameruleName(packet.gamerule);
                if (gameruleName != null) {
                    String gameruleValue = getGameruleValue(server.getGameRules(), gameruleName);
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
        gameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(GameRule<T> gameRule) {
                if (value[0] != null) {
                    return;
                }
                if (gameRule.id().equalsIgnoreCase(gameruleName)) {
                    value[0] = gameRules.getAsString(gameRule);
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
