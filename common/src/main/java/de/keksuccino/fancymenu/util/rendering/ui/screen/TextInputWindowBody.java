package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class TextInputWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 331;
    public static final int PIP_WINDOW_HEIGHT = 133;
    /** How many suggestions show at once below the input. */
    protected static final int SUGGESTION_LINES = 5;
    protected static final int INPUT_HEIGHT = 20;
    /** The height of the body of a window with {@link #PIP_WINDOW_HEIGHT}, which is the window minus its title bar. */
    private static final int BODY_HEIGHT = PIP_WINDOW_HEIGHT - PiPWindow.DEFAULT_TITLE_BAR_HEIGHT;
    /** Where the input of a body with {@link #BODY_HEIGHT} ends, which centers the input above the buttons (see {@link #init()}). */
    private static final int INPUT_BOTTOM = ((BODY_HEIGHT - 40 - 12 - INPUT_HEIGHT) / 2) + INPUT_HEIGHT;
    /**
     * With suggestions, the input stays where it is in a window with {@link #PIP_WINDOW_HEIGHT}, and the suggestions need 2 pixels of space,
     * 12 pixels per line (see {@link EditBoxSuggestions}) and 12 pixels of space to the buttons, which are 40 pixels above the end of the body.
     */
    public static final int PIP_WINDOW_HEIGHT_WITH_SUGGESTIONS = PiPWindow.DEFAULT_TITLE_BAR_HEIGHT + INPUT_BOTTOM + 2 + (SUGGESTION_LINES * 12) + 12 + 40;

    @NotNull
    protected Consumer<String> callback;
    protected ExtendedEditBox input;
    protected ConsumingSupplier<TextInputWindowBody, Boolean> textValidator = null;
    protected UITooltip textValidatorFeedbackUITooltip = null;
    @Nullable
    protected CharacterFilter filter;
    @Nullable
    protected List<String> inputSuggestions;
    @Nullable
    protected EditBoxSuggestions suggestions;

    @Nullable
    private String cachedValue;

    public TextInputWindowBody(@Nullable CharacterFilter filter, @NotNull Consumer<String> callback) {
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
        boolean hasSuggestions = this.hasInputSuggestions();
        int inputWidth = Math.max(160, this.width - 80);
        int inputX = (this.width - inputWidth) / 2;
        int buttonY = this.height - 40;
        // With suggestions, the input sits where it sits without them and the window is taller, so the suggestions fit between it and the buttons
        int contentBottom = (hasSuggestions ? Math.min(buttonY, BODY_HEIGHT - 40) : buttonY) - 12;
        int inputY = Math.max(16, (contentBottom - INPUT_HEIGHT) / 2);

        this.input = this.addRenderableWidget(new ExtendedEditBox(Minecraft.getInstance().font, inputX, inputY, inputWidth, INPUT_HEIGHT, Component.empty()));
        this.input.setMaxLength(10000);
        this.input.setCharacterFilter(this.filter);
        this.input.setValue(val);
        UIBase.applyDefaultWidgetSkinTo(this.input, UIBase.shouldBlur());
        this.setupInitialFocusWidget(this, this.input);

        if (hasSuggestions) {
            this.suggestions = new EditBoxSuggestions(Minecraft.getInstance(), this, this.input, Minecraft.getInstance().font, false, true, 0, SUGGESTION_LINES, false);
            this.suggestions.setAllowSuggestions(true);
            this.suggestions.enableOnlyCustomSuggestionsMode(true);
            this.suggestions.setSuggestionsRenderPosition(EditBoxSuggestions.SuggestionsRenderPosition.BELOW_EDIT_BOX);
            this.suggestions.setAllowRenderUsage(false);
            this.suggestions.setCustomSuggestions(this.inputSuggestions);
            UIBase.applyDefaultWidgetSkinTo(this.suggestions);
            this.input.setResponder(s -> this.suggestions.updateCommandInfo());
            this.suggestions.updateCommandInfo();
        } else {
            this.suggestions = null;
        }

        ExtendedButton cancelButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) - 5 - 100, buttonY, 100, 20, Component.translatable("fancymenu.common_components.cancel"), button -> {
            this.callback.accept(null);
            this.closeWindow();
        }));
        UIBase.applyDefaultWidgetSkinTo(cancelButton, UIBase.shouldBlur());

        ExtendedButton doneButton = this.addRenderableWidget(new ExtendedButton((this.width / 2) + 5, buttonY, 100, 20, Component.translatable("fancymenu.common_components.done"), button -> {
            if (this.isTextValid()) {
                this.callback.accept(this.input.getValue());
                this.closeWindow();
            }
        })).setIsActiveSupplier(consumes -> this.isTextValid())
                .setUITooltip(this.textValidatorFeedbackUITooltip);
        UIBase.applyDefaultWidgetSkinTo(doneButton, UIBase.shouldBlur());

    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    /**
     * PiP windows never render the background of their body, so the input gets its initial focus here.
     */
    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.performInitialWidgetFocusActionInRender();

        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();

    }

    @Override
    public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.suggestions != null) this.suggestions.render(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        if ((this.suggestions != null) && this.suggestions.mouseClicked(event)) return true;
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if ((this.suggestions != null) && this.suggestions.mouseScrolled(scrollDeltaY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((this.suggestions != null) && this.suggestions.keyPressed(keyCode, scanCode, modifiers)) return true;
        if ((keyCode == InputConstants.KEY_ENTER) && this.isTextValid() && (this.input != null) && this.input.isFocused()) {
            this.callback.accept(this.input.getValue());
            this.closeWindow();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public TextInputWindowBody setText(@Nullable String text) {
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

    public TextInputWindowBody setTextValidator(@Nullable ConsumingSupplier<TextInputWindowBody, Boolean> textValidator) {
        this.textValidator = textValidator;
        return this;
    }

    public TextInputWindowBody setTextValidatorUserFeedback(@Nullable UITooltip feedback) {
        this.textValidatorFeedbackUITooltip = feedback;
        return this;
    }

    /**
     * Sets texts the input suggests while typing, like the names of existing variables. They are only suggestions, any other text works too.
     * Has to get set before the window opens, and the window should get {@link #getPipWindowHeight()} as height, so the suggestions fit in.
     */
    public TextInputWindowBody setInputSuggestions(@Nullable List<String> inputSuggestions) {
        this.inputSuggestions = (inputSuggestions != null) ? List.copyOf(inputSuggestions) : null;
        return this;
    }

    public boolean hasInputSuggestions() {
        return (this.inputSuggestions != null) && !this.inputSuggestions.isEmpty();
    }

    /**
     * @return The height the window of this body needs, which is taller when it has suggestions.
     */
    public int getPipWindowHeight() {
        return this.hasInputSuggestions() ? PIP_WINDOW_HEIGHT_WITH_SUGGESTIONS : PIP_WINDOW_HEIGHT;
    }

}
