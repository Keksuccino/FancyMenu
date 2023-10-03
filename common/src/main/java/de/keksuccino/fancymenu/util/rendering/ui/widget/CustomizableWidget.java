package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface gets applied to the {@link AbstractWidget} class to add a bunch of helper methods for easier customization.
 */
@SuppressWarnings("unused")
public interface CustomizableWidget {

    @Nullable
    default Component getOriginalMessageFancyMenu() {
        Component custom = this.getCustomLabelFancyMenu();
        Component hover = this.getHoverLabelFancyMenu();
        this.setCustomLabelFancyMenu(null);
        this.setHoverLabelFancyMenu(null);
        Component original = null;
        if (this instanceof AbstractWidget w) original = w.getMessage();
        this.setCustomLabelFancyMenu(custom);
        this.setHoverLabelFancyMenu(hover);
        return original;
    }

    default boolean renderCustomBackgroundFancyMenu(@NotNull AbstractWidget widget, @NotNull PoseStack pose, int x, int y, int width, int height) {
        RenderableResource customBackground;
        RenderableResource customBackgroundNormal = this.getCustomBackgroundNormalFancyMenu();
        RenderableResource customBackgroundHover = this.getCustomBackgroundHoverFancyMenu();
        RenderableResource customBackgroundInactive = this.getCustomBackgroundInactiveFancyMenu();
        if (widget.active) {
            if (widget.isHoveredOrFocused()) {
                customBackground = customBackgroundHover;
                if (customBackgroundNormal instanceof PlayableResource p) p.pause();
            } else {
                customBackground = customBackgroundNormal;
                if (customBackgroundHover instanceof PlayableResource p) p.pause();
            }
            if (customBackgroundInactive instanceof PlayableResource p) p.pause();
        } else {
            customBackground = customBackgroundInactive;
            if (customBackgroundNormal instanceof PlayableResource p) p.pause();
            if (customBackgroundHover instanceof PlayableResource p) p.pause();
        }
        boolean renderVanilla = true;
        if (customBackground != null) {
            if (customBackground instanceof PlayableResource p) p.play();
            ResourceLocation location = customBackground.getResourceLocation();
            if (location != null) {
                renderVanilla = false;
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ((IMixinAbstractWidget)widget).getAlphaFancyMenu());
                RenderSystem.enableBlend();
                RenderingUtils.bindTexture(location);
                GuiComponent.blit(pose, x, y, 0.0F, 0.0F, width, height, width, height);
                RenderingUtils.resetShaderColor();
            }
        }
        return renderVanilla;
    }

    default void resetWidgetCustomizationsFancyMenu() {
        if (this.getCustomBackgroundNormalFancyMenu() instanceof PlayableResource p) p.pause();
        if (this.getCustomBackgroundHoverFancyMenu() instanceof PlayableResource p) p.pause();
        if (this.getCustomBackgroundInactiveFancyMenu() instanceof PlayableResource p) p.pause();
        this.setCustomBackgroundNormalFancyMenu(null);
        this.setCustomBackgroundHoverFancyMenu(null);
        this.setCustomBackgroundInactiveFancyMenu(null);
        this.setCustomBackgroundResetBehaviorFancyMenu(CustomBackgroundResetBehavior.RESET_NEVER);
        this.setHoverSoundFancyMenu(null);
        this.setCustomClickSoundFancyMenu(null);
        this.setHiddenFancyMenu(false);
        this.setCustomLabelFancyMenu(null);
        this.setHoverLabelFancyMenu(null);
        this.setCustomWidthFancyMenu(null);
        this.setCustomHeightFancyMenu(null);
        this.setCustomXFancyMenu(null);
        this.setCustomYFancyMenu(null);
    }

    default void resetWidgetSizeAndPositionFancyMenu() {
        this.setCustomXFancyMenu(null);
        this.setCustomYFancyMenu(null);
        this.setCustomWidthFancyMenu(null);
        this.setCustomHeightFancyMenu(null);
    }

    void setCustomLabelFancyMenu(@Nullable Component label);

    @Nullable
    Component getCustomLabelFancyMenu();

    void setHoverLabelFancyMenu(@Nullable Component hoverLabel);

    @Nullable
    Component getHoverLabelFancyMenu();

    void setCustomClickSoundFancyMenu(@Nullable String wavClickSoundPath);

    @Nullable
    String getCustomClickSoundFancyMenu();

    void setHoverSoundFancyMenu(@Nullable String wavHoverSoundPath);

    @Nullable
    String getHoverSoundFancyMenu();

    void setHiddenFancyMenu(boolean hidden);

    boolean isHiddenFancyMenu();

    void setCustomBackgroundNormalFancyMenu(@Nullable RenderableResource background);

    @Nullable
    RenderableResource getCustomBackgroundNormalFancyMenu();

    void setCustomBackgroundHoverFancyMenu(@Nullable RenderableResource background);

    @Nullable
    RenderableResource getCustomBackgroundHoverFancyMenu();

    void setCustomBackgroundInactiveFancyMenu(@Nullable RenderableResource background);

    @Nullable
    RenderableResource getCustomBackgroundInactiveFancyMenu();

    void setCustomBackgroundResetBehaviorFancyMenu(@NotNull CustomBackgroundResetBehavior resetBehavior);

    @NotNull
    CustomBackgroundResetBehavior getCustomBackgroundResetBehaviorFancyMenu();

    @Nullable
    Integer getCustomWidthFancyMenu();

    void setCustomWidthFancyMenu(@Nullable Integer width);

    @Nullable
    Integer getCustomHeightFancyMenu();

    void setCustomHeightFancyMenu(@Nullable Integer height);

    @Nullable
    Integer getCustomXFancyMenu();

    void setCustomXFancyMenu(@Nullable Integer x);

    @Nullable
    Integer getCustomYFancyMenu();

    void setCustomYFancyMenu(@Nullable Integer y);

    enum CustomBackgroundResetBehavior {
        RESET_NEVER,
        RESET_ON_HOVER,
        RESET_ON_UNHOVER,
        RESET_ON_HOVER_AND_UNHOVER
    }

}
