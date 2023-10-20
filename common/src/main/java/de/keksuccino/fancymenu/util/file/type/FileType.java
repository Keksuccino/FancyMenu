package de.keksuccino.fancymenu.util.file.type;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import de.keksuccino.fancymenu.util.file.type.types.*;

/**
 * Defines a specific file type.<br>
 * Has methods to identify local and web sources as the defined file type.<br>
 * Is used to decode files of the defined file type.<br><br>
 *
 * It is not recommended to create subclasses of this class.<br>
 * Instead, use the already defined subclasses for all {@link FileMediaType}s:<br>
 * {@link ImageFileType}, {@link AudioFileType}, {@link VideoFileType}, {@link TextFileType}
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
    protected FileCodec<T> codec;
    protected boolean allowLocation = true;
    protected boolean allowLocal = true;
    protected boolean allowWeb = true;
    @Nullable
    protected Component customDisplayName;

    protected FileType(@NotNull FileCodec<T> codec) {
        this.mediaType = FileMediaType.OTHER;
        this.codec = codec;
    }

    protected FileType(@NotNull FileCodec<T> codec, @Nullable String mimeType, @NotNull FileMediaType mediaType, @NotNull String... extensions) {
        Arrays.asList(extensions).forEach(this::addExtension);
        this.mediaType = mediaType;
        this.mimeType = mimeType;
        this.codec = codec;
    }

    public boolean isFileTypeLocation(@NotNull ResourceLocation location) {
        return this.extensions.contains(Files.getFileExtension(location.getPath()).toLowerCase());
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

    public FileType<T> addExtension(@NotNull String extension) {
        extension = Objects.requireNonNull(extension).toLowerCase().replace(".", "").replace(" ", "");
        if (this.extensions.contains(extension)) return this;
        this.extensions.add(extension);
        return this;
    }

    public FileType<T> removeExtension(@NotNull String extension) {
        extension = Objects.requireNonNull(extension).toLowerCase().replace(".", "").replace(" ", "");
        while (this.extensions.contains(extension)) {
            this.extensions.remove(extension);
        }
        return this;
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

    public FileType<T> setCodec(@NotNull FileCodec<T> codec) {
        this.codec = Objects.requireNonNull(codec);
        return this;
    }

    public boolean isLocationAllowed() {
        return this.allowLocation;
    }

    public FileType<T> setLocationAllowed(boolean allowLocation) {
        this.allowLocation = allowLocation;
        return this;
    }

    public boolean isLocalAllowed() {
        return this.allowLocal;
    }

    public FileType<T> setLocalAllowed(boolean allowLocal) {
        this.allowLocal = allowLocal;
        return this;
    }

    public boolean isWebAllowed() {
        return this.allowWeb;
    }

    public FileType<T> setWebAllowed(boolean allowWeb) {
        this.allowWeb = allowWeb;
        return this;
    }

    @NotNull
    public Component getDisplayName() {
        if (this.customDisplayName != null) return this.customDisplayName;
        if (!this.extensions.isEmpty()) return Component.literal(this.extensions.get(0).toUpperCase());
        return Component.empty();
    }

    public FileType<T> setCustomDisplayName(@Nullable Component name) {
        this.customDisplayName = name;
        return this;
    }

    @Override
    public String toString() {
        return "FileType{" +
                "extensions=" + extensions +
                ", mediaType=" + mediaType +
                ", mimeType='" + mimeType + '\'' +
                ", allowLocation=" + allowLocation +
                ", allowLocal=" + allowLocal +
                ", allowWeb=" + allowWeb +
                '}';
    }

}
