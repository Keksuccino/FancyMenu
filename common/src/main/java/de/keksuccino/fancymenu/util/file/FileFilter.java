package de.keksuccino.fancymenu.util.file;

import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import org.jetbrains.annotations.NotNull;
import java.io.File;

@FunctionalInterface
public interface FileFilter {

    FileFilter RESOURCE_NAME_FILTER = file -> {
        String name = GameDirectoryUtils.getPathWithoutGameDirectory(file.getAbsolutePath()).replace("/", "").replace("\\", "");
        return CharacterFilter.buildResourceNameFilter().isAllowedText(name);
    };
    FileFilter WAV_AUDIO_FILE_FILTER = FileTypes.WAV_AUDIO::isFileTypeLocal;
    FileFilter OGG_AUDIO_FILE_FILTER = FileTypes.OGG_AUDIO::isFileTypeLocal;
    FileFilter TXT_FILE_FILTER = FileTypes.TXT_TEXT::isFileTypeLocal;
    FileFilter PLAIN_TEXT_FILE_FILTER = file -> {
        for (TextFileType type : FileTypes.getAllTextFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };
    FileFilter IMAGE_FILE_FILTER = file -> {
        for (ImageFileType type : FileTypes.getAllImageFileTypes()) {
            if (type.isFileTypeLocal(file)) return true;
        }
        return false;
    };

    boolean checkFile(@NotNull File file);

}
