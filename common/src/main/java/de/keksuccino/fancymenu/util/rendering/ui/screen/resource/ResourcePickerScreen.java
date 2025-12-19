package de.keksuccino.fancymenu.util.rendering.ui.screen.resource;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("unused")
public class ResourcePickerScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation GO_UP_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/go_up_icon.png");
    private static final ResourceLocation FILE_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_icon.png");
    private static final ResourceLocation FOLDER_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/folder_icon.png");
    private static final Component FILE_TYPE_PREFIX_TEXT = Component.translatable("fancymenu.file_browser.file_type");

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

    protected ScrollArea resourceListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea fileTypeScrollArea = new ScrollArea(0, 0, 0, 20);
    protected ScrollArea previewTextScrollArea = new ScrollArea(0, 0, 0, 0);
    @Nullable
    protected ResourceSupplier<ITexture> previewTextureSupplier;
    @Nullable
    protected ResourceSupplier<IText> previewTextSupplier;
    @Nullable
    protected IText currentPreviewText;
    protected ExtendedButton confirmButton;
    protected ExtendedButton cancelButton;
    protected ComponentWidget currentDirectoryComponent;
    protected int resourceScrollListHeightOffset = 0;
    protected int fileTypeScrollListYOffset = 0;
    @Nullable
    protected MutableComponent currentFileTypesComponent;
    protected boolean blockResourceUnfriendlyNames = true;
    protected boolean showBlockedResourceUnfriendlyNames = true;

    public ResourcePickerScreen(@Nullable ResourceLocation startLocation, @Nullable FileTypeGroup<?> allowedFileTypes, @NotNull Consumer<ResourceLocation> callback) {
        super(Component.translatable("fancymenu.ui.resourcepicker.choose.resource"));
        this.allowedFileTypes = allowedFileTypes;
        this.callback = Objects.requireNonNull(callback);
        this.applyStartLocation(startLocation);
        this.setTextPreview(null);
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
    protected void init() {

        this.confirmButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.ok"), (button) -> {
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
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultWidgetSkinTo(this.confirmButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.updateCurrentDirectoryComponent();

        this.updateFileTypeScrollArea();

    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.currentFileTypesComponent != null) {
            this.fileTypeScrollArea.horizontalScrollBar.active = (Minecraft.getInstance().font.width(this.currentFileTypesComponent) > (this.fileTypeScrollArea.getInnerWidth() - 10));
        }

        RenderSystem.enableBlend();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, Component.translatable("fancymenu.ui.resourcepicker.resources"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        int currentDirFieldYEnd = this.renderCurrentDirectoryField(graphics, mouseX, mouseY, partial, 20, 50 + 15, this.width - 260 - 20, this.font.lineHeight + 6);

        this.renderResourceScrollArea(graphics, mouseX, mouseY, partial, currentDirFieldYEnd);

        this.renderFileTypeScrollArea(graphics, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("fancymenu.ui.filechooser.preview");
        int previewLabelWidth = this.font.width(previewLabel);
        graphics.drawString(this.font, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.renderConfirmButton(graphics, mouseX, mouseY, partial);

        this.renderCancelButton(graphics, mouseX, mouseY, partial);

        this.renderPreview(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    protected void renderConfirmButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.confirmButton.setX(this.width - 20 - this.confirmButton.getWidth());
        this.confirmButton.setY(this.height - 20 - 20);
        this.confirmButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderCancelButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.confirmButton.getY() - 5 - 20);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderFileTypeScrollArea(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.fileTypeScrollArea.verticalScrollBar.active = false;
        this.fileTypeScrollArea.setWidth(this.getBelowResourceScrollAreaElementWidth());
        this.fileTypeScrollArea.setX(this.resourceListScrollArea.getXWithBorder() + this.resourceListScrollArea.getWidthWithBorder() - this.fileTypeScrollArea.getWidthWithBorder());
        this.fileTypeScrollArea.setY(this.resourceListScrollArea.getYWithBorder() + this.resourceListScrollArea.getHeightWithBorder() + 5 + this.fileTypeScrollListYOffset);
        this.fileTypeScrollArea.render(graphics, mouseX, mouseY, partial);
        graphics.drawString(this.font, FILE_TYPE_PREFIX_TEXT, this.fileTypeScrollArea.getXWithBorder() - Minecraft.getInstance().font.width(FILE_TYPE_PREFIX_TEXT) - 5, this.fileTypeScrollArea.getYWithBorder() + (this.fileTypeScrollArea.getHeightWithBorder() / 2) - (Minecraft.getInstance().font.lineHeight / 2), UIBase.getUIColorTheme().element_label_color_normal.getColorInt(), false);
    }

    protected void renderResourceScrollArea(GuiGraphics graphics, int mouseX, int mouseY, float partial, int currentDirFieldYEnd) {
        this.resourceListScrollArea.setWidth(this.width - 260 - 20, true);
        this.resourceListScrollArea.setHeight(this.height - 85 - (this.font.lineHeight + 6) - 2 - 25 + this.resourceScrollListHeightOffset, true);
        this.resourceListScrollArea.setX(20, true);
        this.resourceListScrollArea.setY(currentDirFieldYEnd + 2, true);
        this.resourceListScrollArea.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderPreview(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.tickTextPreview();
        if (this.previewTextureSupplier != null) {
            ITexture t = this.previewTextureSupplier.get();
            ResourceLocation loc = (t != null) ? t.getResourceLocation() : null;
            if (loc != null) {
                AspectRatio ratio = t.getAspectRatio();
                int[] size = ratio.getAspectRatioSizeByMaximumSize(200, (this.cancelButton.getY() - 50) - (50 + 15));
                int w = size[0];
                int h = size[1];
                int x = this.width - 20 - w;
                int y = 50 + 15;
                UIBase.resetShaderColor(graphics);
                graphics.fill(x, y, x + w, y + h, UIBase.getUIColorTheme().area_background_color.getColorInt());
                RenderingUtils.resetShaderColor(graphics);
                RenderSystem.enableBlend();
                graphics.blit(loc, x, y, 0.0F, 0.0F, w, h, w, h);
                UIBase.resetShaderColor(graphics);
                UIBase.renderBorder(graphics, x, y, x + w, y + h, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUIColorTheme().element_border_color_normal.getColor(), true, true, true, true);
            }
        } else {
            this.previewTextScrollArea.setWidth(200, true);
            this.previewTextScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
            this.previewTextScrollArea.setX(this.width - 20 - this.previewTextScrollArea.getWidthWithBorder(), true);
            this.previewTextScrollArea.setY(50 + 15, true);
            this.previewTextScrollArea.render(graphics, mouseX, mouseY, partial);
        }
        UIBase.resetShaderColor(graphics);
    }

    protected int renderCurrentDirectoryField(GuiGraphics graphics, int mouseX, int mouseY, float partial, int x, int y, int width, int height) {
        int xEnd = x + width;
        int yEnd = y + height;
        graphics.fill(x + 1, y + 1, xEnd - 1, yEnd - 1, UIBase.getUIColorTheme().area_background_color.getColorInt());
        UIBase.renderBorder(graphics, x, y, xEnd, yEnd, 1, UIBase.getUIColorTheme().element_border_color_normal.getColor(), true, true, true, true);
        this.currentDirectoryComponent.setX(x + 4);
        this.currentDirectoryComponent.setY(y + (height / 2) - (this.currentDirectoryComponent.getHeight() / 2));
        this.currentDirectoryComponent.render(graphics, mouseX, mouseY, partial);
        return yEnd;
    }

    protected int getBelowResourceScrollAreaElementWidth() {
        return this.resourceListScrollArea.getWidthWithBorder() - Minecraft.getInstance().font.width(FILE_TYPE_PREFIX_TEXT) - 5;
    }

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

    @Nullable
    protected ResourceScrollAreaEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.resourceListScrollArea.getEntries()) {
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
        entry.setBackgroundColorHover(entry.getBackgroundColorIdle());
        entry.setHeight(this.fileTypeScrollArea.getInnerHeight());
        this.fileTypeScrollArea.addEntry(entry);
    }

    public void updatePreview(@Nullable ResourceLocation location) {
        if (location != null) {
            this.setTextPreview(location);
            if (isImageLocation(location)) {
                this.previewTextureSupplier = ResourceSupplier.image(location.toString());
            } else {
                this.previewTextureSupplier = null;
            }
        } else {
            this.setTextPreview(null);
            this.previewTextureSupplier = null;
        }
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

    protected void tickTextPreview() {
        if (this.previewTextScrollArea == null) return;
        if (this.previewTextSupplier != null) {
            IText text = this.previewTextSupplier.get();
            if (!Objects.equals(this.currentPreviewText, text)) {
                if (text == null) {
                    this.setNoTextPreview();
                } else {
                    this.previewTextScrollArea.clearEntries();
                    List<String> lines = text.getTextLines();
                    if (lines != null) {
                        int line = 0;
                        for (String s : lines) {
                            line++;
                            if (line < 70) {
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal(s).withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorIdle());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                            } else {
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("......").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorIdle());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                                TextScrollAreaEntry e2 = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("  ").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
                                e2.setSelectable(false);
                                e2.setBackgroundColorHover(e2.getBackgroundColorIdle());
                                e2.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e2);
                                break;
                            }
                        }
                        int totalWidth = this.previewTextScrollArea.getTotalEntryWidth();
                        for (ScrollAreaEntry e : this.previewTextScrollArea.getEntries()) {
                            e.setWidth(totalWidth);
                        }
                    } else {
                        return;
                    }
                }
                this.currentPreviewText = text;
            }
        } else {
            if (this.currentPreviewText != null) this.setNoTextPreview();
            this.currentPreviewText = null;
        }
    }

    protected void setNoTextPreview() {
        if (this.previewTextScrollArea == null) return;
        this.previewTextScrollArea.clearEntries();
        TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.translatable("fancymenu.ui.filechooser.no_preview").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
        e.setSelectable(false);
        e.setBackgroundColorHover(e.getBackgroundColorIdle());
        e.setPlayClickSound(false);
        this.previewTextScrollArea.addEntry(e);
    }

    protected void updateResourceList() {
        this.resourceListScrollArea.clearEntries();
        if (!this.currentIsRootDirectory()) {
            ParentDirScrollAreaEntry e = new ParentDirScrollAreaEntry(this.resourceListScrollArea);
            this.resourceListScrollArea.addEntry(e);
        }

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (this.currentNamespace == null) {
            List<String> namespaces = new ArrayList<>(resourceManager.getNamespaces());
            FilenameComparator comparator = new FilenameComparator();
            namespaces.sort(comparator);
            for (String namespace : namespaces) {
                DirectoryScrollAreaEntry entry = new DirectoryScrollAreaEntry(this.resourceListScrollArea, namespace, namespace, "");
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyName(namespace);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.resourceListScrollArea.addEntry(entry);
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
                DirectoryScrollAreaEntry entry = new DirectoryScrollAreaEntry(this.resourceListScrollArea, dir, this.currentNamespace, fullPath);
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyName(dir);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.resourceListScrollArea.addEntry(entry);
            }

            files.sort((o1, o2) -> comparator.compare(getLocationDisplayName(o1), getLocationDisplayName(o2)));
            for (ResourceLocation location : files) {
                ResourceScrollAreaEntry entry = new ResourceScrollAreaEntry(this.resourceListScrollArea, location);
                if (this.blockResourceUnfriendlyNames) entry.resourceUnfriendlyName = !isResourceFriendlyLocation(location);
                if (entry.resourceUnfriendlyName) entry.setSelectable(false);
                if (entry.resourceUnfriendlyName && !this.showBlockedResourceUnfriendlyNames) continue;
                this.resourceListScrollArea.addEntry(entry);
                if (this.preselectedLocation != null && this.preselectedLocation.equals(location)) {
                    entry.setSelected(true);
                    this.updatePreview(location);
                }
            }
        }
    }

    @NotNull
    protected Set<ResourceLocation> getAllResourceLocations() {
        if (this.cachedResourceLocations == null) {
            this.cachedResourceLocations = new HashSet<>();
            try {
                ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
                resourceManager.listPacks().forEach(pack -> this.collectLocationsFromPack(pack, this.cachedResourceLocations));
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to scan resource packs for ResourcePickerScreen!", ex);
            }
        }
        return this.cachedResourceLocations;
    }

    protected void collectLocationsFromPack(@NotNull PackResources pack, @NotNull Set<ResourceLocation> output) {
        try {
            if (pack instanceof CompositePackResources composite) {
                List<PackResources> stack = getCompositePackResourcesStack(composite);
                if (stack != null) {
                    for (PackResources nested : stack) {
                        this.collectLocationsFromPack(nested, output);
                    }
                } else {
                    PackResources primary = getFieldValue(composite, "primaryPackResources", PackResources.class);
                    if (primary != null) {
                        this.collectLocationsFromPack(primary, output);
                    }
                }
                return;
            }
            if (pack instanceof FilePackResources filePack) {
                this.collectLocationsFromFilePack(filePack, output);
                return;
            }
            if (pack instanceof PathPackResources pathPack) {
                this.collectLocationsFromPathPack(pathPack, output);
                return;
            }
            if (pack instanceof VanillaPackResources vanillaPack) {
                this.collectLocationsFromVanillaPack(vanillaPack, output);
            }
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to scan resource pack: " + pack, ex);
        }
    }

    protected void collectLocationsFromPathPack(@NotNull PathPackResources pack, @NotNull Set<ResourceLocation> output) {
        Path root = getFieldValue(pack, "root", Path.class);
        if (root == null) return;
        Path assetsRoot = root.resolve(PackType.CLIENT_RESOURCES.getDirectory());
        this.collectLocationsFromRoot(assetsRoot, output);
    }

    @SuppressWarnings("unchecked")
    protected void collectLocationsFromVanillaPack(@NotNull VanillaPackResources pack, @NotNull Set<ResourceLocation> output) {
        Map<PackType, List<Path>> pathsForType = getFieldValue(pack, "pathsForType", Map.class);
        if (pathsForType == null) return;
        List<Path> roots = pathsForType.get(PackType.CLIENT_RESOURCES);
        if (roots == null) return;
        for (Path root : roots) {
            this.collectLocationsFromRoot(root, output);
        }
    }

    protected void collectLocationsFromRoot(@NotNull Path assetsRoot, @NotNull Set<ResourceLocation> output) {
        if (!Files.exists(assetsRoot) || !Files.isDirectory(assetsRoot)) return;
        try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(assetsRoot)) {
            for (Path namespaceDir : namespaces) {
                if (!Files.isDirectory(namespaceDir)) continue;
                String namespace = namespaceDir.getFileName().toString();
                if (!ResourceLocation.isValidNamespace(namespace)) continue;
                try (Stream<Path> files = Files.walk(namespaceDir)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        String path = namespaceDir.relativize(file).toString().replace(File.separatorChar, '/');
                        if (path.isEmpty() || path.endsWith(".mcmeta")) return;
                        ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
                        if (location != null) output.add(location);
                    });
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to scan resource root: " + assetsRoot, ex);
        }
    }

    protected void collectLocationsFromFilePack(@NotNull FilePackResources pack, @NotNull Set<ResourceLocation> output) {
        Object zipAccess = getFieldValue(pack, "zipFileAccess", Object.class);
        ZipFile zipFile = getZipFile(zipAccess);
        if (zipFile == null) return;
        String prefix = getFieldValue(pack, "prefix", String.class);
        if (prefix == null) prefix = "";
        String basePrefix = prefix.isEmpty() ? "" : prefix + "/";
        String assetsPrefix = basePrefix + PackType.CLIENT_RESOURCES.getDirectory() + "/";
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            String name = entry.getName();
            if (!name.startsWith(assetsPrefix)) continue;
            String remainder = name.substring(assetsPrefix.length());
            if (remainder.isEmpty()) continue;
            int slashIndex = remainder.indexOf('/');
            if (slashIndex <= 0) continue;
            String namespace = remainder.substring(0, slashIndex);
            if (!ResourceLocation.isValidNamespace(namespace)) continue;
            String path = remainder.substring(slashIndex + 1);
            if (path.isEmpty() || path.endsWith(".mcmeta")) continue;
            ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
            if (location != null) output.add(location);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected List<PackResources> getCompositePackResourcesStack(@NotNull CompositePackResources pack) {
        return getFieldValue(pack, "packResourcesStack", List.class);
    }

    @Nullable
    protected ZipFile getZipFile(@Nullable Object zipAccess) {
        if (zipAccess == null) return null;
        try {
            Method method = zipAccess.getClass().getDeclaredMethod("getOrCreateZipFile");
            method.setAccessible(true);
            return (ZipFile) method.invoke(zipAccess);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to access zip file for resource pack.", ex);
            return null;
        }
    }

    @Nullable
    protected <T> T getFieldValue(@NotNull Object target, @NotNull String fieldName, @NotNull Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            if (type.isInstance(value)) return type.cast(value);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to access field " + fieldName + " on " + target.getClass().getName(), ex);
        }
        return null;
    }

    protected boolean currentIsRootDirectory() {
        return this.currentNamespace == null;
    }

    public void setDirectory(@Nullable String namespace, @NotNull String path, boolean playSound) {
        if (playSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.updatePreview(null);
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

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (keycode == InputConstants.KEY_ENTER) {
            ResourceLocation selected = this.getSelectedLocation();
            if (selected != null) {
                this.callback.accept(selected);
                return true;
            }
        }
        return super.keyPressed(keycode, scancode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0) && !this.resourceListScrollArea.isMouseInsideArea() && !this.resourceListScrollArea.isMouseInteractingWithGrabbers() && !this.previewTextScrollArea.isMouseInsideArea() && !this.previewTextScrollArea.isMouseInteractingWithGrabbers() && !this.isWidgetHovered()) {
            for (ScrollAreaEntry e : this.resourceListScrollArea.getEntries()) {
                e.setSelected(false);
            }
            this.updatePreview(null);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected boolean isWidgetHovered() {
        for (GuiEventListener l : this.children()) {
            if (l instanceof AbstractWidget w) {
                if (w.isHovered()) return true;
            }
        }
        return false;
    }

    public abstract class AbstractResourceScrollAreaEntry extends ScrollAreaEntry {

        private static final int BORDER = 3;

        public Font font = Minecraft.getInstance().font;
        protected boolean resourceUnfriendlyName = false;
        protected final MutableComponent entryNameComponent;
        protected long lastClick = -1;
        protected boolean directory;

        public AbstractResourceScrollAreaEntry(@NotNull ScrollArea parent, @NotNull String entryName, boolean directory) {
            super(parent, 100, 30);
            this.directory = directory;
            this.entryNameComponent = Component.literal(entryName);

            this.setWidth(this.font.width(this.entryNameComponent) + (BORDER * 2) + 20 + 3);
            this.setHeight((BORDER * 2) + 20);

            this.playClickSound = false;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            super.render(graphics, mouseX, mouseY, partial);

            RenderSystem.enableBlend();

            UIBase.getUIColorTheme().setUITextureShaderColor(graphics, 1.0F);
            ResourceLocation loc = this.directory ? FOLDER_ICON_TEXTURE : FILE_ICON_TEXTURE;
            graphics.blit(loc, this.x + BORDER, this.y + BORDER, 0.0F, 0.0F, 20, 20, 20, 20);
            UIBase.resetShaderColor(graphics);

            int textColor = this.resourceUnfriendlyName ? UIBase.getUIColorTheme().error_text_color.getColorInt() : UIBase.getUIColorTheme().description_area_text_color.getColorInt();
            graphics.drawString(this.font, this.entryNameComponent, this.x + BORDER + 20 + 3, this.y + (this.height / 2) - (this.font.lineHeight / 2), textColor, false);

            if (this.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height) && this.parent.isMouseInsideArea() && this.resourceUnfriendlyName) {
                TooltipHandler.INSTANCE.addTooltip(Tooltip.of(Component.translatable("fancymenu.ui.filechooser.resource_name_check.not_passed.tooltip")).setDefaultStyle(), () -> true, true, true);
            }

        }

        @Override
        public abstract void onClick(ScrollAreaEntry entry);

    }

    public class DirectoryScrollAreaEntry extends AbstractResourceScrollAreaEntry {

        protected final String namespace;
        protected final String path;

        public DirectoryScrollAreaEntry(@NotNull ScrollArea parent, @NotNull String entryName, @Nullable String namespace, @NotNull String path) {
            super(parent, entryName, true);
            this.namespace = namespace;
            this.path = path;
        }

        @Override
        public void onClick(ScrollAreaEntry entry) {
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
            super(parent, ResourcePickerScreen.this.getLocationDisplayName(location), false);
            this.location = location;
        }

        @Override
        public void onClick(ScrollAreaEntry entry) {
            if (this.resourceUnfriendlyName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                ResourcePickerScreen.this.callback.accept(this.location);
            }
            ResourcePickerScreen.this.updatePreview(this.location);
            this.lastClick = now;
        }

    }

    public class ParentDirScrollAreaEntry extends ScrollAreaEntry {

        private static final int BORDER = 3;

        public Font font = Minecraft.getInstance().font;
        protected final MutableComponent labelComponent;
        protected long lastClick = -1;

        public ParentDirScrollAreaEntry(@NotNull ScrollArea parent) {
            super(parent, 100, 30);
            this.labelComponent = Component.translatable("fancymenu.ui.filechooser.go_up").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()).withBold(true));

            this.setWidth(this.font.width(this.labelComponent) + (BORDER * 2) + 20 + 3);
            this.setHeight((BORDER * 2) + 20);

            this.playClickSound = false;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            super.render(graphics, mouseX, mouseY, partial);

            RenderSystem.enableBlend();

            UIBase.getUIColorTheme().setUITextureShaderColor(graphics, 1.0F);
            graphics.blit(GO_UP_ICON_TEXTURE, this.x + BORDER, this.y + BORDER, 0.0F, 0.0F, 20, 20, 20, 20);
            UIBase.resetShaderColor(graphics);

            graphics.drawString(this.font, this.labelComponent, this.x + BORDER + 20 + 3, this.y + (this.height / 2) - (this.font.lineHeight / 2) , -1, false);

        }

        @Override
        public void onClick(ScrollAreaEntry entry) {
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (!ResourcePickerScreen.this.currentPath.isEmpty()) {
                    int lastSlash = ResourcePickerScreen.this.currentPath.lastIndexOf('/');
                    if (lastSlash > -1) {
                        String newPath = ResourcePickerScreen.this.currentPath.substring(0, lastSlash);
                        ResourcePickerScreen.this.setDirectory(ResourcePickerScreen.this.currentNamespace, newPath, true);
                    } else {
                        ResourcePickerScreen.this.setDirectory(ResourcePickerScreen.this.currentNamespace, "", true);
                    }
                } else if (ResourcePickerScreen.this.currentNamespace != null) {
                    ResourcePickerScreen.this.setDirectory(null, "", true);
                }
            }
            ResourcePickerScreen.this.updatePreview(null);
            this.lastClick = now;
        }

    }

}
