package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Consumer;

public class ChooseDirectoryWindowBody extends AbstractFileBrowserWindowBody {

    @NotNull
    public static ChooseDirectoryWindowBody build(@NotNull File rootDirectory, @NotNull Consumer<File> callback) {
        return new ChooseDirectoryWindowBody(rootDirectory, rootDirectory, callback);
    }

    public ChooseDirectoryWindowBody(@Nullable File rootDirectory, @NotNull File startDirectory, @NotNull Consumer<File> callback) {
        super(Component.translatable("fancymenu.ui.filechooser.choose.directory"), rootDirectory, startDirectory, callback);
        this.setWindowAlwaysOnTop(false);
        this.setWindowBlocksMinecraftScreenInputs(false);
        this.setWindowForceFocus(false);
    }

    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.ui.filechooser.choose.directory.confirm"), button -> {
            File selectedDirectory = this.getSelectedDirectory();
            if (selectedDirectory != null) {
                this.callback.accept(new File(selectedDirectory.getPath().replace("\\", "/")));
                this.closeWindow();
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                this.active = ChooseDirectoryWindowBody.this.getSelectedDirectory() != null;
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
    }

    @Override
    protected @Nullable ExtendedButton buildApplyButton() {
        return null;
    }

    @Override
    protected AbstractFileScrollAreaEntry buildFileEntry(@NotNull File file) {
        return new DirectoryScrollAreaEntry(this.fileListScrollArea, file);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Nullable
    protected File getSelectedDirectory() {
        AbstractFileScrollAreaEntry selected = this.getSelectedEntry();
        if ((selected != null) && selected.file.isDirectory() && !selected.resourceUnfriendlyFileName) {
            return selected.file;
        }
        if ((this.currentDir != null) && this.currentDir.isDirectory()) {
            return this.currentDir;
        }
        return null;
    }

    protected class DirectoryScrollAreaEntry extends AbstractFileScrollAreaEntry {

        public DirectoryScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {
            super(parent, file);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyFileName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (this.file.isDirectory()) {
                    ChooseDirectoryWindowBody.this.setDirectory(this.file, true);
                }
            }
            ChooseDirectoryWindowBody.this.updatePreview(this.file);
            this.lastClick = now;
        }

    }

}
