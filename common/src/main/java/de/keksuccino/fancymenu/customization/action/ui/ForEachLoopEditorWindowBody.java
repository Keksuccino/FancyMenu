package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.customization.action.ValuePlaceholderHolder;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ForEachExecutableBlock;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.variables.VariableType;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.screen.PiPCellStringBuilderWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lets the user choose the list variable of a FOR EACH loop. The name field suggests the list variables, and a line below it tells
 * what the loop would find right now: how many entries the list has, or why it would not run (a text variable, a missing variable,
 * or a {@code $$} reference to the list instead of its name).
 */
public class ForEachLoopEditorWindowBody extends PiPCellStringBuilderWindowBody {

    private static final int TEXT_LINE_SPACING = 4;

    @NotNull
    private final String initialListVariableName;
    private TextInputCell nameCell;
    private EditBoxSuggestions nameSuggestions;
    private WrappedTextCell statusCell;
    /** If the name is empty when the window opens, the field suggests all list variables as soon as it has its position. */
    private boolean suggestListsAfterLayout = false;
    private boolean resultHandled = false;

    /**
     * @param callback Gets the name of the list variable, or {@code null} if the user canceled. Gets called exactly once.
     */
    public ForEachLoopEditorWindowBody(@NotNull String listVariableName, @NotNull Consumer<String> callback) {
        super(Component.translatable("fancymenu.actions.blocks.for_each.edit"), callback);
        this.initialListVariableName = listVariableName;
    }

    @NotNull
    public static Pair<ForEachLoopEditorWindowBody, PiPWindow> openInWindow(@NotNull ForEachLoopEditorWindowBody body) {
        return Dialogs.openGeneric(body, body.getTitle(), null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
    }

    @Override
    protected void initCells() {

        String name = (this.nameCell != null) ? this.nameCell.getText() : this.initialListVariableName;

        this.addStartEndSpacerCell();

        this.addCell(new WrappedTextCell(Component.translatable("fancymenu.actions.blocks.for_each.edit.desc", this.buildPlaceholderComponent(ForEachExecutableBlock.LIST_ENTRY_PLACEHOLDER), this.buildPlaceholderComponent(ForEachExecutableBlock.LIST_INDEX_PLACEHOLDER))));

        this.addDescriptionEndSeparatorCell();

        this.addLabelCell(Component.translatable("fancymenu.actions.blocks.for_each.edit.list_name"));
        this.nameCell = this.addTextInputCell(null, true, true).setText(name);

        // Above the field, so the status line below it stays visible while typing
        this.nameSuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.nameCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, getListVariableNames());
        UIBase.applyDefaultWidgetSkinTo(this.nameSuggestions);
        // The field has no position yet, so the list the suggestions just built would show up in the wrong spot, see renderLateBody()
        this.nameSuggestions.setSuggestions(null);
        this.suggestListsAfterLayout = name.isBlank();
        this.nameCell.editBox.setResponder(s -> {
            this.nameSuggestions.updateCommandInfo();
            this.updateStatus();
        });

        this.statusCell = this.addCell(new WrappedTextCell(Component.empty()));
        this.updateStatus();

        this.addStartEndSpacerCell();

        // The name is the only thing to edit, so it gets the focus right away. Its text gets selected, so typing replaces it.
        // Selecting from the end to the start also keeps the start of the text visible while the field doesn't have its final width yet.
        this.selectCell(this.nameCell, true);
        this.nameCell.editBox.setCursorPosition(this.nameCell.editBox.getValue().length());
        this.nameCell.editBox.setHighlightPos(0);

    }

    @NotNull
    private MutableComponent buildPlaceholderComponent(@NotNull String placeholder) {
        return Component.literal(ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX + placeholder).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().info_color.getColorInt()));
    }

    @NotNull
    private static List<String> getListVariableNames() {
        List<String> names = new ArrayList<>(VariableHandler.getVariableNames(VariableType.LIST));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /**
     * Tells what the loop would find for the typed name right now. Names with placeholders only get resolved when the loop starts,
     * so for them it only says that, except for a {@code $$} reference to an existing variable, which is a common mistake: the
     * reference resolves to the value of the list, not to its name.
     */
    private void updateStatus() {
        UITheme theme = UIBase.getUITheme();
        String name = VariableHandler.normalizeVariableReferenceName(this.nameCell.getText());
        int neutralColor = UIBase.shouldBlur() ? theme.ui_blur_interface_widget_label_color_inactive.getColorInt() : theme.ui_interface_widget_label_color_inactive.getColorInt();
        if (name.isEmpty()) {
            this.statusCell.setText(Component.empty(), neutralColor);
            return;
        }
        String prefix = ValuePlaceholderHolder.VALUE_PLACEHOLDER_PREFIX;
        if (name.startsWith(prefix)) {
            String referencedName = VariableHandler.normalizeVariableReferenceName(name.substring(prefix.length()));
            if (!referencedName.isEmpty() && VariableHandler.variableExists(referencedName)) {
                this.statusCell.setText(Component.translatable("fancymenu.actions.blocks.for_each.edit.status.reference", Component.literal(referencedName)), theme.warning_color.getColorInt());
                return;
            }
        }
        if (PlaceholderParser.containsPlaceholders(name)) {
            this.statusCell.setText(Component.translatable("fancymenu.actions.blocks.for_each.edit.status.placeholders"), neutralColor);
            return;
        }
        VariableType type = VariableHandler.getVariableType(name);
        if (type == null) {
            this.statusCell.setText(Component.translatable("fancymenu.actions.blocks.for_each.edit.status.missing"), neutralColor);
        } else if (type == VariableType.TEXT) {
            this.statusCell.setText(Component.translatable("fancymenu.actions.blocks.for_each.edit.status.text"), theme.warning_color.getColorInt());
        } else {
            List<String> entries = VariableHandler.getListEntries(name);
            int size = (entries != null) ? entries.size() : 0;
            this.statusCell.setText(Component.translatable("fancymenu.actions.blocks.for_each.edit.status.list", Component.literal(Integer.toString(size))), theme.success_color.getColorInt());
        }
    }

    @Override
    public void renderLateBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        // The cells got their positions in this frame, so the suggestions can show up at the field now
        if (this.suggestListsAfterLayout) {
            this.suggestListsAfterLayout = false;
            this.nameSuggestions.updateCommandInfo();
        }
        this.nameSuggestions.extractRenderState(graphics, mouseX, mouseY);
    }

    @Override
    protected boolean isLateBodyHovered(int mouseX, int mouseY) {
        return this.nameSuggestions.isMouseOverList(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameSuggestions.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (this.nameSuggestions.mouseScrolled(scrollDeltaY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.nameSuggestions.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean allowDone() {
        return (this.nameCell != null) && !this.nameCell.getText().isBlank();
    }

    @Override
    public @NotNull String buildString() {
        return VariableHandler.normalizeVariableReferenceName(this.nameCell.getText());
    }

    @Override
    protected void onCancel() {
        this.closeWithResult(null);
    }

    @Override
    protected void onDone() {
        this.closeWithResult(this.buildString());
    }

    @Override
    public void onWindowClosedExternally() {
        this.handleResult(null);
    }

    private void closeWithResult(@Nullable String result) {
        if (this.handleResult(result)) this.closeWindow();
    }

    private boolean handleResult(@Nullable String result) {
        if (this.resultHandled) return false;
        this.resultHandled = true;
        this.callback.accept(result);
        return true;
    }

    /**
     * Text that wraps at the width of the cell list, so long translations never need horizontal scrolling.
     */
    private class WrappedTextCell extends RenderCell {

        @NotNull
        private Component text;
        private int color;
        @NotNull
        private List<MutableComponent> lines = List.of();
        /** The width the lines got wrapped at, or -1 if they need to get wrapped again. */
        private int wrappedWidth = -1;

        private WrappedTextCell(@NotNull Component text) {
            this.text = text;
            this.color = UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
            this.setSearchStringSupplier(() -> this.text.getString());
            this.setSelectable(false);
        }

        private void setText(@NotNull Component text, int color) {
            this.text = text;
            this.color = color;
            this.wrappedWidth = -1;
        }

        @Override
        public void renderCell(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            float lineY = this.getY();
            float lineHeight = UIBase.getUITextHeightNormal() + TEXT_LINE_SPACING;
            for (MutableComponent line : this.lines) {
                UIBase.renderText(graphics, line, this.getX(), lineY, this.color);
                lineY += lineHeight;
            }
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            int width = Math.max(20, (int)(ForEachLoopEditorWindowBody.this.scrollArea.getInnerWidth() - 40));
            if (width != this.wrappedWidth) {
                this.wrappedWidth = width;
                this.lines = this.text.getString().isEmpty() ? List.of() : UIBase.lineWrapUIComponentsNormal(this.text, width);
            }
            this.setWidth(width);
            // One line even without text, so the cells below don't jump when a status line appears
            int lineCount = Math.max(1, this.lines.size());
            this.setHeight((int)((lineCount * UIBase.getUITextHeightNormal()) + ((lineCount - 1) * TEXT_LINE_SPACING)));
        }

    }

}
