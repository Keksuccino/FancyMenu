package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TamagotchiBuddy is a cute Easter egg that adds a pixel art pet
 * to your Minecraft GUI. It walks along the bottom of the screen,
 * has needs, and reacts to your interactions.
 */
public class TamagotchiBuddy extends AbstractContainerEventHandler implements Renderable, FancyMenuUiComponent {

    private static final Logger LOGGER = LogManager.getLogger();

    // Resource locations for textures
    private static final ResourceLocation TEXTURE_ATLAS = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/buddy_atlas.png");
    private static final ResourceLocation TEXTURE_FOOD = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/food.png");
    private static final ResourceLocation TEXTURE_HEART = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/heart.png");
    private static final ResourceLocation TEXTURE_WORK = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/work.png");
    private static final ResourceLocation TEXTURE_THOUGHT = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/thought.png");

    // Constants
    private static final int SPRITE_WIDTH = 32;
    private static final int SPRITE_HEIGHT = 32;
    private static final int ATLAS_COLUMNS = 4; // 4 animation frames per row
    private static final int ATLAS_ROWS = 6; // Different states (idle, happy, eating, etc.)

    // State indices in the texture atlas (rows)
    private static final int STATE_IDLE = 0;
    private static final int STATE_HAPPY = 1;
    private static final int STATE_SAD = 2;
    private static final int STATE_EATING = 3;
    private static final int STATE_WORKING = 4;
    private static final int STATE_SLEEPING = 5;

    // Game state
    private int posX;
    private int posY;
    private int screenWidth;
    private int screenHeight;
    private boolean facingLeft = false;
    private boolean isVisible = true;
    private boolean isOffScreen = false;

    // Animation
    private int currentState = STATE_IDLE;
    private int currentFrame = 0;
    private int animationTicks = 0;
    private int animationSpeed = 5; // Ticks per frame

    // Needs and stats
    private float hunger = 100.0f;
    private float happiness = 100.0f;
    private float energy = 100.0f;
    private boolean needsFood = false;
    private boolean needsPet = false;
    private boolean needsWork = false;

    // Activity states
    private boolean isBeingPet = false;
    private boolean isEating = false;
    private boolean isWorking = false;
    private boolean isSleeping = false;

    // Timers and behaviors
    private int stateChangeTimer = 0;
    private int actionDuration = 0;
    private int walkSpeed = 2;
    private Random random = new Random();

    // Food item
    private FoodItem droppedFood = null;

    // Event listeners
    private final List<GuiEventListener> children = new ArrayList<>();

    /**
     * Creates a new TamagotchiBuddy
     */
    public TamagotchiBuddy(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Position at bottom of screen
        this.posX = screenWidth / 2;
        this.posY = screenHeight - SPRITE_HEIGHT - 10;

        // Initialize with random timer
        this.stateChangeTimer = random.nextInt(200) + 100;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible) return;

        // Don't render if off screen
        if (isOffScreen) return;

        // Update animation frame
        animationTicks++;
        if (animationTicks >= animationSpeed) {
            animationTicks = 0;
            currentFrame = (currentFrame + 1) % ATLAS_COLUMNS;
        }

        // Calculate texture coordinates
        int texX = currentFrame * SPRITE_WIDTH;
        int texY = currentState * SPRITE_HEIGHT;

        // Render the buddy
        if (facingLeft) {
            // Use our custom method for mirrored rendering
            RenderingUtils.blitMirrored(
                    graphics,
                    TEXTURE_ATLAS,
                    posX, posY,
                    texX, texY,
                    SPRITE_WIDTH, SPRITE_HEIGHT,
                    SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
            );
        } else {
            graphics.blit(
                    RenderType::guiTextured,
                    TEXTURE_ATLAS,
                    posX, posY,
                    texX, texY,
                    SPRITE_WIDTH, SPRITE_HEIGHT,
                    SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
            );
        }

        // Render needs indicator (if needed)
        renderNeedsIndicator(graphics);

        // Render any dropped food
        if (droppedFood != null) {
            droppedFood.render(graphics);
        }

        // Render status bars
        renderStatusBars(graphics);
    }

    /**
     * Renders an indicator above the buddy's head showing its current need
     */
    private void renderNeedsIndicator(GuiGraphics graphics) {
        if (isEating || isBeingPet || isWorking || isSleeping) return;

        int iconSize = 16;
        int iconX = posX + (SPRITE_WIDTH / 2) - (iconSize / 2);
        int iconY = posY - iconSize - 5;

        ResourceLocation icon = null;

        if (needsFood) {
            icon = TEXTURE_FOOD;
        } else if (needsPet) {
            icon = TEXTURE_HEART;
        } else if (needsWork) {
            icon = TEXTURE_WORK;
        }

        if (icon != null) {
            // First render the thought bubble
            graphics.blit(
                    RenderType::guiTextured,
                    TEXTURE_THOUGHT,
                    iconX - 4, iconY - 4,
                    0, 0,
                    iconSize + 8, iconSize + 8,
                    iconSize + 8, iconSize + 8
            );

            // Then render the icon inside it
            graphics.blit(
                    RenderType::guiTextured,
                    icon,
                    iconX, iconY,
                    0, 0,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        }
    }

    /**
     * Renders status bars showing the buddy's current stats
     */
    private void renderStatusBars(GuiGraphics graphics) {
        int barWidth = 40;
        int barHeight = 4;
        int barX = posX + (SPRITE_WIDTH / 2) - (barWidth / 2);
        int barY = posY - 25;
        int spacing = 6;

        // Hunger (red)
        renderBar(graphics, barX, barY, barWidth, barHeight, hunger/100f, DrawableColor.of(255, 80, 80));

        // Happiness (green)
        renderBar(graphics, barX, barY + spacing, barWidth, barHeight, happiness/100f, DrawableColor.of(80, 255, 80));

        // Energy (blue)
        renderBar(graphics, barX, barY + spacing*2, barWidth, barHeight, energy/100f, DrawableColor.of(80, 80, 255));
    }

    /**
     * Helper method to render a status bar
     */
    private void renderBar(GuiGraphics graphics, int x, int y, int width, int height, float fill, DrawableColor color) {
        // Bar background
        graphics.fill(x, y, x + width, y + height, 0x80000000);

        // Bar fill
        int fillWidth = Math.max(1, (int)(width * fill));
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, color.getColorInt());
        }
    }

    /**
     * Updates the buddy's state, position, and behaviors
     */
    public void tick() {
        if (!isVisible) return;

        // Update stats over time
        updateStats();

        // Update food item if it exists
        if (droppedFood != null) {
            droppedFood.tick();

            // Check if food is close enough to eat
            if (droppedFood.isNearBuddy(posX + SPRITE_WIDTH/2, posY + SPRITE_HEIGHT/2)) {
                eatFood();
                droppedFood = null;
            }

            // Remove food if expired
            if (droppedFood != null && droppedFood.shouldRemove()) {
                droppedFood = null;
            }
        }

        // Update state change timer
        stateChangeTimer--;
        if (stateChangeTimer <= 0) {
            decideNextBehavior();
            stateChangeTimer = random.nextInt(300) + 100;
        }

        // Update action duration timer
        if (actionDuration > 0) {
            actionDuration--;
            if (actionDuration <= 0) {
                // End current action
                isEating = false;
                isBeingPet = false;
                isWorking = false;

                // Don't end sleep here, it ends when energy is full
                if (!isSleeping) {
                    currentState = STATE_IDLE;
                }
            }
        }

        // End sleeping if energy is full
        if (isSleeping && energy >= 100) {
            isSleeping = false;
            currentState = STATE_IDLE;
        }

        // Update movement if not doing an action
        if (!isEating && !isBeingPet && !isWorking && !isSleeping) {
            updateMovement();
        }

        // Update current state visualization
        updateVisualState();
    }

    /**
     * Updates the buddy's stats over time
     */
    private void updateStats() {
        // Decrease stats over time
        hunger = Math.max(0, hunger - 0.05f);
        happiness = Math.max(0, happiness - 0.03f);

        // Energy decreases unless sleeping
        if (isSleeping) {
            energy = Math.min(100, energy + 0.3f);
        } else {
            energy = Math.max(0, energy - 0.02f);

            // Working drains energy faster
            if (isWorking) {
                energy = Math.max(0, energy - 0.1f);
            }
        }

        // Set needs based on stats
        needsFood = hunger < 30;
        needsPet = happiness < 30;
        needsWork = energy > 50 && random.nextFloat() < 0.005f;

        // Auto-sleep if energy is critically low
        if (energy < 10 && !isSleeping) {
            isSleeping = true;
            isEating = false;
            isBeingPet = false;
            isWorking = false;
            currentState = STATE_SLEEPING;
        }
    }

    /**
     * Updates the buddy's movement
     */
    private void updateMovement() {
        if (facingLeft) {
            posX -= walkSpeed;

            // Check if we should turn around or go off screen
            if (posX < -SPRITE_WIDTH) {
                isOffScreen = true;
                // Random chance to come back
                if (random.nextFloat() < 0.01f) {
                    facingLeft = false;
                    isOffScreen = false;
                    posX = -SPRITE_WIDTH;
                }
            } else if (posX < 0 && random.nextFloat() < 0.01f) {
                facingLeft = false;
            }
        } else {
            posX += walkSpeed;

            // Check if we should turn around or go off screen
            if (posX > screenWidth) {
                isOffScreen = true;
                // Random chance to come back
                if (random.nextFloat() < 0.01f) {
                    facingLeft = true;
                    isOffScreen = false;
                    posX = screenWidth;
                }
            } else if (posX > screenWidth - SPRITE_WIDTH && random.nextFloat() < 0.01f) {
                facingLeft = true;
            }
        }
    }

    /**
     * Decides what behavior the buddy should exhibit next
     */
    private void decideNextBehavior() {
        // Don't change behavior if off screen or sleeping
        if (isOffScreen || isSleeping || isEating || isBeingPet || isWorking) return;

        float chance = random.nextFloat();

        // Behaviors based on needs
        if (needsFood && chance < 0.4f) {
            // Look sad when hungry
            currentState = STATE_SAD;
        } else if (needsPet && chance < 0.4f) {
            // Look sad when needing attention
            currentState = STATE_SAD;
        } else if (chance < 0.1f) {
            // Small chance to decide to leave screen
            if (posX < screenWidth / 2) {
                facingLeft = true;
            } else {
                facingLeft = false;
            }
        } else if (energy < 30 && chance < 0.3f) {
            // Get sleepy when low energy
            isSleeping = true;
            currentState = STATE_SLEEPING;
        } else {
            // Just walk around randomly
            if (chance < 0.5f) {
                facingLeft = !facingLeft;
            }
        }
    }

    /**
     * Updates the visual state based on current actions
     */
    private void updateVisualState() {
        if (isSleeping) {
            currentState = STATE_SLEEPING;
        } else if (isEating) {
            currentState = STATE_EATING;
        } else if (isBeingPet) {
            currentState = STATE_HAPPY;
        } else if (isWorking) {
            currentState = STATE_WORKING;
        } else if (needsFood || needsPet) {
            currentState = STATE_SAD;
        } else {
            currentState = STATE_IDLE;
        }
    }

    /**
     * Called when the buddy eats a food item
     */
    private void eatFood() {
        isEating = true;
        actionDuration = 60; // 3 seconds
        hunger = Math.min(100, hunger + 40);
        happiness = Math.min(100, happiness + 10);
        currentState = STATE_EATING;
    }

    /**
     * Called when the buddy is petted
     */
    public void pet() {
        if (isSleeping) {
            // Waking up
            isSleeping = false;
            happiness += 5;
        } else {
            isBeingPet = true;
            actionDuration = 40; // 2 seconds
            happiness = Math.min(100, happiness + 30);
            currentState = STATE_HAPPY;
        }
    }

    /**
     * Makes the buddy start working
     */
    public void startWorking() {
        if (!isSleeping && energy > 20) {
            isWorking = true;
            actionDuration = 100; // 5 seconds
            currentState = STATE_WORKING;
            hunger = Math.max(0, hunger - 10); // Working makes buddy hungry
        }
    }

    /**
     * Creates and drops a food item at the specified position
     */
    public void dropFoodAt(int x, int y) {
        if (droppedFood == null) {
            droppedFood = new FoodItem(x, y);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (!isVisible || isOffScreen) return false;

        if (button == 0) { // Left click
            // Check if clicked on buddy
            if (isMouseOverBuddy(mouseX, mouseY)) {
                pet();
                return true;
            }

            // Check if clicked on food
            if (droppedFood != null && droppedFood.isMouseOver(mouseX, mouseY)) {
                droppedFood.pickup((int)mouseX, (int)mouseY);
                return true;
            }

            // Drop food if clicked elsewhere
            if (droppedFood == null) {
                dropFoodAt((int)mouseX, (int)mouseY);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && droppedFood != null && droppedFood.isBeingDragged()) {
            droppedFood.drop((int)mouseX, (int)mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && droppedFood != null && droppedFood.isBeingDragged()) {
            droppedFood.setPosition((int)mouseX, (int)mouseY);
            return true;
        }
        return false;
    }

    /**
     * Checks if the mouse is over the buddy
     */
    private boolean isMouseOverBuddy(double mouseX, double mouseY) {
        return mouseX >= posX && mouseX < posX + SPRITE_WIDTH &&
                mouseY >= posY && mouseY < posY + SPRITE_HEIGHT;
    }

    /**
     * Returns whether the buddy needs work
     */
    public boolean needsWork() {
        return needsWork;
    }

    /**
     * Sets the screen dimensions for the buddy
     */
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.posY = height - SPRITE_HEIGHT - 10; // Keep at bottom
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(posX, posY, SPRITE_WIDTH, SPRITE_HEIGHT);
    }

    /**
     * Food item class for the buddy to eat
     */
    private class FoodItem {
        private int x;
        private int y;
        private boolean isDragged = false;
        private int lifetime = 600; // 30 seconds
        private int size = 16;

        public FoodItem(int x, int y) {
            this.x = x;
            this.y = y;
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
                if (y > screenHeight) {
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
    }
}