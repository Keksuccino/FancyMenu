package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.variables.VariableType;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base of the windows the values of the list actions get edited in, so they all look and work the same: the name of the list variable
 * gets typed into a field that suggests the existing lists and tells when the name can't work, index fields explain how indexes work,
 * and {@link HintCell}s below fields explain what got typed in.
 */
public abstract class ListActionValueWindowBody extends PiPCellWindowBody {

    /**
     * The localizations all list action windows share. Not named LOCALIZATION_KEY like the keys of the actions, because the windows are
     * nested in their actions, and a field they inherit would hide the key of their action.
     */
    static final String LIST_LOCALIZATION_KEY = "fancymenu.actions.variables.list.edit";
    private static final String VARIABLE_REFERENCE_PREFIX = "$$";

    @NotNull
    private final Consumer<String> callback;
    @Nullable
    private EditBoxSuggestions listNameSuggestions;
    /** If the text of the index field of the window can work, see {@link AbstractListAction#isValidIndexInput(String, boolean)}. */
    private boolean indexInputValid = true;

    /**
     * @param callback Gets the edited value, or {@code null} if the editing got canceled.
     */
    protected ListActionValueWindowBody(@NotNull Component title, @NotNull Consumer<String> callback) {
        super(title);
        this.callback = Objects.requireNonNull(callback);
    }

    /**
     * @return The value the window edits, as it gets saved.
     */
    @NotNull
    protected abstract String serialize();

    /**
     * Adds the field for the name of the list variable, which suggests the names of the existing lists, with a hint below it that warns
     * about names that can't work as intended.
     */
    @NotNull
    protected TextInputCell addListNameCells(@NotNull String listName, @NotNull Consumer<String> listener) {
        this.addLabelCell(Component.translatable(LIST_LOCALIZATION_KEY + ".list_name"));
        TextInputCell cell = this.addTextInputCell(null, true, true)
                .setText(listName)
                .setTooltip(() -> UITooltip.of(Component.translatable(LIST_LOCALIZATION_KEY + ".list_name.desc")));
        HintCell hint = this.addHintCell().setHint(buildListNameHint(listName), true);
        List<String> listNames = VariableHandler.getVariableNames(VariableType.LIST);
        // Suggestions without any names of their own suggest what the server suggests for the chat, so they only exist if there are lists
        this.listNameSuggestions = listNames.isEmpty() ? null : EditBoxSuggestions.createWithCustomSuggestions(this, cell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.BELOW_EDIT_BOX, listNames);
        if (this.listNameSuggestions != null) UIBase.applyDefaultWidgetSkinTo(this.listNameSuggestions);
        cell.setEditListener(s -> {
            if (this.listNameSuggestions != null) this.listNameSuggestions.updateCommandInfo();
            hint.setHint(buildListNameHint(s), true);
            listener.accept(s);
        });
        return cell;
    }

    /**
     * Adds a field for an index, with a hint below it that explains how indexes work, or that the index is no whole number.
     *
     * @param optional If the index can be empty.
     */
    @NotNull
    protected TextInputCell addIndexCells(@NotNull Component label, @NotNull Component tooltip, @NotNull String index, boolean optional, @NotNull Consumer<String> listener) {
        this.addLabelCell(label);
        TextInputCell cell = this.addTextInputCell(null, true, true)
                .setText(index)
                .setTooltip(() -> UITooltip.of(tooltip));
        HintCell hint = this.addHintCell();
        this.updateIndexInput(hint, index, optional);
        cell.setEditListener(s -> {
            this.updateIndexInput(hint, s, optional);
            listener.accept(s);
        });
        return cell;
    }

    private void updateIndexInput(@NotNull HintCell hint, @NotNull String index, boolean optional) {
        this.indexInputValid = AbstractListAction.isValidIndexInput(index, optional);
        hint.setHint(Component.translatable(LIST_LOCALIZATION_KEY + (this.indexInputValid ? ".index.hint" : ".index.invalid")), !this.indexInputValid);
    }

    /**
     * @return If the text of the index field of the window can work. Only meaningful while the window shows an index field.
     */
    protected boolean isIndexInputValid() {
        return this.indexInputValid;
    }

    @NotNull
    protected HintCell addHintCell() {
        return this.addCell(new HintCell());
    }

    @Nullable
    private static Component buildListNameHint(@NotNull String listName) {
        String name = VariableHandler.normalizeVariableReferenceName(listName);
        // "$$" in front of the name of an existing variable makes the action use the value of that variable as name, which is easy to do by accident
        if (name.startsWith(VARIABLE_REFERENCE_PREFIX)) {
            String referencedName = name.substring(VARIABLE_REFERENCE_PREFIX.length());
            return VariableHandler.variableExists(referencedName) ? Component.translatable(LIST_LOCALIZATION_KEY + ".list_name.reference", referencedName) : null;
        }
        if (name.isEmpty() || PlaceholderParser.containsPlaceholders(name) || (VariableHandler.getVariableType(name) != VariableType.TEXT)) return null;
        String value = VariableHandler.getVariableValue(name);
        // List edits take over empty text variables, so only text variables with a value stay as they are
        return ((value != null) && !value.isEmpty()) ? Component.translatable(LIST_LOCALIZATION_KEY + ".list_name.text_variable", name) : null;
    }

    @Override
    public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.listNameSuggestions != null) this.listNameSuggestions.render(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((this.listNameSuggestions != null) && this.listNameSuggestions.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaY) {
        if ((this.listNameSuggestions != null) && this.listNameSuggestions.mouseScrolled(scrollDeltaY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollDeltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((this.listNameSuggestions != null) && this.listNameSuggestions.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.serialize());
    }

    /**
     * Small text below a field that explains it, or warns about what got typed in. It takes no space while it has nothing to say, so a
     * hint can come and go while typing without rebuilding the window.
     */
    public class HintCell extends RenderCell {

        private static final int TOP_SPACE = 3;
        private static final int BOTTOM_SPACE = 2;
        private static final float LINE_SPACING = 2.0F;

        @Nullable
        private Component hint;
        private boolean warning;
        /** The hint wrapped at {@link #wrappedWidth} with {@link #wrappedTextSize}, so it only gets wrapped again when one of them changes. */
        @NotNull
        private List<MutableComponent> lines = List.of();
        private int wrappedWidth = -1;
        private float wrappedTextSize = -1;

        public HintCell() {
            super.setSelectable(false);
        }

        /**
         * @param hint The hint, or {@code null} to show nothing.
         * @param warning If the hint warns about something, which shows it in the warning color.
         */
        @NotNull
        public HintCell setHint(@Nullable Component hint, boolean warning) {
            this.hint = hint;
            this.warning = warning;
            this.wrappedWidth = -1;
            return this;
        }

        @Override
        public int getTopBottomSpace() {
            return 0;
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            int width = (int) (ListActionValueWindowBody.this.scrollArea.getInnerWidth() - 40);
            this.setWidth(width);
            float textSize = UIBase.getUITextSizeSmall();
            if ((this.wrappedWidth != width) || (this.wrappedTextSize != textSize)) {
                this.lines = (this.hint != null) ? UIBase.lineWrapUIComponentsSmall(this.hint, Math.max(20, width)) : List.of();
                this.wrappedWidth = width;
                this.wrappedTextSize = textSize;
            }
            // The last line needs no spacing below it
            this.setHeight(this.lines.isEmpty() ? 0 : (TOP_SPACE + (int) Math.ceil((this.lines.size() * this.getLineHeight()) - LINE_SPACING) + BOTTOM_SPACE));
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (this.lines.isEmpty()) return;
            UITheme theme = UIBase.getUITheme();
            int color = this.warning ? theme.warning_color.getColorInt() : (UIBase.shouldBlur() ? theme.ui_blur_interface_widget_label_color_inactive.getColorInt() : theme.ui_interface_widget_label_color_inactive.getColorInt());
            float textSize = UIBase.getUITextSizeSmall();
            float lineHeight = this.getLineHeight();
            float y = this.getY() + TOP_SPACE;
            RenderingUtils.resetShaderColor(graphics);
            for (MutableComponent line : this.lines) {
                UIBase.renderText(graphics, line, this.getX(), y, color, textSize);
                y += lineHeight;
            }
            RenderingUtils.resetShaderColor(graphics);
        }

        private float getLineHeight() {
            return UIBase.getUITextHeightSmall() + LINE_SPACING;
        }

    }

}
