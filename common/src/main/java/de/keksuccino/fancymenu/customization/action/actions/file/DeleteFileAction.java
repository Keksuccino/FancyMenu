package de.keksuccino.fancymenu.customization.action.actions.file;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolver;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DeleteFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public DeleteFileAction() {
        super("delete_file_in_game_dir");
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
            LOGGER.error("[FANCYMENU] Failed to delete file in game directory via DeleteFileAction: " + value, ex);
        }
    }

    void executeWithResolver(@Nullable String value, @NotNull GameDirectoryActionPathResolver resolver) throws IOException {
        if (value == null) {
            return;
        }
        boolean wildcardTarget = isWildcardPath(value);
        String processedPath = wildcardTarget ? stripTrailingWildcard(value) : value;
        GameDirectoryActionPathResolver.ResolvedPath target = resolver.resolve(processedPath).requireDescendant();
        Path targetPath = target.path();
        target.revalidate();
        if (!Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileNotFoundException("Target not found! Can't delete: " + (wildcardTarget ? value : targetPath));
        }
        if (wildcardTarget) {
            target.revalidate();
            if (!Files.isDirectory(targetPath)) {
                throw new FileNotFoundException("Target directory not found! Can't delete: " + value);
            }
            this.deleteWildcardFiles(target);
            return;
        }
        target.revalidate();
        if (Files.isDirectory(targetPath)) {
            this.deleteDirectoryRecursively(target);
        } else if (Files.isRegularFile(targetPath)) {
            target.revalidate();
            Files.delete(targetPath);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.delete_file");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.delete_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.delete_file.value");
    }

    @Override
    public String getValuePreset() {
        return "/config/some_mod_folder/some_file.txt";
    }

    private void deleteDirectoryRecursively(@NotNull GameDirectoryActionPathResolver.ResolvedPath directory) throws IOException {
        // The no-follow walk only inventories entries; deletion starts after the whole tree passes real-path validation.
        List<GameDirectoryActionPathResolver.ResolvedPath> deletePlan = new ArrayList<>();
        directory.revalidate();
        Files.walkFileTree(directory.path(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                deletePlan.add(resolveEntry(directory, dir));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                deletePlan.add(resolveEntry(directory, file));
                return FileVisitResult.CONTINUE;
            }
        });
        for (GameDirectoryActionPathResolver.ResolvedPath entry : deletePlan) {
            entry.revalidate();
        }
        for (int i = deletePlan.size() - 1; i >= 0; i--) {
            GameDirectoryActionPathResolver.ResolvedPath entry = deletePlan.get(i);
            entry.revalidate();
            Files.delete(entry.path());
        }
    }

    private void deleteWildcardFiles(@NotNull GameDirectoryActionPathResolver.ResolvedPath directory) throws IOException {
        // Preflight the complete direct-child batch before deleting any entry.
        List<GameDirectoryActionPathResolver.ResolvedPath> deletePlan = new ArrayList<>();
        directory.revalidate();
        try (Stream<Path> children = Files.list(directory.path())) {
            for (Path child : children.toList()) {
                String fileName = child.getFileName().toString();
                GameDirectoryActionPathResolver.ResolvedPath entry = directory.resolveSingleComponentChild(fileName);
                entry.revalidate();
                if (Files.isRegularFile(child)) {
                    deletePlan.add(entry);
                }
            }
        }
        for (GameDirectoryActionPathResolver.ResolvedPath entry : deletePlan) {
            entry.revalidate();
        }
        for (GameDirectoryActionPathResolver.ResolvedPath entry : deletePlan) {
            entry.revalidate();
            Files.delete(entry.path());
        }
    }

    private @NotNull GameDirectoryActionPathResolver.ResolvedPath resolveEntry(@NotNull GameDirectoryActionPathResolver.ResolvedPath directory, @NotNull Path entryPath) throws IOException {
        Path relativePath = directory.path().relativize(entryPath);
        if (relativePath.toString().isEmpty()) {
            return directory;
        }
        return directory.resolveRelativeChild(relativePath.toString());
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

}
