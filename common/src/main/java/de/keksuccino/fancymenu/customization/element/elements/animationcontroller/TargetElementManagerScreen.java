package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TargetElementManagerScreen extends CellScreen {

    protected final AnimationControllerElement element;
    protected final LayoutEditorScreen parentLayoutEditor;
    protected final Consumer<List<AnimationControllerElement.TargetElement>> callback;
    protected final List<AnimationControllerElement.TargetElement> targets;

    public TargetElementManagerScreen(@NotNull AnimationControllerEditorElement editorElement, @NotNull Consumer<List<AnimationControllerElement.TargetElement>> callback) {
        super(Component.translatable("fancymenu.elements.animation_controller.manage_targets"));
        this.element = editorElement.element;
        this.parentLayoutEditor = editorElement.editor;
        this.callback = callback;
        this.targets = new ArrayList<>(this.element.targetElements);
        this.setSearchBarEnabled(true);
    }

    @Override
    protected void initCells() {

        this.addCellGroupEndSpacerCell().setIgnoreSearch();

        // Header
        this.addLabelCell(Component.translatable("fancymenu.elements.animation_controller.manage_targets.info")
                .setStyle(Style.EMPTY.withItalic(true)))
                .setIgnoreSearch();

        this.addCellGroupEndSpacerCell().setIgnoreSearch();
        this.addCellGroupEndSpacerCell().setIgnoreSearch();

        // List current targets
        if (this.targets.isEmpty()) {
            this.addLabelCell(Component.translatable("fancymenu.elements.animation_controller.manage_targets.no_targets")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt())));
        } else {
            for (AnimationControllerElement.TargetElement target : this.targets) {
                AbstractEditorElement e = this.parentLayoutEditor.getElementByInstanceIdentifier(target.targetElementId);
                MutableComponent label = (e != null) ? e.element.getDisplayName().copy() : Component.literal("---");
                label = label.setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()));
                label = label.append(Component.literal(" [" + target.targetElementId + "]").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().generic_text_base_color.getColorInt())));
                label = label.append(Component.literal(" ").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().generic_text_base_color.getColorInt())));
                label = label.append(Component.translatable("fancymenu.elements.animation_controller.manage_targets.offset.label", target.timingOffsetMs)
                        .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_text_color.getColorInt())));
                this.addCell(new TargetEntryCell(target, label));
            }
        }

        this.addCellGroupEndSpacerCell().setIgnoreSearch();

    }

    @Override
    protected void initRightSideWidgets() {
        // Add target button
        this.addRightSideButton(20, Component.translatable("fancymenu.elements.animation_controller.manage_targets.add"), button -> {
            ElementSelectorScreen selector = new ElementSelectorScreen(
                    this.parentLayoutEditor,
                    this,
                    this.getElementIds(),
                    selectedElement -> {
                        if (selectedElement != null) {
                            String elementId = selectedElement.element.getInstanceIdentifier();
                            // Check if already in targets
                            if (this.targets.stream().anyMatch(t -> t.targetElementId.equals(elementId))) {
                                Dialogs.openMessage(Component.translatable("fancymenu.elements.animation_controller.manage_targets.already_exists"), MessageDialogStyle.WARNING);
                            } else {
                                this.targets.add(new AnimationControllerElement.TargetElement(elementId));
                                Minecraft.getInstance().setScreen(this);
                            }
                        } else {
                            Minecraft.getInstance().setScreen(this);
                        }
                    }
            );
            Minecraft.getInstance().setScreen(selector);
        });
        this.addRightSideDefaultSpacer();

        this.addRightSideButton(20, Component.translatable("fancymenu.elements.animation_controller.manage_targets.offset.edit"), button -> {
            AnimationControllerElement.TargetElement target = this.getSelectedTarget();
            if (target == null) return;
            TextInputScreen inputScreen = new TextInputScreen(CharacterFilter.buildIntegerFilter(), result -> {
                if (result != null) {
                    String trimmed = result.trim();
                    int offsetMs = 0;
                    if (!trimmed.isEmpty()) {
                        try {
                            offsetMs = Integer.parseInt(trimmed);
                        } catch (NumberFormatException ignored) {
                            offsetMs = 0;
                        }
                    }
                    target.timingOffsetMs = offsetMs;
                }
                this.rebuild();
            });
            Dialogs.openGeneric(inputScreen,
                    Component.translatable("fancymenu.elements.animation_controller.manage_targets.offset.input"),
                    ContextMenu.IconFactory.getIcon("text"), TextInputScreen.PIP_WINDOW_WIDTH, TextInputScreen.PIP_WINDOW_HEIGHT);
            inputScreen.setText("" + target.timingOffsetMs);
        }).setIsActiveSupplier(consumes -> (this.getSelectedTarget() != null));
        this.addRightSideDefaultSpacer();

        this.addRightSideButton(20, Component.translatable("fancymenu.elements.animation_controller.manage_targets.remove"), button -> {
            AnimationControllerElement.TargetElement target = this.getSelectedTarget();
            if (target == null) return;
            this.targets.remove(target);
            this.rebuild();
        }).setIsActiveSupplier(consumes -> (this.getSelectedTarget() != null));
        this.addRightSideDefaultSpacer();

        this.addRightSideButton(20, Component.translatable("fancymenu.elements.animation_controller.manage_targets.remove_by_id"), button -> {
            TextInputScreen inputScreen = new TextInputScreen(null, result -> {
                boolean removed = false;
                if (result != null) {
                    String trimmed = result.trim();
                    if (!trimmed.isEmpty()) {
                        removed = TargetElementManagerScreen.this.targets.removeIf(target -> target.targetElementId.equals(trimmed));
                    }
                }
                if (removed) {
                    TargetElementManagerScreen.this.rebuild();
                }
            });
            Dialogs.openGeneric(inputScreen,
                    Component.translatable("fancymenu.elements.animation_controller.manage_targets.remove_by_id.input"),
                    ContextMenu.IconFactory.getIcon("text"), TextInputScreen.PIP_WINDOW_WIDTH, TextInputScreen.PIP_WINDOW_HEIGHT);
        });
        this.addRightSideDefaultSpacer();
    }

    protected List<String> getElementIds() {
        List<String> ids = new ArrayList<>();
        this.targets.forEach(targetElement -> ids.add(targetElement.targetElementId));
        return ids;
    }

    protected AnimationControllerElement.TargetElement getSelectedTarget() {
        RenderCell cell = this.getSelectedCell();
        if (cell != null) {
            String id = cell.getMemoryValue("target_id");
            if (id != null) {
                for (AnimationControllerElement.TargetElement target : this.targets) {
                    if (id.equals(target.targetElementId)) {
                        return target;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.targets);
    }

    protected class TargetEntryCell extends CellScreen.RenderCell {

        protected final AnimationControllerElement.TargetElement targetElement;
        protected final Component label;

        protected TargetEntryCell(@NotNull AnimationControllerElement.TargetElement targetElement, @NotNull Component label) {
            this.targetElement = targetElement;
            this.label = label;
            this.setSearchStringSupplier(() -> this.label.getString());
            this.putMemoryValue("target_id", targetElement.targetElementId);
            this.setSelectable(true);
        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderingUtils.resetShaderColor(graphics);
            int textY = (int) (this.getY() + (this.getHeight() - UIBase.getUITextHeight()) / 2);
            UIBase.renderText(graphics, this.label, this.getX(), textY);
            RenderingUtils.resetShaderColor(graphics);
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            super.updateSize(scrollEntry);
            this.setHeight((int) Math.max(UIBase.getUITextHeight() + 4, 20));
        }
    }

}
