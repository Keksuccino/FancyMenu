package de.keksuccino.fancymenu.customization.requirement.requirements;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.customization.variables.VariableLists;
import de.keksuccino.fancymenu.customization.variables.VariableType;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Checks if a list variable has an entry that matches a text, optionally ignoring case. Missing variables and text variables never
 * have the entry. The size of a list can get checked with the List Size placeholder and the Is Number requirement.
 */
public class ListContainsEntryRequirement extends Requirement {

    @NotNull
    private final Function<String, List<String>> listLookup;

    public ListContainsEntryRequirement() {
        this(VariableHandler::getListEntries);
    }

    /**
     * @param listLookup Returns the entries of the list variable with the given name, or null if there is no list variable with that name.
     */
    ListContainsEntryRequirement(@NotNull Function<String, List<String>> listLookup) {
        super("list_contains_entry");
        this.listLookup = Objects.requireNonNull(listLookup);
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        if (value == null) return false;
        ListEntryCheck check = ListEntryCheck.parse(value);
        if (check.listName().isEmpty()) return false;
        List<String> entries = this.listLookup.apply(check.listName());
        return (entries != null) && (VariableLists.indexOf(entries, check.entry(), check.ignoreCase()) >= 0);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.list_contains_entry");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.list_contains_entry.desc");
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Component getValueDisplayName() {
        return Component.literal("");
    }

    @Override
    public String getValuePreset() {
        return new ListEntryCheck("some_list", "some_entry", false).serialize();
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull RequirementInstance instance, @NotNull RequirementEditingCompletedFeedback onEditingCompleted, @NotNull RequirementEditingCanceledFeedback onEditingCanceled) {
        boolean[] handled = {false};
        final Runnable[] closeAction = new Runnable[] {() -> {}};
        ListContainsEntryConfigScreen s = new ListContainsEntryConfigScreen(ListEntryCheck.parse(Objects.requireNonNullElse(instance.value, "")), callback -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            if (callback != null) {
                String oldValue = instance.value;
                instance.value = callback;
                onEditingCompleted.accept(instance, oldValue, callback);
            } else {
                onEditingCanceled.accept(instance);
            }
            closeAction[0].run();
        });
        closeAction[0] = Requirement.openRequirementValueEditor(s, () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    /**
     * The parsed requirement value.<br><br>
     *
     * Serialized as {@code list_name:ignore_case:entry}, like {@code fruits:false:apple}. The entry is the last part, so it can contain
     * anything, also after placeholders got replaced. Missing parts get defaults: {@code fruits:apple} checks for "apple" without ignoring
     * case (like the value of the Is Variable Value requirement), and {@code fruits} checks for an empty entry.
     */
    public record ListEntryCheck(@NotNull String listName, @NotNull String entry, boolean ignoreCase) {

        private static final char SEPARATOR = ':';
        private static final String IGNORE_CASE_PART = "true:";
        private static final String MATCH_CASE_PART = "false:";

        public ListEntryCheck {
            Objects.requireNonNull(listName);
            Objects.requireNonNull(entry);
        }

        @NotNull
        public static ListEntryCheck parse(@NotNull String value) {
            int nameEnd = findNameEnd(value);
            if (nameEnd < 0) return new ListEntryCheck(Variable.normalizeReferenceName(value), "", false);
            String listName = Variable.normalizeReferenceName(value.substring(0, nameEnd));
            int entryStart = nameEnd + 1;
            boolean ignoreCase = false;
            if (value.startsWith(IGNORE_CASE_PART, entryStart)) {
                ignoreCase = true;
                entryStart += IGNORE_CASE_PART.length();
            } else if (value.startsWith(MATCH_CASE_PART, entryStart)) {
                entryStart += MATCH_CASE_PART.length();
            }
            return new ListEntryCheck(listName, value.substring(entryStart), ignoreCase);
        }

        /**
         * Finds the separator behind the list name: the first one outside of braces. The editor reads values before their placeholders
         * got replaced, so a list name that is a placeholder (which has separators in its JSON) stays one part. Values that get checked
         * already have their placeholders replaced, and list names never contain braces.
         *
         * @return The index of the separator, or -1 if the value is only a list name.
         */
        private static int findNameEnd(@NotNull String value) {
            int depth = 0;
            int length = value.length();
            for (int i = 0; i < length; i++) {
                char c = value.charAt(i);
                if (c == '{') {
                    depth++;
                } else if ((c == '}') && (depth > 0)) {
                    depth--;
                } else if ((c == SEPARATOR) && (depth == 0)) {
                    return i;
                }
            }
            return -1;
        }

        @NotNull
        public String serialize() {
            return this.listName + SEPARATOR + (this.ignoreCase ? IGNORE_CASE_PART : MATCH_CASE_PART) + this.entry;
        }

    }

    public static class ListContainsEntryConfigScreen extends Requirement.RequirementValueEditScreen {

        protected static final int HINT_LINE_SPACING = 3;

        @NotNull
        protected String listName;
        @NotNull
        protected String entry;
        protected boolean ignoreCase;
        /** If the typed list name belongs to a text variable, which never has entries. Gets updated whenever the name changes. */
        protected boolean textVariableTyped;

        protected TextInputCell listNameCell;
        protected TextInputCell entryCell;
        protected EditBoxSuggestions listNameSuggestions;

        protected ListContainsEntryConfigScreen(@NotNull ListEntryCheck check, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.requirements.list_contains_entry.value.desc"), callback);
            this.listName = check.listName();
            this.entry = check.entry();
            this.ignoreCase = check.ignoreCase();
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.requirements.list_contains_entry.list_name"));
            this.listNameCell = this.addTextInputCell(null, true, true).setText(this.getListNameString());
            this.addCell(new TextVariableHintCell());

            // Text variables never have entries, so only list variables get suggested
            this.listNameSuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.listNameCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, VariableHandler.getVariableNames(VariableType.LIST));
            UIBase.applyDefaultWidgetSkinTo(this.listNameSuggestions);
            this.listNameCell.editBox.setResponder(s -> {
                this.updateTextVariableTyped();
                this.listNameSuggestions.updateCommandInfo();
            });
            this.updateTextVariableTyped();

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.requirements.list_contains_entry.entry"));
            this.entryCell = this.addTextInputCell(null, true, true).setText(this.getEntryString())
                    .setTooltip(() -> UITooltip.of(Component.translatable("fancymenu.requirements.list_contains_entry.entry.desc")));

            this.addCellGroupEndSpacerCell();

            CycleButton<CommonCycles.CycleEnabledDisabled> ignoreCaseButton = new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.requirements.list_contains_entry.ignore_case", this.ignoreCase), (value, button) -> this.ignoreCase = value.getAsBoolean());
            ignoreCaseButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.requirements.list_contains_entry.ignore_case.desc")));
            this.addWidgetCell(ignoreCaseButton, true);

            this.addStartEndSpacerCell();

        }

        protected void updateTextVariableTyped() {
            String name = Variable.normalizeReferenceName(this.getListNameString());
            this.textVariableTyped = !name.isEmpty() && (VariableHandler.getVariableType(name) == VariableType.TEXT);
        }

        @Override
        public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.listNameSuggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        protected boolean isLateBodyHovered(int mouseX, int mouseY) {
            return this.listNameSuggestions.isMouseOverList(mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            return this.keyPressed(event.key(), event.scancode(), event.modifiers());
        }

        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.listNameSuggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaX, double scrollDeltaY) {
            if (this.listNameSuggestions.mouseScrolled(scrollDeltaY)) return true;
            return super.mouseScrolled($$0, $$1, scrollDeltaX, scrollDeltaY);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (this.listNameSuggestions.mouseClicked($$0, $$1, $$2)) return true;
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public boolean allowDone() {
            return !this.getListNameString().isBlank();
        }

        @Override
        public @NotNull String buildString() {
            return new ListEntryCheck(Variable.normalizeReferenceName(this.getListNameString()), this.getEntryString(), this.ignoreCase).serialize();
        }

        @NotNull
        protected String getListNameString() {
            if (this.listNameCell != null) {
                return this.listNameCell.getText();
            }
            return this.listName;
        }

        @NotNull
        protected String getEntryString() {
            if (this.entryCell != null) {
                return this.entryCell.getText();
            }
            return this.entry;
        }

        /**
         * A hint in warning color below the name input, telling that the typed name belongs to a text variable. It wraps at the width of
         * the cell, so long translations stay inside the window, and it only takes space while it is shown, so it doesn't leave a gap below
         * the input the rest of the time.
         */
        protected class TextVariableHintCell extends RenderCell {

            @NotNull
            protected final MutableComponent hint = Component.translatable("fancymenu.requirements.list_contains_entry.text_variable_hint").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
            @NotNull
            protected List<MutableComponent> lines = List.of();
            /** The width {@link #lines} got wrapped at. Wrapping measures every word, so it only runs again when the width changes. */
            protected int wrappedWidth = -1;

            protected TextVariableHintCell() {
                super.setSelectable(false);
            }

            @Override
            public int getTopBottomSpace() {
                return ListContainsEntryConfigScreen.this.textVariableTyped ? super.getTopBottomSpace() : 0;
            }

            @Override
            protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
                int width = (int) (ListContainsEntryConfigScreen.this.scrollArea.getInnerWidth() - 40);
                this.setWidth(width);
                if (!ListContainsEntryConfigScreen.this.textVariableTyped) {
                    this.setHeight(0);
                    return;
                }
                if (width != this.wrappedWidth) {
                    this.lines = UIBase.lineWrapUIComponentsNormal(this.hint, width);
                    this.wrappedWidth = width;
                }
                this.setHeight((int) ((this.lines.size() * (UIBase.getUITextHeightNormal() + HINT_LINE_SPACING)) - HINT_LINE_SPACING));
            }

            @Override
            public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (!ListContainsEntryConfigScreen.this.textVariableTyped) return;
                float lineY = this.getY();
                for (MutableComponent line : this.lines) {
                    UIBase.renderText(graphics, line, this.getX(), lineY);
                    lineY += UIBase.getUITextHeightNormal() + HINT_LINE_SPACING;
                }
            }

        }

    }

}
