package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.placeholders.nbt.ServerNbtDataRequestPacket;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSideNbtDataGetPlaceholder extends Placeholder {

    private static final long CACHE_DURATION_MS = 100L;
    private static final long REQUEST_TIMEOUT_MS = 2000L;

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> PENDING_REQUESTS = new ConcurrentHashMap<>();

    public ServerSideNbtDataGetPlaceholder() {
        super("nbt_data_get_server");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String placeholderKey = dps.placeholderString;
        if ((placeholderKey == null) || placeholderKey.isEmpty()) {
            return "";
        }

        long now = System.currentTimeMillis();
        CacheEntry entry = CACHE.get(placeholderKey);

        Long pendingSince = PENDING_REQUESTS.get(placeholderKey);
        if (pendingSince != null && ((now - pendingSince) > REQUEST_TIMEOUT_MS)) {
            PENDING_REQUESTS.remove(placeholderKey);
            pendingSince = null;
        }

        boolean awaitingUpdate = (pendingSince != null);

        if (!awaitingUpdate) {
            long lastUpdate = entry != null ? entry.timestampMs : Long.MIN_VALUE;
            if ((entry == null) || ((now - lastUpdate) > CACHE_DURATION_MS)) {
                requestDataFromServer(dps, placeholderKey, now);
                awaitingUpdate = true;
            }
        }

        if (entry != null) {
            return entry.value;
        }

        return "";
    }

    private void requestDataFromServer(DeserializedPlaceholderString dps, String placeholderKey, long now) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().getConnection() == null) {
            return;
        }

        ServerNbtDataRequestPacket packet = new ServerNbtDataRequestPacket();
        packet.placeholder = placeholderKey;
        packet.source_type = normalizeValue(dps.values.get("source_type"));
        packet.entity_selector = normalizeValue(dps.values.get("entity_selector"));
        packet.block_pos = normalizeValue(dps.values.get("block_pos"));
        packet.storage_id = normalizeValue(dps.values.get("storage_id"));
        packet.nbt_path = normalizeValue(dps.values.get("nbt_path"));
        packet.return_type = normalizeValue(dps.values.get("return_type"));

        Double scale = parseScale(dps.values.get("scale"));
        packet.scale = scale;

        PENDING_REQUESTS.put(placeholderKey, now);
        PacketHandler.sendToServer(packet);
    }

    @Nullable
    private Double parseScale(@Nullable String scaleString) {
        if ((scaleString == null) || scaleString.isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(scaleString);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private String normalizeValue(@Nullable String value) {
        if ((value == null) || value.isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static void handleServerResponse(@NotNull String placeholderKey, @NotNull String value) {
        Objects.requireNonNull(placeholderKey, "placeholderKey");
        CACHE.put(placeholderKey, new CacheEntry(value, System.currentTimeMillis()));
        PENDING_REQUESTS.remove(placeholderKey);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("source_type", "entity_selector", "block_pos", "storage_id", "nbt_path", "scale", "return_type");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.nbt_data_get.server");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.nbt_data_get.server.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("source_type", "entity");
        values.put("entity_selector", "@s");
        values.put("block_pos", "");
        values.put("storage_id", "minecraft:storage_key");
        values.put("nbt_path", "foodLevel");
        values.put("scale", "1.0");
        values.put("return_type", "value");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    private record CacheEntry(String value, long timestampMs) {
    }

}
