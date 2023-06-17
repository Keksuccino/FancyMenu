
package de.keksuccino.fancymenu.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.misc.FilenameComparator;
import de.keksuccino.fancymenu.misc.InputConstants;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.resources.texture.TextureHandler;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
public class FileChooserScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final ResourceLocation GO_UP_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/go_up_icon.png");
    protected static final ResourceLocation FILE_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/file_icon.png");
    protected static final ResourceLocation FOLDER_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/folder_icon.png");

    public static final FileFilter RESOURCE_NAME_FILTER = file -> {
        return CharacterFilter.getBasicFilenameCharacterFilter().isAllowed(ScreenCustomization.getPathWithoutGameDirectory(file.getAbsolutePath()).replace("/", "").replace("\\", ""));
    };
    public static final FileFilter WAV_AUDIO_FILE_FILTER = file -> {
        return (file.getPath().toLowerCase().endsWith(".wav"));
    };
    public static final FileFilter OGG_AUDIO_FILE_FILTER = file -> {
        if (!RESOURCE_NAME_FILTER.checkFile(file)) return false;
        return (file.getPath().toLowerCase().endsWith(".ogg"));
    };
    public static final FileFilter TXT_FILE_FILTER = file -> {
        return (file.getPath().toLowerCase().endsWith(".txt"));
    };
    public static final FileFilter PLAIN_TEXT_FILE_FILTER = file -> {
        if (file.getPath().toLowerCase().endsWith(".txt")) return true;
        if (file.getPath().toLowerCase().endsWith(".json")) return true;
        if (file.getPath().toLowerCase().endsWith(".log")) return true;
        if (file.getPath().toLowerCase().endsWith(".lang")) return true;
        if (file.getPath().toLowerCase().endsWith(".local")) return true;
        if (file.getPath().toLowerCase().endsWith(".properties")) return true;
        return false;
    };
    public static final FileFilter IMAGE_FILE_FILTER = file -> {
        if (!RESOURCE_NAME_FILTER.checkFile(file)) return false;
        if (file.getPath().toLowerCase().endsWith(".png")) return true;
        if (file.getPath().toLowerCase().endsWith(".jpg")) return true;
        if (file.getPath().toLowerCase().endsWith(".jpeg")) return true;
        return false;
    };
    public static final FileFilter IMAGE_AND_GIF_FILE_FILTER = file -> {
        if (!RESOURCE_NAME_FILTER.checkFile(file)) return false;
        if (file.getPath().toLowerCase().endsWith(".png")) return true;
        if (file.getPath().toLowerCase().endsWith(".jpg")) return true;
        if (file.getPath().toLowerCase().endsWith(".jpeg")) return true;
        if (file.getPath().toLowerCase().endsWith(".gif")) return true;
        return false;
    };

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

    protected ScrollArea fileListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea textFilePreviewScrollArea = new ScrollArea(0, 0, 0, 0);
    protected LocalTexture previewTexture;
    protected ExtendedButton okButton;
    protected ExtendedButton cancelButton;

    public FileChooserScreen(@Nullable File rootDirectory, @NotNull File startDirectory, @NotNull Consumer<File> callback) {

        super(Component.translatable("fancymenu.ui.filechooser.choose.file"));

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

        super.init();

        this.okButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.ok"), (button) -> {
            FileScrollAreaEntry selected = this.getSelectedEntry();
            if (selected != null) {
                this.callback.accept(selected.file);
            }
        }) {
            @Override
            public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
                FileScrollAreaEntry e = FileChooserScreen.this.getSelectedEntry();
                this.active = (e != null) && (e.file.isFile());
                super.render(pose, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.okButton);
        UIBase.applyDefaultButtonSkinTo(this.okButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screenBackgroundColor.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(pose, titleComp, 20, 20, UIBase.getUIColorScheme().genericTextBaseColor.getColorInt());

        this.font.draw(pose, Component.translatable("fancymenu.ui.filechooser.files"), 20, 50, UIBase.getUIColorScheme().genericTextBaseColor.getColorInt());

        this.fileListScrollArea.setWidth((this.width / 2) - 40, true);
        this.fileListScrollArea.setHeight(this.height - 85, true);
        this.fileListScrollArea.setX(20, true);
        this.fileListScrollArea.setY(50 + 15, true);
        this.fileListScrollArea.render(pose, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("fancymenu.ui.filechooser.preview");
        int previewLabelWidth = this.font.width(previewLabel);
        this.font.draw(pose, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUIColorScheme().genericTextBaseColor.getColorInt());

        this.okButton.setX(this.width - 20 - this.okButton.getWidth());
        this.okButton.setY(this.height - 20 - 20);
        this.okButton.render(pose, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.okButton.getY() - 5 - 20);
        this.cancelButton.render(pose, mouseX, mouseY, partial);

        if (this.previewTexture != null) {
            AspectRatio ratio = this.previewTexture.getAspectRatio();
            int[] size = ratio.getAspectRatioSizeByMaximumSize((this.width / 2) - 40, (this.cancelButton.y - 50) - (50 + 15));
            int w = size[0];
            int h = size[1];
            int x = this.width - 20 - w;
            int y = 50 + 15;
            UIBase.resetShaderColor();
            fill(pose, x, y, x + w, y + h, UIBase.getUIColorScheme().areaBackgroundColor.getColorInt());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderUtils.bindTexture(this.previewTexture.getResourceLocation());
            blit(pose, x, y, 0.0F, 0.0F, w, h, w, h);
            UIBase.resetShaderColor();
            UIBase.renderBorder(pose, x, y, x + w, y + h, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUIColorScheme().elementBorderColorNormal.getColor(), true, true, true, true);
        } else {
            this.textFilePreviewScrollArea.setWidth((this.width / 2) - 40, true);
            this.textFilePreviewScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
            this.textFilePreviewScrollArea.setX(this.width - 20 - this.textFilePreviewScrollArea.getWidthWithBorder(), true);
            this.textFilePreviewScrollArea.setY(50 + 15, true);
            this.textFilePreviewScrollArea.render(pose, mouseX, mouseY, partial);
        }

        super.render(pose, mouseX, mouseY, partial);

    }

    @Nullable
    public FileFilter getFileFilter() {
        return fileFilter;
    }

    public FileChooserScreen setFileFilter(@Nullable FileFilter fileFilter) {
        this.fileFilter = fileFilter;
        this.updateFilesList();
        return this;
    }

    public FileChooserScreen setDirectory(@NotNull File newDirectory, boolean playSound) {
        Objects.requireNonNull(newDirectory);
        if (!this.isInRootOrSubOfRoot(newDirectory)) return this;
        if (playSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.updateTextPreview(null);
        this.previewTexture = null;
        this.currentDir = newDirectory;
        lastDirectory = newDirectory;
        this.updateFilesList();
        return this;
    }

    @Nullable
    protected FileScrollAreaEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e instanceof FileScrollAreaEntry f) {
                if (f.isSelected()) return f;
            }
        }
        return null;
    }

    protected void updateTextPreview(@Nullable File file) {
        this.textFilePreviewScrollArea.clearEntries();
        if ((file != null) && file.isFile() && PLAIN_TEXT_FILE_FILTER.checkFile(file)) {
            for (String s : FileUtils.getFileLines(file)) {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.textFilePreviewScrollArea, Component.literal(s).withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().descriptionAreaTextColor.getColorInt())), (entry) -> {});
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
            TextScrollAreaEntry e = new TextScrollAreaEntry(this.textFilePreviewScrollArea, Component.translatable("fancymenu.ui.filechooser.no_preview").withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().descriptionAreaTextColor.getColorInt())), (entry) -> {});
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
            for (File f : folders) {
                FileScrollAreaEntry e = new FileScrollAreaEntry(this.fileListScrollArea, f);
                this.fileListScrollArea.addEntry(e);
            }
            for (File f : files) {
                if ((this.fileFilter != null) && !this.fileFilter.checkFile(f)) continue;
                FileScrollAreaEntry e = new FileScrollAreaEntry(this.fileListScrollArea, f);
                this.fileListScrollArea.addEntry(e);
            }
        }
    }

    protected boolean currentIsRootDirectory() {
        if (this.rootDirectory == null) return false;
        return this.rootDirectory.getAbsolutePath().equals(this.currentDir.getAbsolutePath());
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
    public boolean keyPressed(int button, int $$1, int $$2) {

        if (button == InputConstants.KEY_ENTER) {
            FileScrollAreaEntry selected = this.getSelectedEntry();
            if (selected != null) {
                this.callback.accept(selected.file);
                return true;
            }
        }

        return super.keyPressed(button, $$1, $$2);

    }

    public class FileScrollAreaEntry extends ScrollAreaEntry {

        private static final int BORDER = 3;

        public File file;
        public Font font = Minecraft.getInstance().font;

        protected final MutableComponent fileNameComponent;
        protected long lastClick = -1;

        public FileScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {

            super(parent, 100, 30);
            this.file = file;
            this.fileNameComponent = Component.literal(this.file.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().descriptionAreaTextColor.getColorInt()));

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
                this.font.draw(pose, this.fileNameComponent, this.x + BORDER + 20 + 3, this.y + ((float)this.height / 2) - ((float)this.font.lineHeight / 2) , -1);

            }

        }

        @Override
        public void onClick(ScrollAreaEntry entry) {
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (this.file.isFile()) {
                    FileChooserScreen.this.callback.accept(this.file);
                } else if (this.file.isDirectory()) {
                    FileChooserScreen.this.setDirectory(this.file, true);
                }
            }
            if (this.file.isFile()) {
                FileChooserScreen.this.updateTextPreview(this.file);
                if (IMAGE_FILE_FILTER.checkFile(this.file)) {
                    FileChooserScreen.this.previewTexture = TextureHandler.INSTANCE.getTexture(this.file);
                } else {
                    FileChooserScreen.this.previewTexture = null;
                }
            } else {
                FileChooserScreen.this.updateTextPreview(null);
                FileChooserScreen.this.previewTexture = null;
            }
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
            this.labelComponent = Component.translatable("fancymenu.ui.filechooser.go_up").setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().elementLabelColorNormal.getColorInt()).withBold(true));

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
                if (!FileChooserScreen.this.currentIsRootDirectory()) {
                    File parent = FileChooserScreen.this.getParentDirectoryOfCurrent();
                    if ((parent != null) && parent.isDirectory()) {
                        FileChooserScreen.this.setDirectory(parent, true);
                    }
                }
            }
            FileChooserScreen.this.updateTextPreview(null);
            FileChooserScreen.this.previewTexture = null;
            this.lastClick = now;
        }

    }

    @FunctionalInterface
    public interface FileFilter {
        boolean checkFile(@NotNull File file);
    }

}
