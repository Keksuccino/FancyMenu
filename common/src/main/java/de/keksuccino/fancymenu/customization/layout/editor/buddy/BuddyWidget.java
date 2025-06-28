package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.items.Poop;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper to integrate the TamagotchiBuddy with any Minecraft screen
 */
public class BuddyWidget extends AbstractContainerEventHandler implements Renderable, NarratableEntry, FancyMenuUiComponent {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Buddy buddy;
    private final List<GuiEventListener> unusedDummyChildren = new ArrayList<>(); // don't use this and handle event method calls manually instead
    private int screenWidth;
    private int screenHeight;

    // Flag to track if buddy has been initialized with proper screen size
    private boolean fullyInitialized = false;
    
    public BuddyWidget(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        LOGGER.debug("Creating new TamagotchiEasterEgg with screen size: {}x{}", screenWidth, screenHeight);
        this.buddy = new Buddy(screenWidth, screenHeight);
        
        // Only try to load the state if we have valid screen dimensions
        if (screenWidth > 0 && screenHeight > 0) {
            loadBuddyState();
            fullyInitialized = true;
        } else {
            LOGGER.debug("Delaying buddy state loading until screen size is properly set");
        }
    }
    
    /**
     * Helper method to load the buddy state
     */
    private void loadBuddyState() {
        // Try to load saved state
        if (!buddy.loadState()) {
            LOGGER.debug("No saved state found, starting with default buddy state");
        } else {
            LOGGER.debug("Successfully loaded saved buddy state");
            
            // Log poop positions after loading
            List<Poop> poops = buddy.getPoops();
            if (poops.isEmpty()) {
                LOGGER.debug("No poops loaded");
            } else {
                LOGGER.debug("Loaded {} poops, current positions:", poops.size());
                for (int i = 0; i < poops.size(); i++) {
                    Poop poop = poops.get(i);
                    LOGGER.debug("  Poop {}: ({}, {})", i+1, poop.getX(), poop.getY());
                }
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render the buddy (including radial menu when active)
        buddy.render(graphics, mouseX, mouseY, partialTick);
    }

    // Auto-save timer
    private int saveTimer = 0;
    private static final int SAVE_INTERVAL = 2400; // Save every 2 minutes (20 ticks/sec * 120 sec) - increased from 1 minute
    
    public void tick() {
        buddy.tick();
        
        // Handle auto-saving
        saveTimer++;
        if (saveTimer >= SAVE_INTERVAL) {
            saveTimer = 0;
            buddy.saveState();
            LOGGER.debug("Auto-saved buddy state");
            
            // Log current poop positions for debugging
            List<Poop> poops = buddy.getPoops();
            if (!poops.isEmpty()) {
                LOGGER.debug("Current poop positions after save:");
                for (int i = 0; i < poops.size(); i++) {
                    Poop poop = poops.get(i);
                    LOGGER.debug("  Poop {}: ({}, {})", i+1, poop.getX(), poop.getY());
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.buddy.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.buddy.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.buddy.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        return this.buddy.mouseScrolled(mouseX, mouseY, deltaY);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return unusedDummyChildren;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(0, 0, screenWidth, screenHeight);
    }

    public void setScreenSize(int width, int height) {
        LOGGER.debug("TamagotchiEasterEgg screen size changed: {}x{} -> {}x{}", 
                    this.screenWidth, this.screenHeight, width, height);
        
        // Update screen dimensions
        this.screenWidth = width;
        this.screenHeight = height;
        buddy.setScreenSize(width, height);
        
        // If we have valid dimensions and haven't loaded the buddy state yet, do it now
        if (!fullyInitialized && width > 0 && height > 0) {
            LOGGER.debug("Screen size now valid, loading buddy state");
            loadBuddyState();
            fullyInitialized = true;
        }
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }
    
    /**
     * Saves the buddy state and prepares for closing
     * Call this method when the buddy is about to be removed from the screen
     */
    public void cleanup() {
        LOGGER.debug("TamagotchiEasterEgg cleanup - saving buddy state");
        buddy.saveState();
        
        // Also save leveling data if available
        if (buddy.getLevelingManager() != null) {
            LOGGER.debug("Saving buddy leveling data");
            buddy.getLevelingManager().saveState();
        }
    }

}