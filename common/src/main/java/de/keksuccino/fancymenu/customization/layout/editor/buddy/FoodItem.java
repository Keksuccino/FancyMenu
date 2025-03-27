package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents a food item that can be fed to the buddy.
 */
public class FoodItem {
    public static final ResourceLocation TEXTURE_FOOD = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/food.png");

    private int x;
    private int y;
    private boolean isDragged = false;
    private int lifetime = 600; // 30 seconds
    private final int size = 16;

    // Reference to the buddy
    private final TamagotchiBuddy buddy;

    public FoodItem(int x, int y, TamagotchiBuddy buddy) {
        this.x = x;
        this.y = y;
        this.buddy = buddy;
    }

    public void render(GuiGraphics graphics) {
        graphics.blit(
                RenderType::guiTextured,
                TEXTURE_FOOD,
                x - size/2, y - size/2,
                0, 0,
                size, size,
                size, size
        );
    }

    public void tick() {
        lifetime--;

        // Fall if not being dragged
        if (!isDragged) {
            y += 1;
            if (y > buddy.getScreenHeight()) {
                lifetime = 0;
            }
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x - size/2 && mouseX < x + size/2 &&
                mouseY >= y - size/2 && mouseY < y + size/2;
    }

    public void pickup(int mouseX, int mouseY) {
        isDragged = true;
        x = mouseX;
        y = mouseY;
    }

    public void drop(int mouseX, int mouseY) {
        isDragged = false;
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

    public boolean shouldRemove() {
        return lifetime <= 0;
    }

    public boolean isNearBuddy(int buddyX, int buddyY) {
        int dx = x - buddyX;
        int dy = y - buddyY;
        return Math.sqrt(dx*dx + dy*dy) < 30;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}