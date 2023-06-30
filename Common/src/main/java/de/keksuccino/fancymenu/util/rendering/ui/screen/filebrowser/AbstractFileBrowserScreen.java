package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.text.component.ComponentWidget;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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

    protected static final ResourceLocation GO_UP_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/go_up_icon.png");
    protected static final ResourceLocation FILE_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/file_icon.png");
    protected static final ResourceLocation FOLDER_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/folder_icon.png");

    @Nullable
    protected static File lastDirectory;

    @Nullable
    protected File rootDirectory;
    @NotNull
    protected File currentDir;
    @Nullable
    protected FileFilter fileFilter;
    @NotNull
    protected Consumer<File> callback;
    protected int visibleDirectoryLevelsAboveRoot = 0;
    protected boolean showSubDirectories = true;
    protected boolean blockResourceUnfriendlyFileNames = true;
    protected boolean showBlockedResourceUnfriendlyFiles = true;

    protected ScrollArea fileListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea textFilePreviewScrollArea = new ScrollArea(0, 0, 0, 0);
    protected LocalTexture previewTexture;
    protected ExtendedButton confirmButton;
    protected ExtendedButton cancelButton;
    protected ExtendedButton openInExplorerButton;
    protected ComponentWidget currentDirectoryComponent;

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

        this.updateTextPreview(null);
        this.updateFilesList();

    }

    @Override
    protected void init() {

        this.confirmButton = this.buildConfirmButton();
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultWidgetSkinTo(this.confirmButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.openInExplorerButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.ui.filechooser.open_in_explorer"), (button) -> {
            if (this.currentDir != null) de.keksuccino.fancymenu.util.file.FileUtils.openFile(this.currentDir);
        });
        this.addWidget(this.openInExplorerButton);
        UIBase.applyDefaultWidgetSkinTo(this.openInExplorerButton);

        this.updateCurrentDirectoryComponent();

    }

    @NotNull
    protected abstract ExtendedButton buildConfirmButton();

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(pose, titleComp, 20, 20, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.font.draw(pose, Component.translatable("fancymenu.ui.filechooser.files"), 20, 50, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        int currentDirFieldYEnd = this.renderCurrentDirectoryField(pose, mouseX, mouseY, partial, 20, 50 + 15, (this.width / 2) - 40, this.font.lineHeight + 6);

        this.renderFileScrollArea(pose, mouseX, mouseY, partial, currentDirFieldYEnd);

        Component previewLabel = Component.translatable("fancymenu.ui.filechooser.preview");
        int previewLabelWidth = this.font.width(previewLabel);
        this.font.draw(pose, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.renderConfirmButton(pose, mouseX, mouseY, partial);

        this.renderCancelButton(pose, mouseX, mouseY, partial);

        this.renderOpenInExplorerButton(pose, mouseX, mouseY, partial);

        this.renderPreview(pose, mouseX, mouseY, partial);

        super.render(pose, mouseX, mouseY, partial);

    }

    protected void renderOpenInExplorerButton(PoseStack pose, int mouseX, int mouseY, float partial) {
        this.openInExplorerButton.setX(this.width - 20 - this.openInExplorerButton.getWidth());
        this.openInExplorerButton.setY(this.cancelButton.getY() - 15 - 20);
        this.openInExplorerButton.render(pose, mouseX, mouseY, partial);
    }

    protected void renderConfirmButton(PoseStack pose, int mouseX, int mouseY, float partial) {
        this.confirmButton.setX(this.width - 20 - this.confirmButton.getWidth());
        this.confirmButton.setY(this.height - 20 - 20);
        this.confirmButton.render(pose, mouseX, mouseY, partial);
    }

    protected void renderCancelButton(PoseStack pose, int mouseX, int mouseY, float partial) {
        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.confirmButton.getY() - 5 - 20);
        this.cancelButton.render(pose, mouseX, mouseY, partial);
    }

    protected void renderFileScrollArea(PoseStack pose, int mouseX, int mouseY, float partial, int currentDirFieldYEnd) {
        this.fileListScrollArea.setWidth((this.width / 2) - 40, true);
        this.fileListScrollArea.setHeight(this.height - 85 - (this.font.lineHeight + 6) - 2, true);
        this.fileListScrollArea.setX(20, true);
        this.fileListScrollArea.setY(currentDirFieldYEnd + 2, true);
        this.fileListScrollArea.render(pose, mouseX, mouseY, partial);
    }

    protected void renderPreview(PoseStack pose, int mouseX, int mouseY, float partial) {
        if (this.previewTexture != null) {
            AspectRatio ratio = this.previewTexture.getAspectRatio();
            int[] size = ratio.getAspectRatioSizeByMaximumSize((this.width / 2) - 40, (this.cancelButton.y - 50) - (50 + 15));
            int w = size[0];
            int h = size[1];
            int x = this.width - 20 - w;
            int y = 50 + 15;
            UIBase.resetShaderColor();
            fill(pose, x, y, x + w, y + h, UIBase.getUIColorScheme().area_background_color.getColorInt());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderUtils.bindTexture(this.previewTexture.getResourceLocation());
            blit(pose, x, y, 0.0F, 0.0F, w, h, w, h);
            UIBase.resetShaderColor();
            UIBase.renderBorder(pose, x, y, x + w, y + h, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUIColorScheme().element_border_color_normal.getColor(), true, true, true, true);
        } else {
            this.textFilePreviewScrollArea.setWidth((this.width / 2) - 40, true);
            this.textFilePreviewScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
            this.textFilePreviewScrollArea.setX(this.width - 20 - this.textFilePreviewScrollArea.getWidthWithBorder(), true);
            this.textFilePreviewScrollArea.setY(50 + 15, true);
            this.textFilePreviewScrollArea.render(pose, mouseX, mouseY, partial);
        }
    }

    protected int renderCurrentDirectoryField(PoseStack pose, int mouseX, int mouseY, float partial, int x, int y, int width, int height) {
        int xEnd = x + width;
        int yEnd = y + height;
        fill(pose, x + 1, y + 1, xEnd - 1, yEnd - 1, UIBase.getUIColorScheme().area_background_color.getColorInt());
        UIBase.renderBorder(pose, x, y, xEnd, yEnd, 1, UIBase.getUIColorScheme().element_border_color_normal.getColor(), true, true, true, true);
        this.currentDirectoryComponent.x = x + 4;
        this.currentDirectoryComponent.y = y + (height / 2) - (this.currentDirectoryComponent.getHeight() / 2);
        this.currentDirectoryComponent.render(pose, mouseX, mouseY, partial);
        return yEnd;
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
                                    if (consumes.isHoveredOrFocused()) return Component.literal(fFinal.getName()).setStyle(Style.EMPTY.withStrikethrough(true).withColor(UIBase.getUIColorScheme().error_text_color.getColorInt()));
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
            while (this.currentDirectoryComponent.getWidth() > ((this.width / 2) - 40 - 8)) {
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
            this.currentDirectoryComponent.setBaseColorSupplier(consumes -> UIBase.getUIColorScheme().description_area_text_color);
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

    public AbstractFileBrowserScreen setDirectory(@NotNull File newDirectory, boolean playSound) {
        Objects.requireNonNull(newDirectory);
        if (!this.isInRootOrSubOfRoot(newDirectory)) return this;
        if (playSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.updateTextPreview(null);
        this.previewTexture = null;
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

    protected void updateTextPreview(@Nullable File file) {
        this.textFilePreviewScrollArea.clearEntries();
        if ((file != null) && file.isFile() && FileFilter.PLAIN_TEXT_FILE_FILTER.checkFile(file)) {
            for (String s : FileUtils.getFileLines(file)) {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.textFilePreviewScrollArea, Component.literal(s).withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().description_area_text_color.getColorInt())), (entry) -> {});
                e.setSelectable(false);
                e.setBackgroundColorHover(e.getBackgroundColorIdle());
                e.setPlayClickSound(false);
                this.textFilePreviewScrollArea.addEntry(e);
            }
            int totalWidth = this.textFilePreviewScrollArea.getTotalEntryWidth();
            for (ScrollAreaEntry e : this.textFilePreviewScrollArea.getEntries()) {
                e.setWidth(totalWidth);
            }
        } else {
            TextScrollAreaEntry e = new TextScrollAreaEntry(this.textFilePreviewScrollArea, Component.translatable("fancymenu.ui.filechooser.no_preview").withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().description_area_text_color.getColorInt())), (entry) -> {});
            e.setSelectable(false);
            e.setBackgroundColorHover(e.getBackgroundColorIdle());
            e.setPlayClickSound(false);
            this.textFilePreviewScrollArea.addEntry(e);
        }
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
                if ((this.fileFilter != null) && !this.fileFilter.checkFile(f)) continue;
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

        if ((button == 0) && !this.fileListScrollArea.isMouseInsideArea() && !this.fileListScrollArea.isMouseInteractingWithGrabbers() && !this.isWidgetHovered()) {
            for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
                e.setSelected(false);
            }
            this.updateTextPreview(null);
            this.previewTexture = null;
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
        public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

            super.render(pose, mouseX, mouseY, partial);

            if (this.file.exists()) {

                RenderSystem.enableBlend();

                //Render icon
                UIBase.getUIColorScheme().setUITextureShaderColor(1.0F);
                RenderUtils.bindTexture(this.file.isFile() ? FILE_ICON_TEXTURE : FOLDER_ICON_TEXTURE);
                blit(pose, this.x + BORDER, this.y + BORDER, 0.0F, 0.0F, 20, 20, 20, 20);
                UIBase.resetShaderColor();

                //Render file name
                int textColor = this.resourceUnfriendlyFileName ? UIBase.getUIColorScheme().error_text_color.getColorInt() : UIBase.getUIColorScheme().description_area_text_color.getColorInt();
                this.font.draw(pose, this.fileNameComponent, this.x + BORDER + 20 + 3, this.y + ((float)this.height / 2) - ((float)this.font.lineHeight / 2) , textColor);

                //Show "unsupported characters" tooltip on hover, if resource-unfriendly file name
                if (this.isHovered() && this.resourceUnfriendlyFileName) {
                    TooltipHandler.INSTANCE.addTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.ui.filechooser.resource_name_check.not_passed.tooltip")).setDefaultStyle(), () -> true, false, true);
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
            this.labelComponent = Component.translatable("fancymenu.ui.filechooser.go_up").setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().element_label_color_normal.getColorInt()).withBold(true));

            this.setWidth(this.font.width(this.labelComponent) + (BORDER * 2) + 20 + 3);
            this.setHeight((BORDER * 2) + 20);

            this.playClickSound = false;

        }

        @Override
        public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

            super.render(pose, mouseX, mouseY, partial);

            RenderSystem.enableBlend();

            //Render icon
            UIBase.getUIColorScheme().setUITextureShaderColor(1.0F);
            RenderUtils.bindTexture(GO_UP_ICON_TEXTURE);
            blit(pose, this.x + BORDER, this.y + BORDER, 0.0F, 0.0F, 20, 20, 20, 20);
            UIBase.resetShaderColor();

            //Render file name
            this.font.draw(pose, this.labelComponent, this.x + BORDER + 20 + 3, this.y + ((float)this.height / 2) - ((float)this.font.lineHeight / 2) , -1);

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
            AbstractFileBrowserScreen.this.updateTextPreview(null);
            AbstractFileBrowserScreen.this.previewTexture = null;
            this.lastClick = now;
        }

    }

}
