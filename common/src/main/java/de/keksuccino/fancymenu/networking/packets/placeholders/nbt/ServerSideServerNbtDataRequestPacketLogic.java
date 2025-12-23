package de.keksuccino.fancymenu.networking.packets.placeholders.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.placeholders.nbt.ServerNbtDataResponsePacket.ResultType;
import de.keksuccino.fancymenu.util.rendering.text.ComponentParser;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.CommandStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;

public class ServerSideServerNbtDataRequestPacketLogic {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static boolean handle(@NotNull ServerPlayer sender, @NotNull ServerNbtDataRequestPacket packet) {
        if (packet.placeholder == null || packet.placeholder.isEmpty()) {
            LOGGER.warn("[FANCYMENU] Received malformed server NBT placeholder request without placeholder string.");
            return false;
        }

        String result = "";
        ResultType resultType = ResultType.EMPTY;

        try {
            CommandContextData context = buildContext(sender, packet);
            if (context != null) {
                Tag tag = resolveTag(context, packet);
                if (tag != null) {
                    result = convertResult(tag, packet, context);
                    resultType = ResultType.SUCCESS;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to resolve server-side NBT placeholder.", ex);
        }

        PacketHandler.sendToClient(sender, new ServerNbtDataResponsePacket(packet.placeholder, result, resultType));
        return true;
    }

    @Nullable
    private static CommandContextData buildContext(@NotNull ServerPlayer sender, @NotNull ServerNbtDataRequestPacket packet) throws CommandSyntaxException {
        if (packet.source_type == null) {
            return null;
        }
        String sourceType = packet.source_type.toLowerCase(Locale.ROOT);
        return switch (sourceType) {
            case "entity" -> resolveEntity(sender, packet.entity_selector);
            case "block" -> resolveBlock(sender, packet.block_pos);
            case "storage" -> resolveStorage(sender, packet.storage_id);
            default -> null;
        };
    }

    @Nullable
    private static CommandContextData resolveEntity(@NotNull ServerPlayer sender, @Nullable String selectorString) throws CommandSyntaxException {
        if ((selectorString == null) || selectorString.isEmpty()) {
            return null;
        }
        EntitySelectorParser parser = new EntitySelectorParser(new StringReader(selectorString), true);
        EntitySelector selector = parser.parse();
        CommandSourceStack source = sender.createCommandSourceStack();
        CompoundTag tag = NbtPredicate.getEntityTagToCompare(selector.findSingleEntity(source));
        return new CommandContextData(tag, source);
    }

    @Nullable
    private static CommandContextData resolveBlock(@NotNull ServerPlayer sender, @Nullable String positionString) throws CommandSyntaxException {
        if ((positionString == null) || positionString.isEmpty()) {
            return null;
        }
        ServerLevel level = sender.level();
        CommandSourceStack source = sender.createCommandSourceStack();
        Coordinates coordinates = BlockPosArgument.blockPos().parse(new StringReader(positionString));
        BlockPos pos = coordinates.getBlockPos(source);
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
        return new CommandContextData(tag, source);
    }

    @Nullable
    private static CommandContextData resolveStorage(@NotNull ServerPlayer sender, @Nullable String storageId) {
        if ((storageId == null) || storageId.isEmpty()) {
            return null;
        }
        try {
            Identifier id = Identifier.tryParse(storageId);
            if (id == null) {
                return null;
            }
            CommandSourceStack source = sender.createCommandSourceStack();
            CommandStorage storage = sender.level().getServer().getCommandStorage();
            CompoundTag tag = storage.get(id);
            return new CommandContextData(tag, source);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to resolve command storage for '{}'.", storageId, ex);
            return null;
        }
    }

    @Nullable
    private static Tag resolveTag(@NotNull CommandContextData context, @NotNull ServerNbtDataRequestPacket packet) throws CommandSyntaxException {
        if (context.baseTag() == null) {
            return null;
        }

        if ((packet.nbt_path == null) || packet.nbt_path.isEmpty()) {
            return context.baseTag();
        }

        NbtPathArgument.NbtPath path = NbtPathArgument.nbtPath().parse(new StringReader(packet.nbt_path));
        List<Tag> tags = path.get(context.baseTag());
        if (tags.isEmpty()) {
            return null;
        }
        return tags.get(0);
    }

    @NotNull
    private static String convertResult(@NotNull Tag tag, @NotNull ServerNbtDataRequestPacket packet, @NotNull CommandContextData context) throws CommandSyntaxException {
        String returnType = packet.return_type == null ? "value" : packet.return_type.toLowerCase(Locale.ROOT);
        double scale = packet.scale == null ? 1.0D : packet.scale;

        switch (returnType) {
            case "string" -> {
                return tag.asString().orElse("");
            }
            case "snbt" -> {
                return tag.toString();
            }
            case "json" -> {
                if (tag instanceof CompoundTag) {
                    Component component = NbtUtils.toPrettyComponent(tag);
                    return ComponentParser.toJson(component);
                }
                return tag.toString();
            }
            default -> {
                if (tag instanceof NumericTag numericTag) {
                    return formatScaledNumeric(numericTag, scale);
                }
                return tag.asString().orElse("");
            }
        }
    }

    @NotNull
    private static String formatScaledNumeric(@NotNull NumericTag tag, double scale) {
        if (scale != 1.0D) {
            return formatScaled(tag, scale);
        }
        return tag.asString().orElse("");
    }

    @NotNull
    private static String formatScaled(@NotNull NumericTag tag, double scale) {
        double scaled = tag.asDouble().orElse(1D) * scale;
        if (tag instanceof net.minecraft.nbt.FloatTag) {
            return Float.toString((float) scaled) + "f";
        }
        if (tag instanceof net.minecraft.nbt.DoubleTag) {
            return Double.toString(scaled) + "d";
        }
        long rounded = Math.round(scaled);
        if (tag instanceof net.minecraft.nbt.ByteTag) {
            return Byte.toString((byte) rounded) + "b";
        }
        if (tag instanceof net.minecraft.nbt.ShortTag) {
            return Short.toString((short) rounded) + "s";
        }
        if (tag instanceof net.minecraft.nbt.IntTag) {
            return Integer.toString((int) rounded);
        }
        if (tag instanceof net.minecraft.nbt.LongTag) {
            return Long.toString(rounded) + "L";
        }
        return Long.toString(rounded);
    }

    private record CommandContextData(Tag baseTag, CommandSourceStack source) {
    }

}
