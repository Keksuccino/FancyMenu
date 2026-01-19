package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.types.*;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SuppressWarnings("all")
public abstract class AbstractFileBrowserScreen extends AbstractBrowserScreen {

    protected static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected static File lastDirectory;

    @Nullable
    protected File rootDirectory;
    @NotNull
    protected File currentDir;
    @Nullable
    protected FileFilter fileFilter;
    @Nullable
    protected FileTypeGroup<?> fileTypes;
    @NotNull
    protected Consumer<File> callback;
    protected int visibleDirectoryLevelsAboveRoot = 0;
    protected boolean showSubDirectories = true;
    protected boolean blockResourceUnfriendlyFileNames = true;
    protected boolean showBlockedResourceUnfriendlyFiles = true;
    protected ExtendedButton createFolderButton;
    protected ExtendedButton openInExplorerButton;
    @Nullable
    protected WatchService directoryWatchService;
    @Nullable
    protected Thread directoryWatchThread;
    @Nullable
    protected Path watchedDirectoryPath;
    protected volatile boolean directoryWatchStopRequested = false;
    protected volatile boolean directoryReloadPending = false;

    public AbstractFileBrowserScreen(@NotNull Component title, @Nullable File rootDirectory, @NotNull File startDirectory, @NotNull Consumer<File> callback) {

        super(title);

        this.rootDirectory = rootDirectory;

        if (!this.isInRootOrSubOfRoot(startDirectory)) {
            startDirectory = this.rootDirectory;
        }
        if ((lastDirectory != null) && this.isInRootOrSubOfRoot(lastDirectory)) {
            startDirectory = lastDirectory;
        }

        this.currentDir = startDirectory;
        lastDirectory = startDirectory;
        this.callback = callback;

        this.updatePreviewForKey(null);
        this.updateFilesList();
        this.refreshDirectoryWatcherForCurrentDir();

    }

    @Override
    public void tick() {
        super.tick();
        this.tickDirectoryWatcher();
    }

    @Override
    protected void initExtraButtons() {
        this.createFolderButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.ui.filechooser.create_folder"), (button) -> {
            this.openCreateFolderDialog();
            MainThreadTaskExecutor.executeInMainThread(() -> button.setFocused(false), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        });
        this.addWidget(this.createFolderButton);
        UIBase.applyDefaultWidgetSkinTo(this.createFolderButton);

        this.openInExplorerButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.ui.filechooser.open_in_explorer"), (button) -> {
            File selected = this.getSelectedFile();
            if ((selected != null) && selected.isDirectory()) {
                FileUtils.openFile(selected);
            } else if (this.currentDir != null) {
                FileUtils.openFile(this.currentDir);
            }
            MainThreadTaskExecutor.executeInMainThread(() -> button.setFocused(false), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        });
        this.addWidget(this.openInExplorerButton);
        UIBase.applyDefaultWidgetSkinTo(this.openInExplorerButton);
    }

    @Override
    protected void renderExtraButtons(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderCreateFolderButton(graphics, mouseX, mouseY, partial);
        this.renderOpenInExplorerButton(graphics, mouseX, mouseY, partial);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    @NotNull
    protected Component getEntriesLabel() {
        return Component.translatable("fancymenu.ui.filechooser.files");
    }

    @Override
    protected void updateEntryList() {
        this.updateFilesList();
    }

    @Override
    protected boolean goUpDirectory() {
        if (this.currentIsRootDirectory()) return false;
        File parent = this.getParentDirectoryOfCurrent();
        if ((parent != null) && parent.isDirectory()) {
            this.setDirectory(parent, true);
            return true;
        }
        return false;
    }

    @Override
    protected boolean isGoUpEntry(@NotNull ScrollAreaEntry entry) {
        return entry instanceof ParentDirScrollAreaEntry;
    }

    @Override
    protected boolean openDirectoryEntry(@NotNull ScrollAreaEntry entry) {
        if (entry instanceof AbstractFileScrollAreaEntry fileEntry) {
            if (!fileEntry.resourceUnfriendlyFileName && fileEntry.file.isDirectory()) {
                this.setDirectory(fileEntry.file, true);
                return true;
            }
        }
        return false;
    }

    @Override
    protected Object getPreviewKeyForEntry(@NotNull ScrollAreaEntry entry) {
        if (entry instanceof AbstractFileScrollAreaEntry fileEntry) {
            if (fileEntry.file.isFile()) {
                return fileEntry.file;
            }
        }
        return null;
    }

    @Override
    protected void loadPreviewForKey(@NotNull Object previewKey) {
        if (!(previewKey instanceof File file)) return;
        this.setTextPreview(file);
        FileType<?> type = FileTypes.getLocalType(file);
        if (type instanceof ImageFileType) {
            this.previewTextureSupplier = ResourceSupplier.image(file.getPath());
            this.setPreviewAudio(null, null);
        } else if (type instanceof AudioFileType) {
            this.previewTextureSupplier = null;
            this.setPreviewAudio(ResourceSupplier.audio(GameDirectoryUtils.getAbsoluteGameDirectoryPath(file.getPath())), file);
        } else {
            this.previewTextureSupplier = null;
            this.setPreviewAudio(null, null);
        }
    }

    protected void renderOpenInExplorerButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.openInExplorerButton.setX(this.width - 20 - this.openInExplorerButton.getWidth());
        this.openInExplorerButton.setY(this.cancelButton.getY() - 15 - 20);
        this.openInExplorerButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderCreateFolderButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.createFolderButton.setX(this.width - 20 - this.createFolderButton.getWidth());
        this.createFolderButton.setY(this.cancelButton.getY() - 15 - 20 - 5 - 20);
        this.createFolderButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void openCreateFolderDialog() {
        TextInputWindowBody inputScreen = new TextInputWindowBody(CharacterFilter.buildResourceNameFilter(), (call) -> {
            if (call == null) return;
            this.createFolder(call);
        });
        Dialogs.openGeneric(inputScreen, Component.translatable("fancymenu.ui.filechooser.create_folder"), null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT);
        inputScreen.setText(Component.translatable("fancymenu.ui.filechooser.create_folder.default_name").getString());
    }

    protected void createFolder(@NotNull String folderName) {
        String trimmed = folderName.trim();
        if (trimmed.isEmpty()) return;
        File folder = new File(this.currentDir, trimmed);
        FileUtils.createDirectory(folder);
        if (!folder.isDirectory()) return;
        this.clearSearchBar();
        this.updateFilesList();
        this.selectEntryForFile(folder);
    }

    protected void clearSearchBar() {
        if (!this.searchBarEnabled || this.searchBar == null) return;
        if (this.searchBar.getValue().isEmpty()) return;
        this.searchBar.setValue("");
    }

    @Nullable
    protected AbstractFileScrollAreaEntry findEntryForFile(@NotNull File file) {
        String targetPath = file.getAbsoluteFile().getPath();
        for (ScrollAreaEntry entry : this.fileListScrollArea.getEntries()) {
            if (entry instanceof AbstractFileScrollAreaEntry fileEntry) {
                if (fileEntry.file.getAbsoluteFile().getPath().equals(targetPath)) {
                    return fileEntry;
                }
            }
        }
        return null;
    }

    protected void selectEntryForFile(@NotNull File file) {
        AbstractFileScrollAreaEntry entry = this.findEntryForFile(file);
        if (entry != null) {
            if (entry.resourceUnfriendlyFileName && !entry.isSelectable()) {
                entry.setSelectable(true);
            }
            this.selectEntry(entry);
        }
    }

    protected void updateCurrentDirectoryComponent() {
        try {

            if (this.currentDirectoryComponent != null) {
                this.removeWidget(this.currentDirectoryComponent);
            }
            this.currentDirectoryComponent = ComponentWidget.literal("/", 0, 0);
            if (this.visibleDirectoryLevelsAboveRoot > 0) {
                List<File> aboveRootFiles = new ArrayList<>();
                File f = this.rootDirectory.getAbsoluteFile().getParentFile();
                if (f != null) {
                    int i = this.visibleDirectoryLevelsAboveRoot;
                    while (true) {
                        i--;
                        f = f.getAbsoluteFile();
                        aboveRootFiles.add(f);
                        f = f.getParentFile();
                        if ((f == null) || (i <= 0)) {
                            break;
                        }
                    }
                    Collections.reverse(aboveRootFiles);
                    for (File f2 : aboveRootFiles) {
                        File fFinal = f2;
                        this.currentDirectoryComponent.append(ComponentWidget.empty(0, 0)
                                .setTextSupplier(consumes -> {
                                    if (consumes.isHoveredOrFocused()) return Component.literal(fFinal.getName()).setStyle(Style.EMPTY.withStrikethrough(true).withColor(UIBase.getUITheme().error_text_color.getColorInt()));
                                    return Component.literal(fFinal.getName());
                                })
                        );
                        this.currentDirectoryComponent.append(ComponentWidget.literal("/", 0, 0));
                    }
                }
            }
            for (File f : this.splitCurrentIntoSeparateDirectories()) {
                ComponentWidget w = ComponentWidget.empty(0, 0)
                        .setTextSupplier(consumes -> {
                            if (consumes.isHoveredOrFocused()) return Component.literal(f.getName()).withStyle(Style.EMPTY.withUnderlined(true));
                            return Component.literal(f.getName());
                        })
                        .setOnClick(componentWidget -> {
                            this.setDirectory(f, true);
                        });
                this.currentDirectoryComponent.append(w);
                this.currentDirectoryComponent.append(ComponentWidget.literal("/", 0, 0));
            }

            //Trim path to fit into the path area
            while (this.currentDirectoryComponent.getWidth() > (this.width - 260 - 20 - 8)) {
                if (!this.currentDirectoryComponent.getChildren().isEmpty()) {
                    this.currentDirectoryComponent.getChildren().remove(0);
                } else {
                    break;
                }
            }
            if (!this.currentDirectoryComponent.getChildren().isEmpty()) {
                ComponentWidget firstChild = this.currentDirectoryComponent.getChildren().get(0);
                if (firstChild.getText().getString().equals("/")) this.currentDirectoryComponent.getChildren().remove(0);
            }

            this.currentDirectoryComponent.setShadow(false);
            this.currentDirectoryComponent.setBaseColorSupplier(consumes -> UIBase.getUITheme().ui_interface_widget_label_color_normal);
            this.addWidget(this.currentDirectoryComponent);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @NotNull
    protected List<File> splitCurrentIntoSeparateDirectories() {
        List<File> dirs = new ArrayList<>();
        dirs.add(this.currentDir);
        try {
            if (!this.currentIsRootDirectory()) {
                File f = this.currentDir;
                while (true) {
                    f = f.getParentFile();
                    if (f != null) {
                        dirs.add(f);
                        if (this.isRootDirectory(f)) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Collections.reverse(dirs);
        return dirs;
    }

    @Nullable
    public FileFilter getFileFilter() {
        return fileFilter;
    }

    public AbstractFileBrowserScreen setFileFilter(@Nullable FileFilter fileFilter) {
        this.fileFilter = fileFilter;
        this.updateFilesList();
        return this;
    }

    public boolean shouldShowFile(@NotNull File file) {
        if ((this.fileFilter != null) && !this.fileFilter.checkFile(file)) return false;
        if (this.fileTypes != null) {
            for (FileType<?> type : this.fileTypes.getFileTypes()) {
                if (type.isFileTypeLocal(file)) return true;
            }
            return false;
        }
        return true;
    }

    public AbstractFileBrowserScreen setDirectory(@NotNull File newDirectory, boolean playSound) {
        Objects.requireNonNull(newDirectory);
        if (!this.isInRootOrSubOfRoot(newDirectory)) return this;
        if (playSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.updatePreviewForKey(null);
        this.currentDir = newDirectory;
        lastDirectory = newDirectory;
        this.updateFilesList();
        MainThreadTaskExecutor.executeInMainThread(this::updateCurrentDirectoryComponent, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        this.refreshDirectoryWatcherForCurrentDir();
        return this;
    }

    public int getVisibleDirectoryLevelsAboveRoot() {
        return this.visibleDirectoryLevelsAboveRoot;
    }

    public AbstractFileBrowserScreen setVisibleDirectoryLevelsAboveRoot(int visibleDirectoryLevelsAboveRoot) {
        this.visibleDirectoryLevelsAboveRoot = visibleDirectoryLevelsAboveRoot;
        return this;
    }

    public boolean showSubDirectories() {
        return this.showSubDirectories;
    }

    public AbstractFileBrowserScreen setShowSubDirectories(boolean showSubDirectories) {
        this.showSubDirectories = showSubDirectories;
        this.updateFilesList();
        return this;
    }

    public boolean blockResourceUnfriendlyFileNames() {
        return this.blockResourceUnfriendlyFileNames;
    }

    public AbstractFileBrowserScreen setBlockResourceUnfriendlyFileNames(boolean blockResourceUnfriendlyFileNames) {
        this.blockResourceUnfriendlyFileNames = blockResourceUnfriendlyFileNames;
        this.updateFilesList();
        return this;
    }

    public boolean showBlockedResourceUnfriendlyFileNames() {
        return this.showBlockedResourceUnfriendlyFiles;
    }

    public AbstractFileBrowserScreen setShowBlockedResourceUnfriendlyFiles(boolean showBlockedResourceUnfriendlyFiles) {
        this.showBlockedResourceUnfriendlyFiles = showBlockedResourceUnfriendlyFiles;
        return this;
    }

    @Nullable
    protected AbstractFileScrollAreaEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e instanceof AbstractFileScrollAreaEntry f) {
                if (f.isSelected()) return f;
            }
        }
        return null;
    }

    @Nullable
    protected File getSelectedFile() {
        AbstractFileScrollAreaEntry selected = this.getSelectedEntry();
        if ((selected != null) && !selected.resourceUnfriendlyFileName) {
            return new File(selected.file.getPath().replace("\\", "/"));
        }
        return null;
    }

    public void setFileTypes(@Nullable FileTypeGroup<?> typeGroup) {
        this.fileTypes = typeGroup;
        this.updateFilesList();
        this.updateFileTypeScrollArea();
    }

    @Nullable
    public FileTypeGroup<?> getFileTypes() {
        return this.fileTypes;
    }

    public void updateFileTypeScrollArea() {
        this.fileTypeScrollArea.clearEntries();
        this.currentFileTypesComponent = Component.translatable("fancymenu.file_browser.file_type.types.all").append(" (*)");
        if (this.fileTypes != null) {
            String types = "";
            for (FileType<?> type : this.fileTypes.getFileTypes()) {
                for (String s : type.getExtensions()) {
                    if (!types.isEmpty()) types += ";";
                    types += "*." + s.toUpperCase();
                }
            }
            Component fileTypeDisplayName = this.fileTypes.getDisplayName();
            if (fileTypeDisplayName == null) fileTypeDisplayName = Component.empty();
            this.currentFileTypesComponent = Component.empty().append(fileTypeDisplayName).append(Component.literal(" (")).append(Component.literal(types)).append(Component.literal(")"));
        }
        this.currentFileTypesComponent = this.currentFileTypesComponent.withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt()));
        TextScrollAreaEntry entry = new TextScrollAreaEntry(this.fileTypeScrollArea, this.currentFileTypesComponent, textScrollAreaEntry -> {});
        entry.setPlayClickSound(false);
        entry.setSelectable(false);
        entry.setBackgroundColorHover(entry.getBackgroundColorNormal());
        entry.setHeight(this.fileTypeScrollArea.getInnerHeight());
        this.fileTypeScrollArea.addEntry(entry);
    }

    public void updatePreview(@Nullable File file) {
        this.updatePreviewForKey(file);
    }

    @Override
    public void removed() {
        this.stopDirectoryWatcher();
        super.removed();
    }

    @Override
    public void onFilesDrop(@NotNull List<Path> paths) {
        if (paths.isEmpty()) return;
        File dropTargetDir = this.currentDir;
        if ((dropTargetDir == null) || !dropTargetDir.isDirectory()) return;

        List<Path> safePaths = new ArrayList<>(paths);
        TaskExecutor.execute(() -> {
            List<File> copied = this.copyDroppedFilesIntoDirectory(dropTargetDir, safePaths);
            if (copied.isEmpty()) return;
            MainThreadTaskExecutor.executeInMainThread(() -> {
                this.clearSearchBar();
                this.updateFilesList();
                this.selectEntryForFile(copied.get(0));
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }, false);
    }

    protected void setTextPreview(@Nullable File file) {
        if (file == null) {
            this.previewTextSupplier = null;
        } else {
            for (TextFileType type : FileTypes.getAllTextFileTypes()) {
                if (type.isFileTypeLocal(file)) {
                    this.previewTextSupplier = ResourceSupplier.text(GameDirectoryUtils.getAbsoluteGameDirectoryPath(file.getPath()));
                    return;
                }
            }
            this.previewTextSupplier = null;
        }
    }

    protected void updateFilesList() {
        this.fileListScrollArea.clearEntries();
        if (!this.currentIsRootDirectory()) {
            ParentDirScrollAreaEntry e = new ParentDirScrollAreaEntry(this.fileListScrollArea);
            this.fileListScrollArea.addEntry(e);
        }
        String searchValue = this.getSearchValue();
        if (searchValue != null) {
            List<File> matches = new ArrayList<>();
            this.collectSearchMatches(this.currentDir, searchValue.toLowerCase(), matches);
            FilenameComparator comp = new FilenameComparator();
            Collections.sort(matches, (o1, o2) -> comp.compare(this.getSearchSortKey(o1), this.getSearchSortKey(o2)));
            for (File f : matches) {
                AbstractFileScrollAreaEntry e = this.buildFileEntry(f);
                if (this.blockResourceUnfriendlyFileNames) e.resourceUnfriendlyFileName = !FileFilter.RESOURCE_NAME_FILTER.checkFile(f);
                if (e.resourceUnfriendlyFileName) e.setSelectable(false);
                if (e.resourceUnfriendlyFileName && !this.showBlockedResourceUnfriendlyFiles) continue;
                this.fileListScrollArea.addEntry(e);
            }
        } else {
            File[] filesList = this.currentDir.listFiles();
            if (filesList != null) {
                List<File> files = new ArrayList<>();
                List<File> folders = new ArrayList<>();
                for (File f : filesList) {
                    if (f.isFile()) {
                        files.add(f);
                    } else if (f.isDirectory()) {
                        folders.add(f);
                    }
                }
                FilenameComparator comp = new FilenameComparator();
                Collections.sort(folders, (o1, o2) -> {
                    return comp.compare(o1.getName(), o2.getName());
                });
                Collections.sort(files, (o1, o2) -> {
                    return comp.compare(o1.getName(), o2.getName());
                });
                if (this.showSubDirectories) {
                    for (File f : folders) {
                        AbstractFileScrollAreaEntry e = this.buildFileEntry(f);
                        if (this.blockResourceUnfriendlyFileNames) e.resourceUnfriendlyFileName = !FileFilter.RESOURCE_NAME_FILTER.checkFile(f);
                        if (e.resourceUnfriendlyFileName) e.setSelectable(false);
                        this.fileListScrollArea.addEntry(e);
                    }
                }
                for (File f : files) {
                    if (!this.shouldShowFile(f)) continue;
                    AbstractFileScrollAreaEntry e = this.buildFileEntry(f);
                    if (this.blockResourceUnfriendlyFileNames) e.resourceUnfriendlyFileName = !FileFilter.RESOURCE_NAME_FILTER.checkFile(f);
                    if (e.resourceUnfriendlyFileName) e.setSelectable(false);
                    if (e.resourceUnfriendlyFileName && !this.showBlockedResourceUnfriendlyFiles) continue;
                    this.fileListScrollArea.addEntry(e);
                }
            }
        }
    }

    protected abstract AbstractFileScrollAreaEntry buildFileEntry(@NotNull File f);

    protected void tickDirectoryWatcher() {
        if (!this.directoryReloadPending) return;
        this.directoryReloadPending = false;
        this.updateFilesList();
    }

    protected void refreshDirectoryWatcherForCurrentDir() {
        Path newPath;
        try {
            newPath = this.currentDir.getAbsoluteFile().toPath();
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to resolve current directory for watching!", ex);
            this.stopDirectoryWatcher();
            return;
        }

        if ((this.watchedDirectoryPath != null) && this.watchedDirectoryPath.equals(newPath) && (this.directoryWatchService != null)) {
            return;
        }

        this.stopDirectoryWatcher();
        if (!this.currentDir.isDirectory()) return;

        try {
            WatchService service = FileSystems.getDefault().newWatchService();
            newPath.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            this.directoryWatchService = service;
            this.watchedDirectoryPath = newPath;
            this.directoryWatchStopRequested = false;
            Thread watcherThread = new Thread(() -> this.watchDirectoryLoop(service, newPath), "FancyMenu-FileBrowserWatcher");
            watcherThread.setDaemon(true);
            this.directoryWatchThread = watcherThread;
            watcherThread.start();
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to start directory watcher for '{}'.", this.currentDir.getAbsolutePath(), ex);
            this.stopDirectoryWatcher();
        }
    }

    protected void watchDirectoryLoop(@NotNull WatchService service, @NotNull Path path) {
        try {
            while (!this.directoryWatchStopRequested) {
                WatchKey key;
                try {
                    key = service.take();
                } catch (InterruptedException ignored) {
                    continue;
                } catch (ClosedWatchServiceException ignored) {
                    break;
                }

                boolean triggerRefresh = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;
                    triggerRefresh = true;
                    break;
                }

                if (!key.reset()) {
                    break;
                }

                if (triggerRefresh) {
                    this.directoryReloadPending = true;
                }
            }
        } catch (ClosedWatchServiceException ignored) {
            // Closed while stopping watcher
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Directory watcher crashed for '{}'.", path.toAbsolutePath(), ex);
        }
    }

    protected void stopDirectoryWatcher() {
        this.directoryWatchStopRequested = true;

        if (this.directoryWatchService != null) {
            try {
                this.directoryWatchService.close();
            } catch (IOException ignored) {
            }
        }

        if ((this.directoryWatchThread != null) && this.directoryWatchThread.isAlive()) {
            this.directoryWatchThread.interrupt();
        }

        this.directoryWatchService = null;
        this.directoryWatchThread = null;
        this.watchedDirectoryPath = null;
    }

    protected String getSearchSortKey(@NotNull File file) {
        String relativePath = this.getRelativePathForFile(file);
        if (relativePath != null) return relativePath;
        return file.getName();
    }

    protected void collectSearchMatches(@NotNull File directory, @NotNull String searchLower, @NotNull List<File> matches) {
        try (Stream<Path> stream = Files.walk(directory.toPath())) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                File f = path.toFile();
                if (!this.shouldShowFile(f)) return;
                if (this.fileMatchesSearch(f, searchLower)) {
                    matches.add(f);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected boolean fileMatchesSearch(@NotNull File file, @NotNull String searchLower) {
        String fileNameLower = file.getName().toLowerCase();
        if (fileNameLower.contains(searchLower)) return true;
        String relativePath = this.getRelativePathForFile(file);
        return (relativePath != null) && relativePath.toLowerCase().contains(searchLower);
    }

    @Nullable
    protected String getRelativePathForFile(@NotNull File file) {
        try {
            Path base = this.currentDir.toPath();
            Path target = file.toPath();
            if (target.startsWith(base)) {
                String relative = base.relativize(target).toString().replace("\\", "/");
                return relative.isEmpty() ? file.getName() : relative;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return file.getName();
    }

    protected boolean currentIsRootDirectory() {
        return this.isRootDirectory(this.currentDir);
    }

    protected boolean isRootDirectory(File dir) {
        if (this.rootDirectory == null) return false;
        return this.rootDirectory.getAbsolutePath().equals(dir.getAbsolutePath());
    }

    @Nullable
    protected File getParentDirectoryOfCurrent() {
        return this.currentDir.getParentFile();
    }

    protected boolean isInRootOrSubOfRoot(File file) {
        if (this.rootDirectory == null) return true;
        return file.getAbsolutePath().startsWith(this.rootDirectory.getAbsolutePath());
    }

    protected @NotNull List<File> copyDroppedFilesIntoDirectory(@NotNull File targetDir, @NotNull List<Path> droppedPaths) {
        List<File> copied = new ArrayList<>();
        for (Path p : droppedPaths) {
            try {
                if (!Files.isRegularFile(p)) continue;
                File source = p.toFile();
                File target = new File(targetDir, source.getName());
                if (target.isFile()) target = FileUtils.generateUniqueFileName(target, false);
                try {
                    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                    copied.add(target);
                } catch (Exception copyWithAttributesFailed) {
                    try {
                        Files.copy(source.toPath(), target.toPath());
                        copied.add(target);
                    } catch (Exception fallbackEx) {
                        LOGGER.warn("[FANCYMENU] Failed to copy dropped file '{}' into '{}'.", p, targetDir, fallbackEx);
                    }
                }
            } catch (Exception ex) {
                LOGGER.warn("[FANCYMENU] Failed to prepare dropped file '{}' for copy into '{}'.", p, targetDir, ex);
            }
        }
        return copied;
    }

    public abstract class AbstractFileScrollAreaEntry extends AbstractIconTextScrollAreaEntry {

        public File file;
        protected boolean resourceUnfriendlyFileName = false;
        protected final FileType<?> fileType;

        public AbstractFileScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {
            super(parent, Component.literal(AbstractFileBrowserScreen.this.getDisplayNameForFile(file)));
            this.file = file;
            this.fileType = this.file.isFile() ? FileTypes.getLocalType(this.file) : null;
        }

        @Override
        protected @NotNull ResourceLocation getIconTexture() {
            if (this.file.isFile() && (this.fileType != null)) {
                if (this.fileType instanceof TextFileType) return TEXT_FILE_ICON_TEXTURE;
                if (this.fileType instanceof VideoFileType) return VIDEO_FILE_ICON_TEXTURE;
                if (this.fileType instanceof AudioFileType) return AUDIO_FILE_ICON_TEXTURE;
                if (this.fileType instanceof ImageFileType) return IMAGE_FILE_ICON_TEXTURE;
            }
            return this.file.isFile() ? GENERIC_FILE_ICON_TEXTURE : FOLDER_ICON_TEXTURE;
        }

        @Override
        protected boolean isResourceUnfriendly() {
            return this.resourceUnfriendlyFileName;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (!this.file.exists()) return;
            super.renderEntry(graphics, mouseX, mouseY, partial);
        }

        @Override
        public abstract void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button);

    }

    protected String getDisplayNameForFile(@NotNull File file) {
        String searchValue = this.getSearchValue();
        if (searchValue != null) {
            String relative = this.getRelativePathForFile(file);
            if (relative != null) return relative;
        }
        return file.getName();
    }

    public class ParentDirScrollAreaEntry extends AbstractIconTextScrollAreaEntry {

        public ParentDirScrollAreaEntry(@NotNull ScrollArea parent) {
            super(parent, Component.translatable("fancymenu.ui.filechooser.go_up").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt()).withBold(true)));
        }

        @Override
        protected @NotNull ResourceLocation getIconTexture() {
            return GO_UP_ICON_TEXTURE;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                AbstractFileBrowserScreen.this.goUpDirectory();
            }
            AbstractFileBrowserScreen.this.updatePreviewForKey(null);
            this.lastClick = now;
        }

    }

}
