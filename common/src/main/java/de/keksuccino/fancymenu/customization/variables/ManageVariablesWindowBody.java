package de.keksuccino.fancymenu.customization.variables;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ManageVariablesWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;
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
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.a"))
                .setIcon(MaterialIcons.ADD);

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
                    selectedVariable.setResetOnLaunch(!selectedVariable.isResetOnLaunch());
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean contextMenuActive = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        if (Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown() && this.isLetterKeyPressed(keyCode, scanCode, "s")) {
            if ((this.doneButton != null) && this.doneButton.visible && this.doneButton.active) {
                this.doneButton.onPress();
                return true;
            }
        }

        if ((keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
            if (contextMenuActive) {
                ContextMenuHandler.INSTANCE.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
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
                    VariableHandler.setVariable(call, "");
                    this.refreshVariablesList();
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
                variable.setValue(call);
                this.refreshVariablesList();
            }
        });
        Dialogs.openGeneric(s,
                Component.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"),
                null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
        s.setText(variable.getValue());
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
        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.delete_variable.confirm"), MessageDialogStyle.WARNING, call -> {
            if (call) {
                VariableHandler.removeVariable(variable.getName());
                this.refreshVariablesList();
            }
        });
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
