package de.keksuccino.fancymenu.customization.layout.editor.buddy.gui;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddyAttribute;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.BuddyAchievement;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.leveling.LevelingManager;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final List<AttributeButton> attributeButtons = new ArrayList<>();
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
        // Close button
        buttons.add(new BuddyGuiButton(
                this.buddy,
                buddy -> "Close",
                this::hide,
                () -> true
        ));

        // Initialize attribute buttons
        for (BuddyAttribute.AttributeType type : BuddyAttribute.AttributeType.values()) {
            attributeButtons.add(new AttributeButton(type));
        }
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

        // Position attribute buttons in a grid
        int attributeStartX = guiX + 20;
        int attributeStartY = guiY + 70;
        int attributeSpacingX = 140;
        int attributeSpacingY = 25;

        for (int i = 0; i < attributeButtons.size(); i++) {
            int row = i / 2;
            int col = i % 2;
            int x = attributeStartX + (col * attributeSpacingX);
            int y = attributeStartY + (row * attributeSpacingY);
            attributeButtons.get(i).setPosition(x, y);
        }

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
        String title = "Buddy Statistics";
        graphics.drawString(font, title, guiX + (SCREEN_WIDTH - font.width(title)) / 2, contentStartY, 0xFFFFFF);

        // Draw level and XP
        String levelText = "Level: " + levelingManager.getCurrentLevel();
        graphics.drawString(font, levelText, contentStartX, contentStartY + 20, 0xFFFFFF);

        // Draw XP bar
        int xpBarX = contentStartX + 80;
        int xpBarY = contentStartY + 20;
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
        graphics.drawString(font, xpText, contentStartX, contentStartY + 35, 0xFFFFFF);

        // Draw attribute points
        String attrPointsText = "Attribute Points: " + levelingManager.getUnspentAttributePoints();
        graphics.drawString(font, attrPointsText, contentStartX, contentStartY + 50, 0xFFFFFF);

        // Skill points removed

        // Draw attributes
        String attributesTitle = "Attributes";
        graphics.drawString(font, attributesTitle, contentStartX, contentStartY + 65, 0xFFFFFF);

        // Render attribute buttons if in stats tab
        for (AttributeButton button : attributeButtons) {
            button.render(graphics, mouseX, mouseY);
        }
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
                    for (AttributeButton attributeButton : attributeButtons) {
                        if (attributeButton.isMouseOver(mouseX, mouseY)) {
                            attributeButton.onClick();
                            return true;
                        }
                    }
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

    /**
     * Button for attribute points allocation
     */
    private class AttributeButton {
        private final BuddyAttribute.AttributeType type;
        private int x;
        private int y;
        private static final int WIDTH = 130;
        private static final int HEIGHT = 20;

        public AttributeButton(BuddyAttribute.AttributeType type) {
            this.type = type;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
        }

        public void onClick() {
            if (levelingManager.getUnspentAttributePoints() > 0) {
                levelingManager.addAttributePoints(type, 1);
            }
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
            Font font = Minecraft.getInstance().font;
            
            // Get attribute value
            BuddyAttribute attribute = levelingManager.getAttributes().get(type);
            if (attribute == null) return;
            
            int value = attribute.getTotalValue();
            int maxValue = BuddyAttribute.MAX_ATTRIBUTE_VALUE;
            
            // Draw background
            boolean hovered = isMouseOver(mouseX, mouseY);
            boolean canImprove = levelingManager.getUnspentAttributePoints() > 0;
            
            int bgColor = hovered && canImprove ? 0x80FFFFFF : 0x80000000;
            graphics.fill(x, y, x + WIDTH, y + HEIGHT, bgColor);
            
            // Draw attribute name
            String name = type.getName();
            graphics.drawString(font, name, x + 5, y + 2, 0xFFFFFF);
            
            // Draw attribute value
            String valueText = value + "/" + maxValue;
            int valueColor = (value == maxValue) ? 0x00FF00 : 0xFFFFFF;
            graphics.drawString(font, valueText, x + WIDTH - font.width(valueText) - 5, y + 2, valueColor);
            
            // Show tooltip on hover
            if (hovered) {
                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(Component.literal(type.getName() + ": " + type.getShortDescription()));
                tooltipLines.add(Component.literal(type.getDetailedDescription()));
                
                if (canImprove) {
                    tooltipLines.add(Component.literal("§aClick to add a point"));
                }
                
                graphics.renderTooltip(font, tooltipLines, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    // SkillButton class removed
}