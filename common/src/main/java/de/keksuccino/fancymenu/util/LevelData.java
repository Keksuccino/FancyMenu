package de.keksuccino.fancymenu.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class LevelData {

    private static final Gson GSON = new GsonBuilder().create();

    public String file_name;
    public String display_name;
    public boolean requires_manual_conversion;
    public boolean locked;
    public boolean experimental;
    public String icon_path;
    public String game_type;
    public String difficulty;
    public boolean allow_commands;
    public String settings_level_name;
    public long last_played;
    public int level_data_version;
    public String minecraft_version_name;
    public boolean snapshot;
    public boolean can_edit;
    public boolean can_recreate;
    public boolean can_delete;

    @NotNull
    public static LevelData fromLevelSummary(@NotNull LevelSummary summary) {

        LevelData data = new LevelData();

        data.file_name = summary.getLevelId();
        data.display_name = summary.getLevelName();
        data.requires_manual_conversion = summary.requiresManualConversion();
        data.locked = summary.isLocked();
        data.experimental = summary.isExperimental();
        data.icon_path = summary.getIcon().toAbsolutePath().toString();
        data.game_type = summary.getGameMode().getSerializedName();
        data.difficulty = summary.getSettings().difficulty().getSerializedName();
        data.allow_commands = summary.getSettings().allowCommands();
        data.settings_level_name = summary.getSettings().levelName();
        data.last_played = summary.getLastPlayed();
        data.level_data_version = summary.levelVersion().levelDataVersion();
        data.minecraft_version_name = summary.levelVersion().minecraftVersionName();
        data.snapshot = summary.levelVersion().snapshot();
        data.can_edit = summary.canEdit();
        data.can_recreate = summary.canRecreate();
        data.can_delete = summary.canDelete();

        return data;

    }

    @NotNull
    public static LevelData deserialize(@NotNull String json) {
        return Objects.requireNonNullElse(GSON.fromJson(json, LevelData.class), new LevelData());
    }

    @NotNull
    public String serialize() {
        return GSON.toJson(this);
    }

}
