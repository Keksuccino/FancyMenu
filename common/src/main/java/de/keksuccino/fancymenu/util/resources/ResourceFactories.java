package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ResourceFactories {

    @Nullable
    protected static ConsumingSupplier<String, ITexture> imageFactory = TextureHandler.INSTANCE::getTexture;
    @Nullable
    protected static ConsumingSupplier<String, IAudio> audioFactory;
    @Nullable
    protected static ConsumingSupplier<String, IVideo> videoFactory;
    @Nullable
    protected static ConsumingSupplier<String, TextFileType.PlainText> textFactory;

    @Nullable
    public static ConsumingSupplier<String, ITexture> getImageFactory() {
        return imageFactory;
    }

    public static void setImageFactory(@Nullable ConsumingSupplier<String, ITexture> imageSupplier) {
        ResourceFactories.imageFactory = imageSupplier;
    }

    @Nullable
    public static ConsumingSupplier<String, IAudio> getAudioFactory() {
        return audioFactory;
    }

    public static void setAudioFactory(@Nullable ConsumingSupplier<String, IAudio> audioFactory) {
        ResourceFactories.audioFactory = audioFactory;
    }

    @Nullable
    public static ConsumingSupplier<String, IVideo> getVideoFactory() {
        return videoFactory;
    }

    public static void setVideoFactory(@Nullable ConsumingSupplier<String, IVideo> videoFactory) {
        ResourceFactories.videoFactory = videoFactory;
    }

    @Nullable
    public static ConsumingSupplier<String, TextFileType.PlainText> getTextFactory() {
        return textFactory;
    }

    public static void setTextFactory(@Nullable ConsumingSupplier<String, TextFileType.PlainText> textFactory) {
        ResourceFactories.textFactory = textFactory;
    }

}
