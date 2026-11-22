package de.keksuccino.fancymenu.customization.listener.gui;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptDescriptions;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.aiexport.AiExportMenus;
import de.keksuccino.fancymenu.customization.aiexport.exporters.ListenersAiExporter;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ManageListenersWindowBody extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    @NotNull
    protected final Consumer<Boolean> callback;
    protected boolean resultHandled = false;
    protected boolean skipCloseWarning = false;
    @NotNull
    protected final List<ListenerInstance> tempInstances = new ArrayList<>();
    @Nullable
    protected ListenerInstance selectedInstance;
    /**
     * The listeners as they were when the window opened. Closing the window without saving only asks for confirmation when they differ from it.
     */
    @NotNull
    private final Map<String, ListenerState> initialListenersState;

    public ManageListenersWindowBody(@NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.listeners.manage"));
        this.callback = callback;
        this.setAllowCloseOnEsc(false);
        // Work with copies of all listener instances, so only Done changes the active listeners and Cancel throws away every change
        ListenerHandler.getInstances().forEach(instance -> this.tempInstances.add(instance.copy()));
        this.initialListenersState = serializeListenersState(this.tempInstances);
        this.setSearchBarEnabled(true);
        this.setDescriptionAreaEnabled(true);
    }

    @Override
    protected void initCells() {
        
        // Track which instance was being edited
        String editingInstanceId = null;
        String editingValue = null;
        for (RenderCell cell : this.allCells) {
            if (cell instanceof ListenerInstanceCell instanceCell) {
                if (instanceCell.editMode && instanceCell.editBox != null) {
                    editingInstanceId = instanceCell.instance.instanceIdentifier;
                    editingValue = instanceCell.editBox.getValue();
                    break;
                }
            }
        }

        this.addSpacerCell(5).setIgnoreSearch();

        // Add all listener instances to the list
        List<ListenerInstanceCell> instanceCells = new ArrayList<>();
        for (ListenerInstance instance : this.tempInstances) {
            ListenerInstanceCell cell = new ListenerInstanceCell(instance);
            instanceCells.add(cell);
        }

        instanceCells.sort(Comparator
                .comparing((ListenerInstanceCell cell) -> cell.labelComponent.getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(cell -> cell.labelComponent.getString())
                .thenComparing(cell -> cell.instance.instanceIdentifier));

        String finalEditingInstanceId = editingInstanceId;
        String finalEditingValue = editingValue;
        instanceCells.forEach(cell -> {
            this.addCell(cell).setSelectable(true);
            // Restore edit mode if this was the cell being edited
            if (finalEditingInstanceId != null && finalEditingInstanceId.equals(cell.instance.instanceIdentifier)) {
                cell.enterEditMode();
                if (cell.editBox != null && finalEditingValue != null) {
                    cell.editBox.setValue(finalEditingValue);
                    cell.editBox.setCursorPosition(finalEditingValue.length());
                }
            }
        });

        this.addSpacerCell(5).setIgnoreSearch();
        
    }

    @Override
    protected void initRightSideWidgets() {
        
        // Add listener button
        this.addRightSideButton(20, Component.translatable("fancymenu.listeners.manage.add"), button -> {
            PiPWindow parentWindow = this.getWindow();
            if (parentWindow != null) {
                parentWindow.setVisible(false);
            }
            boolean[] restoreParentOnClose = new boolean[] { true };
            ChooseListenerTypeScreen chooseScreen = new ChooseListenerTypeScreen(listener -> {
                if (listener != null) {
                    restoreParentOnClose[0] = false;
                    // Create new instance and open action editor
                    ListenerInstance newInstance = listener.createFreshInstance();
                    ActionScriptEditorWindowBody actionsScreen = new ActionScriptEditorWindowBody(newInstance.getActionScript(), updatedScript -> {
                        if (updatedScript != null) {
                            newInstance.setActionScript(updatedScript);
                            this.tempInstances.add(newInstance);
                            this.rebuild();
                        }
                    });
                    PiPWindow actionWindow = ActionScriptEditorWindowBody.openInWindow(actionsScreen, parentWindow);
                    if (parentWindow != null) {
                        actionWindow.setPosition(parentWindow.getX(), parentWindow.getY());
                        actionWindow.addCloseCallback(() -> parentWindow.setVisible(true));
                    }
                }
            });
            PiPWindow chooseWindow = ChooseListenerTypeScreen.openInWindow(chooseScreen, parentWindow);
            if (parentWindow != null) {
                chooseWindow.setPosition(parentWindow.getX(), parentWindow.getY());
                chooseWindow.addCloseCallback(() -> {
                    if (restoreParentOnClose[0]) {
                        parentWindow.setVisible(true);
                    }
                });
            }
        });
        
        // Edit listener button
        this.addRightSideButton(20, Component.translatable("fancymenu.listeners.manage.edit"), button -> {
            this.onEditActionsOfSelected();
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);

        // Remove listener button
        this.addRightSideButton(20, Component.translatable("fancymenu.listeners.manage.remove"), button -> {
            ListenerInstance sel = this.selectedInstance;
            if (sel != null) {
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.listeners.manage.delete_warning"), MessageDialogStyle.WARNING, call -> {
                    if (call) {
                        this.tempInstances.remove(sel);
                        this.selectedInstance = null;
                        this.rebuild();
                    }
                });
            }
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);

        this.addRightSideDefaultSpacer();

        // Exports the listeners of this window, so unsaved changes are part of the export
        this.addRightSideButton(20, Component.translatable("fancymenu.ai_export"), button -> {
            ContextMenu exportMenu = new ContextMenu();
            ListenerInstance selected = this.selectedInstance;
            AiExportMenus.addExportSubMenu(exportMenu, "selected_listener", Component.translatable("fancymenu.ai_export.listeners.selected"), () -> ListenersAiExporter.exportSingle(Objects.requireNonNull(selected)))
                    .addIsActiveSupplier((menu, entry) -> selected != null);
            AiExportMenus.addExportSubMenu(exportMenu, "all_listeners", Component.translatable("fancymenu.ai_export.listeners.all"), () -> ListenersAiExporter.exportAll(List.copyOf(this.tempInstances)));
            AiExportMenus.openMenuAtMouse(exportMenu);
        }).setUITooltipSupplier(consumes -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.ai_export.desc")));

    }

    protected void onEditActionsOfSelected() {
        if (this.selectedInstance != null) {
            ListenerInstance cached = this.selectedInstance;
            ActionScriptEditorWindowBody actionsScreen = new ActionScriptEditorWindowBody(cached.getActionScript(), updatedScript -> {
                if (updatedScript != null) {
                    cached.setActionScript(updatedScript);
                }
            });
            PiPWindow parentWindow = this.getWindow();
            if (parentWindow != null) {
                parentWindow.setVisible(false);
            }
            PiPWindow actionWindow = ActionScriptEditorWindowBody.openInWindow(actionsScreen, parentWindow);
            if (parentWindow != null) {
                actionWindow.setPosition(parentWindow.getX(), parentWindow.getY());
                actionWindow.addCloseCallback(() -> parentWindow.setVisible(true));
            }
        }
    }

    @Override
    protected @Nullable List<Component> getCurrentDescription() {

        this.updateSelectedInstance();

        List<Component> desc = super.getCurrentDescription();
        if (desc == null) return null;
        if (this.selectedInstance == null) return null;

        List<Component> newDesc = new ArrayList<>();

        newDesc.add(Component.translatable("fancymenu.listeners.manage.description.listener_desc").withStyle(ChatFormatting.BOLD));
        newDesc.add(Component.literal("(").append(this.selectedInstance.parent.getDisplayName()).append(")"));
        newDesc.add(Component.empty());
        newDesc.addAll(desc);

        newDesc.add(Component.empty());
        newDesc.add(Component.empty());

        newDesc.add(Component.translatable("fancymenu.listeners.manage.description.actions").withStyle(ChatFormatting.BOLD));
        newDesc.add(Component.empty());

        newDesc.addAll(ActionScriptDescriptions.buildScriptOverview(this.selectedInstance.getActionScript(), UIBase.getUITheme(), UIBase.shouldBlur()));

        return newDesc;

    }

    @Override
    public boolean allowEnterForDone() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ListenerInstanceCell editingCell = this.getEditingInstanceCell();
        if (editingCell != null && editingCell.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.onCancel();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        ListenerInstanceCell editingCell = this.getEditingInstanceCell();
        if (editingCell != null && editingCell.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ListenerInstanceCell editingCell = this.getEditingInstanceCell();
        if ((button == 0) && (editingCell != null) && !editingCell.isMouseOverEditBox(mouseX, mouseY)) {
            editingCell.exitEditMode(true);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nullable
    protected ListenerInstanceCell getEditingInstanceCell() {
        for (RenderCell cell : this.allCells) {
            if (cell instanceof ListenerInstanceCell instanceCell && instanceCell.editMode && instanceCell.editBox != null) {
                return instanceCell;
            }
        }
        return null;
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.updateSelectedInstance();
    }

    @Override
    protected @NotNull List<Component> getDescriptionAreaTips() {
        return List.of(Component.translatable("fancymenu.listeners.manage.rename_tip"), Component.translatable("fancymenu.listeners.manage.quick_edit_tip"));
    }

    @Override
    public void setWindow(@Nullable PiPWindow window) {
        if (window != null) {
            window.setCloseWindowCheck((window1, decision) -> {
                if (this.skipCloseWarning || !this.hasUnsavedChanges()) {
                    this.skipCloseWarning = false;
                    decision.supply(true);
                    return;
                }
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.listeners.manage.cancel_warning"), MessageDialogStyle.WARNING, decision::supply);
            });
            window.addCloseCallback(() -> {
                if (this.resultHandled) {
                    return;
                }
                this.resultHandled = true;
                this.callback.accept(false);
            });
        }
        super.setWindow(window);
    }

    @Override
    protected void onCancel() {
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        ListenerHandler.replaceInstances(this.tempInstances);
        this.closeWithResult(true);
    }

    @Override
    public void onWindowClosedExternally() {
    }

    protected void closeWithResult(boolean accepted) {
        if (this.resultHandled) {
            return;
        }
        this.resultHandled = true;
        this.skipCloseWarning = accepted;
        this.callback.accept(accepted);
        this.closeWindow();
    }

    /**
     * Returns {@code true} when the listeners differ from how they were when the window opened,
     * or when the rename box of a listener holds a name that is not applied yet.
     */
    protected boolean hasUnsavedChanges() {
        ListenerInstanceCell editingCell = this.getEditingInstanceCell();
        if ((editingCell != null) && (editingCell.editBox != null) && !Objects.equals(editingCell.editBox.getValue(), Objects.requireNonNullElse(editingCell.instance.getDisplayName(), ""))) {
            return true;
        }
        return hasListenersChanged(this.initialListenersState, this.tempInstances);
    }

    @NotNull
    static Map<String, ListenerState> serializeListenersState(@NotNull List<ListenerInstance> instances) {
        Map<String, ListenerState> state = new HashMap<>();
        for (ListenerInstance instance : instances) {
            state.put(instance.instanceIdentifier, new ListenerState(instance.getDisplayName(), ActionScriptEditorWindowBody.serializeScriptState(instance.getActionScript())));
        }
        return state;
    }

    static boolean hasListenersChanged(@NotNull Map<String, ListenerState> initialListenersState, @NotNull List<ListenerInstance> instances) {
        return !initialListenersState.equals(serializeListenersState(instances));
    }

    /**
     * The parts of a listener instance that get compared to tell whether it got edited.
     * Collapse states of blocks in the action script are left out, see {@link ActionScriptEditorWindowBody#serializeScriptState(AbstractExecutableBlock)}.
     */
    record ListenerState(@Nullable String displayName, @NotNull Map<String, String> actionScript) {
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageListenersWindowBody screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageListenersWindowBody screen) {
        return openInWindow(screen, null);
    }

    protected void updateSelectedInstance() {
        this.updateSelectedCell();
        RenderCell selected = this.getSelectedCell();
        if (selected instanceof ListenerInstanceCell cell) {
            this.selectedInstance = cell.instance;
        } else {
            this.selectedInstance = null;
        }
    }

    protected int getLabelTextColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
    }

    public class ListenerInstanceCell extends RenderCell {
        
        @NotNull
        protected final ListenerInstance instance;
        @NotNull
        protected Component labelComponent;
        @Nullable
        protected ExtendedEditBox editBox;
        protected boolean editMode = false;
        protected long lastClickTime = 0;
        protected static final long DOUBLE_CLICK_TIME = 500; // milliseconds
        protected static final int TOP_DOWN_CELL_BORDER = 1;
        
        public ListenerInstanceCell(@NotNull ListenerInstance instance) {
            this.instance = instance;
            this.updateLabelComponent();
            this.setDescriptionSupplier(this.instance.parent::getDescription);
            this.setSearchStringSupplier(() -> {
                if (this.instance.getDisplayName() != null) {
                    return this.instance.getDisplayName();
                }
                return this.instance.parent.getDisplayName().getString();
            });
        }
        
        protected void updateLabelComponent() {
            if (this.instance.getDisplayName() != null && !this.instance.getDisplayName().isBlank()) {
                // Show display name if it exists
                this.labelComponent = Component.literal(this.instance.getDisplayName())
                        .setStyle(Style.EMPTY.withColor(ManageListenersWindowBody.this.getLabelTextColor()));
            } else {
                // Show default label (listener type)
                this.labelComponent = this.instance.parent.getDisplayName().copy()
                        .setStyle(Style.EMPTY.withColor(ManageListenersWindowBody.this.getLabelTextColor()));
            }
        }
        
        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (this.editMode && this.editBox != null) {
                // Render edit box
                this.editBox.setX(this.getX());
                this.editBox.setY(this.getY() + TOP_DOWN_CELL_BORDER);
                this.editBox.setWidth(Math.min(this.getWidth(), 200));
                this.editBox.setHeight((int)(UIBase.getUITextHeightNormal() + 1));
                this.editBox.render(graphics, mouseX, mouseY, partial);
            } else {
                // Render label
                RenderingUtils.resetShaderColor(graphics);
                UIBase.renderText(graphics, this.labelComponent, this.getX(), this.getY() + TOP_DOWN_CELL_BORDER);
                RenderingUtils.resetShaderColor(graphics);
                // Show Writing cursor when the label is hovered
                if (UIBase.isXYInArea(mouseX, mouseY, this.getX(), this.getY() + TOP_DOWN_CELL_BORDER, UIBase.getUITextWidthNormal(this.labelComponent), UIBase.getUITextHeightNormal())) {
                    CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
                }
            }
        }
        
        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            if (this.editMode && this.editBox != null) {
                this.setWidth(Math.min((int)(ManageListenersWindowBody.this.scrollArea.getInnerWidth() - 40), 200));
            } else {
                this.setWidth((int)UIBase.getUITextWidthNormal(this.labelComponent));
            }
            this.setHeight((int)(UIBase.getUITextHeightNormal() + (TOP_DOWN_CELL_BORDER * 2)));
        }

        @Override
        protected void updatePosition(@NotNull CellScrollEntry scrollEntry) {
            this.setX((int)(scrollEntry.getX() + 5));
            this.setY((int)scrollEntry.getY());
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && this.isHovered() && !this.editMode) { // Left click
                long currentTime = System.currentTimeMillis();
                if (currentTime - this.lastClickTime < DOUBLE_CLICK_TIME) {
                    // Double click detected - enter edit mode
                    this.enterEditMode();
                    this.lastClickTime = 0; // Reset to prevent triple clicks
                } else {
                    this.lastClickTime = currentTime;
                }
            }
            boolean b = super.mouseClicked(mouseX, mouseY, button);
            if ((button == 1) && this.isHovered() && !this.editMode) {
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        this.setSelected(true);
                        ManageListenersWindowBody.this.updateSelectedInstance();
                        ManageListenersWindowBody.this.onEditActionsOfSelected();
                    }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
                }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            }
            return b;
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.editMode && this.editBox != null) {
                if (keyCode == InputConstants.KEY_ENTER || keyCode == InputConstants.KEY_NUMPADENTER) { // Enter or Numpad Enter
                    this.exitEditMode(true);
                    return true;
                } else if (keyCode == InputConstants.KEY_ESCAPE) { // Escape
                    this.exitEditMode(false);
                    return true;
                }
                return this.editBox.keyPressed(keyCode, scanCode, modifiers);
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            if (this.editMode && this.editBox != null) {
                return this.editBox.charTyped(codePoint, modifiers);
            }
            return super.charTyped(codePoint, modifiers);
        }

        protected boolean isMouseOverEditBox(double mouseX, double mouseY) {
            return this.editBox != null && this.editBox.isMouseOver(mouseX, mouseY);
        }
        
        protected void enterEditMode() {
            if (this.editMode) return; // Already in edit mode
            
            this.editMode = true;
            
            // Create edit box
            this.editBox = new ExtendedEditBox(
                    Minecraft.getInstance().font, 
                    this.getX(), 
                    this.getY(), 
                    Math.min(200, (int)(ManageListenersWindowBody.this.scrollArea.getInnerWidth() - 40)),
                    18, 
                    Component.empty()
            );
            UIBase.applyDefaultWidgetSkinTo(this.editBox, UIBase.shouldBlur());
            this.editBox.setMaxLength(100000);
            
            // Set current display name or empty if null
            String currentName = this.instance.getDisplayName();
            if (currentName != null) {
                this.editBox.setValue(currentName);
            } else {
                this.editBox.setValue("");
            }
            
            this.editBox.setCursorPosition(this.editBox.getValue().length());
            this.editBox.setHighlightPos(0);
            
            // Add to children for input handling
            this.children.clear();
            this.children.add(this.editBox);
            ManageListenersWindowBody.this.focusTarget(this, this.editBox);
        }
        
        protected void exitEditMode(boolean save) {
            if (!this.editMode || this.editBox == null) return;
            
            if (save) {
                String newName = this.editBox.getValue();
                if (newName.isBlank()) {
                    this.instance.setDisplayName(null);
                } else {
                    this.instance.setDisplayName(newName);
                }
                this.updateLabelComponent();
                // Update the description area if this is the selected cell
                if (ManageListenersWindowBody.this.selectedInstance == this.instance) {
                    ManageListenersWindowBody.this.updateDescriptionArea();
                }
            }
            
            this.setFocused(null);
            if (ManageListenersWindowBody.this.getFocused() == this) {
                ManageListenersWindowBody.this.setFocused(null);
            }
            this.editMode = false;
            this.editBox = null;
            this.children.clear();
        }
        
    }

}
