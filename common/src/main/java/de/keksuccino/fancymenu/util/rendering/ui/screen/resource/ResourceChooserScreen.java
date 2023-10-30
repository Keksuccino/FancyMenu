package de.keksuccino.fancymenu.util.rendering.ui.screen.resource;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroups;
import de.keksuccino.fancymenu.util.file.type.types.AudioFileType;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.TextFileType;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
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
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.text.IText;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.video.IVideo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ResourceChooserScreen<R extends Resource, F extends FileType<R>> extends ConfiguratorScreen {

    //TODO hier weiter screen fixen !!!
    // - vor allem local ist komplett broken
    // - nach init() kann man input field nicht klicken/editieren

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
    protected boolean legacySource = false;
    @NotNull
    protected ResourceSourceType resourceSourceType = ResourceSourceType.LOCATION;
    protected boolean allowLocation = true;
    protected boolean allowLocal = true;
    protected boolean allowWeb = true;
    protected CycleButton<ResourceSourceType> resourceSourceTypeCycleButton;
    protected ExtendedEditBox editBox;
    protected LabelCell warningNoExtensionLine1;
    protected LabelCell warningNoExtensionLine2;
    protected LabelCell warningNoExtensionLine3;

    @NotNull
    public static ResourceChooserScreen<ITexture, ImageFileType> image(@NotNull Component title, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserScreen<>(title, FileTypeGroups.IMAGE_TYPES, fileFilter, resourceSourceCallback);
    }

    @NotNull
    public static ResourceChooserScreen<ITexture, ImageFileType> image(@Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return image(Component.translatable("fancymenu.resources.chooser_screen.choose.image"), fileFilter, resourceSourceCallback);
    }

    @NotNull
    public static ResourceChooserScreen<IAudio, AudioFileType> audio(@NotNull Component title, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserScreen<>(title, FileTypeGroups.AUDIO_TYPES, fileFilter, resourceSourceCallback);
    }

    @NotNull
    public static ResourceChooserScreen<IAudio, AudioFileType> audio(@Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return audio(Component.translatable("fancymenu.resources.chooser_screen.choose.audio"), fileFilter, resourceSourceCallback);
    }

    @NotNull
    public static ResourceChooserScreen<IVideo, VideoFileType> video(@NotNull Component title, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserScreen<>(title, FileTypeGroups.VIDEO_TYPES, fileFilter, resourceSourceCallback);
    }

    @NotNull
    public static ResourceChooserScreen<IVideo, VideoFileType> video(@Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return video(Component.translatable("fancymenu.resources.chooser_screen.choose.video"), fileFilter, resourceSourceCallback);
    }

    @NotNull
    public static ResourceChooserScreen<IText, TextFileType> text(@NotNull Component title, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return new ResourceChooserScreen<>(title, FileTypeGroups.TEXT_TYPES, fileFilter, resourceSourceCallback);
    }

    @NotNull
    public static ResourceChooserScreen<IText, TextFileType> text(@Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        return text(Component.translatable("fancymenu.resources.chooser_screen.choose.text"), fileFilter, resourceSourceCallback);
    }

    public ResourceChooserScreen(@NotNull Component title, @Nullable FileTypeGroup<F> allowedFileTypes, @Nullable FileFilter fileFilter, @NotNull Consumer<String> resourceSourceCallback) {
        super(title);
        this.allowedFileTypes = allowedFileTypes;
        this.fileFilter = fileFilter;
        this.resourceSourceCallback = resourceSourceCallback;
    }


    @Override
    protected void initCells() {

        boolean isLocal = (this.resourceSourceType == ResourceSourceType.LOCAL);
        boolean isLegacyLocal = (isLocal && (this.resourceSource != null) && !(this.resourceSource.startsWith("config/fancymenu/assets/") || this.resourceSource.startsWith("/config/fancymenu/assets/")));

        //Fix local sources that don't start with "/"
        if (isLocal && (this.resourceSource != null) && !this.resourceSource.startsWith("/") && this.resourceSource.startsWith("config/fancymenu/assets/")) {
            this.resourceSource = "/" + this.resourceSource;
        }

        this.addStartEndSpacerCell();

        LocalizedGenericValueCycle<ResourceSourceType> sourceTypeCycle = ResourceSourceType.LOCATION.cycle();
        if (!this.allowLocation) sourceTypeCycle.removeValue(ResourceSourceType.LOCATION);
        if (!this.allowLocal) sourceTypeCycle.removeValue(ResourceSourceType.LOCAL);
        if (!this.allowWeb) sourceTypeCycle.removeValue(ResourceSourceType.WEB);
        this.resourceSourceTypeCycleButton = new CycleButton<>(0, 0, 20, 20, sourceTypeCycle, (value, button) -> {
            //Reset the source when changing the source type, because it is not valid anymore
            this.resourceSource = null;
            this.resourceSourceType = value;
            this.legacySource = false;
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
        this.editBox.setResponder(s -> {
            this.resourceSource = s;
            this.updateNoExtensionWarning();
        });
        if (isLocal && !isLegacyLocal) this.editBox.setInputPrefix("/config/fancymenu/assets/");
        if (this.resourceSource != null) this.editBox.setValue(this.resourceSource);
        this.editBox.setCursorPosition(0);
        this.editBox.setDisplayPosition(0);
        this.editBox.setHighlightPos(0);
        this.editBox.applyInputPrefixSuffixCharacterRenderFormatter();
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
                        String s = GameDirectoryUtils.getPathWithoutGameDirectory(call.getAbsolutePath());
                        if (!s.startsWith("/")) s = "/" + s;
                        s = ResourceSourceType.LOCAL.getSourcePrefix() + s;
                        this.setSource(s, false);
                    }
                    this.init();
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

        this.addCellGroupEndSpacerCell();

        this.warningNoExtensionLine1 = this.addLabelCell(Component.empty());
        this.warningNoExtensionLine2 = this.addLabelCell(Component.empty());
        this.warningNoExtensionLine3 = this.addLabelCell(Component.empty());
        this.updateNoExtensionWarning();

        this.addStartEndSpacerCell();

    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

        super.render(pose, mouseX, mouseY, partial);

        if ((this.resourceSourceType == ResourceSourceType.LOCATION) && (this.editBox != null) && this.editBox.isHovered()) {
            TooltipHandler.INSTANCE.addTooltip(
                    Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.resources.source_type.location.desc.input")).setDefaultStyle(),
                    () -> this.editBox.isHovered(), false, true);
        }

        //TODO render preview on the right, if source allows it

    }

    protected void updateNoExtensionWarning() {
        if ((this.warningNoExtensionLine1 == null) || (this.warningNoExtensionLine2 == null) || (this.warningNoExtensionLine3 == null)) return;
        if ((this.resourceSource != null) && !this.resourceSource.replace(" ", "").isEmpty()) {
            if ((this.resourceSourceType == ResourceSourceType.LOCATION) || (this.resourceSourceType == ResourceSourceType.WEB)) {
                boolean extensionFound = false;
                if (this.allowedFileTypes != null) {
                    for (FileType<?> fileType : this.allowedFileTypes.getFileTypes()) {
                        for (String extension : fileType.getExtensions()) {
                            if (this.resourceSource.toLowerCase().endsWith("." + extension)) {
                                extensionFound = true;
                                break;
                            }
                        }
                        if (extensionFound) break;
                    }
                } else {
                    extensionFound = true;
                }
                if (!extensionFound) {
                    this.warningNoExtensionLine1.setText(Component.translatable("fancymenu.resources.chooser_screen.no_extension.warning.line1"));
                    this.warningNoExtensionLine2.setText(Component.translatable("fancymenu.resources.chooser_screen.no_extension.warning.line2"));
                    this.warningNoExtensionLine3.setText(Component.translatable("fancymenu.resources.chooser_screen.no_extension.warning.line3"));
                    return;
                }
            }
        }
        this.warningNoExtensionLine1.setText(Component.empty());
        this.warningNoExtensionLine2.setText(Component.empty());
        this.warningNoExtensionLine3.setText(Component.empty());
    }

    public ResourceChooserScreen<R,F> setSource(@Nullable String resourceSource, boolean updateScreen) {
        if (resourceSource == null) {
            this.resourceSource = null;
            this.resourceSourceType = ResourceSourceType.LOCATION;
            this.legacySource = false;
        } else {
            resourceSource = resourceSource.trim();
            this.legacySource = !ResourceSourceType.hasSourcePrefix(resourceSource);
            this.resourceSourceType = ResourceSourceType.getSourceTypeOf(PlaceholderParser.replacePlaceholders(resourceSource, false));
            //Remove the prefix for easier handling inside the chooser screen (source type is saved as variable)
            this.resourceSource = ResourceSourceType.getWithoutSourcePrefix(resourceSource);
        }
        if (updateScreen) this.init();
        return this;
    }

    public ResourceChooserScreen<R,F> setAllowedFileTypes(@Nullable FileTypeGroup<F> allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
        this.init();
        return this;
    }

    public ResourceChooserScreen<R,F> setFileFilter(@Nullable FileFilter fileFilter) {
        this.fileFilter = fileFilter;
        this.init();
        return this;
    }

    public ResourceChooserScreen<R,F> setResourceSourceCallback(@NotNull Consumer<String> resourceSourceCallback) {
        this.resourceSourceCallback = Objects.requireNonNull(resourceSourceCallback);
        return this;
    }

    public boolean isLocationSourceAllowed() {
        return this.allowLocation;
    }

    public ResourceChooserScreen<R,F> setLocationSourceAllowed(boolean allowLocation) {
        this.allowLocation = allowLocation;
        this.init();
        return this;
    }

    public boolean isLocalSourceAllowed() {
        return this.allowLocal;
    }

    public ResourceChooserScreen<R,F> setLocalSourceAllowed(boolean allowLocal) {
        this.allowLocal = allowLocal;
        this.init();
        return this;
    }

    public boolean isWebSourceAllowed() {
        return this.allowWeb;
    }

    public ResourceChooserScreen<R,F> setWebSourceAllowed(boolean allowWeb) {
        this.allowWeb = allowWeb;
        this.init();
        return this;
    }

    @Override
    public boolean allowDone() {
        if ((this.resourceSource == null)  || this.resourceSource.replace(" ", "").isEmpty()) return false;
        if ((this.resourceSourceType == ResourceSourceType.LOCAL) && this.resourceSource.equals("/config/fancymenu/assets/")) return false;
        return true;
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
