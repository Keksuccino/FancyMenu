package de.keksuccino.fancymenu.util.file.type;

import com.google.common.io.Files;
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
 * @param <T> The object type returned by the read() methods of the {@link FileCodec} of this {@link FileType}.
 */
@SuppressWarnings("unused")
public class FileType<T> {

    protected final List<String> extensions = new ArrayList<>();
    @NotNull
    protected FileMediaType mediaType;
    @Nullable
    protected String mimeType;
    @NotNull
    protected final FileCodec<T> codec;

    public FileType(@NotNull FileCodec<T> codec) {
        this.mediaType = FileMediaType.OTHER;
        this.codec = codec;
    }

    public FileType(@NotNull FileCodec<T> codec, @Nullable String mimeType, @NotNull FileMediaType mediaType, @NotNull String... extensions) {
        Arrays.asList(extensions).forEach(s -> this.extensions.add(s.toLowerCase().replace(".", "").replace(" ", "")));
        this.mediaType = mediaType;
        this.mimeType = mimeType;
        this.codec = codec;
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

    @NotNull
    public FileCodec<T> getCodec() {
        return this.codec;
    }

}
