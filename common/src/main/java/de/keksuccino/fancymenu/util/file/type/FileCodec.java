package de.keksuccino.fancymenu.util.file.type;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.WebUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * Used to read files via {@link FileType}.
 *
 * @param <T> The object type returned by the read() methods of the codec.
 */
@SuppressWarnings("unused")
public abstract class FileCodec<T> {

    @NotNull
    public static <T> FileCodec<T> basic(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        return new FileCodec<T>() {
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                Objects.requireNonNull(file);
                try {
                    return streamReader.get(new FileInputStream(file));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                Objects.requireNonNull(fileUrl);
                InputStream in = WebUtils.openResourceStream(fileUrl);
                if (in != null) return streamReader.get(in);
                return null;
            }
        };
    }

    @NotNull
    public static <T> FileCodec<T> basicWithLocal(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<File, T> fileReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        Objects.requireNonNull(fileReader);
        return new FileCodec<T>() {
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                Objects.requireNonNull(file);
                return fileReader.get(file);
            }
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                Objects.requireNonNull(fileUrl);
                InputStream in = WebUtils.openResourceStream(fileUrl);
                if (in != null) return streamReader.get(in);
                return null;
            }
        };
    }

    @NotNull
    public static <T> FileCodec<T> basicWithWeb(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<String, T> urlReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        Objects.requireNonNull(urlReader);
        return new FileCodec<T>() {
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                Objects.requireNonNull(file);
                try {
                    return streamReader.get(new FileInputStream(file));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                Objects.requireNonNull(fileUrl);
                return urlReader.get(fileUrl);
            }
        };
    }

    @NotNull
    public static <T> FileCodec<T> advanced(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<File, T> fileReader, @NotNull ConsumingSupplier<String, T> urlReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        Objects.requireNonNull(fileReader);
        Objects.requireNonNull(urlReader);
        return new FileCodec<T>() {
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                Objects.requireNonNull(file);
                return fileReader.get(file);
            }
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                Objects.requireNonNull(fileUrl);
                return urlReader.get(fileUrl);
            }
        };
    }

    @Nullable
    public abstract T read(@NotNull InputStream in);

    @Nullable
    public abstract T readLocal(@NotNull File file);

    @Nullable
    public abstract T readWeb(@NotNull String fileUrl);

}
