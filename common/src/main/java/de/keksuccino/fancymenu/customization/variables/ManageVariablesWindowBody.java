package de.keksuccino.fancymenu.customization.variables;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.aiexport.AiExportMenus;
import de.keksuccino.fancymenu.customization.aiexport.exporters.VariablesAiExporter;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.input.InputUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ManageVariablesWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;
    private static final int HISTORY_LIMIT = 500;
    private static final int LIST_ENTRY_TOP_DOWN_BORDER = 1;
    private static final int LIST_ENTRY_OUTER_PADDING = 3;
    private static final int LIST_TOP_SPACER_HEIGHT = 5;
    private static final long DOUBLE_CLICK_TIME_MS = 500L;
    private static final Comparator<UserVariableSnapshot> SNAPSHOT_ORDER = Comparator
            .comparing(UserVariableSnapshot::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(UserVariableSnapshot::name);

    protected Consumer<List<Variable>> callback;

    protected ScrollArea variableListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;
    @Nullable
    protected ExtendedButton doneButton;
    protected ContextMenu rightClickContextMenu;
    protected float rightClickContextMenuLastOpenX = Float.NaN;
    protected float rightClickContextMenuLastOpenY = Float.NaN;
    @Nullable
    protected String contextMenuTargetVariableName;
    @Nullable
    protected UserVariableSnapshot variableClipboard;
    @Nullable
    private String lastClickedVariableName;
    private long lastClickTime = 0L;
    private final Deque<VariablesSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<VariablesSnapshot> redoHistory = new ArrayDeque<>();

    public ManageVariablesWindowBody(@NotNull Consumer<List<Variable>> callback) {
        super(Component.translatable("fancymenu.overlay.menu_bar.variables.manage"));
        this.callback = callback;
        // Rows fill the width of the list and cut off long values, so the list never scrolls sideways
        this.variableListScrollArea.makeEntriesWidthOfArea = true;
        this.variableListScrollArea.makeAllEntriesWidthOfWidestEntry = false;
    }

    @Override
    protected void init() {
        boolean blur = UIBase.shouldBlur();
        int contentTop = getLabeledContentTop();
        this.variableListScrollArea.setSetupForBlurInterface(blur);
        this.updateRightClickContextMenu(false);

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, contentTop + 1, 200, 20 - 2, Component.empty());
        this.searchBar.setHintFancyMenu(consumes -> Component.translatable("fancymenu.variables.manage_variables.screen.search_variable"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateVariablesList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar, blur);
        this.setupInitialFocusWidget(this, this.searchBar);

        // Set positions for scroll area
        this.variableListScrollArea.setWidth(this.width - 40, true);
        this.variableListScrollArea.setHeight(this.height - contentTop - 25 - CONTENT_MARGIN, true);
        this.variableListScrollArea.setX(20, true);
        this.variableListScrollArea.setY(contentTop + 25, true);
        this.addRenderableWidget(this.variableListScrollArea);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.done"), (button) -> this.triggerDoneAction());
        this.doneButton.setNavigatable(false);
        this.addRenderableWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, blur);

        this.refreshVariablesList();

    }

    protected void updateRightClickContextMenu(boolean reopen) {
        boolean wasOpen = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.rightClickContextMenu = new ContextMenu();

        this.rightClickContextMenu.addClickableEntry("add_text_variable", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.add_text_variable"), (menu, entry) -> {
                    menu.closeMenuChain();
                    this.requestAddVariable(VariableType.TEXT);
                }).setIcon(getTypeIcon(VariableType.TEXT));

        this.rightClickContextMenu.addClickableEntry("add_list_variable", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.add_list_variable"), (menu, entry) -> {
                    menu.closeMenuChain();
                    this.requestAddVariable(VariableType.LIST);
                }).setIcon(getTypeIcon(VariableType.LIST));

        this.rightClickContextMenu.addSeparatorEntry("separator_after_add")
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTargetVariable());

        this.rightClickContextMenu.addClickableEntry("set_value", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"), (menu, entry) -> {
                    String targetName = this.contextMenuTargetVariableName;
                    if (targetName == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.requestEditVariable(targetName);
                }).addIsVisibleSupplier((menu, entry) -> this.isContextMenuTargetOfType(VariableType.TEXT))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.enter"))
                .setIcon(MaterialIcons.EDIT);

        this.rightClickContextMenu.addClickableEntry("edit_list", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.edit_list"), (menu, entry) -> {
                    String targetName = this.contextMenuTargetVariableName;
                    if (targetName == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.requestEditVariable(targetName);
                }).addIsVisibleSupplier((menu, entry) -> this.isContextMenuTargetOfType(VariableType.LIST))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.enter"))
                .setIcon(MaterialIcons.EDIT);

        this.rightClickContextMenu.addClickableEntry("convert_to_list", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_list"), (menu, entry) -> {
                    String targetName = this.contextMenuTargetVariableName;
                    if (targetName == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.requestConvertToList(targetName);
                }).addIsVisibleSupplier((menu, entry) -> this.isContextMenuTargetOfType(VariableType.TEXT))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_list.desc")))
                .setIcon(MaterialIcons.SWAP_HORIZ);

        this.rightClickContextMenu.addClickableEntry("convert_to_text", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_text"), (menu, entry) -> {
                    String targetName = this.contextMenuTargetVariableName;
                    if (targetName == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.requestConvertToText(targetName);
                }).addIsVisibleSupplier((menu, entry) -> this.isContextMenuTargetOfType(VariableType.LIST))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.convert_to_text.desc")))
                .setIcon(MaterialIcons.SWAP_HORIZ);

        this.rightClickContextMenu.addClickableEntry("toggle_reset_on_launch", this.getContextMenuToggleResetOnLaunchLabel(), (menu, entry) -> {
                    Variable selectedVariable = this.getContextMenuTargetVariable();
                    if (selectedVariable == null) {
                        return;
                    }
                    VariablesSnapshot beforeSnapshot = this.captureCurrentState();
                    selectedVariable.toggleResetOnLaunch();
                    this.createUndoPointIfChanged(beforeSnapshot);
                }).setLabelSupplier((menu, entry) -> this.getContextMenuToggleResetOnLaunchLabel())
                .addIsActiveSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .setIcon(MaterialIcons.TOGGLE_ON);

        this.rightClickContextMenu.addClickableEntry("delete_variable", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.delete_variable"), (menu, entry) -> {
                    String targetName = this.contextMenuTargetVariableName;
                    if (targetName == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.requestDeleteVariable(targetName);
                }).addIsActiveSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.delete"))
                .setIcon(MaterialIcons.DELETE);

        this.rightClickContextMenu.addSeparatorEntry("separator_before_copy_paste");

        this.rightClickContextMenu.addClickableEntry("copy_variable", Component.translatable("fancymenu.editor.edit.copy"), (menu, entry) -> {
                    Variable selectedVariable = this.getContextMenuTargetVariable();
                    if (selectedVariable == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.variableClipboard = selectedVariable.snapshot();
                }).addIsActiveSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"))
                .setIcon(MaterialIcons.CONTENT_COPY);

        this.rightClickContextMenu.addClickableEntry("paste_variable", Component.translatable("fancymenu.editor.edit.paste"), (menu, entry) -> {
                    menu.closeMenuChain();
                    this.pasteVariableFromClipboard();
                }).addIsActiveSupplier((menu, entry) -> this.canPasteVariableFromClipboard())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.paste"))
                .setIcon(MaterialIcons.CONTENT_PASTE);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_copy_paste");

        this.rightClickContextMenu.addClickableEntry("undo", Component.translatable("fancymenu.editor.edit.undo"), (menu, entry) -> {
                    if (this.undo()) {
                        this.updateRightClickContextMenu(true);
                    }
                }).addIsActiveSupplier((menu, entry) -> this.canUndo())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.undo"))
                .setIcon(MaterialIcons.UNDO);

        this.rightClickContextMenu.addClickableEntry("redo", Component.translatable("fancymenu.editor.edit.redo"), (menu, entry) -> {
                    if (this.redo()) {
                        this.updateRightClickContextMenu(true);
                    }
                }).addIsActiveSupplier((menu, entry) -> this.canRedo())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.redo"))
                .setIcon(MaterialIcons.REDO);

        this.rightClickContextMenu.addSeparatorEntry("separator_before_ai_export");

        AiExportMenus.addExportSubMenu(this.rightClickContextMenu, "export_variable_for_ai", Component.translatable("fancymenu.ai_export.variables.export_selected"), () -> {
                    Variable selectedVariable = this.getContextMenuTargetVariable();
                    return (selectedVariable != null) ? VariablesAiExporter.exportSingle(selectedVariable) : VariablesAiExporter.exportAll();
                }).addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTargetVariable());

        AiExportMenus.addExportSubMenu(this.rightClickContextMenu, "export_all_variables_for_ai", Component.translatable("fancymenu.ai_export.variables.export_all"), VariablesAiExporter::exportAll);

        if (reopen || wasOpen) {
            this.openRightClickContextMenuAtMouse(true);
        }

    }

    @Override
    public void onWindowClosedExternally() {
        this.closeRightClickContextMenu();
        this.callback.accept(VariableHandler.getVariables());
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();

        RenderSystem.enableBlend();

        int contentTop = getLabeledContentTop();
        this.variableListScrollArea.setWidth(this.width - 40, true);
        this.variableListScrollArea.setHeight(this.height - contentTop - 25 - CONTENT_MARGIN, true);
        this.variableListScrollArea.setX(20, true);
        this.variableListScrollArea.setY(contentTop + 25, true);

        int buttonsRightEdge = this.width - 20;
        if (this.doneButton != null) {
            int buttonY = Math.max(CONTENT_MARGIN, (int)(this.variableListScrollArea.getYWithBorder() - this.doneButton.getHeight() - 5));
            this.doneButton.setX(buttonsRightEdge - this.doneButton.getWidth());
            this.doneButton.setY(buttonY);
        }
        if ((this.searchBar != null) && (this.doneButton != null)) {
            int searchBarX = 20 + 1;
            int searchBarY = contentTop + 1;
            int searchBarWidth = Math.max(120, this.doneButton.getX() - 5 - searchBarX);
            this.searchBar.setX(searchBarX);
            this.searchBar.setY(searchBarY);
            this.searchBar.setWidth(searchBarWidth);
        }

        int textColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt()
                : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
        int searchBarTop = (this.searchBar != null) ? this.searchBar.getY() : (contentTop + 1);
        float labelY = searchBarTop - UIBase.getUITextHeightNormal() - UIBase.getAreaLabelVerticalPadding();
        UIBase.renderText(graphics, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.variables"), 20, labelY, textColor);

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean contextMenuActive = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        String keyName = this.getLetterKeyName(keyCode, scanCode);
        boolean shortcutModifierDown = InputUtils.isGuiShortcutModifierDown(modifiers);
        boolean shiftDown = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean altDown = (modifiers & GLFW.GLFW_MOD_ALT) != 0;

        if (!contextMenuActive && (keyCode == InputConstants.KEY_BACKSPACE)) {
            if (this.searchBar != null) {
                if (!this.searchBar.isFocused()) {
                    this.focusSearchBar();
                }
                this.searchBar.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }

        if (shortcutModifierDown && !shiftDown && !altDown && "s".equals(keyName)) {
            if ((this.doneButton != null) && this.doneButton.visible && this.doneButton.active) {
                this.doneButton.onPress();
                return true;
            }
        }

        if (shortcutModifierDown && "z".equals(keyName)) {
            if (shiftDown ? this.redo() : this.undo()) {
                return true;
            }
        }

        if (shortcutModifierDown && "y".equals(keyName)) {
            if (this.redo()) {
                return true;
            }
        }

        boolean searchBarFocused = (this.searchBar != null) && this.searchBar.isFocused();
        if (!contextMenuActive && !searchBarFocused && shortcutModifierDown && "c".equals(keyName)) {
            if (this.copySelectedVariableToClipboard()) {
                return true;
            }
        }

        if (!contextMenuActive && !searchBarFocused && shortcutModifierDown && "v".equals(keyName)) {
            if (this.pasteVariableFromClipboard()) {
                return true;
            }
        }

        if (keyCode == InputConstants.KEY_TAB) {
            return true;
        }

        if ((keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
            if (contextMenuActive) {
                ContextMenuHandler.INSTANCE.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
            return this.selectAdjacentVariableEntry(keyCode == InputConstants.KEY_DOWN);
        }

        // Left and right only move the cursor of the search bar
        if (!contextMenuActive && !searchBarFocused && ((keyCode == InputConstants.KEY_LEFT) || (keyCode == InputConstants.KEY_RIGHT))) {
            return true;
        }

        if (!contextMenuActive && ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER))) {
            VariableScrollEntry selectedEntry = this.getSelectedEntry();
            if (selectedEntry != null) {
                this.requestEditVariable(selectedEntry.getVariableName());
                return true;
            }
        }

        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode == InputConstants.KEY_DELETE) {
            if (this.getSelectedEntry() != null) {
                this.requestDeleteSelectedVariable();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean contextMenuActive = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        if (!contextMenuActive && this.shouldRouteTypedCharacterToSearchBar(codePoint, modifiers) && (this.searchBar != null) && !this.searchBar.isFocused()) {
            this.focusSearchBar();
            if (this.searchBar.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @NotNull
    protected String getLetterKeyName(int keyCode, int scanCode) {
        String keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        if (keyName == null) {
            return "";
        }
        return keyName.toLowerCase(Locale.ROOT);
    }

    private void focusSearchBar() {
        if (this.searchBar == null) {
            return;
        }
        this.setFocused(this.searchBar);
        this.searchBar.setFocused(true);
    }

    private void defocusSearchBar() {
        if (this.searchBar == null) {
            return;
        }
        this.searchBar.setFocused(false);
        if (this.getFocused() == this.searchBar) {
            this.setFocused(null);
        }
    }

    private void clearSelectedVariableEntries() {
        for (ScrollAreaEntry entry : this.variableListScrollArea.getEntries()) {
            if (entry instanceof VariableScrollEntry) {
                entry.setSelected(false);
            }
        }
    }

    private boolean selectAdjacentVariableEntry(boolean moveDown) {
        List<VariableScrollEntry> entries = this.getVariableEntriesForNavigation();
        boolean searchBarAvailable = this.searchBar != null;
        boolean searchBarFocused = searchBarAvailable && this.searchBar.isFocused();
        int totalNavigationTargets = entries.size() + (searchBarAvailable ? 1 : 0);

        if (totalNavigationTargets <= 0) {
            return false;
        }

        int currentIndex = -1;
        if (searchBarAvailable && searchBarFocused) {
            currentIndex = 0;
        } else {
            VariableScrollEntry selectedEntry = this.getSelectedEntry();
            if (selectedEntry != null) {
                int selectedEntryIndex = entries.indexOf(selectedEntry);
                if (selectedEntryIndex != -1) {
                    currentIndex = searchBarAvailable ? selectedEntryIndex + 1 : selectedEntryIndex;
                }
            }
        }

        int targetIndex;
        if (currentIndex == -1) {
            targetIndex = moveDown ? 0 : (totalNavigationTargets - 1);
        } else {
            targetIndex = currentIndex + (moveDown ? 1 : -1);
            if (targetIndex < 0) {
                targetIndex = totalNavigationTargets - 1;
            } else if (targetIndex >= totalNavigationTargets) {
                targetIndex = 0;
            }
        }

        if (searchBarAvailable && (targetIndex == 0)) {
            this.clearSelectedVariableEntries();
            this.focusSearchBar();
            return true;
        }

        int entryIndex = searchBarAvailable ? targetIndex - 1 : targetIndex;
        if ((entryIndex < 0) || (entryIndex >= entries.size())) {
            return false;
        }
        VariableScrollEntry target = entries.get(entryIndex);
        this.defocusSearchBar();
        target.setSelected(true);
        this.scrollVariableEntryIntoView(target);
        return true;
    }

    private boolean shouldRouteTypedCharacterToSearchBar(char codePoint, int modifiers) {
        if (InputUtils.isGuiShortcutModifierDown(modifiers) || (modifiers & GLFW.GLFW_MOD_ALT) != 0) {
            return false;
        }
        return !Character.isISOControl(codePoint);
    }

    @NotNull
    private List<VariableScrollEntry> getVariableEntriesForNavigation() {
        List<VariableScrollEntry> entries = new ArrayList<>();
        for (ScrollAreaEntry entry : this.variableListScrollArea.getEntries()) {
            if (entry instanceof VariableScrollEntry variableEntry) {
                entries.add(variableEntry);
            }
        }
        return entries;
    }

    private void scrollVariableEntryIntoView(@NotNull ScrollAreaEntry entry) {
        List<ScrollAreaEntry> entries = this.variableListScrollArea.getEntries();
        int totalHeight = 0;
        for (ScrollAreaEntry scrollEntry : entries) {
            totalHeight += (int)scrollEntry.getHeight();
        }
        int visibleHeight = (int)this.variableListScrollArea.getInnerHeight();
        if (totalHeight <= visibleHeight) {
            return;
        }
        int offset = 0;
        for (ScrollAreaEntry scrollEntry : entries) {
            if (scrollEntry == entry) {
                break;
            }
            offset += (int)scrollEntry.getHeight();
        }
        int entryCenter = offset + ((int)entry.getHeight() / 2);
        int targetOffset = Math.max(0, entryCenter - (visibleHeight / 2));
        int maxScroll = Math.max(1, totalHeight - visibleHeight);
        float scroll = Math.min(1.0F, (float)targetOffset / (float)maxScroll);
        this.variableListScrollArea.verticalScrollBar.setScroll(scroll);
        this.variableListScrollArea.updateEntries(null);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.hasVariableEntriesInList()) {
            return;
        }
        boolean searching = (this.searchBar != null) && !this.searchBar.getValue().isBlank();
        Component hint = Component.translatable(searching ? "fancymenu.overlay.menu_bar.variables.manage.no_search_results" : "fancymenu.overlay.menu_bar.variables.manage.empty_hint");
        float hintWidth = UIBase.getUITextWidthNormal(hint);
        float hintX = this.variableListScrollArea.getInnerX() + (this.variableListScrollArea.getInnerWidth() / 2F) - (hintWidth / 2F);
        float hintY = this.variableListScrollArea.getInnerY() + (this.variableListScrollArea.getInnerHeight() / 2F) - (UIBase.getUITextHeightNormal() / 2F);
        UIBase.renderText(graphics, hint, hintX, hintY, VariableRowEntry.TextStyle.MUTED.getColor());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean contextMenuInteracting = this.isUserNavigatingInRightClickContextMenu();

        if ((button == 0) && !contextMenuInteracting && (this.rightClickContextMenu != null)) {
            this.rightClickContextMenu.closeMenu();
            this.clearContextMenuTarget();
        }

        if (!contextMenuInteracting && (button == 1) && this.isInsideVariableScrollArea((int)mouseX, (int)mouseY)) {
            ScrollAreaEntry targetEntry = this.getVariableEntryAt(mouseX, mouseY);
            this.setContextMenuTarget(targetEntry);
            if (targetEntry != null) {
                targetEntry.setSelected(true);
            }
            this.openRightClickContextMenuAtMouse(false);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Opens the editor of a variable when its row gets clicked twice in a row.<br>
     * PiP windows don't pass on if a click is a double-click, so the rows count their clicks themselves.
     */
    private void onVariableEntryClicked(@NotNull VariableScrollEntry entry, int button) {
        if ((button != 0) || this.isUserNavigatingInRightClickContextMenu()) {
            return;
        }
        long now = System.currentTimeMillis();
        String name = entry.getVariableName();
        if (name.equals(this.lastClickedVariableName) && ((now - this.lastClickTime) <= DOUBLE_CLICK_TIME_MS)) {
            this.lastClickedVariableName = null;
            this.requestEditVariable(name);
            return;
        }
        this.lastClickedVariableName = name;
        this.lastClickTime = now;
    }

    private void triggerDoneAction() {
        this.closeRightClickContextMenu();
        this.callback.accept(VariableHandler.getVariables());
        this.closeWindow();
    }

    protected boolean isUserNavigatingInRightClickContextMenu() {
        return (this.rightClickContextMenu != null) && this.rightClickContextMenu.isUserNavigatingInMenu();
    }

    protected boolean isInsideVariableScrollArea(int mouseX, int mouseY) {
        return UIBase.isXYInArea(mouseX, mouseY, this.variableListScrollArea.getXWithBorder(), this.variableListScrollArea.getYWithBorder(), this.variableListScrollArea.getWidthWithBorder(), this.variableListScrollArea.getHeightWithBorder());
    }

    @Nullable
    protected ScrollAreaEntry getVariableEntryAt(double mouseX, double mouseY) {
        if (!this.variableListScrollArea.isMouseOverInnerArea(mouseX, mouseY)) {
            return null;
        }
        for (ScrollAreaEntry entry : this.variableListScrollArea.getEntries()) {
            if (UIBase.isXYInArea(mouseX, mouseY, entry.getX(), entry.getY(), this.variableListScrollArea.getInnerWidth(), entry.getHeight())) {
                if (entry instanceof VariableScrollEntry) {
                    return entry;
                }
                return null;
            }
        }
        return null;
    }

    protected void setContextMenuTarget(@Nullable ScrollAreaEntry targetEntry) {
        this.clearContextMenuTarget();
        if (targetEntry instanceof VariableScrollEntry variableEntry) {
            this.contextMenuTargetVariableName = variableEntry.getVariableName();
        }
    }

    protected void clearContextMenuTarget() {
        this.contextMenuTargetVariableName = null;
    }

    protected void closeRightClickContextMenu() {
        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.clearContextMenuTarget();
    }

    protected boolean hasContextMenuTargetVariable() {
        return this.getContextMenuTargetVariable() != null;
    }

    protected boolean isContextMenuTargetOfType(@NotNull VariableType type) {
        return (this.contextMenuTargetVariableName != null) && (VariableHandler.getVariableType(this.contextMenuTargetVariableName) == type);
    }

    protected void openRightClickContextMenuAtMouse(boolean reopen) {
        if (this.rightClickContextMenu == null) {
            return;
        }
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

    @NotNull
    protected Component getContextMenuToggleResetOnLaunchLabel() {
        Variable selectedVariable = this.getContextMenuTargetVariable();
        CommonCycles.CycleEnabledDisabled selectedValue = (selectedVariable == null)
                ? CommonCycles.CycleEnabledDisabled.DISABLED
                : CommonCycles.CycleEnabledDisabled.getByBoolean(selectedVariable.isResetOnLaunch());
        return CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.variables.manage.clear_on_launch", selectedValue).getCycleComponent();
    }

    @Nullable
    protected Variable getContextMenuTargetVariable() {
        if (this.contextMenuTargetVariableName == null) {
            return null;
        }
        return VariableHandler.getVariable(this.contextMenuTargetVariableName);
    }

    protected void requestAddVariable(@NotNull VariableType type) {
        TextInputWindowBody s = new TextInputWindowBody(CharacterFilter.buildOnlyLowercaseFileNameFilter(), (call) -> {
            if ((call == null) || call.isBlank()) {
                return;
            }
            VariablesSnapshot beforeSnapshot = this.captureCurrentState();
            boolean added = (type == VariableType.LIST) ? VariableHandler.setListIfAbsent(call, List.of()) : VariableHandler.setVariableIfAbsent(call, "");
            if (added) {
                this.contextMenuTargetVariableName = call;
                this.refreshVariablesList();
                this.selectAndRevealVariable(call);
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        });
        s.setTextValidator(consumes -> !consumes.getText().isBlank() && !VariableHandler.variableExists(consumes.getText()));
        s.setTextValidatorUserFeedback(UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.add_variable.name_rules")));
        Dialogs.openGeneric(s,
                Component.translatable("fancymenu.overlay.menu_bar.variables.manage.add_variable.input_name"),
                null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                .getSecond().setIcon(getTypeIcon(type));
    }

    /**
     * Opens the editor that fits the type of the variable: the value dialog for text variables, the list editor for lists.
     */
    protected void requestEditVariable(@NotNull String name) {
        VariableType type = VariableHandler.getVariableType(name);
        if (type == VariableType.LIST) {
            this.openListEditor(name);
        } else if (type == VariableType.TEXT) {
            this.requestSetVariableValue(name);
        }
    }

    protected void requestSetVariableValue(@NotNull String name) {
        String value = VariableHandler.getVariableValue(name);
        if (value == null) {
            return;
        }
        TextInputWindowBody s = new TextInputWindowBody(null, (call) -> {
            if (call != null) {
                Variable variable = VariableHandler.getVariable(name);
                if (variable == null) {
                    return;
                }
                VariablesSnapshot beforeSnapshot = this.captureCurrentState();
                variable.setValue(call);
                this.refreshVariablesList();
                this.restoreSelection(name);
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        });
        Dialogs.openGeneric(s,
                Component.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"),
                null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                .getSecond().setIcon(getTypeIcon(VariableType.TEXT));
        s.setText(value);
    }

    /**
     * Opens the list editor in place of this window. It edits a copy of the entries, which only becomes the value of the variable
     * when the user is done, so all edits of one editing session are a single undo step here.
     */
    protected void openListEditor(@NotNull String name) {
        List<String> entries = VariableHandler.getListEntries(name);
        if (entries == null) {
            return;
        }
        ListVariableEditorWindowBody editor = new ListVariableEditorWindowBody(name, entries, result -> {
            if (result == null) {
                return;
            }
            VariablesSnapshot beforeSnapshot = this.captureCurrentState();
            ListEditResult editResult = VariableHandler.setList(name, result);
            if ((editResult == ListEditResult.NOT_A_LIST) || (editResult == ListEditResult.UNAVAILABLE)) {
                LOGGER.warn("[FANCYMENU] Failed to apply the edited entries of list variable '{}': {}", name, editResult);
            }
            this.refreshVariablesList();
            this.restoreSelection(name);
            this.createUndoPointIfChanged(beforeSnapshot);
        });
        this.openChildWindow(ListVariableEditorWindowBody.openInWindow(editor, this.getWindow()));
    }

    protected void requestConvertToList(@NotNull String name) {
        String value = VariableHandler.getVariableValue(name);
        if (value == null) {
            return;
        }
        ConvertToListWindowBody dialog = new ConvertToListWindowBody(value, entries -> {
            if (entries == null) {
                return;
            }
            VariablesSnapshot beforeSnapshot = this.captureCurrentState();
            if (VariableHandler.convertToList(name, entries)) {
                this.refreshVariablesList();
                this.restoreSelection(name);
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        });
        ConvertToListWindowBody.openInWindow(dialog);
    }

    protected void requestConvertToText(@NotNull String name) {
        List<String> entries = VariableHandler.getListEntries(name);
        if (entries == null) {
            return;
        }
        ConvertToTextWindowBody dialog = new ConvertToTextWindowBody(entries, text -> {
            if (text == null) {
                return;
            }
            VariablesSnapshot beforeSnapshot = this.captureCurrentState();
            if (VariableHandler.convertToText(name, text)) {
                this.refreshVariablesList();
                this.restoreSelection(name);
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        });
        ConvertToTextWindowBody.openInWindow(dialog);
    }

    /**
     * Shows the child window in place of this window, at the same position and size, and shows this window again once the child closes.
     */
    private void openChildWindow(@NotNull PiPWindow childWindow) {
        PiPWindow parentWindow = this.getWindow();
        if (parentWindow == null) {
            return;
        }
        childWindow.setBounds(parentWindow.getX(), parentWindow.getY(), parentWindow.getWidth(), parentWindow.getHeight());
        childWindow.setMaximized(parentWindow.isMaximized());
        parentWindow.setVisible(false);
        // Runs after the close callback of the window handler, which removed the child already, so this window can take the focus back
        childWindow.addCloseCallback(() -> {
            parentWindow.setMaximized(childWindow.isMaximized());
            if (!childWindow.isMaximized()) {
                // The child might have been moved or resized, and this window takes its place again without jumping
                parentWindow.setBounds(childWindow.getX(), childWindow.getY(), childWindow.getWidth(), childWindow.getHeight());
            }
            parentWindow.setVisible(true);
            PiPWindowHandler.INSTANCE.bringToFront(parentWindow);
        });
    }

    protected boolean copySelectedVariableToClipboard() {
        VariableScrollEntry selectedEntry = this.getSelectedEntry();
        if (selectedEntry == null) {
            return false;
        }
        Variable variable = VariableHandler.getVariable(selectedEntry.getVariableName());
        if (variable == null) {
            return false;
        }
        this.variableClipboard = variable.snapshot();
        return true;
    }

    protected boolean canPasteVariableFromClipboard() {
        return this.variableClipboard != null;
    }

    protected boolean pasteVariableFromClipboard() {
        if (this.variableClipboard == null) {
            return false;
        }
        VariablesSnapshot beforeSnapshot = this.captureCurrentState();
        String pastedVariableName = VariableHandler.createVariableWithUniqueCopyName(this.variableClipboard);
        if (pastedVariableName == null) return false;
        this.contextMenuTargetVariableName = pastedVariableName;
        this.refreshVariablesList();
        this.selectAndRevealVariable(pastedVariableName);
        this.createUndoPointIfChanged(beforeSnapshot);
        return true;
    }

    @Nullable
    protected ManageVariablesWindowBody.VariableScrollEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.variableListScrollArea.getEntries()) {
            if (e instanceof VariableScrollEntry s) {
                if (s.isSelected()) return s;
            }
        }
        return null;
    }

    /**
     * Searches the name of the variable, the value of text variables and the entries of lists.
     *
     * @param search The search text in lower case, or {@code null} if nothing gets searched.
     */
    protected boolean variableFitsSearchValue(@NotNull UserVariableSnapshot variable, @Nullable String search) {
        if (search == null) return true;
        if (variable.name().toLowerCase(Locale.ROOT).contains(search)) return true;
        if (!variable.isList()) return variable.value().toLowerCase(Locale.ROOT).contains(search);
        for (String entry : variable.listEntries()) {
            if (entry.toLowerCase(Locale.ROOT).contains(search)) return true;
        }
        return false;
    }

    protected void updateVariablesList() {

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        String search = searchValue.isBlank() ? null : searchValue.toLowerCase(Locale.ROOT);

        this.variableListScrollArea.clearEntries();
        CellScreen.SpacerScrollAreaEntry spacer = new CellScreen.SpacerScrollAreaEntry(this.variableListScrollArea, LIST_TOP_SPACER_HEIGHT);
        spacer.setSelectable(false);
        spacer.selectOnClick = false;
        spacer.setPlayClickSound(false);
        spacer.setBackgroundColorNormal(() -> DrawableColor.FULLY_TRANSPARENT);
        spacer.setBackgroundColorHover(() -> DrawableColor.FULLY_TRANSPARENT);
        this.variableListScrollArea.addEntry(spacer);

        // One snapshot of all variables, instead of reading every variable on its own
        List<UserVariableSnapshot> variables = new ArrayList<>(VariableHandler.getVariableSnapshots());
        variables.sort(SNAPSHOT_ORDER);

        for (UserVariableSnapshot v : variables) {
            if (!this.variableFitsSearchValue(v, search)) continue;
            VariableScrollEntry e = new VariableScrollEntry(this.variableListScrollArea, v, this::onVariableEntryClicked);
            e.setHeight(this.getListEntryHeight());
            this.variableListScrollArea.addEntry(e);
        }
    }

    /**
     * Rebuilds the rows after the variables changed, keeping the scroll position.
     */
    protected void refreshVariablesList() {
        float scroll = this.variableListScrollArea.verticalScrollBar.getScroll();
        this.updateVariablesList();
        this.variableListScrollArea.verticalScrollBar.setScroll(scroll);
        this.variableListScrollArea.updateEntries(null);
        if ((this.contextMenuTargetVariableName != null) && (this.getContextMenuTargetVariable() == null)) {
            this.clearContextMenuTarget();
        }
    }

    protected void requestDeleteSelectedVariable() {
        VariableScrollEntry e = this.getSelectedEntry();
        if (e == null) return;
        this.requestDeleteVariable(e.getVariableName());
    }

    protected void requestDeleteVariable(@NotNull String name) {
        VariablesSnapshot beforeSnapshot = this.captureCurrentState();
        VariableHandler.removeVariable(name);
        this.refreshVariablesList();
        this.createUndoPointIfChanged(beforeSnapshot);
    }

    protected int getListEntryHeight() {
        return (int)(UIBase.getUITextHeightNormal()
                + (LIST_ENTRY_TOP_DOWN_BORDER * 2)
                + (LIST_ENTRY_OUTER_PADDING * 2));
    }

    protected boolean hasVariableEntriesInList() {
        for (ScrollAreaEntry entry : this.variableListScrollArea.getEntries()) {
            if (entry instanceof VariableScrollEntry) {
                return true;
            }
        }
        return false;
    }

    private boolean canUndo() {
        return !this.undoHistory.isEmpty();
    }

    private boolean canRedo() {
        return !this.redoHistory.isEmpty();
    }

    private boolean undo() {
        if (!this.canUndo()) {
            return false;
        }
        VariablesSnapshot snapshot = this.undoHistory.pop();
        this.redoHistory.push(this.captureCurrentState());
        this.trimHistory(this.redoHistory);
        this.applySnapshot(snapshot);
        return true;
    }

    private boolean redo() {
        if (!this.canRedo()) {
            return false;
        }
        VariablesSnapshot snapshot = this.redoHistory.pop();
        this.undoHistory.push(this.captureCurrentState());
        this.trimHistory(this.undoHistory);
        this.applySnapshot(snapshot);
        return true;
    }

    private void createUndoPointIfChanged(@NotNull VariablesSnapshot previousState) {
        if (this.captureVariableSnapshots().equals(previousState.variables())) {
            return;
        }
        this.undoHistory.push(previousState);
        this.trimHistory(this.undoHistory);
        this.redoHistory.clear();
    }

    private void trimHistory(@NotNull Deque<VariablesSnapshot> history) {
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    private @NotNull VariablesSnapshot captureCurrentState() {
        VariableScrollEntry selectedEntry = this.getSelectedEntry();
        String selectedVariableName = (selectedEntry != null) ? selectedEntry.getVariableName() : null;
        return new VariablesSnapshot(
                this.captureVariableSnapshots(),
                this.variableListScrollArea.verticalScrollBar.getScroll(),
                this.variableListScrollArea.horizontalScrollBar.getScroll(),
                selectedVariableName,
                this.contextMenuTargetVariableName
        );
    }

    /**
     * Snapshots keep the type, value, entries and settings of every variable, so undo and redo bring back lists as lists.
     */
    private @NotNull List<UserVariableSnapshot> captureVariableSnapshots() {
        List<UserVariableSnapshot> variables = new ArrayList<>(VariableHandler.getVariableSnapshots());
        variables.sort(SNAPSHOT_ORDER);
        return variables;
    }

    private void applySnapshot(@NotNull VariablesSnapshot snapshot) {
        VariableHandler.replaceVariables(snapshot.variables());
        this.updateVariablesList();
        this.variableListScrollArea.verticalScrollBar.setScroll(Mth.clamp(snapshot.verticalScroll(), 0.0F, 1.0F));
        this.variableListScrollArea.horizontalScrollBar.setScroll(Mth.clamp(snapshot.horizontalScroll(), 0.0F, 1.0F));
        this.variableListScrollArea.updateEntries(null);
        this.restoreSelection(snapshot.selectedVariableName());
        this.contextMenuTargetVariableName = snapshot.contextMenuTargetVariableName();
        if (this.getContextMenuTargetVariable() == null) {
            this.clearContextMenuTarget();
        }
    }

    private void restoreSelection(@Nullable String selectedVariableName) {
        VariableScrollEntry entry = this.findEntry(selectedVariableName);
        if (entry != null) {
            entry.setSelected(true);
        }
    }

    /**
     * Selects the row of the variable and scrolls to it, which is what adding a variable does, so the new variable never ends up out of view.
     */
    private void selectAndRevealVariable(@NotNull String name) {
        VariableScrollEntry entry = this.findEntry(name);
        if (entry != null) {
            this.defocusSearchBar();
            entry.setSelected(true);
            this.scrollVariableEntryIntoView(entry);
        }
    }

    @Nullable
    private VariableScrollEntry findEntry(@Nullable String variableName) {
        if (variableName == null) {
            return null;
        }
        for (ScrollAreaEntry entry : this.variableListScrollArea.getEntries()) {
            if ((entry instanceof VariableScrollEntry variableEntry) && variableName.equals(variableEntry.getVariableName())) {
                return variableEntry;
            }
        }
        return null;
    }

    @NotNull
    static MaterialIcon getTypeIcon(@NotNull VariableType type) {
        return (type == VariableType.LIST) ? MaterialIcons.FORMAT_LIST_BULLETED : MaterialIcons.TEXT_FIELDS;
    }

    /**
     * @return The localized number of list entries, like {@code 3 entries}.
     */
    @NotNull
    static String getEntryCountText(int count) {
        return (count == 1) ? I18n.get("fancymenu.overlay.menu_bar.variables.manage.entry_count.one") : I18n.get("fancymenu.overlay.menu_bar.variables.manage.entry_count.other", count);
    }

    private record VariablesSnapshot(@NotNull List<UserVariableSnapshot> variables, float verticalScroll, float horizontalScroll, @Nullable String selectedVariableName, @Nullable String contextMenuTargetVariableName) {
    }

    /**
     * The row of a variable: its type icon, its name and a preview of its value, like {@code fruits  3 entries · apple, banana, cherry}.
     */
    public static class VariableScrollEntry extends VariableRowEntry {

        private static final String NAME_VALUE_GAP = "   ";
        private static final String ENTRY_SEPARATOR = ", ";
        private static final String COUNT_SEPARATOR = " · ";

        @NotNull
        private final UserVariableSnapshot variable;
        @NotNull
        private final OnClick onClick;

        public VariableScrollEntry(@NotNull ScrollArea parent, @NotNull UserVariableSnapshot variable, @NotNull OnClick onClick) {
            super(parent, 14);
            this.variable = variable;
            this.onClick = onClick;
        }

        @NotNull
        public String getVariableName() {
            return this.variable.name();
        }

        @NotNull
        public UserVariableSnapshot getVariable() {
            return this.variable;
        }

        @Override
        protected float getLeadingWidth() {
            return getIconSize();
        }

        @Override
        protected void renderLeading(@NotNull GuiGraphics graphics, float x, float centerY) {
            renderIcon(graphics, getTypeIcon(this.variable.type()), x, centerY, getSubtleIconColor());
        }

        @Override
        protected @NotNull List<Segment> buildSegments() {
            List<Segment> segments = new ArrayList<>();
            segments.add(new Segment(this.variable.name(), TextStyle.LABEL));
            segments.add(new Segment(NAME_VALUE_GAP, TextStyle.LABEL));
            if (this.variable.isList()) {
                List<String> entries = this.variable.listEntries();
                if (entries.isEmpty()) {
                    segments.add(new Segment(I18n.get("fancymenu.overlay.menu_bar.variables.manage.empty_list"), TextStyle.MUTED));
                } else {
                    segments.add(new Segment(getEntryCountText(entries.size()) + COUNT_SEPARATOR, TextStyle.MUTED));
                    segments.add(new Segment(VariableTextUtils.toEntriesPreview(entries, ENTRY_SEPARATOR, LINE_BREAK_MARKER, MAX_SEGMENT_CHARS), TextStyle.VALUE));
                }
            } else if (this.variable.value().isEmpty()) {
                segments.add(new Segment(I18n.get("fancymenu.overlay.menu_bar.variables.manage.empty_value"), TextStyle.MUTED));
            } else {
                addSingleLineSegments(segments, this.variable.value(), TextStyle.VALUE);
            }
            return segments;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            this.onClick.onClick(this, button);
        }

        @FunctionalInterface
        public interface OnClick {

            void onClick(@NotNull VariableScrollEntry entry, int button);

        }

    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageVariablesWindowBody screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageVariablesWindowBody screen) {
        return openInWindow(screen, null);
    }

}
