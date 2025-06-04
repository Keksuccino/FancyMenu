package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.Objects;

public class CheckboxButton extends ExtendedButton {

    public static final DrawableColor CHECKBOX_BORDER_DEFAULT = DrawableColor.of(new Color(38, 38, 38,255));
    public static final DrawableColor CHECKBOX_BACKGROUND_DEFAULT = DrawableColor.of(new Color(56, 56, 56,255));
    public static final ResourceLocation CHECKBOX_CHECKMARK_TEXTURE_DEFAULT = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/widgets/checkbox/checkmark.png");

    protected boolean checkboxState = false;
    @NotNull
    protected StateChangedAction onStateChanged;
    @Nullable
    protected ITexture customCheckmarkTexture = null;
    @Nullable
    protected DrawableColor customBorderColor = null;
    @Nullable
    protected DrawableColor customBackgroundColor = null;

    public CheckboxButton(int x, int y, int width, int height, @NotNull StateChangedAction onStateChanged) {
        super(x, y, width, height, Component.empty(), button -> {});
        this.onStateChanged = onStateChanged;
        this.setPressAction(button -> {
            this.checkboxState = !this.checkboxState;
            onStateChanged.onStateChanged(this, checkboxState);
        });
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.setBackgroundColor(this.getCheckboxBackgroundColor(), this.getCheckboxBackgroundColor(), this.getCheckboxBackgroundColor(), this.getCheckboxBorderColor(), this.getCheckboxBorderColor(), this.getCheckboxBorderColor());

        super.renderWidget(graphics, mouseX, mouseY, partial);

        if (this.checkboxState) {
            graphics.blit(this.getCheckboxCheckmarkTexture(), this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        }

    }

    @NotNull
    public DrawableColor getCheckboxBorderColor() {
        return Objects.requireNonNullElse(this.customBorderColor, CHECKBOX_BORDER_DEFAULT);
    }

    @NotNull
    public DrawableColor getCheckboxBackgroundColor() {
        return Objects.requireNonNullElse(this.customBackgroundColor, CHECKBOX_BACKGROUND_DEFAULT);
    }

    @NotNull
    public ResourceLocation getCheckboxCheckmarkTexture() {
        if (this.customCheckmarkTexture != null) {
            ResourceLocation loc = this.customCheckmarkTexture.getResourceLocation();
            if (loc != null) return loc;
        }
        return CHECKBOX_CHECKMARK_TEXTURE_DEFAULT;
    }

    public void setCustomCheckboxCheckmarkTexture(@Nullable ITexture customCheckmarkTexture) {
        this.customCheckmarkTexture = customCheckmarkTexture;
    }

    public void setCustomCheckboxBorderColor(@Nullable DrawableColor customBorderColor) {
        this.customBorderColor = customBorderColor;
    }

    public void setCustomCheckboxBackgroundColor(@Nullable DrawableColor customBackgroundColor) {
        this.customBackgroundColor = customBackgroundColor;
    }

    public boolean getCheckboxState() {
        return checkboxState;
    }

    public void setCheckboxState(boolean state, boolean callOnStateChanged) {
        this.checkboxState = state;
        if (callOnStateChanged) {
            this.onStateChanged.onStateChanged(this, this.checkboxState);
        }
    }

    @FunctionalInterface
    public interface StateChangedAction {
        void onStateChanged(@NotNull CheckboxButton checkbox, boolean state);
    }

}
