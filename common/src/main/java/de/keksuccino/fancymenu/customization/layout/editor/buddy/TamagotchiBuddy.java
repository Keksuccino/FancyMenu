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
    private static final ResourceLocation TEXTURE_HEART = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/heart.png");
    private static final ResourceLocation TEXTURE_PLAY = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/play.png");
    private static final ResourceLocation TEXTURE_THOUGHT = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/thought.png");
    private static final ResourceLocation TEXTURE_FEED_BUTTON = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/feed_button.png");
    private static final ResourceLocation TEXTURE_PLAY_BUTTON = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/play_button.png");

    // Constants
    private static final int SPRITE_WIDTH = 32;
    private static final int SPRITE_HEIGHT = 32;
    private static final int ATLAS_COLUMNS = 4; // 4 animation frames per row
    private static final int ATLAS_ROWS = 13; // Different states (idle, happy, eating, etc.) - now 13 rows with looking around and grumpy
    private static final int CHASE_SPEED = 3; // Faster than normal walk speed

    // State indices in the texture atlas (rows)
    private static final int STATE_IDLE_WALK = 0;
    private static final int STATE_HAPPY_STAND = 1;
    private static final int STATE_SAD_WALK = 2;
    private static final int STATE_EATING_STAND = 3;
    private static final int STATE_PLAYING_STAND = 4; // Reusing work animation for play
    private static final int STATE_SLEEPING = 5;
    private static final int STATE_IDLE_STAND = 6;
    private static final int STATE_SAD_STAND = 7;
    private static final int STATE_STRETCHING = 8;  // New state for stretching animation
    private static final int STATE_EXCITED = 9;     // New state for excitement animation
    private static final int STATE_POOPING = 10;    // New state for pooping animation
    private static final int STATE_LOOKING_AROUND = 11; // New state for looking around animation
    private static final int STATE_GRUMPY = 12;    // New state for grumpy animation when woken up

    // Game state
    private int posX;
    private int posY;
    private int screenWidth;
    private int screenHeight;
    private boolean facingLeft = false;
    private boolean isVisible = true;
    private boolean isOffScreen = false;
    private boolean isStandingIdle = false;
    private TamagotchiBuddyGui gui; // New GUI to replace radial menu

    // Animation
    private int currentState = STATE_IDLE_WALK;
    private int currentFrame = 0;
    private int animationTicks = 0;
    private int animationSpeed = 5; // Ticks per frame for regular animations
    private int lookingAroundAnimationSpeed = 40; // Slower animation for looking around (2 sec per frame)
    private float hopAnimationCounter = 0; // Dedicated counter for hop animation to ensure smooth motion
    private float hopAnimationSpeed = 0.3f; // Speed of hop animation cycle

    // Needs and stats
    private float hunger = 100.0f;
    private float happiness = 100.0f;
    private float energy = 100.0f;
    private float funLevel = 100.0f; // NEW: Separate fun meter
    private boolean needsFood = false;
    private boolean needsPet = false;
    private boolean needsPlay = false;

    // Activity states
    private boolean isBeingPet = false;
    private boolean isEating = false;
    private boolean isPlaying = false;
    private boolean isSleeping = false;
    private boolean isChasingBall = false;
    private boolean isHoldingBall = false;

    // New movement states
    private boolean isStanding = false;
    private boolean isHopping = false;
    private boolean isLookingAround = false;
    private boolean isStretching = false;
    private boolean isExcited = false;
    private boolean isGrumpy = false;
    private int activityDuration = 0;
    private int hopHeight = 0;
    private int lookDirection = 0; // -1 for left, 0 for center, 1 for right
    private int stretchFrame = 0;

    // Timers and behaviors
    private int stateChangeTimer = 0;
    private int actionDuration = 0;
    private int walkSpeed = 2;
    private Random random = new Random();

    // Movement variation parameters
    private int minWalkDistance = 30;  // Minimum pixels to walk before considering a turn
    private int pixelsSinceLastDirectionChange = 0;
    private int maxWalkDistance = 200; // Maximum pixels to walk before forcing a behavior change
    private float standChance = 0.01f;   // Reduced chance to stop and stand
    private float walkChance = 0.08f;    // Increased chance to start walking again when standing
    private float hopChance = 0.003f;  // Reduced chance per tick to hop while walking (makes it more special)
    private float lookChance = 0.002f;  // Chance per tick to look around
    private float stretchChance = 0.001f; // Chance per tick to stretch
    private float excitedChance = 0.001f; // Chance per tick to get excited
    private int minStandTime = 30;     // Shorter minimum ticks to stand
    private int maxStandTime = 80;     // Shorter maximum ticks to stand

    // Food item and play ball
    private FoodItem droppedFood = null;
    private PlayBall playBall = null;

    // Poop system
    private List<Poop> poops = new ArrayList<>();
    private boolean isPooping = false;
    private int timeSinceLastPoop = 0;
    private int poopingInterval = 1500; // Time between potential poops (in ticks)
    private float poopChance = 0.05f; // Chance to poop when the interval is reached
    private static final int MAX_POOPS_BEFORE_SAD = 3; // Buddy gets sad if there are too many poops

    // Event listeners
    private final List<GuiEventListener> children = new ArrayList<>();

    private long lastRenderDebugOut = -1L;

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

        // Initialize GUI
        this.gui = new TamagotchiBuddyGui(this);
    }

    // Radial menu buttons have been replaced by the GUI

    // Track visibility changes
    private boolean wasVisible = true;
    private boolean wasOffScreen = false;

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Log visibility changes
        if (wasVisible != isVisible) {
            LOGGER.info("Buddy visibility changed: {} -> {}", wasVisible, isVisible);
            wasVisible = isVisible;
        }

        // Check if renderer is completely disabled
        if (!isVisible) {
            // Even when buddy is invisible, we still need to render poops
            renderPoops(graphics);
            return;
        }

        // Log off-screen changes
        if (wasOffScreen != isOffScreen) {
            LOGGER.info("Buddy off-screen state changed: {} -> {}", wasOffScreen, isOffScreen);
            wasOffScreen = isOffScreen;
        }

        // Always render poops regardless of buddy visibility
        renderPoops(graphics);
        
        // Don't render buddy if off screen
        if (isOffScreen) return;

        // Log render position and state
        long now = System.currentTimeMillis();
        if ((lastRenderDebugOut + 200) < now) {  // Only log periodically to avoid spam
            LOGGER.info("Rendering buddy: pos=({},{}), state={}, frame={}, hopping={}",
                    posX, posY, getStateName(currentState), currentFrame, isHopping);
            lastRenderDebugOut = now;
        }

        // Update animation frame
        animationTicks++;
        // Use different animation speeds for different states
        int currentAnimationSpeed = isLookingAround ? lookingAroundAnimationSpeed : animationSpeed;
        if (animationTicks >= currentAnimationSpeed) {
            animationTicks = 0;
            currentFrame = (currentFrame + 1) % ATLAS_COLUMNS;
        }

        // Calculate texture coordinates
        int texX = currentFrame * SPRITE_WIDTH;
        int texY = currentState * SPRITE_HEIGHT;

        // Render the play ball if it exists (non-dragged balls render before buddy)
        if (playBall != null && !playBall.isBeingDragged()) {
            playBall.render(graphics);
        }

        try {
            if (isHopping && currentState != STATE_EXCITED) {
                // For hopping animation, we'll just use a simple Y offset without squash/stretch

                // Calculate a simple vertical hop offset using sine wave
                float hopOffset = (float)Math.sin(hopAnimationCounter) * 10.0f;

                // Apply the vertical offset to the rendering position
                int renderY = posY - (int)hopOffset;

                // Regular rendering but with adjusted Y position
                if (facingLeft) {
                    // Use our custom method for mirrored rendering
                    RenderingUtils.blitMirrored(
                            graphics,
                            TEXTURE_ATLAS,
                            posX, renderY,
                            texX, texY,
                            SPRITE_WIDTH, SPRITE_HEIGHT,
                            SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                    );
                } else {
                    // Standard rendering
                    graphics.blit(
                            RenderType::guiTextured,
                            TEXTURE_ATLAS,
                            posX, renderY,
                            texX, texY,
                            SPRITE_WIDTH, SPRITE_HEIGHT,
                            SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                    );
                }
            } else {
                // Normal rendering without hopping offset
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
                    // Standard rendering
                    graphics.blit(
                            RenderType::guiTextured,
                            TEXTURE_ATLAS,
                            posX, posY,
                            texX, texY,
                            SPRITE_WIDTH, SPRITE_HEIGHT,
                            SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                    );
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error during buddy rendering!", ex);
            // Always show basic rendering if there's an error
            graphics.blit(
                    RenderType::guiTextured,
                    TEXTURE_ATLAS,
                    posX, posY,
                    texX, texY,
                    SPRITE_WIDTH, SPRITE_HEIGHT,
                    SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
            );
        }

        // Removed looking direction indicator in favor of a dedicated texture animation

        // Render needs indicator
        if (!isEating && !isBeingPet && !isPlaying && !isSleeping) {
            renderNeedsIndicator(graphics);
        }

        // Render dragged play ball (after buddy, so it appears on top)
        if (playBall != null && playBall.isBeingDragged()) {
            playBall.render(graphics);
        }

        // Render any dropped food (always render on top when dragged)
        if (droppedFood != null) {
            droppedFood.render(graphics);
        }

        // Render the GUI
        gui.render(graphics, mouseX, mouseY);
    }

    // Radial menu has been replaced by the GUI

    /**
     * Renders an indicator above the buddy's head showing its current need
     */
    private void renderNeedsIndicator(GuiGraphics graphics) {
        // Don't render thought bubble if any of these conditions are true
        if (isEating || isBeingPet || isPlaying || isSleeping) return;

        int iconSize = 16;
        int iconX = posX + (SPRITE_WIDTH / 2) - (iconSize / 2);
        int iconY = posY - iconSize - 5;

        ResourceLocation icon = null;

        if (needsFood) {
            icon = FoodItem.TEXTURE_FOOD;
        } else if (needsPet) {
            icon = TEXTURE_HEART;
        } else if (needsPlay && !isPlaying && !isChasingBall) {
            // Only show play indicator if buddy isn't already playing or chasing the ball
            icon = TEXTURE_PLAY;
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

        // Fun (purple) - NEW: Added fourth bar for fun level
        renderBar(graphics, barX, barY + spacing*3, barWidth, barHeight, funLevel/100f, DrawableColor.of(200, 80, 255));
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

        // Immediately stop hopping if buddy becomes sad
        if (isHopping && (needsFood || needsPet || needsPlay || poops.size() >= MAX_POOPS_BEFORE_SAD)) {
            LOGGER.info("Stopping hopping because buddy is sad");
            isHopping = false;
            hopAnimationCounter = 0;
        }

        // Update special hop animation counter - ensures smooth hopping regardless of frame rate
        if (isHopping) {
            // Use configurable speed for energetic hopping animation
            hopAnimationCounter += hopAnimationSpeed;

            // Reset counter when it completes a full cycle for continuous animation
            if (hopAnimationCounter >= 2 * Math.PI) {
                hopAnimationCounter -= 2 * Math.PI;
            }
        } else {
            hopAnimationCounter = 0;
        }

        // Update standing idle state
        isStandingIdle = isEating || isBeingPet || (isPlaying && isHoldingBall) || isSleeping || isPooping || isGrumpy;

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

        // Update play ball if it exists
        if (playBall != null) {
            playBall.tick();

            // End playing if ball is gone
            if (playBall.shouldRemove()) {
                playBall = null;
                isPlaying = false;
                isHoldingBall = false;
                isChasingBall = false;
                updateVisualState();
            }
        }

        // Update poops
        for (int i = poops.size() - 1; i >= 0; i--) {
            Poop poop = poops.get(i);
            poop.tick();

            if (poop.shouldRemove()) {
                poops.remove(i);
            }
        }

        // Periodically check for invalid poops (every 30 seconds)
        if (animationTicks % (20 * 30) == 0) {
            cleanupInvalidPoops();
        }

        // Check if there are too many poops and make buddy sad
        if (poops.size() >= MAX_POOPS_BEFORE_SAD) {
            // Decrease happiness due to too many poops
            happiness = Math.max(0, happiness - 0.05f);
        }

        // Handle pooping timer and chance
        if (!isPooping) {
            timeSinceLastPoop++;

            // Consider pooping if enough time has passed
            if (timeSinceLastPoop >= poopingInterval) {
                // Small chance to poop if not already doing something important
                // IMPORTANT: Only allow pooping when on screen
                if (!isSleeping && !isEating && !isPlaying && !isChasingBall &&
                        !isOffScreen && random.nextFloat() < poopChance) {
                    startPooping();
                }
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
                LOGGER.info("Buddy action duration ended: eating={}, beingPet={}, playing={}, pooping={}",
                        isEating, isBeingPet, isPlaying, isPooping);

                // End current action
                isEating = false;
                isBeingPet = false;

                // End pooping and create poop
                if (isPooping) {
                    isPooping = false;
                    dropPoop();
                }

                // Don't end play here, it ends when the ball is removed
                if (isPlaying && playBall == null) {
                    LOGGER.info("Ending play due to no ball");
                    isPlaying = false;
                    isHoldingBall = false;
                }

                // Don't end sleep here, it ends when energy is full
                if (!isSleeping && !isPlaying && !isPooping) {
                    updateVisualState();
                }
            }
        }

        // Update activity duration
        if (activityDuration > 0) {
            activityDuration--;
            if (activityDuration <= 0) {
                LOGGER.info("Buddy activity duration ended: looking={}, stretching={}, hopping={}, excited={}, grumpy={}",
                        isLookingAround, isStretching, isHopping, isExcited, isGrumpy);

                // End the current special activity
                if (isLookingAround || isStretching || isGrumpy) {
                    // These activities set isStanding=true, so we need to end both
                    isStanding = false;
                    LOGGER.info("Ending standing due to end of special activity");
                }

                isHopping = false;
                isLookingAround = false;
                isStretching = false;
                isExcited = false;
                isGrumpy = false;
                updateVisualState();
            }
        }

        // End sleeping if energy is full
        if (isSleeping && energy >= 100) {
            isSleeping = false;
            updateVisualState();
        }

        // Update movement if not standing idle
        if (!isStandingIdle) {
            updateMovement();
        }

        // Update current state visualization
        updateVisualState();

        // Determine if buddy needs to play based on fun level
        needsPlay = funLevel < 30;
    }

    /**
     * Updates the buddy's stats over time
     */
    private void updateStats() {
        // Decrease stats over time
        hunger = Math.max(0, hunger - 0.05f);
        happiness = Math.max(0, happiness - 0.03f);
        funLevel = Math.max(0, funLevel - 0.02f);  // NEW: Fun decreases slower than hunger

        // Energy decreases unless sleeping
        if (isSleeping) {
            energy = Math.min(100, energy + 0.3f);
        } else {
            energy = Math.max(0, energy - 0.02f);

            // Playing increases fun but drains energy faster
            if (isPlaying) {
                energy = Math.max(0, energy - 0.1f);
                funLevel = Math.min(100, funLevel + 0.5f);  // Playing significantly increases fun
            }

            // Chasing drains energy faster
            if (isChasingBall) {
                energy = Math.max(0, energy - 0.08f);
            }

            // Hopping drains energy faster
            if (isHopping) {
                energy = Math.max(0, energy - 0.05f);
            }

            // Excitement drains energy faster
            if (isExcited) {
                energy = Math.max(0, energy - 0.1f);
                // But it also increases happiness!
                happiness = Math.min(100, happiness + 0.1f);
            }
        }

        // Set needs based on stats
        needsFood = hunger < 30;
        needsPet = happiness < 30;
        // needsPlay is set in the tick method

        // Auto-sleep if energy is critically low
        if (energy < 10 && !isSleeping) {
            isSleeping = true;
            isEating = false;
            isBeingPet = false;
            isPlaying = false;
            isHoldingBall = false;
            isChasingBall = false;

            // Cancel all special activities
            isStanding = false;
            isHopping = false;
            isLookingAround = false;
            isStretching = false;
            isExcited = false;

            if (playBall != null) {
                playBall = null; // Remove play ball if buddy falls asleep
            }
            currentState = STATE_SLEEPING;
        }
    }

    /**
     * Updates the buddy's movement with enhanced behaviors
     */
    private void updateMovement() {
        // If chasing ball, move toward the ball
        if (isChasingBall && playBall != null) {
            int ballCenterX = playBall.getX();

            // Calculate direction to ball
            if (ballCenterX < posX + SPRITE_WIDTH/2 - 5) {
                // Ball is to the left
                facingLeft = true;
                posX -= CHASE_SPEED;
            } else if (ballCenterX > posX + SPRITE_WIDTH/2 + 5) {
                // Ball is to the right
                facingLeft = false;
                posX += CHASE_SPEED;
            }

            // Check if we've caught up with the ball
            if (playBall.isNearBuddy(posX + SPRITE_WIDTH/2, posY + SPRITE_HEIGHT/2)) {
                grabBall();
            }

            return; // Skip normal movement logic when chasing
        }

        // Handle special activities
        if (isStanding) {
            // When standing, randomly decide to walk again
            if (random.nextFloat() < walkChance || activityDuration <= 0) {
                isStanding = false;
                isLookingAround = false;  // Make sure we clear these too
                activityDuration = 0;

                // Sometimes hop when starting to walk again
                if (random.nextFloat() < 0.2f) {
                    startHopping();
                }
            }
            return; // Skip movement when standing still
        }

        if (isLookingAround) {
            // When looking around, update look direction occasionally
            if (random.nextFloat() < 0.05f) {
                lookDirection = random.nextInt(3) - 1; // -1, 0, or 1
            }
            return; // Skip movement when looking around
        }

        if (isStretching || isExcited) {
            // These activities have their own animation states, so just wait for them to finish
            return; // Skip movement during these animations
        }

        // Normal walking logic with random behaviors
        pixelsSinceLastDirectionChange++;

        // Chance to randomly stop and stand
        if (pixelsSinceLastDirectionChange > minWalkDistance && random.nextFloat() < standChance) {
            startStanding();
            return;
        }

        // Chance to randomly get excited
        if (happiness > 70 && random.nextFloat() < excitedChance && !(needsFood || needsPet || needsPlay)) {
            startExcitement();
            return;
        }

        // Chance to look around
        if (random.nextFloat() < lookChance) {
            startLookingAround();
            return;
        }

        // Chance to stretch
        if (random.nextFloat() < stretchChance && !(needsFood || needsPet || needsPlay)) {
            startStretching();
            return;
        }

        // Chance to randomly hop while walking
        if (!isHopping && random.nextFloat() < hopChance && !(needsFood || needsPet || needsPlay)) {
            startHopping();
        }

        // Random walk speed based on excitement/mood
        int currentWalkSpeed = walkSpeed;
        if (happiness > 80) {
            // Happier buddies sometimes walk faster
            currentWalkSpeed += random.nextInt(2);
        } else if (energy < 30) {
            // Tired buddies sometimes walk slower
            currentWalkSpeed = Math.max(1, currentWalkSpeed - 1);
        }

        // Apply movement
        if (facingLeft) {
            posX -= currentWalkSpeed;

            // Check for random direction change or screen edge
            boolean shouldTurn = (pixelsSinceLastDirectionChange > minWalkDistance &&
                    random.nextFloat() < 0.01f) ||
                    (pixelsSinceLastDirectionChange > maxWalkDistance);

            // Check if we should turn around or go off screen
            if (posX < -SPRITE_WIDTH) {
                if (!isOffScreen) {
                    LOGGER.info("Buddy going offscreen to the left at x={}", posX);
                    isOffScreen = true;
                }

                // Random chance to come back
                if (random.nextFloat() < 0.01f) {
                    LOGGER.info("Buddy coming back onscreen from the left");
                    facingLeft = false;
                    isOffScreen = false;
                    posX = -SPRITE_WIDTH;
                    pixelsSinceLastDirectionChange = 0;
                }
            } else if ((posX < 0 && random.nextFloat() < 0.01f) || shouldTurn) {
                facingLeft = false;
                pixelsSinceLastDirectionChange = 0;

                // Sometimes do a special action when turning
                performRandomActionOnTurn();
            }
        } else {
            posX += currentWalkSpeed;

            // Check for random direction change or max distance
            boolean shouldTurn = (pixelsSinceLastDirectionChange > minWalkDistance &&
                    random.nextFloat() < 0.01f) ||
                    (pixelsSinceLastDirectionChange > maxWalkDistance);

            // Check if we should turn around or go off screen
            if (posX > screenWidth) {
                if (!isOffScreen) {
                    LOGGER.info("Buddy going offscreen to the right at x={}", posX);
                    isOffScreen = true;
                }

                // Random chance to come back
                if (random.nextFloat() < 0.01f) {
                    LOGGER.info("Buddy coming back onscreen from the right");
                    facingLeft = true;
                    isOffScreen = false;
                    posX = screenWidth;
                    pixelsSinceLastDirectionChange = 0;
                }
            } else if ((posX > screenWidth - SPRITE_WIDTH && random.nextFloat() < 0.01f) || shouldTurn) {
                facingLeft = true;
                pixelsSinceLastDirectionChange = 0;

                // Sometimes do a special action when turning
                performRandomActionOnTurn();
            }
        }
    }

    /**
     * Starts the buddy standing idle animation
     */
    private void startStanding() {
        LOGGER.info("Buddy starting to stand: x={}, y={}, state={}", posX, posY, getStateName(currentState));

        isStanding = true;
        isHopping = false;
        isLookingAround = false;
        isStretching = false;
        isExcited = false;
        activityDuration = minStandTime + random.nextInt(maxStandTime - minStandTime);

        // Make sure we have a valid activity duration as a failsafe
        if (activityDuration <= 0) {
            activityDuration = minStandTime;
        }

        LOGGER.info("Standing activity duration set to: {}", activityDuration);
    }

    /**
     * Starts the buddy hopping animation
     */
    private void startHopping() {
        // Don't hop if the buddy is sad or has critical needs
        if (needsFood || needsPet || needsPlay) {
            LOGGER.info("Buddy is too sad to hop right now");
            return;
        }

        LOGGER.info("Buddy starting to hop: x={}, y={}, state={}", posX, posY, getStateName(currentState));

        isHopping = true;

        // Shorter duration for more energetic hopping sequences
        // Quick burst of happy hops makes it more lively
        activityDuration = 40 + random.nextInt(60); // Hop for 2-5 seconds

        // If buddy is very happy, hop for longer with more enthusiasm
        if (happiness > 80) {
            activityDuration += 40; // Added time for happy hopping
        }

        // Reset animation ticks for smooth start
        animationTicks = 0;
        hopAnimationCounter = 0;

        // Use a faster hop animation speed than before
        hopAnimationSpeed = 0.3f;  // Increased from 0.25f for more energetic hops

        // Small chance to do an excited animation when starting
        if (currentState == STATE_IDLE_WALK && random.nextFloat() < 0.3f && happiness > 60) {
            // Will switch to excited animation when very happy
            isExcited = true;
            activityDuration = Math.min(activityDuration, 40); // Shorter excited animation
            LOGGER.info("Buddy switching to excited animation during hop");
        }

        LOGGER.info("Hop activity duration set to: {}", activityDuration);
    }

    /**
     * Starts the buddy looking around animation
     */
    private void startLookingAround() {
        LOGGER.info("Buddy starting to look around: x={}, y={}, state={}", posX, posY, getStateName(currentState));

        isLookingAround = true;
        isStanding = true;
        // Reset animation frame to 0 for consistent start of looking around animation
        currentFrame = 0;
        animationTicks = 0;
        
        // Set the direction (for code that might use this later)
        lookDirection = random.nextInt(3) - 1; // -1, 0, or 1
        
        // Duration between 6-8 seconds (120-160 ticks) to match the slower animation speed
        activityDuration = 120 + random.nextInt(40);

        LOGGER.info("Looking around activity duration set to: {} ticks ({} seconds), direction: {}", 
                   activityDuration, activityDuration/20f, lookDirection);
    }

    /**
     * Starts the buddy stretching animation
     */
    private void startStretching() {
        // Don't stretch if the buddy is sad or has critical needs
        if (needsFood || needsPet || needsPlay) {
            LOGGER.info("Buddy is too sad to stretch right now");
            return;
        }

        LOGGER.info("Buddy starting to stretch: x={}, y={}, state={}", posX, posY, getStateName(currentState));

        isStretching = true;
        isStanding = false; // Not needed since we have a dedicated animation state
        activityDuration = 60; // Stretch for 3 seconds

        LOGGER.info("Stretching activity duration set to: {}", activityDuration);
    }

    /**
     * Starts the buddy excitement animation
     */
    private void startExcitement() {
        // Don't get excited if the buddy is sad or has critical needs
        if (needsFood || needsPet || needsPlay || happiness < 50) {
            LOGGER.info("Buddy is too sad to get excited right now");
            return;
        }

        LOGGER.info("Buddy starting to get excited: x={}, y={}, state={}", posX, posY, getStateName(currentState));

        isExcited = true;
        activityDuration = 40 + random.nextInt(40); // Get excited for 2-4 seconds

        LOGGER.info("Excitement activity duration set to: {}", activityDuration);
    }

    /**
     * Starts the buddy pooping animation
     */
    private void startPooping() {
        // Don't allow pooping when off-screen
        if (isOffScreen) {
            LOGGER.info("Buddy tried to poop while off-screen, preventing");
            return;
        }

        LOGGER.info("Buddy starting to poop: x={}, y={}", posX, posY);

        isPooping = true;
        actionDuration = 60; // 3 seconds for pooping animation
        currentState = STATE_POOPING;

        // Reset timer
        timeSinceLastPoop = 0;
    }

    /**
     * Creates and drops a poop after the pooping animation ends
     */
    private void dropPoop() {
        int poopX;

        // Drop poop to the side based on which way the buddy is facing
        if (facingLeft) {
            poopX = posX + SPRITE_WIDTH + 5; // Poop appears to the right when facing left
        } else {
            poopX = posX - 5; // Poop appears to the left when facing right
        }

        // Position poop on the ground (at buddy's feet)
        int poopY = posY + SPRITE_HEIGHT - 8;

        // Ensure poop doesn't go too close to screen edges (at least 50px from edges)
        int minX = 50;
        int maxX = screenWidth - 50;

        // If the calculated position would be outside the allowed range,
        // place the poop at a reasonable distance from the buddy in the valid range
        if (poopX < minX) {
            // Too close to left edge, place it to the right of buddy instead
            poopX = posX + SPRITE_WIDTH + 5;
            // If still out of bounds, force it into the valid range
            poopX = Math.max(minX, Math.min(maxX, poopX));
        } else if (poopX > maxX) {
            // Too close to right edge, place it to the left of buddy instead
            poopX = posX - 5;
            // If still out of bounds, force it into the valid range
            poopX = Math.max(minX, Math.min(maxX, poopX));
        }

        // Ensure vertical position is also reasonable
        poopY = Math.max(10, Math.min(screenHeight - 10, poopY));

        // Safety check for invalid coordinates
        if (poopX < 0 || poopX > screenWidth || poopY < 0 || poopY > screenHeight ||
                poopX == Integer.MAX_VALUE || poopY == Integer.MAX_VALUE) {
            LOGGER.warn("Attempted to create poop with invalid coordinates: ({}, {}), skipping", poopX, poopY);
            return;
        }

        // Create and add the poop
        poops.add(new Poop(poopX, poopY, this));

        LOGGER.info("Buddy pooped at position: x={}, y={}, total poops: {}", poopX, poopY, poops.size());

        // If too many poops, make the buddy sad
        if (poops.size() >= MAX_POOPS_BEFORE_SAD) {
            LOGGER.info("Too many poops! Buddy is getting sad");
        }
    }

    /**
     * Performs a random action when turning around
     */
    private void performRandomActionOnTurn() {
        // Don't perform happy actions if the buddy is sad
        boolean isSad = needsFood || needsPet || needsPlay;

        float chance = random.nextFloat();

        if (chance < 0.3f) {
            // 30% chance to just stand for a moment - can do this even when sad
            startStanding();
        } else if (!isSad && chance < 0.4f) {
            // 10% chance to look around (only when not sad)
            startLookingAround();
        } else if (!isSad && chance < 0.45f && happiness > 70) {
            // 5% chance to get excited if happy (only when not sad)
            startExcitement();
        } else if (!isSad && chance < 0.5f) {
            // 5% chance to stretch (only when not sad)
            startStretching();
        }
        // Otherwise just continue walking
    }

    /**
     * Grabs the ball and starts playing with it
     */
    private void grabBall() {
        if (playBall != null) {
            playBall.resetInactivityTimer(); // Count this as interaction
            isChasingBall = false;
            isHoldingBall = true;
            isPlaying = true;
            playBall.setGrabbedByBuddy(true);
            currentState = STATE_PLAYING_STAND;
        }
    }

    /**
     * Decides what behavior the buddy should exhibit next
     */
    private void decideNextBehavior() {
        // Don't change behavior if off screen or in a special state that requires standing still
        if (isOffScreen || isEating || isBeingPet || (isPlaying && isHoldingBall) || isSleeping || isPooping || isGrumpy) return;

        float chance = random.nextFloat();

        // Behaviors based on needs
        if (needsFood && chance < 0.4f) {
            // Look sad when hungry
            currentState = STATE_SAD_WALK;
        } else if (needsPet && chance < 0.4f) {
            // Look sad when needing attention
            currentState = STATE_SAD_WALK;
        } else if (needsPlay && chance < 0.4f) {
            // Look sad when wanting to play
            currentState = STATE_SAD_WALK;
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
                pixelsSinceLastDirectionChange = 0;
            }

            // Chance to start a random activity
            if (chance < 0.2f) {
                performRandomActionOnTurn();
            }
        }
    }

    /**
     * Updates the visual state based on current actions
     */
    private void updateVisualState() {
        int previousState = currentState;

        // Check if buddy is sad
        boolean isSad = needsFood || needsPet ||
                (needsPlay && !isPlaying && !isChasingBall) || // Only sad about play if not already playing
                poops.size() >= MAX_POOPS_BEFORE_SAD;

        // Stop hopping if buddy is sad
        if (isHopping && isSad) {
            isHopping = false;
            hopAnimationCounter = 0;
        }

        if (isPooping) {
            currentState = STATE_POOPING;
        } else if (isSleeping) {
            currentState = STATE_SLEEPING;
        } else if (isGrumpy) {
            currentState = STATE_GRUMPY;     // Prioritize grumpy state
        } else if (isEating) {
            currentState = STATE_EATING_STAND;
        } else if (isBeingPet) {
            currentState = STATE_HAPPY_STAND;
        } else if (isExcited) {
            currentState = STATE_EXCITED;     // Use dedicated excitement state
        } else if (isStretching) {
            currentState = STATE_STRETCHING;  // Use dedicated stretching state
        } else if (isLookingAround) {
            currentState = STATE_LOOKING_AROUND; // Use dedicated looking around state
        } else if (isPlaying && isHoldingBall) {
            // Only show playing animation when actively playing with ball
            currentState = STATE_PLAYING_STAND;
        } else if (isChasingBall) {
            // Use walking animation when chasing (but could be sad if energy low)
            currentState = energy < 30 || isSad ? STATE_SAD_WALK : STATE_IDLE_WALK;
        } else if (isStanding || isLookingAround) {
            // Use standing animations for special activities
            currentState = isSad ? STATE_SAD_STAND : STATE_IDLE_STAND;
        } else if (isSad) {
            // When walking and sad (also when too much poop)
            currentState = STATE_SAD_WALK;
        } else {
            // When walking and happy/normal
            currentState = STATE_IDLE_WALK;
        }

        // Log state changes to help debug invisibility issues
        if (previousState != currentState) {
            LOGGER.info("Buddy state changed: {} -> {}", getStateName(previousState), getStateName(currentState));
        }
    }

    /**
     * Helper method to get state name for logging
     */
    private String getStateName(int state) {
        switch (state) {
            case STATE_IDLE_WALK: return "IDLE_WALK";
            case STATE_HAPPY_STAND: return "HAPPY_STAND";
            case STATE_SAD_WALK: return "SAD_WALK";
            case STATE_EATING_STAND: return "EATING_STAND";
            case STATE_PLAYING_STAND: return "PLAYING_STAND";
            case STATE_SLEEPING: return "SLEEPING";
            case STATE_IDLE_STAND: return "IDLE_STAND";
            case STATE_SAD_STAND: return "SAD_STAND";
            case STATE_STRETCHING: return "STRETCHING";
            case STATE_EXCITED: return "EXCITED";
            case STATE_POOPING: return "POOPING";
            case STATE_LOOKING_AROUND: return "LOOKING_AROUND";
            case STATE_GRUMPY: return "GRUMPY";
            default: return "UNKNOWN_STATE";
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
        currentState = STATE_EATING_STAND;
    }

    /**
     * Called when the buddy is petted
     */
    public void pet() {
        if (isSleeping) {
            // Waking up - make buddy grumpy
            isSleeping = false;
            // Slightly decrease happiness when woken up
            happiness = Math.max(0, happiness - 5);
            // Start grumpy state
            startGrumpyState();
        } else {
            isBeingPet = true;
            actionDuration = 40; // 2 seconds
            happiness = Math.min(100, happiness + 30);
            currentState = STATE_HAPPY_STAND;

            // Sometimes get excited when petted if already happy
            if (happiness > 70 && random.nextFloat() < 0.3f) {
                startExcitement();
            }
        }
    }
    
    /**
     * Starts the buddy grumpy animation
     */
    private void startGrumpyState() {
        LOGGER.info("Buddy is grumpy after being woken up");
        
        // Clear other states
        isHopping = false;
        isLookingAround = false;
        isStretching = false;
        isExcited = false;
        
        // Set grumpy state
        isGrumpy = true;
        
        // Reset animation frame
        currentFrame = 0;
        animationTicks = 0;
        
        // Set duration - 3 seconds
        activityDuration = 60;
        
        // Update visual state immediately
        currentState = STATE_GRUMPY;
        
        LOGGER.info("Started grumpy state with duration: {} ticks", activityDuration);
    }

    /**
     * Makes the buddy start playing
     */
    public void startPlaying() {
        if (!isSleeping && energy > 20 && playBall == null) {
            // Create a new play ball
            int ballX = posX + SPRITE_WIDTH / 2;
            int ballY = posY - 20;
            playBall = new PlayBall(ballX, ballY, this);

            // Start with buddy holding the ball
            isHoldingBall = true;
            isPlaying = true;
            playBall.setGrabbedByBuddy(true);
            needsPlay = false; // Reset the play need after starting to play
            currentState = STATE_PLAYING_STAND;
        }
    }

    /**
     * Creates and drops a food item above the buddy
     */
    public void dropFoodAboveBuddy() {
        if (droppedFood == null) {
            // Position food above the buddy's head
            int foodX = posX + SPRITE_WIDTH / 2;
            int foodY = posY - 15;
            droppedFood = new FoodItem(foodX, foodY, this);
        }
    }

    /**
     * Creates and drops a food item at the specified position
     */
    public void dropFoodAt(int x, int y) {
        if (droppedFood == null) {
            droppedFood = new FoodItem(x, y, this);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if ((droppedFood != null) && !droppedFood.justCreated) {
            droppedFood.stickToCursor = false;
        }

        if ((playBall != null) && !playBall.justCreated) {
            playBall.stickToCursor = false;
            // Check if ball is near buddy before throwing
            if (playBall.isNearBuddy(posX + SPRITE_WIDTH/2, posY + SPRITE_HEIGHT/2)) {
                // If close to buddy, don't throw - start playing
                isPlaying = true;
                isHoldingBall = true;
                isChasingBall = false;
                playBall.setGrabbedByBuddy(true);
                needsPlay = false; // Reset play need immediately
                currentState = STATE_PLAYING_STAND;
            } else {
                // If not close, throw normally
                playBall.throwBall((int)mouseX, (int)mouseY);
                // Always make buddy chase the ball when thrown
                isPlaying = true;
                isChasingBall = true;
                isHoldingBall = false;
            }
        }

        // Always handle poop clicks regardless of buddy visibility
        if (button == 0) { // Left click
            // Check if clicked on a poop
            for (Poop poop : new ArrayList<>(poops)) {
                if (poop.isMouseOver(mouseX, mouseY)) {
                    poop.startCleaning();
                    // Increase happiness slightly for cleaning up poop
                    happiness = Math.min(100, happiness + 5);
                    LOGGER.info("Cleaned up poop at ({},{}), happiness: {}", poop.getX(), poop.getY(), happiness);
                    return true;
                }
            }
        }
        
        // Skip other interaction if buddy is not visible or offscreen
        if (!isVisible || isOffScreen) return false;

        // First handle the GUI if it's visible
        if (gui.isVisible()) {
            return gui.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 1) { // Right click
            // If clicked on buddy, open the GUI
            if (isMouseOverBuddy(mouseX, mouseY)) {
                gui.show(screenWidth, screenHeight);
                LOGGER.info("Opening buddy GUI");
                updateVisualState(); // Update visual state immediately when opening GUI
                return true;
            }
        } else if (button == 0) { // Left click
            // Normal petting if menu isn't open
            if (isMouseOverBuddy(mouseX, mouseY)) {
                pet();
                return true;
            }

            // Check if clicked on food
            if (droppedFood != null && droppedFood.isMouseOver(mouseX, mouseY)) {
                droppedFood.pickup((int)mouseX, (int)mouseY);
                return true;
            }

            // Check if clicked on play ball
            if (playBall != null && playBall.isMouseOver(mouseX, mouseY)) {
                // Start dragging ball instead of just kicking it
                playBall.pickup((int)mouseX, (int)mouseY);

                // If buddy was holding the ball, it's not anymore
                if (isHoldingBall) {
                    isHoldingBall = false;
                    // Don't start chasing immediately when player grabs from buddy's hands
                } else if (!isChasingBall) {
                    // If ball was on the ground or rolling and buddy wasn't already chasing,
                    // make buddy start chasing the ball
                    isChasingBall = true;
                    isPlaying = true;
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (droppedFood != null && droppedFood.isBeingDragged()) {
                // Check if food is near buddy before dropping it
                if (droppedFood.isNearBuddy(posX + SPRITE_WIDTH/2, posY + SPRITE_HEIGHT/2)) {
                    // If close to buddy, don't drop - just start eating
                    eatFood();
                    droppedFood = null;
                } else {
                    // If not close, just drop normally
                    droppedFood.drop((int)mouseX, (int)mouseY);
                }
                return true;
            }

            // Handle ball throw on release
            if (playBall != null && playBall.isBeingDragged()) {
                // Check if ball is near buddy before throwing
                if (playBall.isNearBuddy(posX + SPRITE_WIDTH/2, posY + SPRITE_HEIGHT/2)) {
                    // If close to buddy, don't throw - start playing
                    isPlaying = true;
                    isHoldingBall = true;
                    isChasingBall = false;
                    playBall.setGrabbedByBuddy(true);
                    needsPlay = false; // Reset play need immediately
                    currentState = STATE_PLAYING_STAND;
                } else {
                    // If not close, throw normally
                    playBall.throwBall((int)mouseX, (int)mouseY);

                    // Always make buddy chase the ball when thrown
                    isPlaying = true;
                    isChasingBall = true;
                    isHoldingBall = false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (droppedFood != null && droppedFood.isBeingDragged()) {
                droppedFood.setPosition((int)mouseX, (int)mouseY);
                return true;
            }

            // Handle ball dragging with velocity tracking
            if (playBall != null && playBall.isBeingDragged()) {
                playBall.updateDragPosition((int)mouseX, (int)mouseY);
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Update position of dragged items when mouse moves without buttons pressed
        if (droppedFood != null && droppedFood.isBeingDragged()) {
            LOGGER.debug("Updating dragged food position to: ({}, {})", (int)mouseX, (int)mouseY);
            droppedFood.setPosition((int)mouseX, (int)mouseY);
        }

        if (playBall != null && playBall.isBeingDragged()) {
            LOGGER.debug("Updating dragged ball position to: ({}, {})", (int)mouseX, (int)mouseY);
            playBall.updateDragPosition((int)mouseX, (int)mouseY);
        }
    }

    /**
     * Renders all poops in the world
     */
    private void renderPoops(GuiGraphics graphics) {
        // Render any poops (so they appear behind the buddy)
        for (Poop poop : new ArrayList<>(poops)) {
            poop.render(graphics);
        }
    }

    /**
     * Checks if the mouse is over the buddy
     */
    private boolean isMouseOverBuddy(double mouseX, double mouseY) {
        return mouseX >= posX && mouseX < posX + SPRITE_WIDTH &&
                mouseY >= posY && mouseY < posY + SPRITE_HEIGHT;
    }

    /**
     * Returns whether the buddy needs play
     */
    public boolean needsPlay() {
        return needsPlay;
    }

    /**
     * Sets the screen dimensions for the buddy
     */
    public void setScreenSize(int width, int height) {
        LOGGER.info("Screen size changed: {}x{} -> {}x{}", this.screenWidth, this.screenHeight, width, height);

        // Ensure valid dimensions
        width = Math.max(1, width);
        height = Math.max(1, height);

        this.screenWidth = width;
        this.screenHeight = height;
        this.posY = height - SPRITE_HEIGHT - 10; // Keep at bottom

        // Update all poop positions when screen size changes
        for (Poop poop : new ArrayList<>(poops)) {
            poop.updatePosition(width, height);
        }

        // Clean up any poops that might now be invalid
        cleanupInvalidPoops();

        // If GUI is visible, update its position based on new screen size
        if (gui.isVisible()) {
            gui.show(width, height);
        }

        LOGGER.info("Updated buddy position to y={}", this.posY);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(posX, posY, SPRITE_WIDTH, SPRITE_HEIGHT);
    }

    // Getters and setters for external classes

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getSpriteWidth() {
        return SPRITE_WIDTH;
    }

    public int getSpriteHeight() {
        return SPRITE_HEIGHT;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public boolean isFacingLeft() {
        return facingLeft;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public int getAnimationTicks() {
        return animationTicks;
    }

    public void increaseFunLevel(float amount) {
        funLevel = Math.min(100, funLevel + amount);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public boolean isHoldingBall() {
        return isHoldingBall;
    }

    public void setHoldingBall(boolean holdingBall) {
        isHoldingBall = holdingBall;
    }

    public boolean isChasingBall() {
        return isChasingBall;
    }

    public void setChasingBall(boolean chasingBall) {
        isChasingBall = chasingBall;
    }

    // Additional getters and setters for persistence

    public float getHunger() {
        return hunger;
    }

    public void setHunger(float hunger) {
        this.hunger = hunger;
        // Update needs based on new hunger value
        this.needsFood = hunger < 30;
    }

    public float getHappiness() {
        return happiness;
    }

    public void setHappiness(float happiness) {
        this.happiness = happiness;
        // Update needs based on new happiness value
        this.needsPet = happiness < 30;
    }

    public float getEnergy() {
        return energy;
    }

    public void setEnergy(float energy) {
        this.energy = energy;
        // Handle auto-sleep if energy is critically low
        if (energy < 10 && !isSleeping) {
            isSleeping = true;
            currentState = STATE_SLEEPING;
        }
    }

    public float getFunLevel() {
        return funLevel;
    }

    public void setFunLevel(float funLevel) {
        this.funLevel = funLevel;
        // Update needs based on new fun level
        this.needsPlay = funLevel < 30;
    }

    public List<Poop> getPoops() {
        return new ArrayList<>(poops);
    }

    public void setPoops(List<Poop> poops) {
        this.poops = new ArrayList<>(poops);
    }

    /**
     * Gets the current food item, if any
     */
    public FoodItem getDroppedFood() {
        return droppedFood;
    }

    /**
     * Sets the current food item
     */
    public void setDroppedFood(FoodItem food) {
        this.droppedFood = food;
    }

    /**
     * Gets the current play ball, if any
     */
    public PlayBall getPlayBall() {
        return playBall;
    }

    /**
     * Sets the current play ball
     */
    public void setPlayBall(PlayBall ball) {
        this.playBall = ball;
    }

    public boolean isSleeping() {
        return isSleeping;
    }

    /**
     * Saves the buddy's state to persistent storage
     */
    public void saveState() {
        TamagotchiBuddyPersistence.saveBuddy(this);
    }

    /**
     * Loads the buddy's state from persistent storage
     * @return true if state was successfully loaded, false otherwise
     */
    public boolean loadState() {
        boolean result = TamagotchiBuddyPersistence.loadBuddy(this);

        // After loading, clean up any poops that might be off-screen
        cleanupInvalidPoops();

        return result;
    }

    /**
     * Repositions poops that are off-screen and removes any with invalid coordinates
     */
    private void cleanupInvalidPoops() {
        List<Poop> validPoops = new ArrayList<>();
        boolean madeChanges = false;

        for (Poop poop : poops) {
            int x = poop.getX();
            int y = poop.getY();

            // Check for extreme invalid values that should be removed
            boolean isExtremlyInvalid = x < 0 || x == Integer.MAX_VALUE ||
                    y < 0 || y == Integer.MAX_VALUE ||
                    x > 10000 || y > 10000;

            if (isExtremlyInvalid) {
                LOGGER.info("Removed invalid poop at position ({}, {})", x, y);
                madeChanges = true;
                continue;  // Skip this poop
            }

            // Check if poop is off-screen but has reasonable coordinates
            if (x > screenWidth || y > screenHeight) {
                // Update position to bring it within screen bounds
                LOGGER.info("Repositioning off-screen poop from ({}, {}) to within screen bounds", x, y);
                poop.updatePosition(screenWidth, screenHeight);
                madeChanges = true;
            }

            validPoops.add(poop);
        }

        // If we removed or repositioned any poops, update the list
        if (madeChanges) {
            LOGGER.info("Cleaned up {} invalid poops", poops.size() - validPoops.size());
            poops = validPoops;
        }
    }
}