package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.keksuccino.fancymenu.fmdata.FmDataMatchingType;
import de.keksuccino.fancymenu.fmdata.FmDataServerListener;
import de.keksuccino.fancymenu.fmdata.FmDataServerListenerHandler;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.fmdata.FmDataToClientPacket;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.konkrete.command.CommandUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FmDataCommand {

    private static final CharacterFilter LISTENER_NAME_FILTER = CharacterFilter.buildResourceNameFilter();
    private static final int COLOR_IDENTIFIER_LABEL = 0x59C8FF;
    private static final int COLOR_IDENTIFIER_VALUE = 0xA4E3FF;
    private static final int COLOR_DATA_LABEL = 0xFFB86B;
    private static final int COLOR_DATA_VALUE = 0xFFE1BA;
    private static final int COLOR_MATCH_META = 0xA8B0BA;
    private static final int COLOR_MATCH_TRUE = 0x7EE787;
    private static final int COLOR_MATCH_FALSE = 0xFF7B72;

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {

        FmDataServerListenerHandler.init();

        dispatcher.register(Commands.literal("fmdata")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("send")
                        .then(Commands.argument("target_player", EntityArgument.players())
                                .then(Commands.argument("data_identifier", StringArgumentType.string())
                                        .then(Commands.argument("string_data", StringArgumentType.greedyString())
                                                .executes(FmDataCommand::sendDataToClients)
                                        )
                                )
                        )
                )
                .then(Commands.literal("listener")
                        .then(Commands.literal("list")
                                .executes(context -> listListeners(context.getSource()))
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("listener_name", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandUtils.getStringSuggestions(builder, FmDataServerListenerHandler.getListenerNameSuggestions()))
                                        .executes(FmDataCommand::removeListener)
                                )
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("unique_listener_name", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandUtils.getStringSuggestions(builder, "<unique_listener_name>"))
                                        .then(Commands.argument("matching_type_identifier", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandUtils.getStringSuggestions(builder, FmDataMatchingType.identifiers().toArray(new String[0])))
                                                .then(Commands.argument("matching_type_data", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandUtils.getStringSuggestions(builder, FmDataMatchingType.identifiers().toArray(new String[0])))
                                                        .then(Commands.argument("ignore_case_identifier", BoolArgumentType.bool())
                                                                .then(Commands.argument("ignore_case_data", BoolArgumentType.bool())
                                                                        .then(Commands.argument("fire_for_player", EntityArgument.players())
                                                                                .then(Commands.argument("listen_for_identifier", StringArgumentType.string())
                                                                                        .then(Commands.argument("listen_for_data", StringArgumentType.string())
                                                                                                .then(Commands.argument("commands_to_execute_on_fire", StringArgumentType.greedyString())
                                                                                                        .executes(context -> addListener(context, getRawArgument(context, "fire_for_player")))
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("edit")
                                .then(Commands.argument("listener_name", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandUtils.getStringSuggestions(builder, FmDataServerListenerHandler.getListenerNameSuggestions()))
                                        .then(Commands.argument("matching_type_identifier", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandUtils.getStringSuggestions(builder, FmDataMatchingType.identifiers().toArray(new String[0])))
                                                .then(Commands.argument("matching_type_data", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandUtils.getStringSuggestions(builder, FmDataMatchingType.identifiers().toArray(new String[0])))
                                                        .then(Commands.argument("ignore_case_identifier", BoolArgumentType.bool())
                                                                .then(Commands.argument("ignore_case_data", BoolArgumentType.bool())
                                                                        .then(Commands.argument("fire_for_player", EntityArgument.players())
                                                                                .then(Commands.argument("listen_for_identifier", StringArgumentType.string())
                                                                                        .then(Commands.argument("listen_for_data", StringArgumentType.string())
                                                                                                .then(Commands.argument("commands_to_execute_on_fire", StringArgumentType.greedyString())
                                                                                                        .executes(context -> editListener(context, getRawArgument(context, "fire_for_player")))
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int sendDataToClients(@NotNull CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target_player");
        String dataIdentifier = StringArgumentType.getString(context, "data_identifier");
        String data = normalizeGreedyString(StringArgumentType.getString(context, "string_data"));

        String sentBy = getSentBy(source.getServer());

        int targetedCount = 0;
        int fancyMenuClientCount = 0;
        for (ServerPlayer target : targets) {
            targetedCount++;
            if (PacketHandler.isFancyMenuClient(target)) {
                fancyMenuClientCount++;
            }

            FmDataToClientPacket packet = new FmDataToClientPacket();
            packet.data_identifier = dataIdentifier;
            packet.data = data;
            packet.sent_by = sentBy;
            PacketHandler.sendToClient(target, packet);
        }

        int finalFancyMenuClientCount = fancyMenuClientCount;
        int finalTargetedCount = targetedCount;
        source.sendSuccess(() -> Component.literal("[FancyMenu] Sent data identifier '")
                .append(Component.literal(dataIdentifier).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("' to "))
                .append(Component.literal(String.valueOf(finalFancyMenuClientCount)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" FancyMenu client(s) out of "))
                .append(Component.literal(String.valueOf(finalTargetedCount)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" target player(s).")), false);

        return fancyMenuClientCount;
    }

    private static int addListener(@NotNull CommandContext<CommandSourceStack> context, @Nullable String fireForPlayerSelector) {
        CommandSourceStack source = context.getSource();
        String listenerName = StringArgumentType.getString(context, "unique_listener_name");

        if (!isListenerNameValid(listenerName)) {
            source.sendFailure(Component.literal("[FancyMenu] Invalid listener name! Use only [a-z], [0-9], '-' and '_'."));
            return 0;
        }
        if (FmDataServerListenerHandler.isListenerNameAlreadyUsed(listenerName)) {
            source.sendFailure(Component.literal("[FancyMenu] A listener with that name already exists: " + listenerName));
            return 0;
        }

        FmDataServerListener listener = buildListenerFromContext(context, listenerName, fireForPlayerSelector);
        if (listener == null) {
            source.sendFailure(Component.literal("[FancyMenu] Failed to create listener. Check your matching type(s) and command list."));
            return 0;
        }

        if (!FmDataServerListenerHandler.addListener(listener)) {
            source.sendFailure(Component.literal("[FancyMenu] Failed to add listener: " + listenerName));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[FancyMenu] Added listener '")
                .append(Component.literal(listenerName).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("'.")), false);
        return 1;
    }

    private static int editListener(@NotNull CommandContext<CommandSourceStack> context, @Nullable String fireForPlayerSelector) {
        CommandSourceStack source = context.getSource();
        String listenerName = StringArgumentType.getString(context, "listener_name");

        if (FmDataServerListenerHandler.getListener(listenerName) == null) {
            source.sendFailure(Component.literal("[FancyMenu] Listener not found: " + listenerName));
            return 0;
        }

        FmDataServerListener listener = buildListenerFromContext(context, listenerName, fireForPlayerSelector);
        if (listener == null) {
            source.sendFailure(Component.literal("[FancyMenu] Failed to edit listener. Check your matching type(s) and command list."));
            return 0;
        }

        if (!FmDataServerListenerHandler.editListener(listenerName, listener)) {
            source.sendFailure(Component.literal("[FancyMenu] Failed to edit listener: " + listenerName));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[FancyMenu] Updated listener '")
                .append(Component.literal(listenerName).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("'.")), false);
        return 1;
    }

    private static int removeListener(@NotNull CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String listenerName = StringArgumentType.getString(context, "listener_name");

        if (!FmDataServerListenerHandler.removeListener(listenerName)) {
            source.sendFailure(Component.literal("[FancyMenu] Listener not found: " + listenerName));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[FancyMenu] Removed listener '")
                .append(Component.literal(listenerName).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("'.")), false);
        return 1;
    }

    private static int listListeners(@NotNull CommandSourceStack source) {
        List<FmDataServerListener> listeners = FmDataServerListenerHandler.getListeners();

        source.sendSuccess(() -> Component.literal("---------------------------------------------").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH), false);
        source.sendSuccess(() -> Component.literal("FancyMenu FMData Server Listeners").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Total listeners: " + listeners.size()).withStyle(ChatFormatting.GRAY), false);

        if (listeners.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No server-side FMData listeners configured yet.").withStyle(ChatFormatting.RED), false);
            source.sendSuccess(() -> Component.literal("Use /fmdata listener add ... to create one.").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("---------------------------------------------").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH), false);
            return 1;
        }

        int index = 1;
        for (FmDataServerListener listener : listeners) {
            int entryIndex = index;
            source.sendSuccess(() -> Component.literal("" + entryIndex + ") " + listener.listener_name).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), false);
            source.sendSuccess(() -> buildMatchLine(
                    "Identifier Match",
                    listener.matchingTypeIdentifierNormalized(),
                    listener.listen_for_identifier,
                    listener.ignore_case_identifier,
                    COLOR_IDENTIFIER_LABEL,
                    COLOR_IDENTIFIER_VALUE
            ), false);
            source.sendSuccess(() -> buildMatchLine(
                    "Data Match",
                    listener.matchingTypeDataNormalized(),
                    listener.listen_for_data,
                    listener.ignore_case_data,
                    COLOR_DATA_LABEL,
                    COLOR_DATA_VALUE
            ), false);
            source.sendSuccess(() -> Component.literal("   Fire For Player: " + listener.describePlayerFilter()).withStyle(ChatFormatting.AQUA), false);
            source.sendSuccess(() -> Component.literal("   Commands on Fire: " + listener.commands_to_execute_on_fire.size()).withStyle(ChatFormatting.LIGHT_PURPLE), false);

            for (String command : listener.commands_to_execute_on_fire) {
                source.sendSuccess(() -> Component.literal("      - " + command).withStyle(ChatFormatting.GRAY), false);
            }

            index++;
        }

        source.sendSuccess(() -> Component.literal("---------------------------------------------").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH), false);
        return listeners.size();
    }

    @Nullable
    private static FmDataServerListener buildListenerFromContext(@NotNull CommandContext<CommandSourceStack> context, @NotNull String listenerName, @Nullable String fireForPlayerSelector) {
        String matchingTypeIdentifier = StringArgumentType.getString(context, "matching_type_identifier");
        String matchingTypeData = StringArgumentType.getString(context, "matching_type_data");

        FmDataMatchingType matchingIdentifier = FmDataMatchingType.fromIdentifier(matchingTypeIdentifier);
        FmDataMatchingType matchingData = FmDataMatchingType.fromIdentifier(matchingTypeData);
        if ((matchingIdentifier == null) || (matchingData == null)) {
            return null;
        }

        String listenForIdentifier = StringArgumentType.getString(context, "listen_for_identifier");
        String listenForData = StringArgumentType.getString(context, "listen_for_data");
        boolean ignoreCaseIdentifier = BoolArgumentType.getBool(context, "ignore_case_identifier");
        boolean ignoreCaseData = BoolArgumentType.getBool(context, "ignore_case_data");

        String commandInput = StringArgumentType.getString(context, "commands_to_execute_on_fire");
        List<String> commands = splitCommands(normalizeGreedyString(commandInput));
        if (commands.isEmpty()) {
            return null;
        }

        FmDataServerListener listener = new FmDataServerListener();
        listener.listener_name = listenerName;
        listener.matching_type_identifier = matchingIdentifier.getIdentifier();
        listener.matching_type_data = matchingData.getIdentifier();
        listener.ignore_case_identifier = ignoreCaseIdentifier;
        listener.ignore_case_data = ignoreCaseData;
        listener.listen_for_identifier = Objects.requireNonNullElse(listenForIdentifier, "");
        listener.listen_for_data = Objects.requireNonNullElse(listenForData, "");
        listener.fire_for_player = normalizeFireForPlayer(fireForPlayerSelector);
        listener.commands_to_execute_on_fire = commands;
        listener.normalize();
        return listener;
    }

    @Nullable
    private static String normalizeFireForPlayer(@Nullable String fireForPlayerSelector) {
        if (fireForPlayerSelector == null) {
            return null;
        }
        String trimmed = fireForPlayerSelector.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed;
    }

    @NotNull
    private static String getSentBy(@NotNull MinecraftServer server) {
        if (!server.isDedicatedServer()) {
            return "integrated_server";
        }

        String localIp = server.getLocalIp();
        if ((localIp == null) || localIp.isBlank()) {
            return "unknown_server";
        }
        return localIp;
    }

    private static boolean isListenerNameValid(@NotNull String listenerName) {
        return LISTENER_NAME_FILTER.isAllowedText(listenerName) && !listenerName.isBlank();
    }

    @NotNull
    private static String normalizeGreedyString(@NotNull String value) {
        String trimmed = value.trim();
        if ((trimmed.length() >= 2) && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return value;
    }

    @NotNull
    private static List<String> splitCommands(@NotNull String commandInput) {
        List<String> commands = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < commandInput.length(); ) {
            if (commandInput.startsWith("\\|\\|\\|", i)) {
                current.append("|||");
                i += 6;
                continue;
            }
            if (commandInput.startsWith("|||", i)) {
                addCommandIfNotBlank(commands, current.toString());
                current.setLength(0);
                i += 3;
                continue;
            }
            current.append(commandInput.charAt(i));
            i++;
        }

        addCommandIfNotBlank(commands, current.toString());
        return commands;
    }

    private static void addCommandIfNotBlank(@NotNull List<String> commands, @NotNull String command) {
        String cleaned = command.trim();
        if (!cleaned.isBlank()) {
            commands.add(cleaned);
        }
    }

    @NotNull
    private static Component buildMatchLine(
            @NotNull String label,
            @NotNull String matchingType,
            @NotNull String value,
            boolean ignoreCase,
            int labelColor,
            int valueColor
    ) {
        return Component.literal("   " + label + ": ").withStyle(style -> style.withColor(labelColor))
                .append(Component.literal(matchingType).withStyle(style -> style.withColor(valueColor)))
                .append(Component.literal(" | value='").withStyle(style -> style.withColor(COLOR_MATCH_META)))
                .append(Component.literal(value).withStyle(style -> style.withColor(valueColor)))
                .append(Component.literal("' | ignore_case=").withStyle(style -> style.withColor(COLOR_MATCH_META)))
                .append(Component.literal(String.valueOf(ignoreCase)).withStyle(style -> style.withColor(ignoreCase ? COLOR_MATCH_TRUE : COLOR_MATCH_FALSE)));
    }

    @Nullable
    private static String getRawArgument(@NotNull CommandContext<CommandSourceStack> context, @NotNull String argumentName) {
        for (ParsedCommandNode<CommandSourceStack> parsedNode : context.getNodes()) {
            if (argumentName.equals(parsedNode.getNode().getName())) {
                return parsedNode.getRange().get(context.getInput());
            }
        }
        return null;
    }

}
