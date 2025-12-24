package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.player;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class HasPlayerPermissionLevelRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public HasPlayerPermissionLevelRequirement() {
        super("fancymenu_loading_requirement_has_player_permission_level");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        int requiredLevel;
        try {
            requiredLevel = Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            LOGGER.warn("[FANCYMENU] Invalid permission level '{}' provided to '{}' requirement!", trimmed, this.getIdentifier());
            return false;
        }
        PermissionLevel permissionLevel = PermissionLevel.byId(requiredLevel);
        if (permissionLevel == PermissionLevel.ALL) {
            return true;
        }
        try {
            ClientLevel level = Minecraft.getInstance().level;
            LocalPlayer player = Minecraft.getInstance().player;
            if ((level != null) && (player != null)) {
                PermissionSet permissionSet = player.permissions();
                return permissionSet.hasPermission(new Permission.HasCommandLevel(permissionLevel));
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
        }
        return false;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.world.has_player_permission_level");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.world.has_player_permission_level.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.requirements.world.has_player_permission_level.value_name");
    }

    @Override
    public String getValuePreset() {
        return String.valueOf(PermissionLevel.GAMEMASTERS.id());
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }
}
