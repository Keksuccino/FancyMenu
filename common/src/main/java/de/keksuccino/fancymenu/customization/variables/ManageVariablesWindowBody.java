package de.keksuccino.fancymenu.customization.variables;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.VanillaEvents;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
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

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;
    private static final int HISTORY_LIMIT = 500;
    private static final int LIST_ENTRY_TOP_DOWN_BORDER = 1;
    private static final int LIST_ENTRY_OUTER_PADDING = 3;
    private static final int LIST_TOP_SPACER_HEIGHT = 5;

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
    protected VariableSnapshot variableClipboard;
    private final Deque<VariablesSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<VariablesSnapshot> redoHistory = new ArrayDeque<>();

    public ManageVariablesWindowBody(@NotNull Consumer<List<Variable>> callback) {
        super(Component.translatable("fancymenu.overlay.menu_bar.variables.manage"));
        this.callback = callback;
    }

    @Override
    protected void init() {
        boolean blur = UIBase.shouldBlur();
        this.variableListScrollArea.setSetupForBlurInterface(blur);
        this.updateRightClickContextMenu(false);

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, 50 + 15 + 1, 200, 20 - 2, Component.empty());
        this.searchBar.setHintFancyMenu(consumes -> Component.translatable("fancymenu.variables.manage_variables.screen.search_variable"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateVariablesList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar, blur);
        this.setupInitialFocusWidget(this, this.searchBar);

        // Set positions for scroll area
        this.variableListScrollArea.setWidth(this.width - 40, true);
        this.variableListScrollArea.setHeight(this.height - 85 - 25, true);
        this.variableListScrollArea.setX(20, true);
        this.variableListScrollArea.setY(50 + 15 + 25, true);
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

        this.rightClickContextMenu.addClickableEntry("add_variable", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.add_variable"), (menu, entry) -> {
                    menu.closeMenuChain();
                    this.requestAddVariable();
                }).setIcon(MaterialIcons.ADD);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_add")
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTargetVariable());

        this.rightClickContextMenu.addClickableEntry("set_value", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"), (menu, entry) -> {
                    Variable selectedVariable = this.getContextMenuTargetVariable();
                    if (selectedVariable == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.requestSetVariableValue(selectedVariable);
                }).addIsActiveSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.enter"))
                .setIcon(MaterialIcons.TEXT_FIELDS);

        this.rightClickContextMenu.addClickableEntry("toggle_reset_on_launch", this.getContextMenuToggleResetOnLaunchLabel(), (menu, entry) -> {
                    Variable selectedVariable = this.getContextMenuTargetVariable();
                    if (selectedVariable == null) {
                        return;
                    }
                    VariablesSnapshot beforeSnapshot = this.captureCurrentState();
                    selectedVariable.setResetOnLaunch(!selectedVariable.isResetOnLaunch());
                    this.createUndoPointIfChanged(beforeSnapshot);
                }).setLabelSupplier((menu, entry) -> this.getContextMenuToggleResetOnLaunchLabel())
                .addIsActiveSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTargetVariable())
                .setIcon(MaterialIcons.TOGGLE_ON);

        this.rightClickContextMenu.addClickableEntry("delete_variable", Component.translatable("fancymenu.overlay.menu_bar.variables.manage.delete_variable"), (menu, entry) -> {
                    Variable selectedVariable = this.getContextMenuTargetVariable();
                    if (selectedVariable == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.requestDeleteVariable(selectedVariable);
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
                    this.copyVariableToClipboard(selectedVariable);
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

        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();

        this.variableListScrollArea.setWidth(this.width - 40, true);
        this.variableListScrollArea.setHeight(this.height - 85 - 25, true);
        this.variableListScrollArea.setX(20, true);
        this.variableListScrollArea.setY(50 + 15 + 25, true);

        int buttonsRightEdge = this.width - 20;
        if (this.doneButton != null) {
            int buttonY = Math.max(20, (int)(this.variableListScrollArea.getYWithBorder() - this.doneButton.getHeight() - 5));
            this.doneButton.setX(buttonsRightEdge - this.doneButton.getWidth());
            this.doneButton.setY(buttonY);
        }
        if ((this.searchBar != null) && (this.doneButton != null)) {
            int searchBarX = 20 + 1;
            int searchBarY = 50 + 15 + 1;
            int searchBarWidth = Math.max(120, this.doneButton.getX() - 5 - searchBarX);
            this.searchBar.setX(searchBarX);
            this.searchBar.setY(searchBarY);
            this.searchBar.setWidth(searchBarWidth);
        }

        int textColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt()
                : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
        int searchBarTop = (this.searchBar != null) ? this.searchBar.getY() : (50 + 15 + 1);
        float labelY = searchBarTop - UIBase.getUITextHeightNormal() - UIBase.getAreaLabelVerticalPadding();
        UIBase.renderText(graphics, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.variables"), 20, labelY, textColor);

    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean contextMenuActive = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        String keyName = this.getLetterKeyName(keyCode, scanCode);

        if (!contextMenuActive && (keyCode == InputConstants.KEY_BACKSPACE)) {
            if (this.searchBar != null) {
                if (!this.searchBar.isFocused()) {
                    this.focusSearchBar();
                }
                this.searchBar.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }

        if (Minecraft.getInstance().hasControlDown() && !Minecraft.getInstance().hasShiftDown() && !Minecraft.getInstance().hasAltDown() && this.isLetterKeyPressed(keyCode, scanCode, "s")) {
            if ((this.doneButton != null) && this.doneButton.visible && this.doneButton.active) {
                this.doneButton.onPress(new KeyEvent(keyCode, scanCode, modifiers));
                return true;
            }
        }

        if (Minecraft.getInstance().hasControlDown() && "z".equals(keyName)) {
            if (this.undo()) {
                return true;
            }
        }

        if (Minecraft.getInstance().hasControlDown() && "y".equals(keyName)) {
            if (this.redo()) {
                return true;
            }
        }

        boolean searchBarFocused = (this.searchBar != null) && this.searchBar.isFocused();
        if (!contextMenuActive && !searchBarFocused && Minecraft.getInstance().hasControlDown() && "c".equals(keyName)) {
            if (this.copySelectedVariableToClipboard()) {
                return true;
            }
        }

        if (!contextMenuActive && !searchBarFocused && Minecraft.getInstance().hasControlDown() && "v".equals(keyName)) {
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

        if (!contextMenuActive && ((keyCode == InputConstants.KEY_LEFT) || (keyCode == InputConstants.KEY_RIGHT))) {
            return true;
        }

        if (!contextMenuActive && ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER))) {
            VariableScrollEntry selectedEntry = this.getSelectedEntry();
            if (selectedEntry != null) {
                this.requestSetVariableValue(selectedEntry.variable);
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
    public boolean charTyped(CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), event.modifiers());
    }
    
    public boolean charTyped(char codePoint, int modifiers) {
        boolean contextMenuActive = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        if (!contextMenuActive && this.shouldRouteTypedCharacterToSearchBar(codePoint) && (this.searchBar != null) && !this.searchBar.isFocused()) {
            this.focusSearchBar();
            if (this.searchBar.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(new CharacterEvent(codePoint, modifiers));
    }

    @NotNull
    protected String getLetterKeyName(int keyCode, int scanCode) {
        String keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        if (keyName == null) {
            return "";
        }
        return keyName.toLowerCase(Locale.ROOT);
    }

    protected boolean isLetterKeyPressed(int keyCode, int scanCode, @NotNull String letter) {
        return letter.toLowerCase(Locale.ROOT).equals(this.getLetterKeyName(keyCode, scanCode));
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

    private boolean shouldRouteTypedCharacterToSearchBar(char codePoint) {
        if (net.minecraft.client.Minecraft.getInstance().hasControlDown() || net.minecraft.client.Minecraft.getInstance().hasAltDown()) {
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
        Component hint = Component.translatable("fancymenu.overlay.menu_bar.variables.manage.empty_hint");
        float hintWidth = UIBase.getUITextWidthNormal(hint);
        float hintX = this.variableListScrollArea.getInnerX() + (this.variableListScrollArea.getInnerWidth() / 2F) - (hintWidth / 2F);
        float hintY = this.variableListScrollArea.getInnerY() + (this.variableListScrollArea.getInnerHeight() / 2F) - (UIBase.getUITextHeightNormal() / 2F);
        UIBase.renderText(graphics, hint, hintX, hintY, UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }
    
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

        return super.mouseClicked(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button), false);
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
            this.contextMenuTargetVariableName = variableEntry.variable.getName();
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

    protected void requestAddVariable() {
        TextInputWindowBody s = new TextInputWindowBody(CharacterFilter.buildOnlyLowercaseFileNameFilter(), (call) -> {
            if (call != null) {
                if (!VariableHandler.variableExists(call)) {
                    VariablesSnapshot beforeSnapshot = this.captureCurrentState();
                    VariableHandler.setVariable(call, "");
                    this.contextMenuTargetVariableName = call;
                    this.refreshVariablesList();
                    this.restoreSelection(call);
                    this.createUndoPointIfChanged(beforeSnapshot);
                }
            }
        });
        Dialogs.openGeneric(s,
                Component.translatable("fancymenu.overlay.menu_bar.variables.manage.add_variable.input_name"),
                null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
    }

    protected void requestSetVariableValue(@NotNull Variable variable) {
        TextInputWindowBody s = new TextInputWindowBody(null, (call) -> {
            if (call != null) {
                VariablesSnapshot beforeSnapshot = this.captureCurrentState();
                variable.setValue(call);
                this.refreshVariablesList();
                this.restoreSelection(variable.getName());
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        });
        Dialogs.openGeneric(s,
                Component.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"),
                null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
        s.setText(variable.getValue());
    }

    protected boolean copySelectedVariableToClipboard() {
        VariableScrollEntry selectedEntry = this.getSelectedEntry();
        if (selectedEntry == null) {
            return false;
        }
        this.copyVariableToClipboard(selectedEntry.variable);
        return true;
    }

    protected void copyVariableToClipboard(@NotNull Variable variable) {
        this.variableClipboard = new VariableSnapshot(variable.getName(), variable.getValue(), variable.isResetOnLaunch());
    }

    protected boolean canPasteVariableFromClipboard() {
        return this.variableClipboard != null;
    }

    protected boolean pasteVariableFromClipboard() {
        if (this.variableClipboard == null) {
            return false;
        }
        VariablesSnapshot beforeSnapshot = this.captureCurrentState();
        String pastedVariableName = this.generatePastedVariableName(this.variableClipboard.name());
        VariableHandler.setVariable(pastedVariableName, this.variableClipboard.value());
        Variable pastedVariable = VariableHandler.getVariable(pastedVariableName);
        if (pastedVariable != null) {
            pastedVariable.setResetOnLaunch(this.variableClipboard.resetOnLaunch());
        }
        this.contextMenuTargetVariableName = pastedVariableName;
        this.refreshVariablesList();
        this.restoreSelection(pastedVariableName);
        this.createUndoPointIfChanged(beforeSnapshot);
        return true;
    }

    @NotNull
    protected String generatePastedVariableName(@NotNull String sourceVariableName) {
        String baseName = sourceVariableName + "_Copy";
        String candidate = baseName;
        int suffix = 2;
        while (VariableHandler.variableExists(candidate)) {
            candidate = baseName + suffix;
            suffix++;
        }
        return candidate;
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

    protected boolean variableFitsSearchValue(@NotNull Variable variable, @Nullable String s) {
        if ((s == null) || s.isBlank()) return true;
        s = s.toLowerCase();
        if (variable.getName().toLowerCase().contains(s)) return true;
        return variable.getValue().toLowerCase().contains(s);
    }

    protected void updateVariablesList() {

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;

        this.variableListScrollArea.clearEntries();
        CellScreen.SpacerScrollAreaEntry spacer = new CellScreen.SpacerScrollAreaEntry(this.variableListScrollArea, LIST_TOP_SPACER_HEIGHT);
        spacer.setSelectable(false);
        spacer.selectOnClick = false;
        spacer.setPlayClickSound(false);
        spacer.setBackgroundColorNormal(() -> DrawableColor.FULLY_TRANSPARENT);
        spacer.setBackgroundColorHover(() -> DrawableColor.FULLY_TRANSPARENT);
        this.variableListScrollArea.addEntry(spacer);

        List<Variable> variables = VariableHandler.getVariables();
        variables.sort(Comparator
                .comparing(Variable::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Variable::getName));

        for (Variable v : variables) {
            if (!this.variableFitsSearchValue(v, searchValue)) continue;
            VariableScrollEntry e = new VariableScrollEntry(this.variableListScrollArea, v, (entry) -> {
            });
            e.setHeight(this.getListEntryHeight());
            this.variableListScrollArea.addEntry(e);
        }
    }

    protected void refreshVariablesList() {
        this.updateVariablesList();
        if ((this.contextMenuTargetVariableName != null) && (this.getContextMenuTargetVariable() == null)) {
            this.clearContextMenuTarget();
        }
    }

    protected void requestDeleteSelectedVariable() {
        VariableScrollEntry e = this.getSelectedEntry();
        if (e == null) return;
        this.requestDeleteVariable(e.variable);
    }

    protected void requestDeleteVariable(@NotNull Variable variable) {
        VariablesSnapshot beforeSnapshot = this.captureCurrentState();
        VariableHandler.removeVariable(variable.getName());
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
        String selectedVariableName = (selectedEntry != null) ? selectedEntry.variable.getName() : null;
        return new VariablesSnapshot(
                this.captureVariableSnapshots(),
                this.variableListScrollArea.verticalScrollBar.getScroll(),
                this.variableListScrollArea.horizontalScrollBar.getScroll(),
                selectedVariableName,
                this.contextMenuTargetVariableName
        );
    }

    private @NotNull List<VariableSnapshot> captureVariableSnapshots() {
        List<Variable> variables = VariableHandler.getVariables();
        variables.sort(Comparator
                .comparing(Variable::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Variable::getName));
        List<VariableSnapshot> snapshots = new ArrayList<>();
        for (Variable variable : variables) {
            snapshots.add(new VariableSnapshot(variable.getName(), variable.getValue(), variable.isResetOnLaunch()));
        }
        return snapshots;
    }

    private void applySnapshot(@NotNull VariablesSnapshot snapshot) {
        this.applyVariableSnapshots(snapshot.variables());
        this.refreshVariablesList();
        this.variableListScrollArea.verticalScrollBar.setScroll(Mth.clamp(snapshot.verticalScroll(), 0.0F, 1.0F));
        this.variableListScrollArea.horizontalScrollBar.setScroll(Mth.clamp(snapshot.horizontalScroll(), 0.0F, 1.0F));
        this.variableListScrollArea.updateEntries(null);
        this.restoreSelection(snapshot.selectedVariableName());
        this.contextMenuTargetVariableName = snapshot.contextMenuTargetVariableName();
        if (this.getContextMenuTargetVariable() == null) {
            this.clearContextMenuTarget();
        }
    }

    private void applyVariableSnapshots(@NotNull List<VariableSnapshot> snapshots) {
        VariableHandler.clearVariables();
        for (VariableSnapshot snapshot : snapshots) {
            VariableHandler.setVariable(snapshot.name(), snapshot.value());
            Variable variable = VariableHandler.getVariable(snapshot.name());
            if (variable != null) {
                variable.setResetOnLaunch(snapshot.resetOnLaunch());
            }
        }
    }

    private void restoreSelection(@Nullable String selectedVariableName) {
        if (selectedVariableName == null) {
            return;
        }
        for (ScrollAreaEntry entry : this.variableListScrollArea.getEntries()) {
            if ((entry instanceof VariableScrollEntry variableEntry) && selectedVariableName.equals(variableEntry.variable.getName())) {
                entry.setSelected(true);
                return;
            }
        }
    }

    private record VariablesSnapshot(@NotNull List<VariableSnapshot> variables, float verticalScroll, float horizontalScroll, @Nullable String selectedVariableName, @Nullable String contextMenuTargetVariableName) {
    }

    private record VariableSnapshot(@NotNull String name, @NotNull String value, boolean resetOnLaunch) {
    }

    public static class VariableScrollEntry extends TextScrollAreaEntry {

        public Variable variable;

        public VariableScrollEntry(ScrollArea parent, @NotNull Variable variable, @NotNull Consumer<TextScrollAreaEntry> onClick) {
            super(parent, Component.literal(variable.name).setStyle(Style.EMPTY.withColor(resolveLabelColor())).append(Component.literal(" (" + variable.getValue() + ")").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()))), onClick);
            this.variable = variable;
        }

        private static int resolveLabelColor() {
            return UIBase.shouldBlur()
                    ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                    : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
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
