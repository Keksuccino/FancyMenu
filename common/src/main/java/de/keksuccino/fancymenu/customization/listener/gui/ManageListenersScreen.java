package de.keksuccino.fancymenu.customization.listener.gui;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorScreen;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class ManageListenersScreen extends CellScreen {

    @NotNull
    protected final Consumer<Boolean> callback;
    @NotNull
    protected final List<ListenerInstance> tempInstances = new ArrayList<>();
    @Nullable
    protected ListenerInstance selectedInstance;

    public ManageListenersScreen(@NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.listeners.manage"));
        this.callback = callback;
        // Create a copy of all listener instances to work with
        this.tempInstances.addAll(ListenerHandler.getInstances());
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
            ChooseListenerTypeScreen chooseScreen = new ChooseListenerTypeScreen(listener -> {
                if (listener != null) {
                    // Create new instance and open action editor
                    ListenerInstance newInstance = listener.createFreshInstance();
                    ActionScriptEditorScreen actionsScreen = new ActionScriptEditorScreen(newInstance.getActionScript(), updatedScript -> {
                        if (updatedScript != null) {
                            newInstance.setActionScript(updatedScript);
                            this.tempInstances.add(newInstance);
                            this.rebuild();
                        }
                        Minecraft.getInstance().setScreen(this);
                    });
                    Minecraft.getInstance().setScreen(actionsScreen);
                } else {
                    Minecraft.getInstance().setScreen(this);
                }
            });
            Minecraft.getInstance().setScreen(chooseScreen);
        });
        
        // Edit listener button
        this.addRightSideButton(20, Component.translatable("fancymenu.listeners.manage.edit"), button -> {
            this.onEditActionsOfSelected();
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);

        // Remove listener button
        this.addRightSideButton(20, Component.translatable("fancymenu.listeners.manage.remove"), button -> {
            ListenerInstance sel = this.selectedInstance;
            if (sel != null) {
                Minecraft.getInstance().setScreen(ConfirmationScreen.critical(call -> {
                    if (call) {
                        this.tempInstances.remove(sel);
                        this.selectedInstance = null;
                        this.rebuild();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, Component.translatable("fancymenu.listeners.manage.delete_warning")));
            }
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);
        
    }

    protected void onEditActionsOfSelected() {
        if (this.selectedInstance != null) {
            ListenerInstance cached = this.selectedInstance;
            ActionScriptEditorScreen actionsScreen = new ActionScriptEditorScreen(cached.getActionScript(), updatedScript -> {
                if (updatedScript != null) {
                    cached.setActionScript(updatedScript);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(actionsScreen);
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

        List<Component> actionLines = this.buildActionScriptDescription(this.selectedInstance.getActionScript(), 0);
        newDesc.addAll(actionLines);

        return newDesc;

    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.updateSelectedInstance();

        super.render(graphics, mouseX, mouseY, partial);

        if (this.descriptionScrollArea != null) {
            int descW = (int) this.descriptionScrollArea.getWidthWithBorder();
            int descEndX = (int) (this.descriptionScrollArea.getXWithBorder() + this.descriptionScrollArea.getWidthWithBorder());
            int descEndY = (int) (this.descriptionScrollArea.getYWithBorder() + this.descriptionScrollArea.getHeightWithBorder());
            List<FormattedCharSequence> renameTip = this.font.split(Component.translatable("fancymenu.listeners.manage.rename_tip").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_inactive.getColorInt()).withItalic(true)), descW);
            List<FormattedCharSequence> quickEditTip = this.font.split(Component.translatable("fancymenu.listeners.manage.quick_edit_tip").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_inactive.getColorInt()).withItalic(true)), descW);
            int lineY = descEndY + 4;
            for (FormattedCharSequence line : renameTip) {
                int lineWidth = this.font.width(line);
                int lineX = descEndX - lineWidth;
                graphics.drawString(this.font, line, lineX, lineY, -1, false);
                lineY += this.font.lineHeight + 2;
            }
            lineY += 2;
            for (FormattedCharSequence line : quickEditTip) {
                int lineWidth = this.font.width(line);
                int lineX = descEndX - lineWidth;
                graphics.drawString(this.font, line, lineX, lineY, -1, false);
                lineY += this.font.lineHeight + 2;
            }
        }

    }

    @Override
    protected void onCancel() {
        Minecraft.getInstance().setScreen(
                ConfirmationScreen.critical(callback -> {
                    if (callback) {
                        this.callback.accept(false);
                    } else {
                        Minecraft.getInstance().setScreen(this);
                    }
                }, Component.translatable("fancymenu.listeners.manage.cancel_warning"))
        );
    }

    @Override
    protected void onDone() {
        // Clear existing instances
        for (ListenerInstance instance : new ArrayList<>(ListenerHandler.getInstances())) {
            ListenerHandler.removeInstance(instance.instanceIdentifier);
        }
        
        // Add all temp instances to handler
        for (ListenerInstance instance : this.tempInstances) {
            ListenerHandler.addInstance(instance);
        }
        
        this.callback.accept(true);
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

    @NotNull
    protected List<Component> buildActionScriptDescription(@NotNull GenericExecutableBlock block, int indentLevel) {
        List<Component> lines = new ArrayList<>();
        
        for (Executable executable : block.getExecutables()) {
            lines.addAll(this.buildExecutableDescription(executable, indentLevel));
        }
        
        if (lines.isEmpty()) {
            String indent = "  ".repeat(Math.max(0, indentLevel));
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().listing_dot_color_1.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.screens.manage_screen.info.value.none")
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()))));
        }
        
        return lines;
    }
    
    @NotNull
    protected List<Component> buildExecutableDescription(@NotNull Executable executable, int indentLevel) {
        List<Component> lines = new ArrayList<>();
        String indent = "  ".repeat(Math.max(0, indentLevel));
        
        if (executable instanceof ActionInstance actionInstance) {
            // Action display name
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().listing_dot_color_2.getColorInt()))
                    .append(actionInstance.action.getActionDisplayName().copy()
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Action value (indented more)
            String cachedValue = actionInstance.value;
            String valueString = ((cachedValue != null) && actionInstance.action.hasValue()) 
                    ? cachedValue 
                    : I18n.get("fancymenu.actions.screens.manage_screen.info.value.none");
            lines.add(Component.literal(indent + "    ◦ ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().listing_dot_color_1.getColorInt()))
                    .append(Component.literal(I18n.get("fancymenu.actions.screens.manage_screen.info.value") + " ")
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())))
                    .append(Component.literal(valueString)
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
        } else if (executable instanceof IfExecutableBlock ifBlock) {
            String requirements = this.buildRequirementsString(ifBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.if", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Add nested executables
            for (Executable nested : ifBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
            
            // Handle appended blocks (else if, else)
            AbstractExecutableBlock appended = ifBlock.getAppendedBlock();
            while (appended != null) {
                lines.addAll(this.buildAppendedBlockDescription(appended, indentLevel));
                appended = appended.getAppendedBlock();
            }
            
        } else if (executable instanceof WhileExecutableBlock whileBlock) {
            String requirements = this.buildRequirementsString(whileBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.while", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Add nested executables
            for (Executable nested : whileBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
            
        } else if (executable instanceof AbstractExecutableBlock) {
            // For any other abstract executable blocks
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.literal("[UNKNOWN BLOCK]")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))));
        }
        
        return lines;
    }
    
    @NotNull
    protected List<Component> buildAppendedBlockDescription(@NotNull AbstractExecutableBlock block, int indentLevel) {
        List<Component> lines = new ArrayList<>();
        String indent = "  ".repeat(Math.max(0, indentLevel));
        
        if (block instanceof ElseIfExecutableBlock elseIfBlock) {
            String requirements = this.buildRequirementsString(elseIfBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.else_if", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Add nested executables
            for (Executable nested : elseIfBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
            
        } else if (block instanceof ElseExecutableBlock elseBlock) {
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.else")
                            .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))));
            
            // Add nested executables
            for (Executable nested : elseBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
        }
        
        return lines;
    }
    
    @NotNull
    protected String buildRequirementsString(@NotNull IfExecutableBlock block) {
        String requirements = "";
        for (LoadingRequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (LoadingRequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
    }
    
    @NotNull
    protected String buildRequirementsString(@NotNull ElseIfExecutableBlock block) {
        String requirements = "";
        for (LoadingRequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (LoadingRequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
    }
    
    @NotNull
    protected String buildRequirementsString(@NotNull WhileExecutableBlock block) {
        String requirements = "";
        for (LoadingRequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (LoadingRequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
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
                        .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()));
            } else {
                // Show default label (listener type)
                this.labelComponent = this.instance.parent.getDisplayName().copy()
                        .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()));
            }
        }
        
        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (this.editMode && this.editBox != null) {
                // Render edit box
                this.editBox.setX(this.getX());
                this.editBox.setY(this.getY() + TOP_DOWN_CELL_BORDER);
                this.editBox.setWidth(Math.min(this.getWidth(), 200));
                this.editBox.setHeight(Minecraft.getInstance().font.lineHeight + 1);
                this.editBox.render(graphics, mouseX, mouseY, partial);
                
                // Check if user clicked outside or pressed enter
                if (MouseInput.isLeftMouseDown() && !this.editBox.isHovered()) {
                    this.exitEditMode(true);
                }
            } else {
                // Render label
                RenderingUtils.resetShaderColor(graphics);
                UIBase.drawElementLabel(graphics, Minecraft.getInstance().font, this.labelComponent, this.getX(), this.getY() + TOP_DOWN_CELL_BORDER);
                RenderingUtils.resetShaderColor(graphics);
                // Show Writing cursor when the label is hovered
                if (UIBase.isXYInArea(mouseX, mouseY, this.getX(), this.getY() + TOP_DOWN_CELL_BORDER, Minecraft.getInstance().font.width(this.labelComponent), Minecraft.getInstance().font.lineHeight)) {
                    CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
                }
            }
        }
        
        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            if (this.editMode && this.editBox != null) {
                this.setWidth(Math.min((int)(ManageListenersScreen.this.scrollArea.getInnerWidth() - 40), 200));
            } else {
                this.setWidth(Minecraft.getInstance().font.width(this.labelComponent));
            }
            this.setHeight(Minecraft.getInstance().font.lineHeight + (TOP_DOWN_CELL_BORDER * 2));
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
                        ManageListenersScreen.this.updateSelectedInstance();
                        ManageListenersScreen.this.onEditActionsOfSelected();
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
        
        protected void enterEditMode() {
            if (this.editMode) return; // Already in edit mode
            
            this.editMode = true;
            
            // Create edit box
            this.editBox = new ExtendedEditBox(
                    Minecraft.getInstance().font, 
                    this.getX(), 
                    this.getY(), 
                    Math.min(200, (int)(ManageListenersScreen.this.scrollArea.getInnerWidth() - 40)), 
                    18, 
                    Component.empty()
            );
            UIBase.applyDefaultWidgetSkinTo(this.editBox);
            this.editBox.setMaxLength(100000);
            
            // Set current display name or empty if null
            String currentName = this.instance.getDisplayName();
            if (currentName != null) {
                this.editBox.setValue(currentName);
            } else {
                this.editBox.setValue("");
            }
            
            this.editBox.setFocused(true);
            this.editBox.setCursorPosition(this.editBox.getValue().length());
            this.editBox.setHighlightPos(0);
            
            // Add to children for input handling
            this.children.clear();
            this.children.add(this.editBox);
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
                if (ManageListenersScreen.this.selectedInstance == this.instance) {
                    ManageListenersScreen.this.updateDescriptionArea();
                }
            }
            
            this.editMode = false;
            this.editBox = null;
            this.children.clear();
        }
        
    }

}
