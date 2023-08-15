package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedEditBox;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class SaveFileScreen extends AbstractFileBrowserScreen {

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

        this.fileNameEditBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 150, 20, Component.translatable("fancymenu.ui.save_file.file_name")) {
            @Override
            public boolean charTyped(char character, int modifiers) {
                if (forcedFileExtension != null) {
                    if (this.getCursorPosition() >= Math.max(0, this.getValue().length() - forcedFileExtension.length())) {
                        return false;
                    }
                }
                return super.charTyped(character, modifiers);
            }
            @Override
            public boolean keyPressed(int keycode, int scancode, int $$2) {
                if (isSelectAll(keycode)) return false;
                if (forcedFileExtension != null) {
                    if (Math.max(0, this.getCursorPosition()) >= Math.max(0, this.getValue().length() - forcedFileExtension.length())) {
                        if ((keycode != InputConstants.KEY_LEFT) && (keycode != InputConstants.KEY_RIGHT) && (keycode != InputConstants.KEY_UP) && (keycode != InputConstants.KEY_DOWN)) {
                            return false;
                        }
                    }
                }
                return super.keyPressed(keycode, scancode, $$2);
            }
        }.setCharacterRenderFormatter((editBox, component, characterIndex, character, visiblePartOfLine, fullLine) -> {
            if (characterIndex >= Math.max(0, (editBox.getValue().length() - forcedFileExtension.length())-1)) {
                component.withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().edit_box_text_color_uneditable.getColorInt()));
            }
            return component;
        });
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
        this.defaultFileName = editBoxPresetValue;

        this.setForceResourceFriendlyFileNames(true);

    }

    @Override
    protected void init() {

        this.addWidget(this.fileNameEditBox);

        super.init();

    }

    @Override
    public void tick() {
        this.fileNameEditBox.tick();
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if ((this.forcedFileExtension != null) && !this.fileNameEditBox.getValue().toLowerCase().endsWith("." + this.forcedFileExtension.toLowerCase())) {
            this.fileNameEditBox.setValue(this.defaultFileName);
        }

        super.render(pose, mouseX, mouseY, partial);

        this.fileNameEditBox.setWidth(this.fileListScrollArea.getWidthWithBorder() - 2);
        this.fileNameEditBox.setX(21);
        this.fileNameEditBox.setY(this.fileListScrollArea.getYWithBorder() + this.fileListScrollArea.getHeightWithBorder() + 5);
        this.fileNameEditBox.render(pose, mouseX, mouseY, partial);

    }

    @Override
    protected void renderFileScrollArea(PoseStack pose, int mouseX, int mouseY, float partial, int currentDirFieldYEnd) {
        this.fileListScrollArea.setWidth((this.width / 2) - 40, true);
        this.fileListScrollArea.setHeight(this.height - 85 - (this.font.lineHeight + 6) - 2 - 25, true);
        this.fileListScrollArea.setX(20, true);
        this.fileListScrollArea.setY(currentDirFieldYEnd + 2, true);
        this.fileListScrollArea.render(pose, mouseX, mouseY, partial);
    }

    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.ui.save_file.save"), (button) -> {
            this.trySave();
        }) {
            @Override
            public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
                AbstractFileScrollAreaEntry selected = getSelectedEntry();
                this.active = canSave();
                super.render(pose, mouseX, mouseY, partial);
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
            this.fileNameEditBox.setCharacterFilter(CharacterFilter.buildBasicFilenameCharacterFilter());
        }
        return this;
    }

    protected void trySave() {
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
            if ((this.fileFilter != null) && !this.fileFilter.checkFile(f)) return null;
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

        String key = InputUtils.getKeyName(keycode, scancode);

        if (key.equals("a") && hasControlDown() && this.fileNameEditBox.isFocused()) {
            this.fileNameEditBox.setHighlightPos(0);
            int cursorPos = this.fileNameEditBox.getValue().length();
            if (this.forcedFileExtension != null) {
                cursorPos = Math.max(0, this.fileNameEditBox.getValue().length() - (this.forcedFileExtension.length() + 1));
            }
            this.fileNameEditBox.setCursorPosition(cursorPos);
            if (this.forcedFileExtension != null)
                return true;
        }

        if (keycode == InputConstants.KEY_ENTER) {
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
        public void onClick(ScrollAreaEntry entry) {
            if (this.resourceUnfriendlyFileName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (this.file.isDirectory()) {
                    SaveFileScreen.this.setDirectory(this.file, true);
                }
            }
            if (this.file.isFile()) {
                SaveFileScreen.this.fileNameEditBox.setValue(this.file.getName());
                SaveFileScreen.this.updateTextPreview(this.file);
                if (FileFilter.IMAGE_FILE_FILTER.checkFile(this.file)) {
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

}
