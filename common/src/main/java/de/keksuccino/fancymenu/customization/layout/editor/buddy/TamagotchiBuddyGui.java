package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A GUI interface for interacting with the TamagotchiBuddy
 */
public class TamagotchiBuddyGui {
    private static final Logger LOGGER = LogManager.getLogger();

    // GUI Constants
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 200; // Increased height to accommodate buttons below status bars
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int STATUS_BAR_WIDTH = 150;
    private static final int STATUS_BAR_HEIGHT = 10;
    private static final int BACKGROUND_COLOR = 0xC0000000; // Fallback semi-transparent black background
    private static final int BORDER_COLOR = 0xFF666666; // Fallback gray border

    // GUI Texture
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/buddy_gui.png");

    // Nine-slice border sizes
    private static final int BORDER_SIZE = 7;

    // Flag to use texture (set to false for debug/fallback mode)
    private static final boolean USE_TEXTURE_BACKGROUND = true;

    // Reference to the buddy
    private final TamagotchiBuddy buddy;

    // GUI state
    private boolean isVisible = false;
    private int guiX;
    private int guiY;
    private List<GuiButton> buttons = new ArrayList<>();

    // Resource locations for button textures (reusing from the radial menu)
    private static final ResourceLocation TEXTURE_FEED_BUTTON = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/feed_button.png");
    private static final ResourceLocation TEXTURE_PLAY_BUTTON = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/play_button.png");

    public TamagotchiBuddyGui(TamagotchiBuddy buddy) {
        this.buddy = buddy;
        initButtons();
    }

    /**
     * Initializes the GUI buttons
     */
    private void initButtons() {
        // Clear existing buttons to avoid duplicates
        buttons.clear();

        // Create feed button
        buttons.add(new GuiButton(
                "Feed",
                () -> {
                    // Create a food item at the cursor position and immediately make it stick to cursor
                    int mouseX = (int)Minecraft.getInstance().mouseHandler.xpos();
                    int mouseY = (int)Minecraft.getInstance().mouseHandler.ypos();

                    // Create the food with drag mode already enabled
                    FoodItem food = new FoodItem(mouseX, mouseY, buddy);
                    food.pickup(mouseX, mouseY); // Ensure it's in dragged mode
                    buddy.setDroppedFood(food);

                    // Close the GUI to let the player feed
                    hide();
                },
                // Condition for button to be active
                () -> buddy.getDroppedFood() == null
        ));

        // Create play button
        buttons.add(new GuiButton(
                "Play",
                () -> {
                    // Create a ball at the cursor position and immediately make it stick to cursor
                    int mouseX = (int)Minecraft.getInstance().mouseHandler.xpos();
                    int mouseY = (int)Minecraft.getInstance().mouseHandler.ypos();

                    // Create the ball with drag mode already enabled
                    PlayBall ball = new PlayBall(mouseX, mouseY, buddy);
                    ball.pickup(mouseX, mouseY); // Ensure it's in dragged mode
                    buddy.setPlayBall(ball);

                    // Close the GUI to let the player give the ball to buddy
                    hide();
                },
                // Condition for button to be active
                () -> !buddy.isSleeping() && buddy.getEnergy() > 20 && buddy.getPlayBall() == null
        ));

        LOGGER.info("Initialized " + buttons.size() + " GUI buttons");
    }

    /**
     * Shows the GUI at the specified position
     */
    public void show(int screenWidth, int screenHeight) {
        this.isVisible = true;

        // Calculate GUI position - centered on screen
        this.guiX = (screenWidth - GUI_WIDTH) / 2;
        this.guiY = (screenHeight - GUI_HEIGHT) / 2;

        // Update button positions
        updateButtonPositions();

        LOGGER.info("Showing buddy GUI at ({}, {})", guiX, guiY);
    }

    /**
     * Hides the GUI
     */
    public void hide() {
        this.isVisible = false;
        LOGGER.info("Hiding buddy GUI");
    }

    /**
     * Updates the positions of all buttons within the GUI
     */
    private void updateButtonPositions() {
        // Calculate the position below the status bars
        Font font = Minecraft.getInstance().font;
        int labelHeight = font.lineHeight;
        int barHeight = STATUS_BAR_HEIGHT;
        int verticalGap = 4;
        int totalItemHeight = labelHeight + barHeight + verticalGap;

        // Calculate where the status bars end
        int statusBarsEndY = guiY + 40 + (totalItemHeight * 4);

        // Add some spacing after the status bars
        int buttonsY = statusBarsEndY + 15;

        // Calculate total width needed for all buttons with spacing
        int buttonSpacing = 10; // Reduced from 20 to 10 for closer buttons
        int totalButtonsWidth = (buttons.size() * BUTTON_WIDTH) + ((buttons.size() - 1) * buttonSpacing);

        // Calculate starting X position to center all buttons
        int startX = guiX + (GUI_WIDTH - totalButtonsWidth) / 2;

        // Position each button horizontally
        for (int i = 0; i < buttons.size(); i++) {
            GuiButton button = buttons.get(i);
            int buttonX = startX + (i * (BUTTON_WIDTH + buttonSpacing));

            button.setPosition(buttonX, buttonsY);
            button.updateActiveState();
        }
    }

    /**
     * Renders the GUI if it's visible
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isVisible) return;

        // Push pose stack and move to z=400 for rendering on top of everything
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400); // Use z=400 (same as tooltips)

        if (USE_TEXTURE_BACKGROUND) {
            // Use the built-in nine-slice rendering
            RenderingUtils.blitNineSlicedTexture(
                    graphics,
                    GUI_TEXTURE,
                    guiX, guiY,
                    GUI_WIDTH, GUI_HEIGHT,
                    256, 256,  // Texture dimensions (assuming standard 256x256 texture)
                    BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE,  // Border sizes (top, right, bottom, left)
                    -1  // No color tint
            );
        } else {
            // Fallback to a basic filled rectangle
            graphics.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, BACKGROUND_COLOR);
            graphics.renderOutline(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, BORDER_COLOR);
        }

        // Draw title
        String title = "Buddy Status";
        Font font = Minecraft.getInstance().font;
        int titleX = guiX + (GUI_WIDTH - font.width(title)) / 2;
        int titleY = guiY + 20; // Moved down from 10 to 20 pixels
        graphics.drawString(font, title, titleX, titleY, 0xFFFFFFFF);

        // Draw status bars
        renderStatusBars(graphics);

        // Draw buttons
        for (GuiButton button : buttons) {
            // Update button active state before rendering
            button.updateActiveState();
            button.render(graphics, mouseX, mouseY);
        }

        // Pop pose stack
        graphics.pose().popPose();
    }

    /**
     * Renders the buddy's status bars in the GUI
     */
    private void renderStatusBars(GuiGraphics graphics) {
        int barX = guiX + (GUI_WIDTH - STATUS_BAR_WIDTH) / 2;
        int startY = guiY + 40; // Adjusted to account for lower title position
        Font font = Minecraft.getInstance().font;

        // Calculate better spacing using font line height
        int labelHeight = font.lineHeight;
        int barHeight = STATUS_BAR_HEIGHT;
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

            graphics.drawString(font, label, barX, currentY, color);

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
            graphics.fill(barX, barY, barX + STATUS_BAR_WIDTH, barY + barHeight, 0x80000000);

            // Bar fill
            int fillWidth = Math.max(1, (int)(STATUS_BAR_WIDTH * fillAmount));
            if (fillWidth > 0) {
                graphics.fill(barX, barY, barX + fillWidth, barY + barHeight, barColor.getColorInt());
            }
        }
    }

    // Helper methods have been integrated into renderStatusBars

    /**
     * Handles mouse clicks on the GUI
     *
     * @return true if the click was handled by the GUI, false otherwise
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible) return false;

        // Check if click is within GUI bounds
        if (mouseX >= guiX && mouseX < guiX + GUI_WIDTH &&
                mouseY >= guiY && mouseY < guiY + GUI_HEIGHT) {

            // Check if clicked on any button
            for (GuiButton guiButton : buttons) {
                // Make sure to update the active state before checking
                guiButton.updateActiveState();
                if (guiButton.isActive() && guiButton.isMouseOver(mouseX, mouseY)) {
                    guiButton.onClick();
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

    /**
     * Represents a button in the buddy GUI
     */
    public static class GuiButton {
        private final String name;
        private final Runnable action;
        private final BooleanSupplier activeCondition;

        private int x, y;
        private boolean active = true;

        public GuiButton(String name, Runnable action, BooleanSupplier activeCondition) {
            this.name = name;
            this.action = action;
            this.activeCondition = activeCondition;
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + BUTTON_WIDTH &&
                    mouseY >= y && mouseY < y + BUTTON_HEIGHT;
        }

        public void onClick() {
            if (active && action != null) {
                action.run();
            }
        }

        public boolean isActive() {
            return active;
        }

        public void updateActiveState() {
            this.active = activeCondition == null || activeCondition.getAsBoolean();
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
            boolean hovered = isMouseOver(mouseX, mouseY) && active;
            int backgroundColor = active ? (hovered ? 0xFF909090 : 0xFF606060) : 0xFF404040;
            int textColor = active ? 0xFFFFFFFF : 0xFFAAAAAA;
            Font font = Minecraft.getInstance().font;

            // Draw button background
            graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, backgroundColor);
            graphics.renderOutline(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 0xFF000000);

            // Draw button text
            int textX = x + (BUTTON_WIDTH - font.width(name)) / 2;
            int textY = y + (BUTTON_HEIGHT - 8) / 2;
            graphics.drawString(font, name, textX, textY, textColor);
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Adds a new button to the GUI
     */
    public void addButton(String name, Runnable action, BooleanSupplier activeCondition) {
        buttons.add(new GuiButton(name, action, activeCondition));
        updateButtonPositions();
    }
}