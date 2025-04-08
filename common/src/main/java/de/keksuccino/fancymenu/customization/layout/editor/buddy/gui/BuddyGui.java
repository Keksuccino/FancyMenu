package de.keksuccino.fancymenu.customization.layout.editor.buddy.gui;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.FoodItem;
import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.PlayBall;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static de.keksuccino.fancymenu.customization.layout.editor.buddy.gui.BuddyGuiButton.*;

/**
 * A GUI interface for interacting with the TamagotchiBuddy
 */
public class BuddyGui {

    private static final Logger LOGGER = LogManager.getLogger();

    // GUI Constants
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 200; // Increased height to accommodate buttons below status bars
    private static final int STATUS_BAR_WIDTH = 150;
    private static final int STATUS_BAR_HEIGHT = 10;

    // GUI Texture
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("fancymenu", "textures/buddy/buddy_gui.png");

    // Nine-slice border sizes
    private static final int BORDER_SIZE = 7;

    // Reference to the buddy
    private final TamagotchiBuddy buddy;

    // GUI state
    private boolean isVisible = false;
    private int guiX;
    private int guiY;
    private final List<BuddyGuiButton> buttons = new ArrayList<>();

    public BuddyGui(TamagotchiBuddy buddy) {
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
        buttons.add(new BuddyGuiButton(
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

                    // Close the GUI to let the player feed
                    hide();
                },
                // Condition for button to be active
                () -> (buddy.getDroppedFood() == null) && !buddy.isSleeping && !buddy.isPlaying && !buddy.isEating && !buddy.isPooping
        ));

        // Create play button
        buttons.add(new BuddyGuiButton(
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

                    // Close the GUI to let the player give the ball to buddy
                    hide();
                },
                // Condition for button to be active
                () -> !buddy.isSleeping && (buddy.getEnergy() > 20) && (buddy.getPlayBall() == null) && !buddy.isEating && !buddy.isPooping
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
            BuddyGuiButton button = buttons.get(i);
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

        // Use the built-in nine-slice rendering
        RenderingUtils.blitNineSlicedTexture(
                graphics,
                GUI_TEXTURE,
                guiX, guiY,
                GUI_WIDTH, GUI_HEIGHT,
                256, 256,  // Texture dimensions (assuming standard 256x256 texture)
                BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE  // Border sizes (top, right, bottom, left)
        );

        // Draw title
        String title = "Buddy Status";
        Font font = Minecraft.getInstance().font;
        int titleX = guiX + (GUI_WIDTH - font.width(title)) / 2;
        int titleY = guiY + 20; // Moved down from 10 to 20 pixels
        graphics.drawString(font, title, titleX, titleY, 0xFFFFFFFF);

        // Draw status bars
        renderStatusBars(graphics);

        // Draw buttons
        for (BuddyGuiButton button : buttons) {
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
            for (BuddyGuiButton buddyGuiButton : buttons) {
                // Make sure to update the active state before checking
                buddyGuiButton.updateActiveState();
                if (buddyGuiButton.isActive() && buddyGuiButton.isMouseOver(mouseX, mouseY)) {
                    buddyGuiButton.onClick();
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

    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Adds a new button to the GUI
     */
    public void addButton(@NotNull TamagotchiBuddy buddy, @NotNull BuddyGuiButton.ButtonNameSupplier nameSupplier, @NotNull Runnable action, @Nullable BooleanSupplier activeCondition) {
        buttons.add(new BuddyGuiButton(buddy, nameSupplier, action, activeCondition));
        updateButtonPositions();
    }

}