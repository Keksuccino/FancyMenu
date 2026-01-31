package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.UIIconButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NumberPickerWindowBody<N extends Number> extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 360;
    public static final int PIP_WINDOW_HEIGHT = 210;
    private static final long PREVIEW_UPDATE_THROTTLE_MS = 33;
    private static final long ARROW_REPEAT_DELAY_MS = 300L;
    private static final long ARROW_REPEAT_INTERVAL_MS = 50L;

    public enum InputMode {
        FREE_INPUT,
        RANGE_INPUT,
        CYCLE_INPUT
    }

    public interface ValueAdapter<N extends Number> {
        @Nullable
        N parseInput(@NotNull String text);

        @NotNull
        String formatValue(@Nullable N value);

        double toSliderValue(@NotNull N value);

        @NotNull
        N fromSliderValue(double sliderValue);

        boolean showAsInteger();

        int getRoundingDecimalPlaces();
    }

    @NotNull
    private final InputMode inputMode;
    @Nullable
    private final N minValue;
    @Nullable
    private final N maxValue;
    @Nullable
    private final List<N> cycleValues;
    @NotNull
    private final N presetValue;
    @NotNull
    private final CharacterFilter inputFilter;
    @NotNull
    private final ValueAdapter<N> adapter;
    @NotNull
    private final Consumer<N> onValueUpdate;
    @NotNull
    private final Consumer<N> onDone;
    @NotNull
    private final Consumer<N> onCancel;

    private N currentValue;
    private ExtendedEditBox input;
    private RangeSlider slider;
    private ExtendedButton doneButton;
    private ExtendedButton cancelButton;
    @Nullable
    private UIIconButton arrowUpButton;
    @Nullable
    private UIIconButton arrowDownButton;
    private boolean updatingFromSlider = false;
    private boolean updatingFromInput = false;
    private long lastPreviewUpdateTime = 0L;
    @Nullable
    private String lastPreviewValueSignature = null;
    private int holdDirection = 0;
    private boolean holdFromKeyboard = false;
    private boolean holdMouseDown = false;
    private long holdStartTime = 0L;
    private long holdLastRepeatTime = 0L;

    public NumberPickerWindowBody(@NotNull InputMode inputMode, @Nullable N minValue, @Nullable N maxValue, @Nullable List<N> cycleValues, @NotNull N presetValue, @NotNull CharacterFilter inputFilter, @NotNull ValueAdapter<N> adapter, @NotNull Consumer<N> onValueUpdate, @NotNull Consumer<N> onDone, @NotNull Consumer<N> onCancel) {
        super(Component.empty());
        this.inputMode = sanitizeInputMode(inputMode, minValue, maxValue, cycleValues);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.cycleValues = (cycleValues != null) ? new ArrayList<>(cycleValues) : null;
        this.presetValue = presetValue;
        this.currentValue = presetValue;
        this.inputFilter = inputFilter;
        this.adapter = adapter;
        this.onValueUpdate = onValueUpdate;
        this.onDone = onDone;
        this.onCancel = onCancel;
    }

    private static <N extends Number> InputMode sanitizeInputMode(@NotNull InputMode mode, @Nullable N minValue, @Nullable N maxValue, @Nullable List<N> cycleValues) {
        if (mode == InputMode.RANGE_INPUT && (minValue == null || maxValue == null)) {
            return InputMode.FREE_INPUT;
        }
        if (mode == InputMode.CYCLE_INPUT && (cycleValues == null || cycleValues.isEmpty())) {
            return InputMode.FREE_INPUT;
        }
        return mode;
    }

    @Override
    protected void init() {
        int inputHeight = 20;
        int sliderHeight = 20;
        int buttonHeight = 20;
        int gapInputSlider = 8;
        int gapSliderButtons = 18;
        int arrowButtonGap = 4;
        int arrowButtonWidth = 24;
        int arrowButtonHeight = 28;

        int contentHeight = inputHeight + gapInputSlider + sliderHeight + gapSliderButtons + buttonHeight;
        int startY = Math.max(16, (this.height - contentHeight) / 2);

        int inputWidth = Math.max(160, this.width - 80 - (arrowButtonWidth * 2) - (arrowButtonGap * 2));
        int inputGroupWidth = inputWidth + (arrowButtonWidth * 2) + (arrowButtonGap * 2);
        int inputGroupX = (this.width - inputGroupWidth) / 2;
        int arrowUpX = inputGroupX;
        int inputX = inputGroupX + arrowButtonWidth + arrowButtonGap;
        int inputY = startY;
        int arrowButtonY = inputY + ((inputHeight - arrowButtonHeight) / 2);
        int arrowDownX = inputX + inputWidth + arrowButtonGap;
        int sliderY = inputY + inputHeight + gapInputSlider;
        int buttonY = sliderY + sliderHeight + gapSliderButtons;

        this.input = this.addRenderableWidget(new ExtendedEditBox(Minecraft.getInstance().font, inputX, inputY, inputWidth, inputHeight, Component.empty()));
        this.input.setMaxLength(10000);
        this.input.setCharacterFilter(this.inputFilter);
        this.input.setNavigatable(false);
        UIBase.applyDefaultWidgetSkinTo(this.input, UIBase.shouldBlur());

        if (this.inputMode != InputMode.FREE_INPUT) {
            this.input.setEditable(false);
        } else {
            this.input.setResponder(this::onInputChanged);
            this.setupInitialFocusWidget(this, this.input);
        }

        this.arrowUpButton = new UIIconButton(arrowUpX, arrowButtonY, arrowButtonWidth, arrowButtonHeight, MaterialIcons.EXPAND_LESS, button -> this.startArrowHold(1, false));
        this.arrowDownButton = new UIIconButton(arrowDownX, arrowButtonY, arrowButtonWidth, arrowButtonHeight, MaterialIcons.EXPAND_MORE, button -> this.startArrowHold(-1, false));

        this.slider = new RangeSlider(inputX, sliderY, inputWidth, sliderHeight, Component.empty(), 0.0D, 1.0D, 0.5D);
        UIBase.applyDefaultWidgetSkinTo(this.slider, UIBase.shouldBlur());
        this.slider.setIsActiveSupplier(sliderRef -> this.inputMode != InputMode.FREE_INPUT);
        this.slider.setNavigatable(false);

        if (this.inputMode == InputMode.CYCLE_INPUT && this.cycleValues != null && !this.cycleValues.isEmpty()) {
            this.currentValue = resolveCyclePreset(this.currentValue, this.cycleValues);
            this.slider.setMinRangeValue(0.0D);
            this.slider.setMaxRangeValue(this.cycleValues.size() - 1.0D);
            this.slider.setShowAsInteger(true);
            this.slider.setRoundingDecimalPlace(0);
            this.slider.setRangeValue(resolveCycleIndex(this.currentValue, this.cycleValues));
            this.slider.setLabelSupplier(consumes -> Component.literal(this.adapter.formatValue(this.cycleValues.get(resolveCycleIndex(this.currentValue, this.cycleValues)))));
        } else if (this.inputMode == InputMode.RANGE_INPUT && this.minValue != null && this.maxValue != null) {
            double min = Math.min(this.minValue.doubleValue(), this.maxValue.doubleValue());
            double max = Math.max(this.minValue.doubleValue(), this.maxValue.doubleValue());
            this.slider.setMinRangeValue(min);
            this.slider.setMaxRangeValue(max);
            this.slider.setShowAsInteger(this.adapter.showAsInteger());
            this.slider.setRoundingDecimalPlace(this.adapter.getRoundingDecimalPlaces());
            this.slider.setRangeValue(this.adapter.toSliderValue(this.currentValue));
            this.slider.setLabelSupplier(consumes -> Component.literal(this.adapter.formatValue(this.adapter.fromSliderValue(((RangeSlider)consumes).getRangeValue()))));
        } else {
            this.slider.setShowAsInteger(this.adapter.showAsInteger());
            this.slider.setRoundingDecimalPlace(this.adapter.getRoundingDecimalPlaces());
            this.slider.setRangeValue(0.5D);
            this.slider.setLabelSupplier(consumes -> Component.literal(this.adapter.formatValue(this.currentValue)));
        }

        this.slider.setSliderValueUpdateListener((sliderRef, valueDisplayText, value) -> {
            if (this.updatingFromInput) return;
            if (this.inputMode == InputMode.FREE_INPUT) return;
            this.updatingFromSlider = true;
            N resolved = resolveValueFromSlider(((RangeSlider)sliderRef).getRangeValue());
            this.currentValue = resolved;
            updateInputField(resolved);
            this.applyPreviewUpdate(resolved);
            this.updatingFromSlider = false;
        });
        this.addRenderableWidget(this.slider);

        this.cancelButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) - 5 - 100, buttonY, 100, buttonHeight, Component.translatable("fancymenu.common_components.cancel"), button -> {
            this.onCancel.accept(this.presetValue);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());
        this.cancelButton.setNavigatable(false);

        this.doneButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) + 5, buttonY, 100, buttonHeight, Component.translatable("fancymenu.common_components.done"), button -> {
            this.onDone.accept(this.currentValue);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());
        this.doneButton.setNavigatable(false);

        this.updatingFromSlider = true;
        updateInputField(this.currentValue);
        this.updatingFromSlider = false;
    }

    @Override
    public void onWindowClosedExternally() {
        this.onCancel.accept(this.presetValue);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.performInitialWidgetFocusActionInRender();
        RenderSystem.enableBlend();
    }

    @Override
    public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        boolean active = this.isInputActive();
        if (this.arrowUpButton != null && active) {
            this.arrowUpButton.render(graphics, mouseX, mouseY, partial);
        }
        if (this.arrowDownButton != null && active) {
            this.arrowDownButton.render(graphics, mouseX, mouseY, partial);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.tickArrowHold();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ENTER) {
            this.onDone.accept(this.currentValue);
            this.closeWindow();
            return true;
        }
        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.onCancel.accept(this.presetValue);
            this.closeWindow();
            return true;
        }
        if ((keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
            if (this.isInputFocused()) {
                this.startArrowHold(keyCode == InputConstants.KEY_UP ? 1 : -1, true);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
            this.stopArrowHoldFromKeyboard();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean active = this.isInputActive();
        if ((button == 0) && active) {
            if ((this.arrowUpButton != null) && this.arrowUpButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if ((this.arrowDownButton != null) && this.arrowDownButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.stopArrowHoldFromMouse();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void onInputChanged(@NotNull String text) {
        if (this.updatingFromSlider) return;
        if (this.inputMode != InputMode.FREE_INPUT) return;
        N parsed = this.adapter.parseInput(text);
        if (parsed == null) return;
        this.currentValue = parsed;
        this.updatingFromInput = true;
        updateSliderFromValue(parsed);
        this.applyPreviewUpdate(parsed);
        this.updatingFromInput = false;
    }

    private void updateInputField(@NotNull N value) {
        if (this.input == null) return;
        String formatted = this.adapter.formatValue(value);
        this.input.setValue(formatted);
    }

    private void updateSliderFromValue(@NotNull N value) {
        if (this.slider == null) return;
        if (this.inputMode == InputMode.CYCLE_INPUT && this.cycleValues != null && !this.cycleValues.isEmpty()) {
            this.slider.setRangeValue(resolveCycleIndex(value, this.cycleValues));
            return;
        }
        this.slider.setRangeValue(this.adapter.toSliderValue(value));
    }

    private void applyPreviewUpdate(@NotNull N value) {
        String signature = this.adapter.formatValue(value);
        if (signature.equals(this.lastPreviewValueSignature)) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - this.lastPreviewUpdateTime) < PREVIEW_UPDATE_THROTTLE_MS) {
            return;
        }
        this.lastPreviewUpdateTime = now;
        this.lastPreviewValueSignature = signature;
        this.onValueUpdate.accept(value);
    }

    private boolean isInputActive() {
        return (this.input != null) && this.input.active;
    }

    private boolean isInputFocused() {
        return (this.input != null) && this.input.isFocused();
    }

    private void startArrowHold(int direction, boolean fromKeyboard) {
        if (direction == 0) return;
        if (!this.isInputActive()) return;
        if (fromKeyboard && !this.isInputFocused()) return;
        if ((this.holdDirection == direction) && (this.holdFromKeyboard == fromKeyboard)) return;
        this.holdDirection = direction;
        this.holdFromKeyboard = fromKeyboard;
        this.holdMouseDown = !fromKeyboard;
        long now = System.currentTimeMillis();
        this.holdStartTime = now;
        this.holdLastRepeatTime = now;
        this.adjustValueByStep(direction);
    }

    private void stopArrowHoldFromMouse() {
        if (!this.holdFromKeyboard) {
            this.clearArrowHold();
        }
        this.holdMouseDown = false;
    }

    private void stopArrowHoldFromKeyboard() {
        if (this.holdFromKeyboard) {
            this.clearArrowHold();
        }
    }

    private void clearArrowHold() {
        this.holdDirection = 0;
        this.holdFromKeyboard = false;
        this.holdMouseDown = false;
        this.holdStartTime = 0L;
        this.holdLastRepeatTime = 0L;
    }

    private void tickArrowHold() {
        if (this.holdDirection == 0) return;
        if (!this.isInputActive()) {
            this.clearArrowHold();
            return;
        }
        if (this.holdFromKeyboard) {
            if (!this.isInputFocused()) {
                this.clearArrowHold();
                return;
            }
            int keyCode = this.holdDirection > 0 ? InputConstants.KEY_UP : InputConstants.KEY_DOWN;
            if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyCode)) {
                this.clearArrowHold();
                return;
            }
        } else if (!this.holdMouseDown) {
            this.clearArrowHold();
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - this.holdStartTime) < ARROW_REPEAT_DELAY_MS) {
            return;
        }
        if ((now - this.holdLastRepeatTime) >= ARROW_REPEAT_INTERVAL_MS) {
            this.adjustValueByStep(this.holdDirection);
            this.holdLastRepeatTime = now;
        }
    }

    private void adjustValueByStep(int direction) {
        if (direction == 0) return;
        N next = resolveValueAfterStep(direction);
        if (next == null) return;
        this.currentValue = next;
        this.updatingFromSlider = true;
        updateInputField(next);
        updateSliderFromValue(next);
        this.applyPreviewUpdate(next);
        this.updatingFromSlider = false;
    }

    @NotNull
    private N resolveValueAfterStep(int direction) {
        if (this.inputMode == InputMode.CYCLE_INPUT && this.cycleValues != null && !this.cycleValues.isEmpty()) {
            int index = resolveCycleIndex(this.currentValue, this.cycleValues);
            int nextIndex = Mth.clamp(index + direction, 0, this.cycleValues.size() - 1);
            return this.cycleValues.get(nextIndex);
        }
        double value = this.currentValue.doubleValue() + direction;
        if (this.inputMode == InputMode.RANGE_INPUT && (this.minValue != null) && (this.maxValue != null)) {
            double min = Math.min(this.minValue.doubleValue(), this.maxValue.doubleValue());
            double max = Math.max(this.minValue.doubleValue(), this.maxValue.doubleValue());
            value = Mth.clamp(value, min, max);
        }
        return this.adapter.fromSliderValue(value);
    }

    @NotNull
    private N resolveValueFromSlider(double sliderValue) {
        if (this.inputMode == InputMode.CYCLE_INPUT && this.cycleValues != null && !this.cycleValues.isEmpty()) {
            int index = resolveCycleIndex(sliderValue, this.cycleValues.size());
            return this.cycleValues.get(index);
        }
        return this.adapter.fromSliderValue(sliderValue);
    }

    @NotNull
    private N resolveCyclePreset(@NotNull N value, @NotNull List<N> values) {
        int index = values.indexOf(value);
        if (index < 0) {
            return values.get(0);
        }
        return value;
    }

    private static int resolveCycleIndex(@NotNull Number value, @NotNull List<? extends Number> values) {
        int index = values.indexOf(value);
        if (index < 0) return 0;
        return index;
    }

    private static int resolveCycleIndex(double sliderValue, int size) {
        if (size <= 1) return 0;
        int index = (int)Math.round(sliderValue);
        if (index < 0) index = 0;
        if (index >= size) index = size - 1;
        return index;
    }

}
