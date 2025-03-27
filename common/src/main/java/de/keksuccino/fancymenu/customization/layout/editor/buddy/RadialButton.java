package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import net.minecraft.resources.ResourceLocation;
import java.util.function.BooleanSupplier;

/**
 * Button for the radial menu that appears when right-clicking the buddy.
 */
public class RadialButton {
    public static final int BUTTON_SIZE = 24;

    private final ResourceLocation texture;
    private final String name;
    private final Runnable action;
    private final BooleanSupplier activeCondition;

    private int x, y;
    private boolean active = true;

    public RadialButton(ResourceLocation texture, String name, Runnable action, BooleanSupplier activeCondition) {
        this.texture = texture;
        this.name = name;
        this.action = action;
        this.activeCondition = activeCondition;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + BUTTON_SIZE &&
                mouseY >= y && mouseY < y + BUTTON_SIZE;
    }

    public void onClick() {
        if (active && action != null) {
            action.run();
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void updateActiveState() {
        this.active = activeCondition == null || activeCondition.getAsBoolean();
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public static int getButtonSize() {
        return BUTTON_SIZE;
    }
}