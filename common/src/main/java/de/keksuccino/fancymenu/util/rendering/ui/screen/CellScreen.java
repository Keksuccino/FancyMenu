package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("all")
public abstract class CellScreen extends Screen implements InitialWidgetFocusScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    public ScrollArea scrollArea;
    @Nullable
    protected RenderCell selectedCell;
    protected final List<AbstractWidget> rightSideWidgets = new ArrayList<>();
    @Nullable
    protected ExtendedButton doneButton;
    @Nullable
    protected ExtendedButton cancelButton;
    protected int lastWidth = 0;
    protected int lastHeight = 0;
    protected final List<RenderCell> allCells = new ArrayList<>();
    protected boolean searchBarEnabled = false;
    @Nullable
    protected ExtendedEditBox searchBar;
    @NotNull
    protected Component searchBarPlaceholder = Component.translatable("fancymenu.ui.generic.search");
    protected boolean descriptionAreaEnabled = false;
    @Nullable
    protected ScrollArea descriptionScrollArea;

    protected CellScreen(@NotNull Component title) {
        super(title);
    }

    /**
     * This is to add cells to the cell view.<br>
     * Gets called in {@link CellScreen#init()}, before {@link CellScreen#initRightSideWidgets()}.
     */
    protected void initCells() {
    }

    /**
     * This is for custom widgets that should get added to the right side.<br>
     * Gets called in {@link CellScreen#init()}.<br>
     * The {@link CellScreen#cancelButton} and {@link CellScreen#doneButton} are NOT INITIALIZED yet when this method gets called!
     */
    protected void initRightSideWidgets() {
    }

    public void rebuild() {
        this.resize(Minecraft.getInstance(), this.width, this.height);
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
     * Enable or disable the description area feature.
     * Should be called before {@link #init()} for proper initialization.
     */
    protected void setDescriptionAreaEnabled(boolean enabled) {
        this.descriptionAreaEnabled = enabled;
    }

    /**
     * Get the current description to display in the description area.
     * By default, returns the description of the selected cell.
     * Can be overridden for custom logic.
     */
    @Nullable
    protected List<Component> getCurrentDescription() {
        if (this.selectedCell != null) {
            Supplier<List<Component>> supplier = this.selectedCell.getDescriptionSupplier();
            if (supplier != null) {
                return supplier.get();
            }
        }
        return null;
    }

    /**
     * Updates the description area with the current description.
     * Called automatically when the selected cell changes.
     */
    protected void updateDescriptionArea() {

        if (this.descriptionScrollArea == null) return;
        this.descriptionScrollArea.clearEntries();

        this.descriptionScrollArea.addEntry(new SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

        List<Component> description = this.getCurrentDescription();
        if (description != null) {
            for (Component line : description) {
                this.addDescriptionLine(line);
            }
        }

        this.descriptionScrollArea.addEntry(new SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

    }

    protected void addDescriptionLine(@NotNull Component line) {
        List<Component> lines = new ArrayList<>();
        int maxWidth = (int)(this.descriptionScrollArea.getInnerWidth() - 15F);
        if (this.font.width(line) > maxWidth) {
            this.font.getSplitter().splitLines(line, maxWidth, Style.EMPTY).forEach(formatted -> {
                lines.add(TextFormattingUtils.convertFormattedTextToComponent(formatted));
            });
        } else {
            lines.add(line);
        }
        lines.forEach(component -> {
            TextScrollAreaEntry e = new TextScrollAreaEntry(this.descriptionScrollArea, component, (entry) -> {});
            e.setSelectable(false);
            e.setBackgroundColorHover(e.getBackgroundColorNormal());
            e.setPlayClickSound(false);
            e.setTextBaseColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt());
            this.descriptionScrollArea.addEntry(e);
        });
    }

    /**
     * Updates the cell list based on the search filter.
     * Only cells that match the search query are visible.
     */
    protected void updateCellsVisibility() {
        if (!this.searchBarEnabled || this.searchBar == null || this.scrollArea == null) return;

        String searchValue = this.searchBar.getValue();
        if (searchValue.isBlank()) searchValue = null;

        // Remember current scroll position
        float scrollX = this.scrollArea.horizontalScrollBar.getScroll();
        float scrollY = this.scrollArea.verticalScrollBar.getScroll();

        // Clear scroll area entries
        this.scrollArea.clearEntries();

        // Re-add only cells that match the search
        for (RenderCell cell : this.allCells) {
            if (this.cellMatchesSearch(cell, searchValue)) {
                CellScrollEntry entry = new CellScrollEntry(this.scrollArea, cell);
                this.scrollArea.addEntry(entry);
                // Update cell size
                cell.updateSize(entry);
                entry.setHeight(cell.getHeight());
            }
        }

        // Restore scroll position
        this.scrollArea.horizontalScrollBar.setScroll(scrollX);
        this.scrollArea.verticalScrollBar.setScroll(scrollY);
    }

    /**
     * Check if a cell matches the search query.
     * Searches both the cell's search string and its description (if any).
     */
    protected boolean cellMatchesSearch(@NotNull RenderCell cell, @Nullable String searchValue) {
        if (searchValue == null || searchValue.isBlank()) return true;

        if (cell.ignoreSearch) return true;

        String searchLower = searchValue.toLowerCase();

        // Check the cell's search string
        String cellSearchString = cell.getSearchString();
        if (cellSearchString != null && cellSearchString.toLowerCase().contains(searchLower)) {
            return true;
        }

        // Check the cell's description
        Supplier<List<Component>> descSupplier = cell.getDescriptionSupplier();
        if (descSupplier != null) {
            List<Component> description = descSupplier.get();
            if (description != null) {
                for (Component c : description) {
                    if (c.getString().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected void init() {

        this.rightSideWidgets.clear();
        this.allCells.clear();
        this.selectedCell = null;

        // Calculate scroll area dimensions based on enabled features
        int scrollAreaX = 20;
        int scrollAreaY = 50 + 15;
        int scrollAreaWidth = this.width - 40 - this.getRightSideWidgetWidth() - 20;
        int scrollAreaHeight = this.height - 85;

        // Adjust for description area if enabled
        if (this.descriptionAreaEnabled) {
            scrollAreaWidth = (this.width / 2) - 40;

            // Initialize description area
            this.descriptionScrollArea = new ScrollArea(0, 0, 0, 0);
            this.descriptionScrollArea.setWidth((this.width / 2) - 40, true);
            this.descriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
            this.descriptionScrollArea.setX(this.width - 20 - this.descriptionScrollArea.getWidthWithBorder(), true);
            this.descriptionScrollArea.setY(50 + 15, true);
            this.descriptionScrollArea.horizontalScrollBar.active = false;
            this.addRenderableWidget(this.descriptionScrollArea);
        }

        // Adjust for search bar if enabled
        if (this.searchBarEnabled) {
            scrollAreaY += 25; // Make room for search bar
            scrollAreaHeight -= 25;

            // Initialize search bar
            String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
            this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, scrollAreaX + 1, 50 + 15 + 1, scrollAreaWidth - 2, 20 - 2, Component.empty());
            if (CellScreen.this.searchBarPlaceholder != null) {
                this.searchBar.setHintFancyMenu(consumes -> CellScreen.this.searchBarPlaceholder);
            }
            this.searchBar.setValue(oldSearchValue);
            this.searchBar.setResponder(s -> CellScreen.this.updateCellsVisibility());
            UIBase.applyDefaultWidgetSkinTo(this.searchBar);
            this.searchBar.setMaxLength(100000);
            this.addRenderableWidget(this.searchBar);
            this.setupInitialFocusWidget(this, this.searchBar);
        }

        float oldScrollX = 0.0F;
        float oldScrollY = 0.0F;
        if (this.scrollArea != null) {
            oldScrollX = this.scrollArea.horizontalScrollBar.getScroll();
            oldScrollY = this.scrollArea.verticalScrollBar.getScroll();
        }
        this.scrollArea = new de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea(scrollAreaX, scrollAreaY, scrollAreaWidth, scrollAreaHeight);
        this.initCells();
        this.addWidget(this.scrollArea);
        this.scrollArea.horizontalScrollBar.setScroll(oldScrollX);
        this.scrollArea.verticalScrollBar.setScroll(oldScrollY);

        for (de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry ce) {
                ce.cell.updateSize(ce);
                ce.setHeight(ce.cell.getHeight());
            }
        }

        this.initRightSideWidgets();

        this.addRightSideDefaultSpacer();

        this.cancelButton = this.addRightSideButton(20, Component.translatable("fancymenu.common_components.cancel"), button -> {
            this.onCancel();
        });

        this.doneButton = this.addRightSideButton(20, Component.translatable("fancymenu.common_components.done"), button -> {
            if (this.allowDone()) this.onDone();
        }).setIsActiveSupplier(consumes -> this.allowDone());

        int widgetWidth = this.getRightSideWidgetWidth();
        int widgetX = this.width - 20 - widgetWidth;
        int widgetY = this.height - 20;
        AbstractWidget topRightSideWidget = null;
        for (AbstractWidget w : Lists.reverse(this.rightSideWidgets)) {
            if (!(w instanceof RightSideSpacer)) {
                UIBase.applyDefaultWidgetSkinTo(w);
                w.x = (widgetX);
                w.y = (widgetY - w.getHeight());
                w.setWidth(widgetWidth);
                this.addRenderableWidget(w);
                topRightSideWidget = w;
            }
            widgetY -= w.getHeight() + this.getRightSideDefaultSpaceBetweenWidgets();
        }

        Window window = Minecraft.getInstance().getWindow();
        boolean resized = (window.getScreenWidth() != this.lastWidth) || (window.getScreenHeight() != this.lastHeight);
        this.lastWidth = window.getScreenWidth();
        this.lastHeight = window.getScreenHeight();

        //Adjust GUI scale to make all right-side buttons fit in the screen
        if ((topRightSideWidget != null) && (topRightSideWidget.getY() < 20) && (window.getGuiScale() > 1)) {
            double newScale = window.getGuiScale();
            newScale--;
            if (newScale < 1) newScale = 1;
            window.setGuiScale(newScale);
            this.resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        } else if ((topRightSideWidget != null) && (topRightSideWidget.getY() >= 20) && resized) {
            RenderingUtils.resetGuiScale();
            this.resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }

        if (this.descriptionAreaEnabled) {
            this.updateDescriptionArea();
        }

    }

    protected abstract void onCancel();

    protected abstract void onDone();

    @Override
    public void onClose() {
        this.onCancel();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.updateSelectedCell();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        if (this.descriptionAreaEnabled && (this.descriptionScrollArea != null)) {
            this.descriptionScrollArea.render(graphics, mouseX, mouseY, partial);
        }

        this.scrollArea.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

        this.performInitialWidgetFocusActionInRender();

    }

    @Override
    public void tick() {
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry c) {
                c.cell.tick();
            }
        }
    }

    public int getRightSideWidgetWidth() {
        return 150;
    }

    public int getRightSideDefaultSpaceBetweenWidgets() {
        return 5;
    }

    public boolean allowDone() {
        return true;
    }

    public boolean allowEnterForDone() {
        return true;
    }

    protected void addRightSideDefaultSpacer() {
        this.addRightSideSpacer(5);
    }

    protected void addRightSideSpacer(int height) {
        this.rightSideWidgets.add(new RightSideSpacer(height));
    }

    protected <T> CycleButton<T> addRightSideCycleButton(int height, @NotNull ILocalizedValueCycle<T> cycle, @NotNull CycleButton.CycleButtonClickFeedback<T> clickFeedback) {
        return this.addRightSideWidget(new CycleButton<>(0, 0, 0, height, cycle, clickFeedback));
    }

    protected ExtendedButton addRightSideButton(int height, @NotNull Component label, @NotNull Consumer<ExtendedButton> onClick) {
        return this.addRightSideWidget(new ExtendedButton(0, 0, 0, height, label, var1 -> {
            onClick.accept((ExtendedButton) var1);
        }));
    }

    protected <T extends AbstractWidget> T addRightSideWidget(@NotNull T widget) {
        this.rightSideWidgets.add(widget);
        return widget;
    }

    @NotNull
    protected TextInputCell addTextInputCell(@Nullable CharacterFilter characterFilter, boolean allowEditor, boolean allowEditorPlaceholders) {
        return this.addCell(new TextInputCell(characterFilter, allowEditor, allowEditorPlaceholders));
    }

    @NotNull
    protected CellScreen.LabelCell addLabelCell(@NotNull Component text) {
        return this.addCell(new LabelCell(text));
    }

    protected void addDescriptionEndSeparatorCell() {
        this.addSpacerCell(5);
        this.addSeparatorCell();
        this.addSpacerCell(5);
    }

    @NotNull
    protected SeparatorCell addSeparatorCell(int height) {
        return this.addCell(new SeparatorCell(height));
    }

    @NotNull
    protected SeparatorCell addSeparatorCell() {
        return this.addCell(new SeparatorCell());
    }

    @NotNull
    protected SpacerCell addCellGroupEndSpacerCell() {
        return this.addSpacerCell(7);
    }

    @NotNull
    protected SpacerCell addStartEndSpacerCell() {
        return this.addSpacerCell(20);
    }

    @NotNull
    protected SpacerCell addSpacerCell(int height) {
        return this.addCell(new SpacerCell(height));
    }

    @NotNull
    protected <T> CellScreen.WidgetCell addCycleButtonCell(@NotNull ILocalizedValueCycle<T> cycle, boolean applyDefaultButtonSkin, CycleButton.CycleButtonClickFeedback<T> clickFeedback) {
        return this.addWidgetCell(new CycleButton(0, 0, 20, 20, cycle, clickFeedback), applyDefaultButtonSkin);
    }

    @NotNull
    protected CellScreen.WidgetCell addWidgetCell(@NotNull AbstractWidget widget, boolean applyDefaultButtonSkin) {
        return this.addCell(new WidgetCell(widget, applyDefaultButtonSkin));
    }

    @NotNull
    protected <T extends RenderCell> T addCell(@NotNull T cell) {
        // Always add to the complete list of cells
        this.allCells.add(cell);

        // Only add to scroll area if it matches the search filter (or if search is disabled)
        if (cell.ignoreSearch || (!this.searchBarEnabled || this.searchBar == null || this.cellMatchesSearch(cell, this.searchBar.getValue()))) {
            CellScrollEntry entry = new CellScrollEntry(this.scrollArea, cell);
            this.scrollArea.addEntry(entry);
        }

        return this.addWidget(cell);
    }

    protected void updateSelectedCell() {
        RenderCell last = this.selectedCell;
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry c) {
                if (c.cell.selectable && c.cell.selected) {
                    this.selectedCell = c.cell;
                    if (last != this.selectedCell) {
                        this.updateDescriptionArea();
                    }
                    return;
                }
            }
        }
        this.selectedCell = null;
        if (last != this.selectedCell) {
            this.updateDescriptionArea();
        }
    }

    @Nullable
    protected RenderCell getSelectedCell() {
        return this.selectedCell;
    }

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        if (keycode == InputConstants.KEY_ENTER) {
            if (this.allowDone() && this.allowEnterForDone()) {
                this.onDone();
                return true;
            }
        }
        return super.keyPressed(keycode, scancode, modifiers);
    }

    @Override
    public boolean mouseClicked(double $$0, double $$1, int $$2) {
        if (this.searchBarEnabled && (this.searchBar != null) && !this.searchBar.isHovered()) {
            this.searchBar.setFocused(false);
        }
        return super.mouseClicked($$0, $$1, $$2);
    }

    @Override
    public FocusNavigationEvent.ArrowNavigation createArrowEvent(ScreenDirection $$0) {
        return null;
    }

    @Override
    public FocusNavigationEvent.TabNavigation createTabEvent() {
        return null;
    }

    protected class CellScrollEntry extends de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry {

        public final RenderCell cell;

        public CellScrollEntry(@NotNull de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea parent, @NotNull RenderCell cell) {
            super(parent, 10, 10);
            this.clickable = false;
            this.selectable = false;
            this.selectOnClick = false;
            this.playClickSound = false;
            this.setBackgroundColorHover(this.getBackgroundColorNormal());
            this.cell = cell;
        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.cell.updateSize(this);
            this.setWidth(this.cell.getWidth() + 40);
            if (this.getWidth() < this.parent.getInnerWidth()) this.setWidth(this.parent.getInnerWidth());
            this.setHeight(this.cell.getHeight());
            this.cell.updatePosition(this);
            //Use the scroll entry position and size to check for cell hover, to cover the whole cell line and not just the (sometimes too small) actual cell size
            this.cell.hovered = UIBase.isXYInArea(mouseX, mouseY, this.getX(), this.getY(), this.parent.getInnerWidth(), this.getHeight());
            if ((cell.isSelectable() && cell.isHovered()) || (cell == CellScreen.this.selectedCell)) {
                RenderingUtils.resetShaderColor(graphics);
                graphics.fill((int) this.getX(), (int) this.getY(), (int) (this.getX() + this.parent.getInnerWidth()), (int) (this.getY() + this.getHeight()), this.cell.hoverColorSupplier.get().getColorInt());
                RenderingUtils.resetShaderColor(graphics);
            }
            this.cell.render(graphics, mouseX, mouseY, partial);
        }

        @Override
        public void onClick(de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }

    }

    public class SeparatorCell extends RenderCell {

        protected Supplier<DrawableColor> separatorColorSupplier = () -> UIBase.getUIColorTheme().element_border_color_normal;
        protected int separatorThickness = 1;

        public SeparatorCell() {
            this.setHeight(10);
        }

        public SeparatorCell(int height) {
            this.setHeight(height);
        }

        @Override
        public int getTopBottomSpace() {
            return 0;
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            int centerY = this.getY() + (this.getHeight() / 2);
            int halfThickness = Math.max(1, this.separatorThickness / 2);
            graphics.fill(this.getX(), centerY - ((halfThickness > 1) ? halfThickness : 0), this.getX() + this.getWidth(), centerY + halfThickness, this.separatorColorSupplier.get().getColorInt());
            RenderingUtils.resetShaderColor(graphics);
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int)(CellScreen.this.scrollArea.getInnerWidth() - 40));
        }

        @NotNull
        public Supplier<DrawableColor> getSeparatorColorSupplier() {
            return this.separatorColorSupplier;
        }

        public SeparatorCell setSeparatorColorSupplier(@NotNull Supplier<DrawableColor> separatorColorSupplier) {
            this.separatorColorSupplier = separatorColorSupplier;
            return this;
        }

        public int getSeparatorThickness() {
            return this.separatorThickness;
        }

        public SeparatorCell setSeparatorThickness(int separatorThickness) {
            this.separatorThickness = separatorThickness;
            return this;
        }

    }

    public class SpacerCell extends RenderCell {

        public SpacerCell(int height) {
            this.setHeight(height);
            this.setWidth(10);
        }

        @Override
        public int getTopBottomSpace() {
            return 0;
        }

        @Override
        public boolean isSelectable() {
            return false;
        }

        @Override
        public RenderCell setSelectable(boolean selectable) {
            throw new RuntimeException("You can't make SpacerCells selectable.");
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
        }

    }

    public class WidgetCell extends RenderCell {

        public final AbstractWidget widget;

        public WidgetCell(@NotNull AbstractWidget widget, boolean applyDefaultSkin) {
            this.widget = widget;
            if (applyDefaultSkin) UIBase.applyDefaultWidgetSkinTo(this.widget);
            this.children().add(this.widget);
            this.setSearchStringSupplier(() -> this.widget.getMessage().getString());
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.widget.x = (this.getX());
            this.widget.y = (this.getY());
            this.widget.setWidth(this.getWidth());
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int)(CellScreen.this.scrollArea.getInnerWidth() - 40));
            this.setHeight(this.widget.getHeight());
        }

    }

    public class LabelCell extends RenderCell {

        @NotNull
        protected Component text;

        public LabelCell(@NotNull Component label) {
            this.text = label;
            this.setSearchStringSupplier(() -> this.text.getString());
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderingUtils.resetShaderColor(graphics);
            UIBase.drawElementLabel(graphics, Minecraft.getInstance().font, this.text, this.getX(), this.getY());
            RenderingUtils.resetShaderColor(graphics);
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth(Minecraft.getInstance().font.width(this.text));
            this.setHeight(Minecraft.getInstance().font.lineHeight);
        }

        @NotNull
        public Component getText() {
            return this.text;
        }

        public LabelCell setText(@NotNull Component text) {
            this.text = text;
            return this;
        }

    }

    public class TextInputCell extends RenderCell {

        public ExtendedEditBox editBox;
        public ExtendedButton openEditorButton;
        public final boolean allowEditor;
        protected boolean widgetSizesSet = false;
        protected BiConsumer<String, TextInputCell> editorCallback = (s, cell) -> cell.editBox.setValue(s.replace("\n", "\\n"));
        protected ConsumingSupplier<TextInputCell, String> editorSetTextSupplier = consumes -> {
            if (this.editorMultiLineMode) {
                return consumes.editBox.getValue().replace("\\n", "\n");
            }
            return consumes.editBox.getValue().replace("\n", "\\n");
        };
        protected boolean editorMultiLineMode = false;

        public TextInputCell(@Nullable CharacterFilter characterFilter, boolean allowEditor, boolean allowEditorPlaceholders) {

            this.allowEditor = allowEditor;

            this.editBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 20, 18, Component.empty());
            this.editBox.setMaxLength(1000000);
            this.editBox.setCharacterFilter(characterFilter);
            UIBase.applyDefaultWidgetSkinTo(this.editBox);
            this.children().add(this.editBox);

            if (this.allowEditor) {
                this.openEditorButton = new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                    if (allowEditor) {
                        TextEditorScreen s = new TextEditorScreen((characterFilter != null) ? characterFilter.convertToLegacyFilter() : null, callback -> {
                            if (callback != null) {
                                this.editorCallback.accept(callback, this);
                            }
                            Minecraft.getInstance().setScreen(CellScreen.this);
                        });
                        s.setMultilineMode(this.editorMultiLineMode);
                        s.setPlaceholdersAllowed(allowEditorPlaceholders);
                        s.setText(this.editorSetTextSupplier.get(this));
                        Minecraft.getInstance().setScreen(s);
                    }
                });
                UIBase.applyDefaultWidgetSkinTo(this.openEditorButton);
                this.children().add(this.openEditorButton);
            }

            this.setSearchStringSupplier(() -> this.editBox.getValue());

        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            if (!this.widgetSizesSet) {
                this.setWidgetSizes();
                this.widgetSizesSet = true;
            }

            this.editBox.x = (this.getX() + 1);
            this.editBox.y = (this.getY() + 1);

            if (this.allowEditor) {
                this.openEditorButton.x = (this.getX() + this.getWidth() - this.openEditorButton.getWidth());
                this.openEditorButton.y = (this.getY());
            }

            if (MouseInput.isLeftMouseDown() && !((IMixinAbstractWidget)this.editBox).getIsHoveredFancyMenu()) {
                this.editBox.setFocused(false);
            }

        }

        protected void setWidgetSizes() {

            int editorButtonWidth = (this.allowEditor ? Minecraft.getInstance().font.width(this.openEditorButton.getLabelSupplier().get(this.openEditorButton)) : 0) + 6;

            this.editBox.setWidth(this.allowEditor ? this.getWidth() - editorButtonWidth - 5 : this.getWidth());

            if (this.allowEditor) {
                this.openEditorButton.setWidth(editorButtonWidth);
            }

        }

        @Override
        public void tick() {
            this.editBox.tick();
        }

        public TextInputCell setEditorPresetTextSupplier(@NotNull ConsumingSupplier<TextInputCell, String> supplier) {
            this.editorSetTextSupplier = Objects.requireNonNull(supplier);
            return this;
        }

        public TextInputCell setEditorCallback(@NotNull BiConsumer<String, TextInputCell> callback) {
            this.editorCallback = Objects.requireNonNull(callback);
            return this;
        }

        public TextInputCell setEditListener(@Nullable Consumer<String> listener) {
            this.editBox.setResponder(listener);
            return this;
        }

        @NotNull
        public String getText() {
            return this.editBox.getValue();
        }

        public TextInputCell setText(@Nullable String text) {
            if (text == null) text = "";
            this.editBox.setValue(text);
            this.editBox.setCursorPosition(0);
            this.editBox.setHighlightPos(0);
            this.editBox.setDisplayPosition(0);
            return this;
        }

        public boolean isEditorMultiLineMode() {
            return editorMultiLineMode;
        }

        public TextInputCell setEditorMultiLineMode(boolean editorMultiLineMode) {
            this.editorMultiLineMode = editorMultiLineMode;
            return this;
        }

    }

    public abstract class RenderCell extends AbstractContainerEventHandler implements Renderable, NarratableEntry {

        protected int x;
        protected int y;
        protected int width;
        protected int height;
        private boolean selectable = false;
        private boolean selected = false;
        protected boolean hovered = false;
        protected Supplier<DrawableColor> hoverColorSupplier = () -> UIBase.getUIColorTheme().list_entry_color_selected_hovered;
        @Nullable
        protected Supplier<List<Component>> descriptionSupplier = null;
        @NotNull
        protected Supplier<String> searchStringSupplier = () -> null;
        protected final List<GuiEventListener> children = new ArrayList<>();
        protected final Map<String, String> memory = new HashMap<>();
        protected boolean ignoreSearch = false;

        public abstract void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            if (!this.selectable) this.selected = false;

            this.renderCell(graphics, mouseX, mouseY, partial);

            for (GuiEventListener l : this.children) {
                if (l instanceof Renderable r) {
                    r.render(graphics, mouseX, mouseY, partial);
                }
                if (l instanceof Widget r) {
                    r.render(graphics.pose(), mouseX, mouseY, partial);
                }
            }

        }

        public void tick() {
        }

        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int)(CellScreen.this.scrollArea.getInnerWidth() - 40));
            this.setHeight(20);
        }

        protected void updatePosition(@NotNull CellScrollEntry scrollEntry) {
            this.x = ((int)(scrollEntry.getX() + 20));
            this.y = ((int)scrollEntry.getY());
        }

        /**
         * Returns a string used for searching this cell.
         * Return null to exclude this cell from search filtering.
         * By default, returns null.
         */
        @Nullable
        public String getSearchString() {
            return this.searchStringSupplier.get();
        }

        public @NotNull Supplier<String> getSearchStringSupplier() {
            return searchStringSupplier;
        }

        public RenderCell setSearchStringSupplier(@NotNull Supplier<String> searchStringSupplier) {
            this.searchStringSupplier = searchStringSupplier;
            return this;
        }

        /**
         * Get the description supplier for this cell.
         * Returns null if no description is set.
         */
        @Nullable
        public Supplier<List<Component>> getDescriptionSupplier() {
            return this.descriptionSupplier;
        }

        /**
         * Set the description supplier for this cell.
         * The supplier should return a list of text components to display in the description area.
         * Set to null to remove the description.
         */
        public RenderCell setDescriptionSupplier(@Nullable Supplier<List<Component>> descriptionSupplier) {
            this.descriptionSupplier = descriptionSupplier;
            return this;
        }

        public int getTopBottomSpace() {
            return 3;
        }

        public int getX() {
            return x;
        }

        public RenderCell setX(int x) {
            this.x = x;
            return this;
        }

        public int getY() {
            return y + this.getTopBottomSpace();
        }

        public RenderCell setY(int y) {
            this.y = y;
            return this;
        }

        public int getWidth() {
            return width;
        }

        public RenderCell setWidth(int width) {
            this.width = width;
            return this;
        }

        public int getHeight() {
            return height + (this.getTopBottomSpace() * 2);
        }

        public RenderCell setHeight(int height) {
            this.height = height;
            return this;
        }

        public boolean isHovered() {
            return this.hovered;
        }

        public RenderCell setSelected(boolean selected) {
            this.selected = selected;
            if (!this.selectable) this.selected = false;
            // Update description area if enabled and selection changed
            if (CellScreen.this.descriptionAreaEnabled) {
                CellScreen.this.updateDescriptionArea();
            }
            return this;
        }

        public boolean isSelected() {
            return this.selected;
        }

        public boolean isSelectable() {
            return this.selectable;
        }

        public RenderCell setSelectable(boolean selectable) {
            this.selectable = selectable;
            if (!this.selectable) this.setSelected(false);
            return this;
        }

        public RenderCell setHoverColorSupplier(@NotNull Supplier<DrawableColor> hoverColorSupplier) {
            this.hoverColorSupplier = hoverColorSupplier;
            return this;
        }

        public RenderCell putMemoryValue(@NotNull String key, @NotNull String value) {
            this.memory.put(key, value);
            return this;
        }

        public RenderCell setIgnoreSearch() {
            this.ignoreSearch = true;
            return this;
        }

        @Nullable
        public String getMemoryValue(@NotNull String key) {
            return this.memory.get(key);
        }

        @Override
        public @NotNull List<GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NotNull NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput var1) {
        }

        @Override
        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (CellScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            if (!CellScreen.this.scrollArea.isInnerAreaHovered()) {
                return false;
            }
            if (this.hovered && this.selectable) {
                this.setSelected(true);
            } else {
                this.setSelected(false);
            }
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public boolean mouseDragged(double $$0, double $$1, int $$2, double $$3, double $$4) {
            if (CellScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            return super.mouseDragged($$0, $$1, $$2, $$3, $$4);
        }

        @Override
        public boolean mouseReleased(double $$0, double $$1, int $$2) {
            if (CellScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            return super.mouseReleased($$0, $$1, $$2);
        }

    }

    protected class RightSideSpacer extends AbstractWidget {

        protected RightSideSpacer(int height) {
            super(0, 0, 0, height, Component.empty());
        }

        @Override
        public void setFocused(boolean var1) {
        }

        public final void renderWidget(GuiGraphics graphics, int var2, int var3, float var4) {
        }

        @Override
        public final void renderButton(PoseStack graphics, int var2, int var3, float var4) {
            this.renderWidget(GuiGraphics.currentGraphics(), var2, var3, var4);
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public void updateNarration(NarrationElementOutput var1) {
        }

    }

    public static class SpacerScrollAreaEntry extends TextScrollAreaEntry {

        private int spacerHeight;

        public SpacerScrollAreaEntry(ScrollArea parent, int height) {
            super(parent, Component.empty(), button -> {});
            this.spacerHeight = height;
            this.height = height;
        }

        @Override
        public float getHeight() {
            return this.spacerHeight;
        }

        @Override
        public void setHeight(float height) {
            this.spacerHeight = (int) height;
        }

    }

}