package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.ComponentParser;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ClientSideNbtDataGetPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public ClientSideNbtDataGetPlaceholder() {
        super("nbt_data_get");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        String sourceType = dps.values.get("source_type");
        String nbtPath = dps.values.get("nbt_path");
        String scaleStr = dps.values.get("scale");
        String returnType = dps.values.get("return_type");

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return "";

        if ((sourceType == null) || sourceType.isEmpty() || (nbtPath == null) || nbtPath.isEmpty()) {
            return "";
        }

        if ((returnType == null) || returnType.isEmpty()) {
            returnType = "value";
        }

        try {
            NbtPathArgument.NbtPath path = NbtPathArgument.NbtPath.of(nbtPath);
            CompoundTag sourceData = getSourceData(sourceType, dps);
            if (sourceData == null) {
                return "";
            }

            List<Tag> tags = path.get(sourceData);
            if (tags.isEmpty()) {
                return "";
            }

            Tag tag = tags.get(0);

            if ("string".equalsIgnoreCase(returnType)) {
                return tag.asString().orElse("");
            } else if ("snbt".equalsIgnoreCase(returnType)) {
                return tag.toString();
            } else if ("json".equalsIgnoreCase(returnType) && tag instanceof CompoundTag) {
                String json = ComponentParser.toJson(NbtUtils.toPrettyComponent(tag));
                if (json.startsWith("\"") && json.endsWith("\"")) {
                    return json.substring(1, json.length() - 1);
                }
                return json;
            } else {
                double scale = 1.0D;
                if ((scaleStr != null) && !scaleStr.isEmpty()) {
                    try {
                        scale = Double.parseDouble(scaleStr);
                    } catch (NumberFormatException ignored) {
                        // ignore invalid scale values
                    }
                }
                return getTagValue(tag, scale);
            }
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error in nbt_data_get placeholder", e);
        }
        return "";
    }

    @Nullable
    private CompoundTag getSourceData(String sourceType, DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        switch (sourceType.toLowerCase(Locale.ROOT)) {
            case "entity":
                return getEntityData(dps);
            case "block":
                return getBlockData(dps);
            default:
                return null;
        }
    }

    @Nullable
    private CompoundTag getEntityData(DeserializedPlaceholderString dps) {
        String selector = dps.values.get("entity_selector");
        if (selector == null || selector.isEmpty()) return null;

        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        if (player == null || level == null) return null;

        Entity targetEntity = resolveEntitySelector(selector.trim(), player, level);
        if (targetEntity == null) {
            return null;
        }

        ProblemReporter.Collector collector = new ProblemReporter.Collector();
        TagValueOutput tagValueOutput = TagValueOutput.createWithContext(collector, targetEntity.registryAccess());
        targetEntity.saveWithoutId(tagValueOutput);
        CompoundTag result = tagValueOutput.buildResult();
        if (!collector.isEmpty()) {
            LOGGER.warn("[FANCYMENU] Issues serializing entity NBT: {}", collector.getTreeReport());
        }
        return result;
    }

    @Nullable
    private CompoundTag getBlockData(DeserializedPlaceholderString dps) {
        String posStr = dps.values.get("block_pos");
        if (posStr == null || posStr.isEmpty()) return null;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        try {
            String[] parts = posStr.trim().split("\\s+");
            if (parts.length != 3) return null;

            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            BlockPos pos = new BlockPos(x, y, z);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                return blockEntity.saveWithoutMetadata(level.registryAccess());
            }
        } catch (NumberFormatException ex) {
            LOGGER.error("[FANCYMENU] Invalid block position: {}", posStr);
        }
        return null;
    }

    private String getTagValue(Tag tag, double scale) {
        if (tag instanceof NumericTag numericTag) {
            if (scale != 1.0D) {
                return formatScaledNumeric(numericTag, scale);
            }
            return numericTag.asString().orElse("");
        }
        if (tag instanceof StringTag) {
            return tag.asString().orElse("");
        }
        return tag.toString();
    }

    private String formatScaledNumeric(NumericTag tag, double scale) {
        if (tag instanceof FloatTag) {
            float value = (float)(tag.asDouble().orElse(1D) * scale);
            return Float.toString(value) + "f";
        }
        if (tag instanceof DoubleTag) {
            double value = tag.asDouble().orElse(1D) * scale;
            return Double.toString(value) + "d";
        }

        long rounded = Math.round(tag.asDouble().orElse(1D) * scale);
        if (tag instanceof ByteTag) {
            return Byte.toString((byte)rounded) + "b";
        }
        if (tag instanceof ShortTag) {
            return Short.toString((short)rounded) + "s";
        }
        if (tag instanceof IntTag) {
            return Integer.toString((int)rounded);
        }
        if (tag instanceof LongTag) {
            return Long.toString(rounded) + "L";
        }
        return Long.toString(rounded);
    }

    @Nullable
    private Entity resolveEntitySelector(String selector, LocalPlayer player, ClientLevel level) {
        if (!selector.startsWith("@")) {
            return resolveByDirectReference(selector, level);
        }

        int bracketIndex = selector.indexOf('[');
        int closingIndex = selector.endsWith("]") ? selector.lastIndexOf(']') : -1;

        String base = bracketIndex == -1 ? selector : selector.substring(0, bracketIndex);
        String optionsRaw = "";
        if (bracketIndex != -1) {
            int end = closingIndex > bracketIndex ? closingIndex : selector.length();
            optionsRaw = selector.substring(bracketIndex + 1, end).trim();
        }

        SelectorTarget target = SelectorTarget.fromToken(base.trim());
        if (target == null) {
            return resolveByDirectReference(selector, level);
        }

        List<OptionValue> options = parseSelectorOptions(optionsRaw);

        double defaultX = player.getX();
        double defaultY = player.getY();
        double defaultZ = player.getZ();

        Double originX = null;
        Double originY = null;
        Double originZ = null;
        Double dx = null;
        Double dy = null;
        Double dz = null;

        MinMaxBounds.Doubles distance = MinMaxBounds.Doubles.ANY;
        SortOrder sort = target.defaultSort;
        int limit = target.defaultLimit;

        @Nullable Filter<ResourceLocation> typeFilter = null;
        final List<Filter<String>> nameFilters = new ArrayList<>();
        final List<Filter<String>> tagFilters = new ArrayList<>();

        for (OptionValue option : options) {
            String key = option.key();
            String value = option.value();
            switch (key) {
                case "type":
                    if (!value.isEmpty()) {
                        boolean inverted = value.startsWith("!");
                        String raw = inverted ? value.substring(1) : value;
                        Identifier typeId = parseResourceLocation(raw);
                        if (typeId != null) {
                            typeFilter = new Filter<>(typeId, inverted);
                        }
                    }
                    break;
                case "name":
                    if (!value.isEmpty()) {
                        boolean inverted = value.startsWith("!");
                        String raw = inverted ? value.substring(1) : value;
                        if (!raw.isEmpty()) {
                            nameFilters.add(new Filter<>(raw, inverted));
                        }
                    }
                    break;
                case "tag":
                    if (!value.isEmpty()) {
                        boolean inverted = value.startsWith("!");
                        String raw = inverted ? value.substring(1) : value;
                        if (!raw.isEmpty()) {
                            tagFilters.add(new Filter<>(raw, inverted));
                        }
                    }
                    break;
                case "limit":
                    if (!value.isEmpty()) {
                        try {
                            int parsed = Integer.parseInt(value);
                            if (parsed > 0) {
                                limit = parsed;
                            }
                        } catch (NumberFormatException ignored) {
                            // ignore invalid limit values
                        }
                    }
                    break;
                case "sort":
                    if (!value.isEmpty()) {
                        SortOrder parsed = SortOrder.fromOption(value);
                        if (parsed != null) {
                            sort = parsed;
                        }
                    }
                    break;
                case "distance":
                    if (!value.isEmpty()) {
                        try {
                            distance = MinMaxBounds.Doubles.fromReader(new StringReader(value));
                        } catch (CommandSyntaxException ex) {
                            LOGGER.error("[FANCYMENU] Invalid distance option in selector '{}': {}", selector, ex.getMessage());
                        }
                    }
                    break;
                case "x":
                    originX = parseCoordinate(value, defaultX);
                    break;
                case "y":
                    originY = parseCoordinate(value, defaultY);
                    break;
                case "z":
                    originZ = parseCoordinate(value, defaultZ);
                    break;
                case "dx":
                    dx = parseDouble(value);
                    break;
                case "dy":
                    dy = parseDouble(value);
                    break;
                case "dz":
                    dz = parseDouble(value);
                    break;
                default:
                    // Unsupported option - ignore for now
                    break;
            }
        }

        if (target == SelectorTarget.SELF) {
            Vec3 origin = new Vec3(
                    originX != null ? originX : defaultX,
                    originY != null ? originY : defaultY,
                    originZ != null ? originZ : defaultZ
            );
            AABB bounds = buildBounds(origin, dx, dy, dz);
            return entityMatchesFilters(player, typeFilter, nameFilters, tagFilters, distance, origin, bounds) ? player : null;
        }

        List<Entity> candidates = collectCandidates(target, level);
        if (candidates.isEmpty()) {
            return null;
        }

        Vec3 origin = new Vec3(
                originX != null ? originX : defaultX,
                originY != null ? originY : defaultY,
                originZ != null ? originZ : defaultZ
        );
        AABB bounds = buildBounds(origin, dx, dy, dz);

        @Nullable Filter<ResourceLocation> finalTypeFilter = typeFilter;
        MinMaxBounds.Doubles finalDistance = distance;
        candidates.removeIf(entity -> !entityMatchesFilters(entity, finalTypeFilter, nameFilters, tagFilters, finalDistance, origin, bounds));
        if (candidates.isEmpty()) {
            return null;
        }

        applySorting(candidates, sort, origin);

        if (limit < candidates.size()) {
            candidates = new ArrayList<>(candidates.subList(0, limit));
        }

        return candidates.isEmpty() ? null : candidates.get(0);
    }

    @Nullable
    private Entity resolveByDirectReference(String selector, ClientLevel level) {
        String trimmed = selector.trim();
        if (trimmed.isEmpty()) return null;

        try {
            UUID uuid = UUID.fromString(trimmed);
            for (Entity entity : collectAllEntities(level)) {
                if (uuid.equals(entity.getUUID())) {
                    return entity;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // not a UUID
        }

        for (Entity entity : collectAllEntities(level)) {
            if (entity.getName().getString().equals(trimmed)) {
                return entity;
            }
        }

        return null;
    }

    private List<Entity> collectCandidates(SelectorTarget target, ClientLevel level) {
        List<Entity> all = collectAllEntities(level);
        if (target.includePlayers && target.includeNonPlayers) {
            return new ArrayList<>(all);
        }

        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : all) {
            boolean isPlayer = entity instanceof Player;
            if ((isPlayer && target.includePlayers) || (!isPlayer && target.includeNonPlayers)) {
                filtered.add(entity);
            }
        }
        return filtered;
    }

    private List<Entity> collectAllEntities(ClientLevel level) {
        Set<Integer> seenIds = new HashSet<>();
        List<Entity> result = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity != null && seenIds.add(entity.getId())) {
                result.add(entity);
            }
        }
        for (Player player : level.players()) {
            if (player != null && seenIds.add(player.getId())) {
                result.add(player);
            }
        }
        return result;
    }

    private void applySorting(List<Entity> entities, SortOrder sort, Vec3 origin) {
        switch (sort) {
            case NEAREST -> entities.sort((a, b) -> Double.compare(a.distanceToSqr(origin), b.distanceToSqr(origin)));
            case FURTHEST -> entities.sort((a, b) -> Double.compare(b.distanceToSqr(origin), a.distanceToSqr(origin)));
            case RANDOM -> {
                for (int i = entities.size() - 1; i > 0; i--) {
                    int j = ThreadLocalRandom.current().nextInt(i + 1);
                    Collections.swap(entities, i, j);
                }
            }
            case ARBITRARY -> {
                // keep natural iteration order
            }
        }
    }

    private boolean entityMatchesFilters(
            Entity entity,
            @Nullable Filter<ResourceLocation> typeFilter,
            List<Filter<String>> nameFilters,
            List<Filter<String>> tagFilters,
            MinMaxBounds.Doubles distance,
            Vec3 origin,
            @Nullable AABB bounds
    ) {
        if (typeFilter != null) {
            Optional<EntityType<?>> targetType = BuiltInRegistries.ENTITY_TYPE.getOptional(typeFilter.value());
            if (targetType.isPresent()) {
                boolean matches = entity.getType() == targetType.get();
                if (typeFilter.inverted() == matches) return false;
            } else if (!typeFilter.inverted()) {
                return false;
            }
        }

        if (!nameFilters.isEmpty()) {
            String name = entity.getName().getString();
            for (Filter<String> filter : nameFilters) {
                boolean matches = name.equals(filter.value());
                if (filter.inverted() == matches) {
                    return false;
                }
            }
        }

        if (!tagFilters.isEmpty()) {
            Set<String> tags = entity.getTags();
            for (Filter<String> filter : tagFilters) {
                boolean matches = tags.contains(filter.value());
                if (filter.inverted() == matches) {
                    return false;
                }
            }
        }

        if (bounds != null && !bounds.intersects(entity.getBoundingBox())) {
            return false;
        }

        if (distance != MinMaxBounds.Doubles.ANY) {
            if (!distance.matchesSqr(entity.distanceToSqr(origin))) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    private AABB buildBounds(Vec3 origin, @Nullable Double dx, @Nullable Double dy, @Nullable Double dz) {
        if (dx == null && dy == null && dz == null) {
            return null;
        }
        double sizeX = dx != null ? dx : 0.0D;
        double sizeY = dy != null ? dy : 0.0D;
        double sizeZ = dz != null ? dz : 0.0D;

        double minX = Math.min(origin.x(), origin.x() + sizeX);
        double minY = Math.min(origin.y(), origin.y() + sizeY);
        double minZ = Math.min(origin.z(), origin.z() + sizeZ);
        double maxX = Math.max(origin.x(), origin.x() + sizeX);
        double maxY = Math.max(origin.y(), origin.y() + sizeY);
        double maxZ = Math.max(origin.z(), origin.z() + sizeZ);

        // Expand by one block to mimic vanilla selector volume behaviour
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
    }

    @Nullable
    private Double parseCoordinate(String raw, double reference) {
        if (raw == null || raw.isEmpty()) return reference;
        if (raw.charAt(0) == '~') {
            if (raw.length() == 1) {
                return reference;
            }
            Double offset = parseDouble(raw.substring(1));
            return offset == null ? null : reference + offset;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            LOGGER.error("[FANCYMENU] Invalid coordinate value: {}", raw);
            return null;
        }
    }

    @Nullable
    private Double parseDouble(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            LOGGER.error("[FANCYMENU] Invalid numeric selector option: {}", raw);
            return null;
        }
    }

    private Identifier parseResourceLocation(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String normalized = raw.contains(":") ? raw : "minecraft:" + raw;
        return Identifier.tryParse(normalized.toLowerCase(Locale.ROOT));
    }

    private List<OptionValue> parseSelectorOptions(String raw) {
        List<OptionValue> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return result;

        StringBuilder token = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else if (c == '\\' && (i + 1) < raw.length()) {
                    token.append(raw.charAt(++i));
                } else {
                    token.append(c);
                }
                continue;
            }

            if (c == '\\' && (i + 1) < raw.length()) {
                token.append(raw.charAt(++i));
                continue;
            }

            if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
                continue;
            }

            if (c == '{' || c == '[' || c == '(') {
                depth++;
            } else if (c == '}' || c == ']' || c == ')') {
                if (depth > 0) depth--;
            }

            if (c == ',' && depth == 0) {
                appendOptionToken(token, result);
                token.setLength(0);
                continue;
            }

            token.append(c);
        }

        appendOptionToken(token, result);
        return result;
    }

    private void appendOptionToken(StringBuilder token, List<OptionValue> result) {
        if (token.isEmpty()) return;
        String entry = token.toString().trim();
        if (entry.isEmpty()) return;
        int index = entry.indexOf('=');
        String key;
        String value;
        if (index == -1) {
            key = entry.toLowerCase(Locale.ROOT);
            value = "";
        } else {
            key = entry.substring(0, index).trim().toLowerCase(Locale.ROOT);
            value = entry.substring(index + 1).trim();
        }
        result.add(new OptionValue(key, value));
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("source_type", "entity_selector", "block_pos", "nbt_path", "scale", "return_type");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.nbt_data_get.client");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.nbt_data_get.client.desc"));
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
        values.put("nbt_path", "foodLevel");
        values.put("scale", "1.0");
        values.put("return_type", "value");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    private record OptionValue(String key, String value) {
    }

    private record Filter<T>(T value, boolean inverted) {
    }

    private enum SortOrder {
        NEAREST("nearest"),
        FURTHEST("furthest"),
        RANDOM("random"),
        ARBITRARY("arbitrary");

        private final String key;

        SortOrder(String key) {
            this.key = key;
        }

        @Nullable
        static SortOrder fromOption(String value) {
            for (SortOrder order : values()) {
                if (order.key.equalsIgnoreCase(value)) {
                    return order;
                }
            }
            return null;
        }
    }

    private enum SelectorTarget {
        SELF("@s", 1, SortOrder.ARBITRARY, true, true),
        NEAREST_PLAYER("@p", 1, SortOrder.NEAREST, true, false),
        ALL_PLAYERS("@a", Integer.MAX_VALUE, SortOrder.ARBITRARY, true, false),
        RANDOM_PLAYER("@r", 1, SortOrder.RANDOM, true, false),
        ALL_ENTITIES("@e", Integer.MAX_VALUE, SortOrder.ARBITRARY, true, true);

        private final String token;
        private final int defaultLimit;
        private final SortOrder defaultSort;
        private final boolean includePlayers;
        private final boolean includeNonPlayers;

        SelectorTarget(String token, int defaultLimit, SortOrder defaultSort, boolean includePlayers, boolean includeNonPlayers) {
            this.token = token;
            this.defaultLimit = defaultLimit;
            this.defaultSort = defaultSort;
            this.includePlayers = includePlayers;
            this.includeNonPlayers = includeNonPlayers;
        }

        static SelectorTarget fromToken(String token) {
            for (SelectorTarget target : values()) {
                if (target.token.equals(token)) {
                    return target;
                }
            }
            return null;
        }
    }

}











