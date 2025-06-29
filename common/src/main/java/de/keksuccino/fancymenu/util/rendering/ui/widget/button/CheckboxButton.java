package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckboxButton extends ExtendedButton {

    public static final ResourceLocation CHECKBOX_BACKGROUND_TEXTURE_NORMAL_DEFAULT = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/widgets/checkbox/background_normal.png");
    public static final ResourceLocation CHECKBOX_BACKGROUND_TEXTURE_HOVER_DEFAULT = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/widgets/checkbox/background_hover.png");
    public static final ResourceLocation CHECKBOX_BACKGROUND_TEXTURE_INACTIVE_DEFAULT = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/widgets/checkbox/background_inactive.png");
    public static final ResourceLocation CHECKBOX_CHECKMARK_TEXTURE_DEFAULT = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/widgets/checkbox/checkmark.png");

    protected boolean checkboxState = false;
    @NotNull
    protected StateChangedAction onStateChanged;
    @Nullable
    protected ITexture customCheckmarkTexture = null;
    @Nullable
    protected ITexture customBackgroundTextureNormal = null;
    @Nullable
    protected ITexture customBackgroundTextureHover = null;
    @Nullable
    protected ITexture customBackgroundTextureInactive = null;

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

        super.renderWidget(graphics, mouseX, mouseY, partial);

        if (this.checkboxState && this.isActive()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.getCheckboxCheckmarkTexture(), this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        }

    }

    @Override
    protected void renderBackground(@NotNull GuiGraphics graphics) {

        graphics.blit(RenderPipelines.GUI_TEXTURED, this.getCheckboxBackground(), this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());

    }

    @Override
    protected void renderLabelText(@NotNull GuiGraphics graphics) {
        // do nothing
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

    public void setCustomBackgroundTextureNormal(@Nullable ITexture customBackgroundTextureNormal) {
        this.customBackgroundTextureNormal = customBackgroundTextureNormal;
    }

    public void setCustomBackgroundTextureHover(@Nullable ITexture customBackgroundTextureHover) {
        this.customBackgroundTextureHover = customBackgroundTextureHover;
    }

    public void setCustomBackgroundTextureInactive(@Nullable ITexture customBackgroundTextureInactive) {
        this.customBackgroundTextureInactive = customBackgroundTextureInactive;
    }

    @NotNull
    public ResourceLocation getCheckboxBackground() {
        if (!this.isActive()) {
            return this.getCheckboxBackgroundTextureInactive();
        }
        if (this.isHoveredOrFocused()) {
            return this.getCheckboxBackgroundTextureHover();
        }
        return this.getCheckboxBackgroundTextureNormal();
    }

    @NotNull
    public ResourceLocation getCheckboxBackgroundTextureNormal() {
        if (this.customBackgroundTextureNormal != null) {
            ResourceLocation loc = this.customBackgroundTextureNormal.getResourceLocation();
            if (loc != null) return loc;
        }
        return CHECKBOX_BACKGROUND_TEXTURE_NORMAL_DEFAULT;
    }

    @NotNull
    public ResourceLocation getCheckboxBackgroundTextureHover() {
        if (this.customBackgroundTextureHover != null) {
            ResourceLocation loc = this.customBackgroundTextureHover.getResourceLocation();
            if (loc != null) return loc;
        }
        return CHECKBOX_BACKGROUND_TEXTURE_HOVER_DEFAULT;
    }

    @NotNull
    public ResourceLocation getCheckboxBackgroundTextureInactive() {
        if (this.customBackgroundTextureInactive != null) {
            ResourceLocation loc = this.customBackgroundTextureInactive.getResourceLocation();
            if (loc != null) return loc;
        }
        return CHECKBOX_BACKGROUND_TEXTURE_INACTIVE_DEFAULT;
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
