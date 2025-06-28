package de.keksuccino.fancymenu.customization.layout.editor.buddy.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.Buddy;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.FoodItem;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.PlayBall;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddyAchievement;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.LevelingManager;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.Renderable;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A screen that displays and allows management of the buddy's leveling stats.
 */
public class BuddyStatusScreen implements Renderable {

    private static final Logger LOGGER = LogManager.getLogger();

    // GUI Constants
    private static final int SCREEN_WIDTH = 320;
    private static final int SCREEN_HEIGHT = 240;
    private static final int SCREEN_BORDER_WIDTH = 390;
    private static final int SCREEN_BORDER_HEIGHT = 293;

    // GUI Texture
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("fancymenu", "textures/buddy/gui/status_screen_background.png");
    private static final ResourceLocation BACKGROUND_BORDER_TEXTURE = new ResourceLocation("fancymenu", "textures/buddy/gui/status_screen_background_border.png");
    private static final ResourceLocation TAB_BUTTON_TEXTURE_NORMAL = new ResourceLocation("fancymenu", "textures/buddy/gui/tab_button_normal.png");
    private static final ResourceLocation TAB_BUTTON_TEXTURE_SELECTED = new ResourceLocation("fancymenu", "textures/buddy/gui/tab_button_selected.png");

    // Tab Indices
    private static final int TAB_STATS = 0;
    private static final int TAB_ACHIEVEMENTS = 1;

    // Reference to the buddy and its leveling manager
    private final Buddy buddy;
    private final LevelingManager levelingManager;
    private final Font font;

    // GUI state
    private boolean isVisible = false;
    private int guiX;
    private int guiY;
    private int currentTab = TAB_STATS;
    private final List<BuddyGuiButton> buttons = new ArrayList<>();
    private final List<BuddyGuiButton> actionButtons = new ArrayList<>();
    private int achievementsScrollOffset = 0;
    
    // Sleep button cooldown
    public long sleepButtonCooldownEnd = 0;

    // Mouse handling
    private boolean isMouseClicked = false;

    /**
     * Creates a new leveling stats screen.
     *
     * @param buddy The buddy this screen is for
     * @param levelingManager The leveling manager to display stats from
     */
    public BuddyStatusScreen(@NotNull Buddy buddy, @NotNull LevelingManager levelingManager) {
        this.buddy = buddy;
        this.levelingManager = levelingManager;
        this.font = Minecraft.getInstance().font;
        initButtons();
    }

    /**
     * Initializes the GUI buttons
     */
    private void initButtons() {

        buttons.clear();

        // Close button (X icon)
        buttons.add(new BuddyGuiButton(
                this.buddy,
                0, 0, 20, 20,
                buddy -> "",
                this::hide,
                () -> true
        ).setCloseButtonTextures());

        actionButtons.clear();

        // Create feed button
        actionButtons.add(new BuddyGuiButton(
                this.buddy,
                buddy -> "Feed",
                () -> {
                    // Only allow feeding if awakened
                    if (!buddy.hasBeenAwakened) return;
                    
                    int mouseX = MouseInput.getMouseX();
                    int mouseY = MouseInput.getMouseY();
                    
                    LOGGER.debug("Creating food at screen coordinates: ({}, {})", mouseX, mouseY);

                    // Create the food with drag mode already enabled
                    FoodItem food = new FoodItem(mouseX, mouseY, buddy);
                    buddy.setDroppedFood(food);
                    food.stickToCursor = true;

                    // Close the screen to let the player feed
                    hide();
                },
                // Condition for button to be active
                () -> (buddy.getDroppedFood() == null) && !buddy.isSleeping && !buddy.isPlaying && !buddy.isEating && !buddy.isPooping
        ));

        // Create play button
        actionButtons.add(new BuddyGuiButton(
                this.buddy,
                buddy -> "Play",
                () -> {
                    // Only allow feeding if awakened
                    if (!buddy.hasBeenAwakened) return;
                    
                    int mouseX = MouseInput.getMouseX();
                    int mouseY = MouseInput.getMouseY();
                    
                    LOGGER.debug("Creating play ball at screen coordinates: ({}, {})", mouseX, mouseY);

                    // Create the ball with drag mode already enabled
                    PlayBall ball = new PlayBall(mouseX, mouseY, buddy);
                    buddy.setPlayBall(ball);
                    buddy.setChasingBall(true);
                    ball.stickToCursor = true;

                    // Close the screen to let the player give the ball to buddy
                    hide();
                },
                // Condition for button to be active - disable when buddy enters sleepy walk (energy <= 10)
                () -> !buddy.isSleeping && (buddy.getEnergy() > 10) && (buddy.getPlayBall() == null) && !buddy.isEating && !buddy.isPooping
        ));
        
        // Create sleep button
        actionButtons.add(new BuddyGuiButton(
                this.buddy,
                buddy -> {
                    // Show cooldown time if on cooldown
                    if (System.currentTimeMillis() < sleepButtonCooldownEnd) {
                        long secondsLeft = (sleepButtonCooldownEnd - System.currentTimeMillis()) / 1000;
                        return "Sleep (" + secondsLeft + "s)";
                    }
                    return "Sleep";
                },
                () -> {
                    // Only allow if not awakened
                    if (!buddy.hasBeenAwakened) return;
                    
                    // Check if buddy refuses to sleep (8% chance)
                    if (buddy.chanceCheck(8f)) {
                        LOGGER.debug("Buddy refuses to go to sleep!");
                        
                        // Start grumpy animation without negative effects
                        buddy.refuseSleep();
                        
                        // Set cooldown for sleep button (60 seconds)
                        sleepButtonCooldownEnd = System.currentTimeMillis() + 60000;
                        
                        // Close the screen
                        hide();
                    } else {
                        LOGGER.debug("Buddy agrees to go to sleep");
                        
                        // Start sleeping
                        buddy.startSleeping();
                        
                        // Close the screen
                        hide();
                    }
                },
                // Condition for button to be active - energy must be below 20 and not on cooldown
                () -> !buddy.isSleeping && (buddy.getEnergy() < 20) && !buddy.isEating && !buddy.isPooping && 
                      (System.currentTimeMillis() >= sleepButtonCooldownEnd)
        ));
    }

    /**
     * Shows the GUI at the specified position
     */
    public void show(int screenWidth, int screenHeight) {

        this.isMouseClicked = false;
        this.isVisible = true;

        // Calculate GUI position - centered on screen
        this.guiX = (screenWidth - SCREEN_WIDTH) / 2;
        this.guiY = (screenHeight - SCREEN_HEIGHT) / 2;

        // Update button positions
        updateButtonPositions();

        LOGGER.debug("Showing buddy leveling stats screen at ({}, {})", guiX, guiY);
    }

    /**
     * Hides the GUI
     */
    public void hide() {
        this.isVisible = false;
        LOGGER.debug("Hiding buddy leveling stats screen");
    }

    /**
     * Updates the positions of all buttons within the GUI
     */
    private void updateButtonPositions() {
        // Position close button (X) in the top right corner
        int closeButtonX = guiX + SCREEN_WIDTH - 25;
        int closeButtonY = guiY + 5;

        if (!buttons.isEmpty()) {
            buttons.get(0).setPosition(closeButtonX, closeButtonY);
        }
        
        // Position action buttons centered between status bars and right edge
        int statusBarsEndX = guiX + 20 + 150; // status bars start + bar width
        int rightEdgeX = guiX + SCREEN_WIDTH - 20; // right edge minus padding
        int actionButtonWidth = 80; // approximate button width
        int actionButtonStartX = statusBarsEndX + ((rightEdgeX - statusBarsEndX - actionButtonWidth) / 2);
        
        // Vertically center the buttons to the status bars area
        int statusBarsStartY = guiY + 50; // where status bars start
        int statusBarsHeight = 4 * (font.lineHeight + 10 + 2 + 4); // 4 bars with spacing
        int totalButtonsHeight = actionButtons.size() * 20 + (actionButtons.size() - 1) * 5; // buttons + spacing
        int actionButtonStartY = statusBarsStartY + ((statusBarsHeight - totalButtonsHeight) / 2);
        int actionButtonSpacing = 25; // Reduced from 40 for tighter spacing
        
        for (int i = 0; i < actionButtons.size(); i++) {
            int y = actionButtonStartY + (i * actionButtonSpacing);
            actionButtons.get(i).setPosition(actionButtonStartX, y);
        }
    }

    /**
     * Renders the GUI if it's visible
     */
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!isVisible) return;

        RenderSystem.enableBlend();

        // Push pose stack and move to z=400 for rendering on top of everything
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400); // Use z=400 (same as tooltips)

        // Render background
        int borderXDiff = 35;
        int borderYDiff = 27;
        graphics.blit(BACKGROUND_TEXTURE, this.guiX, this.guiY, 0.0F, 0.0F, SCREEN_WIDTH, SCREEN_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT);
        graphics.blit(BACKGROUND_BORDER_TEXTURE, this.guiX - borderXDiff, this.guiY - borderYDiff, 0.0F, 0.0F, SCREEN_BORDER_WIDTH, SCREEN_BORDER_HEIGHT, SCREEN_BORDER_WIDTH, SCREEN_BORDER_HEIGHT);

        // Render tabs
        renderTabs(graphics, mouseX, mouseY, partial);

        // Render content based on current tab
        switch (currentTab) {
            case TAB_STATS:
                renderStatsTab(graphics, mouseX, mouseY, partial);
                break;
            case TAB_ACHIEVEMENTS:
                renderAchievementsTab(graphics, mouseX, mouseY, partial);
                break;
        }

        // Render close button
        for (BuddyGuiButton button : buttons) {
            button.render(graphics, mouseX, mouseY, partial);
        }

        // Pop pose stack
        graphics.pose().popPose();

    }

    /**
     * Renders the tabs at the top of the screen
     */
    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        Font font = Minecraft.getInstance().font;
        int tabWidth = 80;
        int tabHeight = 20;
        int tabStartX = guiX + 5;
        int tabY = guiY - 8; // Positioned 40% outside the GUI (8 pixels out of 20)

        String[] tabNames = {"Stats", "Achievements"};

        for (int i = 0; i < tabNames.length; i++) {
            int tabX = tabStartX + (i * tabWidth);
            boolean isSelected = (i == currentTab);

            // Draw tab button background using proper button textures
            ResourceLocation buttonTexture = isSelected ? TAB_BUTTON_TEXTURE_SELECTED : TAB_BUTTON_TEXTURE_NORMAL;
            
            // Render button background
            graphics.blit(buttonTexture, tabX, tabY, 0.0F, 0.0F, tabWidth, tabHeight, tabWidth, tabHeight);

            // Draw tab text
            int textColor = isSelected ? 0xFFFFFF : 0xAAAAAA;
            int textX = tabX + (tabWidth - font.width(tabNames[i])) / 2;
            int textY = tabY + (tabHeight - font.lineHeight) / 2;
            graphics.drawString(font, tabNames[i], textX, textY, textColor);
        }
    }

    /**
     * Renders the stats tab content
     */
    private void renderStatsTab(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        Font font = Minecraft.getInstance().font;
        int contentStartX = guiX + 20;
        int contentStartY = guiY + 30;

        // Draw title
        String title = "Status & Stats";
        graphics.drawString(font, title, guiX + (SCREEN_WIDTH - font.width(title)) / 2, contentStartY, 0xFFFFFF);

        // Draw status bars (moved from BuddyGui)
        renderStatusBars(graphics, contentStartX, contentStartY + 20);
        
        // Update button states before rendering
        for (BuddyGuiButton button : actionButtons) {
            button.updateActiveState();
        }
        
        // Render action buttons (feed and play)
        for (BuddyGuiButton button : actionButtons) {
            // Update button active state before rendering
            button.updateActiveState();
            button.render(graphics, mouseX, mouseY, partial);
        }
        
        // Draw separator line - moved further down to avoid overlapping with status bars
        graphics.fill(contentStartX, contentStartY + 130, contentStartX + SCREEN_WIDTH - 40, contentStartY + 131, 0x80FFFFFF);

        // Draw level and XP - moved further down
        String levelText = "Level: " + levelingManager.getCurrentLevel();
        graphics.drawString(font, levelText, contentStartX, contentStartY + 140, 0xFFFFFF);

        // Draw XP bar - moved further down
        int xpBarX = contentStartX + 80;
        int xpBarY = contentStartY + 140;
        int xpBarWidth = 150;
        int xpBarHeight = 10;

        // Background
        graphics.fill(xpBarX, xpBarY, xpBarX + xpBarWidth, xpBarY + xpBarHeight, 0x80000000);

        // Fill based on progress to next level
        int progressPercentage = levelingManager.getLevelProgressPercentage();
        int fillWidth = (xpBarWidth * progressPercentage) / 100;
        if (fillWidth > 0) {
            graphics.fill(xpBarX, xpBarY, xpBarX + fillWidth, xpBarY + xpBarHeight, 0xFF00FF00);
        }

        // XP text - moved further down
        String xpText = levelingManager.getExperience() + " XP";
        if (levelingManager.getCurrentLevel() < 30) {
            xpText += " / Next Level: " + levelingManager.getExperienceForNextLevel() + " XP";
        } else {
            xpText += " (Max Level)";
        }
        graphics.drawString(font, xpText, contentStartX, contentStartY + 155, 0xFFFFFF);

        // Removed attribute points, titles and buttons
    }

    /**
     * Renders the achievements tab content
     */
    private void renderAchievementsTab(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        Font font = Minecraft.getInstance().font;
        int contentStartX = guiX + 20;
        int contentStartY = guiY + 30;

        // Draw title
        String title = "Achievements";
        graphics.drawString(font, title, guiX + (SCREEN_WIDTH - font.width(title)) / 2, contentStartY, 0xFFFFFF);

        // Create a scrollable list of achievements
        int listStartY = contentStartY + 20;
        int listItemHeight = 25;
        int listWidth = SCREEN_WIDTH - 45; // Reduced by 5 pixels
        int maxVisibleItems = 6;

        // Get achievements and sort by tier
        List<BuddyAchievement> achievementsList = new ArrayList<>();
        for (Map.Entry<BuddyAchievement.AchievementType, BuddyAchievement> entry : 
                levelingManager.getAchievements().entrySet()) {
            achievementsList.add(entry.getValue());
        }
        
        achievementsList.sort((a1, a2) -> {
            // First sort by unlocked status (unlocked first)
            if (a1.isUnlocked() != a2.isUnlocked()) {
                return a1.isUnlocked() ? -1 : 1;
            }
            // Then sort by tier
            return Integer.compare(a1.getType().getTier(), a2.getType().getTier());
        });

        // Calculate max scroll offset
        int maxScrollOffset = Math.max(0, achievementsList.size() - maxVisibleItems) * listItemHeight;
        achievementsScrollOffset = Math.min(achievementsScrollOffset, maxScrollOffset);

        // Set up scissor area for the achievement list
        int scissorY = listStartY;
        int scissorHeight = maxVisibleItems * listItemHeight;
        graphics.enableScissor(contentStartX - 5, scissorY, contentStartX + listWidth, scissorY + scissorHeight);

        // Draw achievement items
        for (int i = 0; i < achievementsList.size(); i++) {
            int itemY = listStartY + (i * listItemHeight) - achievementsScrollOffset;
            
            // Only skip if completely out of view (with some margin for safety)
            if (itemY + listItemHeight < scissorY - 50 || itemY > scissorY + scissorHeight + 50) {
                continue;
            }
            
            BuddyAchievement achievement = achievementsList.get(i);
            boolean isUnlocked = achievement.isUnlocked();
            
            // Draw item background
            int bgColor = isUnlocked ? 0x8000FF00 : 0x80FF0000;
            graphics.fill(contentStartX, itemY, contentStartX + listWidth, itemY + listItemHeight - 2, bgColor);
            
            // Draw achievement name with scrolling if needed
            String name = achievement.getType().getName();
            int nameColor = isUnlocked ? 0xFFFFFF : 0xAAAAAA;
            
            // Calculate available width for name
            int nameMaxX = contentStartX + listWidth - 5;
            
            // Render name with scrolling if it's too long
            renderScrollingString(graphics, font, name, contentStartX + 5, itemY + 2, nameMaxX, itemY + 2 + font.lineHeight, nameColor);
            
            // Draw achievement description with scrolling if needed
            String description = achievement.getDescription();
            if (!isUnlocked) {
                description = "???" + (achievement.getType().getTier() > 2 ? " (Tier " + achievement.getType().getTier() + ")" : "");
            }
            
            // Calculate available width for description (leave space for reward text)
            int descriptionMaxX = contentStartX + listWidth - 5;
            if (isUnlocked && achievement.getExperienceReward() > 0) {
                String rewardText = "+" + achievement.getExperienceReward() + " XP";
                descriptionMaxX -= font.width(rewardText) + 10; // Leave space for reward text
            }
            
            // Render description with scrolling if it's too long
            renderScrollingString(graphics, font, description, contentStartX + 5, itemY + 14, descriptionMaxX, itemY + 14 + font.lineHeight, nameColor);
            
            // Draw reward info if unlocked
            if (isUnlocked && achievement.getExperienceReward() > 0) {
                String rewardText = "+" + achievement.getExperienceReward() + " XP";
                graphics.drawString(font, rewardText, contentStartX + listWidth - font.width(rewardText) - 5, itemY + 5, 0xFFFF00);
            }
        }

        // Disable scissor after drawing all achievements
        graphics.disableScissor();

        // Draw scroll indicators if needed (outside scissor area)
        boolean canScrollUp = achievementsScrollOffset > 0;
        boolean canScrollDown = achievementsScrollOffset < maxScrollOffset;

        if (canScrollUp) {
            graphics.drawString(font, "▲", guiX + SCREEN_WIDTH - 20, listStartY - 5, 0xFFFFFF);
        }
        if (canScrollDown) {
            graphics.drawString(font, "▼", guiX + SCREEN_WIDTH - 20, listStartY + (maxVisibleItems * listItemHeight) + 5, 0xFFFFFF);
        }
    }

    /**
     * Handles mouse clicks on the GUI
     *
     * @return true if the click was handled by the GUI, false otherwise
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (!isVisible) return false;

        // Mark mouse as clicked for tab selection
        isMouseClicked = true;

        // First check if clicking on tabs (even if outside main GUI bounds)
        int tabWidth = 80;
        int tabHeight = 20;
        int tabStartX = guiX + 5;
        int tabY = guiY - 8; // Tabs are positioned outside GUI
        
        // Check each tab button
        for (int i = 0; i < 2; i++) { // 2 tabs: Stats and Achievements
            int tabX = tabStartX + (i * tabWidth);
            if (mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
                // Tab was clicked, switch to it
                currentTab = i;
                updateButtonPositions();
                return true; // Consume the click event
            }
        }

        // Check if click is within GUI bounds
        if (mouseX >= guiX && mouseX < guiX + SCREEN_WIDTH &&
                mouseY >= guiY && mouseY < guiY + SCREEN_HEIGHT) {

            // Handle clicks based on current tab
            switch (currentTab) {
                case TAB_STATS:
                    // Removed attribute button click handling
                    break;
                    
                case TAB_ACHIEVEMENTS:
                    // Check for scroll up/down clicks
                    int listStartY = guiY + 50;
                    int maxVisibleItems = 6;
                    int listItemHeight = 25;
                    if (mouseX >= guiX + SCREEN_WIDTH - 20) {
                        if (mouseY >= listStartY - 10 && mouseY <= listStartY + 10) {
                            // Scroll up
                            achievementsScrollOffset = Math.max(0, achievementsScrollOffset - listItemHeight);
                            return true;
                        } else if (mouseY >= listStartY + (maxVisibleItems * listItemHeight) && 
                                  mouseY <= listStartY + (maxVisibleItems * listItemHeight) + 20) {
                            // Scroll down
                            achievementsScrollOffset += listItemHeight;
                            return true;
                        }
                    }
                    break;
            }

            // Check if in stats tab and clicked on action buttons
            if (currentTab == TAB_STATS) {
                for (BuddyGuiButton actionButton : actionButtons) {
                    actionButton.updateActiveState();
                    if (actionButton.isActive() && actionButton.isMouseOver(mouseX, mouseY)) {
                        actionButton.onClick();
                        return true;
                    }
                }
            }
            
            // Check for button clicks
            for (BuddyGuiButton button2 : buttons) {
                if (button2.isMouseOver(mouseX, mouseY)) {
                    button2.onClick();
                    return true;
                }
            }

            // If clicked inside GUI but not on a button, consume the click
            return true;
        } else {
            // If clicked outside GUI, close it
            hide();
            return true;
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        this.isMouseClicked = false;

        return false;

    }

    /**
     * Called when the mouse wheel is scrolled
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (!isVisible) return false;

        // Check if mouse is within GUI bounds
        if (mouseX >= guiX && mouseX < guiX + SCREEN_WIDTH &&
                mouseY >= guiY && mouseY < guiY + SCREEN_HEIGHT) {

            // Handle scrolling based on current tab
            switch (currentTab) {
                case TAB_ACHIEVEMENTS:
                    // Scroll the achievements list
                    achievementsScrollOffset = Math.max(0, achievementsScrollOffset - (int)(deltaY * 20));
                    return true;
            }

            return true;
        }
        
        return false;
    }

    /**
     * @return Whether this screen is currently visible
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Renders a scrolling string when it's too long to fit in the given bounds.
     * Adapted from AbstractWidget.renderScrollingString()
     */
    private static void renderScrollingString(GuiGraphics graphics, Font font, String text, int minX, int minY, int maxX, int maxY, int color) {
        int textWidth = font.width(text);
        int availableWidth = maxX - minX;
        
        if (textWidth > availableWidth) {
            // Text is too long, apply scrolling
            int overflow = textWidth - availableWidth;
            double time = (double)System.currentTimeMillis() / 1000.0;
            double scrollPeriod = Math.max((double)overflow * 0.5, 3.0);
            double scrollProgress = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * time / scrollPeriod)) / 2.0 + 0.5;
            double scrollOffset = scrollProgress * (double)overflow;
            
            // Enable scissor to clip the text
            graphics.enableScissor(minX, minY, maxX, maxY);
            graphics.drawString(font, text, minX - (int)scrollOffset, minY, color);
            graphics.disableScissor();
        } else {
            // Text fits, just draw it normally
            graphics.drawString(font, text, minX, minY, color);
        }
    }

    // Removed AttributeButton class

    // SkillButton class removed
    
    /**
     * Renders the buddy's status bars in the stats screen
     * Adapted from BuddyGui.renderStatusBars()
     */
    private void renderStatusBars(GuiGraphics graphics, int startX, int startY) {
        int barWidth = 150;
        int barHeight = 10;
        Font font = Minecraft.getInstance().font;

        // Calculate spacing using font line height
        int labelHeight = font.lineHeight;
        int verticalGap = 4; // Gap between components
        int totalItemHeight = labelHeight + barHeight + verticalGap;

        // Draw each status bar with proper spacing
        for (int i = 0; i < 4; i++) {
            int currentY = startY + (totalItemHeight * i);

            // Draw the label
            String label;
            int color;

            switch (i) {
                case 0:
                    label = "Hunger";
                    color = 0xFFFF5050; // Red
                    break;
                case 1:
                    label = "Happiness";
                    color = 0xFF50FF50; // Green
                    break;
                case 2:
                    label = "Energy";
                    color = 0xFF5050FF; // Blue
                    break;
                case 3:
                    label = "Fun";
                    color = 0xFFC850FF; // Purple
                    break;
                default:
                    label = "";
                    color = 0xFFFFFFFF;
            }

            graphics.drawString(font, label, startX, currentY, color);

            // Draw the bar below its label
            int barY = currentY + labelHeight + 2;
            float fillAmount = 0;
            DrawableColor barColor;

            switch (i) {
                case 0:
                    fillAmount = buddy.getHunger() / 100f;
                    barColor = DrawableColor.of(255, 80, 80);
                    break;
                case 1:
                    fillAmount = buddy.getHappiness() / 100f;
                    barColor = DrawableColor.of(80, 255, 80);
                    break;
                case 2:
                    fillAmount = buddy.getEnergy() / 100f;
                    barColor = DrawableColor.of(80, 80, 255);
                    break;
                case 3:
                    fillAmount = buddy.getFunLevel() / 100f;
                    barColor = DrawableColor.of(200, 80, 255);
                    break;
                default:
                    fillAmount = 0;
                    barColor = DrawableColor.of(255, 255, 255);
            }

            // Bar background
            graphics.fill(startX, barY, startX + barWidth, barY + barHeight, 0x80000000);

            // Bar fill
            int fillWidth = Math.max(1, (int)(barWidth * fillAmount));
            if (fillWidth > 0) {
                graphics.fill(startX, barY, startX + fillWidth, barY + barHeight, barColor.getColorInt());
            }
        }
    }
}