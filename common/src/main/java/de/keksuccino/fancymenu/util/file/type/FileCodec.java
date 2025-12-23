package de.keksuccino.fancymenu.util.file.type;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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

    /**
     * Should only be used for placeholder-like cases.
     */
    @NotNull public static <T> FileCodec<T> empty(@NotNull Class<T> type) {
        return new FileCodec<T>() {
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                return null;
            }
            @Override
            public @Nullable T readLocation(@NotNull Identifier location) {
                return null;
            }
            @Override
            public @Nullable T readLocal(@NotNull File file) {
                return null;
            }
            @Override
            public @Nullable T readWeb(@NotNull String fileUrl) {
                return null;
            }
        };
    }

    @NotNull
    public static <T> FileCodec<T> generic(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        ConsumingSupplier<ResourceLocation, T> locationReader = consumes -> {
          try {
              InputStream in = Minecraft.getInstance().getResourceManager().open(consumes);
              return streamReader.get(in);
          } catch (Exception ex) {
              ex.printStackTrace();
          }
          return null;
        };
        return basic(type, streamReader, locationReader);
    }

    @NotNull
    public static <T> FileCodec<T> basic(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<ResourceLocation, T> locationReader) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(streamReader);
        return new FileCodec<T>() {
            @Override
            public @Nullable T read(@NotNull InputStream in) {
                Objects.requireNonNull(in);
                return streamReader.get(in);
            }
            @Override
            public @Nullable T readLocation(@NotNull Identifier location) {
                Objects.requireNonNull(location);
                return locationReader.get(location);
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
    public static <T> FileCodec<T> basicWithLocal(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<ResourceLocation, T> locationReader, @NotNull ConsumingSupplier<File, T> fileReader) {
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
            public @Nullable T readLocation(@NotNull Identifier location) {
                Objects.requireNonNull(location);
                return locationReader.get(location);
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
    public static <T> FileCodec<T> basicWithWeb(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<ResourceLocation, T> locationReader, @NotNull ConsumingSupplier<String, T> urlReader) {
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
            public @Nullable T readLocation(@NotNull Identifier location) {
                Objects.requireNonNull(location);
                return locationReader.get(location);
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
    public static <T> FileCodec<T> advanced(@NotNull Class<T> type, @NotNull ConsumingSupplier<InputStream, T> streamReader, @NotNull ConsumingSupplier<ResourceLocation, T> locationReader, @NotNull ConsumingSupplier<File, T> fileReader, @NotNull ConsumingSupplier<String, T> urlReader) {
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
            public @Nullable T readLocation(@NotNull Identifier location) {
                Objects.requireNonNull(location);
                return locationReader.get(location);
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
    public abstract T readLocation(@NotNull Identifier location);

    @Nullable
    public abstract T readLocal(@NotNull File file);

    @Nullable
    public abstract T readWeb(@NotNull String fileUrl);

    @Nullable
    public T read(@NotNull ResourceSource resourceSource) {
        Objects.requireNonNull(resourceSource);
        try {
            if (resourceSource.getSourceType() == ResourceSourceType.LOCATION) {
                Identifier loc = Identifier.tryParse(resourceSource.getSourceWithoutPrefix());
                return (loc != null) ? this.readLocation(loc) : null;
            }
            if (resourceSource.getSourceType() == ResourceSourceType.LOCAL) {
                return this.readLocal(new File(resourceSource.getSourceWithoutPrefix()));
            }
            if (resourceSource.getSourceType() == ResourceSourceType.WEB) {
                return this.readWeb(resourceSource.getSourceWithoutPrefix());
            }
        } catch (Exception ignore) {}
        return null;
    }

}
