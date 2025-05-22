package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationState;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationStates;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.gui.BuddyStatusScreen;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.FoodItem;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.PlayBall;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.Poop;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddyAchievement;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.LevelingManager;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationState.*;
import static de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationStates.RUNNING;
import static de.keksuccino.fancymenu.customization.layout.editor.buddy.animation.AnimationStates.WALKING;

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
    public boolean isPeeking = true; // Start in peeking mode until user clicks
    public boolean hasBeenAwakened = false; // Track if buddy has been clicked for the first time ever
    public boolean isActivelyPeeking = false; // Whether buddy is currently visible during a peek
    public int peekTimer = 0; // Timer for when to peek next
    public int peekDuration = 0; // How long the current peek should last

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
    public int poopingInterval = 6000; // Time between potential poops (in ticks) - increased from 1500
    public float poopChancePercentage = 1f; // Chance to poop when the interval is reached - reduced from 5%
    public static final int MAX_POOPS_BEFORE_SAD = 3; // Buddy gets sad if there are too many poops

    // Track visibility changes
    public boolean wasDisabled = true;
    public boolean wasOffScreen = false;

    public BuddyStatusScreen statusScreen;
    
    // Leveling system
    private final LevelingManager levelingManager;
    
    // Level effect multipliers (set by leveling manager based on level)
    private float hungerMultiplier = 1.0f;
    private float happinessMultiplier = 1.0f;
    private float energyMultiplier = 1.0f;
    private float happinessGainMultiplier = 1.0f;
    private float experienceMultiplier = 1.0f;
    private float needsUnderstandingBonus = 0.0f;
    private float luckBonus = 0.0f;
    
    // XP Sources tracking
    private long lastXpGainTime = 0;
    private final Map<String, Long> xpCooldowns = new HashMap<>();

    private long lastSessionUpdateTime;
    
    // Event listeners
    public final List<GuiEventListener> children = new ArrayList<>();

    /**
     * Creates a new TamagotchiBuddy
     */
    public TamagotchiBuddy(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Initialize with random timer
        this.stateChangeTimer = random.nextInt(200) + 100;

        // Initialize session timers
        // Activity tracking for achievements
        this.lastSessionUpdateTime = System.currentTimeMillis();

        // Initialize leveling manager
        this.levelingManager = new LevelingManager(this);
        
        // Initialize GUI
        this.statusScreen = new BuddyStatusScreen(this, levelingManager);
        
        // Start in peeking state
        this.isPeeking = true;
        
        // Set initial peek timer (buddy will peek after 1-2 minutes initially)
        this.peekTimer = (60 * 20) + MathUtils.getRandomNumberInRange(0, 1200); // 1-2 minutes in ticks
        this.isActivelyPeeking = false; // Start hidden
        
        // Position buddy off-screen initially
        this.buddyPosX = -SPRITE_WIDTH - 10;
        this.buddyPosY = screenHeight - SPRITE_HEIGHT - 10;
        
        LOGGER.info("Buddy created in hidden state, will peek in {} seconds", peekTimer / 20.0f);
        
        // Start with full stats since buddy is dormant until awakened
        this.hunger = 100.0f;
        this.happiness = 100.0f;
        this.energy = 100.0f;
        this.funLevel = 100.0f;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        RenderSystem.enableBlend();

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

        // Don't render buddy if it's peeking but not actively showing
        if (isPeeking && !isActivelyPeeking) {
            // Buddy is hidden off-screen
            return;
        }

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
        
        // Render the status screen if visible
        if (statusScreen.isVisible()) {
            statusScreen.render(graphics, mouseX, mouseY, partialTick);
        }
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
        
        // Don't show needs if buddy hasn't been awakened yet
        if (!hasBeenAwakened) return;

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
                    TEXTURE_THOUGHT_BUBBLE,
                    iconX - 4, iconY - 4,
                    0, 0,
                    iconSize + 8, iconSize + 8,
                    iconSize + 8, iconSize + 8
            );

            // Then render the icon inside it
            graphics.blit(
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
        
        // If buddy is peeking, only update minimal things
        if (isPeeking) {
            // First-time peeking - buddy is dormant until awakened
            if (!hasBeenAwakened) {
                // Update peek timer
                if (peekTimer > 0) {
                    peekTimer--;
                } else if (!isActivelyPeeking) {
                    // Start peeking
                    startActivelyPeeking();
                    // Set duration for this peek (15 seconds +/- a few seconds)
                    peekDuration = (15 * 20) + MathUtils.getRandomNumberInRange(-60, 60); // 14-16 seconds in ticks
                    LOGGER.info("Buddy starting to peek for {} seconds", peekDuration / 20.0f);
                }
                
                // Update peek duration if actively peeking
                if (isActivelyPeeking && peekDuration > 0) {
                    peekDuration--;
                    if (peekDuration <= 0) {
                        // Stop peeking and go back off-screen
                        stopActivelyPeeking();
                        // Set timer for next peek (5 minutes +/- 1-2 minutes)
                        peekTimer = (5 * 60 * 20) + MathUtils.getRandomNumberInRange(-2400, 2400); // 3-7 minutes in ticks
                        LOGGER.info("Buddy hiding again, will peek again in {} seconds", peekTimer / 20.0f);
                    }
                }
                
                // No stat changes, no needs, just wait for the player
                updateVisualState();
                return;
            }
            
            // Regular peeking after being awakened - stats still decrease but at half rate
            hunger = Math.max(0, hunger - (0.0025f * hungerMultiplier));
            happiness = Math.max(0, happiness - (0.0015f * happinessMultiplier));
            energy = Math.max(0, energy - (0.001f * energyMultiplier));
            funLevel = Math.max(0, funLevel - 0.001f);
            
            // Update needs
            updateStatsAndNeeds();
            
            // Stop peeking if buddy has critical needs
            if (needsFood || needsPet || needsPlay || isSleepy) {
                isPeeking = false;
                LOGGER.info("Buddy stopped peeking due to critical needs");
                // Move away from edge
                if (facingLeft) {
                    buddyPosX = screenWidth - SPRITE_WIDTH - 50;
                } else {
                    buddyPosX = 50;
                }
            }
            
            // Small chance to come out of peeking on its own (only after being awakened)
            if (this.chanceCheck(0.1f)) {
                isPeeking = false;
                LOGGER.info("Buddy came out of peeking on its own!");
                // Move away from edge
                if (facingLeft) {
                    buddyPosX = screenWidth - SPRITE_WIDTH - 50;
                } else {
                    buddyPosX = 50;
                }
            }
            
            // Update visual state for peeking
            updateVisualState();
            
            // Skip most other behaviors while peeking
            return;
        }
        
        // Update session time tracking
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSessionUpdateTime > 60000) { // Update every minute
            if (levelingManager != null) {
                levelingManager.updateSessionTime();
            }
            lastSessionUpdateTime = currentTime;
            
            // Check for midnight companion achievement
            checkTimeBasedAchievements(currentTime);
        }

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

        // Don't update stats or needs if buddy hasn't been awakened yet
        if (!hasBeenAwakened) {
            return;
        }

        // Apply attribute effects from leveling system to stat changes
        
        // Decrease stats over time - modified by attributes (10x slower)
        hunger = Math.max(0, hunger - (0.005f * hungerMultiplier));
        happiness = Math.max(0, happiness - (0.003f * happinessMultiplier));
        funLevel = Math.max(0, funLevel - 0.002f);  // Fun decreases slower than hunger

        // Energy decreases unless sleeping
        if (isSleeping) {
            energy = Math.min(100, energy + 0.03f);  // Reduced from 0.3f to match slower stat decrease
        } else {
            energy = Math.max(0, energy - (0.002f * energyMultiplier));

            // Playing increases fun but drains energy faster
            if (isPlaying) {
                energy = Math.max(0, energy - (0.01f * energyMultiplier));
                funLevel = Math.min(100, funLevel + 0.05f);  // Playing significantly increases fun
            }

            // Chasing drains energy faster
            if (isChasingBall) {
                energy = Math.max(0, energy - (0.008f * energyMultiplier));
            }

            // Hopping drains energy faster
            if (isHopping) {
                energy = Math.max(0, energy - (0.005f * energyMultiplier));
            }

            // Excitement drains energy faster
            if (isExcited) {
                energy = Math.max(0, energy - (0.01f * energyMultiplier));
                // But it also increases happiness!
                happiness = Math.min(100, happiness + (0.01f * happinessGainMultiplier));
            }

            // Running drains energy faster
            if (this.currentState == RUNNING) {
                energy = Math.max(0, energy - (0.01f * energyMultiplier));
            }

        }

        // Set needs based on stats, improved by Empathy attribute
        float needThreshold = 20f * (1f - (needsUnderstandingBonus * 0.3f)); // Reduced from 30f - gives more time before needs trigger
        needsFood = hunger < needThreshold;
        needsPet = happiness < needThreshold;
        needsPlay = funLevel < needThreshold;
        isSleepy = energy < needThreshold && energy >= (needThreshold / 3);

        // Auto-sleep if energy is critically low
        if (energy < (needThreshold / 3) && !isSleeping) {
            this.startSleeping();
        }
        
        // Check if we should award experience for maintaining good stats
        awardStatMaintenanceXp();
        
        // Check for stat-based achievements
        if (levelingManager != null) {
            levelingManager.checkStatAchievements();
        }
    }
    
    /**
     * Awards experience for maintaining good stats over time
     */
    private void awardStatMaintenanceXp() {
        // Only check once every minute
        long now = System.currentTimeMillis();
        if (now - lastXpGainTime < 60000) return;
        
        // Check if all stats are good
        if (hunger >= 70 && happiness >= 70 && energy >= 70 && funLevel >= 70) {
            // Award XP for good stat maintenance
            gainExperience("statMaintenance", 5, 60000);
            
            // If ALL stats are excellent (90+), award bonus XP
            if (hunger >= 90 && happiness >= 90 && energy >= 90 && funLevel >= 90) {
                gainExperience("excellentStats", 10, 300000); // 5 minute cooldown on excellent stats bonus
            }
        }
        
        lastXpGainTime = now;
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

        // Small chance to go back to peeking (only if already awakened)
        if (hasBeenAwakened && this.chanceCheck(0.5f) && !needsFood && !needsPet && !needsPlay && !isSleepy && happiness > 30) {
            startPeeking();
            return;
        }

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
        // Don't stop isPeeking here - it's handled separately
    }

    /**
     * Starts actively showing the buddy during a peek
     */
    public void startActivelyPeeking() {
        isActivelyPeeking = true;
        
        // Position buddy at edge for peeking
        if (random.nextBoolean()) {
            // Peek from left side
            buddyPosX = -(int)(SPRITE_WIDTH * 0.1f);
            facingLeft = false;
        } else {
            // Peek from right side  
            buddyPosX = screenWidth - (int)(SPRITE_WIDTH * 0.9f);
            facingLeft = true;
        }
        
        LOGGER.info("Buddy actively peeking from {} side", facingLeft ? "right" : "left");
    }
    
    /**
     * Stops actively showing the buddy and hides it off-screen
     */
    public void stopActivelyPeeking() {
        isActivelyPeeking = false;
        
        // Move buddy completely off-screen
        if (facingLeft) {
            buddyPosX = screenWidth + SPRITE_WIDTH;
        } else {
            buddyPosX = -SPRITE_WIDTH - 10;
        }
        
        LOGGER.info("Buddy hiding off-screen");
    }

    /**
     * Starts the buddy peeking animation
     */
    public void startPeeking() {
        if (this.lockedInState()) return;
        
        LOGGER.info("Buddy starting to peek from the edge");
        
        isPeeking = true;
        // Move buddy to edge of screen
        if (buddyPosX < screenWidth / 2) {
            // Peek from left edge - hide only 10% of sprite
            buddyPosX = -(int)(SPRITE_WIDTH * 0.1f);
            facingLeft = false; // Face right when peeking from left
        } else {
            // Peek from right edge - show 90% of sprite
            buddyPosX = screenWidth - (int)(SPRITE_WIDTH * 0.9f);
            facingLeft = true; // Face left when peeking from right
        }
        
        // Stop all other activities
        stopAllStandingActions();
        isPeeking = true; // Set again after stopAllStandingActions
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
        if (poopX > screenWidth || poopY > screenHeight || poopX == Integer.MAX_VALUE || poopY == Integer.MAX_VALUE) {
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
        // Apply level-based bonus to food effectiveness
        float levelRatio = (float) Math.min(30, levelingManager.getCurrentLevel()) / 30f;
        float foodEffectiveness = 1.0f + (levelRatio * 0.5f); // Up to 50% more effective at max level
        hunger = Math.min(100, hunger + (20 * foodEffectiveness)); // Reduced from 40
        happiness = Math.min(100, happiness + (5 * happinessGainMultiplier)); // Reduced from 10
    }

    /**
     * Called when the buddy is petted
     */
    public void pet() {
        if (isSleeping) {
            // Waking up - make buddy grumpy
            isSleeping = false;
            // Slightly decrease happiness when woken up
            happiness = Math.max(0, happiness - 2.5f); // Reduced from 5
            // Start grumpy state
            startGrumpyState();
        } else {
            isBeingPet = true;
            // Happiness gain is affected by the happiness gain multiplier from Charisma attribute
            happiness = Math.min(100, happiness + (15 * happinessGainMultiplier)); // Reduced from 30
            // Sometimes get excited when petted if already happy
            if ((happiness > 70) && this.chanceCheck(30.0f + (luckBonus * 10.0f))) { // Luck affects chance of excitement
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
        if (happiness < 15) return true; // Reduced from 20 to match slower stat decrease
        return (needsFood || needsPet || needsPlay);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (!isDisabled) return false;
        
        // First handle the leveling screen if it's visible (highest priority)
        if (statusScreen.isVisible()) {
            return statusScreen.mouseClicked(mouseX, mouseY, button);
        }

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
                
                // Award XP for playing with buddy
                gainExperience("playWithBuddy", 10, 60000);
                if (levelingManager != null) {
                    levelingManager.incrementPlayCount();
                }
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
                    happiness = Math.min(100, happiness + 2.5f); // Reduced from 5
                    LOGGER.info("Cleaned up poop at ({},{}), happiness: {}", poop.getX(), poop.getY(), happiness);
                    
                    // Award XP for cleaning poop
                    gainExperience("cleanPoop", 5, 30000);
                    if (levelingManager != null) {
                        levelingManager.incrementPoopCleanCount();
                    }
                    
                    return true;
                }
            }
        }

        if (button == 1) { // Right click
            // If clicked on buddy, open the stats screen directly (only if awakened)
            if (isMouseOverBuddy(mouseX, mouseY) && hasBeenAwakened) {
                statusScreen.show(screenWidth, screenHeight);
                LOGGER.info("Opening buddy stats screen (on right-click)");
                return true;
            }
        } else if (button == 0) { // Left click
            // Normal petting if menu isn't open
            if (isMouseOverBuddy(mouseX, mouseY)) {
                // If buddy is peeking, stop peeking and start walking
                if (isPeeking && isActivelyPeeking) {
                    isPeeking = false;
                    isActivelyPeeking = false;
                    peekTimer = 0;
                    peekDuration = 0;
                    
                    // Move buddy away from edge to a visible position
                    if (facingLeft) {
                        // Was peeking from right, move left a bit
                        buddyPosX = screenWidth - SPRITE_WIDTH - 50;
                    } else {
                        // Was peeking from left, move right a bit
                        buddyPosX = 50;
                    }
                    
                    // First time being awakened?
                    if (!hasBeenAwakened) {
                        hasBeenAwakened = true;
                        LOGGER.info("Buddy has been awakened for the first time!");
                        // Give a bigger happiness boost for first awakening
                        happiness = Math.min(100, happiness + 10);
                        // Award special XP for first interaction
                        gainExperience("firstAwakening", 50, Long.MAX_VALUE); // One-time XP bonus
                    } else {
                        LOGGER.info("Buddy stopped peeking and came out to play!");
                        // Give a small happiness boost for coming out
                        happiness = Math.min(100, happiness + 2.5f); // Reduced from 5
                    }
                } else {
                    // Normal petting
                    pet();
                    
                    // Award XP for petting
                    gainExperience("petBuddy", 5, 10000); // Increased from 2 to compensate for slower gameplay
                    if (levelingManager != null) {
                        levelingManager.incrementPetCount();
                    }
                }
                
                return true;
            }

            // Check if clicked on food (only if awakened)
            if (hasBeenAwakened && droppedFood != null && droppedFood.isMouseOver(mouseX, mouseY)) {
                droppedFood.pickup((int)mouseX, (int)mouseY);
                return true;
            }

            // Check if clicked on play ball (only if awakened)
            if (hasBeenAwakened && playBall != null && playBall.isMouseOver(mouseX, mouseY)) {
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
        
        // Handle the leveling screen first if it's visible
        if (statusScreen.isVisible()) {
            statusScreen.mouseReleased(mouseX, mouseY, button);
            return true;
        }

        if (button == 0) {
            if (droppedFood != null && droppedFood.isBeingDragged()) {
                // Check if food is near buddy before dropping it
                if (droppedFood.isNearBuddy(buddyPosX + SPRITE_WIDTH/2, buddyPosY + SPRITE_HEIGHT/2)) {
                    // If close to buddy, don't drop - just start eating
                    eatFood();
                    droppedFood = null;
                    
                    // Award XP for feeding
                    gainExperience("feedBuddy", 5, 30000);
                    if (levelingManager != null) {
                        levelingManager.incrementFeedCount();
                    }
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
                    
                    // Award XP for playing with buddy
                    gainExperience("playWithBuddy", 10, 60000);
                    if (levelingManager != null) {
                        levelingManager.incrementPlayCount();
                    }
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
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Handle mouse scroll for leveling screen if visible
        if (statusScreen.isVisible()) {
            return statusScreen.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
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
        
        // Also save the leveling data
        if (levelingManager != null) {
            boolean levelingSaveResult = levelingManager.saveState();
            LOGGER.info("Leveling data saving result: {}", levelingSaveResult);
        }
    }

    /**
     * Loads the buddy's state from persistent storage
     * @return true if state was successfully loaded, false otherwise
     */
    public boolean loadState() {
        boolean result = TamagotchiBuddySerializer.loadBuddy(this);
        
        // Also load the leveling data
        if (levelingManager != null) {
            boolean levelingResult = levelingManager.loadState();
            LOGGER.info("Leveling data loading result: {}", levelingResult);
        }

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
        
        // Apply luck bonus to chance checks
        float effectivePercentage = percentage * (1.0f + (luckBonus * 0.3f));
        effectivePercentage = Math.min(100.0f, effectivePercentage);
        
        return this.random.nextFloat() < (effectivePercentage / 100.0f);
    }
    
    /**
     * Awards experience points to the buddy
     * 
     * @param source The source of the experience (for cooldown tracking)
     * @param amount The base amount of experience to award
     * @param cooldownMs The cooldown in milliseconds before this source can award XP again
     */
    public void gainExperience(String source, int amount, long cooldownMs) {
        if (levelingManager == null) return;
        
        // Check if this source is on cooldown
        long now = System.currentTimeMillis();
        Long lastAwardTime = xpCooldowns.get(source);
        if (lastAwardTime != null && (now - lastAwardTime < cooldownMs)) {
            return;
        }
        
        // Apply intelligence bonus to experience gain
        float finalAmount = amount * experienceMultiplier;
        
        // Award the experience
        List<Integer> newLevels = levelingManager.addExperience(Math.round(finalAmount));
        
        // Update cooldown
        xpCooldowns.put(source, now);
        
        // If leveled up, show a special animation
        if (!newLevels.isEmpty()) {
            startLevelUpCelebration(newLevels);
        }
    }
    
    /**
     * Starts a celebration animation for leveling up
     * 
     * @param newLevels The list of new levels achieved
     */
    private void startLevelUpCelebration(List<Integer> newLevels) {
        // For now, just show excitement (we could add a special animation later)
        startExcitement();
        
        // Log the level up
        for (int level : newLevels) {
            LOGGER.info("Buddy leveled up to level {}!", level);
        }
    }
    
    /**
     * Checks for time-based achievements based on the current time
     * 
     * @param currentTime The current time in milliseconds
     */
    private void checkTimeBasedAchievements(long currentTime) {
        if (levelingManager == null) return;
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        // Check for midnight companion achievement (between midnight and 4 AM)
        if (hour >= 0 && hour < 4) {
            levelingManager.unlockAchievement(BuddyAchievement.AchievementType.MIDNIGHT_COMPANION);
        }
    }
    
    /**
     * Sets the attribute effect multipliers and bonuses from the leveling manager
     * 
     * @param hungerMultiplier The multiplier for hunger decrease rate
     * @param happinessMultiplier The multiplier for happiness decrease rate
     * @param energyMultiplier The multiplier for energy decrease rate
     * @param happinessGainMultiplier The multiplier for happiness gain
     * @param experienceMultiplier The multiplier for experience gain
     * @param needsUnderstandingBonus The bonus to needs understanding
     * @param luckBonus The bonus to luck
     */
    public void setAttributeEffects(float hungerMultiplier, float happinessMultiplier, float energyMultiplier,
                                   float happinessGainMultiplier, float experienceMultiplier, float needsUnderstandingBonus, float luckBonus) {
        this.hungerMultiplier = hungerMultiplier;
        this.happinessMultiplier = happinessMultiplier;
        this.energyMultiplier = energyMultiplier;
        this.happinessGainMultiplier = happinessGainMultiplier;
        this.experienceMultiplier = experienceMultiplier;
        this.needsUnderstandingBonus = needsUnderstandingBonus;
        this.luckBonus = luckBonus;
    }
    
    /**
     * Opens the leveling stats screen
     */
    public void openLevelingScreen() {
        statusScreen.show(screenWidth, screenHeight);
    }
    
    /**
     * Gets the leveling manager for this buddy
     * 
     * @return The leveling manager
     */
    public LevelingManager getLevelingManager() {
        return levelingManager;
    }

}