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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MoveFileAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public MoveFileAction() {
        super("move_file_in_game_dir");
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
            LOGGER.error("[FANCYMENU] Failed to move file in game directory via MoveFileAction: " + value, ex);
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
        GameDirectoryActionPathResolver.ResolvedPath source = resolver.resolve(processedSourcePath).requireDescendant();
        GameDirectoryActionPathResolver.ResolvedPath destination = resolver.resolve(rawDestinationPath);
        Path sourcePath = source.path();
        Path destinationPath = destination.path();
        source.revalidate();
        destination.revalidate();
        if (!Files.exists(sourcePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileNotFoundException("Source not found! Can't move: " + (wildcardSource ? rawSourcePath : sourcePath));
        }
        if (wildcardSource) {
            if (!Files.isDirectory(sourcePath)) {
                throw new FileNotFoundException("Source directory not found! Can't move: " + rawSourcePath);
            }
            this.moveWildcardFiles(source, destination);
            return;
        }
        if (Files.exists(destinationPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException("Destination exists already! Can't move to: " + destinationPath);
        }
        if (destinationPath.startsWith(sourcePath)) {
            throw new IllegalArgumentException("Destination path cannot be inside the source path: " + destinationPath);
        }
        source.revalidate();
        if (!Files.isDirectory(sourcePath) && !Files.isRegularFile(sourcePath)) {
            throw new FileNotFoundException("Source not found! Can't move: " + sourcePath);
        }
        Path destinationParent = destinationPath.getParent();
        if (destinationParent != null) {
            destination.revalidate();
            Files.createDirectories(destinationParent);
        }
        source.revalidate();
        destination.revalidate();
        Files.move(sourcePath, destinationPath);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.move_file");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.move_file.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty(); // We handle the display names in the custom value edit screen
    }

    @Override
    public String getValuePreset() {
        return "/config/source_directory/some_file.txt||/config/destination_directory";
    }

    @Override
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        String oldValue = instance.value;
        boolean[] handled = {false};

        DualTextInputWindowBody s = DualTextInputWindowBody.build(
                this.getDisplayName(),
                Component.translatable("fancymenu.actions.move_file.value.source"),
                Component.translatable("fancymenu.actions.move_file.value.destination"), null, callback -> {
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

    private void moveWildcardFiles(@NotNull GameDirectoryActionPathResolver.ResolvedPath sourceDirectory, @NotNull GameDirectoryActionPathResolver.ResolvedPath destinationDirectory) throws IOException {
        this.validateDestinationDirectory(destinationDirectory);
        // Validate every source and destination before the first move so a later conflict or escaping link cannot produce a partial batch.
        List<MoveEntry> movePlan = new ArrayList<>();
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
                    throw new FileAlreadyExistsException("File exists at the destination path already! Can't move to: " + destination.path());
                }
                destination.revalidate();
                movePlan.add(new MoveEntry(source, destination));
            }
        }
        destinationDirectory.revalidate();
        Files.createDirectories(destinationDirectory.path());
        destinationDirectory.revalidate();
        for (MoveEntry entry : movePlan) {
            entry.source().revalidate();
            entry.destination().revalidate();
            Files.move(entry.source().path(), entry.destination().path());
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

    private record MoveEntry(GameDirectoryActionPathResolver.ResolvedPath source, GameDirectoryActionPathResolver.ResolvedPath destination) {
    }

}
