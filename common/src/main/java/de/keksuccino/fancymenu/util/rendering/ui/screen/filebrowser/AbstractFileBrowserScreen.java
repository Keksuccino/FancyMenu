package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
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
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("all")
public abstract class AbstractFileBrowserScreen extends Screen {

    protected static final Logger LOGGER = LogManager.getLogger();

    protected static final ResourceLocation GO_UP_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/go_up_icon.png");
    protected static final ResourceLocation FILE_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_icon.png");
    protected static final ResourceLocation FOLDER_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/folder_icon.png");
    protected static final Component FILE_TYPE_PREFIX_TEXT = Component.translatable("fancymenu.file_browser.file_type");

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
    protected ScrollArea fileListScrollArea = new ScrollArea(0, 0, 0, 0);
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
    protected ExtendedButton openInExplorerButton;
    protected ComponentWidget currentDirectoryComponent;
    protected int fileScrollListHeightOffset = 0;
    protected int fileTypeScrollListYOffset = 0;
    @Nullable
    protected MutableComponent currentFileTypesComponent;

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

        this.setTextPreview(null);
        this.updateFilesList();

    }

    @Override
    protected void init() {

        this.confirmButton = this.buildConfirmButton();
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultWidgetSkinTo(this.confirmButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

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

        this.updateCurrentDirectoryComponent();

        this.updateFileTypeScrollArea();

    }

    @NotNull
    protected abstract ExtendedButton buildConfirmButton();

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        //Update horizontal scroll bar of file types area
        if (this.currentFileTypesComponent != null) {
            this.fileTypeScrollArea.horizontalScrollBar.active = (Minecraft.getInstance().font.width(this.currentFileTypesComponent) > (this.fileTypeScrollArea.getInnerWidth() - 10));
        }

        RenderSystem.enableBlend();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, Component.translatable("fancymenu.ui.filechooser.files"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        int currentDirFieldYEnd = this.renderCurrentDirectoryField(graphics, mouseX, mouseY, partial, 20, 50 + 15, this.width - 260 - 20, this.font.lineHeight + 6);

        this.renderFileScrollArea(graphics, mouseX, mouseY, partial, currentDirFieldYEnd);

        this.renderFileTypeScrollArea(graphics, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("fancymenu.ui.filechooser.preview");
        int previewLabelWidth = this.font.width(previewLabel);
        graphics.drawString(this.font, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.renderConfirmButton(graphics, mouseX, mouseY, partial);

        this.renderCancelButton(graphics, mouseX, mouseY, partial);

        this.renderOpenInExplorerButton(graphics, mouseX, mouseY, partial);

        this.renderPreview(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    protected void renderOpenInExplorerButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.openInExplorerButton.setX(this.width - 20 - this.openInExplorerButton.getWidth());
        this.openInExplorerButton.setY(this.cancelButton.getY() - 15 - 20);
        this.openInExplorerButton.render(graphics, mouseX, mouseY, partial);
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
        this.fileTypeScrollArea.setWidth(this.getBelowFileScrollAreaElementWidth());
        this.fileTypeScrollArea.setX(this.fileListScrollArea.getXWithBorder() + this.fileListScrollArea.getWidthWithBorder() - this.fileTypeScrollArea.getWidthWithBorder());
        this.fileTypeScrollArea.setY(this.fileListScrollArea.getYWithBorder() + this.fileListScrollArea.getHeightWithBorder() + 5 + this.fileTypeScrollListYOffset);
        this.fileTypeScrollArea.render(graphics, mouseX, mouseY, partial);
        graphics.drawString(this.font, FILE_TYPE_PREFIX_TEXT, this.fileTypeScrollArea.getXWithBorder() - Minecraft.getInstance().font.width(FILE_TYPE_PREFIX_TEXT) - 5, this.fileTypeScrollArea.getYWithBorder() + (this.fileTypeScrollArea.getHeightWithBorder() / 2) - (Minecraft.getInstance().font.lineHeight / 2), UIBase.getUIColorTheme().element_label_color_normal.getColorInt(), false);
    }

    protected void renderFileScrollArea(GuiGraphics graphics, int mouseX, int mouseY, float partial, int currentDirFieldYEnd) {
        this.fileListScrollArea.setWidth(this.width - 260 - 20, true);
        this.fileListScrollArea.setHeight(this.height - 85 - (this.font.lineHeight + 6) - 2 - 25 + this.fileScrollListHeightOffset, true);
        this.fileListScrollArea.setX(20, true);
        this.fileListScrollArea.setY(currentDirFieldYEnd + 2, true);
        this.fileListScrollArea.render(graphics, mouseX, mouseY, partial);
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

    protected int getBelowFileScrollAreaElementWidth() {
        return this.fileListScrollArea.getWidthWithBorder() - Minecraft.getInstance().font.width(FILE_TYPE_PREFIX_TEXT) - 5;
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
                                    if (consumes.isHoveredOrFocused()) return Component.literal(fFinal.getName()).setStyle(Style.EMPTY.withStrikethrough(true).withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
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
            this.currentDirectoryComponent.setBaseColorSupplier(consumes -> UIBase.getUIColorTheme().description_area_text_color);
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
        this.updatePreview(null);
        this.currentDir = newDirectory;
        lastDirectory = newDirectory;
        this.updateFilesList();
        MainThreadTaskExecutor.executeInMainThread(this::updateCurrentDirectoryComponent, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
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
        this.currentFileTypesComponent = this.currentFileTypesComponent.withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()));
        TextScrollAreaEntry entry = new TextScrollAreaEntry(this.fileTypeScrollArea, this.currentFileTypesComponent, textScrollAreaEntry -> {});
        entry.setPlayClickSound(false);
        entry.setSelectable(false);
        entry.setBackgroundColorHover(entry.getBackgroundColorIdle());
        entry.setHeight(this.fileTypeScrollArea.getInnerHeight());
        this.fileTypeScrollArea.addEntry(entry);
    }

    public void updatePreview(@Nullable File file) {
        if ((file != null) && file.isFile()) {
            this.setTextPreview(file);
            if (FileTypes.getLocalType(file) instanceof ImageFileType) {
                this.previewTextureSupplier = ResourceSupplier.image(file.getPath());
            } else {
                this.previewTextureSupplier = null;
            }
        } else {
            this.setTextPreview(null);
            this.previewTextureSupplier = null;
        }
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
                        //Don't update currentPreviewText, so the method keeps checking if the lines list is null and if not, update the preview
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

    protected void updateFilesList() {
        this.fileListScrollArea.clearEntries();
        if (!this.currentIsRootDirectory()) {
            ParentDirScrollAreaEntry e = new ParentDirScrollAreaEntry(this.fileListScrollArea);
            this.fileListScrollArea.addEntry(e);
        }
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

    protected abstract AbstractFileScrollAreaEntry buildFileEntry(@NotNull File f);

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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if ((button == 0) && !this.fileListScrollArea.isMouseInsideArea() && !this.fileListScrollArea.isMouseInteractingWithGrabbers() && !this.previewTextScrollArea.isMouseInsideArea() && !this.previewTextScrollArea.isMouseInteractingWithGrabbers() && !this.isWidgetHovered()) {
            for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
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

    public abstract class AbstractFileScrollAreaEntry extends ScrollAreaEntry {

        private static final int BORDER = 3;

        public File file;
        public Font font = Minecraft.getInstance().font;
        protected boolean resourceUnfriendlyFileName = false;
        protected final MutableComponent fileNameComponent;
        protected long lastClick = -1;

        public AbstractFileScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {

            super(parent, 100, 30);
            this.file = file;
            this.fileNameComponent = Component.literal(this.file.getName());

            this.setWidth(this.font.width(this.fileNameComponent) + (BORDER * 2) + 20 + 3);
            this.setHeight((BORDER * 2) + 20);

            this.playClickSound = false;

        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            super.render(graphics, mouseX, mouseY, partial);

            if (this.file.exists()) {

                RenderSystem.enableBlend();

                //Render icon
                UIBase.getUIColorTheme().setUITextureShaderColor(graphics, 1.0F);
                ResourceLocation loc = this.file.isFile() ? FILE_ICON_TEXTURE : FOLDER_ICON_TEXTURE;
                graphics.blit(loc, this.x + BORDER, this.y + BORDER, 0.0F, 0.0F, 20, 20, 20, 20);
                UIBase.resetShaderColor(graphics);

                //Render file name
                int textColor = this.resourceUnfriendlyFileName ? UIBase.getUIColorTheme().error_text_color.getColorInt() : UIBase.getUIColorTheme().description_area_text_color.getColorInt();
                graphics.drawString(this.font, this.fileNameComponent, this.x + BORDER + 20 + 3, this.y + (this.height / 2) - (this.font.lineHeight / 2) , textColor, false);

                //Show "unsupported characters" tooltip on hover, if resource-unfriendly file name
                if (this.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height) && this.parent.isMouseInsideArea() && this.resourceUnfriendlyFileName) {
                    TooltipHandler.INSTANCE.addTooltip(Tooltip.of(Component.translatable("fancymenu.ui.filechooser.resource_name_check.not_passed.tooltip")).setDefaultStyle(), () -> true, true, true);
                }

            }

        }

        @Override
        public abstract void onClick(ScrollAreaEntry entry);

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

            //Render icon
            UIBase.getUIColorTheme().setUITextureShaderColor(graphics, 1.0F);
            graphics.blit(GO_UP_ICON_TEXTURE, this.x + BORDER, this.y + BORDER, 0.0F, 0.0F, 20, 20, 20, 20);
            UIBase.resetShaderColor(graphics);

            //Render file name
            graphics.drawString(this.font, this.labelComponent, this.x + BORDER + 20 + 3, this.y + (this.height / 2) - (this.font.lineHeight / 2) , -1, false);

        }

        @Override
        public void onClick(ScrollAreaEntry entry) {
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (!AbstractFileBrowserScreen.this.currentIsRootDirectory()) {
                    File parent = AbstractFileBrowserScreen.this.getParentDirectoryOfCurrent();
                    if ((parent != null) && parent.isDirectory()) {
                        AbstractFileBrowserScreen.this.setDirectory(parent, true);
                    }
                }
            }
            AbstractFileBrowserScreen.this.updatePreview(null);
            this.lastClick = now;
        }

    }

}
