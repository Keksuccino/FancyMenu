package de.keksuccino.fancymenu.util.rendering.ui.screen.resource;

import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.AbstractBrowserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class ResourcePickerScreen extends AbstractBrowserScreen {

    @Nullable
    protected FileTypeGroup<?> allowedFileTypes;
    @NotNull
    protected Consumer<ResourceLocation> callback;
    @Nullable
    protected String currentNamespace;
    @NotNull
    protected String currentPath = "";
    @Nullable
    protected ResourceLocation preselectedLocation;
    @Nullable
    protected Set<ResourceLocation> cachedResourceLocations;
    protected boolean blockResourceUnfriendlyNames = true;
    protected boolean showBlockedResourceUnfriendlyNames = true;

    public ResourcePickerScreen(@Nullable ResourceLocation startLocation, @Nullable FileTypeGroup<?> allowedFileTypes, @NotNull Consumer<ResourceLocation> callback) {
        super(Component.translatable("fancymenu.ui.resourcepicker.choose.resource"));
        this.allowedFileTypes = allowedFileTypes;
        this.callback = Objects.requireNonNull(callback);
        this.applyStartLocation(startLocation);
        this.updatePreviewForKey(null);
        this.updateResourceList();
        this.updateFileTypeScrollArea();
    }

    protected void applyStartLocation(@Nullable ResourceLocation startLocation) {
        this.preselectedLocation = startLocation;
        if (startLocation != null) {
            this.currentNamespace = startLocation.getNamespace();
            String path = startLocation.getPath();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > -1) {
                this.currentPath = path.substring(0, lastSlash);
            } else {
                this.currentPath = "";
            }
        }
    }

    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.ok"), (button) -> {
            ResourceScrollAreaEntry selected = this.getSelectedEntry();
            if ((selected != null) && !selected.resourceUnfriendlyName) {
                this.callback.accept(selected.location);
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                ResourceScrollAreaEntry selected = ResourcePickerScreen.this.getSelectedEntry();
                this.active = (selected != null) && !selected.resourceUnfriendlyName;
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    @NotNull
    protected Component getEntriesLabel() {
        return Component.translatable("fancymenu.ui.resourcepicker.resources");
    }

    @Override
    protected void updateEntryList() {
        this.updateResourceList();
    }

    @Override
    protected boolean goUpDirectory() {
        if (!this.currentPath.isEmpty()) {
            int lastSlash = this.currentPath.lastIndexOf('/');
            if (lastSlash > -1) {
                String newPath = this.currentPath.substring(0, lastSlash);
                this.setDirectory(this.currentNamespace, newPath, true);
                return true;
            } else {
                this.setDirectory(this.currentNamespace, "", true);
                return true;
            }
        } else if (this.currentNamespace != null) {
            this.setDirectory(null, "", true);
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
        if (entry instanceof DirectoryScrollAreaEntry dirEntry) {
            if (dirEntry.resourceUnfriendlyName) return false;
            this.setDirectory(dirEntry.namespace, dirEntry.path, true);
            return true;
        }
        return false;
    }

    @Override
    protected Object getPreviewKeyForEntry(@NotNull ScrollAreaEntry entry) {
        if (entry instanceof ResourceScrollAreaEntry resourceEntry) {
            return resourceEntry.location;
        }
        return null;
    }

    @Override
    protected void loadPreviewForKey(@NotNull Object previewKey) {
        if (!(previewKey instanceof ResourceLocation location)) return;
        this.setTextPreview(location);
        if (this.isImageLocation(location)) {
            this.previewTextureSupplier = ResourceSupplier.image(location.toString());
        } else {
            this.previewTextureSupplier = null;
        }
    }

    @Nullable
    protected ResourceScrollAreaEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e instanceof ResourceScrollAreaEntry entry) {
                if (entry.isSelected()) return entry;
            }
        }
        return null;
    }

    @Nullable
    protected ResourceLocation getSelectedLocation() {
        ResourceScrollAreaEntry selected = this.getSelectedEntry();
        if ((selected != null) && !selected.resourceUnfriendlyName) {
            return selected.location;
        }
        return null;
    }

    public void setFileTypes(@Nullable FileTypeGroup<?> typeGroup) {
        this.allowedFileTypes = typeGroup;
        this.updateResourceList();
        this.updateFileTypeScrollArea();
    }

    @Nullable
    public FileTypeGroup<?> getFileTypes() {
        return this.allowedFileTypes;
    }

    @Override
    public void updateFileTypeScrollArea() {
        this.fileTypeScrollArea.clearEntries();
        this.currentFileTypesComponent = Component.translatable("fancymenu.file_browser.file_type.types.all").append(" (*)");
        if (this.allowedFileTypes != null) {
            String types = "";
            for (FileType<?> type : this.allowedFileTypes.getFileTypes()) {
                if (!type.isLocationAllowed()) continue;
                for (String s : type.getExtensions()) {
                    if (!types.isEmpty()) types += ";";
                    types += "*." + s.toUpperCase();
                }
            }
            Component fileTypeDisplayName = this.allowedFileTypes.getDisplayName();
            if (fileTypeDisplayName == null) fileTypeDisplayName = Component.empty();
            this.currentFileTypesComponent = Component.empty().append(fileTypeDisplayName).append(Component.literal(" (")).append(Component.literal(types)).append(Component.literal(")"));
        }
        this.currentFileTypesComponent = this.currentFileTypesComponent.withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()));
        TextScrollAreaEntry entry = new TextScrollAreaEntry(this.fileTypeScrollArea, this.currentFileTypesComponent, textScrollAreaEntry -> {});
        entry.setPlayClickSound(false);
        entry.setSelectable(false);
        entry.setBackgroundColorHover(entry.getBackgroundColorNormal());
        entry.setHeight(this.fileTypeScrollArea.getInnerHeight());
        this.fileTypeScrollArea.addEntry(entry);
    }

    public void updatePreview(@Nullable ResourceLocation location) {
        this.updatePreviewForKey(location);
    }

    protected void setTextPreview(@Nullable ResourceLocation location) {
        if (location == null) {
            this.previewTextSupplier = null;
        } else {
            for (TextFileType type : FileTypes.getAllTextFileTypes()) {
                if (type.isFileTypeLocation(location)) {
                    this.previewTextSupplier = ResourceSupplier.text(location.toString());
                    return;
                }
            }
            this.previewTextSupplier = null;
        }
    }

    protected void updateResourceList() {
        this.fileListScrollArea.clearEntries();
        if (!this.currentIsRootDirectory()) {
            ParentDirScrollAreaEntry e = new ParentDirScrollAreaEntry(this.fileListScrollArea);
            this.fileListScrollArea.addEntry(e);
        }

        String searchValue = this.getSearchValue();
        if (searchValue != null) {
            List<ResourceLocation> matches = new ArrayList<>();
            this.collectSearchMatches(searchValue.toLowerCase(), matches);
            FilenameComparator comparator = new FilenameComparator();
            matches.sort((o1, o2) -> comparator.compare(this.getSearchSortKey(o1), this.getSearchSortKey(o2)));
            for (ResourceLocation location : matches) {
                ResourceScrollAreaEntry entry = new ResourceScrollAreaEntry(this.fileListScrollArea, location);
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyLocation(location);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.fileListScrollArea.addEntry(entry);
                if (this.preselectedLocation != null && this.preselectedLocation.equals(location)) {
                    entry.setSelected(true);
                    this.updatePreview(location);
                }
            }
            return;
        }

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (this.currentNamespace == null) {
            List<String> namespaces = new ArrayList<>(resourceManager.getNamespaces());
            FilenameComparator comparator = new FilenameComparator();
            namespaces.sort(comparator);
            for (String namespace : namespaces) {
                DirectoryScrollAreaEntry entry = new DirectoryScrollAreaEntry(this.fileListScrollArea, namespace, namespace, "");
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyName(namespace);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.fileListScrollArea.addEntry(entry);
            }
        } else {
            String prefix = this.currentPath.isEmpty() ? "" : this.currentPath + "/";
            Set<String> directories = new HashSet<>();
            List<ResourceLocation> files = new ArrayList<>();
            Set<ResourceLocation> allLocations = this.getAllResourceLocations();

            for (ResourceLocation location : allLocations) {
                if (!Objects.equals(location.getNamespace(), this.currentNamespace)) continue;
                String path = location.getPath();
                if (!prefix.isEmpty()) {
                    if (!path.startsWith(prefix)) continue;
                    path = path.substring(prefix.length());
                }
                if (path.isEmpty()) continue;
                int slashIndex = path.indexOf('/');
                if (slashIndex >= 0) {
                    directories.add(path.substring(0, slashIndex));
                } else {
                    if (isAllowedLocation(location)) {
                        files.add(location);
                    }
                }
            }

            FilenameComparator comparator = new FilenameComparator();
            List<String> sortedDirs = new ArrayList<>(directories);
            sortedDirs.sort(comparator);
            for (String dir : sortedDirs) {
                String fullPath = prefix.isEmpty() ? dir : prefix + dir;
                DirectoryScrollAreaEntry entry = new DirectoryScrollAreaEntry(this.fileListScrollArea, dir, this.currentNamespace, fullPath);
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyName(dir);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.fileListScrollArea.addEntry(entry);
            }

            files.sort((o1, o2) -> comparator.compare(getLocationDisplayName(o1), getLocationDisplayName(o2)));
            for (ResourceLocation location : files) {
                ResourceScrollAreaEntry entry = new ResourceScrollAreaEntry(this.fileListScrollArea, location);
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyLocation(location);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.fileListScrollArea.addEntry(entry);
                if (this.preselectedLocation != null && this.preselectedLocation.equals(location)) {
                    entry.setSelected(true);
                    this.updatePreview(location);
                }
            }
        }
    }

    protected void collectSearchMatches(@NotNull String searchLower, @NotNull List<ResourceLocation> matches) {
        String prefix = this.currentPath.isEmpty() ? "" : this.currentPath + "/";
        for (ResourceLocation location : this.getAllResourceLocations()) {
            if (!isAllowedLocation(location)) continue;
            if (this.currentNamespace != null) {
                if (!Objects.equals(location.getNamespace(), this.currentNamespace)) continue;
                String path = location.getPath();
                if (!prefix.isEmpty()) {
                    if (!path.startsWith(prefix)) continue;
                    path = path.substring(prefix.length());
                }
                if (path.isEmpty()) continue;
            }
            if (this.locationMatchesSearch(location, searchLower)) {
                matches.add(location);
            }
        }
    }

    protected boolean locationMatchesSearch(@NotNull ResourceLocation location, @NotNull String searchLower) {
        String fileNameLower = getLocationDisplayName(location).toLowerCase();
        if (fileNameLower.contains(searchLower)) return true;
        String relativePath = this.getRelativePathForLocation(location);
        return (relativePath != null) && relativePath.toLowerCase().contains(searchLower);
    }

    protected String getSearchSortKey(@NotNull ResourceLocation location) {
        String relativePath = this.getRelativePathForLocation(location);
        if (relativePath != null) return relativePath;
        return this.getLocationDisplayName(location);
    }

    @Nullable
    protected String getRelativePathForLocation(@NotNull ResourceLocation location) {
        if (this.currentNamespace == null) {
            return location.getNamespace() + "/" + location.getPath();
        }
        if (!Objects.equals(location.getNamespace(), this.currentNamespace)) {
            return location.getNamespace() + "/" + location.getPath();
        }
        String prefix = this.currentPath.isEmpty() ? "" : this.currentPath + "/";
        String path = location.getPath();
        if (!prefix.isEmpty() && path.startsWith(prefix)) {
            String relative = path.substring(prefix.length());
            return relative.isEmpty() ? this.getLocationDisplayName(location) : relative;
        }
        return path;
    }

    @NotNull
    protected Set<ResourceLocation> getAllResourceLocations() {
        if (this.cachedResourceLocations == null) {
            this.cachedResourceLocations = new HashSet<>(Services.PLATFORM.getLoadedClientResourceLocations());
        }
        return this.cachedResourceLocations;
    }

    protected boolean currentIsRootDirectory() {
        return this.currentNamespace == null;
    }

    public void setDirectory(@Nullable String namespace, @NotNull String path, boolean playSound) {
        if (playSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.updatePreviewForKey(null);
        this.currentNamespace = namespace;
        this.currentPath = path;
        this.preselectedLocation = null;
        this.updateResourceList();
        this.updateCurrentDirectoryComponent();
    }

    public void updatePreviewSelection() {
        ResourceLocation location = this.getSelectedLocation();
        this.updatePreview(location);
    }

    protected boolean isAllowedLocation(@NotNull ResourceLocation location) {
        if (this.allowedFileTypes == null) return true;
        for (FileType<?> type : this.allowedFileTypes.getFileTypes()) {
            if (!type.isLocationAllowed()) continue;
            if (type.isFileTypeLocation(location)) return true;
        }
        return false;
    }

    protected boolean isImageLocation(@NotNull ResourceLocation location) {
        for (ImageFileType type : FileTypes.getAllImageFileTypes()) {
            if (type.isFileTypeLocation(location)) return true;
        }
        return false;
    }

    protected boolean isResourceFriendlyName(@NotNull String name) {
        return CharacterFilter.buildResourceNameFilter().isAllowedText(name);
    }

    protected boolean isResourceFriendlyLocation(@NotNull ResourceLocation location) {
        String combined = location.getNamespace() + "/" + location.getPath();
        combined = combined.replace("/", "");
        return CharacterFilter.buildResourceNameFilter().isAllowedText(combined);
    }

    protected String getLocationDisplayName(@NotNull ResourceLocation location) {
        String path = location.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) return path.substring(lastSlash + 1);
        return path;
    }

    protected String getDisplayNameForLocation(@NotNull ResourceLocation location) {
        String searchValue = this.getSearchValue();
        if (searchValue != null) {
            String relative = this.getRelativePathForLocation(location);
            if (relative != null) return relative;
        }
        return this.getLocationDisplayName(location);
    }

    @Override
    protected void updateCurrentDirectoryComponent() {
        try {

            if (this.currentDirectoryComponent != null) {
                this.removeWidget(this.currentDirectoryComponent);
            }
            this.currentDirectoryComponent = ComponentWidget.literal("/", 0, 0)
                    .setTextSupplier(consumes -> {
                        if (consumes.isHoveredOrFocused()) return Component.literal("/").withStyle(Style.EMPTY.withUnderlined(true));
                        return Component.literal("/");
                    })
                    .setOnClick(componentWidget -> this.setDirectory(null, "", true));

            if (this.currentNamespace != null) {
                ComponentWidget namespaceWidget = ComponentWidget.empty(0, 0)
                        .setTextSupplier(consumes -> {
                            if (consumes.isHoveredOrFocused()) return Component.literal(this.currentNamespace).withStyle(Style.EMPTY.withUnderlined(true));
                            return Component.literal(this.currentNamespace);
                        })
                        .setOnClick(componentWidget -> this.setDirectory(this.currentNamespace, "", true));
                this.currentDirectoryComponent.append(namespaceWidget);
                this.currentDirectoryComponent.append(ComponentWidget.literal("/", 0, 0));
                if (!this.currentPath.isEmpty()) {
                    String[] parts = this.currentPath.split("/");
                    StringBuilder current = new StringBuilder();
                    for (String part : parts) {
                        if (!current.isEmpty()) current.append("/");
                        current.append(part);
                        String targetPath = current.toString();
                        ComponentWidget w = ComponentWidget.empty(0, 0)
                                .setTextSupplier(consumes -> {
                                    if (consumes.isHoveredOrFocused()) return Component.literal(part).withStyle(Style.EMPTY.withUnderlined(true));
                                    return Component.literal(part);
                                })
                                .setOnClick(componentWidget -> this.setDirectory(this.currentNamespace, targetPath, true));
                        this.currentDirectoryComponent.append(w);
                        this.currentDirectoryComponent.append(ComponentWidget.literal("/", 0, 0));
                    }
                }
            }

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
            this.currentDirectoryComponent.setBaseColorSupplier(consumes -> UIBase.getUIColorTheme().description_area_text_color);
            this.addWidget(this.currentDirectoryComponent);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public abstract class AbstractResourceScrollAreaEntry extends AbstractIconTextScrollAreaEntry {

        protected boolean resourceUnfriendlyName = false;

        public AbstractResourceScrollAreaEntry(@NotNull ScrollArea parent, @NotNull String entryName) {
            super(parent, Component.literal(entryName));
        }

        @Override
        protected boolean useThemeTextureColor() {
            return true;
        }

        @Override
        protected boolean isResourceUnfriendly() {
            return this.resourceUnfriendlyName;
        }

        @Override
        public abstract void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button);

    }

    public class DirectoryScrollAreaEntry extends AbstractResourceScrollAreaEntry {

        protected final String namespace;
        protected final String path;

        public DirectoryScrollAreaEntry(@NotNull ScrollArea parent, @NotNull String entryName, @Nullable String namespace, @NotNull String path) {
            super(parent, entryName);
            this.namespace = namespace;
            this.path = path;
        }

        @Override
        protected @NotNull ResourceLocation getIconTexture() {
            return FOLDER_ICON_TEXTURE;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                ResourcePickerScreen.this.setDirectory(this.namespace, this.path, true);
            }
            ResourcePickerScreen.this.updatePreview(null);
            this.lastClick = now;
        }

    }

    public class ResourceScrollAreaEntry extends AbstractResourceScrollAreaEntry {

        protected final ResourceLocation location;

        public ResourceScrollAreaEntry(@NotNull ScrollArea parent, @NotNull ResourceLocation location) {
            super(parent, ResourcePickerScreen.this.getDisplayNameForLocation(location));
            this.location = location;
        }

        @Override
        protected @NotNull ResourceLocation getIconTexture() {
            return GENERIC_FILE_ICON_TEXTURE;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                ResourcePickerScreen.this.callback.accept(this.location);
            }
            ResourcePickerScreen.this.updatePreview(this.location);
            this.lastClick = now;
        }

    }

    public class ParentDirScrollAreaEntry extends AbstractIconTextScrollAreaEntry {

        public ParentDirScrollAreaEntry(@NotNull ScrollArea parent) {
            super(parent, Component.translatable("fancymenu.ui.filechooser.go_up").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()).withBold(true)));
        }

        @Override
        protected @NotNull ResourceLocation getIconTexture() {
            return GO_UP_ICON_TEXTURE;
        }

        @Override
        protected boolean useThemeTextureColor() {
            return true;
        }

        @Override
        protected int getTextColor() {
            return -1;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                ResourcePickerScreen.this.goUpDirectory();
            }
            ResourcePickerScreen.this.updatePreview(null);
            this.lastClick = now;
        }

    }

}
