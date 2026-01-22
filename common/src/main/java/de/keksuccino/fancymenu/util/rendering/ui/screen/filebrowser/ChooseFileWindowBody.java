package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class ChooseFileWindowBody extends AbstractFileBrowserWindowBody {

    @Nullable
    private Consumer<File> previewApplyCallback;
    @Nullable
    private Runnable previewCancelCallback;

    @NotNull
    public static ChooseFileWindowBody build(@NotNull File rootDirectory, @NotNull Consumer<File> callback) {
        return new ChooseFileWindowBody(rootDirectory, rootDirectory, callback);
    }

    public ChooseFileWindowBody(@Nullable File rootDirectory, @NotNull File startDirectory, @NotNull Consumer<File> callback) {
        super(Component.translatable("fancymenu.ui.filechooser.choose.file"), rootDirectory, startDirectory, callback);
    }

    @Override
    protected @NotNull ExtendedButton buildConfirmButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.ok"), (button) -> {
            AbstractFileScrollAreaEntry selected = this.getSelectedEntry();
            if ((selected != null) && !selected.resourceUnfriendlyFileName) {
                this.callback.accept(new File(selected.file.getPath().replace("\\", "/")));
                this.closeWindow();
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                AbstractFileScrollAreaEntry e = ChooseFileWindowBody.this.getSelectedEntry();
                this.active = (e != null) && !e.resourceUnfriendlyFileName && (e.file.isFile());
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
    }

    @Override
    protected @Nullable ExtendedButton buildApplyButton() {
        return new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.apply"), (button) -> {
            Consumer<File> callback = this.previewApplyCallback;
            if (callback == null) return;
            File selected = this.getSelectedFile();
            if ((selected != null) && selected.isFile()) {
                callback.accept(selected);
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                AbstractFileScrollAreaEntry selected = ChooseFileWindowBody.this.getSelectedEntry();
                this.active = (ChooseFileWindowBody.this.previewApplyCallback != null)
                        && (selected != null)
                        && !selected.resourceUnfriendlyFileName
                        && selected.file.isFile();
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
    }

    @Override
    protected void onCancel() {
        if (this.previewCancelCallback != null) {
            this.previewCancelCallback.run();
        }
        super.onCancel();
    }

    @Override
    protected AbstractFileScrollAreaEntry buildFileEntry(@NotNull File f) {
        return new FileScrollAreaEntry(this.fileListScrollArea, f);
    }

    public ChooseFileWindowBody setPreviewApplyCallback(@Nullable Consumer<File> previewApplyCallback) {
        this.previewApplyCallback = previewApplyCallback;
        return this;
    }

    public ChooseFileWindowBody setPreviewCancelCallback(@Nullable Runnable previewCancelCallback) {
        this.previewCancelCallback = previewCancelCallback;
        return this;
    }

    public class FileScrollAreaEntry extends AbstractFileScrollAreaEntry {

        public FileScrollAreaEntry(@NotNull ScrollArea parent, @NotNull File file) {
            super(parent, file);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            if (this.resourceUnfriendlyFileName) return;
            long now = System.currentTimeMillis();
            if ((now - this.lastClick) < 400) {
                if (this.file.isFile()) {
                    ChooseFileWindowBody.this.callback.accept(new File(this.file.getPath().replace("\\", "/")));
                    ChooseFileWindowBody.this.closeWindow();
                } else if (this.file.isDirectory()) {
                    ChooseFileWindowBody.this.setDirectory(this.file, true);
                }
            }
            ChooseFileWindowBody.this.updatePreview(this.file);
            this.lastClick = now;
        }

    }

}
