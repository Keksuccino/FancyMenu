package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class SaveFileScreen extends AbstractFileBrowserScreen {

    protected static final Component FILE_NAME_PREFIX_TEXT = Component.translatable("fancymenu.file_browser.save_file.file_name");

    @Nullable
    protected String forcedFileExtension;
    protected String defaultFileName;
    protected boolean forceResourceFriendlyFileNames = true;
    protected ExtendedEditBox fileNameEditBox;

    @NotNull
    public static SaveFileScreen build(@NotNull File rootDirectory, @Nullable String fileNamePreset, @Nullable String forcedFileExtension, @NotNull Consumer<File> callback) {
        return new SaveFileScreen(rootDirectory, rootDirectory, fileNamePreset, forcedFileExtension, callback);
    }

    public SaveFileScreen(@Nullable File rootDirectory, @NotNull File startDirectory, @Nullable String fileNamePreset, @Nullable String forcedFileExtension, @NotNull Consumer<File> callback) {

        super(Component.translatable("fancymenu.ui.save_file"), rootDirectory, startDirectory, callback);

        this.forcedFileExtension = forcedFileExtension;
        if (this.forcedFileExtension != null) {
            if (this.forcedFileExtension.startsWith(".")) this.forcedFileExtension = this.forcedFileExtension.substring(1);
            this.fileFilter = file -> file.getName().toLowerCase().endsWith("." + this.forcedFileExtension.toLowerCase());
        }

        this.fileNameEditBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 150, 18, Component.translatable("fancymenu.ui.save_file.file_name"));
        if (this.forcedFileExtension != null) {
            this.fileNameEditBox.setInputSuffix("." + this.forcedFileExtension.toLowerCase());
            this.fileNameEditBox.applyInputPrefixSuffixCharacterRenderFormatter();
        }
        this.fileNameEditBox.setMaxLength(10000);
        UIBase.applyDefaultWidgetSkinTo(this.fileNameEditBox);

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
        this.fileNameEditBox.setCursorPosition(0);
        this.fileNameEditBox.setHighlightPos(0);
        this.fileNameEditBox.setDisplayPosition(0);
        this.defaultFileName = editBoxPresetValue;

        this.setForceResourceFriendlyFileNames(true);

        this.fileScrollListHeightOffset = -25;
        this.fileTypeScrollListYOffset = 25;

    }

    @Override
    protected void init() {

        this.addWidget(this.fileNameEditBox);

        super.init();

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if ((this.forcedFileExtension != null) && !this.fileNameEditBox.getValue().toLowerCase().endsWith("." + this.forcedFileExtension.toLowerCase())) {
            this.fileNameEditBox.setValue(this.defaultFileName);
        }

        super.render(graphics, mouseX, mouseY, partial);

        this.renderFileNameEditBox(graphics, mouseX, mouseY, partial);

    }

    protected void renderFileNameEditBox(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.fileNameEditBox.setWidth(this.getBelowFileScrollAreaElementWidth() - 2);
        this.fileNameEditBox.setX((int)(this.fileListScrollArea.getXWithBorder() + this.fileListScrollArea.getWidthWithBorder() - this.fileNameEditBox.getWidth() - 1));
        this.fileNameEditBox.setY((int)(this.fileListScrollArea.getYWithBorder() + this.fileListScrollArea.getHeightWithBorder() + 5 + 1));
        this.fileNameEditBox.render(graphics, mouseX, mouseY, partial);
        graphics.drawString(this.font, FILE_NAME_PREFIX_TEXT, this.fileNameEditBox.getX() - 1 - Minecraft.getInstance().font.width(FILE_NAME_PREFIX_TEXT) - 5, this.fileNameEditBox.getY() - 1 + (this.fileNameEditBox.getHeight() / 2) - (Minecraft.getInstance().font.lineHeight / 2), UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt(), false);
    }

    @Override
    protected int getBelowFileScrollAreaElementWidth() {
        int w = (int)(this.fileListScrollArea.getWidthWithBorder() - Minecraft.getInstance().font.width(FILE_NAME_PREFIX_TEXT) - 5);
        return Math.min(super.getBelowFileScrollAreaElementWidth(), w);
    }

    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.ui.save_file.save"), (button) -> {
            this.trySave();
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                AbstractFileScrollAreaEntry selected = getSelectedEntry();
                this.active = canSave();
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
    }

    @Override
    public AbstractFileBrowserScreen setFileFilter(@Nullable FileFilter fileFilter) {
        if (this.forcedFileExtension == null) {
            return super.setFileFilter(fileFilter);
        } else {
            LOGGER.error("[FANCYMENU] Can't set file filter for SaveFileScreen with forced file extension!");
        }
        return this;
    }

    @Override
    protected boolean isWidgetHovered() {
        if (this.fileNameEditBox.isHoveredOrFocused()) return false;
        return super.isWidgetHovered();
    }

    @Nullable
    public CharacterFilter getFileNameCharacterFilter() {
        return this.fileNameEditBox.getCharacterFilter();
    }

    public SaveFileScreen setFileNameCharacterFilter(@Nullable CharacterFilter characterFilter) {
        if (this.forceResourceFriendlyFileNames) {
            LOGGER.error("[FANCYMENU] Unable to set file name character filter for SaveFileScreen while 'forceResourceFriendlyFileNames' is enabled!");
            return this;
        }
        this.fileNameEditBox.setCharacterFilter(characterFilter);
        return this;
    }

    public SaveFileScreen setFileName(@NotNull String fileName) {
        this.fileNameEditBox.setValue(fileName);
        return this;
    }

    public boolean forceResourceFriendlyFileNames() {
        return this.forceResourceFriendlyFileNames;
    }

    public SaveFileScreen setForceResourceFriendlyFileNames(boolean forceResourceFriendlyFileNames) {
        this.forceResourceFriendlyFileNames = forceResourceFriendlyFileNames;
        if (!this.forceResourceFriendlyFileNames) {
            this.fileNameEditBox.setCharacterFilter(null);
        } else {
            this.fileNameEditBox.setCharacterFilter(CharacterFilter.buildOnlyLowercaseFileNameFilter());
        }
        return this;
    }

    protected void trySave() {
        File f = this.getSaveFile();
        if (f != null) {
            if (!f.isFile()) {
                this.callback.accept(new File(f.getPath().replace("\\", "/")));
            } else {
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.ui.save_file.save.override_warning"), MessageDialogStyle.WARNING, call -> {
                    if (call) {
                        try {
                            this.callback.accept(new File(f.getPath().replace("\\", "/")));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    protected boolean canSave() {
        return (this.getSaveFile() != null);
    }

    @Nullable
    protected File getSaveFile() {
        AbstractFileScrollAreaEntry e = this.getSelectedEntry();
        if ((e != null) && e.file.isDirectory()) return null;
        if (!this.fileNameEditBox.getValue().replace(" ", "").isEmpty()) {
            File f = new File(this.currentDir, "/" + this.fileNameEditBox.getValue());
            if (!this.shouldShowFile(f)) return null;
            return f;
        }
        return null;
    }

    @Override
    protected AbstractFileScrollAreaEntry buildFileEntry(@NotNull File f) {
        return new SaveFileScrollAreaEntry(this.fileListScrollArea, f);
    }

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {

        if ((keycode == InputConstants.KEY_ENTER) || (keycode == InputConstants.KEY_NUMPADENTER)) {
            ScrollAreaEntry selectedEntry = this.getSelectedScrollEntry();
            if (selectedEntry instanceof ParentDirScrollAreaEntry) {
                this.goUpDirectory();
                return true;
            }
            if (selectedEntry instanceof AbstractFileScrollAreaEntry fileEntry) {
                if (!fileEntry.resourceUnfriendlyFileName && fileEntry.file.isDirectory()) {
                    this.setDirectory(fileEntry.file, true);
                    return true;
                }
            }
            this.trySave();
            return true;
        }

        return super.keyPressed(keycode, scancode, modifiers);

    }

    public class SaveFileScrollAreaEntry extends AbstractFileScrollAreaEntry {

        public SaveFileScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {
            super(parent, file);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyFileName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (this.file.isDirectory()) {
                    SaveFileScreen.this.setDirectory(this.file, true);
                } else if (this.file.isFile()) {
                    String name = this.file.getName();
                    if ((SaveFileScreen.this.forcedFileExtension == null) || (name.toLowerCase().endsWith("." + SaveFileScreen.this.forcedFileExtension.toLowerCase()))) {
                        SaveFileScreen.this.fileNameEditBox.setValue(name);
                    }
                }
            }
            SaveFileScreen.this.updatePreview(this.file);
            this.lastClick = now;
        }

    }

}
