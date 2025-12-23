package de.keksuccino.fancymenu.customization.action.ui;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.ActionFavoritesManager;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu.ContextMenuEntry;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu.SubMenuContextMenuEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ActionScriptEditorScreen extends Screen {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger();

    protected GenericExecutableBlock executableBlock;
    protected Consumer<GenericExecutableBlock> callback;
    protected ScrollArea scriptEntriesScrollArea = new ScrollArea(0, 0, 0, 0).setAllowScrollWheelSupplier(() -> !this.isUserNavigatingInRightClickContextMenu());
    protected ContextMenu rightClickContextMenu;
    protected float rightClickContextMenuLastOpenX = Float.NaN;
    protected float rightClickContextMenuLastOpenY = Float.NaN;
    @Nullable
    protected Executable contextMenuTargetExecutable = null;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;
    @Nullable
    protected ExecutableEntry renderTickDragHoveredEntry = null;
    @Nullable
    protected ExecutableEntry renderTickDraggedEntry = null;
    private final ExecutableEntry BEFORE_FIRST = new ExecutableEntry(this.scriptEntriesScrollArea, new GenericExecutableBlock(), 1, 0);
    private final ExecutableEntry AFTER_LAST = new ExecutableEntry(this.scriptEntriesScrollArea, new GenericExecutableBlock(), 1, 0);
    @Nullable
    protected Executable pendingSelectionExecutable = null;
    protected boolean pendingSelectionKeepViewAnchor = false;
    protected boolean skipNextContextMenuSelection = false;
    protected boolean contextMenuSelectionOverrideActive = false;
    protected static final int LEFT_MARGIN = 20;
    protected static final int RIGHT_MARGIN = 20;
    protected static final int MINIMAP_WIDTH = 64;
    protected static final int MINIMAP_GAP = 8;
    protected static final int MINIMAP_PADDING = 4;
    protected static final int ACTION_BUTTON_GAP = 5;
    protected static final int MINIMAP_INDENT_STEP = 4;
    protected static final int CHAIN_BAR_WIDTH = 3;
    protected static final int CHAIN_BAR_OFFSET = 2;
    protected static final int INLINE_EDIT_RIGHT_MARGIN = 5;
    protected static final float MINIMAP_TOOLTIP_SCALE = 0.5F;
    protected static final int MINIMAP_TOOLTIP_PADDING = 4;
    protected static final int MINIMAP_TOOLTIP_OFFSET = 12;
    protected static final long VALUE_DOUBLE_CLICK_TIME_MS = 500L;
    protected static final long NAME_DOUBLE_CLICK_TIME_MS = 500L;
    @Nullable
    protected ExecutableEntry hoveredEntry = null;
    @Nullable
    protected ExecutableEntry minimapHoveredEntry = null;
    @Nullable
    protected ExecutableEntry selectedEntry = null;
    @Nullable
    protected ExtendedEditBox inlineValueEditBox = null;
    @Nullable
    protected ExecutableEntry inlineValueEntry = null;
    @Nullable
    protected String inlineValueOriginal = null;
    @Nullable
    protected ExtendedEditBox inlineNameEditBox = null;
    @Nullable
    protected ExecutableEntry inlineNameEntry = null;
    @Nullable
    protected String inlineNameOriginal = null;
    protected List<ExecutableEntry> hoveredStatementChainEntries = Collections.emptyList();
    protected List<ExecutableEntry> hoveredPrimaryChainEntries = Collections.emptyList();
    protected List<ExecutableEntry> minimapHoveredStatementChainEntries = Collections.emptyList();
    protected List<ExecutableEntry> minimapHoveredPrimaryChainEntries = Collections.emptyList();
    protected List<ExecutableEntry> selectedStatementChainEntries = Collections.emptyList();
    protected final List<MinimapEntrySegment> minimapSegments = new ArrayList<>();
    protected int minimapX = 0;
    protected int minimapY = 0;
    protected int minimapHeight = 0;
    protected int minimapContentX = 0;
    protected int minimapContentY = 0;
    protected int minimapContentWidth = 0;
    protected int minimapContentHeight = 0;
    protected int minimapTotalEntriesHeight = 0;
    @Nullable
    protected ActionInstance clipboardActionInstance = null;
    private static final Identifier ILLEGAL_ACTION_ICON = Identifier.fromNamespaceAndPath("fancymenu", "textures/not_allowed.png");
    private static final AspectRatio ILLEGAL_ACTION_ICON_RATIO = new AspectRatio(32, 32);
    private static final long ILLEGAL_ACTION_VISIBLE_DURATION_MS = 1500L;
    private static final long ILLEGAL_ACTION_FADE_DURATION_MS = 300L;
    private static final float ILLEGAL_ACTION_MAX_ALPHA = 0.5F;
    protected long illegalActionIndicatorStartTime = -1L;
    private static final int HISTORY_LIMIT = 100;
    private final Deque<ScriptSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<ScriptSnapshot> redoHistory = new ArrayDeque<>();
    private boolean suppressHistoryCapture = false;
    private boolean actionsMenuRightClickConsumedByEntry = false;

    public ActionScriptEditorScreen(@NotNull GenericExecutableBlock executableBlock, @NotNull Consumer<GenericExecutableBlock> callback) {
        super(Component.translatable("fancymenu.actions.screens.manage_screen.manage"));
        this.executableBlock = executableBlock.copy(false);
        this.callback = callback;
        this.updateActionInstanceScrollArea(false);
    }

    @Override
    protected void init() {

        this.updateRightClickContextMenu(false, null);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.done"), (button) -> this.callback.accept(this.executableBlock));
        this.doneButton.setNavigatable(false);
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            Minecraft.getInstance().setScreen(
                    ConfirmationScreen.critical(callback -> {
                        if (callback) {
                            this.callback.accept(null);
                        } else {
                            Minecraft.getInstance().setScreen(this);
                        }
                    }, Component.translatable("fancymenu.actions.script_editor.cancel_warning"))
            );
        });
        this.cancelButton.setNavigatable(false);
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.actionsMenuRightClickConsumedByEntry = false;

    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        this.finishInlineNameEditing(true);
        this.finishInlineNameEditing(true);
        this.finishInlineValueEditing(true);
        this.callback.accept(null);
    }

    protected void updateRightClickContextMenu(boolean reopen, @Nullable List<String> entryPath) {

        boolean wasOpen = (this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen();
        List<String> resolvedEntryPath = null;
        List<String> currentPath = (wasOpen && (this.rightClickContextMenu != null)) ? this.findOpenContextMenuPath(this.rightClickContextMenu) : null;
        if (currentPath != null) {
            resolvedEntryPath = currentPath;
        } else if (entryPath != null) {
            resolvedEntryPath = new ArrayList<>(entryPath);
        }

        if (this.rightClickContextMenu != null) {
            this.rightClickContextMenu.closeMenu();
            this.removeWidget(this.rightClickContextMenu);
        }

        this.rightClickContextMenu = new ContextMenu();
        this.addWidget(this.rightClickContextMenu);

        this.rightClickContextMenu.addClickableEntry("edit", Component.translatable("fancymenu.actions.screens.edit_action"), (menu, entry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    ExecutableEntry target = this.getContextMenuTargetEntry();
                    menu.closeMenu();
                    this.onEdit(target);
                }).addIsActiveSupplier((menu, entry) -> this.canEditEntry(this.getContextMenuTargetEntry()))
                .setIcon(ContextMenu.IconFactory.getIcon("edit"));

        this.rightClickContextMenu.addSeparatorEntry("separator_after_edit");

        this.rightClickContextMenu.addClickableEntry("delete", Component.translatable("fancymenu.actions.screens.remove_action"), (menu, entry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    ExecutableEntry target = this.getContextMenuTargetEntry();
                    menu.closeMenu();
                    this.onRemove(target);
                }).addIsActiveSupplier((menu, entry) -> this.getContextMenuTargetEntry() != null)
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.delete"))
                .setIcon(ContextMenu.IconFactory.getIcon("delete"));

        if (reopen || wasOpen) {
            float reopenX = this.hasStoredRightClickContextMenuPosition() ? this.rightClickContextMenuLastOpenX : (float)MouseInput.getMouseX();
            float reopenY = this.hasStoredRightClickContextMenuPosition() ? this.rightClickContextMenuLastOpenY : (float)MouseInput.getMouseY();
            this.openRightClickContextMenuAt(reopenX, reopenY, resolvedEntryPath);
        }

        this.rightClickContextMenu.addSeparatorEntry("separator_after_remove");

        this.rightClickContextMenu.addClickableEntry("move_up", Component.translatable("fancymenu.actions.screens.move_action_up"), (menu, contextMenuEntry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    ExecutableEntry target = this.getContextMenuTargetEntry();
                    if (target != null) {
                        this.handleContextMenuMove(target, true);
                    }
                }).addIsActiveSupplier((menu, entry) -> this.getContextMenuTargetEntry() != null)
                .setTooltipSupplier((menu, entry) -> {
                    if (this.getContextMenuTargetEntry() == null) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.actions.screens.finish.no_action_selected"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.actions.screens.move_action_up.desc"));
                })
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.actions.script_editor.shortcuts.shift_arrow_up"))
                .setIcon(ContextMenu.IconFactory.getIcon("arrow_up"));

        this.rightClickContextMenu.addClickableEntry("move_down", Component.translatable("fancymenu.actions.screens.move_action_down"), (menu, contextMenuEntry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    ExecutableEntry target = this.getContextMenuTargetEntry();
                    if (target != null) {
                        this.handleContextMenuMove(target, false);
                    }
                }).addIsActiveSupplier((menu, entry) -> this.getContextMenuTargetEntry() != null)
                .setTooltipSupplier((menu, entry) -> {
                    if (this.getContextMenuTargetEntry() == null) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.actions.screens.finish.no_action_selected"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.actions.screens.move_action_down.desc"));
                })
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.actions.script_editor.shortcuts.shift_arrow_down"))
                .setIcon(ContextMenu.IconFactory.getIcon("arrow_down"));

        this.rightClickContextMenu.addSeparatorEntry("separator_after_reorder");

        this.rightClickContextMenu.addClickableEntry("undo", Component.translatable("fancymenu.editor.edit.undo"), (menu, entry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    this.undo();
                }).addIsActiveSupplier((menu, entry) -> this.canUndo())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.undo"))
                .setIcon(ContextMenu.IconFactory.getIcon("undo"));

        this.rightClickContextMenu.addClickableEntry("redo", Component.translatable("fancymenu.editor.edit.redo"), (menu, entry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    this.redo();
                }).addIsActiveSupplier((menu, entry) -> this.canRedo())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.redo"))
                .setIcon(ContextMenu.IconFactory.getIcon("redo"));

        this.rightClickContextMenu.addSeparatorEntry("separator_after_history");

        ContextMenu addActionSubMenu = this.buildAddActionSubMenu(null);
        boolean hasActionEntries = !addActionSubMenu.getEntries().isEmpty();
        this.rightClickContextMenu.addSubMenuEntry("add_action", Component.translatable("fancymenu.actions.screens.add_action"), addActionSubMenu)
                .addIsActiveSupplier((menu, entry) -> hasActionEntries)
                .setIcon(ContextMenu.IconFactory.getIcon("add"));

        this.rightClickContextMenu.addClickableEntry("add_if", Component.translatable("fancymenu.actions.blocks.add.if"), (menu, entry) -> {
            this.markContextMenuActionSelectionSuppressed();
            ExecutableEntry target = this.getContextMenuTargetEntry();
            menu.closeMenu();
            this.onAddIf(target);
        }).setIcon(ContextMenu.IconFactory.getIcon("add"));

        this.rightClickContextMenu.addClickableEntry("add_while", Component.translatable("fancymenu.actions.blocks.add.while"), (menu, entry) -> {
            this.markContextMenuActionSelectionSuppressed();
            ExecutableEntry target = this.getContextMenuTargetEntry();
            menu.closeMenu();
            this.onAddWhile(target);
        }).setIcon(ContextMenu.IconFactory.getIcon("add"));

        this.rightClickContextMenu.addClickableEntry("add_folder", Component.translatable("fancymenu.actions.blocks.add.folder"), (menu, entry) -> {
            this.markContextMenuActionSelectionSuppressed();
            ExecutableEntry target = this.getContextMenuTargetEntry();
            menu.closeMenu();
            this.onAddFolder(target);
        }).setIcon(ContextMenu.IconFactory.getIcon("add"));

        this.rightClickContextMenu.addSeparatorEntry("separator_after_add");

        this.rightClickContextMenu.addClickableEntry("append_else_if", Component.translatable("fancymenu.actions.blocks.add.else_if"), (menu, entry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    ExecutableEntry target = this.getContextMenuTargetEntry();
                    menu.closeMenu();
                    this.onAppendElseIf(target);
                }).addIsActiveSupplier((menu, entry) -> this.canAppendConditionalBlock(this.getContextMenuTargetEntry()))
                .setIcon(ContextMenu.IconFactory.getIcon("link"));

        this.rightClickContextMenu.addClickableEntry("append_else", Component.translatable("fancymenu.actions.blocks.add.else"), (menu, entry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    ExecutableEntry target = this.getContextMenuTargetEntry();
                    menu.closeMenu();
                    this.onAppendElse(target);
                }).addIsActiveSupplier((menu, entry) -> this.canAppendElseBlock(this.getContextMenuTargetEntry()))
                .setIcon(ContextMenu.IconFactory.getIcon("link"));

    }

    @NotNull
    protected ContextMenu buildAddActionSubMenu(@Nullable ContextMenu updateContent) {

        LayoutEditorScreen editor = LayoutEditorScreen.getCurrentInstance();

        ContextMenu subMenu = (updateContent != null) ? updateContent : new ContextMenu() {
            // This rebuilds the context menu on right-click without closing it
            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
                boolean b = super.mouseClicked(event, isDoubleClick); // Do this first, so the action entries can do their thing (toggle favorites) before the menu gets rebuilt
                if (this.isUserNavigatingInMenu() && (event.button() == 1)) {
                    this.entries.clear();
                    buildAddActionSubMenu(this);
                    return true;
                }
                return b;
            }
        };

        Map<String, Action> availableActions = new LinkedHashMap<>();
        for (Action action : ActionRegistry.getActions()) {
            if ((editor != null) && !action.shouldShowUpInEditorActionMenu(editor)) {
                continue;
            }
            availableActions.put(action.getIdentifier(), action);
        }

        ActionFavoritesManager.retainFavorites(new LinkedHashSet<>(availableActions.keySet()));
        List<String> favoriteIdentifiers = ActionFavoritesManager.getFavorites();

        List<Action> favoriteActions = new ArrayList<>();
        for (String identifier : favoriteIdentifiers) {
            Action action = availableActions.remove(identifier);
            if (action != null) {
                favoriteActions.add(action);
            }
        }

        List<Action> regularActions = new ArrayList<>(availableActions.values());
        regularActions.sort((left, right) -> {
            String leftName = left.getActionDisplayName().getString();
            String rightName = right.getActionDisplayName().getString();
            return String.CASE_INSENSITIVE_ORDER.compare(leftName, rightName);
        });

        UIColorTheme theme = UIBase.getUIColorTheme();
        MutableComponent openChooserLabel = Component.translatable("fancymenu.actions.open_action_chooser").setStyle(Style.EMPTY.withColor(theme.element_label_color_normal.getColorInt()));
        subMenu.addClickableEntry("open_action_chooser", openChooserLabel, (menu, contextMenuEntry) -> {
                    this.markContextMenuActionSelectionSuppressed();
                    ExecutableEntry selectionReference = this.getContextMenuTargetEntry();
                    this.rightClickContextMenu.closeMenu();
                    this.onOpenActionChooser(selectionReference);
                }).setIcon(ContextMenu.IconFactory.getIcon("pick"))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.actions.script_editor.shortcuts.a"))
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.open_action_chooser.desc")));

        if (!favoriteActions.isEmpty() || !regularActions.isEmpty()) {
            subMenu.addSeparatorEntry("after_open_action_chooser");
        }

        for (Action action : favoriteActions) {
            subMenu.addEntry(new FavoriteAwareActionEntry(subMenu, action));
        }

        if (!favoriteActions.isEmpty() && !regularActions.isEmpty()) {
            subMenu.addSeparatorEntry("after_favorites");
        }

        for (Action action : regularActions) {
            subMenu.addEntry(new FavoriteAwareActionEntry(subMenu, action));
        }
        return subMenu;
    }

    @NotNull
    protected MutableComponent buildActionMenuLabel(@NotNull Action action) {
        UIColorTheme theme = UIBase.getUIColorTheme();
        MutableComponent label = action.getActionDisplayName().copy().setStyle(Style.EMPTY.withColor(theme.element_label_color_normal.getColorInt()));
        if (action.isDeprecated()) {
            label = label.withStyle(Style.EMPTY.withStrikethrough(true));
            label = label.append(Component.literal(" ").setStyle(Style.EMPTY.withStrikethrough(false)));
            label = label.append(Component.translatable("fancymenu.actions.deprecated").setStyle(Style.EMPTY.withColor(theme.error_text_color.getColorInt()).withStrikethrough(false)));
        }
        return label;
    }

    @Nullable
    protected Tooltip createActionTooltip(@NotNull Action action, boolean isFavorite) {
        List<Component> lines = new ArrayList<>();
        Component[] description = action.getActionDescription();
        if ((description != null) && (description.length > 0)) {
            Collections.addAll(lines, description);
        }
        UIColorTheme theme = UIBase.getUIColorTheme();
        Style hintStyle = Style.EMPTY
                .withColor(theme.description_area_text_color.getColorInt())
                .withItalic(true);
        Component hint = Component.translatable(isFavorite ? "fancymenu.actions.favorite.remove" : "fancymenu.actions.favorite.add").setStyle(hintStyle);
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }
        lines.add(hint);
        return Tooltip.of(lines.toArray(new Component[0]));
    }

    protected void markContextMenuActionSelectionSuppressed() {
        this.skipNextContextMenuSelection = true;
    }

    protected boolean isFavorite(@NotNull Action action) {
        return ActionFavoritesManager.isFavorite(action.getIdentifier());
    }

    protected void toggleFavorite(@NotNull Action action) {
        ActionFavoritesManager.toggleFavorite(action.getIdentifier());
    }

    @Nullable
    protected ExecutableEntry getContextMenuTargetEntry() {
        if (this.contextMenuTargetExecutable == null) {
            return null;
        }
        return this.findEntryForExecutable(this.contextMenuTargetExecutable);
    }

    protected boolean hasStoredRightClickContextMenuPosition() {
        return !Float.isNaN(this.rightClickContextMenuLastOpenX) && !Float.isNaN(this.rightClickContextMenuLastOpenY);
    }

    @Nullable
    protected List<String> findOpenContextMenuPath(@NotNull ContextMenu menu) {
        for (ContextMenuEntry<?> entry : menu.getEntries()) {
            if (entry instanceof SubMenuContextMenuEntry subEntry) {
                ContextMenu subMenu = subEntry.getSubContextMenu();
                if (subMenu.isOpen()) {
                    List<String> childPath = this.findOpenContextMenuPath(subMenu);
                    List<String> path = new ArrayList<>();
                    path.add(subEntry.getIdentifier());
                    if (childPath != null) {
                        path.addAll(childPath);
                    }
                    return path;
                }
            }
        }
        return null;
    }

    protected void onEdit(@Nullable ExecutableEntry entry) {
        if (!this.canEditEntry(entry)) {
            return;
        }
        if (entry == null) {
            return;
        }
        final Executable targetExecutable = entry.executable;
        if (targetExecutable instanceof ActionInstance instance) {
            if (!instance.action.hasValue()) {
                return;
            }
            ChooseActionScreen s = new ChooseActionScreen(instance.copy(false), call -> {
                if (call != null) {
                    ExecutableEntry currentEntry = this.findEntryForExecutable(targetExecutable);
                    if (currentEntry != null) {
                        currentEntry.getParentBlock();
                        AbstractExecutableBlock parentBlock = currentEntry.getParentBlock();
                        boolean changed = (call.action != instance.action) || !Objects.equals(call.value, instance.value);
                        if (changed) {
                            this.createUndoPoint();
                            int index = parentBlock.getExecutables().indexOf(currentEntry.executable);
                            parentBlock.getExecutables().remove(currentEntry.executable);
                            if (index != -1) {
                                parentBlock.getExecutables().add(index, call);
                            } else {
                                parentBlock.getExecutables().add(call);
                            }
                            this.updateActionInstanceScrollArea(false);
                            this.focusEntryForExecutable(call, true, true);
                        }
                    }
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        } else if (targetExecutable instanceof IfExecutableBlock block) {
            ManageRequirementsScreen s = new ManageRequirementsScreen(block.condition.copy(false), container -> {
                if (container != null) {
                    ExecutableEntry currentEntry = this.findEntryForExecutable(block);
                    if ((currentEntry != null) && (currentEntry.executable instanceof IfExecutableBlock currentBlock)) {
                        if (!container.equals(currentBlock.condition)) {
                            this.createUndoPoint();
                            currentBlock.condition = container;
                            this.updateActionInstanceScrollArea(true);
                            this.focusEntryForExecutable(currentBlock, true, true);
                        }
                    }
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        } else if (targetExecutable instanceof ElseIfExecutableBlock block) {
            ManageRequirementsScreen s = new ManageRequirementsScreen(block.condition.copy(false), container -> {
                if (container != null) {
                    ExecutableEntry currentEntry = this.findEntryForExecutable(block);
                    if ((currentEntry != null) && (currentEntry.executable instanceof ElseIfExecutableBlock currentBlock)) {
                        if (!container.equals(currentBlock.condition)) {
                            this.createUndoPoint();
                            currentBlock.condition = container;
                            this.updateActionInstanceScrollArea(true);
                            this.focusEntryForExecutable(currentBlock, true, true);
                        }
                    }
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        } else if (targetExecutable instanceof WhileExecutableBlock block) {
            ManageRequirementsScreen s = new ManageRequirementsScreen(block.condition.copy(false), container -> {
                if (container != null) {
                    ExecutableEntry currentEntry = this.findEntryForExecutable(block);
                    if ((currentEntry != null) && (currentEntry.executable instanceof WhileExecutableBlock currentBlock)) {
                        if (!container.equals(currentBlock.condition)) {
                            this.createUndoPoint();
                            currentBlock.condition = container;
                            this.updateActionInstanceScrollArea(true);
                            this.focusEntryForExecutable(currentBlock, true, true);
                        }
                    }
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        }
    }

    protected void onOpenActionChooser(@Nullable ExecutableEntry selectionReference) {
        final Executable selectionExecutable = (selectionReference != null) ? selectionReference.executable : null;
        ChooseActionScreen screen = new ChooseActionScreen(null, call -> {
            if (call != null) {
                ExecutableEntry resolvedReference = (selectionExecutable != null) ? this.findEntryForExecutable(selectionExecutable) : null;
                this.finalizeActionAddition(call, resolvedReference);
            }
            Minecraft.getInstance().setScreen(this);
        });
        Minecraft.getInstance().setScreen(screen);
    }

    protected void onAddAction(@NotNull Action action, @Nullable ExecutableEntry selectionReference) {
        ExecutableEntry resolvedReference = (selectionReference != null) ? this.findEntryForExecutable(selectionReference.executable) : null;
        ActionInstance instance = new ActionInstance(action, action.hasValue() ? action.getValueExample() : null);
        this.finalizeActionAddition(instance, resolvedReference, true);
    }

    protected void finalizeActionAddition(@NotNull ActionInstance instance, @Nullable ExecutableEntry selectionReference) {
        this.finalizeActionAddition(instance, selectionReference, false);
    }

    protected void finalizeActionAddition(@NotNull ActionInstance instance, @Nullable ExecutableEntry selectionReference, boolean preserveViewAnchor) {
        this.finalizeExecutableAddition(instance, selectionReference, preserveViewAnchor);
    }

    protected void finalizeExecutableAddition(@NotNull Executable executable, @Nullable ExecutableEntry selectionReference, boolean preserveViewAnchor) {
        this.createUndoPoint();

        Executable previousExecutable = null;
        int previousEntryY = Integer.MIN_VALUE;
        if (preserveViewAnchor) {
            ExecutableEntry previouslySelected = this.getSelectedEntry();
            if (previouslySelected != null) {
                previousExecutable = previouslySelected.executable;
                previousEntryY = previouslySelected.getY();
            }
        }

        this.addExecutableRelativeToSelection(executable, selectionReference);
        this.updateActionInstanceScrollArea(false);

        boolean anchorActive = preserveViewAnchor && (previousExecutable != null) && (previousEntryY != Integer.MIN_VALUE);
        if (anchorActive) {
            ExecutableEntry previousEntry = this.findEntryForExecutable(previousExecutable);
            if (previousEntry != null) {
                this.adjustScrollToKeepEntryInPlace(previousEntryY, previousEntry);
            }
        }

        this.focusEntryForExecutable(executable, anchorActive, true);
    }

    protected void onAddFolder(@Nullable ExecutableEntry selectionReference) {
        ExecutableEntry resolvedReference = (selectionReference != null) ? this.findEntryForExecutable(selectionReference.executable) : null;
        FolderExecutableBlock block = new FolderExecutableBlock();
        this.finalizeExecutableAddition(block, resolvedReference, true);
    }

    protected void onAddIf(@Nullable ExecutableEntry selectionReference) {
        final Executable selectionExecutable = (selectionReference != null) ? selectionReference.executable : null;
        ManageRequirementsScreen s = new ManageRequirementsScreen(new LoadingRequirementContainer(), container -> {
            if (container != null) {
                ExecutableEntry resolvedReference = (selectionExecutable != null) ? this.findEntryForExecutable(selectionExecutable) : null;
                IfExecutableBlock block = new IfExecutableBlock(container);
                this.finalizeExecutableAddition(block, resolvedReference, true);
            }
            Minecraft.getInstance().setScreen(this);
        });
        Minecraft.getInstance().setScreen(s);
    }

    protected void onAddWhile(@Nullable ExecutableEntry selectionReference) {
        final Executable selectionExecutable = (selectionReference != null) ? selectionReference.executable : null;
        ManageRequirementsScreen s = new ManageRequirementsScreen(new LoadingRequirementContainer(), container -> {
            if (container != null) {
                ExecutableEntry resolvedReference = (selectionExecutable != null) ? this.findEntryForExecutable(selectionExecutable) : null;
                WhileExecutableBlock block = new WhileExecutableBlock(container);
                this.finalizeExecutableAddition(block, resolvedReference, true);
            }
            Minecraft.getInstance().setScreen(this);
        });
        Minecraft.getInstance().setScreen(s);
    }

    protected void onAppendElseIf(@Nullable ExecutableEntry targetEntry) {
        if (!this.canAppendConditionalBlock(targetEntry)) {
            return;
        }
        if ((targetEntry == null) || !(targetEntry.executable instanceof AbstractExecutableBlock)) {
            return;
        }
        final Executable targetExecutable = targetEntry.executable;
        ManageRequirementsScreen s = new ManageRequirementsScreen(new LoadingRequirementContainer(), container -> {
            if (container != null) {
                ExecutableEntry resolvedEntry = this.findEntryForExecutable(targetExecutable);
                if ((resolvedEntry == null) || !(resolvedEntry.executable instanceof AbstractExecutableBlock resolvedBlock)) {
                    Minecraft.getInstance().setScreen(this);
                    return;
                }
                this.createUndoPoint();
                ElseIfExecutableBlock appended = new ElseIfExecutableBlock(container);
                appended.setAppendedBlock(resolvedBlock.getAppendedBlock());
                resolvedBlock.setAppendedBlock(appended);
                this.updateActionInstanceScrollArea(true);
                this.focusEntryForExecutable(appended, true, true);
            }
            Minecraft.getInstance().setScreen(this);
        });
        Minecraft.getInstance().setScreen(s);
    }

    protected void onAppendElse(@Nullable ExecutableEntry targetEntry) {
        if (!this.canAppendElseBlock(targetEntry)) {
            return;
        }
        if ((targetEntry == null) || !(targetEntry.executable instanceof AbstractExecutableBlock)) {
            return;
        }
        final Executable targetExecutable = targetEntry.executable;
        ExecutableEntry resolvedEntry = this.findEntryForExecutable(targetExecutable);
        if ((resolvedEntry == null) || !(resolvedEntry.executable instanceof AbstractExecutableBlock block)) {
            return;
        }
        AbstractExecutableBlock appendTarget = this.findAppendElseTarget(block);
        if (appendTarget == null) {
            return;
        }
        ElseExecutableBlock appended = new ElseExecutableBlock();
        this.createUndoPoint();
        appendTarget.setAppendedBlock(appended);
        this.updateActionInstanceScrollArea(true);
        this.focusEntryForExecutable(appended, true, true);
    }

    protected void onRemove(@Nullable ExecutableEntry entry) {
        if (entry == null) {
            return;
        }
        final Executable targetExecutable = entry.executable;
        ExecutableEntry currentEntry = this.findEntryForExecutable(targetExecutable);
        if (currentEntry != null) {
            this.createUndoPoint();
            if (currentEntry.appendParent != null) {
                currentEntry.appendParent.setAppendedBlock(null);
            }
            currentEntry.getParentBlock().getExecutables().remove(currentEntry.executable);
            this.updateActionInstanceScrollArea(true);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.renderTickDragHoveredEntry = this.getDragHoveredEntry();
        this.renderTickDraggedEntry = this.getDraggedEntry();

        //Auto-scroll scroll area vertically if dragged and out-of-area
        if (this.renderTickDraggedEntry != null) {
            float scrollOffset = 0.1F * this.scriptEntriesScrollArea.verticalScrollBar.getWheelScrollSpeed();
            if (MouseInput.getMouseY() <= this.scriptEntriesScrollArea.getInnerY()) {
                this.scriptEntriesScrollArea.verticalScrollBar.setScroll(this.scriptEntriesScrollArea.verticalScrollBar.getScroll() - scrollOffset);
            }
            if (MouseInput.getMouseY() >= (this.scriptEntriesScrollArea.getInnerY() + this.scriptEntriesScrollArea.getInnerHeight())) {
                this.scriptEntriesScrollArea.verticalScrollBar.setScroll(this.scriptEntriesScrollArea.verticalScrollBar.getScroll() + scrollOffset);
            }
        }

        UIColorTheme theme = UIBase.getUIColorTheme();
        graphics.fill(0, 0, this.width, this.height, theme.screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, theme.generic_text_base_color.getColorInt(), false);
        graphics.drawString(this.font, Component.translatable("fancymenu.actions.screens.manage_screen.actions"), 20, 50, theme.generic_text_base_color.getColorInt(), false);

        int scrollAreaWidth = Math.max(120, this.width - LEFT_MARGIN - RIGHT_MARGIN - MINIMAP_WIDTH - MINIMAP_GAP);
        this.scriptEntriesScrollArea.setWidth(scrollAreaWidth, true);
        this.scriptEntriesScrollArea.setHeight(this.height - 85, true);
        this.scriptEntriesScrollArea.setX(LEFT_MARGIN, true);
        this.scriptEntriesScrollArea.setY(50 + 15, true);

        this.scriptEntriesScrollArea.updateScrollArea();
        this.scriptEntriesScrollArea.updateEntries(null);
        this.processPendingSelection();

        this.minimapX = this.scriptEntriesScrollArea.getXWithBorder() + this.scriptEntriesScrollArea.getWidthWithBorder() + MINIMAP_GAP;
        this.minimapY = this.scriptEntriesScrollArea.getInnerY() - 1;
        this.minimapHeight = this.scriptEntriesScrollArea.getInnerHeight() + 2;

        this.selectedEntry = this.getSelectedEntry();
        this.selectedStatementChainEntries = (this.selectedEntry != null) ? this.getStatementChainOf(this.selectedEntry) : Collections.emptyList();

        this.hoveredEntry = this.getScrollAreaHoveredEntry();
        this.hoveredPrimaryChainEntries = (this.hoveredEntry != null) ? this.getStatementChainOf(this.hoveredEntry) : Collections.emptyList();
        this.hoveredStatementChainEntries = (this.hoveredEntry != null) ? this.collectChainWithSubChains(this.hoveredEntry) : Collections.emptyList();

        this.rebuildMinimapSegments(mouseX, mouseY);

        int buttonsRightEdge = this.width - RIGHT_MARGIN;
        int buttonY = Math.max(20, this.scriptEntriesScrollArea.getYWithBorder() - this.doneButton.getHeight() - ACTION_BUTTON_GAP);
        this.doneButton.setX(buttonsRightEdge - this.doneButton.getWidth());
        this.doneButton.setY(buttonY);
        this.cancelButton.setX(Math.max(LEFT_MARGIN, this.doneButton.getX() - ACTION_BUTTON_GAP - this.cancelButton.getWidth()));
        this.cancelButton.setY(buttonY);

        this.scriptEntriesScrollArea.render(graphics, mouseX, mouseY, partial);
        if (this.scriptEntriesScrollArea.getEntries().isEmpty()) {
            // Hint overlay when no actions exist
            Component hint = Component.translatable("fancymenu.actions.script_editor.empty_hint");
            int hintWidth = this.font.width(hint);
            int hintX = this.scriptEntriesScrollArea.getInnerX() + (this.scriptEntriesScrollArea.getInnerWidth() / 2) - (hintWidth / 2);
            int hintY = this.scriptEntriesScrollArea.getInnerY() + (this.scriptEntriesScrollArea.getInnerHeight() / 2) - (this.font.lineHeight / 2);
            graphics.drawString(this.font, hint, hintX, hintY, theme.element_label_color_inactive.getColorInt(), false);
        }
        this.renderInlineEditors(graphics, mouseX, mouseY, partial);
        this.updateCursor(mouseX, mouseY);

        if (this.renderTickDragHoveredEntry != null) {
            int dY = this.renderTickDragHoveredEntry.getY();
            int dH = this.renderTickDragHoveredEntry.getHeight();
            if (this.renderTickDragHoveredEntry == BEFORE_FIRST) {
                dY = this.scriptEntriesScrollArea.getInnerY();
                dH = 1;
            }
            if (this.renderTickDragHoveredEntry == AFTER_LAST) {
                dY = this.scriptEntriesScrollArea.getInnerY() + this.scriptEntriesScrollArea.getInnerHeight() - 1;
                dH = 1;
            }
            graphics.fill(this.scriptEntriesScrollArea.getInnerX(), dY + dH - 1, this.scriptEntriesScrollArea.getInnerX() + this.scriptEntriesScrollArea.getInnerWidth(), dY + dH, theme.description_area_text_color.getColorInt());
        }

        this.renderChainMinimap(graphics);

        this.doneButton.render(graphics, mouseX, mouseY, partial);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

        this.renderIllegalActionIndicator(graphics);

        // Needs to render as late as possible
        this.renderMinimapEntryTooltip(graphics, mouseX, mouseY);

        // Needs to render after everything else
        this.rightClickContextMenu.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    protected void renderInlineEditors(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.isInlineValueEditing()) {
            if (!this.scriptEntriesScrollArea.getEntries().contains(this.inlineValueEntry)) {
                this.finishInlineValueEditing(false);
            } else {
                this.updateInlineValueEditorBounds();
                if (this.inlineValueEditBox != null) {
                    this.inlineValueEditBox.render(graphics, mouseX, mouseY, partial);
                }
            }
        }
        if (this.isInlineNameEditing()) {
            if (!this.scriptEntriesScrollArea.getEntries().contains(this.inlineNameEntry)) {
                this.finishInlineNameEditing(false);
            } else {
                this.updateInlineNameEditorBounds();
                if (this.inlineNameEditBox != null) {
                    this.inlineNameEditBox.render(graphics, mouseX, mouseY, partial);
                }
            }
        }
    }

    protected void renderIllegalActionIndicator(@NotNull GuiGraphics graphics) {

        if (this.illegalActionIndicatorStartTime <= 0L) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - this.illegalActionIndicatorStartTime;
        long totalDuration = ILLEGAL_ACTION_VISIBLE_DURATION_MS + ILLEGAL_ACTION_FADE_DURATION_MS;
        if (elapsed >= totalDuration) {
            this.illegalActionIndicatorStartTime = -1L;
            return;
        }

        float alpha = ILLEGAL_ACTION_MAX_ALPHA;
        if (elapsed > ILLEGAL_ACTION_VISIBLE_DURATION_MS) {
            long fadeElapsed = elapsed - ILLEGAL_ACTION_VISIBLE_DURATION_MS;
            float fadeProgress = (float)fadeElapsed / (float)ILLEGAL_ACTION_FADE_DURATION_MS;
            alpha = ILLEGAL_ACTION_MAX_ALPHA * (1.0F - Mth.clamp(fadeProgress, 0.0F, 1.0F));
        }

        int targetHeight = Math.max(1, Math.round(this.height / 3.0F));
        int[] size = ILLEGAL_ACTION_ICON_RATIO.getAspectRatioSizeByMaximumSize(Math.max(1, this.width), targetHeight);
        int iconWidth = size[0];
        int iconHeight = size[1];
        int iconX = (this.width - iconWidth) / 2;
        int iconY = (this.height - iconHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, ILLEGAL_ACTION_ICON, iconX, iconY, 0.0F, 0.0F, iconWidth, iconHeight, iconWidth, iconHeight, UIBase.getUIColorTheme().ui_texture_color.getColorIntWithAlpha(alpha));

    }

    protected void showIllegalActionIndicator() {
        this.illegalActionIndicatorStartTime = System.currentTimeMillis();
    }

    protected void renderChainMinimap(@NotNull GuiGraphics graphics) {

        if (this.minimapHeight <= 0) {
            return;
        }
        UIColorTheme theme = UIBase.getUIColorTheme();
        graphics.fill(this.minimapX, this.minimapY, this.minimapX + MINIMAP_WIDTH, this.minimapY + this.minimapHeight, theme.actions_minimap_background_color.getColorInt());
        UIBase.renderBorder(graphics, this.minimapX, this.minimapY, this.minimapX + MINIMAP_WIDTH, this.minimapY + this.minimapHeight, 1, theme.actions_minimap_border_color, true, true, true, true);

        List<ExecutableEntry> hoverChain = this.getActiveHoveredChain();
        ExecutableEntry activeHoverEntry = this.getActiveHoveredEntry();

        for (MinimapEntrySegment segment : this.minimapSegments) {
            ExecutableEntry entry = segment.entry;
            graphics.fill(segment.x, segment.y, segment.x + segment.width, segment.y + segment.height, this.getMinimapEntryBaseColor(entry).getRGB());
            if (this.selectedEntry == entry) {
                UIBase.renderBorder(graphics, segment.x, segment.y, segment.x + segment.width, segment.y + segment.height, 1, theme.actions_chain_indicator_selected_color, true, true, true, true);
            } else if (activeHoverEntry == entry) {
                UIBase.renderBorder(graphics, segment.x, segment.y, segment.x + segment.width, segment.y + segment.height, 1, theme.description_area_text_color, true, true, true, true);
            }
        }
        if (!hoverChain.isEmpty()) {
            this.renderChainMinimapBorder(graphics, hoverChain, theme.actions_chain_indicator_hovered_color.getColorInt());
        }
        this.renderMinimapViewport(graphics, theme);
    }

    protected void renderMinimapEntryTooltip(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.minimapHoveredEntry == null) {
            return;
        }
        int entryWidth = this.minimapHoveredEntry.getWidth();
        int entryHeight = this.minimapHoveredEntry.getHeight();
        if ((entryWidth <= 0) || (entryHeight <= 0)) {
            return;
        }
        int scaledWidth = Math.max(1, Math.round(entryWidth * MINIMAP_TOOLTIP_SCALE));
        int scaledHeight = Math.max(1, Math.round(entryHeight * MINIMAP_TOOLTIP_SCALE));
        int tooltipWidth = scaledWidth + (MINIMAP_TOOLTIP_PADDING * 2);
        int tooltipHeight = scaledHeight + (MINIMAP_TOOLTIP_PADDING * 2);

        int tooltipX = mouseX + MINIMAP_TOOLTIP_OFFSET;
        int tooltipY = mouseY + MINIMAP_TOOLTIP_OFFSET;

        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = Math.max(0, this.width - tooltipWidth);
        }
        if (tooltipY + tooltipHeight > this.height) {
            tooltipY = Math.max(0, this.height - tooltipHeight);
        }
        if (tooltipX < 0) {
            tooltipX = 0;
        }
        if (tooltipY < 0) {
            tooltipY = 0;
        }

        UIColorTheme theme = UIBase.getUIColorTheme();
        Color backgroundColor = withAlpha(theme.screen_background_color.getColor(), 220);

        Matrix3x2fStack matrix = graphics.pose();
        matrix.pushMatrix();

        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, backgroundColor.getRGB());
        UIBase.renderBorder(graphics, tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 1, theme.actions_minimap_tooltip_border_color, true, true, true, true);

        matrix.translate(tooltipX + MINIMAP_TOOLTIP_PADDING, tooltipY + MINIMAP_TOOLTIP_PADDING);
        matrix.scale(MINIMAP_TOOLTIP_SCALE, MINIMAP_TOOLTIP_SCALE);
        matrix.translate(-this.minimapHoveredEntry.getX(), -this.minimapHoveredEntry.getY());

        this.minimapHoveredEntry.renderThumbnail(graphics);

        matrix.popMatrix();

    }

    protected void renderChainMinimapBorder(@NotNull GuiGraphics graphics, @NotNull List<ExecutableEntry> chainEntries, int color) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean found = false;
        for (MinimapEntrySegment segment : this.minimapSegments) {
            if (chainEntries.contains(segment.entry)) {
                found = true;
                minX = Math.min(minX, segment.x);
                maxX = Math.max(maxX, segment.x + segment.width);
                minY = Math.min(minY, segment.y);
                maxY = Math.max(maxY, segment.y + segment.height);
            }
        }
        if (!found) {
            return;
        }
        if ((maxX <= minX) || (maxY <= minY)) {
            return;
        }
        UIBase.renderBorder(graphics, minX, minY, maxX, maxY, 1, color, true, true, true, true);
    }

    protected void renderMinimapViewport(@NotNull GuiGraphics graphics, @NotNull UIColorTheme theme) {
        if (this.minimapTotalEntriesHeight <= 0 || this.minimapContentHeight <= 0) {
            return;
        }
        int visibleHeight = this.scriptEntriesScrollArea.getInnerHeight();
        if (this.minimapTotalEntriesHeight <= visibleHeight) {
            return;
        }
        int maxScroll = Math.max(1, this.minimapTotalEntriesHeight - visibleHeight);
        int scrollPixels = Math.round(this.scriptEntriesScrollArea.verticalScrollBar.getScroll() * maxScroll);
        float topRatio = (float)scrollPixels / (float)this.minimapTotalEntriesHeight;
        float heightRatio = (float)visibleHeight / (float)this.minimapTotalEntriesHeight;

        int viewportTop = this.minimapContentY + Math.round(topRatio * this.minimapContentHeight);
        int viewportHeight = Math.max(2, Math.round(heightRatio * this.minimapContentHeight));
        int maxViewportBottom = this.minimapContentY + this.minimapContentHeight;
        if (viewportTop + viewportHeight > maxViewportBottom) {
            viewportHeight = maxViewportBottom - viewportTop;
        }
        Color viewportBaseColor = theme.actions_minimap_viewport_color.getColor();
        Color viewportColor = withAlpha(viewportBaseColor, Math.max(0, Math.min(255, viewportBaseColor.getAlpha() / 2)));
        graphics.fill(this.minimapContentX, viewportTop, this.minimapContentX + this.minimapContentWidth, viewportTop + viewportHeight, viewportColor.getRGB());
        UIBase.renderBorder(graphics, this.minimapContentX, viewportTop, this.minimapContentX + this.minimapContentWidth, viewportTop + viewportHeight, 1, theme.actions_minimap_viewport_border_color, true, true, true, true);
    }

    @NotNull
    protected Color getMinimapEntryBaseColor(@NotNull ExecutableEntry entry) {
        UIColorTheme theme = UIBase.getUIColorTheme();
        Color base;
        if (entry.executable instanceof IfExecutableBlock) {
            base = theme.actions_entry_background_color_if.getColor();
        } else if (entry.executable instanceof ElseIfExecutableBlock) {
            base = theme.actions_entry_background_color_else_if.getColor();
        } else if (entry.executable instanceof ElseExecutableBlock) {
            base = theme.actions_entry_background_color_else.getColor();
        } else if (entry.executable instanceof WhileExecutableBlock) {
            base = theme.actions_entry_background_color_while.getColor();
        } else if (entry.executable instanceof FolderExecutableBlock) {
            base = theme.actions_entry_background_color_folder.getColor();
        } else if (entry.executable instanceof AbstractExecutableBlock) {
            base = theme.actions_entry_background_color_generic_block.getColor();
        } else {
            base = theme.actions_entry_background_color_action.getColor();
        }
        return withAlpha(base, 180);
    }

    protected void rebuildMinimapSegments(int mouseX, int mouseY) {
        this.minimapSegments.clear();
        this.minimapHoveredEntry = null;
        this.minimapHoveredPrimaryChainEntries = Collections.emptyList();
        this.minimapHoveredStatementChainEntries = Collections.emptyList();
        this.minimapContentX = this.minimapX + MINIMAP_PADDING;
        this.minimapContentY = this.minimapY + MINIMAP_PADDING;
        this.minimapContentWidth = Math.max(1, MINIMAP_WIDTH - (MINIMAP_PADDING * 2));
        this.minimapContentHeight = Math.max(1, this.minimapHeight - (MINIMAP_PADDING * 2));
        this.minimapTotalEntriesHeight = 0;

        List<ScrollAreaEntry> scrollEntries = this.scriptEntriesScrollArea.getEntries();
        List<ExecutableEntry> execEntries = new ArrayList<>();
        for (ScrollAreaEntry entry : scrollEntries) {
            if (entry instanceof ExecutableEntry ee) {
                execEntries.add(ee);
            }
        }
        if (execEntries.isEmpty()) {
            return;
        }

        for (ExecutableEntry entry : execEntries) {
            this.minimapTotalEntriesHeight += entry.getHeight();
        }
        if (this.minimapTotalEntriesHeight <= 0) {
            this.minimapTotalEntriesHeight = 1;
        }

        int offset = 0;
        for (ExecutableEntry entry : execEntries) {
            int entryHeight = entry.getHeight();
            float startRatio = (float)offset / (float)this.minimapTotalEntriesHeight;
            float endRatio = (float)(offset + entryHeight) / (float)this.minimapTotalEntriesHeight;
            int top = this.minimapContentY + Math.round(startRatio * this.minimapContentHeight);
            int bottom = this.minimapContentY + Math.round(endRatio * this.minimapContentHeight);
            if (bottom <= top) {
                bottom = top + 1;
            }
            int height = bottom - top;
            int indentOffset = entry.indentLevel * MINIMAP_INDENT_STEP;
            int maxIndent = Math.max(0, this.minimapContentWidth - 1);
            if (indentOffset > maxIndent) {
                indentOffset = maxIndent;
            }
            int segmentX = this.minimapContentX + indentOffset;
            int segmentWidth = Math.max(1, this.minimapContentWidth - indentOffset);

            MinimapEntrySegment segment = new MinimapEntrySegment(entry, segmentX, top, segmentWidth, height);
            this.minimapSegments.add(segment);

            if (UIBase.isXYInArea(mouseX, mouseY, segment.x, segment.y, segment.width, segment.height)) {
                this.minimapHoveredEntry = entry;
            }

            offset += entryHeight;
        }

        if (this.minimapHoveredEntry != null) {
            this.minimapHoveredPrimaryChainEntries = this.getStatementChainOf(this.minimapHoveredEntry);
            this.minimapHoveredStatementChainEntries = this.collectChainWithSubChains(this.minimapHoveredEntry);
        }
    }

    @Nullable
    protected ExecutableEntry getMinimapEntryAt(int mouseX, int mouseY) {
        for (MinimapEntrySegment segment : this.minimapSegments) {
            if (UIBase.isXYInArea(mouseX, mouseY, segment.x, segment.y, segment.width, segment.height)) {
                return segment.entry;
            }
        }
        return null;
    }

    public boolean isMinimapHovered(int mouseX, int mouseY) {
        return (this.minimapHeight > 0) && UIBase.isXYInArea((int)mouseX, (int)mouseY, this.minimapX, this.minimapY, MINIMAP_WIDTH, this.minimapHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMinimapHovered((int)mouseX, (int)mouseY) && !this.isUserNavigatingInRightClickContextMenu()) {
            if (scrollY != 0.0D) {
                float scrollStep = 0.1F * this.scriptEntriesScrollArea.verticalScrollBar.getWheelScrollSpeed();
                float totalOffset = scrollStep * (float)Math.abs(scrollY);
                if (scrollY > 0.0D) {
                    totalOffset = -totalOffset;
                }
                this.scriptEntriesScrollArea.verticalScrollBar.setScroll(this.scriptEntriesScrollArea.verticalScrollBar.getScroll() + totalOffset);
                this.scriptEntriesScrollArea.updateEntries(null);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {

        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        boolean skipSelection = this.skipNextContextMenuSelection;
        this.skipNextContextMenuSelection = false;

        // De-select the selected entry when clicking somewhere else
        if (!skipSelection && (this.hoveredEntry == null) && !this.isMinimapHovered((int)mouseX, (int)mouseY)) {
            if (this.selectedEntry != null) {
                this.selectedEntry.forceSetSelected(false);
            }
            this.selectedEntry = null;
        }

        if (this.isInlineNameEditing()) {
            boolean insideNameEditor = UIBase.isXYInArea((int) mouseX, (int) mouseY, this.inlineNameEditBox.getX(), this.inlineNameEditBox.getY(), this.inlineNameEditBox.getWidth(), this.inlineNameEditBox.getHeight());
            if (insideNameEditor && this.inlineNameEditBox.mouseClicked(event, isDoubleClick)) {
                return true;
            }
            if (!insideNameEditor) {
                this.finishInlineNameEditing(true);
            }
        }

        if (this.isInlineValueEditing()) {
            boolean insideEditor = UIBase.isXYInArea((int) mouseX, (int) mouseY, this.inlineValueEditBox.getX(), this.inlineValueEditBox.getY(), this.inlineValueEditBox.getWidth(), this.inlineValueEditBox.getHeight());
            if (insideEditor && this.inlineValueEditBox.mouseClicked(event, isDoubleClick)) {
                return true;
            }
            if (!insideEditor) {
                this.finishInlineValueEditing(true);
            }
        }

        boolean actionsMenuInteracting = this.isUserNavigatingInRightClickContextMenu();

        if ((button == 0) && !actionsMenuInteracting) {
            this.rightClickContextMenu.closeMenu();
            this.contextMenuTargetExecutable = null;
        }

        if (!actionsMenuInteracting && (button == 1)) {
            if (this.isInsideActionsScrollArea((int)mouseX, (int)mouseY)) {
                ExecutableEntry hovered = this.getScrollAreaHoveredEntry();
                ExecutableEntry target = null;
                if ((hovered != null) && (hovered != BEFORE_FIRST) && (hovered != AFTER_LAST)) {
                    target = hovered;
                    if (!skipSelection) {
                        hovered.setSelected(true);
                    }
                }
                this.contextMenuTargetExecutable = (target != null) ? target.executable : null;
                if (this.rightClickContextMenu != null) {
                    this.openRightClickContextMenuAt((float)mouseX, (float)mouseY, null);
                }
                return true;
            }
        }

        if (!skipSelection && (button == 0) && (this.minimapHeight > 0) && UIBase.isXYInArea((int)mouseX, (int)mouseY, this.minimapX, this.minimapY, MINIMAP_WIDTH, this.minimapHeight)) {
            ExecutableEntry entry = this.getMinimapEntryAt((int)mouseX, (int)mouseY);
            if (entry != null) {
                entry.setSelected(true);
                this.scrollEntryIntoView(entry);
                return true;
            }
        }

        if ((button == 0) && !actionsMenuInteracting && !skipSelection) {
            ExecutableEntry hovered = this.getScrollAreaHoveredEntry();
            if (hovered != null) {
                if (hovered.canToggleCollapse() && hovered.isMouseOverCollapseToggle((int)mouseX, (int)mouseY)) {
                    this.finishInlineNameEditing(true);
                    this.finishInlineValueEditing(true);
                    this.toggleCollapseAndPreserveView(hovered);
                    return true;
                }

                if (hovered.canInlineEditName() && hovered.isMouseOverName((int)mouseX, (int)mouseY)) {
                    if (hovered.registerNameClick((int)mouseX, (int)mouseY)) {
                        this.startInlineNameEditing(hovered);
                        return true;
                    }
                } else if (hovered.canInlineEditName()) {
                    hovered.resetNameClickTimer();
                }

                if (hovered.canInlineEditValue() && hovered.isMouseOverValue((int)mouseX, (int)mouseY)) {
                    if (hovered.registerValueClick((int)mouseX, (int)mouseY)) {
                        this.startInlineValueEditing(hovered);
                        return true;
                    }
                } else {
                    hovered.resetValueClickTimer();
                }
            }
        }

        boolean handled = super.mouseClicked(event, isDoubleClick);
        if (!skipSelection && !actionsMenuInteracting && (button == 0) && !this.isInlineValueEditing()) {
            ExecutableEntry hoveredAfter = this.getScrollAreaHoveredEntry();
            if ((hoveredAfter == null) || !hoveredAfter.isMouseOverValue((int)mouseX, (int)mouseY)) {
                for (ScrollAreaEntry entry : this.scriptEntriesScrollArea.getEntries()) {
                    if (entry instanceof ExecutableEntry ee) {
                        ee.resetValueClickTimer();
                    }
                }
            }
        }

        if (!skipSelection && !actionsMenuInteracting && (button == 0) && !this.isInlineNameEditing()) {
            ExecutableEntry hoveredAfter = this.getScrollAreaHoveredEntry();
            if ((hoveredAfter == null) || !hoveredAfter.isMouseOverName((int)mouseX, (int)mouseY)) {
                for (ScrollAreaEntry entry : this.scriptEntriesScrollArea.getEntries()) {
                    if (entry instanceof ExecutableEntry ee && ee.canInlineEditName()) {
                        ee.resetNameClickTimer();
                    }
                }
            }
        }

        return handled;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 1) {
            // Because of some client tick shenanigans it's important to delay the reset of the click consume var some ticks
            MainThreadTaskExecutor.executeInMainThread(() ->
                    MainThreadTaskExecutor.executeInMainThread(() -> this.actionsMenuRightClickConsumedByEntry = false,
                            MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }
        if ((this.inlineNameEditBox != null) && this.inlineNameEditBox.mouseReleased(event)) {
            return true;
        }
        if ((this.inlineValueEditBox != null) && this.inlineValueEditBox.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int scanCode = event.scancode();
        if (this.inlineNameEditBox != null) {
            if ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER)) {
                this.finishInlineNameEditing(true);
                return true;
            }
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.finishInlineNameEditing(false);
                return true;
            }
            if (this.inlineNameEditBox.keyPressed(event)) {
                return true;
            }
        }

        if (this.inlineValueEditBox != null) {
            if ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER)) {
                this.finishInlineValueEditing(true);
                return true;
            }
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.finishInlineValueEditing(false);
                return true;
            }
            if (this.inlineValueEditBox.keyPressed(event)) {
                return true;
            }
        }

        boolean contextMenuActive = this.isUserNavigatingInRightClickContextMenu() || ((this.rightClickContextMenu != null) && this.rightClickContextMenu.isOpen());
        boolean inlineEditingActive = this.isInlineNameEditing() || this.isInlineValueEditing();

        if (!contextMenuActive && !inlineEditingActive) {
            ExecutableEntry selected = this.getSelectedEntry();
            boolean ctrlDown = event.hasControlDown();
            boolean shiftDown = event.hasShiftDown();
            String keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
            keyName = (keyName != null) ? keyName.toLowerCase(Locale.ROOT) : "";

            if (keyCode == InputConstants.KEY_DELETE) {
                if (this.deleteSelectedEntryDirectly()) {
                    return true;
                }
            }

            if ("z".equals(keyName) && ctrlDown && shiftDown) {
                if (this.redo()) {
                    return true;
                }
            }

            if ("z".equals(keyName) && ctrlDown && !shiftDown) {
                if (this.undo()) {
                    return true;
                }
            }

            if ("y".equals(keyName) && ctrlDown) {
                if (this.redo()) {
                    return true;
                }
            }

            if ("c".equals(keyName) && ctrlDown) {
                if (this.copySelectedAction()) {
                    return true;
                }
                if (selected != null) {
                    this.showIllegalActionIndicator();
                    return true;
                }
            }

            if ("v".equals(keyName) && ctrlDown) {
                if (this.pasteCopiedAction(selected)) {
                    return true;
                }
            }

            if ("a".equals(keyName)) {
                this.onOpenActionChooser(this.selectedEntry);
                return true;
            }

            if ((keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
                if (shiftDown) {
                    if (this.moveSelectedEntry(keyCode == InputConstants.KEY_UP)) {
                        return true;
                    }
                } else {
                    if (this.selectAdjacentEntry(keyCode == InputConstants.KEY_DOWN)) {
                        return true;
                    }
                }
            }

            if ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER)) {
                if (this.handleEnterShortcut(selected)) {
                    return true;
                }
            }
        }

        return super.keyPressed(event);
    }

    protected boolean handleEnterShortcut(@Nullable ExecutableEntry selected) {
        if (selected == null) {
            return false;
        }
        if (selected.executable instanceof ActionInstance instance) {
            if (!instance.action.hasValue()) {
                return false;
            }
            this.startInlineValueEditing(selected);
            return true;
        }
        if (selected.executable instanceof FolderExecutableBlock) {
            this.startInlineNameEditing(selected);
            return true;
        }
        if ((selected.executable instanceof IfExecutableBlock) || (selected.executable instanceof ElseIfExecutableBlock) || (selected.executable instanceof WhileExecutableBlock)) {
            this.onEdit(this.getSelectedEntry());
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if ((this.inlineNameEditBox != null) && this.inlineNameEditBox.charTyped(event)) {
            return true;
        }
        if ((this.inlineValueEditBox != null) && this.inlineValueEditBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    private void updateCursor(int mouseX, int mouseY) {
        if (this.inlineNameEditBox != null) {
            if (UIBase.isXYInArea(mouseX, mouseY, this.inlineNameEditBox.getX(), this.inlineNameEditBox.getY(), this.inlineNameEditBox.getWidth(), this.inlineNameEditBox.getHeight())) {
                CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
                return;
            }
        }
        if (this.inlineValueEditBox != null) {
            if (UIBase.isXYInArea(mouseX, mouseY, this.inlineValueEditBox.getX(), this.inlineValueEditBox.getY(), this.inlineValueEditBox.getWidth(), this.inlineValueEditBox.getHeight())) {
                CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
                return;
            }
        }
        ExecutableEntry hovered = this.hoveredEntry;
        if ((hovered != null) && hovered.canToggleCollapse() && hovered.isMouseOverCollapseToggle(mouseX, mouseY)) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
            return;
        }
        if ((hovered != null) && hovered.canInlineEditName() && hovered.isMouseOverName(mouseX, mouseY)) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
            return;
        }
        if ((hovered != null) && hovered.canInlineEditValue() && hovered.isMouseOverValue(mouseX, mouseY)) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
        }
    }

    private void toggleCollapseAndPreserveView(@NotNull ExecutableEntry entry) {
        if (!entry.canToggleCollapse()) {
            return;
        }
        this.createUndoPoint();
        ScrollArea scrollArea = this.scriptEntriesScrollArea;
        int desiredOffset = entry.getY() - scrollArea.getInnerY();
        Executable executable = entry.executable;
        entry.toggleCollapsed();
        this.updateActionInstanceScrollArea(true);
        this.restoreEntryTopPosition(executable, desiredOffset);
    }

    private void restoreEntryTopPosition(@NotNull Executable executable, int desiredOffset) {
        ScrollArea scrollArea = this.scriptEntriesScrollArea;
        scrollArea.updateEntries(null);

        ExecutableEntry target = null;
        int cumulativeHeight = 0;
        List<ScrollAreaEntry> entries = scrollArea.getEntries();
        for (ScrollAreaEntry scrollEntry : entries) {
            if (scrollEntry instanceof ExecutableEntry ee) {
                if (ee.executable == executable) {
                    target = ee;
                    break;
                }
                cumulativeHeight += ee.getHeight();
            } else {
                cumulativeHeight += scrollEntry.getHeight();
            }
        }
        if (target == null) {
            return;
        }

        int totalScrollHeight = scrollArea.getTotalScrollHeight();
        if (totalScrollHeight <= 0) {
            scrollArea.verticalScrollBar.setScroll(0.0F);
            scrollArea.updateEntries(null);
            return;
        }

        int innerY = scrollArea.getInnerY();
        int maxOffset = Math.max(0, scrollArea.getInnerHeight() - target.getHeight());
        int minOffset = -target.getHeight();
        int clampedOffset = Mth.clamp(desiredOffset, minOffset, maxOffset);
        float targetScroll = (float)(cumulativeHeight - clampedOffset) / (float) totalScrollHeight;
        float clampedScroll = Mth.clamp(targetScroll, 0.0F, 1.0F);
        scrollArea.verticalScrollBar.setScroll(clampedScroll);
        scrollArea.updateEntries(null);

        for (int attempt = 0; attempt < 2; attempt++) {
            int currentOffset = target.getY() - innerY;
            int diff = currentOffset - clampedOffset;
            if (Math.abs(diff) <= 1) {
                break;
            }
            float adjust = (float) diff / (float) totalScrollHeight;
            float newScroll = Mth.clamp(scrollArea.verticalScrollBar.getScroll() + adjust, 0.0F, 1.0F);
            if (Math.abs(newScroll - scrollArea.verticalScrollBar.getScroll()) < 1.0E-4F) {
                break;
            }
            scrollArea.verticalScrollBar.setScroll(newScroll);
            scrollArea.updateEntries(null);
        }
    }
    protected void startInlineValueEditing(@NotNull ExecutableEntry entry) {
        if (!(entry.executable instanceof ActionInstance instance) || !instance.action.hasValue()) {
            return;
        }
        this.finishInlineValueEditing(true);
        this.inlineValueEntry = entry;
        this.inlineValueOriginal = instance.value;
        this.inlineValueEditBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 10, 10, Component.empty());
        this.inlineValueEditBox.setHeight(10);
        this.inlineValueEditBox.setMaxLength(100000);
        String value = instance.value;
        this.inlineValueEditBox.setValue((value != null) ? value : "");
        this.inlineValueEditBox.setCursorPosition(this.inlineValueEditBox.getValue().length());
        this.inlineValueEditBox.setHighlightPos(0);
        UIBase.applyDefaultWidgetSkinTo(this.inlineValueEditBox);
        this.updateInlineValueEditorBounds();
        this.inlineValueEditBox.setFocused(true);
        this.setFocused(this.inlineValueEditBox);
        entry.setSelected(true);
        entry.leftMouseDownDragging = false;
        entry.dragging = false;
        entry.resetValueClickTimer();
    }

    protected void finishInlineValueEditing(boolean save) {
        if (!this.isInlineValueEditing()) {
            return;
        }
        ExecutableEntry entry = this.inlineValueEntry;
        ExtendedEditBox editBox = this.inlineValueEditBox;
        this.inlineValueEntry = null;
        this.inlineValueEditBox = null;
        this.setFocused(null);
        if (entry.executable instanceof ActionInstance instance) {
            String result = Objects.requireNonNullElse(editBox.getValue(), "");
            if (!save) {
                instance.value = this.inlineValueOriginal;
            } else {
                String normalized = !result.isEmpty() ? result : null;
                if (!Objects.equals(instance.value, normalized)) {
                    this.createUndoPoint();
                    instance.value = normalized;
                }
            }
            entry.updateValueComponent();
            entry.setWidth(entry.calculateWidth());
            entry.resetValueClickTimer();
        }
        this.inlineValueOriginal = null;
        this.scriptEntriesScrollArea.updateEntries(null);
    }

    private void updateInlineValueEditorBounds() {
        if (!this.isInlineValueEditing()) {
            return;
        }
        ExecutableEntry entry = this.inlineValueEntry;
        int valueX = entry.getValueFieldX();
        int valueY = entry.getValueFieldY();
        int availableWidth = entry.getValueFieldAvailableWidth();
        this.inlineValueEditBox.setX(valueX);
        this.inlineValueEditBox.setY(valueY);
        this.inlineValueEditBox.setWidth(availableWidth);
        this.inlineValueEditBox.setHeight(10);
    }

    private boolean isInlineValueEditing() {
        return (this.inlineValueEditBox != null) && (this.inlineValueEntry != null);
    }

    protected void startInlineNameEditing(@NotNull ExecutableEntry entry) {
        if (!(entry.executable instanceof FolderExecutableBlock folder)) {
            return;
        }
        this.finishInlineNameEditing(true);
        this.finishInlineValueEditing(true);
        this.inlineNameEntry = entry;
        this.inlineNameOriginal = folder.getName();
        this.inlineNameEditBox = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 10, 10, Component.empty());
        this.inlineNameEditBox.setHeight(10);
        this.inlineNameEditBox.setMaxLength(256);
        this.inlineNameEditBox.setValue(folder.getName());
        this.inlineNameEditBox.setCursorPosition(this.inlineNameEditBox.getValue().length());
        this.inlineNameEditBox.setHighlightPos(0);
        UIBase.applyDefaultWidgetSkinTo(this.inlineNameEditBox);
        this.updateInlineNameEditorBounds();
        this.inlineNameEditBox.setFocused(true);
        this.setFocused(this.inlineNameEditBox);
        entry.setSelected(true);
        entry.leftMouseDownDragging = false;
        entry.dragging = false;
        entry.resetNameClickTimer();
    }

    protected void finishInlineNameEditing(boolean save) {
        if (!this.isInlineNameEditing()) {
            return;
        }
        ExecutableEntry entry = this.inlineNameEntry;
        ExtendedEditBox editBox = this.inlineNameEditBox;
        this.inlineNameEntry = null;
        this.inlineNameEditBox = null;
        this.setFocused(null);
        if (entry.executable instanceof FolderExecutableBlock folder) {
            String result = Objects.requireNonNullElse(editBox.getValue(), "");
            if (!save) {
                folder.setName(this.inlineNameOriginal != null ? this.inlineNameOriginal : FolderExecutableBlock.DEFAULT_NAME);
            } else {
                String normalized = result.trim();
                if (normalized.isEmpty()) {
                    normalized = FolderExecutableBlock.DEFAULT_NAME;
                }
                if (!Objects.equals(folder.getName(), normalized)) {
                    this.createUndoPoint();
                    folder.setName(normalized);
                }
            }
            entry.rebuildComponents();
            entry.setWidth(entry.calculateWidth());
            entry.resetNameClickTimer();
        }
        this.inlineNameOriginal = null;
        this.scriptEntriesScrollArea.updateEntries(null);
    }

    private void updateInlineNameEditorBounds() {
        if (!this.isInlineNameEditing()) {
            return;
        }
        ExecutableEntry entry = this.inlineNameEntry;
        int nameX = entry.getNameFieldX();
        int nameY = entry.getNameFieldY();
        int availableWidth = entry.getNameFieldAvailableWidth();
        this.inlineNameEditBox.setX(nameX);
        this.inlineNameEditBox.setY(nameY);
        this.inlineNameEditBox.setWidth(availableWidth);
        this.inlineNameEditBox.setHeight(10);
    }

    private boolean isInlineNameEditing() {
        return (this.inlineNameEditBox != null) && (this.inlineNameEntry != null);
    }

    @Nullable
    protected ExecutableEntry getScrollAreaHoveredEntry() {
        if (this.isUserNavigatingInRightClickContextMenu()) return null;
        if (!this.scriptEntriesScrollArea.isMouseInsideArea()) {
            return null;
        }
        int mouseX = MouseInput.getMouseX();
        int mouseY = MouseInput.getMouseY();
        for (ScrollAreaEntry entry : this.scriptEntriesScrollArea.getEntries()) {
            if (entry instanceof ExecutableEntry ee) {
                if (UIBase.isXYInArea(mouseX, mouseY, ee.getX(), ee.getY(), this.scriptEntriesScrollArea.getInnerWidth(), ee.getHeight())) {
                    return ee;
                }
            }
        }
        return null;
    }

    @NotNull
    protected List<ExecutableEntry> getActiveHoveredChain() {
        if (!this.minimapHoveredStatementChainEntries.isEmpty()) {
            return this.minimapHoveredStatementChainEntries;
        }
        if (!this.hoveredStatementChainEntries.isEmpty()) {
            return this.hoveredStatementChainEntries;
        }
        return Collections.emptyList();
    }

    @NotNull
    protected List<ExecutableEntry> getHoveredChainForIndicator() {
        if (!this.minimapHoveredPrimaryChainEntries.isEmpty()) {
            return this.minimapHoveredPrimaryChainEntries;
        }
        if (!this.hoveredPrimaryChainEntries.isEmpty()) {
            return this.hoveredPrimaryChainEntries;
        }
        ExecutableEntry active = this.getActiveHoveredEntry();
        if (active != null) {
            return this.getStatementChainOf(active);
        }
        return Collections.emptyList();
    }

    @Nullable
    protected ExecutableEntry getActiveHoveredEntry() {
        if (this.minimapHoveredEntry != null) {
            return this.minimapHoveredEntry;
        }
        return this.hoveredEntry;
    }

    protected boolean isEntryPartOfChain(@NotNull ExecutableEntry entry, @NotNull List<ExecutableEntry> chainEntries) {
        if (chainEntries.isEmpty()) {
            return false;
        }
        if (chainEntries.contains(entry)) {
            return true;
        }
        ExecutableEntry anchor = this.getChainAnchor(entry);
        return (anchor != null) && chainEntries.contains(anchor);
    }

    @NotNull
    protected Color getChainIndicatorColorFor(@NotNull ExecutableEntry entry) {
        UIColorTheme theme = UIBase.getUIColorTheme();
        if (!this.selectedStatementChainEntries.isEmpty() && this.isEntryPartOfChain(entry, this.selectedStatementChainEntries)) {
            return theme.actions_chain_indicator_selected_color.getColor();
        }
        List<ExecutableEntry> hoveredChain = this.getHoveredChainForIndicator();
        if (!hoveredChain.isEmpty() && this.isEntryPartOfChain(entry, hoveredChain)) {
            return theme.actions_chain_indicator_hovered_color.getColor();
        }
        return theme.actions_chain_indicator_color.getColor();
    }

    protected void scrollEntryIntoView(@NotNull ExecutableEntry entry) {
        List<ScrollAreaEntry> scrollEntries = this.scriptEntriesScrollArea.getEntries();
        int totalHeight = 0;
        for (ScrollAreaEntry e : scrollEntries) {
            totalHeight += e.getHeight();
        }
        int visibleHeight = this.scriptEntriesScrollArea.getInnerHeight();
        if (totalHeight <= visibleHeight) {
            return;
        }
        int offset = 0;
        for (ScrollAreaEntry e : scrollEntries) {
            if (e == entry) {
                break;
            }
            offset += e.getHeight();
        }
        int entryCenter = offset + (entry.getHeight() / 2);
        int target = Math.max(0, entryCenter - (visibleHeight / 2));
        int maxScroll = Math.max(1, totalHeight - visibleHeight);
        float scroll = Math.min(1.0F, (float)target / (float)maxScroll);
        this.scriptEntriesScrollArea.verticalScrollBar.setScroll(scroll);
        this.scriptEntriesScrollArea.updateEntries(null);
    }

    private static Color withAlpha(@NotNull Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, Math.max(0, alpha)));
    }

    protected boolean isContentOfStatementChain(@NotNull ExecutableEntry entry, @NotNull List<ExecutableEntry> statementChain) {
        List<ExecutableEntry> parentBlockHierarchy = this.getParentBlockHierarchyOf(entry);
        for (ExecutableEntry parentBlock : parentBlockHierarchy) {
            if (statementChain.contains(parentBlock)) return true;
        }
        return false;
    }

    @NotNull
    protected List<ExecutableEntry> getParentBlockHierarchyOf(@NotNull ExecutableEntry entry) {
        List<ExecutableEntry> blocks = new ArrayList<>();
        ExecutableEntry e = entry;
        while (e != null) {
            e = (e.parentBlock != null) ? this.findEntryForExecutable(e.parentBlock) : null;
            if (e != null) {
                blocks.add(0, e);
            }
        }
        return blocks;
    }

    @NotNull
    protected List<ExecutableEntry> getStatementChainOf(@NotNull ExecutableEntry entry) {
        ExecutableEntry anchor = this.getChainAnchor(entry);
        if (anchor == null) {
            return Collections.emptyList();
        }

        List<ExecutableEntry> entries = new ArrayList<>();

        List<ExecutableEntry> beforeEntry = new ArrayList<>();
        ExecutableEntry e1 = anchor;
        while (e1 != null) {
            e1 = (e1.appendParent != null) ? this.findEntryForExecutable(e1.appendParent) : null;
            if (e1 != null) {
                beforeEntry.add(0, e1);
            }
        }

        List<ExecutableEntry> afterEntry = new ArrayList<>();
        ExecutableEntry e2 = anchor;
        while (e2 != null) {
            if (e2.executable instanceof AbstractExecutableBlock b) {
                AbstractExecutableBlock appendChild = b.getAppendedBlock();
                e2 = (appendChild != null) ? this.findEntryForExecutable(appendChild) : null;
                if (e2 != null) {
                    afterEntry.add(e2);
                }
            } else {
                e2 = null;
            }
        }

        entries.addAll(beforeEntry);
        entries.add(anchor);
        entries.addAll(afterEntry);

        if (anchor.executable instanceof WhileExecutableBlock) {
            this.addWhileChainEntries(anchor, entries);
        }

        if ((anchor != entry) && (entry.parentBlock != null) && (entries.size() > 1)) {
            ExecutableEntry lastStatement = entries.get(entries.size() - 1);
            if (lastStatement.executable == entry.parentBlock) {
                List<ExecutableEntry> extended = new ArrayList<>(entries);
                extended.add(entry);
                entries = extended;
            }
        }

        return entries;
    }

    @Nullable
    protected ExecutableEntry getChainAnchor(@NotNull ExecutableEntry entry) {
        if (entry.executable instanceof AbstractExecutableBlock) {
            return entry;
        }
        if (entry.parentBlock != null) {
            return this.findEntryForExecutable(entry.parentBlock);
        }
        if (entry.appendParent != null) {
            return this.findEntryForExecutable(entry.appendParent);
        }
        return null;
    }

    @NotNull
    protected List<ExecutableEntry> getChainAnchorsFor(@NotNull ExecutableEntry entry) {
        LinkedHashSet<ExecutableEntry> anchors = new LinkedHashSet<>();
        ExecutableEntry cursor = entry;
        while (cursor != null) {
            ExecutableEntry anchor = this.getChainAnchor(cursor);
            while (anchor != null) {
                anchors.add(anchor);
                if (anchor.appendParent != null) {
                    anchor = this.findEntryForExecutable(anchor.appendParent);
                } else {
                    anchor = null;
                }
            }
            if ((cursor.parentBlock != null) && (cursor.parentBlock != cursor.executable)) {
                cursor = this.findEntryForExecutable(cursor.parentBlock);
            } else {
                cursor = null;
            }
        }
        return new ArrayList<>(anchors);
    }

    protected int getChainBarX(@NotNull ExecutableEntry entry) {
        ExecutableEntry anchor = this.getChainAnchor(entry);
        ExecutableEntry reference = (anchor != null) ? anchor : entry;
        return reference.getX() + (ExecutableEntry.INDENT_X_OFFSET * reference.indentLevel) + CHAIN_BAR_OFFSET;
    }

    protected void addWhileChainEntries(@NotNull ExecutableEntry anchor, @NotNull List<ExecutableEntry> entries) {
        if (!(anchor.executable instanceof WhileExecutableBlock)) {
            return;
        }
        List<ScrollAreaEntry> scrollEntries = this.scriptEntriesScrollArea.getEntries();
        int anchorIndex = scrollEntries.indexOf(anchor);
        if (anchorIndex < 0) {
            return;
        }
        for (int i = anchorIndex + 1; i < scrollEntries.size(); i++) {
            ScrollAreaEntry raw = scrollEntries.get(i);
            if (!(raw instanceof ExecutableEntry candidate)) {
                continue;
            }
            if (!this.isEntryDescendantOf(candidate, anchor)) {
                break;
            }
            if (candidate.executable instanceof IfExecutableBlock ||
                    candidate.executable instanceof ElseIfExecutableBlock ||
                    candidate.executable instanceof ElseExecutableBlock ||
                    candidate.executable instanceof WhileExecutableBlock) {
                continue;
            }
            if (!entries.contains(candidate)) {
                entries.add(candidate);
            }
        }
    }

    @NotNull
    protected List<ExecutableEntry> collectChainWithSubChains(@NotNull ExecutableEntry entry) {
        LinkedHashSet<ExecutableEntry> expanded = new LinkedHashSet<>();
        ArrayDeque<ExecutableEntry> queue = new ArrayDeque<>();
        this.enqueueChainEntries(entry, expanded, queue);
        List<ScrollAreaEntry> scrollEntries = this.scriptEntriesScrollArea.getEntries();
        while (!queue.isEmpty()) {
            ExecutableEntry current = queue.poll();
            for (ScrollAreaEntry raw : scrollEntries) {
                if (!(raw instanceof ExecutableEntry candidate)) {
                    continue;
                }
                if (expanded.contains(candidate)) {
                    continue;
                }
                if (this.isEntryDescendantOf(candidate, current)) {
                    if (candidate.executable instanceof AbstractExecutableBlock) {
                        this.enqueueChainEntries(candidate, expanded, queue);
                    } else {
                        expanded.add(candidate);
                    }
                }
            }
        }
        return new ArrayList<>(expanded);
    }

    protected void enqueueChainEntries(@NotNull ExecutableEntry source, @NotNull LinkedHashSet<ExecutableEntry> expanded, @NotNull ArrayDeque<ExecutableEntry> queue) {
        for (ExecutableEntry chainEntry : this.getStatementChainOf(source)) {
            if (expanded.add(chainEntry) && (chainEntry.executable instanceof AbstractExecutableBlock)) {
                queue.add(chainEntry);
            }
        }
    }

    protected boolean isEntryDescendantOf(@NotNull ExecutableEntry entry, @NotNull ExecutableEntry potentialAncestor) {
        AbstractExecutableBlock parentBlock = entry.parentBlock;
        while (parentBlock != null) {
            ExecutableEntry parentEntry = this.findEntryForExecutable(parentBlock);
            if (parentEntry == null) {
                return false;
            }
            if (parentEntry == potentialAncestor) {
                return true;
            }
            parentBlock = parentEntry.parentBlock;
        }
        return false;
    }

    @Nullable
    protected ExecutableEntry getDragHoveredEntry() {
        ExecutableEntry draggedEntry = this.getDraggedEntry();
        if (draggedEntry != null) {
            if ((MouseInput.getMouseY() <= this.scriptEntriesScrollArea.getInnerY()) && (this.scriptEntriesScrollArea.verticalScrollBar.getScroll() == 0.0F)) {
                return BEFORE_FIRST;
            }
            if ((MouseInput.getMouseY() >= (this.scriptEntriesScrollArea.getInnerY() + this.scriptEntriesScrollArea.getInnerHeight())) && (this.scriptEntriesScrollArea.verticalScrollBar.getScroll() == 1.0F)) {
                return AFTER_LAST;
            }
            for (ScrollAreaEntry e : this.scriptEntriesScrollArea.getEntries()) {
                if (e instanceof ExecutableEntry ee) {
                    if ((e.getY() + e.getHeight()) > (this.scriptEntriesScrollArea.getInnerY() + this.scriptEntriesScrollArea.getInnerHeight())) {
                        continue;
                    }
                    if ((ee != draggedEntry) && UIBase.isXYInArea(MouseInput.getMouseX(), MouseInput.getMouseY(), ee.getX(), ee.getY(), ee.getWidth(), ee.getHeight()) && this.scriptEntriesScrollArea.isMouseInsideArea()) {
                        List<ExecutableEntry> statementChain = new ArrayList<>();
                        if (draggedEntry.executable instanceof AbstractExecutableBlock) {
                            statementChain = this.getStatementChainOf(draggedEntry);
                        }
                        if ((draggedEntry.executable instanceof AbstractExecutableBlock) && statementChain.contains(ee)) {
                            return null;
                        }
                        if ((ee.parentBlock != null) && (ee.parentBlock != this.executableBlock)) {
                            ExecutableEntry pb = this.findEntryForExecutable(ee.parentBlock);
                            if ((pb != null) && statementChain.contains(pb)) {
                                return null;
                            }
                        }
                        if ((draggedEntry.executable instanceof AbstractExecutableBlock) && this.isContentOfStatementChain(ee, statementChain)) {
                            return null;
                        }
                        return ee;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    protected ExecutableEntry getDraggedEntry() {
        for (ScrollAreaEntry e : this.scriptEntriesScrollArea.getEntries()) {
            if (e instanceof ExecutableEntry ee) {
                if (ee.dragging) return ee;
            }
        }
        return null;
    }

    @Nullable
    protected ExecutableEntry findEntryForExecutable(Executable executable) {
        for (ScrollAreaEntry e : this.scriptEntriesScrollArea.getEntries()) {
            if (e instanceof ExecutableEntry ee) {
                if (ee.executable == executable) return ee;
            }
        }
        return null;
    }

    protected void focusEntryForExecutable(@NotNull Executable executable, boolean keepViewAnchor) {
        this.focusEntryForExecutable(executable, keepViewAnchor, false);
    }

    protected void focusEntryForExecutable(@NotNull Executable executable, boolean keepViewAnchor, boolean allowSelectionWhileMenuOpen) {
        if (allowSelectionWhileMenuOpen) {
            this.scheduleFocusEntryForExecutable(executable, keepViewAnchor);
            return;
        }
        ExecutableEntry entry = this.findEntryForExecutable(executable);
        if (entry != null) {
            if (!this.isUserNavigatingInRightClickContextMenu()) {
                entry.setSelected(true);
                if (!keepViewAnchor) {
                    this.scrollEntryIntoView(entry);
                }
            } else {
                this.pendingSelectionExecutable = executable;
                this.pendingSelectionKeepViewAnchor = keepViewAnchor;
            }
        }
    }

    protected void scheduleFocusEntryForExecutable(@NotNull Executable executable, boolean keepViewAnchor) {
        MainThreadTaskExecutor.executeInMainThread(() -> {
            if (Minecraft.getInstance().screen == this) {
                this.focusEntryForExecutable(executable, keepViewAnchor, false);
            }
        }, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    protected void withContextMenuSelectionOverride(@NotNull Runnable action) {
        boolean previous = this.contextMenuSelectionOverrideActive;
        this.contextMenuSelectionOverrideActive = true;
        try {
            action.run();
        } finally {
            this.contextMenuSelectionOverrideActive = previous;
        }
    }

    protected void setEntrySelectedRespectingContextMenu(@NotNull ExecutableEntry entry) {
        if (this.isUserNavigatingInRightClickContextMenu()) {
            this.withContextMenuSelectionOverride(() -> entry.setSelected(true));
        } else {
            entry.setSelected(true);
        }
    }

    @Nullable
    protected ExecutableEntry getSelectedEntry() {
        ScrollAreaEntry e = this.scriptEntriesScrollArea.getFocusedEntry();
        if (e instanceof ExecutableEntry ee) {
            return ee;
        }
        return null;
    }

    @Nullable
    private MoveTarget calculateMoveTarget(@NotNull ExecutableEntry entry, @NotNull ExecutableEntry moveAfter) {
        AbstractExecutableBlock currentParent = entry.getParentBlock();
        List<Executable> currentList = currentParent.getExecutables();
        int currentIndex = currentList.indexOf(entry.executable);
        if (currentIndex < 0) {
            return null;
        }

        AbstractExecutableBlock targetParent;
        int targetIndex;

        if (moveAfter == BEFORE_FIRST) {
            targetParent = this.executableBlock;
            targetIndex = 0;
        } else if (moveAfter == AFTER_LAST) {
            targetParent = this.executableBlock;
            targetIndex = this.executableBlock.getExecutables().size();
        } else if (moveAfter.executable instanceof AbstractExecutableBlock block) {
            targetParent = block;
            targetIndex = 0;
        } else {
            targetParent = moveAfter.getParentBlock();
            List<Executable> targetList = targetParent.getExecutables();
            int moveAfterIndex = targetList.indexOf(moveAfter.executable);
            targetIndex = (moveAfterIndex >= 0) ? moveAfterIndex + 1 : targetList.size();
        }

        if (targetParent == currentParent) {
            if (targetIndex > currentIndex) {
                targetIndex--;
            }
            if (targetIndex == currentIndex) {
                return null;
            }
        }

        return new MoveTarget(targetParent, Math.max(0, targetIndex));
    }

    @Nullable
    protected ExecutableEntry getValidMoveToEntryBefore(@NotNull ExecutableEntry entry, boolean ignoreValidityChecks) {
        if (entry == BEFORE_FIRST) return BEFORE_FIRST;
        int index = this.scriptEntriesScrollArea.getEntries().size();
        boolean foundEntry = false;
        boolean foundValidMoveTo = false;
        for (ScrollAreaEntry e : Lists.reverse(this.scriptEntriesScrollArea.getEntries())) {
            if (e instanceof ExecutableEntry ee) {
                index--;
                if (e == entry) {
                    foundEntry = true;
                    continue;
                }
                if (!ignoreValidityChecks) {
                    List<ExecutableEntry> statementChain = new ArrayList<>();
                    if (entry.executable instanceof AbstractExecutableBlock) {
                        statementChain = this.getStatementChainOf(entry);
                    }
                    if ((entry.executable instanceof AbstractExecutableBlock) && statementChain.contains(ee)) {
                        continue;
                    }
                    if ((ee.parentBlock != null) && (ee.parentBlock != this.executableBlock)) {
                        ExecutableEntry pb = this.findEntryForExecutable(ee.parentBlock);
                        if ((pb != null) && statementChain.contains(pb)) {
                            continue;
                        }
                    }
                    if ((entry.executable instanceof AbstractExecutableBlock) && this.isContentOfStatementChain(ee, statementChain)) {
                        continue;
                    }
                }
                if (foundEntry) {
                    foundValidMoveTo = true;
                    break;
                }
            }
        }
        if (!foundValidMoveTo) return null;
        ScrollAreaEntry e = this.scriptEntriesScrollArea.getEntry(index);
        if (e instanceof ExecutableEntry ee) return ee;
        return null;
    }

    @Nullable
    protected ExecutableEntry getValidMoveToEntryAfter(@NotNull ExecutableEntry entry, boolean ignoreValidityChecks) {
        if (entry == AFTER_LAST) return AFTER_LAST;
        int index = -1;
        boolean foundEntry = false;
        boolean foundValidMoveTo = false;
        for (ScrollAreaEntry e : this.scriptEntriesScrollArea.getEntries()) {
            if (e instanceof ExecutableEntry ee) {
                index++;
                if (e == entry) {
                    foundEntry = true;
                    continue;
                }
                if (!ignoreValidityChecks) {
                    List<ExecutableEntry> statementChain = new ArrayList<>();
                    if (entry.executable instanceof AbstractExecutableBlock) {
                        statementChain = this.getStatementChainOf(entry);
                    }
                    if ((entry.executable instanceof AbstractExecutableBlock) && statementChain.contains(ee)) {
                        continue;
                    }
                    if ((ee.parentBlock != null) && (ee.parentBlock != this.executableBlock)) {
                        ExecutableEntry pb = this.findEntryForExecutable(ee.parentBlock);
                        if ((pb != null) && statementChain.contains(pb)) {
                            continue;
                        }
                    }
                    if ((entry.executable instanceof AbstractExecutableBlock) && this.isContentOfStatementChain(ee, statementChain)) {
                        continue;
                    }
                }
                if (foundEntry) {
                    foundValidMoveTo = true;
                    break;
                }
            }
        }
        if (!foundValidMoveTo) return null;
        ScrollAreaEntry e = this.scriptEntriesScrollArea.getEntry(index);
        if (e instanceof ExecutableEntry ee) return ee;
        return null;
    }

    protected boolean moveAfter(@NotNull ExecutableEntry entry, @NotNull ExecutableEntry moveAfter) {
        MoveTarget target = this.calculateMoveTarget(entry, moveAfter);
        if (target == null) {
            return false;
        }

        this.createUndoPoint();

        entry.getParentBlock().getExecutables().remove(entry.executable);
        target.parent().getExecutables().add(target.index(), entry.executable);

        this.updateActionInstanceScrollArea(true);
        ExecutableEntry newEntry = this.findEntryForExecutable(entry.executable);
        if (newEntry != null) {
            this.setEntrySelectedRespectingContextMenu(newEntry);
        }
        return true;
    }

    protected void moveUp(ExecutableEntry entry) {
        if (entry != null) {
            if ((entry.executable instanceof ActionInstance) || (entry.executable instanceof IfExecutableBlock) || (entry.executable instanceof WhileExecutableBlock)) {
                boolean manualUpdate = false;
                if (this.scriptEntriesScrollArea.getEntries().indexOf(entry) == 1) {
                    this.moveAfter(entry, BEFORE_FIRST);
                } else {
                    if ((entry.getParentBlock() != this.executableBlock) && ((entry.getParentBlock() instanceof ElseIfExecutableBlock) || (entry.getParentBlock() instanceof ElseExecutableBlock)) && (entry.getParentBlock().getExecutables().indexOf(entry.executable) == 0)) {
                        ExecutableEntry parentBlock = this.findEntryForExecutable(entry.getParentBlock());
                        if ((parentBlock != null) && (parentBlock.appendParent != null)) {
                            this.createUndoPoint();
                            entry.getParentBlock().getExecutables().remove(entry.executable);
                            parentBlock.appendParent.getExecutables().add(entry.executable);
                            manualUpdate = true;
                        }
                    } else if ((entry.getParentBlock() != this.executableBlock) && (entry.getParentBlock() instanceof IfExecutableBlock) && (entry.getParentBlock().getExecutables().indexOf(entry.executable) == 0)) {
                        ExecutableEntry parentBlock = this.findEntryForExecutable(entry.getParentBlock());
                        if (parentBlock != null) {
                            int parentIndex = Math.max(0, parentBlock.getParentBlock().getExecutables().indexOf(parentBlock.executable));
                            if (parentIndex >= 0) {
                                this.createUndoPoint();
                                entry.getParentBlock().getExecutables().remove(entry.executable);
                                parentBlock.getParentBlock().getExecutables().add(parentIndex, entry.executable);
                                manualUpdate = true;
                            }
                        }
                    } else {
                        ExecutableEntry before = this.getValidMoveToEntryBefore(entry, false);
                        if (before != null) {
                            boolean isMovable = (entry.executable instanceof IfExecutableBlock) || (entry.executable instanceof ActionInstance);
                            if (isMovable && (before.executable instanceof AbstractExecutableBlock b) && (entry.getParentBlock() != b)) {
                                this.moveAfter(entry, before);
                            } else if (isMovable && !(before.executable instanceof AbstractExecutableBlock) && (before.getParentBlock().getExecutables().indexOf(before.executable) == before.getParentBlock().getExecutables().size()-1)) {
                                this.moveAfter(entry, before);
                            } else {
                                ExecutableEntry beforeBefore = this.getValidMoveToEntryBefore(before, true);
                                if (beforeBefore != null) {
                                    this.moveAfter(entry, beforeBefore);
                                }
                            }
                        }
                    }
                }
                if (manualUpdate) {
                    this.updateActionInstanceScrollArea(true);
                    //Re-select entry after updating scroll area
                    ExecutableEntry newEntry = this.findEntryForExecutable(entry.executable);
                    if (newEntry != null) {
                        this.setEntrySelectedRespectingContextMenu(newEntry);
                    }
                }
                return;
            }
            if (entry.executable instanceof ElseIfExecutableBlock ei) {
                AbstractExecutableBlock entryAppendParent = entry.appendParent;
                if (entryAppendParent != null) {
                    ExecutableEntry appendParentEntry = this.findEntryForExecutable(entryAppendParent);
                    if (appendParentEntry != null) {
                        AbstractExecutableBlock parentOfParent = appendParentEntry.appendParent;
                        if (parentOfParent != null) {
                            this.createUndoPoint();
                            entryAppendParent.setAppendedBlock(ei.getAppendedBlock());
                            ei.setAppendedBlock(entryAppendParent);
                            parentOfParent.setAppendedBlock(ei);
                        }
                    }
                }
            }
            this.updateActionInstanceScrollArea(true);
            //Re-select entry after updating scroll area
            ExecutableEntry newEntry = this.findEntryForExecutable(entry.executable);
            if (newEntry != null) {
                this.setEntrySelectedRespectingContextMenu(newEntry);
            }
        }
    }

    protected void moveDown(ExecutableEntry entry) {
        if (entry != null) {
            if ((entry.executable instanceof ActionInstance) || (entry.executable instanceof IfExecutableBlock) || (entry.executable instanceof WhileExecutableBlock)) {
                boolean manualUpdate = false;
                if ((entry.getParentBlock() != this.executableBlock) && (entry.getParentBlock().getAppendedBlock() == null) && (entry.getParentBlock().getExecutables().indexOf(entry.executable) == entry.getParentBlock().getExecutables().size()-1)) {
                    ExecutableEntry parentBlock = this.findEntryForExecutable(entry.getParentBlock());
                    if (parentBlock != null) {
                        int parentIndex = -1;
                        if (parentBlock.executable instanceof IfExecutableBlock) {
                            parentIndex = Math.max(0, parentBlock.getParentBlock().getExecutables().indexOf(parentBlock.executable));
                        } else {
                            List<ExecutableEntry> chain = this.getStatementChainOf(parentBlock);
                            if (!chain.isEmpty()) {
                                parentIndex = chain.get(0).getParentBlock().getExecutables().indexOf(chain.get(0).executable);
                            }
                        }
                        if (parentIndex != -1) {
                            this.createUndoPoint();
                            entry.getParentBlock().getExecutables().remove(entry.executable);
                            parentBlock.getParentBlock().getExecutables().add(parentIndex+1, entry.executable);
                            manualUpdate = true;
                        }
                    }
                } else {
                    ExecutableEntry after = this.getValidMoveToEntryAfter(entry, false);
                    if (after != null) {
                        this.moveAfter(entry, after);
                    }
                }
                if (manualUpdate) {
                    this.updateActionInstanceScrollArea(true);
                    //Re-select entry after updating scroll area
                    ExecutableEntry newEntry = this.findEntryForExecutable(entry.executable);
                    if (newEntry != null) {
                        this.setEntrySelectedRespectingContextMenu(newEntry);
                    }
                }
                return;
            }
            if (entry.executable instanceof ElseIfExecutableBlock ei) {
                AbstractExecutableBlock entryAppendChild = ei.getAppendedBlock();
                AbstractExecutableBlock entryAppendParent = entry.appendParent;
                if ((entryAppendChild instanceof ElseIfExecutableBlock) && (entryAppendParent != null)) {
                    this.createUndoPoint();
                    ei.setAppendedBlock(entryAppendChild.getAppendedBlock());
                    entryAppendChild.setAppendedBlock(ei);
                    entryAppendParent.setAppendedBlock(entryAppendChild);
                }
            }
            this.updateActionInstanceScrollArea(true);
            //Re-select entry after updating scroll area
            ExecutableEntry newEntry = this.findEntryForExecutable(entry.executable);
            if (newEntry != null) {
                this.setEntrySelectedRespectingContextMenu(newEntry);
            }
        }
    }

    protected void handleContextMenuMove(@NotNull ExecutableEntry entry, boolean moveUp) {
        this.scriptEntriesScrollArea.updateEntries(null);
        int previousEntryY = entry.getY();
        Executable executable = entry.executable;
        if (moveUp) {
            this.moveUp(entry);
        } else {
            this.moveDown(entry);
        }
        ExecutableEntry newEntry = this.findEntryForExecutable(executable);
        if (newEntry != null) {
            this.adjustScrollToKeepEntryInPlace(previousEntryY, newEntry);
            this.setEntrySelectedRespectingContextMenu(newEntry);
            this.scheduleFocusEntryForExecutable(executable, true);
        }
    }

    protected void processPendingSelection() {
        if ((this.pendingSelectionExecutable != null) && !this.isUserNavigatingInRightClickContextMenu()) {
            Executable pending = this.pendingSelectionExecutable;
            boolean keep = this.pendingSelectionKeepViewAnchor;
            this.pendingSelectionExecutable = null;
            this.pendingSelectionKeepViewAnchor = false;
            ExecutableEntry entry = this.findEntryForExecutable(pending);
            if (entry != null) {
                entry.setSelected(true);
                if (!keep) {
                    this.scrollEntryIntoView(entry);
                }
            }
        }
    }

    protected void adjustScrollToKeepEntryInPlace(int previousEntryY, @NotNull ExecutableEntry entry) {
        this.scriptEntriesScrollArea.updateEntries(null);
        if (previousEntryY == Integer.MIN_VALUE) {
            return;
        }
        int newEntryY = entry.getY();
        int delta = newEntryY - previousEntryY;
        if (delta == 0) {
            return;
        }
        float totalScrollHeight = (float) this.scriptEntriesScrollArea.getTotalScrollHeight();
        if (totalScrollHeight <= 0.0F) {
            return;
        }
        float currentScroll = this.scriptEntriesScrollArea.verticalScrollBar.getScroll();
        float adjustedScroll = Mth.clamp(currentScroll + ((float) delta / totalScrollHeight), 0.0F, 1.0F);
        if (adjustedScroll != currentScroll) {
            this.scriptEntriesScrollArea.verticalScrollBar.setScroll(adjustedScroll);
            this.scriptEntriesScrollArea.updateEntries(null);
        }
    }

    protected void updateActionInstanceScrollArea(boolean keepScroll) {
        this.finishInlineNameEditing(true);
        this.finishInlineValueEditing(true);

        this.minimapSegments.clear();
        this.minimapHoveredEntry = null;
        this.minimapHoveredPrimaryChainEntries = Collections.emptyList();
        this.minimapHoveredStatementChainEntries = Collections.emptyList();
        this.hoveredEntry = null;
        this.hoveredPrimaryChainEntries = Collections.emptyList();
        this.hoveredStatementChainEntries = Collections.emptyList();

        for (ScrollAreaEntry e : this.scriptEntriesScrollArea.getEntries()) {
            if (e instanceof ExecutableEntry ee) {
                ee.leftMouseDownDragging = false;
                ee.dragging = false;
            }
        }

        float oldScrollVertical = this.scriptEntriesScrollArea.verticalScrollBar.getScroll();
        float oldScrollHorizontal = this.scriptEntriesScrollArea.horizontalScrollBar.getScroll();

        this.scriptEntriesScrollArea.clearEntries();

        this.addExecutableToEntries(-1, this.executableBlock, null, null);

        if (keepScroll) {
            this.scriptEntriesScrollArea.verticalScrollBar.setScroll(oldScrollVertical);
            this.scriptEntriesScrollArea.horizontalScrollBar.setScroll(oldScrollHorizontal);
        }

    }

    protected void addExecutableRelativeToSelection(@NotNull Executable executable, @Nullable ExecutableEntry selectionReference) {
        if (selectionReference == BEFORE_FIRST) {
            this.executableBlock.addExecutable(executable);
            List<Executable> rootExecutables = this.executableBlock.getExecutables();
            if (rootExecutables.remove(executable)) {
                rootExecutables.add(0, executable);
            }
            return;
        }
        if (selectionReference == AFTER_LAST) {
            this.executableBlock.addExecutable(executable);
            return;
        }
        if (selectionReference == null) {
            this.executableBlock.addExecutable(executable);
            return;
        }
        if (selectionReference.executable instanceof AbstractExecutableBlock block) {
            block.addExecutable(executable);
            return;
        }
        AbstractExecutableBlock parentBlock = selectionReference.getParentBlock();
        List<Executable> executables = parentBlock.getExecutables();
        int selectedIndex = executables.indexOf(selectionReference.executable);
        parentBlock.addExecutable(executable);
        if ((selectedIndex >= 0) && executables.remove(executable)) {
            int insertIndex = Math.min(selectedIndex + 1, executables.size());
            executables.add(insertIndex, executable);
        }
    }

    protected void addExecutableToEntries(int level, Executable executable, @Nullable AbstractExecutableBlock appendParent, @Nullable AbstractExecutableBlock parentBlock) {

        if (level >= 0) {
            ExecutableEntry entry = new ExecutableEntry(this.scriptEntriesScrollArea, executable, 14, level);
            entry.appendParent = appendParent;
            entry.parentBlock = parentBlock;
            this.scriptEntriesScrollArea.addEntry(entry);
        }

        if (executable instanceof AbstractExecutableBlock b) {
            boolean skipChildren = false;
            if (b instanceof FolderExecutableBlock folder && folder.isCollapsed()) {
                skipChildren = true;
            } else if (b instanceof IfExecutableBlock ifBlock && ifBlock.isCollapsed()) {
                skipChildren = true;
            } else if (b instanceof WhileExecutableBlock whileBlock && whileBlock.isCollapsed()) {
                skipChildren = true;
            }
            if (!skipChildren) {
                for (Executable e : b.getExecutables()) {
                    this.addExecutableToEntries(level+1, e, null, b);
                }
            }
            if (!skipChildren && b.getAppendedBlock() != null) {
                this.addExecutableToEntries(level, b.getAppendedBlock(), b, parentBlock);
            }
        }

    }

    @Nullable
    private Executable findExecutableByIdentifier(@NotNull AbstractExecutableBlock start, @NotNull String identifier) {
        if (identifier.equals(start.getIdentifier())) {
            return start;
        }
        for (Executable executable : start.getExecutables()) {
            if (identifier.equals(executable.getIdentifier())) {
                return executable;
            }
            if (executable instanceof AbstractExecutableBlock block) {
                Executable nested = this.findExecutableByIdentifier(block, identifier);
                if (nested != null) {
                    return nested;
                }
            }
        }
        AbstractExecutableBlock appended = start.getAppendedBlock();
        if (appended != null) {
            Executable nested = this.findExecutableByIdentifier(appended, identifier);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    protected void openRightClickContextMenuAt(float x, float y, @Nullable List<String> entryPath) {
        if (this.rightClickContextMenu == null) {
            return;
        }
        this.rightClickContextMenuLastOpenX = x;
        this.rightClickContextMenuLastOpenY = y;
        List<String> path = (entryPath != null && !entryPath.isEmpty()) ? new ArrayList<>(entryPath) : null;
        this.rightClickContextMenu.openMenuAt(x, y, path);
    }

    protected boolean isInsideActionsScrollArea(int mouseX, int mouseY) {
        return UIBase.isXYInArea(mouseX, mouseY, this.scriptEntriesScrollArea.getXWithBorder(), this.scriptEntriesScrollArea.getYWithBorder(), this.scriptEntriesScrollArea.getWidthWithBorder(), this.scriptEntriesScrollArea.getHeightWithBorder());
    }

    protected boolean canAppendConditionalBlock(@Nullable ExecutableEntry entry) {
        if (entry == null) {
            return false;
        }
        return (entry.executable instanceof IfExecutableBlock) || (entry.executable instanceof ElseIfExecutableBlock);
    }

    protected boolean canAppendElseBlock(@Nullable ExecutableEntry entry) {
        if ((entry == null) || !(entry.executable instanceof AbstractExecutableBlock block)) {
            return false;
        }
        if (!(entry.executable instanceof IfExecutableBlock) && !(entry.executable instanceof ElseIfExecutableBlock)) {
            return false;
        }
        return this.findAppendElseTarget(block) != null;
    }

    @Nullable
    protected AbstractExecutableBlock findAppendElseTarget(@NotNull AbstractExecutableBlock block) {
        AbstractExecutableBlock current = block;
        AbstractExecutableBlock appended = current.getAppendedBlock();
        while (appended != null) {
            if (appended instanceof ElseExecutableBlock) {
                return null;
            }
            current = appended;
            appended = current.getAppendedBlock();
        }
        return current;
    }

    protected boolean canEditEntry(@Nullable ExecutableEntry entry) {
        if ((entry == null) || (entry.executable instanceof ElseExecutableBlock)) {
            return false;
        }
        if (entry.executable instanceof FolderExecutableBlock) {
            return false;
        }
        if ((entry.executable instanceof ActionInstance i) && !i.action.hasValue()) {
            return false;
        }
        return true;
    }

    protected boolean deleteSelectedEntryDirectly() {
        ExecutableEntry selected = this.getSelectedEntry();
        if (selected == null) {
            return false;
        }
        Executable nextExecutable = null;
        List<ScrollAreaEntry> currentEntries = this.scriptEntriesScrollArea.getEntries();
        int selectedIndex = currentEntries.indexOf(selected);
        if (selectedIndex != -1) {
            for (int i = selectedIndex + 1; i < currentEntries.size(); i++) {
                ScrollAreaEntry entry = currentEntries.get(i);
                if ((entry instanceof ExecutableEntry ee) && (ee != selected)) {
                    nextExecutable = ee.executable;
                    break;
                }
            }
            if (nextExecutable == null) {
                for (int i = selectedIndex - 1; i >= 0; i--) {
                    ScrollAreaEntry entry = currentEntries.get(i);
                    if ((entry instanceof ExecutableEntry ee) && (ee != selected)) {
                        nextExecutable = ee.executable;
                        break;
                    }
                }
            }
        }
        this.createUndoPoint();
        if (selected.appendParent != null) {
            selected.appendParent.setAppendedBlock(null);
        }
        selected.getParentBlock().getExecutables().remove(selected.executable);
        this.updateActionInstanceScrollArea(true);
        if (nextExecutable != null) {
            this.focusEntryForExecutable(nextExecutable, true);
        }
        return true;
    }

    protected boolean copySelectedAction() {
        ExecutableEntry selected = this.getSelectedEntry();
        if ((selected == null) || !(selected.executable instanceof ActionInstance instance)) {
            return false;
        }
        this.clipboardActionInstance = instance.copy(true);
        return true;
    }

    protected boolean pasteCopiedAction(@Nullable ExecutableEntry selectionReference) {
        if (this.clipboardActionInstance == null) {
            return false;
        }
        ActionInstance instance = this.clipboardActionInstance.copy(true);
        this.finalizeActionAddition(instance, selectionReference);
        return true;
    }

    protected boolean moveSelectedEntry(boolean moveUp) {
        ExecutableEntry selected = this.getSelectedEntry();
        if (selected == null) {
            return false;
        }
        this.handleContextMenuMove(selected, moveUp);
        return true;
    }

    protected boolean selectAdjacentEntry(boolean moveDown) {
        List<ScrollAreaEntry> entries = this.scriptEntriesScrollArea.getEntries();
        if (entries.isEmpty()) {
            return false;
        }
        ExecutableEntry selected = this.getSelectedEntry();
        int targetIndex;
        if (selected == null) {
            targetIndex = moveDown ? 0 : entries.size() - 1;
        } else {
            int currentIndex = entries.indexOf(selected);
            if (currentIndex == -1) {
                return false;
            }
            int desiredIndex = currentIndex + (moveDown ? 1 : -1);
            desiredIndex = Mth.clamp(desiredIndex, 0, entries.size() - 1);
            if (desiredIndex == currentIndex) {
                return false;
            }
            targetIndex = desiredIndex;
        }
        ScrollAreaEntry target = entries.get(targetIndex);
        if (target instanceof ExecutableEntry entry) {
            entry.setSelected(true);
            this.scrollEntryIntoView(entry);
            return true;
        }
        return false;
    }

    public boolean isUserNavigatingInRightClickContextMenu() {
        return (this.rightClickContextMenu != null) && this.rightClickContextMenu.isUserNavigatingInMenu();
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
        ScriptSnapshot snapshot = this.undoHistory.pop();
        this.redoHistory.push(this.captureCurrentState());
        this.trimHistory(this.redoHistory);
        this.applySnapshot(snapshot);
        return true;
    }

    private boolean redo() {
        if (!this.canRedo()) {
            return false;
        }
        ScriptSnapshot snapshot = this.redoHistory.pop();
        this.undoHistory.push(this.captureCurrentState());
        this.trimHistory(this.undoHistory);
        this.applySnapshot(snapshot);
        return true;
    }

    private void createUndoPoint() {
        if (this.suppressHistoryCapture) {
            return;
        }
        ScriptSnapshot snapshot = this.captureCurrentState();
        this.undoHistory.push(snapshot);
        this.trimHistory(this.undoHistory);
        this.redoHistory.clear();
    }

    private void trimHistory(@NotNull Deque<ScriptSnapshot> history) {
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    @NotNull
    private ScriptSnapshot captureCurrentState() {
        GenericExecutableBlock snapshotBlock = this.executableBlock.copy(false);
        float verticalScroll = this.scriptEntriesScrollArea.verticalScrollBar.getScroll();
        float horizontalScroll = this.scriptEntriesScrollArea.horizontalScrollBar.getScroll();
        ExecutableEntry selected = this.getSelectedEntry();
        String selectedId = (selected != null) ? selected.executable.getIdentifier() : null;
        String targetId = (this.contextMenuTargetExecutable != null) ? this.contextMenuTargetExecutable.getIdentifier() : null;
        return new ScriptSnapshot(snapshotBlock, verticalScroll, horizontalScroll, selectedId, targetId);
    }

    private void applySnapshot(@NotNull ScriptSnapshot snapshot) {
        this.suppressHistoryCapture = true;
        try {
            this.executableBlock = snapshot.block.copy(false);
            this.updateActionInstanceScrollArea(false);
            this.scriptEntriesScrollArea.verticalScrollBar.setScroll(Mth.clamp(snapshot.verticalScroll, 0.0F, 1.0F));
            this.scriptEntriesScrollArea.horizontalScrollBar.setScroll(Mth.clamp(snapshot.horizontalScroll, 0.0F, 1.0F));
            this.scriptEntriesScrollArea.updateEntries(null);
            if (snapshot.selectedExecutableId != null) {
                Executable executable = this.findExecutableByIdentifier(this.executableBlock, snapshot.selectedExecutableId);
                if (executable != null) {
                    this.focusEntryForExecutable(executable, true, false);
                }
            }
            if (snapshot.contextMenuTargetExecutableId != null) {
                this.contextMenuTargetExecutable = this.findExecutableByIdentifier(this.executableBlock, snapshot.contextMenuTargetExecutableId);
            } else {
                this.contextMenuTargetExecutable = null;
            }
        } finally {
            this.suppressHistoryCapture = false;
        }
    }

    public class ExecutableEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;
        public static final int INDENT_X_OFFSET = 20;
        public static final int STATEMENT_CONTENT_OFFSET = 4;
        private static final int COLLAPSE_TOGGLE_SIZE = 8;

        @NotNull
        public Executable executable;
        @Nullable
        public AbstractExecutableBlock parentBlock;
        @Nullable
        public AbstractExecutableBlock appendParent;
        public final int lineHeight;
        public Font font = Minecraft.getInstance().font;
        public int indentLevel;
        public boolean leftMouseDownDragging = false;
        public double leftMouseDownDraggingPosX = 0;
        public double leftMouseDownDraggingPosY = 0;
        public boolean dragging = false;
        private String cachedThemeIdentifier = "";

        private MutableComponent displayNameComponent;
        private MutableComponent valueComponent;
        @Nullable
        private MutableComponent folderLabelComponent;
        @Nullable
        private MutableComponent folderNameComponent;
        @Nullable
        private MutableComponent folderCollapsedSuffixComponent;
        @Nullable
        private MutableComponent valueLabelComponent;
        @Nullable
        private MutableComponent valueOnlyComponent;
        private long lastValueClickTime = 0L;
        private long lastNameClickTime = 0L;

        public ExecutableEntry(@NotNull ScrollArea parentScrollArea, @NotNull Executable executable, int lineHeight, int indentLevel) {

            super(parentScrollArea, 100, 30);
            this.setPlayClickSound(false);
            this.executable = executable;
            this.lineHeight = lineHeight;
            this.indentLevel = indentLevel;

            this.rebuildComponents();

            this.applyThemeBackground(true);
            this.setWidth(this.calculateWidth());
            if (this.executable instanceof AbstractExecutableBlock) {
                this.setHeight(lineHeight + (HEADER_FOOTER_HEIGHT * 2));
            } else {
                this.setHeight((lineHeight * 2) + (HEADER_FOOTER_HEIGHT * 2));
            }

        }

        private void applyThemeBackground(boolean force) {
            UIColorTheme theme = UIBase.getUIColorTheme();
            String themeIdentifier = theme.getIdentifier();
            if (!force && themeIdentifier.equals(this.cachedThemeIdentifier)) {
                return;
            }
            this.cachedThemeIdentifier = themeIdentifier;
            this.rebuildComponents();

            Color idle = theme.actions_entry_background_color_action.getColor();
            Color hover = theme.actions_entry_background_color_action_hover.getColor();

            if (this.executable instanceof IfExecutableBlock) {
                idle = theme.actions_entry_background_color_if.getColor();
                hover = theme.actions_entry_background_color_if_hover.getColor();
            } else if (this.executable instanceof ElseIfExecutableBlock) {
                idle = theme.actions_entry_background_color_else_if.getColor();
                hover = theme.actions_entry_background_color_else_if_hover.getColor();
            } else if (this.executable instanceof ElseExecutableBlock) {
                idle = theme.actions_entry_background_color_else.getColor();
                hover = theme.actions_entry_background_color_else_hover.getColor();
            } else if (this.executable instanceof WhileExecutableBlock) {
                idle = theme.actions_entry_background_color_while.getColor();
                hover = theme.actions_entry_background_color_while_hover.getColor();
            } else if (this.executable instanceof FolderExecutableBlock) {
                idle = theme.actions_entry_background_color_folder.getColor();
                hover = theme.actions_entry_background_color_folder_hover.getColor();
            } else if (this.executable instanceof AbstractExecutableBlock) {
                idle = theme.actions_entry_background_color_generic_block.getColor();
                hover = theme.actions_entry_background_color_generic_block_hover.getColor();
            }

            this.setBackgroundColorIdle(idle);
            this.setBackgroundColorHover(hover);
        }

        @SuppressWarnings("all")
        private void rebuildComponents() {
            UIColorTheme theme = UIBase.getUIColorTheme();
            this.folderLabelComponent = null;
            this.folderNameComponent = null;
            this.folderCollapsedSuffixComponent = null;
            this.valueLabelComponent = null;
            this.valueOnlyComponent = null;
            if (this.executable instanceof ActionInstance i) {
                this.displayNameComponent = i.action.getActionDisplayName().copy().setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                this.updateValueComponent();
            } else if (this.executable instanceof IfExecutableBlock b) {
                String requirements = "";
                for (LoadingRequirementGroup g : b.condition.getGroups()) {
                    if (!requirements.isEmpty()) requirements += ", ";
                    requirements += g.identifier;
                }
                for (LoadingRequirementInstance i : b.condition.getInstances()) {
                    if (!requirements.isEmpty()) requirements += ", ";
                    requirements += i.requirement.getDisplayName();
                }
                MutableComponent display = Component.translatable("fancymenu.actions.blocks.if", Component.literal(requirements)).setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                if (b.isCollapsed()) {
                    display = display.append(this.createCollapsedSuffixComponent(theme));
                }
                this.displayNameComponent = display;
                this.valueComponent = Component.empty();
            } else if (this.executable instanceof ElseIfExecutableBlock b) {
                String requirements = "";
                for (LoadingRequirementGroup g : b.condition.getGroups()) {
                    if (!requirements.isEmpty()) requirements += ", ";
                    requirements += g.identifier;
                }
                for (LoadingRequirementInstance i : b.condition.getInstances()) {
                    if (!requirements.isEmpty()) requirements += ", ";
                    requirements += i.requirement.getDisplayName();
                }
                this.displayNameComponent = Component.translatable("fancymenu.actions.blocks.else_if", Component.literal(requirements)).setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                this.valueComponent = Component.empty();
            } else if (this.executable instanceof ElseExecutableBlock) {
                this.displayNameComponent = Component.translatable("fancymenu.actions.blocks.else").setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                this.valueComponent = Component.empty();
            } else if (this.executable instanceof WhileExecutableBlock b) {
                String requirements = "";
                for (LoadingRequirementGroup g : b.condition.getGroups()) {
                    if (!requirements.isEmpty()) requirements += ", ";
                    requirements += g.identifier;
                }
                for (LoadingRequirementInstance i : b.condition.getInstances()) {
                    if (!requirements.isEmpty()) requirements += ", ";
                    requirements += i.requirement.getDisplayName();
                }
                MutableComponent display = Component.translatable("fancymenu.actions.blocks.while", Component.literal(requirements)).setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                if (b.isCollapsed()) {
                    display = display.append(this.createCollapsedSuffixComponent(theme));
                }
                this.displayNameComponent = display;
                this.valueComponent = Component.empty();
            } else if (this.executable instanceof FolderExecutableBlock folder) {
                MutableComponent labelComponent = Component.literal(I18n.get("fancymenu.actions.blocks.folder.display", "")).setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                MutableComponent nameComponent = Component.literal(folder.getName()).setStyle(Style.EMPTY.withColor(theme.element_label_color_normal.getColorInt()));
                this.folderLabelComponent = labelComponent;
                this.folderNameComponent = nameComponent;
                MutableComponent display = labelComponent.copy().append(nameComponent.copy());
                if (folder.isCollapsed()) {
                    MutableComponent collapsedComponent = this.createCollapsedSuffixComponent(theme);
                    this.folderCollapsedSuffixComponent = collapsedComponent;
                    display = display.append(collapsedComponent.copy());
                }
                this.displayNameComponent = display;
                this.valueComponent = Component.empty();
            } else {
                this.displayNameComponent = Component.literal("[UNKNOWN EXECUTABLE]").withStyle(ChatFormatting.RED);
                this.valueComponent = Component.empty();
            }
        }

        @Override
        public void setSelected(boolean selected) {
            if (ActionScriptEditorScreen.this.isUserNavigatingInRightClickContextMenu() && !ActionScriptEditorScreen.this.contextMenuSelectionOverrideActive) return;
            super.setSelected(selected);
        }

        public void forceSetSelected(boolean selected) {
            super.setSelected(selected);
        }

        @Override
        public boolean isHovered() {
            if (ActionScriptEditorScreen.this.isUserNavigatingInRightClickContextMenu()) return false;
            return super.isHovered();
        }

        @Override
        public void updateEntry() {
            super.updateEntry();
            // Make the entry not look like it is hovered when navigating in the context menu
            if (!this.isSelected() && ActionScriptEditorScreen.this.isUserNavigatingInRightClickContextMenu()) {
                this.buttonBase.setBackgroundColor(this.backgroundColorIdle, this.backgroundColorIdle, this.backgroundColorIdle, this.backgroundColorIdle, this.backgroundColorIdle, this.backgroundColorIdle);
            }
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.renderInternal(graphics, mouseX, mouseY, partial, true);
        }

        public void renderThumbnail(@NotNull GuiGraphics graphics) {
            this.renderInternal(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, 0.0F, false);
        }

        private void renderInternal(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, boolean interactive) {
            this.applyThemeBackground(false);
            if (interactive) {
                this.handleDragging();
            }
            super.render(graphics, mouseX, mouseY, partial);
            this.renderEntryDecorations(graphics);
        }

        private void renderEntryDecorations(@NotNull GuiGraphics graphics) {
            List<ExecutableEntry> chainAnchors = ActionScriptEditorScreen.this.getChainAnchorsFor(this);
            for (ExecutableEntry anchorEntry : chainAnchors) {
                List<ExecutableEntry> chainEntries = ActionScriptEditorScreen.this.getStatementChainOf(anchorEntry);
                if (chainEntries.size() > 1) {
                    Color chainColor = ActionScriptEditorScreen.this.getChainIndicatorColorFor(anchorEntry);
                    this.renderChainColumn(graphics, chainColor, anchorEntry);
                }
            }

            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);

            int renderX = this.getX() + (INDENT_X_OFFSET * this.indentLevel) + this.getContentOffset();

            UIColorTheme theme = UIBase.getUIColorTheme();

            if (this.executable instanceof FolderExecutableBlock folder) {
                int toggleX = renderX + 5;
                int toggleY = centerYLine1 - (COLLAPSE_TOGGLE_SIZE / 2);
                this.renderCollapseToggle(graphics, toggleX, toggleY, folder.isCollapsed());
                int textX = toggleX + COLLAPSE_TOGGLE_SIZE + 3;
                int textY = centerYLine1 - (this.font.lineHeight / 2);
                if (ActionScriptEditorScreen.this.inlineNameEntry != this) {
                    graphics.drawString(this.font, this.displayNameComponent, textX, textY, -1, false);
                } else {
                    if (this.folderLabelComponent != null) {
                        graphics.drawString(this.font, this.folderLabelComponent, textX, textY, -1, false);
                    }
                    if ((ActionScriptEditorScreen.this.inlineNameEditBox != null) && (this.folderCollapsedSuffixComponent != null)) {
                        int suffixX = ActionScriptEditorScreen.this.inlineNameEditBox.getX() + ActionScriptEditorScreen.this.inlineNameEditBox.getWidth() + 2;
                        graphics.drawString(this.font, this.folderCollapsedSuffixComponent, suffixX, textY, -1, false);
                    }
                }
            } else if (this.executable instanceof IfExecutableBlock ifBlock) {
                int toggleX = renderX + 5;
                int toggleY = centerYLine1 - (COLLAPSE_TOGGLE_SIZE / 2);
                this.renderCollapseToggle(graphics, toggleX, toggleY, ifBlock.isCollapsed());
                int textX = toggleX + COLLAPSE_TOGGLE_SIZE + 3;
                int textY = centerYLine1 - (this.font.lineHeight / 2);
                graphics.drawString(this.font, this.displayNameComponent, textX, textY, -1, false);
            } else if (this.executable instanceof WhileExecutableBlock whileBlock) {
                int toggleX = renderX + 5;
                int toggleY = centerYLine1 - (COLLAPSE_TOGGLE_SIZE / 2);
                this.renderCollapseToggle(graphics, toggleX, toggleY, whileBlock.isCollapsed());
                int textX = toggleX + COLLAPSE_TOGGLE_SIZE + 3;
                int textY = centerYLine1 - (this.font.lineHeight / 2);
                graphics.drawString(this.font, this.displayNameComponent, textX, textY, -1, false);
            } else if (this.executable instanceof ElseIfExecutableBlock) {
                int indicatorX = renderX + 5;
                int indicatorY = centerYLine1 - (COLLAPSE_TOGGLE_SIZE / 2);
                this.renderStatementBadge(graphics, indicatorX, indicatorY, theme.listing_dot_color_2.getColor());
                int textX = indicatorX + COLLAPSE_TOGGLE_SIZE + 3;
                int textY = centerYLine1 - (this.font.lineHeight / 2);
                graphics.drawString(this.font, this.displayNameComponent, textX, textY, -1, false);
            } else if (this.executable instanceof ElseExecutableBlock) {
                int indicatorX = renderX + 5;
                int indicatorY = centerYLine1 - (COLLAPSE_TOGGLE_SIZE / 2);
                this.renderStatementBadge(graphics, indicatorX, indicatorY, theme.listing_dot_color_2.getColor());
                int textX = indicatorX + COLLAPSE_TOGGLE_SIZE + 3;
                int textY = centerYLine1 - (this.font.lineHeight / 2);
                graphics.drawString(this.font, this.displayNameComponent, textX, textY, -1, false);
            } else if (this.executable instanceof ActionInstance) {
                UIBase.renderListingDot(graphics, renderX + 5, centerYLine1 - 2, theme.listing_dot_color_2.getColor());
                graphics.drawString(this.font, this.displayNameComponent, (renderX + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);

                UIBase.renderListingDot(graphics, renderX + 5 + 4 + 3, centerYLine2 - 2, theme.listing_dot_color_1.getColor());
                int valueTextX = renderX + 5 + 4 + 3 + 4 + 3;
                int valueTextY = centerYLine2 - (this.font.lineHeight / 2);
                if (ActionScriptEditorScreen.this.inlineValueEntry != this) {
                    graphics.drawString(this.font, this.valueComponent, valueTextX, valueTextY, -1, false);
                } else if (this.valueLabelComponent != null) {
                    graphics.drawString(this.font, this.valueLabelComponent, valueTextX, valueTextY, -1, false);
                }
            } else {
                UIBase.renderListingDot(graphics, renderX + 5, centerYLine1 - 2, theme.warning_text_color.getColor());
                graphics.drawString(this.font, this.displayNameComponent, (renderX + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);
            }
        }
        private void renderChainColumn(@NotNull GuiGraphics graphics, @NotNull Color color, @NotNull ExecutableEntry anchorEntry) {
            int barX = ActionScriptEditorScreen.this.getChainBarX(anchorEntry);
            int barTop = this.getY() + 1;
            int barBottom = this.getY() + this.getHeight() - 1;
            if (barBottom <= barTop) {
                barBottom = barTop + 1;
            }
            graphics.fill(barX, barTop, barX + CHAIN_BAR_WIDTH, barBottom, color.getRGB());
        }

        protected void handleDragging() {
            if (ActionScriptEditorScreen.this.isUserNavigatingInRightClickContextMenu()) return;
            if (!MouseInput.isLeftMouseDown()) {
                if (this.dragging) {
                    ExecutableEntry hover = ActionScriptEditorScreen.this.renderTickDragHoveredEntry;
                    if ((hover != null) && (ActionScriptEditorScreen.this.renderTickDraggedEntry == this)) {
                        ActionScriptEditorScreen.this.moveAfter(this, hover);
                    }
                }
                this.leftMouseDownDragging = false;
                this.dragging = false;
            }
            if (this.leftMouseDownDragging) {
                if ((this.leftMouseDownDraggingPosX != MouseInput.getMouseX()) || (this.leftMouseDownDraggingPosY != MouseInput.getMouseY())) {
                    // Only allow dragging for specific entries
                    if (!(this.executable instanceof AbstractExecutableBlock) || (this.executable instanceof IfExecutableBlock) || (this.executable instanceof WhileExecutableBlock) || (this.executable instanceof FolderExecutableBlock)) {
                        this.dragging = true;
                    }
                }
            }
        }

        private int getContentOffset() {
            if (this.executable instanceof AbstractExecutableBlock) {
                return STATEMENT_CONTENT_OFFSET;
            }
            return 0;
        }

        @NotNull
        public AbstractExecutableBlock getParentBlock() {
            if (this.parentBlock == null) {
                return ActionScriptEditorScreen.this.executableBlock;
            }
            return this.parentBlock;
        }

        private int calculateWidth() {
            int w;
            if ((this.executable instanceof FolderExecutableBlock) || (this.executable instanceof IfExecutableBlock) || (this.executable instanceof WhileExecutableBlock) || (this.executable instanceof ElseIfExecutableBlock) || (this.executable instanceof ElseExecutableBlock)) {
                int textWidth = this.font.width(this.displayNameComponent);
                w = 5 + COLLAPSE_TOGGLE_SIZE + 3 + textWidth + 5;
            } else {
                int w1 = 5 + 4 + 3 + this.font.width(this.displayNameComponent) + 5;
                int w2 = 5 + 4 + 3 + 4 + 3 + this.font.width(this.valueComponent) + 5;
                w = Math.max(w1, w2);
            }
            w += INDENT_X_OFFSET * this.indentLevel;
            w += this.getContentOffset();
            return w;
        }

        protected boolean canInlineEditValue() {
            return (this.executable instanceof ActionInstance i) && i.action.hasValue();
        }

        protected boolean isMouseOverValue(int mouseX, int mouseY) {
            if (ActionScriptEditorScreen.this.isUserNavigatingInRightClickContextMenu()) return false;
            if (!this.canInlineEditValue()) {
                return false;
            }
            int valueX = this.getValueFieldX();
            int valueY = this.getValueFieldY();
            int height = this.font.lineHeight;
            int width = (this.valueOnlyComponent != null) ? this.font.width(this.valueOnlyComponent) : 0;
            return UIBase.isXYInArea(mouseX, mouseY, valueX, valueY, Math.max(width, 6), height);
        }

        protected boolean registerValueClick(int mouseX, int mouseY) {
            if (!this.isMouseOverValue(mouseX, mouseY)) {
                this.lastValueClickTime = 0L;
                return false;
            }
            long now = System.currentTimeMillis();
            if ((now - this.lastValueClickTime) <= VALUE_DOUBLE_CLICK_TIME_MS) {
                this.lastValueClickTime = 0L;
                return true;
            }
            this.lastValueClickTime = now;
            return false;
        }

        protected void resetValueClickTimer() {
            this.lastValueClickTime = 0L;
        }

        protected boolean canInlineEditName() {
            return this.executable instanceof FolderExecutableBlock;
        }

        protected boolean isMouseOverName(int mouseX, int mouseY) {
            if (ActionScriptEditorScreen.this.isUserNavigatingInRightClickContextMenu()) return false;
            if (!this.canInlineEditName()) {
                return false;
            }
            int nameX = this.getNameFieldX();
            int nameY = this.getNameFieldY();
            int height = this.font.lineHeight;
            int width = (this.folderNameComponent != null) ? this.font.width(this.folderNameComponent) : 0;
            return UIBase.isXYInArea(mouseX, mouseY, nameX, nameY, Math.max(width, 6), height);
        }

        protected boolean registerNameClick(int mouseX, int mouseY) {
            if (!this.isMouseOverName(mouseX, mouseY)) {
                this.lastNameClickTime = 0L;
                return false;
            }
            long now = System.currentTimeMillis();
            if ((now - this.lastNameClickTime) <= ActionScriptEditorScreen.NAME_DOUBLE_CLICK_TIME_MS) {
                this.lastNameClickTime = 0L;
                return true;
            }
            this.lastNameClickTime = now;
            return false;
        }

        protected void resetNameClickTimer() {
            this.lastNameClickTime = 0L;
        }

        protected int getNameFieldX() {
            int baseX = this.getX() + (INDENT_X_OFFSET * this.indentLevel) + this.getContentOffset();
            int nameX = baseX + 5 + COLLAPSE_TOGGLE_SIZE + 3;
            if (this.folderLabelComponent != null) {
                nameX += this.font.width(this.folderLabelComponent);
            }
            return nameX;
        }

        protected int getNameFieldY() {
            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            return centerYLine1 - (this.font.lineHeight / 2);
        }

        protected int getNameFieldAvailableWidth() {
            ScrollArea scrollArea = ActionScriptEditorScreen.this.scriptEntriesScrollArea;
            int visibleRight = scrollArea.getInnerX() + scrollArea.getInnerWidth() - INLINE_EDIT_RIGHT_MARGIN;
            return Math.max(1, visibleRight - this.getNameFieldX());
        }

        protected int getValueFieldX() {
            int baseX = this.getX() + (INDENT_X_OFFSET * this.indentLevel) + this.getContentOffset();
            int valueX = baseX + 5 + 4 + 3 + 4 + 3;
            if (this.valueLabelComponent != null) {
                valueX += this.font.width(this.valueLabelComponent);
            }
            return valueX;
        }

        protected int getValueFieldY() {
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);
            return centerYLine2 - (this.font.lineHeight / 2);
        }

        protected int getValueFieldAvailableWidth() {
            int valueX = this.getValueFieldX();
            ScrollArea scrollArea = ActionScriptEditorScreen.this.scriptEntriesScrollArea;
            int visibleRight = scrollArea.getInnerX() + scrollArea.getInnerWidth() - INLINE_EDIT_RIGHT_MARGIN;
            int available = visibleRight - valueX;
            return Math.max(1, available);
        }

        protected boolean canToggleCollapse() {
            return (this.executable instanceof FolderExecutableBlock) || (this.executable instanceof IfExecutableBlock) || (this.executable instanceof WhileExecutableBlock);
        }

        protected boolean isMouseOverCollapseToggle(int mouseX, int mouseY) {
            if (ActionScriptEditorScreen.this.isUserNavigatingInRightClickContextMenu()) return false;
            if (!this.canToggleCollapse()) {
                return false;
            }
            int toggleX = this.getCollapseToggleX();
            int toggleY = this.getCollapseToggleY();
            return UIBase.isXYInArea(mouseX, mouseY, toggleX, toggleY, COLLAPSE_TOGGLE_SIZE, COLLAPSE_TOGGLE_SIZE);
        }

        private int getCollapseToggleX() {
            int baseX = this.getX() + (INDENT_X_OFFSET * this.indentLevel) + this.getContentOffset();
            return baseX + 5;
        }

        private int getCollapseToggleY() {
            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            return centerYLine1 - (COLLAPSE_TOGGLE_SIZE / 2);
        }

        protected void toggleCollapsed() {
            boolean toggled = false;
            if (this.executable instanceof FolderExecutableBlock folder) {
                folder.setCollapsed(!folder.isCollapsed());
                toggled = true;
            } else if (this.executable instanceof IfExecutableBlock ifBlock) {
                ifBlock.setCollapsed(!ifBlock.isCollapsed());
                toggled = true;
            } else if (this.executable instanceof WhileExecutableBlock whileBlock) {
                whileBlock.setCollapsed(!whileBlock.isCollapsed());
                toggled = true;
            }
            if (toggled) {
                this.rebuildComponents();
                this.setWidth(this.calculateWidth());
            }
        }

        private void renderCollapseToggle(@NotNull GuiGraphics graphics, int x, int y, boolean collapsed) {
            UIColorTheme theme = UIBase.getUIColorTheme();
            int size = COLLAPSE_TOGGLE_SIZE;
            Color background = theme.actions_entry_background_color_generic_block.getColor();
            graphics.fill(x, y, x + size, y + size, background.getRGB());
            UIBase.renderBorder(graphics, x, y, x + size, y + size, 1, theme.actions_chain_indicator_color, true, true, true, true);
            int midY = y + (size / 2);
            graphics.fill(x + 2, midY - 1, x + size - 2, midY + 1, theme.description_area_text_color.getColorInt());
            if (collapsed) {
                int midX = x + (size / 2);
                graphics.fill(midX - 1, y + 2, midX + 1, y + size - 2, theme.description_area_text_color.getColorInt());
            }
        }

        private void renderStatementBadge(@NotNull GuiGraphics graphics, int x, int y, @NotNull Color fillColor) {
            UIColorTheme theme = UIBase.getUIColorTheme();
            int size = COLLAPSE_TOGGLE_SIZE;
            Color background = theme.actions_entry_background_color_generic_block.getColor();
            graphics.fill(x, y, x + size, y + size, background.getRGB());
            UIBase.renderBorder(graphics, x, y, x + size, y + size, 1, theme.actions_chain_indicator_color, true, true, true, true);
            int inset = 3;
            graphics.fill(x + inset, y + inset, x + size - inset, y + size - inset, fillColor.getRGB());
        }

        private MutableComponent createCollapsedSuffixComponent(@NotNull UIColorTheme theme) {
            return Component.literal(" ").append(Component.translatable("fancymenu.actions.blocks.folder.collapsed").setStyle(Style.EMPTY.withColor(theme.warning_text_color.getColorInt())));
        }

        protected void updateValueComponent() {
            this.valueLabelComponent = null;
            this.valueOnlyComponent = null;
            if (this.executable instanceof ActionInstance i) {
                UIColorTheme theme = UIBase.getUIColorTheme();
                String cachedValue = i.value;
                boolean hasValue = i.action.hasValue();
                String valueString = ((cachedValue != null) && hasValue) ? cachedValue : I18n.get("fancymenu.actions.screens.manage_screen.info.value.none");
                MutableComponent label = Component.literal(I18n.get("fancymenu.actions.screens.manage_screen.info.value") + " ").setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                MutableComponent value = Component.literal(valueString).setStyle(Style.EMPTY.withColor(theme.element_label_color_normal.getColorInt()));
                this.valueLabelComponent = label;
                this.valueOnlyComponent = value;
                this.valueComponent = label.copy().append(value.copy());
            } else {
                this.valueComponent = Component.empty();
            }
        }

        @Override
        public void onClick(ScrollAreaEntry entry) {
            if (ActionScriptEditorScreen.this.isUserNavigatingInRightClickContextMenu()) return;
            if (this.parent.getEntries().contains(this)) {
                this.leftMouseDownDragging = true;
                this.leftMouseDownDraggingPosX = MouseInput.getMouseX();
                this.leftMouseDownDraggingPosY = MouseInput.getMouseY();
            }
        }

    }

    protected static final class MinimapEntrySegment {

        protected final ExecutableEntry entry;
        protected final int x;
        protected final int y;
        protected final int width;
        protected final int height;

        protected MinimapEntrySegment(@NotNull ExecutableEntry entry, int x, int y, int width, int height) {
            this.entry = entry;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

    }

    protected class FavoriteAwareActionEntry extends ContextMenu.ClickableContextMenuEntry<FavoriteAwareActionEntry> {

        @NotNull
        private final Action action;

        protected FavoriteAwareActionEntry(@NotNull ContextMenu parent, @NotNull Action action) {
            super("action_" + action.getIdentifier(), parent, ActionScriptEditorScreen.this.buildActionMenuLabel(action), (menu, entry) -> {
                ActionScriptEditorScreen.this.markContextMenuActionSelectionSuppressed();
                ExecutableEntry selectionReference = ActionScriptEditorScreen.this.getContextMenuTargetEntry();
                menu.closeMenu();
                ActionScriptEditorScreen.this.onAddAction(action, selectionReference);
            });
            this.action = action;
            this.setLabelSupplier((menu, entry) -> ActionScriptEditorScreen.this.buildActionMenuLabel(action));
            this.setTooltipSupplier((menu, entry) -> ActionScriptEditorScreen.this.createActionTooltip(action, ActionScriptEditorScreen.this.isFavorite(action)));
            this.updateFavoriteIcon();
        }

        private void updateFavoriteIcon() {
            if (ActionScriptEditorScreen.this.isFavorite(this.action)) {
                this.setIcon(ContextMenu.IconFactory.getIcon("favorite"));
            } else {
                this.setIcon(null);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            if ((event.button() == 1) && this.isHovered() && this.isActive() && !this.parent.isSubMenuHovered() && !this.tooltipIconHovered && !actionsMenuRightClickConsumedByEntry) {
                ActionScriptEditorScreen.this.toggleFavorite(this.action);
                actionsMenuRightClickConsumedByEntry = true;
                return true;
            }
            return super.mouseClicked(event, isDoubleClick);
        }

    }

    private static final class ScriptSnapshot {

        private final GenericExecutableBlock block;
        private final float verticalScroll;
        private final float horizontalScroll;
        @Nullable
        private final String selectedExecutableId;
        @Nullable
        private final String contextMenuTargetExecutableId;

        private ScriptSnapshot(@NotNull GenericExecutableBlock block, float verticalScroll, float horizontalScroll, @Nullable String selectedExecutableId, @Nullable String contextMenuTargetExecutableId) {
            this.block = block;
            this.verticalScroll = verticalScroll;
            this.horizontalScroll = horizontalScroll;
            this.selectedExecutableId = selectedExecutableId;
            this.contextMenuTargetExecutableId = contextMenuTargetExecutableId;
        }

    }

    private record MoveTarget(AbstractExecutableBlock parent, int index) {
    }

}