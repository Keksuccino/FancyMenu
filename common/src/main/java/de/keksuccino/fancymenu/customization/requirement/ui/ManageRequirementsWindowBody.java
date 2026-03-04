package de.keksuccino.fancymenu.customization.requirement.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    protected RequirementContainer container;
    protected Consumer<RequirementContainer> callback;

    protected ScrollArea requirementsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;
    protected ContextMenu rightClickContextMenu;
    @Nullable
    protected String contextMenuTargetGroupIdentifier;
    @Nullable
    protected String contextMenuTargetInstanceIdentifier;
    private final Deque<RequirementsSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<RequirementsSnapshot> redoHistory = new ArrayDeque<>();

    public ManageRequirementsWindowBody(@NotNull RequirementContainer container, @NotNull Consumer<RequirementContainer> callback) {
        super(Component.literal(I18n.get("fancymenu.requirements.screens.manage_screen.manage")));
        this.container = container;
        this.callback = callback;
        this.updateRequirementsScrollArea();
    }

    @Override
    protected void init() {
        boolean blur = UIBase.shouldBlur();
        this.requirementsScrollArea.setSetupForBlurInterface(blur);
        this.updateRightClickContextMenu();

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.cancel"), (button) -> {
            this.closeRightClickContextMenu();
            this.callback.accept(null);
            this.closeWindow();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, blur);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.done"), (button) -> this.triggerDoneAction());
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, blur);

        this.addWidget(this.requirementsScrollArea);

    }

    protected void updateRightClickContextMenu() {
        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
        }
        this.rightClickContextMenu = new ContextMenu();

        this.rightClickContextMenu.addClickableEntry("add_requirement", Component.translatable("fancymenu.requirements.screens.add_requirement"), (menu, entry) -> {
                    menu.closeMenu();
                    this.onAddRequirement();
                }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.add_requirement.desc")))
                .setIcon(MaterialIcons.ADD);

        this.rightClickContextMenu.addClickableEntry("add_group", Component.translatable("fancymenu.requirements.screens.add_group"), (menu, entry) -> {
                    menu.closeMenu();
                    this.onAddGroup();
                }).setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.add_group.desc")))
                .setIcon(MaterialIcons.ADD);

        this.rightClickContextMenu.addSeparatorEntry("separator_after_add");

        this.rightClickContextMenu.addClickableEntry("edit", Component.translatable("fancymenu.requirements.screens.manage_screen.edit.generic"), (menu, entry) -> {
                    RequirementInstance selectedInstance = this.getContextMenuTargetInstance();
                    RequirementGroup selectedGroup = this.getContextMenuTargetGroup();
                    if ((selectedInstance == null) && (selectedGroup == null)) {
                        return;
                    }
                    menu.closeMenu();
                    this.onEdit(selectedInstance, selectedGroup);
                }).setLabelSupplier((menu, entry) -> this.getContextMenuEditLabel())
                .addIsActiveSupplier((menu, entry) -> this.hasContextMenuTarget())
                .setTooltipSupplier((menu, entry) -> this.hasContextMenuTarget()
                        ? UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.edit.desc"))
                        : UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.no_entry_selected")))
                .setIcon(MaterialIcons.EDIT);

        this.rightClickContextMenu.addClickableEntry("remove", Component.translatable("fancymenu.requirements.screens.manage_screen.remove.generic"), (menu, entry) -> {
                    RequirementInstance selectedInstance = this.getContextMenuTargetInstance();
                    RequirementGroup selectedGroup = this.getContextMenuTargetGroup();
                    if ((selectedInstance == null) && (selectedGroup == null)) {
                        return;
                    }
                    menu.closeMenu();
                    this.onRemove(selectedInstance, selectedGroup);
                }).setLabelSupplier((menu, entry) -> this.getContextMenuRemoveLabel())
                .addIsActiveSupplier((menu, entry) -> this.hasContextMenuTarget())
                .setTooltipSupplier((menu, entry) -> this.hasContextMenuTarget()
                        ? UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.remove.desc"))
                        : UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.no_entry_selected")))
                .setIcon(MaterialIcons.DELETE);
    }

    protected void onAddRequirement() {
        RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
        BuildRequirementScreen screen = new BuildRequirementScreen(this.container, null, (call) -> {
            if (call != null) {
                this.container.addInstance(call);
                this.updateRequirementsScrollArea();
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        });
        this.openChildWindow(parentWindow -> BuildRequirementScreen.openInWindow(screen, parentWindow));
    }

    protected void onAddGroup() {
        RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
        BuildRequirementGroupScreen screen = new BuildRequirementGroupScreen(this.container, null, (call) -> {
            if (call != null) {
                this.container.addGroup(call);
                this.updateRequirementsScrollArea();
                this.createUndoPointIfChanged(beforeSnapshot);
            }
        });
        this.openChildWindow(parentWindow -> BuildRequirementGroupScreen.openInWindow(screen, parentWindow));
    }

    protected void onEdit(@Nullable RequirementInstance selectedInstance, @Nullable RequirementGroup selectedGroup) {
        if (selectedInstance != null) {
            RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
            BuildRequirementScreen requirementScreen = new BuildRequirementScreen(this.container, selectedInstance, (call) -> {
                if (call != null) {
                    this.updateRequirementsScrollArea();
                    this.createUndoPointIfChanged(beforeSnapshot);
                }
            });
            this.openChildWindow(parentWindow -> BuildRequirementScreen.openInWindow(requirementScreen, parentWindow));
            return;
        }
        if (selectedGroup != null) {
            RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
            BuildRequirementGroupScreen groupScreen = new BuildRequirementGroupScreen(this.container, selectedGroup, (call) -> {
                if (call != null) {
                    this.updateRequirementsScrollArea();
                    this.createUndoPointIfChanged(beforeSnapshot);
                }
            });
            this.openChildWindow(parentWindow -> BuildRequirementGroupScreen.openInWindow(groupScreen, parentWindow));
        }
    }

    protected void onRemove(@Nullable RequirementInstance selectedInstance, @Nullable RequirementGroup selectedGroup) {
        if (selectedInstance != null) {
            RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
            Dialogs.openMessageWithCallback(Component.translatable("fancymenu.requirements.screens.remove_requirement.confirm"), MessageDialogStyle.WARNING, call -> {
                if (call) {
                    this.container.removeInstance(selectedInstance);
                    this.updateRequirementsScrollArea();
                    this.createUndoPointIfChanged(beforeSnapshot);
                }
            });
            return;
        }
        if (selectedGroup != null) {
            RequirementsSnapshot beforeSnapshot = this.captureCurrentState();
            Dialogs.openMessageWithCallback(Component.translatable("fancymenu.requirements.screens.remove_group.confirm"), MessageDialogStyle.WARNING, call -> {
                if (call) {
                    this.container.removeGroup(selectedGroup);
                    this.updateRequirementsScrollArea();
                    this.createUndoPointIfChanged(beforeSnapshot);
                }
            });
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
        float labelY = scrollAreaTop - UIBase.getUITextHeightNormal() - 3.0F;
        UIBase.renderText(graphics, I18n.get("fancymenu.requirements.screens.manage_screen.requirements_and_groups"), 20, labelY, textColor);

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
            Component hint = Component.translatable("fancymenu.requirements.screens.manage_screen.empty_hint");
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

        if (!contextMenuInteracting && (button == 1) && this.isInsideRequirementsScrollArea((int)mouseX, (int)mouseY)) {
            ScrollAreaEntry targetEntry = this.getRequirementsEntryAt(mouseX, mouseY);
            this.setContextMenuTarget(targetEntry);
            if (targetEntry != null) {
                targetEntry.setSelected(true);
            }
            if (this.rightClickContextMenu != null) {
                ContextMenuHandler.INSTANCE.setAndOpenAtMouse(this.rightClickContextMenu);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
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

    @NotNull
    protected Component getContextMenuEditLabel() {
        if (this.getContextMenuTargetInstance() != null) {
            return Component.translatable("fancymenu.requirements.screens.edit_requirement");
        }
        if (this.getContextMenuTargetGroup() != null) {
            return Component.translatable("fancymenu.requirements.screens.edit_group");
        }
        return Component.translatable("fancymenu.requirements.screens.manage_screen.edit.generic");
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

        for (RequirementGroup g : this.container.getGroups()) {
            RequirementGroupEntry e = new RequirementGroupEntry(this.requirementsScrollArea, g);
            this.requirementsScrollArea.addEntry(e);
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
        PiPWindow parentWindow = this.getWindow();
        PiPWindow childWindow = opener.apply(parentWindow);
        if (parentWindow == null || childWindow == null) {
            return;
        }
        childWindow.setPosition(parentWindow.getX(), parentWindow.getY());
        parentWindow.setVisible(false);
        childWindow.addCloseCallback(() -> parentWindow.setVisible(true));
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
        public Font font = Minecraft.getInstance().font;

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

            RenderSystem.enableBlend();
            int labelColor = UIBase.shouldBlur()
                    ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                    : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();

            renderListingDot(graphics, baseX + 5, centerYLine1 - 2, UIBase.getUITheme().bullet_list_dot_color_2.getColorInt());
            graphics.drawString(this.font, this.displayNameComponent, (baseX + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), labelColor, false);

            renderListingDot(graphics, baseX + 5 + 4 + 3, centerYLine2 - 2, UIBase.getUITheme().bullet_list_dot_color_1.getColorInt());
            graphics.drawString(this.font, this.modeComponent, (baseX + 5 + 4 + 3 + 4 + 3), (centerYLine2 - (this.font.lineHeight / 2)), labelColor, false);

            renderListingDot(graphics, baseX + 5 + 4 + 3, centerYLine3 - 2, UIBase.getUITheme().bullet_list_dot_color_1.getColorInt());
            graphics.drawString(this.font, this.valueComponent, (baseX + 5 + 4 + 3 + 4 + 3), (centerYLine3 - (this.font.lineHeight / 2)), labelColor, false);

        }

        private int calculateWidth() {
            int w = 5 + 4 + 3 + this.font.width(this.displayNameComponent) + 5;
            int w2 = 5 + 4 + 3 + 4 + 3 + this.font.width(this.modeComponent) + 5;
            int w3 = 5 + 4 + 3 + 4 + 3 + this.font.width(this.valueComponent) + 5;
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
