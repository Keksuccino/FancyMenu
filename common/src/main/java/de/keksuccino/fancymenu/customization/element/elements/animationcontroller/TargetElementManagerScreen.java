package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import net.minecraft.client.Minecraft;
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
        this.element = editorElement.getElement();
        this.parentLayoutEditor = editorElement.editor;
        this.callback = callback;
        this.targets = new ArrayList<>(this.element.targetElements);
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        // Header
        this.addLabelCell(Component.translatable("fancymenu.elements.animation_controller.manage_targets.info")
                .setStyle(Style.EMPTY.withItalic(true)));

        this.addCellGroupEndSpacerCell();
        this.addCellGroupEndSpacerCell();

        // List current targets
        if (this.targets.isEmpty()) {
            this.addLabelCell(Component.translatable("fancymenu.elements.animation_controller.manage_targets.no_targets")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())));
        } else {
            for (AnimationControllerElement.TargetElement target : this.targets) {
                AbstractEditorElement e = this.parentLayoutEditor.getElementByInstanceIdentifier(target.targetElementId);
                MutableComponent label = (e != null) ? e.element.getDisplayName().copy() : Component.literal("---");
                label = label.setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
                label = label.append(Component.literal(" [" + target.targetElementId + "]").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().generic_text_base_color.getColorInt())));
                this.addLabelCell(label);
            }
        }

        this.addStartEndSpacerCell();
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
                                Minecraft.getInstance().setScreen(ConfirmationScreen.warning(result -> {
                                    Minecraft.getInstance().setScreen(this);
                                }, LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.manage_targets.already_exists")));
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

        // Remove target button
        this.addRightSideButton(20, Component.translatable("fancymenu.elements.animation_controller.manage_targets.remove"), button -> {
            ElementSelectorScreen selector = new ElementSelectorScreen(
                    this.parentLayoutEditor,
                    this,
                    this.getElementIds(),
                    selectedElement -> {
                        if (selectedElement != null) {
                            String elementId = selectedElement.element.getInstanceIdentifier();
                            this.targets.removeIf(t -> t.targetElementId.equals(elementId));
                        }
                        Minecraft.getInstance().setScreen(this);
                    },
                    // Only show currently targeted elements
                    element -> this.targets.stream().anyMatch(t -> t.targetElementId.equals(element.element.getInstanceIdentifier()))
            );
            Minecraft.getInstance().setScreen(selector);
        });

        this.addRightSideDefaultSpacer();
    }

    protected List<String> getElementIds() {
        List<String> ids = new ArrayList<>();
        this.targets.forEach(targetElement -> ids.add(targetElement.targetElementId));
        return ids;
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.targets);
    }

}