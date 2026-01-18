package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class DualTextInputScreen extends PiPScreen implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 420;
    public static final int PIP_WINDOW_HEIGHT = 220;

    @NotNull
    protected Consumer<Pair<String, String>> callback;
    protected ExtendedEditBox input_one;
    protected ExtendedEditBox input_two;
    protected ExtendedButton cancelButton;
    protected ExtendedButton doneButton;
    protected ConsumingSupplier<DualTextInputScreen, Boolean> textValidator = null;
    protected UITooltip textValidatorFeedbackUITooltip = null;
    @NotNull
    protected MutableComponent firstInputLabel;
    @NotNull
    protected MutableComponent secondInputLabel;
    @Nullable
    protected CharacterFilter filter;
    @Nullable
    protected String initialValueOne = null;
    @Nullable
    protected String initialValueTwo = null;
    protected boolean allowPlaceholders = true;

    @NotNull
    public static DualTextInputScreen build(@NotNull Component title, @NotNull Component firstInputLabel, @NotNull Component secondInputLabel, @Nullable CharacterFilter filter, @NotNull Consumer<Pair<String, String>> callback) {
        return new DualTextInputScreen(title, firstInputLabel, secondInputLabel, filter, callback);
    }

    public DualTextInputScreen(@NotNull Component title, @NotNull Component firstInputLabel, @NotNull Component secondInputLabel, @Nullable CharacterFilter filter, @NotNull Consumer<Pair<String, String>> callback) {
        super(Component.empty());
        this.callback = callback;
        this.firstInputLabel = (firstInputLabel instanceof MutableComponent l) ? l : Component.empty();
        this.secondInputLabel = (secondInputLabel instanceof MutableComponent l) ? l : Component.empty();
        this.filter = filter;
    }

    @Override
    protected void init() {

        int editorButtonWidth = 100;
        int editorButtonGap = 5;
        int inputHeight = 20;
        int labelGap = 6;
        int groupGap = 24;

        int totalWidth = Math.max(160, this.width - 80);
        int inputWidth = this.allowPlaceholders ? Math.max(120, totalWidth - editorButtonWidth - editorButtonGap) : totalWidth;
        int groupWidth = inputWidth + (this.allowPlaceholders ? (editorButtonWidth + editorButtonGap) : 0);
        int inputX = (this.width - groupWidth) / 2;
        int editorButtonX = inputX + inputWidth + editorButtonGap;

        int buttonY = this.height - 40;
        int contentHeight = (this.font.lineHeight + labelGap + inputHeight) * 2 + groupGap;
        int contentBottom = buttonY - 12;
        int top = Math.max(16, (contentBottom - contentHeight) / 2);

        int firstLabelY = top;
        int firstInputY = firstLabelY + this.font.lineHeight + labelGap;
        int secondLabelY = firstInputY + inputHeight + groupGap;
        int secondInputY = secondLabelY + this.font.lineHeight + labelGap;

        this.addRenderableWidget(new TextWidget(0, firstLabelY, this.width, 20, this.font, this.firstInputLabel)
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setBaseColor(UIBase.getUITheme().ui_interface_generic_text_color))
                .setShadowEnabled(false);

        String oldValueOne = "";
        if (this.input_one != null) {
            oldValueOne = this.input_one.getValue();
        } else if (this.initialValueOne != null) {
            oldValueOne = this.initialValueOne;
        }
        this.input_one = new ExtendedEditBox(this.font, inputX, firstInputY, inputWidth, inputHeight, Component.empty());
        this.input_one.setMaxLength(10000000);
        this.input_one.setCharacterFilter(this.filter);
        this.input_one.setValue(oldValueOne);
        UIBase.applyDefaultWidgetSkinTo(this.input_one, UIBase.shouldBlur());
        this.addRenderableWidget(this.input_one);
        this.setupInitialFocusWidget(this, this.input_one);

        if (this.allowPlaceholders) {
            UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(editorButtonX, this.input_one.getY(), editorButtonWidth, inputHeight, Component.translatable("fancymenu.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                TextEditorScreen s = new TextEditorScreen(this.firstInputLabel, (this.filter != null) ? this.filter.convertToLegacyFilter() : null, callback -> {
                    if (callback != null) {
                        this.setFirstText(callback);
                    }
                });
                s.setText(this.getFirstText());
                Dialogs.openGeneric(s, this.firstInputLabel, null, TextEditorScreen.PIP_WINDOW_WIDTH, TextEditorScreen.PIP_WINDOW_HEIGHT);
            })), UIBase.shouldBlur());
        }

        this.addRenderableWidget(new TextWidget(0, secondLabelY, this.width, 20, this.font, this.secondInputLabel)
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setBaseColor(UIBase.getUITheme().ui_interface_generic_text_color))
                .setShadowEnabled(false);

        String oldValueTwo = "";
        if (this.input_two != null) {
            oldValueTwo = this.input_two.getValue();
        } else if (this.initialValueTwo != null) {
            oldValueTwo = this.initialValueTwo;
        }
        this.input_two = new ExtendedEditBox(Minecraft.getInstance().font, inputX, secondInputY, inputWidth, inputHeight, Component.empty());
        this.input_two.setMaxLength(10000000);
        this.input_two.setCharacterFilter(this.filter);
        this.input_two.setValue(oldValueTwo);
        UIBase.applyDefaultWidgetSkinTo(this.input_two, UIBase.shouldBlur());
        this.addRenderableWidget(this.input_two);

        if (this.allowPlaceholders) {
            UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(editorButtonX, this.input_two.getY(), editorButtonWidth, inputHeight, Component.translatable("fancymenu.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                TextEditorScreen s = new TextEditorScreen(this.secondInputLabel, (this.filter != null) ? this.filter.convertToLegacyFilter() : null, callback -> {
                    if (callback != null) {
                        this.setSecondText(callback);
                    }
                });
                s.setText(this.getSecondText());
                Dialogs.openGeneric(s, this.secondInputLabel, null, TextEditorScreen.PIP_WINDOW_WIDTH, TextEditorScreen.PIP_WINDOW_HEIGHT);
            })), UIBase.shouldBlur());
        }

        this.cancelButton = new ExtendedButton((this.width / 2) - 5 - 100, buttonY, 100, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(null);
            this.closeWindow();
        });
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());
        this.addRenderableWidget(this.cancelButton);

        this.doneButton = new ExtendedButton((this.width / 2) + 5, buttonY, 100, 20, Component.translatable("fancymenu.common_components.done"), (button) -> {
            if (this.isTextValid()) {
                this.callback.accept(Pair.of(this.getFirstText(), this.getSecondText()));
                this.closeWindow();
            }
        }).setIsActiveSupplier(consumes -> this.isTextValid())
                .setUITooltipSupplier(consumes -> {
                    return this.textValidatorFeedbackUITooltip;
                });
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());
        this.addRenderableWidget(this.doneButton);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.performInitialWidgetFocusActionInRender();
        RenderSystem.enableBlend();
    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        if ((button == InputConstants.KEY_ENTER) && this.isTextValid() && ((this.input_one != null && this.input_one.isFocused()) || (this.input_two != null && this.input_two.isFocused()))) {
            this.callback.accept(Pair.of(this.getFirstText(), this.getSecondText()));
            this.closeWindow();
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    public DualTextInputScreen setFirstText(@Nullable String text) {
        if (text == null) text = "";
        if (this.input_one != null) {
            this.input_one.setValue(text);
        } else {
            this.initialValueOne = text;
        }
        return this;
    }

    public DualTextInputScreen setSecondText(@Nullable String text) {
        if (text == null) text = "";
        if (this.input_two != null) {
            this.input_two.setValue(text);
        } else {
            this.initialValueTwo = text;
        }
        return this;
    }

    @NotNull
    public String getFirstText() {
        if (this.input_one != null) return this.input_one.getValue();
        if (this.initialValueOne != null) return this.initialValueOne;
        return "";
    }

    @NotNull
    public String getSecondText() {
        if (this.input_two != null) return this.input_two.getValue();
        if (this.initialValueTwo != null) return this.initialValueTwo;
        return "";
    }

    protected boolean isTextValid() {
        if (this.textValidator != null) return this.textValidator.get(this);
        return true;
    }

    public DualTextInputScreen setTextValidator(@Nullable ConsumingSupplier<DualTextInputScreen, Boolean> textValidator) {
        this.textValidator = textValidator;
        return this;
    }

    public DualTextInputScreen setTextValidatorUserFeedback(@Nullable UITooltip feedback) {
        this.textValidatorFeedbackUITooltip = feedback;
        return this;
    }

    public boolean isAllowPlaceholders() {
        return allowPlaceholders;
    }

    public DualTextInputScreen setAllowPlaceholders(boolean allowPlaceholders) {
        this.allowPlaceholders = allowPlaceholders;
        return this;
    }

}
