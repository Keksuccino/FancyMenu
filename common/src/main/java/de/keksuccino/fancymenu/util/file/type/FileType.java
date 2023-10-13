package de.keksuccino.fancymenu.util.file.type;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Defines a specific file type.<br>
 * Has methods to identify local and web sources as the defined file type.<br>
 * Is used to decode files of the defined file type.
 *
 * @param <T> The class used for decoded instances of files of the defined file type.
 */
@SuppressWarnings("unused")
public class FileType<T> {

    protected final List<String> extensions = new ArrayList<>();
    @NotNull
    protected FileMediaType mediaType;
    @Nullable
    protected String mimeType;
    @NotNull
    protected ConsumingSupplier<String, T> localCodec = consumes -> null;
    @NotNull
    protected ConsumingSupplier<String, T> webCodec = consumes -> null;

    public FileType() {
        this.mediaType = FileMediaType.OTHER;
    }

    public FileType(@Nullable String mimeType, @NotNull FileMediaType mediaType, @NotNull String... extensions) {
        Arrays.asList(extensions).forEach(s -> this.extensions.add(s.toLowerCase().replace(".", "").replace(" ", "")));
        this.mediaType = mediaType;
        this.mimeType = mimeType;
    }

    @Nullable
    public T decodeLocal(@NotNull String filePath) {
        try {
            return this.localCodec.get(filePath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Nullable
    public T decodeWeb(@NotNull String fileUrl) {
        try {
            return this.webCodec.get(fileUrl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public FileType<T> setLocalCodec(@NotNull ConsumingSupplier<String, T> codec) {
        this.localCodec = codec;
        return this;
    }

    public FileType<T> setWebCodec(@NotNull ConsumingSupplier<String, T> codec) {
        this.webCodec = codec;
        return this;
    }

    public boolean isFileTypeLocal(@NotNull File file) {
        return this.extensions.contains(Files.getFileExtension(file.getPath()).toLowerCase());
    }

    public boolean isFileTypeWeb(@NotNull String fileUrl) {
        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(fileUrl)) return false;
        if (fileUrl.endsWith("/")) fileUrl = fileUrl.substring(0, fileUrl.length()-1);
        for (String extension : this.extensions) {
            if (fileUrl.endsWith("." + extension)) return true;
        }
        return this.isFileTypeWebInternal(fileUrl);
    }

    /**
     * If the base checks aren't enough, this method will open a connection to the web source and tries to get its file type.<br>
     * If the file type is the correct one, return true.
     */
    protected boolean isFileTypeWebInternal(@NotNull String fileUrl) {
        if (this.mimeType == null) return true;
        return Objects.equals(WebUtils.getMimeType(fileUrl), this.mimeType);
    }

    @NotNull
    public List<String> getExtensions() {
        return new ArrayList<>(this.extensions);
    }

    @NotNull
    public FileMediaType getMediaType() {
        return this.mediaType;
    }

    @Nullable
    public String getMimeType() {
        return this.mimeType;
    }

}
