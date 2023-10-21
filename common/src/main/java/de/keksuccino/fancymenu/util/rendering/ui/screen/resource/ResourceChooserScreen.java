package de.keksuccino.fancymenu.util.rendering.ui.screen.resource;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfiguratorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.resources.Resource;
import de.keksuccino.fancymenu.util.resources.ResourceSourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

public class ResourceChooserScreen<R extends Resource, F extends FileType<R>> extends ConfiguratorScreen {

    //TODO Somehow tell the user what source types are possible for all allowed file types
    // (maybe tell the user if some source type for some file type is NOT available?? inform only about NOT available source types; maybe under the control cells)

    @Nullable
    protected FileTypeGroup<F> allowedFileTypes;
    @Nullable
    protected FileFilter fileFilter;
    @NotNull
    protected Consumer<String> resourceSourceCallback;
    @Nullable
    protected String resourceSource;
    @NotNull
    protected ResourceSourceType resourceSourceType = ResourceSourceType.LOCATION;
    protected CycleButton<ResourceSourceType> resourceSourceTypeCycleButton;
    protected ExtendedEditBox editBox;

    protected ResourceChooserScreen(@NotNull Component title, @Nullable FileTypeGroup<F> allowedFileTypes, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        super(title);
        this.allowedFileTypes = allowedFileTypes;
        this.fileFilter = fileFilter;
        this.resourceSourceCallback = resourceSourceCallback;
    }


    @Override
    protected void initCells() {

        boolean isLocal = (this.resourceSourceType == ResourceSourceType.LOCAL);
        boolean isLegacyLocal = (isLocal && (this.resourceSource != null) && !this.resourceSource.startsWith("config/fancymenu/assets/") && !this.resourceSource.startsWith("/config/fancymenu/assets/"));

        //Fix local sources that don't start with "/"
        if (isLocal && (this.resourceSource != null) && !this.resourceSource.startsWith("/") && this.resourceSource.startsWith("config/fancymenu/assets/")) {
            this.resourceSource = this.resourceSource.substring("config/fancymenu/assets/".length());
            this.resourceSource = "/config/fancymenu/assets/" + this.resourceSource;
        }

        this.addStartEndSpacerCell();

        this.resourceSourceTypeCycleButton = new CycleButton<>(0, 0, 20, 20, ResourceSourceType.LOCATION.cycle(), (value, button) -> {
            this.resourceSourceType = value;
            //Reset the source when changing the source type, because it is not valid anymore
            this.resourceSource = null;
            this.init();
        });
        this.resourceSourceTypeCycleButton.setTooltipSupplier(consumes -> {
            if (this.resourceSourceType == ResourceSourceType.LOCATION) return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.resources.source_type.location.desc"));
            if (this.resourceSourceType == ResourceSourceType.LOCAL) return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.resources.source_type.local.desc"));
            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.resources.source_type.web.desc"));
        });
        this.resourceSourceTypeCycleButton.setSelectedValue(this.resourceSourceType);
        this.addWidgetCell(this.resourceSourceTypeCycleButton, true);

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.resources.chooser_screen.source"));

        this.editBox = this.addTextInputCell(null, !isLegacyLocal, !isLegacyLocal).editBox;
        this.editBox.setResponder(s -> this.resourceSource = s);
        if (isLocal && !isLegacyLocal) this.editBox.setInputPrefix("/config/fancymenu/assets/");
        if (this.resourceSource != null) this.editBox.setValue(this.resourceSource);
        this.editBox.setEditable(!isLegacyLocal);

        if (isLocal) {
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.resources.chooser_screen.choose_local"), var1 -> {
                File startDir = LayoutHandler.ASSETS_DIR;
                String path = this.resourceSource;
                if (path != null) {
                    startDir = new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(path)).getParentFile();
                    if (startDir == null) startDir = LayoutHandler.ASSETS_DIR;
                }
                ChooseFileScreen fileChooser = new ChooseFileScreen(LayoutHandler.ASSETS_DIR, startDir, call -> {
                    if (call != null) {
                        this.resourceSource = GameDirectoryUtils.getPathWithoutGameDirectory(call.getAbsolutePath());
                        if (!this.resourceSource.startsWith("/")) this.resourceSource = "/" + this.resourceSource;
                    }
                    this.resourceSourceType = ResourceSourceType.LOCAL;
                    Minecraft.getInstance().setScreen(this);
                });
                fileChooser.setVisibleDirectoryLevelsAboveRoot(2);
                fileChooser.setFileTypes(this.allowedFileTypes);
                fileChooser.setFileFilter(this.fileFilter);
                Minecraft.getInstance().setScreen(fileChooser);
            }), true);
        }

        if (isLegacyLocal) {
            this.addSeparatorCell(3);
            this.addLabelCell(Component.translatable("fancymenu.resources.chooser_screen.legacy_local.warning.line1").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())));
            this.addLabelCell(Component.translatable("fancymenu.resources.chooser_screen.legacy_local.warning.line2").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())));
            this.addLabelCell(Component.translatable("fancymenu.resources.chooser_screen.legacy_local.warning.line3").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())));
        }

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.resources.chooser_screen.allowed_file_types"));

        MutableComponent typesComponent = Component.translatable("fancymenu.file_browser.file_type.types.all").append(" (*)");
        if (this.allowedFileTypes != null) {
            String types = "";
            for (FileType<?> type : this.allowedFileTypes.getFileTypes()) {
                for (String s : type.getExtensions()) {
                    if (!types.isEmpty()) types += ";";
                    types += "*." + s.toUpperCase();
                }
            }
            Component fileTypeDisplayName = this.allowedFileTypes.getDisplayName();
            if (fileTypeDisplayName == null) fileTypeDisplayName = Component.empty();
            typesComponent = Component.empty().append(fileTypeDisplayName).append(Component.literal(" (")).append(Component.literal(types)).append(Component.literal(")"));
        }
        this.addLabelCell(typesComponent.setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())));

        this.addStartEndSpacerCell();

    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

        super.render(pose, mouseX, mouseY, partial);

        if ((this.resourceSourceType == ResourceSourceType.LOCATION) && (this.editBox != null) && this.editBox.isHovered()) {
            TooltipHandler.INSTANCE.addTooltip(
                    Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.resources.source_type.location.desc.input")),
                    () -> this.editBox.isHovered(), false, true);
        }

        //TODO render preview on the right, if source allows it

    }

    public ResourceChooserScreen<R,F> setSource(@NotNull String resourceSource) {
        this.resourceSourceType = ResourceSourceType.getSourceTypeOf(Objects.requireNonNull(resourceSource));
        //Remove the prefix for easier handling inside the chooser screen (source type is saved as variable)
        this.resourceSource = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
        this.init();
        return this;
    }

    @Override
    protected void onCancel() {
        this.resourceSourceCallback.accept(null);
    }

    @Override
    protected void onDone() {
        if (this.resourceSource == null) {
            this.resourceSourceCallback.accept(null);
        } else {
            //Return the resource source with prefix
            this.resourceSourceCallback.accept(this.resourceSourceType.getSourcePrefix() + this.resourceSource);
        }
    }

}
