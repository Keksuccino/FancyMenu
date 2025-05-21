package de.keksuccino.fancymenu.customization.layout.editor.buddy.items;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents a poop object that appears after the buddy poops.
 * The poop stays on the ground until the user cleans it up.
 */
public class Poop {

    public static final ResourceLocation TEXTURE_POOP = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/poop.png");

    public int x;
    public int y;
    public final int size = 16;
    public boolean isBeingCleaned = false;
    public int cleaningAnimation = 0;
    public static final int CLEANING_DURATION = 20; // increased from 10 to 20 frames
    
    // Store relative position for screen resizing
    public float relativeX; // Position as percentage of screen width
    public float relativeY; // Position as percentage of screen height (NEW - replaces groundLevel)
    
    // Reference to the buddy
    public final TamagotchiBuddy buddy;

    public Poop(int x, int y, TamagotchiBuddy buddy) {
        this.x = x;
        this.y = y;
        this.buddy = buddy;
        
        // Calculate and store relative coordinates for screen resizing
        int screenWidth = buddy.getScreenWidth();
        int screenHeight = buddy.getScreenHeight();
        
        // If screen dimensions are not valid yet, use reasonable defaults
        if (screenWidth <= 0 || screenHeight <= 0) {
            this.relativeX = 0.5f; // Default to middle of screen horizontally
            this.relativeY = 0.9f; // Default to near bottom of screen vertically
            return;
        }
        
        // Clamp coordinates to be within screen bounds
        this.x = Math.min(Math.max(x, 0), screenWidth);
        this.y = Math.min(Math.max(y, 0), screenHeight);
        
        // Now calculate relative positions using the clamped values
        this.relativeX = (float)this.x / screenWidth;
        this.relativeY = (float)this.y / screenHeight;
    }

    public void render(GuiGraphics graphics) {
        if (isBeingCleaned) {
            // Fade out during cleaning animation
            float alpha = 1.0f - (cleaningAnimation / (float)CLEANING_DURATION);
            int color = (int)(alpha * 255) << 24 | 0xFFFFFF;
            DrawableColor.of(color).setAsShaderColor(graphics);
            graphics.blit(
                TEXTURE_POOP,
                x - size/2, y - size/2,
                0, 0,
                size, size,
                size, size
            );
            RenderingUtils.resetShaderColor(graphics);
        } else {
            graphics.blit(
                TEXTURE_POOP,
                x - size/2, y - size/2,
                0, 0,
                size, size,
                size, size
            );
        }
    }

    public void tick() {
        if (isBeingCleaned) {
            // Increment by 2 instead of 1 to make cleaning faster
            cleaningAnimation += 2;
        }
    }
    
    /**
     * Updates the poop's position when screen size changes
     */
    public void updatePosition(int screenWidth, int screenHeight) {
        // Check if screen dimensions are valid
        if (screenWidth <= 0 || screenHeight <= 0) {
            // Don't update position if screen dimensions are invalid
            return;
        }
        
        // Calculate new position using relative coordinates
        x = (int)(relativeX * screenWidth);
        y = (int)(relativeY * screenHeight);
        
        // Ensure poop is within screen bounds with some margin
        x = Math.min(Math.max(x, 10), screenWidth - 10);
        y = Math.min(Math.max(y, 10), screenHeight - 10);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x - size/2 && mouseX < x + size/2 &&
               mouseY >= y - size/2 && mouseY < y + size/2;
    }

    public void startCleaning() {
        if (!isBeingCleaned) {  // Only start cleaning if not already being cleaned
            isBeingCleaned = true;
            cleaningAnimation = 0;
        } else {
            // If already being cleaned but taking too long, force completion
            cleaningAnimation = CLEANING_DURATION;
        }
    }

    public boolean shouldRemove() {
        return isBeingCleaned && cleaningAnimation >= CLEANING_DURATION;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}