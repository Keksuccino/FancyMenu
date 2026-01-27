package de.keksuccino.fancymenu.customization.scheduler.gui;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.DelayExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ExecuteLaterExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerInstance;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ManageSchedulersScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 476;

    @NotNull
    protected final Consumer<Boolean> callback;
    @NotNull
    protected final List<SchedulerInstance> tempInstances = new ArrayList<>();
    @Nullable
    protected SchedulerInstance selectedInstance;
    @Nullable
    protected String cachedStatusIdentifier;
    protected boolean cachedStatusRunning = false;

    public ManageSchedulersScreen(@NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.schedulers.manage"));
        this.callback = callback;
        this.setAllowCloseOnEsc(false);
        this.tempInstances.addAll(SchedulerHandler.getInstances());
        this.setSearchBarEnabled(true);
        this.setDescriptionAreaEnabled(true);
    }

    @Override
    protected void initCells() {

        String editingInstanceId = null;
        String editingValue = null;
        for (RenderCell cell : this.allCells) {
            if (cell instanceof SchedulerInstanceCell instanceCell) {
                if (instanceCell.editMode && instanceCell.editBox != null) {
                    editingInstanceId = instanceCell.instance.getIdentifier();
                    editingValue = instanceCell.editBox.getValue();
                    break;
                }
            }
        }

        this.addSpacerCell(5).setIgnoreSearch();

        List<SchedulerInstanceCell> instanceCells = new ArrayList<>();
        for (SchedulerInstance instance : this.tempInstances) {
            SchedulerInstanceCell cell = new SchedulerInstanceCell(instance);
            instanceCells.add(cell);
        }

        instanceCells.sort(Comparator
                .comparing((SchedulerInstanceCell cell) -> cell.labelComponent.getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(cell -> cell.labelComponent.getString())
                .thenComparing(cell -> cell.instance.getIdentifier()));

        String finalEditingInstanceId = editingInstanceId;
        String finalEditingValue = editingValue;
        instanceCells.forEach(cell -> {
            this.addCell(cell).setSelectable(true);
            if (finalEditingInstanceId != null && finalEditingInstanceId.equals(cell.instance.getIdentifier())) {
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

        this.addRightSideButton(20, Component.translatable("fancymenu.schedulers.manage.add"), button -> {
            SchedulerInstance newInstance = SchedulerHandler.createFreshInstance();
            ActionScriptEditorWindowBody actionsScreen = new ActionScriptEditorWindowBody(newInstance.getActionScript(), updatedScript -> {
                if (updatedScript != null) {
                    newInstance.setActionScript(updatedScript);
                    this.tempInstances.add(newInstance);
                    this.applyChangesNow();
                    this.rebuild();
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
        });

        this.addRightSideButton(20, Component.translatable("fancymenu.schedulers.manage.edit"), button -> {
            this.onEditActionsOfSelected();
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);

        this.addRightSideButton(20, Component.empty(), button -> {
            SchedulerInstance selected = this.selectedInstance;
            if (selected != null) {
                if (SchedulerHandler.isRunning(selected.getIdentifier())) {
                    SchedulerHandler.stopScheduler(selected.getIdentifier());
                } else {
                    SchedulerHandler.startScheduler(selected.getIdentifier());
                }
                this.updateDescriptionArea();
            }
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null)
                .setLabelSupplier(consumes -> {
                    SchedulerInstance selected = this.selectedInstance;
                    if (selected != null && SchedulerHandler.isRunning(selected.getIdentifier())) {
                        return Component.translatable("fancymenu.schedulers.manage.stop_now");
                    }
                    return Component.translatable("fancymenu.schedulers.manage.start_now");
                })
                .setUITooltipSupplier(consumes -> {
                    SchedulerInstance selected = this.selectedInstance;
                    if (selected != null && SchedulerHandler.isRunning(selected.getIdentifier())) {
                        return UITooltip.of(Component.translatable("fancymenu.schedulers.manage.stop_now.desc"));
                    }
                    return UITooltip.of(Component.translatable("fancymenu.schedulers.manage.start_now.desc"));
                });

        this.addRightSideButton(20, Component.translatable("fancymenu.schedulers.manage.settings"), button -> {
            SchedulerInstance selected = this.selectedInstance;
            if (selected != null) {
                PiPWindow parentWindow = this.getWindow();
                if (parentWindow != null) {
                    parentWindow.setVisible(false);
                }
                ManageSchedulerSettingsScreen settingsScreen = new ManageSchedulerSettingsScreen(selected, identifier -> isIdentifierAvailable(selected, identifier), call -> {
                    if (call) {
                        this.applyChangesNow();
                        this.rebuild();
                        this.updateDescriptionArea();
                    }
                });
                PiPWindow settingsWindow = ManageSchedulerSettingsScreen.openInWindow(settingsScreen, parentWindow);
                if (parentWindow != null) {
                    settingsWindow.setPosition(parentWindow.getX(), parentWindow.getY());
                    settingsWindow.addCloseCallback(() -> parentWindow.setVisible(true));
                }
            }
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);

        this.addRightSideButton(20, Component.translatable("fancymenu.schedulers.manage.remove"), button -> {
            SchedulerInstance sel = this.selectedInstance;
            if (sel != null) {
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.schedulers.manage.delete_warning"), MessageDialogStyle.WARNING, call -> {
                    if (call) {
                        this.tempInstances.remove(sel);
                        this.applyChangesNow();
                        this.selectedInstance = null;
                        this.rebuild();
                    }
                });
            }
        }).setIsActiveSupplier(consumes -> this.selectedInstance != null);

    }

    protected void onEditActionsOfSelected() {
        if (this.selectedInstance != null) {
            SchedulerInstance cached = this.selectedInstance;
            ActionScriptEditorWindowBody actionsScreen = new ActionScriptEditorWindowBody(cached.getActionScript(), updatedScript -> {
                if (updatedScript != null) {
                    cached.setActionScript(updatedScript);
                    this.applyChangesNow();
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

        if (this.selectedInstance == null) return null;

        List<Component> newDesc = new ArrayList<>();

        newDesc.add(Component.translatable("fancymenu.schedulers.manage.description.scheduler_info").withStyle(ChatFormatting.BOLD));
        newDesc.add(Component.empty());
        newDesc.add(Component.translatable("fancymenu.schedulers.manage.description.id", Component.literal(this.selectedInstance.getIdentifier())));

        Component tickDelayValue = (this.selectedInstance.getTickDelayMs() <= 0)
                ? Component.translatable("fancymenu.schedulers.manage.description.tick_delay.every_tick")
                : Component.literal(this.selectedInstance.getTickDelayMs() + " ms");
        Component ticksToRunValue = (this.selectedInstance.getTicksToRun() <= 0)
                ? Component.translatable("fancymenu.schedulers.manage.description.ticks_to_run.permanent")
                : Component.literal("" + this.selectedInstance.getTicksToRun());
        Component startOnLaunchValue = Component.translatable(this.selectedInstance.isStartOnLaunch()
                ? "fancymenu.general.cycle.enabled_disabled.enabled"
                : "fancymenu.general.cycle.enabled_disabled.disabled");
        boolean isRunning = SchedulerHandler.isRunning(this.selectedInstance.getIdentifier());
        Component statusValue = Component.translatable(isRunning
                ? "fancymenu.schedulers.manage.description.status.running"
                : "fancymenu.schedulers.manage.description.status.not_running")
                .withStyle(Style.EMPTY.withColor(isRunning
                        ? UIBase.getUITheme().success_text_color.getColorInt()
                        : UIBase.getUITheme().error_text_color.getColorInt()));

        newDesc.add(Component.translatable("fancymenu.schedulers.manage.description.start_delay", Component.literal(this.selectedInstance.getStartDelayMs() + " ms")));
        newDesc.add(Component.translatable("fancymenu.schedulers.manage.description.tick_delay", tickDelayValue));
        newDesc.add(Component.translatable("fancymenu.schedulers.manage.description.ticks_to_run", ticksToRunValue));
        newDesc.add(Component.translatable("fancymenu.schedulers.manage.description.start_on_launch", startOnLaunchValue));
        newDesc.add(Component.translatable("fancymenu.schedulers.manage.description.status", statusValue));

        newDesc.add(Component.empty());
        newDesc.add(Component.empty());

        newDesc.add(Component.translatable("fancymenu.schedulers.manage.description.actions").withStyle(ChatFormatting.BOLD));
        newDesc.add(Component.empty());

        List<Component> actionLines = this.buildActionScriptDescription(this.selectedInstance.getActionScript(), 0);
        newDesc.addAll(actionLines);

        return newDesc;

    }

    @Override
    public boolean allowEnterForDone() {
        return false;
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.updateSelectedInstance();
        SchedulerInstance selected = this.selectedInstance;
        if (selected == null) {
            this.cachedStatusIdentifier = null;
            return;
        }
        boolean running = SchedulerHandler.isRunning(selected.getIdentifier());
        if (!selected.getIdentifier().equals(this.cachedStatusIdentifier) || (running != this.cachedStatusRunning)) {
            this.cachedStatusIdentifier = selected.getIdentifier();
            this.cachedStatusRunning = running;
            this.updateDescriptionArea();
        }
    }

    @Override
    public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.descriptionScrollArea != null) {
            int descW = (int) this.descriptionScrollArea.getWidthWithBorder();
            int descEndX = (int) (this.descriptionScrollArea.getXWithBorder() + this.descriptionScrollArea.getWidthWithBorder());
            int descEndY = (int) (this.descriptionScrollArea.getYWithBorder() + this.descriptionScrollArea.getHeightWithBorder());
            int inactiveLabelColor = this.getInactiveLabelTextColor();
            List<MutableComponent> renameTip = UIBase.lineWrapUIComponentsSmall(Component.translatable("fancymenu.schedulers.manage.rename_tip").withStyle(Style.EMPTY.withColor(inactiveLabelColor)), descW);
            List<MutableComponent> quickEditTip = UIBase.lineWrapUIComponentsSmall(Component.translatable("fancymenu.schedulers.manage.quick_edit_tip").withStyle(Style.EMPTY.withColor(inactiveLabelColor)), descW);
            int lineY = descEndY + 4;
            for (MutableComponent line : renameTip) {
                int lineWidth = (int)UIBase.getUITextWidthSmall(line);
                int lineX = descEndX - lineWidth;
                UIBase.renderText(graphics, line, lineX, lineY, -1, UIBase.getUITextSizeSmall());
                lineY += (int) (UIBase.getUITextHeightSmall() + 2);
            }
            lineY += 2;
            for (MutableComponent line : quickEditTip) {
                int lineWidth = (int)UIBase.getUITextWidthSmall(line);
                int lineX = descEndX - lineWidth;
                UIBase.renderText(graphics, line, lineX, lineY, -1, UIBase.getUITextSizeSmall());
                lineY += (int) (UIBase.getUITextHeightSmall() + 2);
            }
        }

    }

    @Override
    public void setWindow(@Nullable PiPWindow window) {
        super.setWindow(window);
    }

    @Override
    protected void onCancel() {
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(true);
    }

    @Override
    public boolean showCancel() {
        return false;
    }

    @Override
    public boolean allowCancel() {
        return false;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageSchedulersScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageSchedulersScreen screen) {
        return openInWindow(screen, null);
    }

    protected void updateSelectedInstance() {
        this.updateSelectedCell();
        RenderCell selected = this.getSelectedCell();
        if (selected instanceof SchedulerInstanceCell cell) {
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

    protected int getInactiveLabelTextColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt();
    }

    protected boolean isIdentifierAvailable(@NotNull SchedulerInstance current, @NotNull String identifier) {
        for (SchedulerInstance instance : this.tempInstances) {
            if (instance != current && instance.getIdentifier().equals(identifier)) {
                return false;
            }
        }
        return true;
    }

    protected boolean validateInstances() {
        Set<String> ids = new HashSet<>();
        for (SchedulerInstance instance : this.tempInstances) {
            String identifier = instance.getIdentifier();
            if ((identifier == null) || identifier.isBlank() || !SchedulerHandler.isIdentifierValid(identifier)) {
                Dialogs.openMessage(Component.translatable("fancymenu.schedulers.manage.invalid_id"), MessageDialogStyle.ERROR);
                return false;
            }
            if (!ids.add(identifier)) {
                Dialogs.openMessage(Component.translatable("fancymenu.schedulers.manage.id_in_use"), MessageDialogStyle.ERROR);
                return false;
            }
        }
        return true;
    }

    protected void applyChangesNow() {
        if (this.validateInstances()) {
            SchedulerHandler.applyChanges(this.tempInstances);
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
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().bullet_list_dot_color_1.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.screens.manage_screen.info.value.none")
                            .setStyle(Style.EMPTY.withColor(this.getLabelTextColor()))));
        }

        return lines;
    }

    @NotNull
    protected List<Component> buildExecutableDescription(@NotNull Executable executable, int indentLevel) {
        List<Component> lines = new ArrayList<>();
        String indent = "  ".repeat(Math.max(0, indentLevel));
        int labelColor = this.getLabelTextColor();

        if (executable instanceof ActionInstance actionInstance) {
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().bullet_list_dot_color_2.getColorInt()))
                    .append(actionInstance.action.getDisplayName().copy()
                            .setStyle(Style.EMPTY.withColor(labelColor))));

            String cachedValue = actionInstance.value;
            String valueString = ((cachedValue != null) && actionInstance.action.hasValue())
                    ? cachedValue
                    : I18n.get("fancymenu.actions.screens.manage_screen.info.value.none");
            lines.add(Component.literal(indent + "    ◦ ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().bullet_list_dot_color_1.getColorInt()))
                    .append(Component.literal(I18n.get("fancymenu.actions.screens.manage_screen.info.value") + " ")
                            .setStyle(Style.EMPTY.withColor(labelColor)))
                    .append(Component.literal(valueString)
                            .setStyle(Style.EMPTY.withColor(labelColor))));

        } else if (executable instanceof IfExecutableBlock ifBlock) {
            String requirements = this.buildRequirementsString(ifBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.if", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(labelColor))));

            for (Executable nested : ifBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }

            AbstractExecutableBlock appended = ifBlock.getAppendedBlock();
            while (appended != null) {
                lines.addAll(this.buildAppendedBlockDescription(appended, indentLevel));
                appended = appended.getAppendedBlock();
            }

        } else if (executable instanceof WhileExecutableBlock whileBlock) {
            String requirements = this.buildRequirementsString(whileBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.while", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(labelColor))));

            for (Executable nested : whileBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
        } else if (executable instanceof DelayExecutableBlock delayBlock) {
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.delay", Component.literal(delayBlock.getDelayMsRaw()))
                            .setStyle(Style.EMPTY.withColor(labelColor))));

            for (Executable nested : delayBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
        } else if (executable instanceof ExecuteLaterExecutableBlock executeLaterBlock) {
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.execute_later", Component.literal(executeLaterBlock.getDelayMsRaw()))
                            .setStyle(Style.EMPTY.withColor(labelColor))));

            for (Executable nested : executeLaterBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
        } else if (executable instanceof AbstractExecutableBlock) {
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))
                    .append(Component.literal("[UNKNOWN BLOCK]")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))));
        }

        return lines;
    }

    @NotNull
    protected List<Component> buildAppendedBlockDescription(@NotNull AbstractExecutableBlock block, int indentLevel) {
        List<Component> lines = new ArrayList<>();
        String indent = "  ".repeat(Math.max(0, indentLevel));
        int labelColor = this.getLabelTextColor();

        if (block instanceof ElseIfExecutableBlock elseIfBlock) {
            String requirements = this.buildRequirementsString(elseIfBlock);
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.else_if", Component.literal(requirements))
                            .setStyle(Style.EMPTY.withColor(labelColor))));

            for (Executable nested : elseIfBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }

        } else if (block instanceof ElseExecutableBlock elseBlock) {
            lines.add(Component.literal(indent + "• ")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()))
                    .append(Component.translatable("fancymenu.actions.blocks.else")
                            .setStyle(Style.EMPTY.withColor(labelColor))));

            for (Executable nested : elseBlock.getExecutables()) {
                lines.addAll(this.buildExecutableDescription(nested, indentLevel + 1));
            }
        }

        return lines;
    }

    @NotNull
    protected String buildRequirementsString(@NotNull IfExecutableBlock block) {
        String requirements = "";
        for (RequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (RequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
    }

    @NotNull
    protected String buildRequirementsString(@NotNull ElseIfExecutableBlock block) {
        String requirements = "";
        for (RequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (RequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
    }

    @NotNull
    protected String buildRequirementsString(@NotNull WhileExecutableBlock block) {
        String requirements = "";
        for (RequirementGroup g : block.condition.getGroups()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += g.identifier;
        }
        for (RequirementInstance i : block.condition.getInstances()) {
            if (!requirements.isEmpty()) requirements += ", ";
            requirements += i.requirement.getDisplayName();
        }
        return requirements.isEmpty() ? "none" : requirements;
    }

    public class SchedulerInstanceCell extends RenderCell {

        @NotNull
        protected final SchedulerInstance instance;
        @NotNull
        protected Component labelComponent;
        @Nullable
        protected ExtendedEditBox editBox;
        protected boolean editMode = false;
        protected long lastClickTime = 0;
        protected static final long DOUBLE_CLICK_TIME = 500;
        protected static final int TOP_DOWN_CELL_BORDER = 1;

        public SchedulerInstanceCell(@NotNull SchedulerInstance instance) {
            this.instance = instance;
            this.updateLabelComponent();
            this.setSearchStringSupplier(() -> {
                return this.instance.getIdentifier();
            });
        }
        
        protected void updateLabelComponent() {
            this.labelComponent = Component.literal(this.instance.getIdentifier())
                    .setStyle(Style.EMPTY.withColor(ManageSchedulersScreen.this.getLabelTextColor()));
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (this.editMode && this.editBox != null) {
                this.editBox.setX(this.getX());
                this.editBox.setY(this.getY() + TOP_DOWN_CELL_BORDER);
                this.editBox.setWidth(Math.min(this.getWidth(), 200));
                this.editBox.setHeight((int)(UIBase.getUITextHeightNormal() + 1));
                this.editBox.render(graphics, mouseX, mouseY, partial);

                if (MouseInput.isLeftMouseDown() && !this.editBox.isHovered()) {
                    this.exitEditMode(true);
                }
            } else {
                RenderingUtils.resetShaderColor(graphics);
                UIBase.renderText(graphics, this.labelComponent, this.getX(), this.getY() + TOP_DOWN_CELL_BORDER);
                RenderingUtils.resetShaderColor(graphics);
                if (UIBase.isXYInArea(mouseX, mouseY, this.getX(), this.getY() + TOP_DOWN_CELL_BORDER, UIBase.getUITextWidthNormal(this.labelComponent), UIBase.getUITextHeightNormal())) {
                    CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
                }
            }
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            if (this.editMode && this.editBox != null) {
                this.setWidth(Math.min((int)(ManageSchedulersScreen.this.scrollArea.getInnerWidth() - 40), 200));
            } else {
                this.setWidth((int)UIBase.getUITextWidthNormal(this.labelComponent));
            }
            this.setHeight((int)(UIBase.getUITextHeightNormal() + (TOP_DOWN_CELL_BORDER * 2)));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && this.isHovered() && !this.editMode) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - this.lastClickTime < DOUBLE_CLICK_TIME) {
                    this.enterEditMode();
                    this.lastClickTime = 0;
                } else {
                    this.lastClickTime = currentTime;
                }
            }
            boolean b = super.mouseClicked(mouseX, mouseY, button);
            if ((button == 1) && this.isHovered() && !this.editMode) {
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        this.setSelected(true);
                        ManageSchedulersScreen.this.updateSelectedInstance();
                        ManageSchedulersScreen.this.onEditActionsOfSelected();
                    }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
                }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            }
            return b;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.editMode && this.editBox != null) {
                if (keyCode == InputConstants.KEY_ENTER || keyCode == InputConstants.KEY_NUMPADENTER) {
                    this.exitEditMode(true);
                    return true;
                } else if (keyCode == InputConstants.KEY_ESCAPE) {
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
            if (this.editMode) return;

            this.editMode = true;
            this.editBox = new ExtendedEditBox(
                    Minecraft.getInstance().font,
                    this.getX(),
                    this.getY(),
                    Math.min(200, (int)(ManageSchedulersScreen.this.scrollArea.getInnerWidth() - 40)),
                    18,
                    Component.empty()
            );
            UIBase.applyDefaultWidgetSkinTo(this.editBox, UIBase.shouldBlur());
            this.editBox.setMaxLength(100000);

            this.editBox.setValue(this.instance.getIdentifier());

            this.editBox.setFocused(true);
            this.editBox.setCursorPosition(this.editBox.getValue().length());
            this.editBox.setHighlightPos(0);

            this.children.clear();
            this.children.add(this.editBox);
        }

        protected void exitEditMode(boolean save) {
            if (!this.editMode || this.editBox == null) return;

            if (save) {
                String newIdentifier = this.editBox.getValue().trim();
                String oldIdentifier = this.instance.getIdentifier();
                if (!newIdentifier.isBlank() && SchedulerHandler.isIdentifierValid(newIdentifier) && ManageSchedulersScreen.this.isIdentifierAvailable(this.instance, newIdentifier)) {
                    if (!oldIdentifier.equals(newIdentifier)) {
                        SchedulerHandler.stopScheduler(oldIdentifier);
                        this.instance.setIdentifier(newIdentifier);
                        ManageSchedulersScreen.this.applyChangesNow();
                    }
                } else if (!newIdentifier.equals(oldIdentifier)) {
                    if (!SchedulerHandler.isIdentifierValid(newIdentifier) || newIdentifier.isBlank()) {
                        Dialogs.openMessage(Component.translatable("fancymenu.schedulers.manage.invalid_id"), MessageDialogStyle.ERROR);
                    } else {
                        Dialogs.openMessage(Component.translatable("fancymenu.schedulers.manage.id_in_use"), MessageDialogStyle.ERROR);
                    }
                }
                this.updateLabelComponent();
                if (ManageSchedulersScreen.this.selectedInstance == this.instance) {
                    ManageSchedulersScreen.this.updateDescriptionArea();
                }
            }

            this.editMode = false;
            this.editBox = null;
            this.children.clear();
        }

    }

}
