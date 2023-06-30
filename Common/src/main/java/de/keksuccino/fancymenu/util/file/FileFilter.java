package de.keksuccino.fancymenu.util.file;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@FunctionalInterface
public interface FileFilter {

    FileFilter RESOURCE_NAME_FILTER = file -> {
        String name = ScreenCustomization.getPathWithoutGameDirectory(file.getAbsolutePath()).replace("/", "").replace("\\", "");
        return CharacterFilter.getBasicFilenameCharacterFilter().isAllowedText(name);
    };
    FileFilter WAV_AUDIO_FILE_FILTER = file -> {
        return (file.getPath().toLowerCase().endsWith(".wav"));
    };
    FileFilter OGG_AUDIO_FILE_FILTER = file -> {
        return (file.getPath().toLowerCase().endsWith(".ogg"));
    };
    FileFilter TXT_FILE_FILTER = file -> {
        return (file.getPath().toLowerCase().endsWith(".txt"));
    };
    FileFilter PLAIN_TEXT_FILE_FILTER = file -> {
        if (file.getPath().toLowerCase().endsWith(".txt")) return true;
        if (file.getPath().toLowerCase().endsWith(".json")) return true;
        if (file.getPath().toLowerCase().endsWith(".log")) return true;
        if (file.getPath().toLowerCase().endsWith(".lang")) return true;
        if (file.getPath().toLowerCase().endsWith(".local")) return true;
        if (file.getPath().toLowerCase().endsWith(".properties")) return true;
        return false;
    };
    FileFilter IMAGE_FILE_FILTER = file -> {
        if (file.getPath().toLowerCase().endsWith(".png")) return true;
        if (file.getPath().toLowerCase().endsWith(".jpg")) return true;
        if (file.getPath().toLowerCase().endsWith(".jpeg")) return true;
        return false;
    };
    FileFilter IMAGE_AND_GIF_FILE_FILTER = file -> {
        if (file.getPath().toLowerCase().endsWith(".png")) return true;
        if (file.getPath().toLowerCase().endsWith(".jpg")) return true;
        if (file.getPath().toLowerCase().endsWith(".jpeg")) return true;
        if (file.getPath().toLowerCase().endsWith(".gif")) return true;
        return false;
    };

    boolean checkFile(@NotNull File file);

}
