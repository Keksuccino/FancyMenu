package de.keksuccino.fancymenu.customization.layout.editor.buddy.items;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.Buddy;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents a food item that can be fed to the buddy.
 */
public class FoodItem {

    public static final ResourceLocation TEXTURE_FOOD = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/food.png");

    public int x;
    public int y;
    public boolean isDragged = false;
    public int lifetime = 600; // 30 seconds
    public final int size = 16;
    public boolean stickToCursor = false;
    public boolean justCreated = true;

    // Reference to the buddy
    public final Buddy buddy;

    public FoodItem(int x, int y, Buddy buddy) {
        this.x = x;
        this.y = y;
        this.buddy = buddy;
    }

    public void render(GuiGraphics graphics) {
        graphics.blit(
                TEXTURE_FOOD,
                x - size/2, y - size/2,
                0, 0,
                size, size,
                size, size
        );
    }

    public void tick() {

        this.justCreated = false;

        lifetime--;

        // Fall if not being dragged
        if (!this.isDragged && !this.stickToCursor) {
            y += 1;
            if (y > buddy.getScreenHeight()) {
                lifetime = 0;
            }
        } else if (this.stickToCursor) {
            x = MouseInput.getMouseX();
            y = MouseInput.getMouseY();
        }

    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x - size/2 && mouseX < x + size/2 &&
                mouseY >= y - size/2 && mouseY < y + size/2;
    }

    public void pickup(int mouseX, int mouseY) {
        if (this.justCreated) return;
        this.setBeingDragged(true); // Use setter to ensure state is properly set
        x = mouseX;
        y = mouseY;
    }

    public void drop(int mouseX, int mouseY) {
        if (this.justCreated) return;
        this.setBeingDragged(false);
        x = mouseX;
        y = mouseY;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isBeingDragged() {
        return isDragged;
    }
    
    /**
     * Explicitly sets the dragged state for the food item
     */
    public void setBeingDragged(boolean dragged) {
        isDragged = dragged;
    }

    public boolean shouldRemove() {
        return lifetime <= 0;
    }

    public boolean isNearBuddy(int buddyX, int buddyY) {
        int dx = x - buddyX;
        int dy = y - buddyY;
        // Increased detection radius to make feeding easier
        return Math.sqrt(dx*dx + dy*dy) < 40;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}