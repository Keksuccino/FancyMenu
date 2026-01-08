package de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.component.ComponentWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("all")
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

    protected static final long PREVIEW_DELAY_MS = 2000L;
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
    protected IText currentPreviewText;
    @Nullable
    protected Object pendingPreviewKey;
    @Nullable
    protected Object activePreviewKey;
    protected long pendingPreviewLoadAtMs = 0L;
    protected boolean previewPending = false;
    protected ExtendedButton confirmButton;
    protected ExtendedButton cancelButton;
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
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultWidgetSkinTo(this.confirmButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.onCancel();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

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
        this.onCancel();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();

        if (this.currentFileTypesComponent != null) {
            this.fileTypeScrollArea.horizontalScrollBar.active = (Minecraft.getInstance().font.width(this.currentFileTypesComponent) > (this.fileTypeScrollArea.getInnerWidth() - 10));
        }

        RenderSystem.enableBlend();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, this.getEntriesLabel(), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

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
        graphics.drawString(this.font, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

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
        graphics.drawString(this.font, FILE_TYPE_PREFIX_TEXT, (int)(this.fileTypeScrollArea.getXWithBorder() - Minecraft.getInstance().font.width(FILE_TYPE_PREFIX_TEXT) - 5), (int)(this.fileTypeScrollArea.getYWithBorder() + (this.fileTypeScrollArea.getHeightWithBorder() / 2) - (Minecraft.getInstance().font.lineHeight / 2)), UIBase.getUIColorTheme().element_label_color_normal.getColorInt(), false);
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
        this.tickTextPreview();
        if (this.previewTextureSupplier != null) {
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
                graphics.fill(x, y, x + w, y + h, UIBase.getUIColorTheme().area_background_color.getColorInt());
                RenderingUtils.resetShaderColor(graphics);
                RenderSystem.enableBlend();
                graphics.blit(loc, x, y, 0.0F, 0.0F, w, h, w, h);
                UIBase.resetShaderColor(graphics);
                UIBase.renderBorder(graphics, x, y, x + w, y + h, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUIColorTheme().element_border_color_normal.getColor(), true, true, true, true);
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
        graphics.fill(x + 1, y + 1, xEnd - 1, yEnd - 1, UIBase.getUIColorTheme().area_background_color.getColorInt());
        UIBase.renderBorder(graphics, x, y, xEnd, yEnd, 1, UIBase.getUIColorTheme().element_border_color_normal.getColor(), true, true, true, true);
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
            if (this.previewTextureSupplier == null && this.previewTextSupplier == null) {
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
        this.currentPreviewText = null;
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
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal(s).withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                            } else {
                                TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("......").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
                                e.setSelectable(false);
                                e.setBackgroundColorHover(e.getBackgroundColorNormal());
                                e.setPlayClickSound(false);
                                this.previewTextScrollArea.addEntry(e);
                                TextScrollAreaEntry e2 = new TextScrollAreaEntry(this.previewTextScrollArea, Component.literal("  ").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
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
        if (this.previewTextScrollArea == null) return;
        this.previewTextScrollArea.clearEntries();
        TextScrollAreaEntry e = new TextScrollAreaEntry(this.previewTextScrollArea, Component.translatable("fancymenu.ui.filechooser.no_preview").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
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

    public abstract class AbstractIconTextScrollAreaEntry extends ScrollAreaEntry {

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
                return UIBase.getUIColorTheme().error_text_color.getColorInt();
            }
            return UIBase.getUIColorTheme().description_area_text_color.getColorInt();
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
                TooltipHandler.INSTANCE.addTooltip(Tooltip.of(Component.translatable("fancymenu.ui.filechooser.resource_name_check.not_passed.tooltip")).setDefaultStyle(), () -> true, true, true);
            }

        }

    }

}
