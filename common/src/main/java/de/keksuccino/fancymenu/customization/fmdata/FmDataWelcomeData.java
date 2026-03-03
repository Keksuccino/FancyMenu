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

public class FmDataWelcomeData {

    private static final Logger LOGGER = LogManager.getLogger();

    public String welcome_data_name = "";
    @Nullable
    public String target_player = null;
    public String data_identifier = "";
    public String data = "";

    public void normalize() {
        if (this.welcome_data_name == null) {
            this.welcome_data_name = "";
        } else {
            this.welcome_data_name = this.welcome_data_name.trim();
        }
        if (this.target_player != null) {
            String trimmed = this.target_player.trim();
            if (trimmed.isBlank()) {
                this.target_player = null;
            } else {
                this.target_player = trimmed;
            }
        }
        if (this.data_identifier == null) {
            this.data_identifier = "";
        }
        if (this.data == null) {
            this.data = "";
        }
    }

    public boolean hasValidName() {
        return CharacterFilter.buildResourceNameFilter().isAllowedText(this.welcome_data_name) && !this.welcome_data_name.isBlank();
    }

    public boolean hasValidTargetSelector() {
        String selectorString = this.target_player;
        if ((selectorString == null) || selectorString.isBlank()) {
            return true;
        }
        try {
            new EntitySelectorParser(new StringReader(selectorString), true).parse();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean shouldSendTo(@NotNull ServerPlayer targetPlayer) {
        String playerFilter = this.target_player;
        if ((playerFilter == null) || playerFilter.isBlank()) {
            return true;
        }

        try {
            EntitySelector selector = new EntitySelectorParser(new StringReader(playerFilter), true).parse();
            CommandSourceStack source = targetPlayer.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            for (ServerPlayer matchedPlayer : selector.findPlayers(source)) {
                if (matchedPlayer.getUUID().equals(targetPlayer.getUUID())) {
                    return true;
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to evaluate FMData welcome-data target '{}' for entry '{}'", playerFilter, this.welcome_data_name, ex);
        }

        return false;
    }

    @NotNull
    public String describeTargetPlayer() {
        String value = this.target_player;
        if ((value == null) || value.isBlank()) {
            return "ANY_PLAYER";
        }
        return value;
    }

}
