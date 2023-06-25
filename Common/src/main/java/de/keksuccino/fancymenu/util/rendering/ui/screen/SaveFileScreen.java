package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.text.component.ComponentWidget;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedEditBox;
import de.keksuccino.fancymenu.util.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
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
public class SaveFileScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final ResourceLocation GO_UP_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/go_up_icon.png");
    protected static final ResourceLocation FILE_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/file_icon.png");
    protected static final ResourceLocation FOLDER_ICON_TEXTURE = new ResourceLocation("fancymenu", "textures/folder_icon.png");

    @Nullable
    protected File rootDirectory;
    @NotNull
    protected File currentDir;
    @Nullable
    protected FileChooserScreen.FileFilter fileFilter;
    @Nullable
    protected String forcedFileExtension;
    @NotNull
    protected Consumer<File> callback;
    protected int visibleDirectoryLevelsAboveRoot = 0;

    protected ScrollArea fileListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea textFilePreviewScrollArea = new ScrollArea(0, 0, 0, 0);
    protected LocalTexture previewTexture;
    protected ExtendedButton saveButton;
    protected ExtendedButton cancelButton;
    protected ExtendedEditBox fileNameEditBox;
    protected ComponentWidget currentDirectoryComponent;

    @NotNull
    public static SaveFileScreen build(@NotNull File rootDirectory, @Nullable String fileNamePreset, @Nullable String forcedFileExtension, @NotNull Consumer<File> callback) {
        return new SaveFileScreen(rootDirectory, rootDirectory, fileNamePreset, forcedFileExtension, callback);
    }

    public SaveFileScreen(@Nullable File rootDirectory, @NotNull File startDirectory, @Nullable String fileNamePreset, @Nullable String forcedFileExtension, @NotNull Consumer<File> callback) {

        super(Component.translatable("fancymenu.ui.save_file"));

        this.rootDirectory = rootDirectory;

        this.forcedFileExtension = forcedFileExtension;
        if (this.forcedFileExtension != null) {
            if (this.forcedFileExtension.startsWith(".")) this.forcedFileExtension = this.forcedFileExtension.substring(1);
            this.fileFilter = file -> file.getName().toLowerCase().endsWith("." + this.forcedFileExtension.toLowerCase());
        }

        if (!this.isInRootOrSubOfRoot(startDirectory)) {
            startDirectory = this.rootDirectory;
        }
        if ((FileChooserScreen.lastDirectory != null) && this.isInRootOrSubOfRoot(FileChooserScreen.lastDirectory)) {
            startDirectory = FileChooserScreen.lastDirectory;
        }

        this.currentDir = startDirectory;
        FileChooserScreen.lastDirectory = startDirectory;
        this.callback = callback;

        this.updateTextPreview(null);
        this.updateFilesList();

        this.fileNameEditBox = new ExtendedEditBox(this.font, 0, 0, 150, 20, Component.translatable("fancymenu.ui.save_file.file_name")) {
            @Override
            public boolean canConsumeInput() {
                if (forcedFileExtension != null) {
                    if (this.getCursorPosition() >= Math.max(0, this.getValue().length() - forcedFileExtension.length())) {
                        return false;
                    }
                }
                return super.canConsumeInput();
            }
        };
        this.fileNameEditBox.setMaxLength(10000);

        String editBoxPresetValue = "new_file";
        if (fileNamePreset != null) {
            if ((this.forcedFileExtension != null) && (fileNamePreset.toLowerCase().endsWith("." + this.forcedFileExtension.toLowerCase()))) {
                fileNamePreset = fileNamePreset.substring(0, Math.max(1, fileNamePreset.length() - (this.forcedFileExtension.length() + 1)));
            }
            editBoxPresetValue = fileNamePreset;
        }
        if (this.forcedFileExtension != null) {
            editBoxPresetValue += "." + this.forcedFileExtension;
        }
        this.fileNameEditBox.setValue(editBoxPresetValue);

    }

    @Override
    protected void init() {

        this.addWidget(this.fileNameEditBox);

        this.saveButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.ui.save_file.save"), (button) -> {
            File f = this.getSaveFile();
            if (f != null) {
                if (!f.isFile()) {
                    this.callback.accept(new File(f.getPath().replace("\\", "/")));
                } else {
                    Minecraft.getInstance().setScreen(ConfirmationScreen.warning((call) -> {
                        Minecraft.getInstance().setScreen(this);
                        if (call) {
                            try {
                                this.callback.accept(new File(f.getPath().replace("\\", "/")));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }, LocalizationUtils.splitLocalizedLines("fancymenu.ui.save_file.save.override_warning")));
                }
            }
        }) {
            @Override
            public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
                SaveFileScrollAreaEntry e = SaveFileScreen.this.getSelectedEntry();
                this.active = canSave();
                super.render(pose, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.saveButton);
        UIBase.applyDefaultButtonSkinTo(this.saveButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

        this.updateCurrentDirectoryComponent();

    }

    @Override
    public void tick() {
        this.fileNameEditBox.tick();
    }

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

        this.fileListScrollArea.setWidth((this.width / 2) - 40, true);
        this.fileListScrollArea.setHeight(this.height - 85 - (this.font.lineHeight + 6) - 2 - 25, true);
        this.fileListScrollArea.setX(20, true);
        this.fileListScrollArea.setY(currentDirFieldYEnd + 2, true);
        this.fileListScrollArea.render(pose, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("fancymenu.ui.filechooser.preview");
        int previewLabelWidth = this.font.width(previewLabel);
        this.font.draw(pose, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.fileNameEditBox.setWidth(this.fileListScrollArea.getWidthWithBorder());
        this.fileNameEditBox.setX(20);
        this.fileNameEditBox.setY(this.fileListScrollArea.getYWithBorder() + this.fileListScrollArea.getHeightWithBorder() + 5);
        this.fileNameEditBox.render(pose, mouseX, mouseY, partial);

        this.saveButton.setX(this.width - 20 - this.saveButton.getWidth());
        this.saveButton.setY(this.height - 20 - 20);
        this.saveButton.render(pose, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.saveButton.getY() - 5 - 20);
        this.cancelButton.render(pose, mouseX, mouseY, partial);

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

        super.render(pose, mouseX, mouseY, partial);

    }

    protected int renderCurrentDirectoryField(PoseStack pose, int mouseX, int mouseY, float partial, int x, int y, int width, int height) {
        int xEnd = x + width;
        int yEnd = y + height;
        fill(pose, x + 1, y + 1, xEnd - 2, yEnd - 2, UIBase.getUIColorScheme().area_background_color.getColorInt());
        UIBase.renderBorder(pose, x, y, xEnd, yEnd, 1, UIBase.getUIColorScheme().element_border_color_normal.getColor(), true, true, true, true);
        this.currentDirectoryComponent.x = x + 4;
        this.currentDirectoryComponent.y = y + (height / 2) - (this.currentDirectoryComponent.getHeight() / 2);
        this.currentDirectoryComponent.render(pose, mouseX, mouseY, partial);
        return yEnd;
    }

    protected boolean canSave() {
        return (this.getSaveFile() != null);
    }

    @Nullable
    protected File getSaveFile() {
        File selected = currentDir;
        SaveFileScrollAreaEntry e = this.getSelectedEntry();
        if ((e != null) && e.file.isDirectory()) return null;
        if ((e != null) && e.file.isFile()) return e.file;
        if (!this.fileNameEditBox.getValue().replace(" ", "").isEmpty()) {
            File f = new File(selected, "/" + this.fileNameEditBox.getValue());
            if ((this.fileFilter != null) && !this.fileFilter.checkFile(f)) return null;
            return f;
        }
        return null;
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
    public FileChooserScreen.FileFilter getFileFilter() {
        return fileFilter;
    }

    public SaveFileScreen setFileFilter(@Nullable FileChooserScreen.FileFilter fileFilter) {
        if (this.forcedFileExtension == null) {
            this.fileFilter = fileFilter;
            this.updateFilesList();
        } else {
            LOGGER.error("[FANCYMENU] Can't set file filter for SaveFileScreen with forced file extension!");
        }
        return this;
    }

    @Nullable
    public CharacterFilter getCharacterFilter() {
        return this.fileNameEditBox.getCharacterFilter();
    }

    public SaveFileScreen setCharacterFilter(@Nullable CharacterFilter characterFilter) {
        this.fileNameEditBox.setCharacterFilter(characterFilter);
        return this;
    }

    public SaveFileScreen setDirectory(@NotNull File newDirectory, boolean playSound) {
        Objects.requireNonNull(newDirectory);
        if (!this.isInRootOrSubOfRoot(newDirectory)) return this;
        if (playSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.updateTextPreview(null);
        this.previewTexture = null;
        this.currentDir = newDirectory;
        FileChooserScreen.lastDirectory = newDirectory;
        this.updateFilesList();
        MainThreadTaskExecutor.executeInMainThread(this::updateCurrentDirectoryComponent, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        return this;
    }

    public int getVisibleDirectoryLevelsAboveRoot() {
        return this.visibleDirectoryLevelsAboveRoot;
    }

    public SaveFileScreen setVisibleDirectoryLevelsAboveRoot(int visibleDirectoryLevelsAboveRoot) {
        this.visibleDirectoryLevelsAboveRoot = visibleDirectoryLevelsAboveRoot;
        return this;
    }

    public SaveFileScreen setFileName(String fileName) {
        this.fileNameEditBox.setValue(fileName);
        return this;
    }

    @Nullable
    protected SaveFileScreen.SaveFileScrollAreaEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e instanceof SaveFileScrollAreaEntry f) {
                if (f.isSelected()) return f;
            }
        }
        return null;
    }

    protected void updateTextPreview(@Nullable File file) {
        this.textFilePreviewScrollArea.clearEntries();
        if ((file != null) && file.isFile() && FileChooserScreen.PLAIN_TEXT_FILE_FILTER.checkFile(file)) {
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
            for (File f : folders) {
                SaveFileScrollAreaEntry e = new SaveFileScrollAreaEntry(this.fileListScrollArea, f);
                this.fileListScrollArea.addEntry(e);
            }
            for (File f : files) {
                if ((this.fileFilter != null) && !this.fileFilter.checkFile(f)) continue;
                SaveFileScrollAreaEntry e = new SaveFileScrollAreaEntry(this.fileListScrollArea, f);
                this.fileListScrollArea.addEntry(e);
            }
        }
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

    @Override
    public boolean keyPressed(int button, int $$1, int $$2) {

        if (button == InputConstants.KEY_ENTER) {
            SaveFileScrollAreaEntry selected = this.getSelectedEntry();
            if (selected != null) {
                this.callback.accept(new File(selected.file.getPath().replace("\\", "/")));
                return true;
            }
        }

        return super.keyPressed(button, $$1, $$2);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if ((button == 0) && !this.fileListScrollArea.isMouseInsideArea() && !this.fileListScrollArea.isMouseInteractingWithGrabbers()) {
            for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
                e.setSelected(false);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);

    }

    public class SaveFileScrollAreaEntry extends ScrollAreaEntry {

        private static final int BORDER = 3;

        public File file;
        public Font font = Minecraft.getInstance().font;

        protected final MutableComponent fileNameComponent;
        protected long lastClick = -1;

        public SaveFileScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {

            super(parent, 100, 30);
            this.file = file;
            this.fileNameComponent = Component.literal(this.file.getName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().description_area_text_color.getColorInt()));

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
                    SaveFileScreen.this.callback.accept(new File(this.file.getPath().replace("\\", "/")));
                } else if (this.file.isDirectory()) {
                    SaveFileScreen.this.setDirectory(this.file, true);
                }
            }
            if (this.file.isFile()) {
                SaveFileScreen.this.updateTextPreview(this.file);
                if (FileChooserScreen.IMAGE_FILE_FILTER.checkFile(this.file)) {
                    SaveFileScreen.this.previewTexture = TextureHandler.INSTANCE.getTexture(this.file);
                } else {
                    SaveFileScreen.this.previewTexture = null;
                }
            } else {
                SaveFileScreen.this.updateTextPreview(null);
                SaveFileScreen.this.previewTexture = null;
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
                if (!SaveFileScreen.this.currentIsRootDirectory()) {
                    File parent = SaveFileScreen.this.getParentDirectoryOfCurrent();
                    if ((parent != null) && parent.isDirectory()) {
                        SaveFileScreen.this.setDirectory(parent, true);
                    }
                }
            }
            SaveFileScreen.this.updateTextPreview(null);
            SaveFileScreen.this.previewTexture = null;
            this.lastClick = now;
        }

    }

}
