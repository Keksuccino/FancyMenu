package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class AbstractBrowserScreen extends Screen implements InitialWidgetFocusScreen {

    protected static final int ICON_PIXEL_SIZE = 32;

    // All icon textures are 32x32 pixels
    protected static final ResourceLocation GO_UP_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/go_up_icon.png");
    protected static final ResourceLocation GENERIC_FILE_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/file_icon.png");
    protected static final ResourceLocation TEXT_FILE_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/text_file_icon.png");
    protected static final ResourceLocation AUDIO_FILE_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/audio_file_icon.png");
    protected static final ResourceLocation VIDEO_FILE_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/video_file_icon.png");
    protected static final ResourceLocation IMAGE_FILE_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/image_file_icon.png");
    protected static final ResourceLocation FOLDER_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/folder_icon.png");

    protected static final ResourceLocation AUDIO_PREVIEW_PLAY_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/controls/play_button_normal.png");
    protected static final ResourceLocation AUDIO_PREVIEW_PAUSE_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/controls/pause_button_normal.png");
    protected static final ResourceLocation AUDIO_PREVIEW_PLAY_BUTTON_TEXTURE_HOVER = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/controls/play_button_hover.png");
    protected static final ResourceLocation AUDIO_PREVIEW_PAUSE_BUTTON_TEXTURE_HOVER = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/file_browser/controls/pause_button_hover.png");

    protected static final int AUDIO_PREVIEW_BUTTON_SIZE = 20;
    protected static final int AUDIO_PREVIEW_BUTTON_SPACING = 6;
    protected static final int AUDIO_PREVIEW_SLIDER_HEIGHT = 20;
    protected static final int AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT = 6;
    protected static final int AUDIO_PREVIEW_PROGRESS_BAR_SPACING = 4;
    protected static final int AUDIO_PREVIEW_TIME_SPACING = 2;

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
    protected IText currentPreviewText;
    @Nullable
    protected IAudio currentPreviewAudio;
    @Nullable
    protected Object pendingPreviewKey;
    @Nullable
    protected Object activePreviewKey;
    protected long pendingPreviewLoadAtMs = 0L;
    protected boolean previewPending = false;
    protected boolean previewAudioPlaying = false;
    protected long previewAudioSeed = 0L;
    protected ExtendedButton confirmButton;
    protected ExtendedButton cancelButton;
    protected ExtendedButton audioPreviewToggleButton;
    protected RangeSlider audioPreviewVolumeSlider;
    protected ComponentWidget currentDirectoryComponent;
    protected int fileScrollListHeightOffset = 0;
    protected int fileTypeScrollListYOffset = 0;
    @Nullable
    protected MutableComponent currentFileTypesComponent;

    protected AbstractBrowserScreen(@NotNull Component title) {
        super(title);
    }

    @Override
    protected void init() {

        if (this.searchBar != null) {
            this.removeWidget(this.searchBar);
        }
        if (this.searchBarEnabled) {
            String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
            this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 0, 20 - 2, Component.empty());
            this.searchBar.setHintFancyMenu(consumes -> AbstractBrowserScreen.this.searchBarPlaceholder);
            this.searchBar.setValue(oldSearchValue);
            this.searchBar.setResponder(s -> AbstractBrowserScreen.this.updateEntryList());
            UIBase.applyDefaultWidgetSkinTo(this.searchBar);
            this.searchBar.setMaxLength(100000);
            this.addWidget(this.searchBar);
            this.setupInitialFocusWidget(this, this.searchBar);
        }

        this.confirmButton = this.buildConfirmButton();
        if (this.confirmButton != null) {
            Button.OnPress originalConfirmAction = this.confirmButton.getPressAction();
            this.confirmButton.setPressAction(button -> {
                this.stopPreviewAudio();
                if (originalConfirmAction != null) {
                    originalConfirmAction.onPress(button);
                }
            });
        }
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultWidgetSkinTo(this.confirmButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.stopPreviewAudio();
            this.onCancel();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.initAudioPreviewButton();
        this.initAudioPreviewVolumeSlider();

        this.updateCurrentDirectoryComponent();

        this.updateFileTypeScrollArea();

        this.initExtraButtons();

        this.addWidget(this.fileListScrollArea);
        this.addWidget(this.fileTypeScrollArea);
        this.addWidget(this.previewTextScrollArea);

    }

    @NotNull
    protected abstract ExtendedButton buildConfirmButton();

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
    public void onClose() {
        this.stopPreviewAudio();
        this.onCancel();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();

        if (this.currentFileTypesComponent != null) {
            this.fileTypeScrollArea.horizontalScrollBar.active = (Minecraft.getInstance().font.width(this.currentFileTypesComponent) > (this.fileTypeScrollArea.getInnerWidth() - 10));
        }

        RenderSystem.enableBlend();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);

        graphics.drawString(this.font, this.getEntriesLabel(), 20, 50, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);

        int leftAreaWidth = this.width - 260 - 20;
        int currentDirFieldY = 50 + 15;
        if (this.searchBarEnabled) {
            this.renderSearchBar(graphics, mouseX, mouseY, partial, 20, currentDirFieldY, leftAreaWidth, 20);
            currentDirFieldY += 25;
        }
        int currentDirFieldYEnd = this.renderCurrentDirectoryField(graphics, mouseX, mouseY, partial, 20, currentDirFieldY, leftAreaWidth, this.font.lineHeight + 6);

        this.renderFileScrollArea(graphics, mouseX, mouseY, partial, currentDirFieldYEnd);

        this.renderFileTypeScrollArea(graphics, mouseX, mouseY, partial);

        Component previewLabel = Component.translatable("fancymenu.ui.filechooser.preview");
        int previewLabelWidth = this.font.width(previewLabel);
        graphics.drawString(this.font, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);

        this.renderConfirmButton(graphics, mouseX, mouseY, partial);

        this.renderCancelButton(graphics, mouseX, mouseY, partial);

        this.renderExtraButtons(graphics, mouseX, mouseY, partial);

        this.renderPreview(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
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

    protected void renderCancelButton(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.confirmButton.getY() - 5 - 20);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);
    }

    protected void renderFileTypeScrollArea(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.fileTypeScrollArea.verticalScrollBar.active = false;
        this.fileTypeScrollArea.setWidth(this.getBelowFileScrollAreaElementWidth());
        this.fileTypeScrollArea.setX(this.fileListScrollArea.getXWithBorder() + this.fileListScrollArea.getWidthWithBorder() - this.fileTypeScrollArea.getWidthWithBorder());
        this.fileTypeScrollArea.setY(this.fileListScrollArea.getYWithBorder() + this.fileListScrollArea.getHeightWithBorder() + 5 + this.fileTypeScrollListYOffset);
        this.fileTypeScrollArea.render(graphics, mouseX, mouseY, partial);
        graphics.drawString(this.font, FILE_TYPE_PREFIX_TEXT, (int)(this.fileTypeScrollArea.getXWithBorder() - Minecraft.getInstance().font.width(FILE_TYPE_PREFIX_TEXT) - 5), (int)(this.fileTypeScrollArea.getYWithBorder() + (this.fileTypeScrollArea.getHeightWithBorder() / 2) - (Minecraft.getInstance().font.lineHeight / 2)), UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt(), false);
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
        this.tickPreviewDelay();
        this.tickAudioPreview();
        this.tickTextPreview();
        if (this.previewAudioSupplier == null && this.audioPreviewToggleButton != null) {
            this.audioPreviewToggleButton.visible = false;
            this.audioPreviewToggleButton.active = false;
        }
        if (this.previewAudioSupplier == null && this.audioPreviewVolumeSlider != null) {
            this.audioPreviewVolumeSlider.visible = false;
            this.audioPreviewVolumeSlider.active = false;
        }
        if (this.previewAudioSupplier != null) {
            this.renderAudioPreview(graphics, mouseX, mouseY, partial);
        } else if (this.previewTextureSupplier != null) {
            ITexture t = this.previewTextureSupplier.get();
            ResourceLocation loc = (t != null) ? t.getResourceLocation() : null;
            if (loc != null) {
                AspectRatio ratio = t.getAspectRatio();
                int[] size = ratio.getAspectRatioSizeByMaximumSize(200, (this.cancelButton.getY() - 50) - (50 + 15));
                int w = size[0];
                int h = size[1];
                int x = this.width - 20 - w;
                int y = 50 + 15;
                UIBase.resetShaderColor(graphics);
                graphics.fill(x, y, x + w, y + h, UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt());
                RenderingUtils.resetShaderColor(graphics);
                RenderSystem.enableBlend();
                graphics.blit(loc, x, y, 0.0F, 0.0F, w, h, w, h);
                UIBase.resetShaderColor(graphics);
                UIBase.renderBorder(graphics, x, y, x + w, y + h, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUITheme().ui_interface_widget_border_color.getColor(), true, true, true, true);
            }
        } else {
            this.previewTextScrollArea.setWidth(200, true);
            this.previewTextScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
            this.previewTextScrollArea.setX(this.width - 20 - this.previewTextScrollArea.getWidthWithBorder(), true);
            this.previewTextScrollArea.setY(50 + 15, true);
            this.previewTextScrollArea.render(graphics, mouseX, mouseY, partial);
        }
        UIBase.resetShaderColor(graphics);
    }

    protected int renderCurrentDirectoryField(GuiGraphics graphics, int mouseX, int mouseY, float partial, int x, int y, int width, int height) {
        int xEnd = x + width;
        int yEnd = y + height;
        graphics.fill(x + 1, y + 1, xEnd - 1, yEnd - 1, UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt());
        UIBase.renderBorder(graphics, x, y, xEnd, yEnd, 1, UIBase.getUITheme().ui_interface_widget_border_color.getColor(), true, true, true, true);
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
        return (int)(this.fileListScrollArea.getWidthWithBorder() - Minecraft.getInstance().font.width(FILE_TYPE_PREFIX_TEXT) - 5);
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

        if ((button == 0) && !this.fileListScrollArea.isMouseOverInnerArea(mouseX, mouseY) && !this.fileListScrollArea.isMouseInteractingWithGrabbers() && !this.previewTextScrollArea.isMouseOverInnerArea(mouseX, mouseY) && !this.previewTextScrollArea.isMouseInteractingWithGrabbers() && !this.isWidgetHovered()) {
            for (ScrollAreaEntry e : this.fileListScrollArea.getEntries()) {
                e.setSelected(false);
            }
            this.updatePreviewForKey(null);
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
            if (this.previewTextureSupplier == null && this.previewTextSupplier == null && this.previewAudioSupplier == null) {
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
        this.currentPreviewText = null;
        this.stopPreviewAudio();
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
        if (this.previewAudioSupplier != null) return;
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
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal(s).withStyle(Style.EMPTY.withColor(UIBase.getUITheme().description_area_text_color.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                            } else {
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("......").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().description_area_text_color.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                                TextScrollAreaEntry e2 = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("  ").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().description_area_text_color.getColorInt())), (entry) -> {});
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
        if (this.previewAudioSupplier != null) return;
        if (this.previewTextScrollArea == null) return;
        this.previewTextScrollArea.clearEntries();
        TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.translatable("fancymenu.ui.filechooser.no_preview").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().description_area_text_color.getColorInt())), (entry) -> {});
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
        if (this.audioPreviewToggleButton != null) {
            this.removeWidget(this.audioPreviewToggleButton);
        }
        this.audioPreviewToggleButton = new AudioPreviewToggleButton(0, 0, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_BUTTON_SIZE, button -> {
            this.togglePreviewAudio();
        });
        DrawableColor transparent = DrawableColor.of(new Color(0,0,0,0));
        this.audioPreviewToggleButton.setBackgroundColor(transparent, transparent, transparent, transparent, transparent, transparent);
        this.audioPreviewToggleButton.setLabelEnabled(false);
        this.audioPreviewToggleButton.setFocusable(false);
        this.audioPreviewToggleButton.setNavigatable(false);
        this.audioPreviewToggleButton.visible = false;
        this.audioPreviewToggleButton.active = false;
        this.addWidget(this.audioPreviewToggleButton);
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
        UIBase.applyDefaultWidgetSkinTo(this.audioPreviewVolumeSlider);
    }

    protected void togglePreviewAudio() {
        if (this.previewAudioSupplier == null) return;
        this.setPreviewAudioPlaying(!this.previewAudioPlaying);
    }

    protected void setPreviewAudio(@Nullable ResourceSupplier<IAudio> supplier, @Nullable Object previewKey) {
        this.stopPreviewAudio();
        this.previewAudioSupplier = supplier;
        this.previewAudioPlaying = false;
        this.previewAudioSeed = (previewKey != null) ? previewKey.hashCode() * 37L : System.nanoTime();
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

    protected void stopPreviewAudio() {
        if (this.currentPreviewAudio != null) {
            this.currentPreviewAudio.stop();
        }
        this.currentPreviewAudio = null;
        this.previewAudioPlaying = false;
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

    protected void renderAudioPreview(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int previewWidth = 200;
        int topY = 50 + 15;
        int availableHeight = (this.cancelButton.getY() - 50) - topY;
        int progressAreaHeight = AUDIO_PREVIEW_PROGRESS_BAR_SPACING + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + this.font.lineHeight;
        int basePreviewHeight = Math.max(40, availableHeight - (AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING + progressAreaHeight));
        int previewHeight = Math.max(12, basePreviewHeight / 3);
        int x = this.width - 20 - previewWidth;
        int y = topY;
        graphics.fill(x, y, x + previewWidth, y + previewHeight, UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt());
        this.renderAudioVisualizer(graphics, x + 4, y + 4, previewWidth - 8, previewHeight - 8);
        UIBase.renderBorder(graphics, x, y, x + previewWidth, y + previewHeight, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUITheme().ui_interface_widget_border_color.getColor(), true, true, true, true);
        int progressY = y + previewHeight + AUDIO_PREVIEW_PROGRESS_BAR_SPACING;
        this.renderAudioPreviewProgress(graphics, x, progressY, previewWidth);
        int controlsY = progressY + AUDIO_PREVIEW_PROGRESS_BAR_HEIGHT + AUDIO_PREVIEW_TIME_SPACING + this.font.lineHeight + AUDIO_PREVIEW_BUTTON_SPACING;
        int buttonX = x;
        int buttonY = controlsY;
        int sliderX = buttonX + AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING;
        int sliderWidth = Math.max(10, previewWidth - (AUDIO_PREVIEW_BUTTON_SIZE + AUDIO_PREVIEW_BUTTON_SPACING));
        int sliderY = controlsY + (AUDIO_PREVIEW_BUTTON_SIZE - AUDIO_PREVIEW_SLIDER_HEIGHT) / 2;
        this.renderAudioPreviewButton(graphics, mouseX, mouseY, partial, buttonX, buttonY);
        this.renderAudioPreviewVolumeSlider(graphics, mouseX, mouseY, partial, sliderX, sliderY, sliderWidth);
    }

    protected void renderAudioPreviewButton(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, int buttonX, int buttonY) {
        if (this.audioPreviewToggleButton == null) return;
        this.audioPreviewToggleButton.setX(buttonX);
        this.audioPreviewToggleButton.setY(buttonY);
        this.audioPreviewToggleButton.visible = true;
        this.audioPreviewToggleButton.active = (this.previewAudioSupplier != null);
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
        graphics.fill(barX, barY, barX + barWidth, barYEnd, UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt());
        UIBase.renderBorder(graphics, barX, barY, barX + barWidth, barYEnd, 1, UIBase.getUITheme().ui_interface_widget_border_color.getColor(), true, true, true, true);

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
        int textWidth = this.font.width(timeText);
        int textX = previewX + (previewWidth / 2) - (textWidth / 2);
        graphics.drawString(this.font, timeText, textX, timeY, UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt(), false);
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

    protected class AudioPreviewToggleButton extends ExtendedButton {

        public AudioPreviewToggleButton(int x, int y, int width, int height, @NotNull Button.OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.renderWidget(graphics, mouseX, mouseY, partial);
            ResourceLocation icon = this.getButtonTexture();
            RenderSystem.enableBlend();
            graphics.blit(icon, this.getX(), this.getY(), 0.0F, 0.0F, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_BUTTON_SIZE, AUDIO_PREVIEW_BUTTON_SIZE);
            UIBase.resetShaderColor(graphics);
        }

        protected ResourceLocation getButtonTexture() {
            if (this.isHoveredOrFocused()) {
                return AbstractBrowserScreen.this.previewAudioPlaying ? AUDIO_PREVIEW_PAUSE_BUTTON_TEXTURE_HOVER : AUDIO_PREVIEW_PLAY_BUTTON_TEXTURE_HOVER;
            }
            return AbstractBrowserScreen.this.previewAudioPlaying ? AUDIO_PREVIEW_PAUSE_BUTTON_TEXTURE : AUDIO_PREVIEW_PLAY_BUTTON_TEXTURE;
        }

    }

    public abstract static class AbstractIconTextScrollAreaEntry extends ScrollAreaEntry {

        protected static final int BORDER = 3;

        protected final Font font = Minecraft.getInstance().font;
        protected final MutableComponent entryNameComponent;
        protected long lastClick = -1;

        public AbstractIconTextScrollAreaEntry(@NotNull ScrollArea parent, @NotNull MutableComponent entryNameComponent) {
            super(parent, 100, 30);
            this.entryNameComponent = entryNameComponent;

            this.setWidth(this.font.width(this.entryNameComponent) + (BORDER * 2) + ICON_PIXEL_SIZE + 3);
            this.setHeight((BORDER * 2) + ICON_PIXEL_SIZE);

            this.playClickSound = false;
        }

        @NotNull
        protected abstract ResourceLocation getIconTexture();

        protected int getIconTextureWidth() {
            return ICON_PIXEL_SIZE;
        }

        protected int getIconTextureHeight() {
            return ICON_PIXEL_SIZE;
        }

        protected boolean isResourceUnfriendly() {
            return false;
        }

        protected int getTextColor() {
            if (this.isResourceUnfriendly()) {
                return UIBase.getUITheme().error_text_color.getColorInt();
            }
            return UIBase.getUITheme().description_area_text_color.getColorInt();
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            RenderSystem.enableBlend();

            int textureWidth = this.getIconTextureWidth();
            int textureHeight = this.getIconTextureHeight();
            if ((textureWidth == ICON_PIXEL_SIZE) && (textureHeight == ICON_PIXEL_SIZE)) {
                graphics.blit(this.getIconTexture(), (int)(this.x + BORDER), (int)(this.y + BORDER), 0.0F, 0.0F, ICON_PIXEL_SIZE, ICON_PIXEL_SIZE, ICON_PIXEL_SIZE, ICON_PIXEL_SIZE);
            } else {
                float scaleX = (float) ICON_PIXEL_SIZE / (float) textureWidth;
                float scaleY = (float) ICON_PIXEL_SIZE / (float) textureHeight;
                graphics.pose().pushPose();
                graphics.pose().translate(this.x + BORDER, this.y + BORDER, 0.0F);
                graphics.pose().scale(scaleX, scaleY, 1.0F);
                graphics.blit(this.getIconTexture(), 0, 0, 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
                graphics.pose().popPose();
            }

            graphics.drawString(this.font, this.entryNameComponent, (int)(this.x + BORDER + ICON_PIXEL_SIZE + 3), (int)(this.y + (this.height / 2) - (this.font.lineHeight / 2)), this.getTextColor(), false);

            if (this.isResourceUnfriendly() && this.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height) && this.parent.isMouseOverInnerArea(mouseX, mouseY)) {
                TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(Component.translatable("fancymenu.ui.filechooser.resource_name_check.not_passed.tooltip")), () -> true);
            }

        }

    }

}
