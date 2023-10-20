package de.keksuccino.fancymenu.util.file.type.groups;

import de.keksuccino.fancymenu.util.file.type.FileType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class FileTypeGroup<T extends FileType<?>> {

    @NotNull
    protected Supplier<List<T>> typeSupplier;
    @Nullable
    protected Component displayName;

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
