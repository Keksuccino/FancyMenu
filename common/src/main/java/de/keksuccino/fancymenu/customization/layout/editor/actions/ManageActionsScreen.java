package de.keksuccino.fancymenu.customization.layout.editor.actions;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.FolderExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.List;
import java.util.function.Consumer;

public class ManageActionsScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();

    protected GenericExecutableBlock executableBlock;
    protected Consumer<GenericExecutableBlock> callback;
    protected ScrollArea actionsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton addActionButton;
    protected ExtendedButton moveUpButton;
    protected ExtendedButton moveDownButton;
    protected ExtendedButton editButton;
    protected ExtendedButton removeButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;
    protected ExtendedButton addIfButton;
    protected ExtendedButton appendElseIfButton;
    protected ExtendedButton appendElseButton;
    protected ExtendedButton addWhileButton;
    protected ExtendedButton addFolderButton;
    @Nullable
    protected ExecutableEntry renderTickDragHoveredEntry = null;
    @Nullable
    protected ExecutableEntry renderTickDraggedEntry = null;
    private final ExecutableEntry BEFORE_FIRST = new ExecutableEntry(this.actionsScrollArea, new GenericExecutableBlock(), 1, 0);
    private final ExecutableEntry AFTER_LAST = new ExecutableEntry(this.actionsScrollArea, new GenericExecutableBlock(), 1, 0);
    protected int lastWidth = 0;
    protected int lastHeight = 0;
    protected static final int LEFT_MARGIN = 20;
    protected static final int RIGHT_MARGIN = 20;
    protected static final int BUTTON_COLUMN_WIDTH = 150;
    protected static final int MINIMAP_WIDTH = 64;
    protected static final int MINIMAP_GAP = 8;
    protected static final int MINIMAP_TO_BUTTON_GAP = 12;
    protected static final int MINIMAP_PADDING = 4;
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

    public ManageActionsScreen(@NotNull GenericExecutableBlock executableBlock, @NotNull Consumer<GenericExecutableBlock> callback) {

        super(Component.translatable("fancymenu.editor.action.screens.manage_screen.manage"));

        this.executableBlock = executableBlock.copy(false);
        this.callback = callback;
        this.updateActionInstanceScrollArea(false);

    }

    @Override
    protected void init() {

        this.addIfButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.editor.actions.blocks.add.if"), button -> {
            ManageRequirementsScreen s = new ManageRequirementsScreen(new LoadingRequirementContainer(), container -> {
                if (container != null) {
                    IfExecutableBlock block = new IfExecutableBlock(container);
                    this.executableBlock.addExecutable(block);
                    this.updateActionInstanceScrollArea(false);
                    this.focusEntryForExecutable(block);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addIfButton);
        UIBase.applyDefaultWidgetSkinTo(this.addIfButton);

        this.appendElseIfButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.editor.actions.blocks.add.else_if"), button -> {
            ExecutableEntry selected = this.getSelectedEntry();
            if ((selected != null) && ((selected.executable instanceof IfExecutableBlock) || (selected.executable instanceof ElseIfExecutableBlock))) {
                ManageRequirementsScreen s = new ManageRequirementsScreen(new LoadingRequirementContainer(), container -> {
                    if (container != null) {
                        ElseIfExecutableBlock b = new ElseIfExecutableBlock(container);
                        b.setAppendedBlock(((AbstractExecutableBlock)selected.executable).getAppendedBlock());
                        ((AbstractExecutableBlock)selected.executable).setAppendedBlock(b);
                        this.updateActionInstanceScrollArea(true);
                        this.focusEntryForExecutable(b);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                Minecraft.getInstance().setScreen(s);
            }
        }).setIsActiveSupplier(consumes -> {
            ExecutableEntry selected = this.getSelectedEntry();
            if (selected == null) return false;
            return (selected.executable instanceof IfExecutableBlock) || (selected.executable instanceof ElseIfExecutableBlock);
        });
        this.addWidget(this.appendElseIfButton);
        UIBase.applyDefaultWidgetSkinTo(this.appendElseIfButton);

        this.appendElseButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.editor.actions.blocks.add.else"), button -> {
            ExecutableEntry selected = this.getSelectedEntry();
            if ((selected != null) && ((selected.executable instanceof IfExecutableBlock) || (selected.executable instanceof ElseIfExecutableBlock))) {
                ElseExecutableBlock b = new ElseExecutableBlock();
                b.setAppendedBlock(((AbstractExecutableBlock)selected.executable).getAppendedBlock());
                ((AbstractExecutableBlock)selected.executable).setAppendedBlock(b);
                this.updateActionInstanceScrollArea(true);
                this.focusEntryForExecutable(b);
            }
        }).setIsActiveSupplier(consumes -> {
            ExecutableEntry selected = this.getSelectedEntry();
            if (selected == null) return false;
            return (selected.executable instanceof IfExecutableBlock) || (selected.executable instanceof ElseIfExecutableBlock);
        });
        this.addWidget(this.appendElseButton);
        UIBase.applyDefaultWidgetSkinTo(this.appendElseButton);

        this.addWhileButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.editor.actions.blocks.add.while"), button -> {
            ManageRequirementsScreen s = new ManageRequirementsScreen(new LoadingRequirementContainer(), container -> {
                if (container != null) {
                    WhileExecutableBlock block = new WhileExecutableBlock(container);
                    this.executableBlock.addExecutable(block);
                    this.updateActionInstanceScrollArea(false);
                    this.focusEntryForExecutable(block);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addWhileButton);
        UIBase.applyDefaultWidgetSkinTo(this.addWhileButton);

        this.addFolderButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.editor.actions.blocks.add.folder"), button -> {
            ExecutableEntry selectedOnCreate = this.getSelectedEntry();
            FolderExecutableBlock block = new FolderExecutableBlock();
            this.addExecutableRelativeToSelection(block, selectedOnCreate);
            this.updateActionInstanceScrollArea(false);
            this.focusEntryForExecutable(block);
        });
        this.addWidget(this.addFolderButton);
        UIBase.applyDefaultWidgetSkinTo(this.addFolderButton);

        this.addActionButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.add_action"), (button) -> {
            ExecutableEntry selectedOnCreate = this.getSelectedEntry();
            BuildActionScreen s = new BuildActionScreen(null, (call) -> {
                if (call != null) {
                    this.addExecutableRelativeToSelection(call, selectedOnCreate);
                    this.updateActionInstanceScrollArea(false);
                    this.focusEntryForExecutable(call);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addActionButton);
        this.addActionButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.add_action.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.addActionButton);

        this.moveUpButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.move_action_up"), (button) -> {
            this.moveUp(this.getSelectedEntry());
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isAnyExecutableSelected()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                    this.active = false;
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.move_action_up.desc")));
                    this.active = true;
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.moveUpButton);
        UIBase.applyDefaultWidgetSkinTo(this.moveUpButton);

        this.moveDownButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.move_action_down"), (button) -> {
            this.moveDown(this.getSelectedEntry());
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isAnyExecutableSelected()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                    this.active = false;
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.move_action_down.desc")));
                    this.active = true;
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.moveDownButton);
        UIBase.applyDefaultWidgetSkinTo(this.moveDownButton);

        this.editButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.edit_action"), (button) -> {
            this.onEdit();
        }).setIsActiveSupplier(consumes -> {
            ExecutableEntry selected = this.getSelectedEntry();
            if ((selected == null) || (selected.executable instanceof ElseExecutableBlock)) {
                consumes.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                return false;
            }
            if (selected.executable instanceof FolderExecutableBlock) {
                consumes.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.actions.manage.folder_no_edit")));
                return false;
            }
            if ((selected.executable instanceof ActionInstance i) && !i.action.hasValue()) {
                consumes.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.actions.manage.no_value_to_edit")));
                return false;
            }
            consumes.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.edit_action.desc")));
            return true;
        });
        this.addWidget(this.editButton);
        UIBase.applyDefaultWidgetSkinTo(this.editButton);

        this.removeButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.remove_action"), (button) -> {
            ExecutableEntry selected = this.getSelectedEntry();
            if (selected != null) {
                Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call) -> {
                    if (call) {
                        if (selected.appendParent != null) {
                            selected.appendParent.setAppendedBlock(null);
                        }
                        selected.getParentBlock().getExecutables().remove(selected.executable);
                        this.updateActionInstanceScrollArea(true);
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.remove_action.confirm")));
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isAnyExecutableSelected()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                    this.active = false;
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.remove_action.desc")));
                    this.active = true;
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.removeButton);
        UIBase.applyDefaultWidgetSkinTo(this.removeButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept(this.executableBlock);
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
        this.removeButton.setX(this.width - 20 - this.removeButton.getWidth());
        this.removeButton.setY(this.cancelButton.getY() - 15 - 20);
        this.editButton.setX(this.width - 20 - this.editButton.getWidth());
        this.editButton.setY(this.removeButton.getY() - 5 - 20);
        this.moveDownButton.setX(this.width - 20 - this.moveDownButton.getWidth());
        this.moveDownButton.setY(this.editButton.getY() - 5 - 20);
        this.moveUpButton.setX(this.width - 20 - this.moveUpButton.getWidth());
        this.moveUpButton.setY(this.moveDownButton.getY() - 5 - 20);
        this.appendElseButton.setX(this.width - 20 - this.appendElseButton.getWidth());
        this.appendElseButton.setY(this.moveUpButton.getY() - 15 - 20);
        this.appendElseIfButton.setX(this.width - 20 - this.appendElseIfButton.getWidth());
        this.appendElseIfButton.setY(this.appendElseButton.getY() - 5 - 20);
        this.addIfButton.setX(this.width - 20 - this.addIfButton.getWidth());
        this.addIfButton.setY(this.appendElseIfButton.getY() - 5 - 20);
        this.addWhileButton.setX(this.width - 20 - this.addWhileButton.getWidth());
        this.addWhileButton.setY(this.addIfButton.getY() - 5 - 20);
        this.addFolderButton.setX(this.width - 20 - this.addFolderButton.getWidth());
        this.addFolderButton.setY(this.addWhileButton.getY() - 5 - 20);
        this.addActionButton.setX(this.width - 20 - this.addActionButton.getWidth());
        this.addActionButton.setY(this.addFolderButton.getY() - 5 - 20);

        AbstractWidget topRightSideWidget = this.addActionButton;
        Window window = Minecraft.getInstance().getWindow();
        boolean resized = (window.getScreenWidth() != this.lastWidth) || (window.getScreenHeight() != this.lastHeight);
        this.lastWidth = window.getScreenWidth();
        this.lastHeight = window.getScreenHeight();

        //Adjust GUI scale to make all right-side buttons fit in the screen
        if ((topRightSideWidget.getY() < 20) && (window.getGuiScale() > 1)) {
            double newScale = window.getGuiScale();
            newScale--;
            if (newScale < 1) newScale = 1;
            window.setGuiScale(newScale);
            this.resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        } else if ((topRightSideWidget.getY() >= 20) && resized) {
            RenderingUtils.resetGuiScale();
            this.resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }

    }

    protected void onEdit() {
        ExecutableEntry selected = this.getSelectedEntry();
        if (selected != null) {
            AbstractExecutableBlock block = selected.getParentBlock();
            if (selected.executable instanceof ActionInstance i) {
                if (!i.action.hasValue()) return; // If action has no value to edit, do nothing
                BuildActionScreen s = new BuildActionScreen(i.copy(false), (call) -> {
                    if (call != null) {
                        int index = block.getExecutables().indexOf(selected.executable);
                        block.getExecutables().remove(selected.executable);
                        if (index != -1) {
                            block.getExecutables().add(index, call);
                        } else {
                            block.getExecutables().add(call);
                        }
                        this.updateActionInstanceScrollArea(false);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                Minecraft.getInstance().setScreen(s);
            } else if (selected.executable instanceof IfExecutableBlock b) {
                ManageRequirementsScreen s = new ManageRequirementsScreen(b.condition.copy(false), container -> {
                    if (container != null) {
                        b.condition = container;
                        this.updateActionInstanceScrollArea(true);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                Minecraft.getInstance().setScreen(s);
            } else if (selected.executable instanceof ElseIfExecutableBlock b) {
                ManageRequirementsScreen s = new ManageRequirementsScreen(b.condition.copy(false), container -> {
                    if (container != null) {
                        b.condition = container;
                        this.updateActionInstanceScrollArea(true);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                Minecraft.getInstance().setScreen(s);
            } else if (selected.executable instanceof WhileExecutableBlock b) {
                ManageRequirementsScreen s = new ManageRequirementsScreen(b.condition.copy(false), container -> {
                    if (container != null) {
                        b.condition = container;
                        this.updateActionInstanceScrollArea(true);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                Minecraft.getInstance().setScreen(s);
            }
        }
    }

    @Override
    public void onClose() {
        this.finishInlineNameEditing(true);
        this.finishInlineNameEditing(true);
        this.finishInlineValueEditing(true);
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.renderTickDragHoveredEntry = this.getDragHoveredEntry();
        this.renderTickDraggedEntry = this.getDraggedEntry();

        //Auto-scroll scroll area vertically if dragged and out-of-area
        if (this.renderTickDraggedEntry != null) {
            float scrollOffset = 0.1F * this.actionsScrollArea.verticalScrollBar.getWheelScrollSpeed();
            if (MouseInput.getMouseY() <= this.actionsScrollArea.getInnerY()) {
                this.actionsScrollArea.verticalScrollBar.setScroll(this.actionsScrollArea.verticalScrollBar.getScroll() - scrollOffset);
            }
            if (MouseInput.getMouseY() >= (this.actionsScrollArea.getInnerY() + this.actionsScrollArea.getInnerHeight())) {
                this.actionsScrollArea.verticalScrollBar.setScroll(this.actionsScrollArea.verticalScrollBar.getScroll() + scrollOffset);
            }
        }

        UIColorTheme theme = UIBase.getUIColorTheme();
        graphics.fill(0, 0, this.width, this.height, theme.screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, theme.generic_text_base_color.getColorInt(), false);
        graphics.drawString(this.font, I18n.get("fancymenu.editor.action.screens.manage_screen.actions"), 20, 50, theme.generic_text_base_color.getColorInt(), false);

        int scrollAreaWidth = Math.max(120, this.width - LEFT_MARGIN - RIGHT_MARGIN - BUTTON_COLUMN_WIDTH - MINIMAP_WIDTH - MINIMAP_GAP - MINIMAP_TO_BUTTON_GAP);
        this.actionsScrollArea.setWidth(scrollAreaWidth, true);
        this.actionsScrollArea.setHeight(this.height - 85, true);
        this.actionsScrollArea.setX(LEFT_MARGIN, true);
        this.actionsScrollArea.setY(50 + 15, true);

        this.actionsScrollArea.updateScrollArea();
        this.actionsScrollArea.updateEntries(null);

        int buttonsLeftX = this.width - RIGHT_MARGIN - BUTTON_COLUMN_WIDTH;
        this.minimapX = buttonsLeftX - MINIMAP_TO_BUTTON_GAP - MINIMAP_WIDTH;
        this.minimapY = this.actionsScrollArea.getInnerY() - 1;
        this.minimapHeight = this.actionsScrollArea.getInnerHeight() + 2;

        this.selectedEntry = this.getSelectedEntry();
        this.selectedStatementChainEntries = (this.selectedEntry != null) ? this.getStatementChainOf(this.selectedEntry) : Collections.emptyList();

        this.hoveredEntry = this.getScrollAreaHoveredEntry();
        this.hoveredPrimaryChainEntries = (this.hoveredEntry != null) ? this.getStatementChainOf(this.hoveredEntry) : Collections.emptyList();
        this.hoveredStatementChainEntries = (this.hoveredEntry != null) ? this.collectChainWithSubChains(this.hoveredEntry) : Collections.emptyList();

        this.rebuildMinimapSegments(mouseX, mouseY);

        this.actionsScrollArea.render(graphics, mouseX, mouseY, partial);
        this.renderInlineEditors(graphics, mouseX, mouseY, partial);
        this.updateCursor(mouseX, mouseY);

        if (this.renderTickDragHoveredEntry != null) {
            int dY = this.renderTickDragHoveredEntry.getY();
            int dH = this.renderTickDragHoveredEntry.getHeight();
            if (this.renderTickDragHoveredEntry == BEFORE_FIRST) {
                dY = this.actionsScrollArea.getInnerY();
                dH = 1;
            }
            if (this.renderTickDragHoveredEntry == AFTER_LAST) {
                dY = this.actionsScrollArea.getInnerY() + this.actionsScrollArea.getInnerHeight() - 1;
                dH = 1;
            }
            graphics.fill(this.actionsScrollArea.getInnerX(), dY + dH - 1, this.actionsScrollArea.getInnerX() + this.actionsScrollArea.getInnerWidth(), dY + dH, theme.description_area_text_color.getColorInt());
        }

        this.renderChainMinimap(graphics);

        this.doneButton.render(graphics, mouseX, mouseY, partial);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);
        this.removeButton.render(graphics, mouseX, mouseY, partial);
        this.editButton.render(graphics, mouseX, mouseY, partial);
        this.moveDownButton.render(graphics, mouseX, mouseY, partial);
        this.moveUpButton.render(graphics, mouseX, mouseY, partial);
        this.appendElseButton.render(graphics, mouseX, mouseY, partial);
        this.appendElseIfButton.render(graphics, mouseX, mouseY, partial);
        this.addIfButton.render(graphics, mouseX, mouseY, partial);
        this.addWhileButton.render(graphics, mouseX, mouseY, partial);
        this.addFolderButton.render(graphics, mouseX, mouseY, partial);
        this.addActionButton.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);
        this.renderMinimapEntryTooltip(graphics, mouseX, mouseY);

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ((this.minimapHeight > 0) && UIBase.isXYInArea((int)mouseX, (int)mouseY, this.minimapX, this.minimapY, MINIMAP_WIDTH, this.minimapHeight)) {
            if (scrollY != 0.0D) {
                float scrollStep = 0.1F * this.actionsScrollArea.verticalScrollBar.getWheelScrollSpeed();
                float totalOffset = scrollStep * (float)Math.abs(scrollY);
                if (scrollY > 0.0D) {
                    totalOffset = -totalOffset;
                }
                this.actionsScrollArea.verticalScrollBar.setScroll(this.actionsScrollArea.verticalScrollBar.getScroll() + totalOffset);
                this.actionsScrollArea.updateEntries(null);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isInlineNameEditing()) {
            boolean insideNameEditor = UIBase.isXYInArea((int) mouseX, (int) mouseY, this.inlineNameEditBox.getX(), this.inlineNameEditBox.getY(), this.inlineNameEditBox.getWidth(), this.inlineNameEditBox.getHeight());
            if (insideNameEditor && this.inlineNameEditBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (!insideNameEditor) {
                this.finishInlineNameEditing(true);
            }
        }

        if (this.isInlineValueEditing()) {
            boolean insideEditor = UIBase.isXYInArea((int) mouseX, (int) mouseY, this.inlineValueEditBox.getX(), this.inlineValueEditBox.getY(), this.inlineValueEditBox.getWidth(), this.inlineValueEditBox.getHeight());
            if (insideEditor && this.inlineValueEditBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (!insideEditor) {
                this.finishInlineValueEditing(true);
            }
        }

        if ((button == 0) && (this.minimapHeight > 0) && UIBase.isXYInArea((int)mouseX, (int)mouseY, this.minimapX, this.minimapY, MINIMAP_WIDTH, this.minimapHeight)) {
            ExecutableEntry entry = this.getMinimapEntryAt((int)mouseX, (int)mouseY);
            if (entry != null) {
                entry.setSelected(true);
                this.scrollEntryIntoView(entry);
                return true;
            }
        }

        if (button == 0) {
            ExecutableEntry hovered = this.getScrollAreaHoveredEntry();
            if (hovered != null) {
                if (hovered.canToggleCollapse() && hovered.isMouseOverCollapseToggle((int)mouseX, (int)mouseY)) {
                    this.finishInlineNameEditing(true);
                    this.finishInlineValueEditing(true);
                    hovered.toggleFolderCollapsed();
                    this.updateActionInstanceScrollArea(true);
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

        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if ((button == 0) && !this.isInlineValueEditing()) {
            ExecutableEntry hoveredAfter = this.getScrollAreaHoveredEntry();
            if ((hoveredAfter == null) || !hoveredAfter.isMouseOverValue((int)mouseX, (int)mouseY)) {
                for (ScrollAreaEntry entry : this.actionsScrollArea.getEntries()) {
                    if (entry instanceof ExecutableEntry ee) {
                        ee.resetValueClickTimer();
                    }
                }
            }
        }

        if ((button == 0) && !this.isInlineNameEditing()) {
            ExecutableEntry hoveredAfter = this.getScrollAreaHoveredEntry();
            if ((hoveredAfter == null) || !hoveredAfter.isMouseOverName((int)mouseX, (int)mouseY)) {
                for (ScrollAreaEntry entry : this.actionsScrollArea.getEntries()) {
                    if (entry instanceof ExecutableEntry ee && ee.canInlineEditName()) {
                        ee.resetNameClickTimer();
                    }
                }
            }
        }

        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inlineNameEditBox != null) {
            if ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER)) {
                this.finishInlineNameEditing(true);
                return true;
            }
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.finishInlineNameEditing(false);
                return true;
            }
            if (this.inlineNameEditBox.keyPressed(keyCode, scanCode, modifiers)) {
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
            if (this.inlineValueEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if ((this.inlineNameEditBox != null) && this.inlineNameEditBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if ((this.inlineValueEditBox != null) && this.inlineValueEditBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if ((this.inlineNameEditBox != null) && this.inlineNameEditBox.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if ((this.inlineValueEditBox != null) && this.inlineValueEditBox.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected void renderInlineEditors(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.isInlineValueEditing()) {
            if (!this.actionsScrollArea.getEntries().contains(this.inlineValueEntry)) {
                this.finishInlineValueEditing(false);
            } else {
                this.updateInlineValueEditorBounds();
                if (this.inlineValueEditBox != null) {
                    this.inlineValueEditBox.render(graphics, mouseX, mouseY, partial);
                }
            }
        }
        if (this.isInlineNameEditing()) {
            if (!this.actionsScrollArea.getEntries().contains(this.inlineNameEntry)) {
                this.finishInlineNameEditing(false);
            } else {
                this.updateInlineNameEditorBounds();
                if (this.inlineNameEditBox != null) {
                    this.inlineNameEditBox.render(graphics, mouseX, mouseY, partial);
                }
            }
        }
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
        if ((hovered != null) && hovered.canInlineEditName() && hovered.isMouseOverName(mouseX, mouseY)) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
            return;
        }
        if ((hovered != null) && hovered.canInlineEditValue() && hovered.isMouseOverValue(mouseX, mouseY)) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_WRITING);
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
            String result = editBox.getValue();
            if (!save) {
                instance.value = this.inlineValueOriginal;
            } else {
                String normalized = ((result != null) && !result.isEmpty()) ? result : null;
                if (!Objects.equals(instance.value, normalized)) {
                    instance.value = normalized;
                }
            }
            entry.updateValueComponent();
            entry.setWidth(entry.calculateWidth());
            entry.resetValueClickTimer();
        }
        this.inlineValueOriginal = null;
        this.actionsScrollArea.updateEntries(null);
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
            String result = editBox.getValue();
            if (!save) {
                folder.setName(this.inlineNameOriginal != null ? this.inlineNameOriginal : FolderExecutableBlock.DEFAULT_NAME);
            } else {
                String normalized = (result != null) ? result.trim() : "";
                folder.setName(normalized);
            }
            entry.rebuildComponents();
            entry.setWidth(entry.calculateWidth());
            entry.resetNameClickTimer();
        }
        this.inlineNameOriginal = null;
        this.actionsScrollArea.updateEntries(null);
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

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
    }

    @Nullable
    protected ExecutableEntry getScrollAreaHoveredEntry() {
        if (!this.actionsScrollArea.isMouseInsideArea()) {
            return null;
        }
        int mouseX = MouseInput.getMouseX();
        int mouseY = MouseInput.getMouseY();
        for (ScrollAreaEntry entry : this.actionsScrollArea.getEntries()) {
            if (entry instanceof ExecutableEntry ee) {
                if (UIBase.isXYInArea(mouseX, mouseY, ee.getX(), ee.getY(), ee.getWidth(), ee.getHeight())) {
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

        List<ScrollAreaEntry> scrollEntries = this.actionsScrollArea.getEntries();
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

    protected void renderChainMinimap(@NotNull GuiGraphics graphics) {

        if (this.minimapHeight <= 0) {
            return;
        }
        UIColorTheme theme = UIBase.getUIColorTheme();
        RenderSystem.enableBlend();
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

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 400.0F);

        RenderSystem.enableBlend();
        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, backgroundColor.getRGB());
        UIBase.renderBorder(graphics, tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 1, theme.actions_minimap_tooltip_border_color, true, true, true, true);

        poseStack.translate(tooltipX + MINIMAP_TOOLTIP_PADDING, tooltipY + MINIMAP_TOOLTIP_PADDING, 0.0F);
        poseStack.scale(MINIMAP_TOOLTIP_SCALE, MINIMAP_TOOLTIP_SCALE, 1.0F);
        poseStack.translate(-this.minimapHoveredEntry.getX(), -this.minimapHoveredEntry.getY(), 0.0F);

        this.minimapHoveredEntry.renderThumbnail(graphics);

        poseStack.popPose();
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
        int visibleHeight = this.actionsScrollArea.getInnerHeight();
        if (this.minimapTotalEntriesHeight <= visibleHeight) {
            return;
        }
        int maxScroll = Math.max(1, this.minimapTotalEntriesHeight - visibleHeight);
        int scrollPixels = Math.round(this.actionsScrollArea.verticalScrollBar.getScroll() * maxScroll);
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

    @Nullable
    protected ExecutableEntry getMinimapEntryAt(int mouseX, int mouseY) {
        for (MinimapEntrySegment segment : this.minimapSegments) {
            if (UIBase.isXYInArea(mouseX, mouseY, segment.x, segment.y, segment.width, segment.height)) {
                return segment.entry;
            }
        }
        return null;
    }

    protected void scrollEntryIntoView(@NotNull ExecutableEntry entry) {
        List<ScrollAreaEntry> scrollEntries = this.actionsScrollArea.getEntries();
        int totalHeight = 0;
        for (ScrollAreaEntry e : scrollEntries) {
            totalHeight += e.getHeight();
        }
        int visibleHeight = this.actionsScrollArea.getInnerHeight();
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
        this.actionsScrollArea.verticalScrollBar.setScroll(scroll);
        this.actionsScrollArea.updateEntries(null);
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
        List<ScrollAreaEntry> scrollEntries = this.actionsScrollArea.getEntries();
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
        List<ScrollAreaEntry> scrollEntries = this.actionsScrollArea.getEntries();
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
            if ((MouseInput.getMouseY() <= this.actionsScrollArea.getInnerY()) && (this.actionsScrollArea.verticalScrollBar.getScroll() == 0.0F)) {
                return BEFORE_FIRST;
            }
            if ((MouseInput.getMouseY() >= (this.actionsScrollArea.getInnerY() + this.actionsScrollArea.getInnerHeight())) && (this.actionsScrollArea.verticalScrollBar.getScroll() == 1.0F)) {
                return AFTER_LAST;
            }
            for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
                if (e instanceof ExecutableEntry ee) {
                    if ((e.getY() + e.getHeight()) > (this.actionsScrollArea.getInnerY() + this.actionsScrollArea.getInnerHeight())) {
                        continue;
                    }
                    if ((ee != draggedEntry) && UIBase.isXYInArea(MouseInput.getMouseX(), MouseInput.getMouseY(), ee.getX(), ee.getY(), ee.getWidth(), ee.getHeight()) && this.actionsScrollArea.isMouseInsideArea()) {
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
        for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
            if (e instanceof ExecutableEntry ee) {
                if (ee.dragging) return ee;
            }
        }
        return null;
    }

    @Nullable
    protected ExecutableEntry findEntryForExecutable(Executable executable) {
        for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
            if (e instanceof ExecutableEntry ee) {
                if (ee.executable == executable) return ee;
            }
        }
        return null;
    }

    protected void focusEntryForExecutable(@NotNull Executable executable) {
        ExecutableEntry entry = this.findEntryForExecutable(executable);
        if (entry != null) {
            entry.setSelected(true);
            this.scrollEntryIntoView(entry);
        }
    }

    @Nullable
    protected ExecutableEntry getSelectedEntry() {
        ScrollAreaEntry e = this.actionsScrollArea.getFocusedEntry();
        if (e instanceof ExecutableEntry ee) {
            return ee;
        }
        return null;
    }

    protected boolean isAnyExecutableSelected() {
        return this.getSelectedEntry() != null;
    }

    @Nullable
    protected ExecutableEntry getValidMoveToEntryBefore(@NotNull ExecutableEntry entry, boolean ignoreValidityChecks) {
        if (entry == BEFORE_FIRST) return BEFORE_FIRST;
        int index = this.actionsScrollArea.getEntries().size();
        boolean foundEntry = false;
        boolean foundValidMoveTo = false;
        for (ScrollAreaEntry e : Lists.reverse(this.actionsScrollArea.getEntries())) {
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
        ScrollAreaEntry e = this.actionsScrollArea.getEntry(index);
        if (e instanceof ExecutableEntry ee) return ee;
        return null;
    }

    @Nullable
    protected ExecutableEntry getValidMoveToEntryAfter(@NotNull ExecutableEntry entry, boolean ignoreValidityChecks) {
        if (entry == AFTER_LAST) return AFTER_LAST;
        int index = -1;
        boolean foundEntry = false;
        boolean foundValidMoveTo = false;
        for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
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
        ScrollAreaEntry e = this.actionsScrollArea.getEntry(index);
        if (e instanceof ExecutableEntry ee) return ee;
        return null;
    }

    protected void moveAfter(@NotNull ExecutableEntry entry, @NotNull ExecutableEntry moveAfter) {
        entry.getParentBlock().getExecutables().remove(entry.executable);
        int moveAfterIndex = Math.max(0, moveAfter.getParentBlock().getExecutables().indexOf(moveAfter.executable));
        if (moveAfter == BEFORE_FIRST) {
            this.executableBlock.getExecutables().add(0, entry.executable);
        } else if (moveAfter == AFTER_LAST) {
            this.executableBlock.getExecutables().add(entry.executable);
        } else {
            if (moveAfter.executable instanceof AbstractExecutableBlock b) {
                b.getExecutables().add(0, entry.executable);
            } else {
                moveAfter.getParentBlock().getExecutables().add(moveAfterIndex+1, entry.executable);
            }
        }
        this.updateActionInstanceScrollArea(true);
        //Re-select entry after updating scroll area
        ExecutableEntry newEntry = this.findEntryForExecutable(entry.executable);
        if (newEntry != null) {
            newEntry.setSelected(true);
        }
    }

    protected void moveUp(ExecutableEntry entry) {
        if (entry != null) {
            if ((entry.executable instanceof ActionInstance) || (entry.executable instanceof IfExecutableBlock) || (entry.executable instanceof WhileExecutableBlock)) {
                boolean manualUpdate = false;
                if (this.actionsScrollArea.getEntries().indexOf(entry) == 1) {
                    this.moveAfter(entry, BEFORE_FIRST);
                } else {
                    if ((entry.getParentBlock() != this.executableBlock) && ((entry.getParentBlock() instanceof ElseIfExecutableBlock) || (entry.getParentBlock() instanceof ElseExecutableBlock)) && (entry.getParentBlock().getExecutables().indexOf(entry.executable) == 0)) {
                        ExecutableEntry parentBlock = this.findEntryForExecutable(entry.getParentBlock());
                        if (parentBlock != null) {
                            entry.getParentBlock().getExecutables().remove(entry.executable);
                            if (parentBlock.appendParent != null) {
                                parentBlock.appendParent.getExecutables().add(entry.executable);
                                manualUpdate = true;
                            }
                        }
                    } else if ((entry.getParentBlock() != this.executableBlock) && (entry.getParentBlock() instanceof IfExecutableBlock) && (entry.getParentBlock().getExecutables().indexOf(entry.executable) == 0)) {
                        ExecutableEntry parentBlock = this.findEntryForExecutable(entry.getParentBlock());
                        if (parentBlock != null) {
                            int parentIndex = Math.max(0, parentBlock.getParentBlock().getExecutables().indexOf(parentBlock.executable));
                            entry.getParentBlock().getExecutables().remove(entry.executable);
                            parentBlock.getParentBlock().getExecutables().add(parentIndex, entry.executable);
                            manualUpdate = true;
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
                        newEntry.setSelected(true);
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
                newEntry.setSelected(true);
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
                        newEntry.setSelected(true);
                    }
                }
                return;
            }
            if (entry.executable instanceof ElseIfExecutableBlock ei) {
                AbstractExecutableBlock entryAppendChild = ei.getAppendedBlock();
                AbstractExecutableBlock entryAppendParent = entry.appendParent;
                if ((entryAppendChild instanceof ElseIfExecutableBlock) && (entryAppendParent != null)) {
                    ei.setAppendedBlock(entryAppendChild.getAppendedBlock());
                    entryAppendChild.setAppendedBlock(ei);
                    entryAppendParent.setAppendedBlock(entryAppendChild);
                }
            }
            this.updateActionInstanceScrollArea(true);
            //Re-select entry after updating scroll area
            ExecutableEntry newEntry = this.findEntryForExecutable(entry.executable);
            if (newEntry != null) {
                newEntry.setSelected(true);
            }
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

        for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
            if (e instanceof ExecutableEntry ee) {
                ee.leftMouseDownDragging = false;
                ee.dragging = false;
            }
        }

        float oldScrollVertical = this.actionsScrollArea.verticalScrollBar.getScroll();
        float oldScrollHorizontal = this.actionsScrollArea.horizontalScrollBar.getScroll();

        this.actionsScrollArea.clearEntries();

        this.addExecutableToEntries(-1, this.executableBlock, null, null);

        if (keepScroll) {
            this.actionsScrollArea.verticalScrollBar.setScroll(oldScrollVertical);
            this.actionsScrollArea.horizontalScrollBar.setScroll(oldScrollHorizontal);
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
            ExecutableEntry entry = new ExecutableEntry(this.actionsScrollArea, executable, 14, level);
            entry.appendParent = appendParent;
            entry.parentBlock = parentBlock;
            this.actionsScrollArea.addEntry(entry);
        }

        if (executable instanceof AbstractExecutableBlock b) {
            boolean skipChildren = (b instanceof FolderExecutableBlock folder) && folder.isCollapsed();
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

    public class ExecutableEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;
        public static final int INDENT_X_OFFSET = 20;
        public static final int STATEMENT_CONTENT_OFFSET = 4;
        private static final int FOLDER_TOGGLE_SIZE = 8;

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
            } else if (this.executable instanceof AbstractExecutableBlock) {
                idle = theme.actions_entry_background_color_generic_block.getColor();
                hover = theme.actions_entry_background_color_generic_block_hover.getColor();
            }

            this.setBackgroundColorIdle(idle);
            this.setBackgroundColorHover(hover);
        }

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
                this.displayNameComponent = Component.translatable("fancymenu.editor.actions.blocks.if", Component.literal(requirements)).setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
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
                this.displayNameComponent = Component.translatable("fancymenu.editor.actions.blocks.else_if", Component.literal(requirements)).setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                this.valueComponent = Component.empty();
            } else if (this.executable instanceof ElseExecutableBlock) {
                this.displayNameComponent = Component.translatable("fancymenu.editor.actions.blocks.else").setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
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
                this.displayNameComponent = Component.translatable("fancymenu.editor.actions.blocks.while", Component.literal(requirements)).setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                this.valueComponent = Component.empty();
            } else if (this.executable instanceof FolderExecutableBlock folder) {
                MutableComponent labelComponent = Component.literal(I18n.get("fancymenu.editor.actions.blocks.folder.display", "")).setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
                MutableComponent nameComponent = Component.literal(folder.getName()).setStyle(Style.EMPTY.withColor(theme.element_label_color_normal.getColorInt()));
                this.folderLabelComponent = labelComponent;
                this.folderNameComponent = nameComponent;
                MutableComponent display = labelComponent.copy().append(nameComponent.copy());
                if (folder.isCollapsed()) {
                    MutableComponent collapsedComponent = Component.literal(" ").append(Component.translatable("fancymenu.editor.actions.blocks.folder.collapsed").setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt())));
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
            RenderSystem.enableBlend();
            List<ExecutableEntry> chainAnchors = ManageActionsScreen.this.getChainAnchorsFor(this);
            for (ExecutableEntry anchorEntry : chainAnchors) {
                List<ExecutableEntry> chainEntries = ManageActionsScreen.this.getStatementChainOf(anchorEntry);
                if (chainEntries.size() > 1) {
                    Color chainColor = ManageActionsScreen.this.getChainIndicatorColorFor(anchorEntry);
                    this.renderChainColumn(graphics, chainColor, anchorEntry);
                }
            }

            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);

            int renderX = this.getX() + (INDENT_X_OFFSET * this.indentLevel) + this.getContentOffset();

            if (this.executable instanceof FolderExecutableBlock folder) {
                int toggleX = renderX + 5;
                int toggleY = centerYLine1 - (FOLDER_TOGGLE_SIZE / 2);
                this.renderFolderToggle(graphics, toggleX, toggleY, folder.isCollapsed());
                int textX = toggleX + FOLDER_TOGGLE_SIZE + 3;
                int textY = centerYLine1 - (this.font.lineHeight / 2);
                if (ManageActionsScreen.this.inlineNameEntry != this) {
                    graphics.drawString(this.font, this.displayNameComponent, textX, textY, -1, false);
                } else {
                    if (this.folderLabelComponent != null) {
                        graphics.drawString(this.font, this.folderLabelComponent, textX, textY, -1, false);
                    }
                    if ((ManageActionsScreen.this.inlineNameEditBox != null) && (this.folderCollapsedSuffixComponent != null)) {
                        int suffixX = ManageActionsScreen.this.inlineNameEditBox.getX() + ManageActionsScreen.this.inlineNameEditBox.getWidth() + 2;
                        graphics.drawString(this.font, this.folderCollapsedSuffixComponent, suffixX, textY, -1, false);
                    }
                }
            } else if (this.executable instanceof ActionInstance) {
                UIBase.renderListingDot(graphics, renderX + 5, centerYLine1 - 2, UIBase.getUIColorTheme().listing_dot_color_2.getColor());
                graphics.drawString(this.font, this.displayNameComponent, (renderX + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);

                UIBase.renderListingDot(graphics, renderX + 5 + 4 + 3, centerYLine2 - 2, UIBase.getUIColorTheme().listing_dot_color_1.getColor());
                int valueTextX = renderX + 5 + 4 + 3 + 4 + 3;
                int valueTextY = centerYLine2 - (this.font.lineHeight / 2);
                if (ManageActionsScreen.this.inlineValueEntry != this) {
                    graphics.drawString(this.font, this.valueComponent, valueTextX, valueTextY, -1, false);
                } else if (this.valueLabelComponent != null) {
                    graphics.drawString(this.font, this.valueLabelComponent, valueTextX, valueTextY, -1, false);
                }
            } else {
                UIBase.renderListingDot(graphics, renderX + 5, centerYLine1 - 2, UIBase.getUIColorTheme().warning_text_color.getColor());
                graphics.drawString(this.font, this.displayNameComponent, (renderX + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);
            }
        }

        private void renderChainColumn(@NotNull GuiGraphics graphics, @NotNull Color color, @NotNull ExecutableEntry anchorEntry) {
            int barX = ManageActionsScreen.this.getChainBarX(anchorEntry);
            int barTop = this.getY() + 1;
            int barBottom = this.getY() + this.getHeight() - 1;
            if (barBottom <= barTop) {
                barBottom = barTop + 1;
            }
            graphics.fill(barX, barTop, barX + CHAIN_BAR_WIDTH, barBottom, color.getRGB());
        }

        protected void handleDragging() {
            if (!MouseInput.isLeftMouseDown()) {
                if (this.dragging) {
                    ExecutableEntry hover = ManageActionsScreen.this.renderTickDragHoveredEntry;
                    if ((hover != null) && (ManageActionsScreen.this.renderTickDraggedEntry == this)) {
                        ManageActionsScreen.this.moveAfter(this, hover);
                    }
                }
                this.leftMouseDownDragging = false;
                this.dragging = false;
            }
            if (this.leftMouseDownDragging) {
                if ((this.leftMouseDownDraggingPosX != MouseInput.getMouseX()) || (this.leftMouseDownDraggingPosY != MouseInput.getMouseY())) {
                    //Only allow dragging for ActionInstances and If-Blocks
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
                return ManageActionsScreen.this.executableBlock;
            }
            return this.parentBlock;
        }

        private int calculateWidth() {
            int w;
            if (this.executable instanceof FolderExecutableBlock) {
                int textWidth = this.font.width(this.displayNameComponent);
                w = 5 + FOLDER_TOGGLE_SIZE + 3 + textWidth + 5;
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
            if ((now - this.lastNameClickTime) <= ManageActionsScreen.NAME_DOUBLE_CLICK_TIME_MS) {
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
            int nameX = baseX + 5 + FOLDER_TOGGLE_SIZE + 3;
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
            ScrollArea scrollArea = ManageActionsScreen.this.actionsScrollArea;
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
            ScrollArea scrollArea = ManageActionsScreen.this.actionsScrollArea;
            int visibleRight = scrollArea.getInnerX() + scrollArea.getInnerWidth() - INLINE_EDIT_RIGHT_MARGIN;
            int available = visibleRight - valueX;
            return Math.max(1, available);
        }

        protected boolean canToggleCollapse() {
            return this.executable instanceof FolderExecutableBlock;
        }

        protected boolean isMouseOverCollapseToggle(int mouseX, int mouseY) {
            if (!this.canToggleCollapse()) {
                return false;
            }
            int toggleX = this.getFolderToggleX();
            int toggleY = this.getFolderToggleY();
            return UIBase.isXYInArea(mouseX, mouseY, toggleX, toggleY, FOLDER_TOGGLE_SIZE, FOLDER_TOGGLE_SIZE);
        }

        private int getFolderToggleX() {
            int baseX = this.getX() + (INDENT_X_OFFSET * this.indentLevel) + this.getContentOffset();
            return baseX + 5;
        }

        private int getFolderToggleY() {
            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            return centerYLine1 - (FOLDER_TOGGLE_SIZE / 2);
        }

        protected void toggleFolderCollapsed() {
            if (this.executable instanceof FolderExecutableBlock folder) {
                folder.setCollapsed(!folder.isCollapsed());
                this.rebuildComponents();
                this.setWidth(this.calculateWidth());
            }
        }

        private void renderFolderToggle(@NotNull GuiGraphics graphics, int x, int y, boolean collapsed) {
            UIColorTheme theme = UIBase.getUIColorTheme();
            int size = FOLDER_TOGGLE_SIZE;
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

        protected void updateValueComponent() {
            this.valueLabelComponent = null;
            this.valueOnlyComponent = null;
            if (this.executable instanceof ActionInstance i) {
                UIColorTheme theme = UIBase.getUIColorTheme();
                String cachedValue = i.value;
                boolean hasValue = i.action.hasValue();
                String valueString = ((cachedValue != null) && hasValue) ? cachedValue : I18n.get("fancymenu.editor.action.screens.manage_screen.info.value.none");
                MutableComponent label = Component.literal(I18n.get("fancymenu.editor.action.screens.manage_screen.info.value") + " ").setStyle(Style.EMPTY.withColor(theme.description_area_text_color.getColorInt()));
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

}























