package de.keksuccino.fancymenu.util.file.type.groups;

import de.keksuccino.fancymenu.util.file.type.types.*;
import net.minecraft.network.chat.Component;

public class FileTypeGroups {

    public static final FileTypeGroup<ImageFileType> IMAGE_TYPES = new FileTypeGroup<>(FileTypes::getAllImageFileTypes, Component.translatable("fancymenu.file_types.groups.image"));
    public static final FileTypeGroup<AudioFileType> AUDIO_TYPES = new FileTypeGroup<>(FileTypes::getAllAudioFileTypes, Component.translatable("fancymenu.file_types.groups.audio"));
    public static final FileTypeGroup<VideoFileType> VIDEO_TYPES = new FileTypeGroup<>(FileTypes::getAllVideoFileTypes, Component.translatable("fancymenu.file_types.groups.video"));
    public static final FileTypeGroup<TextFileType> TEXT_TYPES = new FileTypeGroup<>(FileTypes::getAllTextFileTypes, Component.translatable("fancymenu.file_types.groups.text"));

}
