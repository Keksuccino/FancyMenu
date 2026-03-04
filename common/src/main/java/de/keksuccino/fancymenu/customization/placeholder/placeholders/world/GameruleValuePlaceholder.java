package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.placeholders.gamerule.ServerGameruleValueRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GameruleValuePlaceholder extends AbstractWorldPlaceholder {

    private static final long CACHE_DURATION_MS = 100L;
    private static final long REQUEST_TIMEOUT_MS = 2000L;

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> PENDING_REQUESTS = new ConcurrentHashMap<>();

    public GameruleValuePlaceholder() {
        super("gamerule_value");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String gameruleName = normalizeGameruleName(dps.values.get("name"));
        if (gameruleName == null) {
            return "";
        }

        LocalPlayer player = this.getPlayer();
        if ((this.getLevel() == null) || (player == null)) {
            return "";
        }

        if (!player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
            return "";
        }

        IntegratedServer singleplayerServer = Minecraft.getInstance().getSingleplayerServer();
        if (singleplayerServer != null) {
            String value = getGameruleValue(singleplayerServer.getGameRules(), gameruleName);
            return value == null ? "" : value;
        }

        String placeholderKey = dps.placeholderString;
        if ((placeholderKey == null) || placeholderKey.isEmpty()) {
            return "";
        }

        long now = System.currentTimeMillis();
        CacheEntry entry = CACHE.get(placeholderKey);

        Long pendingSince = PENDING_REQUESTS.get(placeholderKey);
        if ((pendingSince != null) && ((now - pendingSince) > REQUEST_TIMEOUT_MS)) {
            PENDING_REQUESTS.remove(placeholderKey);
            pendingSince = null;
        }

        if (pendingSince == null) {
            long lastUpdate = entry != null ? entry.timestampMs : Long.MIN_VALUE;
            if ((entry == null) || ((now - lastUpdate) > CACHE_DURATION_MS)) {
                requestDataFromServer(placeholderKey, gameruleName, now);
            }
        }

        if (entry != null) {
            return entry.value;
        }

        return "";
    }

    @Nullable
    private String getGameruleValue(@NotNull GameRules gameRules, @NotNull String gameruleName) {
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
    private String normalizeGameruleName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requestDataFromServer(@NotNull String placeholderKey, @NotNull String gameruleName, long now) {
        if ((Minecraft.getInstance().player == null) || (Minecraft.getInstance().getConnection() == null)) {
            return;
        }

        ServerGameruleValueRequestPacket packet = new ServerGameruleValueRequestPacket();
        packet.placeholder = placeholderKey;
        packet.gamerule = gameruleName;

        PENDING_REQUESTS.put(placeholderKey, now);
        PacketHandler.sendToServer(packet);
    }

    public static void handleServerResponse(@NotNull String placeholderKey, @NotNull String value) {
        Objects.requireNonNull(placeholderKey, "placeholderKey");
        CACHE.put(placeholderKey, new CacheEntry(value, System.currentTimeMillis()));
        PENDING_REQUESTS.remove(placeholderKey);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("name");
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.gamerule_value";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("name", "doDaylightCycle");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    private record CacheEntry(String value, long timestampMs) {
    }

}
