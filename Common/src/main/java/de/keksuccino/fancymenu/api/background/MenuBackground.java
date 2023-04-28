package de.keksuccino.fancymenu.api.background;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nonnull;

public abstract class MenuBackground {

    private final String backgroundIdentifier;
    private final MenuBackgroundType type;
    
    public float opacity = 1.0F;

    /**
     * A menu background.<br><br>
     *
     * Needs to be loaded into a {@link MenuBackgroundType} instance.<br><br>
     *
     * @param uniqueBackgroundIdentifier The <b>unique</b> identifier of the background.
     * @param type The {@link MenuBackgroundType} this background is part of.
     */
    public MenuBackground(@Nonnull String uniqueBackgroundIdentifier, @Nonnull MenuBackgroundType type) {
        this.backgroundIdentifier = uniqueBackgroundIdentifier;
        this.type = type;
    }

    /**
     * Called whenever a new GUI is getting opened containing this background.<br><br>
     *
     * So for example, if you want to reset stuff when a new menu is getting opened, do it here.
     */
    public abstract void onOpenMenu();

    /**
     * Called to render the background in the menu.<br><br>
     *
     * Backgrounds should get rendered at X0 Y0 and in the size of the screen,<br>
     * to cover the full menu background.<br><br>
     *
     * @param matrix The matrix/pose stack used to render the screen.
     * @param screen The screen this background is getting rendered in.
     * @param keepAspectRatio If the background should keep its aspect ratio or if it should get stretched to the screen size.
     */
    public abstract void render(PoseStack matrix, Screen screen, boolean keepAspectRatio);

    /**
     * Called when the Clear/Reset Background button of the editor is pressed to remove this background.
     */
    public void onResetBackground() {
    }

    /**
     * The background identifier of this menu background.
     */
    public String getIdentifier() {
        return this.backgroundIdentifier;
    }

    /**
     * The {@link MenuBackgroundType} this background is part of.
     */
    public MenuBackgroundType getType() {
        return this.type;
    }

}
