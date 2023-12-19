package de.keksuccino.fancymenu.util.file.type.groups;

import de.keksuccino.fancymenu.util.file.type.types.*;
import net.minecraft.network.chat.Component;

public class FileTypeGroups {

    public static final Component IMAGE_GROUP_COMPONENT = Component.translatable("fancymenu.file_types.groups.image");
    public static final Component AUDIO_GROUP_COMPONENT = Component.translatable("fancymenu.file_types.groups.audio");
    public static final Component VIDEO_GROUP_COMPONENT = Component.translatable("fancymenu.file_types.groups.video");
    public static final Component TEXT_GROUP_COMPONENT = Component.translatable("fancymenu.file_types.groups.text");

    public static final FileTypeGroup<ImageFileType> IMAGE_TYPES = new FileTypeGroup<>(FileTypes::getAllImageFileTypes, IMAGE_GROUP_COMPONENT);
    public static final FileTypeGroup<AudioFileType> AUDIO_TYPES = new FileTypeGroup<>(FileTypes::getAllAudioFileTypes, AUDIO_GROUP_COMPONENT);
    public static final FileTypeGroup<VideoFileType> VIDEO_TYPES = new FileTypeGroup<>(FileTypes::getAllVideoFileTypes, VIDEO_GROUP_COMPONENT);
    public static final FileTypeGroup<TextFileType> TEXT_TYPES = new FileTypeGroup<>(FileTypes::getAllTextFileTypes, TEXT_GROUP_COMPONENT);

}
