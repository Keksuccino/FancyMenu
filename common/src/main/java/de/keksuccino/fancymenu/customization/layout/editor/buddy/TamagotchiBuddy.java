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
import java.util.function.BooleanSupplier;

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
    private static final int ATLAS_ROWS = 10; // Different states (idle, happy, eating, etc.) - now 10 rows
    private static final int RADIAL_MENU_RADIUS = 50; // Distance of buttons from buddy's center
    private static final int DISABLED_BUTTON_COLOR = 0x80FFFFFF; // 50% transparent white for disabled buttons
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

    // Game state
    private int posX;
    private int posY;
    private int screenWidth;
    private int screenHeight;
    private boolean facingLeft = false;
    private boolean isVisible = true;
    private boolean isOffScreen = false;
    private boolean isRadialMenuOpen = false;
    private boolean isStandingIdle = false;
    private List<RadialButton> radialButtons = new ArrayList<>();

    // Animation
    private int currentState = STATE_IDLE_WALK;
    private int currentFrame = 0;
    private int animationTicks = 0;
    private int animationSpeed = 5; // Ticks per frame

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
    private float hopChance = 0.005f;  // Chance per tick to hop while walking
    private float lookChance = 0.002f;  // Chance per tick to look around
    private float stretchChance = 0.001f; // Chance per tick to stretch
    private float excitedChance = 0.001f; // Chance per tick to get excited
    private int minStandTime = 30;     // Shorter minimum ticks to stand
    private int maxStandTime = 80;     // Shorter maximum ticks to stand

    // Food item and play ball
    private FoodItem droppedFood = null;
    private PlayBall playBall = null;

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

        // Initialize radial buttons
        initRadialButtons();
    }

    /**
     * Initializes the radial menu buttons
     */
    private void initRadialButtons() {
        // Clear existing buttons to avoid duplicates
        radialButtons.clear();

        // Create feed button
        radialButtons.add(new RadialButton(
                TEXTURE_FEED_BUTTON,
                "Feed",
                () -> {
                    dropFoodAboveBuddy();
                    isRadialMenuOpen = false;
                },
                // Condition for button to be active
                () -> droppedFood == null
        ));

        // Create play button (replacing work button)
        radialButtons.add(new RadialButton(
                TEXTURE_PLAY_BUTTON,
                "Play",
                () -> {
                    startPlaying();
                    isRadialMenuOpen = false;
                },
                // Condition for button to be active
                () -> !isSleeping && energy > 20 && playBall == null
        ));

        // Log the number of buttons for debugging
        LOGGER.info("Initialized " + radialButtons.size() + " radial menu buttons");
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

        // Render the play ball if it exists
        if (playBall != null) {
            playBall.render(graphics);
        }

        // Calculate vertical offset and squash/stretch for hopping animation
        int verticalOffset = 0;
        int heightAdjust = 0;
        int widthAdjust = 0;
        int yPosAdjust = 0;

        if (isHopping && currentState != STATE_EXCITED) {
            // Create a more dynamic bouncing effect with sine wave
            float hopProgress = animationTicks * 0.3f;
            float bounceFactor = (float)Math.sin(hopProgress) * 6.0f;
            verticalOffset = (int)bounceFactor;

            // Apply squash and stretch effect
            if (bounceFactor < -3.0f) {
                // At bottom of hop (squash)
                heightAdjust = -3; // Reduce height
                widthAdjust = 4;   // Increase width
                yPosAdjust = 3;    // Adjust Y position to keep bottom aligned
            } else if (bounceFactor > 3.0f) {
                // At top of hop (stretch)
                heightAdjust = 3;  // Increase height
                widthAdjust = -2;  // Reduce width
            }
        }

        // Render the buddy with squash/stretch if hopping
        if (isHopping && currentState != STATE_EXCITED) {
            // Custom rendering for squash/stretch
            int renderWidth = SPRITE_WIDTH + widthAdjust;
            int renderHeight = SPRITE_HEIGHT + heightAdjust;
            int xPos = posX - widthAdjust/2;  // Center the width adjustment
            int yPos = posY + verticalOffset + yPosAdjust;

            if (facingLeft) {
                // Custom stretch/squash rendering when facing left
                graphics.blit(
                        RenderType::guiTextured,
                        TEXTURE_ATLAS,
                        xPos, yPos,
                        texX, texY,
                        renderWidth, renderHeight,
                        SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                );
            } else {
                // Custom stretch/squash rendering when facing right
                graphics.blit(
                        RenderType::guiTextured,
                        TEXTURE_ATLAS,
                        xPos, yPos,
                        texX, texY,
                        renderWidth, renderHeight,
                        SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                );
            }
        } else {
            // Normal rendering without squash/stretch
            if (facingLeft) {
                // Use our custom method for mirrored rendering
                RenderingUtils.blitMirrored(
                        graphics,
                        TEXTURE_ATLAS,
                        posX, posY + verticalOffset,
                        texX, texY,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                );
            } else {
                graphics.blit(
                        RenderType::guiTextured,
                        TEXTURE_ATLAS,
                        posX, posY + verticalOffset,
                        texX, texY,
                        SPRITE_WIDTH, SPRITE_HEIGHT,
                        SPRITE_WIDTH * ATLAS_COLUMNS, SPRITE_HEIGHT * ATLAS_ROWS
                );
            }
        }

        // Render looking direction indicator if looking around
        if (isLookingAround && !isRadialMenuOpen) {
            int lookX = posX + SPRITE_WIDTH/2 + (lookDirection * 10);
            int lookY = posY - 10;

            // Draw a small eye or attention mark
            int dotSize = 3;
            int color = 0xFFFFFFFF; // White
            graphics.fill(lookX - dotSize/2, lookY - dotSize/2,
                    lookX + dotSize/2, lookY + dotSize/2, color);
        }

        // Render needs indicator when radial menu is not open
        renderNeedsIndicator(graphics);

        // Render the radial menu if open
        if (isRadialMenuOpen) {
            renderRadialMenu(graphics, mouseX, mouseY);
        }

        // Render any dropped food
        if (droppedFood != null) {
            droppedFood.render(graphics);
        }

        // Render status bars
        renderStatusBars(graphics);
    }

    /**
     * Renders the radial menu when it's open
     */
    private void renderRadialMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isRadialMenuOpen) return;

        int centerX = posX + SPRITE_WIDTH / 2;
        int centerY = posY + SPRITE_HEIGHT / 2;
        int numButtons = radialButtons.size();

        if (numButtons > 0) {
            // Use a 180-degree arc above the buddy instead of a full circle
            double arcAngle = Math.PI; // 180 degrees
            double startAngle = -Math.PI; // Start at left side (-180 degrees)

            // Calculate angle step based on number of buttons and arc angle
            double angleStep = arcAngle / (numButtons - 1);
            if (numButtons == 1) {
                // If there's only one button, place it directly above
                startAngle = -Math.PI / 2;
                angleStep = 0;
            }

            LOGGER.info("Rendering radial menu with " + numButtons + " buttons");

            int buttonIndex = 0;
            for (RadialButton button : radialButtons) {
                // Calculate position using the arc positioning
                double angle = startAngle + buttonIndex * angleStep;

                // Place buttons above buddy's head
                int buttonX = centerX + (int)(Math.cos(angle) * RADIAL_MENU_RADIUS) - RadialButton.BUTTON_SIZE / 2;
                // Make sure buttons are above the buddy (use negative sin values)
                int buttonY = centerY - Math.abs((int)(Math.sin(angle) * RADIAL_MENU_RADIUS)) - RadialButton.BUTTON_SIZE / 2 - 10;

                // Update button position
                button.setPosition(buttonX, buttonY);

                // Update button active state
                button.updateActiveState();

                // Check if button is hovered
                boolean hovered = button.isMouseOver(mouseX, mouseY) && button.isActive();

                LOGGER.info("Button " + buttonIndex + " texture: " + button.getTexture() +
                        " position: (" + buttonX + "," + buttonY + ") active: " + button.isActive());

                // Always render the button, but with different appearance based on state
                if (button.isActive()) {
                    // Render normally if active
                    int yOffset = hovered ? RadialButton.BUTTON_SIZE : 0;
                    graphics.blit(
                            RenderType::guiTextured,
                            button.getTexture(),
                            buttonX, buttonY,
                            0, yOffset,
                            RadialButton.BUTTON_SIZE, RadialButton.BUTTON_SIZE,
                            RadialButton.BUTTON_SIZE, RadialButton.BUTTON_SIZE * 2
                    );
                } else {
                    // Render with transparency if inactive (disabled)
                    graphics.blit(
                            RenderType::guiTextured,
                            button.getTexture(),
                            buttonX, buttonY,
                            0, 0, // Always use non-hovered state for disabled buttons
                            RadialButton.BUTTON_SIZE, RadialButton.BUTTON_SIZE,
                            RadialButton.BUTTON_SIZE, RadialButton.BUTTON_SIZE * 2,
                            DISABLED_BUTTON_COLOR
                    );
                }

                buttonIndex++;
            }
        }
    }

    /**
     * Renders an indicator above the buddy's head showing its current need
     */
    private void renderNeedsIndicator(GuiGraphics graphics) {
        // Don't render thought bubble if any of these conditions are true
        if (isRadialMenuOpen || isEating || isBeingPet || isPlaying || isSleeping) return;

        int iconSize = 16;
        int iconX = posX + (SPRITE_WIDTH / 2) - (iconSize / 2);
        int iconY = posY - iconSize - 5;

        ResourceLocation icon = null;

        if (needsFood) {
            icon = FoodItem.TEXTURE_FOOD;
        } else if (needsPet) {
            icon = TEXTURE_HEART;
        } else if (needsPlay) {
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

        // Update standing idle state
        isStandingIdle = isRadialMenuOpen || isEating || isBeingPet || (isPlaying && isHoldingBall) || isSleeping;

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

                // Don't end play here, it ends when the ball is removed
                if (isPlaying && playBall == null) {
                    isPlaying = false;
                    isHoldingBall = false;
                }

                // Don't end sleep here, it ends when energy is full
                if (!isSleeping && !isPlaying) {
                    updateVisualState();
                }
            }
        }

        // Update activity duration
        if (activityDuration > 0) {
            activityDuration--;
            if (activityDuration <= 0) {
                // End the current special activity
                if (isLookingAround || isStretching) {
                    // These activities set isStanding=true, so we need to end both
                    isStanding = false;
                }

                isHopping = false;
                isLookingAround = false;
                isStretching = false;
                isExcited = false;
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
        if (happiness > 70 && random.nextFloat() < excitedChance) {
            startExcitement();
            return;
        }

        // Chance to look around
        if (random.nextFloat() < lookChance) {
            startLookingAround();
            return;
        }

        // Chance to stretch
        if (random.nextFloat() < stretchChance) {
            startStretching();
            return;
        }

        // Chance to hop while walking
        if (!isHopping && random.nextFloat() < hopChance) {
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
                isOffScreen = true;
                // Random chance to come back
                if (random.nextFloat() < 0.01f) {
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
                isOffScreen = true;
                // Random chance to come back
                if (random.nextFloat() < 0.01f) {
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
    }

    /**
     * Starts the buddy hopping animation
     */
    private void startHopping() {
        isHopping = true;
        // Longer duration for more noticeable hopping
        activityDuration = 40 + random.nextInt(80); // Hop for 2-6 seconds

        // If buddy is very happy, hop for even longer
        if (happiness > 80) {
            activityDuration += 60;
        }

        // Reset animation ticks for smooth start
        animationTicks = 0;
    }

    /**
     * Starts the buddy looking around animation
     */
    private void startLookingAround() {
        isLookingAround = true;
        isStanding = true;
        lookDirection = random.nextInt(3) - 1; // -1, 0, or 1
        activityDuration = 60 + random.nextInt(60); // Look for 3-6 seconds
    }

    /**
     * Starts the buddy stretching animation
     */
    private void startStretching() {
        isStretching = true;
        isStanding = false; // Not needed since we have a dedicated animation state
        activityDuration = 60; // Stretch for 3 seconds
    }

    /**
     * Starts the buddy excitement animation
     */
    private void startExcitement() {
        isExcited = true;
        activityDuration = 40 + random.nextInt(40); // Get excited for 2-4 seconds
    }

    /**
     * Performs a random action when turning around
     */
    private void performRandomActionOnTurn() {
        float chance = random.nextFloat();

        if (chance < 0.3f) {
            // 30% chance to just stand for a moment
            startStanding();
        } else if (chance < 0.4f) {
            // 10% chance to look around
            startLookingAround();
        } else if (chance < 0.45f && happiness > 70) {
            // 5% chance to get excited if happy
            startExcitement();
        } else if (chance < 0.5f) {
            // 5% chance to stretch
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
            playBall.setGrabbed(true);
            currentState = STATE_PLAYING_STAND;
        }
    }

    /**
     * Decides what behavior the buddy should exhibit next
     */
    private void decideNextBehavior() {
        // Don't change behavior if off screen or standing idle
        if (isOffScreen || isStandingIdle) return;

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
        if (isSleeping) {
            currentState = STATE_SLEEPING;
        } else if (isEating) {
            currentState = STATE_EATING_STAND;
        } else if (isBeingPet) {
            currentState = STATE_HAPPY_STAND;
        } else if (isExcited) {
            currentState = STATE_EXCITED;     // Use dedicated excitement state
        } else if (isStretching) {
            currentState = STATE_STRETCHING;  // Use dedicated stretching state
        } else if (isPlaying && isHoldingBall) {
            // Only show playing animation when actively playing with ball
            currentState = STATE_PLAYING_STAND;
        } else if (isChasingBall) {
            // Use walking animation when chasing (but could be sad if energy low)
            currentState = energy < 30 ? STATE_SAD_WALK : STATE_IDLE_WALK;
        } else if (isStanding || isLookingAround) {
            // Use standing animations for special activities
            currentState = (needsFood || needsPet || needsPlay) ? STATE_SAD_STAND : STATE_IDLE_STAND;
        } else if (isRadialMenuOpen) {
            // Use standing animations when menu is open
            currentState = (needsFood || needsPet || needsPlay) ? STATE_SAD_STAND : STATE_IDLE_STAND;
        } else if (needsFood || needsPet || needsPlay) {
            // When walking and sad
            currentState = STATE_SAD_WALK;
        } else {
            // When walking and happy/normal
            currentState = STATE_IDLE_WALK;
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
            // Waking up
            isSleeping = false;
            happiness += 5;
            updateVisualState();
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
            playBall.setGrabbed(true);
            needsPlay = false; // Reset the play need after starting to play
            currentState = STATE_PLAYING_STAND;
        }
    }

    /**
     * Creates and drops a food item above the buddy
     */
    private void dropFoodAboveBuddy() {
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
        if (!isVisible || isOffScreen) return false;

        if (button == 1) { // Right click
            // If clicked on buddy, toggle radial menu
            if (isMouseOverBuddy(mouseX, mouseY)) {
                isRadialMenuOpen = !isRadialMenuOpen;
                updateVisualState(); // Update visual state immediately when opening/closing menu
                return true;
            }
        } else if (button == 0) { // Left click
            // If radial menu is open, check for button clicks
            if (isRadialMenuOpen) {
                for (RadialButton radialButton : radialButtons) {
                    if (radialButton.isActive() && radialButton.isMouseOver(mouseX, mouseY)) {
                        radialButton.onClick();
                        return true;
                    }
                }

                // Close menu if clicked outside
                isRadialMenuOpen = false;
                updateVisualState(); // Update visual state when closing menu
                return true;
            }

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
                droppedFood.drop((int)mouseX, (int)mouseY);
                return true;
            }

            // Handle ball throw on release
            if (playBall != null && playBall.isBeingDragged()) {
                playBall.throw_((int)mouseX, (int)mouseY);

                // If buddy was playing, stop playing and start chasing
                if (isPlaying) {
                    isPlaying = false;
                    isChasingBall = true;
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
}