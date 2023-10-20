package de.keksuccino.fancymenu.util.file;

import de.keksuccino.fancymenu.util.file.type.types.*;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import org.jetbrains.annotations.NotNull;
import java.io.File;

@FunctionalInterface
public interface FileFilter {

    FileFilter RESOURCE_NAME_FILTER = file -> {
        String name = GameDirectoryUtils.getPathWithoutGameDirectory(file.getAbsolutePath()).replace("/", "").replace("\\", "");
        return CharacterFilter.buildResourceNameFilter().isAllowedText(name);
    };

    FileFilter IMAGE_FILE_FILTER = file -> {
        for (ImageFileType type : FileTypes.getAllImageFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };
    FileFilter AUDIO_FILE_FILTER = file -> {
        for (AudioFileType type : FileTypes.getAllAudioFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };
    FileFilter VIDEO_FILE_FILTER = file -> {
        for (VideoFileType type : FileTypes.getAllVideoFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };
    FileFilter TEXT_FILE_FILTER = file -> {
        for (TextFileType type : FileTypes.getAllTextFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };

    boolean checkFile(@NotNull File file);

}
