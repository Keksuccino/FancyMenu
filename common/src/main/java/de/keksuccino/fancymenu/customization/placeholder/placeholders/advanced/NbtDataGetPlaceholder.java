package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class NbtDataGetPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public NbtDataGetPlaceholder() {
        super("nbt_data_get");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        String sourceType = dps.values.get("source_type");
        String nbtPath = dps.values.get("nbt_path");
        String scaleStr = dps.values.get("scale");
        String returnType = dps.values.get("return_type");

        ClientLevel level = Minecraft.getInstance().level;

        if (level == null) return "";

        if (sourceType == null || nbtPath == null) {
            return "";
        }

        // Default return type is "value"
        if (returnType == null || returnType.isEmpty()) {
            returnType = "value";
        }

        try {
            // Parse NBT path
            NbtPathArgument.NbtPath path = NbtPathArgument.NbtPath.of(nbtPath);

            // Get the source NBT data
            CompoundTag sourceData = getSourceData(sourceType, dps);
            if (sourceData == null) {
                return "";
            }

            // Get the tag at the path
            List<Tag> tags = path.get(sourceData);
            if (tags.isEmpty()) {
                return "";
            }

            Tag tag = tags.get(0);

            // Return based on return type
            if ("string".equalsIgnoreCase(returnType)) {
                // Return the actual NBT data as string
                return getTagAsString(tag);
            } else if ("snbt".equalsIgnoreCase(returnType)) {
                // Return as SNBT (formatted NBT)
                return tag.toString();
            } else if ("json".equalsIgnoreCase(returnType) && tag instanceof CompoundTag) {
                // Return as JSON-formatted component
                Component component = NbtUtils.toPrettyComponent(tag);
                String json = ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), component).getOrThrow().toString();
                if (json.startsWith("\"") && json.endsWith("\"")) {
                    json = json.substring(1, json.length() - 1);
                }
                return json;
            } else {
                // Default: return value (like /data get command)
                double scale = 1.0;
                if (scaleStr != null && !scaleStr.isEmpty()) {
                    try {
                        scale = Double.parseDouble(scaleStr);
                    } catch (NumberFormatException e) {
                        // Invalid scale, use default
                    }
                }
                return getTagValue(tag, scale);
            }

        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error in nbt_data_get placeholder: " + e.getMessage());
            return "";
        }

    }

    private CompoundTag getSourceData(String sourceType, DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        switch (sourceType.toLowerCase()) {
            case "entity":
                return getEntityData(dps);
            case "block":
                return getBlockData(dps);
            default:
                return null;
        }
    }

    private CompoundTag getEntityData(DeserializedPlaceholderString dps) {
        String selector = dps.values.get("entity_selector");
        if (selector == null) return null;

        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        if (player == null || level == null) return null;

        // Handle common selectors
        Entity targetEntity = null;
        switch (selector) {
            case "@s":
            case "@p":
                targetEntity = player;
                break;
            case "@e":
                // Get nearest entity
                double nearestDist = Double.MAX_VALUE;
                for (Entity entity : level.entitiesForRendering()) {
                    double dist = entity.distanceToSqr(player);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        targetEntity = entity;
                    }
                }
                break;
            default:
                // Try to find entity by UUID or name
                try {
                    UUID uuid = UUID.fromString(selector);
                    for (Entity entity : level.entitiesForRendering()) {
                        if (entity.getUUID().equals(uuid)) {
                            targetEntity = entity;
                            break;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Not a UUID, try as player name
                    for (Entity entity : level.entitiesForRendering()) {
                        if (entity.getName().getString().equals(selector)) {
                            targetEntity = entity;
                            break;
                        }
                    }
                }
                break;
        }

        if (targetEntity == null) return null;

        // Get entity NBT data using the new ValueOutput system in 1.21.6
        // Use DISCARDING reporter since we don't need to collect problems
        net.minecraft.world.level.storage.TagValueOutput valueOutput = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(
                net.minecraft.util.ProblemReporter.DISCARDING
        );
        targetEntity.saveWithoutId(valueOutput);
        return valueOutput.buildResult();
    }

    private CompoundTag getBlockData(DeserializedPlaceholderString dps) {
        String posStr = dps.values.get("block_pos");
        if (posStr == null) return null;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        try {
            // Parse block position (format: "x y z")
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
        } catch (NumberFormatException e) {
            LOGGER.error("[FANCYMENU] Invalid block position: " + posStr);
        }

        return null;
    }

    private String getTagAsString(Tag tag) {
        // Handle different tag types appropriately
        if (tag instanceof StringTag stringTag) {
            return stringTag.value();
        } else if (tag instanceof NumericTag) {
            return tag.toString();
        } else {
            // For compound tags and lists, use the string representation
            return tag.toString();
        }
    }

    private String getTagValue(Tag tag, double scale) {
        if (tag instanceof NumericTag numericTag) {
            // Numeric value with optional scaling
            if (scale != 1.0) {
                return String.valueOf(Mth.floor(numericTag.doubleValue() * scale));
            } else {
                return String.valueOf(Mth.floor(numericTag.doubleValue()));
            }
        } else if (tag instanceof StringTag stringTag) {
            // String length
            return String.valueOf(stringTag.value().length());
        } else if (tag instanceof CollectionTag collectionTag) {
            // Collection size
            return String.valueOf(collectionTag.size());
        } else if (tag instanceof CompoundTag compoundTag) {
            // Compound size
            return String.valueOf(compoundTag.size());
        } else {
            // For other types, return the string representation
            return getTagAsString(tag);
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("source_type", "entity_selector", "block_pos", "nbt_path", "scale", "return_type");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.nbt_data_get");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.nbt_data_get.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("source_type", "entity");
        values.put("entity_selector", "@s");
        values.put("block_pos", "");
        values.put("nbt_path", "foodLevel");
        values.put("scale", "1.0");
        values.put("return_type", "value");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
