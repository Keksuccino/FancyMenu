package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.profile.Profile;
import de.keksuccino.fancymenu.customization.profile.ProfileArchive;
import de.keksuccino.fancymenu.customization.profile.ProfileHandler;
import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileTask;
import de.keksuccino.fancymenu.customization.profile.ProfileTasks;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.SaveFileWindowBody;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.List;

/**
 * The flows behind the buttons of the profile manager, including their dialogs and error messages.
 */
final class ProfileActions {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Remembers where the user exported or imported archives last, because people usually keep them in one place. */
    @Nullable
    private static File lastArchiveDirectory;

    @NotNull
    private final ManageProfilesWindowBody manager;

    ProfileActions(@NotNull ManageProfilesWindowBody manager) {
        this.manager = manager;
    }

    void load(@NotNull Profile profile) {
        ProfileHandler.switchTo(profile, this.manager.getWindow(), error -> {
            this.manager.clearCaches();
            this.manager.refreshProfiles(profile.getId());
            if (error != null) this.showError("fancymenu.profiles.error.switch", error);
        });
    }

    void edit(@NotNull Profile profile) {
        EditProfileWindowBody.openInWindow(profile, this.manager.getWindow(), changed -> {
            if (!changed) return;
            this.manager.invalidateIcon(profile);
            this.manager.refreshProfiles(profile.getId());
        });
    }

    void createEmpty() {
        String title = I18n.get("fancymenu.profiles.new.default_title");
        this.runTask(() -> ProfileTasks.createEmptyProfile(title, this::onProfileCreated));
    }

    void duplicate(@NotNull Profile profile) {
        String title = I18n.get("fancymenu.profiles.duplicate.default_title", this.manager.getDisplayTitle(profile));
        this.runTask(() -> ProfileTasks.copyProfile(profile, title, task -> this.onTaskDone(task, "fancymenu.profiles.error.duplicate")));
    }

    /**
     * New profiles get opened in the editor right away, because the first thing to do with them is giving them a name.
     */
    private void onProfileCreated(@NotNull ProfileTask task) {
        this.onTaskDone(task, "fancymenu.profiles.error.create");
        String id = task.getResultProfileId();
        if ((task.getState() != ProfileTask.State.SUCCEEDED) || (id == null) || !this.manager.isOpen()) return;
        Profile created = ProfileHandler.getProfile(id);
        if (created != null) this.edit(created);
    }

    void delete(@NotNull Profile profile) {
        if (profile.active()) return;
        Component message = Component.translatable("fancymenu.profiles.delete.confirm", this.manager.getDisplayTitle(profile));
        Dialogs.openMessageWithCallback(message, MessageDialogStyle.WARNING, accepted -> {
            if (accepted) this.runTask(() -> ProfileTasks.deleteProfile(profile, task -> this.onTaskDone(task, "fancymenu.profiles.error.delete")));
        });
    }

    void openFolder(@NotNull Profile profile) {
        FileUtils.openFile(profile.directory().toFile());
    }

    void export(@NotNull Profile profile) {
        File startDirectory = getArchiveStartDirectory();
        String fileName = toFileName(this.manager.getDisplayTitle(profile));
        SaveFileWindowBody chooser = new SaveFileWindowBody(null, startDirectory, fileName, ProfileArchive.FILE_EXTENSION, file -> {
            if (file == null) return;
            Path target = file.toPath();
            if (!ProfileArchive.hasArchiveExtension(target)) target = target.resolveSibling(target.getFileName() + "." + ProfileArchive.FILE_EXTENSION);
            Path finalTarget = target;
            lastArchiveDirectory = (finalTarget.getParent() != null) ? finalTarget.getParent().toFile() : lastArchiveDirectory;
            this.runTask(() -> ProfileTasks.exportProfile(profile, finalTarget, task -> {
                this.onTaskDone(task, "fancymenu.profiles.error.export");
                if (task.getState() == ProfileTask.State.SUCCEEDED) this.showExportSuccess(finalTarget);
            }));
        });
        chooser.setForceResourceFriendlyFileNames(false);
        chooser.setBlockResourceUnfriendlyFileNames(false);
        chooser.openInWindow(this.manager.getWindow());
    }

    private void showExportSuccess(@NotNull Path archive) {
        Component message = Component.translatable("fancymenu.profiles.export.success", archive.toAbsolutePath().toString());
        List<Component> choices = List.of(Component.translatable("fancymenu.profiles.export.success.open_folder"), Component.translatable("fancymenu.common_components.ok"));
        ProfileChoiceDialogBody.open(Component.translatable("fancymenu.profiles.export.success.title"), MaterialIcons.CHECK_CIRCLE, UIBase.getUITheme().success_color.getColorInt(), message, choices, this.manager.getWindow(), choice -> {
            Path parent = archive.toAbsolutePath().getParent();
            if ((choice == 0) && (parent != null)) FileUtils.openFile(parent.toFile());
        });
    }

    void chooseArchiveToImport() {
        ChooseFileWindowBody chooser = new ChooseFileWindowBody(null, getArchiveStartDirectory(), file -> {
            if (file == null) return;
            if (file.getParentFile() != null) lastArchiveDirectory = file.getParentFile();
            this.importArchive(file.toPath());
        });
        chooser.setFileFilter(file -> ProfileArchive.hasArchiveExtension(file.toPath()));
        chooser.setBlockResourceUnfriendlyFileNames(false);
        chooser.openInWindow(this.manager.getWindow());
    }

    /**
     * Checks an archive before importing it: warns about archives of newer FancyMenu versions and asks what to do if the profile already exists.
     */
    void importArchive(@NotNull Path archive) {
        if (ProfileHandler.isBusy()) return;
        ProfileMetadata metadata;
        try {
            metadata = ProfileArchive.readMetadata(archive, "");
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read profile archive: " + archive, ex);
            this.showError("fancymenu.profiles.error.import", ex);
            return;
        }
        String madeWith = metadata.getFancyMenuVersion();
        if ((madeWith != null) && (compareVersions(madeWith, FancyMenu.VERSION) > 0)) {
            Component warning = Component.translatable("fancymenu.profiles.import.newer_version", madeWith, FancyMenu.VERSION);
            Dialogs.openMessageWithCallback(warning, MessageDialogStyle.WARNING, accepted -> {
                if (accepted) this.importArchiveChecked(archive, metadata);
            });
            return;
        }
        this.importArchiveChecked(archive, metadata);
    }

    private void importArchiveChecked(@NotNull Path archive, @NotNull ProfileMetadata metadata) {
        Profile existing = null;
        for (Profile profile : ProfileHandler.getProfiles()) {
            if (profile.getId().equals(metadata.getId())) existing = profile;
        }
        if (existing == null) {
            this.startImport(archive, false);
            return;
        }
        String existingTitle = this.manager.getDisplayTitle(existing);
        String existingVersion = existing.metadata().getVersion().isEmpty() ? "-" : existing.metadata().getVersion();
        String importedVersion = metadata.getVersion().isEmpty() ? "-" : metadata.getVersion();
        Component message = Component.translatable(existing.active() ? "fancymenu.profiles.import.conflict.active" : "fancymenu.profiles.import.conflict", existingTitle, existingVersion, importedVersion);
        List<Component> choices = List.of(
                Component.translatable("fancymenu.profiles.import.conflict.replace"),
                Component.translatable("fancymenu.profiles.import.conflict.keep_both"),
                Component.translatable("fancymenu.common_components.cancel"));
        ProfileChoiceDialogBody.open(Component.translatable("fancymenu.profiles.import.conflict.title"), MaterialIcons.WARNING, UIBase.getUITheme().warning_color.getColorInt(), message, choices, this.manager.getWindow(), choice -> {
            if (choice == 0) this.startImport(archive, true);
            if (choice == 1) this.startImport(archive, false);
        });
    }

    private void startImport(@NotNull Path archive, boolean replaceExisting) {
        this.runTask(() -> ProfileTasks.importArchive(archive, replaceExisting, this.manager.getWindow(), task -> {
            this.manager.clearCaches();
            this.onTaskDone(task, "fancymenu.profiles.error.import");
        }));
    }

    void showNoArchiveDropped() {
        Dialogs.openMessage(Component.translatable("fancymenu.profiles.import.drop_no_archive"), MessageDialogStyle.INFO);
    }

    /**
     * Refreshes the manager after a task and selects the profile the task created or changed.
     */
    private void onTaskDone(@NotNull ProfileTask task, @NotNull String errorKey) {
        String resultId = task.getResultProfileId();
        if (resultId != null) this.manager.invalidateStats(resultId);
        if (this.manager.isOpen()) {
            this.manager.refreshProfiles((task.getState() == ProfileTask.State.SUCCEEDED) ? resultId : null);
        }
        Throwable error = task.getError();
        if ((task.getState() == ProfileTask.State.FAILED) && (error != null)) this.showError(errorKey, error);
    }

    private void runTask(@NotNull TaskStarter starter) {
        if (ProfileHandler.isBusy()) return;
        try {
            starter.start();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to start profile task!", ex);
            this.showError("fancymenu.profiles.error.generic", ex);
        }
    }

    /**
     * Shows an error with a hint that matches the cause, because raw exception messages don't help users much.
     */
    void showError(@NotNull String key, @NotNull Throwable error) {
        Component reason;
        if (error instanceof ProfileArchive.InvalidProfileArchiveException) {
            reason = Component.translatable("fancymenu.profiles.error.reason.invalid_archive");
        } else if (error instanceof ProfileArchive.NotEnoughSpaceException space) {
            reason = Component.translatable("fancymenu.profiles.error.reason.no_space", ProfileUiRendering.formatBytes(space.getRequiredBytes()), ProfileUiRendering.formatBytes(space.getUsableBytes()));
        } else if (isFileInUse(error)) {
            reason = Component.translatable("fancymenu.profiles.error.reason.file_in_use");
        } else {
            reason = Component.translatable("fancymenu.profiles.error.reason.generic", String.valueOf(error.getMessage()));
        }
        Dialogs.openMessage(Component.translatable(key).append("\n\n").append(reason), MessageDialogStyle.ERROR);
    }

    /**
     * Windows locks files that are open in other programs, which makes moving or deleting them fail.
     */
    private static boolean isFileInUse(@NotNull Throwable error) {
        if (error instanceof AccessDeniedException) return true;
        return (error instanceof FileSystemException) && (error.getMessage() != null) && error.getMessage().contains("another process");
    }

    @NotNull
    private static File getArchiveStartDirectory() {
        if ((lastArchiveDirectory != null) && lastArchiveDirectory.isDirectory()) return lastArchiveDirectory;
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.isDirectory()) return downloads;
        return GameDirectoryUtils.getGameDirectory();
    }

    /**
     * Turns a title into a file name that works on all operating systems, but keeps it readable.
     */
    @NotNull
    static String toFileName(@NotNull String title) {
        String cleaned = title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").replaceAll("\\s+", " ").strip();
        while (cleaned.endsWith(".")) cleaned = cleaned.substring(0, cleaned.length() - 1).strip();
        if (cleaned.length() > 64) cleaned = cleaned.substring(0, 64).strip();
        return cleaned.isEmpty() ? "profile" : cleaned;
    }

    /**
     * Compares versions like "3.9.15" number by number. Parts that are no numbers count as 0.
     */
    static int compareVersions(@NotNull String first, @NotNull String second) {
        String[] a = first.split("[^0-9]+");
        String[] b = second.split("[^0-9]+");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int partA = (i < a.length) ? parseOrZero(a[i]) : 0;
            int partB = (i < b.length) ? parseOrZero(b[i]) : 0;
            if (partA != partB) return Integer.compare(partA, partB);
        }
        return 0;
    }

    private static int parseOrZero(@NotNull String part) {
        try {
            return part.isEmpty() ? 0 : Integer.parseInt(part);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface TaskStarter {

        void start() throws Exception;

    }

}
