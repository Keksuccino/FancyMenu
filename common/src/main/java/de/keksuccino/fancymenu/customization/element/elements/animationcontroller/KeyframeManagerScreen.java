package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class KeyframeManagerScreen extends CellScreen {

    protected final AnimationControllerElement element;
    protected final Consumer<List<AnimationKeyframe>> callback;
    protected final List<AnimationKeyframe> keyframesTemp;
    protected KeyframeCell selectedKeyframe;

    public KeyframeManagerScreen(@NotNull AnimationControllerElement element, @NotNull Consumer<List<AnimationKeyframe>> callback) {
        super(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager"));
        this.element = element;
        this.callback = callback;
        this.keyframesTemp = new ArrayList<>(element.getKeyframes());
    }

    @Override
    protected void initCells() {
        this.addStartEndSpacerCell();

        // Add cells for each keyframe
        for (int i = 0; i < this.keyframesTemp.size(); i++) {
            AnimationKeyframe keyframe = this.keyframesTemp.get(i);
            this.addCell(new KeyframeCell(keyframe, i)).setSelectable(true);
        }

        this.addStartEndSpacerCell();
    }

    @Override
    protected void initRightSideWidgets() {
        // Add keyframe button
        this.addRightSideButton(20, Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.add"), button -> {
            AnimationKeyframe newKeyframe = new AnimationKeyframe(
                    this.keyframesTemp.isEmpty() ? 0 : this.keyframesTemp.get(this.keyframesTemp.size() - 1).timestamp + 1000,
                    0, 0, 100, 100,
                    ElementAnchorPoints.TOP_LEFT,
                    false
            );
            this.keyframesTemp.add(newKeyframe);
            this.rebuild();
        });

        this.addRightSideDefaultSpacer();

        // Edit keyframe button
        this.addRightSideButton(20, Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.edit"), button -> {
            if (this.selectedKeyframe != null) {
                EditKeyframeScreen editScreen = new EditKeyframeScreen(
                        this.selectedKeyframe.keyframe,
                        editedKeyframe -> {
                            if (editedKeyframe != null) {
                                int index = this.keyframesTemp.indexOf(this.selectedKeyframe.keyframe);
                                this.keyframesTemp.set(index, editedKeyframe);
                            }
                            Minecraft.getInstance().setScreen(this);
                            this.rebuild();
                        }
                );
                Minecraft.getInstance().setScreen(editScreen);
            }
        }).setIsActiveSupplier(consumes -> this.selectedKeyframe != null);

        // Remove keyframe button
        this.addRightSideButton(20, Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.remove"), button -> {
            if (this.selectedKeyframe != null) {
                Minecraft.getInstance().setScreen(ConfirmationScreen.warning(confirmed -> {
                    if (confirmed) {
                        this.keyframesTemp.remove(this.selectedKeyframe.keyframe);
                        this.selectedKeyframe = null;
                        this.rebuild();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, "Are you sure you want to remove this keyframe?"));
            }
        }).setIsActiveSupplier(consumes -> this.selectedKeyframe != null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        // Update selected keyframe
        RenderCell selected = this.getSelectedCell();
        this.selectedKeyframe = selected instanceof KeyframeCell ? (KeyframeCell) selected : null;

        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        // Sort keyframes by timestamp before saving
        this.keyframesTemp.sort((k1, k2) -> (int)(k1.timestamp - k2.timestamp));
        this.callback.accept(this.keyframesTemp);
    }

    public class KeyframeCell extends LabelCell {

        protected final AnimationKeyframe keyframe;
        protected final int index;

        public KeyframeCell(@NotNull AnimationKeyframe keyframe, int index) {
            super(buildKeyframeLabel(keyframe, index));
            this.keyframe = keyframe;
            this.index = index;
        }

        private static Component buildKeyframeLabel(@NotNull AnimationKeyframe keyframe, int index) {
            return Component.literal(String.format(
                    "Keyframe %d (%.2fs) - Pos(%d, %d) Size(%d, %d) Anchor(%s)",
                    index + 1,
                    keyframe.timestamp / 1000.0f,
                    keyframe.posOffsetX,
                    keyframe.posOffsetY,
                    keyframe.baseWidth,
                    keyframe.baseHeight,
                    keyframe.anchorPoint.getName()
            )).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
        }

    }

    public static class EditKeyframeScreen extends CellScreen {

        protected final AnimationKeyframe keyframe;
        protected final Consumer<AnimationKeyframe> callback;
        protected ElementAnchorPoint selectedAnchorPoint;
        protected boolean selectedSticky;
        protected TextInputCell timestampInput;
        protected TextInputCell posXInput;
        protected TextInputCell posYInput;
        protected TextInputCell widthInput;
        protected TextInputCell heightInput;

        public EditKeyframeScreen(@NotNull AnimationKeyframe keyframe, @NotNull Consumer<AnimationKeyframe> callback) {
            super(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.edit_keyframe"));
            this.keyframe = keyframe;
            this.callback = callback;
            this.selectedAnchorPoint = keyframe.anchorPoint;
            this.selectedSticky = keyframe.stickyAnchor;
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            // Timestamp
            this.addLabelCell(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.timestamp"));
            this.timestampInput = (this.timestampInput == null) ? this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false).setText(String.valueOf(keyframe.timestamp)) : this.addCell(this.timestampInput);

            this.addCellGroupEndSpacerCell();

            // Position
            this.addLabelCell(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.position"));
            this.posXInput = (this.posXInput == null) ? this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false).setText(String.valueOf(keyframe.posOffsetX)) : this.addCell(this.posXInput);
            this.posYInput = (this.posYInput == null) ? this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false).setText(String.valueOf(keyframe.posOffsetY)) : this.addCell(this.posYInput);

            this.addCellGroupEndSpacerCell();

            // Size
            this.addLabelCell(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.size"));
            this.widthInput = (this.widthInput == null) ? this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false).setText(String.valueOf(keyframe.baseWidth)) : this.addCell(this.widthInput);
            this.heightInput = (this.heightInput == null) ? this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false).setText(String.valueOf(keyframe.baseHeight)) : this.addCell(this.heightInput);

            this.addCellGroupEndSpacerCell();

            // Anchor point selector
            this.addLabelCell(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.anchor_point"));
            for (ElementAnchorPoint anchorPoint : ElementAnchorPoints.getAnchorPoints()) {
                this.addCell(new AnchorPointCell(anchorPoint)).setSelectable(true);
            }

            this.addCellGroupEndSpacerCell();

            // Sticky anchor toggle
            this.addCycleButtonCell(CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.sticky", this.selectedSticky),
                    true, (value, button) -> this.selectedSticky = value.getAsBoolean());

            this.addSpacerCell(20);
        }

        @Override
        protected void onCancel() {
            this.callback.accept(null);
        }

        @Override
        protected void onDone() {
            try {
                long timestamp = Long.parseLong(this.timestampInput.getText());
                int posX = Integer.parseInt(this.posXInput.getText());
                int posY = Integer.parseInt(this.posYInput.getText());
                int width = Integer.parseInt(this.widthInput.getText());
                int height = Integer.parseInt(this.heightInput.getText());

                AnimationKeyframe editedKeyframe = new AnimationKeyframe(
                        timestamp, posX, posY, width, height,
                        this.selectedAnchorPoint,
                        this.selectedSticky
                );

                this.callback.accept(editedKeyframe);

            } catch (NumberFormatException e) {
                // Invalid number format - don't save changes
                this.callback.accept(null);
            }
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);

            // Update selected anchor point
            RenderCell selected = this.getSelectedCell();
            if (selected instanceof AnchorPointCell) {
                this.selectedAnchorPoint = ((AnchorPointCell) selected).anchorPoint;
            }
        }

        public class AnchorPointCell extends LabelCell {
            protected final ElementAnchorPoint anchorPoint;

            public AnchorPointCell(@NotNull ElementAnchorPoint anchorPoint) {
                super(anchorPoint.getDisplayName());
                this.anchorPoint = anchorPoint;
            }
        }
    }
}