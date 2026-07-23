package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolver;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.DualTextInputWindowBody;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CopyFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public CopyFileAction() {
        super("copy_file_in_game_dir");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        try {
            this.executeWithResolver(value, GameDirectoryActionPathResolver.create());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to copy file in game directory via CopyFileAction: " + value, ex);
        }
    }

    void executeWithResolver(@Nullable String value, @NotNull GameDirectoryActionPathResolver resolver) throws IOException {
        if ((value == null) || !value.contains("||")) {
            return;
        }
        String[] valueArray = value.split("\\|\\|", 2);
        String rawSourcePath = valueArray[0];
        String rawDestinationPath = valueArray[1];
        boolean wildcardSource = isWildcardPath(rawSourcePath);
        if (isWildcardPath(rawDestinationPath)) {
            throw new IllegalArgumentException("Destination path cannot end with '*': " + rawDestinationPath);
        }
        String processedSourcePath = wildcardSource ? stripTrailingWildcard(rawSourcePath) : rawSourcePath;
        GameDirectoryActionPathResolver.ResolvedPath source = resolver.resolve(processedSourcePath);
        GameDirectoryActionPathResolver.ResolvedPath destination = resolver.resolve(rawDestinationPath);
        Path sourcePath = source.path();
        Path destinationPath = destination.path();
        source.revalidate();
        destination.revalidate();
        if (!Files.exists(sourcePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileNotFoundException("Source not found! Can't copy: " + (wildcardSource ? rawSourcePath : sourcePath));
        }
        if (wildcardSource) {
            if (!Files.isDirectory(sourcePath)) {
                throw new FileNotFoundException("Source directory not found! Can't copy: " + rawSourcePath);
            }
            this.copyWildcardFiles(source, destination);
            return;
        }
        if (Files.exists(destinationPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException("Destination exists already! Can't copy to: " + destinationPath);
        }
        if (destinationPath.startsWith(sourcePath)) {
            throw new IllegalArgumentException("Destination path cannot be inside the source path: " + destinationPath);
        }
        source.revalidate();
        if (Files.isDirectory(sourcePath)) {
            this.copyDirectoryRecursively(source, destination);
        } else {
            source.revalidate();
            if (!Files.isRegularFile(sourcePath)) {
                throw new FileNotFoundException("Source not found! Can't copy: " + sourcePath);
            }
            Path destinationParent = destinationPath.getParent();
            if (destinationParent != null) {
                destination.revalidate();
                Files.createDirectories(destinationParent);
            }
            source.revalidate();
            destination.revalidate();
            this.copyFileContents(source, destination);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.copy_file");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.copy_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValuePreset() {
        return "/config/source_directory/some_file.txt||/config/destination_directory/some_file_copy.txt";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getDisplayName(),
                Component.translatable("fancymenu.actions.copy_file.value.source"),
                Component.translatable("fancymenu.actions.copy_file.value.destination"), null, callback -> {
                    if (handled[0]) {
                        return;
                    }
                    handled[0] = true;
                    if (callback != null) {
                        String newValue = callback.getFirst() + "||" + callback.getSecond();
                        instance.value = newValue;
                        onEditingCompleted.accept(instance, oldValue, newValue);
                    } else {
                        onEditingCanceled.accept(instance);
                    }
                });

        String val = instance.value;
        if ((val != null) && val.contains("||")) {
            String[] array = val.split("\\|\\|", 2);
            s.setFirstText(array[0]);
            s.setSecondText(array[1]);
        }

        var opened = Dialogs.openGeneric(s, this.getDisplayName(), null, DualTextInputWindowBody.PIP_WINDOW_WIDTH, DualTextInputWindowBody.PIP_WINDOW_HEIGHT);
        opened.getSecond().addCloseCallback(() -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });

    }

    private void copyDirectoryRecursively(@NotNull GameDirectoryActionPathResolver.ResolvedPath sourceDirectory, @NotNull GameDirectoryActionPathResolver.ResolvedPath destinationDirectory) throws IOException {
        // Finish validating the complete tree before creating anything, otherwise a late escaping link could leave a partial copy behind.
        List<CopyEntry> copyPlan = new ArrayList<>();
        sourceDirectory.revalidate();
        destinationDirectory.revalidate();
        Files.walkFileTree(sourceDirectory.path(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                copyPlan.add(createCopyEntry(sourceDirectory, destinationDirectory, dir, true));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                copyPlan.add(createCopyEntry(sourceDirectory, destinationDirectory, file, false));
                return FileVisitResult.CONTINUE;
            }
        });
        for (CopyEntry entry : copyPlan) {
            entry.source().revalidate();
            entry.destination().revalidate();
        }
        for (CopyEntry entry : copyPlan) {
            entry.source().revalidate();
            entry.destination().revalidate();
            if (entry.directory()) {
                Files.createDirectories(entry.destination().path());
                entry.destination().revalidate();
            } else {
                this.copyFileContents(entry.source(), entry.destination());
            }
        }
    }

    private void copyWildcardFiles(@NotNull GameDirectoryActionPathResolver.ResolvedPath sourceDirectory, @NotNull GameDirectoryActionPathResolver.ResolvedPath destinationDirectory) throws IOException {
        this.validateDestinationDirectory(destinationDirectory);
        // Destination creation and all copies must wait until every direct child and conflict has passed preflight.
        List<CopyEntry> copyPlan = new ArrayList<>();
        sourceDirectory.revalidate();
        destinationDirectory.revalidate();
        try (Stream<Path> children = Files.list(sourceDirectory.path())) {
            for (Path child : children.toList()) {
                String fileName = child.getFileName().toString();
                GameDirectoryActionPathResolver.ResolvedPath source = sourceDirectory.resolveSingleComponentChild(fileName);
                source.revalidate();
                if (!Files.isRegularFile(child)) {
                    continue;
                }
                GameDirectoryActionPathResolver.ResolvedPath destination = destinationDirectory.resolveSingleComponentChild(fileName);
                if (Files.exists(destination.path(), LinkOption.NOFOLLOW_LINKS)) {
                    throw new FileAlreadyExistsException("File exists at the destination path already! Can't copy to: " + destination.path());
                }
                destination.revalidate();
                copyPlan.add(new CopyEntry(source, destination, false));
            }
        }
        destinationDirectory.revalidate();
        Files.createDirectories(destinationDirectory.path());
        destinationDirectory.revalidate();
        for (CopyEntry entry : copyPlan) {
            entry.source().revalidate();
            entry.destination().revalidate();
            this.copyFileContents(entry.source(), entry.destination());
        }
    }

    private void validateDestinationDirectory(@NotNull GameDirectoryActionPathResolver.ResolvedPath destinationDirectory) throws IOException {
        Path destinationPath = destinationDirectory.path();
        if (Files.exists(destinationPath, LinkOption.NOFOLLOW_LINKS)) {
            destinationDirectory.revalidate();
            if (!Files.isDirectory(destinationPath)) {
                throw new IllegalArgumentException("Destination must be a directory when using '*': " + destinationPath);
            }
        }
    }

    private @NotNull CopyEntry createCopyEntry(@NotNull GameDirectoryActionPathResolver.ResolvedPath sourceDirectory, @NotNull GameDirectoryActionPathResolver.ResolvedPath destinationDirectory, @NotNull Path sourcePath, boolean directory) throws IOException {
        Path relativePath = sourceDirectory.path().relativize(sourcePath);
        if (relativePath.toString().isEmpty()) {
            sourceDirectory.revalidate();
            return new CopyEntry(sourceDirectory, destinationDirectory, directory || Files.isDirectory(sourceDirectory.path()));
        }
        String relative = relativePath.toString();
        GameDirectoryActionPathResolver.ResolvedPath source = sourceDirectory.resolveRelativeChild(relative);
        GameDirectoryActionPathResolver.ResolvedPath destination = destinationDirectory.resolveRelativeChild(relative);
        source.revalidate();
        return new CopyEntry(source, destination, directory || Files.isDirectory(source.path()));
    }

    private void copyFileContents(@NotNull GameDirectoryActionPathResolver.ResolvedPath source, @NotNull GameDirectoryActionPathResolver.ResolvedPath destination) throws IOException {
        source.revalidate();
        destination.revalidate();
        // Resolve a validated final source link once, then refuse to follow another final link during either stream open.
        Path realSourcePath = source.path().toRealPath();
        source.revalidate();
        try (InputStream input = Files.newInputStream(realSourcePath, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS); OutputStream output = Files.newOutputStream(destination.path(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            input.transferTo(output);
        }
    }

    private @NotNull String stripTrailingWildcard(@NotNull String path) {
        if (path.length() <= 1) {
            throw new IllegalArgumentException("Wildcard path requires a directory before '*': " + path);
        }
        String withoutWildcard = path.substring(0, path.length() - 1);
        if (withoutWildcard.isEmpty()) {
            throw new IllegalArgumentException("Wildcard path requires a directory before '*': " + path);
        }
        return withoutWildcard;
    }

    private boolean isWildcardPath(@Nullable String path) {
        return (path != null) && path.endsWith("*");
    }

    private record CopyEntry(GameDirectoryActionPathResolver.ResolvedPath source, GameDirectoryActionPathResolver.ResolvedPath destination, boolean directory) {
    }

}
