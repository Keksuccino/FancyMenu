package de.keksuccino.fancymenu.customization.layout.editor.buddy.gui;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.FoodItem;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.PlayBall;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddyAchievement;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.LevelingManager;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
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
public class LevelingStatsScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    // GUI Constants
    private static final int SCREEN_WIDTH = 320;
    private static final int SCREEN_HEIGHT = 240;
    private static final int BORDER_SIZE = 7;

    // GUI Texture
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/leveling_gui.png");
    private static final ResourceLocation TABS_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/leveling_tabs.png");

    // Tab Indices
    private static final int TAB_STATS = 0;
    private static final int TAB_ACHIEVEMENTS = 1;

    // Reference to the buddy and its leveling manager
    private final TamagotchiBuddy buddy;
    private final LevelingManager levelingManager;

    // GUI state
    private boolean isVisible = false;
    private int guiX;
    private int guiY;
    private int currentTab = TAB_STATS;
    private final List<BuddyGuiButton> buttons = new ArrayList<>();
    // Removed attribute buttons list
    private final List<BuddyGuiButton> actionButtons = new ArrayList<>();
    // Removed skill buttons and scroll offset
    private int achievementsScrollOffset = 0;

    // Mouse handling
    private boolean isMouseClicked = false;

    /**
     * Creates a new leveling stats screen.
     *
     * @param buddy The buddy this screen is for
     * @param levelingManager The leveling manager to display stats from
     */
    public LevelingStatsScreen(@NotNull TamagotchiBuddy buddy, @NotNull LevelingManager levelingManager) {
        this.buddy = buddy;
        this.levelingManager = levelingManager;
        initButtons();
    }

    /**
     * Initializes the GUI buttons
     */
    private void initButtons() {

        buttons.clear();

        // Close button
        buttons.add(new BuddyGuiButton(
                this.buddy,
                buddy -> "Close",
                this::hide,
                () -> true
        ));

        actionButtons.clear();

        // Create feed button
        actionButtons.add(new BuddyGuiButton(
                this.buddy,
                buddy -> "Feed",
                () -> {
                    int mouseX = MouseInput.getMouseX();
                    int mouseY = MouseInput.getMouseY();
                    
                    LOGGER.info("Creating food at screen coordinates: ({}, {})", mouseX, mouseY);

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
                    int mouseX = MouseInput.getMouseX();
                    int mouseY = MouseInput.getMouseY();
                    
                    LOGGER.info("Creating play ball at screen coordinates: ({}, {})", mouseX, mouseY);

                    // Create the ball with drag mode already enabled
                    PlayBall ball = new PlayBall(mouseX, mouseY, buddy);
                    buddy.setPlayBall(ball);
                    buddy.setChasingBall(true);
                    ball.stickToCursor = true;

                    // Close the screen to let the player give the ball to buddy
                    hide();
                },
                // Condition for button to be active
                () -> !buddy.isSleeping && (buddy.getEnergy() > 20) && (buddy.getPlayBall() == null) && !buddy.isEating && !buddy.isPooping
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

        LOGGER.info("Showing buddy leveling stats screen at ({}, {})", guiX, guiY);
    }

    /**
     * Hides the GUI
     */
    public void hide() {
        this.isVisible = false;
        LOGGER.info("Hiding buddy leveling stats screen");
    }

    /**
     * Updates the positions of all buttons within the GUI
     */
    private void updateButtonPositions() {
        // Position close button in bottom right
        int closeButtonX = guiX + SCREEN_WIDTH - 60;
        int closeButtonY = guiY + SCREEN_HEIGHT - 30;

        if (!buttons.isEmpty()) {
            buttons.get(0).setPosition(closeButtonX, closeButtonY);
        }
        
        // Position action buttons (feed and play) next to status bars
        int actionButtonStartX = guiX + 190;
        int actionButtonStartY = guiY + 50;
        int actionButtonSpacing = 35;
        
        for (int i = 0; i < actionButtons.size(); i++) {
            int y = actionButtonStartY + (i * actionButtonSpacing);
            actionButtons.get(i).setPosition(actionButtonStartX, y);
        }

        // Removed attribute button positioning

        // Skill buttons removed
    }

    /**
     * Renders the GUI if it's visible
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isVisible) return;

        // Push pose stack and move to z=400 for rendering on top of everything
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400); // Use z=400 (same as tooltips)

        // Use nine-slice rendering for the background
        RenderingUtils.blitNineSlicedTexture(
                graphics,
                GUI_TEXTURE,
                guiX, guiY,
                SCREEN_WIDTH, SCREEN_HEIGHT,
                256, 256,  // Texture dimensions
                BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE,  // Border sizes
                -1  // No color tint
        );

        // Render tabs
        renderTabs(graphics, mouseX, mouseY);

        // Render content based on current tab
        switch (currentTab) {
            case TAB_STATS:
                renderStatsTab(graphics, mouseX, mouseY);
                break;
            case TAB_ACHIEVEMENTS:
                renderAchievementsTab(graphics, mouseX, mouseY);
                break;
        }

        // Render close button
        for (BuddyGuiButton button : buttons) {
            button.render(graphics, mouseX, mouseY);
        }

        // Pop pose stack
        graphics.pose().popPose();
    }

    /**
     * Renders the tabs at the top of the screen
     */
    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int tabWidth = 70;
        int tabHeight = 20;
        int tabStartX = guiX + 15;
        int tabY = guiY;

        String[] tabNames = {"Stats", "Achievements"};

        for (int i = 0; i < tabNames.length; i++) {
            int tabX = tabStartX + (i * tabWidth);
            boolean isSelected = (i == currentTab);

            // Draw tab background
            int u = isSelected ? 0 : 70;
            int v = 0;
            graphics.blit(RenderType::guiTextured, TABS_TEXTURE, tabX, tabY, u, v, tabWidth, tabHeight, 256, 256);

            // Draw tab text
            int textColor = isSelected ? 0xFFFFFF : 0xAAAAAA;
            int textX = tabX + (tabWidth - font.width(tabNames[i])) / 2;
            int textY = tabY + (tabHeight - font.lineHeight) / 2;
            graphics.drawString(font, tabNames[i], textX, textY, textColor);

            // Check for tab click
            if (mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
                if (isMouseClicked) {
                    currentTab = i;
                    isMouseClicked = false;
                    updateButtonPositions();
                }
            }
        }
    }

    /**
     * Renders the stats tab content
     */
    private void renderStatsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int contentStartX = guiX + 20;
        int contentStartY = guiY + 30;

        // Draw title
        String title = "Buddy Status & Stats";
        graphics.drawString(font, title, guiX + (SCREEN_WIDTH - font.width(title)) / 2, contentStartY, 0xFFFFFF);

        // Draw status bars (moved from BuddyGui)
        renderStatusBars(graphics, contentStartX, contentStartY + 20);
        
        // Render action buttons (feed and play)
        for (BuddyGuiButton button : actionButtons) {
            // Update button active state before rendering
            button.updateActiveState();
            button.render(graphics, mouseX, mouseY);
        }
        
        // Draw separator line
        graphics.fill(contentStartX, contentStartY + 100, contentStartX + SCREEN_WIDTH - 40, contentStartY + 101, 0x80FFFFFF);

        // Draw level and XP
        String levelText = "Level: " + levelingManager.getCurrentLevel();
        graphics.drawString(font, levelText, contentStartX, contentStartY + 110, 0xFFFFFF);

        // Draw XP bar
        int xpBarX = contentStartX + 80;
        int xpBarY = contentStartY + 110;
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

        // XP text
        String xpText = levelingManager.getExperience() + " XP";
        if (levelingManager.getCurrentLevel() < 30) {
            xpText += " / Next Level: " + levelingManager.getExperienceForNextLevel() + " XP";
        } else {
            xpText += " (Max Level)";
        }
        graphics.drawString(font, xpText, contentStartX, contentStartY + 125, 0xFFFFFF);

        // Removed attribute points, titles and buttons
    }

    /**
     * Renders the achievements tab content
     */
    private void renderAchievementsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int contentStartX = guiX + 20;
        int contentStartY = guiY + 30;

        // Draw title
        String title = "Buddy Achievements";
        graphics.drawString(font, title, guiX + (SCREEN_WIDTH - font.width(title)) / 2, contentStartY, 0xFFFFFF);

        // Create a scrollable list of achievements
        int listStartY = contentStartY + 20;
        int listItemHeight = 25;
        int listWidth = SCREEN_WIDTH - 40;
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

        // Draw achievement items
        for (int i = 0; i < achievementsList.size(); i++) {
            int itemY = listStartY + (i * listItemHeight) - achievementsScrollOffset;
            
            // Skip if not visible
            if (itemY < listStartY || itemY + listItemHeight > listStartY + (maxVisibleItems * listItemHeight)) {
                continue;
            }
            
            BuddyAchievement achievement = achievementsList.get(i);
            boolean isUnlocked = achievement.isUnlocked();
            
            // Draw item background
            int bgColor = isUnlocked ? 0x8000FF00 : 0x80FF0000;
            graphics.fill(contentStartX, itemY, contentStartX + listWidth, itemY + listItemHeight - 2, bgColor);
            
            // Draw achievement name
            String name = achievement.getType().getName();
            int nameColor = isUnlocked ? 0xFFFFFF : 0xAAAAAA;
            graphics.drawString(font, name, contentStartX + 5, itemY + 2, nameColor);
            
            // Draw achievement description
            String description = achievement.getDescription();
            if (!isUnlocked) {
                description = "???" + (achievement.getType().getTier() > 2 ? " (Tier " + achievement.getType().getTier() + ")" : "");
            }
            graphics.drawString(font, description, contentStartX + 5, itemY + 14, nameColor);
            
            // Draw reward info if unlocked
            if (isUnlocked && achievement.getExperienceReward() > 0) {
                String rewardText = "+" + achievement.getExperienceReward() + " XP";
                graphics.drawString(font, rewardText, contentStartX + listWidth - font.width(rewardText) - 5, itemY + 5, 0xFFFF00);
            }
        }

        // Draw scroll indicators if needed
        boolean canScrollUp = achievementsScrollOffset > 0;
        boolean canScrollDown = achievementsScrollOffset < maxScrollOffset;

        if (canScrollUp) {
            graphics.drawString(font, "▲", guiX + SCREEN_WIDTH - 20, contentStartY + 25, 0xFFFFFF);
        }
        if (canScrollDown) {
            graphics.drawString(font, "▼", guiX + SCREEN_WIDTH - 20, listStartY + (maxVisibleItems * listItemHeight) - 15, 0xFFFFFF);
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
                        if (mouseY >= listStartY && mouseY <= listStartY + 20) {
                            // Scroll up
                            achievementsScrollOffset = Math.max(0, achievementsScrollOffset - listItemHeight);
                            return true;
                        } else if (mouseY >= listStartY + (maxVisibleItems * listItemHeight) - 20 && 
                                  mouseY <= listStartY + (maxVisibleItems * listItemHeight)) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
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