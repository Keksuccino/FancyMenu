package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public abstract class ConfiguratorScreen extends Screen {

    public ScrollArea scrollArea;
    public ExtendedButton cancelButton;
    public ExtendedButton doneButton;

    protected ConfiguratorScreen(@NotNull Component title) {
        super(title);
    }

    protected void initCells() {
    }

    protected void initWidgets() {
    }

    @Override
    protected final void init() {

        float oldScrollX = 0.0F;
        float oldScrollY = 0.0F;
        if (this.scrollArea != null) {
            oldScrollX = this.scrollArea.horizontalScrollBar.getScroll();
            oldScrollY = this.scrollArea.verticalScrollBar.getScroll();
        }
        this.scrollArea = new ScrollArea(20, 50 + 15, this.width - 40 - this.getRightSideButtonWidth() - 20, this.height - 85);
        this.scrollArea.makeEntriesWidthOfArea = true;
        this.scrollArea.minimumEntryWidthIsAreaWidth = true;
        this.initCells();
        this.addWidget(this.scrollArea);
        this.scrollArea.horizontalScrollBar.setScroll(oldScrollX);
        this.scrollArea.verticalScrollBar.setScroll(oldScrollY);

        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry ce) {
                ce.cell.updateSize();
                ce.setHeight(ce.cell.getHeight());
            }
        }

        int buttonWidth = this.getRightSideButtonWidth();
        int buttonX = this.width - 20 - buttonWidth;

        this.doneButton = new ExtendedButton(buttonX, this.height - 20 - 20, buttonWidth, 20, Component.translatable("fancymenu.guicomponents.done"), button -> {
            this.onDone();
        });
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);
        this.addWidget(this.doneButton);

        this.cancelButton = new ExtendedButton(buttonX, this.doneButton.getY() - 5 - 20, buttonWidth, 20, Component.translatable("fancymenu.guicomponents.cancel"), button -> {
           this.onCancel();
        });
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);
        this.addWidget(this.cancelButton);

        this.initWidgets();

    }

    protected abstract void onCancel();

    protected abstract void onDone();

    @Override
    public void onClose() {
        this.onCancel();
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(pose, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt());

        this.scrollArea.render(pose, mouseX, mouseY, partial);
        this.doneButton.render(pose, mouseX, mouseY, partial);
        this.cancelButton.render(pose, mouseX, mouseY, partial);

        super.render(pose, mouseX, mouseY, partial);

        //Render suggestions of TextInputCells
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry ce) {
                if (ce.cell instanceof TextInputCell t) {
                    t.suggestions.render(pose, mouseX, mouseY);
                }
            }
        }

    }

    @Override
    public void tick() {
        for (ScrollAreaEntry e : this.scrollArea.getEntries()) {
            if (e instanceof CellScrollEntry c) {
                c.cell.tick();
            }
        }
    }

    public int getRightSideButtonWidth() {
        return 150;
    }

    @NotNull
    protected TextInputCell addTextInputCell(@Nullable CharacterFilter characterFilter, boolean allowEditor, boolean allowEditorPlaceholders) {
        return this.addCell(new TextInputCell(characterFilter, allowEditor, allowEditorPlaceholders));
    }

    @NotNull
    protected ConfiguratorScreen.LabelCell addLabelCell(@NotNull Component text) {
        return this.addCell(new LabelCell(text));
    }

    @NotNull
    protected SpacerCell addSpacerCell(int height) {
        return this.addCell(new SpacerCell(height));
    }

    @NotNull
    protected <T> ButtonCell addCycleButtonCell(@NotNull ILocalizedValueCycle<T> cycle, boolean applyDefaultButtonSkin, CycleButton.CycleButtonClickFeedback<T> clickFeedback) {
        return this.addButtonCell(new CycleButton(0, 0, 20, 20, cycle, clickFeedback), applyDefaultButtonSkin);
    }

    @NotNull
    protected ButtonCell addButtonCell(@NotNull ExtendedButton button, boolean applyDefaultButtonSkin) {
        return this.addCell(new ButtonCell(button, applyDefaultButtonSkin));
    }

    @NotNull
    protected <T extends RenderCell> T addCell(@NotNull T cell) {
        this.scrollArea.addEntry(new CellScrollEntry(this.scrollArea, cell));
        return this.addWidget(cell);
    }

    @Override
    public boolean keyPressed(int keycode, int scancode, int modifiers) {

        if (keycode == InputConstants.KEY_ENTER) {
            this.onDone();
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

    protected static class CellScrollEntry extends ScrollAreaEntry {

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
        public void renderEntry(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.cell.updateSize();
            this.setHeight(this.cell.getHeight());
            this.cell.setX((int)(this.getX() + (this.getWidth() / 2) - (this.cell.getWidth() / 2)));
            this.cell.setY((int)this.getY());
            this.cell.render(pose, mouseX, mouseY, partial);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
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
        public void renderCell(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        }

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        }

        @Override
        public void updateSize() {
        }

    }

    public class ButtonCell extends RenderCell {

        public final ExtendedButton button;

        public ButtonCell(@NotNull ExtendedButton button, boolean applyDefaultSkin) {
            this.button = button;
            if (applyDefaultSkin) UIBase.applyDefaultWidgetSkinTo(this.button);
            this.children().add(this.button);
        }

        @Override
        public void renderCell(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.button.setX(this.getX());
            this.button.setY(this.getY());
            this.button.setWidth(this.getWidth());
        }

        @Override
        public void updateSize() {
            this.setWidth((int)(ConfiguratorScreen.this.scrollArea.getInnerWidth() - 40));
            this.setHeight(this.button.getHeight());
        }

    }

    public class LabelCell extends RenderCell {

        @NotNull
        public final Component text;

        public LabelCell(@NotNull Component label) {
            this.text = label;
        }

        @Override
        public void renderCell(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            RenderingUtils.resetShaderColor();
            UIBase.drawElementLabel(pose, Minecraft.getInstance().font, this.text, this.getX(), this.getY() + (this.getHeight() / 2) - (Minecraft.getInstance().font.lineHeight / 2));
            RenderingUtils.resetShaderColor();
        }

        @Override
        public void updateSize() {
            this.setWidth((int)(ConfiguratorScreen.this.scrollArea.getInnerWidth() - 40));
            this.setHeight(Minecraft.getInstance().font.lineHeight + 4);
        }

    }

    public class TextInputCell extends RenderCell {

        public ExtendedEditBox editBox;
        public ExtendedButton openEditorButton;
        public final boolean allowEditor;
        public EditBoxSuggestions suggestions;

        protected boolean widgetSizesSet = false;

        public TextInputCell(@Nullable CharacterFilter characterFilter, boolean allowEditor, boolean allowEditorPlaceholders) {

            this.allowEditor = allowEditor;

            this.editBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 20, 20, Component.empty());
            this.editBox.setMaxLength(100000);
            this.editBox.setResponder(this::onEdited);
            this.editBox.setCharacterFilter(characterFilter);
            UIBase.applyDefaultWidgetSkinTo(this.editBox);
            this.children().add(this.editBox);

            this.suggestions = new EditBoxSuggestions(Minecraft.getInstance(), ConfiguratorScreen.this, this.editBox, Minecraft.getInstance().font, false, true, 0, 7, false, Integer.MIN_VALUE);

            this.openEditorButton = new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                if (allowEditor) {
                    TextEditorScreen s = new TextEditorScreen((characterFilter != null) ? characterFilter.convertToLegacyFilter() : null, callback -> {
                        if (callback != null) {
                            this.editBox.setValue(callback);
                        }
                        Minecraft.getInstance().setScreen(ConfiguratorScreen.this);
                    });
                    s.setMultilineMode(false);
                    s.setPlaceholdersAllowed(allowEditorPlaceholders);
                    s.setText(this.editBox.getValue());
                    Minecraft.getInstance().setScreen(s);
                }
            });
            UIBase.applyDefaultWidgetSkinTo(this.openEditorButton);
            this.children().add(this.openEditorButton);

        }

        @Override
        public void renderCell(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            if (!this.widgetSizesSet) {
                this.setWidgetSizes();
                this.widgetSizesSet = true;
            }
            if (MouseInput.isLeftMouseDown() && !this.editBox.isHovered()) {
                this.editBox.setFocused(false);
            }
        }

        protected void setWidgetSizes() {

            int editorButtonWidth = (this.allowEditor ? Minecraft.getInstance().font.width(this.openEditorButton.getLabelSupplier().get(this.openEditorButton)) : 0) + 6;

            this.editBox.setX(this.getX());
            this.editBox.setY(this.getY());
            this.editBox.setWidth(this.allowEditor ? this.getWidth() - editorButtonWidth - 5 : this.getWidth());

            if (this.allowEditor) {
                this.openEditorButton.setX(this.getX() + this.getWidth() - editorButtonWidth);
                this.openEditorButton.setY(this.getY());
                this.openEditorButton.setWidth(editorButtonWidth);
            }

        }

        @Override
        public void tick() {
            this.editBox.tick();
        }

        protected void onEdited(String s) {
            this.suggestions.updateCommandInfo();
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

        public void setCustomSuggestions(@Nullable List<String> suggestions) {
            this.suggestions.setCustomSuggestions(suggestions);
        }

        public void enableOnlyCustomSuggestionsMode(boolean enabled) {
            this.suggestions.enableOnlyCustomSuggestionsMode(enabled);
        }

        public void enableSuggestions(boolean enabled) {
            this.suggestions.setAllowSuggestions(enabled);
            this.suggestions.updateCommandInfo();
        }

        public boolean mouseScrolled(double $$0, double $$1, double $$2) {
            return this.suggestions.mouseScrolled($$2) ? true : super.mouseScrolled($$0, $$1, $$2);
        }

        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            return this.suggestions.mouseClicked($$0, $$1, $$2) ? true : super.mouseClicked($$0, $$1, $$2);
        }

        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.suggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

    }

    public abstract class RenderCell extends AbstractContainerEventHandler implements Renderable, NarratableEntry {

        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected final List<GuiEventListener> children = new ArrayList<>();

        public abstract void renderCell(@NotNull PoseStack pose, int mouseX, int mouseY, float partial);

        @Override
        public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

            this.renderCell(pose, mouseX, mouseY, partial);

            for (GuiEventListener l : this.children) {
                if (l instanceof Renderable r) {
                    r.render(pose, mouseX, mouseY, partial);
                }
            }

        }

        public void tick() {
        }

        public void updateSize() {
            this.setWidth((int)(ConfiguratorScreen.this.scrollArea.getInnerWidth() - 40));
            this.setHeight(20);
        }

        public int getTopBottomSpace() {
            return 3;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y + this.getTopBottomSpace();
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height + (this.getTopBottomSpace() * 2);
        }

        public void setHeight(int height) {
            this.height = height;
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
            if (ConfiguratorScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public boolean mouseDragged(double $$0, double $$1, int $$2, double $$3, double $$4) {
            if (ConfiguratorScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            return super.mouseDragged($$0, $$1, $$2, $$3, $$4);
        }

        @Override
        public boolean mouseReleased(double $$0, double $$1, int $$2) {
            if (ConfiguratorScreen.this.scrollArea.isMouseInteractingWithGrabbers()) {
                return false;
            }
            return super.mouseReleased($$0, $$1, $$2);
        }

    }

}
