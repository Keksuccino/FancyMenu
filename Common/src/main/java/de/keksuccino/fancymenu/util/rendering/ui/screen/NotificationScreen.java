package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
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
public class NotificationScreen extends Screen {

    protected List<Component> textLines;
    protected DrawableColor headlineColor;
    protected boolean headlineBold = false;
    protected Consumer<Boolean> callback;

    protected ExtendedButton okayButton;

    @NotNull
    public static NotificationScreen error(@NotNull Consumer<Boolean> callback, @NotNull Component... textLines) {
        return ofComponents(callback, textLines).setHeadlineBold(true).setHeadlineColor(UIBase.getUIColorScheme().error_text_color);
    }

    @NotNull
    public static NotificationScreen error(@NotNull Consumer<Boolean> callback, @NotNull String... textLines) {
        return ofStrings(callback, textLines).setHeadlineBold(true).setHeadlineColor(UIBase.getUIColorScheme().error_text_color);
    }

    @NotNull
    public static NotificationScreen warning(@NotNull Consumer<Boolean> callback, @NotNull Component... textLines) {
        return ofComponents(callback, textLines).setHeadlineBold(true).setHeadlineColor(UIBase.getUIColorScheme().warning_text_color);
    }

    @NotNull
    public static NotificationScreen warning(@NotNull Consumer<Boolean> callback, @NotNull String... textLines) {
        return ofStrings(callback, textLines).setHeadlineBold(true).setHeadlineColor(UIBase.getUIColorScheme().warning_text_color);
    }

    @NotNull
    public static NotificationScreen notificationWithHeadline(@NotNull Consumer<Boolean> callback, @NotNull Component... textLines) {
        return ofComponents(callback, textLines).setHeadlineBold(true);
    }

    @NotNull
    public static NotificationScreen notificationWithHeadline(@NotNull Consumer<Boolean> callback, @NotNull String... textLines) {
        return ofStrings(callback, textLines).setHeadlineBold(true);
    }

    @NotNull
    public static NotificationScreen ofStrings(@NotNull Consumer<Boolean> callback, @NotNull String... textLines) {
        NotificationScreen s = new NotificationScreen(callback, new ArrayList<>());
        for (String line : textLines) {
            s.textLines.add(Component.literal(line));
        }
        return s;
    }

    @NotNull
    public static NotificationScreen ofStrings(@NotNull Consumer<Boolean> callback, @NotNull List<String> textLines) {
        return ofStrings(callback, textLines.toArray(new String[0]));
    }

    @NotNull
    public static NotificationScreen ofComponents(@NotNull Consumer<Boolean> callback, @NotNull Component... textLines) {
        return new NotificationScreen(callback, Arrays.asList(textLines));
    }

    @NotNull
    public static NotificationScreen ofComponents(@NotNull Consumer<Boolean> callback, @NotNull List<Component> textLines) {
        return new NotificationScreen(callback, textLines);
    }

    protected NotificationScreen(@NotNull Consumer<Boolean> callback, @NotNull List<Component> textLines) {
        super((!textLines.isEmpty()) ? textLines.get(0) : Component.empty());
        this.callback = callback;
        this.textLines = textLines;
    }

    @Override
    protected void init() {

        this.okayButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.ok"), (button) -> {
            this.callback.accept(true);
        });
        this.addWidget(this.okayButton);
        UIBase.applyDefaultWidgetSkinTo(this.okayButton);

    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screen_background_color.getColorInt());

        int y = (this.height / 2) - ((this.textLines.size() * 14) / 2);
        int lineCounter = 0;
        for (Component c : this.textLines) {
            MutableComponent line = c.copy();
            if (lineCounter == 0) {
                if (this.headlineColor != null) line.setStyle(line.getStyle().withColor(this.headlineColor.getColorInt()));
                if (this.headlineBold) line.setStyle(line.getStyle().withBold(true));
            }
            int textWidth = this.font.width(line);
            this.font.draw(pose, line, (int)((this.width / 2) - (textWidth / 2)), y, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());
            y += 14;
            lineCounter++;
        }

        this.okayButton.setX((this.width / 2) - (this.okayButton.getWidth() / 2));
        this.okayButton.setY(this.height - 40);
        this.okayButton.render(pose, mouseX, mouseY, partial);

        super.render(pose, mouseX, mouseY, partial);

    }

    @Nullable
    public DrawableColor getHeadlineColor() {
        return this.headlineColor;
    }

    public NotificationScreen setHeadlineColor(@Nullable DrawableColor headlineColor) {
        this.headlineColor = headlineColor;
        return this;
    }

    public boolean isHeadlineBold() {
        return this.headlineBold;
    }

    public NotificationScreen setHeadlineBold(boolean headlineBold) {
        this.headlineBold = headlineBold;
        return this;
    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        if (button == InputConstants.KEY_ENTER) {
            this.callback.accept(true);
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    @Override
    public void onClose() {
        this.callback.accept(true);
    }

}