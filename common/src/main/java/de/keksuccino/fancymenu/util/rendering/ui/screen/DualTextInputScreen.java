package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class DualTextInputScreen extends Screen {

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
        super(title);
        this.callback = callback;
        this.firstInputLabel = (firstInputLabel instanceof MutableComponent l) ? l : Component.empty();
        this.secondInputLabel = (secondInputLabel instanceof MutableComponent l) ? l : Component.empty();
        this.filter = filter;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int editorButtonWidth = 100;
        int inputWidth = this.allowPlaceholders ? 150 : 250;
        int inputWidthHalf = inputWidth / 2;
        int inputX = (centerX - inputWidthHalf) - (this.allowPlaceholders ? (editorButtonWidth / 2) : 0);

        this.addRenderableWidget(new TextWidget(0, centerY - 43, this.width, 20, this.font, this.firstInputLabel)
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setBaseColor(UIBase.getUIColorTheme().generic_text_base_color))
                .setShadowEnabled(false);

        String oldValueOne = "";
        if (this.input_one != null) {
            oldValueOne = this.input_one.getValue();
        } else if (this.initialValueOne != null) {
            oldValueOne = this.initialValueOne;
        }
        this.input_one = new ExtendedEditBox(this.font, inputX, centerY - 30, inputWidth, 20, Component.empty());
        this.input_one.setMaxLength(10000000);
        this.input_one.setCharacterFilter(this.filter);
        this.input_one.setValue(oldValueOne);
        UIBase.applyDefaultWidgetSkinTo(this.input_one);
        this.addRenderableWidget(this.input_one);
        this.setFocused(this.input_one);

        if (this.allowPlaceholders) {
            UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(this.input_one.getX() + this.input_one.getWidth() + 5, this.input_one.getY(), editorButtonWidth, 20, Component.translatable("fancymenu.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                TextEditorScreen s = new TextEditorScreen((this.filter != null) ? this.filter.convertToLegacyFilter() : null, callback -> {
                    if (callback != null) {
                        this.setFirstText(callback);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                s.setText(this.getFirstText());
                Minecraft.getInstance().setScreen(s);
            })));
        }

        this.addRenderableWidget(new TextWidget(0, centerY + 27, this.width, 20, this.font, this.secondInputLabel)
                .centerWidget(this)
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .setBaseColor(UIBase.getUIColorTheme().generic_text_base_color))
                .setShadowEnabled(false);

        String oldValueTwo = "";
        if (this.input_two != null) {
            oldValueTwo = this.input_two.getValue();
        } else if (this.initialValueTwo != null) {
            oldValueTwo = this.initialValueTwo;
        }
        this.input_two = new ExtendedEditBox(Minecraft.getInstance().font, inputX, centerY + 40, inputWidth, 20, Component.empty());
        this.input_two.setMaxLength(10000000);
        this.input_two.setCharacterFilter(this.filter);
        this.input_two.setValue(oldValueTwo);
        UIBase.applyDefaultWidgetSkinTo(this.input_two);
        this.addRenderableWidget(this.input_two);

        if (this.allowPlaceholders) {
            UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(this.input_two.getX() + this.input_two.getWidth() + 5, this.input_two.getY(), editorButtonWidth, 20, Component.translatable("fancymenu.ui.screens.string_builder_screen.edit_in_editor"), button -> {
                TextEditorScreen s = new TextEditorScreen((this.filter != null) ? this.filter.convertToLegacyFilter() : null, callback -> {
                    if (callback != null) {
                        this.setSecondText(callback);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                s.setText(this.getSecondText());
                Minecraft.getInstance().setScreen(s);
            })));
        }

        this.cancelButton = new ExtendedButton((this.width / 2) - 5 - 100, centerY + 90, 100, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.onClose();
        });
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);
        this.addRenderableWidget(this.cancelButton);

        this.doneButton = new ExtendedButton((this.width / 2) + 5, centerY + 90, 100, 20, Component.translatable("fancymenu.common_components.done"), (button) -> {
            if (this.isTextValid()) this.onDone();
        }).setIsActiveSupplier(consumes -> this.isTextValid())
                .setUITooltipSupplier(consumes -> {
                    return this.textValidatorFeedbackUITooltip;
                });
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);
        this.addRenderableWidget(this.doneButton);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();
        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().interface_background_color.getColorInt());

        MutableComponent t = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        int titleWidth = Minecraft.getInstance().font.width(t);
        graphics.drawString(this.font, t, (this.width / 2) - (titleWidth / 2), 30, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        if ((button == InputConstants.KEY_ENTER) && this.isTextValid()) {
            this.onDone();
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    protected void onDone() {
        this.callback.accept(Pair.of(this.getFirstText(), this.getSecondText()));
    }

    @Override
    public void onClose() {
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
