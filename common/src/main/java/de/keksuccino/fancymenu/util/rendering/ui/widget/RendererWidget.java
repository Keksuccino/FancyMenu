package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RendererWidget extends AbstractWidget implements UniqueWidget, NavigatableWidget, FancyMenuWidget {

    @NotNull
    protected RendererWidgetBody body;
    @Nullable
    protected String identifier;

    public RendererWidget(int x, int y, int width, int height, @NotNull RendererWidgetBody body) {
        super(x, y, width, height, Component.empty());
        this.body = body;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.body.render(graphics, mouseX, mouseY, partial, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput var1) {
    }

    public RendererWidget setBody(@NotNull RendererWidgetBody body) {
        this.body = body;
        return this;
    }

    @Override
    @Nullable
    public String getWidgetIdentifierFancyMenu() {
        return this.identifier;
    }

    @Override
    public RendererWidget setWidgetIdentifierFancyMenu(@Nullable String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("RendererWidgets are not focusable!");
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("RendererWidgets are not navigatable!");
    }

    @Override
    public void playDownSound(@NotNull SoundManager $$0) {
        //no click sound
    }

    public float getAlpha() {
        return this.alpha;
    }

    @FunctionalInterface
    public interface RendererWidgetBody {
        void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, int x, int y, int width, int height, @NotNull RendererWidget renderer);
    }

}
