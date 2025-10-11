package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ConfirmationScreen extends Screen {

    protected List<Component> textLines;
    protected DrawableColor headlineColor;
    protected boolean headlineBold = false;
    protected Consumer<Boolean> callback;
    protected long delayEnd = -1;

    protected ExtendedButton confirmButton;
    protected ExtendedButton cancelButton;

    @NotNull
    public static ConfirmationScreen critical(@NotNull Consumer<Boolean> callback, @NotNull Component... textLines) {
        return ofComponents(callback, textLines).setHeadlineBold(true).setHeadlineColor(UIBase.getUIColorTheme().error_text_color);
    }

    @NotNull
    public static ConfirmationScreen critical(@NotNull Consumer<Boolean> callback, @NotNull String... textLines) {
        return ofStrings(callback, textLines).setHeadlineBold(true).setHeadlineColor(UIBase.getUIColorTheme().error_text_color);
    }

    @NotNull
    public static ConfirmationScreen warning(@NotNull Consumer<Boolean> callback, @NotNull Component... textLines) {
        return ofComponents(callback, textLines).setHeadlineBold(true).setHeadlineColor(UIBase.getUIColorTheme().warning_text_color);
    }

    @NotNull
    public static ConfirmationScreen warning(@NotNull Consumer<Boolean> callback, @NotNull String... textLines) {
        return ofStrings(callback, textLines).setHeadlineBold(true).setHeadlineColor(UIBase.getUIColorTheme().warning_text_color);
    }

    @NotNull
    public static ConfirmationScreen ofStrings(@NotNull Consumer<Boolean> callback, @NotNull String... textLines) {
        ConfirmationScreen s = new ConfirmationScreen(callback, new ArrayList<>());
        for (String line : textLines) {
            s.textLines.add(Component.literal(line));
        }
        return s;
    }

    @NotNull
    public static ConfirmationScreen ofStrings(@NotNull Consumer<Boolean> callback, @NotNull List<String> textLines) {
        return ofStrings(callback, textLines.toArray(new String[0]));
    }

    @NotNull
    public static ConfirmationScreen ofComponents(@NotNull Consumer<Boolean> callback, @NotNull Component... textLines) {
        return new ConfirmationScreen(callback, Arrays.asList(textLines));
    }

    @NotNull
    public static ConfirmationScreen ofComponents(@NotNull Consumer<Boolean> callback, @NotNull List<Component> textLines) {
        return new ConfirmationScreen(callback, textLines);
    }

    protected ConfirmationScreen(@NotNull Consumer<Boolean> callback, @NotNull List<Component> textLines) {
        super((!textLines.isEmpty()) ? textLines.get(0) : Component.empty());
        this.callback = callback;
        this.textLines = textLines;
    }

    @Override
    protected void init() {

        this.confirmButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.ok"), (button) -> {
            this.callback.accept(true);
        }).setForceDefaultTooltipStyle(true).setTooltipSupplier(consumes -> {
            if (!consumes.active) {
                int secs = (int)((this.delayEnd - System.currentTimeMillis()) / 1000);
                return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.ui.confirmation_screen.delay.tooltip", "" + secs));
            }
            return null;
        });
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultWidgetSkinTo(this.confirmButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(false);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        int y = (this.height / 2) - ((this.textLines.size() * 14) / 2);
        int lineCounter = 0;
        for (Component c : this.textLines) {
            MutableComponent line = c.copy();
            if (lineCounter == 0) {
                if (this.headlineColor != null) line.setStyle(line.getStyle().withColor(this.headlineColor.getColorInt()));
                if (this.headlineBold) line.setStyle(line.getStyle().withBold(true));
            }
            int textWidth = this.font.width(line);
            graphics.drawString(this.font, line, (int)((this.width / 2) - (textWidth / 2)), y, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);
            y += 14;
            lineCounter++;
        }

        this.confirmButton.active = this.delayEnd <= System.currentTimeMillis();
        this.confirmButton.setX((this.width / 2) - this.confirmButton.getWidth() - 5);
        this.confirmButton.setY(this.height - 40);
        this.confirmButton.render(graphics, mouseX, mouseY, partial);

        this.cancelButton.setX((this.width / 2) + 5);
        this.cancelButton.setY(this.height - 40);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    public ConfirmationScreen setDelay(long delay) {
        this.delayEnd = System.currentTimeMillis() + delay;
        return this;
    }

    @Nullable
    public DrawableColor getHeadlineColor() {
        return this.headlineColor;
    }

    public ConfirmationScreen setHeadlineColor(@Nullable DrawableColor headlineColor) {
        this.headlineColor = headlineColor;
        return this;
    }

    public boolean isHeadlineBold() {
        return this.headlineBold;
    }

    public ConfirmationScreen setHeadlineBold(boolean headlineBold) {
        this.headlineBold = headlineBold;
        return this;
    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        //ENTER
        if (button == InputConstants.KEY_ENTER) {
            this.callback.accept(true);
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

}
