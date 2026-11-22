package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Edits the entries of a list variable: numbered rows (indexes start at 0, like in the list actions), editing in place, adding,
 * duplicating, deleting, reordering by keyboard or drag and drop, copy and paste, search, undo and redo, and editing all entries as
 * text with one entry per line.
 *
 * <p>All edits go to a {@link ListEntriesDraft}. The callback gets the entries when the user is done, or {@code null} when the user
 * cancels, so the variable only changes once per editing session.</p>
 */
public class ListVariableEditorWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;
    private static final int LIST_ENTRY_TOP_DOWN_BORDER = 1;
    private static final int LIST_ENTRY_OUTER_PADDING = 3;
    private static final int LIST_TOP_SPACER_HEIGHT = 5;
    private static final int BUTTON_GAP = 5;
    private static final int BUTTON_MIN_WIDTH = 80;
    private static final int BUTTON_LABEL_PADDING = 20;
    private static final long DOUBLE_CLICK_TIME_MS = 500L;
    private static final double DRAG_START_DISTANCE = 3.0D;
    /** How close to the top or bottom edge of the list the pointer has to be while dragging to scroll the list. */
    private static final float DRAG_AUTO_SCROLL_EDGE = 8.0F;

    @NotNull
    private final ListEntriesDraft draft;
    @NotNull
    private final Consumer<List<String>> callback;
    private boolean resultHandled = false;
    private boolean skipCloseWarning = false;

    protected ScrollArea entriesScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;
    protected ExtendedButton editAsTextButton;
    protected ExtendedButton cancelButton;
    protected ExtendedButton doneButton;
    protected ContextMenu rightClickContextMenu;
    protected float rightClickContextMenuLastOpenX = Float.NaN;
    protected float rightClickContextMenuLastOpenY = Float.NaN;
    /** The entry the context menu was opened for, or -1 if it was opened for no entry. */
    private int contextMenuTargetIndex = -1;
    private boolean canEditAsText;
    private float indexGutterWidth = 0.0F;
    private int indexGutterDigits = -1;

    /** The edit box of the entry that gets edited in place, or {@code null} if no entry gets edited. */
    @Nullable
    private ExtendedEditBox inlineEditBox;
    /** The entry that gets edited in place, or the index a new entry gets. */
    private int inlineEditIndex = -1;
    /** If the entry that gets edited in place is a new one, which only becomes part of the list once it gets saved. */
    private boolean inlineEditIsNew = false;
    @NotNull
    private String inlineEditOriginal = "";
    private int selectionBeforeAdd = -1;

    private int lastClickedIndex = -1;
    private long lastClickTime = 0L;

    private int pressedEntryIndex = -1;
    private double pressX;
    private double pressY;
    private boolean dragging = false;
    private int dropSlot = -1;

    public ListVariableEditorWindowBody(@NotNull String variableName, @NotNull List<String> entries, @NotNull Consumer<List<String>> callback) {
        super(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.title", Objects.requireNonNull(variableName)));
        this.draft = new ListEntriesDraft(entries);
        this.callback = Objects.requireNonNull(callback);
        // Escape cancels inline edits first, so the window handles it itself
        this.setAllowCloseOnEsc(false);
        // Rows fill the width of the list and cut off long entries, so the list never scrolls sideways
        this.entriesScrollArea.makeEntriesWidthOfArea = true;
        this.entriesScrollArea.makeAllEntriesWidthOfWidestEntry = false;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ListVariableEditorWindowBody screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setIcon(ManageVariablesWindowBody.getTypeIcon(VariableType.LIST))
                .setForceFancyMenuUiScale(true)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    @Override
    protected void init() {

        boolean blur = UIBase.shouldBlur();
        this.entriesScrollArea.setSetupForBlurInterface(blur);
        this.updateRightClickContextMenu(false);

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, getLabeledContentTop() + 1, 200, 20 - 2, Component.empty());
        this.searchBar.setHintFancyMenu(consumes -> Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.search"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.onSearchChanged());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar, blur);
        this.setupInitialFocusWidget(this, this.searchBar);

        Component doneLabel = Component.translatable("fancymenu.common_components.done");
        Component cancelLabel = Component.translatable("fancymenu.common_components.cancel");
        Component editAsTextLabel = Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_as_text");
        int doneCancelWidth = Math.max(getButtonWidth(doneLabel), getButtonWidth(cancelLabel));

        this.doneButton = new ExtendedButton(0, 0, doneCancelWidth, 20, doneLabel, button -> this.onDone());
        this.cancelButton = new ExtendedButton(0, 0, doneCancelWidth, 20, cancelLabel, button -> this.requestCancel());
        this.editAsTextButton = new ExtendedButton(0, 0, getButtonWidth(editAsTextLabel), 20, editAsTextLabel, button -> this.openEditAsText());
        this.editAsTextButton.setIsActiveSupplier(consumes -> this.canEditAsText);
        this.editAsTextButton.setUITooltipSupplier(consumes -> UITooltip.of(Component.translatable(this.canEditAsText
                ? "fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_as_text.desc"
                : "fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_as_text.unavailable")));
        for (ExtendedButton button : List.of(this.editAsTextButton, this.cancelButton, this.doneButton)) {
            button.setNavigatable(false);
            this.addRenderableWidget(button);
            UIBase.applyDefaultWidgetSkinTo(button, blur);
        }

        this.updateLayout();
        this.addRenderableWidget(this.entriesScrollArea);

        this.rebuildRows(true);

    }

    private static int getButtonWidth(@NotNull Component label) {
        return Math.max(BUTTON_MIN_WIDTH, (int) UIBase.getUITextWidthNormal(label) + BUTTON_LABEL_PADDING);
    }

    private void updateLayout() {
        int contentTop = getLabeledContentTop();
        this.entriesScrollArea.setWidth(this.width - 40, true);
        this.entriesScrollArea.setHeight(this.height - contentTop - 25 - CONTENT_MARGIN, true);
        this.entriesScrollArea.setX(20, true);
        this.entriesScrollArea.setY(contentTop + 25, true);
        int buttonY = Math.max(CONTENT_MARGIN, (int) (this.entriesScrollArea.getYWithBorder() - 20 - BUTTON_GAP));
        int x = this.width - 20;
        for (ExtendedButton button : new ExtendedButton[] {this.doneButton, this.cancelButton, this.editAsTextButton}) {
            if (button == null) continue;
            x -= button.getWidth();
            button.setX(x);
            button.setY(buttonY);
            x -= BUTTON_GAP;
        }
        if (this.searchBar != null) {
            int searchBarX = 20 + 1;
            this.searchBar.setX(searchBarX);
            this.searchBar.setY(contentTop + 1);
            this.searchBar.setWidth(Math.max(100, x - searchBarX));
        }
    }

    @Override
    public void setWindow(@Nullable PiPWindow window) {
        if (window != null) {
            window.setCloseWindowCheck((window1, decision) -> {
                if (this.skipCloseWarning) {
                    this.skipCloseWarning = false;
                    decision.supply(true);
                    return;
                }
                this.confirmDiscardChanges(decision::supply);
            });
        }
        super.setWindow(window);
    }

    @Override
    public void onScreenClosed() {
        this.closeWithoutResult();
    }

    @Override
    public void onWindowClosedExternally() {
        this.closeWithoutResult();
    }

    private void closeWithoutResult() {
        this.closeRightClickContextMenu();
        if (this.resultHandled) return;
        this.resultHandled = true;
        this.callback.accept(null);
    }

    protected void onDone() {
        if (this.resultHandled) return;
        this.finishInlineEdit(true, false);
        this.closeRightClickContextMenu();
        this.resultHandled = true;
        this.skipCloseWarning = true;
        this.callback.accept(List.copyOf(this.draft.getEntries()));
        this.closeWindow();
    }

    protected void requestCancel() {
        this.confirmDiscardChanges(discard -> {
            if (discard) {
                this.closeRightClickContextMenu();
                this.resultHandled = true;
                this.skipCloseWarning = true;
                this.callback.accept(null);
                this.closeWindow();
            }
        });
    }

    /**
     * Asks the user to confirm losing their changes. When there is nothing to lose, the callback gets {@code true} right away.
     */
    private void confirmDiscardChanges(@NotNull Consumer<Boolean> callback) {
        if (!this.hasUnsavedChanges()) {
            callback.accept(true);
            return;
        }
        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.cancel_warning"), MessageDialogStyle.WARNING, callback);
    }

    private boolean hasUnsavedChanges() {
        if (this.draft.isChanged()) return true;
        if (this.inlineEditBox == null) return false;
        String value = this.inlineEditBox.getValue();
        return this.inlineEditIsNew ? !value.isEmpty() : !value.equals(this.inlineEditOriginal);
    }

    protected void updateRightClickContextMenu(boolean reopen) {

        boolean wasOpen = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.rightClickContextMenu = new ContextMenu();

        this.rightClickContextMenu.addClickableEntry("add_entry", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.add_entry"), (menu, entry) -> {
                    int target = this.contextMenuTargetIndex;
                    menu.closeMenuChain();
                    this.startInlineAdd((target >= 0) ? (target + 1) : this.draft.size());
                }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.add_entry.desc")))
                .setIcon(MaterialIcons.ADD);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_add")
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget());

        this.rightClickContextMenu.addClickableEntry("edit_entry", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_entry"), (menu, entry) -> {
                    int target = this.contextMenuTargetIndex;
                    menu.closeMenuChain();
                    this.startInlineEdit(target);
                }).addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.enter"))
                .setIcon(MaterialIcons.EDIT);

        this.rightClickContextMenu.addClickableEntry("edit_entry_in_text_editor", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_in_text_editor"), (menu, entry) -> {
                    int target = this.contextMenuTargetIndex;
                    menu.closeMenuChain();
                    if (this.isValidEntryIndex(target)) {
                        this.openEntryTextEditor(target, this.draft.get(target), false);
                    }
                }).addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_in_text_editor.desc")))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.shortcuts.shift_enter"))
                .setIcon(MaterialIcons.EDIT_NOTE);

        this.rightClickContextMenu.addClickableEntry("duplicate_entry", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.duplicate_entry"), (menu, entry) -> {
                    int target = this.contextMenuTargetIndex;
                    menu.closeMenuChain();
                    this.duplicateEntry(target);
                }).addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.shortcuts.duplicate"))
                .setIcon(MaterialIcons.LIBRARY_ADD);

        this.rightClickContextMenu.addClickableEntry("delete_entry", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.delete_entry"), (menu, entry) -> {
                    int target = this.contextMenuTargetIndex;
                    menu.closeMenuChain();
                    this.deleteEntry(target);
                }).addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.delete"))
                .setIcon(MaterialIcons.DELETE);

        this.rightClickContextMenu.addSeparatorEntry("separator_before_move")
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget());

        this.rightClickContextMenu.addClickableEntry("move_entry_up", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.move_up"), (menu, entry) -> {
                    this.moveEntry(this.contextMenuTargetIndex, true);
                }).addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget())
                .addIsActiveSupplier((menu, entry) -> this.canReorder() && (this.contextMenuTargetIndex > 0))
                .setTooltipSupplier((menu, entry) -> this.getReorderTooltip())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.shortcuts.alt_up"))
                .setIcon(MaterialIcons.ARROW_UPWARD);

        this.rightClickContextMenu.addClickableEntry("move_entry_down", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.move_down"), (menu, entry) -> {
                    this.moveEntry(this.contextMenuTargetIndex, false);
                }).addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget())
                .addIsActiveSupplier((menu, entry) -> this.canReorder() && this.isValidEntryIndex(this.contextMenuTargetIndex + 1))
                .setTooltipSupplier((menu, entry) -> this.getReorderTooltip())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.shortcuts.alt_down"))
                .setIcon(MaterialIcons.ARROW_DOWNWARD);

        this.rightClickContextMenu.addSeparatorEntry("separator_before_copy_paste");

        this.rightClickContextMenu.addClickableEntry("copy_entry", Component.translatable("fancymenu.editor.edit.copy"), (menu, entry) -> {
                    int target = this.contextMenuTargetIndex;
                    menu.closeMenuChain();
                    this.copyEntry(target);
                }).addIsActiveSupplier((menu, entry) -> this.hasContextMenuTarget())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"))
                .setIcon(MaterialIcons.CONTENT_COPY);

        this.rightClickContextMenu.addClickableEntry("paste_entry", Component.translatable("fancymenu.editor.edit.paste"), (menu, entry) -> {
                    int target = this.contextMenuTargetIndex;
                    menu.closeMenuChain();
                    this.pasteEntry(target);
                }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.paste.desc")))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.paste"))
                .setIcon(MaterialIcons.CONTENT_PASTE);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_copy_paste");

        this.rightClickContextMenu.addClickableEntry("undo", Component.translatable("fancymenu.editor.edit.undo"), (menu, entry) -> {
                    if (this.undo()) {
                        this.updateRightClickContextMenu(true);
                    }
                }).addIsActiveSupplier((menu, entry) -> this.draft.canUndo())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.undo"))
                .setIcon(MaterialIcons.UNDO);

        this.rightClickContextMenu.addClickableEntry("redo", Component.translatable("fancymenu.editor.edit.redo"), (menu, entry) -> {
                    if (this.redo()) {
                        this.updateRightClickContextMenu(true);
                    }
                }).addIsActiveSupplier((menu, entry) -> this.draft.canRedo())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.redo"))
                .setIcon(MaterialIcons.REDO);

        this.rightClickContextMenu.addSeparatorEntry("separator_before_edit_as_text");

        this.rightClickContextMenu.addClickableEntry("edit_as_text", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_as_text"), (menu, entry) -> {
                    menu.closeMenuChain();
                    this.openEditAsText();
                }).addIsActiveSupplier((menu, entry) -> this.canEditAsText)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable(this.canEditAsText
                        ? "fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_as_text.desc"
                        : "fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_as_text.unavailable")))
                .setIcon(MaterialIcons.NOTES);

        if (reopen || wasOpen) {
            this.openRightClickContextMenuAtMouse(true);
        }

    }

    @Nullable
    private UITooltip getReorderTooltip() {
        return this.isSearching() ? UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.reorder_while_searching")) : null;
    }

    @Override
    public void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();
        this.updateLayout();
        this.updateIndexGutter();

        if (this.dragging) {
            this.autoScrollWhileDragging(mouseY);
            this.entriesScrollArea.updateEntries(null);
            this.dropSlot = this.computeDropSlot(mouseY);
        }

        int searchBarTop = (this.searchBar != null) ? this.searchBar.getY() : (getLabeledContentTop() + 1);
        float labelY = searchBarTop - UIBase.getUITextHeightNormal() - UIBase.getAreaLabelVerticalPadding();
        int textColor = UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt() : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
        UIBase.renderText(graphics, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.entries"), 20, labelY, textColor);
        String countText = ManageVariablesWindowBody.getEntryCountText(this.draft.size());
        UIBase.renderText(graphics, countText, this.width - 20 - UIBase.getUITextWidth(countText), labelY, VariableRowEntry.TextStyle.MUTED.getColor());

    }

    @Override
    public void renderLateBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        ScrollArea area = this.entriesScrollArea;

        if (!this.hasEntryRows()) {
            Component hint = Component.translatable(this.isSearching() ? "fancymenu.overlay.menu_bar.variables.manage.edit_list.no_search_results" : "fancymenu.overlay.menu_bar.variables.manage.edit_list.empty_hint");
            float hintWidth = UIBase.getUITextWidthNormal(hint);
            float hintX = area.getInnerX() + (area.getInnerWidth() / 2F) - (hintWidth / 2F);
            float hintY = area.getInnerY() + (area.getInnerHeight() / 2F) - (UIBase.getUITextHeightNormal() / 2F);
            UIBase.renderText(graphics, hint, hintX, hintY, VariableRowEntry.TextStyle.MUTED.getColor());
        }

        if (this.dragging) {
            this.renderDropIndicator(graphics);
        }

        ExtendedEditBox editBox = this.inlineEditBox;
        if (editBox != null) {
            EntryRow row = this.findEditedRow();
            if (row == null) {
                this.finishInlineEdit(false, false);
            } else {
                editBox.setX((int) row.getTextX() - 3);
                editBox.setY((int) row.getY() + 1);
                editBox.setWidth(Math.max(20, (int) (row.getTextEndX() - row.getTextX()) + 3));
                editBox.setHeight((int) row.getHeight() - 2);
                graphics.enableScissor((int) area.getInnerX(), (int) area.getInnerY(), (int) (area.getInnerX() + area.getInnerWidth()), (int) (area.getInnerY() + area.getInnerHeight()));
                editBox.extractRenderState(graphics, mouseX, mouseY, partial);
                graphics.disableScissor();
                if (editBox.isMouseOver(mouseX, mouseY)) {
                    CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
                }
            }
        }

    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
    }

    private void updateIndexGutter() {
        int maxIndex = Math.max(0, this.draft.size() - 1 + (this.inlineEditIsNew ? 1 : 0));
        int digits = String.valueOf(maxIndex).length();
        if (digits != this.indexGutterDigits) {
            this.indexGutterDigits = digits;
            this.indexGutterWidth = VariableRowEntry.getIndexColumnWidth(maxIndex);
        }
    }

    private boolean isSearching() {
        return (this.searchBar != null) && !this.searchBar.getValue().isBlank();
    }

    private void onSearchChanged() {
        this.finishInlineEdit(true, false);
        this.resetDrag();
        this.rebuildRows(false);
    }

    /**
     * Builds the rows again from the draft: the entries that fit the search, the new entry that gets added (if any) and the Add Entry row.
     */
    protected void rebuildRows(boolean keepScroll) {

        float scroll = this.entriesScrollArea.verticalScrollBar.getScroll();
        this.canEditAsText = ListEntriesDraft.canEditAsText(this.draft.getEntries());
        this.indexGutterDigits = -1;

        this.entriesScrollArea.clearEntries();
        CellScreen.SpacerScrollAreaEntry spacer = new CellScreen.SpacerScrollAreaEntry(this.entriesScrollArea, LIST_TOP_SPACER_HEIGHT);
        spacer.setSelectable(false);
        spacer.selectOnClick = false;
        spacer.setPlayClickSound(false);
        spacer.setBackgroundColorNormal(() -> DrawableColor.FULLY_TRANSPARENT);
        spacer.setBackgroundColorHover(() -> DrawableColor.FULLY_TRANSPARENT);
        this.entriesScrollArea.addEntry(spacer);

        String search = this.isSearching() ? this.searchBar.getValue().toLowerCase(Locale.ROOT) : null;
        int size = this.draft.size();
        int rowHeight = this.getRowHeight();
        for (int i = 0; i <= size; i++) {
            if (this.inlineEditIsNew && (i == this.inlineEditIndex)) {
                this.entriesScrollArea.addEntry(new EntryRow(this.entriesScrollArea, -1, i, "", rowHeight));
            }
            if (i == size) break;
            String entry = this.draft.get(i);
            if ((search != null) && !entry.toLowerCase(Locale.ROOT).contains(search)) continue;
            int displayIndex = (this.inlineEditIsNew && (i >= this.inlineEditIndex)) ? (i + 1) : i;
            this.entriesScrollArea.addEntry(new EntryRow(this.entriesScrollArea, i, displayIndex, entry, rowHeight));
        }
        this.entriesScrollArea.addEntry(new AddEntryRow(this.entriesScrollArea, rowHeight));

        if (keepScroll) {
            this.entriesScrollArea.verticalScrollBar.setScroll(scroll);
        }
        this.entriesScrollArea.updateEntries(null);

    }

    protected int getRowHeight() {
        return (int) (UIBase.getUITextHeightNormal() + (LIST_ENTRY_TOP_DOWN_BORDER * 2) + (LIST_ENTRY_OUTER_PADDING * 2));
    }

    private boolean hasEntryRows() {
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if (entry instanceof EntryRow) return true;
        }
        return false;
    }

    @Nullable
    private EntryRow findEntryRow(int entryIndex) {
        if (entryIndex < 0) return null;
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if ((entry instanceof EntryRow row) && (row.entryIndex == entryIndex)) return row;
        }
        return null;
    }

    @Nullable
    private EntryRow findEditedRow() {
        if (this.inlineEditBox == null) return null;
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if (entry instanceof EntryRow row) {
                if (this.inlineEditIsNew ? row.isNew() : (row.entryIndex == this.inlineEditIndex)) return row;
            }
        }
        return null;
    }

    @NotNull
    private List<ScrollAreaEntry> getNavigableRows() {
        List<ScrollAreaEntry> rows = new ArrayList<>();
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if ((entry instanceof EntryRow row && !row.isNew()) || (entry instanceof AddEntryRow)) rows.add(entry);
        }
        return rows;
    }

    @Nullable
    private ScrollAreaEntry getSelectedRow() {
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if (entry.isSelected()) return entry;
        }
        return null;
    }

    /**
     * @return The index of the selected entry, or -1 if no entry is selected.
     */
    private int getSelectedEntryIndex() {
        return (this.getSelectedRow() instanceof EntryRow row) ? row.entryIndex : -1;
    }

    private boolean isValidEntryIndex(int index) {
        return (index >= 0) && (index < this.draft.size());
    }

    /**
     * Selects the row of the entry (or the Add Entry row if the index is the end of the list) and scrolls it into view.
     */
    private void selectEntry(int entryIndex) {
        ScrollAreaEntry row = (entryIndex >= this.draft.size()) ? this.findAddEntryRow() : this.findEntryRow(entryIndex);
        if (row == null) {
            this.clearSelection();
            return;
        }
        this.defocusSearchBar();
        row.setSelected(true);
        this.ensureRowVisible(row);
    }

    @Nullable
    private AddEntryRow findAddEntryRow() {
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if (entry instanceof AddEntryRow row) return row;
        }
        return null;
    }

    private void clearSelection() {
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            entry.setSelected(false);
        }
    }

    /**
     * Scrolls as little as needed to show the whole row.
     */
    private void ensureRowVisible(@NotNull ScrollAreaEntry row) {
        ScrollArea area = this.entriesScrollArea;
        float totalScrollHeight = area.getTotalScrollHeight();
        if (totalScrollHeight <= 0.0F) return;
        float rowOffset = 0.0F;
        for (ScrollAreaEntry entry : area.getEntries()) {
            if (entry == row) break;
            rowOffset += entry.getHeight();
        }
        float scrolledPixels = area.verticalScrollBar.getScroll() * totalScrollHeight;
        float rowTop = rowOffset - scrolledPixels;
        float targetPixels = scrolledPixels;
        if (rowTop < 0.0F) {
            targetPixels = rowOffset;
        } else if ((rowTop + row.getHeight()) > area.getInnerHeight()) {
            targetPixels = rowOffset + row.getHeight() - area.getInnerHeight();
        }
        if (targetPixels != scrolledPixels) {
            area.verticalScrollBar.setScroll(Mth.clamp(targetPixels / totalScrollHeight, 0.0F, 1.0F));
            area.updateEntries(null);
        }
    }

    @Nullable
    private ScrollAreaEntry getRowAt(double mouseX, double mouseY) {
        if (!this.entriesScrollArea.isMouseOverInnerArea(mouseX, mouseY)) return null;
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if (((entry instanceof EntryRow) || (entry instanceof AddEntryRow)) && UIBase.isXYInArea(mouseX, mouseY, entry.getX(), entry.getY(), entry.getWidth(), entry.getHeight())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Edits the entry in place. Entries with line breaks don't fit into a single line, so they open in the text editor instead.
     */
    protected void startInlineEdit(int entryIndex) {
        if (!this.isValidEntryIndex(entryIndex)) return;
        this.finishInlineEdit(true, false);
        String entry = this.draft.get(entryIndex);
        if (VariableTextUtils.containsLineBreak(entry)) {
            this.openEntryTextEditor(entryIndex, entry, false);
            return;
        }
        EntryRow row = this.findEntryRow(entryIndex);
        if (row == null) return;
        this.selectEntry(entryIndex);
        this.inlineEditIndex = entryIndex;
        this.inlineEditIsNew = false;
        this.inlineEditOriginal = entry;
        this.openInlineEditBox(entry);
    }

    /**
     * Adds a new entry at the index and edits it in place. It only becomes part of the list once it gets saved with some text.
     */
    protected void startInlineAdd(int index) {
        this.finishInlineEdit(true, false);
        this.resetDrag();
        int selectedBefore = this.getSelectedEntryIndex();
        if (this.isSearching()) {
            // The new entry has to stay visible, whatever its text is
            this.searchBar.setValue("");
        }
        this.selectionBeforeAdd = selectedBefore;
        this.inlineEditIndex = Mth.clamp(index, 0, this.draft.size());
        this.inlineEditIsNew = true;
        this.inlineEditOriginal = "";
        this.rebuildRows(true);
        this.openInlineEditBox("");
        EntryRow row = this.findEditedRow();
        if (row != null) {
            row.setSelected(true);
            this.ensureRowVisible(row);
        }
    }

    private void openInlineEditBox(@NotNull String value) {
        ExtendedEditBox editBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 20, 10, Component.empty());
        editBox.setMaxLength(1000000);
        editBox.setValue(value);
        editBox.setCursorPosition(value.length());
        editBox.setHighlightPos(0);
        if (this.inlineEditIsNew) {
            editBox.setHintFancyMenu(consumes -> Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.new_entry_hint"));
        }
        UIBase.applyDefaultWidgetSkinTo(editBox, UIBase.shouldBlur());
        this.inlineEditBox = editBox;
        if (this.searchBar != null) this.searchBar.setFocused(false);
        editBox.setFocused(true);
        this.setFocused(editBox);
    }

    /**
     * Ends editing in place.
     *
     * @param save If the edited text gets saved. New entries without text never get saved.
     * @param continueAdding If saving a new entry starts adding the next one right below it.
     * @return If rows got added or removed, which moves the rows below them.
     */
    protected boolean finishInlineEdit(boolean save, boolean continueAdding) {
        ExtendedEditBox editBox = this.inlineEditBox;
        if (editBox == null) return false;
        String value = editBox.getValue();
        int index = this.inlineEditIndex;
        boolean isNew = this.inlineEditIsNew;
        this.inlineEditBox = null;
        this.inlineEditIndex = -1;
        this.inlineEditIsNew = false;
        editBox.setFocused(false);
        if (this.getFocused() == editBox) this.setFocused(null);

        if (!isNew) {
            if (save && this.isValidEntryIndex(index) && this.draft.set(index, value, index)) {
                // Updating the row in place keeps it under the pointer, so a click that ended the edit still reaches the row it hit
                EntryRow row = this.findEntryRow(index);
                if (row != null) row.setEntry(value);
            }
            return false;
        }

        if (!save || value.isEmpty()) {
            this.rebuildRows(true);
            this.selectEntry(this.isValidEntryIndex(this.selectionBeforeAdd) ? this.selectionBeforeAdd : Math.min(index, this.draft.size()));
            return true;
        }
        this.draft.insert(index, value, this.selectionBeforeAdd);
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if (entry instanceof EntryRow row) {
                if (row.isNew()) {
                    row.entryIndex = index;
                    row.setEntry(value);
                } else if (row.entryIndex >= index) {
                    row.entryIndex++;
                }
            }
        }
        if (continueAdding) {
            this.startInlineAdd(index + 1);
        }
        return false;
    }

    /**
     * Moves the text of the entry that gets edited in place into the text editor, where it can have line breaks.
     */
    private void switchInlineEditToTextEditor() {
        ExtendedEditBox editBox = this.inlineEditBox;
        if (editBox == null) return;
        String value = editBox.getValue();
        int index = this.inlineEditIndex;
        boolean isNew = this.inlineEditIsNew;
        this.inlineEditBox = null;
        this.inlineEditIndex = -1;
        this.inlineEditIsNew = false;
        if (this.getFocused() == editBox) this.setFocused(null);
        if (isNew) this.rebuildRows(true);
        this.openEntryTextEditor(index, value, isNew);
    }

    /**
     * Edits an entry in the text editor, where it can have line breaks.
     *
     * @param insert If the text becomes a new entry at the index, instead of replacing the entry there.
     */
    protected void openEntryTextEditor(int index, @NotNull String text, boolean insert) {
        Component title = Component.translatable(insert ? "fancymenu.overlay.menu_bar.variables.manage.edit_list.add_entry" : "fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_entry");
        int selectedBefore = insert ? this.selectionBeforeAdd : index;
        TextEditorWindowBody editor = new TextEditorWindowBody(title, null, result -> {
            if (result == null) return;
            if (insert) {
                int insertIndex = Mth.clamp(index, 0, this.draft.size());
                this.draft.insert(insertIndex, result, selectedBefore);
                this.rebuildRows(true);
                this.selectEntry(insertIndex);
            } else if (this.isValidEntryIndex(index) && this.draft.set(index, result, index)) {
                this.rebuildRows(true);
                this.selectEntry(index);
            }
        });
        editor.setMultilineMode(true);
        editor.setPlaceholdersAllowed(false);
        editor.setText(text);
        Pair<TextEditorWindowBody, PiPWindow> dialog = Dialogs.openGeneric(editor, title, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        dialog.getSecond().setIcon(MaterialIcons.EDIT_NOTE);
    }

    /**
     * Edits all entries as text with one entry per line, like the values of list sliders. Entries with line breaks would turn into
     * several entries that way, so it only works without them.
     */
    protected void openEditAsText() {
        if (!this.canEditAsText) return;
        this.finishInlineEdit(true, false);
        String text = ListEntriesDraft.toText(this.draft.getEntries());
        Component title = Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list.edit_as_text");
        TextEditorWindowBody editor = new TextEditorWindowBody(title, null, result -> {
            // Unchanged text keeps the entries as they are, also a list of only one empty entry, which reads back as an empty list
            if ((result == null) || result.equals(text)) return;
            int selected = this.getSelectedEntryIndex();
            if (this.draft.replaceAll(ListEntriesDraft.fromText(result), selected)) {
                this.rebuildRows(true);
                if (this.isValidEntryIndex(selected)) this.selectEntry(selected);
            }
        });
        editor.setMultilineMode(true);
        editor.setPlaceholdersAllowed(false);
        editor.setText(text);
        Pair<TextEditorWindowBody, PiPWindow> dialog = Dialogs.openGeneric(editor, title, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        dialog.getSecond().setIcon(MaterialIcons.NOTES);
    }

    protected void deleteEntry(int entryIndex) {
        if (!this.isValidEntryIndex(entryIndex)) return;
        this.finishInlineEdit(true, false);
        this.draft.remove(entryIndex, entryIndex);
        this.rebuildRows(true);
        // Selecting the entry that moved up lets Delete remove several entries in a row
        if (this.draft.size() > 0) this.selectEntry(Math.min(entryIndex, this.draft.size() - 1));
    }

    protected void duplicateEntry(int entryIndex) {
        if (!this.isValidEntryIndex(entryIndex)) return;
        this.finishInlineEdit(true, false);
        int copyIndex = this.draft.duplicate(entryIndex, entryIndex);
        this.rebuildRows(true);
        this.selectEntry(copyIndex);
    }

    protected boolean canReorder() {
        return !this.isSearching() && (this.inlineEditBox == null);
    }

    protected void moveEntry(int entryIndex, boolean up) {
        if (!this.canReorder() || !this.isValidEntryIndex(entryIndex)) return;
        int target = up ? (entryIndex - 1) : (entryIndex + 1);
        if (!this.isValidEntryIndex(target)) return;
        this.draft.move(entryIndex, target, entryIndex);
        this.rebuildRows(true);
        this.selectEntry(target);
        if (this.contextMenuTargetIndex == entryIndex) {
            // Moving again from the still open context menu moves the same entry
            this.contextMenuTargetIndex = target;
        }
    }

    protected void copyEntry(int entryIndex) {
        if (!this.isValidEntryIndex(entryIndex)) return;
        Minecraft.getInstance().keyboardHandler.setClipboard(this.draft.get(entryIndex));
    }

    /**
     * Adds the text of the clipboard as one new entry below the entry, or at the end of the list if no entry is given.
     * Line breaks stay part of the entry, so copying and pasting an entry never changes it.
     */
    protected void pasteEntry(int belowEntryIndex) {
        String text = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (text.isEmpty()) return;
        this.finishInlineEdit(true, false);
        int index = this.isValidEntryIndex(belowEntryIndex) ? (belowEntryIndex + 1) : this.draft.size();
        this.draft.insert(index, text, this.getSelectedEntryIndex());
        if (this.isSearching()) this.searchBar.setValue("");
        this.rebuildRows(true);
        this.selectEntry(index);
    }

    protected boolean undo() {
        return this.applyHistoryStep(true);
    }

    protected boolean redo() {
        return this.applyHistoryStep(false);
    }

    private boolean applyHistoryStep(boolean undo) {
        this.finishInlineEdit(false, false);
        this.resetDrag();
        int selected = this.getSelectedEntryIndex();
        ListEntriesDraft.Snapshot snapshot = undo ? this.draft.undo(selected) : this.draft.redo(selected);
        if (snapshot == null) return false;
        this.rebuildRows(true);
        if (this.isValidEntryIndex(snapshot.selectedIndex())) {
            this.selectEntry(snapshot.selectedIndex());
        } else {
            this.clearSelection();
        }
        return true;
    }

    private void resetDrag() {
        this.pressedEntryIndex = -1;
        this.dragging = false;
        this.dropSlot = -1;
    }

    /**
     * @return The index the dragged entry gets dropped in front of, from 0 to the size of the list (behind the last entry).
     */
    private int computeDropSlot(double mouseY) {
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if ((entry instanceof EntryRow row) && !row.isNew() && (mouseY < (row.getY() + (row.getHeight() / 2.0F)))) {
                return row.entryIndex;
            }
        }
        return this.draft.size();
    }

    private void autoScrollWhileDragging(int mouseY) {
        ScrollArea area = this.entriesScrollArea;
        float step = 0.1F * area.verticalScrollBar.getWheelScrollSpeed();
        float scroll = area.verticalScrollBar.getScroll();
        if (mouseY <= (area.getInnerY() + DRAG_AUTO_SCROLL_EDGE)) {
            area.verticalScrollBar.setScroll(Math.max(0.0F, scroll - step));
        } else if (mouseY >= (area.getInnerY() + area.getInnerHeight() - DRAG_AUTO_SCROLL_EDGE)) {
            area.verticalScrollBar.setScroll(Math.min(1.0F, scroll + step));
        }
    }

    private void renderDropIndicator(@NotNull GuiGraphicsExtractor graphics) {
        int slot = this.dropSlot;
        // Dropping an entry right above or below itself changes nothing, so there is nothing to show
        if ((slot < 0) || (slot == this.pressedEntryIndex) || (slot == (this.pressedEntryIndex + 1))) return;
        float lineY = -1.0F;
        for (ScrollAreaEntry entry : this.entriesScrollArea.getEntries()) {
            if ((entry instanceof EntryRow row) && !row.isNew()) {
                if (row.entryIndex == slot) {
                    lineY = row.getY();
                    break;
                }
                lineY = row.getY() + row.getHeight();
            }
        }
        ScrollArea area = this.entriesScrollArea;
        float clampedY = Mth.clamp(lineY, area.getInnerY(), area.getInnerY() + area.getInnerHeight() - 1.0F);
        UIBase.fillF(graphics, area.getInnerX(), clampedY - 0.5F, area.getInnerX() + area.getInnerWidth(), clampedY + 0.5F, VariableRowEntry.TextStyle.LABEL.getColor());
    }

    private void dropDraggedEntry() {
        int from = this.pressedEntryIndex;
        int slot = this.dropSlot;
        this.resetDrag();
        if (!this.isValidEntryIndex(from) || (slot < 0)) return;
        int to = (slot > from) ? (slot - 1) : slot;
        to = Mth.clamp(to, 0, this.draft.size() - 1);
        if (this.draft.move(from, to, from)) {
            this.rebuildRows(true);
            this.selectEntry(to);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.keyPressed(event.key(), event.keycode(), event.modifiers());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        boolean shortcutModifierDown = InputUtils.isGuiShortcutModifierDown(modifiers);
        boolean shiftDown = (modifiers & InputConstants.MOD_SHIFT) != 0;
        boolean altDown = (modifiers & InputConstants.MOD_ALT) != 0;
        String keyName = InputUtils.getKeyName(keyCode, scanCode).toLowerCase(Locale.ROOT);
        boolean enter = (keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER);
        boolean upOrDown = (keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN);

        if (shortcutModifierDown && !shiftDown && !altDown && "s".equals(keyName)) {
            this.onDone();
            return true;
        }

        ExtendedEditBox editBox = this.inlineEditBox;
        if (editBox != null) {
            if (enter) {
                if (shiftDown) {
                    this.switchInlineEditToTextEditor();
                } else {
                    this.finishInlineEdit(true, this.inlineEditIsNew);
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.finishInlineEdit(false, false);
                return true;
            }
            if (upOrDown || (keyCode == InputConstants.KEY_TAB)) {
                this.finishInlineEdit(true, false);
                if (upOrDown) this.selectAdjacentRow(keyCode == InputConstants.KEY_DOWN);
                return true;
            }
            editBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        boolean contextMenuOpen = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        if (contextMenuOpen) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.closeRightClickContextMenu();
                return true;
            }
            if (upOrDown) {
                ContextMenuHandler.INSTANCE.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.requestCancel();
            return true;
        }

        boolean searchBarFocused = (this.searchBar != null) && this.searchBar.isFocused();

        if (keyCode == InputConstants.KEY_BACKSPACE) {
            if (this.searchBar != null) {
                if (!searchBarFocused) this.focusSearchBar();
                this.searchBar.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }

        if (shortcutModifierDown && "z".equals(keyName)) {
            if (shiftDown ? this.redo() : this.undo()) return true;
        }

        if (shortcutModifierDown && "y".equals(keyName)) {
            if (this.redo()) return true;
        }

        if (!searchBarFocused) {
            int selected = this.getSelectedEntryIndex();
            if (shortcutModifierDown && "c".equals(keyName) && (selected >= 0)) {
                this.copyEntry(selected);
                return true;
            }
            if (shortcutModifierDown && "v".equals(keyName)) {
                this.pasteEntry(selected);
                return true;
            }
            if (shortcutModifierDown && "d".equals(keyName) && (selected >= 0)) {
                this.duplicateEntry(selected);
                return true;
            }
            if (altDown && upOrDown && (selected >= 0)) {
                this.moveEntry(selected, keyCode == InputConstants.KEY_UP);
                return true;
            }
            if (keyCode == InputConstants.KEY_DELETE && (selected >= 0)) {
                this.deleteEntry(selected);
                return true;
            }
            if (enter) {
                ScrollAreaEntry selectedRow = this.getSelectedRow();
                if (selectedRow instanceof AddEntryRow) {
                    this.startInlineAdd(this.draft.size());
                    return true;
                }
                if (selected >= 0) {
                    if (shiftDown) {
                        this.openEntryTextEditor(selected, this.draft.get(selected), false);
                    } else {
                        this.startInlineEdit(selected);
                    }
                    return true;
                }
            }
        }

        if (keyCode == InputConstants.KEY_TAB) {
            return true;
        }

        if (upOrDown) {
            return this.selectAdjacentRow(keyCode == InputConstants.KEY_DOWN);
        }

        // Left and right only move the cursor of the search bar
        if (!searchBarFocused && ((keyCode == InputConstants.KEY_LEFT) || (keyCode == InputConstants.KEY_RIGHT))) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);

    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        ExtendedEditBox editBox = this.inlineEditBox;
        if (editBox != null) {
            return editBox.charTyped(event);
        }
        boolean contextMenuOpen = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        char character = (char) event.codepoint();
        if (!contextMenuOpen && !Character.isISOControl(character) && !InputUtils.isGuiShortcutModifierDown() && (this.searchBar != null) && !this.searchBar.isFocused()) {
            this.focusSearchBar();
            if (this.searchBar.charTyped(event)) return true;
        }
        return super.charTyped(event);
    }

    private void focusSearchBar() {
        if (this.searchBar == null) return;
        this.clearSelection();
        this.setFocused(this.searchBar);
        this.searchBar.setFocused(true);
    }

    private void defocusSearchBar() {
        if (this.searchBar == null) return;
        this.searchBar.setFocused(false);
        if (this.getFocused() == this.searchBar) this.setFocused(null);
    }

    /**
     * Moves the selection through the search bar, the entries and the Add Entry row, wrapping around at both ends.
     */
    private boolean selectAdjacentRow(boolean down) {
        List<ScrollAreaEntry> rows = this.getNavigableRows();
        int targets = rows.size() + 1;
        int current;
        if ((this.searchBar != null) && this.searchBar.isFocused()) {
            current = 0;
        } else {
            ScrollAreaEntry selected = this.getSelectedRow();
            int rowIndex = (selected != null) ? rows.indexOf(selected) : -1;
            current = (rowIndex >= 0) ? (rowIndex + 1) : -1;
        }
        int target = (current < 0) ? (down ? 1 : (targets - 1)) : Math.floorMod(current + (down ? 1 : -1), targets);
        if (target == 0) {
            this.focusSearchBar();
            return true;
        }
        ScrollAreaEntry row = rows.get(Math.min(target - 1, rows.size() - 1));
        this.defocusSearchBar();
        row.setSelected(true);
        this.ensureRowVisible(row);
        return true;
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {

        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        boolean contextMenuInteracting = this.isUserNavigatingInRightClickContextMenu();

        ExtendedEditBox editBox = this.inlineEditBox;
        if ((editBox != null) && !contextMenuInteracting) {
            if (editBox.isMouseOver(mouseX, mouseY) && this.entriesScrollArea.isMouseOverInnerArea(mouseX, mouseY)) {
                editBox.mouseClicked(event, isDoubleClick);
                return true;
            }
            // Removing an unsaved new entry moves the rows below it, so the click would hit another row than the user aimed at
            if (this.finishInlineEdit(true, false)) return true;
        }

        if ((button == InputConstants.MOUSE_BUTTON_LEFT) && !contextMenuInteracting) {
            this.closeRightClickContextMenu();
        }

        if (!contextMenuInteracting && (button == InputConstants.MOUSE_BUTTON_RIGHT) && this.isInsideEntriesScrollArea(mouseX, mouseY)) {
            ScrollAreaEntry row = this.getRowAt(mouseX, mouseY);
            this.contextMenuTargetIndex = (row instanceof EntryRow entryRow) ? entryRow.entryIndex : -1;
            if (row != null) {
                this.defocusSearchBar();
                row.setSelected(true);
            }
            this.openRightClickContextMenuAtMouse(false);
            return true;
        }

        boolean handled = super.mouseClicked(event, isDoubleClick);
        // A click that starts editing in place lands on the list, which takes the focus afterward, so the edit box gets it back
        ExtendedEditBox newEditBox = this.inlineEditBox;
        if ((newEditBox != null) && (this.getFocused() != newEditBox)) {
            this.setFocused(newEditBox);
            newEditBox.setFocused(true);
        }
        return handled;

    }

    private void onEntryRowClicked(@NotNull EntryRow row, double mouseX, double mouseY, int button) {
        if ((button != InputConstants.MOUSE_BUTTON_LEFT) || this.isUserNavigatingInRightClickContextMenu() || row.isNew()) return;
        this.defocusSearchBar();
        // PiP windows don't pass on if a click is a double-click, so rows count their clicks themselves
        long now = System.currentTimeMillis();
        if ((row.entryIndex == this.lastClickedIndex) && ((now - this.lastClickTime) <= DOUBLE_CLICK_TIME_MS)) {
            this.lastClickedIndex = -1;
            this.resetDrag();
            this.startInlineEdit(row.entryIndex);
            return;
        }
        this.lastClickedIndex = row.entryIndex;
        this.lastClickTime = now;
        this.pressedEntryIndex = row.entryIndex;
        this.pressX = mouseX;
        this.pressY = mouseY;
        this.dragging = false;
    }

    private void onAddEntryRowClicked(int button) {
        if ((button != InputConstants.MOUSE_BUTTON_LEFT) || this.isUserNavigatingInRightClickContextMenu()) return;
        this.startInlineAdd(this.draft.size());
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        ExtendedEditBox editBox = this.inlineEditBox;
        if ((editBox != null) && editBox.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        if ((event.button() == InputConstants.MOUSE_BUTTON_LEFT) && (this.pressedEntryIndex >= 0)) {
            if (!this.dragging && this.canReorder() && (Math.hypot(event.x() - this.pressX, event.y() - this.pressY) >= DRAG_START_DISTANCE)) {
                this.dragging = true;
                this.lastClickedIndex = -1;
                this.closeRightClickContextMenu();
            }
            if (this.dragging) {
                this.dropSlot = this.computeDropSlot(event.y());
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        ExtendedEditBox editBox = this.inlineEditBox;
        if (editBox != null) {
            editBox.mouseReleased(event);
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (this.dragging) {
                this.dropDraggedEntry();
                return true;
            }
            this.resetDrag();
        }
        return super.mouseReleased(event);
    }

    protected boolean isInsideEntriesScrollArea(double mouseX, double mouseY) {
        ScrollArea area = this.entriesScrollArea;
        return UIBase.isXYInArea(mouseX, mouseY, area.getXWithBorder(), area.getYWithBorder(), area.getWidthWithBorder(), area.getHeightWithBorder());
    }

    protected boolean isUserNavigatingInRightClickContextMenu() {
        return (this.rightClickContextMenu != null) && this.rightClickContextMenu.isUserNavigatingInMenu();
    }

    protected boolean hasContextMenuTarget() {
        return this.isValidEntryIndex(this.contextMenuTargetIndex);
    }

    protected void closeRightClickContextMenu() {
        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.contextMenuTargetIndex = -1;
    }

    protected void openRightClickContextMenuAtMouse(boolean reopen) {
        if (this.rightClickContextMenu == null) return;
        int mX = MouseInput.getMouseX();
        if (reopen && !Float.isNaN(this.rightClickContextMenuLastOpenX)) mX = (int) this.rightClickContextMenuLastOpenX;
        int mY = MouseInput.getMouseY();
        if (reopen && !Float.isNaN(this.rightClickContextMenuLastOpenY)) mY = (int) this.rightClickContextMenuLastOpenY;
        if (reopen) {
            this.rightClickContextMenu.supressOpenAnimationForNextOpen();
        }
        ContextMenuHandler.INSTANCE.setAndOpen(this.rightClickContextMenu, mX, mY);
        this.rightClickContextMenuLastOpenX = mX;
        this.rightClickContextMenuLastOpenY = mY;
    }

    /**
     * The row of an entry: its index and its text. The row of a new entry that gets added has no entry index yet.
     */
    protected class EntryRow extends VariableRowEntry {

        /** The index of the entry in the draft, or -1 for a new entry that isn't part of the list yet. */
        protected int entryIndex;
        /** The index the row shows, which is what the entry has (or gets) in the list once the user is done. */
        protected final int displayIndex;
        @NotNull
        protected String entry;
        @NotNull
        private final String displayIndexText;

        protected EntryRow(@NotNull ScrollArea parent, int entryIndex, int displayIndex, @NotNull String entry, int height) {
            super(parent, height);
            this.entryIndex = entryIndex;
            this.displayIndex = displayIndex;
            this.entry = entry;
            this.displayIndexText = String.valueOf(displayIndex);
        }

        protected boolean isNew() {
            return this.entryIndex < 0;
        }

        protected void setEntry(@NotNull String entry) {
            this.entry = entry;
            this.invalidateText();
        }

        @Override
        protected float getLeadingWidth() {
            return ListVariableEditorWindowBody.this.indexGutterWidth;
        }

        @Override
        protected void renderLeading(@NotNull GuiGraphicsExtractor graphics, float x, float centerY) {
            renderIndex(graphics, this.displayIndexText, x, this.getLeadingWidth(), centerY);
        }

        @Override
        protected float getTrailingWidth() {
            return (!this.isNew() && ListVariableEditorWindowBody.this.canReorder()) ? getIconSize() : 0.0F;
        }

        @Override
        protected void renderTrailing(@NotNull GuiGraphicsExtractor graphics, float x, float centerY, int mouseX, int mouseY) {
            // The drag handle shows that rows can get dragged, without cluttering rows the pointer isn't on
            boolean dragged = ListVariableEditorWindowBody.this.dragging && (ListVariableEditorWindowBody.this.pressedEntryIndex == this.entryIndex);
            if (dragged || (this.isHovered() && !ListVariableEditorWindowBody.this.dragging)) {
                renderIcon(graphics, MaterialIcons.DRAG_INDICATOR, x, centerY, getSubtleIconColor());
            }
        }

        @Override
        protected boolean shouldRenderText() {
            ListVariableEditorWindowBody editor = ListVariableEditorWindowBody.this;
            if (editor.inlineEditBox == null) return true;
            return editor.inlineEditIsNew ? !this.isNew() : (this.entryIndex != editor.inlineEditIndex);
        }

        @Override
        protected @NotNull List<Segment> buildSegments() {
            List<Segment> segments = new ArrayList<>();
            if (this.entry.isEmpty()) {
                segments.add(new Segment(I18n.get("fancymenu.overlay.menu_bar.variables.manage.empty_value"), TextStyle.MUTED));
            } else {
                addSingleLineSegments(segments, this.entry, TextStyle.LABEL);
            }
            return segments;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            ListVariableEditorWindowBody.this.onEntryRowClicked(this, mouseX, mouseY, button);
        }

    }

    /**
     * The row at the end of the list that adds a new entry with one click.
     */
    protected class AddEntryRow extends VariableRowEntry {

        protected AddEntryRow(@NotNull ScrollArea parent, int height) {
            super(parent, height);
        }

        @Override
        protected float getLeadingWidth() {
            return ListVariableEditorWindowBody.this.indexGutterWidth;
        }

        @Override
        protected void renderLeading(@NotNull GuiGraphicsExtractor graphics, float x, float centerY) {
            boolean highlighted = this.isHovered() || this.isSelected();
            renderIcon(graphics, MaterialIcons.ADD, x + this.getLeadingWidth() - getIconSize(), centerY, highlighted ? UIBase.getUITheme().getUIIconColor(1.0F) : getSubtleIconColor());
        }

        @Override
        protected @NotNull List<Segment> buildSegments() {
            return List.of(new Segment(I18n.get("fancymenu.overlay.menu_bar.variables.manage.edit_list.add_entry"), TextStyle.MUTED));
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            ListVariableEditorWindowBody.this.onAddEntryRowClicked(button);
        }

    }

}
