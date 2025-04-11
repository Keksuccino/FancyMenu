package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RendererWidget extends AbstractWidget implements UniqueWidget, NavigatableWidget {

    @NotNull
    protected RendererWidgetBody body;
    @Nullable
    protected String identifier;

    public RendererWidget(int x, int y, int width, int height, @NotNull RendererWidgetBody body) {
        super(x, y, width, height, Components.empty());
        this.body = body;
    }

    @Override
    public void renderButton(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        this.body.render(pose, mouseX, mouseY, partial, this.x, this.y, this.getWidth(), this.getHeight(), this);
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
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
        void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial, int x, int y, int width, int height, @NotNull RendererWidget renderer);
    }

}
