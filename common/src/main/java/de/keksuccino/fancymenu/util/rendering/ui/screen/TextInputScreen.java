package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class TextInputScreen extends Screen {

    @NotNull
    protected Consumer<String> callback;

    protected ExtendedEditBox input;
    protected ExtendedButton cancelButton;
    protected ExtendedButton doneButton;
    protected ConsumingSupplier<TextInputScreen, Boolean> textValidator = null;
    protected Tooltip textValidatorFeedbackTooltip = null;

    @NotNull
    public static TextInputScreen build(@NotNull Component title, @Nullable CharacterFilter filter, @NotNull Consumer<String> callback) {
        return new TextInputScreen(title, filter, callback);
    }

    public TextInputScreen(@NotNull Component title, @Nullable CharacterFilter filter, @NotNull Consumer<String> callback) {

        super(title);

        this.callback = callback;

        this.input = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 200, 20, Component.empty());
        this.input.setMaxLength(10000);
        this.input.setCharacterFilter(filter);
        UIBase.applyDefaultWidgetSkinTo(this.input);

    }

    @Override
    protected void init() {

        this.addWidget(this.input);
        this.setFocused(this.input);

        this.cancelButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.doneButton = new ExtendedButton(0, 0, 100, 20, Component.translatable("fancymenu.guicomponents.done"), (button) -> {
            if (this.isTextValid()) this.callback.accept(this.input.getValue());
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

         
        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

         
        MutableComponent t = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        int titleWidth = Minecraft.getInstance().font.width(t);
        graphics.drawString(this.font, t, (int)(this.width / 2) - (int)(titleWidth / 2), (int)(this.height / 2) - 30, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.input.setX((this.width / 2) - (this.input.getWidth() / 2));
        this.input.setY((this.height / 2) - (this.input.getHeight() / 2));
        this.input.render(graphics, mouseX, mouseY, partial);

        this.cancelButton.setX((this.width / 2) - 5 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.height - 40);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        this.doneButton.active = this.isTextValid();
        if (this.textValidatorFeedbackTooltip != null) this.textValidatorFeedbackTooltip.setDefaultStyle();
        this.doneButton.setTooltip(this.textValidatorFeedbackTooltip);
        this.doneButton.setX((this.width / 2) + 5);
        this.doneButton.setY(this.height - 40);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        if ((event.key() == InputConstants.KEY_ENTER) && this.isTextValid()) {
            this.callback.accept(this.input.getValue());
            return true;
        }

        return super.keyPressed(event);

    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    public TextInputScreen setText(@Nullable String text) {
        if (text == null) text = "";
        this.input.setValue(text);
        return this;
    }

    public String getText() {
        return this.input.getValue();
    }

    protected boolean isTextValid() {
        if (this.textValidator != null) return this.textValidator.get(this);
        return true;
    }

    public TextInputScreen setTextValidator(@Nullable ConsumingSupplier<TextInputScreen, Boolean> textValidator) {
        this.textValidator = textValidator;
        return this;
    }

    public TextInputScreen setTextValidatorUserFeedback(@Nullable Tooltip feedback) {
        this.textValidatorFeedbackTooltip = feedback;
        return this;
    }

}
