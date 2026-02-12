package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.UIIconButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import de.keksuccino.fancymenu.util.watermedia.WatermediaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class AbstractBrowserWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    protected static final int ICON_PIXEL_SIZE = 32;
    public static final int PIP_WINDOW_WIDTH = 600;
    public static final int PIP_WINDOW_HEIGHT = 446;

    // All icon renders are 32x32 pixels
    protected static final MaterialIcon GO_UP_ICON = MaterialIcons.ARROW_UPWARD;
    protected static final MaterialIcon GENERIC_FILE_ICON = MaterialIcons.DRAFTS;
    protected static final MaterialIcon TEXT_FILE_ICON = MaterialIcons.ARTICLE;
    protected static final MaterialIcon AUDIO_FILE_ICON = MaterialIcons.MUSIC_NOTE;
    protected static final MaterialIcon VIDEO_FILE_ICON = MaterialIcons.MOVIE;
    protected static final MaterialIcon IMAGE_FILE_ICON = MaterialIcons.IMAGE;
    protected static final MaterialIcon FOLDER_ICON = MaterialIcons.FOLDER;

    protected static final MaterialIcon AUDIO_PREVIEW_PLAY_ICON = MaterialIcons.PLAY_ARROW;
    protected static final MaterialIcon AUDIO_PREVIEW_PAUSE_ICON = MaterialIcons.PAUSE;

    protected static final int AUDIO_PREVIEW_BUTTON_SIZE = 26;
    protected static final int AUDIO_PREVIEW_BUTTON_SPACING = 6;
    protected static final int AUDIO_PREVIEW_SLIDER_HEIGHT = 20;
    protected static final int AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT = 6;
    protected static final int AUDIO_PREVIEW_PROGRESS_BAR_SPACING = 4;
    protected static final int AUDIO_PREVIEW_TIME_SPACING = 2;
    protected static final DrawableColor VIDEO_PREVIEW_WATERMEDIA_WARNING_BACKGROUND_COLOR = DrawableColor.of(180, 0, 0);
    protected static final String WATERMEDIA_V3_DOWNLOAD_URL_FANCYMENU = "https://www.curseforge.com/minecraft/mc-mods/watermedia/files/all?page=1&pageSize=20&showAlphaFiles=show";
    protected static final String WATERMEDIA_BINARIES_DOWNLOAD_URL_FANCYMENU = "https://www.curseforge.com/minecraft/mc-mods/watermedia-binaries/files/all?page=1&pageSize=20&showAlphaFiles=show";

    protected static final long PREVIEW_DELAY_MS = 1000L;
    protected static final Component FILE_TYPE_PREFIX_TEXT = Component.translatable("fancymenu.file_browser.file_type");

    protected ScrollArea fileListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea fileTypeScrollArea = new ScrollArea(0, 0, 0, 20);
    protected ScrollArea previewTextScrollArea = new ScrollArea(0, 0, 0, 0);
    protected boolean searchBarEnabled = true;
    @Nullable
    protected ExtendedEditBox searchBar;
    @NotNull
    protected Component searchBarPlaceholder = Component.translatable("fancymenu.ui.generic.search");
    protected boolean enterKeyForDoneEnabled = true;
    @Nullable
    protected ResourceSupplier<ITexture> previewTextureSupplier;
    @Nullable
    protected ResourceSupplier<IText> previewTextSupplier;
    @Nullable
    protected ResourceSupplier<IAudio> previewAudioSupplier;
    @Nullable
    protected ResourceSupplier<IVideo> previewVideoSupplier;
    @Nullable
    protected IText currentPreviewText;
    @Nullable
    protected IAudio currentPreviewAudio;
    @Nullable
    protected IVideo currentPreviewVideo;
    @Nullable
    protected Object pendingPreviewKey;
    @Nullable
    protected Object activePreviewKey;
    protected long pendingPreviewLoadAtMs = 0L;
    protected boolean previewPending = false;
    protected boolean previewAudioPlaying = false;
    protected long previewAudioSeed = 0L;
    protected boolean previewVideoPlaying = false;
    protected int audioPreviewProgressBarX = 0;
    protected int audioPreviewProgressBarY = 0;
    protected int audioPreviewProgressBarWidth = 0;
    protected int audioPreviewProgressBarHeight = 0;
    protected int videoPreviewProgressBarX = 0;
    protected int videoPreviewProgressBarY = 0;
    protected int videoPreviewProgressBarWidth = 0;
    protected int videoPreviewProgressBarHeight = 0;
    protected boolean audioPreviewProgressDragging = false;
    protected boolean videoPreviewProgressDragging = false;
    protected float watermediaDownloadX_FancyMenu = Float.NaN;
    protected float watermediaDownloadY_FancyMenu = Float.NaN;
    protected float watermediaDownloadWidth_FancyMenu = Float.NaN;
    protected float watermediaDownloadHeight_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadX_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadY_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadWidth_FancyMenu = Float.NaN;
    protected float watermediaBinariesDownloadHeight_FancyMenu = Float.NaN;
    protected ExtendedButton confirmButton;
    @Nullable
    protected ExtendedButton applyButton;
    protected boolean applyButtonEnabled = false;
    protected ExtendedButton cancelButton;
    protected UIIconButton audioPreviewToggleButton;
    protected RangeSlider audioPreviewVolumeSlider;
    protected UIIconButton videoPreviewToggleButton;
    protected RangeSlider videoPreviewVolumeSlider;
    protected ComponentWidget currentDirectoryComponent;
    protected int fileScrollListHeightOffset = 0;
    protected int fileTypeScrollListYOffset = 0;
    @Nullable
    protected MutableComponent currentFileTypesComponent;
    protected boolean windowAlwaysOnTop = true;
    protected boolean windowBlocksMinecraftScreenInputs = true;
    protected boolean windowForceFocus = true;

    protected AbstractBrowserWindowBody(@NotNull Component title) {
        super(title);
    }

    public @NotNull PiPWindow openInWindow(@Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(this.getTitle())
                .setScreen(this)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(this.windowAlwaysOnTop)
                .setBlockMinecraftScreenInputs(this.windowBlocksMinecraftScreenInputs)
                .setForceFocus(this.windowForceFocus)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    @Override
    protected void init() {

        boolean blur = UIBase.shouldBlur();
        this.fileListScrollArea.setSetupForBlurInterface(blur);
        this.fileTypeScrollArea.setSetupForBlurInterface(blur);
        this.previewTextScrollArea.setSetupForBlurInterface(blur);

        if (this.searchBar != null) {
            this.removeWidget(this.searchBar);
        }
        if (this.searchBarEnabled) {
            String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
            this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 0, 20 - 2, Component.empty());
            this.searchBar.setHintFancyMenu(consumes -> AbstractBrowserWindowBody.this.searchBarPlaceholder);
            this.searchBar.setValue(oldSearchValue);
            this.searchBar.setResponder(s -> AbstractBrowserWindowBody.this.updateEntryList());
            UIBase.applyDefaultWidgetSkinTo(this.searchBar, UIBase.shouldBlur());
            this.searchBar.setMaxLength(100000);
            this.addWidget(this.searchBar);
            this.setupInitialFocusWidget(this, this.searchBar);
        }

        this.confirmButton = this.buildConfirmButton();
        if (this.confirmButton != null) {
            Button.OnPress originalConfirmAction = this.confirmButton.getPressAction();
            this.confirmButton.setPressAction(button -> {
                this.stopPreviewMedia();
                if (originalConfirmAction != null) {
                    originalConfirmAction.onPress(button);
                }
            });
        }
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultWidgetSkinTo(this.confirmButton, UIBase.shouldBlur());

        if (this.applyButtonEnabled) {
            this.applyButton = this.buildApplyButton();
            if (this.applyButton != null) {
                this.addWidget(this.applyButton);
                UIBase.applyDefaultWidgetSkinTo(this.applyButton, UIBase.shouldBlur());
            }
        }

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.stopPreviewMedia();
            this.onCancel();
            this.closeWindow();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

        this.initAudioPreviewButton();
        this.initAudioPreviewVolumeSlider();
        this.initVideoPreviewButton();
        this.initVideoPreviewVolumeSlider();

        this.updateCurrentDirectoryComponent();

        this.updateFileTypeScrollArea();

        this.initExtraButtons();

        this.addWidget(this.fileListScrollArea);
        this.addWidget(this.fileTypeScrollArea);
        this.addWidget(this.previewTextScrollArea);

    }

    @NotNull
    protected abstract ExtendedButton buildConfirmButton();

    @Nullable
    protected ExtendedButton buildApplyButton() {
        return null;
    }

    protected abstract void updateEntryList();

    protected abstract void updateFileTypeScrollArea();

    protected abstract void updateCurrentDirectoryComponent();

    @NotNull
    protected abstract Component getEntriesLabel();

    protected abstract void onCancel();

    protected abstract boolean goUpDirectory();

    protected abstract boolean isGoUpEntry(@NotNull ScrollAreaEntry entry);

    protected abstract boolean openDirectoryEntry(@NotNull ScrollAreaEntry entry);

    @Nullable
    protected abstract Object getPreviewKeyForEntry(@NotNull ScrollAreaEntry entry);

    protected abstract void loadPreviewForKey(@NotNull Object previewKey);

    protected void initExtraButtons() {
    }

    protected void renderExtraButtons(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public void onScreenClosed() {
        this.stopPreviewMedia();
    }

    @Override
    public void onWindowClosedExternally() {
        this.stopPreviewMedia();
        this.onCancel();
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();

        if (this.currentFileTypesComponent != null) {
            float textWidth = UIBase.getUITextWidthNormal(this.currentFileTypesComponent);
            this.fileTypeScrollArea.horizontalScrollBar.active = (textWidth > (this.fileTypeScrollArea.getInnerWidth() - 10));
        }

        RenderSystem.enableBlend();

        float labelY = this.getMainAreaLabelY();
        float contentTopY = this.getMainAreaTopY();
        UIBase.renderText(graphics, this.getEntriesLabel(), 20.0F, labelY, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt());

        int leftAreaWidth = this.width - 260 - 20;
        int currentDirFieldY = (int) contentTopY;
        if (this.searchBarEnabled) {
            this.renderSearchBar(graphics, mouseX, mouseY, partial, 20, currentDirFieldY, leftAreaWidth, 20);
            currentDirFieldY += 25;
        }
        int currentDirFieldYEnd = this.renderCurrentDirectoryField(graphics, mouseX, mouseY, partial, 20, currentDirFieldY, leftAreaWidth, this.font.lineHeight + 6);

        this.renderFileScrollArea(graphics, mouseX, mouseY, partial, currentDirFieldYEnd);

        this.renderFileTypeScrollArea(graphics, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("fancymenu.ui.filechooser.preview");
        float previewLabelWidth = UIBase.getUITextWidthNormal(previewLabel);
        float previewLabelX = this.width - 20 - previewLabelWidth;
        UIBase.renderText(graphics, previewLabel, previewLabelX, labelY, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt());

        this.renderConfirmButton(graphics, mouseX, mouseY, partial);

        this.renderApplyButton(graphics, mouseX, mouseY, partial);

        this.renderCancelButton(graphics, mouseX, mouseY, partial);

        this.renderExtraButtons(graphics, mouseX, mouseY, partial);

        this.renderPreview(graphics, mouseX, mouseY, partial);

    }

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (keycode == InputConstants.KEY_TAB) {
            return true;
        }
        if ((keycode == InputConstants.KEY_ENTER) || (keycode == InputConstants.KEY_NUMPADENTER)) {
            return this.handleEnterKey();
        }
        if ((keycode == InputConstants.KEY_UP) || (keycode == InputConstants.KEY_DOWN)) {
            return this.handleVerticalNavigation(keycode == InputConstants.KEY_DOWN);
        }
        if ((keycode == InputConstants.KEY_LEFT) || (keycode == InputConstants.KEY_RIGHT)) {
            return this.forwardKeyToFocusedWidget(keycode, scancode, modifiers);
        }
        return super.keyPressed(keycode, scancode, modifiers);
    }

    protected void renderConfirmButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.confirmButton.setX(this.width - 20 - this.confirmButton.getWidth());
        this.confirmButton.setY(this.height - 20 - 20);
        this.confirmButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderApplyButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.applyButton == null) return;
        this.applyButton.setX(this.width - 20 - this.applyButton.getWidth());
        this.applyButton.setY(this.confirmButton.getY() - 5 - 20);
        this.applyButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderCancelButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        int anchorY = (this.applyButton != null) ? this.applyButton.getY() : this.confirmButton.getY();
        this.cancelButton.setY(anchorY - 5 - 20);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderFileTypeScrollArea(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.fileTypeScrollArea.verticalScrollBar.active = false;
        this.fileTypeScrollArea.setWidth(this.getBelowFileScrollAreaElementWidth());
        this.fileTypeScrollArea.setX(this.fileListScrollArea.getXWithBorder() + this.fileListScrollArea.getWidthWithBorder() - this.fileTypeScrollArea.getWidthWithBorder());
        this.fileTypeScrollArea.setY(this.fileListScrollArea.getYWithBorder() + this.fileListScrollArea.getHeightWithBorder() + 5 + this.fileTypeScrollListYOffset);
        this.fileTypeScrollArea.render(graphics, mouseX, mouseY, partial);
        float labelPadding = UIBase.getAreaLabelVerticalPadding();
        float labelWidth = UIBase.getUITextWidthNormal(FILE_TYPE_PREFIX_TEXT);
        float labelHeight = UIBase.getUITextHeightNormal();
        float labelX = this.fileTypeScrollArea.getXWithBorder() - labelWidth - labelPadding;
        float labelY = this.fileTypeScrollArea.getYWithBorder() + (this.fileTypeScrollArea.getHeightWithBorder() / 2.0F) - (labelHeight / 2.0F);
        UIBase.renderText(graphics, FILE_TYPE_PREFIX_TEXT, labelX, labelY, UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt());
    }

    protected void renderFileScrollArea(GuiGraphics graphics, int mouseX, int mouseY, float partial, int currentDirFieldYEnd) {
        this.fileListScrollArea.setWidth(this.width - 260 - 20, true);
        int listHeight = this.height - 85 - (this.font.lineHeight + 6) - 2 - 25 + this.fileScrollListHeightOffset;
        if (this.searchBarEnabled) listHeight -= 25;
        this.fileListScrollArea.setHeight(listHeight, true);
        this.fileListScrollArea.setX(20, true);
        this.fileListScrollArea.setY(currentDirFieldYEnd + 2, true);
        this.fileListScrollArea.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderPreview(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.audioPreviewProgressBarWidth = 0;
        this.audioPreviewProgressBarHeight = 0;
        this.videoPreviewProgressBarWidth = 0;
        this.videoPreviewProgressBarHeight = 0;
        this.resetWatermediaDownloadLinkBounds_FancyMenu();
        this.tickPreviewDelay();
        this.tickAudioPreview();
        this.tickVideoPreview();
        this.tickTextPreview();
        boolean showVideoDependencyWarning = this.shouldRenderWatermediaMissingWarning_FancyMenu();
        if (this.previewAudioSupplier == null && this.audioPreviewVolumeSlider != null) {
            this.audioPreviewVolumeSlider.visible = false;
            this.audioPreviewVolumeSlider.active = false;
        }
        if ((this.previewVideoSupplier == null || showVideoDependencyWarning) && this.videoPreviewVolumeSlider != null) {
            this.videoPreviewVolumeSlider.visible = false;
            this.videoPreviewVolumeSlider.active = false;
        }
        if (this.previewAudioSupplier != null) {
            this.renderAudioPreview(graphics, mouseX, mouseY, partial);
        } else if (this.previewVideoSupplier != null) {
            if (showVideoDependencyWarning) {
                this.renderWatermediaMissingWarning_FancyMenu(graphics, mouseX, mouseY);
            } else {
                this.renderVideoPreview(graphics, mouseX, mouseY, partial);
            }
        } else if (this.previewTextureSupplier != null) {
            ITexture t = this.previewTextureSupplier.get();
            ResourceLocation loc = (t != null) ? t.getResourceLocation() : null;
            if (loc != null) {
                int previewBackgroundColor = UIBase.shouldBlur()
                        ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                        : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
                int previewBorderColor = UIBase.shouldBlur()
                        ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                        : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
                AspectRatio ratio = t.getAspectRatio();
                int previewTopY = (int) this.getMainAreaTopY();
                int[] size = ratio.getAspectRatioSizeByMaximumSize(200, (this.cancelButton.getY() - 50) - previewTopY);
                int w = size[0];
                int h = size[1];
                int x = this.width - 20 - w;
                int y = previewTopY;
                UIBase.resetShaderColor(graphics);
                graphics.fill(x, y, x + w, y + h, previewBackgroundColor);
                RenderingUtils.resetShaderColor(graphics);
                RenderSystem.enableBlend();
                graphics.blit(loc, x, y, 0.0F, 0.0F, w, h, w, h);
                UIBase.resetShaderColor(graphics);
                UIBase.renderBorder(graphics, x, y, x + w, y + h, UIBase.ELEMENT_BORDER_THICKNESS, previewBorderColor, true, true, true, true);
            }
        } else {
            this.previewTextScrollArea.setWidth(200, true);
            this.previewTextScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
            this.previewTextScrollArea.setX(this.width - 20 - this.previewTextScrollArea.getWidthWithBorder(), true);
            this.previewTextScrollArea.setY((int) this.getMainAreaTopY(), true);
            this.previewTextScrollArea.render(graphics, mouseX, mouseY, partial);
        }
        UIBase.resetShaderColor(graphics);
    }

    protected int renderCurrentDirectoryField(GuiGraphics graphics, int mouseX, int mouseY, float partial, int x, int y, int width, int height) {
        int xEnd = x + width;
        int yEnd = y + height;
        int backgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int borderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        UIBase.renderRoundedRect(graphics, x + 1, y + 1, width - 2, height - 2, radius, radius, radius, radius, backgroundColor);
        UIBase.renderRoundedBorder(graphics, x, y, xEnd, yEnd, 1, radius, radius, radius, radius, borderColor);
        this.currentDirectoryComponent.setX(x + 4);
        this.currentDirectoryComponent.setY(y + (height / 2) - (this.currentDirectoryComponent.getHeight() / 2));
        this.currentDirectoryComponent.render(graphics, mouseX, mouseY, partial);
        return yEnd;
    }

    protected void renderSearchBar(GuiGraphics graphics, int mouseX, int mouseY, float partial, int x, int y, int width, int height) {
        if (this.searchBar == null) return;
        this.searchBar.setX(x + 1);
        this.searchBar.setY(y + 1);
        this.searchBar.setWidth(width - 2);
        this.searchBar.setHeight(height - 2);
        this.searchBar.render(graphics, mouseX, mouseY, partial);
    }

    protected int getBelowFileScrollAreaElementWidth() {
        float labelPadding = UIBase.getAreaLabelVerticalPadding();
        float labelWidth = UIBase.getUITextWidthNormal(FILE_TYPE_PREFIX_TEXT);
        return (int) (this.fileListScrollArea.getWidthWithBorder() - labelWidth - labelPadding);
    }

    /**
     * Enable or disable the search bar feature.
     * Should be called before {@link #init()} for proper initialization.
     */
    protected void setSearchBarEnabled(boolean enabled) {
        this.searchBarEnabled = enabled;
    }

    /**
     * Set the placeholder text for the search bar.
     * Only used when search bar is enabled.
     */
    protected void setSearchBarPlaceholder(@NotNull Component placeholder) {
        this.searchBarPlaceholder = placeholder;
    }

    /**
     * Enable or disable the enter key action (acts like pressing the confirm button).
     */
    protected void setEnterKeyForDoneEnabled(boolean enabled) {
        this.enterKeyForDoneEnabled = enabled;
    }

    protected boolean allowEnterForDone() {
        return this.enterKeyForDoneEnabled;
    }

    /**
     * Enable or disable the optional apply button.
     * Should be called before {@link #init()} for proper initialization.
     */
    public void setApplyButtonEnabled(boolean enabled) {
        this.applyButtonEnabled = enabled;
    }

    /**
     * Set whether the PiP window should stay above other windows.
     * Should be called before {@link #openInWindow(PiPWindow)}.
     */
    public void setWindowAlwaysOnTop(boolean alwaysOnTop) {
        this.windowAlwaysOnTop = alwaysOnTop;
    }

    /**
     * Set whether the PiP window blocks Minecraft screen inputs.
     * Should be called before {@link #openInWindow(PiPWindow)}.
     */
    public void setWindowBlocksMinecraftScreenInputs(boolean blockInputs) {
        this.windowBlocksMinecraftScreenInputs = blockInputs;
    }

    /**
     * Set whether the PiP window forces focus on open.
     * Should be called before {@link #openInWindow(PiPWindow)}.
     */
    public void setWindowForceFocus(boolean forceFocus) {
        this.windowForceFocus = forceFocus;
    }

    @Nullable
    protected ScrollAreaEntry getSelectedScrollEntry() {
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e.isSelected()) return e;
        }
        return null;
    }

    @Nullable
    protected String getSearchValue() {
        if (!this.searchBarEnabled || this.searchBar == null) return null;
        String value = this.searchBar.getValue();
        if (value == null || value.isBlank()) return null;
        return value;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0) && this.handleWatermediaMissingWarningClick_FancyMenu(mouseX, mouseY)) {
            return true;
        }
        if ((button == 0) && this.handleProgressBarClick(mouseX, mouseY)) {
            return true;
        }
        if ((button == 0) && (this.previewAudioSupplier != null) && (this.audioPreviewToggleButton != null)) {
            if (this.audioPreviewToggleButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if ((button == 0) && (this.previewVideoSupplier != null) && !this.shouldRenderWatermediaMissingWarning_FancyMenu() && (this.videoPreviewToggleButton != null)) {
            if (this.videoPreviewToggleButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if ((button == 0) && !this.fileListScrollArea.isMouseOverInnerArea(mouseX, mouseY) && !this.fileListScrollArea.isMouseInteractingWithGrabbers() && !this.previewTextScrollArea.isMouseOverInnerArea(mouseX, mouseY) && !this.previewTextScrollArea.isMouseInteractingWithGrabbers() && !this.isAudioProgressBarHovered(mouseX, mouseY) && !this.isVideoProgressBarHovered(mouseX, mouseY) && !this.isWidgetHovered()) {
            for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
                e.setSelected(false);
            }
            this.updatePreviewForKey(null);
        }

        return super.mouseClicked(mouseX, mouseY, button);

    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (this.audioPreviewProgressDragging) {
                this.seekAudioPreviewByMouseX(mouseX);
                return true;
            }
            if (this.videoPreviewProgressDragging) {
                this.seekVideoPreviewByMouseX(mouseX);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (button == 0) {
            if (this.audioPreviewProgressDragging) {
                this.seekAudioPreviewByMouseX(mouseX);
                handled = true;
            }
            if (this.videoPreviewProgressDragging) {
                this.seekVideoPreviewByMouseX(mouseX);
                handled = true;
            }
            this.audioPreviewProgressDragging = false;
            this.videoPreviewProgressDragging = false;
        }
        if (handled) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean isWidgetHovered() {
        for (GuiEventListener l : this.children()) {
            if (l instanceof AbstractWidget w) {
                if (w.isHovered()) return true;
            }
        }
        if (this.previewAudioSupplier != null && this.audioPreviewToggleButton != null && this.audioPreviewToggleButton.isHovered()) {
            return true;
        }
        if (this.previewVideoSupplier != null && !this.shouldRenderWatermediaMissingWarning_FancyMenu() && this.videoPreviewToggleButton != null && this.videoPreviewToggleButton.isHovered()) {
            return true;
        }
        return false;
    }

    protected boolean handleProgressBarClick(double mouseX, double mouseY) {
        this.audioPreviewProgressDragging = false;
        this.videoPreviewProgressDragging = false;
        if (this.isAudioProgressBarHovered(mouseX, mouseY)) {
            this.audioPreviewProgressDragging = true;
            this.seekAudioPreviewByMouseX(mouseX);
            return true;
        }
        if (this.isVideoProgressBarHovered(mouseX, mouseY)) {
            this.videoPreviewProgressDragging = true;
            this.seekVideoPreviewByMouseX(mouseX);
            return true;
        }
        return false;
    }

    protected boolean handleWatermediaMissingWarningClick_FancyMenu(double mouseX, double mouseY) {
        if (!this.shouldRenderWatermediaMissingWarning_FancyMenu()) return false;
        if (this.isMouseOverWatermediaDownloadLink_FancyMenu(mouseX, mouseY)) {
            WebUtils.openWebLink(WATERMEDIA_V3_DOWNLOAD_URL_FANCYMENU);
            return true;
        }
        if (this.isMouseOverWatermediaBinariesDownloadLink_FancyMenu(mouseX, mouseY)) {
            WebUtils.openWebLink(WATERMEDIA_BINARIES_DOWNLOAD_URL_FANCYMENU);
            return true;
        }
        return false;
    }

    protected boolean isAudioProgressBarHovered(double mouseX, double mouseY) {
        if (this.previewAudioSupplier == null) return false;
        return this.isPreviewProgressBarHovered(mouseX, mouseY, this.audioPreviewProgressBarX, this.audioPreviewProgressBarY, this.audioPreviewProgressBarWidth, this.audioPreviewProgressBarHeight);
    }

    protected boolean isVideoProgressBarHovered(double mouseX, double mouseY) {
        if (this.shouldRenderWatermediaMissingWarning_FancyMenu()) return false;
        if (this.previewVideoSupplier == null) return false;
        return this.isPreviewProgressBarHovered(mouseX, mouseY, this.videoPreviewProgressBarX, this.videoPreviewProgressBarY, this.videoPreviewProgressBarWidth, this.videoPreviewProgressBarHeight);
    }

    protected boolean isPreviewProgressBarHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return false;
        return mouseX >= x && mouseX < (x + width) && mouseY >= y && mouseY < (y + height);
    }

    protected void seekAudioPreviewByMouseX(double mouseX) {
        if (this.audioPreviewProgressBarWidth <= 0) return;
        float progress = (float) ((mouseX - this.audioPreviewProgressBarX) / (double) this.audioPreviewProgressBarWidth);
        this.seekAudioPreviewByProgress(progress);
    }

    protected void seekVideoPreviewByMouseX(double mouseX) {
        if (this.videoPreviewProgressBarWidth <= 0) return;
        float progress = (float) ((mouseX - this.videoPreviewProgressBarX) / (double) this.videoPreviewProgressBarWidth);
        this.seekVideoPreviewByProgress(progress);
    }

    protected void seekAudioPreviewByProgress(float progress) {
        IAudio audio = this.getPreviewAudio();
        if (audio == null) return;
        float duration = Math.max(0.0F, audio.getDuration());
        if (duration <= 0.0F) return;
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        audio.setPlayTime(duration * clamped);
    }

    protected void seekVideoPreviewByProgress(float progress) {
        IVideo video = this.getPreviewVideo();
        if (video == null) return;
        float duration = Math.max(0.0F, video.getDuration());
        if (duration <= 0.0F) return;
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        video.setPlayTime(duration * clamped);
    }

    protected void updatePreviewForEntry(@Nullable ScrollAreaEntry entry) {
        if (entry == null) {
            this.updatePreviewForKey(null);
            return;
        }
        Object previewKey = this.getPreviewKeyForEntry(entry);
        this.updatePreviewForKey(previewKey);
    }

    protected void updatePreviewForKey(@Nullable Object previewKey) {
        if (previewKey != null) {
            if (!this.previewPending && (this.activePreviewKey != null) && this.activePreviewKey.equals(previewKey)) {
                return;
            }
            this.pendingPreviewKey = previewKey;
            this.pendingPreviewLoadAtMs = System.currentTimeMillis() + PREVIEW_DELAY_MS;
            this.previewPending = true;
            this.activePreviewKey = null;
            this.clearPreviewDisplay(false);
        } else {
            this.cancelPendingPreview();
            this.activePreviewKey = null;
            this.clearPreviewDisplay(true);
        }
    }

    protected void tickPreviewDelay() {
        if (!this.previewPending) return;
        if (this.pendingPreviewKey == null) {
            this.previewPending = false;
            return;
        }
        if (System.currentTimeMillis() < this.pendingPreviewLoadAtMs) return;
        Object pending = this.pendingPreviewKey;
        this.pendingPreviewKey = null;
        this.previewPending = false;
        if (this.isPreviewKeyStillSelected(pending)) {
            this.loadPreviewForKey(pending);
            if (this.previewTextureSupplier == null && this.previewTextSupplier == null && this.previewAudioSupplier == null && this.previewVideoSupplier == null) {
                this.setNoTextPreview();
            }
            this.activePreviewKey = pending;
        }
    }

    protected boolean isPreviewKeyStillSelected(@NotNull Object previewKey) {
        Object selectedKey = this.getSelectedPreviewKey();
        return Objects.equals(previewKey, selectedKey);
    }

    @Nullable
    protected Object getSelectedPreviewKey() {
        ScrollAreaEntry selected = this.getSelectedScrollEntry();
        if (selected == null) return null;
        return this.getPreviewKeyForEntry(selected);
    }

    protected void clearPreviewDisplay(boolean showNoPreview) {
        this.previewTextureSupplier = null;
        this.previewTextSupplier = null;
        this.previewAudioSupplier = null;
        this.previewVideoSupplier = null;
        this.audioPreviewProgressBarWidth = 0;
        this.audioPreviewProgressBarHeight = 0;
        this.videoPreviewProgressBarWidth = 0;
        this.videoPreviewProgressBarHeight = 0;
        this.currentPreviewText = null;
        this.resetWatermediaDownloadLinkBounds_FancyMenu();
        this.stopPreviewMedia();
        if (showNoPreview) {
            this.setNoTextPreview();
        } else if (this.previewTextScrollArea != null) {
            this.previewTextScrollArea.clearEntries();
        }
    }

    protected void cancelPendingPreview() {
        this.pendingPreviewKey = null;
        this.previewPending = false;
    }

    protected void tickTextPreview() {
        if (this.previewAudioSupplier != null || this.previewVideoSupplier != null) return;
        if (this.previewPending) return;
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
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal(s).withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                            } else {
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("......").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                                TextScrollAreaEntry e2 = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("  ").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())), (entry) -> {});
                                e2.setSelectable(false);
                                e2.setBackgroundColorHover(e2.getBackgroundColorNormal());
                                e2.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e2);
                                break;
                            }
                        }
                        float totalWidth = this.previewTextScrollArea.getTotalEntryWidth();
                        for (ScrollAreaEntry e : this.previewTextScrollArea.getEntries()) {
                            e.setWidth(totalWidth);
                        }
                    } else {
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
        if (this.previewAudioSupplier != null || this.previewVideoSupplier != null) return;
        if (this.previewTextScrollArea == null) return;
        this.previewTextScrollArea.clearEntries();
        TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.translatable("fancymenu.ui.filechooser.no_preview").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())), (entry) -> {});
        e.setSelectable(false);
        e.setBackgroundColorHover(e.getBackgroundColorNormal());
        e.setPlayClickSound(false);
        this.previewTextScrollArea.addEntry(e);
    }

    protected boolean handleEnterKey() {
        ScrollAreaEntry selectedEntry = this.getSelectedScrollEntry();
        if (selectedEntry != null) {
            if (this.isGoUpEntry(selectedEntry)) {
                this.goUpDirectory();
                return true;
            }
            if (this.openDirectoryEntry(selectedEntry)) {
                return true;
            }
        }
        if (!this.allowEnterForDone()) return true;
        if (this.confirmButton != null && this.confirmButton.active) {
            this.confirmButton.onPress();
            return true;
        }
        return true;
    }

    protected boolean handleVerticalNavigation(boolean moveDown) {
        List<ScrollAreaEntry> entries = this.getSelectableFileEntries();
        if (entries.isEmpty()) {
            if (!moveDown) {
                this.focusSearchBar();
            }
            return true;
        }
        if (this.searchBarEnabled && this.searchBar != null && this.searchBar.isFocused()) {
            if (moveDown) {
                this.selectEntry(entries.get(0));
            }
            return true;
        }
        ScrollAreaEntry selected = this.getSelectedScrollEntry();
        int selectedIndex = (selected != null) ? entries.indexOf(selected) : -1;
        if (moveDown) {
            if (selectedIndex < 0) {
                this.selectEntry(entries.get(0));
            } else if (selectedIndex < entries.size() - 1) {
                this.selectEntry(entries.get(selectedIndex + 1));
            }
        } else {
            if (selectedIndex <= 0) {
                if (this.searchBarEnabled && this.searchBar != null) {
                    this.clearSelectedEntries();
                    this.focusSearchBar();
                }
            } else {
                this.selectEntry(entries.get(selectedIndex - 1));
            }
        }
        return true;
    }

    @NotNull
    protected List<ScrollAreaEntry> getSelectableFileEntries() {
        List<ScrollAreaEntry> entries = new ArrayList<>();
        for (ScrollAreaEntry entry : this.fileListScrollArea.getEntries()) {
            if (entry.isSelectable()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    protected void selectEntry(@NotNull ScrollAreaEntry entry) {
        entry.setSelected(true);
        this.ensureEntryVisible(entry);
        this.updatePreviewForEntry(entry);
        this.setFocused(null);
    }

    protected void clearSelectedEntries() {
        for (ScrollAreaEntry entry : this.fileListScrollArea.getEntries()) {
            entry.setSelected(false);
        }
        this.updatePreviewForKey(null);
    }

    protected void ensureEntryVisible(@NotNull ScrollAreaEntry entry) {
        float totalScrollHeight = this.fileListScrollArea.getTotalScrollHeight();
        if (totalScrollHeight <= 0.0F) return;
        float innerY = this.fileListScrollArea.getInnerY();
        float innerHeight = this.fileListScrollArea.getInnerHeight();
        float entryTopUnscrolled = innerY;
        for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
            if (e == entry) break;
            entryTopUnscrolled += e.getHeight();
        }
        float entryTop = entryTopUnscrolled + this.fileListScrollArea.getEntryRenderOffsetY(totalScrollHeight);
        float entryBottom = entryTop + entry.getHeight();
        float innerBottom = innerY + innerHeight;
        float scroll = this.fileListScrollArea.verticalScrollBar.getScroll();
        float newScroll = scroll;
        if (entryTop < innerY) {
            float delta = innerY - entryTop;
            newScroll = scroll - (delta / totalScrollHeight);
        } else if (entryBottom > innerBottom) {
            float delta = entryBottom - innerBottom;
            newScroll = scroll + (delta / totalScrollHeight);
        }
        if (newScroll < 0.0F) newScroll = 0.0F;
        if (newScroll > 1.0F) newScroll = 1.0F;
        if (newScroll != scroll) {
            this.fileListScrollArea.verticalScrollBar.setScroll(newScroll);
        }
    }

    protected void focusSearchBar() {
        if (!this.searchBarEnabled || this.searchBar == null) return;
        this.setFocused(this.searchBar);
    }

    protected boolean forwardKeyToFocusedWidget(int keycode, int scancode, int modifiers) {
        GuiEventListener focused = this.getFocused();
        if (focused != null) {
            return focused.keyPressed(keycode, scancode, modifiers);
        }
        return false;
    }

    protected void initAudioPreviewButton() {
        this.audioPreviewToggleButton = new UIIconButton(0.0F, 0.0F, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_PLAY_ICON, button -> {
            this.togglePreviewAudio();
        });
    }

    protected void initAudioPreviewVolumeSlider() {
        if (this.audioPreviewVolumeSlider != null) {
            this.removeWidget(this.audioPreviewVolumeSlider);
        }
        float volume = BrowserAudioSettings.getVolume();
        RangeSlider slider = new RangeSlider(0, 0, 100, AUDIO_PREVIEW_SLIDER_HEIGHT, Component.empty(), 0.0D, 1.0D, volume);
        slider.setRoundingDecimalPlace(2);
        slider.setLabelSupplier(consumes -> Component.empty());
        slider.setSliderValueUpdateListener((s, valueDisplayText, value) -> {
            float newVolume = (float) ((RangeSlider) s).getRangeValue();
            BrowserAudioSettings.setVolume(newVolume);
            this.applyPreviewAudioVolume(newVolume);
        });
        this.audioPreviewVolumeSlider = slider;
        this.audioPreviewVolumeSlider.visible = false;
        this.audioPreviewVolumeSlider.active = false;
        this.addWidget(this.audioPreviewVolumeSlider);
        UIBase.applyDefaultWidgetSkinTo(this.audioPreviewVolumeSlider, UIBase.shouldBlur());
    }

    protected void initVideoPreviewButton() {
        this.videoPreviewToggleButton = new UIIconButton(0.0F, 0.0F, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_PLAY_ICON, button -> {
            this.togglePreviewVideo();
        });
    }

    protected void initVideoPreviewVolumeSlider() {
        if (this.videoPreviewVolumeSlider != null) {
            this.removeWidget(this.videoPreviewVolumeSlider);
        }
        float volume = BrowserVideoSettings.getVolume();
        RangeSlider slider = new RangeSlider(0, 0, 100, AUDIO_PREVIEW_SLIDER_HEIGHT, Component.empty(), 0.0D, 1.0D, volume);
        slider.setRoundingDecimalPlace(2);
        slider.setLabelSupplier(consumes -> Component.empty());
        slider.setSliderValueUpdateListener((s, valueDisplayText, value) -> {
            float newVolume = (float) ((RangeSlider) s).getRangeValue();
            BrowserVideoSettings.setVolume(newVolume);
            this.applyPreviewVideoVolume(newVolume);
        });
        this.videoPreviewVolumeSlider = slider;
        this.videoPreviewVolumeSlider.visible = false;
        this.videoPreviewVolumeSlider.active = false;
        this.addWidget(this.videoPreviewVolumeSlider);
        UIBase.applyDefaultWidgetSkinTo(this.videoPreviewVolumeSlider, UIBase.shouldBlur());
    }

    protected void togglePreviewAudio() {
        if (this.previewAudioSupplier == null) return;
        this.setPreviewAudioPlaying(!this.previewAudioPlaying);
    }

    protected void togglePreviewVideo() {
        if (this.previewVideoSupplier == null) return;
        this.setPreviewVideoPlaying(!this.previewVideoPlaying);
    }

    protected void setPreviewAudio(@Nullable ResourceSupplier<IAudio> supplier, @Nullable Object previewKey) {
        this.stopPreviewAudio();
        this.previewAudioSupplier = supplier;
        this.previewAudioPlaying = false;
        this.previewAudioSeed = (previewKey != null) ? previewKey.hashCode() * 37L : System.nanoTime();
    }

    protected void setPreviewVideo(@Nullable ResourceSupplier<IVideo> supplier, @Nullable Object previewKey) {
        this.stopPreviewVideo();
        this.previewVideoSupplier = supplier;
        this.previewVideoPlaying = false;
    }

    protected void setPreviewAudioPlaying(boolean playing) {
        this.previewAudioPlaying = playing;
        IAudio audio = this.getPreviewAudio();
        if (audio == null) {
            this.previewAudioPlaying = false;
            return;
        }
        if (playing) {
            if (!audio.isPlaying()) audio.play();
        } else {
            if (audio.isPlaying()) audio.pause();
        }
    }

    protected void setPreviewVideoPlaying(boolean playing) {
        this.previewVideoPlaying = playing;
        IVideo video = this.getPreviewVideo();
        if (video == null) {
            this.previewVideoPlaying = false;
            return;
        }
        if (playing) {
            if (!video.isPlaying()) video.play();
        } else {
            if (video.isPlaying()) video.pause();
        }
    }

    @Nullable
    protected IAudio getPreviewAudio() {
        if (this.previewAudioSupplier == null) return null;
        IAudio audio = this.previewAudioSupplier.get();
        if (audio == null || audio.isClosed()) return null;
        if (!Objects.equals(this.currentPreviewAudio, audio)) {
            this.stopPreviewAudio();
            this.currentPreviewAudio = audio;
            audio.pause();
            this.applyPreviewAudioVolume(BrowserAudioSettings.getVolume());
        }
        return this.currentPreviewAudio;
    }

    @Nullable
    protected IVideo getPreviewVideo() {
        if (this.previewVideoSupplier == null) return null;
        IVideo video = this.previewVideoSupplier.get();
        if (video == null || video.isClosed()) return null;
        if (!Objects.equals(this.currentPreviewVideo, video)) {
            this.stopPreviewVideo();
            this.currentPreviewVideo = video;
            video.pause();
            video.setLooping(false);
            this.applyPreviewVideoVolume(BrowserVideoSettings.getVolume());
        }
        return this.currentPreviewVideo;
    }

    protected void stopPreviewAudio() {
        if (this.currentPreviewAudio != null) {
            this.currentPreviewAudio.stop();
        }
        this.currentPreviewAudio = null;
        this.previewAudioPlaying = false;
    }

    protected void stopPreviewVideo() {
        if (this.currentPreviewVideo != null) {
            this.currentPreviewVideo.stop();
        }
        this.currentPreviewVideo = null;
        this.previewVideoPlaying = false;
    }

    protected void stopPreviewMedia() {
        this.stopPreviewAudio();
        this.stopPreviewVideo();
        this.audioPreviewProgressDragging = false;
        this.videoPreviewProgressDragging = false;
    }

    protected void tickAudioPreview() {
        if (this.previewAudioSupplier == null) {
            this.stopPreviewAudio();
            return;
        }
        IAudio audio = this.getPreviewAudio();
        if (audio == null) {
            this.previewAudioPlaying = false;
            return;
        }
        if (this.previewAudioPlaying) {
            if (!audio.isPlaying()) audio.play();
            if (audio.getDuration() > 0.0F && audio.getPlayTime() >= audio.getDuration()) {
                this.previewAudioPlaying = false;
                audio.stop();
            }
        } else if (audio.isPlaying()) {
            audio.pause();
        }
    }

    protected void tickVideoPreview() {
        if (this.previewVideoSupplier == null) {
            this.stopPreviewVideo();
            return;
        }
        if (this.shouldRenderWatermediaMissingWarning_FancyMenu()) {
            this.stopPreviewVideo();
            return;
        }
        IVideo video = this.getPreviewVideo();
        if (video == null) {
            this.previewVideoPlaying = false;
            return;
        }
        this.applyPreviewVideoVolume(BrowserVideoSettings.getVolume());
        if (this.previewVideoPlaying) {
            if (!video.isPlaying()) video.play();
            if (video.isEnded() || (video.getDuration() > 0.0F && video.getPlayTime() >= video.getDuration())) {
                this.previewVideoPlaying = false;
                video.stop();
            }
        } else if (video.isPlaying()) {
            video.pause();
        }
    }

    protected boolean shouldRenderWatermediaMissingWarning_FancyMenu() {
        return (this.previewVideoSupplier != null) && !WatermediaUtil.isWatermediaVideoPlaybackAvailable();
    }

    protected void renderWatermediaMissingWarning_FancyMenu(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        int previewMaxWidth = 200;
        int topY = (int) this.getMainAreaTopY();
        int availableHeight = Math.max(40, (this.cancelButton.getY() - 50) - topY);
        AspectRatio previewAspectRatio = new AspectRatio(16, 9);
        int[] previewSize = previewAspectRatio.getAspectRatioSizeByMaximumSize(previewMaxWidth, availableHeight);
        int previewWidth = Math.max(1, previewSize[0]);
        int previewHeight = Math.max(1, previewSize[1]);
        int x = this.width - 20 - previewWidth;
        int y = topY;

        int warningBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        graphics.fill(x, y, x + previewWidth, y + previewHeight, VIDEO_PREVIEW_WATERMEDIA_WARNING_BACKGROUND_COLOR.getColorInt());
        UIBase.renderBorder(graphics, x, y, x + previewWidth, y + previewHeight, UIBase.ELEMENT_BORDER_THICKNESS, warningBorderColor, true, true, true, true);

        Component infoText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.info");
        Component downloadText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.download");
        Component downloadBinariesText = Component.translatable("fancymenu.backgrounds.video.watermedia_missing.download_binaries");

        float maxTextWidth = previewWidth - 12.0F;
        float spacing = Math.max(3.0F, UIBase.getUITextHeightSmall() * 0.5F);
        float infoTextSize = UIBase.getUITextSizeNormal();
        float linkTextSize = UIBase.getUITextSizeLarge();
        List<MutableComponent> infoLines = UIBase.lineWrapUIComponentsNormal(infoText, maxTextWidth);
        float infoLineHeight = UIBase.getUITextHeight(infoTextSize);
        float infoHeight = infoLines.size() * infoLineHeight;
        float downloadTextWidth = UIBase.getUITextWidth(downloadText, linkTextSize);
        float downloadTextHeight = UIBase.getUITextHeight(linkTextSize);
        float downloadBinariesTextWidth = UIBase.getUITextWidth(downloadBinariesText, linkTextSize);
        float downloadBinariesTextHeight = UIBase.getUITextHeight(linkTextSize);
        if ((downloadTextWidth > maxTextWidth) || (downloadBinariesTextWidth > maxTextWidth)) {
            linkTextSize = UIBase.getUITextSizeNormal();
            downloadTextWidth = UIBase.getUITextWidth(downloadText, linkTextSize);
            downloadTextHeight = UIBase.getUITextHeight(linkTextSize);
            downloadBinariesTextWidth = UIBase.getUITextWidth(downloadBinariesText, linkTextSize);
            downloadBinariesTextHeight = UIBase.getUITextHeight(linkTextSize);
        }
        if ((downloadTextWidth > maxTextWidth) || (downloadBinariesTextWidth > maxTextWidth)) {
            linkTextSize = UIBase.getUITextSizeSmall();
            downloadTextWidth = UIBase.getUITextWidth(downloadText, linkTextSize);
            downloadTextHeight = UIBase.getUITextHeight(linkTextSize);
            downloadBinariesTextWidth = UIBase.getUITextWidth(downloadBinariesText, linkTextSize);
            downloadBinariesTextHeight = UIBase.getUITextHeight(linkTextSize);
        }
        float totalHeight = infoHeight + spacing + downloadTextHeight + spacing + downloadBinariesTextHeight;
        float maxContentHeight = previewHeight - 8.0F;
        if (totalHeight > maxContentHeight) {
            infoTextSize = UIBase.getUITextSizeSmall();
            infoLines = UIBase.lineWrapUIComponentsSmall(infoText, maxTextWidth);
            infoLineHeight = UIBase.getUITextHeight(infoTextSize);
            infoHeight = infoLines.size() * infoLineHeight;
            linkTextSize = UIBase.getUITextSizeSmall();
            downloadTextWidth = UIBase.getUITextWidth(downloadText, linkTextSize);
            downloadTextHeight = UIBase.getUITextHeight(linkTextSize);
            downloadBinariesTextWidth = UIBase.getUITextWidth(downloadBinariesText, linkTextSize);
            downloadBinariesTextHeight = UIBase.getUITextHeight(linkTextSize);
            totalHeight = infoHeight + spacing + downloadTextHeight + spacing + downloadBinariesTextHeight;
        }
        float currentY = y + (previewHeight / 2.0F) - (totalHeight / 2.0F);

        int textColor = DrawableColor.WHITE.getColorInt();
        for (MutableComponent line : infoLines) {
            float lineWidth = UIBase.getUITextWidth(line, infoTextSize);
            float lineX = x + (previewWidth / 2.0F) - (lineWidth / 2.0F);
            UIBase.renderText(graphics, line, lineX, currentY, textColor, infoTextSize);
            currentY += infoLineHeight;
        }

        float downloadX = x + (previewWidth / 2.0F) - (downloadTextWidth / 2.0F);
        float downloadY = currentY + spacing;
        float downloadBinariesX = x + (previewWidth / 2.0F) - (downloadBinariesTextWidth / 2.0F);
        float downloadBinariesY = downloadY + downloadTextHeight + spacing;

        this.watermediaDownloadX_FancyMenu = downloadX;
        this.watermediaDownloadY_FancyMenu = downloadY;
        this.watermediaDownloadWidth_FancyMenu = downloadTextWidth;
        this.watermediaDownloadHeight_FancyMenu = downloadTextHeight;
        this.watermediaBinariesDownloadX_FancyMenu = downloadBinariesX;
        this.watermediaBinariesDownloadY_FancyMenu = downloadBinariesY;
        this.watermediaBinariesDownloadWidth_FancyMenu = downloadBinariesTextWidth;
        this.watermediaBinariesDownloadHeight_FancyMenu = downloadBinariesTextHeight;

        boolean hoveredMain = this.isMouseOverWatermediaDownloadLink_FancyMenu(mouseX, mouseY);
        boolean hoveredBinaries = this.isMouseOverWatermediaBinariesDownloadLink_FancyMenu(mouseX, mouseY);
        if (hoveredMain || hoveredBinaries) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
        }
        Component renderedDownloadText = downloadText.copy().setStyle(Style.EMPTY.withUnderlined(hoveredMain));
        Component renderedDownloadBinariesText = downloadBinariesText.copy().setStyle(Style.EMPTY.withUnderlined(hoveredBinaries));
        UIBase.renderText(graphics, renderedDownloadText, downloadX, downloadY, textColor, linkTextSize);
        UIBase.renderText(graphics, renderedDownloadBinariesText, downloadBinariesX, downloadBinariesY, textColor, linkTextSize);
    }

    protected boolean isMouseOverWatermediaDownloadLink_FancyMenu(double mouseX, double mouseY) {
        if (!Float.isFinite(this.watermediaDownloadX_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadY_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadWidth_FancyMenu)
                || !Float.isFinite(this.watermediaDownloadHeight_FancyMenu)) {
            return false;
        }
        return (mouseX >= this.watermediaDownloadX_FancyMenu)
                && (mouseX <= (this.watermediaDownloadX_FancyMenu + this.watermediaDownloadWidth_FancyMenu))
                && (mouseY >= this.watermediaDownloadY_FancyMenu)
                && (mouseY <= (this.watermediaDownloadY_FancyMenu + this.watermediaDownloadHeight_FancyMenu));
    }

    protected boolean isMouseOverWatermediaBinariesDownloadLink_FancyMenu(double mouseX, double mouseY) {
        if (!Float.isFinite(this.watermediaBinariesDownloadX_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadY_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadWidth_FancyMenu)
                || !Float.isFinite(this.watermediaBinariesDownloadHeight_FancyMenu)) {
            return false;
        }
        return (mouseX >= this.watermediaBinariesDownloadX_FancyMenu)
                && (mouseX <= (this.watermediaBinariesDownloadX_FancyMenu + this.watermediaBinariesDownloadWidth_FancyMenu))
                && (mouseY >= this.watermediaBinariesDownloadY_FancyMenu)
                && (mouseY <= (this.watermediaBinariesDownloadY_FancyMenu + this.watermediaBinariesDownloadHeight_FancyMenu));
    }

    protected void resetWatermediaDownloadLinkBounds_FancyMenu() {
        this.watermediaDownloadX_FancyMenu = Float.NaN;
        this.watermediaDownloadY_FancyMenu = Float.NaN;
        this.watermediaDownloadWidth_FancyMenu = Float.NaN;
        this.watermediaDownloadHeight_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadX_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadY_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadWidth_FancyMenu = Float.NaN;
        this.watermediaBinariesDownloadHeight_FancyMenu = Float.NaN;
    }

    protected void renderVideoPreview(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int previewMaxWidth = 200;
        int topY = (int) this.getMainAreaTopY();
        int availableHeight = (this.cancelButton.getY() - 50) - topY;
        int textHeight = Math.round(UIBase.getUITextHeightNormal());
        int progressAreaHeight = AUDIO_PREVIEW_PROGRESS_BAR_SPACING + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + textHeight;
        int controlsAreaHeight = AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING + progressAreaHeight;
        int maxFrameHeight = Math.max(12, availableHeight - controlsAreaHeight);
        IVideo video = this.getPreviewVideo();
        AspectRatio ratio = this.getPreviewVideoAspectRatio(video);
        int[] frameSize = ratio.getAspectRatioSizeByMaximumSize(previewMaxWidth, maxFrameHeight);
        int previewWidth = Math.max(1, frameSize[0]);
        int previewHeight = Math.max(1, frameSize[1]);
        int x = this.width - 20 - previewWidth;
        int y = topY;
        int previewBackgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int previewBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();

        graphics.fill(x, y, x + previewWidth, y + previewHeight, previewBackgroundColor);

        ResourceLocation location = (video != null) ? video.getResourceLocation() : null;
        if (location != null) {
            RenderingUtils.resetShaderColor(graphics);
            RenderSystem.enableBlend();
            graphics.blit(location, x, y, 0.0F, 0.0F, previewWidth, previewHeight, previewWidth, previewHeight);
            UIBase.resetShaderColor(graphics);
        }

        UIBase.renderBorder(graphics, x, y, x + previewWidth, y + previewHeight, UIBase.ELEMENT_BORDER_THICKNESS, previewBorderColor, true, true, true, true);
        int progressY = y + previewHeight + AUDIO_PREVIEW_PROGRESS_BAR_SPACING;
        this.renderVideoPreviewProgress(graphics, x, progressY, previewWidth);
        int controlsY = progressY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + textHeight + AUDIO_PREVIEW_BUTTON_SPACING;
        int buttonX = x;
        int buttonY = controlsY;
        int sliderX = buttonX + AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING;
        int sliderWidth = Math.max(10, previewWidth - (AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING));
        int sliderY = controlsY + (AUDIO_PREVIEW_BUTTON_SIZE - AUDIO_PREVIEW_SLIDER_HEIGHT) / 2;
        this.renderVideoPreviewButton(graphics, mouseX, mouseY, partial, buttonX, buttonY);
        this.renderVideoPreviewVolumeSlider(graphics, mouseX, mouseY, partial, sliderX, sliderY, sliderWidth);
    }

    protected void renderVideoPreviewButton(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, int buttonX, int buttonY) {
        if (this.videoPreviewToggleButton == null || this.previewVideoSupplier == null) return;
        this.videoPreviewToggleButton
                .setX(buttonX)
                .setY(buttonY)
                .setWidth(AUDIO_PREVIEW_BUTTON_SIZE)
                .setHeight(AUDIO_PREVIEW_BUTTON_SIZE)
                .setIcon(this.previewVideoPlaying ? AUDIO_PREVIEW_PAUSE_ICON : AUDIO_PREVIEW_PLAY_ICON);
        this.videoPreviewToggleButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderVideoPreviewVolumeSlider(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, int sliderX, int sliderY, int sliderWidth) {
        if (this.videoPreviewVolumeSlider == null) return;
        this.videoPreviewVolumeSlider.setX(sliderX);
        this.videoPreviewVolumeSlider.setY(sliderY);
        this.videoPreviewVolumeSlider.setWidth(sliderWidth);
        this.videoPreviewVolumeSlider.setHeight(AUDIO_PREVIEW_SLIDER_HEIGHT);
        this.videoPreviewVolumeSlider.visible = true;
        this.videoPreviewVolumeSlider.active = (this.previewVideoSupplier != null);
        this.videoPreviewVolumeSlider.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderVideoPreviewProgress(@NotNull GuiGraphics graphics, int previewX, int progressY, int previewWidth) {
        int barX = previewX;
        int barWidth = previewWidth;
        int barY = progressY;
        int barYEnd = barY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT;
        this.videoPreviewProgressBarX = barX;
        this.videoPreviewProgressBarY = barY;
        this.videoPreviewProgressBarWidth = barWidth;
        this.videoPreviewProgressBarHeight = AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT;
        int progressBackgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int progressBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        graphics.fill(barX, barY, barX + barWidth, barYEnd, progressBackgroundColor);
        UIBase.renderBorder(graphics, barX, barY, barX + barWidth, barYEnd, 1, progressBorderColor, true, true, true, true);

        IVideo video = this.getPreviewVideo();
        float duration = video != null ? Math.max(0.0F, video.getDuration()) : 0.0F;
        float playTime = video != null ? Math.max(0.0F, video.getPlayTime()) : 0.0F;
        float progress = duration > 0.0F ? Math.min(1.0F, playTime / duration) : 0.0F;
        int filledWidth = (int) (barWidth * progress);
        if (filledWidth > 0) {
            int fillColor = this.getAudioVisualizerGradientColor(progress);
            graphics.fill(barX, barY, barX + filledWidth, barYEnd, fillColor);
        }

        String timeText = this.formatAudioTime(playTime) + " / " + this.formatAudioTime(duration);
        int timeY = barYEnd + AUDIO_PREVIEW_TIME_SPACING;
        float textWidth = UIBase.getUITextWidth(timeText);
        float textX = previewX + (previewWidth / 2.0F) - (textWidth / 2.0F);
        UIBase.renderText(graphics, timeText, textX, timeY, UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt());
    }

    protected void renderAudioPreview(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int previewWidth = 200;
        int topY = (int) this.getMainAreaTopY();
        int availableHeight = (this.cancelButton.getY() - 50) - topY;
        int textHeight = Math.round(UIBase.getUITextHeightNormal());
        int progressAreaHeight = AUDIO_PREVIEW_PROGRESS_BAR_SPACING + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + textHeight;
        int basePreviewHeight = Math.max(40, availableHeight - (AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING + progressAreaHeight));
        int previewHeight = Math.max(12, basePreviewHeight / 3);
        int x = this.width - 20 - previewWidth;
        int y = topY;
        int previewBackgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int previewBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        graphics.fill(x, y, x + previewWidth, y + previewHeight, previewBackgroundColor);
        this.renderAudioVisualizer(graphics, x + 4, y + 4, previewWidth - 8, previewHeight - 8);
        UIBase.renderBorder(graphics, x, y, x + previewWidth, y + previewHeight, UIBase.ELEMENT_BORDER_THICKNESS, previewBorderColor, true, true, true, true);
        int progressY = y + previewHeight + AUDIO_PREVIEW_PROGRESS_BAR_SPACING;
        this.renderAudioPreviewProgress(graphics, x, progressY, previewWidth);
        int controlsY = progressY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + textHeight + AUDIO_PREVIEW_BUTTON_SPACING;
        int buttonX = x;
        int buttonY = controlsY;
        int sliderX = buttonX + AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING;
        int sliderWidth = Math.max(10, previewWidth - (AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING));
        int sliderY = controlsY + (AUDIO_PREVIEW_BUTTON_SIZE - AUDIO_PREVIEW_SLIDER_HEIGHT) / 2;
        this.renderAudioPreviewButton(graphics, mouseX, mouseY, partial, buttonX, buttonY);
        this.renderAudioPreviewVolumeSlider(graphics, mouseX, mouseY, partial, sliderX, sliderY, sliderWidth);
    }

    protected void renderAudioPreviewButton(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, int buttonX, int buttonY) {
        if (this.audioPreviewToggleButton == null || this.previewAudioSupplier == null) return;
        this.audioPreviewToggleButton
                .setX(buttonX)
                .setY(buttonY)
                .setWidth(AUDIO_PREVIEW_BUTTON_SIZE)
                .setHeight(AUDIO_PREVIEW_BUTTON_SIZE)
                .setIcon(this.previewAudioPlaying ? AUDIO_PREVIEW_PAUSE_ICON : AUDIO_PREVIEW_PLAY_ICON);
        this.audioPreviewToggleButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderAudioPreviewVolumeSlider(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, int sliderX, int sliderY, int sliderWidth) {
        if (this.audioPreviewVolumeSlider == null) return;
        this.audioPreviewVolumeSlider.setX(sliderX);
        this.audioPreviewVolumeSlider.setY(sliderY);
        this.audioPreviewVolumeSlider.setWidth(sliderWidth);
        this.audioPreviewVolumeSlider.setHeight(AUDIO_PREVIEW_SLIDER_HEIGHT);
        this.audioPreviewVolumeSlider.visible = true;
        this.audioPreviewVolumeSlider.active = (this.previewAudioSupplier != null);
        this.audioPreviewVolumeSlider.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderAudioVisualizer(@NotNull GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int barWidth = Math.max(2, width / 64);
        int gap = Math.max(1, barWidth / 2);
        int barCount = Math.max(8, (width + gap) / (barWidth + gap));
        float time = this.getAudioVisualizerTime();
        float baseIntensity = this.previewAudioPlaying ? 1.0F : 0.35F;
        for (int i = 0; i < barCount; i++) {
            int barX = x + i * (barWidth + gap);
            if (barX + barWidth > x + width) break;
            float phase = (time * 2.6F) + (i * 0.35F);
            float wave = (float)((Math.sin(phase) + 1.0) * 0.5);
            float wave2 = (float)((Math.sin(phase * 0.7F + 1.3F) + 1.0) * 0.5);
            float intensity = (wave * 0.7F + wave2 * 0.3F) * baseIntensity;
            int barHeight = Math.max(2, (int)(intensity * height));
            int barY = y + (height - barHeight);
            int color = this.getAudioVisualizerGradientColor((float)i / (float)Math.max(1, barCount - 1));
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, color);
        }
    }

    protected void renderAudioPreviewProgress(@NotNull GuiGraphics graphics, int previewX, int progressY, int previewWidth) {
        int barX = previewX;
        int barWidth = previewWidth;
        int barY = progressY;
        int barYEnd = barY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT;
        this.audioPreviewProgressBarX = barX;
        this.audioPreviewProgressBarY = barY;
        this.audioPreviewProgressBarWidth = barWidth;
        this.audioPreviewProgressBarHeight = AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT;
        int progressBackgroundColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        int progressBorderColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_border_color.getColorInt();
        graphics.fill(barX, barY, barX + barWidth, barYEnd, progressBackgroundColor);
        UIBase.renderBorder(graphics, barX, barY, barX + barWidth, barYEnd, 1, progressBorderColor, true, true, true, true);

        IAudio audio = this.getPreviewAudio();
        float duration = audio != null ? Math.max(0.0F, audio.getDuration()) : 0.0F;
        float playTime = audio != null ? Math.max(0.0F, audio.getPlayTime()) : 0.0F;
        float progress = duration > 0.0F ? Math.min(1.0F, playTime / duration) : 0.0F;
        int filledWidth = (int)(barWidth * progress);
        if (filledWidth > 0) {
            int fillColor = this.getAudioVisualizerGradientColor(progress);
            graphics.fill(barX, barY, barX + filledWidth, barYEnd, fillColor);
        }

        String timeText = this.formatAudioTime(playTime) + " / " + this.formatAudioTime(duration);
        int timeY = barYEnd + AUDIO_PREVIEW_TIME_SPACING;
        float textWidth = UIBase.getUITextWidth(timeText);
        float textX = previewX + (previewWidth / 2.0F) - (textWidth / 2.0F);
        UIBase.renderText(graphics, timeText, textX, timeY, UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt());
    }

    protected float getMainAreaLabelY() {
        return 50.0F;
    }

    protected float getMainAreaTopY() {
        return this.getMainAreaLabelY() + UIBase.getUITextHeightNormal() + UIBase.getAreaLabelVerticalPadding();
    }

    @NotNull
    protected String formatAudioTime(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0.0F) seconds = 0.0F;
        int totalSeconds = (int) seconds;
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, secs);
    }

    protected void applyPreviewAudioVolume(float volume) {
        if (this.currentPreviewAudio != null) {
            this.currentPreviewAudio.setVolume(volume);
        }
    }

    protected void applyPreviewVideoVolume(float volume) {
        float clampedVolume = Math.max(0.0F, Math.min(1.0F, volume));
        float masterVolume = this.getPreviewVideoMasterVolume();
        float effectiveVolume = Math.max(0.0F, Math.min(1.0F, clampedVolume * masterVolume));
        if (this.currentPreviewVideo != null) {
            this.currentPreviewVideo.setVolume(effectiveVolume);
        }
    }

    @NotNull
    protected AspectRatio getPreviewVideoAspectRatio(@Nullable IVideo video) {
        if (video == null) {
            return new AspectRatio(16, 9);
        }
        int videoWidth = video.getWidth();
        int videoHeight = video.getHeight();
        if (videoWidth <= 16 || videoHeight <= 16) {
            return new AspectRatio(16, 9);
        }
        return video.getAspectRatio();
    }

    protected float getPreviewVideoMasterVolume() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, minecraft.options.getSoundSourceVolume(SoundSource.MASTER)));
    }

    protected float getAudioVisualizerTime() {
        if (this.previewAudioPlaying && this.currentPreviewAudio != null) {
            return this.currentPreviewAudio.getPlayTime();
        }
        return (float)(this.previewAudioSeed % 10000L) / 1000.0F;
    }

    protected int getAudioVisualizerGradientColor(float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        int orange = 0xFFFFA500;
        int red = 0xFFFF0000;
        int magenta = 0xFFFF00FF;
        int purple = 0xFF8000FF;
        if (clamped <= 0.33F) {
            return this.lerpColor(orange, red, clamped / 0.33F);
        }
        if (clamped <= 0.66F) {
            return this.lerpColor(red, magenta, (clamped - 0.33F) / 0.33F);
        }
        return this.lerpColor(magenta, purple, (clamped - 0.66F) / 0.34F);
    }

    protected int lerpColor(int start, int end, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        int a1 = (start >> 24) & 0xFF;
        int r1 = (start >> 16) & 0xFF;
        int g1 = (start >> 8) & 0xFF;
        int b1 = start & 0xFF;
        int a2 = (end >> 24) & 0xFF;
        int r2 = (end >> 16) & 0xFF;
        int g2 = (end >> 8) & 0xFF;
        int b2 = end & 0xFF;
        int a = (int)(a1 + (a2 - a1) * clamped);
        int r = (int)(r1 + (r2 - r1) * clamped);
        int g = (int)(g1 + (g2 - g1) * clamped);
        int b = (int)(b1 + (b2 - b1) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public abstract static class AbstractIconTextScrollAreaEntry extends ScrollAreaEntry {

        protected static final int BORDER = 3;

        protected final MutableComponent entryNameComponent;
        protected long lastClick = -1;

        public AbstractIconTextScrollAreaEntry(@NotNull ScrollArea parent, @NotNull MutableComponent entryNameComponent) {
            super(parent, 100, 30);
            this.entryNameComponent = entryNameComponent;

            this.setWidth((int)UIBase.getUITextWidthNormal(this.entryNameComponent) + (BORDER * 2) + ICON_PIXEL_SIZE + 3);
            this.setHeight((BORDER * 2) + ICON_PIXEL_SIZE);

            this.playClickSound = false;
        }

        @NotNull
        protected abstract MaterialIcon getIcon();

        protected int getIconRenderWidth() {
            return ICON_PIXEL_SIZE;
        }

        protected int getIconRenderHeight() {
            return ICON_PIXEL_SIZE;
        }

        protected int getIconInnerPadding() {
            return 0;
        }

        protected boolean isResourceUnfriendly() {
            return false;
        }

        protected int getTextColor() {
            if (this.isResourceUnfriendly()) {
                return UIBase.getUITheme().error_color.getColorInt();
            }
            return UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            RenderSystem.enableBlend();

            int iconWidth = this.getIconRenderWidth();
            int iconHeight = this.getIconRenderHeight();
            int padding = Math.max(0, this.getIconInnerPadding());
            float areaX = this.x + BORDER + padding;
            float areaY = this.y + BORDER + padding;
            float areaWidth = iconWidth - (padding * 2);
            float areaHeight = iconHeight - (padding * 2);
            IconRenderData iconData = resolveMaterialIconData(this.getIcon(), areaWidth, areaHeight);
            if (iconData != null) {
                UIBase.getUITheme().setUITextureShaderColor(graphics, 1.0F);
                blitScaledIcon(graphics, iconData, areaX, areaY, areaWidth, areaHeight);
                UIBase.resetShaderColor(graphics);
            }

            UIBase.renderText(graphics, this.entryNameComponent, this.x + BORDER + ICON_PIXEL_SIZE + 3, this.y + (this.height / 2f) - (UIBase.getUITextHeightNormal() / 2f), this.getTextColor());

            if (this.isResourceUnfriendly() && this.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height) && this.parent.isMouseOverInnerArea(mouseX, mouseY)) {
                TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(Component.translatable("fancymenu.ui.filechooser.resource_name_check.not_passed.tooltip")), () -> true);
            }

        }

        @Nullable
        private static IconRenderData resolveMaterialIconData(@Nullable MaterialIcon icon, float renderWidth, float renderHeight) {
            if (icon == null) {
                return null;
            }
            ResourceLocation location = icon.getTextureLocationForUI(renderWidth, renderHeight);
            if (location == null) {
                return null;
            }
            int iconSize = icon.calculateBestTextureSizeForUI(renderWidth, renderHeight);
            int width = icon.getWidth(iconSize);
            int height = icon.getHeight(iconSize);
            if (width <= 0 || height <= 0) {
                return null;
            }
            return new IconRenderData(location, width, height);
        }

        private static void blitScaledIcon(@NotNull GuiGraphics graphics, @NotNull IconRenderData iconData, float areaX, float areaY, float areaWidth, float areaHeight) {
            if (areaWidth <= 0.0F || areaHeight <= 0.0F) {
                return;
            }
            float scale = Math.min(areaWidth / (float) iconData.width, areaHeight / (float) iconData.height);
            if (!Float.isFinite(scale) || scale <= 0.0F) {
                return;
            }
            float scaledWidth = iconData.width * scale;
            float scaledHeight = iconData.height * scale;
            float drawX = areaX + (areaWidth - scaledWidth) * 0.5F;
            float drawY = areaY + (areaHeight - scaledHeight) * 0.5F;
            graphics.pose().pushPose();
            graphics.pose().translate(drawX, drawY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.blit(iconData.texture, 0, 0, 0.0F, 0.0F, iconData.width, iconData.height, iconData.width, iconData.height);
            graphics.pose().popPose();
        }

        private static final class IconRenderData {
            private final ResourceLocation texture;
            private final int width;
            private final int height;

            private IconRenderData(@NotNull ResourceLocation texture, int width, int height) {
                this.texture = texture;
                this.width = width;
                this.height = height;
            }
        }

    }

}
