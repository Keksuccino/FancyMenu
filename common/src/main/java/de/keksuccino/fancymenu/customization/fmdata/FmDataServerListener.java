package de.keksuccino.fancymenu.customization.fmdata;

import com.mojang.brigadier.StringReader;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FmDataServerListener {

    private static final Logger LOGGER = LogManager.getLogger();

    public String listener_name = "";
    public String matching_type_identifier = FmDataMatchingType.EQUALS.getIdentifier();
    public String matching_type_data = FmDataMatchingType.EQUALS.getIdentifier();
    public boolean ignore_case_identifier = false;
    public boolean ignore_case_data = false;
    @Nullable
    public String fire_for_player = null;
    public String listen_for_identifier = "*";
    public String listen_for_data = "*";
    public List<String> commands_to_execute_on_fire = new ArrayList<>();

    public void normalize() {
        if (this.listener_name == null) {
            this.listener_name = "";
        } else {
            this.listener_name = this.listener_name.trim();
        }
        if (this.matching_type_identifier == null || (FmDataMatchingType.fromIdentifier(this.matching_type_identifier) == null)) {
            this.matching_type_identifier = FmDataMatchingType.EQUALS.getIdentifier();
        }
        if (this.matching_type_data == null || (FmDataMatchingType.fromIdentifier(this.matching_type_data) == null)) {
            this.matching_type_data = FmDataMatchingType.EQUALS.getIdentifier();
        }
        if (this.listen_for_identifier == null) {
            this.listen_for_identifier = "";
        }
        if (this.listen_for_data == null) {
            this.listen_for_data = "";
        }
        if (this.fire_for_player != null) {
            String trimmed = this.fire_for_player.trim();
            if (trimmed.isBlank() || "*".equals(trimmed)) {
                this.fire_for_player = null;
            } else {
                this.fire_for_player = trimmed;
            }
        }
        if ((this.commands_to_execute_on_fire == null) || this.commands_to_execute_on_fire.isEmpty()) {
            this.commands_to_execute_on_fire = new ArrayList<>();
            return;
        }
        List<String> filteredCommands = new ArrayList<>();
        for (String command : this.commands_to_execute_on_fire) {
            if (command != null) {
                String cleaned = command.trim();
                if (!cleaned.isBlank()) {
                    filteredCommands.add(cleaned);
                }
            }
        }
        this.commands_to_execute_on_fire = filteredCommands;
    }

    public boolean hasValidName() {
        return CharacterFilter.buildResourceNameFilter().isAllowedText(this.listener_name) && !this.listener_name.isBlank();
    }

    public boolean hasCommands() {
        return (this.commands_to_execute_on_fire != null) && !this.commands_to_execute_on_fire.isEmpty();
    }

    public boolean shouldFire(@NotNull ServerPlayer sender, @NotNull String incomingIdentifier, @NotNull String incomingData) {
        if (!this.matchesSender(sender)) {
            return false;
        }

        FmDataMatchingType identifierMatchingType = Objects.requireNonNullElse(
                FmDataMatchingType.fromIdentifier(this.matching_type_identifier),
                FmDataMatchingType.EQUALS
        );
        FmDataMatchingType dataMatchingType = Objects.requireNonNullElse(
                FmDataMatchingType.fromIdentifier(this.matching_type_data),
                FmDataMatchingType.EQUALS
        );

        boolean identifierMatches = identifierMatchingType.matches(incomingIdentifier, this.listen_for_identifier, this.ignore_case_identifier);
        boolean dataMatches = dataMatchingType.matches(incomingData, this.listen_for_data, this.ignore_case_data);

        return identifierMatches && dataMatches;
    }

    private boolean matchesSender(@NotNull ServerPlayer sender) {
        String playerFilter = this.fire_for_player;
        if ((playerFilter == null) || playerFilter.isBlank() || "*".equals(playerFilter)) {
            return true;
        }

        try {
            EntitySelector selector = new EntitySelectorParser(new StringReader(playerFilter), true).parse();
            CommandSourceStack source = sender.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            for (ServerPlayer matchedPlayer : selector.findPlayers(source)) {
                if (matchedPlayer.getUUID().equals(sender.getUUID())) {
                    return true;
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to evaluate FMData listener player filter '{}' for listener '{}'", playerFilter, this.listener_name, ex);
        }
        return false;
    }

    @NotNull
    public String describePlayerFilter() {
        String value = this.fire_for_player;
        if ((value == null) || value.isBlank() || "*".equals(value)) {
            return "ANY_PLAYER";
        }
        return value;
    }

    @NotNull
    public String matchingTypeIdentifierNormalized() {
        FmDataMatchingType type = Objects.requireNonNullElse(FmDataMatchingType.fromIdentifier(this.matching_type_identifier), FmDataMatchingType.EQUALS);
        return type.getIdentifier().toLowerCase(Locale.ROOT);
    }

    @NotNull
    public String matchingTypeDataNormalized() {
        FmDataMatchingType type = Objects.requireNonNullElse(FmDataMatchingType.fromIdentifier(this.matching_type_data), FmDataMatchingType.EQUALS);
        return type.getIdentifier().toLowerCase(Locale.ROOT);
    }

}
