package de.keksuccino.fancymenu.customization.layout.editor.buddy.items;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.Random;

/**
 * A ball that the buddy can play with, featuring physics and interaction.
 */
public class PlayBall {

    public static final ResourceLocation TEXTURE_BALL = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/ball.png");
    public static final int CATCH_DISTANCE = 25; // Increased from 15 to make giving the ball easier
    public static final int INACTIVITY_TIMEOUT = 400; // Ball despawns after 20 seconds of no interaction
    public static final int USER_INACTIVITY_TIMEOUT = 200; // Ball despawns after 10 seconds of no USER interaction

    public int x;
    public int y;
    public float velocityX = 0;
    public float velocityY = 0;
    public int inactivityTimer = 0;
    public int userInactivityTimer = 0; // New timer that only tracks user interaction
    public final int size = 12;
    public int kickCooldown = 0;
    public boolean isUp = false; // Whether the ball is being tossed up
    public boolean isRolling = false; // Whether the ball is rolling on ground
    public boolean isGrabbedByBuddy = false; // Whether buddy is holding the ball
    public boolean wasKickedByUser = false; // Whether ball was kicked by user
    public boolean isDragged = false; // Whether ball is being dragged by user
    public float gravity = 0.2f;
    public float groundY; // Ground level for the ball
    public int holdTimer = 0; // Time buddy holds the ball before throwing
    public float playBouncePhase = 0; // Phase for vertical bounce when playing
    public float playBounceSpeed = 0.15f; // Speed of bounce cycle

    // Fields for tracking mouse movement velocity
    public static final int VELOCITY_SAMPLE_SIZE = 5; // Number of samples to track
    public int[] recentMouseX = new int[VELOCITY_SAMPLE_SIZE];
    public int[] recentMouseY = new int[VELOCITY_SAMPLE_SIZE];
    public long[] recentMouseTimes = new long[VELOCITY_SAMPLE_SIZE];
    public int mouseSampleIndex = 0;
    public boolean hasFullSamples = false;

    // Reference to the buddy
    public final TamagotchiBuddy buddy;
    public final Random random;

    public boolean stickToCursor = false;
    public boolean justCreated = true;

    public PlayBall(int x, int y, TamagotchiBuddy buddy) {
        this.x = x;
        this.y = y;
        this.buddy = buddy;
        this.groundY = buddy.getBuddyPosY() + buddy.getSpriteHeight() - size;
        this.inactivityTimer = 0;
        this.userInactivityTimer = 0;
        this.random = new Random();
    }

    /**
     * Records that an interaction happened, resetting both inactivity timers
     */
    public void resetInactivityTimer() {
        inactivityTimer = 0;
    }

    /**
     * Records that a user interaction happened
     */
    public void resetUserInactivityTimer() {
        userInactivityTimer = 0;
        inactivityTimer = 0; // Also reset the regular timer
    }

    public void render(GuiGraphics graphics) {
        // If ball is grabbed, position it relative to buddy
        if (isGrabbedByBuddy) {
            // Position ball above buddy's head during play animation
            x = buddy.getBuddyPosX() + buddy.getSpriteWidth() / 2;

            // Animated vertical movement when playing - make it look like throw and catch
            playBouncePhase += playBounceSpeed;
            if (playBouncePhase > Math.PI * 2) {
                playBouncePhase -= Math.PI * 2;
            }

            // Apply smooth sinusoidal motion with varying amplitude based on animation frame
            float amplitude = 10 + (buddy.getCurrentFrame() % 2) * 5; // Amplitude varies with animation
            y = buddy.getBuddyPosY() - 20 - (int)(Math.sin(playBouncePhase) * amplitude);
        }

        // Add a slight bounce effect when rolling
        int drawY = y;
        if (isRolling && buddy.getAnimationRenderTicks() % 20 < 10) {
            drawY -= 2; // Small bounce when rolling
        }

        // Apply trail effect if moving fast
        float speed = (float)Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (speed > 5 && !isGrabbedByBuddy && !isDragged && !stickToCursor) {
            // Render motion blur/trail (semi-transparent copies behind the ball)
            float trailLength = Math.min(speed / 2, 3); // Cap trail length
            for (int i = 1; i <= trailLength; i++) {
                float alpha = 0.7f - (i / trailLength) * 0.6f; // Fade out trail
                int trailColor = (int)(alpha * 255) << 24 | 0xFFFFFF; // Apply alpha

                int trailX = x - (int)(velocityX * 0.5f * i);
                int trailY = y - (int)(velocityY * 0.5f * i);

                graphics.blit(
                        RenderType::guiTextured,
                        TEXTURE_BALL,
                        trailX - size/2, trailY - size/2,
                        0, 0,
                        size, size,
                        size, size,
                        trailColor
                );
            }
        }

        // Draw the actual ball
        graphics.blit(
                RenderType::guiTextured,
                TEXTURE_BALL,
                x - size/2, drawY - size/2,
                0, 0,
                size, size,
                size, size
        );
    }

    public void tick() {

        this.justCreated = false;

        // Increment inactivity timers
        inactivityTimer++;
        userInactivityTimer++;

        // Skip physics updates if being dragged
        if (isDragged || stickToCursor) {
            if (stickToCursor) {
                x = MouseInput.getMouseX();
                y = MouseInput.getMouseY();
            }
            return;
        }

        // If grabbed by buddy
        if (isGrabbedByBuddy) {
            resetInactivityTimer(); // Buddy interaction counts for general timer
            // But NOT for user inactivity timer!

            holdTimer++;

            // Throw the ball after holding it for a while
            if (holdTimer > 40) {
                buddyThrowBallUp();
            }
            return;
        }

        if (kickCooldown > 0) {
            kickCooldown--;
        }

        // Apply physics when not grabbed
        if (isUp) {
            // Ball is in the air
            velocityY += gravity;
            y += velocityY;
            x += velocityX;

            // Ball hits ground
            if (y >= groundY) {
                y = (int)groundY;
                if (Math.abs(velocityY) < 1.0f) {
                    // Ball starts rolling
                    isUp = false;
                    isRolling = true;
                    velocityX = (buddy.isFacingLeft() ? -1 : 1) * 2;
                } else {
                    // Ball bounces
                    velocityY = -velocityY * 0.6f; // Dampen bounce
                }
            }

            // Ball hits wall
            if (x < 10 || x > buddy.getScreenWidth() - 10) {
                velocityX = -velocityX * 0.8f;
                x = Math.max(10, Math.min(buddy.getScreenWidth() - 10, x));
            }
        } else if (isRolling) {
            // Ball is rolling on ground
            x += velocityX;

            // Ball slows down when rolling
            velocityX *= 0.98f;

            // Stop if very slow
            if (Math.abs(velocityX) < 0.1f) {
                velocityX = 0;
                isRolling = false;
            }

            // Ball hits wall
            if (x < 10 || x > buddy.getScreenWidth() - 10) {
                velocityX = -velocityX * 0.8f;
                x = Math.max(10, Math.min(buddy.getScreenWidth() - 10, x));
            }
        }

        // Check if ball is far from buddy and not being interacted with
        boolean isFarFromBuddy = !isNearBuddy(buddy.getBuddyPosX() + buddy.getSpriteWidth()/2, buddy.getBuddyPosY() + buddy.getSpriteHeight()/2);

        // Start chasing if ball is far and not recently kicked by user
        if (isFarFromBuddy && !buddy.isChasingBall() && !wasKickedByUser) {
            buddy.setChasingBall(true);
            buddy.setPlaying(false);
            resetInactivityTimer(); // Buddy chasing counts as interaction
        }

        // Reset wasKickedByUser flag after some time
        if (wasKickedByUser && kickCooldown <= 0) {
            wasKickedByUser = false;
        }
    }

    /**
     * Let buddy throw the ball up for play
     */
    public void buddyThrowBallUp() {
        if (this.justCreated) return;
        isGrabbedByBuddy = false;
        holdTimer = 0;
        isUp = true;

        resetInactivityTimer(); // Buddy throwing counts as interaction

        // Throw the ball up and slightly forward or backward
        velocityY = -6f;
        velocityX = (buddy.isFacingLeft() ? -2f : 2f) + (random.nextFloat() - 0.5f) * 2f;

        kickCooldown = 30;
    }

    /**
     * Sets the ball as grabbed by buddy
     */
    public void setGrabbedByBuddy(boolean grabbedByBuddy) {
        this.isGrabbedByBuddy = grabbedByBuddy;
        if (grabbedByBuddy) {
            resetInactivityTimer(); // Buddy grabbing counts as interaction
            holdTimer = 0;
            isUp = false;
            isRolling = false;
            velocityX = 0;
            velocityY = 0;
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x - size/2 && mouseX < x + size/2 &&
                mouseY >= y - size/2 && mouseY < y + size/2;
    }

    /**
     * Starts dragging the ball
     */
    public void pickup(int mouseX, int mouseY) {
        if (this.justCreated) return;
        setBeingDragged(true); // Use setter to ensure state is properly set
        resetUserInactivityTimer(); // User interaction

        // Initialize mouse tracking with current position
        for (int i = 0; i < VELOCITY_SAMPLE_SIZE; i++) {
            recentMouseX[i] = mouseX;
            recentMouseY[i] = mouseY;
            recentMouseTimes[i] = System.currentTimeMillis();
        }
        mouseSampleIndex = 0;
        hasFullSamples = false;

        // Set current position
        x = mouseX;
        y = mouseY;

        // If buddy was holding the ball, release it
        if (isGrabbedByBuddy) {
            isGrabbedByBuddy = false;
            buddy.setHoldingBall(false);
        }

        // Increase fun slightly when interacting with the ball
        buddy.increaseFunLevel(2);
    }

    /**
     * Updates position and tracks mouse movement while dragging
     */
    public void updateDragPosition(int mouseX, int mouseY) {
        if (!isDragged) return;

        // Update position
        x = mouseX;
        y = mouseY;

        // Record mouse position for velocity calculation
        long currentTime = System.currentTimeMillis();
        if (currentTime - recentMouseTimes[mouseSampleIndex] > 20) { // Only sample every 20ms
            mouseSampleIndex = (mouseSampleIndex + 1) % VELOCITY_SAMPLE_SIZE;
            recentMouseX[mouseSampleIndex] = mouseX;
            recentMouseY[mouseSampleIndex] = mouseY;
            recentMouseTimes[mouseSampleIndex] = currentTime;

            if (mouseSampleIndex == 0) {
                hasFullSamples = true;
            }
        }
    }

    /**
     * Throws the ball based on recent mouse movement velocity
     */
    public void throwBall(int mouseX, int mouseY) {
        if (this.justCreated) return;
        isDragged = false;
        resetUserInactivityTimer(); // User interaction

        // Calculate throw velocity based on recent mouse movement
        float throwPowerX = 0;
        float throwPowerY = 0;

        if (hasFullSamples) {
            // Calculate velocity from samples
            int oldestIdx = (mouseSampleIndex + 1) % VELOCITY_SAMPLE_SIZE;
            int deltaX = recentMouseX[mouseSampleIndex] - recentMouseX[oldestIdx];
            int deltaY = recentMouseY[mouseSampleIndex] - recentMouseY[oldestIdx];
            long deltaTime = recentMouseTimes[mouseSampleIndex] - recentMouseTimes[oldestIdx];

            // Prevent division by zero and ensure reasonable time delta
            if (deltaTime > 10) {
                // Scale factor controls throw strength
                float velocityScale = 0.2f;
                throwPowerX = deltaX * velocityScale / (deltaTime / 60f);
                throwPowerY = deltaY * velocityScale / (deltaTime / 60f);
            }
        }

        // If velocity is too small, provide a default throw
        if (Math.abs(throwPowerX) < 0.5f && Math.abs(throwPowerY) < 0.5f) {
            throwPowerX = buddy.isFacingLeft() ? -3f : 3f;
            throwPowerY = -4f;
        }

        // Limit max throw power
        float maxPower = 12f;
        float currentPower = (float)Math.sqrt(throwPowerX * throwPowerX + throwPowerY * throwPowerY);
        if (currentPower > maxPower) {
            float scaleFactor = maxPower / currentPower;
            throwPowerX *= scaleFactor;
            throwPowerY *= scaleFactor;
        }

        // Apply throw
        x = mouseX;
        y = mouseY;
        isUp = true;
        velocityX = throwPowerX;
        velocityY = throwPowerY;

        kickCooldown = 20;
        wasKickedByUser = true;

        // If buddy was playing, stop playing and start chasing
        if (buddy.isPlaying()) {
            buddy.setPlaying(false);
            buddy.setChasingBall(true);
        }

        // Bonus fun for throwing the ball
        buddy.increaseFunLevel(10);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isBeingDragged() {
        return isDragged;
    }
    
    /**
     * Explicitly sets the dragged state for the play ball
     */
    public void setBeingDragged(boolean dragged) {
        isDragged = dragged;
    }

    public boolean shouldRemove() {
        // Ball should be removed if general inactivity timeout is reached
        // OR if there's been no user interaction for the shorter timeout
        return inactivityTimer > INACTIVITY_TIMEOUT || userInactivityTimer > USER_INACTIVITY_TIMEOUT;
    }

    public boolean isNearBuddy(int buddyX, int buddyY) {
        int dx = x - buddyX;
        int dy = y - buddyY;
        return Math.sqrt(dx*dx + dy*dy) < CATCH_DISTANCE;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isGrabbedByBuddy() {
        return isGrabbedByBuddy;
    }

}