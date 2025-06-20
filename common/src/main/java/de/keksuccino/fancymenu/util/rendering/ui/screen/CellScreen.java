package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("all")
public abstract class CellScreen extends Screen {

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

    @Override
    protected void init() {

        this.rightSideWidgets.clear();
        this.selectedCell = null;

        float oldScrollX = 0.0F;
        float oldScrollY = 0.0F;
        if (this.scrollArea != null) {
            oldScrollX = this.scrollArea.horizontalScrollBar.getScroll();
            oldScrollY = this.scrollArea.verticalScrollBar.getScroll();
        }
        this.scrollArea = new ScrollArea(20, 50 + 15, this.width - 40 - this.getRightSideWidgetWidth() - 20, this.height - 85);
        this.initCells();
        this.addWidget(this.scrollArea);
        this.scrollArea.horizontalScrollBar.setScroll(oldScrollX);
        this.scrollArea.verticalScrollBar.setScroll(oldScrollY);

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry ce) {
                ce.cell.updateSize(ce);
                ce.setHeight(ce.cell.getHeight());
            }
        }

        this.initRightSideWidgets();

        this.addRightSideDefaultSpacer();

        this.cancelButton = this.addRightSideButton(20, Component.translatable("fancymenu.guicomponents.cancel"), button -> {
            this.onCancel();
        });

        this.doneButton = this.addRightSideButton(20, Component.translatable("fancymenu.guicomponents.done"), button -> {
            if (this.allowDone()) this.onDone();
        }).setIsActiveSupplier(consumes -> this.allowDone());

        int widgetWidth = this.getRightSideWidgetWidth();
        int widgetX = this.width - 20 - widgetWidth;
        int widgetY = this.height - 20;
        AbstractWidget topRightSideWidget = null;
        for (AbstractWidget w : Lists.reverse(this.rightSideWidgets)) {
            if (!(w instanceof RightSideSpacer)) {
                UIBase.applyDefaultWidgetSkinTo(w);
                w.setX(widgetX);
                w.setY(widgetY - w.getHeight());
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
        if ((topRightSideWidget.getY() < 20) && (WindowHandler.getGuiScale() > 1)) {
            double newScale = WindowHandler.getGuiScale();
            newScale--;
            if (newScale < 1) newScale = 1;
            WindowHandler.setGuiScale(newScale);
            this.resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        } else if ((topRightSideWidget.getY() >= 20) && resized) {
            RenderingUtils.resetGuiScale();
            this.resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }

    }

    @Override
    protected void setInitialFocus() {
        //This fixes a crash related to the custom GUI scale handling in init()
    }

    protected abstract void onCancel();

    protected abstract void onDone();

    @Override
    public void onClose() {
        this.onCancel();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.updateSelectedCell();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.scrollArea.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
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
        this.scrollArea.addEntry(new CellScrollEntry(this.scrollArea, cell));
        return this.addWidget(cell);
    }

    protected void updateSelectedCell() {
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry c) {
                if (c.cell.selectable && c.cell.selected) {
                    this.selectedCell = c.cell;
                    return;
                }
            }
        }
        this.selectedCell = null;
    }

    @Nullable
    protected RenderCell getSelectedCell() {
        return this.selectedCell;
    }

    /**
     * This restores the old click logic.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener listener : this.children()) {
            if (listener.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(listener);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {

        if (keycode == InputConstants.KEY_ENTER) {
            if (this.allowDone()) this.onDone();
            return true;
        }

        return super.keyPressed(keycode, scancode, modifiers);

    }

    @Override
    public FocusNavigationEvent.ArrowNavigation createArrowEvent(ScreenDirection $$0) {
        return null;
    }

    @Override
    public FocusNavigationEvent.TabNavigation createTabEvent() {
        return null;
    }

    protected class CellScrollEntry extends ScrollAreaEntry {

        public final RenderCell cell;

        public CellScrollEntry(@NotNull ScrollArea parent, @NotNull RenderCell cell) {
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
                graphics.fill((int) this.getX(), (int) this.getY(), (int) (this.getX() + this.parent.getInnerWidth()), (int) (this.getY() + this.getHeight()), this.cell.hoverColorSupplier.get().getColorInt());
            }
            this.cell.render(graphics, mouseX, mouseY, partial);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
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
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.widget.setX(this.getX());
            this.widget.setY(this.getY());
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
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            UIBase.drawElementLabel(graphics, Minecraft.getInstance().font, this.text, this.getX(), this.getY());
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
        protected BiConsumer<String, TextInputCell> editorCallback = (s, cell) -> cell.editBox.setValue(s);
        protected ConsumingSupplier<TextInputCell, String> editorSetTextSupplier = consumes -> consumes.editBox.getValue();

        public TextInputCell(@Nullable CharacterFilter characterFilter, boolean allowEditor, boolean allowEditorPlaceholders) {

            this.allowEditor = allowEditor;

            this.editBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 20, 18, Component.empty());
            this.editBox.setMaxLength(100000);
            this.editBox.setCharacterFilter(characterFilter);
            UIBase.applyDefaultWidgetSkinTo(this.editBox);
            this.children().add(this.editBox);

            if (this.allowEditor) {
                this.openEditorButton = new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                    if (allowEditor) {
                        TextEditorScreen s = new TextEditorScreen((characterFilter != null) ? characterFilter : null, callback -> {
                            if (callback != null) {
                                this.editorCallback.accept(callback, this);
                            }
                            Minecraft.getInstance().setScreen(CellScreen.this);
                        });
                        s.setMultilineMode(false);
                        s.setPlaceholdersAllowed(allowEditorPlaceholders);
                        s.setText(this.editorSetTextSupplier.get(this));
                        Minecraft.getInstance().setScreen(s);
                    }
                });
                UIBase.applyDefaultWidgetSkinTo(this.openEditorButton);
                this.children().add(this.openEditorButton);
            }

        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            if (!this.widgetSizesSet) {
                this.setWidgetSizes();
                this.widgetSizesSet = true;
            }

            this.editBox.setX(this.getX() + 1);
            this.editBox.setY(this.getY() + 1);

            if (this.allowEditor) {
                this.openEditorButton.setX(this.getX() + this.getWidth() - this.openEditorButton.getWidth());
                this.openEditorButton.setY(this.getY());
            }

            if (MouseInput.isLeftMouseDown() && !this.editBox.isHovered()) {
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

    }

    public abstract class RenderCell extends AbstractContainerEventHandler implements Renderable, NarratableEntry, FancyMenuUiComponent {

        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected boolean selectable = false;
        protected boolean selected = false;
        protected boolean hovered = false;
        protected Supplier<DrawableColor> hoverColorSupplier = () -> UIBase.getUIColorTheme().list_entry_color_selected_hovered;
        protected final List<GuiEventListener> children = new ArrayList<>();
        protected final Map<String, String> memory = new HashMap<>();

        public abstract void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            if (!this.selectable) this.selected = false;

            this.renderCell(graphics, mouseX, mouseY, partial);

            for (GuiEventListener l : this.children) {
                if (l instanceof Renderable r) {
                    r.render(graphics, mouseX, mouseY, partial);
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
            this.setX((int)(scrollEntry.getX() + 20));
            this.setY((int)scrollEntry.getY());
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
            if (!this.selectable) this.selected = false;
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
                this.selected = true;
            } else {
                this.selected = false;
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

        @Override
        public void renderWidget(GuiGraphics graphics, int var2, int var3, float var4) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput var1) {
        }

    }

}
