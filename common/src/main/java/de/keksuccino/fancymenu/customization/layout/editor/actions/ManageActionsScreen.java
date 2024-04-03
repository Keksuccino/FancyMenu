package de.keksuccino.fancymenu.customization.layout.editor.actions;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementGroup;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
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
    @Nullable
    protected ExecutableEntry renderTickDragHoveredEntry = null;
    @Nullable
    protected ExecutableEntry renderTickDraggedEntry = null;
    private final ExecutableEntry BEFORE_FIRST = new ExecutableEntry(this.actionsScrollArea, new GenericExecutableBlock(), 1, 0);
    private final ExecutableEntry AFTER_LAST = new ExecutableEntry(this.actionsScrollArea, new GenericExecutableBlock(), 1, 0);
    //TODO übernehmen
    protected int lastWidth = 0;
    protected int lastHeight = 0;
    //--------------------

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
                    this.executableBlock.addExecutable(new IfExecutableBlock(container));
                    this.updateActionInstanceScrollArea(false);
                    this.actionsScrollArea.verticalScrollBar.setScroll(1.0F);
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
            }
        }).setIsActiveSupplier(consumes -> {
            ExecutableEntry selected = this.getSelectedEntry();
            if (selected == null) return false;
            return (selected.executable instanceof IfExecutableBlock) || (selected.executable instanceof ElseIfExecutableBlock);
        });
        this.addWidget(this.appendElseButton);
        UIBase.applyDefaultWidgetSkinTo(this.appendElseButton);

        this.addActionButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.add_action"), (button) -> {
            BuildActionScreen s = new BuildActionScreen(null, (call) -> {
                if (call != null) {
                    this.executableBlock.addExecutable(call);
                    this.updateActionInstanceScrollArea(false);
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
            ExecutableEntry selected = this.getSelectedEntry();
            if (selected != null) {
                AbstractExecutableBlock block = selected.getParentBlock();
                if (selected.executable instanceof ActionInstance i) {
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
                }
            }
        }).setIsActiveSupplier(consumes -> {
            ExecutableEntry selected = this.getSelectedEntry();
            if ((selected == null) || (selected.executable instanceof ElseExecutableBlock)) {
                consumes.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
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

        //TODO übernehmen

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
        this.addActionButton.setX(this.width - 20 - this.addActionButton.getWidth());
        this.addActionButton.setY(this.addIfButton.getY() - 5 - 20);

        //-----------------------

        //TODO übernehmen

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

        //------------------------

    }

    @Override
    public void onClose() {
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

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, I18n.get("fancymenu.editor.action.screens.manage_screen.actions"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.actionsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.actionsScrollArea.setHeight(this.height - 85, true);
        this.actionsScrollArea.setX(20, true);
        this.actionsScrollArea.setY(50 + 15, true);
        this.actionsScrollArea.render(graphics, mouseX, mouseY, partial);

        //Render line to visualize where the dragged entry gets dropped when stop dragging it
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
            graphics.fill(this.actionsScrollArea.getInnerX(), dY + dH - 1, this.actionsScrollArea.getInnerX() + this.actionsScrollArea.getInnerWidth(), dY + dH, UIBase.getUIColorTheme().description_area_text_color.getColorInt());
        }

        //TODO übernehmen
        this.doneButton.render(graphics, mouseX, mouseY, partial);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);
        this.removeButton.render(graphics, mouseX, mouseY, partial);
        this.editButton.render(graphics, mouseX, mouseY, partial);
        this.moveDownButton.render(graphics, mouseX, mouseY, partial);
        this.moveUpButton.render(graphics, mouseX, mouseY, partial);
        this.appendElseButton.render(graphics, mouseX, mouseY, partial);
        this.appendElseIfButton.render(graphics, mouseX, mouseY, partial);
        this.addIfButton.render(graphics, mouseX, mouseY, partial);
        this.addActionButton.render(graphics, mouseX, mouseY, partial);
        //--------------------------

        super.render(graphics, mouseX, mouseY, partial);

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
        List<ExecutableEntry> entries = new ArrayList<>();
        if (entry.executable instanceof AbstractExecutableBlock) {
            List<ExecutableEntry> beforeEntry = new ArrayList<>();
            ExecutableEntry e1 = entry;
            while (e1 != null) {
                e1 = (e1.appendParent != null) ? this.findEntryForExecutable(e1.appendParent) : null;
                if (e1 != null) {
                    beforeEntry.add(0, e1);
                }
            }
            List<ExecutableEntry> afterEntry = new ArrayList<>();
            ExecutableEntry e2 = entry;
            while (e2 != null) {
                if (e2.executable instanceof AbstractExecutableBlock b) {
                    AbstractExecutableBlock appendChild = b.getAppendedBlock();
                    e2 = (appendChild != null) ? this.findEntryForExecutable(appendChild) : null;
                    if (e2 != null) {
                        afterEntry.add(e2);
                    }
                }
            }
            entries.addAll(beforeEntry);
            entries.add(entry);
            entries.addAll(afterEntry);
        }
        return entries;
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
            if ((entry.executable instanceof ActionInstance) || (entry.executable instanceof IfExecutableBlock)) {
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
            if ((entry.executable instanceof ActionInstance) || (entry.executable instanceof IfExecutableBlock)) {
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

    protected void addExecutableToEntries(int level, Executable executable, @Nullable AbstractExecutableBlock appendParent, @Nullable AbstractExecutableBlock parentBlock) {

        if (level >= 0) {
            ExecutableEntry entry = new ExecutableEntry(this.actionsScrollArea, executable, 14, level);
            entry.appendParent = appendParent;
            entry.parentBlock = parentBlock;
            this.actionsScrollArea.addEntry(entry);
        }

        if (executable instanceof AbstractExecutableBlock b) {
            for (Executable e : b.getExecutables()) {
                this.addExecutableToEntries(level+1, e, null, b);
            }
            if (b.getAppendedBlock() != null) {
                this.addExecutableToEntries(level, b.getAppendedBlock(), b, parentBlock);
            }
        }

    }

    public class ExecutableEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;
        public static final int INDENT_X_OFFSET = 20;

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

        private final MutableComponent displayNameComponent;
        private final MutableComponent valueComponent;

        @SuppressWarnings("all")
        public ExecutableEntry(@NotNull ScrollArea parentScrollArea, @NotNull Executable executable, int lineHeight, int indentLevel) {

            super(parentScrollArea, 100, 30);
            this.executable = executable;
            this.lineHeight = lineHeight;
            this.indentLevel = indentLevel;

            if (this.executable instanceof ActionInstance i) {
                this.displayNameComponent = i.action.getActionDisplayName().copy().setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
                String cachedValue = i.value;
                String valueString = ((cachedValue != null) && i.action.hasValue()) ? cachedValue : I18n.get("fancymenu.editor.action.screens.manage_screen.info.value.none");
                this.valueComponent = Component.literal(I18n.get("fancymenu.editor.action.screens.manage_screen.info.value") + " ").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())).append(Component.literal(valueString).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt())));
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
                this.displayNameComponent = Component.translatable("fancymenu.editor.actions.blocks.if", Component.literal(requirements)).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
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
                this.displayNameComponent = Component.translatable("fancymenu.editor.actions.blocks.else_if", Component.literal(requirements)).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
                this.valueComponent = Component.empty();
            } else if (this.executable instanceof ElseExecutableBlock b) {
                this.displayNameComponent = Component.translatable("fancymenu.editor.actions.blocks.else").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
                this.valueComponent = Component.empty();
            } else {
                this.displayNameComponent = Component.literal("[UNKNOWN EXECUTABLE]").withStyle(ChatFormatting.RED);
                this.valueComponent = Component.empty();
            }

            this.setWidth(this.calculateWidth());
            if (this.executable instanceof AbstractExecutableBlock) {
                this.setHeight(lineHeight + (HEADER_FOOTER_HEIGHT * 2));
            } else {
                this.setHeight((lineHeight * 2) + (HEADER_FOOTER_HEIGHT * 2));
            }

        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            this.handleDragging();

            super.render(graphics, mouseX, mouseY, partial);

            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);

            RenderSystem.enableBlend();

            int renderX = this.getX() + (INDENT_X_OFFSET * this.indentLevel);

            if (this.executable instanceof ActionInstance) {

                renderListingDot(graphics, renderX + 5, centerYLine1 - 2, UIBase.getUIColorTheme().listing_dot_color_2.getColor());
                graphics.drawString(this.font, this.displayNameComponent, (renderX + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);

                renderListingDot(graphics, renderX + 5 + 4 + 3, centerYLine2 - 2, UIBase.getUIColorTheme().listing_dot_color_1.getColor());
                graphics.drawString(this.font, this.valueComponent, (renderX + 5 + 4 + 3 + 4 + 3), (centerYLine2 - (this.font.lineHeight / 2)), -1, false);

            } else {

                renderListingDot(graphics, renderX + 5, centerYLine1 - 2, UIBase.getUIColorTheme().warning_text_color.getColor());
                graphics.drawString(this.font, this.displayNameComponent, (renderX + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);

            }

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
                    if (!(this.executable instanceof AbstractExecutableBlock) || (this.executable instanceof IfExecutableBlock)) {
                        this.dragging = true;
                    }
                }
            }
        }

        @NotNull
        public AbstractExecutableBlock getParentBlock() {
            if (this.parentBlock == null) {
                return ManageActionsScreen.this.executableBlock;
            }
            return this.parentBlock;
        }

        private int calculateWidth() {
            int w = 5 + 4 + 3 + this.font.width(this.displayNameComponent) + 5;
            int w2 = 5 + 4 + 3 + 4 + 3 + this.font.width(this.valueComponent) + 5;
            if (w2 > w) {
                w = w2;
            }
            w += INDENT_X_OFFSET * this.indentLevel;
            return w;
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

}
