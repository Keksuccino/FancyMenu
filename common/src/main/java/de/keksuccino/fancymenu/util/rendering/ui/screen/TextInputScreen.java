package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class TextInputScreen extends PiPScreen implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 663;
    public static final int PIP_WINDOW_HEIGHT = 294;

    @NotNull
    protected Consumer<String> callback;
    protected ExtendedEditBox input;
    protected ConsumingSupplier<TextInputScreen, Boolean> textValidator = null;
    protected Tooltip textValidatorFeedbackTooltip = null;
    @Nullable
    protected CharacterFilter filter;

    @Nullable
    private String cachedValue;

    public TextInputScreen(@Nullable CharacterFilter filter, @NotNull Consumer<String> callback) {
        super(Component.empty());
        this.callback = callback;
        this.filter = filter;
    }

    @Override
    protected void init() {

        String val = "";
        if (this.input != null) {
            val = this.input.getValue();
        } else if (this.cachedValue != null) {
            val = this.cachedValue;
            this.cachedValue = null;
        }
        this.input = this.addRenderableWidget(new ExtendedEditBox(Minecraft.getInstance().font, (this.width / 2) - 100, (this.height / 2) - 30, 200, 20, Component.empty()));
        this.input.setMaxLength(10000);
        this.input.setCharacterFilter(this.filter);
        this.input.setValue(val);
        UIBase.applyDefaultWidgetSkinTo(this.input);
        this.setupInitialFocusWidget(this, this.input);

        ExtendedButton cancelButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) - 5 - 100, this.height - 40, 100, 20, Component.translatable("fancymenu.common_components.cancel"), button -> {
            this.callback.accept(null);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(cancelButton);

        if (this.textValidatorFeedbackTooltip != null) this.textValidatorFeedbackTooltip.setDefaultStyle();

        ExtendedButton doneButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) + 5, this.height - 40, 100, 20, Component.translatable("fancymenu.common_components.done"), button -> {
            if (this.isTextValid()) {
                this.callback.accept(this.input.getValue());
                this.closeWindow();
            }
        })).setIsActiveSupplier(consumes -> this.isTextValid())
                .setTooltip(this.textValidatorFeedbackTooltip);
        UIBase.applyDefaultWidgetSkinTo(doneButton);

    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();

        RenderSystem.enableBlend();
        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == InputConstants.KEY_ENTER) && this.isTextValid() && (this.input != null) && this.input.isFocused()) {
            this.callback.accept(this.input.getValue());
            this.closeWindow();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public TextInputScreen setText(@Nullable String text) {
        if (text == null) text = "";
        if (this.input != null) {
            this.input.setValue(text);
        } else {
            this.cachedValue = text;
        }
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
