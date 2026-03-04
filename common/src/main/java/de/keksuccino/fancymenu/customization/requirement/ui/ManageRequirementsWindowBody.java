package de.keksuccino.fancymenu.customization.requirement.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Function;

public class ManageRequirementsWindowBody extends PiPWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;
    private static final int HISTORY_LIMIT = 500;
    private static final long ENTRY_DOUBLE_CLICK_TIME_MS = 500L;

    protected RequirementContainer container;
    protected Consumer<RequirementContainer> callback;
    protected final boolean allowGroupManagement;

    protected ScrollArea requirementsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;
    protected ContextMenu rightClickContextMenu;
    protected float rightClickContextMenuLastOpenX = Float.NaN;
    protected float rightClickContextMenuLastOpenY = Float.NaN;
    @Nullable
    protected String contextMenuTargetGroupIdentifier;
    @Nullable
    protected String contextMenuTargetInstanceIdentifier;
    @Nullable
    protected ScrollAreaEntry lastLeftClickedRequirementsEntry;
    protected long lastLeftRequirementsEntryClickTimeMs = 0L;
    private final Deque<RequirementsSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<RequirementsSnapshot> redoHistory = new ArrayDeque<>();

    public ManageRequirementsWindowBody(@NotNull RequirementContainer container, @NotNull Consumer<RequirementContainer> callback) {
        this(container, callback, true);
    }

    public ManageRequirementsWindowBody(@NotNull RequirementContainer container, @NotNull Consumer<RequirementContainer> callback, boolean allowGroupManagement) {
        super(Component.literal(I18n.get("fancymenu.requirements.screens.manage_screen.manage")));
        this.container = container;
        this.callback = callback;
        this.allowGroupManagement = allowGroupManagement;
        this.updateRequirementsScrollArea();
    }

    @Override
    protected void init() {
        boolean blur = UIBase.shouldBlur();
        this.requirementsScrollArea.setSetupForBlurInterface(blur);
        this.updateRightClickContextMenu(false);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.cancel"), (button) -> {
            this.closeRightClickContextMenu();
            this.callback.accept(null);
            this.closeWindow();
        });
        this.cancelButton.setNavigatable(false);
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, blur);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.done"), (button) -> this.triggerDoneAction());
        this.doneButton.setNavigatable(false);
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, blur);

        this.addWidget(this.requirementsScrollArea);

    }

    protected void updateRightClickContextMenu(boolean reopen) {
        boolean wasOpen = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.rightClickContextMenu = new ContextMenu();

        this.rightClickContextMenu.addClickableEntry("add_requirement", Component.translatable("fancymenu.requirements.screens.add_requirement"), (menu, entry) -> {
                    menu.closeMenuChain();
                    this.onAddRequirement();
                }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.add_requirement.desc")))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.a"))
                .setIcon(MaterialIcons.ADD);

        if (this.allowGroupManagement) {
            this.rightClickContextMenu.addClickableEntry("add_group", Component.translatable("fancymenu.requirements.screens.add_group"), (menu, entry) -> {
                        menu.closeMenuChain();
                        this.onAddGroup();
                    }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.add_group.desc")))
                    .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.g"))
                    .setIcon(MaterialIcons.ADD);
        }

        this.rightClickContextMenu.addSeparatorEntry("separator_after_add");

        if (this.allowGroupManagement) {
            this.rightClickContextMenu.addClickableEntry("edit_group_requirements", Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.edit_requirements"), (menu, entry) -> {
                        RequirementGroup selectedGroup = this.getContextMenuTargetGroup();
                        if (selectedGroup == null) {
                            return;
                        }
                        menu.closeMenuChain();
                        this.onEditGroupRequirements(selectedGroup);
                        }).addIsActiveSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null)
                    .addIsVisibleSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null)
                    .setTooltipSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null
                            ? UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.edit_requirements.desc"))
                            : UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.no_group_selected")))
                    .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.enter"))
                    .setIcon(MaterialIcons.EDIT);

            this.rightClickContextMenu.addClickableEntry("edit_group_identifier", Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.edit_identifier"), (menu, entry) -> {
                        RequirementGroup selectedGroup = this.getContextMenuTargetGroup();
                        if (selectedGroup == null) {
                            return;
                        }
                        menu.closeMenuChain();
                        this.onEditGroupIdentifier(selectedGroup);
                    }).addIsActiveSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null)
                    .addIsVisibleSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null)
                    .setTooltipSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null
                            ? UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.edit_identifier.desc"))
                            : UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.no_group_selected")))
                    .setIcon(MaterialIcons.DRIVE_FILE_RENAME_OUTLINE);

            this.rightClickContextMenu.addClickableEntry("cycle_group_mode", Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.cycle_mode.generic"), (menu, entry) -> {
                        RequirementGroup selectedGroup = this.getContextMenuTargetGroup();
                        if (selectedGroup == null) {
                            return;
                        }
                        this.onCycleGroupMode(selectedGroup);
                    }).setLabelSupplier((menu, entry) -> this.getContextMenuCycleGroupModeLabel())
                    .addIsActiveSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null)
                    .addIsVisibleSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null)
                    .setTooltipSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null
                            ? UITooltip.of(Component.translatable("fancymenu.requirements.screens.build_group_screen.mode.desc"))
                            : UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.no_group_selected")))
                    .setIcon(MaterialIcons.SWAP_HORIZ);

            this.rightClickContextMenu.addSeparatorEntry("separator_after_group_settings")
                    .addIsVisibleSupplier((menu, entry) -> this.getContextMenuTargetGroup() != null);
        }

        this.rightClickContextMenu.addClickableEntry("edit", Component.translatable("fancymenu.requirements.screens.manage_screen.edit.generic"), (menu, entry) -> {
                    RequirementInstance selectedInstance = this.getContextMenuTargetInstance();
                    if (selectedInstance == null) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.onEdit(selectedInstance, null);
                }).setLabelSupplier((menu, entry) -> this.getContextMenuEditLabel())
                .addIsActiveSupplier((menu, entry) -> this.getContextMenuTargetInstance() != null)
                .addIsVisibleSupplier((menu, entry) -> this.getContextMenuTargetInstance() != null)
                .setTooltipSupplier((menu, entry) -> (this.getContextMenuTargetInstance() != null)
                        ? UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.edit.desc"))
                        : UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.no_entry_selected")))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.enter"))
                .setIcon(MaterialIcons.EDIT);

        this.rightClickContextMenu.addSeparatorEntry("separator_before_remove")
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget());

        this.rightClickContextMenu.addClickableEntry("remove", Component.translatable("fancymenu.requirements.screens.manage_screen.remove.generic"), (menu, entry) -> {
                    RequirementInstance selectedInstance = this.getContextMenuTargetInstance();
                    RequirementGroup selectedGroup = this.getContextMenuTargetGroup();
                    if ((selectedInstance == null) && (selectedGroup == null)) {
                        return;
                    }
                    menu.closeMenuChain();
                    this.onRemove(selectedInstance, selectedGroup);
                }).setLabelSupplier((menu, entry) -> this.getContextMenuRemoveLabel())
                .addIsActiveSupplier((menu, entry) -> this.hasContextMenuTarget())
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget())
                .setTooltipSupplier((menu, entry) -> this.hasContextMenuTarget()
                        ? UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.remove.desc"))
                        : UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.no_entry_selected")))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.delete"))
                .setIcon(MaterialIcons.DELETE);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_remove")
                .addIsVisibleSupplier((menu, entry) -> this.hasContextMenuTarget());

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

    protected void onAddRequirement() {
        RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
        BuildRequirementWindowBody screen = new BuildRequirementWindowBody(this.container, null, (call) -> {
            if (call != null) {
                this.container.addInstance(call);
                this.updateRequirementsScrollArea();
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        });
        this.openChildWindow(parentWindow -> BuildRequirementWindowBody.openInWindow(screen, parentWindow));
    }

    protected void onAddGroup() {
        if (!this.allowGroupManagement) {
            return;
        }
        RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
        RequirementGroup group = this.container.createAndAddGroup(this.generateUniqueGroupIdentifier(), RequirementGroup.GroupMode.AND);
        if (group != null) {
            this.contextMenuTargetGroupIdentifier = group.identifier;
            this.contextMenuTargetInstanceIdentifier = null;
            this.updateRequirementsScrollArea();
            this.restoreSelection(group.identifier, null);
            ScrollAreaEntry focusedEntry = this.requirementsScrollArea.getFocusedEntry();
            if (focusedEntry != null) {
                this.scrollEntryIntoView(focusedEntry);
            }
            this.createUndoPointIfChanged(beforeSnapshot);
        }
    }

    protected void onEdit(@Nullable RequirementInstance selectedInstance, @Nullable RequirementGroup selectedGroup) {
        if (selectedInstance != null) {
            RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
            BuildRequirementWindowBody requirementScreen = new BuildRequirementWindowBody(this.container, selectedInstance, (call) -> {
                if (call != null) {
                    this.updateRequirementsScrollArea();
                    this.createUndoPointIfChanged(beforeSnapshot);
                }
            });
            this.openChildWindow(parentWindow -> BuildRequirementWindowBody.openInWindow(requirementScreen, parentWindow));
            return;
        }
        if (selectedGroup != null) {
            this.onEditGroupRequirements(selectedGroup);
        }
    }

    protected void onEditGroupRequirements(@NotNull RequirementGroup selectedGroup) {
        RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
        String selectedGroupIdentifier = selectedGroup.identifier;
        RequirementContainer groupRequirements = this.buildGroupRequirementsContainer(selectedGroup);
        ManageRequirementsWindowBody groupRequirementsScreen = new ManageRequirementsWindowBody(groupRequirements, (call) -> {
            if (call == null) {
                return;
            }
            RequirementGroup currentGroup = this.container.getGroup(selectedGroupIdentifier);
            if (currentGroup == null) {
                return;
            }
            this.applyGroupRequirementsFromContainer(currentGroup, call);
            this.updateRequirementsScrollArea();
            this.restoreSelection(currentGroup.identifier, null);
            this.createUndoPointIfChanged(beforeSnapshot);
        }, false);
        this.openChildWindow(parentWindow -> ManageRequirementsWindowBody.openInWindow(groupRequirementsScreen, parentWindow), true);
    }

    protected void onEditGroupIdentifier(@NotNull RequirementGroup selectedGroup) {
        TextInputWindowBody inputScreen = new TextInputWindowBody(CharacterFilter.buildOnlyLowercaseFileNameFilter(), (call) -> {
            if (call == null) {
                return;
            }
            if (call.equals(selectedGroup.identifier)) {
                return;
            }
            RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
            selectedGroup.identifier = call;
            this.contextMenuTargetGroupIdentifier = call;
            this.contextMenuTargetInstanceIdentifier = null;
            this.updateRequirementsScrollArea();
            this.restoreSelection(call, null);
            this.createUndoPointIfChanged(beforeSnapshot);
        });
        inputScreen.setText(selectedGroup.identifier);
        inputScreen.setTextValidator(s -> this.isGroupIdentifierValidForRename(selectedGroup, s.getText()));
        inputScreen.setTextValidatorUserFeedback(UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.edit_identifier.invalid")));
        Dialogs.openGeneric(inputScreen,
                Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.edit_identifier"),
                null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                .getSecond().setIcon(MaterialIcons.DRIVE_FILE_RENAME_OUTLINE);
    }

    protected void onCycleGroupMode(@NotNull RequirementGroup selectedGroup) {
        RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
        selectedGroup.mode = (selectedGroup.mode == RequirementGroup.GroupMode.AND)
                ? RequirementGroup.GroupMode.OR
                : RequirementGroup.GroupMode.AND;
        this.updateRequirementsScrollArea();
        this.restoreSelection(selectedGroup.identifier, null);
        this.createUndoPointIfChanged(beforeSnapshot);
        this.updateRightClickContextMenu(true);
    }

    protected void onRemove(@Nullable RequirementInstance selectedInstance, @Nullable RequirementGroup selectedGroup) {
        if (selectedInstance != null) {
            RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
            if (this.container.removeInstance(selectedInstance)) {
                this.clearContextMenuTarget();
                this.updateRequirementsScrollArea();
                this.createUndoPointIfChanged(beforeSnapshot);
            }
            return;
        }
        if (selectedGroup != null) {
            RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
            if (this.container.removeGroup(selectedGroup)) {
                this.clearContextMenuTarget();
                this.updateRequirementsScrollArea();
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        }
    }

    @Override
    public void onWindowClosedExternally() {
        this.closeRightClickContextMenu();
        this.callback.accept(null);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int textColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt()
                : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
        int scrollAreaTop = 50 + 15;
        float labelY = scrollAreaTop - UIBase.getUITextHeightNormal() - UIBase.getAreaLabelVerticalPadding();
        Component requirementsLabel = this.allowGroupManagement
                ? Component.translatable("fancymenu.requirements.screens.manage_screen.requirements_and_groups")
                : Component.translatable("fancymenu.requirements.screens.build_group_screen.group_requirements");
        UIBase.renderText(graphics, requirementsLabel, 20, labelY, textColor);

        this.requirementsScrollArea.setWidth(this.width - 20 - 20, true);
        this.requirementsScrollArea.setHeight(this.height - 85, true);
        this.requirementsScrollArea.setX(20, true);
        this.requirementsScrollArea.setY(scrollAreaTop, true);

        int buttonsRightEdge = this.width - 20;
        int buttonY = Math.max(20, (int)(this.requirementsScrollArea.getYWithBorder() - this.doneButton.getHeight() - 5));
        this.doneButton.setX(buttonsRightEdge - this.doneButton.getWidth());
        this.doneButton.setY(buttonY);
        this.cancelButton.setX(Math.max(20, this.doneButton.getX() - 5 - this.cancelButton.getWidth()));
        this.cancelButton.setY(buttonY);

        this.requirementsScrollArea.render(graphics, mouseX, mouseY, partial);
        if (this.requirementsScrollArea.getEntries().isEmpty()) {
            Component hint = Component.translatable(this.allowGroupManagement
                    ? "fancymenu.requirements.screens.manage_screen.empty_hint"
                    : "fancymenu.requirements.screens.manage_screen.empty_hint.requirements_only");
            float hintWidth = UIBase.getUITextWidthNormal(hint);
            float hintX = this.requirementsScrollArea.getInnerX() + (this.requirementsScrollArea.getInnerWidth() / 2F) - (hintWidth / 2F);
            float hintY = this.requirementsScrollArea.getInnerY() + (this.requirementsScrollArea.getInnerHeight() / 2F) - (UIBase.getUITextHeightNormal() / 2F);
            UIBase.renderText(graphics, hint, hintX, hintY, UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt());
        }

        this.doneButton.render(graphics, mouseX, mouseY, partial);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        String keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        if (keyName == null) {
            keyName = "";
        }
        keyName = keyName.toLowerCase();
        boolean contextMenuActive = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();

        if (keyCode == InputConstants.KEY_DELETE) {
            if (this.deleteSelectedEntryWithoutConfirmation()) {
                return true;
            }
        }

        if (hasControlDown() && "z".equals(keyName)) {
            if (this.undo()) {
                return true;
            }
        }

        if (hasControlDown() && "y".equals(keyName)) {
            if (this.redo()) {
                return true;
            }
        }

        if (!contextMenuActive && !hasControlDown() && (keyCode == GLFW.GLFW_KEY_A)) {
            this.onAddRequirement();
            return true;
        }

        if (!contextMenuActive && !hasControlDown() && this.allowGroupManagement && (keyCode == GLFW.GLFW_KEY_G)) {
            this.onAddGroup();
            return true;
        }

        if ((keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
            if (contextMenuActive) {
                ContextMenuHandler.INSTANCE.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
            this.selectAdjacentRequirementsEntry(keyCode == InputConstants.KEY_DOWN);
            return true;
        }

        if (!contextMenuActive && ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER))) {
            RequirementInstance selectedInstance = this.getSelectedInstance();
            RequirementGroup selectedGroup = this.getSelectedGroup();
            if ((selectedInstance != null) || (selectedGroup != null)) {
                this.onEdit(selectedInstance, selectedGroup);
                return true;
            }
        }

        if (hasControlDown() && (keyCode == GLFW.GLFW_KEY_S)) {
            this.triggerDoneAction();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void triggerDoneAction() {
        this.closeRightClickContextMenu();
        this.callback.accept(this.container);
        this.closeWindow();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean contextMenuInteracting = this.isUserNavigatingInRightClickContextMenu();

        if ((button == 0) && !contextMenuInteracting && (this.rightClickContextMenu != null)) {
            this.rightClickContextMenu.closeMenu();
            this.clearContextMenuTarget();
        }

        if (!contextMenuInteracting && (button == 0) && this.isInsideRequirementsScrollArea((int)mouseX, (int)mouseY)) {
            ScrollAreaEntry targetEntry = this.getRequirementsEntryAt(mouseX, mouseY);
            boolean isEditableEntry = (targetEntry instanceof RequirementInstanceEntry) || (targetEntry instanceof RequirementGroupEntry);
            boolean isDoubleClick = false;
            if (isEditableEntry) {
                long now = System.currentTimeMillis();
                isDoubleClick = (targetEntry == this.lastLeftClickedRequirementsEntry)
                        && ((now - this.lastLeftRequirementsEntryClickTimeMs) <= ENTRY_DOUBLE_CLICK_TIME_MS);
                if (isDoubleClick) {
                    this.resetRequirementsEntryDoubleClickState();
                } else {
                    this.lastLeftClickedRequirementsEntry = targetEntry;
                    this.lastLeftRequirementsEntryClickTimeMs = now;
                }
            } else {
                this.resetRequirementsEntryDoubleClickState();
            }

            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (isDoubleClick) {
                RequirementInstance selectedInstance = (targetEntry instanceof RequirementInstanceEntry instanceEntry) ? instanceEntry.instance : null;
                RequirementGroup selectedGroup = (targetEntry instanceof RequirementGroupEntry groupEntry) ? groupEntry.group : null;
                this.onEdit(selectedInstance, selectedGroup);
                return true;
            }
            return handled;
        }

        if (!contextMenuInteracting && (button == 1) && this.isInsideRequirementsScrollArea((int)mouseX, (int)mouseY)) {
            ScrollAreaEntry targetEntry = this.getRequirementsEntryAt(mouseX, mouseY);
            this.setContextMenuTarget(targetEntry);
            if (targetEntry != null) {
                targetEntry.setSelected(true);
            }
            this.openRightClickContextMenuAtMouse(false);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void resetRequirementsEntryDoubleClickState() {
        this.lastLeftClickedRequirementsEntry = null;
        this.lastLeftRequirementsEntryClickTimeMs = 0L;
    }

    protected boolean isUserNavigatingInRightClickContextMenu() {
        return (this.rightClickContextMenu != null) && this.rightClickContextMenu.isUserNavigatingInMenu();
    }

    protected boolean isInsideRequirementsScrollArea(int mouseX, int mouseY) {
        return UIBase.isXYInArea(mouseX, mouseY, this.requirementsScrollArea.getXWithBorder(), this.requirementsScrollArea.getYWithBorder(), this.requirementsScrollArea.getWidthWithBorder(), this.requirementsScrollArea.getHeightWithBorder());
    }

    @Nullable
    protected ScrollAreaEntry getRequirementsEntryAt(double mouseX, double mouseY) {
        if (!this.requirementsScrollArea.isMouseOverInnerArea(mouseX, mouseY)) {
            return null;
        }
        for (ScrollAreaEntry entry : this.requirementsScrollArea.getEntries()) {
            if (UIBase.isXYInArea(mouseX, mouseY, entry.getX(), entry.getY(), this.requirementsScrollArea.getInnerWidth(), entry.getHeight())) {
                return entry;
            }
        }
        return null;
    }

    protected void setContextMenuTarget(@Nullable ScrollAreaEntry targetEntry) {
        this.clearContextMenuTarget();
        if (targetEntry instanceof RequirementGroupEntry groupEntry) {
            this.contextMenuTargetGroupIdentifier = groupEntry.group.identifier;
            return;
        }
        if (targetEntry instanceof RequirementInstanceEntry instanceEntry) {
            this.contextMenuTargetInstanceIdentifier = instanceEntry.instance.instanceIdentifier;
        }
    }

    protected void clearContextMenuTarget() {
        this.contextMenuTargetGroupIdentifier = null;
        this.contextMenuTargetInstanceIdentifier = null;
    }

    protected void closeRightClickContextMenu() {
        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.clearContextMenuTarget();
    }

    protected boolean hasContextMenuTarget() {
        return (this.getContextMenuTargetInstance() != null) || (this.getContextMenuTargetGroup() != null);
    }

    protected boolean hasStoredRightClickContextMenuPosition() {
        return !Float.isNaN(this.rightClickContextMenuLastOpenX) && !Float.isNaN(this.rightClickContextMenuLastOpenY);
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
    protected Component getContextMenuEditLabel() {
        if (this.getContextMenuTargetInstance() != null) {
            return Component.translatable("fancymenu.requirements.screens.edit_requirement");
        }
        return Component.translatable("fancymenu.requirements.screens.manage_screen.edit.generic");
    }

    @NotNull
    protected Component getContextMenuCycleGroupModeLabel() {
        RequirementGroup selectedGroup = this.getContextMenuTargetGroup();
        if (selectedGroup == null) {
            return Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.cycle_mode.generic");
        }
        MutableComponent currentMode = Component.translatable((selectedGroup.mode == RequirementGroup.GroupMode.AND)
                ? "fancymenu.requirements.screens.manage_screen.group_mode.and"
                : "fancymenu.requirements.screens.manage_screen.group_mode.or")
                .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
        return Component.translatable("fancymenu.requirements.screens.manage_screen.group_settings.cycle_mode", currentMode);
    }

    @NotNull
    protected Component getContextMenuRemoveLabel() {
        if (this.getContextMenuTargetInstance() != null) {
            return Component.translatable("fancymenu.requirements.screens.remove_requirement");
        }
        if (this.getContextMenuTargetGroup() != null) {
            return Component.translatable("fancymenu.requirements.screens.remove_group");
        }
        return Component.translatable("fancymenu.requirements.screens.manage_screen.remove.generic");
    }

    @Nullable
    protected RequirementInstance getContextMenuTargetInstance() {
        if (this.contextMenuTargetInstanceIdentifier == null) {
            return null;
        }
        for (RequirementInstance instance : this.container.getInstances()) {
            if (this.contextMenuTargetInstanceIdentifier.equals(instance.instanceIdentifier)) {
                return instance;
            }
        }
        return null;
    }

    @Nullable
    protected RequirementGroup getContextMenuTargetGroup() {
        if (this.contextMenuTargetGroupIdentifier == null) {
            return null;
        }
        for (RequirementGroup group : this.container.getGroups()) {
            if (this.contextMenuTargetGroupIdentifier.equals(group.identifier)) {
                return group;
            }
        }
        return null;
    }

    @Nullable
    protected RequirementInstance getSelectedInstance() {
        ScrollAreaEntry e = this.requirementsScrollArea.getFocusedEntry();
        if (e instanceof RequirementInstanceEntry) {
            return ((RequirementInstanceEntry)e).instance;
        }
        return null;
    }

    protected boolean isInstanceSelected() {
        return this.getSelectedInstance() != null;
    }

    @Nullable
    protected RequirementGroup getSelectedGroup() {
        ScrollAreaEntry e = this.requirementsScrollArea.getFocusedEntry();
        if (e instanceof RequirementGroupEntry) {
            return ((RequirementGroupEntry)e).group;
        }
        return null;
    }

    protected boolean isGroupSelected() {
        return this.getSelectedGroup() != null;
    }

    protected void updateRequirementsScrollArea() {

        this.requirementsScrollArea.clearEntries();

        if (this.allowGroupManagement) {
            for (RequirementGroup g : this.container.getGroups()) {
                RequirementGroupEntry e = new RequirementGroupEntry(this.requirementsScrollArea, g);
                this.requirementsScrollArea.addEntry(e);
            }
        }

        for (RequirementInstance i : this.container.getInstances()) {
            RequirementInstanceEntry e = new RequirementInstanceEntry(this.requirementsScrollArea, i, 14);
            this.requirementsScrollArea.addEntry(e);
        }

    }

    private boolean deleteSelectedEntryWithoutConfirmation() {
        RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
        boolean changed = false;
        if (this.isInstanceSelected()) {
            RequirementInstance selectedInstance = this.getSelectedInstance();
            if (selectedInstance != null) {
                changed = this.container.removeInstance(selectedInstance);
            }
        } else if (this.isGroupSelected()) {
            RequirementGroup selectedGroup = this.getSelectedGroup();
            if (selectedGroup != null) {
                changed = this.container.removeGroup(selectedGroup);
            }
        }
        if (changed) {
            this.updateRequirementsScrollArea();
            this.createUndoPointIfChanged(beforeSnapshot);
        }
        return changed;
    }

    protected @NotNull RequirementContainer buildGroupRequirementsContainer(@NotNull RequirementGroup group) {
        RequirementContainer groupRequirements = new RequirementContainer();
        group.getValuePlaceholders().forEach(groupRequirements::addValuePlaceholder);
        for (RequirementInstance instance : group.getInstances()) {
            RequirementInstance instanceCopy = instance.copy(false);
            instanceCopy.parent = groupRequirements;
            instanceCopy.group = null;
            groupRequirements.addInstance(instanceCopy);
        }
        return groupRequirements;
    }

    protected void applyGroupRequirementsFromContainer(@NotNull RequirementGroup group, @NotNull RequirementContainer editedContainer) {
        for (RequirementInstance instance : group.getInstances()) {
            group.removeInstance(instance);
        }
        for (RequirementInstance editedInstance : editedContainer.getInstances()) {
            RequirementInstance instanceCopy = editedInstance.copy(false);
            instanceCopy.parent = this.container;
            group.addInstance(instanceCopy);
        }
    }

    protected boolean isGroupIdentifierValidForRename(@NotNull RequirementGroup selectedGroup, @Nullable String newIdentifier) {
        if ((newIdentifier == null) || newIdentifier.replace(" ", "").isEmpty()) {
            return false;
        }
        RequirementGroup existingGroup = this.container.getGroup(newIdentifier);
        return (existingGroup == null) || (existingGroup == selectedGroup);
    }

    protected @NotNull String generateUniqueGroupIdentifier() {
        int index = this.container.getGroups().size() + 1;
        String identifier = "group_" + index;
        while (this.container.groupExists(identifier)) {
            index++;
            identifier = "group_" + index;
        }
        return identifier;
    }

    private boolean selectAdjacentRequirementsEntry(boolean moveDown) {
        java.util.List<ScrollAreaEntry> entries = this.requirementsScrollArea.getEntries();
        if (entries.isEmpty()) {
            return false;
        }
        ScrollAreaEntry selected = this.requirementsScrollArea.getFocusedEntry();
        int targetIndex;
        if (selected == null) {
            targetIndex = moveDown ? 0 : entries.size() - 1;
        } else {
            int currentIndex = entries.indexOf(selected);
            if (currentIndex == -1) {
                targetIndex = moveDown ? 0 : entries.size() - 1;
            } else {
                targetIndex = currentIndex + (moveDown ? 1 : -1);
                if (targetIndex < 0) {
                    targetIndex = entries.size() - 1;
                } else if (targetIndex >= entries.size()) {
                    targetIndex = 0;
                }
            }
        }
        ScrollAreaEntry target = entries.get(targetIndex);
        target.setSelected(true);
        this.scrollEntryIntoView(target);
        return true;
    }

    private void scrollEntryIntoView(@NotNull ScrollAreaEntry entry) {
        java.util.List<ScrollAreaEntry> entries = this.requirementsScrollArea.getEntries();
        int totalHeight = 0;
        for (ScrollAreaEntry e : entries) {
            totalHeight += (int)e.getHeight();
        }
        int visibleHeight = (int)this.requirementsScrollArea.getInnerHeight();
        if (totalHeight <= visibleHeight) {
            return;
        }
        int offset = 0;
        for (ScrollAreaEntry e : entries) {
            if (e == entry) {
                break;
            }
            offset += (int)e.getHeight();
        }
        int entryCenter = offset + ((int)entry.getHeight() / 2);
        int target = Math.max(0, entryCenter - (visibleHeight / 2));
        int maxScroll = Math.max(1, totalHeight - visibleHeight);
        float scroll = Math.min(1.0F, (float)target / (float)maxScroll);
        this.requirementsScrollArea.verticalScrollBar.setScroll(scroll);
        this.requirementsScrollArea.updateEntries(null);
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
        RequirementsSnapshot snapshot = this.undoHistory.pop();
        this.redoHistory.push(this.captureCurrentState());
        this.trimHistory(this.redoHistory);
        this.applySnapshot(snapshot);
        return true;
    }

    private boolean redo() {
        if (!this.canRedo()) {
            return false;
        }
        RequirementsSnapshot snapshot = this.redoHistory.pop();
        this.undoHistory.push(this.captureCurrentState());
        this.trimHistory(this.undoHistory);
        this.applySnapshot(snapshot);
        return true;
    }

    private void createUndoPointIfChanged(@NotNull RequirementsSnapshot previousState) {
        if (this.container.equals(previousState.container())) {
            return;
        }
        this.undoHistory.push(previousState);
        this.trimHistory(this.undoHistory);
        this.redoHistory.clear();
    }

    private void trimHistory(@NotNull Deque<RequirementsSnapshot> history) {
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    private @NotNull RequirementsSnapshot captureCurrentState() {
        String selectedGroupIdentifier = null;
        String selectedInstanceIdentifier = null;
        ScrollAreaEntry selectedEntry = this.requirementsScrollArea.getFocusedEntry();
        if (selectedEntry instanceof RequirementGroupEntry groupEntry) {
            selectedGroupIdentifier = groupEntry.group.identifier;
        } else if (selectedEntry instanceof RequirementInstanceEntry instanceEntry) {
            selectedInstanceIdentifier = instanceEntry.instance.instanceIdentifier;
        }
        return new RequirementsSnapshot(
                this.container.copy(false),
                this.requirementsScrollArea.verticalScrollBar.getScroll(),
                this.requirementsScrollArea.horizontalScrollBar.getScroll(),
                selectedGroupIdentifier,
                selectedInstanceIdentifier
        );
    }

    private void applySnapshot(@NotNull RequirementsSnapshot snapshot) {
        this.container = snapshot.container().copy(false);
        this.updateRequirementsScrollArea();
        this.requirementsScrollArea.verticalScrollBar.setScroll(Mth.clamp(snapshot.verticalScroll(), 0.0F, 1.0F));
        this.requirementsScrollArea.horizontalScrollBar.setScroll(Mth.clamp(snapshot.horizontalScroll(), 0.0F, 1.0F));
        this.requirementsScrollArea.updateEntries(null);
        this.restoreSelection(snapshot.selectedGroupIdentifier(), snapshot.selectedInstanceIdentifier());
    }

    private void restoreSelection(@Nullable String selectedGroupIdentifier, @Nullable String selectedInstanceIdentifier) {
        for (ScrollAreaEntry entry : this.requirementsScrollArea.getEntries()) {
            if ((selectedGroupIdentifier != null) && (entry instanceof RequirementGroupEntry groupEntry) && selectedGroupIdentifier.equals(groupEntry.group.identifier)) {
                entry.setSelected(true);
                return;
            }
            if ((selectedInstanceIdentifier != null) && (entry instanceof RequirementInstanceEntry instanceEntry) && selectedInstanceIdentifier.equals(instanceEntry.instance.instanceIdentifier)) {
                entry.setSelected(true);
                return;
            }
        }
    }

    private void openChildWindow(@NotNull Function<PiPWindow, PiPWindow> opener) {
        this.openChildWindow(opener, false);
    }

    private void openChildWindow(@NotNull Function<PiPWindow, PiPWindow> opener, boolean syncWindowSizeBothWays) {
        PiPWindow parentWindow = this.getWindow();
        PiPWindow childWindow = opener.apply(parentWindow);
        if (parentWindow == null || childWindow == null) {
            return;
        }
        childWindow.setPosition(parentWindow.getX(), parentWindow.getY());
        if (syncWindowSizeBothWays) {
            this.syncWindowSize(parentWindow, childWindow);
        }
        parentWindow.setVisible(false);
        childWindow.addCloseCallback(() -> {
            if (syncWindowSizeBothWays) {
                this.syncWindowSize(childWindow, parentWindow);
            }
            this.restoreAndFocusParentWindow(parentWindow);
        });
    }

    private void restoreAndFocusParentWindow(@NotNull PiPWindow parentWindow) {
        parentWindow.setVisible(true);
        this.focusParentWindow(parentWindow);
        MainThreadTaskExecutor.executeInMainThread(() ->
                        MainThreadTaskExecutor.executeInMainThread(() -> this.focusParentWindow(parentWindow),
                                MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK),
                MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    private void focusParentWindow(@NotNull PiPWindow parentWindow) {
        if (PiPWindowHandler.INSTANCE.getOpenWindows().contains(parentWindow)) {
            PiPWindowHandler.INSTANCE.bringToFront(parentWindow);
        }
    }

    private void syncWindowSize(@NotNull PiPWindow sourceWindow, @NotNull PiPWindow targetWindow) {
        if (sourceWindow.isMaximized()) {
            targetWindow.setMaximized(true);
            return;
        }
        targetWindow.setMaximized(false);
        targetWindow.setSize(
                this.convertScaledWindowSizeToRaw(targetWindow, sourceWindow.getWidth()),
                this.convertScaledWindowSizeToRaw(targetWindow, sourceWindow.getHeight())
        );
    }

    private int convertScaledWindowSizeToRaw(@NotNull PiPWindow targetWindow, int scaledSize) {
        if (!targetWindow.isSizeScaledToGuiScale()) {
            return scaledSize;
        }
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        if (guiScale <= 1.0D) {
            return scaledSize;
        }
        return Math.max(1, (int)Math.round((double)scaledSize * guiScale));
    }

    private record RequirementsSnapshot(@NotNull RequirementContainer container, float verticalScroll, float horizontalScroll, @Nullable String selectedGroupIdentifier, @Nullable String selectedInstanceIdentifier) {
    }

    public static class RequirementGroupEntry extends TextListScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public RequirementGroup group;

        public RequirementGroupEntry(ScrollArea parent, RequirementGroup group) {
            super(parent, Component.literal(group.identifier + " (" + I18n.get("fancymenu.requirements.screens.manage_screen.group.info", "" + group.getInstances().size()) + ")"), UIBase.getUITheme().bullet_list_dot_color_3, (entry) -> {});
            this.group = group;
            this.setHeight(this.getHeight() + (HEADER_FOOTER_HEIGHT * 2));
            this.setTextBaseColor(UIBase.shouldBlur()
                    ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                    : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt());
        }

    }

    public static class RequirementInstanceEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public RequirementInstance instance;
        public final int lineHeight;

        private final MutableComponent displayNameComponent;
        private final MutableComponent modeComponent;
        private final MutableComponent valueComponent;

        public RequirementInstanceEntry(ScrollArea parent, RequirementInstance instance, int lineHeight) {

            super(parent, 100, 30);
            this.instance = instance;
            this.lineHeight = lineHeight;

            this.displayNameComponent = this.instance.requirement.getDisplayName().copy();
            String modeString = (this.instance.mode == RequirementInstance.RequirementMode.IF) ? I18n.get("fancymenu.requirements.screens.requirement.info.mode.normal") : I18n.get("fancymenu.requirements.screens.requirement.info.mode.opposite");
            this.modeComponent = Component.literal(I18n.get("fancymenu.requirements.screens.requirement.info.mode") + " " + modeString);
            String valueString = (this.instance.value != null) ? this.instance.value : I18n.get("fancymenu.requirements.screens.requirement.info.value.none");
            this.valueComponent = Component.literal(I18n.get("fancymenu.requirements.screens.requirement.info.value") + " " + valueString);

            this.setWidth(this.calculateWidth());
            this.setHeight((lineHeight * 3) + (HEADER_FOOTER_HEIGHT * 2));

        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            int baseX = (int)this.getX();
            int baseY = (int)this.getY();
            int centerYLine1 = baseY + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = baseY + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);
            int centerYLine3 = baseY + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 5);
            float textHeight = UIBase.getUITextHeightNormal();

            RenderSystem.enableBlend();
            int labelColor = UIBase.shouldBlur()
                    ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                    : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();

            renderListingDot(graphics, baseX + 5, centerYLine1 - 2, UIBase.getUITheme().bullet_list_dot_color_2.getColorInt());
            UIBase.renderText(graphics, this.displayNameComponent, (baseX + 5 + 4 + 3), (centerYLine1 - (textHeight / 2f)), labelColor);

            renderListingDot(graphics, baseX + 5 + 4 + 3, centerYLine2 - 2, UIBase.getUITheme().bullet_list_dot_color_1.getColorInt());
            UIBase.renderText(graphics, this.modeComponent, (baseX + 5 + 4 + 3 + 4 + 3), (centerYLine2 - (textHeight / 2f)), labelColor);

            renderListingDot(graphics, baseX + 5 + 4 + 3, centerYLine3 - 2, UIBase.getUITheme().bullet_list_dot_color_1.getColorInt());
            UIBase.renderText(graphics, this.valueComponent, (baseX + 5 + 4 + 3 + 4 + 3), (centerYLine3 - (textHeight / 2f)), labelColor);

        }

        private int calculateWidth() {
            int w = (int)(5 + 4 + 3 + UIBase.getUITextWidthNormal(this.displayNameComponent) + 5);
            int w2 = (int)(5 + 4 + 3 + 4 + 3 + UIBase.getUITextWidthNormal(this.modeComponent) + 5);
            int w3 = (int)(5 + 4 + 3 + 4 + 3 + UIBase.getUITextWidthNormal(this.valueComponent) + 5);
            if (w2 > w) {
                w = w2;
            }
            if (w3 > w) {
                w = w3;
            }
            return w;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {}

    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageRequirementsWindowBody screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageRequirementsWindowBody screen) {
        return openInWindow(screen, null);
    }

}
