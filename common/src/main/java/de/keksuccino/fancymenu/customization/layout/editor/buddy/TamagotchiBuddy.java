package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationState;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationStates;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.gui.BuddyGui;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.FoodItem;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.PlayBall;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.Poop;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationState.*;
import static de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationStates.*;

/**
 * TamagotchiBuddy is a cute Easter egg that adds a pixel art pet
 * to the layout editor. It walks along the bottom of the screen,
 * has needs, and reacts to your interactions.
 */
public class TamagotchiBuddy extends AbstractContainerEventHandler implements Renderable, FancyMenuUiComponent {

    public static final Logger LOGGER = LogManager.getLogger();

    // Resource locations for textures
    public static final ResourceLocation TEXTURE_ICON_WANTS_BEING_PET = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/heart.png");
    public static final ResourceLocation TEXTURE_ICON_WANTS_TO_PLAY = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/play.png");
    public static final ResourceLocation TEXTURE_THOUGHT_BUBBLE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/thought.png");

    // Game state
    public int buddyPosX;
    public int buddyPosY;
    public int screenWidth;
    public int screenHeight;
    public boolean facingLeft = false;
    public boolean isDisabled = true;
    public boolean isOffScreen = false;

    // Animation
    @NotNull
    public AnimationState currentState = WALKING;
    public int currentStateDuration = 0; // Gets automatically set by temporary states when setting them as currentState
    public int currentFrame = 0;
    public int animationRenderTicks = 0;
    public float hopAnimationCounter = 0; // Dedicated counter for hop animation to ensure smooth motion
    public float hopAnimationSpeed = 0.3f; // Speed of hop animation cycle
    public int hopAnimationDuration = 0;

    // Needs and stats
    public float hunger = 100.0f;
    public float happiness = 100.0f;
    public float energy = 100.0f;
    public float funLevel = 100.0f;
    public boolean needsFood = false;
    public boolean needsPet = false;
    public boolean needsPlay = false;

    // Activity states
    public boolean isBeingPet = false;
    public boolean isEating = false;
    public boolean isPlaying = false;
    public boolean isSleeping = false;
    public boolean isChasingBall = false;
    public boolean isHoldingBall = false;
    public boolean isSleepy = false;
    public boolean isStanding = false;
    public boolean isHopping = false;
    public boolean isLookingAround = false;
    public boolean isStretching = false;
    public boolean isExcited = false;
    public boolean isGrumpy = false;
    public boolean isSitting = false;
    public boolean isWaving = false;
    public boolean isYawning = false;

    // Timers and behaviors
    public int stateChangeTimer;
    public Random random = new Random();

    // Movement variation parameters
    public int pixelsSinceLastDirectionChange = 0;
    public int minWalkDistance = 30;  // Minimum pixels to walk before considering a turn
    public int maxWalkDistance = 200; // Maximum pixels to walk before forcing a behavior change
    public float standChancePercentage = 1f;
    public float hopChancePercentage = 0.3f;
    public float lookChancePercentage = 0.2f;
    public float stretchChancePercentage = 0.1f;
    public float excitedChancePercentage = 0.1f;

    // Interactive objects
    public FoodItem droppedFood = null;
    public PlayBall playBall = null;

    // Poop system
    public List<Poop> poops = new ArrayList<>();
    public boolean isPooping = false;
    public int timeSinceLastPoop = 0;
    public int poopingInterval = 1500; // Time between potential poops (in ticks)
    public float poopChancePercentage = 5f; // Chance to poop when the interval is reached
    public static final int MAX_POOPS_BEFORE_SAD = 3; // Buddy gets sad if there are too many poops

    // Track visibility changes
    public boolean wasDisabled = true;
    public boolean wasOffScreen = false;

    public BuddyGui gui;

    // Event listeners
    public final List<GuiEventListener> children = new ArrayList<>();

    /**
     * Creates a new TamagotchiBuddy
     */
    public TamagotchiBuddy(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Position at bottom of screen
        this.buddyPosX = screenWidth / 2;
        this.buddyPosY = screenHeight - SPRITE_HEIGHT - 10;

        // Initialize with random timer
        this.stateChangeTimer = random.nextInt(200) + 100;

        // Initialize GUI
        this.gui = new BuddyGui(this);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        // Log disabled changes
        if (wasDisabled != isDisabled) {
            LOGGER.info("Buddy disabled state changed: {} -> {}", wasDisabled, isDisabled);
            wasDisabled = isDisabled;
        }

        // Check if buddy is disabled
        if (!isDisabled) return;

        // Log off-screen changes
        if (wasOffScreen != isOffScreen) {
            LOGGER.info("Buddy off-screen state changed: {} -> {}", wasOffScreen, isOffScreen);
            wasOffScreen = isOffScreen;
            
            // Force state reevaluation when coming back onscreen
            if (!isOffScreen) {
                // Clear standing state when coming back onscreen
                isStanding = false;
            }
        }

        renderPoops(graphics);

        animationRenderTicks++;
        
        // Use the animation speed from the current state
        int currentAnimationSpeed = currentState.getAnimationSpeed(this);
        
        if (animationRenderTicks >= currentAnimationSpeed) {
            animationRenderTicks = 0;
            currentFrame = (currentFrame + 1) % ATLAS_COLUMNS;
        }

        // Calculate texture coordinates
        int texX = currentFrame * SPRITE_WIDTH;
        int texY = currentState.getAtlasIndex() * SPRITE_HEIGHT;

        // Render the play ball if it exists (non-dragged balls render before buddy)
        if (playBall != null && !playBall.isBeingDragged()) {
            playBall.render(graphics);
        }

        if (isHopping && !isExcited) {
            // For hopping animation, we'll just use a simple Y offset

            // Calculate a simple vertical hop offset using sine wave
            float hopOffset = (float)Math.sin(hopAnimationCounter) * 10.0f;

            // Apply the vertical offset to the rendering position
            int renderY = buddyPosY - (int)hopOffset;

            // Regular rendering but with adjusted Y position
            if (facingLeft) {
                // Use our custom method for mirrored rendering
                RenderingUtils.blitMirrored(
                        graphics,
                        TEXTURE_ATLAS,
                        buddyPosX, renderY,
                        texX, texY,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                );
            } else {
                // Standard rendering
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE_ATLAS,
                        buddyPosX, renderY,
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
                        buddyPosX, buddyPosY,
                        texX, texY,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                );
            } else {
                // Standard rendering
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE_ATLAS,
                        buddyPosX, buddyPosY,
                        texX, texY,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                );
            }
        }

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

    /**
     * Renders all poops in the world
     */
    public void renderPoops(GuiGraphics graphics) {
        for (Poop poop : new ArrayList<>(poops)) {
            poop.render(graphics);
        }
    }

    /**
     * Renders an indicator above the buddy's head showing its current need
     */
    public void renderNeedsIndicator(GuiGraphics graphics) {

        // Don't render thought bubble if any of these conditions are true
        if (isEating || isBeingPet || isPlaying || isSleeping) return;

        int iconSize = 16;
        int iconX = buddyPosX + (SPRITE_WIDTH / 2) - (iconSize / 2);
        int iconY = buddyPosY - iconSize - 5;

        ResourceLocation icon = null;

        if (needsFood) {
            icon = FoodItem.TEXTURE_FOOD;
        } else if (needsPet) {
            icon = TEXTURE_ICON_WANTS_BEING_PET;
        } else if (needsPlay && !isChasingBall) {
            // Only show play indicator if buddy isn't already playing or chasing the ball
            icon = TEXTURE_ICON_WANTS_TO_PLAY;
        }

        if (icon != null) {
            // First render the thought bubble
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE_THOUGHT_BUBBLE,
                    iconX - 4, iconY - 4,
                    0, 0,
                    iconSize + 8, iconSize + 8,
                    iconSize + 8, iconSize + 8
            );

            // Then render the icon inside it
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    icon,
                    iconX, iconY,
                    0, 0,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        }

    }

    /**
     * Updates the buddy's state, position, and behaviors
     */
    public void tick() {

        if (!isDisabled) return;

        // Immediately stop hopping if buddy becomes sad
        if (isHopping && isSad()) {
            LOGGER.info("Stopping hopping because buddy is sad");
            isHopping = false;
            hopAnimationCounter = 0;
        }
        if (isHopping) {
            // Update hop duration and end hopping if 0
            if (hopAnimationDuration > 0) {
                hopAnimationDuration--;
                if (hopAnimationDuration <= 0) {
                    isHopping = false;
                    hopAnimationCounter = 0;
                }
            }
        }
        // Update special hop animation counter - ensures smooth hopping regardless of frame rate
        if (isHopping) {
            // Use configurable speed for energetic hopping animation
            hopAnimationCounter += hopAnimationSpeed;
            // Reset counter when it completes a full cycle for continuous animation
            if (hopAnimationCounter >= 2 * Math.PI) {
                hopAnimationCounter -= (float) (2 * Math.PI);
            }
        } else {
            hopAnimationCounter = 0;
        }

        // Update stats and needs over time
        updateStatsAndNeeds();

        // Update food item if it exists
        if (droppedFood != null) {
            droppedFood.tick();
            // Check if food is close enough to eat
            if (droppedFood.isNearBuddy(buddyPosX + SPRITE_WIDTH/2, buddyPosY + SPRITE_HEIGHT/2)) {
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
            }
        }

        if (isPlaying && playBall == null) {
            LOGGER.info("Ending play due to no ball");
            isPlaying = false;
            isHoldingBall = false;
            isChasingBall = false;
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
        if (animationRenderTicks % (20 * 30) == 0) {
            cleanupInvalidPoops();
        }
        
        // Periodically force state reevaluation to prevent getting stuck in states
        if (animationRenderTicks % (20 * 5) == 0) {  // Every 5 seconds
            // If buddy is standing for too long, consider clearing the flag
            if (isStanding && !isLookingAround && !isStretching && !isGrumpy && (currentStateDuration <= 0) && !isOffScreen) {
                LOGGER.info("Buddy has been standing still too long, forcing movement");
                isStanding = false;
            }
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
                if (!isSleeping && !isEating && !isPlaying && !isChasingBall && this.chanceCheck(poopChancePercentage)) {
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

        // Update activity duration for temporary states
        if (currentStateDuration > 0) {

            currentStateDuration--;

            if (currentStateDuration <= 0) {

                LOGGER.info("Buddy activity duration ended for temporary state: {}", currentState.getName());

                // End temporary states --->

                // If pooping, drop poop
                if (isPooping) {
                    dropPoop();
                }
                isPooping = false;
                isLookingAround = false;
                isStretching = false;
                isExcited = false;
                isGrumpy = false;
                isEating = false;
                isBeingPet = false;
                isYawning = false;
                isSitting = false;
                isWaving = false;

            }

        }

        // End sleeping if energy is full
        if (isSleeping && energy >= 100) {
            isSleeping = false;
        }

        updateMovement();

        updateVisualState();

    }

    /**
     * Updates the buddy's stats and needs
     */
    public void updateStatsAndNeeds() {

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

            // Running drains energy faster
            if (this.currentState == RUNNING) {
                energy = Math.max(0, energy - 0.1f);
            }

        }

        // Set needs based on stats
        needsFood = hunger < 30;
        needsPet = happiness < 30;
        needsPlay = funLevel < 30;
        isSleepy = energy < 30 && energy >= 10;

        // Auto-sleep if energy is critically low
        if (energy < 10 && !isSleeping) {
            this.startSleeping();
        }

    }

    /**
     * Updates the buddy's movement with enhanced behaviors
     */
    public void updateMovement() {

        // Skip movement if current state doesn't allow it
        if (!currentState.allowsMovement()) {
            return;
        }

        // Random walk speed based on excitement/mood
        int currentWalkSpeed = currentState.getCurrentWalkingSpeed(this);
        
        // If chasing ball, move toward the ball
        if (isChasingBall && playBall != null) {
            int ballCenterX = playBall.getX();

            // Calculate direction to ball
            if (ballCenterX < buddyPosX + SPRITE_WIDTH/2 - 5) {
                // Ball is to the left
                facingLeft = true;
                buddyPosX -= currentWalkSpeed;
                if (buddyPosX < screenWidth) isOffScreen = false; // reset off screen when back on screen
            } else if (ballCenterX > buddyPosX + SPRITE_WIDTH/2 + 5) {
                // Ball is to the right
                facingLeft = false;
                buddyPosX += currentWalkSpeed;
                if (buddyPosX > 0) isOffScreen = false; // reset off screen when back on screen
            }

            // Check if we've caught up with the ball
            if (playBall.isNearBuddy(buddyPosX + SPRITE_WIDTH/2, buddyPosY + SPRITE_HEIGHT/2)) {
                grabBall();
            }

            return; // Skip normal movement logic when chasing
        }

        // Normal walking logic with random behaviors
        pixelsSinceLastDirectionChange++;

        // Check for random direction change or screen edge
        boolean shouldTurn = ((pixelsSinceLastDirectionChange > minWalkDistance) && this.chanceCheck(1f)) || (pixelsSinceLastDirectionChange > maxWalkDistance);

        if (facingLeft) {

            buddyPosX -= currentWalkSpeed;

            // If buddy is off screen to the LEFT
            if (buddyPosX < -SPRITE_WIDTH) {
                if (!isOffScreen) {
                    LOGGER.info("Buddy going offscreen to the left at x={}", buddyPosX);
                    isOffScreen = true;
                    return;
                }
                // Random chance to come back
                if (this.chanceCheck(1f)) {
                    LOGGER.info("Buddy coming back onscreen from the left");
                    facingLeft = false;
                    isOffScreen = false;
                    buddyPosX = -SPRITE_WIDTH; // teleport buddy to screen edge
                    pixelsSinceLastDirectionChange = 0;
                    this.stopAllStandingActions();
                }
            } else if ((buddyPosX < 0 && this.chanceCheck(1f)) || shouldTurn) {
                facingLeft = false;
                pixelsSinceLastDirectionChange = 0;
                // Sometimes do a special action when turning
                performRandomAction();
            }

        } else {

            buddyPosX += currentWalkSpeed;

            // If buddy is off screen to the RIGHT
            if (buddyPosX > screenWidth) {
                if (!isOffScreen) {
                    LOGGER.info("Buddy going offscreen to the right at x={}", buddyPosX);
                    isOffScreen = true;
                    return;
                }
                // Random chance to come back
                if (this.chanceCheck(1f)) {
                    LOGGER.info("Buddy coming back onscreen from the right");
                    facingLeft = true;
                    isOffScreen = false;
                    buddyPosX = screenWidth; // teleport buddy to screen edge
                    pixelsSinceLastDirectionChange = 0;
                    this.stopAllStandingActions();
                }
            } else if ((buddyPosX > screenWidth - SPRITE_WIDTH && this.chanceCheck(1f)) || shouldTurn) {
                facingLeft = true;
                pixelsSinceLastDirectionChange = 0;
                // Sometimes do a special action when turning
                performRandomAction();
            }

        }
    }

    /**
     * Decides what behavior the buddy should exhibit next
     */
    public void decideNextBehavior() {

        if (this.chanceCheck(10f)) {
            // Small chance to decide to leave screen
            facingLeft = buddyPosX < screenWidth / 2;
        } else {
            // Just walk around randomly
            if (this.chanceCheck(50f)) {
                facingLeft = !facingLeft;
                pixelsSinceLastDirectionChange = 0;
            }
            // Chance to start a random activity
            if (this.chanceCheck(20f)) {
                performRandomAction();
            }
        }

    }

    /**
     * Performs a random action
     */
    public void performRandomAction() {

        // Chance to randomly stop and stand
        if ((pixelsSinceLastDirectionChange > minWalkDistance) && this.chanceCheck(standChancePercentage)) {
            startStanding();
            return;
        }

        // Chance to randomly get excited
        if ((happiness > 70) && (funLevel > 80) && this.chanceCheck(excitedChancePercentage) && !isSad()) {
            startExcitement();
            return;
        }
        
        // Chance to wave when happy
        if ((happiness > 60) && this.chanceCheck(1.0f) && !isSad() && !isSleepy) {
            startWaving();
            return;
        }

        // Chance to yawn when tired
        if ((energy < 60) && this.chanceCheck(2.0f)) {
            startYawning();
            return;
        }

        // Chance to sit down and rest for a bit
        if (this.chanceCheck(1.5f)) {
            startSitting();
            return;
        }

        // Chance to look around
        if (this.chanceCheck(lookChancePercentage)) {
            startLookingAround();
            return;
        }

        // Chance to stretch
        if (this.chanceCheck(stretchChancePercentage) && !isSad()) {
            startStretching();
            return;
        }

        // Chance to randomly hop while walking
        if (!isHopping && this.chanceCheck(hopChancePercentage) && !isSad()) {
            startHopping();
        }

    }

    /**
     * Updates the visual state based on current stats and actions
     */
    public void updateVisualState() {

        // Check all animation states in priority order and pick the first valid one to set as new state
        AnimationState selectedState = AnimationStates.findFirstValidStateFor(this);
        setState(selectedState);

        // Stop hopping if current state doesn't allow it
        if (isHopping && !currentState.allowsHopping()) {
            LOGGER.info("Stopping hopping because current state {} doesn't allow it", currentState.getName());
            isHopping = false;
            hopAnimationCounter = 0;
        }

    }

    /**
     * The state should ALWAYS get set via this method.
     */
    public void setState(@NotNull AnimationState state) {
        if (this.lockedInState() && !state.shouldIgnoreLockedState()) return;
        Objects.requireNonNull(state);
        // Apply the selected state if it's different from the current one
        if (currentState != state) {
            LOGGER.info("Changing buddy state: {} -> {}", currentState.getName(), state.getName());
            // Deactivate the previous state
            currentState.onDeactivate(this);
            // Activate the new state
            state.onActivate(this);
            currentState = state;
        }
    }

    public boolean lockedInState() {
        return (currentState.shouldLockStateUntilFinished() && (this.currentStateDuration > 0));
    }

    public void stopAllStandingActions() {
        isStanding = false;
        isStretching = false;
        isLookingAround = false;
        isPooping = false;
        isEating = false;
        isSleeping = false;
        isBeingPet = false;
        isSitting = false;
        isWaving = false;
        isYawning = false;
    }

    /**
     * Starts the buddy standing idle animation
     */
    public void startStanding() {

        if (this.lockedInState()) return;

        LOGGER.info("Buddy starting to stand: x={}, y={}, state={}", buddyPosX, buddyPosY, currentState.getName());

        isStanding = true;
        isHopping = false;
        isLookingAround = false;
        isStretching = false;
        isExcited = false;

    }

    /**
     * Starts the buddy hopping animation (special animation without animation state)
     */
    public void startHopping() {

        // Don't hop if the buddy is sad, has critical needs, or is performing another stationary activity
        if (isSad()) {
            LOGGER.info("Buddy is too sad to hop right now");
            return;
        }
        
        // Check if current state allows hopping
        if (!currentState.allowsHopping()) {
            LOGGER.info("Current state {} doesn't allow hopping", currentState.getName());
            return;
        }

        LOGGER.info("Buddy starting to hop: x={}, y={}, state={}", buddyPosX, buddyPosY, currentState.getName());

        isHopping = true;
        hopAnimationCounter = 0;
        hopAnimationDuration = MathUtils.getRandomNumberInRange(30, 100);
        
        // If buddy is very happy, chance to get excited
        if (this.chanceCheck(30f) && (happiness > 60)) {
            isExcited = true;
            LOGGER.info("Buddy switching to excited animation during hop");
        }

    }

    /**
     * Starts the buddy looking around animation
     */
    public void startLookingAround() {
        if (this.lockedInState()) return;
        LOGGER.info("Buddy starting to look around: x={}, y={}, state={}", buddyPosX, buddyPosY, currentState.getName());
        isLookingAround = true;
        isStanding = true;
    }

    /**
     * Starts the buddy stretching animation
     */
    public void startStretching() {

        if (this.lockedInState()) return;

        // Don't stretch if the buddy is sad or has critical needs
        if (isSad()) {
            LOGGER.info("Buddy is too sad to stretch right now");
            return;
        }

        LOGGER.info("Buddy starting to stretch: x={}, y={}, state={}", buddyPosX, buddyPosY, currentState.getName());

        isStretching = true;
    }

    /**
     * Starts the buddy sitting animation
     */
    public void startSitting() {
        if (this.lockedInState()) return;

        LOGGER.info("Buddy starting to sit: x={}, y={}, state={}", buddyPosX, buddyPosY, currentState.getName());

        isSitting = true;
        isHopping = false;
        isLookingAround = false;
        isStretching = false;
        isExcited = false;
        isStanding = false;  // Clear standing state since sitting is a different pose
    }
    
    /**
     * Starts the buddy waving animation
     */
    public void startWaving() {
        if (this.lockedInState()) return;
        
        // Only wave when buddy is happy enough
        if (happiness < 60) {
            LOGGER.info("Buddy is not happy enough to wave right now");
            return;
        }
        
        // Don't wave if buddy is sad or sleepy
        if (isSad() || isSleepy) {
            LOGGER.info("Buddy doesn't feel like waving right now");
            return;
        }
        
        LOGGER.info("Buddy starting to wave: x={}, y={}, state={}", buddyPosX, buddyPosY, currentState.getName());
        
        isWaving = true;
        isHopping = false;
        isLookingAround = false;
        isStretching = false;
        isExcited = false;
        isSitting = false;
    }
    
    /**
     * Starts the buddy yawning animation
     */
    public void startYawning() {
        if (this.lockedInState()) return;
        
        LOGGER.info("Buddy starting to yawn: x={}, y={}, state={}", buddyPosX, buddyPosY, currentState.getName());
        
        isYawning = true;
        isHopping = false;
        isLookingAround = false;
        isStretching = false;
        isExcited = false;
        isSitting = false;
        isWaving = false;
    }

    /**
     * Starts the buddy excitement animation
     */
    public void startExcitement() {

        if (this.lockedInState()) return;

        // Don't get excited if the buddy is sad or has critical needs
        if (isSad() || happiness < 50) {
            LOGGER.info("Buddy is too sad to get excited right now");
            return;
        }

        LOGGER.info("Buddy starting to get excited: x={}, y={}, state={}", buddyPosX, buddyPosY, currentState.getName());

        isExcited = true;
    }

    /**
     * Starts the buddy pooping animation
     */
    public void startPooping() {

        // Don't allow pooping when off-screen
        if (isOffScreen) {
            LOGGER.info("Buddy tried to poop while off-screen, preventing");
            return;
        }

        LOGGER.info("Buddy starting to poop: x={}, y={}", buddyPosX, buddyPosY);

        isPooping = true;

        // Reset timer
        timeSinceLastPoop = 0;

    }

    /**
     * Creates and drops a poop after the pooping animation ends
     */
    public void dropPoop() {
        int poopX;

        // Drop poop to the side based on which way the buddy is facing
        if (facingLeft) {
            poopX = buddyPosX + SPRITE_WIDTH + 5; // Poop appears to the right when facing left
        } else {
            poopX = buddyPosX - 5; // Poop appears to the left when facing right
        }

        // Position poop on the ground (at buddy's feet)
        int poopY = buddyPosY + SPRITE_HEIGHT - 8;

        // Ensure poop doesn't go too close to screen edges (at least 50px from edges)
        int minX = 50;
        int maxX = screenWidth - 50;

        // If the calculated position would be outside the allowed range,
        // place the poop at a reasonable distance from the buddy in the valid range
        if (poopX < minX) {
            // Too close to left edge, place it to the right of buddy instead
            poopX = buddyPosX + SPRITE_WIDTH + 5;
            // If still out of bounds, force it into the valid range
            poopX = Math.max(minX, Math.min(maxX, poopX));
        } else if (poopX > maxX) {
            // Too close to right edge, place it to the left of buddy instead
            poopX = buddyPosX - 5;
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
     * Grabs the ball and starts playing with it
     */
    public void grabBall() {
        if (playBall != null) {
            playBall.resetInactivityTimer(); // Count this as interaction
            isChasingBall = false;
            isHoldingBall = true;
            isPlaying = true;
            playBall.setGrabbedByBuddy(true);
        }
    }

    /**
     * Called when the buddy eats a food item
     */
    public void eatFood() {
        isEating = true;
        hunger = Math.min(100, hunger + 40);
        happiness = Math.min(100, happiness + 10);
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
            happiness = Math.min(100, happiness + 30);
            // Sometimes get excited when petted if already happy
            if ((happiness > 70) && this.chanceCheck(30.0f)) {
                startExcitement();
            }
        }
    }
    
    /**
     * Starts the buddy grumpy animation
     */
    public void startGrumpyState() {
        LOGGER.info("Buddy is grumpy after being woken up");
        
        // Clear other states
        isHopping = false;
        isLookingAround = false;
        isStretching = false;
        isExcited = false;
        
        // Set grumpy state
        isGrumpy = true;
    }

    public boolean isSad() {
        if (isPlaying || isChasingBall) return false;
        if (happiness < 20) return true;
        return (needsFood || needsPet || needsPlay);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (!isDisabled) return false;

        if ((droppedFood != null) && !droppedFood.justCreated) {
            droppedFood.stickToCursor = false;
        }

        if ((playBall != null) && !playBall.justCreated) {
            playBall.stickToCursor = false;
            // Check if ball is near buddy before throwing
            if (playBall.isNearBuddy(buddyPosX + SPRITE_WIDTH/2, buddyPosY + SPRITE_HEIGHT/2)) {
                // If close to buddy, don't throw - start playing
                isPlaying = true;
                isHoldingBall = true;
                isChasingBall = false;
                playBall.setGrabbedByBuddy(true);
                needsPlay = false; // Reset play need immediately
            } else {
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

        // First handle the GUI if it's visible
        if (gui.isVisible()) {
            return gui.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 1) { // Right click
            // If clicked on buddy, open the GUI
            if (isMouseOverBuddy(mouseX, mouseY)) {
                gui.show(screenWidth, screenHeight);
                LOGGER.info("Opening buddy GUI");
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

        if (!isDisabled) return false;

        if (button == 0) {
            if (droppedFood != null && droppedFood.isBeingDragged()) {
                // Check if food is near buddy before dropping it
                if (droppedFood.isNearBuddy(buddyPosX + SPRITE_WIDTH/2, buddyPosY + SPRITE_HEIGHT/2)) {
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
                if (playBall.isNearBuddy(buddyPosX + SPRITE_WIDTH/2, buddyPosY + SPRITE_HEIGHT/2)) {
                    // If close to buddy, don't throw - start playing
                    isPlaying = true;
                    isHoldingBall = true;
                    isChasingBall = false;
                    playBall.setGrabbedByBuddy(true);
                    needsPlay = false; // Reset play need immediately
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

        if (!isDisabled) return false;

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

        if (!isDisabled) return;

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
     * Checks if the mouse is over the buddy
     */
    public boolean isMouseOverBuddy(double mouseX, double mouseY) {
        return mouseX >= buddyPosX && mouseX < buddyPosX + SPRITE_WIDTH &&
                mouseY >= buddyPosY && mouseY < buddyPosY + SPRITE_HEIGHT;
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
        this.buddyPosY = height - SPRITE_HEIGHT - 10; // Keep at bottom

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

        LOGGER.info("Updated buddy position to y={}", this.buddyPosY);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(buddyPosX, buddyPosY, SPRITE_WIDTH, SPRITE_HEIGHT);
    }

    /**
     * Helper method to reset animation frame and counter
     */
    public void resetAnimationFrame() {
        currentFrame = 0;
        animationRenderTicks = 0;
    }

    /**
     * Sets the activity duration
     */
    public void setCurrentStateDuration(int duration) {
        this.currentStateDuration = duration;
    }

    public int getBuddyPosX() {
        return buddyPosX;
    }

    public int getBuddyPosY() {
        return buddyPosY;
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

    public int getAnimationRenderTicks() {
        return animationRenderTicks;
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

    public void setHoldingBall(boolean holdingBall) {
        isHoldingBall = holdingBall;
    }

    public boolean isChasingBall() {
        return isChasingBall;
    }

    public void setChasingBall(boolean chasingBall) {
        isChasingBall = chasingBall;
    }

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
            this.startSleeping();
        }
    }

    public void startSleeping() {

        isSleeping = true;

        isEating = false;
        isBeingPet = false;
        isPlaying = false;
        isHoldingBall = false;
        isChasingBall = false;
        if (playBall != null) {
            playBall = null; // Remove play ball if buddy falls asleep
        }

        // Cancel all special activities
        isStanding = false;
        isHopping = false;
        isLookingAround = false;
        isStretching = false;
        isExcited = false;

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
        TamagotchiBuddySerializer.saveBuddy(this);
    }

    /**
     * Loads the buddy's state from persistent storage
     * @return true if state was successfully loaded, false otherwise
     */
    public boolean loadState() {
        boolean result = TamagotchiBuddySerializer.loadBuddy(this);

        // After loading, clean up any poops that might be off-screen
        cleanupInvalidPoops();

        return result;
    }

    /**
     * Repositions poops that are off-screen and removes any with invalid coordinates
     */
    public void cleanupInvalidPoops() {
        List<Poop> validPoops = new ArrayList<>();
        boolean madeChanges = false;

        for (Poop poop : poops) {
            int x = poop.getX();
            int y = poop.getY();

            // Check for extreme invalid values that should be removed
            boolean isExtremelyInvalid = (x < 0) || (y < 0) || (x > 10000) || (y > 10000);

            if (isExtremelyInvalid) {
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

    /**
     * Returns true X% of the time.
     *
     * @param percentage Value between 0.0 and 100.0
     */
    public boolean chanceCheck(float percentage) {
        if (percentage < 0.0f) percentage = 0.0f;
        if (percentage > 100.0f) percentage = 100.0f;
        return this.random.nextFloat() < (percentage / 100.0f);
    }

}