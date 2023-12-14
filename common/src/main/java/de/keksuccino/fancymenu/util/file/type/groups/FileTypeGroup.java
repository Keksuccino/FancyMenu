package de.keksuccino.fancymenu.util.file.type.groups;

import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.resource.Resource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class FileTypeGroup<T extends FileType<?>> {

    @NotNull
    protected Supplier<List<T>> typeSupplier;
    @Nullable
    protected Component displayName;

    @SuppressWarnings("all")
    @NotNull
    public static FileTypeGroup<FileType<Resource>> allSupported() {
        List<FileType<Resource>> types = new ArrayList<>();
        FileTypes.getAllImageFileTypes().forEach(imageFileType -> types.add((FileType<Resource>)((FileType<?>)imageFileType)));
        FileTypes.getAllAudioFileTypes().forEach(imageFileType -> types.add((FileType<Resource>)((FileType<?>)imageFileType)));
        FileTypes.getAllVideoFileTypes().forEach(imageFileType -> types.add((FileType<Resource>)((FileType<?>)imageFileType)));
        FileTypes.getAllTextFileTypes().forEach(imageFileType -> types.add((FileType<Resource>)((FileType<?>)imageFileType)));
        FileTypeGroup<FileType<Resource>> group = new FileTypeGroup<>(() -> types, null);
        group.setDisplayName(new TranslatableComponent("fancymenu.file_types.groups.all_supported"));
        return group;
    }

    @SafeVarargs
    @NotNull
    public static <T extends FileType<?>> FileTypeGroup<T> of(@NotNull T... types) {
        FileTypeGroup<T> group = new FileTypeGroup<>(() -> Arrays.asList(types));
        if (types.length == 1) group.setDisplayName(types[0].getDisplayName());
        return group;
    }

    public FileTypeGroup(@NotNull Supplier<List<T>> typeSupplier) {
        this(typeSupplier, null);
    }

    public FileTypeGroup(@NotNull Supplier<List<T>> typeSupplier, @Nullable Component displayName) {
        this.typeSupplier = typeSupplier;
        this.displayName = displayName;
    }

    @NotNull
    public List<T> getFileTypes() {
        return Objects.requireNonNullElse(this.typeSupplier.get(), new ArrayList<>());
    }

    @NotNull
    public Supplier<List<T>> getTypeSupplier() {
        return this.typeSupplier;
    }

    public void setTypeSupplier(@NotNull Supplier<List<T>> typeSupplier) {
        this.typeSupplier = typeSupplier;
    }

    @Nullable
    public Component getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(@Nullable Component displayName) {
        this.displayName = displayName;
    }

}
